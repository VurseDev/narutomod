package net.narutomod.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import net.narutomod.NarutomodMod;
import net.narutomod.PlayerStats;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

@SideOnly(Side.CLIENT)
public class GuiPlayerStats extends GuiScreen {
	private static final int WIDTH = 430;
	private static final int HEIGHT = 276;
	private static final int LEFT_PAGE_X = 12;
	private static final int LEFT_PAGE_WIDTH = 194;
	private static final int RIGHT_PAGE_X = 216;
	private static final int RIGHT_PAGE_WIDTH = 202;
	private static final int ROW_TOP = 43;
	private static final int ROW_HEIGHT = 30;
	private static final String[] STAT_NAME_KEYS = {
		"gui.narutomod.stats.speed", "gui.narutomod.stats.strength", "gui.narutomod.stats.resistance",
		"gui.narutomod.stats.health", "gui.narutomod.stats.chakra", "gui.narutomod.stats.spi"
	};
	private static final ItemStack[] STAT_ICONS = {
		new ItemStack(Items.FEATHER), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD),
		new ItemStack(Items.APPLE), new ItemStack(Items.DYE, 1, 4), new ItemStack(Items.BLAZE_POWDER)
	};

	private final DossierButton[] statButtons = new DossierButton[6];
	private int left;
	private int top;
	private int tab;
	private int allocationAmount = 1;
	private DossierButton statsTab;
	private DossierButton uchihaTab;
	private DossierButton sharinganButton;
	private DossierButton plusOneButton;
	private DossierButton plusTenButton;
	private DossierButton maxButton;

	@Override
	public void initGui() {
		this.left = (this.width - WIDTH) / 2;
		this.top = (this.height - HEIGHT) / 2;
		this.buttonList.clear();

		this.statsTab = this.addDossierButton(new DossierButton(100, this.left + 325, this.top + 14, 42, 16,
		 tr("gui.narutomod.stats.tab"), DossierButton.TAB));
		this.uchihaTab = this.addDossierButton(new DossierButton(101, this.left + 369, this.top + 14, 47, 16,
		 "Uchiha", DossierButton.TAB));

		for (int i = 0; i < this.statButtons.length; i++) {
			this.statButtons[i] = this.addDossierButton(new DossierButton(i, this.left + 391,
			 this.top + ROW_TOP + i * ROW_HEIGHT + 3, 20, 22, "+", DossierButton.PLUS));
		}

		this.sharinganButton = this.addDossierButton(new DossierButton(6, this.left + 252, this.top + 184, 130, 22,
		 tr("gui.narutomod.stats.upgrade"), DossierButton.ACTION));
		this.plusOneButton = this.addDossierButton(new DossierButton(200, this.left + 238, this.top + 236, 48, 18,
		 "+1", DossierButton.SELECTOR));
		this.plusTenButton = this.addDossierButton(new DossierButton(201, this.left + 291, this.top + 236, 48, 18,
		 "+10", DossierButton.SELECTOR));
		this.maxButton = this.addDossierButton(new DossierButton(202, this.left + 344, this.top + 236, 62, 18,
		 "MAX", DossierButton.SELECTOR));
	}

	private DossierButton addDossierButton(DossierButton button) {
		this.buttonList.add(button);
		return button;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player == null) return;

		boolean isUchiha = PlayerStats.getClan(player).equalsIgnoreCase("Uchiha");
		if (!isUchiha && this.tab == 1) this.tab = 0;
		boolean uchihaPage = this.tab == 1 && isUchiha;

		this.drawDossier();
		this.drawIdentityPage(player, mouseX, mouseY);
		if (uchihaPage) this.drawUchihaPage(player);
		else this.drawStatsPage(player);

		this.statsTab.selected = !uchihaPage;
		this.uchihaTab.visible = isUchiha;
		this.uchihaTab.selected = uchihaPage;
		this.sharinganButton.visible = uchihaPage;
		for (DossierButton button : this.statButtons) button.visible = !uchihaPage;
		this.plusOneButton.visible = !uchihaPage;
		this.plusTenButton.visible = !uchihaPage;
		this.maxButton.visible = !uchihaPage;
		this.plusOneButton.selected = this.allocationAmount == 1;
		this.plusTenButton.selected = this.allocationAmount == 10;
		this.maxButton.selected = this.allocationAmount < 0;

		super.drawScreen(mouseX, mouseY, partialTicks);
		if (!uchihaPage) this.drawStatTooltip(player, mouseX, mouseY);
	}

	private void drawDossier() {
		// Drop shadow and leather cover.
		drawRect(this.left - 4, this.top - 4, this.left + WIDTH + 5, this.top + HEIGHT + 6, 0x88000000);
		drawRect(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, 0xFF17120F);
		drawRect(this.left + 3, this.top + 3, this.left + WIDTH - 3, this.top + HEIGHT - 3, 0xFF352820);
		drawRect(this.left + 7, this.top + 7, this.left + WIDTH - 7, this.top + HEIGHT - 7, 0xFF15110F);

		// Two slightly different paper pages and the dark binding gutter.
		drawRect(this.left + LEFT_PAGE_X, this.top + 10, this.left + LEFT_PAGE_X + LEFT_PAGE_WIDTH,
		 this.top + HEIGHT - 10, 0xFFF0E1BE);
		drawRect(this.left + RIGHT_PAGE_X, this.top + 10, this.left + RIGHT_PAGE_X + RIGHT_PAGE_WIDTH,
		 this.top + HEIGHT - 10, 0xFFEADAB5);
		drawRect(this.left + 205, this.top + 10, this.left + 216, this.top + HEIGHT - 10, 0xFF4A382C);
		drawRect(this.left + 208, this.top + 10, this.left + 211, this.top + HEIGHT - 10, 0x775E4938);

		// Paper fibres and page rules.
		for (int y = 17; y < HEIGHT - 12; y += 13) {
			drawRect(this.left + 15, this.top + y, this.left + 202, this.top + y + 1, 0x0F5A4530);
			drawRect(this.left + 219, this.top + y + 5, this.left + 415, this.top + y + 6, 0x0C5A4530);
		}
		this.drawPageBorder(this.left + 16, this.top + 14, this.left + 202, this.top + HEIGHT - 14);
		this.drawPageBorder(this.left + 220, this.top + 14, this.left + 414, this.top + HEIGHT - 14);
		this.drawCornerBrackets();
	}

	private void drawPageBorder(int x1, int y1, int x2, int y2) {
		drawRect(x1, y1, x2, y1 + 1, 0xFF66533D);
		drawRect(x1, y2 - 1, x2, y2, 0xFF66533D);
		drawRect(x1, y1, x1 + 1, y2, 0xFF66533D);
		drawRect(x2 - 1, y1, x2, y2, 0xFF66533D);
	}

	private void drawCornerBrackets() {
		int iron = 0xFF6B6760;
		int dark = 0xFF282725;
		int[][] corners = {
			{this.left, this.top}, {this.left + WIDTH - 13, this.top},
			{this.left, this.top + HEIGHT - 13}, {this.left + WIDTH - 13, this.top + HEIGHT - 13}
		};
		for (int[] corner : corners) {
			drawRect(corner[0], corner[1], corner[0] + 13, corner[1] + 13, iron);
			drawRect(corner[0] + 3, corner[1] + 3, corner[0] + 10, corner[1] + 10, dark);
			drawRect(corner[0] + 5, corner[1] + 5, corner[0] + 8, corner[1] + 8, 0xFF9A958A);
		}
	}

	private void drawIdentityPage(EntityPlayer player, int mouseX, int mouseY) {
		int pageLeft = this.left + LEFT_PAGE_X;
		this.drawCenteredString(this.fontRenderer, TextFormatting.DARK_GRAY + tr("gui.narutomod.stats.record"),
		 pageLeft + LEFT_PAGE_WIDTH / 2, this.top + 18, 0xFF2B2926);
		this.fontRenderer.drawString(this.trim(player.getName(), 170), this.left + 24, this.top + 30, 0xFF7A241F);

		// Portrait frame.
		drawRect(this.left + 23, this.top + 41, this.left + 98, this.top + 124, 0xFF2E2721);
		drawRect(this.left + 26, this.top + 44, this.left + 95, this.top + 121, 0xFFD7C8A8);
		drawRect(this.left + 29, this.top + 47, this.left + 92, this.top + 118, 0xFF342F2A);
		GuiInventory.drawEntityOnScreen(this.left + 60, this.top + 117, 34,
		 this.left + 60 - mouseX, this.top + 78 - mouseY, player);

		// Village ID seal/fingerprint box.
		drawRect(this.left + 108, this.top + 41, this.left + 195, this.top + 124, 0xFF6A5841);
		drawRect(this.left + 110, this.top + 43, this.left + 193, this.top + 122, 0xFFE4D4B0);
		this.drawCenteredString(this.fontRenderer, tr("gui.narutomod.stats.id_seal"), this.left + 151,
		 this.top + 48, 0xFF3C342B);
		this.drawFingerprint(this.left + 151, this.top + 84);
		this.drawCenteredString(this.fontRenderer, tr("gui.narutomod.stats.verified"), this.left + 151,
		 this.top + 108, 0xFF9B3027);

		this.drawRecordField(tr("gui.narutomod.stats.rank"), PlayerStats.getRank(player), 130);
		this.drawRecordField(tr("gui.narutomod.stats.clan"), PlayerStats.getClan(player), 147);
		this.drawRecordField(tr("gui.narutomod.stats.affinity"), PlayerStats.getAffinity(player), 164);

		this.drawValueBox(tr("gui.narutomod.stats.points"),
		 formatDisplayNumber(PlayerStats.getAvailablePoints(player)) + "/" + formatDisplayNumber(PlayerStats.getPoints(player)),
		 this.left + 23, this.top + 185, 82, 43);
		this.drawValueBox(tr("gui.narutomod.stats.stat_cap"), formatDisplayNumber(PlayerStats.getStatLimit(player)),
		 this.left + 111, this.top + 185, 83, 43);
		this.fontRenderer.drawString(tr("gui.narutomod.stats.point_format"), this.left + 26, this.top + 231, 0xFF796A55);
		this.fontRenderer.drawString(tr("gui.narutomod.stats.file_number") + "  " + this.shortFileId(player),
		 this.left + 23, this.top + 249, 0xFF605445);
	}

	private void drawRecordField(String label, String value, int yOffset) {
		int x1 = this.left + 23;
		int x2 = this.left + 194;
		int y = this.top + yOffset;
		drawRect(x1, y, x2, y + 15, 0x286C5940);
		drawRect(x1, y + 14, x2, y + 15, 0xFF8A7455);
		this.fontRenderer.drawString(label.toUpperCase(Locale.ROOT), x1 + 4, y + 3, 0xFF4B4033);
		this.fontRenderer.drawString(this.trim(value, 92), x1 + 72, y + 3, 0xFF201C18);
	}

	private void drawValueBox(String label, String value, int x, int y, int width, int height) {
		drawRect(x, y, x + width, y + height, 0xFF8D392F);
		drawRect(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFE0CEAA);
		this.drawCenteredString(this.fontRenderer, label.toUpperCase(Locale.ROOT), x + width / 2, y + 6, 0xFF554738);
		this.drawCenteredString(this.fontRenderer, this.trim(value, width - 8), x + width / 2, y + 23, 0xFF241F1A);
	}

	private void drawFingerprint(int centerX, int centerY) {
		int ink = 0xFF40362C;
		for (int radius = 20; radius >= 5; radius -= 4) {
			drawRect(centerX - radius, centerY - radius, centerX + radius, centerY - radius + 1, ink);
			drawRect(centerX - radius, centerY + radius - 1, centerX + radius, centerY + radius, ink);
			drawRect(centerX - radius, centerY - radius, centerX - radius + 1, centerY + radius, ink);
			drawRect(centerX + radius - 1, centerY - radius + 4, centerX + radius, centerY + radius, ink);
		}
		drawRect(centerX - 2, centerY - 7, centerX + 2, centerY + 8, ink);
		drawRect(centerX + 2, centerY + 7, centerX + 8, centerY + 8, ink);
	}

	private void drawStatsPage(EntityPlayer player) {
		this.fontRenderer.drawString(tr("gui.narutomod.stats.attributes"), this.left + 226, this.top + 19, 0xFF3B3127);
		for (int i = 0; i < this.statButtons.length; i++) {
			int y = this.top + ROW_TOP + i * ROW_HEIGHT;
			this.drawStatRow(player, tr(STAT_NAME_KEYS[i]), i, PlayerStats.getStat(player, i), y);
			this.statButtons[i].enabled = PlayerStats.getAvailablePoints(player) > 0
			 && PlayerStats.getStat(player, i) < PlayerStats.getStatLimit(player);
		}
	}

	private void drawStatRow(EntityPlayer player, String label, int stat, int value, int y) {
		int x1 = this.left + 222;
		int x2 = this.left + 413;
		drawRect(x1, y, x2, y + 28, 0xFF9A8565);
		drawRect(x1 + 1, y + 1, x2 - 1, y + 27, stat % 2 == 0 ? 0xFFE4D4B1 : 0xFFDDCCA8);
		drawRect(x1 + 22, y + 1, x1 + 23, y + 27, 0x55705C43);

		RenderHelper.enableGUIStandardItemLighting();
		Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(STAT_ICONS[stat], x1 + 3, y + 6);
		RenderHelper.disableStandardItemLighting();

		this.fontRenderer.drawString(label.toUpperCase(Locale.ROOT), x1 + 27, y + 4, 0xFF302920);
		String valueText = formatDisplayNumber(value);
		this.fontRenderer.drawString(valueText, this.left + 385 - this.fontRenderer.getStringWidth(valueText), y + 4, 0xFF5D241E);

		int barX = x1 + 27;
		int barY = y + 18;
		int barWidth = 136;
		drawRect(barX, barY, barX + barWidth, barY + 5, 0xFF5A4C3A);
		drawRect(barX + 1, barY + 1, barX + barWidth - 1, barY + 4, 0xFFC4B38F);
		double ratio = PlayerStats.getStatLimit(player) > 0
		 ? Math.min(1.0d, value / (double)PlayerStats.getStatLimit(player)) : 0.0d;
		int fill = Math.max(0, (int)Math.round((barWidth - 2) * ratio));
		int color = stat == 3 ? 0xFF9A3730 : stat == 4 ? 0xFF356D93 : stat == 5 ? 0xFF9A752E : 0xFF61764B;
		if (fill > 0) drawRect(barX + 1, barY + 1, barX + 1 + fill, barY + 4, color);
	}

	private void drawUchihaPage(EntityPlayer player) {
		int stage = PlayerStats.getSharinganStage(player);
		this.fontRenderer.drawString(TextFormatting.DARK_RED + tr("gui.narutomod.stats.uchiha_progression"),
		 this.left + 228, this.top + 47, 0xFF7A211C);
		drawRect(this.left + 235, this.top + 69, this.left + 401, this.top + 70, 0xFF8D392F);
		this.drawSharinganSeal(this.left + 318, this.top + 111, stage);
		this.drawCenteredString(this.fontRenderer, tr("gui.narutomod.stats.sharingan_stage"),
		 this.left + 318, this.top + 145, 0xFF514333);
		this.drawCenteredString(this.fontRenderer, tr("gui.narutomod.stats.tomoe", stage),
		 this.left + 318, this.top + 158, 0xFF8D2822);
		this.drawCenteredString(this.fontRenderer, tr("gui.narutomod.stats.sharingan_cost"),
		 this.left + 318, this.top + 171, 0xFF76644D);
		this.sharinganButton.enabled = PlayerStats.getAvailablePoints(player) > 0 && stage < 3;
		this.drawCenteredString(this.fontRenderer,
		 tr("gui.narutomod.stats.points") + " " + formatDisplayNumber(PlayerStats.getAvailablePoints(player))
			 + "/" + formatDisplayNumber(PlayerStats.getPoints(player)),
		 this.left + 318, this.top + 217, 0xFF514333);
	}

	private void drawSharinganSeal(int centerX, int centerY, int stage) {
		drawRect(centerX - 27, centerY - 27, centerX + 27, centerY + 27, 0xFF4E2723);
		drawRect(centerX - 24, centerY - 24, centerX + 24, centerY + 24, 0xFFB65A50);
		drawRect(centerX - 5, centerY - 5, centerX + 5, centerY + 5, 0xFF1E1715);
		int[][] positions = {{0, -16}, {14, 9}, {-14, 9}};
		for (int i = 0; i < positions.length; i++) {
			int color = i < stage ? 0xFF181313 : 0x557A2B25;
			int x = centerX + positions[i][0];
			int y = centerY + positions[i][1];
			drawRect(x - 3, y - 3, x + 4, y + 4, color);
			drawRect(x + 3, y - 1, x + 8, y + 1, color);
		}
	}

	private void drawStatTooltip(EntityPlayer player, int mouseX, int mouseY) {
		if (mouseX < this.left + 222 || mouseX >= this.left + 413) return;
		for (int i = 0; i < this.statButtons.length; i++) {
			int y = this.top + ROW_TOP + i * ROW_HEIGHT;
			if (mouseY >= y && mouseY < y + 28) {
				this.drawHoveringText(Arrays.asList(
				 TextFormatting.GOLD + tr(STAT_NAME_KEYS[i]),
				 TextFormatting.GRAY + PlayerStats.getStatEffectText(player, i),
				 TextFormatting.DARK_GRAY + formatDisplayNumber(PlayerStats.getStat(player, i)) + " / "
					 + formatDisplayNumber(PlayerStats.getStatLimit(player))
				), mouseX, mouseY);
				return;
			}
		}
	}

	private String shortFileId(EntityPlayer player) {
		String raw = player.getUniqueID().toString().replace("-", "").toUpperCase(Locale.ROOT);
		return "KGV-" + raw.substring(0, Math.min(8, raw.length()));
	}

	private static String formatNumber(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	private static String formatDisplayNumber(long value) {
		long absolute = value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
		if (absolute >= 1000000000L) return String.format(Locale.ROOT, "%.2fB", value / 1000000000.0d);
		if (absolute >= 1000000L) return String.format(Locale.ROOT, "%.2fM", value / 1000000.0d);
		return formatNumber(value);
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
			if (Minecraft.getMinecraft().player != null
			 && PlayerStats.getClan(Minecraft.getMinecraft().player).equalsIgnoreCase("Uchiha")) {
				this.tab = 1;
			}
		} else if (button.id == 200) {
			this.allocationAmount = 1;
		} else if (button.id == 201) {
			this.allocationAmount = 10;
		} else if (button.id == 202) {
			this.allocationAmount = -1;
		} else if (button.id >= 0 && button.id < PlayerStats.getStatKeys().length) {
			NarutomodMod.PACKET_HANDLER.sendToServer(new PlayerStats.UpgradeMessage(button.id, this.allocationAmount));
		} else if (button.id == 6) {
			NarutomodMod.PACKET_HANDLER.sendToServer(new PlayerStats.UpgradeMessage(6, 1));
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	private static class DossierButton extends GuiButton {
		private static final int TAB = 0;
		private static final int PLUS = 1;
		private static final int ACTION = 2;
		private static final int SELECTOR = 3;
		private final int style;
		private boolean selected;

		private DossierButton(int id, int x, int y, int width, int height, String text, int style) {
			super(id, x, y, width, height, text);
			this.style = style;
		}

		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			if (!this.visible) return;
			this.hovered = mouseX >= this.x && mouseY >= this.y
			 && mouseX < this.x + this.width && mouseY < this.y + this.height;
			int border = this.enabled ? (this.hovered ? 0xFF9E3D31 : 0xFF574737) : 0xFF756B5D;
			int fill;
			if (!this.enabled) fill = 0xFFB2A58D;
			else if (this.selected) fill = 0xFF8F392F;
			else if (this.style == PLUS) fill = this.hovered ? 0xFFB38A4A : 0xFF8B6A36;
			else if (this.style == TAB) fill = this.hovered ? 0xFFD2BE97 : 0xFFC4AE85;
			else fill = this.hovered ? 0xFFD4C09A : 0xFFC1AC84;

			drawRect(this.x, this.y, this.x + this.width, this.y + this.height, border);
			drawRect(this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, fill);
			drawRect(this.x + 2, this.y + 2, this.x + this.width - 2, this.y + 3,
			 this.selected ? 0xFFB85A4D : 0x55FFFFFF);
			int textColor = !this.enabled ? 0xFF746B5E : this.selected ? 0xFFFFE6BE : 0xFF2A241D;
			this.drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2,
			 this.y + (this.height - 8) / 2, textColor);
		}
	}
}
