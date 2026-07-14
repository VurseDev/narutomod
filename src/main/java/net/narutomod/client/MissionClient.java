package net.narutomod.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import net.narutomod.MissionSystem;
import net.narutomod.NarutomodMod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class MissionClient {
	private static final String SEP = "\u001f";

	private static String tr(String key, Object... args) {
		return I18n.format(key, args);
	}

	public static void open(boolean bingoFirst) {
		Minecraft.getMinecraft().displayGuiScreen(new GuiMissionBoard(bingoFirst));
	}

	public static void openAdminFromPacket() {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override public void run() {
				Minecraft.getMinecraft().displayGuiScreen(new GuiAdminPanel());
			}
		});
	}

	public static void handleSync(final boolean bingoFirst, final String rank, final String clan, final int reputation, final int bounty,
			final String active, final String activeBounty, final String wanted, final String customMissions) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override public void run() {
				ClientState.rank = rank;
				ClientState.clan = clan;
				ClientState.reputation = reputation;
				ClientState.bounty = bounty;
				ClientState.active = active;
				ClientState.activeBounty = activeBounty;
				ClientState.wanted = parseRows(wanted);
				ClientState.customMissions = parseRows(customMissions);
				if (Minecraft.getMinecraft().currentScreen instanceof GuiMissionBoard) {
					((GuiMissionBoard)Minecraft.getMinecraft().currentScreen).refreshFromSync(bingoFirst);
				}
			}
		});
	}

	public static void handleAdminSync(final String missions, final String bingo, final String clues, final String events, final String notes, final String arcs) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override public void run() {
				ClientState.adminMissions = missions;
				ClientState.adminBingo = bingo;
				ClientState.adminClues = clues;
				ClientState.adminEvents = events;
				ClientState.adminNotes = notes;
				ClientState.adminArcs = arcs;
			}
		});
	}

	private static List<String[]> parseRows(String payload) {
		List<String[]> list = new ArrayList<String[]>();
		if (payload == null || payload.isEmpty()) return list;
		for (String entry : payload.split(";")) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 5) list.add(parts);
		}
		return list;
	}

	public static class GuiAdminPanel extends GuiScreen {
		private int tab;
		private int left;
		private int top;
		private final int guiWidth = 520;
		private final int guiHeight = 340;
		private final List<GuiTextField> fields = new ArrayList<GuiTextField>();
		private String[] labels = new String[0];

		@Override
		public void initGui() {
			this.left = (this.width - this.guiWidth) / 2;
			this.top = (this.height - this.guiHeight) / 2;
			this.buttonList.clear();
			this.buttonList.add(new GuiButton(10, this.left + 12, this.top + 26, 82, 18, tr("gui.narutomod.mission.admin.missions")));
			this.buttonList.add(new GuiButton(11, this.left + 100, this.top + 26, 72, 18, "Bingo"));
			this.buttonList.add(new GuiButton(12, this.left + 178, this.top + 26, 92, 18, tr("gui.narutomod.mission.admin.archive")));
			buildFields();
		}

		private void buildFields() {
			this.fields.clear();
			if (this.tab == 0) {
				this.labels = new String[] {tr("gui.narutomod.mission.field.name"), tr("gui.narutomod.mission.field.description"), "Rank", "Ryo", tr("gui.narutomod.mission.field.rep"), tr("gui.narutomod.mission.field.min_rank"), tr("gui.narutomod.mission.field.hours"), tr("gui.narutomod.mission.field.target"), tr("gui.narutomod.mission.field.area"), tr("gui.narutomod.mission.field.assigned"), tr("gui.narutomod.mission.field.rewards"), tr("gui.narutomod.mission.field.alert")};
				String[] defaults = {"", "", "C", "0", "0", "Genin", "48", "", "", "", "", "false"};
				addFields(defaults);
				this.buttonList.add(new GuiButton(100, this.left + 392, this.top + 304, 110, 20, tr("gui.narutomod.mission.publish")));
			} else if (this.tab == 1) {
				this.labels = new String[] {tr("gui.narutomod.mission.field.name_npc"), tr("gui.narutomod.mission.field.crime"), tr("gui.narutomod.mission.field.threat"), tr("gui.narutomod.mission.field.bounty"), tr("gui.narutomod.mission.field.last_seen"), tr("gui.narutomod.mission.field.village"), tr("gui.narutomod.mission.field.notes"), "Status", tr("gui.narutomod.mission.field.secret")};
				String[] defaults = {tr("gui.narutomod.mission.default.copycat"), tr("gui.narutomod.mission.default.crime"), "S", "100000", tr("gui.narutomod.mission.unknown"), tr("gui.narutomod.mission.unknown_village"), tr("gui.narutomod.mission.default.danger"), tr("gui.narutomod.mission.unknown"), "false"};
				addFields(defaults);
				this.buttonList.add(new GuiButton(101, this.left + 392, this.top + 236, 110, 20, tr("gui.narutomod.mission.add")));
			} else {
				this.labels = new String[] {tr("gui.narutomod.mission.field.title"), tr("gui.narutomod.mission.field.text"), tr("gui.narutomod.mission.field.meta"), tr("gui.narutomod.mission.field.promote_player"), tr("gui.narutomod.mission.field.new_rank"), tr("gui.narutomod.mission.field.nukenin_player"), tr("gui.narutomod.mission.field.crime"), tr("gui.narutomod.mission.field.bounty"), tr("gui.narutomod.mission.field.notes")};
				String[] defaults = {tr("gui.narutomod.mission.default.arc"), tr("gui.narutomod.mission.default.phase"), tr("gui.narutomod.mission.default.place"), "", "Chunin", "", tr("gui.narutomod.mission.default.nukenin"), "5000", ""};
				addFields(defaults);
				this.buttonList.add(new GuiButton(102, this.left + 292, this.top + 178, 92, 18, tr("gui.narutomod.mission.clue")));
				this.buttonList.add(new GuiButton(103, this.left + 390, this.top + 178, 92, 18, tr("gui.narutomod.mission.event")));
				this.buttonList.add(new GuiButton(104, this.left + 292, this.top + 200, 92, 18, tr("gui.narutomod.mission.rp_note")));
				this.buttonList.add(new GuiButton(105, this.left + 390, this.top + 200, 92, 18, tr("gui.narutomod.mission.arc")));
				this.buttonList.add(new GuiButton(106, this.left + 292, this.top + 236, 92, 18, tr("gui.narutomod.mission.promote")));
				this.buttonList.add(new GuiButton(107, this.left + 390, this.top + 236, 92, 18, "Nukenin"));
			}
		}

		private void addFields(String[] defaults) {
			for (int i = 0; i < this.labels.length; i++) {
				int col = i < 6 ? 0 : 1;
				int row = col == 0 ? i : i - 6;
				int x = this.left + 18 + col * 250;
				int y = this.top + 62 + row * 24;
				GuiTextField field = new GuiTextField(i, this.fontRenderer, x, y + 9, 230, 14);
				field.setMaxStringLength(220);
				field.setText(i < defaults.length ? defaults[i] : "");
				this.fields.add(field);
			}
		}

		@Override
		protected void actionPerformed(GuiButton button) throws IOException {
			if (button.id >= 10 && button.id <= 12) {
				this.tab = button.id - 10;
				initGui();
				return;
			}
			if (button.id == 100) sendAdmin(0, collect(0, this.fields.size()));
			else if (button.id == 101) sendAdmin(1, collect(0, this.fields.size()));
			else if (button.id >= 102 && button.id <= 105) sendAdmin(button.id - 100, collect(0, 3));
			else if (button.id == 106) sendAdmin(6, text(3) + SEP + text(4));
			else if (button.id == 107) sendAdmin(7, text(5) + SEP + text(6) + SEP + text(7) + SEP + text(8));
		}

		private void sendAdmin(int action, String payload) {
			NarutomodMod.PACKET_HANDLER.sendToServer(new MissionSystem.AdminActionMessage(action, payload));
		}

		private String collect(int start, int end) {
			StringBuilder builder = new StringBuilder();
			for (int i = start; i < end; i++) {
				if (i > start) builder.append(SEP);
				builder.append(text(i));
			}
			return builder.toString();
		}

		private String text(int index) {
			return index >= 0 && index < this.fields.size() ? this.fields.get(index).getText() : "";
		}

		@Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
			for (GuiTextField field : this.fields) field.textboxKeyTyped(typedChar, keyCode);
			super.keyTyped(typedChar, keyCode);
		}

		@Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
			super.mouseClicked(mouseX, mouseY, mouseButton);
			for (GuiTextField field : this.fields) field.mouseClicked(mouseX, mouseY, mouseButton);
		}

		@Override public void updateScreen() {
			for (GuiTextField field : this.fields) field.updateCursorCounter();
		}

		@Override
		public void drawScreen(int mouseX, int mouseY, float partialTicks) {
			drawDefaultBackground();
			drawRect(this.left, this.top, this.left + this.guiWidth, this.top + this.guiHeight, 0xFFE7D2A2);
			drawRect(this.left + 5, this.top + 5, this.left + this.guiWidth - 5, this.top + this.guiHeight - 5, 0xFF2B2017);
			drawRect(this.left + 8, this.top + 8, this.left + this.guiWidth - 8, this.top + this.guiHeight - 8, 0xFFF2E0B6);
			this.drawCenteredString(this.fontRenderer, TextFormatting.DARK_RED + tr("gui.narutomod.mission.admin.title"), this.left + this.guiWidth / 2, this.top + 12, 0xFFFFFF);
			for (int i = 0; i < this.fields.size(); i++) {
				int col = i < 6 ? 0 : 1;
				int row = col == 0 ? i : i - 6;
				int x = this.left + 18 + col * 250;
				int y = this.top + 62 + row * 24;
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + this.labels[i], x, y, 0xFFFFFF);
				this.fields.get(i).drawTextBox();
			}
			drawSummary();
			super.drawScreen(mouseX, mouseY, partialTicks);
		}

		private void drawSummary() {
			int y = this.top + 214;
			if (this.tab == 0) drawMultiline(tr("gui.narutomod.mission.recent_missions"), ClientState.adminMissions, this.left + 18, y, 340);
			else if (this.tab == 1) drawMultiline(tr("gui.narutomod.mission.recent_bingo"), ClientState.adminBingo, this.left + 18, y, 340);
			else drawMultiline(tr("gui.narutomod.mission.files"), tr("gui.narutomod.mission.clues") + ":\n" + ClientState.adminClues + "\n" + tr("gui.narutomod.mission.events") + ":\n" + ClientState.adminEvents + "\n" + tr("gui.narutomod.mission.arcs") + ":\n" + ClientState.adminArcs, this.left + 18, this.top + 214, 300);
		}

		private void drawMultiline(String title, String body, int x, int y, int width) {
			this.fontRenderer.drawString(TextFormatting.DARK_RED + title, x, y, 0xFFFFFF);
			if (body == null || body.isEmpty()) {
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.no_records"), x, y + 12, 0xFFFFFF);
				return;
			}
			String[] lines = body.split("\n");
			for (int i = 0; i < lines.length && i < 5; i++) {
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + this.fontRenderer.trimStringToWidth(lines[i], width), x, y + 12 + i * 10, 0xFFFFFF);
			}
		}

		@Override public boolean doesGuiPauseGame() { return false; }
	}

	public static class GuiMissionBoard extends GuiScreen {
		private boolean bingoTab;
		private String selectedRank = "D";
		private int left;
		private int top;
		private final int guiWidth = 470;
		private final int guiHeight = 324;

		public GuiMissionBoard(boolean bingoFirst) {
			this.bingoTab = bingoFirst;
		}

		public void refreshFromSync(boolean bingoFirst) {
			this.bingoTab = bingoFirst;
			initGui();
		}

		@Override
		public void initGui() {
			this.left = (this.width - this.guiWidth) / 2;
			this.top = (this.height - this.guiHeight) / 2;
			this.buttonList.clear();
			int x = this.left + 12;
			String[] ranks = MissionSystem.getMissionRanks();
			for (int i = 0; i < ranks.length; i++) {
				this.buttonList.add(new GuiButton(10 + i, x + i * 38, this.top + 34, 34, 18, ranks[i]));
			}
			this.buttonList.add(new GuiButton(20, this.left + 356, this.top + 34, 96, 18, "Bingo Book"));
			if (this.bingoTab) {
				for (int i = 0; i < ClientState.wanted.size() && i < 8; i++) {
					this.buttonList.add(new GuiButton(200 + i, this.left + 394, this.top + 70 + i * 25, 58, 18, tr("gui.narutomod.mission.hunt")));
				}
			} else {
				for (int i = 0; i < ClientState.customMissions.size() && i < 3; i++) {
					this.buttonList.add(new GuiButton(300 + i, this.left + 394, this.top + 72 + i * 34, 58, 18, tr("gui.narutomod.mission.take")));
				}
				this.buttonList.add(new GuiButton(5, this.left + 324, this.top + 288, 58, 18, tr("gui.narutomod.mission.file")));
				this.buttonList.add(new GuiButton(6, this.left + 388, this.top + 288, 64, 18, tr("gui.narutomod.mission.return")));
			}
		}

		@Override
		protected void actionPerformed(GuiButton button) throws IOException {
			String[] ranks = MissionSystem.getMissionRanks();
			if (button.id >= 10 && button.id < 10 + ranks.length) {
				this.selectedRank = ranks[button.id - 10];
				this.bingoTab = false;
				initGui();
			} else if (button.id == 20) {
				this.bingoTab = true;
				initGui();
			} else if (button.id >= 100 && button.id < 110) {
				NarutomodMod.PACKET_HANDLER.sendToServer(new MissionSystem.ActionMessage(0, this.selectedRank, button.id - 100, ""));
			} else if (button.id == 5) {
				NarutomodMod.PACKET_HANDLER.sendToServer(new MissionSystem.ActionMessage(1, "", 0, ""));
			} else if (button.id == 6) {
				NarutomodMod.PACKET_HANDLER.sendToServer(new MissionSystem.ActionMessage(2, "", 0, ""));
			} else if (button.id >= 200 && button.id < 210) {
				int index = button.id - 200;
				if (index < ClientState.wanted.size()) {
					NarutomodMod.PACKET_HANDLER.sendToServer(new MissionSystem.ActionMessage(3, "", 0, ClientState.wanted.get(index)[0]));
				}
			} else if (button.id >= 300 && button.id < 310) {
				int index = button.id - 300;
				if (index < ClientState.customMissions.size()) {
					NarutomodMod.PACKET_HANDLER.sendToServer(new MissionSystem.ActionMessage(4, "", 0, ClientState.customMissions.get(index)[0]));
				}
			}
		}

		@Override
		public void drawScreen(int mouseX, int mouseY, float partialTicks) {
			drawDefaultBackground();
			drawRect(this.left, this.top, this.left + this.guiWidth, this.top + this.guiHeight, 0xFFE7D2A2);
			drawRect(this.left + 5, this.top + 5, this.left + this.guiWidth - 5, this.top + this.guiHeight - 5, 0xFF2B2017);
			drawRect(this.left + 8, this.top + 8, this.left + this.guiWidth - 8, this.top + this.guiHeight - 8, 0xFFF2E0B6);
			this.drawCenteredString(this.fontRenderer, TextFormatting.DARK_RED + (this.bingoTab ? tr("gui.narutomod.mission.bingo_title") : tr("gui.narutomod.mission.board_title")), this.left + this.guiWidth / 2, this.top + 12, 0xFFFFFF);
			this.fontRenderer.drawString(TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.village_files"), this.left + 14, this.top + 24, 0xFFFFFF);
			this.fontRenderer.drawString(TextFormatting.DARK_GRAY + trim(tr("gui.narutomod.mission.player_line", ClientState.rank, ClientState.clan, stars(ClientState.reputation)), 260), this.left + 170, this.top + 24, 0xFFFFFF);
			if (this.bingoTab) drawBingo();
			else drawMissions();
			super.drawScreen(mouseX, mouseY, partialTicks);
		}

		private void drawMissions() {
			drawVillageOrders();
			drawActiveCase();
		}

		private void drawVillageOrders() {
			int headerY = this.top + 60;
			this.fontRenderer.drawString(TextFormatting.DARK_RED + tr("gui.narutomod.mission.orders"), this.left + 14, headerY, 0xFFFFFF);
			if (ClientState.customMissions.isEmpty()) {
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.no_orders"), this.left + 18, headerY + 14, 0xFFFFFF);
				return;
			}
			for (int i = 0; i < ClientState.customMissions.size() && i < 3; i++) {
				String[] p = ClientState.customMissions.get(i);
				int y = this.top + 74 + i * 34;
				drawRect(this.left + 14, y - 2, this.left + 386, y + 29, 0x44804020);
				this.fontRenderer.drawString(TextFormatting.GOLD + p[0] + " " + TextFormatting.DARK_RED + trim(p[2], 290), this.left + 18, y, 0xFFFFFF);
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + trim(p[3] + " | " + p[7] + "+ | " + p[5] + " Ryo | " + (parseInt(p[8]) / 3600) + "h", 340), this.left + 18, y + 10, 0xFFFFFF);
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + trim(p[4], 340), this.left + 18, y + 20, 0xFFFFFF);
			}
		}

		private void drawActiveCase() {
			int y = this.top + 265;
			drawRect(this.left + 14, y, this.left + 314, y + 45, 0x44FFFFFF);
			this.fontRenderer.drawString(TextFormatting.DARK_RED + tr("gui.narutomod.mission.active_scroll"), this.left + 18, y + 5, 0xFFFFFF);
			if (ClientState.active == null || ClientState.active.isEmpty()) {
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.no_active"), this.left + 18, y + 18, 0xFFFFFF);
				return;
			}
			String[] p = ClientState.active.split("\\|", -1);
			if (p.length < 10) return;
			this.fontRenderer.drawString(TextFormatting.GOLD + p[0] + " " + trim(p[2], 250), this.left + 18, y + 17, 0xFFFFFF);
			this.fontRenderer.drawString(TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.progress", p[5], p[6], parseInt(p[9]) / 60), this.left + 18, y + 29, 0xFFFFFF);
		}

		private void drawBingo() {
			this.fontRenderer.drawString(TextFormatting.DARK_RED + tr("gui.narutomod.mission.wanted_registry"), this.left + 14, this.top + 60, 0xFFFFFF);
			this.fontRenderer.drawString(TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.your_bounty", ClientState.bounty), this.left + 270, this.top + 60, 0xFFFFFF);
			if (!ClientState.activeBounty.isEmpty()) {
				this.fontRenderer.drawString(TextFormatting.RED + tr("gui.narutomod.mission.active_hunt", ClientState.activeBounty), this.left + 14, this.top + 292, 0xFFFFFF);
			}
			if (ClientState.wanted.isEmpty()) {
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.no_bingo"), this.left + 18, this.top + 84, 0xFFFFFF);
				return;
			}
			for (int i = 0; i < ClientState.wanted.size() && i < 8; i++) {
				String[] p = ClientState.wanted.get(i);
				int y = this.top + 72 + i * 25;
				drawRect(this.left + 14, y - 2, this.left + 386, y + 21, 0x44AA3333);
				this.fontRenderer.drawString(TextFormatting.DARK_RED + trim(p[0], 150) + TextFormatting.DARK_GRAY + tr("gui.narutomod.mission.threat_line", p[1], p[2]), this.left + 18, y, 0xFFFFFF);
				this.fontRenderer.drawString(TextFormatting.DARK_GRAY + trim(p[3] + " | " + p[4], 340), this.left + 18, y + 11, 0xFFFFFF);
			}
		}

		private String trim(String text, int maxPixels) {
			if (text == null) return "";
			return this.fontRenderer.trimStringToWidth(text, maxPixels);
		}

		private String stars(int reputation) {
			int count = Math.max(0, Math.min(5, reputation / 100));
			String s = "";
			for (int i = 0; i < count; i++) s += "*";
			return s.isEmpty() ? tr("gui.narutomod.mission.registered") : s;
		}

		@Override public boolean doesGuiPauseGame() { return false; }
	}

	private static int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}
	}

	private static class ClientState {
		private static String rank = "None";
		private static String clan = "None";
		private static int reputation;
		private static int bounty;
		private static String active = "";
		private static String activeBounty = "";
		private static List<String[]> wanted = new ArrayList<String[]>();
		private static List<String[]> customMissions = new ArrayList<String[]>();
		private static String adminMissions = "";
		private static String adminBingo = "";
		private static String adminClues = "";
		private static String adminEvents = "";
		private static String adminNotes = "";
		private static String adminArcs = "";
	}
}
