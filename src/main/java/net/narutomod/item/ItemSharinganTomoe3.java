package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.world.World;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemArmor;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.ElementsNarutomodMod;

import java.util.List;
import javax.annotation.Nullable;

@ElementsNarutomodMod.ModElement.Tag
public class ItemSharinganTomoe3 extends ElementsNarutomodMod.ModElement {
	@ObjectHolder("narutomod:sharingan_3_tomoehelmet")
	public static final Item helmet = null;
	public ItemSharinganTomoe3(ElementsNarutomodMod instance) { super(instance, 1006); }

	@Override
	public void initElements() {
		ItemArmor.ArmorMaterial material = EnumHelper.addArmorMaterial("SHARINGAN_3_TOMOE", "narutomod:sharingan_3_tomoe_", 1024,
		 new int[]{2, 5, 6, 10}, 0, null, 0.0F);
		this.elements.items.add(() -> new ItemSharingan.Base(material) {
			@Override public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
				return "narutomod:textures/sharingan_3_tomoehelmet.png";
			}

			@Override
			public boolean onJutsuKey1(boolean isPressed, ItemStack stack, EntityPlayer player) {
				if (!isPressed) {
					ItemSharinganCopy.attemptCopy(player, stack);
				}
				return true;
			}

			@SideOnly(Side.CLIENT)
			@Override
			public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
				super.addInformation(stack, worldIn, tooltip, flagIn);
				tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu1") + ": " + TextFormatting.GRAY + I18n.translateToLocal("entity.sharingan_copy.name"));
				tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted("tooltip.narutomod.sharingan_copy.xp", ItemSharinganCopy.getCopyXp(stack), 3000));
			}
		}.setUnlocalizedName("sharingan_3_tomoehelmet").setRegistryName("sharingan_3_tomoehelmet").setCreativeTab(TabCustomTabs.eyes));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(helmet, 0, new ModelResourceLocation("narutomod:sharingan_3_tomoehelmet", "inventory"));
	}
}
