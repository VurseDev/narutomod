package net.narutomod.item;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.narutomod.Chakra;
import net.narutomod.ElementalTraining;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.procedure.ProcedureUtils;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

@ElementsNarutomodMod.ModElement.Tag
public class ItemSharinganCopy extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:copied_jutsu")
	public static final Item copied = null;

	private static final String LAST_ROOT = "NarutomodSharinganCopyLastJutsu";
	private static final String OWNER = "Owner";
	private static final String ORIGINAL_ITEM = "OriginalItem";
	private static final String JUTSU_NAME = "JutsuName";
	private static final String JUTSU_TYPE = "JutsuType";
	private static final String JUTSU_RANK = "JutsuRank";
	private static final String JUTSU_XP = "JutsuXp";
	private static final String EXPIRES = "Expires";
	private static final String COPY_XP = "SharinganCopyXp";
	private static final String COPY_CD = "SharinganCopyCooldown";
	private static final int COPY_COOLDOWN = 400 * 20;
	private static final int COPY_WINDOW = 5 * 20;
	private static final int STORAGE_TIME = 60 * 20;
	private static final int MASTERED_XP = 3000;

	public ItemSharinganCopy(ElementsNarutomodMod instance) {
		super(instance, 1020);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new CopiedJutsuItem());
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new Hook());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(copied, 0, new ModelResourceLocation("narutomod:copied_jutsu", "inventory"));
	}

	public static void recordJutsuUse(EntityPlayer player, ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
		if (player == null || player.world.isRemote || !(stack.getItem() instanceof ItemJutsu.Base) || jutsu == null) return;
		NBTTagCompound tag = player.getEntityData().getCompoundTag(LAST_ROOT);
		writeJutsu(tag, player, stack, jutsu);
		player.getEntityData().setTag(LAST_ROOT, tag);
	}

	private static void writeJutsu(NBTTagCompound tag, EntityPlayer player, ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
		ResourceLocation itemName = Item.REGISTRY.getNameForObject(stack.getItem());
		tag.setString(ORIGINAL_ITEM, itemName == null ? "" : itemName.toString());
		tag.setString(JUTSU_NAME, jutsu.unlocalizedName);
		tag.setString(JUTSU_TYPE, jutsu.getType() == null ? "" : jutsu.getType().name());
		tag.setString(JUTSU_RANK, String.valueOf(jutsu.rank));
		tag.setInteger(JUTSU_XP, ((ItemJutsu.Base)stack.getItem()).getJutsuXp(stack, jutsu));
		tag.setLong(EXPIRES, player.world.getTotalWorldTime() + COPY_WINDOW);
	}

	private static NBTTagCompound currentChargingJutsu(EntityPlayer target) {
		ItemStack active = target.getActiveItemStack();
		if (!active.isEmpty() && active.getItem() instanceof ItemJutsu.Base) {
			ItemJutsu.JutsuEnum jutsu = ItemJutsu.getCurrentJutsu(active);
			if (jutsu != null) {
				NBTTagCompound tag = new NBTTagCompound();
				writeJutsu(tag, target, active, jutsu);
				tag.setLong(EXPIRES, target.world.getTotalWorldTime() + COPY_WINDOW);
				return tag;
			}
		}
		return null;
	}

	@Nullable
	private static NBTTagCompound observedJutsu(EntityPlayer target) {
		NBTTagCompound charging = currentChargingJutsu(target);
		if (charging != null) return charging;
		NBTTagCompound tag = target.getEntityData().getCompoundTag(LAST_ROOT);
		return tag.hasKey(JUTSU_NAME) && target.world.getTotalWorldTime() <= tag.getLong(EXPIRES) ? tag : null;
	}

	@Nullable
	private static ItemJutsu.JutsuEnum findJutsu(NBTTagCompound tag) {
		String name = tag.getString(JUTSU_NAME);
		String type = tag.getString(JUTSU_TYPE);
		for (ItemJutsu.JutsuEnum jutsu : ItemJutsu.JutsuEnum.getJutsuList()) {
			if (jutsu.unlocalizedName.equals(name) && jutsu.getType() != null && jutsu.getType().name().equals(type)) {
				return jutsu;
			}
		}
		return null;
	}

	public static int getCopyXp(ItemStack sharinganStack) {
		return sharinganStack.hasTagCompound() ? sharinganStack.getTagCompound().getInteger(COPY_XP) : 0;
	}

	public static void addCopyXp(ItemStack sharinganStack, int amount) {
		if (sharinganStack.isEmpty() || sharinganStack.getItem() != ItemSharinganTomoe3.helmet) return;
		if (!sharinganStack.hasTagCompound()) sharinganStack.setTagCompound(new NBTTagCompound());
		sharinganStack.getTagCompound().setInteger(COPY_XP, Math.min(MASTERED_XP * 3, Math.max(0, getCopyXp(sharinganStack) + amount)));
	}

	public static float mastery(ItemStack sharinganStack) {
		return Math.min(1.0f, getCopyXp(sharinganStack) / (float)MASTERED_XP);
	}

	private static boolean wearingThreeTomoe(EntityPlayer player) {
		return player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == ItemSharinganTomoe3.helmet;
	}

	private static char maxRank(float mastery) {
		if (mastery >= 0.75f) return 'A';
		if (mastery >= 0.50f) return 'B';
		if (mastery >= 0.25f) return 'C';
		return 'D';
	}

	private static boolean rankAllowed(char rank, float mastery) {
		char max = maxRank(mastery);
		return rank == 'D' || (rank == 'C' && max != 'D') || (rank == 'B' && (max == 'B' || max == 'A')) || (rank == 'A' && max == 'A');
	}

	private static boolean typeAllowed(ItemJutsu.JutsuEnum jutsu) {
		if (jutsu == null || jutsu.getType() == null) return false;
		switch (jutsu.getType()) {
			case NINJUTSU:
			case DOTON:
			case FUTON:
			case KATON:
			case RAITON:
			case SUITON:
			case IRYO:
				return true;
			default:
				return false;
		}
	}

	private static boolean canCopy(ItemJutsu.JutsuEnum jutsu, float mastery) {
		if (jutsu == null || !rankAllowed(jutsu.rank, mastery) || !typeAllowed(jutsu)) return false;
		String name = jutsu.unlocalizedName.toLowerCase();
		return !name.contains("transform") && !name.contains("sage") && !name.contains("biju") && !name.contains("bijuu")
			&& !name.contains("tailed") && !name.contains("mangekyo") && !name.contains("amaterasu") && !name.contains("kamui")
			&& !name.contains("susanoo") && !name.contains("amenotejikara") && !name.contains("limbo")
			&& !name.contains("bug") && !name.contains("kikaichu") && !name.contains("puppet") && !name.contains("crow");
	}

	private static void removeOldCopies(EntityPlayer player) {
		for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
			ItemStack stack = player.inventory.mainInventory.get(i);
			if (!stack.isEmpty() && stack.getItem() == copied) {
				player.inventory.mainInventory.set(i, ItemStack.EMPTY);
			}
		}
		for (int i = 0; i < player.inventory.offHandInventory.size(); i++) {
			ItemStack stack = player.inventory.offHandInventory.get(i);
			if (!stack.isEmpty() && stack.getItem() == copied) {
				player.inventory.offHandInventory.set(i, ItemStack.EMPTY);
			}
		}
	}

	private static ItemStack makeCopiedStack(EntityPlayer owner, NBTTagCompound source, ItemJutsu.JutsuEnum jutsu) {
		ItemStack stack = new ItemStack(copied);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setUniqueId(OWNER, owner.getUniqueID());
		tag.setString("OwnerName", owner.getName());
		tag.setString(ORIGINAL_ITEM, source.getString(ORIGINAL_ITEM));
		tag.setString(JUTSU_NAME, source.getString(JUTSU_NAME));
		tag.setString(JUTSU_TYPE, source.getString(JUTSU_TYPE));
		tag.setString(JUTSU_RANK, source.getString(JUTSU_RANK));
		tag.setInteger(JUTSU_XP, Math.max(0, (int)(source.getInteger(JUTSU_XP) * 0.60f)));
		tag.setLong(EXPIRES, owner.world.getTotalWorldTime() + STORAGE_TIME);
		stack.setTagCompound(tag);
		stack.setStackDisplayName("Copied: " + jutsu.getName());
		return stack;
	}

	private static ItemStack makeTemporaryOriginal(EntityPlayer owner, ItemStack copiedStack, ItemJutsu.JutsuEnum jutsu) {
		Item item = Item.REGISTRY.getObject(new ResourceLocation(copiedStack.getTagCompound().getString(ORIGINAL_ITEM)));
		if (!(item instanceof ItemJutsu.Base)) return ItemStack.EMPTY;
		ItemStack temp = new ItemStack(item);
		ItemJutsu.Base base = (ItemJutsu.Base)item;
		base.setOwner(temp, owner);
		base.enableJutsu(temp, jutsu, true);
		base.setCurrentJutsu(temp, jutsu);
		base.addJutsuXp(temp, jutsu, copiedStack.getTagCompound().getInteger(JUTSU_XP));
		return temp;
	}

	private static boolean isOwner(ItemStack stack, EntityPlayer player) {
		return stack.hasTagCompound() && stack.getTagCompound().hasUniqueId(OWNER)
			&& stack.getTagCompound().getUniqueId(OWNER).equals(player.getUniqueID());
	}

	private static boolean expired(ItemStack stack, World world) {
		return stack.hasTagCompound() && stack.getTagCompound().getLong(EXPIRES) > 0
			&& world.getTotalWorldTime() > stack.getTagCompound().getLong(EXPIRES);
	}

	private static void redVisuals(EntityPlayer player) {
		player.world.playSound(null, player.posX, player.posY, player.posZ,
			SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:sharingansfx")), SoundCategory.PLAYERS, 1.0f, 1.1f);
		if (player.world instanceof WorldServer) {
			((WorldServer)player.world).spawnParticle(EnumParticleTypes.REDSTONE, player.posX, player.posY + 1.2d, player.posZ,
				48, 0.45d, 0.35d, 0.45d, 0.02d);
		}
	}

	public static boolean attemptCopy(EntityPlayer player, ItemStack sharinganStack) {
		if (player.world.isRemote || !(player instanceof EntityPlayerMP)) return true;
		if (sharinganStack.getItem() != ItemSharinganTomoe3.helmet || !((ItemDojutsu.Base)sharinganStack.getItem()).isOwner(sharinganStack, player)) {
			player.sendStatusMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.requires_3_tomoe"), true);
			return true;
		}
		if (!sharinganStack.hasTagCompound()) sharinganStack.setTagCompound(new NBTTagCompound());
		long cooldown = sharinganStack.getTagCompound().getLong(COPY_CD);
		if (cooldown > player.world.getTotalWorldTime()) {
			player.sendStatusMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.cooldown", (cooldown - player.world.getTotalWorldTime()) / 20), true);
			return true;
		}
		RayTraceResult look = ProcedureUtils.objectEntityLookingAt(player, 32d, 2.5d);
		if (look == null || !(look.entityHit instanceof EntityPlayer)) {
			player.sendStatusMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.no_technique"), true);
			sharinganStack.getTagCompound().setLong(COPY_CD, player.world.getTotalWorldTime() + COPY_COOLDOWN);
			return true;
		}
		EntityPlayer target = (EntityPlayer)look.entityHit;
		NBTTagCompound observed = observedJutsu(target);
		ItemJutsu.JutsuEnum jutsu = observed == null ? null : findJutsu(observed);
		float mastery = mastery(sharinganStack);
		if (!canCopy(jutsu, mastery)) {
			player.sendStatusMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.cannot_copy"), true);
			sharinganStack.getTagCompound().setLong(COPY_CD, player.world.getTotalWorldTime() + COPY_COOLDOWN);
			return true;
		}
		double cost = Math.max(150d, Chakra.pathway(player).getAmount() * 0.35d);
		if (!Chakra.pathway(player).consume(cost)) {
			Chakra.pathway(player).warningDisplay();
			return true;
		}
		float chance = 0.70f + mastery * 0.20f;
		sharinganStack.getTagCompound().setLong(COPY_CD, player.world.getTotalWorldTime() + COPY_COOLDOWN);
		if (player.getRNG().nextFloat() > chance) {
			player.sendMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.failed"));
			return true;
		}
		removeOldCopies(player);
		ItemStack copiedStack = makeCopiedStack(player, observed, jutsu);
		ItemHandlerHelper.giveItemToPlayer(player, copiedStack);
		redVisuals(player);
		player.sendMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.copied", jutsu.getName()));
		target.sendMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.analyzed"));
		addCopyXp(sharinganStack, 6);
		return true;
	}

	public static class CopiedJutsuItem extends Item {
		public CopiedJutsuItem() {
			this.setUnlocalizedName("copied_jutsu");
			this.setRegistryName("copied_jutsu");
			this.setCreativeTab(TabCustomTabs.jutsus);
			this.setMaxStackSize(1);
			this.setFull3D();
		}

		@Override
		public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
			ItemStack stack = player.getHeldItem(hand);
			if (!stack.hasTagCompound() || !isOwner(stack, player) || expired(stack, world)) {
				if (!world.isRemote) player.sendStatusMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.not_owner_or_expired"), true);
				if (!world.isRemote && expired(stack, world)) stack.shrink(1);
				return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
			}
			player.setActiveHand(hand);
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}

		@Override
		public void onUsingTick(ItemStack stack, net.minecraft.entity.EntityLivingBase player, int timeLeft) {
			if (!player.world.isRemote && player instanceof EntityPlayer && stack.hasTagCompound()) {
				ItemJutsu.JutsuEnum jutsu = findJutsu(stack.getTagCompound());
				ItemStack temp = jutsu == null ? ItemStack.EMPTY : makeTemporaryOriginal((EntityPlayer)player, stack, jutsu);
				if (!temp.isEmpty()) {
					jutsu.jutsu.onUsingTick(temp, player, ((ItemJutsu.Base)temp.getItem()).getPower(temp, player, timeLeft));
				}
			}
		}

		@Override
		public void onPlayerStoppedUsing(ItemStack stack, World world, net.minecraft.entity.EntityLivingBase entity, int timeLeft) {
			if (world.isRemote || !(entity instanceof EntityPlayer)) return;
			EntityPlayer player = (EntityPlayer)entity;
			if (!stack.hasTagCompound() || !isOwner(stack, player) || expired(stack, world)) {
				stack.shrink(1);
				return;
			}
			ItemJutsu.JutsuEnum jutsu = findJutsu(stack.getTagCompound());
			ItemStack temp = jutsu == null ? ItemStack.EMPTY : makeTemporaryOriginal(player, stack, jutsu);
			if (temp.isEmpty()) {
				stack.shrink(1);
				return;
			}
			ItemJutsu.Base base = (ItemJutsu.Base)temp.getItem();
			float power = base.getPower(temp, entity, timeLeft);
			double cost = Math.max(jutsu.chakraUsage * 0.70d, jutsu.chakraUsage * power);
			if (jutsu.chakraUsage > 0d && Chakra.pathway(entity).getAmount() < cost) {
				Chakra.pathway(entity).warningDisplay();
				return;
			}
			if (!ItemByakugan.canUseJutsuUnderTenketsu(entity, jutsu)) {
				Chakra.pathway(entity).warningDisplay();
				return;
			}
			if (!ElementalTraining.isElementUnlocked(player, jutsu.getType())) {
				player.sendStatusMessage(new TextComponentTranslation("message.narutomod.element_training.locked", ElementalTraining.getElementName(jutsu.getType())), true);
				return;
			}
			if (jutsu.jutsu.createJutsu(temp, entity, power)) {
				Chakra.pathway(entity).consume(cost);
				stack.shrink(1);
				player.sendStatusMessage(new TextComponentTranslation("message.narutomod.sharingan_copy.forgotten"), true);
			}
		}

		@Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.BOW; }
		@Override public int getMaxItemUseDuration(ItemStack stack) { return 72000; }
		@Override public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) { return true; }

		@Override
		public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
			if (!world.isRemote && expired(stack, world)) stack.shrink(1);
		}

		@Override
		public boolean onEntityItemUpdate(EntityItem entityItem) {
			ItemStack stack = entityItem.getItem();
			if (!entityItem.world.isRemote && expired(stack, entityItem.world)) {
				entityItem.setDead();
				return true;
			}
			return false;
		}

		@SideOnly(Side.CLIENT)
		@Override
		public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
			if (stack.hasTagCompound()) {
				ItemJutsu.JutsuEnum jutsu = findJutsu(stack.getTagCompound());
				tooltip.add(TextFormatting.RED + I18n.format("tooltip.narutomod.copied_jutsu.technique"));
				tooltip.add(TextFormatting.GRAY + (jutsu == null ? stack.getTagCompound().getString(JUTSU_NAME) : jutsu.getName()));
				tooltip.add(TextFormatting.GRAY + I18n.format("tooltip.narutomod.copied_jutsu.stored_xp", stack.getTagCompound().getInteger(JUTSU_XP)));
			}
		}
	}

	public static class Hook {
		@SubscribeEvent
		public void onPlayerHit(LivingHurtEvent event) {
			if (event.getEntityLiving().world.isRemote || !(event.getEntityLiving() instanceof EntityPlayer)) return;
			if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
			EntityPlayer attacker = (EntityPlayer)event.getSource().getTrueSource();
			if (!wearingThreeTomoe(attacker)) return;
			addCopyXp(attacker.getItemStackFromSlot(EntityEquipmentSlot.HEAD), 1);
		}
	}
}
