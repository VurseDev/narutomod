package net.narutomod;

import net.narutomod.item.ItemByakugan;
import net.narutomod.item.ItemDojutsu;
import net.narutomod.item.ItemMangekyoSharingan;
import net.narutomod.item.ItemMangekyoSharinganEternal;
import net.narutomod.item.ItemMangekyoSharinganObito;
import net.narutomod.item.ItemMangekyoSharinganObitoFire;
import net.narutomod.item.ItemRinnegan;
import net.narutomod.item.ItemSharingan;
import net.narutomod.item.ItemSharinganTomoe1;
import net.narutomod.item.ItemSharinganTomoe2;
import net.narutomod.item.ItemSharinganTomoe3;
import net.narutomod.item.ItemTenseigan;
import net.narutomod.procedure.ProcedureUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.NonNullList;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class DojutsuControl extends ElementsNarutomodMod.ModElement {
	private static final String ACTIVE_TAG = "NarutoDojutsuControlActive";
	private static final String ACTIVATION_COOLDOWN_TAG = "NarutoDojutsuActivationCooldown";
	private static final int ACTIVATION_COOLDOWN_TICKS = 20 * 20;

	public DojutsuControl(ElementsNarutomodMod instance) {
		super(instance, 1024);
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || event.player.ticksExisted % 20 != 7) {
			return;
		}
		ItemStack head = event.player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (isControlledActive(head)) {
			double cost = getDrainPerSecond(head);
			if (!Chakra.pathway(event.player).consume(cost)) {
				deactivate((EntityPlayerMP)event.player, true);
			}
		}
	}

	public static boolean isControlledActive(ItemStack stack) {
		return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().getBoolean(ACTIVE_TAG);
	}

	public static List<ItemStack> getOwnedDojutsuStacks(EntityPlayer player) {
		List<ItemStack> result = new ArrayList<>();
		for (ItemStack stack : ProcedureUtils.getAllItemsOfSubType(player, ItemDojutsu.Base.class)) {
			if (isOwnedBy(player, stack) && !containsItem(result, stack.getItem())) {
				result.add(stack);
			}
		}
		result.sort(Comparator.comparingInt(stack -> getSortOrder(stack.getItem())));
		return result;
	}

	public static void toggleSelected(EntityPlayerMP player, String registryName) {
		Item selected = Item.REGISTRY.getObject(new ResourceLocation(registryName));
		if (!(selected instanceof ItemDojutsu.Base)) {
			player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Selected dojutsu is unavailable."), true);
			return;
		}
		ItemStack head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (isControlledActive(head) && head.getItem() == selected) {
			deactivate(player, false);
			return;
		}
		long cooldown = player.getEntityData().getLong(ACTIVATION_COOLDOWN_TAG);
		long now = player.world.getTotalWorldTime();
		if (cooldown > now) {
			player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Dojutsu activation cooldown: "
				+ ((cooldown - now + 19L) / 20L) + "s"), true);
			return;
		}
		if (findOwnedStack(player, selected).isEmpty()) {
			player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "You do not own that dojutsu."), true);
			return;
		}
		activate(player, selected);
	}

	private static void activate(EntityPlayerMP player, Item selected) {
		ItemStack head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (isControlledActive(head)) {
			ItemStack stored = clearActiveTag(head.copy());
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
			moveHeadToInventory(player, stored);
		}

		ItemStack selectedStack = takeOwnedStack(player, selected);
		if (selectedStack.isEmpty()) {
			player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "You do not own that dojutsu."), true);
			return;
		}

		head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (!head.isEmpty()) {
			ItemStack oldHead = head.copy();
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
			moveHeadToInventory(player, oldHead);
		}

		ItemStack active = selectedStack.copy();
		active.setCount(1);
		setActiveTag(active, true);
		player.setItemStackToSlot(EntityEquipmentSlot.HEAD, active);
		player.getEntityData().setLong(ACTIVATION_COOLDOWN_TAG, player.world.getTotalWorldTime() + ACTIVATION_COOLDOWN_TICKS);
		player.inventory.markDirty();
		player.sendStatusMessage(new TextComponentString(TextFormatting.RED + active.getDisplayName() + TextFormatting.WHITE + " activated."), true);
	}

	private static void deactivate(EntityPlayerMP player, boolean exhausted) {
		ItemStack active = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (!isControlledActive(active)) {
			return;
		}
		ItemStack stored = clearActiveTag(active.copy());
		moveHeadToInventory(player, stored);
		player.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);

		Item fallbackItem = getInactiveFallback(active.getItem());
		ItemStack fallback = fallbackItem != null ? takeOwnedStack(player, fallbackItem) : ItemStack.EMPTY;
		if (!fallback.isEmpty()) {
			ItemStack equip = fallback.copy();
			equip.setCount(1);
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, equip);
		}
		player.inventory.markDirty();
		player.sendStatusMessage(new TextComponentString((exhausted ? TextFormatting.RED + "Not enough chakra. " : "")
			+ TextFormatting.GRAY + "Dojutsu deactivated."), true);
	}

	private static void moveHeadToInventory(EntityPlayerMP player, ItemStack stack) {
		if (!stack.isEmpty()) {
			ItemHandlerHelper.giveItemToPlayer(player, stack);
		}
	}

	private static ItemStack findOwnedStack(EntityPlayer player, Item item) {
		for (ItemStack stack : ProcedureUtils.getAllItemsOfSubType(player, ItemDojutsu.Base.class)) {
			if (stack.getItem() == item && isOwnedBy(player, stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack takeOwnedStack(EntityPlayer player, Item item) {
		List<NonNullList<ItemStack>> inventories = Arrays.asList(player.inventory.mainInventory,
			player.inventory.armorInventory, player.inventory.offHandInventory);
		for (NonNullList<ItemStack> inventory : inventories) {
			ListIterator<ItemStack> iterator = inventory.listIterator();
			while (iterator.hasNext()) {
				ItemStack stack = iterator.next();
				if (!stack.isEmpty() && stack.getItem() == item && isOwnedBy(player, stack)) {
					ItemStack taken = stack.copy();
					taken.setCount(1);
					stack.shrink(1);
					if (stack.isEmpty()) {
						iterator.set(ItemStack.EMPTY);
					}
					return taken;
				}
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isOwnedBy(EntityPlayer player, ItemStack stack) {
		return player.isCreative() || ProcedureUtils.isOriginalOwner(player, stack);
	}

	private static ItemStack clearActiveTag(ItemStack stack) {
		setActiveTag(stack, false);
		return stack;
	}

	private static void setActiveTag(ItemStack stack, boolean active) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setBoolean(ACTIVE_TAG, active);
	}

	private static boolean containsItem(List<ItemStack> stacks, Item item) {
		for (ItemStack stack : stacks) {
			if (stack.getItem() == item) {
				return true;
			}
		}
		return false;
	}

	private static Item getInactiveFallback(Item active) {
		if (active instanceof ItemSharingan.Base && active != ItemSharingan.helmet) {
			return ItemSharingan.helmet;
		}
		if (active == ItemTenseigan.helmet) {
			return ItemByakugan.helmet;
		}
		return null;
	}

	private static double getDrainPerSecond(ItemStack stack) {
		Item item = stack.getItem();
		if (item == ItemTenseigan.helmet) {
			return 10.0d;
		}
		if (item instanceof ItemRinnegan.Base) {
			return 12.0d;
		}
		if (item == ItemByakugan.helmet) {
			return 5.0d;
		}
		if (item instanceof ItemSharingan.Base) {
			ItemSharingan.Base sharingan = (ItemSharingan.Base)item;
			if (sharingan.isMangekyo() || item == ItemMangekyoSharingan.helmet || item == ItemMangekyoSharinganEternal.helmet
			 || item == ItemMangekyoSharinganObito.helmet || item == ItemMangekyoSharinganObitoFire.helmet) {
				return 8.0d;
			}
			if (item == ItemSharinganTomoe3.helmet) {
				return 5.0d;
			}
			if (item == ItemSharinganTomoe2.helmet) {
				return 4.0d;
			}
			return 3.0d;
		}
		return 5.0d;
	}

	private static int getSortOrder(Item item) {
		if (item == ItemSharingan.helmet) return 10;
		if (item == ItemSharinganTomoe1.helmet) return 11;
		if (item == ItemSharinganTomoe2.helmet) return 12;
		if (item == ItemSharinganTomoe3.helmet) return 13;
		if (item == ItemMangekyoSharingan.helmet) return 14;
		if (item == ItemMangekyoSharinganObito.helmet) return 15;
		if (item == ItemMangekyoSharinganObitoFire.helmet) return 16;
		if (item == ItemMangekyoSharinganEternal.helmet) return 17;
		if (item == ItemByakugan.helmet) return 30;
		if (item == ItemTenseigan.helmet) return 31;
		if (item == ItemRinnegan.helmet) return 40;
		return 100;
	}
}
