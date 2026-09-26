package net.narutomod;

import com.mojang.authlib.GameProfile;
import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import net.narutomod.item.ItemKaton.EntityFirePhoenix;

/** Production steering, collision, charge, targeting, damage/counter and NBT fixtures; no GL/network. */
public final class PhoenixChecks {
    private static int checks;
    private static void check(boolean condition, String name) {checks++; if(!condition) throw new AssertionError(name);}
    private static void close(double a,double b,String name) {check(Math.abs(a-b)<.0001,name+": "+a+" vs "+b);}
    private static final class Arena extends World {
        final List<Entity> actors=new ArrayList<>();
        final java.util.Map<BlockPos,IBlockState> blocks=new java.util.HashMap<>();
        Arena() {
            super(null,new WorldInfo(new WorldSettings(0,GameType.SURVIVAL,false,false,WorldType.DEFAULT),"Phoenix fixture"),
                new WorldProviderSurface(),new Profiler(),false);
            provider.setWorld(this);
        }
        @Override protected IChunkProvider createChunkProvider() {return null;}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty) {return true;}
        @Override public IBlockState getBlockState(BlockPos pos) {return blocks.getOrDefault(pos,Blocks.AIR.getDefaultState());}
        @Override public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> type,AxisAlignedBB box,Predicate<? super T> filter) {
            List<T> found=new ArrayList<>();
            for(Entity actor:actors) if(type.isInstance(actor)) {
                T value=type.cast(actor);
                if(box.intersects(value.getEntityBoundingBox()) && (filter==null||filter.apply(value))) found.add(value);
            }
            return found;
        }
        @Override public List<Entity> getEntitiesInAABBexcluding(Entity except,AxisAlignedBB box,Predicate<? super Entity> filter) {
            List<Entity> found=new ArrayList<>();
            for(Entity actor:actors) if(actor!=except && box.intersects(actor.getEntityBoundingBox()) && (filter==null||filter.apply(actor)))found.add(actor);
            return found;
        }
        @Override public List<AxisAlignedBB> getCollisionBoxes(Entity actor,AxisAlignedBB box) {
            List<AxisAlignedBB> found=new ArrayList<>();
            for(BlockPos pos:blocks.keySet()) if(getBlockState(pos).getMaterial()==Material.ROCK && new AxisAlignedBB(pos).intersects(box))found.add(new AxisAlignedBB(pos));
            return found;
        }
        @Override public RayTraceResult rayTraceBlocks(Vec3d from,Vec3d to,boolean liquids,boolean ignoreNoBox,boolean last) {
            RayTraceResult found=null; double distance=Double.MAX_VALUE;
            for(BlockPos pos:blocks.keySet()) {
                if(!liquids && getBlockState(pos).getMaterial()==Material.WATER)continue;
                RayTraceResult hit=new AxisAlignedBB(pos).calculateIntercept(from,to);
                if(hit!=null && from.squareDistanceTo(hit.hitVec)<distance) {
                    distance=from.squareDistanceTo(hit.hitVec);found=new RayTraceResult(hit.hitVec,hit.sideHit,pos);
                }
            }
            return found;
        }
        @Override public void playSound(EntityPlayer player,double x,double y,double z,SoundEvent sound,SoundCategory category,float volume,float pitch) { }
    }
    private static final class Player extends EntityPlayer {
        Entity ally;
        int hits,burns;
        float damage;
        boolean cancel,substitute,creative,spectator,pvp=true;
        Player(Arena world,String name) {super(world,new GameProfile(UUID.randomUUID(),name));world.actors.add(this);}
        @Override public boolean isSpectator() {return spectator;}
        @Override public boolean isCreative() {return creative;}
        @Override public boolean isOnSameTeam(Entity entity) {return entity==this||entity==ally;}
        @Override public boolean canAttackPlayer(EntityPlayer other) {return pvp;}
        @Override public boolean attackEntityFrom(DamageSource source,float amount) {
            hits++;damage=amount;
            if(substitute)getEntityData().setInteger("UntargetableTicks",5);
            return !cancel;
        }
        @Override public void setFire(int seconds) {burns++;}
    }
    private static final class Phoenix extends EntityFirePhoenix {
        int quenched,impacts;
        Phoenix(Player owner,float power) {super(owner,power);shoot(0,0,1,1.15f,0);}
        Phoenix(Arena world) {super(world);}
        void steer() {updateFlight();}
        void age(int value) {ticksAlive=value;}
        RayTraceResult collision() {return forwardsRaycast(true,true,shootingEntity);}
        void hit(RayTraceResult result) {onImpact(result);}
        boolean eligible(Entity entity) {return canHit(entity);}
        Vec3d velocity() {return new Vec3d(motionX,motionY,motionZ);}
        void centerAt(double x,double y,double z) {setPosition(x,y-height*.5,z);}
        NBTTagCompound save() {NBTTagCompound tag=new NBTTagCompound();writeEntityToNBT(tag);return tag;}
        void load(NBTTagCompound tag) {readEntityFromNBT(tag);}
        RayTraceResult flightStep() {
            updateFlight();RayTraceResult result=collision();
            if(result==null)setPosition(posX+motionX,posY+motionY,posZ+motionZ);
            ticksAlive++;return result;
        }
        @Override protected void quenchPhoenix() {quenched++;setDead();}
        @Override protected void impactEffects() {impacts++;}
    }
    public static void main(String[] args) {
        Bootstrap.register();
        close(PhoenixFlight.scale(.8f),.95,"tap scale");close(PhoenixFlight.scale(4),2.7,"full scale");
        check(PhoenixFlight.scale(Float.NaN)==PhoenixFlight.scale(.8f),"invalid charge finite");
        float previous=0;
        for(int i=0;i<=320;i++) {float scale=PhoenixFlight.scale(.8f+i*.01f);check(scale>=previous,"charge size monotonic");previous=scale;}
        for(int age=0;age<500;age++) {
            double speed=PhoenixFlight.speed(age);check(speed>=.22 && speed<=.82,"bounded acceleration");
            Vec3d vel=PhoenixFlight.velocity(new Vec3d(0,0,1),new Vec3d(0,0,-2),age);
            close(vel.lengthVector(),speed,"180-degree turn preserves finite speed");
        }
        check(PhoenixFlight.inAcquisitionCone(new Vec3d(0,0,1),new Vec3d(2,0,6)),"forgiving forward cone");
        check(!PhoenixFlight.inAcquisitionCone(new Vec3d(0,0,1),new Vec3d(0,0,-2)),"no lock behind shot");
        check(!PhoenixFlight.inAcquisitionCone(new Vec3d(0,0,1),new Vec3d(0,0,9)),"no long range auto lock");
        // Same production steering against a strafing, fleeing target: cannot simply orbit past it.
        Vec3d position=new Vec3d(0,1,0),velocity=new Vec3d(0,0,.22);boolean caught=false;
        for(int age=0;age<120;age++) {
            Vec3d victim=new Vec3d(Math.sin(age*.15)*2,1,7+age*.23);
            velocity=PhoenixFlight.velocity(velocity,victim.subtract(position),age);position=position.add(velocity);
            if(position.distanceTo(victim)<1.0){caught=true;break;}
        }
        check(caught,"tracked strafing/fleeing target intercepted within lifetime");
        Arena arena=new Arena();Player caster=new Player(arena,"Caster"),victim=new Player(arena,"Victim");
        caster.setPosition(0,0,-4);victim.setPosition(2,0,5);
        Phoenix bird=new Phoenix(caster,4);bird.centerAt(0,.9,0);
        close(bird.width,3.24,"full charge collision matches visual scale");
        bird.steer();check(bird.motionX>0,"near target acquired and steered toward");
        victim.setPosition(-3,0,6);double oldX=bird.motionX;bird.steer();check(bird.motionX<oldX,"begins turning with moving target");
        for(int i=0;i<4;i++)bird.steer();check(bird.motionX<0,"retains moving target through turn");
        victim.getEntityData().setInteger("UntargetableTicks",5);bird.steer();Vec3d escaped=bird.velocity().normalize();
        victim.getEntityData().removeTag("UntargetableTicks");victim.setPosition(3,0,3);bird.steer();
        close(bird.velocity().normalize().dotProduct(escaped),1,"substitution breaks lock permanently");
        check(!bird.eligible(caster),"caster excluded forever, not just launch grace");
        caster.ally=victim;check(!bird.eligible(victim),"team excluded");caster.ally=null;
        caster.pvp=false;check(!bird.eligible(victim),"PvP permission respected");caster.pvp=true;
        victim.creative=true;check(!bird.eligible(victim),"creative excluded");victim.creative=false;
        victim.spectator=true;check(!bird.eligible(victim),"spectator excluded");victim.spectator=false;
        Phoenix saved=new Phoenix(caster,4);saved.centerAt(0,.9,0);victim.setPosition(0,0,5);saved.steer();saved.age(53);
        Phoenix restored=new Phoenix(arena);restored.load(saved.save());NBTTagCompound state=restored.save();
        close(restored.getEntityScale(),2.7,"saved scale restored");
        check(state.getInteger("life")==53 && state.getBoolean("PhoenixAcquired"),"save preserves age and lock history");
        check(state.getUniqueId("PhoenixCaster").equals(caster.getUniqueID())
            && state.getUniqueId("PhoenixTarget").equals(victim.getUniqueID()),"unresolved caster and target survive repeated save");
        // Thin water cell in the swept wing volume, although centerline misses it.
        arena.blocks.put(new BlockPos(1,0,1),Blocks.WATER.getDefaultState());
        Phoenix water=new Phoenix(caster,4);water.centerAt(0,.9,0);water.age(30);water.steer();
        RayTraceResult wet=water.collision();check(wet!=null && wet.typeOfHit==RayTraceResult.Type.BLOCK,"swept wings hit water wall");
        water.hit(wet);check(water.isDead && water.quenched==1 && water.impacts==0 && victim.hits==0,"water extinguishes without damage/explosion");
        arena.blocks.put(new BlockPos(1,0,1),new net.narutomod.block.BlockWaterStill.BlockCustom().getDefaultState());
        Phoenix modWater=new Phoenix(caster,4);modWater.centerAt(0,.9,0);modWater.age(30);modWater.steer();
        modWater.hit(modWater.collision());check(modWater.quenched==1 && victim.hits==0,"actual Water Wall block extinguishes too");
        arena.blocks.clear();arena.blocks.put(new BlockPos(0,0,1),Blocks.STONE.getDefaultState());
        Phoenix solid=new Phoenix(caster,.8f);solid.centerAt(0,.5,.3);solid.age(30);solid.steer();
        check(solid.collision()!=null && solid.collision().typeOfHit==RayTraceResult.Type.BLOCK,"solid cover stops phoenix");
        arena.blocks.clear();victim.setPosition(0,0,1);
        Phoenix impact=new Phoenix(caster,4);impact.centerAt(0,.9,0);
        victim.substitute=true;impact.hit(new RayTraceResult(victim,new Vec3d(0,.9,.5)));
        check(victim.hits==1 && victim.burns==0,"replacement gets one event and no post-escape burn");
        close(victim.damage,44,"full charge damage unchanged");
        impact.hit(new RayTraceResult(victim,new Vec3d(0,.9,.5)));check(victim.hits==1,"dead projectile cannot impact twice");
        victim.substitute=false;victim.getEntityData().removeTag("UntargetableTicks");victim.cancel=true;
        Phoenix canceled=new Phoenix(caster,.8f);canceled.hit(new RayTraceResult(victim,new Vec3d(0,.9,.5)));
        check(victim.hits==2 && victim.burns==0,"canceled damage never ignites");
        victim.cancel=false;
        Phoenix landed=new Phoenix(caster,.8f);landed.hit(new RayTraceResult(victim,new Vec3d(0,.9,.5)));
        check(victim.hits==3 && victim.burns==1,"landed hit still burns");
        // Full-charge body approaches a standing target over a real collision floor.
        arena.blocks.clear();victim.setPosition(0,0,12);
        for(int x=-4;x<=4;x++)for(int z=-5;z<=16;z++)arena.blocks.put(new BlockPos(x,-1,z),Blocks.STONE.getDefaultState());
        Phoenix lowFlight=new Phoenix(caster,4);lowFlight.centerAt(0,1.62,0);
        RayTraceResult arrival=null;
        for(int i=0;i<120 && arrival==null;i++)arrival=lowFlight.flightStep();
        check(arrival!=null && arrival.entityHit==victim,"charged bird reaches standing player without hitting flat floor");
        System.out.println("Phoenix flight/counters: "+checks+" checks passed (headless; live PvP tuning still required).");
    }
}
