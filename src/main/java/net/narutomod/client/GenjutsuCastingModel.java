package net.narutomod.client;

import net.minecraft.client.model.*;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.*;

/** A short raise / seal / ocular release / lower choreography on the caster's normal skin. */
@SideOnly(Side.CLIENT)
final class GenjutsuCastingModel extends ModelPlayer {
    float age;
    GenjutsuCastingModel(boolean slim) { super(0,slim); }
    @Override public void setRotationAngles(float limb,float amount,float time,float yaw,float pitch,float scale,Entity entity) {
        super.setRotationAngles(limb,amount,time,yaw,pitch,scale,entity);
        float blend=GenjutsuVisuals.smooth(age/7)*(1-GenjutsuVisuals.smooth((age-21)/10));
        float seal=GenjutsuVisuals.smooth((age-7)/5);
        bipedRightArm.rotateAngleX+=( -1.38f-bipedRightArm.rotateAngleX)*blend;
        bipedLeftArm.rotateAngleX+=( -1.38f-bipedLeftArm.rotateAngleX)*blend;
        bipedRightArm.rotateAngleY=(-.30f-.10f*seal)*blend;
        bipedLeftArm.rotateAngleY=(.30f+.10f*seal)*blend;
        bipedRightArm.rotateAngleZ=.10f*blend;bipedLeftArm.rotateAngleZ=-.10f*blend;
        bipedHead.rotateAngleX+=.045f*blend;
        ModelBase.copyModelAngles(bipedHead,bipedHeadwear);
        ModelBase.copyModelAngles(bipedRightArm,bipedRightArmwear);
        ModelBase.copyModelAngles(bipedLeftArm,bipedLeftArmwear);
    }
    void copyVisibility(ModelPlayer source) {
        setModelAttributes(source);isSneak=source.isSneak;
        ModelRenderer[] from={source.bipedHead,source.bipedHeadwear,source.bipedBody,source.bipedBodyWear,source.bipedRightArm,
            source.bipedRightArmwear,source.bipedLeftArm,source.bipedLeftArmwear,source.bipedRightLeg,source.bipedRightLegwear,source.bipedLeftLeg,source.bipedLeftLegwear};
        ModelRenderer[] to={bipedHead,bipedHeadwear,bipedBody,bipedBodyWear,bipedRightArm,bipedRightArmwear,bipedLeftArm,bipedLeftArmwear,
            bipedRightLeg,bipedRightLegwear,bipedLeftLeg,bipedLeftLegwear};
        for(int i=0;i<from.length;i++){to[i].showModel=from[i].showModel;to[i].isHidden=from[i].isHidden;}
    }
}
