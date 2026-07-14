package net.narutomod;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

import net.narutomod.item.ItemSharinganTomoe1;
import net.narutomod.item.ItemSharinganTomoe2;
import net.narutomod.item.ItemSharinganTomoe3;
import net.narutomod.item.ItemJutsu;
import net.narutomod.procedure.ProcedureUtils;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ElementsNarutomodMod.ModElement.Tag
public class PlayerStats extends ElementsNarutomodMod.ModElement {
	private static final String ROOT = "NarutomodTrainingStats";
	private static final String POINTS = "points";
	private static final String POINT_LIMIT = "pointLimit";
	private static final String SPECIAL_SPENT = "specialSpent";
	private static final String CLAN = "clan";
	private static final String AFFINITY = "affinity";
	private static final String RANK = "rank";
	private static final String SYNCED_RANK_LIMIT = "syncedRankLimit";
	private static final String SHARINGAN_STAGE = "sharinganStage";
	private static final String CHAKRA_REGEN_LOCK_UNTIL = "chakraRegenLockUntil";
	private static final String[] STAT_KEYS = {"speed", "strength", "resistance", "health", "chakra", "spi"};
	private static final String[] STAT_NAMES = {"Speed", "Strength", "Resistance", "Health", "Chakra Max", "SPI"};
	public static final List<String> CLANS = Arrays.asList("None", "Aburame", "Akimichi", "Amagiri", "Chinoike", "Fuma", "FumaLandOfSound",
	 "Funato", "Hagoromo", "Hatake", "Hirasaka", "Hoki", "Hoshigaki", "Hozuki", "Hyuga", "Iburi", "Inuzuka", "Izuno", "Jugo", "Kagetsu",
	 "Kaguya", "Kamizuru", "Karatachi", "Kazekage", "Kedoin", "Kodon", "Kohaku", "Kumanoi", "Kurama", "Lee", "Nara", "Onikuma",
	 "Otsutsuki", "Rinha", "Ryu", "Sarutobi", "Sendo", "Senju", "Shiin", "Shimura", "Shirogane", "Taketori", "Tenro", "Tsuchigumo",
	 "Taijutsu", "Uchiha", "Uzumaki", "Wagarashi", "Wasabi", "Yamanaka", "Yoimura", "Yota");
	public static final List<String> AFFINITIES = Arrays.asList("None", "TAIJUTSU", "INUZUKA", "NINJUTSU", "DOTON", "FUTON", "KATON", "RAITON", "SUITON", "INTON",
	 "YOTON", "JINTON", "MOKUTON", "JITON", "IRYO", "HYOTON", "BAKUTON", "SHAKUTON", "BYAKUGAN", "SHARINGAN", "RINNEGAN", "RANTON",
	 "FUTTON", "YOOTON", "SHIKOTSUMYAKU", "KUCHIYOSE", "TENSEIGAN", "SENJUTSU", "SIXPATHSENJUTSU", "KEKKEIMORA", "SHOTON", "OTHER");
	public static final List<String> RANKS = Arrays.asList("None", "Genin", "Chunin", "Jonin", "Hokage");
	private static final int MAX_STAT = 1000000;
	private static final int[] DEFAULT_RANK_LIMITS = {MAX_STAT, 75, 150, 250, 400};
	private static final int[] OLD_DEFAULT_RANK_LIMITS = {MAX_STAT, 100, 200, 350, 1000};
	private static final UUID SPEED_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0001");
	private static final UUID STRENGTH_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0002");
	private static final UUID RESIST_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0003");
	private static final UUID HEALTH_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0004");

	public PlayerStats(ElementsNarutomodMod instance) {
		super(instance, 1002);
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		elements.addNetworkMessage(UpgradeMessage.Handler.class, UpgradeMessage.class, Side.SERVER);
		elements.addNetworkMessage(RequestSyncMessage.Handler.class, RequestSyncMessage.class, Side.SERVER);
		elements.addNetworkMessage(SyncMessage.Handler.class, SyncMessage.class, Side.CLIENT);
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new PlayerHook());
	}

	public static String[] getStatKeys() {
		return STAT_KEYS;
	}

	public static int getStat(EntityPlayer player, int stat) {
		return getStatsTag(player).getInteger(STAT_KEYS[stat]);
	}

	public static int getPoints(EntityPlayer player) {
		return getStatsTag(player).getInteger(POINTS);
	}

	public static int getPointLimit(EntityPlayer player) {
		int limit = getStatsTag(player).getInteger(POINT_LIMIT);
		return limit > 0 ? limit : 100;
	}

	public static int getSpendablePointPool(EntityPlayer player) {
		return Math.min(getPoints(player), getPointLimit(player));
	}

	public static int getSpentPoints(EntityPlayer player) {
		int spent = getStatsTag(player).getInteger(SPECIAL_SPENT);
		for (int i = 0; i < STAT_KEYS.length; i++) {
			spent += getStat(player, i);
		}
		return spent;
	}

	public static int getAvailablePoints(EntityPlayer player) {
		return Math.max(0, getSpendablePointPool(player) - getSpentPoints(player));
	}

	public static String getClan(EntityPlayer player) {
		String clan = getStatsTag(player).getString(CLAN);
		return clan.isEmpty() ? "None" : clan;
	}

	public static String getAffinity(EntityPlayer player) {
		String affinity = getStatsTag(player).getString(AFFINITY);
		return affinity.isEmpty() ? "None" : affinity;
	}

	public static String getRank(EntityPlayer player) {
		String rank = getStatsTag(player).getString(RANK);
		return rank.isEmpty() ? "None" : normalizeRank(rank);
	}

	public static boolean isTaijutsuClan(EntityPlayer player) {
		String clan = getClan(player);
		return clan.equalsIgnoreCase("Taijutsu") || clan.equalsIgnoreCase("Lee");
	}

	public static boolean isTaijutsuTrained(EntityPlayer player) {
		return isTaijutsuClan(player) || (player instanceof EntityPlayerMP
		 && ProcedureUtils.advancementAchieved((EntityPlayerMP)player, "narutomod:openedgates"));
	}

	public static int getRankStatLimit(EntityPlayer player) {
		if (player.world.isRemote && getStatsTag(player).hasKey(SYNCED_RANK_LIMIT)) {
			return getStatsTag(player).getInteger(SYNCED_RANK_LIMIT);
		}
		return getRankLimit(player.world, getRank(player));
	}

	public static boolean hasAffinity(EntityPlayer player, ItemJutsu.JutsuEnum.Type type) {
		if (type == null) return false;
		for (String affinity : getAffinity(player).split(",")) {
			if (affinity.trim().equalsIgnoreCase(type.name())) {
				return true;
			}
		}
		return false;
	}

	public static double getChakraBonus(EntityPlayer player) {
		return curve(getStat(player, 4), 95.0d, 4500.0d);
	}

	public static double getStaminaBonus(EntityPlayer player) {
		int physical = getStat(player, 0) + getStat(player, 1) + getStat(player, 2) + getStat(player, 3);
		return curve(physical, 42.0d, 4200.0d);
	}

	public static double getSpiRegenBonus(EntityPlayer player) {
		return curve(getStat(player, 5), 0.012d, 0.20d);
	}

	public static boolean canRegenerateChakra(EntityPlayer player) {
		return player.world.getTotalWorldTime() >= getStatsTag(player).getLong(CHAKRA_REGEN_LOCK_UNTIL);
	}

	public static float getAffinityJutsuXpModifier(EntityPlayer player, ItemJutsu.JutsuEnum.Type type) {
		return hasAffinity(player, type) ? 1.35f : 1.0f;
	}

	public static void setPoints(EntityPlayer player, int points) {
		getStatsTag(player).setInteger(POINTS, Math.max(0, points));
		syncToClient(player);
	}

	public static void setPointLimit(EntityPlayer player, int limit) {
		getStatsTag(player).setInteger(POINT_LIMIT, Math.max(1, limit));
		syncToClient(player);
	}

	public static void setClan(EntityPlayer player, String clan) {
		getStatsTag(player).setString(CLAN, normalizeChoice(clan, CLANS, "None"));
		syncToClient(player);
	}

	public static void setAffinity(EntityPlayer player, String affinity) {
		getStatsTag(player).setString(AFFINITY, normalizeChoice(affinity, AFFINITIES, "None"));
		syncToClient(player);
	}

	public static void setRank(EntityPlayer player, String rank) {
		getStatsTag(player).setString(RANK, normalizeRank(rank));
		clampStatsToRank(player);
		applyAttributes(player);
		syncToClient(player);
	}

	public static void setRankLimit(World world, String rank, int limit) {
		RankLimitData data = RankLimitData.get(world);
		data.setLimit(normalizeRank(rank), Math.max(0, limit));
		data.markDirty();
	}

	public static void refresh(EntityPlayer player) {
		clampStatsToRank(player);
		applyAttributes(player);
		syncToClient(player);
	}

	public static int getRankLimit(World world, String rank) {
		return RankLimitData.get(world).getLimit(normalizeRank(rank));
	}

	public static void addAffinity(EntityPlayer player, String affinity) {
		String normalized = normalizeChoice(affinity, AFFINITIES, "None");
		if ("None".equals(normalized)) {
			return;
		}
		if ("None".equals(getAffinity(player))) {
			getStatsTag(player).setString(AFFINITY, normalized);
		} else if (!hasAffinity(player, ItemJutsu.JutsuEnum.Type.valueOf(normalized))) {
			getStatsTag(player).setString(AFFINITY, getAffinity(player) + "," + normalized);
		}
		syncToClient(player);
	}

	public static void removeAffinity(EntityPlayer player, String affinity) {
		String normalized = normalizeChoice(affinity, AFFINITIES, "None");
		String result = "";
		for (String entry : getAffinity(player).split(",")) {
			if (!entry.trim().equalsIgnoreCase(normalized) && !entry.trim().isEmpty() && !entry.trim().equalsIgnoreCase("None")) {
				result += result.isEmpty() ? entry.trim() : "," + entry.trim();
			}
		}
		getStatsTag(player).setString(AFFINITY, result.isEmpty() ? "None" : result);
		syncToClient(player);
	}

	public static void setStat(EntityPlayer player, int stat, int value) {
		if (stat >= 0 && stat < STAT_KEYS.length) {
			getStatsTag(player).setInteger(STAT_KEYS[stat], Math.max(0, Math.min(getRankStatLimit(player), value)));
			applyAttributes(player);
			syncToClient(player);
		}
	}

	public static void setSharinganStage(EntityPlayer player, int stage) {
		stage = Math.max(0, Math.min(3, stage));
		getStatsTag(player).setInteger(SHARINGAN_STAGE, stage);
		if (stage == 1) {
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemSharinganTomoe1.helmet));
		} else if (stage == 2) {
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemSharinganTomoe2.helmet));
		} else if (stage == 3) {
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemSharinganTomoe3.helmet));
		}
		syncToClient(player);
	}

	public static int getSharinganStage(EntityPlayer player) {
		ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		if (stack.getItem() == ItemSharinganTomoe1.helmet) return 1;
		if (stack.getItem() == ItemSharinganTomoe2.helmet) return 2;
		if (stack.getItem() == ItemSharinganTomoe3.helmet) return 3;
		return getStatsTag(player).getInteger(SHARINGAN_STAGE);
	}

	public static String getStatus(EntityPlayer player) {
		return TextFormatting.YELLOW + "Checksheet: " + player.getName()
		 + "\nRank: " + getRank(player) + " (stat cap " + getRankStatLimit(player) + ")"
		 + "\nClan: " + getClan(player)
		 + "\nAffinity: " + getAffinity(player)
		 + "\nPoints: " + getAvailablePoints(player) + " / " + getPointLimit(player)
		 + "\nSpeed: " + getStat(player, 0)
		 + "\nStrength: " + getStat(player, 1)
		 + "\nResistance: " + getStat(player, 2)
		 + "\nHealth: " + getStat(player, 3)
		 + "\nChakra Max: " + getStat(player, 4)
		 + "\nSPI: " + getStat(player, 5)
		 + "\nSharingan: " + getSharinganStage(player) + " tomoe";
	}

	private static String normalizeChoice(String value, List<String> valid, String fallback) {
		for (String entry : valid) {
			if (entry.equalsIgnoreCase(value)) {
				return entry;
			}
		}
		return fallback;
	}

	public static String normalizeRank(String rank) {
		if (rank == null || rank.trim().isEmpty()) {
			return "None";
		}
		String clean = rank.trim();
		if (clean.equalsIgnoreCase("chunnin")) return "Chunin";
		if (clean.equalsIgnoreCase("jounin")) return "Jonin";
		for (String entry : RANKS) {
			if (entry.equalsIgnoreCase(clean)) {
				return entry;
			}
		}
		return "None";
	}

	private static void clampStatsToRank(EntityPlayer player) {
		NBTTagCompound tag = getStatsTag(player);
		int limit = getRankStatLimit(player);
		for (int i = 0; i < STAT_KEYS.length; i++) {
			if (tag.getInteger(STAT_KEYS[i]) > limit) {
				tag.setInteger(STAT_KEYS[i], limit);
			}
		}
	}

	private static NBTTagCompound getStatsTag(EntityPlayer player) {
		NBTTagCompound data = player.getEntityData();
		if (!data.hasKey(ROOT, 10)) {
			data.setTag(ROOT, new NBTTagCompound());
		}
		return data.getCompoundTag(ROOT);
	}

	private static boolean spendPoint(EntityPlayer player) {
		if (getAvailablePoints(player) <= 0) {
			return false;
		}
		return true;
	}

	private static boolean upgrade(EntityPlayer player, int stat) {
		if (stat < 0 || stat >= STAT_KEYS.length || !spendPoint(player)) {
			return false;
		}
		NBTTagCompound tag = getStatsTag(player);
		int value = tag.getInteger(STAT_KEYS[stat]);
		if (value >= getRankStatLimit(player)) {
			return false;
		}
		tag.setInteger(STAT_KEYS[stat], value + 1);
		syncToClient(player);
		applyAttributes(player);
		return true;
	}

	private static boolean upgradeSharingan(EntityPlayer player) {
		if (!getClan(player).equalsIgnoreCase("Uchiha") || !spendPoint(player) || getSharinganStage(player) >= 3) {
			return false;
		}
		getStatsTag(player).setInteger(SPECIAL_SPENT, getStatsTag(player).getInteger(SPECIAL_SPENT) + 1);
		setSharinganStage(player, getSharinganStage(player) + 1);
		return true;
	}

	private static void syncToClient(EntityPlayer player) {
		if (player instanceof EntityPlayerMP) {
			NarutomodMod.PACKET_HANDLER.sendTo(new SyncMessage(player), (EntityPlayerMP)player);
		}
	}

	public static void applyClientSync(EntityPlayer player, SyncMessage message) {
		NBTTagCompound tag = getStatsTag(player);
		tag.setInteger(POINTS, message.points);
		tag.setInteger(POINT_LIMIT, message.pointLimit);
		tag.setInteger(SPECIAL_SPENT, message.specialSpent);
		tag.setString(CLAN, message.clan);
		tag.setString(AFFINITY, message.affinity);
		tag.setString(RANK, message.rank);
		tag.setInteger(SYNCED_RANK_LIMIT, message.rankLimit);
		tag.setInteger(SHARINGAN_STAGE, message.sharinganStage);
		for (int i = 0; i < STAT_KEYS.length; i++) {
			tag.setInteger(STAT_KEYS[i], message.stats[i]);
		}
	}

	private static void applyAttributes(EntityPlayer player) {
		applyModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, SPEED_UUID, "narutomod.stats.speed", curve(getStat(player, 0), 0.006d, 0.16d));
		applyModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, STRENGTH_UUID, "narutomod.stats.strength", curve(getStat(player, 1), 0.65d, 18.0d));
		applyModifier(player, SharedMonsterAttributes.ARMOR, RESIST_UUID, "narutomod.stats.resistance", curve(getStat(player, 2), 0.45d, 14.0d));
		applyModifier(player, SharedMonsterAttributes.MAX_HEALTH, HEALTH_UUID, "narutomod.stats.health", curve(getStat(player, 3), 2.0d, 55.0d));
	}

	private static double curve(int stat, double scale, double cap) {
		return Math.min(cap, Math.sqrt(Math.max(0, stat)) * scale);
	}

	private static double combatCurve(int stat, double scale, double cap) {
		return Math.min(cap, Math.sqrt(Math.max(0, stat)) * scale);
	}

	private static void applyModifier(EntityPlayer player, IAttribute attribute, UUID uuid, String name, double amount) {
		IAttributeInstance instance = player.getEntityAttribute(attribute);
		if (instance == null) return;
		AttributeModifier old = instance.getModifier(uuid);
		if (old != null) instance.removeModifier(old);
		if (amount != 0.0d) instance.applyModifier(new AttributeModifier(uuid, name, amount, 0));
	}

	public static class PlayerHook {
		@SubscribeEvent
		public void onHurt(LivingHurtEvent event) {
			if (event.getEntityLiving().world.isRemote || event.getAmount() <= 0f) {
				return;
			}
			EntityPlayer defender = event.getEntityLiving() instanceof EntityPlayer ? (EntityPlayer)event.getEntityLiving() : null;
			EntityPlayer attacker = event.getSource().getTrueSource() instanceof EntityPlayer ? (EntityPlayer)event.getSource().getTrueSource() : null;
			float amount = event.getAmount();
			if (attacker != null && !ItemJutsu.isDamageSourceJutsu(event.getSource())) {
				amount *= 1.0f + (float)combatCurve(getStat(attacker, 1), 0.010d, 0.22d);
			}
			if (defender != null && !event.getSource().isDamageAbsolute()) {
				double resistance = combatCurve(getStat(defender, 2), 0.018d, 0.34d);
				double pierce = attacker != null ? combatCurve(getStat(attacker, 1), 0.010d, 0.18d) : 0d;
				amount *= 1.0f - (float)Math.max(0d, resistance - pierce);
				EntityPlayer speedSource = attacker;
				double attackerSpeed = speedSource != null ? Math.sqrt(Math.max(0, getStat(speedSource, 0))) : 0d;
				double defenderSpeed = Math.sqrt(Math.max(0, getStat(defender, 0)));
				boolean dodgeable = event.getSource().isProjectile() || attacker != null;
				double dodgeChance = dodgeable ? Math.min(0.18d, Math.max(0d, defenderSpeed - attackerSpeed) * 0.012d) : 0d;
				if (dodgeChance > 0d && defender.getRNG().nextDouble() < dodgeChance) {
					event.setCanceled(true);
					return;
				}
			}
			event.setAmount(Math.max(0.1f, amount));
		}

		@SubscribeEvent
		public void onDamaged(LivingDamageEvent event) {
			if (!event.getEntityLiving().world.isRemote && event.getEntityLiving() instanceof EntityPlayer && event.getAmount() > 0f) {
				EntityPlayer player = (EntityPlayer)event.getEntityLiving();
				getStatsTag(player).setLong(CHAKRA_REGEN_LOCK_UNTIL, player.world.getTotalWorldTime() + 600L);
			}
		}

		@SubscribeEvent
		public void onTick(TickEvent.PlayerTickEvent event) {
			if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote && event.player.ticksExisted % 20 == 0) {
				applyAttributes(event.player);
			}
		}

		@SubscribeEvent
		public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
			if (!event.player.world.isRemote) {
				applyAttributes(event.player);
				syncToClient(event.player);
			}
		}

		@SubscribeEvent
		public void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
			NBTTagCompound original = getStatsTag(event.getOriginal());
			event.getEntityPlayer().getEntityData().setTag(ROOT, original.copy());
		}

		@SubscribeEvent
		public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
			if (!event.player.world.isRemote) {
				applyAttributes(event.player);
				syncToClient(event.player);
			}
		}
	}

	public static class UpgradeMessage implements IMessage {
		private int stat;
		public UpgradeMessage() { }
		public UpgradeMessage(int statIn) { this.stat = statIn; }
		public static class Handler implements IMessageHandler<UpgradeMessage, IMessage> {
			@Override public IMessage onMessage(UpgradeMessage message, MessageContext context) {
				EntityPlayerMP player = context.getServerHandler().player;
				player.getServerWorld().addScheduledTask(() -> {
					if (message.stat == 6) upgradeSharingan(player);
					else upgrade(player, message.stat);
				});
				return null;
			}
		}
		@Override public void toBytes(ByteBuf buf) { buf.writeInt(this.stat); }
		@Override public void fromBytes(ByteBuf buf) { this.stat = buf.readInt(); }
	}

	public static class RequestSyncMessage implements IMessage {
		public static class Handler implements IMessageHandler<RequestSyncMessage, IMessage> {
			@Override public IMessage onMessage(RequestSyncMessage message, MessageContext context) {
				EntityPlayerMP player = context.getServerHandler().player;
				player.getServerWorld().addScheduledTask(() -> syncToClient(player));
				return null;
			}
		}
		@Override public void toBytes(ByteBuf buf) { }
		@Override public void fromBytes(ByteBuf buf) { }
	}

	public static class SyncMessage implements IMessage {
		private int points, pointLimit, specialSpent, sharinganStage, rankLimit;
		private int[] stats = new int[STAT_KEYS.length];
		private String clan = "None";
		private String affinity = "None";
		private String rank = "None";
		public SyncMessage() { }
		public SyncMessage(EntityPlayer player) {
			this.points = getPoints(player);
			this.pointLimit = getPointLimit(player);
			this.specialSpent = getStatsTag(player).getInteger(SPECIAL_SPENT);
			this.clan = getClan(player);
			this.affinity = getAffinity(player);
			this.rank = getRank(player);
			this.rankLimit = getRankStatLimit(player);
			this.sharinganStage = getSharinganStage(player);
			for (int i = 0; i < STAT_KEYS.length; i++) this.stats[i] = getStat(player, i);
		}
		public static class Handler implements IMessageHandler<SyncMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(SyncMessage message, MessageContext context) {
				net.narutomod.client.PlayerStatsClient.handleSync(message);
				return null;
			}
		}
		@Override public void toBytes(ByteBuf buf) {
			buf.writeInt(this.points);
			buf.writeInt(this.pointLimit);
			buf.writeInt(this.specialSpent);
			buf.writeInt(this.sharinganStage);
			buf.writeInt(this.rankLimit);
			for (int stat : this.stats) buf.writeInt(stat);
			ByteBufUtils.writeUTF8String(buf, this.clan);
			ByteBufUtils.writeUTF8String(buf, this.affinity);
			ByteBufUtils.writeUTF8String(buf, this.rank);
		}
		@Override public void fromBytes(ByteBuf buf) {
			this.points = buf.readInt();
			this.pointLimit = buf.readInt();
			this.specialSpent = buf.readInt();
			this.sharinganStage = buf.readInt();
			this.rankLimit = buf.readInt();
			this.stats = new int[STAT_KEYS.length];
			for (int i = 0; i < this.stats.length; i++) this.stats[i] = buf.readInt();
			this.clan = ByteBufUtils.readUTF8String(buf);
			this.affinity = ByteBufUtils.readUTF8String(buf);
			this.rank = ByteBufUtils.readUTF8String(buf);
		}
	}

	public static class RankLimitData extends WorldSavedData {
		private static final String NAME = "NarutomodRankLimits";
		private final int[] limits = Arrays.copyOf(DEFAULT_RANK_LIMITS, DEFAULT_RANK_LIMITS.length);

		public RankLimitData() {
			super(NAME);
		}

		public RankLimitData(String name) {
			super(name);
		}

		public static RankLimitData get(World world) {
			RankLimitData data = (RankLimitData)world.getMapStorage().getOrLoadData(RankLimitData.class, NAME);
			if (data == null) {
				data = new RankLimitData();
				world.getMapStorage().setData(NAME, data);
			}
			return data;
		}

		public int getLimit(String rank) {
			int index = RANKS.indexOf(normalizeRank(rank));
			return index >= 0 && index < this.limits.length ? this.limits[index] : MAX_STAT;
		}

		public void setLimit(String rank, int limit) {
			int index = RANKS.indexOf(normalizeRank(rank));
			if (index >= 0 && index < this.limits.length) {
				this.limits[index] = limit;
			}
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt) {
			boolean oldDefaults = true;
			for (int i = 0; i < RANKS.size() && i < this.limits.length; i++) {
				String key = "limit_" + RANKS.get(i);
				if (nbt.hasKey(key)) {
					this.limits[i] = nbt.getInteger(key);
				}
				oldDefaults = oldDefaults && this.limits[i] == OLD_DEFAULT_RANK_LIMITS[i];
			}
			if (oldDefaults) {
				for (int i = 0; i < this.limits.length && i < DEFAULT_RANK_LIMITS.length; i++) {
					this.limits[i] = DEFAULT_RANK_LIMITS[i];
				}
			}
		}

		@Override
		public NBTTagCompound writeToNBT(NBTTagCompound compound) {
			for (int i = 0; i < RANKS.size() && i < this.limits.length; i++) {
				compound.setInteger("limit_" + RANKS.get(i), this.limits[i]);
			}
			return compound;
		}
	}

}
