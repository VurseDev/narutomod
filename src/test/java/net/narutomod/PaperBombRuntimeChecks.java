package net.narutomod;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.narutomod.entity.*;
import net.narutomod.item.ItemExplosiveArt;
import sun.misc.Unsafe;

/** Fixtures skip AI/GL setup, exercising the production target predicate and swept raycasts. */
public final class PaperBombRuntimeChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static <T>T fixture(Class<T> type)throws Exception{
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);return type.cast(((Unsafe)f.get(null)).allocateInstance(type));
    }
    private static class Caster extends EntityPlayer {
        Entity ally,mount;
        Caster(){super(null,null);}
        @Override public boolean isOnSameTeam(Entity e){return e==ally;}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
        @Override public Entity getRidingEntity(){return mount;}
    }
    private static class Target extends EntityZombie {
        NBTTagCompound data;boolean ally;
        Target(){super(null);}
        @Override public boolean isEntityAlive(){return true;}
        @Override public boolean canBeCollidedWith(){return true;}
        @Override public NBTTagCompound getEntityData(){return data;}
        @Override public boolean isOnSameTeam(Entity e){return ally;}
    }
    private static class Summon extends Target implements EntitySummonAnimal.ISummon {
        EntityLivingBase owner;
        @Override public EntityLivingBase getSummoner(){return owner;}
    }
    private static class CollisionWorld extends World {
        List<EntityLivingBase> candidates;RayTraceResult block;
        CollisionWorld(){super(null,null,null,null,false);}
        @Override protected IChunkProvider createChunkProvider(){return null;}
        @Override protected boolean isChunkLoaded(int x,int z,boolean empty){return true;}
        @Override public RayTraceResult rayTraceBlocks(Vec3d a,Vec3d b,boolean liquid,boolean ignore,boolean miss){return block;}
        @Override public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> type,AxisAlignedBB bounds){return (List<T>)(List<?>)candidates;}
    }
    public static int run()throws Exception{
        Caster caster=fixture(Caster.class);
        Target enemy=fixture(Target.class);enemy.data=new NBTTagCompound();enemy.setEntityBoundingBox(new AxisAlignedBB(3,-1,-1,4,1,1));
        Target ally=fixture(Target.class);ally.data=new NBTTagCompound();ally.setEntityBoundingBox(new AxisAlignedBB(1,-1,-1,2,1,1));
        caster.ally=ally;
        check(EntityPaperBombCast.eligible(caster,enemy),"ordinary target allowed");
        check(!EntityPaperBombCast.eligible(caster,caster),"owner immune");
        check(!EntityPaperBombCast.eligible(caster,ally),"scoreboard ally immune");
        caster.ally=null;ally.ally=true;
        check(!EntityPaperBombCast.eligible(caster,ally),"target-declared ally immune");
        enemy.data.setInteger("UntargetableTicks",20);
        check(!EntityPaperBombCast.eligible(caster,enemy),"intangible target excluded");
        enemy.data.setInteger("UntargetableTicks",0);
        Summon summon=fixture(Summon.class);summon.data=new NBTTagCompound();summon.owner=caster;
        check(!EntityPaperBombCast.eligible(caster,summon),"owned clone/summon immune");
        CollisionWorld world=fixture(CollisionWorld.class);world.candidates=Arrays.asList(ally,enemy);
        EntityPaperBombCast cast=fixture(EntityPaperBombCast.class);cast.world=world;
        check(cast.isInRangeToRenderDist(32*32)&&!cast.isInRangeToRenderDist(65*65),"tiny controller keeps tags visible across their flight range");
        Method trace=EntityPaperBombCast.class.getDeclaredMethod("trace",Vec3d.class,Vec3d.class,EntityPlayer.class);trace.setAccessible(true);
        Vec3d from=new Vec3d(0,0,0),to=new Vec3d(8,0,0);
        RayTraceResult result=(RayTraceResult)trace.invoke(cast,from,to,caster);
        check(result.entityHit==enemy,"swept flight passes allies and finds enemy");
        world.block=new RayTraceResult(new Vec3d(2.5,0,0),EnumFacing.WEST,new BlockPos(2,0,0));
        check(trace.invoke(cast,from,to,caster)==world.block,"wall wins over entity behind cover");
        world.block=new RayTraceResult(new Vec3d(6,0,0),EnumFacing.WEST,new BlockPos(6,0,0));
        check(((RayTraceResult)trace.invoke(cast,from,to,caster)).entityHit==enemy,"entity before wall is hit");
        world.candidates=Collections.emptyList();world.block=null;
        check(trace.invoke(cast,from,to,caster)==null,"empty trajectory misses cleanly");
        ItemExplosiveArt.ArtItem item=new ItemExplosiveArt.ArtItem();
        check(item.getPower(null,caster,72000)==0&&item.getPower(null,caster,71989)==0,"early release has zero power");
        check(item.getPower(null,caster,71988)==1&&item.getPower(null,caster,0)==1,"windup completes at twelve ticks, fixed power");
        Method read=EntityPaperBombCast.class.getDeclaredMethod("readEntityFromNBT",NBTTagCompound.class);read.setAccessible(true);
        read.invoke(cast,new NBTTagCompound());check(cast.isDead,"reloaded paid effects cannot resume or duplicate");
        return checks;
    }
}
