package net.narutomod.item;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.EyeCustomization;
import net.narutomod.creativetab.TabCustomTabs;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.client.model.ModelLoader;

import net.minecraft.advancements.Advancement;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The permanent human-eye baseline. Normal Eyes use the same snug 3D eye
 * geometry as dojutsu but deliberately are not a dojutsu themselves, keeping
 * them out of the G selection wheel.
 */
@ElementsNarutomodMod.ModElement.Tag
public class ItemNormalEyes extends ElementsNarutomodMod.ModElement {
	@ObjectHolder("narutomod:normal_eyes") public static final Item EYES = null;
	@ObjectHolder("narutomod:normal_eyes_blue") public static final Item EYES_BLUE = null;
	@ObjectHolder("narutomod:normal_eyes_green") public static final Item EYES_GREEN = null;
	@ObjectHolder("narutomod:normal_eyes_grey") public static final Item EYES_GREY = null;
	@ObjectHolder("narutomod:normal_eyes_amber") public static final Item EYES_AMBER = null;
	private static final String EYES_GRANTED = "NarutomodNormalEyesGranted";
	private static final String EYES_VARIANT = "NarutomodNormalEyesVariant";
	private static final String BLIND_FROM_MISSING_EYES = "NarutomodBlindFromMissingEyes";
	private static final ResourceLocation NINJA_ADVANCEMENT = new ResourceLocation("narutomod:ninjaachievement");

	public ItemNormalEyes(ElementsNarutomodMod instance) {
		super(instance, 1028);
	}

	@Override
	public void initElements() {
		ItemArmor.ArmorMaterial material = EnumHelper.addArmorMaterial("NORMAL_EYES", "narutomod:normal_eyes_",
			0, new int[]{0, 0, 0, 0}, 0, null, 0.0F);
		for (EyeColor color : EyeColor.values()) {
			this.elements.items.add(() -> new NormalEyesArmor(material, color)
				.setUnlocalizedName(color.registryName).setRegistryName(color.registryName).setCreativeTab(TabCustomTabs.eyes));
		}
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new PlayerHook());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		for (EyeColor color : EyeColor.values()) {
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("narutomod", color.registryName));
			if (item != null) ModelLoader.setCustomModelResourceLocation(item, 0,
				new ModelResourceLocation("narutomod:" + color.registryName, "inventory"));
		}
	}

	public static boolean isNormalEyes(ItemStack stack) {
		return !stack.isEmpty() && EyeColor.fromItem(stack.getItem()) != null;
	}

	public static EyeColor getEyeColor(ItemStack stack) {
		return stack.isEmpty() ? null : EyeColor.fromItem(stack.getItem());
	}

	public static boolean isWearingNormalEyes(EntityLivingBase entity) {
		return entity != null && isNormalEyes(entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
	}

	/** Sight comes only from the actual equipped eye, never from an item stored in inventory. */
	public static boolean hasSight(EntityLivingBase entity) {
		if (entity == null) return false;
		ItemStack head = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		return isNormalEyes(head) || head.getItem() instanceof ItemDojutsu.Base;
	}

	/** Clears only the blindness continuously maintained by the missing-eye rule. */
	public static void clearMissingEyeBlindness(EntityPlayer player) {
		if (player != null && player.getEntityData().getBoolean(BLIND_FROM_MISSING_EYES)) {
			player.getEntityData().setBoolean(BLIND_FROM_MISSING_EYES, false);
			player.removePotionEffect(MobEffects.BLINDNESS);
		}
	}

	public static boolean hasNinjaAdvancement(EntityPlayer player) {
		if (!(player instanceof EntityPlayerMP) || !(player.world instanceof WorldServer)) return false;
		Advancement advancement = ((WorldServer)player.world).getAdvancementManager().getAdvancement(NINJA_ADVANCEMENT);
		return advancement != null && ((EntityPlayerMP)player).getAdvancements().getProgress(advancement).isDone();
	}

	/** Removes the player's selected normal-eye colour from inventory for the G-wheel restore path. */
	public static ItemStack takeNormalEyes(EntityPlayer player) {
		ItemStack preferred = takeNormalEyes(player, getSavedEyeItem(player));
		return !preferred.isEmpty() ? preferred : takeNormalEyes(player, null);
	}

	private static ItemStack takeNormalEyes(EntityPlayer player, Item preferred) {
		for (int slot = 0; slot < player.inventory.mainInventory.size(); slot++) {
			ItemStack stack = player.inventory.mainInventory.get(slot);
			if (isNormalEyes(stack) && (preferred == null || stack.getItem() == preferred)) return take(player.inventory.mainInventory, slot);
		}
		for (int slot = 0; slot < player.inventory.armorInventory.size(); slot++) {
			ItemStack stack = player.inventory.armorInventory.get(slot);
			if (isNormalEyes(stack) && (preferred == null || stack.getItem() == preferred)) return take(player.inventory.armorInventory, slot);
		}
		for (int slot = 0; slot < player.inventory.offHandInventory.size(); slot++) {
			ItemStack stack = player.inventory.offHandInventory.get(slot);
			if (isNormalEyes(stack) && (preferred == null || stack.getItem() == preferred)) return take(player.inventory.offHandInventory, slot);
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack take(net.minecraft.util.NonNullList<ItemStack> inventory, int slot) {
		ItemStack source = inventory.get(slot);
		ItemStack result = source.copy();
		result.setCount(1);
		source.shrink(1);
		if (source.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
		return result;
	}

	private static void markGranted(EntityPlayer player) {
		player.getEntityData().setBoolean(EYES_GRANTED, true);
	}

	private static boolean hasBeenGranted(EntityPlayer player) {
		return player.getEntityData().getBoolean(EYES_GRANTED);
	}

	/** Saves the exact eye colour last worn, so it returns after G swaps and death. */
	public static void rememberEquippedEyes(EntityPlayer player, ItemStack stack) {
		EyeColor color = getEyeColor(stack);
		if (player != null && color != null) {
			markGranted(player);
			player.getEntityData().setString(EYES_VARIANT, color.name());
		}
	}

	private static Item getSavedEyeItem(EntityPlayer player) {
		EyeColor fallback = EyeColor.HAZEL;
		try {
			fallback = EyeColor.valueOf(player.getEntityData().getString(EYES_VARIANT));
		} catch (IllegalArgumentException ignored) {
		}
		Item item = fallback.getItem();
		return item == null ? EYES : item;
	}

	private static void giveOrEquipNormalEyes(EntityPlayer player) {
		if (EYES == null) return;
		markGranted(player);
		ItemStack head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (isNormalEyes(head) || head.getItem() instanceof ItemDojutsu.Base) return;
		ItemStack eyes = takeNormalEyes(player);
		if (eyes.isEmpty()) eyes = new ItemStack(getSavedEyeItem(player));
		if (!head.isEmpty()) ItemHandlerHelper.giveItemToPlayer(player, head.copy());
		player.setItemStackToSlot(EntityEquipmentSlot.HEAD, eyes);
		player.inventory.markDirty();
		clearMissingEyeBlindness(player);
	}

	private static void restoreAfterRespawn(EntityPlayer player) {
		if (hasNinjaAdvancement(player)) giveOrEquipNormalEyes(player);
	}

	private static class NormalEyesArmor extends ItemArmor {
		private final EyeColor color;
		@SideOnly(Side.CLIENT) private ItemDojutsu.ClientModel.ModelHelmetSnug armorModel;

		private NormalEyesArmor(ItemArmor.ArmorMaterial material, EyeColor colorIn) {
			super(material, 0, EntityEquipmentSlot.HEAD);
			this.color = colorIn;
			this.setMaxStackSize(1);
		}

		@Override
		public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
			if (!world.isRemote) {
				rememberEquippedEyes(player, stack);
				clearMissingEyeBlindness(player);
			}
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
			return "narutomod:textures/" + this.color.textureName + ".png";
		}

		@Override
		@SideOnly(Side.CLIENT)
		public ModelBiped getArmorModel(EntityLivingBase living, ItemStack stack, EntityEquipmentSlot slot, ModelBiped defaultModel) {
			if (this.armorModel == null) this.armorModel = new ItemDojutsu.ClientModel().new ModelHelmetSnug();
			this.armorModel.isSneak = living.isSneaking();
			this.armorModel.isRiding = living.isRiding();
			this.armorModel.isChild = living.isChild();
			this.armorModel.highlightHide = true;
			this.armorModel.eyeOffsetX = 0.0F;
			this.armorModel.eyeOffsetY = EyeCustomization.getVertical(living);
			this.armorModel.eyeOffsetZ = -0.025F;
			this.armorModel.eyeScaleX = 1.0F;
			this.armorModel.eyeScaleY = 1.0F;
			return this.armorModel;
		}
	}

	public static class PlayerHook {
		@SubscribeEvent
		public void onPlayerTick(TickEvent.PlayerTickEvent event) {
			if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || event.player.ticksExisted % 20 != 3) return;
			EntityPlayer player = event.player;
			if (hasNinjaAdvancement(player) && !hasBeenGranted(player)) giveOrEquipNormalEyes(player);
			if (isWearingNormalEyes(player)) rememberEquippedEyes(player, player.getItemStackFromSlot(EntityEquipmentSlot.HEAD));
			if (!hasBeenGranted(player)) return;
			if (!hasSight(player)) {
				player.getEntityData().setBoolean(BLIND_FROM_MISSING_EYES, true);
				player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 0, false, false));
			} else clearMissingEyeBlindness(player);
		}

		@SubscribeEvent
		public void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
			if (!event.player.world.isRemote) restoreAfterRespawn(event.player);
		}

		@SubscribeEvent
		public void onClone(PlayerEvent.Clone event) {
			if (event.getOriginal().getEntityData().getBoolean(EYES_GRANTED)) {
				event.getEntityPlayer().getEntityData().setBoolean(EYES_GRANTED, true);
			}
			if (event.getOriginal().getEntityData().hasKey(EYES_VARIANT)) {
				event.getEntityPlayer().getEntityData().setString(EYES_VARIANT,
					event.getOriginal().getEntityData().getString(EYES_VARIANT));
			}
		}
	}

	public enum EyeColor {
		HAZEL("normal_eyes", "Normal Eyes", "normal_eyes"),
		BLUE("normal_eyes_blue", "Blue Eyes", "normal_eyes_blue"),
		GREEN("normal_eyes_green", "Green Eyes", "normal_eyes_green"),
		GREY("normal_eyes_grey", "Grey Eyes", "normal_eyes_grey"),
		AMBER("normal_eyes_amber", "Amber Eyes", "normal_eyes_amber");

		public final String registryName;
		public final String displayName;
		public final String textureName;
		EyeColor(String registryNameIn, String displayNameIn, String textureNameIn) {
			this.registryName = registryNameIn;
			this.displayName = displayNameIn;
			this.textureName = textureNameIn;
		}

		public Item getItem() {
			return ForgeRegistries.ITEMS.getValue(new ResourceLocation("narutomod", this.registryName));
		}

		public static EyeColor fromItem(Item item) {
			if (item == null) return null;
			for (EyeColor color : values()) if (color.getItem() == item) return color;
			return null;
		}
	}
}
