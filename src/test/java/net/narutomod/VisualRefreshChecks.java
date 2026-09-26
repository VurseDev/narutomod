package net.narutomod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import net.narutomod.client.GenjutsuVisuals;
import net.narutomod.item.ItemNormalEyes;
import net.narutomod.item.ItemCurseMark;

/** Offline presentation checks: same triangles and vertex colours as the in-game overlay. */
public final class VisualRefreshChecks {
    private static int checks;
    private static void check(boolean condition,String reason) { checks++;if(!condition)throw new AssertionError(reason); }
    public static void main(String[] args) throws Exception {
        check(ItemNormalEyes.EyeColor.BLACK.registryName.equals("normal_eyes_black"),"stable black eye item ID");
        check(ItemNormalEyes.EyeColor.valueOf("BLACK")==ItemNormalEyes.EyeColor.BLACK,"saved colour roundtrip");
        check(ItemNormalEyes.EyeColor.values().length==6,"five existing colours preserved");
        check(Files.exists(Paths.get("src/main/resources/assets/narutomod/textures/normal_eyes_black.png")),"black armor texture exists");
        check(Files.exists(Paths.get("src/main/resources/assets/narutomod/textures/blocks/normal_eyes_black.png")),"black item texture exists");
        check(ItemCurseMark.class.getAnnotation(ElementsNarutomodMod.ModElement.Tag.class)==null,"cursed seal feature must not autoload");
        for(ItemCurseMark.Mark mark:ItemCurseMark.Mark.values())
            check(RemovedCursedSeals.retired("narutomod",mark.registryName),"every retired seal has save migration");
        check(!RemovedCursedSeals.retired("othermod","curse_mark_heaven"),"other mods never removed");
        check(!RemovedCursedSeals.retired("narutomod","summoning_souls"),"Edo content never removed");
        check(GenjutsuVisuals.envelope(0,100)==0 && GenjutsuVisuals.envelope(100,0)==0,"clean effect endpoints");
        for(int i=0;i<=200;i++) {
            float f=GenjutsuVisuals.envelope(i,200-i);
            check(f>=0 && f<=1,"bounded envelope");
            if(i>0)check(Math.abs(f-GenjutsuVisuals.envelope(i-1,201-i))<.13,"no abrupt full-screen transitions");
        }
        String[] names={"False Opening","Memory Fracture","Murder Intent","Illusionary Execution","Burning Coffin"};
        BufferedImage sheet=new BufferedImage(1152,500,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=sheet.createGraphics();g.setColor(new Color(13,14,18));g.fillRect(0,0,1152,500);
        for(int type=0;type<5;type++) {
            BufferedImage scene=background(384,216);
            GenjutsuVisuals.draw((xy,c)->raster(scene,xy,c),type,65,1);
            int x=type%3*384,y=type/3*250;
            g.drawImage(scene,x,y,null);g.setColor(new Color(220,212,211));g.drawString(names[type]+" — offline effect preview",x+10,y+235);
            final int[] visible={0};
            GenjutsuVisuals.draw((xy,c)->{
                for(double coordinate:xy)check(Double.isFinite(coordinate),"finite geometry");
                for(int color:c)if((color>>>24)!=0)visible[0]++;
            },type,0,0);
            check(visible[0]==0,"zero envelope fully transparent");
        }
        g.dispose();Path out=Paths.get("build/reports/visual-refresh/genjutsu-previews.png");
        Files.createDirectories(out.getParent());ImageIO.write(sheet,"png",out.toFile());
        System.out.println("PASS: "+checks+" visual/state checks. Preview: "+out);
    }
    private static BufferedImage background(int w,int h) {
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=result.createGraphics();g.setColor(new Color(74,78,80));g.fillRect(0,0,w,h);
        g.setColor(new Color(90,96,94));g.fillRect(0,0,w,h/2);
        for(int y=0;y<h/2;y+=18)for(int x=-18;x<w;x+=36){g.setColor(new Color(71+(x+y&7),78,78));g.drawRect(x+(y/18%2)*18,y,35,17);}
        g.setColor(new Color(34,40,43));g.fillRect(w/2-32,h/4,64,h/2);
        g.setColor(new Color(81,85,79));g.fillPolygon(new int[]{0,w,w/2+44,w/2-44},new int[]{h,h,h/2,h/2},4);
        g.dispose();return result;
    }
    private static void raster(BufferedImage image,double[] xy,int[] colors) {
        double[] x={xy[0]*image.getWidth(),xy[2]*image.getWidth(),xy[4]*image.getWidth()};
        double[] y={xy[1]*image.getHeight(),xy[3]*image.getHeight(),xy[5]*image.getHeight()};
        double denominator=(y[1]-y[2])*(x[0]-x[2])+(x[2]-x[1])*(y[0]-y[2]);
        if(Math.abs(denominator)<.000001)return;
        int left=Math.max(0,(int)Math.floor(Math.min(x[0],Math.min(x[1],x[2]))));
        int right=Math.min(image.getWidth()-1,(int)Math.ceil(Math.max(x[0],Math.max(x[1],x[2]))));
        int top=Math.max(0,(int)Math.floor(Math.min(y[0],Math.min(y[1],y[2]))));
        int bottom=Math.min(image.getHeight()-1,(int)Math.ceil(Math.max(y[0],Math.max(y[1],y[2]))));
        for(int py=top;py<=bottom;py++)for(int px=left;px<=right;px++) {
            double a=((y[1]-y[2])*(px+.5-x[2])+(x[2]-x[1])*(py+.5-y[2]))/denominator;
            double b=((y[2]-y[0])*(px+.5-x[2])+(x[0]-x[2])*(py+.5-y[2]))/denominator,c=1-a-b;
            if(a<0 || b<0 || c<0)continue;
            double alpha=(a*(colors[0]>>>24)+b*(colors[1]>>>24)+c*(colors[2]>>>24))/255;
            int old=image.getRGB(px,py),rgb=0;
            for(int shift=0;shift<=16;shift+=8) {
                double channel=a*((colors[0]>>shift)&255)+b*((colors[1]>>shift)&255)+c*((colors[2]>>shift)&255);
                rgb|=((int)(channel*alpha+((old>>shift)&255)*(1-alpha)))<<shift;
            }
            image.setRGB(px,py,rgb);
        }
    }
}
