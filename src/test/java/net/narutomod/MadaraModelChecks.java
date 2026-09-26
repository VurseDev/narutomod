package net.narutomod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.narutomod.client.MadaraModel;

/** Load the same assets and evaluate the same matrices used in the client.
 * Software previews sample the real texture UVs; no OpenGL context required. */
public final class MadaraModelChecks {
    private static final Path ASSETS=Paths.get("src/main/resources/assets/narutomod");
    private static int checks;
    private static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        BufferedImage sheet=new BufferedImage(1440,1040,BufferedImage.TYPE_INT_RGB);
        BufferedImage castSheet=new BufferedImage(1440,1040,BufferedImage.TYPE_INT_RGB);
        for(int stage=0;stage<6;stage++){
            String key=MadaraSusanooPolicy.assetName(stage);
            MadaraModel model;
            try(Reader r=Files.newBufferedReader(ASSETS.resolve("models/custom/madara/"+key+".json"),StandardCharsets.UTF_8)){model=new MadaraModel(r);}
            BufferedImage texture=ImageIO.read(ASSETS.resolve("textures/"+model.texture).toFile());
            check(texture!=null&&texture.getWidth()==512&&texture.getHeight()==512,"texture for stage "+stage);
            check(model.faces.size()>0,"geometry for stage "+stage);
            double[] bound=model.bounds(stage);
            for(double v:bound)check(Double.isFinite(v),"finite stage bound");
            check(bound[4]>bound[1]&&bound[3]>bound[0],"positive stage size");
            for(String side:new String[]{"front_right","front_left","rear_right","rear_left"})
                for(String bone:new String[]{"upper","forearm","hand","finger0"})check(model.hasBone(side+"_"+bone),"articulated "+side+"_"+bone+" in "+key);
            for(int phase=0;phase<=4;phase++)for(int profile=0;profile<=SusanooCastProfile.TEMPORAL;profile++)for(float age:new float[]{0,2,5,12,18,600}){
                MadaraModel.Pose pose=new MadaraModel.Pose();pose.stage=stage;pose.phase=phase;pose.profile=profile;pose.castAge=age;pose.age=90;pose.armMask=3;
                Map<String,double[]> matrices=model.matrices(pose);
                for(double[] matrix:matrices.values())for(double v:matrix)check(Double.isFinite(v),"finite posed matrix");
                for(MadaraModel.Face face:model.faces){
                    check(matrices.containsKey(face.bone),"renderable face owner");
                    // Also exercise ancestor visibility walks (previous self-root hung here).
                    model.visible(face,pose);
                }
            }
            MadaraModel.Pose idle=new MadaraModel.Pose();idle.stage=stage;idle.bind=true;
            for(float interruptedAge:new float[]{0,2,6,9,15})for(int recovery:new int[]{SusanooCastProfile.CANCEL,SusanooCastProfile.RELEASE}) {
                MadaraModel.Pose interrupted=new MadaraModel.Pose();interrupted.stage=stage;interrupted.profile=SusanooCastProfile.FIRE;
                interrupted.phase=SusanooCastProfile.PREPARE;interrupted.castAge=interruptedAge;
                Map<String,double[]> before=model.matrices(interrupted);
                interrupted.phase=recovery;interrupted.castAge=0;interrupted.recoveryPhase=SusanooCastProfile.PREPARE;interrupted.recoveryAge=interruptedAge;
                Map<String,double[]> after=model.matrices(interrupted);
                for(String bone:before.keySet())for(int i=0;i<16;i++)
                    check(Math.abs(before.get(bone)[i]-after.get(bone)[i])<1e-6,"early interruption preserves reached pose");
                if(recovery==SusanooCastProfile.CANCEL){
                    interrupted.castAge=SusanooCastProfile.CANCEL_TICKS;after=model.matrices(interrupted);
                    interrupted.phase=SusanooCastProfile.IDLE;before=model.matrices(interrupted);
                    for(String bone:before.keySet())for(int i=0;i<16;i++)
                        check(Math.abs(before.get(bone)[i]-after.get(bone)[i])<1e-6,"cancel ends at idle pose");
                }
            }
            MadaraModel.Pose casting=new MadaraModel.Pose();casting.stage=stage;casting.armMask=3;casting.phase=SusanooCastProfile.HOLD;casting.profile=SusanooCastProfile.FIRE;
            render(sheet,stage,model,texture,idle,"Stage "+stage+" / "+key);
            render(castSheet,stage,model,texture,casting,"Stage "+stage+" / held hand seal");
            for(int profile=0;profile<=SusanooCastProfile.TEMPORAL;profile++){
                MadaraModel.Pose held=new MadaraModel.Pose();held.profile=profile;held.armMask=3;held.phase=SusanooCastProfile.HOLD;
                Map<String,double[]> before=model.matrices(held);
                for(int phase:new int[]{SusanooCastProfile.RELEASE,SusanooCastProfile.CANCEL}){
                    held.phase=phase;Map<String,double[]> after=model.matrices(held);
                    for(String arm:new String[]{"front_right","front_left","rear_right","rear_left"})
                        for(String part:new String[]{"upper","forearm","hand"})for(int i=0;i<16;i++)
                            check(Math.abs(before.get(arm+"_"+part)[i]-after.get(arm+"_"+part)[i])<1e-6,"no release/cancel joint snap");
                }
            }
        }
        // Malformed user-edited resources fail at load, before recursive render or visibility.
        try {new MadaraModel(new StringReader("{\"texture\":\"x\",\"groups\":[{\"name\":\"root\",\"parent\":\"root\",\"origin\":[0,0,0],\"rotation\":[0,0,0]}],\"cubes\":[]}"));throw new AssertionError("cyclic root accepted");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("cycle"),"cycle rejected at load");}
        Path report=Paths.get("build/reports/madara");Files.createDirectories(report);
        ImageIO.write(sheet,"png",report.resolve("stages.png").toFile());ImageIO.write(castSheet,"png",report.resolve("casting.png").toFile());
        System.out.println("PASS: all six Madara stage resources, articulated poses, bounds and cycle rejection ("+checks+" assertions). Textured previews: "+report);
    }
    private static final class Quad {
        double[] screen,uv;double depth;
        Quad(double[] s,double[] u,double d){screen=s;uv=u;depth=d;}
    }
    private static void render(BufferedImage sheet,int stage,MadaraModel model,BufferedImage texture,MadaraModel.Pose pose,String title){
        int w=480,h=520;BufferedImage panel=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=panel.createGraphics();g.setColor(new Color(21,28,40));g.fillRect(0,0,w,h);g.setColor(new Color(37,48,61));
        for(int y=75;y<h;y+=28)g.drawLine(0,y,w,y);g.setFont(new Font("SansSerif",Font.PLAIN,18));g.setColor(new Color(192,220,245));g.drawString(title,18,29);g.dispose();
        Map<String,double[]> matrices=model.matrices(pose);java.util.List<Quad> quads=new ArrayList<>();
        double angle=.42,cs=Math.cos(angle),sn=Math.sin(angle),minX=1e9,maxX=-1e9,minY=1e9,maxY=-1e9;
        for(MadaraModel.Face face:model.faces)if(model.visible(face,pose)){
            double[] xyz=face.xyz.clone();MadaraModel.transform(xyz,matrices.get(face.bone));double[] xy=new double[8];double depth=0;
            for(int i=0;i<4;i++){
                double x=xyz[i*3],y=xyz[i*3+1],z=xyz[i*3+2],cameraZ=-sn*x+cs*z;
                xy[i*2]=cs*x+sn*z;xy[i*2+1]=-y+cameraZ*.13;depth+=cameraZ;
                minX=Math.min(minX,xy[i*2]);maxX=Math.max(maxX,xy[i*2]);minY=Math.min(minY,xy[i*2+1]);maxY=Math.max(maxY,xy[i*2+1]);
            }
            quads.add(new Quad(xy,face.uv,depth/4));
        }
        double scale=Math.min((w-65)/(maxX-minX),(h-90)/(maxY-minY));
        quads.sort((a,b)->Double.compare(b.depth,a.depth));
        for(Quad q:quads){for(int i=0;i<4;i++){q.screen[i*2]=w/2+(q.screen[i*2]-(minX+maxX)/2)*scale;q.screen[i*2+1]=58+(q.screen[i*2+1]-minY)*scale;}
            triangle(panel,texture,q,new int[]{0,1,2});triangle(panel,texture,q,new int[]{0,2,3});}
        Graphics2D out=sheet.createGraphics();out.drawImage(panel,stage%3*w,stage/3*h,null);out.dispose();
    }
    private static void triangle(BufferedImage out,BufferedImage texture,Quad q,int[] idx){
        double[] x=new double[3],y=new double[3];for(int i=0;i<3;i++){x[i]=q.screen[idx[i]*2];y[i]=q.screen[idx[i]*2+1];}
        double den=(y[1]-y[2])*(x[0]-x[2])+(x[2]-x[1])*(y[0]-y[2]);if(Math.abs(den)<1e-8)return;
        int left=Math.max(0,(int)Math.floor(Math.min(x[0],Math.min(x[1],x[2])))),right=Math.min(out.getWidth()-1,(int)Math.ceil(Math.max(x[0],Math.max(x[1],x[2]))));
        int top=Math.max(0,(int)Math.floor(Math.min(y[0],Math.min(y[1],y[2])))),bottom=Math.min(out.getHeight()-1,(int)Math.ceil(Math.max(y[0],Math.max(y[1],y[2]))));
        for(int py=top;py<=bottom;py++)for(int px=left;px<=right;px++){
            double a=((y[1]-y[2])*(px+.5-x[2])+(x[2]-x[1])*(py+.5-y[2]))/den,b=((y[2]-y[0])*(px+.5-x[2])+(x[0]-x[2])*(py+.5-y[2]))/den,c=1-a-b;
            if(a<0||b<0||c<0)continue;
            double u=a*q.uv[idx[0]*2]+b*q.uv[idx[1]*2]+c*q.uv[idx[2]*2],v=a*q.uv[idx[0]*2+1]+b*q.uv[idx[1]*2+1]+c*q.uv[idx[2]*2+1];
            int color=texture.getRGB(Math.max(0,Math.min(texture.getWidth()-1,(int)(u*texture.getWidth()))),Math.max(0,Math.min(texture.getHeight()-1,(int)(v*texture.getHeight()))));
            double alpha=(color>>>24)/255d*.82;if(alpha<.02)continue;int old=out.getRGB(px,py),rgb=0;
            for(int shift=0;shift<=16;shift+=8)rgb|=(int)(((color>>>shift)&255)*alpha+((old>>>shift)&255)*(1-alpha))<<shift;
            out.setRGB(px,py,rgb);
        }
    }
}
