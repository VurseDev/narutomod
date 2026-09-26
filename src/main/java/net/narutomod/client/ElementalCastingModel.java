package net.narutomod.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.item.ItemJutsu;

/** Hinged, skin-mapped forearms make the hands meet at the chest, without stretching either arm. */
@SideOnly(Side.CLIENT)
public final class ElementalCastingModel extends ModelPlayer {
    private final ModelRenderer rightForearm, leftForearm, rightSleeve, leftSleeve;
    private final ModelRenderer rigidRight, rigidLeft, rigidRightWear, rigidLeftWear;
    private final ModelRenderer hingedRight, hingedLeft, hingedRightWear, hingedLeftWear;
    private final boolean slim;
    private boolean hinged;
    private float chargeAge, releaseAge = -1;
    private ItemJutsu.JutsuEnum.Type element;

    public ElementalCastingModel(boolean slim) {
        super(0, slim);
        this.slim = slim;
        rigidRight = bipedRightArm; rigidLeft = bipedLeftArm;
        rigidRightWear = bipedRightArmwear; rigidLeftWear = bipedLeftArmwear;
        int width = slim ? 3 : 4;
        float rx = slim ? -2 : -3;
        hingedRight = upper(40, 16, rx, width, 0);
        hingedLeft = upper(32, 48, -1, width, 0);
        rightForearm = lower(40, 22, rx, width, 0);
        leftForearm = lower(32, 54, -1, width, 0);
        hingedRight.addChild(rightForearm); hingedLeft.addChild(leftForearm);
        hingedRightWear = upper(40, 32, rx, width, .25f);
        hingedLeftWear = upper(48, 48, -1, width, .25f);
        rightSleeve = lower(40, 38, rx, width, .25f);
        leftSleeve = lower(48, 54, -1, width, .25f);
        hingedRightWear.addChild(rightSleeve); hingedLeftWear.addChild(leftSleeve);
        selectArms(true);
    }

    private ModelRenderer upper(int u, int v, float x, int width, float inflate) {
        ModelRenderer model = new ModelRenderer(this, u, v);
        model.addBox(x, -2, -2, width, 6, 4, inflate);
        return model;
    }

    private ModelRenderer lower(int u, int v, float x, int width, float inflate) {
        ModelRenderer model = new ModelRenderer(this, u, v);
        model.addBox(x, 0, -2, width, 6, 4, inflate);
        model.setRotationPoint(0, 4, 0);
        return model;
    }

    private void selectArms(boolean articulated) {
        hinged = articulated;
        bipedRightArm = articulated ? hingedRight : rigidRight;
        bipedLeftArm = articulated ? hingedLeft : rigidLeft;
        bipedRightArmwear = articulated ? hingedRightWear : rigidRightWear;
        bipedLeftArmwear = articulated ? hingedLeftWear : rigidLeftWear;
    }

    public void configure(float charge, float release, ItemJutsu.JutsuEnum.Type type,
                          EntityLivingBase player, boolean firstPerson) {
        chargeAge = Math.max(0, charge); releaseAge = release; element = type;
        // Rigid armor sleeves cannot bend at the elbow. Preserve their silhouette when armored.
        selectArms(firstPerson || player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty());
    }

    @Override
    public void setRotationAngles(float limb, float amount, float time, float yaw, float pitch,
                                  float scale, Entity entity) {
        // ModelBiped does not reset all rotation points each frame. Start from the standard skin anchors.
        bipedRightArm.setRotationPoint(-5, slim ? 2.5f : 2, 0);
        bipedLeftArm.setRotationPoint(5, slim ? 2.5f : 2, 0);
        bipedBody.rotateAngleY = bipedBody.rotateAngleZ = 0;
        bipedBody.rotationPointY = 0;
        bipedRightLeg.rotationPointX = -1.9f; bipedLeftLeg.rotationPointX = 1.9f;
        super.setRotationAngles(limb, amount, time, yaw, pitch, scale, entity);
        float release = releaseAge < 0 ? 0 : smooth(releaseAge / 3f);
        float recover = releaseAge < 0 ? 1 : 1 - smooth((releaseAge - 4) / 7f);
        float blend = (releaseAge < 0 ? smooth(chargeAge / 4f) : 1) * recover;
        float beat = element == ItemJutsu.JutsuEnum.Type.RAITON ? 4.5f : 5.5f;
        float step = Math.max(0, Math.min(3, (chargeAge - 4) / beat));
        int first = Math.min(2, (int) step), next = first + 1;
        float between = smooth(step - first);
        float rx = lerp(SEALS[first][0], SEALS[next][0], between);
        float ry = lerp(SEALS[first][1], SEALS[next][1], between);
        float lx = lerp(SEALS[first][2], SEALS[next][2], between);
        float ly = lerp(SEALS[first][3], SEALS[next][3], between);
        float breath = (float) Math.sin(time * .12f) * .012f;
        boolean ground = element == ItemJutsu.JutsuEnum.Type.DOTON || element == ItemJutsu.JutsuEnum.Type.MOKUTON;
        boolean fire = element == ItemJutsu.JutsuEnum.Type.KATON;
        boolean lightning = element == ItemJutsu.JutsuEnum.Type.RAITON;
        if (hinged) {
            arm(bipedRightArm, lerp(-.14f + breath, ground ? -.72f : fire ? -.3f : -1.38f, release),
                lerp(-.02f, lightning ? -.14f : -.06f, release), .045f, blend);
            arm(bipedLeftArm, lerp(-.14f - breath, ground ? -.72f : fire ? -.3f : lightning ? -.22f : -1.38f, release),
                lerp(.02f, .06f, release), -.045f, blend);
            rightForearm.rotateAngleX = lerp(rx, ground ? -.10f : fire ? -1.82f : -.16f, release) * blend;
            rightForearm.rotateAngleY = lerp(ry, ground ? -.35f : fire ? -.84f : -.06f, release) * blend;
            leftForearm.rotateAngleX = lerp(lx, ground ? -.10f : fire ? -1.82f : lightning ? -1.35f : -.16f, release) * blend;
            leftForearm.rotateAngleY = lerp(ly, ground ? .35f : fire ? .84f : lightning ? .5f : .06f, release) * blend;
            rightForearm.rotateAngleZ = leftForearm.rotateAngleZ = 0;
        } else {
            // Armored players retain vanilla cuboids; restrained crossed hands avoid clipping the chest plate.
            arm(bipedRightArm, lerp(-1.32f + (rx + 1.85f) * .23f, ground ? -.82f : -1.42f, release),
                lerp(-.40f + (ry + .88f) * .3f, -.06f, release), .025f, blend);
            arm(bipedLeftArm, lerp(-1.32f + (lx + 1.85f) * .23f, ground ? -.82f : lightning ? -.7f : -1.42f, release),
                lerp(.40f + (ly - .88f) * .3f, .06f, release), -.025f, blend);
        }
        float brace = release * blend * (ground ? .22f : .075f);
        bipedBody.rotateAngleX += brace;
        bipedHead.rotateAngleX += .035f * blend - brace * .2f;
        if (!isRiding && !isSneak) {
            bipedRightLeg.rotateAngleX -= brace * .55f;
            bipedLeftLeg.rotateAngleX -= brace * .55f;
            bipedRightLeg.rotationPointX -= .18f * blend;
            bipedLeftLeg.rotationPointX += .18f * blend;
        }
        copyModelAngles(bipedHead, bipedHeadwear);
        copyModelAngles(bipedBody, bipedBodyWear);
        copyModelAngles(bipedRightArm, bipedRightArmwear);
        copyModelAngles(bipedLeftArm, bipedLeftArmwear);
        copyModelAngles(bipedRightLeg, bipedRightLegwear);
        copyModelAngles(bipedLeftLeg, bipedLeftLegwear);
        if (hinged) {
            copyModelAngles(rightForearm, rightSleeve);
            copyModelAngles(leftForearm, leftSleeve);
        }
    }

    private static final float[][] SEALS = {
        {-1.86f, -.93f, -1.86f, .93f},
        {-2.07f, -.82f, -1.64f, 1.00f},
        {-1.64f, -1.00f, -2.07f, .82f},
        {-1.91f, -.92f, -1.91f, .92f}
    };

    private static void arm(ModelRenderer arm, float x, float y, float z, float blend) {
        arm.rotateAngleX = lerp(arm.rotateAngleX, x, blend);
        arm.rotateAngleY = lerp(arm.rotateAngleY, y, blend);
        arm.rotateAngleZ = lerp(arm.rotateAngleZ, z, blend);
    }

    static float smooth(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    @Override
    public void postRenderArm(float scale, EnumHandSide side) {
        super.postRenderArm(scale, side);
        if (hinged) {
            // Held equipment follows the wrist rather than floating at the unbent vanilla wrist.
            ModelRenderer forearm = side == EnumHandSide.RIGHT ? rightForearm : leftForearm;
            forearm.postRender(scale);
            net.minecraft.client.renderer.GlStateManager.translate(0, -4 * scale, 0);
        }
    }

    void copyVisibility(ModelPlayer source) {
        setModelAttributes(source); isSneak = source.isSneak;
        leftArmPose = source.leftArmPose; rightArmPose = source.rightArmPose;
        ModelRenderer[] from = {source.bipedHead, source.bipedHeadwear, source.bipedBody, source.bipedBodyWear,
            source.bipedRightArm, source.bipedRightArmwear, source.bipedLeftArm, source.bipedLeftArmwear,
            source.bipedRightLeg, source.bipedRightLegwear, source.bipedLeftLeg, source.bipedLeftLegwear};
        ModelRenderer[] to = {bipedHead, bipedHeadwear, bipedBody, bipedBodyWear,
            bipedRightArm, bipedRightArmwear, bipedLeftArm, bipedLeftArmwear,
            bipedRightLeg, bipedRightLegwear, bipedLeftLeg, bipedLeftLegwear};
        for (int i = 0; i < from.length; i++) {
            to[i].showModel = from[i].showModel; to[i].isHidden = from[i].isHidden;
        }
    }
}
