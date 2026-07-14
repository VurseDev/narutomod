package net.narutomod.creativetab;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.item.ItemSharinganTomoe3;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

@ElementsNarutomodMod.ModElement.Tag
public class TabCustomTabs extends ElementsNarutomodMod.ModElement {
	public static CreativeTabs eyes;
	public static CreativeTabs jutsus;

	public TabCustomTabs(ElementsNarutomodMod instance) {
		super(instance, 23);
	}

	@Override
	public void initElements() {
		eyes = new CreativeTabs("tabcustomeyes") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(ItemSharinganTomoe3.helmet == null ? Items.ENDER_EYE : ItemSharinganTomoe3.helmet);
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return true;
			}
		}.setBackgroundImageName("item_search.png");

		jutsus = new CreativeTabs("tabcustomjutsus") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				Item scroll = Item.REGISTRY.getObject(new ResourceLocation("narutomod", "scroll_fire_rasengan"));
				return new ItemStack(scroll == null ? Items.PAPER : scroll);
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return true;
			}
		}.setBackgroundImageName("item_search.png");
	}
}
