package net.narutomod;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.minecraft.util.math.Vec3d;
import net.narutomod.client.JutsuVfxGeometry;
import net.narutomod.client.LightningBindVisual;

/** CPU-only safety checks and a labelled preview using the exact production vertices. */
public final class LightningVfxChecks {
    private static int checks;
    private static final Vec3d CENTER = new Vec3d(128, 64, -256);
    private static void check(boolean condition, String reason) {
        ++checks;
        if (!condition) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        packetChecks();
        scalingChecks();
        int maximum = 0;
        for (int detail : new int[] {0, 1}) {
            for (float age : new float[] {-1, 0, .1f, 3, 5, 7.99f, 8, 12, 50, 73, 80, 84.9f, 85, 100}) {
                Probe probe = new Probe(CENTER);
                LightningBindVisual.solids(probe, CENTER, 4.5f, age, 85);
                int solidCount = probe.count;
                LightningBindVisual.energy(probe, CENTER, CENTER.addVector(0, 1, 0), 4.5f, age, 85, 87654, detail);
                check(probe.count < 15000, "bounded effect geometry");
                check(probe.count % 4 == 0 && solidCount % 4 == 0, "complete quads");
                if (age <= 0 || age >= 85) check(probe.count == 0, "invisible outside effect lifetime");
                maximum = Math.max(maximum, probe.count);
            }
        }
        Probe full = new Probe(CENTER), reduced = new Probe(CENTER);
        LightningBindVisual.energy(full, CENTER, null, 4.5f, 12, 85, 41, 1);
        LightningBindVisual.energy(reduced, CENTER, null, 4.5f, 12, 85, 41, 0);
        check(reduced.count < full.count * .80, "minimal particles reduces energy geometry substantially");
        Probe early = new Probe(CENTER), emerged = new Probe(CENTER), fading = new Probe(CENTER);
        LightningBindVisual.solids(early, CENTER, 4.5f, 3, 85);
        LightningBindVisual.solids(emerged, CENTER, 4.5f, 12, 85);
        LightningBindVisual.solids(fading, CENTER, 4.5f, 83, 85);
        check(early.maxY < emerged.maxY - 2, "pillars visibly rise instead of instantly appearing");
        check(fading.maxY < emerged.maxY - .5, "pillars sink at end");
        check(fading.maxAlpha < .1, "fade reaches low opacity before expiry");
        check(Math.abs(emerged.maxY - CENTER.y - 4.5) < .001, "authored height matches bind height");
        for (float height : new float[] {-100, 100, Float.NaN, Float.POSITIVE_INFINITY}) {
            Probe extreme = new Probe(CENTER);
            LightningBindVisual.solids(extreme, CENTER, height, 12, 85);
            LightningBindVisual.energy(extreme, CENTER, CENTER.addVector(1e5, 1e5, -1e5), height, 12, 85, 52, 1);
            check(extreme.count < 15000, "malformed height cannot multiply geometry");
        }
        BufferedImage texture = stoneTexture();
        BufferedImage sheet = new BufferedImage(1440, 590, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(19, 26, 34)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        float[] ages = {3, 12, 50};
        String[] labels = {"01  /  Emergence - tick 3", "02  /  Bind engages - tick 12", "03  /  Sustained cage - tick 50"};
        for (int i = 0; i < ages.length; i++) {
            Scene scene = new Scene(texture);
            scene.environment();
            LightningBindVisual.solids(scene, Vec3d.ZERO, 4.5f, ages[i], 85);
            scene.additive = true;
            LightningBindVisual.energy(scene, Vec3d.ZERO, new Vec3d(0, 1.1, 0), 4.5f, ages[i], 85, 87654, 1);
            g.drawImage(scene.image, 480 * i, 54, null);
            g.setColor(new Color(221, 233, 244));g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString(labels[i], 480 * i + 24, 550);
        }
        g.setColor(new Color(232, 242, 249));g.setFont(new Font("SansSerif", Font.BOLD, 19));
        g.drawString("FOUR-PILLAR BIND  /  OFFLINE GEOMETRY PREVIEW", 24, 29);
        g.setColor(new Color(155, 175, 193));g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Production stone / energy geometry | CPU rasterizer | neutral player stand-in | not an in-game screenshot", 24, 574);
        g.dispose();
        Path preview = Paths.get("build/reports/lightning/bind-preview.png");
        Files.createDirectories(preview.getParent());ImageIO.write(sheet, "png", preview.toFile());
        System.out.println("PASS: " + checks + " lightning checks; maximum " + maximum + " vertices. Preview: " + preview);
    }

    private static void scalingChecks() {
        check(FourPillarPolicy.percent(1f)==0&&FourPillarPolicy.percent(1.5f)==50
            &&FourPillarPolicy.percent(2f)==100,"power maps to visible 0-100 charge");
        check(FourPillarPolicy.duration(1f)==60&&FourPillarPolicy.duration(2f)==110,"bind length follows charge");
        check(Math.abs(FourPillarPolicy.damage(0,0,1f)-6)<.001,"uncharged novice base damage");
        check(Math.abs(FourPillarPolicy.damage(100000,1,2f)-30.375)<.001,"fully charged trained damage cap");
        check(FourPillarPolicy.damage(0,0,2f)>FourPillarPolicy.damage(0,0,1f),"charge raises damage");
        check(FourPillarPolicy.damage(100000,0,2f)>FourPillarPolicy.damage(0,0,2f),"Ninja XP raises damage");
        check(FourPillarPolicy.damage(100000,1,2f)>FourPillarPolicy.damage(100000,0,2f),"mastery raises damage");
        check(Float.isFinite(FourPillarPolicy.damage(Double.NaN,Float.NaN,Float.NaN)),"invalid values bounded");
        check(FourPillarPolicy.height(1.8)>1.8&&FourPillarPolicy.radius(.6)>1,"player cage exceeds player");
        check(FourPillarPolicy.height(40)>40&&FourPillarPolicy.radius(30)>15,"summon cage exceeds large body");
        check(FourPillarPolicy.height(Double.POSITIVE_INFINITY)<=96&&FourPillarPolicy.radius(Double.NaN)<=64,"invalid hitboxes bounded");
        double[] bounds={Double.POSITIVE_INFINITY,-Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,-Double.POSITIVE_INFINITY};
        int[] vertices={0};
        LightningBindVisual.solids((x,y,z,u,v,rgb,a)->{
            check(Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&Double.isFinite(u)&&Double.isFinite(v),"large cage finite vertex");
            bounds[0]=Math.min(bounds[0],x);bounds[1]=Math.max(bounds[1],x);
            bounds[2]=Math.min(bounds[2],y);bounds[3]=Math.max(bounds[3],y);vertices[0]++;
        },CENTER,65,28,12,110);
        check(vertices[0]>0&&vertices[0]<15000,"large target geometry cost bounded");
        check(bounds[0]<CENTER.x-28&&bounds[1]>CENTER.x+28,"large target pillars surround horizontal hitbox");
        check(Math.abs(bounds[3]-CENTER.y-65)<.01,"large target pillars reach intended height");
        int[] arcs={0};
        LightningBindVisual.energy((x,y,z,rgb,a)->{
            check(Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z),"large arc finite vertex");arcs[0]++;
        },CENTER,CENTER.addVector(5,35,4),65,28,12,110,12345,1);
        check(arcs[0]>0&&arcs[0]<15000,"large target arcs remain bounded");
    }

    private static void packetChecks() {
        JutsuVisualEffects.Message message = new JutsuVisualEffects.Message();
        message.kind = JutsuVisualEffects.LIGHTNING_BIND;message.source = 42;
        message.scale = 65f;message.life = 100;message.dimension = -1;message.element=28000;
        message.color = 0x73CFFF;message.seed = 9876;
        check(message.valid(), "bind message accepted");
        ByteBuf buffer = Unpooled.buffer();
        try {
            message.toBytes(buffer);
            JutsuVisualEffects.Message decoded = new JutsuVisualEffects.Message();decoded.fromBytes(buffer);
            check(buffer.readableBytes() == 0, "bind packet layout consumes complete message");
            check(decoded.valid() && decoded.source == 42 && decoded.kind == JutsuVisualEffects.LIGHTNING_BIND
                && decoded.scale == 65f && decoded.element == 28000 && decoded.life == 100 && decoded.dimension == -1
                && decoded.color == 0x73CFFF && decoded.seed == 9876, "bind packet roundtrip preserves visual state");
        } finally { buffer.release(); }
        message.scale = Float.NaN;check(!message.valid(), "NaN bind height rejected");
        message.scale = 96.1f;check(!message.valid(), "oversize cage rejected");
        message.scale = 65f;message.element=64001;check(!message.valid(), "oversize radius rejected");
        message.element=28000;message.life = 19;check(!message.valid(), "too-short staged bind rejected");
        message.life = 100;message.from = new Vec3d(Double.NaN,0,0);check(!message.valid(), "malformed origin rejected");
        message.from = Vec3d.ZERO;message.kind = 255;check(!message.valid(), "unknown VFX kind rejected");
        message.kind = JutsuVisualEffects.BOLT;message.scale = .04f;message.life = 12;
        message.to = new Vec3d(0,3,4);check(message.valid(), "existing lightning bolt remains accepted");
    }

    private static final class Probe implements LightningBindVisual.TexturedSink, JutsuVfxGeometry.Sink {
        final Vec3d center;int count;double maxY = -Double.MAX_VALUE;float maxAlpha;
        Probe(Vec3d center) { this.center = center; }
        public void vertex(double x, double y, double z, int rgb, float alpha) {
            check(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z), "finite vertices");
            check(Float.isFinite(alpha) && alpha >= 0 && alpha <= 1, "bounded opacity");
            check(Math.abs(x - center.x) < 3.25 && Math.abs(z - center.z) < 3.25, "cage stays local");
            check(y >= center.y - .7 && y < center.y + 6.6, "bounded vertical geometry");
            ++count;maxY = Math.max(maxY, y);maxAlpha = Math.max(maxAlpha, alpha);
        }
        public void vertex(double x, double y, double z, double u, double v, int rgb, float alpha) {
            check(Double.isFinite(u) && Double.isFinite(v), "finite UVs");
            check(y >= center.y, "emergence clips stone at ground surface");
            vertex(x, y, z, rgb, alpha);
        }
    }

    private static BufferedImage stoneTexture() throws Exception {
        String resource = "assets/minecraft/textures/blocks/stone.png";
        try (InputStream in = LightningVfxChecks.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) return ImageIO.read(in);
        }
        Path client = Paths.get(System.getProperty("user.home"), ".gradle", "caches", "forge_gradle",
            "minecraft_repo", "versions", "1.12.2", "client.jar");
        if (Files.exists(client)) {
            try (ZipFile jar = new ZipFile(client.toFile())) {
                ZipEntry entry = jar.getEntry(resource);
                if (entry != null) try (InputStream in = jar.getInputStream(entry)) { return ImageIO.read(in); }
            }
        }
        throw new IllegalStateException("Offline preview needs the existing Minecraft 1.12.2 stone texture on the runtime classpath or in the Gradle cache.");
    }

    private static final class Vertex {
        double x,y,z,u,v;int rgb;float alpha;
        Vertex(double x,double y,double z,double u,double v,int rgb,float alpha) {
            this.x=x;this.y=y;this.z=z;this.u=u;this.v=v;this.rgb=rgb;this.alpha=alpha;
        }
    }

    /** Orthographic Z-buffer, nearest-neighbour stone sampling and additive energy. */
    private static final class Scene implements LightningBindVisual.TexturedSink, JutsuVfxGeometry.Sink {
        final BufferedImage image = new BufferedImage(480,480,BufferedImage.TYPE_INT_RGB);
        final BufferedImage texture;
        final double[] depths = new double[480*480];
        final List<Vertex> quad = new ArrayList<Vertex>(4);
        boolean additive, textured;
        Scene(BufferedImage texture) {
            this.texture=texture;Arrays.fill(depths,-Double.MAX_VALUE);
            for(int y=0;y<480;y++)for(int x=0;x<480;x++) {
                double t=y/480.0;int r=(int)(30+18*t), g=(int)(45+16*t), b=(int)(58+10*t);
                image.setRGB(x,y,(r<<16)|(g<<8)|b);
            }
        }
        public void vertex(double x,double y,double z,int rgb,float alpha) {
            textured=false;accept(x,y,z,0,0,rgb,alpha);
        }
        public void vertex(double x,double y,double z,double u,double v,int rgb,float alpha) {
            textured=true;accept(x,y,z,u,v,rgb,alpha);
        }
        void accept(double x,double y,double z,double u,double v,int rgb,float alpha) {
            double horizontal=x*.819-z*.574, depth=x*.574+z*.819;
            quad.add(new Vertex(240+horizontal*52,333-(y*.906-depth*.423)*52,
                depth*.906+y*.423,u,v,rgb,alpha));
            if(quad.size()==4){triangle(quad.get(0),quad.get(1),quad.get(2));triangle(quad.get(0),quad.get(2),quad.get(3));quad.clear();}
        }
        void environment() {
            for(int x=-5;x<5;x++)for(int z=-5;z<5;z++) {
                int c=((x+z)&1)==0?0x587346:0x536F42;
                flat(x,0,z, x+1,0,z, x+1,0,z+1, x,0,z+1,c);
            }
            cuboid(-.24,0,-.14,.24,.70,.14,0x8A949D);
            cuboid(-.29,.70,-.16,.29,1.34,.16,0xAFB6BB);
            cuboid(-.42,.72,-.13,-.29,1.30,.13,0x929BA4);
            cuboid(.29,.72,-.13,.42,1.30,.13,0x929BA4);
            cuboid(-.24,1.34,-.24,.24,1.82,.24,0xCDD0CD);
        }
        void cuboid(double x0,double y0,double z0,double x1,double y1,double z1,int color) {
            flat(x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,darken(color,.7));
            flat(x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1,darken(color,.86));
            flat(x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0,darken(color,.75));
            flat(x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,color);
            flat(x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,darken(color,1.12));
        }
        void flat(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,int c) {
            vertex(ax,ay,az,c,1);vertex(bx,by,bz,c,1);vertex(cx,cy,cz,c,1);vertex(dx,dy,dz,c,1);
        }
        int darken(int c,double n) {
            return (Math.min(255,(int)(((c>>16)&255)*n))<<16)|(Math.min(255,(int)(((c>>8)&255)*n))<<8)|Math.min(255,(int)((c&255)*n));
        }
        void triangle(Vertex a,Vertex b,Vertex c) {
            double denominator=(b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y);
            if(Math.abs(denominator)<.000001)return;
            int left=Math.max(0,(int)Math.floor(Math.min(a.x,Math.min(b.x,c.x))));
            int right=Math.min(479,(int)Math.ceil(Math.max(a.x,Math.max(b.x,c.x))));
            int top=Math.max(0,(int)Math.floor(Math.min(a.y,Math.min(b.y,c.y))));
            int bottom=Math.min(479,(int)Math.ceil(Math.max(a.y,Math.max(b.y,c.y))));
            for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++) {
                double p=((b.y-c.y)*(x+.5-c.x)+(c.x-b.x)*(y+.5-c.y))/denominator;
                double q=((c.y-a.y)*(x+.5-c.x)+(a.x-c.x)*(y+.5-c.y))/denominator, r=1-p-q;
                if(p<0||q<0||r<0)continue;
                double depth=p*a.z+q*b.z+r*c.z;
                if(depth<depths[y*480+x]-.002)continue;
                double alpha=p*a.alpha+q*b.alpha+r*c.alpha;
                double light=1;
                if(textured) {
                    double u=p*a.u+q*b.u+r*c.u,v=p*a.v+q*b.v+r*c.v;
                    int tx=(int)((u-Math.floor(u))*texture.getWidth()),ty=(int)((v-Math.floor(v))*texture.getHeight());
                    light=(texture.getRGB(tx,ty)&255)/255.0;
                }
                int previous=image.getRGB(x,y),rgb=0;
                for(int shift=0;shift<=16;shift+=8) {
                    double value=(p*((a.rgb>>shift)&255)+q*((b.rgb>>shift)&255)+r*((c.rgb>>shift)&255))*light;
                    double old=(previous>>shift)&255;
                    int channel=(int)Math.min(255,additive?old+value*alpha:old*(1-alpha)+value*alpha);
                    rgb|=channel<<shift;
                }
                image.setRGB(x,y,rgb);
                if(!additive)depths[y*480+x]=depth;
            }
        }
    }
}
