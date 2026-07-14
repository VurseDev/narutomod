package net.narutomod.item;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
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
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.NarutomodMod;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.gui.GuiScrollExtraJutsu;

import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class ItemExtraJutsuScrolls extends ElementsNarutomodMod.ModElement {
	public enum Kind { NINJUTSU, KATON, SUITON, RAITON, DOTON, INTON }

	public static final ScrollDef[] SCROLLS = new ScrollDef[] {
		new ScrollDef("scroll_fire_rasengan", Kind.NINJUTSU, ItemNinjutsu.FIRERASENGAN, "tooltip.narutomod.scroll.rank_a", "narutomod:textures/blocks/ninjutsu.png"),
		new ScrollDef("scroll_fire_phoenix", Kind.KATON, ItemKaton.FIREPHOENIX, "tooltip.narutomod.scroll.rank_b", "narutomod:textures/blocks/katon.png"),
		new ScrollDef("scroll_housenka", Kind.KATON, ItemKaton.HOUSENKA, "tooltip.narutomod.scroll.rank_d", "narutomod:textures/blocks/katon.png"),
		new ScrollDef("scroll_housenka_tsumabeni", Kind.KATON, ItemKaton.HOUSENKATSUMABENI, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/katon.png"),
		new ScrollDef("scroll_water_clone", Kind.SUITON, ItemSuiton.WATERCLONE, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/suiton.png"),
		new ScrollDef("scroll_water_prison_trap", Kind.SUITON, ItemSuiton.WATERPRISONTRAP, "tooltip.narutomod.scroll.rank_b", "narutomod:textures/blocks/suiton.png"),
		new ScrollDef("scroll_mizuame_nabara", Kind.SUITON, ItemSuiton.MIZUAMENABARA, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/suiton.png"),
		new ScrollDef("scroll_water_wall", Kind.SUITON, ItemSuiton.WATERWALL, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/suiton.png"),
		new ScrollDef("scroll_crow_escape", Kind.NINJUTSU, ItemNinjutsu.CROWCLONE, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/ninjutsu.png"),
		new ScrollDef("scroll_crow_trap_clone", Kind.NINJUTSU, ItemNinjutsu.CROWTRAPCLONE, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/ninjutsu.png"),
		new ScrollDef("scroll_explosive_clone", Kind.NINJUTSU, ItemNinjutsu.EXPLOSIVECLONE, "tooltip.narutomod.scroll.rank_b", "narutomod:textures/blocks/ninjutsu.png"),
		new ScrollDef("scroll_shuriken_shadow_clone", Kind.NINJUTSU, ItemNinjutsu.SHURIKENSHADOWCLONE, "tooltip.narutomod.scroll.rank_b", "narutomod:textures/blocks/ninjutsu.png"),
		new ScrollDef("scroll_sensorial_jutsu", Kind.NINJUTSU, ItemNinjutsu.SENSORIAL, "tooltip.narutomod.scroll.rank_s", "narutomod:textures/blocks/ninjutsu.png"),
		new ScrollDef("scroll_lightning_clone", Kind.RAITON, ItemRaiton.LIGHTNINGCLONE, "tooltip.narutomod.scroll.rank_a", "narutomod:textures/blocks/raiton.png"),
		new ScrollDef("scroll_chidori_senbon", Kind.RAITON, ItemRaiton.CHIDORISENBON, "tooltip.narutomod.scroll.rank_a", "narutomod:textures/blocks/raiton.png"),
		new ScrollDef("scroll_retsudo_tensho", Kind.DOTON, ItemDoton.RETSUDOTENSHO, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/doton.png")
		,new ScrollDef("scroll_false_opening", Kind.INTON, ItemInton.FALSE_OPENING, "tooltip.narutomod.scroll.rank_c", "narutomod:textures/blocks/inton.png")
		,new ScrollDef("scroll_memory_fracture", Kind.INTON, ItemInton.MEMORY_FRACTURE, "tooltip.narutomod.scroll.rank_b", "narutomod:textures/blocks/inton.png")
		,new ScrollDef("scroll_murder_intent", Kind.INTON, ItemInton.MURDER_INTENT, "tooltip.narutomod.scroll.rank_b", "narutomod:textures/blocks/inton.png")
		,new ScrollDef("scroll_illusionary_execution", Kind.INTON, ItemInton.ILLUSIONARY_EXECUTION, "tooltip.narutomod.scroll.rank_a", "narutomod:textures/blocks/inton.png")
		,new ScrollDef("scroll_burning_coffin", Kind.INTON, ItemInton.BURNING_COFFIN, "tooltip.narutomod.scroll.rank_a", "narutomod:textures/blocks/inton.png")
	};

	public ItemExtraJutsuScrolls(ElementsNarutomodMod instance) {
		super(instance, 1010);
	}

	@Override
	public void initElements() {
		for (int i = 0; i < SCROLLS.length; i++) {
			final int index = i;
			this.elements.items.add(() -> new ItemCustom(index));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		for (ScrollDef def : SCROLLS) {
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("narutomod", def.registryName));
			if (item != null) {
				ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation("narutomod:" + def.registryName, "inventory"));
			}
		}
	}

	public static class ScrollDef {
		public final String registryName;
		public final Kind kind;
		public final ItemJutsu.JutsuEnum jutsu;
		public final String tooltipKey;
		public final String iconTexture;

		public ScrollDef(String registryName, Kind kind, ItemJutsu.JutsuEnum jutsu, String tooltip, String iconTexture) {
			this.registryName = registryName;
			this.kind = kind;
			this.jutsu = jutsu;
			this.tooltipKey = tooltip;
			this.iconTexture = iconTexture;
		}
	}

	public static class ItemCustom extends Item {
		private final int scrollIndex;

		public ItemCustom(int scrollIndexIn) {
			this.scrollIndex = scrollIndexIn;
			ScrollDef def = SCROLLS[scrollIndexIn];
			this.setMaxDamage(1);
			this.maxStackSize = 1;
			this.setUnlocalizedName(def.registryName);
			this.setRegistryName(def.registryName);
			this.setCreativeTab(TabCustomTabs.jutsus);
		}

		@Override public int getItemEnchantability() { return 0; }
		@Override public int getMaxItemUseDuration(ItemStack itemstack) { return 0; }
		@Override public float getDestroySpeed(ItemStack stack, IBlockState state) { return 0F; }

		@Override
		public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
			super.addInformation(stack, world, list, flag);
			list.add(I18n.format(SCROLLS[this.scrollIndex].tooltipKey));
		}

		@Override
		public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer entity, EnumHand hand) {
			ActionResult<ItemStack> ar = super.onItemRightClick(world, entity, hand);
			entity.openGui(NarutomodMod.instance, GuiScrollExtraJutsu.GUIID_BASE + this.scrollIndex, world,
			 (int)entity.posX, (int)entity.posY, (int)entity.posZ);
			return ar;
		}
	}
}
