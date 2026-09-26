package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.*;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.narutomod.*;
import net.narutomod.item.*;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.opengl.GL11;

/** Two anatomical half-face surfaces using the mod's original eye UVs and vertical fitting. */
@SideOnly(Side.CLIENT)
public final class OcularModel extends ModelBiped {
    private static final OcularModel INSTANCE=new OcularModel();
    public static ModelBiped get(EntityLivingBase e){INSTANCE.isSneak=e.isSneaking();INSTANCE.isRiding=e.isRiding();INSTANCE.isChild=e.isChild();return INSTANCE;}
    @Override public void render(Entity entity,float swing,float amount,float age,float yaw,float pitch,float scale){
        if(!(entity instanceof EntityLivingBase)||!OcularSystem.enabled((EntityLivingBase)entity))return;
        setRotationAngles(swing,amount,age,yaw,pitch,scale,entity);
        GlStateManager.pushMatrix();
        if(entity.isSneaking())GlStateManager.translate(0,.2f,0);
        bipedHead.postRender(scale);GlStateManager.scale(scale,scale,scale);
        GlStateManager.translate(0,EyeCustomization.getVertical(entity),-.025f);
        GlStateManager.enableBlend();GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();GlStateManager.color(1,1,1,1);
        OcularState s=OcularSystem.state((EntityLivingBase)entity);
        for(int side=0;side<2;side++){
            OcularState.Eye eye=s.eyes[side];
            if(eye==null||eye.covered){patch(side);continue;}
            ItemStack stack=eye.stack;
            if(eye.sharingan()&&!eye.active){
                String saved=s.normalVariant;
                ItemNormalEyes.EyeColor normal=ItemNormalEyes.EyeColor.HAZEL;
                for(ItemNormalEyes.EyeColor c:ItemNormalEyes.EyeColor.values())if(c.name().equalsIgnoreCase(saved)||c.registryName.equals(saved))normal=c;
                stack=new ItemStack(normal.getItem());
            }
            if(!(stack.getItem() instanceof ItemArmor))continue;
            String texture=((ItemArmor)stack.getItem()).getArmorTexture(stack,entity,EntityEquipmentSlot.HEAD,null);
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(texture));
            quad(side,8,8,-4.035f); // Base eyelid / sclera front face.
            ItemNormalEyes.EyeColor color=ItemNormalEyes.getEyeColor(stack);
            if((eye.sharingan()&&eye.active&&!ItemSharingan.isBlinded(stack))||eye.rinnegan()||(eye.byakugan()&&eye.active)||color==ItemNormalEyes.EyeColor.BLACK){
                float tint=color==ItemNormalEyes.EyeColor.BLACK?.1f:1f;GlStateManager.color(tint,tint,tint,1);quad(side,24,0,-4.07f);GlStateManager.color(1,1,1,1);
            }
            if(eye.byakugan()&&eye.usable())quad(side,40,8,-4.10f);
        }
        GlStateManager.color(1,1,1,1);GlStateManager.enableCull();GlStateManager.disableBlend();GlStateManager.popMatrix();
    }
    private static void quad(int side,float u,float v,float z){
        // ModelBox north face runs from anatomical left (+X) toward right (-X).
        float lo=side==0?0:-4,hi=lo+4,uv=u+(side==0?0:4);
        BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_NORMAL);
        b.pos(hi,-8,z).tex(uv/64,v/16).normal(0,0,-1).endVertex();
        b.pos(lo,-8,z).tex((uv+4)/64,v/16).normal(0,0,-1).endVertex();
        b.pos(lo,0,z).tex((uv+4)/64,(v+8)/16).normal(0,0,-1).endVertex();
        b.pos(hi,0,z).tex(uv/64,(v+8)/16).normal(0,0,-1).endVertex();Tessellator.getInstance().draw();
    }
    private static void patch(int side){
        float lo=side==0?.25f:-3.95f,hi=side==0?3.95f:-.25f;
        GlStateManager.disableTexture2D();GlStateManager.color(.09f,.10f,.12f,1);
        BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_NORMAL);
        b.pos(hi,-4.25,-4.12).normal(0,0,-1).endVertex();b.pos(lo,-4.25,-4.12).normal(0,0,-1).endVertex();
        b.pos(lo,-1.45,-4.12).normal(0,0,-1).endVertex();b.pos(hi,-1.45,-4.12).normal(0,0,-1).endVertex();
        Tessellator.getInstance().draw();GlStateManager.enableTexture2D();GlStateManager.color(1,1,1,1);
    }
}
