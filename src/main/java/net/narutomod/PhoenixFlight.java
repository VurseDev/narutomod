package net.narutomod;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/** Phoenix-only tuning. Other scalable projectiles retain their original flight. */
public final class PhoenixFlight {
    public static final int LIFETIME = 120;
    public static final double LOCK_RANGE = 8.0;
    public static final double LEASH = 48.0;
    private PhoenixFlight() { }

    public static float power(float power) {
        return Float.isFinite(power) ? Math.max(.8f, Math.min(4f, power)) : .8f;
    }

    public static float scale(float power) {
        return .95f + (power(power) - .8f) / 3.2f * 1.75f;
    }

    public static double speed(int age) {
        return Math.min(.82, .22 + Math.max(0, age) * .028);
    }

    public static Vec3d velocity(Vec3d current, Vec3d towards, int age) {
        Vec3d direction = current.lengthSquared() > 1.0e-8 ? current.normalize() : new Vec3d(0, 0, 1);
        if (towards != null && towards.lengthSquared() > 1.0e-8) {
            double turn = towards.lengthSquared() <= 36 ? .65 : .28;
            direction = direction.scale(1 - turn).add(towards.normalize().scale(turn)).normalize();
        }
        return direction.scale(speed(age));
    }

    public static boolean inAcquisitionCone(Vec3d forward, Vec3d offset) {
        return offset.lengthSquared() <= LOCK_RANGE * LOCK_RANGE
            && (offset.lengthSquared() < .01 || forward.normalize().dotProduct(offset.normalize()) >= .5);
    }

    /** Swept volume, including starting overlaps (important for a newly raised water wall). */
    public static Vec3d contact(AxisAlignedBB box, Vec3d start, Vec3d end, float width, float height) {
        AxisAlignedBB expanded = box.grow(width * .5, height * .5, width * .5);
        if (expanded.contains(start)) return start;
        RayTraceResult hit = expanded.calculateIntercept(start, end);
        return hit == null ? null : hit.hitVec;
    }
}
