package net.narutomod.client;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.*;
import net.narutomod.PaperBombPolicy;
import net.narutomod.entity.EntityPaperBombCast;
import org.lwjgl.opengl.GL11;

/** Original explosive-tag ink and paper palette; hinged quads flutter instead of spinning icons. */
@SideOnly(Side.CLIENT)
public final class RenderPaperBombCast extends Render<EntityPaperBombCast> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("narutomod:textures/blocks/explosivetag.png");
    public RenderPaperBombCast(RenderManager manager){super(manager);}
    public static void register(){RenderingRegistry.registerEntityRenderingHandler(EntityPaperBombCast.class,RenderPaperBombCast::new);}
    @Override protected ResourceLocation getEntityTexture(EntityPaperBombCast e){return TEXTURE;}
    @Override public void doRender(EntityPaperBombCast e,double x,double y,double z,float yaw,float partial){
        NBTTagCompound frame=e.frame();NBTTagList tags=frame.getTagList("Tags",10),old=e.previousFrame().getTagList("Tags",10);
        float blend=e.frameFraction(partial),age=frame.getInteger("Age")+partial;
        int mode=frame.getInteger("Mode");
        GlStateManager.pushMatrix();GlStateManager.translate(x,y,z);
        GlStateManager.disableCull();GlStateManager.disableLighting();GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770,771,1,0);
        bindTexture(TEXTURE);
        for(int i=0;i<tags.tagCount();i++){
            NBTTagCompound n=tags.getCompoundTagAt(i);if(n.getBoolean("Spent"))continue;
            NBTTagCompound p=i<old.tagCount()?old.getCompoundTagAt(i):n;
            GlStateManager.pushMatrix();
            GlStateManager.translate(lerp(p,n,"X",blend),lerp(p,n,"Y",blend),lerp(p,n,"Z",blend));
            int face=n.getInteger("Face");boolean stuck=n.getBoolean("Stuck");
            // Quad normal starts +Z. Align with the actual hit surface, including ceiling seals.
            if(face==0)GlStateManager.rotate(90,1,0,0);
            else if(face==1)GlStateManager.rotate(-90,1,0,0);
            else if(face==2)GlStateManager.rotate(180,0,1,0);
            else if(face==4)GlStateManager.rotate(-90,0,1,0);
            else if(face==5)GlStateManager.rotate(90,0,1,0);
            else if(face<0){
                double vx=n.getDouble("VX"),vy=n.getDouble("VY"),vz=n.getDouble("VZ");
                GlStateManager.rotate((float)Math.toDegrees(Math.atan2(vx,vz)),0,1,0);
                if(!stuck){GlStateManager.rotate((float)(-Math.toDegrees(Math.atan2(vy,Math.sqrt(vx*vx+vz*vz)))+70),1,0,0);GlStateManager.rotate((float)Math.sin(age*.45+i)*14,0,1,0);}
            }
            int fuse=n.getInteger("Fuse");
            float pulse=fuse>0?(float)(.5+.5*Math.sin(age*(fuse<10?1.6:.65))):0;
            int count=mode==PaperBombPolicy.BREACH?5:1;
            for(int j=0;j<count;j++){
                GlStateManager.pushMatrix();
                if(count>1){GlStateManager.rotate(j*72,0,0,1);GlStateManager.translate(0,.13,.001*j);GlStateManager.scale(.7,.7,.7);}
                paper(stuck?0:(float)Math.sin(age*.7+i)*24,pulse);
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
        }
        if(mode==PaperBombPolicy.CIRCUIT){
            GlStateManager.disableTexture2D();
            BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
            float alpha=frame.getBoolean("Triggered")?.7f:age<20?.18f:.35f;
            // Thin ground ribbons mark the danger square, without hiding the terrain.
            if(tags.tagCount()==4){
                int[] order={0,1,3,2,0};
                for(int i=0;i<4;i++)ribbon(b,tags.getCompoundTagAt(order[i]),tags.getCompoundTagAt(order[i+1]),alpha);
            }
            Tessellator.getInstance().draw();GlStateManager.enableTexture2D();
        }
        GlStateManager.color(1,1,1,1);GlStateManager.disableBlend();GlStateManager.enableLighting();GlStateManager.enableCull();GlStateManager.popMatrix();
    }
    private static double lerp(NBTTagCompound a,NBTTagCompound b,String key,float t){return a.getDouble(key)+(b.getDouble(key)-a.getDouble(key))*t;}
    private static void paper(float fold,float pulse){
        half(0,.38,0,.5,pulse);
        GlStateManager.pushMatrix();GlStateManager.rotate(fold,1,0,0);half(-.38,0,.5,1,pulse);GlStateManager.popMatrix();
    }
    private static void half(double bottom,double top,double vTop,double vBottom,float pulse){
        BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
        float green=1-pulse*.35f,blue=1-pulse*.7f;
        b.pos(-.19,top,0).tex(.25,vTop).color(1,green,blue,1).endVertex();
        b.pos(.19,top,0).tex(.75,vTop).color(1,green,blue,1).endVertex();
        b.pos(.19,bottom,0).tex(.75,vBottom).color(1,green,blue,1).endVertex();
        b.pos(-.19,bottom,0).tex(.25,vBottom).color(1,green,blue,1).endVertex();Tessellator.getInstance().draw();
    }
    private static void ribbon(BufferBuilder b,NBTTagCompound a,NBTTagCompound c,float alpha){
        double ax=a.getDouble("X"),az=a.getDouble("Z"),ay=a.getDouble("Y")+.012;
        double bx=c.getDouble("X"),bz=c.getDouble("Z"),by=c.getDouble("Y")+.012;
        double dx=(bz-az)*.006,dz=-(bx-ax)*.006;
        b.pos(ax-dx,ay,az-dz).color(.65f,.2f,.045f,alpha).endVertex();b.pos(bx-dx,by,bz-dz).color(.65f,.2f,.045f,alpha).endVertex();
        b.pos(bx+dx,by,bz+dz).color(.65f,.2f,.045f,alpha).endVertex();b.pos(ax+dx,ay,az+dz).color(.65f,.2f,.045f,alpha).endVertex();
    }
}
