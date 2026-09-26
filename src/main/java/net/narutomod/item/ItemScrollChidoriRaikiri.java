package net.narutomod.item;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.gui.GuiNinjaScroll;

import java.util.List;

/** One-use learning scroll for the separate Nydo-inspired Chidori variant. */
@ElementsNarutomodMod.ModElement.Tag
public class ItemScrollChidoriRaikiri extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:scroll_chidori_raikiri")
	public static final Item block = null;

	public ItemScrollChidoriRaikiri(ElementsNarutomodMod instance) {
		super(instance, 1033);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new ItemCustom());
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(block, 0,
			new ModelResourceLocation("narutomod:scroll_chidori_raikiri", "inventory"));
	}

	public static class ItemCustom extends Item {
		public ItemCustom() {
			this.setMaxDamage(1);
			this.maxStackSize = 1;
			this.setUnlocalizedName("scroll_chidori_raikiri");
			this.setRegistryName("scroll_chidori_raikiri");
			this.setCreativeTab(null);
		}

		@Override
		public int getItemEnchantability() {
			return 0;
		}

		@Override
		public int getMaxItemUseDuration(ItemStack stack) {
			return 0;
		}

		@Override
		public float getDestroySpeed(ItemStack stack, IBlockState state) {
			return 0.0f;
		}

		@Override
		public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
			super.addInformation(stack, world, list, flag);
			list.add("A-rank Raiton jutsu scroll");
			list.add("Chidori: Raikiri — charge, dash, impact burst");
		}

		@Override
		public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
			ItemStack scroll = player.getHeldItem(hand);
			if (!world.isRemote && false) { // Retired scroll retained only for existing saves.
				ItemStack jutsu = GuiNinjaScroll.enableJutsu(player,
					(ItemJutsu.Base)ItemRaiton.block, ItemRaiton.CHIDORI_RAIKIRI, true);
				if (jutsu != null) {
					if (!player.capabilities.isCreativeMode) {
						scroll.shrink(1);
					}
					player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
						"Learned Chidori: Raikiri"), true);
					return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, scroll);
				}
			}
			return new ActionResult<ItemStack>(EnumActionResult.PASS, scroll);
		}
	}
}
