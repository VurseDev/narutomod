package net.narutomod.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.narutomod.item.BloodlineTechniques;
import net.narutomod.item.RenderBloodlineTechniques;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import sun.misc.Unsafe;

/** Offscreen production-renderer check, not a simulated in-game screenshot. */
public final class BloodlineRenderCapture {
    private static final int WIDTH = 1400, HEIGHT = 900;
    public static void main(String[] args) throws Exception {
        File output = new File(args[2]);
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create " + output);
        Pbuffer context = new Pbuffer(WIDTH, HEIGHT, new PixelFormat(), null, null);
        try {
            context.makeCurrent();
            Bootstrap.register();
            Method prepare = OcularUiCapture.class.getDeclaredMethod("prepareMinecraft", File.class, File.class);
            prepare.setAccessible(true);
            Minecraft mc = (Minecraft)prepare.invoke(null, new File(args[0]), new File(args[1]));
            OpenGlHelper.initializeTextures();
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
            RenderManager manager = (RenderManager)((Unsafe)unsafeField.get(null)).allocateInstance(RenderManager.class);
            Field textures = RenderManager.class.getDeclaredField("renderEngine"); textures.setAccessible(true); textures.set(manager, mc.renderEngine);
            manager.options = mc.gameSettings;
            manager.playerViewY = -35f; manager.playerViewX = 12f;
            RenderBloodlineTechniques.Dragon renderer = new RenderBloodlineTechniques.Dragon(manager);
            for (float power : new float[]{1f, 2.8f}) {
                begin(12);
                BloodlineTechniques.TwinDragon dragon = new BloodlineTechniques.TwinDragon(null);
                NBTTagCompound tag = new NBTTagCompound(); tag.setFloat("Power",power);tag.setInteger("Delay",12);tag.setInteger("Age",28);
                read(dragon, tag);
                GlStateManager.rotate(12,1,0,0);GlStateManager.rotate(-35,0,1,0);
                renderer.doRender(dragon,0,0,5,0,.5f);
                player(mc, -4, -2, 5);
                save(output, power==1 ? "dragon-tap" : "dragon-charged");
            }
            begin(4.2);
            GlStateManager.rotate(12,1,0,0);GlStateManager.rotate(-35,0,1,0);
            player(mc,0,-1.5,0);
            BloodlineTechniques.FlameCompany company=new BloodlineTechniques.FlameCompany(null);
            NBTTagCompound tag=new NBTTagCompound();tag.setFloat("Power",2);tag.setInteger("Charges",3);tag.setInteger("Age",20);
            read(company,tag);
            new RenderBloodlineTechniques.Company(manager).doRender(company,0,0,0,0,.5f);
            save(output,"flame-company-charged");
            System.out.println("Production Bloodline render captures: " + output);
        } finally { context.destroy(); }
    }
    private static void read(Object entity,NBTTagCompound nbt) throws Exception {
        Method read=entity.getClass().getDeclaredMethod("readEntityFromNBT",NBTTagCompound.class);read.setAccessible(true);read.invoke(entity,nbt);
    }
    private static void begin(double halfWidth) {
        GlStateManager.viewport(0,0,WIDTH,HEIGHT);
        GlStateManager.clearColor(.08f,.1f,.13f,1);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);GlStateManager.loadIdentity();
        double halfHeight=halfWidth*HEIGHT/WIDTH;
        GlStateManager.ortho(-halfWidth,halfWidth,-halfHeight,halfHeight,-80,80);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.loadIdentity();
        GlStateManager.enableDepth();GlStateManager.depthMask(true);GlStateManager.disableFog();
        GlStateManager.enableTexture2D();GlStateManager.enableAlpha();GlStateManager.alphaFunc(GL11.GL_GREATER,.03f);
        GlStateManager.disableLighting();GlStateManager.color(1,1,1,1);
    }
    private static void player(Minecraft mc,double x,double y,double z) {
        mc.renderEngine.bindTexture(new ResourceLocation("minecraft:textures/entity/steve.png"));
        GlStateManager.pushMatrix();GlStateManager.translate(x,y+1.5,z);GlStateManager.scale(1,-1,-1);
        GlStateManager.disableLighting();GlStateManager.color(.65f,.65f,.65f,1);
        ModelBiped m=new ModelPlayer(0, false);
        m.bipedHead.render(.0625f);m.bipedBody.render(.0625f);m.bipedLeftArm.render(.0625f);
        m.bipedRightArm.render(.0625f);m.bipedLeftLeg.render(.0625f);m.bipedRightLeg.render(.0625f);
        GlStateManager.popMatrix();GlStateManager.color(1,1,1,1);
    }
    private static void save(File output,String name) throws Exception {
        GL11.glFinish();int error=GL11.glGetError();
        if(error!=GL11.GL_NO_ERROR)throw new IllegalStateException("GL error "+error+" rendering "+name);
        ByteBuffer pixels=BufferUtils.createByteBuffer(WIDTH*HEIGHT*4);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);GL11.glReadPixels(0,0,WIDTH,HEIGHT,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
        BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++){
            int p=(y*WIDTH+x)*4;
            image.setRGB(x,HEIGHT-y-1,(pixels.get(p)&255)<<16|(pixels.get(p+1)&255)<<8|pixels.get(p+2)&255);
        }
        ImageIO.write(image,"png",new File(output,name+".png"));
    }
}
