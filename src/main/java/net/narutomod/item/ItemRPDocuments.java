package net.narutomod.item;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.PlayerStats;
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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ElementsNarutomodMod.ModElement.Tag
public class ItemRPDocuments extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:shinobi_id")
	public static final Item shinobiId = null;
	@GameRegistry.ObjectHolder("narutomod:village_passport")
	public static final Item villagePassport = null;

	public ItemRPDocuments(ElementsNarutomodMod instance) {
		super(instance, 1022);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new DocumentItem("shinobi_id", "id"));
		elements.items.add(() -> new DocumentItem("village_passport", "passport"));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(shinobiId, 0, new ModelResourceLocation("narutomod:shinobi_id", "inventory"));
		ModelLoader.setCustomModelResourceLocation(villagePassport, 0, new ModelResourceLocation("narutomod:village_passport", "inventory"));
	}

	public static ItemStack createDocument(String kind, EntityPlayerMP target, String village, String issuer, int expiresDays) {
		boolean passport = "passport".equalsIgnoreCase(kind);
		Item item = passport ? villagePassport : shinobiId;
		ItemStack stack = new ItemStack(item);
		NBTTagCompound tag = new NBTTagCompound();
		long now = System.currentTimeMillis();
		long expires = expiresDays > 0 ? now + expiresDays * 86400000L : 0L;
		tag.setString("DocKind", passport ? "passport" : "id");
		tag.setString("DocId", UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
		tag.setUniqueId("OwnerId", target.getUniqueID());
		tag.setString("OwnerName", target.getName());
		tag.setString("Village", village);
		tag.setString("Rank", PlayerStats.getRank(target));
		tag.setString("Clan", PlayerStats.getClan(target));
		tag.setString("Issuer", issuer);
		tag.setLong("IssuedAt", now);
		tag.setLong("ExpiresAt", expires);
		stack.setTagCompound(tag);
		stack.setStackDisplayName((passport ? "Passport" : "Shinobi ID") + " - " + target.getName());
		return stack;
	}

	public static String describe(ItemStack stack) {
		if (!stack.hasTagCompound()) {
			return TextFormatting.RED + "Blank document.";
		}
		NBTTagCompound tag = stack.getTagCompound();
		StringBuilder out = new StringBuilder();
		out.append(TextFormatting.GOLD).append(tag.getString("DocId")).append(TextFormatting.YELLOW)
		 .append(" | ").append(tag.getString("OwnerName")).append("\n");
		out.append(TextFormatting.YELLOW).append("Village: ").append(TextFormatting.WHITE).append(tag.getString("Village")).append("\n");
		out.append(TextFormatting.YELLOW).append("Rank: ").append(TextFormatting.WHITE).append(tag.getString("Rank")).append("\n");
		out.append(TextFormatting.YELLOW).append("Clan: ").append(TextFormatting.WHITE).append(tag.getString("Clan")).append("\n");
		out.append(TextFormatting.YELLOW).append("Issued by: ").append(TextFormatting.WHITE).append(tag.getString("Issuer")).append("\n");
		out.append(TextFormatting.YELLOW).append("Issued: ").append(TextFormatting.WHITE).append(formatDate(tag.getLong("IssuedAt"))).append("\n");
		long expires = tag.getLong("ExpiresAt");
		out.append(TextFormatting.YELLOW).append("Expires: ").append(TextFormatting.WHITE).append(expires <= 0L ? "Never" : formatDate(expires));
		return out.toString();
	}

	private static String formatDate(long millis) {
		return millis <= 0L ? "Unknown" : new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(millis));
	}

	public static class DocumentItem extends Item {
		private final String kind;

		public DocumentItem(String name, String kind) {
			this.kind = kind;
			setMaxDamage(0);
			maxStackSize = 1;
			setUnlocalizedName(name);
			setRegistryName(name);
			setCreativeTab(TabModTab.tab);
		}

		@Override
		public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
			ItemStack stack = player.getHeldItem(hand);
			if (!world.isRemote) {
				for (String line : describe(stack).split("\n")) {
					player.sendMessage(new TextComponentString(line));
				}
			}
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}

		@Override
		public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
			super.addInformation(stack, world, tooltip, flag);
			tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.narutomod.rp_document." + this.kind));
			if (stack.hasTagCompound()) {
				NBTTagCompound tag = stack.getTagCompound();
				tooltip.add(TextFormatting.YELLOW + tag.getString("OwnerName") + TextFormatting.GRAY + " | " + tag.getString("Village"));
				tooltip.add(TextFormatting.GRAY + tag.getString("Rank") + " / " + tag.getString("Clan"));
				long expires = tag.getLong("ExpiresAt");
				tooltip.add(TextFormatting.DARK_GRAY + "ID " + tag.getString("DocId") + " | " + (expires <= 0L ? "No expiry" : formatDate(expires)));
			} else {
				tooltip.add(TextFormatting.RED + I18n.format("tooltip.narutomod.rp_document.blank"));
			}
		}

		@Override
		public int getItemEnchantability() {
			return 0;
		}

		@Override
		public float getDestroySpeed(ItemStack stack, IBlockState state) {
			return 0F;
		}
	}
}
