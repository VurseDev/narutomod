package net.narutomod.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.item.ItemDoton;
import net.narutomod.item.ItemExtraJutsuScrolls;
import net.narutomod.item.ItemInton;
import net.narutomod.item.ItemJutsu;
import net.narutomod.item.ItemKaton;
import net.narutomod.item.ItemNinjutsu;
import net.narutomod.item.ItemRaiton;
import net.narutomod.item.ItemSuiton;

@ElementsNarutomodMod.ModElement.Tag
public class GuiScrollExtraJutsu extends ElementsNarutomodMod.ModElement {
	public static final int GUIID_BASE = 9300;
	public static final int GUIID_LAST = GUIID_BASE + ItemExtraJutsuScrolls.SCROLLS.length - 1;

	public GuiScrollExtraJutsu(ElementsNarutomodMod instance) {
		super(instance, 1011);
	}

	public static boolean handles(int id) {
		return id >= GUIID_BASE && id <= GUIID_LAST;
	}

	public static class GuiContainerMod extends GuiNinjaScroll.GuiContainerMod {
		private final int scrollIndex;

		public GuiContainerMod(World world, int x, int y, int z, EntityPlayer player, int guiID) {
			super(world, x, y, z, player, guiID);
			this.scrollIndex = guiID - GUIID_BASE;
		}

		@Override
		protected void handleButtonAction(EntityPlayer player, int buttonID) {
			if (this.scrollIndex < 0 || this.scrollIndex >= ItemExtraJutsuScrolls.SCROLLS.length) {
				return;
			}
			ItemExtraJutsuScrolls.ScrollDef def = ItemExtraJutsuScrolls.SCROLLS[this.scrollIndex];
			ItemStack stack = null;
			if (def.kind == ItemExtraJutsuScrolls.Kind.NINJUTSU) {
				stack = GuiNinjaScroll.enableJutsu(player, (ItemNinjutsu.RangedItem)ItemNinjutsu.block, def.jutsu, true);
			} else if (def.kind == ItemExtraJutsuScrolls.Kind.KATON) {
				stack = GuiNinjaScroll.enableJutsu(player, (ItemKaton.RangedItem)ItemKaton.block, def.jutsu, true);
			} else if (def.kind == ItemExtraJutsuScrolls.Kind.SUITON) {
				stack = GuiNinjaScroll.enableJutsu(player, (ItemSuiton.RangedItem)ItemSuiton.block, def.jutsu, true);
			} else if (def.kind == ItemExtraJutsuScrolls.Kind.RAITON) {
				stack = GuiNinjaScroll.enableJutsu(player, (ItemRaiton.RangedItem)ItemRaiton.block, def.jutsu, true);
			} else if (def.kind == ItemExtraJutsuScrolls.Kind.DOTON) {
				stack = GuiNinjaScroll.enableJutsu(player, (ItemDoton.RangedItem)ItemDoton.block, def.jutsu, true);
			} else if (def.kind == ItemExtraJutsuScrolls.Kind.INTON) {
				stack = GuiNinjaScroll.enableJutsu(player, (ItemInton.RangedItem)ItemInton.block, def.jutsu, true);
			}
			if (stack != null) {
				super.handleButtonAction(player, buttonID);
			}
		}
	}

	public static class GuiWindow extends GuiNinjaScroll.GuiWindow {
		private final int scrollIndex;

		public GuiWindow(World world, int x, int y, int z, EntityPlayer entity, int guiID) {
			super(new GuiContainerMod(world, x, y, z, entity, guiID));
			this.scrollIndex = guiID - GUIID_BASE;
		}

		@Override
		protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
			super.drawGuiContainerBackgroundLayer(par1, par2, par3);
			ItemExtraJutsuScrolls.ScrollDef def = ItemExtraJutsuScrolls.SCROLLS[this.scrollIndex];
			this.mc.renderEngine.bindTexture(new ResourceLocation(def.iconTexture));
			this.drawModalRectWithCustomSizedTexture(this.guiLeft + 89, this.guiTop + 49, 0, 0, 48, 48, 48, 48);
		}

		@Override
		protected void drawGuiContainerForegroundLayer(int par1, int par2) {
			ItemJutsu.JutsuEnum jutsu = ItemExtraJutsuScrolls.SCROLLS[this.scrollIndex].jutsu;
			this.fontRenderer.drawString(jutsu.getName(), 18, 13, -16777216);
		}
	}
}
