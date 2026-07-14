package net.narutomod.client;

import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

@SideOnly(Side.CLIENT)
public class ClientGenjutsuOverlay {
	private static int type;
	private static int ticks;
	private static int lastTick;

	public static void handleMessage(int typeIn, int ticksIn) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			if (typeIn < 0) clear();
			else activate(typeIn, ticksIn);
		});
	}

	public static void activate(int typeIn, int ticksIn) {
		type = typeIn;
		ticks = Math.max(ticks, ticksIn);
	}

	public static void clear() {
		ticks = 0;
		type = 0;
	}

	@SubscribeEvent
	public void onInput(InputUpdateEvent event) {
		if (ticks > 0 && type == 0) {
			float f = event.getMovementInput().moveForward;
			event.getMovementInput().moveForward = -f;
			event.getMovementInput().moveStrafe = -event.getMovementInput().moveStrafe;
		}
	}

	@SubscribeEvent
	public void onOverlay(RenderGameOverlayEvent.Post event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (ticks <= 0 || mc.player == null || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
		int alpha = type == 0 ? 55 : type == 1 ? 75 : type == 2 ? 95 : type == 3 ? 120 : 100;
		int color = type == 0 ? 0x550055AA : type == 1 ? 0x66440066 : type == 2 ? 0x77AA0000 : type == 3 ? 0x88000000 : 0x77AA2200;
		if (mc.player.ticksExisted % (type == 1 ? 4 : 7) < 2) {
			Gui.drawRect(0, 0, event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(), (alpha << 24) | (color & 0xFFFFFF));
		}
		if (mc.player.ticksExisted != lastTick) {
			ticks--;
			lastTick = mc.player.ticksExisted;
		}
	}
}
