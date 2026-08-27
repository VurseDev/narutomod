package net.narutomod.client;

import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

/** Client-only presentation for the custom genjutsu. The real player is never moved. */
@SideOnly(Side.CLIENT)
public class ClientGenjutsuOverlay {
	private static int type;
	private static int ticks;
	private static int duration;
	private static int lastTick = Integer.MIN_VALUE;
	private static int lastStab = Integer.MIN_VALUE;

	private final ModelPlayer victimModel = new ModelPlayer(0.0f, false);

	public static void handleMessage(int typeIn, int ticksIn) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			if (typeIn < 0) clear();
			else activate(typeIn, ticksIn);
		});
	}

	public static void activate(int typeIn, int ticksIn) {
		boolean replacing = ticks <= 0 || type != typeIn;
		type = typeIn;
		if (replacing) {
			ticks = Math.max(1, ticksIn);
			duration = ticks;
		} else {
			ticks = Math.max(ticks, ticksIn);
			duration = Math.max(duration, ticks);
		}
		lastStab = Integer.MIN_VALUE;
	}

	public static void clear() {
		ticks = 0;
		duration = 0;
		type = 0;
		lastStab = Integer.MIN_VALUE;
	}

	private static int elapsed() {
		return Math.max(0, duration - ticks);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ticks <= 0) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null || mc.world == null) {
			clear();
			return;
		}
		if (mc.player.ticksExisted != lastTick) {
			lastTick = mc.player.ticksExisted;
			if (--ticks <= 0) clear();
		}
	}

	@SubscribeEvent
	public void onInput(InputUpdateEvent event) {
		if (ticks <= 0) return;
		if (type == 0) {
			event.getMovementInput().moveForward = -event.getMovementInput().moveForward;
			event.getMovementInput().moveStrafe = -event.getMovementInput().moveStrafe;
		} else if (type == 2) {
			event.getMovementInput().moveForward *= 0.25f;
			event.getMovementInput().moveStrafe *= 0.25f;
		} else if (type == 3) {
			event.getMovementInput().moveForward = 0f;
			event.getMovementInput().moveStrafe = 0f;
			event.getMovementInput().jump = false;
			event.getMovementInput().sneak = false;
		}
	}

	@SubscribeEvent
	public void onCamera(EntityViewRenderEvent.CameraSetup event) {
		if (ticks <= 0) return;
		float time = elapsed() + (float)event.getRenderPartialTicks();
		if (type == 0) {
			event.setRoll(MathHelper.sin(time * 0.16f) * 3.5f);
			event.setYaw(event.getYaw() + MathHelper.sin(time * 0.11f) * 1.8f);
		} else if (type == 1) {
			event.setRoll(MathHelper.sin(time * 0.38f) * 1.5f);
		} else if (type == 2) {
			event.setRoll(MathHelper.sin(time * 0.52f) * 2.0f);
		} else if (type == 3) {
			float stab = stabPulse(time);
			event.setRoll(MathHelper.sin(time * 0.31f) * 2.4f + stab * MathHelper.sin(time * 2.7f) * 4f);
		} else {
			event.setRoll(MathHelper.sin(time * 0.25f) * 2.0f);
		}
	}

	@SubscribeEvent
	public void onFogColor(EntityViewRenderEvent.FogColors event) {
		if (ticks <= 0) return;
		if (type == 3) {
			event.setRed(0.28f);
			event.setGreen(0.002f);
			event.setBlue(0.002f);
		} else if (type == 4) {
			event.setRed(0.22f);
			event.setGreen(0.035f);
			event.setBlue(0.005f);
		}
	}

	@SubscribeEvent
	public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
		if (ticks > 0 && type == 3) {
			event.setDensity(0.12f);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onRenderWorld(RenderWorldLastEvent event) {
		if (ticks <= 0 || type != 3) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null || mc.world == null) return;

		float partial = event.getPartialTicks();
		float time = elapsed() + partial;
		Vec3d eye = mc.player.getPositionEyes(partial);
		Vec3d look = mc.player.getLook(partial).normalize();
		Vec3d scene = eye.add(look.scale(5.2d));
		double x = scene.x - mc.getRenderManager().viewerPosX;
		double y = scene.y - mc.getRenderManager().viewerPosY;
		double z = scene.z - mc.getRenderManager().viewerPosZ;

		GlStateManager.pushMatrix();
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0f, 1f, 0f);
		GlStateManager.rotate(mc.getRenderManager().playerViewX, 1f, 0f, 0f);
		GlStateManager.scale(1.12f, 1.12f, 1.12f);
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.disableCull();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
		 GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

		GlStateManager.disableTexture2D();
		renderCross();
		renderBindings();
		renderBlades(time);
		GlStateManager.enableTexture2D();
		renderVictim(mc, time);

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1f, 1f, 1f, 1f);
		GlStateManager.popMatrix();

		int stab = elapsed() / 18;
		if (stab != lastStab && elapsed() % 18 <= 1) {
			lastStab = stab;
			mc.player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.42f, 0.55f + mc.player.getRNG().nextFloat() * 0.18f);
		}
	}

	private void renderCross() {
		renderBox(-0.17f, -1.35f, 0.18f, 0.17f, 1.45f, 0.48f, 0.055f, 0.008f, 0.008f, 1f);
		renderBox(-1.55f, 0.18f, 0.18f, 1.55f, 0.48f, 0.48f, 0.065f, 0.009f, 0.009f, 1f);
		renderBox(-0.12f, -1.28f, 0.15f, 0.12f, 1.38f, 0.17f, 0.42f, 0.02f, 0.02f, 0.72f);
	}

	private void renderBindings() {
		for (int i = -1; i <= 1; i += 2) {
			float x = i * 0.98f;
			renderBox(x - 0.17f, 0.20f, -0.10f, x + 0.17f, 0.48f, 0.20f, 0.025f, 0.025f, 0.025f, 1f);
			renderBox(x - 0.13f, 0.24f, -0.13f, x + 0.13f, 0.44f, -0.10f, 0.55f, 0.03f, 0.03f, 0.85f);
		}
	}

	private void renderBlades(float time) {
		float approach = Math.min(1f, (time % 18f) / 7f);
		for (int i = 0; i < 6; i++) {
			boolean left = (i & 1) == 0;
			float row = i / 2;
			float x = (left ? -1f : 1f) * (2.15f - approach * (1.30f + row * 0.09f));
			float y = 0.44f - row * 0.46f;
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, -0.28f - row * 0.035f);
			GlStateManager.rotate(left ? -17f - row * 8f : 197f + row * 8f, 0f, 0f, 1f);
			renderBox(-0.12f, -0.035f, -0.055f, 0.76f, 0.035f, 0.055f, 0.76f, 0.78f, 0.82f, 1f);
			renderBox(0.76f, -0.085f, -0.075f, 1.02f, 0.085f, 0.075f, 0.045f, 0.015f, 0.015f, 1f);
			GlStateManager.popMatrix();
		}
	}

	private void renderVictim(Minecraft mc, float time) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(0f, -1.04f, -0.08f);
		GlStateManager.rotate(180f, 0f, 1f, 0f);
		GlStateManager.scale(-0.92f, -0.92f, 0.92f);
		GlStateManager.translate(0f, -1.501f, 0f);
		mc.getTextureManager().bindTexture(mc.player.getLocationSkin());
		float unit = 0.0625f;
		this.victimModel.isChild = false;
		this.victimModel.isRiding = false;
		this.victimModel.isSneak = false;
		this.victimModel.swingProgress = 0f;
		this.victimModel.setLivingAnimations(mc.player, 0f, 0f, eventPartial(time));
		this.victimModel.setRotationAngles(0f, 0f, time, 0f, -8f, unit, mc.player);
		this.victimModel.bipedRightArm.rotateAngleX = 0f;
		this.victimModel.bipedLeftArm.rotateAngleX = 0f;
		this.victimModel.bipedRightArm.rotateAngleY = 0f;
		this.victimModel.bipedLeftArm.rotateAngleY = 0f;
		this.victimModel.bipedRightArm.rotateAngleZ = 1.48f;
		this.victimModel.bipedLeftArm.rotateAngleZ = -1.48f;
		this.victimModel.bipedHead.rotateAngleX = -0.18f + MathHelper.sin(time * 0.12f) * 0.04f;
		this.victimModel.bipedHeadwear.rotateAngleX = this.victimModel.bipedHead.rotateAngleX;
		this.victimModel.bipedHead.render(unit);
		this.victimModel.bipedHeadwear.render(unit);
		this.victimModel.bipedBody.render(unit);
		this.victimModel.bipedBodyWear.render(unit);
		this.victimModel.bipedRightArm.render(unit);
		this.victimModel.bipedLeftArm.render(unit);
		this.victimModel.bipedRightArmwear.rotateAngleZ = this.victimModel.bipedRightArm.rotateAngleZ;
		this.victimModel.bipedLeftArmwear.rotateAngleZ = this.victimModel.bipedLeftArm.rotateAngleZ;
		this.victimModel.bipedRightArmwear.render(unit);
		this.victimModel.bipedLeftArmwear.render(unit);
		this.victimModel.bipedRightLeg.render(unit);
		this.victimModel.bipedLeftLeg.render(unit);
		this.victimModel.bipedRightLegwear.render(unit);
		this.victimModel.bipedLeftLegwear.render(unit);
		GlStateManager.popMatrix();
	}

	private float eventPartial(float time) {
		return time - (int)time;
	}

	@SubscribeEvent
	public void onOverlay(RenderGameOverlayEvent.Post event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (ticks <= 0 || mc.player == null || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
		int width = event.getResolution().getScaledWidth();
		int height = event.getResolution().getScaledHeight();
		float time = elapsed() + event.getPartialTicks();

		if (type == 0) renderFalseOpening(width, height, time);
		else if (type == 1) renderMemoryFracture(width, height, time);
		else if (type == 2) renderMurderIntent(width, height, time);
		else if (type == 3) renderExecution(width, height, time);
		else renderBurningCoffin(width, height, time);
	}

	private void renderFalseOpening(int width, int height, float time) {
		int offset = (int)(MathHelper.sin(time * 0.45f) * 9f);
		Gui.drawRect(0, 0, width, height, 0x26002055);
		Gui.drawRect(0, 0, Math.max(0, width / 5 + offset), height, 0x28500080);
		Gui.drawRect(Math.min(width, width * 4 / 5 + offset), 0, width, height, 0x28005080);
		for (int y = Math.floorMod((int)time * 5, 32); y < height; y += 32) Gui.drawRect(0, y, width, y + 1, 0x355A20A0);
	}

	private void renderMemoryFracture(int width, int height, float time) {
		int pulse = 34 + (int)(Math.abs(MathHelper.sin(time * 0.22f)) * 45f);
		Gui.drawRect(0, 0, width, height, (pulse << 24) | 0x310044);
		int slice = Math.floorMod((int)time * 13, Math.max(1, height));
		Gui.drawRect(0, slice, width, Math.min(height, slice + 7), 0x705E267B);
		for (int i = 0; i < 5; i++) {
			int inset = i * Math.min(width, height) / 16;
			int alpha = Math.max(8, 42 - i * 7);
			Gui.drawRect(inset, inset, width - inset, inset + 2, (alpha << 24) | 0xB060D0);
			Gui.drawRect(inset, height - inset - 2, width - inset, height - inset, (alpha << 24) | 0xB060D0);
		}
	}

	private void renderMurderIntent(int width, int height, float time) {
		Gui.drawRect(0, 0, width, height, 0x520F0000);
		drawVignette(width, height, 0xB8000000);
		float blink = Math.abs(MathHelper.sin(time * 0.17f));
		int eyeHeight = Math.max(2, (int)(height * 0.075f * blink));
		Gui.drawRect(width / 4, height / 2 - eyeHeight, width * 3 / 4, height / 2 + eyeHeight, 0xA0900000);
		Gui.drawRect(width / 2 - 2, height / 2 - eyeHeight, width / 2 + 2, height / 2 + eyeHeight, 0xE8000000);
	}

	private void renderExecution(int width, int height, float time) {
		float stab = stabPulse(time);
		if ((int)time % 36 == 0 || ((int)time + 1) % 36 == 0) drawInverse(width, height);
		Gui.drawRect(0, 0, width, height, 0x8C520000);
		drawVignette(width, height, 0xE0000000);
		Gui.drawRect(0, 0, width, height / 12, 0xEB000000);
		Gui.drawRect(0, height * 11 / 12, width, height, 0xEB000000);
		if (stab > 0f) {
			int alpha = Math.min(210, 50 + (int)(stab * 160f));
			drawSlash(width, height, -24f, (alpha << 24) | 0xF0E8E8);
			drawSlash(width, height, 31f, (Math.max(30, alpha - 35) << 24) | 0xA00000);
			Gui.drawRect(0, 0, width, height, ((int)(stab * 70f) << 24) | 0x800000);
		}
	}

	private void renderBurningCoffin(int width, int height, float time) {
		int heat = 42 + (int)(Math.abs(MathHelper.sin(time * 0.34f)) * 48f);
		Gui.drawRect(0, 0, width, height, (heat << 24) | 0x7A1600);
		drawVignette(width, height, 0xC00A0000);
		for (int i = 0; i < 5; i++) {
			int x = Math.floorMod((int)(time * (7 + i * 2) + i * 83), width + 80) - 40;
			int top = height - Math.floorMod((int)(time * (5 + i) + i * 41), height + 60);
			Gui.drawRect(x - 22, top, x + 22, Math.min(height, top + 55), 0x32100000);
		}
	}

	private static float stabPulse(float time) {
		float phase = time % 18f;
		return phase < 7f ? phase / 7f : phase < 10f ? 1f : Math.max(0f, 1f - (phase - 10f) / 5f);
	}

	private void drawVignette(int width, int height, int color) {
		int edgeX = Math.max(18, width / 7);
		int edgeY = Math.max(14, height / 7);
		Gui.drawRect(0, 0, width, edgeY, color);
		Gui.drawRect(0, height - edgeY, width, height, color);
		Gui.drawRect(0, edgeY, edgeX, height - edgeY, color);
		Gui.drawRect(width - edgeX, edgeY, width, height - edgeY, color);
	}

	private void drawSlash(int width, int height, float angle, int color) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(width * 0.5f, height * 0.5f, 0f);
		GlStateManager.rotate(angle, 0f, 0f, 1f);
		Gui.drawRect(-width, -2, width, 2, color);
		GlStateManager.popMatrix();
	}

	private void drawInverse(int width, int height) {
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(0, height, -90).color(1f, 1f, 1f, 1f).endVertex();
		buffer.pos(width, height, -90).color(1f, 1f, 1f, 1f).endVertex();
		buffer.pos(width, 0, -90).color(1f, 1f, 1f, 1f).endVertex();
		buffer.pos(0, 0, -90).color(1f, 1f, 1f, 1f).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
		 GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.disableBlend();
	}

	private void renderBox(float x1, float y1, float z1, float x2, float y2, float z2,
	 float red, float green, float blue, float alpha) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder b = tessellator.getBuffer();
		b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		face(b, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, red, green, blue, alpha);
		face(b, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, red, green, blue, alpha);
		face(b, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, red, green, blue, alpha);
		face(b, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, red, green, blue, alpha);
		face(b, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, red, green, blue, alpha);
		face(b, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, red, green, blue, alpha);
		tessellator.draw();
	}

	private void face(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2,
	 float x3, float y3, float z3, float x4, float y4, float z4, float red, float green, float blue, float alpha) {
		b.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
		b.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
		b.pos(x3, y3, z3).color(red, green, blue, alpha).endVertex();
		b.pos(x4, y4, z4).color(red, green, blue, alpha).endVertex();
	}
}
