package net.narutomod.item;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.narutomod.entity.EntityWaterDragon;

/** Native dragon geometry and the same textured fire used by Great Fireball. */
@SideOnly(Side.CLIENT)
public final class RenderBloodlineTechniques {
    private static final ResourceLocation DRAGON = new ResourceLocation("narutomod:textures/dragon_red.png");
    private static final ResourceLocation FLAME = new ResourceLocation("narutomod:textures/flames_red.png");
    private static final ResourceLocation FIREBALL = new ResourceLocation("narutomod:textures/fireball.png");
    private RenderBloodlineTechniques() { }

    private static void beginFire() {
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    }

    private static void endFire(float lightX, float lightY) {
        GlStateManager.depthMask(true);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
    }

    private static void quad(float width, float bottom, float top, float v0, float v1, int alpha) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder b = tess.getBuffer();
        b.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        b.pos(-width, bottom, 0).tex(0, v1).color(255, 255, 255, alpha).endVertex();
        b.pos(width, bottom, 0).tex(1, v1).color(255, 255, 255, alpha).endVertex();
        b.pos(width, top, 0).tex(1, v0).color(255, 255, 255, alpha).endVertex();
        b.pos(-width, top, 0).tex(0, v0).color(255, 255, 255, alpha).endVertex();
        tess.draw();
    }

    /** Feathered native flame strips: no hard rectangular borders around an orb. */
    private static void crest(float age, float width, float height, int alpha) {
        float v0 = ((int)(age * .6f) & 3) * .25f;
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (int plane = 0; plane < 2; ++plane) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(plane * 90f, 0, 1, 0);
            BufferBuilder b = Tessellator.getInstance().getBuffer();
            b.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (int row = 0; row < 4; ++row) for (int column = 0; column < 10; ++column) {
                flameVertex(b, column / 10f, row / 4f, width, height, v0, alpha, age);
                flameVertex(b, (column + 1) / 10f, row / 4f, width, height, v0, alpha, age);
                flameVertex(b, (column + 1) / 10f, (row + 1) / 4f, width, height, v0, alpha, age);
                flameVertex(b, column / 10f, (row + 1) / 4f, width, height, v0, alpha, age);
            }
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private static void flameVertex(BufferBuilder b, float u, float t, float width, float height, float v0, int alpha, float age) {
        float edge = MathHelper.sin(u * (float)Math.PI);
        float fade = Math.min(1f, t * 4f) * Math.min(1f, (1f - t) * 8f);
        float x = (u - .5f) * width * (.65f + .35f * t) + MathHelper.sin(age * .18f + t * 4f) * width * .045f * t;
        // Crop the row edges; the old editable wisp's tiny corner icon is not part of these flames.
        b.pos(x, height * t, 0).tex(.075f + u * .85f, v0 + .245f * (1f - t))
            .color(255, 255, 255, MathHelper.clamp((int)(alpha * edge * edge * fade), 0, 255)).endVertex();
    }

    public static final class Dragon extends Render<BloodlineTechniques.TwinDragon> {
        private final EntityWaterDragon.Renderer.ModelDragonHead model = new EntityWaterDragon.Renderer().new ModelDragonHead();
        public Dragon(RenderManager manager) { super(manager); shadowSize = .1f; }

        @Override public boolean shouldRender(BloodlineTechniques.TwinDragon dragon, ICamera camera,
                double camX, double camY, double camZ) {
            // Its trailing body is much larger than the collision box at the head.
            return dragon.isInRangeToRender3d(camX, camY, camZ)
                && camera.isBoundingBoxInFrustum(dragon.getEntityBoundingBox().grow(28));
        }

        @Override public void doRender(BloodlineTechniques.TwinDragon dragon, double x, double y, double z, float yaw, float partial) {
            float age = dragon.ticksExisted + partial;
            float charge = MathHelper.clamp((dragon.visualPower() - 1f) / 1.8f, 0f, 1f);
            float formation = MathHelper.clamp((age - dragon.getCastingDelay() + 9f) / 9f, 0f, 1f);
            if (formation <= 0f) return;
            float scale = (1.7f + 1.35f * charge) * (.3f + .7f * formation);
            float facing = dragon.prevRotationYaw + MathHelper.wrapDegrees(dragon.rotationYaw - dragon.prevRotationYaw) * partial;
            float pitch = dragon.prevRotationPitch + (dragon.rotationPitch - dragon.prevRotationPitch) * partial;
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y + scale * .38f, z);
            // Match the original water-dragon axes. An extra 180-degree yaw made
            // the old head look back at its caster instead of at the impact point.
            GlStateManager.rotate(-facing, 0, 1, 0);
            GlStateManager.rotate(pitch - 180f, 1, 0, 0);
            GlStateManager.scale(scale, scale, scale);
            beginFire();
            bindEntityTexture(dragon);
            GlStateManager.color(1f, .9f, .75f, .92f);
            model.setRotationAngles(0, 0, age, 0, 0, .0625f, dragon);
            model.renderFlameDragon(age, .0625f);

            // A continuous, tapered flame mane follows the same articulation as
            // the textured body; the dragon remains readable through the fire.
            bindTexture(FLAME);
            GlStateManager.depthMask(false);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -.1, -.35);
            crest(age, 1.1f, -1.05f, 205);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 6.5f / 16f, 7f / 16f);
            for (int i = 0; i < 17; ++i) {
                GlStateManager.rotate(MathHelper.sin(age * .16f - i * .52f) * 6.5f, 0, 1, 0);
                GlStateManager.rotate(MathHelper.cos(age * .13f - i * .42f) * 3f, 1, 0, 0);
                if ((i & 1) == 0) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(0, -.25, .35);
                    crest(age - i * .7f, 1.35f, -1.2f, 190);
                    GlStateManager.popMatrix();
                }
                GlStateManager.translate(0, 0, 11f / 16f);
                GlStateManager.scale(.935f, .935f, .935f);
            }
            GlStateManager.popMatrix();
            endFire(lightX, lightY);
            GlStateManager.popMatrix();
        }
        @Override protected ResourceLocation getEntityTexture(BloodlineTechniques.TwinDragon entity) { return DRAGON; }
    }

    private abstract static class FlameRender<T extends Entity> extends Render<T> {
        FlameRender(RenderManager manager) { super(manager); shadowSize = 0; }
        @Override protected ResourceLocation getEntityTexture(T entity) { return FIREBALL; }

        private void faceCamera(float spin) {
            GlStateManager.rotate(180f - renderManager.playerViewY, 0, 1, 0);
            GlStateManager.rotate((renderManager.options.thirdPersonView == 2 ? -1 : 1) * -renderManager.playerViewX, 1, 0, 0);
            GlStateManager.rotate(spin, 0, 0, 1);
        }

        void core(double x, double y, double z, float age, float size, int alpha) {
            bindTexture(FIREBALL);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            faceCamera(age * 22f);
            float radius = size * (.62f + MathHelper.sin(age * .8f) * .025f);
            quad(radius, -radius, radius, 0, 1, alpha);
            GlStateManager.popMatrix();
        }

        void orb(double x, double y, double z, float age, float size) {
            GlStateManager.depthMask(false);
            bindTexture(FLAME);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y - size * .38f, z);
            GlStateManager.rotate(age * 5f, 0, 1, 0);
            crest(age, size * 1.55f, size * 1.3f, 225);
            GlStateManager.popMatrix();
            core(x, y, z, age, size, 255);
        }
    }

    public static final class Company extends FlameRender<BloodlineTechniques.FlameCompany> {
        public Company(RenderManager manager) { super(manager); }
        @Override public boolean shouldRender(BloodlineTechniques.FlameCompany company, ICamera camera,
                double camX, double camY, double camZ) {
            return company.isInRangeToRender3d(camX, camY, camZ)
                && camera.isBoundingBoxInFrustum(company.getEntityBoundingBox().grow(4));
        }
        @Override public void doRender(BloodlineTechniques.FlameCompany company, double x, double y, double z, float yaw, float partial) {
            float age = company.ticksExisted + partial;
            float charge = MathHelper.clamp(company.visualPower() - 1f, 0f, 1f);
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            beginFire();
            for (int i = 0; i < company.charges(); i++) {
                double angle = age * .12 + i * Math.PI * 2 / 3;
                double radius = 1.25 + .25 * charge;
                orb(x + Math.cos(angle) * radius, y + Math.sin(age * .2 + i) * .13,
                    z + Math.sin(angle) * radius, age + i * 7f, .85f + .45f * charge);
            }
            endFire(lightX, lightY);
        }
    }

    public static final class Bolt extends FlameRender<BloodlineTechniques.FlameBolt> {
        public Bolt(RenderManager manager) { super(manager); }
        @Override public void doRender(BloodlineTechniques.FlameBolt bolt, double x, double y, double z, float yaw, float partial) {
            float age = bolt.ticksExisted + partial;
            float size = .9f + .5f * MathHelper.clamp(bolt.visualPower() - 1f, 0f, 1f);
            Vec3d movement = new Vec3d(bolt.posX - bolt.prevPosX, bolt.posY - bolt.prevPosY, bolt.posZ - bolt.prevPosZ);
            if (movement.lengthSquared() < .001) movement = new Vec3d(bolt.motionX, bolt.motionY, bolt.motionZ);
            Vec3d trail = movement.normalize().scale(size * .55);
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            beginFire();
            GlStateManager.depthMask(false);
            for (int i = 3; i >= 1; --i) {
                core(x - trail.x * i, y - trail.y * i, z - trail.z * i,
                    age - i * 2f, size * (1f - i * .19f), 180 - i * 32);
            }
            orb(x, y, z, age, size);
            endFire(lightX, lightY);
        }
    }
}
