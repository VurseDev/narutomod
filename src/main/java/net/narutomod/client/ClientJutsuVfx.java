package net.narutomod.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.JutsuVisualEffects;
import org.lwjgl.opengl.GL11;

/** Lightweight client renderer for the bounded elemental VFX messages. */
@SideOnly(Side.CLIENT)
public final class ClientJutsuVfx {
    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static net.minecraft.world.World effectWorld;
    private static final ResourceLocation PILLAR_STONE = new ResourceLocation("minecraft:textures/blocks/stone.png");

    private static final class Effect {
        final int kind, color, source, element, seed, life;
        final float scale;
        final Vec3d from, to;
        int age;
        Effect(JutsuVisualEffects.Message m) {
            kind=m.kind;color=m.color;source=m.source;element=m.element;seed=m.seed;life=m.life;scale=m.scale;
            from=m.from;to=m.to;
        }
    }

    public static void accept(JutsuVisualEffects.Message message) {
        if (message == null || !message.valid()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || message.dimension != mc.world.provider.getDimension()) return;
        synchronized (EFFECTS) {
            if (effectWorld != mc.world) EFFECTS.clear();
            effectWorld = mc.world;
            if(message.kind==JutsuVisualEffects.CAST) ClientElementalCasting.release(message.source,message.element);
            if(message.kind==JutsuVisualEffects.LIGHTNING_BIND) {
                int cages=0;
                for(Effect effect:EFFECTS)if(effect.kind==JutsuVisualEffects.LIGHTNING_BIND)cages++;
                if(cages>=8)for(Iterator<Effect> it=EFFECTS.iterator();it.hasNext();) {
                    if(it.next().kind==JutsuVisualEffects.LIGHTNING_BIND){it.remove();break;}
                }
            }
            if (EFFECTS.size() >= 256) EFFECTS.remove(0);
            EFFECTS.add(new Effect(message));
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        synchronized (EFFECTS) {
            if (mc.world != effectWorld) { EFFECTS.clear(); effectWorld = mc.world; return; }
            if (mc.isGamePaused()) return;
            for (Iterator<Effect> it = EFFECTS.iterator(); it.hasNext();) {
                Effect effect = it.next();
                effect.age++;
                if (effect.age >= effect.life) it.remove();
            }
        }
        if(mc.world!=null && !mc.isGamePaused()) for(net.minecraft.entity.player.EntityPlayer player:mc.world.playerEntities) {
            if(!player.isHandActive() || player.ticksExisted%3!=0 || mc.player==null || player.getDistanceSq(mc.player)>48*48)continue;
            net.minecraft.item.ItemStack stack=player.getActiveItemStack();
            if(!(stack.getItem() instanceof net.narutomod.item.ItemJutsu.Base))continue;
            net.narutomod.item.ItemJutsu.JutsuEnum jutsu=net.narutomod.item.ItemJutsu.getCurrentJutsu(stack);
            if(jutsu==null || !jutsu.usesCustomBalance() || jutsu.getType()!=net.narutomod.item.ItemJutsu.JutsuEnum.Type.RAITON)continue;
            Vec3d hand=player.getPositionEyes(1f).addVector(0,-.4,0).add(player.getLookVec().scale(.55));
            Vec3d end=hand.addVector((player.getRNG().nextDouble()-.5)*.9,(player.getRNG().nextDouble()-.5)*.6,(player.getRNG().nextDouble()-.5)*.9);
            JutsuVisualEffects.bolt(mc.world,hand,end,.022f,4,0x75CFFF);
        }
    }

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity camera = mc.getRenderViewEntity();
        if (mc.world == null || mc.world != effectWorld || camera == null) return;
        final List<Effect> copy;
        synchronized (EFFECTS) { if (EFFECTS.isEmpty()) return; copy = new ArrayList<>(EFFECTS); }
        float partial = event.getPartialTicks();
        double cx=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*partial;
        double cy=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*partial;
        double cz=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*partial;
        boolean light=GL11.glIsEnabled(GL11.GL_LIGHTING),texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),blend=GL11.glIsEnabled(GL11.GL_BLEND),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int sr=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB),dr=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        int sa=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA),da=GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        int oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();GlStateManager.disableTexture2D();GlStateManager.disableCull();GlStateManager.disableAlpha();
        GlStateManager.enableBlend();GlStateManager.tryBlendFuncSeparate(770,771,1,0);GlStateManager.depthMask(false);
        try {
            BufferBuilder buffer=Tessellator.getInstance().getBuffer();
            if(copy.stream().anyMatch(e->e.kind==JutsuVisualEffects.LIGHTNING_BIND)) {
                GlStateManager.enableTexture2D();
                mc.getTextureManager().bindTexture(PILLAR_STONE);
                int wrapS=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S);
                int wrapT=GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);
                try {
                    // Opaque stone writes depth so the electric channels correctly pass behind its faces.
                    for(Effect e:copy)if(e.kind==JutsuVisualEffects.LIGHTNING_BIND) {
                        GlStateManager.depthMask(e.age+partial<e.life-12);
                        buffer.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
                        LightningBindVisual.solids((x,y,z,u,v,rgb,opacity)->buffer.pos(x-cx,y-cy,z-cz).tex(u,v)
                            .color(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,opacity).endVertex(),
                            e.from,e.scale,net.narutomod.FourPillarPolicy.radiusFromPacket(e.element),e.age+partial,e.life);
                        Tessellator.getInstance().draw();
                    }
                } finally {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,wrapS);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,wrapT);
                }
                GlStateManager.disableTexture2D();
                GlStateManager.depthMask(false);
            }
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            JutsuVfxGeometry.Sink sink=(x,y,z,rgb,opacity)->buffer.pos(x-cx,y-cy,z-cz)
                .color(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,Math.max(0,Math.min(1,opacity))).endVertex();
            for(Effect e:copy) {
                float age=e.age+partial, fade=JutsuVfxGeometry.envelope(age,e.life);
                switch(e.kind) {
                    case JutsuVisualEffects.BOLT:
                        float rise = e.life<=16?1:JutsuVfxGeometry.smooth(age / Math.min(8f,e.life*.25f));
                        Vec3d tip=e.from.add(e.to.subtract(e.from).scale(rise));
                        JutsuVfxGeometry.bolt(sink,e.from,tip,e.scale, e.color,fade,e.seed+(e.age/2)*31L,mc.gameSettings.particleSetting==2?0:1); break;
                    case JutsuVisualEffects.RING: JutsuVfxGeometry.ring(sink,e.from,e.scale,.045f,e.color,fade,48); break;
                    case JutsuVisualEffects.BURST: JutsuVfxGeometry.burst(sink,e.from,e.scale,age,e.life,e.color,e.seed,1); break;
                    case JutsuVisualEffects.EARTH:
                        for(int rock=0;rock<9;rock++) {
                            float t=(rock+.5f)/9f;
                            float lift=(float)Math.sin(Math.PI*Math.max(0,Math.min(1,age/e.life)))*e.scale;
                            Vec3d p=e.from.add(e.to.subtract(e.from).scale(t)).addVector(0,lift,0);
                            JutsuVfxGeometry.segment(sink,p,p.addVector(0,e.scale*.7,0),e.scale*.45,e.color,fade);
                        } break;
                    case JutsuVisualEffects.CAST:
                        JutsuVfxGeometry.ring(sink,e.from.addVector(0,.05,0),.55f+.14f*(1-fade),.035f,e.color,fade,40);
                        JutsuVfxGeometry.burst(sink,e.from.addVector(0,1.0,0),.32f,age,e.life,e.color,e.seed,0); break;
                    case JutsuVisualEffects.LIGHTNING_BIND: break; // Electric pass follows the stone and alpha effects.
                    default: JutsuVfxGeometry.ribbon(sink,e.from,e.to,e.scale,age,e.life,e.color,2); break;
                }
            }
            Tessellator.getInstance().draw();
            GlStateManager.tryBlendFuncSeparate(770,1,1,0);
            buffer.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
            for(Effect e:copy)if(e.kind==JutsuVisualEffects.LIGHTNING_BIND) {
                Entity victim=mc.world.getEntityByID(e.source);
                Vec3d target=e.from.addVector(0,Math.max(1,e.scale*.55),0);
                double tetherRange=Math.max(6,net.narutomod.FourPillarPolicy.radiusFromPacket(e.element)*2);
                if(victim!=null && victim.isEntityAlive() && victim.getDistanceSq(e.from.x,e.from.y,e.from.z)<tetherRange*tetherRange) {
                    target=new Vec3d(victim.lastTickPosX+(victim.posX-victim.lastTickPosX)*partial,
                        victim.lastTickPosY+(victim.posY-victim.lastTickPosY)*partial+Math.min(e.scale-.7,victim.height*.6),
                        victim.lastTickPosZ+(victim.posZ-victim.lastTickPosZ)*partial);
                }
                LightningBindVisual.energy(sink,e.from,target,e.scale,net.narutomod.FourPillarPolicy.radiusFromPacket(e.element),e.age+partial,e.life,e.seed,
                    mc.gameSettings.particleSetting==2?0:1);
            }
            Tessellator.getInstance().draw();
        } finally {
            GlStateManager.bindTexture(oldTexture);
            GlStateManager.depthMask(mask);GlStateManager.tryBlendFuncSeparate(sr,dr,sa,da);
            if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
            if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
            if(texture)GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
            if(light)GlStateManager.enableLighting();else GlStateManager.disableLighting();
            if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();
            GlStateManager.color(1,1,1,1);GlStateManager.popMatrix();
        }
    }
}
