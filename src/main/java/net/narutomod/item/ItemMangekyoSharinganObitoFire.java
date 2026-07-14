package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.world.World;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.Item;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.text.translation.I18n;

import net.narutomod.Chakra;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;

import java.util.List;
import javax.annotation.Nullable;

@ElementsNarutomodMod.ModElement.Tag
public class ItemMangekyoSharinganObitoFire extends ElementsNarutomodMod.ModElement {
	@ObjectHolder("narutomod:mangekyosharinganobitofirehelmet")
	public static final Item helmet = null;
	private static final float HIDING_IN_ASH_FULL_POWER = 25.0F;

	public ItemMangekyoSharinganObitoFire(ElementsNarutomodMod instance) {
		super(instance, 1001);
	}

	@Override
	public void initElements() {
		ItemArmor.ArmorMaterial enuma = EnumHelper.addArmorMaterial("MANGEKYOSHARINGANOBITOFIRE",
		 "narutomod:mangekyosharingan_obito_", 1024, new int[]{2, 5, 6, 10}, 0, null, 1.0F);
		this.elements.items.add(() -> new ItemSharingan.Base(enuma) {
			@Override
			public ItemSharingan.Type getSubType() {
				return ItemSharingan.Type.KAMUI;
			}

			@Override
			public boolean isMangekyo() {
				return true;
			}

			@Override
			public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
				return "narutomod:textures/mangekyosharinganhelmet_obito.png";
			}

			@Override
			public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
				super.addInformation(stack, worldIn, tooltip, flagIn);
				tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu1") + ": "
				 + TextFormatting.GRAY + ItemKaton.GREATFIREBALL.getName() + " (max)");
				tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu2") + ": "
				 + TextFormatting.GRAY + ItemKaton.GFANNIHILATION.getName() + " (max)");
				tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu3") + ": "
				 + TextFormatting.GRAY + ItemKaton.HIDINGINASH.getName() + " (max)");
			}

			@Override
			public String getItemStackDisplayName(ItemStack stack) {
				return TextFormatting.RED + super.getItemStackDisplayName(stack) + TextFormatting.WHITE;
			}

			@Override
			public boolean onJutsuKey1(boolean isPressed, ItemStack stack, EntityPlayer entity) {
				return this.castFireJutsuOnRelease(isPressed, stack, entity, ItemKaton.GREATFIREBALL);
			}

			@Override
			public boolean onJutsuKey2(boolean isPressed, ItemStack stack, EntityPlayer entity) {
				return this.castFireJutsuOnRelease(isPressed, stack, entity, ItemKaton.GFANNIHILATION);
			}

			@Override
			public boolean onJutsuKey3(boolean isPressed, ItemStack stack, EntityPlayer entity) {
				return this.castFireJutsuOnRelease(isPressed, stack, entity, ItemKaton.HIDINGINASH);
			}

			private boolean castFireJutsuOnRelease(boolean isPressed, ItemStack stack, EntityPlayer entity, ItemJutsu.JutsuEnum jutsu) {
				if (isPressed || entity.world.isRemote) {
					return true;
				}
				float power = jutsu == ItemKaton.HIDINGINASH ? HIDING_IN_ASH_FULL_POWER : jutsu.jutsu.getMaxPower(stack, entity);
				double chakraCost = jutsu.chakraUsage * power;
				if (!entity.isCreative()) {
					Chakra.Pathway pathway = Chakra.pathway(entity);
					if (pathway.getAmount() < chakraCost) {
						return true;
					}
					pathway.consume(chakraCost);
				}
				jutsu.jutsu.createJutsu(stack, entity, power);
				return true;
			}
		}.setUnlocalizedName("mangekyosharinganobitofirehelmet").setRegistryName("mangekyosharinganobitofirehelmet").setCreativeTab(TabCustomTabs.eyes));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(helmet, 0, new ModelResourceLocation("narutomod:mangekyosharinganobitofirehelmet", "inventory"));
	}
}
