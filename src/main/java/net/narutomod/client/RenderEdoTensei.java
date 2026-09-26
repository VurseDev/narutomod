package net.narutomod.client;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.entity.EntityEdoTensei;
import net.narutomod.entity.EntityEdoTensei.Sequence;
import org.lwjgl.opengl.GL11;

/** Uses the actual Blockbench per-face UVs, not an auto-UV approximation. */
@SideOnly(Side.CLIENT)
public class RenderEdoTensei extends Render<Sequence> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("narutomod:textures/blocks/edo_tensei_coffin.png");
    private static final ResourceLocation MODEL = new ResourceLocation("narutomod:models/custom/edo_tensei_coffin.json");
    private final List<Face> shell = new ArrayList<>(), door = new ArrayList<>();
    private final List<double[]> ink = new SealGeometry().quads();
    private final Map<Sequence, net.narutomod.entity.EntityEdoReanimation> previews = new WeakHashMap<>();
    private boolean loaded;

    public static void register() { RenderingRegistry.registerEntityRenderingHandler(Sequence.class, RenderEdoTensei::new); }
    public RenderEdoTensei(RenderManager manager) {
        super(manager);
        ((net.minecraft.client.resources.IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager())
            .registerReloadListener(resources -> { loaded = false; shell.clear(); door.clear(); });
    }
    @Override protected ResourceLocation getEntityTexture(Sequence entity) { return TEXTURE; }

    @Override public void doRender(Sequence e, double x, double y, double z, float yaw, float partial) {
        float age = e.age() + partial;
        if (e.showBody()) {
            net.narutomod.entity.EntityEdoReanimation body = previews.computeIfAbsent(e, key -> new net.narutomod.entity.EntityEdoReanimation(key.world));
            if(!body.soul().equals(e.identity()))body.configure(null,e.identity());
            body.setNoAI(true);
            double offset = e.ritual() ? Sequence.sink(age) : .36 + Sequence.rise(age);
            // Walk out before handing off to the real server mob, avoiding a visible teleport.
            double walk = e.ritual() ? 0 : .15 + .90 * Sequence.smooth((age - 100) / 20f);
            double dx = -Math.sin(Math.toRadians(e.rotationYaw)) * walk;
            double dz = Math.cos(Math.toRadians(e.rotationYaw)) * walk;
            if (!e.ritual()) offset -= .26 * Sequence.smooth((age - 100) / 20f);
            body.setPosition(e.posX + dx, e.posY + offset, e.posZ + dz);
            body.rotationYaw = body.prevRotationYaw = body.renderYawOffset = body.prevRenderYawOffset = e.rotationYaw;
            body.rotationYawHead = body.prevRotationYawHead = e.rotationYaw;
            body.ticksExisted = e.age();
            GlStateManager.pushMatrix();
            renderManager.renderEntity(body, x + dx, y + offset, z + dz, e.rotationYaw, partial, false);
            GlStateManager.popMatrix();
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        if (e.ritual()) {
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            float grow = Sequence.smooth(age / 20f);
            GlStateManager.scale(grow, 1, grow);
            float alpha = 1 - Sequence.smooth((age - 96) / 14f);
            BufferBuilder b = Tessellator.getInstance().getBuffer();
            b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (double[] q : ink) for (int i = 0; i < 8; i += 2)
                b.pos(q[i], .018, q[i+1]).color(.025f, .018f, .03f, alpha).endVertex();
            Tessellator.getInstance().draw();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
        } else if (age < 144) {
            loadMesh();
            bindTexture(TEXTURE);
            GlStateManager.translate(0, Sequence.rise(age), 0);
            GlStateManager.rotate(-e.rotationYaw, 0, 1, 0);
            GlStateManager.scale(1.35, 1.35, 2.0);
            GlStateManager.translate(-.5, 0, -.5);
            draw(shell);
            GlStateManager.pushMatrix();
            // Original lid_root pivot [8, 2.5, 13], converted through the block export scale.
            double py = 2.5 * 32 / 38 / 16, pz = (8 + 5.0 * 32 / 38) / 16;
            float opening = Sequence.lid(age);
            GlStateManager.translate(.5, py - opening * .065, pz);
            GlStateManager.scale(1 / 1.35, 1 / 1.35, .5);
            GlStateManager.rotate(90 * opening, 1, 0, 0);
            GlStateManager.scale(1.35, 1.35, 2.0);
            GlStateManager.translate(-.5, -py, -pz);
            draw(door);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private void loadMesh() {
        if (loaded) return;
        loaded = true;
        try (InputStreamReader reader = new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(MODEL).getInputStream(), StandardCharsets.UTF_8)) {
            for (JsonElement element : new JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("elements")) {
                JsonObject box = element.getAsJsonObject();
                String name = box.get("name").getAsString();
                List<Face> target = name.matches("^(lid|trim|channel|plaque|binding|fastener)_.*") ? door : shell;
                double[] a = vector(box.getAsJsonArray("from")), c = vector(box.getAsJsonArray("to"));
                for (Map.Entry<String, JsonElement> entry : box.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = entry.getValue().getAsJsonObject();
                    double[][] points;
                    float light;
                    switch (entry.getKey()) {
                        case "north": points = new double[][]{{c[0],c[1],a[2]},{c[0],a[1],a[2]},{a[0],a[1],a[2]},{a[0],c[1],a[2]}}; light=.78f; break;
                        case "south": points = new double[][]{{a[0],c[1],c[2]},{a[0],a[1],c[2]},{c[0],a[1],c[2]},{c[0],c[1],c[2]}}; light=1f; break;
                        case "east": points = new double[][]{{c[0],c[1],c[2]},{c[0],a[1],c[2]},{c[0],a[1],a[2]},{c[0],c[1],a[2]}}; light=.86f; break;
                        case "west": points = new double[][]{{a[0],c[1],a[2]},{a[0],a[1],a[2]},{a[0],a[1],c[2]},{a[0],c[1],c[2]}}; light=.86f; break;
                        case "up": points = new double[][]{{a[0],c[1],a[2]},{a[0],c[1],c[2]},{c[0],c[1],c[2]},{c[0],c[1],a[2]}}; light=1f; break;
                        default: points = new double[][]{{a[0],a[1],c[2]},{a[0],a[1],a[2]},{c[0],a[1],a[2]},{c[0],a[1],c[2]}}; light=.65f;
                    }
                    target.add(new Face(points, vector(face.getAsJsonArray("uv")), light,
                        face.has("rotation") ? face.get("rotation").getAsInt() / 90 : 0));
                }
            }
        } catch (Exception ex) { throw new IllegalStateException("Cannot load Edo Tensei coffin asset " + MODEL, ex); }
    }
    private static double[] vector(JsonArray array) {
        double[] result = new double[array.size()];
        for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsDouble() / 16;
        return result;
    }
    private static class Face {
        final double[][] points; final double[] uv; final float light; final int rotation;
        Face(double[][] p, double[] u, float l, int r) { points=p; uv=u; light=l; rotation=r; }
    }
    private void draw(List<Face> faces) {
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (Face face : faces) {
            double[] u = {face.uv[0],face.uv[0],face.uv[2],face.uv[2]};
            double[] v = {face.uv[1],face.uv[3],face.uv[3],face.uv[1]};
            for (int i = 0; i < 4; i++) {
                int t = Math.floorMod(i + face.rotation, 4);
                b.pos(face.points[i][0], face.points[i][1], face.points[i][2]).tex(u[t],v[t])
                    .color(face.light,face.light,face.light,1f).endVertex();
            }
        }
        Tessellator.getInstance().draw();
    }

    /** Pure geometry, also used by the offline seal preview/regression check. */
    public static final class SealGeometry {
    private final List<double[]> ink = new ArrayList<>();
    public SealGeometry() { buildSeal(); }
    public List<double[]> quads() { return Collections.unmodifiableList(ink); }
    // Thin filled ribbons preserve the ink silhouette at any resolution without a blurry decal.
    private void stroke(double x, double z, double xx, double zz, double width) {
        double len = Math.hypot(xx-x,zz-z);
        if (len < .00001) return;
        double nx = -(zz-z)/len*width*.5, nz = (xx-x)/len*width*.5;
        ink.add(new double[]{x+nx,z+nz,xx+nx,zz+nz,xx-nx,zz-nz,x-nx,z-nz});
    }
    private void ring(double x, double z, double radius, double width, int seed) {
        Random r = new Random(seed);
        double lastX=x+radius,lastZ=z;
        for (int i=1;i<=240;i++) {
            double angle=i*Math.PI*2/240, rr=radius+(i==240 ? 0 : (r.nextDouble()-.5)*.045);
            double px=x+Math.cos(angle)*rr,pz=z+Math.sin(angle)*rr;
            stroke(lastX,lastZ,px,pz,width); lastX=px; lastZ=pz;
        }
    }
    private void buildSeal() {
        for (int i=0;i<120;i++) {
            double a=i*Math.PI*2/120,b=(i+1)*Math.PI*2/120;
            ink.add(new double[]{0,0,.73*Math.cos(a),.73*Math.sin(a),.73*Math.cos(b),.73*Math.sin(b),0,0});
        }
        ring(0,0,1.53,.033,31);
        ring(0,0,1.60,.014,71);
        Random r=new Random(149);
        for(int s=0;s<3;s++) {
            double a=-Math.PI/2+s*Math.PI*2/3, cx=Math.cos(a)*2.03, cz=Math.sin(a)*2.03;
            ring(cx,cz,.36,.026,s+12);
            // Three distinct ritual glyphs, composed as angular Minecraft-readable brush strokes.
            stroke(cx-.18,cz-.12,cx+.18,cz-.12,.038);
            stroke(cx,cz-.24,cx,cz+.22,.045);
            stroke(cx-.17,cz+.18,cx+.18,cz+.18,.037);
            if(s!=0) { stroke(cx-.17,cz-.03,cx+.15,cz+.08,.025); stroke(cx-.09,cz-.23,cx-.17,cz+.18,.027); }
            if(s==2) stroke(cx+.15,cz-.23,cx+.12,cz+.22,.032);
            for(int branch=-1;branch<=1;branch++) {
                double lastX=cx,lastZ=cz;
                for(int j=0;j<=100;j++) {
                    double t=j/100.0, angle=a+branch*.92*Sequence.smooth((float)t);
                    double radius=2.4+t*.8, jitter=(r.nextDouble()-.5)*.06;
                    double px=Math.cos(angle)*(radius+jitter),pz=Math.sin(angle)*(radius+jitter);
                    if(j>0) stroke(lastX,lastZ,px,pz,.019+(1-t)*.014);
                    if(j%7==0) stroke(px,pz,px+Math.cos(angle+.9)*.10,pz+Math.sin(angle+.9)*.10,.021);
                    lastX=px;lastZ=pz;
                }
            }
        }
        for(int i=0;i<150;i++) {
            double a=i*Math.PI*2/150, rr=1.64;
            double x=Math.cos(a)*rr,z=Math.sin(a)*rr;
            stroke(x,z,x+Math.cos(a+.5)*(.05+r.nextDouble()*.09),z+Math.sin(a+.5)*(.05+r.nextDouble()*.09),.02);
        }
    }
    }
}
