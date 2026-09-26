package net.narutomod;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.narutomod.entity.EntityScalableProjectile;
import net.narutomod.entity.EntitySusanooBase;
import sun.misc.Unsafe;

/** Actual collision predicates and Minecraft intersection math; no GL, server or registry bootstrap. */
public final class SusanooCombatChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    // Fixtures bypass AI/registry construction only. Methods being tested are real production methods.
    private static <T> T fixture(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
    }
    private static final class Actor extends EntityZombie {
        Entity mount;
        Actor() { super((World)null); }
        @Override public Entity getRidingEntity() { return mount; }
        @Override public boolean canBeCollidedWith() { return true; }
    }
    private static final class Armor extends EntitySusanooBase {
        EntityLivingBase owner;
        Armor() { super((World)null); }
        @Override public EntityLivingBase getOwnerPlayer() { return owner; }
        @Override public boolean isBeingRidden() { return true; }
        @Override public boolean shouldShowSword() { return false; }
        @Override public void setShowSword(boolean value) { }
        void attach(Entity rider) { super.addPassenger(rider); }
    }
    private static final class CollisionWorld extends World {
        List<Entity> candidates;
        RayTraceResult block;
        CollisionWorld() { super(null, null, null, null, false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean empty) { return true; }
        @Override public RayTraceResult rayTraceBlocks(Vec3d from, Vec3d to, boolean liquid, boolean ignore, boolean miss) { return block; }
        @Override public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB box) { return candidates; }
        @Override public List<AxisAlignedBB> getCollisionBoxes(Entity entity, AxisAlignedBB box) {
            return block == null ? Collections.emptyList() : Arrays.asList(new AxisAlignedBB(block.getBlockPos()));
        }
    }
    private static ProjectileImpactEvent impact(Entity entity, RayTraceResult hit) {
        // Forge normally injects this override for @Cancelable through its launch transformer.
        return new ProjectileImpactEvent(entity,hit) {
            @Override public boolean isCancelable() { return true; }
        };
    }
    public static void main(String[] args) throws Exception {
        Actor owner = fixture(Actor.class), enemy = fixture(Actor.class);
        Armor armor = fixture(Armor.class), enemyArmor = fixture(Armor.class);
        owner.setEntityId(1);enemy.setEntityId(2);armor.setEntityId(3);enemyArmor.setEntityId(4);
        armor.owner = owner; enemyArmor.owner = enemy; owner.mount = armor;
        owner.setEntityBoundingBox(new AxisAlignedBB(-.2, 0, -.2, .2, 2, .2));
        enemy.setEntityBoundingBox(new AxisAlignedBB(5, 0, -.5, 6, 2, .5));
        enemyArmor.setEntityBoundingBox(new AxisAlignedBB(4, 0, -1, 7, 4, 1));
        Vec3d start = new Vec3d(0, 1, 0), end = new Vec3d(10, 1, 0);
        for (int stage = 0; stage < 6; stage++) {
            double width = MadaraSusanooPolicy.width(stage);
            armor.setEntityBoundingBox(new AxisAlignedBB(-width / 2, 0, -width / 2, width / 2, MadaraSusanooPolicy.height(stage), width / 2));
            check(armor.canBeCollidedWith(), "occupied armor remains targetable stage " + stage);
            check(SusanooCombat.isOwnSusanoo(armor, owner), "owner armor excluded stage " + stage);
            check(!SusanooCombat.isOwnSusanoo(armor, enemy), "enemy cannot bypass stage " + stage);
            RayTraceResult hit = SusanooCombat.nearestHit(start, end, null, Arrays.asList(armor, owner, enemy), owner, false);
            check(hit != null && hit.entityHit == enemy, "shot exits armor and hits enemy in same tick " + stage);
            hit = SusanooCombat.nearestHit(end, start, null, Arrays.asList(armor), enemy, false);
            check(hit != null && hit.entityHit == armor, "incoming shot intercepted " + stage);
        }
        RayTraceResult wall = new RayTraceResult(new Vec3d(3, 1, 0), EnumFacing.WEST, new BlockPos(3, 1, 0));
        check(SusanooCombat.nearestHit(start, end, wall, Arrays.asList(armor, enemy), owner, false) == wall, "wall blocks enemy behind it");
        check(SusanooCombat.nearestHit(start, end, null, Arrays.asList(armor), owner, false) == null, "clear shot is not consumed by armor");
        check(SusanooCombat.nearestHit(start, end, null, Arrays.asList(enemy, enemyArmor, armor), owner, false).entityHit == enemyArmor, "enemy armor shields enemy regardless of iteration order");
        check(!SusanooCombat.isOwnSusanoo(armor, null), "unknown shooter gets no exemption");
        check(!SusanooCombat.isOwnSusanoo(null, owner), "null candidate safe");
        owner.mount = null;
        check(SusanooCombat.isOwnSusanoo(armor, owner), "projectile in flight still knows owner after dismount");
        armor.owner = null; owner.mount = armor;
        check(SusanooCombat.isOwnSusanoo(armor, owner), "passenger relation covers owner sync delay");
        armor.owner = owner;
        net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(Entity.class,armor,new java.util.ArrayList<Entity>(),new String[]{"riddenByEntities","field_184244_h"});
        armor.attach(owner);
        check(armor.getPassengers().contains(owner),"mount uses correct passenger mapping");
        check(!armor.attackEntityFrom(new net.minecraft.util.EntityDamageSource("ninjutsu", owner), 100), "owner splash cannot damage armor");
        CollisionWorld world=fixture(CollisionWorld.class);
        EntityTippedArrow arrow=fixture(EntityTippedArrow.class);
        arrow.setEntityId(5);
        arrow.world=world;arrow.shootingEntity=owner;arrow.posY=1;arrow.motionX=10;
        arrow.setEntityBoundingBox(new AxisAlignedBB(-.1,.9,-.1,.1,1.1,.1));
        world.candidates=Arrays.asList(armor,enemy);
        SusanooCombat.Events handler=new SusanooCombat.Events();
        ProjectileImpactEvent event=impact(arrow,new RayTraceResult(armor,start));
        handler.onImpact(event);
        check(!event.isCanceled()&&event.getRayTraceResult().entityHit==enemy,"vanilla impact retargets same-tick enemy");
        world.block=wall;
        event=impact(arrow,new RayTraceResult(armor,start));handler.onImpact(event);
        check(!event.isCanceled()&&event.getRayTraceResult().typeOfHit==RayTraceResult.Type.BLOCK
            &&wall.getBlockPos().equals(event.getRayTraceResult().getBlockPos()),"vanilla impact preserves block position through mapped field");
        for(boolean scaled:new boolean[]{false,true}) {
            RayTraceResult hit=EntityScalableProjectile.forwardsRaycast(arrow,new Vec3d(10,0,0),scaled,true,false,owner);
            check(hit!=null&&hit.typeOfHit==RayTraceResult.Type.BLOCK,"scaled/unscaled projectile respects wall");
            world.block=null;
            hit=EntityScalableProjectile.forwardsRaycast(arrow,new Vec3d(10,0,0),scaled,true,false,owner);
            check(hit!=null&&hit.entityHit==enemy,"scaled/unscaled projectile filters owner armor");
            world.block=wall;
        }
        world.block=null;world.candidates=Arrays.asList(armor);
        event=impact(arrow,new RayTraceResult(armor,start));handler.onImpact(event);
        check(event.isCanceled(),"vanilla clear shot continues flying");
        arrow.shootingEntity=enemy;
        event=impact(arrow,new RayTraceResult(armor,start));handler.onImpact(event);
        check(!event.isCanceled()&&event.getRayTraceResult().entityHit==armor,"enemy vanilla impact untouched");
        System.out.println("Susanoo collision regression: " + checks + " checks passed.");
    }
}
