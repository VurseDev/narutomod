package net.narutomod.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import net.narutomod.PlayerStats;

@SideOnly(Side.CLIENT)
public final class PlayerStatsClient {
	private PlayerStatsClient() {
	}

	public static void handleSync(PlayerStats.SyncMessage message) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			EntityPlayer player = Minecraft.getMinecraft().player;
			if (player != null) {
				PlayerStats.applyClientSync(player, message);
			}
		});
	}
}
