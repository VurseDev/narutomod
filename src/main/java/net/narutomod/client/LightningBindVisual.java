package net.narutomod.client;

import net.minecraft.util.math.Vec3d;

/** Four-Pillar Bind: local stone construction and restrained electrical confinement. */
public final class LightningBindVisual {
    private static final int BLUE = 0x58BBFF;
    private static final double[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    private LightningBindVisual() {}

    public interface TexturedSink {
        void vertex(double x, double y, double z, double u, double v, int rgb, float alpha);
    }

    /** Scale the existing mesh without adding geometry or changing its established proportions. */
    public static void solids(TexturedSink out, Vec3d center, float height, float radius, float age, int life) {
        if(!Float.isFinite(height)||!Float.isFinite(radius))return;
        double horizontal=Math.max(2,Math.min(64,radius))/2, vertical=Math.max(4.5,Math.min(96,height))/4.5;
        solids((x,y,z,u,v,rgb,a)->out.vertex(center.x+x*horizontal,center.y+y*vertical,center.z+z*horizontal,
            u*horizontal,v*vertical,rgb,a),Vec3d.ZERO,4.5f,age,life);
    }

    public static void energy(JutsuVfxGeometry.Sink out, Vec3d center, Vec3d target, float height, float radius,
                              float age, int life, long seed, int detail) {
        if(!Float.isFinite(height)||!Float.isFinite(radius))return;
        double horizontal=Math.max(2,Math.min(64,radius))/2, vertical=Math.max(4.5,Math.min(96,height))/4.5;
        Vec3d local=target==null?new Vec3d(0,1,0):new Vec3d((target.x-center.x)/horizontal,(target.y-center.y)/vertical,(target.z-center.z)/horizontal);
        energy((x,y,z,rgb,a)->out.vertex(center.x+x*horizontal,center.y+y*vertical,center.z+z*horizontal,rgb,a),
            Vec3d.ZERO,local,4.5f,age,life,seed,detail);
    }

    /** Bind minecraft:textures/blocks/stone.png with repeat wrapping before drawing these quads. */
    public static void solids(TexturedSink out, Vec3d center, float height, float age, int life) {
        if (!valid(center, height, age, life)) return;
        float alpha = fade(age, life);
        if (alpha <= .001f) return;
        double h = boundedHeight(height), offset = offset(h, age, life);
        for (double[] direction : DIRECTIONS) {
            double x = center.x + direction[0] * 2, z = center.z + direction[1] * 2;
            // Each tier uses whole Minecraft-style faces; tall shaft UVs repeat per block.
            box(out, center.y, x, z, .44, 0, .17, offset, 0xB3B9C0, alpha);
            box(out, center.y, x, z, .36, .17, .34, offset, 0xCCD0D5, alpha);
            box(out, center.y, x, z, .28, .34, h - .68, offset, 0xC7CFD9, alpha);
            box(out, center.y, x, z, .32, .84, .94, offset, 0x89939F, alpha);
            box(out, center.y, x, z, .35, h - .87, h - .68, offset, 0x8C98A5, alpha);
            box(out, center.y, x, z, .43, h - .68, h - .46, offset, 0xC5CED8, alpha);
            box(out, center.y, x, z, .35, h - .46, h - .19, offset, 0xB2BCC8, alpha);
            box(out, center.y, x, z, .25, h - .19, h, offset, 0xDDE3E9, alpha);
            // A narrow dark inlay on the inner face gives the blue discharge a readable channel.
            double low = Math.max(center.y + .012, center.y + .42 + offset);
            double high = center.y + h - .90 + offset;
            if (high > low) {
                Vec3d inner = new Vec3d(x - direction[0] * .282, 0, z - direction[1] * .282);
                double sx = -direction[1] * .065, sz = direction[0] * .065;
                face(out, inner.x - sx, low, inner.z - sz, inner.x + sx, low, inner.z + sz,
                    inner.x + sx, high, inner.z + sz, inner.x - sx, high, inner.z - sz,
                    .13, high - low, 0x46536A, alpha);
            }
        }
    }

    public static void energy(JutsuVfxGeometry.Sink out, Vec3d center, Vec3d target, float height,
                              float age, int life, long seed, int detail) {
        if (!valid(center, height, age, life)) return;
        float alpha = fade(age, life) * JutsuVfxGeometry.smooth((age - 2) / 5f);
        if (alpha <= .001f) return;
        double h = boundedHeight(height), offset = offset(h, age, life);
        long frame = (long) (age / 2);
        Vec3d victim = boundedTarget(center, target, h);
        Vec3d[] tips = new Vec3d[4];
        JutsuVfxGeometry.Sink groundSparks = (x, y, z, rgb, opacity) ->
            out.vertex(x, Math.max(center.y + .025, y), z, rgb, opacity);
        for (int pillar = 0; pillar < DIRECTIONS.length; pillar++) {
            double[] direction = DIRECTIONS[pillar];
            Vec3d base = center.addVector(direction[0] * 2, .025, direction[1] * 2);
            Vec3d inner = base.addVector(-direction[0] * .365, 0, -direction[1] * .365);
            double bottom = Math.max(.12, .44 + offset), top = h - .92 + offset;
            long localSeed = seed + pillar * 2117L + frame * 97L;
            // Short linked discharges keep the jitter outside the flat stone face.
            if (top > bottom) {
                int pieces = detail > 0 ? 6 : 4;
                for (int part = 0; part < pieces; part++) {
                    double y0 = bottom + (top - bottom) * part / pieces;
                    double y1 = bottom + (top - bottom) * (part + 1) / pieces;
                    JutsuVfxGeometry.bolt(out, inner.addVector(0, y0, 0), inner.addVector(0, y1, 0),
                        .010f, BLUE, alpha * .85f, localSeed + part * 43L, 0);
                }
            }
            float tetherAlpha = alpha * JutsuVfxGeometry.smooth((age - 7) / 5f);
            double tieY = Math.max(.18, h * .48 + offset);
            if (tetherAlpha > .001f && top > .18) {
                JutsuVfxGeometry.bolt(out, inner.addVector(0, tieY, 0), victim, .016f,
                    BLUE, tetherAlpha * .83f, localSeed ^ 0x6DE18L, detail > 0 ? 1 : 0);
            }
            tips[pillar] = inner.addVector(0, Math.max(.10, h - .88 + offset), 0);
            if (age >= 5 && age < 13) {
                JutsuVfxGeometry.burst(groundSparks, base, .24f, age - 5, 8, 0xB6DFFF,
                    seed + pillar * 379L, detail > 0 ? 1 : 0);
            }
        }
        // High-detail upper circuit flickers in alternating pairs, leaving the victim visible.
        if (detail > 0 && age >= 9) {
            float circuitAlpha = alpha * .42f;
            for (int side = (int) (frame & 1); side < 4; side += 2) {
                JutsuVfxGeometry.bolt(out, tips[side], tips[(side + 1) % 4], .009f,
                    0x75CDFF, circuitAlpha, seed + frame * 131 + side * 433L, 0);
            }
        }
    }

    private static boolean valid(Vec3d center, float height, float age, int life) {
        return center != null && Double.isFinite(center.x) && Double.isFinite(center.y)
            && Double.isFinite(center.z) && Float.isFinite(height) && Float.isFinite(age)
            && age > 0 && age < life && life > 0;
    }

    private static double boundedHeight(float height) { return Math.max(2, Math.min(6, height)); }

    private static float fade(float age, int life) {
        return 1 - JutsuVfxGeometry.smooth((age - Math.max(0, life - 12)) / Math.min(12f, life));
    }

    private static double offset(double height, float age, int life) {
        double rise = JutsuVfxGeometry.smooth(age / 8f);
        double sink = JutsuVfxGeometry.smooth((age - Math.max(0, life - 12)) / Math.min(12f, life));
        return -height * (1 - rise) - sink * .65;
    }

    private static Vec3d boundedTarget(Vec3d center, Vec3d target, double height) {
        if (target == null || !Double.isFinite(target.x) || !Double.isFinite(target.y)
            || !Double.isFinite(target.z)) return center.addVector(0, 1, 0);
        Vec3d delta = target.subtract(center);
        double length = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double scale = length > 1.4 ? 1.4 / length : 1;
        return center.addVector(delta.x * scale, Math.max(.45, Math.min(height - .7, delta.y)), delta.z * scale);
    }

    private static void box(TexturedSink out, double ground, double x, double z, double r,
                            double low, double high, double offset, int rgb, float alpha) {
        double y0 = Math.max(ground + .01, ground + low + offset), y1 = ground + high + offset;
        if (y1 <= y0) return;
        double x0 = x - r, x1 = x + r, z0 = z - r, z1 = z + r, w = 2 * r, h = y1 - y0;
        face(out, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, w,h, shade(rgb,.70f),alpha);
        face(out, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, w,h, shade(rgb,.87f),alpha);
        face(out, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, w,h, shade(rgb,.79f),alpha);
        face(out, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, w,h, rgb,alpha);
        face(out, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, w,w, shade(rgb,1.10f),alpha);
        face(out, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, w,w, shade(rgb,.55f),alpha);
    }

    private static int shade(int rgb, float multiplier) {
        return (Math.min(255, Math.round(((rgb >> 16) & 255) * multiplier)) << 16)
            | (Math.min(255, Math.round(((rgb >> 8) & 255) * multiplier)) << 8)
            | Math.min(255, Math.round((rgb & 255) * multiplier));
    }

    private static void face(TexturedSink out,
        double ax,double ay,double az, double bx,double by,double bz,
        double cx,double cy,double cz, double dx,double dy,double dz,
        double u, double v, int rgb, float alpha) {
        out.vertex(ax,ay,az,0,0,rgb,alpha); out.vertex(bx,by,bz,u,0,rgb,alpha);
        out.vertex(cx,cy,cz,u,v,rgb,alpha); out.vertex(dx,dy,dz,0,v,rgb,alpha);
    }
}
