package net.narutomod.item;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.MissionSystem;
import net.narutomod.creativetab.TabModTab;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class ItemMissionPaperwork extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:mission_board")
	public static final Item missionBoard = null;
	@GameRegistry.ObjectHolder("narutomod:bingo_book")
	public static final Item bingoBook = null;

	public ItemMissionPaperwork(ElementsNarutomodMod instance) {
		super(instance, 1017);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new ItemCustom("mission_board", false));
		elements.items.add(() -> new ItemCustom("bingo_book", true));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(missionBoard, 0, new ModelResourceLocation("narutomod:mission_board", "inventory"));
		ModelLoader.setCustomModelResourceLocation(bingoBook, 0, new ModelResourceLocation("narutomod:bingo_book", "inventory"));
	}

	public static class ItemCustom extends Item {
		private final boolean bingo;

		public ItemCustom(String name, boolean bingo) {
			this.bingo = bingo;
			setMaxDamage(0);
			maxStackSize = 1;
			setUnlocalizedName(name);
			setRegistryName(name);
			setCreativeTab(TabModTab.tab);
		}

		@Override
		public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
			ItemStack stack = player.getHeldItem(hand);
			if (world.isRemote) {
				openClient(this.bingo);
			}
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}

		@SideOnly(Side.CLIENT)
		private void openClient(boolean bingo) {
			MissionSystem.openClient(bingo);
			MissionSystem.requestClientSync(bingo);
		}

		@Override
		public int getItemEnchantability() {
			return 0;
		}

		@Override
		public int getMaxItemUseDuration(ItemStack itemstack) {
			return 0;
		}

		@Override
		public float getDestroySpeed(ItemStack stack, IBlockState state) {
			return 0F;
		}

		@Override
		public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
			super.addInformation(stack, world, tooltip, flag);
			if (this.bingo) {
				tooltip.add(TextFormatting.DARK_RED + I18n.format("tooltip.narutomod.bingo_book.ledger"));
				tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.narutomod.bingo_book.hunts"));
			} else {
				tooltip.add(TextFormatting.GOLD + I18n.format("tooltip.narutomod.mission_board.paperwork"));
				tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.narutomod.mission_board.assignments"));
			}
		}
	}
}
