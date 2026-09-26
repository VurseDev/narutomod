package net.narutomod.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.model.*;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.MadaraTemporalController.EffectMessage;
import net.narutomod.TemporalHistory;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Short-lived reverse-path ghosts using the real caster skin (classic and slim). */
@SideOnly(Side.CLIENT)
public final class ClientTemporalEffects {
    private static final Map<UUID,Effect> ANCHORS=new HashMap<>();
    private static final List<Effect> TRAILS=new ArrayList<>();
    private static World effectWorld;
    private final ModelPlayer classic=new ModelPlayer(0,false),slim=new ModelPlayer(0,true);

    private static final class Effect {
        final UUID caster;
        final List<TemporalHistory.Frame> frames;
        final int lifetime;
        int age;
        Effect(EffectMessage message,int lifetime) {
            caster=message.caster;frames=message.frames;this.lifetime=lifetime;
        }
    }

    public static void receive(EffectMessage message) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world==null || mc.player==null || mc.player.dimension!=message.dimension)return;
        if(effectWorld!=mc.world){ANCHORS.clear();TRAILS.clear();effectWorld=mc.world;}
        ANCHORS.remove(message.caster);
        if(message.kind==0)ANCHORS.put(message.caster,new Effect(message,Math.max(1,Math.min(200,message.ticks))));
        else if(message.kind==1 && !message.frames.isEmpty()) {
            if(TRAILS.size()>=12)TRAILS.remove(0);
            TRAILS.add(new Effect(message,26));
        }
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world!=effectWorld || mc.player==null) {
            ANCHORS.clear();TRAILS.clear();effectWorld=mc.world;return;
        }
        if(mc.isGamePaused())return;
        ANCHORS.values().removeIf(effect->++effect.age>=effect.lifetime);
        TRAILS.removeIf(effect->++effect.age>=effect.lifetime);
    }

    @SubscribeEvent public void world(RenderWorldLastEvent event) {
        Minecraft mc=Minecraft.getMinecraft();
        Entity camera=mc.getRenderViewEntity();
        if(mc.world!=effectWorld || camera==null || ANCHORS.isEmpty() && TRAILS.isEmpty())return;
        float partial=event.getPartialTicks();
        double cx=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*partial;
        double cy=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*partial;
        double cz=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*partial;
        boolean lighting=GL11.glIsEnabled(GL11.GL_LIGHTING),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int alphaFunction=GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaReference=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        float lightX=OpenGlHelper.lastBrightnessX,lightY=OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-cx,-cy,-cz);
        try {
            GlStateManager.disableLighting();GlStateManager.enableBlend();GlStateManager.disableCull();GlStateManager.enableDepth();
            GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.depthMask(false);
            GlStateManager.enableAlpha();GlStateManager.alphaFunc(GL11.GL_GREATER,.01f);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
            for(Effect effect:ANCHORS.values()) {
                Entity caster=mc.world.getPlayerEntityByUUID(effect.caster);
                if(caster==null || caster.getDistanceSq(camera)>4096)continue;
                if(caster==mc.player && mc.gameSettings.thirdPersonView==0)continue;
                double x=caster.lastTickPosX+(caster.posX-caster.lastTickPosX)*partial;
                double y=caster.lastTickPosY+(caster.posY-caster.lastTickPosY)*partial;
                double z=caster.lastTickPosZ+(caster.posZ-caster.lastTickPosZ)*partial;
                ring(x,y+.08,z,.68f,effect.age+partial,.38f);
            }
            for(Effect effect:TRAILS) {
                float time=effect.age+partial,fade=Math.max(0,1-time/effect.lifetime);
                if(effect.frames.isEmpty())continue;
                TemporalHistory.Frame first=effect.frames.get(0);
                if((first.x-cx)*(first.x-cx)+(first.y-cy)*(first.y-cy)+(first.z-cz)*(first.z-cz)>4096)continue;
                // The leading ghost travels newest -> oldest; its trailing copies dissolve behind it.
                for(int ghost=5;ghost>=0;ghost--) {
                    float index=time/19f*(effect.frames.size()-1)-ghost*1.35f;
                    if(index<0 || index>effect.frames.size()-1)continue;
                    int low=(int)index,high=Math.min(effect.frames.size()-1,low+1);
                    TemporalHistory.Frame a=effect.frames.get(low),b=effect.frames.get(high);
                    float fraction=index-low;
                    float yaw=a.yaw+net.minecraft.util.math.MathHelper.wrapDegrees(b.yaw-a.yaw)*fraction;
                    TemporalHistory.Frame frame=new TemporalHistory.Frame(0,a.x+(b.x-a.x)*fraction,a.y+(b.y-a.y)*fraction,
                        a.z+(b.z-a.z)*fraction,yaw,a.pitch+(b.pitch-a.pitch)*fraction);
                    actor(effect.caster,frame,time,.43f*fade*(1-ghost*.10f));
                }
                TemporalHistory.Frame end=effect.frames.get(effect.frames.size()-1);
                ring(end.x,end.y+.05,end.z,.65f+time*.042f,-time*2,.55f*fade);
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lightX,lightY);
            GlStateManager.color(1,1,1,1);GlStateManager.depthMask(depthMask);
            GlStateManager.alphaFunc(alphaFunction,alphaReference);
            GlStateManager.tryBlendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
            if(texture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
            if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();
            if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();
            if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
            if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();
            if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    private void actor(UUID id,TemporalHistory.Frame frame,float age,float opacity) {
        Minecraft mc=Minecraft.getMinecraft();
        AbstractClientPlayer caster=mc.world.getPlayerEntityByUUID(id) instanceof AbstractClientPlayer
            ?(AbstractClientPlayer)mc.world.getPlayerEntityByUUID(id):null;
        NetworkPlayerInfo info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(id);
        ResourceLocation skin=caster!=null?caster.getLocationSkin():info!=null?info.getLocationSkin():null;
        if(skin==null)return; // Do not impersonate an unrelated fallback player.
        boolean small=caster!=null?"slim".equals(caster.getSkinType()):"slim".equals(info.getSkinType());
        ModelPlayer model=small?slim:classic;
        Entity actor=caster==null?mc.player:caster;
        GlStateManager.enableTexture2D();mc.getTextureManager().bindTexture(skin);GlStateManager.color(.74f,.90f,1,opacity);
        GlStateManager.pushMatrix();
        GlStateManager.translate(frame.x,frame.y+1.501,frame.z);GlStateManager.rotate(180-frame.yaw,0,1,0);GlStateManager.scale(1,-1,-1);
        model.isChild=model.isRiding=model.isSneak=false;model.swingProgress=0;
        model.leftArmPose=model.rightArmPose=ModelBiped.ArmPose.EMPTY;
        model.setRotationAngles(age*.6f,.55f,age,0,frame.pitch,.0625f,actor);
        ModelBase.copyModelAngles(model.bipedHead,model.bipedHeadwear);
        ModelBase.copyModelAngles(model.bipedBody,model.bipedBodyWear);
        ModelBase.copyModelAngles(model.bipedLeftArm,model.bipedLeftArmwear);
        ModelBase.copyModelAngles(model.bipedRightArm,model.bipedRightArmwear);
        ModelBase.copyModelAngles(model.bipedLeftLeg,model.bipedLeftLegwear);
        ModelBase.copyModelAngles(model.bipedRightLeg,model.bipedRightLegwear);
        for(ModelRenderer part:new ModelRenderer[]{model.bipedHead,model.bipedHeadwear,model.bipedBody,model.bipedBodyWear,
            model.bipedLeftArm,model.bipedLeftArmwear,model.bipedRightArm,model.bipedRightArmwear,
            model.bipedLeftLeg,model.bipedLeftLegwear,model.bipedRightLeg,model.bipedRightLegwear})part.render(.0625f);
        GlStateManager.popMatrix();
    }

    private static void ring(double x,double y,double z,float radius,float time,float opacity) {
        GlStateManager.disableTexture2D();GlStateManager.disableAlpha();
        BufferBuilder buffer=Tessellator.getInstance().getBuffer();buffer.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
        for(int i=0;i<48;i++) {
            if((i+(int)(time*.6f))%12>=9)continue;
            double a=i*Math.PI/24,b=(i+1)*Math.PI/24;
            for(double[] vertex:new double[][]{{a,radius},{b,radius},{b,radius+.035},{a,radius+.035}})
                buffer.pos(x+Math.cos(vertex[0])*vertex[1],y,z+Math.sin(vertex[0])*vertex[1]).color(.42f,.73f,1,opacity).endVertex();
        }
        Tessellator.getInstance().draw();GlStateManager.enableAlpha();GlStateManager.enableTexture2D();
    }

    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL || mc.player==null || mc.world!=effectWorld)return;
        Effect anchor=ANCHORS.get(mc.player.getUniqueID());
        if(anchor!=null) {
            int x=event.getResolution().getScaledWidth()/2,y=event.getResolution().getScaledHeight()-66;
            float remaining=Math.max(0,anchor.lifetime-anchor.age-event.getPartialTicks());
            Gui.drawRect(x-48,y,x+48,y+2,0x99324051);
            Gui.drawRect(x-48,y,x-48+(int)(96*remaining/anchor.lifetime),y+2,0xDC78C7FF);
            String text=net.minecraft.client.resources.I18n.format("overlay.madara.temporal_anchor",String.format(Locale.ROOT,"%.1f",remaining/20f));
            mc.fontRenderer.drawStringWithShadow(text,x-mc.fontRenderer.getStringWidth(text)/2f,y-11,0xBCE8FF);
        }
    }
}
