package net.narutomod.keybind;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.NarutomodMod;
import net.narutomod.PlayerStats;
import net.narutomod.client.GuiPlayerStats;

import org.lwjgl.input.Keyboard;

@ElementsNarutomodMod.ModElement.Tag
public class KeyBindingStatsMenu extends ElementsNarutomodMod.ModElement {
	private KeyBinding key;

	public KeyBindingStatsMenu(ElementsNarutomodMod instance) {
		super(instance, 1003);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void init(FMLInitializationEvent event) {
		this.key = new KeyBinding("key.mcreator.statsmenu", Keyboard.KEY_V, "key.mcreator.category");
		ClientRegistry.registerKeyBinding(this.key);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.currentScreen == null && this.key.isPressed()) {
			NarutomodMod.PACKET_HANDLER.sendToServer(new PlayerStats.RequestSyncMessage());
			mc.displayGuiScreen(new GuiPlayerStats());
		}
	}
}
