package net.narutomod.client;

import java.util.Random;
import net.minecraft.util.math.Vec3d;

/** Bounded geometry shared by the production renderer and its offscreen regression fixture. */
public final class JutsuVfxGeometry {
    private JutsuVfxGeometry(){}
    public interface Sink { void vertex(double x,double y,double z,int rgb,float alpha); }
    public static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    public static float envelope(float age,float life){return Math.max(0,1-smooth((age-life*.55f)/Math.max(1,life*.45f)));}
    public static Vec3d[] path(Vec3d from,Vec3d to,long seed,int detail){
        double length=from.distanceTo(to);int steps=Math.max(3,Math.min(detail>0?24:12,(int)(length*2.4)+3));
        Vec3d axis=to.subtract(from).normalize(),right=axis.crossProduct(Math.abs(axis.y)>.85?new Vec3d(1,0,0):new Vec3d(0,1,0)).normalize();
        Vec3d up=axis.crossProduct(right).normalize();Vec3d[] points=new Vec3d[steps+1];Random random=new Random(seed);
        for(int i=0;i<=steps;i++){
            double t=i/(double)steps,spread=(i==0||i==steps)?0:Math.min(.44,length*.045)*(0.55+Math.sin(t*Math.PI)*.45);
            points[i]=from.add(to.subtract(from).scale(t)).add(right.scale((random.nextDouble()*2-1)*spread)).add(up.scale((random.nextDouble()*2-1)*spread));
        }return points;
    }
    public static void segment(Sink out,Vec3d a,Vec3d b,double width,int rgb,float alpha){
        if(alpha<=.001||width<=0||a.squareDistanceTo(b)<1.0e-12)return;
        Vec3d axis=b.subtract(a).normalize(),side=axis.crossProduct(Math.abs(axis.y)>.8?new Vec3d(1,0,0):new Vec3d(0,1,0)).normalize().scale(width);
        Vec3d other=axis.crossProduct(side);quad(out,a.subtract(side),b.subtract(side),b.add(side),a.add(side),rgb,alpha);
        quad(out,a.subtract(other),b.subtract(other),b.add(other),a.add(other),rgb,alpha);
    }
    public static void bolt(Sink out,Vec3d from,Vec3d to,float width,int color,float alpha,long seed,int detail){
        Vec3d[] points=path(from,to,seed,detail);
        for(int layer=0;layer<3;layer++){
            float w=width*(layer==0?3.8f:layer==1?1.8f:.52f),a=alpha*(layer==0?.075f:layer==1?.31f:.95f);
            int c=layer==2?0xEFFCFF:color;
            for(int i=0;i<points.length-1;i++)segment(out,points[i],points[i+1],w,c,a);
        }
        if(detail>0&&points.length>5){Random r=new Random(seed^0x7AFBE);
            for(int i=2;i<points.length-2;i+=5){Vec3d p=points[i],q=p.add(to.subtract(from).normalize().scale(.25+r.nextDouble()*.55)).addVector(r.nextDouble()-.5,r.nextDouble()-.5,r.nextDouble()-.5);
                Vec3d knee=p.add(q.subtract(p).scale(.55)).addVector(.12,-.1,.08);
                segment(out,p,knee,width*.55,color,alpha*.55f);segment(out,knee,q,width*.25,0xDDF8FF,alpha*.65f);}
        }
    }
    public static void ring(Sink out,Vec3d center,float radius,float width,int color,float alpha,int segments){
        int n=Math.max(12,Math.min(64,segments));for(int i=0;i<n;i++){
            double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;
            Vec3d p=center.addVector(Math.cos(a)*(radius-width),0,Math.sin(a)*(radius-width));
            Vec3d q=center.addVector(Math.cos(b)*(radius-width),0,Math.sin(b)*(radius-width));
            Vec3d r=center.addVector(Math.cos(b)*(radius+width),0,Math.sin(b)*(radius+width));
            Vec3d s=center.addVector(Math.cos(a)*(radius+width),0,Math.sin(a)*(radius+width));quad(out,p,q,r,s,color,alpha);
        }
    }
    public static void burst(Sink out,Vec3d center,float radius,float age,float life,int color,long seed,int detail){
        float t=Math.min(1,age/life),alpha=(1-t)*(1-t);Random r=new Random(seed);
        int count=detail>0?18:9;
        for(int i=0;i<count;i++){
            Vec3d dir=new Vec3d(r.nextDouble()*2-1,r.nextDouble()*1.6-.45,r.nextDouble()*2-1).normalize();
            double reach=radius*(.35+t*1.9)*(0.6+r.nextDouble()*.7);
            Vec3d head=center.add(dir.scale(reach)).addVector(0,-t*t*.45,0),tail=head.subtract(dir.scale(radius*(.10+.20*(1-t))));
            segment(out,tail,head,.013+radius*.014*(1-t),i%3==0?0xF8FCFF:color,alpha);
        }
        if(age<4){float a=(1-age/4)*.35f;segment(out,center.addVector(-radius*.22,0,0),center.addVector(radius*.22,0,0),radius*.07,0xFFFFFF,a);
            segment(out,center.addVector(0,-radius*.22,0),center.addVector(0,radius*.22,0),radius*.07,0xFFFFFF,a);}
    }
    public static void ribbon(Sink out,Vec3d a,Vec3d b,float width,float age,float life,int color,int strands){
        Vec3d axis=b.subtract(a),side=axis.normalize().crossProduct(Math.abs(axis.normalize().y)>.85?new Vec3d(1,0,0):new Vec3d(0,1,0)).normalize();
        Vec3d up=axis.normalize().crossProduct(side);float alpha=envelope(age,life);
        for(int strand=0;strand<strands;strand++){
            Vec3d previous=a;int n=24;
            for(int i=1;i<=n;i++){double t=i/(double)n,angle=t*Math.PI*3-age*.25+strand*Math.PI*2/strands;
                double spread=Math.sin(t*Math.PI)*width*1.7;
                Vec3d p=a.add(axis.scale(t)).add(side.scale(Math.cos(angle)*spread)).add(up.scale(Math.sin(angle)*spread));
                segment(out,previous,p,width*(strand==0?.4:.18),strand==0?color:0xE8FFFF,alpha*(strand==0?.42f:.7f));previous=p;}
        }
    }
    public static void quad(Sink out,Vec3d a,Vec3d b,Vec3d c,Vec3d d,int rgb,float alpha){v(out,a,rgb,alpha);v(out,b,rgb,alpha);v(out,c,rgb,alpha);v(out,d,rgb,alpha);}
    private static void v(Sink out,Vec3d p,int rgb,float a){out.vertex(p.x,p.y,p.z,rgb,Math.max(0,Math.min(1,a)));}
}
