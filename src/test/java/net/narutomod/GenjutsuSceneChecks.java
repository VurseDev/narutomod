package net.narutomod;

import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.narutomod.client.*;

/** Software-rasterized previews of the actual scene geometry and exported Blockbench actor, not concept art. */
public final class GenjutsuSceneChecks {
    private static int checks;
    private static void check(boolean condition,String why) {checks++;if(!condition)throw new AssertionError(why);}
    private static final int W=640,H=360;
    private final BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
    private final double[] depth=new double[W*H];
    private final GenjutsuScene scene;
    private final BufferedImage actor;
    private static BufferedImage fire;
    private final JsonArray cubes;
    GenjutsuSceneChecks(int type,float time,BufferedImage texture,JsonArray mesh) {
        scene=new GenjutsuScene(type,time);actor=texture;cubes=mesh;Arrays.fill(depth,Double.POSITIVE_INFINITY);
        Graphics2D g=image.createGraphics();g.setColor(type==1?new Color(19,9,31):new Color(33,3,8));g.fillRect(0,0,W,H);g.dispose();
    }
    public static void main(String[] args) throws Exception {
        check(GenjutsuSession.boundedDuration(3,900)==120,"execution max six seconds");
        check(GenjutsuSession.boundedDuration(4,900)==120,"coffin max six seconds");
        check(GenjutsuSession.boundedDuration(0,900)==200,"other max ten seconds");
        check(GenjutsuSession.expired(120,120,true,true,true),"expiry inclusive");
        check(!GenjutsuSession.expired(119,120,true,true,true),"active before expiry");
        check(GenjutsuSession.expired(20,120,false,true,true),"death clears");
        check(GenjutsuSession.expired(20,120,true,false,true),"caster loss clears");
        check(GenjutsuSession.expired(20,120,true,true,false),"dimension loss clears");
        for(int i=0;i<5;i++) {
            check(!GenjutsuSession.blocks(i,true),"Kai remains available");
            check(GenjutsuSession.blocks(i,false)==(i>=2),"only immobilizing concepts suppress jutsus");
        }
        check(GenjutsuScene.lid(18)==0 && GenjutsuScene.lid(48)==1,"door choreography endpoints");
        check(GenjutsuScene.impact(38)==0 && GenjutsuScene.impact(39)==1 && GenjutsuScene.impact(45)==0,"one six-tick strike accent");
        for(int type=0;type<5;type++)for(int tick=0;tick<=120;tick+=6) {
            GenjutsuScene scene=new GenjutsuScene(type,tick);
            check(scene.quads.size()<3500,"bounded stage geometry");
            check(scene.actors.size()<=12,"bounded actor count");
            for(GenjutsuScene.Quad q:scene.quads)for(double n:q.xyz)check(Double.isFinite(n),"finite animated geometry");
            check(scene.actors.stream().filter(a->a.victim).count()==(type>=3?1:0),"victim versus caster identity assignment");
        }
        String resource="src/main/resources/assets/narutomod/";
        BufferedImage texture=ImageIO.read(Paths.get(resource+"textures/other/genjutsu_actor.png").toFile());
        fire=ImageIO.read(Paths.get(resource+"textures/blocks/fire_layer_0.png").toFile());
        check(fire.getHeight()==fire.getWidth()*32,"32-frame existing mod flame strip");
        JsonObject model=new JsonParser().parse(new String(Files.readAllBytes(Paths.get(resource+"models/custom/genjutsu_actor.json")),"UTF-8")).getAsJsonObject();
        JsonArray cubes=model.getAsJsonArray("cubes");
        check(texture.getWidth()==64 && texture.getHeight()==64,"exact Blockbench atlas resolution");
        check(cubes.size()==6,"six actual textured biped cubes");
        for(JsonElement c:cubes)for(Map.Entry<String,JsonElement> f:c.getAsJsonObject().getAsJsonObject("faces").entrySet())
            for(JsonElement n:f.getValue().getAsJsonObject().getAsJsonArray("uv"))check(n.getAsDouble()>=0 && n.getAsDouble()<=64,"UV bounds");
        // New packet includes caster identity; the clear packet uses the same layout.
        io.netty.buffer.ByteBuf buf=io.netty.buffer.Unpooled.buffer();
        new net.narutomod.item.ItemInton.ClientGenjutsuMessage(3,120).toBytes(buf);
        check(buf.readableBytes()==12,"packet contains type, duration and caster entity ID");buf.release();
        int[] kinds={0,1,2,3,4,4};float[] times={60,60,60,40,25,70};
        String[] names={"FALSE OPENING / mirrored caster echoes","MEMORY FRACTURE / repeating corridor","MURDER INTENT / ocular pressure","ILLUSIONARY EXECUTION / cross field","BURNING COFFIN / lid closing","BURNING COFFIN / sealed and burning"};
        BufferedImage sheet=new BufferedImage(W*2,(H+42)*3,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();
        g.setColor(new Color(14,15,22));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        for(int i=0;i<kinds.length;i++) {
            GenjutsuSceneChecks preview=new GenjutsuSceneChecks(kinds[i],times[i],texture,cubes);preview.draw();
            int x=i%2*W,y=i/2*(H+42);g.drawImage(preview.image,x,y,null);
            g.setColor(new Color(225,217,214));g.setFont(new Font("SansSerif",Font.BOLD,13));g.drawString(names[i],x+12,y+H+18);
            g.setFont(new Font("SansSerif",Font.PLAIN,11));g.setColor(new Color(157,152,161));g.drawString("Offline geometry preview / fallback skins / live game uses caster + victim skins",x+12,y+H+34);
        }
        g.dispose();Path out=Paths.get("build/reports/genjutsu/scene-previews.png");Files.createDirectories(out.getParent());ImageIO.write(sheet,"png",out.toFile());
        System.out.println("PASS: genjutsu scene/policy/asset checks; "+checks+" assertions. Preview: "+out);
    }
    private void draw() {
        for(GenjutsuScene.Quad q:scene.quads)quad(q.xyz,q.uv,q.color,q.texture,"fire".equals(q.texture)?fire:null);
        for(GenjutsuScene.Actor a:scene.actors)for(JsonElement element:cubes) {
            JsonObject c=element.getAsJsonObject();double[] lo=vec(c.getAsJsonArray("from")),hi=vec(c.getAsJsonArray("to")),pivot=vec(c.getAsJsonArray("origin"));
            String name=c.get("name").getAsString();
            for(Map.Entry<String,JsonElement> entry:c.getAsJsonObject("faces").entrySet()) {
                double[] vertices;
                double x=lo[0],y=lo[1],z=lo[2],xx=hi[0],yy=hi[1],zz=hi[2];
                switch(entry.getKey()) {
                    case "north":vertices=new double[]{xx,yy,z,xx,y,z,x,y,z,x,yy,z};break;
                    case "south":vertices=new double[]{x,yy,zz,x,y,zz,xx,y,zz,xx,yy,zz};break;
                    case "east":vertices=new double[]{xx,yy,zz,xx,y,zz,xx,y,z,xx,yy,z};break;
                    case "west":vertices=new double[]{x,yy,z,x,y,z,x,y,zz,x,yy,zz};break;
                    case "up":vertices=new double[]{x,yy,z,x,yy,zz,xx,yy,zz,xx,yy,z};break;
                    default:vertices=new double[]{x,y,zz,x,y,z,xx,y,z,xx,y,zz};
                }
                for(int i=0;i<4;i++) {
                    double px=vertices[i*3]-pivot[0],py=vertices[i*3+1]-pivot[1],pz=vertices[i*3+2]-pivot[2];
                    if(name.contains("arm")) {
                        if(a.victim && a.pose==1) {
                            double angle=name.contains("right")?-1.52:1.52,nx=px*Math.cos(angle)-py*Math.sin(angle);
                            py=px*Math.sin(angle)+py*Math.cos(angle);px=nx;
                        } else {
                            double angle=Math.toRadians(a.pose*65-15),ny=py*Math.cos(angle)-pz*Math.sin(angle);
                            pz=py*Math.sin(angle)+pz*Math.cos(angle);py=ny;
                        }
                    }
                    px+=pivot[0];py+=pivot[1];pz+=pivot[2];
                    double angle=Math.toRadians(a.yaw+180),rx=px*Math.cos(angle)+pz*Math.sin(angle),rz=-px*Math.sin(angle)+pz*Math.cos(angle);
                    vertices[i*3]=rx*a.scale+a.x;vertices[i*3+1]=py*a.scale+a.y;vertices[i*3+2]=rz*a.scale+a.z;
                }
                JsonArray uv=entry.getValue().getAsJsonObject().getAsJsonArray("uv");
                double u=uv.get(0).getAsDouble()/64,v=uv.get(1).getAsDouble()/64,uu=uv.get(2).getAsDouble()/64,vv=uv.get(3).getAsDouble()/64;
                quad(vertices,new double[]{u,v,u,vv,uu,vv,uu,v},0xFFFFFFFF,null,actor);
            }
        }
    }
    private static double[] vec(JsonArray a) {return new double[]{a.get(0).getAsDouble()/16,a.get(1).getAsDouble()/16,a.get(2).getAsDouble()/16};}
    private void quad(double[] xyz,double[] uv,int color,String material,BufferedImage tex) {
        double[][] p=new double[4][];for(int i=0;i<4;i++)p[i]=project(xyz[i*3],xyz[i*3+1],xyz[i*3+2],uv[i*2],uv[i*2+1]);
        if(Arrays.stream(p).anyMatch(a->a[2]<.08))return;
        triangle(p[0],p[1],p[2],color,material,tex);triangle(p[0],p[2],p[3],color,material,tex);
    }
    private double[] project(double x,double y,double z,double u,double v) {
        double[] f={-scene.cameraX,scene.lookY-scene.cameraY,-scene.cameraZ};normalize(f);
        double[] r={-f[2],0,f[0]};normalize(r);
        double[] up={r[1]*f[2]-r[2]*f[1],r[2]*f[0]-r[0]*f[2],r[0]*f[1]-r[1]*f[0]};
        x-=scene.cameraX;y-=scene.cameraY;z-=scene.cameraZ;
        double d=x*f[0]+y*f[1]+z*f[2],scale=H/(2*Math.tan(Math.toRadians(24)));
        return new double[]{W*.5+(x*r[0]+y*r[1]+z*r[2])*scale/d,H*.5-(x*up[0]+y*up[1]+z*up[2])*scale/d,d,u,v};
    }
    private static void normalize(double[] a) {double length=Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);for(int i=0;i<3;i++)a[i]/=length;}
    private void triangle(double[] a,double[] b,double[] c,int color,String material,BufferedImage texture) {
        double den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<.00001)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),maxX=Math.min(W-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
        int minY=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),maxY=Math.min(H-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++) {
            double wa=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den;
            double wb=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,wc=1-wa-wb;
            if(wa<0 || wb<0 || wc<0)continue;
            double iz=wa/a[2]+wb/b[2]+wc/c[2],z=1/iz;if(z>=depth[y*W+x])continue;
            double u=(wa*a[3]/a[2]+wb*b[3]/b[2]+wc*c[3]/c[2])/iz,v=(wa*a[4]/a[2]+wb*b[4]/b[2]+wc*c[4]/c[2])/iz;
            int pixel=color;
            if(texture!=null)pixel=texture.getRGB(Math.min(texture.getWidth()-1,Math.max(0,(int)(u*texture.getWidth()))),Math.min(texture.getHeight()-1,Math.max(0,(int)(v*texture.getHeight()))));
            else if(material!=null) {
                boolean seam=material.equals("wood")?((int)(v*16)%4==0):((int)(v*16)%8==0 || ((int)(u*16)+((int)(v*2)%2)*8)%16==0);
                pixel=GenjutsuScene.shade(color,seam?.58:.85+((int)(u*16)*7+(int)(v*16)*3)%5*.04);
            }
            if((pixel>>>24)==0)continue;image.setRGB(x,y,pixel);depth[y*W+x]=z;
        }
    }
}
