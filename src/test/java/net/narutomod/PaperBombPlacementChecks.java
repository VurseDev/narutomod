package net.narutomod;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.narutomod.entity.EntityPaperBombCast;
import sun.misc.Unsafe;

/** Sparse world fixture uses the real block collision shapes and production placement/clearing paths. */
public final class PaperBombPlacementChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static <T>T fixture(Class<T> type)throws Exception{
        Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
    }
    private static class Grid extends World {
        Map<BlockPos,IBlockState> blocks;boolean denied,unloaded;int writes;
        Grid(){super(null,null,null,null,false);}
        @Override protected IChunkProvider createChunkProvider(){return null;}
        @Override protected boolean isChunkLoaded(int x,int z,boolean empty){return !unloaded;}
        @Override public IBlockState getBlockState(BlockPos p){return blocks.getOrDefault(p,Blocks.AIR.getDefaultState());}
        @Override public boolean isBlockModifiable(EntityPlayer p,BlockPos pos){return !denied;}
        @Override public boolean setBlockState(BlockPos p,IBlockState state,int flags){blocks.put(p,state);writes++;return true;}
        @Override public void notifyNeighborsOfStateChange(BlockPos p,net.minecraft.block.Block b,boolean update){}
        @Override public RayTraceResult rayTraceBlocks(Vec3d a,Vec3d b,boolean liquid,boolean ignore,boolean miss){
            RayTraceResult nearest=null;double distance=Double.MAX_VALUE;
            for(Map.Entry<BlockPos,IBlockState> e:blocks.entrySet()){
                IBlockState state=e.getValue();
                if(state.getCollisionBoundingBox(this,e.getKey())==null)continue;
                RayTraceResult hit=state.collisionRayTrace(this,e.getKey(),a,b);
                if(hit!=null&&a.squareDistanceTo(hit.hitVec)<distance){nearest=hit;distance=a.squareDistanceTo(hit.hitVec);}
            }
            return nearest;
        }
    }
    private static class Caster extends EntityPlayer {
        Caster(){super(null,null);}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
        @Override public boolean canPlayerEdit(BlockPos p,EnumFacing f,ItemStack stack){return true;}
        @Override public ItemStack getHeldItemMainhand(){return ItemStack.EMPTY;}
    }
    public static int run()throws Exception{
        Grid world=fixture(Grid.class);world.blocks=new LinkedHashMap<>();
        for(int x=-5;x<=5;x++)for(int z=-2;z<=12;z++)world.blocks.put(new BlockPos(x,0,z),Blocks.STONE.getDefaultState());
        Vec3d feet=new Vec3d(.5,1,.5),eyes=feet.addVector(0,1.62,0),forward=new Vec3d(0,0,1);
        RayTraceResult circuit=PaperBombPlacement.aim(world,eyes,feet,forward,0,true);
        check(circuit!=null&&Math.abs(circuit.hitVec.z-4.5)<.01,"horizontal aim auto-places circuit four blocks ahead");
        RayTraceResult breach=PaperBombPlacement.aim(world,eyes,feet,forward,0,false);
        check(breach!=null&&Math.abs(breach.hitVec.z-3.5)<.01,"horizontal aim auto-places breach three blocks ahead");
        check(world.writes==0,"placement preview does not change the world");
        world.blocks.put(new BlockPos(0,1,4),Blocks.TALLGRASS.getDefaultState());
        check(PaperBombPlacement.aim(world,eyes,feet,forward,0,true)!=null,"grass does not block aiming");
        world.blocks.put(new BlockPos(2,1,6),Blocks.STONE.getDefaultState());
        RayTraceResult raised=PaperBombPlacement.ground(world,new Vec3d(2.5,1.035,6.5));
        check(raised!=null&&raised.hitVec.y==2,"corner follows a one-block rise");
        world.blocks.put(new BlockPos(0,0,4),Blocks.STONE_SLAB.getDefaultState());
        RayTraceResult slab=PaperBombPlacement.ground(world,new Vec3d(.5,1.035,4.5));
        check(slab!=null&&slab.hitVec.y==.5,"slab height is used, not rounded to full block");
        world.blocks.put(new BlockPos(0,2,3),Blocks.STONE.getDefaultState());
        RayTraceResult nearWall=PaperBombPlacement.aim(world,eyes,feet,forward,0,true);
        check(nearWall!=null&&nearWall.hitVec.z<3,"circuit fallback stays on near side of wall");
        RayTraceResult wall=PaperBombPlacement.aim(world,eyes,feet,forward,0,false);
        check(wall!=null&&wall.sideHit==EnumFacing.NORTH,"direct breach wall placement preserved");
        world.unloaded=true;
        check(PaperBombPlacement.aim(world,eyes,feet,forward,0,true)==null,"never search unloaded chunks");world.unloaded=false;
        check(PaperBombPlacement.ground(world,new Vec3d(20,1,20))==null,"no floating tag over void");
        BlockPos lower=new BlockPos(1,1,1),upper=lower.up(),flower=new BlockPos(2,1,1),crop=new BlockPos(3,1,1);
        world.blocks.put(lower,Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.VARIANT,BlockDoublePlant.EnumPlantType.GRASS));
        world.blocks.put(upper,Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.HALF,BlockDoublePlant.EnumBlockHalf.UPPER));
        world.blocks.put(flower,Blocks.DOUBLE_PLANT.getDefaultState().withProperty(BlockDoublePlant.VARIANT,BlockDoublePlant.EnumPlantType.SUNFLOWER));
        world.blocks.put(crop,Blocks.WHEAT.getDefaultState());
        check(PaperBombPlacement.grass(world,lower)&&PaperBombPlacement.grass(world,upper),"both double-grass halves recognized");
        check(!PaperBombPlacement.grass(world,flower)&&!PaperBombPlacement.grass(world,crop)&&!PaperBombPlacement.grass(world,new BlockPos(1,0,1)),"flowers, crops and soil excluded");
        EntityPaperBombCast cast=fixture(EntityPaperBombCast.class);cast.world=world;
        Field grass=EntityPaperBombCast.class.getDeclaredField("placementGrass");grass.setAccessible(true);
        Set<BlockPos> toClear=new LinkedHashSet<>(Arrays.asList(lower,upper,flower,crop,new BlockPos(1,0,1)));
        grass.set(cast,toClear);Caster player=fixture(Caster.class);player.world=world;
        world.denied=true;cast.clearPlacementGrass(player);
        check(world.writes==0,"protected grass not removed");
        world.denied=false;toClear.addAll(Arrays.asList(lower,upper,flower,crop,new BlockPos(1,0,1)));cast.clearPlacementGrass(player);
        check(world.getBlockState(lower).getBlock()==Blocks.AIR&&world.getBlockState(upper).getBlock()==Blocks.AIR,"both grass halves cleared together");
        check(world.writes==2,"clearing only modifies grass, not crops, flowers or solid floor");
        return checks;
    }
}
