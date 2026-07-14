package net.narutomod.client;

import net.narutomod.ElementalTraining;
import net.narutomod.NarutomodMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ElementalTrainingClient {
	private static final String[] SEAL_NAMES = {
		"Rat", "Ox", "Tiger", "Rabbit", "Dragon", "Snake",
		"Horse", "Ram", "Monkey", "Bird", "Dog", "Boar"
	};
	private static final int[] KEY_CODES = {
		Keyboard.KEY_Q, Keyboard.KEY_W, Keyboard.KEY_E, Keyboard.KEY_R, Keyboard.KEY_T, Keyboard.KEY_Y,
		Keyboard.KEY_U, Keyboard.KEY_I, Keyboard.KEY_O, Keyboard.KEY_P, Keyboard.KEY_A, Keyboard.KEY_S
	};
	private static final String[] KEY_NAMES = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "A", "S"};

	public static void open(final int element, final long session, final int mastery, final int sealXp) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override public void run() {
				Minecraft.getMinecraft().displayGuiScreen(new GuiTraining(element, session, mastery, sealXp));
			}
		});
	}

	private static class GuiTraining extends GuiScreen {
		private static final int REQUIRED_SEQUENCES = 5;
		private final int element;
		private final long session;
		private final int mastery;
		private final int sealXp;
		private final Random random;
		private final int[] keyForSeal = new int[SEAL_NAMES.length];
		private final String[] keyLabelForSeal = new String[SEAL_NAMES.length];
		private final List<int[]> sequences = new ArrayList<int[]>();
		private int sequenceIndex;
		private int sealIndex;
		private int sealTicks;
		private int sealLimit;
		private int totalReactionTicks;
		private int inputs;
		private boolean sent;
		private boolean success;
		private String failKey = "";
		private int closeTicks = -1;

		GuiTraining(int elementIn, long sessionIn, int masteryIn, int sealXpIn) {
			this.element = Math.max(0, Math.min(ElementalTraining.Element.values().length - 1, elementIn));
			this.session = sessionIn;
			this.mastery = masteryIn;
			this.sealXp = sealXpIn;
			this.random = new Random(sessionIn ^ (long)this.element * 341873128712L);
			this.makeMapping();
			this.makeSequences();
			this.resetSealTimer();
		}

		private void makeMapping() {
			List<Integer> keys = new ArrayList<Integer>();
			for (int key : KEY_CODES) keys.add(key);
			Collections.shuffle(keys, this.random);
			for (int i = 0; i < SEAL_NAMES.length; i++) {
				this.keyForSeal[i] = keys.get(i);
				this.keyLabelForSeal[i] = KEY_NAMES[indexOfKey(keys.get(i))];
			}
		}

		private int indexOfKey(int key) {
			for (int i = 0; i < KEY_CODES.length; i++) {
				if (KEY_CODES[i] == key) return i;
			}
			return 0;
		}

		private void makeSequences() {
			for (int i = 0; i < REQUIRED_SEQUENCES; i++) {
				this.sequences.add(this.randomSequence(chainLength()));
			}
		}

		private int chainLength() {
			if (this.mastery < 25) return 3;
			if (this.mastery < 50) return 4;
			if (this.mastery < 75) return 5;
			return 6 + this.random.nextInt(3);
		}

		private int[] randomSequence(int length) {
			int[][] themed = themedSequences();
			int[] base = themed[this.random.nextInt(themed.length)];
			int[] result = new int[length];
			for (int i = 0; i < length; i++) {
				result[i] = i < base.length ? base[i] : base[this.random.nextInt(base.length)];
			}
			for (int i = 0; i < result.length; i++) {
				int swap = this.random.nextInt(result.length);
				int old = result[i];
				result[i] = result[swap];
				result[swap] = old;
			}
			return result;
		}

		private int[][] themedSequences() {
			switch (ElementalTraining.Element.values()[this.element]) {
				case FIRE:
					return new int[][] {{2, 6, 10, 2, 5}, {2, 4, 5, 6, 2}, {6, 2, 9, 10, 5}};
				case WATER:
					return new int[][] {{5, 2, 3, 8, 6}, {5, 3, 11, 8, 2}, {8, 5, 6, 3, 2}};
				case LIGHTNING:
					return new int[][] {{5, 7, 8}, {8, 5, 7}, {7, 8, 2, 5}};
				case EARTH:
					return new int[][] {{10, 11, 7}, {11, 10, 4, 7}, {10, 7, 11, 1}};
				case WIND:
				default:
					return new int[][] {{2, 8, 9}, {9, 8, 5, 2}, {8, 2, 3, 9}};
			}
		}

		private void resetSealTimer() {
			int base = ElementalTraining.Element.values()[this.element] == ElementalTraining.Element.LIGHTNING ? 24
				: ElementalTraining.Element.values()[this.element] == ElementalTraining.Element.EARTH ? 36 : 30;
			base -= this.mastery >= 75 ? 6 : this.mastery >= 50 ? 4 : this.mastery >= 25 ? 2 : 0;
			base += Math.max(0, Math.min(4, this.sealXp / 250));
			this.sealLimit = Math.max(14, base + this.random.nextInt(7) - 3);
			this.sealTicks = this.sealLimit;
		}

		@Override
		public void updateScreen() {
			if (this.sent) {
				if (this.closeTicks > 0 && --this.closeTicks == 0) {
					Minecraft.getMinecraft().displayGuiScreen(null);
				}
				return;
			}
			if (--this.sealTicks <= 0) {
				this.fail("gui.narutomod.element_training.fail_timeout");
			}
		}

		@Override
		protected void keyTyped(char typedChar, int keyCode) throws IOException {
			if (this.sent) {
				if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE) {
					Minecraft.getMinecraft().displayGuiScreen(null);
				}
				return;
			}
			if (keyCode == Keyboard.KEY_ESCAPE) {
				this.fail("gui.narutomod.element_training.fail_cancelled");
				return;
			}
			int expectedSeal = this.currentSeal();
			if (keyCode == this.keyForSeal[expectedSeal]) {
				this.totalReactionTicks += this.sealLimit - this.sealTicks;
				this.inputs++;
				this.advance();
			} else if (keyCode > 0) {
				this.fail("gui.narutomod.element_training.fail_wrong");
			}
		}

		private int currentSeal() {
			return this.sequences.get(this.sequenceIndex)[this.sealIndex];
		}

		private void advance() {
			this.sealIndex++;
			if (this.sealIndex >= this.sequences.get(this.sequenceIndex).length) {
				this.sequenceIndex++;
				this.sealIndex = 0;
				if (this.sequenceIndex >= REQUIRED_SEQUENCES) {
					this.success = true;
					this.send(true);
					return;
				}
			}
			this.resetSealTimer();
		}

		private void fail(String key) {
			this.failKey = key;
			this.send(false);
		}

		private void send(boolean successIn) {
			this.sent = true;
			this.success = successIn;
			if (!successIn) {
				this.closeTicks = 40;
			}
			boolean perfect = successIn && this.inputs > 0 && (this.totalReactionTicks / (float)this.inputs) <= 7.0f;
			NarutomodMod.PACKET_HANDLER.sendToServer(new ElementalTraining.ResultMessage(this.element, this.session, successIn, perfect));
		}

		@Override
		public void onGuiClosed() {
			if (!this.sent) {
				this.send(false);
			}
		}

		@Override
		public void drawScreen(int mouseX, int mouseY, float partialTicks) {
			drawElementBackground();
			if (this.sent) {
				drawEndScreen();
				return;
			}
			int centerX = this.width / 2;
			int centerY = this.height / 2;
			String elementName = I18n.format("element.narutomod." + ElementalTraining.Element.values()[this.element].name().toLowerCase());
			this.drawCenteredString(this.fontRenderer, TextFormatting.BOLD + elementName + " " + I18n.format("gui.narutomod.element_training.training"), centerX, 28, 0xFFFFFF);
			this.drawCenteredString(this.fontRenderer, I18n.format("gui.narutomod.element_training.objective"), centerX, 46, 0xFFE9D8A8);
			int seal = this.currentSeal();
			this.drawCenteredString(this.fontRenderer, TextFormatting.BOLD + SEAL_NAMES[seal], centerX, centerY - 42, 0xFFFFFF);
			this.drawCenteredString(this.fontRenderer, I18n.format("gui.narutomod.element_training.press_key", this.keyLabelForSeal[seal]), centerX, centerY - 12, 0xFFFFDD66);
			drawTimer(centerX - 86, centerY + 18, 172, 10);
			this.drawCenteredString(this.fontRenderer, I18n.format("gui.narutomod.element_training.sequence", this.sequenceIndex + 1, REQUIRED_SEQUENCES), centerX, centerY + 38, 0xFFFFFFFF);
			drawSealPanel();
			super.drawScreen(mouseX, mouseY, partialTicks);
		}

		private void drawElementBackground() {
			int color = ElementalTraining.Element.values()[this.element].color;
			int r = (color >> 16) & 255;
			int g = (color >> 8) & 255;
			int b = color & 255;
			drawRect(0, 0, this.width, this.height, 0xF0000000 | (r / 5 << 16) | (g / 5 << 8) | (b / 5));
			for (int i = 0; i < 18; i++) {
				int x = (i * 73 + (int)(Minecraft.getSystemTime() / 28L)) % Math.max(1, this.width);
				int y = (i * 41) % Math.max(1, this.height);
				drawRect(x, y, x + 20 + i % 7, y + 2, 0x66333333 | (r << 16) | (g << 8) | b);
			}
		}

		private void drawTimer(int x, int y, int w, int h) {
			drawRect(x, y, x + w, y + h, 0xAA111111);
			int filled = (int)(w * (this.sealTicks / (float)this.sealLimit));
			drawRect(x + 1, y + 1, x + Math.max(1, filled - 1), y + h - 1, 0xFFFFCC44);
		}

		private void drawSealPanel() {
			int x = this.width - 152;
			int y = 46;
			drawRect(x - 8, y - 10, this.width - 12, y + 142, 0x99000000);
			this.fontRenderer.drawString(TextFormatting.GOLD + I18n.format("gui.narutomod.element_training.seals"), x, y - 2, 0xFFFFFF);
			for (int i = 0; i < SEAL_NAMES.length; i++) {
				this.fontRenderer.drawString(this.keyLabelForSeal[i] + " = " + SEAL_NAMES[i], x, y + 12 + i * 10, 0xFFE8E8E8);
			}
		}

		private void drawEndScreen() {
			String title = this.success ? I18n.format("gui.narutomod.element_training.complete") : I18n.format("gui.narutomod.element_training.failed");
			String body = this.success ? I18n.format("gui.narutomod.element_training.success_body") : I18n.format(this.failKey.isEmpty() ? "gui.narutomod.element_training.fail_wrong" : this.failKey);
			this.drawCenteredString(this.fontRenderer, TextFormatting.BOLD + title, this.width / 2, this.height / 2 - 16, this.success ? 0xFF77FF99 : 0xFFFF6666);
			this.drawCenteredString(this.fontRenderer, body, this.width / 2, this.height / 2 + 4, 0xFFFFFFFF);
		}

		@Override public boolean doesGuiPauseGame() { return false; }
	}
}
