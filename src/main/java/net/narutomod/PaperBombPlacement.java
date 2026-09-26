package net.narutomod;

import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** Read-only placement queries. Non-colliding grass is never an aiming obstacle. */
public final class PaperBombPlacement {
    private PaperBombPlacement(){}
    public static RayTraceResult ground(World world,Vec3d probe){
        // One-block steps and slabs are accepted; don't snap across cliffs or onto a distant roof.
        Vec3d from=probe.addVector(0,1.25,0),to=probe.addVector(0,-1.25,0);
        if(!loaded(world,from,to))return null;
        RayTraceResult hit=world.rayTraceBlocks(from,to,false,true,false);
        return hit!=null&&hit.sideHit==EnumFacing.UP?hit:null;
    }
    public static RayTraceResult aim(World world,Vec3d eyes,Vec3d feet,Vec3d look,float yaw,boolean circuit){
        Vec3d end=eyes.add(look.scale(circuit?10:6));
        RayTraceResult direct=loaded(world,eyes,end)?world.rayTraceBlocks(eyes,end,false,true,false):null;
        if(direct!=null&&(!circuit||direct.sideHit==EnumFacing.UP))return direct;
        // A wall blocks the fallback too: search on its near side, never through it.
        Vec3d probe=direct!=null?direct.hitVec.add(new Vec3d(direct.sideHit.getDirectionVec()).scale(.5))
            :feet.add(Vec3d.fromPitchYaw(0,yaw).scale(circuit?4:3));
        if(direct!=null)probe=new Vec3d(probe.x,feet.y,probe.z);
        RayTraceResult ground=ground(world,probe);
        if(ground==null)return null;
        Vec3d visible=ground.hitVec.addVector(0,.08,0);
        return loaded(world,eyes,visible)&&world.rayTraceBlocks(eyes,visible,false,true,false)==null?ground:null;
    }
    private static boolean loaded(World world,Vec3d start,Vec3d end){
        int steps=(int)Math.ceil(start.distanceTo(end)*2);
        for(int i=0;i<=steps;i++)if(!world.isBlockLoaded(new BlockPos(start.add(end.subtract(start).scale(steps==0?0:(double)i/steps)))))return false;
        return true;
    }
    public static boolean grass(World world,BlockPos pos){
        IBlockState state=world.getBlockState(pos);
        if(state.getBlock()==Blocks.TALLGRASS)return true;
        if(state.getBlock()!=Blocks.DOUBLE_PLANT)return false;
        if(state.getValue(BlockDoublePlant.HALF)==BlockDoublePlant.EnumBlockHalf.UPPER){
            if(!world.isBlockLoaded(pos.down()))return false;
            state=world.getBlockState(pos.down());
            if(state.getBlock()!=Blocks.DOUBLE_PLANT)return false;
        }
        BlockDoublePlant.EnumPlantType type=state.getValue(BlockDoublePlant.VARIANT);
        return type==BlockDoublePlant.EnumPlantType.GRASS||type==BlockDoublePlant.EnumPlantType.FERN;
    }
}
