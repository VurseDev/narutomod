package net.narutomod;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.narutomod.entity.EntitySusanooBase;

/** Shooter-specific armor filtering. Enemy shots still collide with an occupied Susanoo. */
public final class SusanooCombat {
    private SusanooCombat() { }

    public static boolean isOwnSusanoo(@Nullable Entity target, @Nullable Entity caster) {
        if (!(target instanceof EntitySusanooBase) || caster == null) return false;
        return caster == ((EntitySusanooBase)target).getOwnerPlayer() || caster.getRidingEntity() == target;
    }

    /** Preserve vanilla raycasts unless their selected hit is the shooter's own armor. */
    public static RayTraceResult forwardsRaycast(Entity projectile, boolean entities, boolean includeShooter, @Nullable Entity shooter) {
        RayTraceResult hit = ProjectileHelper.forwardsRaycast(projectile, entities, includeShooter, shooter);
        return hit != null && isOwnSusanoo(hit.entityHit, shooter) ? tracePastArmor(projectile, shooter, includeShooter) : hit;
    }

    private static RayTraceResult tracePastArmor(Entity projectile, Entity shooter, boolean includeShooter) {
        Vec3d start = projectile.getPositionVector();
        Vec3d end = start.addVector(projectile.motionX, projectile.motionY, projectile.motionZ);
        RayTraceResult block = projectile.world.rayTraceBlocks(start, end, false, true, false);
        AxisAlignedBB bounds = projectile.getEntityBoundingBox().expand(projectile.motionX, projectile.motionY, projectile.motionZ).grow(1);
        return nearestHit(start, end, block, projectile.world.getEntitiesWithinAABBExcludingEntity(projectile, bounds), shooter, includeShooter);
    }

    // Separated from the world query so collision ordering can be exercised without a running client.
    static RayTraceResult nearestHit(Vec3d start, Vec3d end, @Nullable RayTraceResult block,
                                    Iterable<Entity> candidates, Entity shooter, boolean includeShooter) {
        RayTraceResult nearest = block;
        double distance = block == null ? Double.POSITIVE_INFINITY : start.squareDistanceTo(block.hitVec);
        if (block != null) end = block.hitVec;
        for (Entity candidate : candidates) {
            if (!candidate.canBeCollidedWith() || candidate.noClip || isOwnSusanoo(candidate, shooter)
                || (!includeShooter && candidate == shooter)) continue;
            AxisAlignedBB box = candidate.getEntityBoundingBox().grow(.3);
            RayTraceResult intercept = box.calculateIntercept(start, end);
            Vec3d point = box.contains(start) ? start : intercept == null ? null : intercept.hitVec;
            if (point != null && start.squareDistanceTo(point) < distance) {
                distance = start.squareDistanceTo(point);
                nearest = new RayTraceResult(candidate, point);
            }
        }
        return nearest;
    }

    public static final class Events {
        /** Vanilla arrows, throwables and fireballs also back several existing mod jutsus. */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onImpact(ProjectileImpactEvent event) {
            Entity projectile = event.getEntity();
            Entity shooter = projectile instanceof EntityArrow ? ((EntityArrow)projectile).shootingEntity
                : projectile instanceof EntityFireball ? ((EntityFireball)projectile).shootingEntity
                : projectile instanceof EntityThrowable ? ((EntityThrowable)projectile).getThrower() : null;
            RayTraceResult hit = event.getRayTraceResult();
            if (!isOwnSusanoo(hit.entityHit, shooter)) return;
            RayTraceResult next = tracePastArmor(projectile, shooter, false);
            if (next == null) {
                event.setCanceled(true);
            } else {
                // Mutate the result passed to onImpact, including the block position through the constructor's field.
                hit.typeOfHit = next.typeOfHit;
                hit.entityHit = next.entityHit;
                hit.hitVec = next.hitVec;
                hit.sideHit = next.sideHit;
                if (next.typeOfHit == RayTraceResult.Type.BLOCK) {
                    // RayTraceResult.blockPos is private in 1.12.2.
                    net.minecraftforge.fml.relauncher.ReflectionHelper.setPrivateValue(RayTraceResult.class, hit,
                        next.getBlockPos(), new String[]{"blockPos", "field_178783_e"});
                }
            }
        }
    }
}
