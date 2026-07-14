package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemArmor;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.ElementsNarutomodMod;

@ElementsNarutomodMod.ModElement.Tag
public class ItemSharinganTomoe2 extends ElementsNarutomodMod.ModElement {
	@ObjectHolder("narutomod:sharingan_2_tomoehelmet")
	public static final Item helmet = null;
	public ItemSharinganTomoe2(ElementsNarutomodMod instance) { super(instance, 1005); }

	@Override
	public void initElements() {
		ItemArmor.ArmorMaterial material = EnumHelper.addArmorMaterial("SHARINGAN_2_TOMOE", "narutomod:sharingan_2_tomoe_", 1024,
		 new int[]{2, 5, 6, 10}, 0, null, 0.0F);
		this.elements.items.add(() -> new ItemSharingan.Base(material) {
			@Override public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
				return "narutomod:textures/sharingan_2_tomoehelmet.png";
			}
		}.setUnlocalizedName("sharingan_2_tomoehelmet").setRegistryName("sharingan_2_tomoehelmet").setCreativeTab(TabCustomTabs.eyes));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(helmet, 0, new ModelResourceLocation("narutomod:sharingan_2_tomoehelmet", "inventory"));
	}
}
