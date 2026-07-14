package net.narutomod;

import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.item.ItemJutsu;
import net.narutomod.procedure.ProcedureOnLivingUpdate;
import net.narutomod.procedure.ProcedureUtils;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

@ElementsNarutomodMod.ModElement.Tag
public class ElementalTraining extends ElementsNarutomodMod.ModElement {
	private static final String ROOT = "NarutomodElementalTraining";
	private static final String SEAL_XP = "SealMasteryXp";
	private static final String ACTIVE_UNTIL = "ElementalTrainingActiveUntil";
	private static final String ACTIVE_ID = "ElementalTrainingSession";
	private static final String ACTIVE_ELEMENT = "ElementalTrainingElement";
	private static final String ACTIVE_X = "ElementalTrainingX";
	private static final String ACTIVE_Y = "ElementalTrainingY";
	private static final String ACTIVE_Z = "ElementalTrainingZ";
	private static final int SESSION_TIMEOUT = 20 * 90;
	private static final int MAX_ELEMENT_XP = 2500;

	@GameRegistry.ObjectHolder("narutomod:fire_training_scroll")
	public static final Item fireScroll = null;
	@GameRegistry.ObjectHolder("narutomod:water_training_scroll")
	public static final Item waterScroll = null;
	@GameRegistry.ObjectHolder("narutomod:wind_training_scroll")
	public static final Item windScroll = null;
	@GameRegistry.ObjectHolder("narutomod:earth_training_scroll")
	public static final Item earthScroll = null;
	@GameRegistry.ObjectHolder("narutomod:lightning_training_scroll")
	public static final Item lightningScroll = null;

	public ElementalTraining(ElementsNarutomodMod instance) {
		super(instance, 1021);
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		elements.addNetworkMessage(StartMessage.Handler.class, StartMessage.class, Side.CLIENT);
		elements.addNetworkMessage(ResultMessage.Handler.class, ResultMessage.class, Side.SERVER);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new TrainingScroll(Element.FIRE));
		elements.items.add(() -> new TrainingScroll(Element.WATER));
		elements.items.add(() -> new TrainingScroll(Element.WIND));
		elements.items.add(() -> new TrainingScroll(Element.EARTH));
		elements.items.add(() -> new TrainingScroll(Element.LIGHTNING));
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new TrainingHook());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		for (Element element : Element.values()) {
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("narutomod", element.registryName));
			if (item != null) {
				ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation("narutomod:" + element.registryName, "inventory"));
			}
		}
	}

	private static NBTTagCompound root(EntityPlayer player) {
		NBTTagCompound tag = player.getEntityData().getCompoundTag(ROOT);
		player.getEntityData().setTag(ROOT, tag);
		return tag;
	}

	public static int getElementXp(EntityPlayer player, ItemJutsu.JutsuEnum.Type type) {
		Element element = Element.fromType(type);
		return element == null ? 0 : root(player).getInteger(element.xpKey);
	}

	public static int getElementMastery(EntityPlayer player, ItemJutsu.JutsuEnum.Type type) {
		return Math.min(100, getElementXp(player, type) * 100 / MAX_ELEMENT_XP);
	}

	public static boolean isElementUnlocked(EntityPlayer player, ItemJutsu.JutsuEnum.Type type) {
		Element element = Element.fromType(type);
		return element == null || player.isCreative() || PlayerStats.hasAffinity(player, type) || getElementXp(player, type) > 0;
	}

	public static boolean isTraining(EntityPlayer player) {
		return player.getEntityData().getLong(ACTIVE_UNTIL) > player.world.getTotalWorldTime();
	}

	public static boolean isElementalType(ItemJutsu.JutsuEnum.Type type) {
		return Element.fromType(type) != null;
	}

	public static String getElementName(ItemJutsu.JutsuEnum.Type type) {
		Element element = Element.fromType(type);
		return element == null ? String.valueOf(type) : element.displayName();
	}

	private static void startTraining(EntityPlayerMP player, Element element) {
		long session = UUID.randomUUID().getLeastSignificantBits() ^ player.world.getTotalWorldTime();
		player.getEntityData().setLong(ACTIVE_UNTIL, player.world.getTotalWorldTime() + SESSION_TIMEOUT);
		player.getEntityData().setLong(ACTIVE_ID, session);
		player.getEntityData().setString(ACTIVE_ELEMENT, element.name());
		player.getEntityData().setDouble(ACTIVE_X, player.posX);
		player.getEntityData().setDouble(ACTIVE_Y, player.posY);
		player.getEntityData().setDouble(ACTIVE_Z, player.posZ);
		ProcedureOnLivingUpdate.disableMouseClicks(player, SESSION_TIMEOUT);
		NarutomodMod.PACKET_HANDLER.sendTo(new StartMessage(element.ordinal(), session, getElementMastery(player, element.type), root(player).getInteger(SEAL_XP)), player);
	}

	private static void finishTraining(EntityPlayerMP player, Element element, long session, boolean success, boolean perfect) {
		if (player.getEntityData().getLong(ACTIVE_ID) != session || !element.name().equals(player.getEntityData().getString(ACTIVE_ELEMENT))) {
			return;
		}
		player.getEntityData().removeTag(ACTIVE_UNTIL);
		player.getEntityData().removeTag(ACTIVE_ID);
		player.getEntityData().removeTag(ACTIVE_ELEMENT);
		player.getEntityData().removeTag(ACTIVE_X);
		player.getEntityData().removeTag(ACTIVE_Y);
		player.getEntityData().removeTag(ACTIVE_Z);
		ProcedureOnLivingUpdate.disableMouseClicks(player, 0);
		if (!success) {
			player.sendMessage(colored(TextFormatting.RED, "message.narutomod.element_training.failed"));
			return;
		}
		int mastery = getElementMastery(player, element.type);
		int baseXp = mastery >= 75 ? 50 : 25;
		int xp = perfect ? Math.round(baseXp * 1.2f) : baseXp;
		NBTTagCompound tag = root(player);
		tag.setInteger(element.xpKey, Math.min(MAX_ELEMENT_XP, tag.getInteger(element.xpKey) + xp));
		tag.setInteger(SEAL_XP, Math.min(10000, tag.getInteger(SEAL_XP) + (perfect ? 8 : 5)));
		PlayerStats.addAffinity(player, element.type.name());
		player.sendMessage(colored(TextFormatting.GOLD, perfect ? "message.narutomod.element_training.perfect" : "message.narutomod.element_training.success", element.displayName(), xp));
	}

	private static TextComponentTranslation colored(TextFormatting color, String key, Object... args) {
		TextComponentTranslation component = new TextComponentTranslation(key, args);
		component.getStyle().setColor(color);
		return component;
	}

	public enum Element {
		FIRE("fire_training_scroll", "KATON", ItemJutsu.JutsuEnum.Type.KATON, 0xE85B24),
		WATER("water_training_scroll", "SUITON", ItemJutsu.JutsuEnum.Type.SUITON, 0x3D8CFF),
		WIND("wind_training_scroll", "FUTON", ItemJutsu.JutsuEnum.Type.FUTON, 0xB8F0D0),
		EARTH("earth_training_scroll", "DOTON", ItemJutsu.JutsuEnum.Type.DOTON, 0x9B7042),
		LIGHTNING("lightning_training_scroll", "RAITON", ItemJutsu.JutsuEnum.Type.RAITON, 0xC8E8FF);

		public final String registryName;
		public final String xpKey;
		public final ItemJutsu.JutsuEnum.Type type;
		public final int color;

		Element(String registryNameIn, String xpKeyIn, ItemJutsu.JutsuEnum.Type typeIn, int colorIn) {
			this.registryName = registryNameIn;
			this.xpKey = xpKeyIn + "TrainingXp";
			this.type = typeIn;
			this.color = colorIn;
		}

		public String displayName() {
			return new TextComponentTranslation("element.narutomod." + this.name().toLowerCase()).getUnformattedComponentText();
		}

		public static Element fromType(ItemJutsu.JutsuEnum.Type type) {
			for (Element element : values()) {
				if (element.type == type) return element;
			}
			return null;
		}
	}

	public static class TrainingScroll extends Item {
		private final Element element;

		public TrainingScroll(Element elementIn) {
			this.element = elementIn;
			this.setUnlocalizedName(elementIn.registryName);
			this.setRegistryName(elementIn.registryName);
			this.setCreativeTab(TabCustomTabs.jutsus);
			this.setMaxStackSize(16);
		}

		@Override
		public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
			ItemStack stack = player.getHeldItem(hand);
			if (!world.isRemote && player instanceof EntityPlayerMP) {
				if (isTraining(player)) {
					return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
				}
				if (!player.isCreative()) {
					stack.shrink(1);
				}
				startTraining((EntityPlayerMP)player, this.element);
			}
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
		}
	}

	public static class TrainingHook {
		@SubscribeEvent
		public void onPlayerTick(TickEvent.PlayerTickEvent event) {
			if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !isTraining(event.player)) return;
			double x = event.player.getEntityData().getDouble(ACTIVE_X);
			double y = event.player.getEntityData().getDouble(ACTIVE_Y);
			double z = event.player.getEntityData().getDouble(ACTIVE_Z);
			if (event.player.getDistanceSq(x, y, z) > 0.01d) {
				event.player.setPositionAndUpdate(x, y, z);
			}
			ProcedureUtils.setVelocity(event.player, 0d, Math.min(0d, event.player.motionY), 0d);
			ProcedureOnLivingUpdate.disableMouseClicks(event.player, 5);
			if (event.player.getEntityData().getLong(ACTIVE_UNTIL) <= event.player.world.getTotalWorldTime()) {
				event.player.getEntityData().removeTag(ACTIVE_UNTIL);
				event.player.getEntityData().removeTag(ACTIVE_ID);
				event.player.getEntityData().removeTag(ACTIVE_ELEMENT);
				ProcedureOnLivingUpdate.disableMouseClicks(event.player, 0);
			}
		}

		@SubscribeEvent
		public void onAttack(LivingAttackEvent event) {
			Entity source = event.getSource().getTrueSource();
			if (source instanceof EntityPlayer && isTraining((EntityPlayer)source)) {
				event.setCanceled(true);
			}
		}

		@SubscribeEvent
		public void onInteract(PlayerInteractEvent event) {
			if (isTraining(event.getEntityPlayer())) {
				event.setCanceled(true);
			}
		}
	}

	public static class StartMessage implements IMessage {
		private int element;
		private long session;
		private int mastery;
		private int sealXp;

		public StartMessage() {}
		public StartMessage(int elementIn, long sessionIn, int masteryIn, int sealXpIn) {
			this.element = elementIn;
			this.session = sessionIn;
			this.mastery = masteryIn;
			this.sealXp = sealXpIn;
		}
		@Override public void toBytes(ByteBuf buf) { buf.writeInt(this.element); buf.writeLong(this.session); buf.writeInt(this.mastery); buf.writeInt(this.sealXp); }
		@Override public void fromBytes(ByteBuf buf) { this.element = buf.readInt(); this.session = buf.readLong(); this.mastery = buf.readInt(); this.sealXp = buf.readInt(); }

		public static class Handler implements IMessageHandler<StartMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(final StartMessage message, MessageContext ctx) {
				net.narutomod.client.ElementalTrainingClient.open(message.element, message.session, message.mastery, message.sealXp);
				return null;
			}
		}
	}

	public static class ResultMessage implements IMessage {
		private int element;
		private long session;
		private boolean success;
		private boolean perfect;

		public ResultMessage() {}
		public ResultMessage(int elementIn, long sessionIn, boolean successIn, boolean perfectIn) {
			this.element = elementIn;
			this.session = sessionIn;
			this.success = successIn;
			this.perfect = perfectIn;
		}
		@Override public void toBytes(ByteBuf buf) { buf.writeInt(this.element); buf.writeLong(this.session); buf.writeBoolean(this.success); buf.writeBoolean(this.perfect); }
		@Override public void fromBytes(ByteBuf buf) { this.element = buf.readInt(); this.session = buf.readLong(); this.success = buf.readBoolean(); this.perfect = buf.readBoolean(); }

		public static class Handler implements IMessageHandler<ResultMessage, IMessage> {
			@Override public IMessage onMessage(final ResultMessage message, final MessageContext ctx) {
				ctx.getServerHandler().player.getServerWorld().addScheduledTask(new Runnable() {
					@Override public void run() {
						if (message.element >= 0 && message.element < Element.values().length) {
							finishTraining(ctx.getServerHandler().player, Element.values()[message.element], message.session, message.success, message.perfect);
						}
					}
				});
				return null;
			}
		}
	}
}
