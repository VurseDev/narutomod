package net.narutomod.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

/** Private depth-buffered stage. Never clears Minecraft's depth buffer or changes its camera/entity. */
@SideOnly(Side.CLIENT)
public final class GenjutsuStageRenderer {
    private Framebuffer stage;
    private final ModelPlayer standard=new ModelPlayer(0,false),slim=new ModelPlayer(0,true);
    private JsonArray fallback;
    private ResourceLocation casterSkin;
    private UUID casterId;
    private boolean casterSlim;
    private static final ResourceLocation ACTOR=new ResourceLocation("narutomod:textures/other/genjutsu_actor.png");
    public void caster(int id) {
        Minecraft mc=Minecraft.getMinecraft();casterSkin=null;casterSlim=false;casterId=null;
        if(mc.world!=null && mc.world.getEntityByID(id) instanceof AbstractClientPlayer) {
            AbstractClientPlayer player=(AbstractClientPlayer)mc.world.getEntityByID(id);
            casterId=player.getUniqueID();
            casterSkin=player.getLocationSkin();casterSlim="slim".equals(player.getSkinType());
        }
    }
    public void release() { if(stage!=null) {stage.deleteFramebuffer();stage=null;}casterSkin=null;casterId=null; }
    public void render(int type,float time,float opacity,int width,int height) {
        Minecraft mc=Minecraft.getMinecraft();
        if(!OpenGlHelper.isFramebufferEnabled() || mc.player==null || opacity<=0)return;
        if(casterId!=null && mc.getConnection()!=null) {
            net.minecraft.client.network.NetworkPlayerInfo info=mc.getConnection().getPlayerInfo(casterId);
            if(info!=null){casterSkin=info.getLocationSkin();casterSlim="slim".equals(info.getSkinType());}
        }
        // Cap cost at 1280 wide; this is an illusion, not another world renderer.
        GlStateManager.depthMask(true);GlStateManager.colorMask(true,true,true,true);
        mc.entityRenderer.disableLightmap();
        int rw=Math.min(1280,mc.displayWidth),rh=Math.max(1,Math.round((float)mc.displayHeight*rw/mc.displayWidth));
        if(stage==null || stage.framebufferWidth!=rw || stage.framebufferHeight!=rh) {
            if(stage!=null)stage.deleteFramebuffer();stage=new Framebuffer(rw,rh,true);
        }
        GenjutsuScene scene=new GenjutsuScene(type,time);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.pushMatrix();GlStateManager.loadIdentity();
        Project.gluPerspective(48f,(float)rw/rh,.08f,80f);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();GlStateManager.loadIdentity();
        try {
            stage.setFramebufferColor(type==1?.075f:.13f,type==1?.035f:.012f,type==1?.12f:.032f,type==0?0:1);
            stage.framebufferClear();stage.bindFramebuffer(true);
            GlStateManager.enableDepth();GlStateManager.depthMask(true);GlStateManager.disableCull();
            GlStateManager.disableLighting();GlStateManager.disableFog();GlStateManager.disableBlend();GlStateManager.disableAlpha();
            Project.gluLookAt((float)scene.cameraX,(float)scene.cameraY,(float)scene.cameraZ,0,(float)scene.lookY,0,0,1,0);
            drawStage(scene);
            for(GenjutsuScene.Actor actor:scene.actors)drawActor(actor,time);
        } finally {
            mc.getFramebuffer().bindFramebuffer(true);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();GlStateManager.enableAlpha();GlStateManager.enableCull();
            GlStateManager.disableLighting();GlStateManager.disableDepth();GlStateManager.depthMask(false);
            GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.color(1,1,1,1);
        }
        stage.bindFramebufferTexture();
        BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
        b.pos(0,height,-91).tex(0,0).color(1,1,1,opacity).endVertex();
        b.pos(width,height,-91).tex(1,0).color(1,1,1,opacity).endVertex();
        b.pos(width,0,-91).tex(1,1).color(1,1,1,opacity).endVertex();
        b.pos(0,0,-91).tex(0,1).color(1,1,1,opacity).endVertex();
        Tessellator.getInstance().draw();stage.unbindFramebufferTexture();
        GlStateManager.depthMask(true);GlStateManager.enableDepth();GlStateManager.disableBlend();
    }
    private void drawStage(GenjutsuScene scene) {
        // Group opaque geometry by texture: three draw calls rather than one per wooden plank.
        for(String material:new String[]{null,"stone","wood","fire"}) {
            if(material==null)GlStateManager.disableTexture2D();
            else {
                GlStateManager.enableTexture2D();
                if(material.equals("fire")) {
                    GlStateManager.enableAlpha();
                    Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("narutomod:textures/blocks/fire_layer_0.png"));
                } else Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("minecraft:textures/blocks/"+(material.equals("wood")?"planks_oak":"stonebrick")+".png"));
            }
            BufferBuilder b=Tessellator.getInstance().getBuffer();
            b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
            for(GenjutsuScene.Quad q:scene.quads)if(Objects.equals(q.texture,material))for(int i=0;i<4;i++)
                b.pos(q.xyz[i*3],q.xyz[i*3+1],q.xyz[i*3+2]).tex(q.uv[i*2],q.uv[i*2+1])
                    .color((q.color>>16)&255,(q.color>>8)&255,q.color&255,255).endVertex();
            Tessellator.getInstance().draw();
        }
    }
    private void drawActor(GenjutsuScene.Actor actor,float time) {
        Minecraft mc=Minecraft.getMinecraft();
        GlStateManager.enableTexture2D();GlStateManager.enableAlpha();GlStateManager.pushMatrix();
        GlStateManager.translate(actor.x,actor.y,actor.z);GlStateManager.rotate((float)actor.yaw,0,1,0);
        GlStateManager.scale(actor.scale,actor.scale,actor.scale);
        if(!actor.victim && casterSkin==null)fallback(actor.pose,time);
        else {
            boolean isSlim=actor.victim?"slim".equals(mc.player.getSkinType()):casterSlim;
            ModelPlayer model=isSlim?slim:standard;
            mc.getTextureManager().bindTexture(actor.victim?mc.player.getLocationSkin():casterSkin);
            GlStateManager.color(actor.victim?1f:.86f,actor.victim?.91f:.75f,actor.victim?.88f:.78f,1);
            GlStateManager.translate(0,1.50,0);GlStateManager.scale(1,-1,-1);
            model.isChild=model.isRiding=model.isSneak=false;model.swingProgress=0;
            model.leftArmPose=model.rightArmPose=ModelBiped.ArmPose.EMPTY;
            model.setRotationAngles(0,0,time,0,actor.victim?12:0,.0625f,mc.player);
            model.bipedLeftArm.rotateAngleX=model.bipedRightArm.rotateAngleX=0;
            model.bipedLeftArm.rotateAngleY=model.bipedRightArm.rotateAngleY=0;
            if(actor.victim && actor.pose==1) {
                model.bipedRightArm.rotateAngleZ=1.52f;model.bipedLeftArm.rotateAngleZ=-1.52f;
            } else if(!actor.victim) {
                model.bipedRightArm.rotateAngleX=-.3f+actor.pose*1.25f;
                model.bipedLeftArm.rotateAngleX=actor.pose<0?-.65f:-.3f-actor.pose;
                model.bipedRightArm.rotateAngleZ=.1f;model.bipedLeftArm.rotateAngleZ=-.1f;
            }
            ModelBase.copyModelAngles(model.bipedHead,model.bipedHeadwear);
            ModelBase.copyModelAngles(model.bipedBody,model.bipedBodyWear);
            ModelBase.copyModelAngles(model.bipedRightArm,model.bipedRightArmwear);
            ModelBase.copyModelAngles(model.bipedLeftArm,model.bipedLeftArmwear);
            ModelBase.copyModelAngles(model.bipedRightLeg,model.bipedRightLegwear);
            ModelBase.copyModelAngles(model.bipedLeftLeg,model.bipedLeftLegwear);
            for(ModelRenderer part:new ModelRenderer[]{model.bipedHead,model.bipedHeadwear,model.bipedBody,model.bipedBodyWear,
                model.bipedLeftArm,model.bipedLeftArmwear,model.bipedRightArm,model.bipedRightArmwear,
                model.bipedLeftLeg,model.bipedLeftLegwear,model.bipedRightLeg,model.bipedRightLegwear})part.render(.0625f);
        }
        GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();
    }
    private void fallback(float pose,float time) {
        Minecraft mc=Minecraft.getMinecraft();
        if(fallback==null)try(InputStreamReader reader=new InputStreamReader(mc.getResourceManager().getResource(new ResourceLocation("narutomod:models/custom/genjutsu_actor.json")).getInputStream(),StandardCharsets.UTF_8)) {
            fallback=new JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("cubes");
        } catch(IOException ex) {throw new IllegalStateException("Missing Blockbench genjutsu actor",ex);}
        mc.getTextureManager().bindTexture(ACTOR);GlStateManager.rotate(180,0,1,0);GlStateManager.color(1,1,1,1);
        for(JsonElement entry:fallback) {
            JsonObject cube=entry.getAsJsonObject();String name=cube.get("name").getAsString();
            double[] a=vector(cube.getAsJsonArray("from")),c=vector(cube.getAsJsonArray("to")),pivot=vector(cube.getAsJsonArray("origin"));
            GlStateManager.pushMatrix();GlStateManager.translate(pivot[0],pivot[1],pivot[2]);
            if(name.contains("arm"))GlStateManager.rotate(pose*65-15,1,0,0);
            if(name.contains("head"))GlStateManager.rotate((float)Math.sin(time*.035)*4,0,1,0);
            GlStateManager.translate(-pivot[0],-pivot[1],-pivot[2]);
            BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
            for(Map.Entry<String,JsonElement> f:cube.getAsJsonObject("faces").entrySet()) {
                double[][] p;
                switch(f.getKey()) {
                    case "north":p=new double[][]{{c[0],c[1],a[2]},{c[0],a[1],a[2]},{a[0],a[1],a[2]},{a[0],c[1],a[2]}};break;
                    case "south":p=new double[][]{{a[0],c[1],c[2]},{a[0],a[1],c[2]},{c[0],a[1],c[2]},{c[0],c[1],c[2]}};break;
                    case "east":p=new double[][]{{c[0],c[1],c[2]},{c[0],a[1],c[2]},{c[0],a[1],a[2]},{c[0],c[1],a[2]}};break;
                    case "west":p=new double[][]{{a[0],c[1],a[2]},{a[0],a[1],a[2]},{a[0],a[1],c[2]},{a[0],c[1],c[2]}};break;
                    case "up":p=new double[][]{{a[0],c[1],a[2]},{a[0],c[1],c[2]},{c[0],c[1],c[2]},{c[0],c[1],a[2]}};break;
                    default:p=new double[][]{{a[0],a[1],c[2]},{a[0],a[1],a[2]},{c[0],a[1],a[2]},{c[0],a[1],c[2]}};
                }
                JsonArray uv=f.getValue().getAsJsonObject().getAsJsonArray("uv");
                double[] u={uv.get(0).getAsDouble(),uv.get(0).getAsDouble(),uv.get(2).getAsDouble(),uv.get(2).getAsDouble()};
                double[] v={uv.get(1).getAsDouble(),uv.get(3).getAsDouble(),uv.get(3).getAsDouble(),uv.get(1).getAsDouble()};
                for(int i=0;i<4;i++)b.pos(p[i][0],p[i][1],p[i][2]).tex(u[i]/64,v[i]/64).color(1f,1f,1f,1f).endVertex();
            }
            Tessellator.getInstance().draw();GlStateManager.popMatrix();
        }
    }
    private static double[] vector(JsonArray a) {return new double[]{a.get(0).getAsDouble()/16,a.get(1).getAsDouble()/16,a.get(2).getAsDouble()/16};}
}
