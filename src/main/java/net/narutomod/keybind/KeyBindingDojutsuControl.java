package net.narutomod.keybind;

import net.narutomod.DojutsuControl;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.NarutomodMod;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import io.netty.buffer.ByteBuf;
import org.lwjgl.input.Keyboard;

import java.nio.charset.StandardCharsets;
import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class KeyBindingDojutsuControl extends ElementsNarutomodMod.ModElement {
	private KeyBinding toggleKey;
	private KeyBinding confirmKey;
	private int selectedIndex;
	private int confirmedIndex;
	private long showUntil;

	public KeyBindingDojutsuControl(ElementsNarutomodMod instance) {
		super(instance, 1025);
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		elements.addNetworkMessage(ToggleMessage.Handler.class, ToggleMessage.class, Side.SERVER);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void init(FMLInitializationEvent event) {
		this.toggleKey = new KeyBinding("key.mcreator.dojutsu_toggle", Keyboard.KEY_G, "key.mcreator.category");
		this.confirmKey = new KeyBinding("key.mcreator.dojutsu_confirm", Keyboard.KEY_RETURN, "key.mcreator.category");
		ClientRegistry.registerKeyBinding(this.toggleKey);
		ClientRegistry.registerKeyBinding(this.confirmKey);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.currentScreen != null || mc.player == null) {
			return;
		}
		List<ItemStack> available = DojutsuControl.getOwnedDojutsuStacks(mc.player);
		if (available.isEmpty()) {
			return;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			this.selectedIndex = Math.max(0, this.selectedIndex - 1);
			this.showUntil = mc.world.getTotalWorldTime() + 80;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			this.selectedIndex = Math.min(available.size() - 1, this.selectedIndex + 1);
			this.showUntil = mc.world.getTotalWorldTime() + 80;
		}
		this.selectedIndex = Math.min(this.selectedIndex, available.size() - 1);
		this.confirmedIndex = Math.min(this.confirmedIndex, available.size() - 1);
		if (this.confirmKey.isPressed()) {
			this.confirmedIndex = this.selectedIndex;
			this.showUntil = mc.world.getTotalWorldTime() + 100;
		}
		if (this.toggleKey.isPressed()) {
			Item item = available.get(this.confirmedIndex).getItem();
			ResourceLocation name = Item.REGISTRY.getNameForObject(item);
			if (name != null) {
				NarutomodMod.PACKET_HANDLER.sendToServer(new ToggleMessage(name.toString()));
				this.showUntil = mc.world.getTotalWorldTime() + 100;
			}
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onRenderHotbar(RenderGameOverlayEvent.Post event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null || mc.world == null) {
			return;
		}
		List<ItemStack> available = DojutsuControl.getOwnedDojutsuStacks(mc.player);
		if (available.isEmpty() || mc.world.getTotalWorldTime() > this.showUntil) {
			return;
		}
		this.selectedIndex = Math.min(this.selectedIndex, available.size() - 1);
		this.confirmedIndex = Math.min(this.confirmedIndex, available.size() - 1);
		ScaledResolution scaled = new ScaledResolution(mc);
		int x = scaled.getScaledWidth() / 2 + 98;
		int y = scaled.getScaledHeight() - 24;
		int visible = Math.min(4, available.size());
		int start = Math.max(0, Math.min(this.selectedIndex - 1, available.size() - visible));
		GlStateManager.pushMatrix();
		for (int i = 0; i < visible; i++) {
			int index = start + i;
			int slotX = x + i * 22;
			int color = index == this.confirmedIndex ? 0xAAE7D45B : 0xAA111111;
			if (index == this.selectedIndex) {
				color = 0xAAE32727;
			}
			Gui.drawRect(slotX, y, slotX + 20, y + 20, color);
			mc.getRenderItem().renderItemAndEffectIntoGUI(available.get(index), slotX + 2, y + 2);
		}
		String label = "Dojutsu: " + available.get(this.confirmedIndex).getDisplayName();
		mc.fontRenderer.drawStringWithShadow(label, x, y - 10, 0xFFFFFF);
		GlStateManager.popMatrix();
	}

	public static class ToggleMessage implements IMessage {
		private String registryName = "";

		public ToggleMessage() {
		}

		public ToggleMessage(String registryName) {
			this.registryName = registryName;
		}

		@Override
		public void toBytes(ByteBuf buf) {
			byte[] bytes = this.registryName.getBytes(StandardCharsets.UTF_8);
			buf.writeShort(bytes.length);
			buf.writeBytes(bytes);
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			int rawLength = buf.readUnsignedShort();
			int length = Math.min(128, rawLength);
			byte[] bytes = new byte[length];
			buf.readBytes(bytes);
			if (rawLength > length) {
				buf.skipBytes(rawLength - length);
			}
			this.registryName = new String(bytes, StandardCharsets.UTF_8);
		}

		public static class Handler implements IMessageHandler<ToggleMessage, IMessage> {
			@Override
			public IMessage onMessage(ToggleMessage message, MessageContext context) {
				EntityPlayerMP player = context.getServerHandler().player;
				player.getServerWorld().addScheduledTask(() -> DojutsuControl.toggleSelected(player, message.registryName));
				return null;
			}
		}
	}
}
