package net.narutomod.client;

import java.util.*;

/** Deterministic miniature stages. Coordinates are blocks; animation time is server ticks, not frames. */
public final class GenjutsuScene {
    public static final class Quad {
        public final double[] xyz, uv; public final int color; public final String texture;
        Quad(double[] vertices,double[] tex,int tint,String material) { xyz=vertices;uv=tex;color=tint;texture=material; }
    }
    public static final class Actor {
        public final double x,y,z,yaw,scale; public final boolean victim; public final float pose;
        Actor(double xx,double yy,double zz,double ry,double size,boolean v,float p) { x=xx;y=yy;z=zz;yaw=ry;scale=size;victim=v;pose=p; }
    }
    public final List<Quad> quads=new ArrayList<>();
    public final List<Actor> actors=new ArrayList<>();
    public final int type; public final float time;
    public final double cameraX,cameraY,cameraZ,lookY;
    private static final double[] UV={0,0,0,1,1,1,1,0};
    public GenjutsuScene(int kind,float age) {
        type=kind;time=age;
        cameraX=kind==3?Math.sin(age*.009)*.55:Math.sin(age*.012)*.16;
        cameraY=kind==2?1.05:1.65; cameraZ=kind==4?5.4-GenjutsuVisuals.smooth((age-18)/48)*1.3:6.2;
        lookY=kind==2?2.3:1.35;
        if(kind==0) {
            for(int i=-1;i<=1;i+=2)actors.add(new Actor(i*(1.6+.25*Math.sin(age*.06)),0,0,-i*15,1,false,(float)Math.sin(age*.13)*.18f));
            return;
        }
        floor(kind==1?0xFF302245:kind==4?0xFF261415:0xFF43141C);
        if(kind==1)memory();else if(kind==2)murder();else if(kind==3)execution();else coffin();
    }
    public static float stab(float time) {
        float phase=(time-32)%32;
        if(time<32)return 0;
        return GenjutsuVisuals.smooth(phase/7)*(1-GenjutsuVisuals.smooth((phase-10)/14));
    }
    public static float impact(float time) {
        if(time<39)return 0;
        return 1-GenjutsuVisuals.smooth(((time-39)%32)/6);
    }
    public static float lid(float time) { return GenjutsuVisuals.smooth((time-18)/30); }
    private void floor(int tint) {
        for(int x=-6;x<6;x++)for(int z=-9;z<3;z++)box(x*2,-.13,z*2,x*2+2,0,z*2+2,tint,"stone");
    }
    private void memory() {
        for(int row=0;row<6;row++) {
            double z=-row*2.7;
            for(int side=-1;side<=1;side+=2) {
                box(side*3.5-.14,0,z,side*3.5+.14,4.7,z+.3,0xFF30243F,"wood");
                box(side*3.5-.23,3.4,z-.1,side*3.5+.23,3.6,z+.4,0xFFCB93DC,null);
                actors.add(new Actor(side*(1.25+Math.sin(time*.055+row)*.32),0,z+Math.sin(time*.035+row)*.22,side*(8+row*4),1,false,(float)Math.sin(time*.13-row)*.30f));
            }
            box(-3.8,4.5,z,3.8,4.8,z+.3,0xFF5F3E68,"wood");
        }
        eye(0,3.2,-17,2.6,time*.35,0xFFBF6DDA);
        for(int i=0;i<28;i++) {
            double x=Math.sin(i*9.3+time*.012)*3.8,y=(i*.37+time*.006)%4.7,z=-1-(i%6)*2.4;
            double size=.04+(i%3)*.025;
            face(new double[]{x,y+size,z,x-size,y,z,x,y-size,z,x+size,y,z},0xFF9E78B4,null);
        }
    }
    private void murder() {
        eye(0,4.8,-8,3.8,time*.6,0xFFB61D35);
        actors.add(new Actor(0,0,-1.5,0,2.1,false,0));
        for(int side=-1;side<=1;side+=2)for(int i=0;i<6;i++) {
            double x=side*(1.1+i*.58),z=1-i*.6,height=.5+i*.33+.09*Math.sin(time*.04+i);
            face(new double[]{x-.2,0,z,x+.2,0,z,x*.8,height,z-.3,x*.8,height,z-.3},0xFF100913,null);
        }
    }
    private void cross(double x,double z,double size,int tint) {
        box(x-.12*size,0,z-.13,x+.12*size,2.75*size,z+.13,tint,"wood");
        box(x-1.0*size,1.43*size,z-.14,x+1.0*size,1.63*size,z+.14,tint,"wood");
        box(x-.035*size,.1,z+.135,x+.025*size,2.67*size,z+.15,0xFF7F2631,null);
    }
    private void execution() {
        eye(-.9,5.2,-13,3.3,time*.4,0xFFD13846);
        for(int row=0;row<4;row++)for(int col=-3;col<=3;col++) {
            if(row==0 && col==0)continue;
            cross(col*2.8,-2.3-row*3.3,1.1,0xFF2A111D);
        }
        cross(0,-.28,1,0xFF503035);
        actors.add(new Actor(0,.15,0,0,1,true,1));
        for(int side=-1;side<=1;side+=2) {
            float p=stab(time-(side==1?0:12));
            actors.add(new Actor(side*(1.9-p*.48),0,.05,-side*72,1,false,-p));
            for(int row=0;row<3;row++) {
                double reach=side*(1.8-p*.9),y=.8+row*.36;
                sword(reach,y,.4,side,p);
            }
            box(side*.82-.08,1.44,.16,side*.82+.08,1.64,.30,0xFFADA18B,"wood");
        }
    }
    private void sword(double x,double y,double z,int side,float thrust) {
        double tip=x-side*.90;
        face(new double[]{x,y-.055,z,x,y+.055,z,tip,y+.015,z,tip,y-.015,z},0xFFBFC8D1,null);
        face(new double[]{x,y+.055,z+.006,tip,y+.015,z+.006,tip-side*.14,y,z+.006,x,y,z+.006},0xFFF2EEEE,null);
        box(x-.025,y-.14,z-.035,x+.025,y+.14,z+.035,0xFF8F7660,null);
        box(Math.min(x,x+side*.26),y-.047,z-.04,Math.max(x,x+side*.26),y+.047,z+.04,0xFF231925,"wood");
    }
    private void coffin() {
        for(int i=0;i<7;i++) {
            double x=-.77+i*.22;
            box(x,0,-.39,x+.21,2.75,-.24,0xFF796052,"wood");
        }
        box(-.92,0,-.45,-.76,2.75,.44,0xFF8C6852,"wood");
        box(.76,0,-.45,.92,2.75,.44,0xFF8C6852,"wood");
        box(-.92,2.62,-.45,.92,2.81,.44,0xFFAF8664,"wood");
        box(-.92,-.01,-.45,.92,.16,.44,0xFFAF8664,"wood");
        actors.add(new Actor(0,.14,0,0,1,true,0));
        actors.add(new Actor(-1.7,0,0,28,1,false,-.55f));
        double close=lid(time),angle=(1-close)*Math.PI*.58;
        // Door hinged on the left: true rotation, not sliding screen rectangles.
        double hinge=-.90;
        for(int i=0;i<6;i++) {
            double a=i*.30,b=a+.285;
            face(new double[]{hinge+a*Math.cos(angle),.09,.5+a*Math.sin(angle),hinge+a*Math.cos(angle),2.69,.5+a*Math.sin(angle),
                hinge+b*Math.cos(angle),2.69,.5+b*Math.sin(angle),hinge+b*Math.cos(angle),.09,.5+b*Math.sin(angle)},0xFF7A5140,"wood");
        }
        for(int band=0;band<3;band++) {
            double y=.32+band*1.03;
            face(new double[]{hinge,y,.515,hinge,y+.10,.515,hinge+1.8*Math.cos(angle),y+.10,.515+1.8*Math.sin(angle),hinge+1.8*Math.cos(angle),y,.515+1.8*Math.sin(angle)},0xFF2B2730,null);
        }
        float burn=GenjutsuVisuals.smooth((time-40)/25);
        for(int i=0;i<22;i++) {
            double x=-1.12+(i%11)*.225,z=i<11?.62:-.1;
            double h=burn*(.35+(.5+.5*Math.sin(time*.18+i*2.1))*1.35);
            flame(x,0,z,.19,h,time+i);
        }
        for(int i=0;i<24;i++) {
            double t=(time*.023+i*.137)%1,x=Math.sin(i*31+time*.014)*1.4,y=t*3.8;
            box(x,y,.65,x+.022,y+.055,.675,0xFFFFAD55,null);
        }
    }
    private void flame(double x,double y,double z,double w,double h,double time) {
        double lean=Math.sin(time*.13)*w*.7;
        // Reuse the mod's actual 32-frame Minecraft fire atlas. Frame changes are tick-driven.
        double frame=Math.floorMod((int)time,32),v=frame/32,vv=(frame+1)/32;
        quads.add(new Quad(new double[]{x-w+lean,y+h,z,x-w,y,z,x+w,y,z,x+w+lean,y+h,z},
            new double[]{0,v,0,vv,1,vv,1,v},0xFFFFFFFF,"fire"));
    }
    private void eye(double x,double y,double z,double radius,double angle,int red) {
        disc(x,y,z,radius,0xFF100C19);
        disc(x,y,z+.005,radius*.95,red);
        disc(x,y,z+.01,radius*.69,0xFF25101B);
        disc(x,y,z+.015,radius*.67,red);
        disc(x,y,z+.02,radius*.22,0xFF160E18);
        for(int i=0;i<3;i++) {
            double a=i*Math.PI*2/3+Math.toRadians(angle),cx=x+Math.cos(a)*radius*.57,cy=y+Math.sin(a)*radius*.57;
            disc(cx,cy,z+.025,radius*.12,0xFF160E18);
            double tx=x+Math.cos(a+.40)*radius*.77,ty=y+Math.sin(a+.40)*radius*.77;
            face(new double[]{cx-Math.sin(a)*radius*.1,cy+Math.cos(a)*radius*.1,z+.03,cx+Math.sin(a)*radius*.1,cy-Math.cos(a)*radius*.1,z+.03,tx,ty,z+.03,tx,ty,z+.03},0xFF160E18,null);
        }
    }
    private void disc(double x,double y,double z,double r,int color) {
        for(int i=0;i<80;i++) {
            double a=i*Math.PI/40,b=(i+1)*Math.PI/40;
            face(new double[]{x,y,z,x+Math.cos(a)*r,y+Math.sin(a)*r,z,x+Math.cos(b)*r,y+Math.sin(b)*r,z,x,y,z},color,null);
        }
    }
    private void face(double[] points,int color,String texture) { quads.add(new Quad(points,UV,color,texture)); }
    private void box(double x,double y,double z,double xx,double yy,double zz,int color,String material) {
        face(new double[]{x,yy,zz,x,y,zz,xx,y,zz,xx,yy,zz},color,material);
        face(new double[]{xx,yy,z,xx,y,z,x,y,z,x,yy,z},shade(color,.72),material);
        face(new double[]{x,yy,z,x,y,z,x,y,zz,x,yy,zz},shade(color,.8),material);
        face(new double[]{xx,yy,zz,xx,y,zz,xx,y,z,xx,yy,z},shade(color,.87),material);
        face(new double[]{x,yy,z,x,yy,zz,xx,yy,zz,xx,yy,z},shade(color,1.1),material);
    }
    public static int shade(int c,double amount) {
        return (c&0xff000000)|Math.min(255,(int)(((c>>16)&255)*amount))<<16|Math.min(255,(int)(((c>>8)&255)*amount))<<8|Math.min(255,(int)((c&255)*amount));
    }
}
