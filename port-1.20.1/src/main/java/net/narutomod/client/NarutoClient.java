package net.narutomod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.narutomod.NarutoMod;
import org.lwjgl.glfw.GLFW;

public final class NarutoClient {
    private NarutoClient() {
    }

    public static KeyMapping statsMenuKey;

    @Mod.EventBusSubscriber(modid = NarutoMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            statsMenuKey = new KeyMapping("key.narutomod.stats_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.narutomod");
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            if (statsMenuKey != null) {
                event.register(statsMenuKey);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = NarutoMod.MODID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || statsMenuKey == null) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            while (statsMenuKey.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new StatsScreen());
                }
            }
        }
    }

    public static class StatsScreen extends Screen {
        private static final String[] STAT_NAMES = {"Speed", "Strength", "Resistance", "Health", "Chakra Max", "SPI"};

        protected StatsScreen() {
            super(Component.translatable("screen.narutomod.stats"));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics);
            int left = (this.width - 256) / 2;
            int top = (this.height - 192) / 2;
            graphics.fill(left, top, left + 256, top + 192, 0xEE101010);
            graphics.fill(left + 6, top + 6, left + 250, top + 186, 0xAA2A160D);
            graphics.drawCenteredString(this.font, this.title, left + 128, top + 12, 0xFFFFCC55);
            graphics.drawString(this.font, "Port screen active - stat sync comes next.", left + 18, top + 30, 0xFFE6D39A, false);
            for (int i = 0; i < STAT_NAMES.length; i++) {
                int y = top + 52 + i * 18;
                graphics.drawString(this.font, STAT_NAMES[i], left + 18, y, 0xFFFFFFFF, false);
                graphics.drawString(this.font, "0", left + 150, y, 0xFFFFCC66, false);
                graphics.drawString(this.font, "+", left + 196, y, 0xFF66FF66, false);
            }
            graphics.drawString(this.font, "SPI will reduce jutsu charge time.", left + 18, top + 166, 0xFFB9B9B9, false);
            LivingEntity player = Minecraft.getInstance().player;
            if (player != null) {
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, left + 106, top + 152, 45, left + 106 - mouseX, top + 74 - mouseY, player);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
