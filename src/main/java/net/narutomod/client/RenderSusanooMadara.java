package net.narutomod.client;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.MadaraSusanooPolicy;
import net.narutomod.entity.EntitySusanooMadara;
import org.lwjgl.opengl.GL11;

/** Native 1.12 renderer: the same textured, angular style as the legacy Susanoos,
 * with a dedicated four-chain Blockbench rig instead of an external animation runtime. */
@SideOnly(Side.CLIENT)
public final class RenderSusanooMadara extends Render<EntitySusanooMadara> {
    private final Map<String,MadaraModel> models=new HashMap<>();
    private final Map<Integer,double[]> stageBounds=new HashMap<>();
    public static void register(){RenderingRegistry.registerEntityRenderingHandler(EntitySusanooMadara.class,RenderSusanooMadara::new);}
    public RenderSusanooMadara(RenderManager manager){super(manager);shadowSize=0;((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(r->{models.clear();stageBounds.clear();});}
    @Override protected ResourceLocation getEntityTexture(EntitySusanooMadara entity){return texture(entity.getStage());}
    private static ResourceLocation texture(int stage){return new ResourceLocation("narutomod:textures/susanoo_madara_"+MadaraSusanooPolicy.assetName(stage)+".png");}
    private MadaraModel model(int stage){return models.computeIfAbsent(MadaraSusanooPolicy.assetName(stage),key->{
        ResourceLocation asset=new ResourceLocation("narutomod:models/custom/madara/"+key+".json");
        try(InputStreamReader reader=new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(asset).getInputStream(),StandardCharsets.UTF_8)){return new MadaraModel(reader);}
        catch(Exception ex){throw new IllegalStateException("Unable to load Madara Susanoo "+asset,ex);}
    });}
    @Override public void doRender(EntitySusanooMadara e,double x,double y,double z,float yaw,float partial){
        // Keep the caster's real player render in-world. Only the enclosing chakra
        // construct is drawn here; there is no duplicate player or dimension transfer.
        float oldLightX=OpenGlHelper.lastBrightnessX,oldLightY=OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x,y,z);
        GlStateManager.rotate(180-e.rotationYaw,0,1,0);
        GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);
        GlStateManager.disableCull();GlStateManager.disableLighting();
        GlStateManager.alphaFunc(GL11.GL_GREATER,.02f);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
        try {
            float transition=e.getTransitionProgress(partial),spawn=Math.min(1,(e.ticksExisted+partial)/18f);
            int stage=e.getStage(),old=e.getPreviousStage();
            double height=MadaraSusanooPolicy.height(old)+(MadaraSusanooPolicy.height(stage)-MadaraSusanooPolicy.height(old))*transition;
            // Adjacent forms crossfade while world-space height eases continuously.
            if(old!=stage&&transition<1)draw(e,old,partial,(1-transition)*spawn,height);
            draw(e,stage,partial,(old==stage?1:transition)*spawn,height);
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,oldLightX,oldLightY);
            GlStateManager.depthMask(true);GlStateManager.alphaFunc(GL11.GL_GREATER,.1f);
            GlStateManager.color(1,1,1,1);GlStateManager.enableLighting();GlStateManager.enableCull();GlStateManager.disableBlend();GlStateManager.popMatrix();
        }
    }
    private void draw(EntitySusanooMadara e,int stage,float partial,float fade,double height){
        if(fade<.015f)return;
        MadaraModel mesh=model(stage);MadaraModel.Pose pose=new MadaraModel.Pose();
        pose.stage=stage;pose.age=e.ticksExisted+partial;pose.phase=e.getCastPhase();pose.profile=e.getCastProfile();pose.castAge=e.getCastAge(partial);pose.armMask=e.getArmMask();
        pose.walk=e.limbSwing;pose.stride=Math.min(1,e.prevLimbSwingAmount+(e.limbSwingAmount-e.prevLimbSwingAmount)*partial);
        pose.recoveryPhase=e.getRecoveryPhase();pose.recoveryAge=e.getRecoveryAge();
        pose.attack=e.getSwingProgress(partial);pose.swords=e.shouldShowSword();
        EntityLivingBase owner=e.getOwnerPlayer();
        if(owner!=null){pose.headYaw=MathHelper.wrapDegrees(owner.rotationYawHead-e.rotationYaw);pose.headPitch=owner.rotationPitch;}
        Map<String,double[]> matrices=mesh.matrices(pose);
        double[] bounds=stageBounds.computeIfAbsent(stage,s->mesh.bounds(s));
        double baseline=bounds[1],scale=height/Math.max(.001,bounds[4]-baseline);
        bindTexture(texture(stage));
        // Translucent faces are back-to-front and do not mask the real caster.
        final double cameraX=renderManager.viewerPosX-e.posX,cameraY=renderManager.viewerPosY-e.posY,cameraZ=renderManager.viewerPosZ-e.posZ;
        List<DrawFace> draws=new ArrayList<>();
        double yaw=Math.toRadians(180-e.rotationYaw),cos=Math.cos(yaw),sin=Math.sin(yaw);
        for(MadaraModel.Face face:mesh.faces){
            if(!mesh.visible(face,pose))continue;
            double[] vertices=face.xyz.clone();MadaraModel.transform(vertices,matrices.get(face.bone));
            double cx=0,cy=0,cz=0;
            for(int i=0;i<12;i+=3){vertices[i]*=scale;vertices[i+1]=(vertices[i+1]-baseline)*scale;vertices[i+2]*=scale;cx+=vertices[i];cy+=vertices[i+1];cz+=vertices[i+2];}
            cx/=4;cy/=4;cz/=4;double dx=cx*cos+cz*sin-cameraX,dy=cy-cameraY,dz=-cx*sin+cz*cos-cameraZ;
            draws.add(new DrawFace(face,vertices,dx*dx+dy*dy+dz*dz));
        }
        draws.sort((a,b)->Double.compare(b.depth,a.depth));
        GlStateManager.depthMask(false);
        BufferBuilder buffer=Tessellator.getInstance().getBuffer();buffer.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
        float alpha=(stage<2?.70f:stage==5?.83f:.76f)*fade;
        for(DrawFace d:draws)for(int i=0;i<4;i++)buffer.pos(d.xyz[i*3],d.xyz[i*3+1],d.xyz[i*3+2]).tex(d.face.uv[i*2],d.face.uv[i*2+1]).color(d.face.light,d.face.light,d.face.light,alpha).endVertex();
        Tessellator.getInstance().draw();
    }
    private static final class DrawFace {final MadaraModel.Face face;final double[] xyz;final double depth;DrawFace(MadaraModel.Face f,double[] v,double d){face=f;xyz=v;depth=d;}}
}
