package net.narutomod.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

import net.narutomod.NarutomodMod;
import net.narutomod.PlayerStats;

import java.io.IOException;

@SideOnly(Side.CLIENT)
public class GuiPlayerStats extends GuiScreen {
	private static final int WIDTH = 360;
	private static final int HEIGHT = 226;
	private static final String[] STAT_NAME_KEYS = {
		"gui.narutomod.stats.speed", "gui.narutomod.stats.strength", "gui.narutomod.stats.resistance",
		"gui.narutomod.stats.health", "gui.narutomod.stats.chakra", "gui.narutomod.stats.spi"
	};
	private int left;
	private int top;
	private int tab;

	@Override
	public void initGui() {
		this.left = (this.width - WIDTH) / 2;
		this.top = (this.height - HEIGHT) / 2;
		this.buttonList.clear();
		this.buttonList.add(new GuiButton(100, this.left + 158, this.top + 28, 80, 18, tr("gui.narutomod.stats.tab")));
		this.buttonList.add(new GuiButton(101, this.left + 242, this.top + 28, 80, 18, "Uchiha"));
		for (int i = 0; i < PlayerStats.getStatKeys().length; i++) {
			this.buttonList.add(new GuiButton(i, this.left + 320, this.top + 68 + i * 22, 24, 18, "+"));
		}
		this.buttonList.add(new GuiButton(6, this.left + 220, this.top + 154, 104, 20, tr("gui.narutomod.stats.upgrade")));
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player == null) return;
		boolean isUchiha = PlayerStats.getClan(player).equalsIgnoreCase("Uchiha");
		if (!isUchiha && this.tab == 1) {
			this.tab = 0;
		}
		this.drawPanel();
		this.drawCenteredString(this.fontRenderer, TextFormatting.GOLD + tr("gui.narutomod.stats.title"), this.left + WIDTH / 2, this.top + 10, 0xFFFFFF);
		this.fontRenderer.drawString(TextFormatting.YELLOW + player.getName(), this.left + 20, this.top + 30, 0xFFFFFF);
		this.drawInfoLine(tr("gui.narutomod.stats.clan"), PlayerStats.getClan(player), 44);
		this.drawInfoLine(tr("gui.narutomod.stats.rank"), PlayerStats.getRank(player), 58);
		this.drawInfoLine(tr("gui.narutomod.stats.affinity"), PlayerStats.getAffinity(player), 72);
		this.drawInfoLine(tr("gui.narutomod.stats.points"), PlayerStats.getAvailablePoints(player) + " / " + PlayerStats.getPointLimit(player), 84);
		this.buttonList.get(1).visible = isUchiha;
		boolean uchihaTab = this.tab == 1 && isUchiha;
		this.drawSection(this.left + 150, this.top + 52, this.left + WIDTH - 18, this.top + 56);
		for (int i = 0; i < PlayerStats.getStatKeys().length; i++) {
			GuiButton button = this.buttonList.get(i + 2);
			button.visible = !uchihaTab;
			if (!uchihaTab) {
				int y = this.top + 70 + i * 22;
				this.drawStatRow(tr(STAT_NAME_KEYS[i]), PlayerStats.getStat(player, i), y);
				button.enabled = PlayerStats.getAvailablePoints(player) > 0 && PlayerStats.getStat(player, i) < PlayerStats.getRankStatLimit(player);
			}
		}
		GuiButton sharinganButton = this.buttonList.get(8);
		sharinganButton.visible = uchihaTab;
		int stage = PlayerStats.getSharinganStage(player);
		if (uchihaTab) {
			this.fontRenderer.drawString(TextFormatting.RED + tr("gui.narutomod.stats.uchiha_progression"), this.left + 166, this.top + 76, 0xFFFFFF);
			this.fontRenderer.drawString(tr("gui.narutomod.stats.sharingan_stage"), this.left + 166, this.top + 104, 0xFFE8D59B);
			this.fontRenderer.drawString(tr("gui.narutomod.stats.tomoe", stage), this.left + 270, this.top + 104, 0xFFFFCC66);
			this.fontRenderer.drawString(tr("gui.narutomod.stats.sharingan_cost"), this.left + 166, this.top + 126, 0xFFB9B9B9);
			sharinganButton.enabled = PlayerStats.getAvailablePoints(player) > 0 && stage < 3;
		}
		GuiInventory.drawEntityOnScreen(this.left + 82, this.top + 190, 50, this.left + 82 - mouseX, this.top + 98 - mouseY, player);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawPanel() {
		drawRect(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, 0xF00A0A0A);
		drawRect(this.left + 4, this.top + 4, this.left + WIDTH - 4, this.top + HEIGHT - 4, 0xFF3A2414);
		drawRect(this.left + 8, this.top + 8, this.left + WIDTH - 8, this.top + HEIGHT - 8, 0xEE15100B);
		drawRect(this.left + 14, this.top + 24, this.left + 138, this.top + HEIGHT - 14, 0xAA0B0B0B);
		drawRect(this.left + 146, this.top + 24, this.left + WIDTH - 14, this.top + HEIGHT - 14, 0xAA0B0B0B);
		this.drawSection(this.left + 14, this.top + 86, this.left + 138, this.top + 90);
		this.drawSection(this.left + 146, this.top + 24, this.left + WIDTH - 14, this.top + 28);
	}

	private void drawSection(int x1, int y1, int x2, int y2) {
		drawRect(x1, y1, x2, y2, 0xFFB9852B);
		drawRect(x1, y2, x2, y2 + 1, 0xFF4B3017);
	}

	private void drawInfoLine(String label, String value, int y) {
		this.fontRenderer.drawString(label + ":", this.left + 20, this.top + y, 0xFFB9B9B9);
		this.fontRenderer.drawString(this.trim(value, 62), this.left + 70, this.top + y, 0xFFFFCC66);
	}

	private void drawStatRow(String label, int value, int y) {
		drawRect(this.left + 160, y - 4, this.left + 314, y + 15, 0x552A1A10);
		this.fontRenderer.drawString(this.trim(label, 76), this.left + 166, y, 0xFFE8D59B);
		this.fontRenderer.drawString(this.trim(value + "/" + PlayerStats.getRankStatLimit(Minecraft.getMinecraft().player), 56), this.left + 254, y, 0xFFFFCC66);
	}

	private String trim(String text, int width) {
		return this.fontRenderer.trimStringToWidth(text == null ? "" : text, width);
	}

	private static String tr(String key, Object... args) {
		return I18n.format(key, args);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 100) {
			this.tab = 0;
		} else if (button.id == 101) {
			if (Minecraft.getMinecraft().player != null && PlayerStats.getClan(Minecraft.getMinecraft().player).equalsIgnoreCase("Uchiha")) {
				this.tab = 1;
			}
		} else {
			NarutomodMod.PACKET_HANDLER.sendToServer(new PlayerStats.UpgradeMessage(button.id));
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
