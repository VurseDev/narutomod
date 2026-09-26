package net.narutomod.client;

/** Resolution-independent presentation geometry shared by the game and offline visual checks. */
public final class GenjutsuVisuals {
    private GenjutsuVisuals() { }
    public interface Painter { void triangle(double[] xy, int[] colors); }
    public static float envelope(float elapsed, float remaining) { return smooth(elapsed/12f)*smooth(remaining/20f); }
    public static float smooth(float x) { x=Math.max(0,Math.min(1,x)); return x*x*(3-2*x); }
    public static void drawCast(Painter p,int type,float time,float strength) {
        sharingan(p,.5,.46,.065-.02*smooth(time/30),strength*(1-smooth((time-17)/15))*.8,time*.045);
    }
    private static int color(int rgb,double alpha) { return ((int)(Math.max(0,Math.min(1,alpha))*255)<<24)|(rgb&0xffffff); }
    private static void polygon(Painter p,int color,double... xy) {
        for(int i=2;i<xy.length-2;i+=2) p.triangle(new double[]{xy[0],xy[1],xy[i],xy[i+1],xy[i+2],xy[i+3]},new int[]{color,color,color});
    }
    private static void line(Painter p,double x,double y,double xx,double yy,double width,int color) {
        double len=Math.hypot(xx-x,yy-y); if(len<.000001)return;
        double dx=-(yy-y)/len*width,dy=(xx-x)/len*width;
        polygon(p,color,x+dx,y+dy,xx+dx,yy+dy,xx-dx,yy-dy,x-dx,y-dy);
    }
    private static void oval(Painter p,double x,double y,double rx,double ry,int c) {
        double[] xy=new double[48];
        for(int i=0;i<24;i++){double a=i*Math.PI/12;xy[i*2]=x+Math.cos(a)*rx;xy[i*2+1]=y+Math.sin(a)*ry;}
        polygon(p,c,xy);
    }
    private static void vignette(Painter p,double strength,double breath) {
        for(int band=0;band<4;band++) for(int i=0;i<48;i++) {
            double r=band/4d,rr=(band+1)/4d,a=i*Math.PI/24,b=(i+1)*Math.PI/24;
            double x=.5+Math.cos(a)*(.22+r*.53),y=.48+Math.sin(a)*(.22+r*.63);
            double xx=.5+Math.cos(b)*(.22+r*.53),yy=.48+Math.sin(b)*(.22+r*.63);
            double ox=.5+Math.cos(a)*(.22+rr*.53),oy=.48+Math.sin(a)*(.22+rr*.63);
            double oxx=.5+Math.cos(b)*(.22+rr*.53),oyy=.48+Math.sin(b)*(.22+rr*.63);
            int inner=color(0x050307,r*r*strength*breath),outer=color(0x030205,rr*rr*strength*breath);
            p.triangle(new double[]{x,y,xx,yy,oxx,oyy},new int[]{inner,inner,outer});
            p.triangle(new double[]{x,y,oxx,oyy,ox,oy},new int[]{inner,outer,outer});
        }
    }
    private static void eye(Painter p,double x,double y,double size,double alpha,double time) {
        double blink=.60+.40*Math.pow(Math.sin(time*.025),2);
        polygon(p,color(0x3E111A,alpha),x-size,y,x-size*.48,y-size*.32*blink,x+size*.5,y-size*.24*blink,
            x+size,y,x+size*.48,y+size*.27*blink,x-size*.5,y+size*.24*blink);
        oval(p,x,y,size*.17,size*.25*blink,color(0x070308,alpha));
        oval(p,x-size*.07,y-size*.1,size*.035,size*.035,color(0xA35A59,alpha*.6));
    }
    public static void sharingan(Painter p,double x,double y,double radius,double alpha,double angle) {
        oval(p,x,y,radius,radius*1.777,color(0x130D1C,alpha));
        oval(p,x,y,radius*.94,radius*1.67,color(0xBE243B,alpha));
        oval(p,x,y,radius*.64,radius*1.14,color(0x251020,alpha));
        oval(p,x,y,radius*.61,radius*1.084,color(0xD73749,alpha));
        oval(p,x,y,radius*.22,radius*.391,color(0x100C18,alpha));
        for(int i=0;i<3;i++) {
            double a=i*Math.PI*2/3+angle,cx=x+Math.cos(a)*radius*.57,cy=y+Math.sin(a)*radius*1.014;
            oval(p,cx,cy,radius*.105,radius*.187,color(0x100C18,alpha));
            polygon(p,color(0x100C18,alpha),cx-Math.sin(a)*radius*.1,cy+Math.cos(a)*radius*.178,
                cx+Math.sin(a)*radius*.1,cy-Math.cos(a)*radius*.178,
                x+Math.cos(a+.42)*radius*.75,y+Math.sin(a+.42)*radius*1.33);
        }
    }
    public static void release(Painter p,float strength) {
        polygon(p,color(0xADCCDA,strength*.18),0,0,1,0,1,1,0,1);
        vignette(p,strength*.32,1);
    }
    public static void draw(Painter p,int type,float time,float strength) {
        double s=Math.max(0,Math.min(1,strength));vignette(p,s*(type>=2?.82:.45),1);
        if(type>=2) {
            polygon(p,color(0x080611,s*.91),0,0,1,0,1,.065,0,.065);
            polygon(p,color(0x080611,s*.91),0,.935,1,.935,1,1,0,1);
        }
        if(type==0) {
            polygon(p,color(0x302975,s*.12),0,0,1,0,1,1,0,1);
            for(int i=0;i<9;i++) {
                double y=(i/9d+time*.0013)%1;
                line(p,0,y,1,y+.013*Math.sin(time*.07+i),.001,color(0xBD9DE9,s*.18));
            }
        } else if(type==1) {
            for(int ray=0;ray<11;ray++) {
                double a=ray*Math.PI*2/11+.23,x=.5+Math.cos(a)*.13,y=.47+Math.sin(a)*.16;
                for(int j=0;j<5;j++) {
                    double r=.23+j*.14,nx=.5+Math.cos(a+.10*Math.sin(j*3+ray))*r,ny=.47+Math.sin(a)*r;
                    line(p,x,y,nx,ny,.0009,color(0xE2B9FA,s*.45));
                    if(j==2)line(p,nx,ny,nx+.055*Math.sin(ray),ny-.07,.0006,color(0xE2B9FA,s*.34));
                    x=nx;y=ny;
                }
            }
        } else if(type==2) {
            double tension=smooth(time/28);
            for(int i=0;i<48;i++) {
                double a=i*Math.PI/24,r=.40-.025*Math.sin(i*17),outer=.78;
                line(p,.5+Math.cos(a)*r,.48+Math.sin(a)*r,.5+Math.cos(a)*outer,.48+Math.sin(a)*outer,.001,
                    color(0x190D1A,s*tension*.72));
            }
        } else if(type==3) {
            double impact=GenjutsuScene.impact(time)*s;
            if(impact>0) {
                boolean flip=((int)(time-39)/32)%2==0;
                line(p,flip?.27:.72,.24,flip?.72:.28,.75,.0025,color(0xF5DCD9,impact*.82));
                line(p,flip?.28:.73,.24,flip?.73:.29,.75,.007,color(0xB72338,impact*.42));
            }
        } else if(type==4) {
            polygon(p,color(0xA44610,s*smooth((time-40)/30)*.14),0,0,1,0,1,1,0,1);
        }
        // One ocular reveal; no periodic full-screen inverse/strobe.
        if(type>0 && time<25)sharingan(p,.5,.46,.12+.10*smooth(time/23),s*(1-smooth((time-9)/16)),time*.04);
    }
}
