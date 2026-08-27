package net.narutomod.client;

import net.narutomod.EyeCustomization;
import net.narutomod.NarutomodMod;
import net.narutomod.item.ItemSharingan;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Character-creation style editor for fitting Sharingan eye textures to a skin. */
@SideOnly(Side.CLIENT)
public class GuiEyeCustomization extends GuiScreen implements GuiPageButtonList.GuiResponder {
	private static final int VERTICAL = 0;
	private static final int SAVE = 10;
	private static final int RESET = 11;
	private static final int CANCEL = 12;
	private static final Map<Integer, float[]> PENDING_SYNC = new HashMap<>();
	private static boolean initialized;
	private static boolean openEditorNextTick;

	private final float originalVertical;
	private float vertical;
	private GuiSlider verticalSlider;
	private boolean saved;

	public GuiEyeCustomization() {
		Entity player = Minecraft.getMinecraft().player;
		this.vertical = this.originalVertical = EyeCustomization.getVertical(player);
	}

	public static void initialize() {
		if (!initialized) {
			initialized = true;
			ClientCommandHandler.instance.registerCommand(new EyeCommand());
			FMLCommonHandler.instance().bus().register(new ClientSyncHook());
		}
	}

	public static void handleSync(int entityId, float horizontal, float vertical, float depth, float width, float height) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> applyOrQueue(entityId, new float[] { horizontal, vertical, depth, width, height }));
	}

	private static void applyOrQueue(int entityId, float[] values) {
		Minecraft mc = Minecraft.getMinecraft();
		Entity entity = mc.world == null ? null : mc.world.getEntityByID(entityId);
		if (entity == null) {
			PENDING_SYNC.put(entityId, values);
		} else {
			EyeCustomization.setClientValues(entity, values[0], values[1], values[2], values[3], values[4]);
			PENDING_SYNC.remove(entityId);
		}
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		int panelWidth = Math.min(420, this.width - 24);
		int left = (this.width - panelWidth) / 2;
		int controlX = left + 18;
		int controlWidth = Math.max(140, panelWidth / 2 - 28);
		int firstY = Math.max(72, this.height / 2 - 16);
		GuiSlider.FormatHelper formatter = (id, name, value) -> name + ": " + String.format(Locale.ROOT, "%.2f", value);

		this.verticalSlider = new GuiSlider(this, VERTICAL, controlX, firstY, "Eye height",
			EyeCustomization.MIN_VERTICAL, EyeCustomization.MAX_VERTICAL, this.vertical, formatter);
		this.verticalSlider.width = controlWidth;
		this.buttonList.add(this.verticalSlider);

		int buttonsY = Math.min(this.height - 32, firstY + 58);
		int buttonWidth = (panelWidth - 44) / 3;
		this.buttonList.add(new GuiButton(SAVE, left + 12, buttonsY, buttonWidth, 20, "Save"));
		this.buttonList.add(new GuiButton(RESET, left + 22 + buttonWidth, buttonsY, buttonWidth, 20, "Reset"));
		this.buttonList.add(new GuiButton(CANCEL, left + 32 + buttonWidth * 2, buttonsY, buttonWidth, 20, "Cancel"));
		preview();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		int panelWidth = Math.min(420, this.width - 24);
		int left = (this.width - panelWidth) / 2;
		int top = Math.max(14, this.height / 2 - 112);
		int bottom = Math.min(this.height - 10, this.height / 2 + 112);
		drawRect(left, top, left + panelWidth, bottom, 0xD016161B);
		drawRect(left, top, left + panelWidth, top + 2, 0xFFC52B32);
		this.drawCenteredString(this.fontRenderer, "Sharingan Eye Position", this.width / 2, top + 10, 0xFFFFFF);
		this.drawCenteredString(this.fontRenderer, "Move the Sharingan over your skin's eyes", this.width / 2, top + 23, 0xA9A9B0);

		if (this.mc.player != null) {
			int previewX = left + panelWidth * 3 / 4;
			int previewY = Math.min(bottom - 36, top + 157);
			GuiInventory.drawEntityOnScreen(previewX, previewY, 58, previewX - mouseX, previewY - 82 - mouseY, this.mc.player);
			if (!ItemSharingan.wearingAny(this.mc.player)) {
				this.drawCenteredString(this.fontRenderer, TextFormatting.GRAY + "Equip a Sharingan to preview it",
					previewX, bottom - 27, 0xA9A9B0);
			}
		}
		this.drawString(this.fontRenderer, "Drag the slider for a live preview", left + 18, bottom - 27, 0xA9A9B0);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (!button.enabled) return;
		if (button.id == SAVE) {
			this.saved = true;
			NarutomodMod.PACKET_HANDLER.sendToServer(new EyeCustomization.SaveMessage(
				0.0F, this.vertical, 0.0F, 1.0F, 1.0F));
			this.mc.displayGuiScreen(null);
		} else if (button.id == RESET) {
			this.vertical = 0.0F;
			this.verticalSlider.setSliderValue(this.vertical, false);
			preview();
		} else if (button.id == CANCEL) {
			this.mc.displayGuiScreen(null);
		}
	}

	@Override
	public void onGuiClosed() {
		if (!this.saved && this.mc.player != null) {
			EyeCustomization.setClientValues(this.mc.player, 0.0F, this.originalVertical, 0.0F, 1.0F, 1.0F);
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void setEntryValue(int id, boolean value) {
	}

	@Override
	public void setEntryValue(int id, float value) {
		if (id != VERTICAL) return;
		this.vertical = value;
		preview();
	}

	@Override
	public void setEntryValue(int id, String value) {
	}

	private void preview() {
		if (this.mc != null && this.mc.player != null) {
			EyeCustomization.setClientValues(this.mc.player, 0.0F, this.vertical, 0.0F, 1.0F, 1.0F);
		}
	}

	private static final class EyeCommand extends CommandBase {
		@Override
		public String getName() {
			return "eyes";
		}

		@Override
		public List<String> getAliases() {
			return Arrays.asList("eyepos", "eyeposition");
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return "/eyes";
		}

		@Override
		public int getRequiredPermissionLevel() {
			return 0;
		}

		@Override
		public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
			return true;
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			Minecraft mc = Minecraft.getMinecraft();
			if (mc.player == null) throw new CommandException("You must be in a world to edit your eyes.");
			// GuiChat closes itself after executing a command. Opening immediately would
			// make that close operation also dismiss this editor in the same frame.
			openEditorNextTick = true;
		}
	}

	private static final class ClientSyncHook {
		@SubscribeEvent
		public void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase != TickEvent.Phase.END) return;
			Minecraft mc = Minecraft.getMinecraft();
			if (openEditorNextTick && mc.player != null && mc.currentScreen == null) {
				openEditorNextTick = false;
				mc.displayGuiScreen(new GuiEyeCustomization());
			}
			if (PENDING_SYNC.isEmpty()) return;
			if (mc.world == null) return;
			Iterator<Map.Entry<Integer, float[]>> iterator = PENDING_SYNC.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Integer, float[]> entry = iterator.next();
				Entity entity = mc.world.getEntityByID(entry.getKey());
				if (entity != null) {
					float[] values = entry.getValue();
					EyeCustomization.setClientValues(entity, values[0], values[1], values[2], values[3], values[4]);
					iterator.remove();
				}
			}
		}

		@SubscribeEvent
		public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
			openEditorNextTick = false;
			PENDING_SYNC.clear();
		}
	}
}
