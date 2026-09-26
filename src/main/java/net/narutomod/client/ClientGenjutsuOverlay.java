package net.narutomod.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Anime-inspired private stages; the real player stays in the multiplayer world. */
@SideOnly(Side.CLIENT)
public class ClientGenjutsuOverlay {
    private static int type, ticks, duration;
    private static World effectWorld;
    private static final GenjutsuStageRenderer stage = new GenjutsuStageRenderer();
    private static int releaseTicks;
    private static final Map<Integer, Cast> casts = new HashMap<>();
    private final Map<net.minecraft.client.renderer.entity.RenderPlayer,net.minecraft.client.model.ModelPlayer> poses = new IdentityHashMap<>();
    private final GenjutsuCastingModel castStandard=new GenjutsuCastingModel(false),castSlim=new GenjutsuCastingModel(true);
    private static final java.lang.reflect.Field MAIN_MODEL=net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.client.renderer.entity.RenderLivingBase.class,"mainModel","field_77045_g");
    private static class Cast { int age, type; Cast(int t) { type=t; } }
    public static void handleMessage(int kind, int length, int casterId) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if(kind<0) { releaseTicks=ticks>0?10:0; clear(); }
            else {activate(kind,length);stage.caster(casterId);}
        });
    }
    public static void activate(int kind,int length) {
        if(kind<0 || kind>4) return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null || mc.world==null)return;
        if(effectWorld!=mc.world)casts.clear();
        effectWorld=mc.world;type=kind;ticks=duration=Math.max(1,Math.min(200,length));releaseTicks=0;
        if(kind>0) {
            SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:sharingansfx"));
            if(sound!=null)mc.player.playSound(sound,.80f,1f);
        }
    }
    public static void cast(int entityId,int kind) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world==null || kind<0 || kind>4)return;
        if(effectWorld!=mc.world){clear();casts.clear();}
        effectWorld=mc.world;casts.put(entityId,new Cast(kind));
    }
    public static void clear() { ticks=duration=0;type=0; }
    private static int elapsed() { return Math.max(0,duration-ticks); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world!=effectWorld || mc.player==null || !mc.player.isEntityAlive()) {
            clear();casts.clear();releaseTicks=0;stage.release();effectWorld=mc.world;return;
        }
        if(mc.isGamePaused())return;
        casts.values().removeIf(c -> ++c.age>36);
        if(releaseTicks>0)--releaseTicks;
        if(ticks<=0)return;
        --ticks;
        float fade=GenjutsuVisuals.envelope(elapsed(),ticks);
        // One cue per choreography beat, independent of FPS. No repeated global Sharingan audio.
        if(type>=2 && (elapsed()%44==0 || elapsed()%44==7))
            mc.player.playSound(SoundEvents.BLOCK_NOTE_BASEDRUM,.14f*fade,.55f);
        if(type==1 && elapsed()>16 && elapsed()%36==18)mc.player.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT,.19f*fade,.72f);
        if(type==3 && elapsed()>=32 && (elapsed()-32)%32==0)mc.player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,.45f*fade,.72f);
        if(type==3 && elapsed()>=39 && (elapsed()-39)%32==0)mc.player.playSound(SoundEvents.ENTITY_PLAYER_HURT,.26f*fade,.72f);
        if(type==4 && elapsed()==48)mc.player.playSound(SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,.70f,.65f);
        if(type==4 && elapsed()>48 && elapsed()%36==16)mc.player.playSound(SoundEvents.BLOCK_FIRE_AMBIENT,.30f*fade,.75f);
        if(ticks==12)mc.player.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH,.20f,1.3f);
    }
    @SubscribeEvent public void input(InputUpdateEvent event) {
        if(ticks<=0 || event.getEntityPlayer()!=Minecraft.getMinecraft().player)return;
        if(type==0) {event.getMovementInput().moveForward*=-1;event.getMovementInput().moveStrafe*=-1;}
        else if(type==2) {event.getMovementInput().moveForward*=.25f;event.getMovementInput().moveStrafe*=.25f;}
        else if(type>=3) {
            event.getMovementInput().moveForward=event.getMovementInput().moveStrafe=0;
            event.getMovementInput().jump=event.getMovementInput().sneak=false;
        }
    }
    @SubscribeEvent public void camera(EntityViewRenderEvent.CameraSetup event) {
        if(ticks<=0)return;
        float time=elapsed()+(float)event.getRenderPartialTicks();
        float amount=GenjutsuVisuals.envelope(time,ticks);
        event.setRoll(event.getRoll()+(float)Math.sin(time*.075)*amount*(type==0?2.3f:type==1?1.2f:.45f));
    }
    @SubscribeEvent public void fog(EntityViewRenderEvent.FogColors event) {
        if(ticks<=0)return;
        float f=GenjutsuVisuals.envelope(elapsed(),ticks)*.55f;
        event.setRed(event.getRed()*(1-f)+.09f*f);
        event.setGreen(event.getGreen()*(1-f)+.035f*f);
        event.setBlue(event.getBlue()*(1-f)+.05f*f);
    }
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Pre event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null || event.getType()!=RenderGameOverlayEvent.ElementType.ALL)return;
        Cast self=casts.get(mc.player.getEntityId());
        if(ticks<=0 && self==null && releaseTicks<=0)return;
        float time=elapsed()+event.getPartialTicks(),fade=GenjutsuVisuals.envelope(time,ticks-event.getPartialTicks());
        double w=event.getResolution().getScaledWidth(),h=event.getResolution().getScaledHeight();
        // Forge fires Pre.ALL BEFORE setupOverlayRendering. Establish our own GUI projection
        // without calling that method (which clears the main depth buffer).
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();GlStateManager.loadIdentity();
        GlStateManager.ortho(0,w,h,0,1000,3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();GlStateManager.loadIdentity();
        GlStateManager.translate(0,0,-2000);
        if(ticks>0)stage.render(type,time,fade*(type==0?.52f:type==1?.94f:1f),(int)w,(int)h);
        GlStateManager.disableTexture2D();GlStateManager.disableDepth();
        GlStateManager.depthMask(false);GlStateManager.disableAlpha();GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770,771,1,0);
        BufferBuilder b=Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_TRIANGLES,DefaultVertexFormats.POSITION_COLOR);
        GenjutsuVisuals.Painter painter=(xy,colors)->{
            for(int i=0;i<3;i++){int c=colors[i];b.pos(xy[i*2]*w,xy[i*2+1]*h,-90).color((c>>16)&255,(c>>8)&255,c&255,(c>>>24)&255).endVertex();}
        };
        if(ticks>0)GenjutsuVisuals.draw(painter,type,time,fade);
        if(releaseTicks>0)GenjutsuVisuals.release(painter,releaseTicks/10f);
        if(self!=null)GenjutsuVisuals.drawCast(painter,self.type,self.age+event.getPartialTicks(),GenjutsuVisuals.envelope(self.age,36-self.age));
        Tessellator.getInstance().draw();
        GlStateManager.enableAlpha();GlStateManager.depthMask(true);GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();GlStateManager.disableBlend();GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }
    @SubscribeEvent(priority=net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST) public void pose(RenderPlayerEvent.Pre event) {
        Cast cast=casts.get(event.getEntityPlayer().getEntityId());
        if(cast==null || cast.age>31)return;
        net.minecraft.client.model.ModelPlayer model=event.getRenderer().getMainModel();
        GenjutsuCastingModel animated=event.getEntityPlayer() instanceof net.minecraft.client.entity.AbstractClientPlayer
            && "slim".equals(((net.minecraft.client.entity.AbstractClientPlayer)event.getEntityPlayer()).getSkinType())?castSlim:castStandard;
        animated.copyVisibility(model);animated.age=cast.age+event.getPartialRenderTick();poses.put(event.getRenderer(),model);
        try { MAIN_MODEL.set(event.getRenderer(),animated); } catch(IllegalAccessException ex) {throw new IllegalStateException(ex);}
    }
    @SubscribeEvent public void unpose(RenderPlayerEvent.Post event) {
        net.minecraft.client.model.ModelPlayer previous=poses.remove(event.getRenderer());
        if(previous!=null)try {MAIN_MODEL.set(event.getRenderer(),previous);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
    }
    @SubscribeEvent public void restoreModels(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        // Also restore if another renderer canceled a player draw after our Pre event.
        for(Map.Entry<net.minecraft.client.renderer.entity.RenderPlayer,net.minecraft.client.model.ModelPlayer> entry:poses.entrySet())
            try {MAIN_MODEL.set(entry.getKey(),entry.getValue());}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
        poses.clear();
    }
    @SubscribeEvent public void world(RenderWorldLastEvent event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world==null)return;
        for(Map.Entry<Integer,Cast> entry:casts.entrySet()) {
            Entity entity=mc.world.getEntityByID(entry.getKey());
            if(!(entity instanceof EntityLivingBase) || (entity==mc.player && mc.gameSettings.thirdPersonView==0))continue;
            EntityLivingBase caster=(EntityLivingBase)entity;Cast cast=entry.getValue();
            float age=cast.age+event.getPartialTicks(),fade=GenjutsuVisuals.envelope(age,36-age);
            double partial=event.getPartialTicks();
            GlStateManager.pushMatrix();
            GlStateManager.translate(entity.lastTickPosX+(entity.posX-entity.lastTickPosX)*partial-mc.getRenderManager().viewerPosX,
                entity.lastTickPosY+(entity.posY-entity.lastTickPosY)*partial+entity.getEyeHeight()-mc.getRenderManager().viewerPosY,
                entity.lastTickPosZ+(entity.posZ-entity.lastTickPosZ)*partial-mc.getRenderManager().viewerPosZ);
            GlStateManager.rotate(-caster.rotationYawHead,0,1,0);GlStateManager.translate(0,0,.31);
            GlStateManager.disableLighting();GlStateManager.disableTexture2D();GlStateManager.disableCull();
            GlStateManager.enableBlend();GlStateManager.disableAlpha();GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(770,771,1,0);
            BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_TRIANGLES,DefaultVertexFormats.POSITION_COLOR);
            double radius=.11+.17*(1-GenjutsuVisuals.smooth(age/24));
            for(int i=0;i<64;i++) {
                double a=i*Math.PI/32,aa=(i+1)*Math.PI/32;
                double x=Math.cos(a)*radius,y=Math.sin(a)*radius,xx=Math.cos(aa)*radius,yy=Math.sin(aa)*radius;
                tri(b,x,y,xx,yy,xx*.90,yy*.90,cast.type==0?.20f:.47f,.025f,.04f,fade*.8f);
                tri(b,x,y,xx*.90,yy*.90,x*.90,y*.90,cast.type==0?.20f:.47f,.025f,.04f,fade*.8f);
            }
            if(cast.type>0)for(int i=0;i<3;i++) {
                double a=i*Math.PI*2/3+GenjutsuVisuals.smooth(age/30)*1.5;
                double x=Math.cos(a)*radius*.62,y=Math.sin(a)*radius*.62;
                tri(b,x-.025,y-.018,x+.025,y-.018,x,y+.04,.015f,.01f,.02f,fade);
            }
            Tessellator.getInstance().draw();
            GlStateManager.depthMask(true);GlStateManager.enableAlpha();GlStateManager.disableBlend();
            GlStateManager.enableCull();GlStateManager.enableTexture2D();GlStateManager.enableLighting();
            GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();
        }
    }
    private static void tri(BufferBuilder b,double x,double y,double xx,double yy,double xxx,double yyy,float r,float g,float blue,float alpha) {
        b.pos(x,y,0).color(r,g,blue,alpha).endVertex();b.pos(xx,yy,0).color(r,g,blue,alpha).endVertex();b.pos(xxx,yyy,0).color(r,g,blue,alpha).endVertex();
    }
}
