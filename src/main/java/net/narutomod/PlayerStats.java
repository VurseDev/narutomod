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
import java.util.Locale;
import java.util.UUID;

@ElementsNarutomodMod.ModElement.Tag
public class PlayerStats extends ElementsNarutomodMod.ModElement {
	private static final String ROOT = "NarutomodTrainingStats";
	private static final String POINTS = "points";
	private static final String LEGACY_POINT_LIMIT = "pointLimit";
	private static final String PERSONAL_STAT_LIMIT = "personalStatLimit";
	private static final String SPECIAL_SPENT = "specialSpent";
	private static final String CLAN = "clan";
	private static final String AFFINITY = "affinity";
	private static final String RANK = "rank";
	private static final String SYNCED_RANK_LIMIT = "syncedRankLimit";
	private static final String SHARINGAN_STAGE = "sharinganStage";
	private static final String CHAKRA_REGEN_LOCK_UNTIL = "chakraRegenLockUntil";
	private static final String DATA_VERSION = "dataVersion";
	private static final int CURRENT_DATA_VERSION = 5;
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
	private static final int MAX_STAT = 100000000;
	private static final long MAX_TOTAL_POINTS = (long)MAX_STAT * STAT_KEYS.length + 3L;
	private static final int[] DEFAULT_RANK_LIMITS = {100, 250, 600, 1200, 2500};
	private static final int[] PREVIOUS_DEFAULT_RANK_LIMITS = {1000000, 75, 150, 250, 400};
	private static final int[] LEGACY_DEFAULT_RANK_LIMITS = {1000000, 100, 200, 350, 1000};
	private static final UUID SPEED_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0001");
	private static final UUID STRENGTH_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0002");
	private static final UUID RESIST_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0003");
	private static final UUID HEALTH_UUID = UUID.fromString("4eb29d0e-5da4-4d3f-9378-bb4afc7d0004");
	private static final double MAX_VISIBLE_ATTACK_BONUS = 2000.0d;
	private static final double MAX_VISIBLE_HEALTH = 1000.0d;

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
		return Math.max(0, getStatsTag(player).getInteger(STAT_KEYS[stat]));
	}

	public static long getPoints(EntityPlayer player) {
		return Math.max(0L, getStatsTag(player).getLong(POINTS));
	}

	/**
	 * Kept for compatibility with older commands and UI code. There is now only
	 * one point pool, so its total is also its effective capacity.
	 */
	@Deprecated
	public static long getPointLimit(EntityPlayer player) {
		return getPoints(player);
	}

	public static long getSpendablePointPool(EntityPlayer player) {
		return getPoints(player);
	}

	public static long getSpentPoints(EntityPlayer player) {
		long spent = Math.max(0L, Math.min(3L, getStatsTag(player).getLong(SPECIAL_SPENT)));
		for (int i = 0; i < STAT_KEYS.length; i++) {
			spent += getStat(player, i);
		}
		return spent;
	}

	public static long getAvailablePoints(EntityPlayer player) {
		return Math.max(0L, getSpendablePointPool(player) - getSpentPoints(player));
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

	public static int getPersonalStatLimit(EntityPlayer player) {
		return Math.max(0, getStatsTag(player).getInteger(PERSONAL_STAT_LIMIT));
	}

	/**
	 * A personal limit overrides the rank default. This lets staff distinguish
	 * prodigies and veterans without inventing another rank.
	 */
	public static int getStatLimit(EntityPlayer player) {
		int personal = getPersonalStatLimit(player);
		return personal > 0 ? personal : getRankStatLimit(player);
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

	public static double getMovementBonus(EntityPlayer player) {
		double stat = getStat(player, 0);
		return 0.012d * Math.log10(1.0d + stat) + 0.00015d * Math.pow(stat, 0.45d);
	}

	public static double getStrengthAttackBonus(EntityPlayer player) {
		return Math.min(MAX_VISIBLE_ATTACK_BONUS, getEffectiveAttackBonus(player));
	}

	public static double getEffectiveAttackBonus(EntityPlayer player) {
		return progressive(getStat(player, 1), 0.04d, 0.80d);
	}

	public static double getMeleeOverflowMultiplier(EntityPlayer player) {
		double effective = getEffectiveAttackBonus(player);
		double visible = getStrengthAttackBonus(player);
		return (4.0d + effective) / (4.0d + visible);
	}

	public static double getResistanceRating(EntityPlayer player) {
		return progressive(getStat(player, 2), 0.12d, 0.75d);
	}

	public static double getResistanceDamageMultiplier(EntityPlayer player) {
		return 1.0d / Math.sqrt(1.0d + getResistanceRating(player) / 100.0d);
	}

	public static double getResistanceReduction(EntityPlayer player) {
		return 1.0d - getResistanceDamageMultiplier(player);
	}

	public static double getEffectiveMaxHealth(EntityPlayer player) {
		return 20.0d + PlayerTracker.getBattleXp(player) * 0.005d
		 + progressive(getStat(player, 3), 0.24d, 0.80d);
	}

	public static double getDisplayedMaxHealth(EntityPlayer player) {
		return Math.min(MAX_VISIBLE_HEALTH, getEffectiveMaxHealth(player));
	}

	public static double getHealthBonus(EntityPlayer player) {
		double battleHealth = PlayerTracker.getBattleXp(player) * 0.005d;
		return Math.max(0.0d, getDisplayedMaxHealth(player) - 20.0d - battleHealth);
	}

	public static double getHealthDamageMultiplier(EntityPlayer player) {
		double effective = getEffectiveMaxHealth(player);
		return effective > 0.0d ? Math.min(1.0d, getDisplayedMaxHealth(player) / effective) : 1.0d;
	}

	public static double getChakraBonus(EntityPlayer player) {
		return progressive(getStat(player, 4), 6.0d, 0.80d);
	}

	public static double getStaminaBonus(EntityPlayer player) {
		long physical = 0L;
		for (int i = 0; i < 4; i++) physical += getStat(player, i);
		return progressive(physical, 5.0d, 0.78d);
	}

	public static double getSpiRegenBonus(EntityPlayer player, double maximumChakra) {
		long stat = getStat(player, 5);
		double flat = progressive(stat, 0.0025d, 0.65d);
		double reserveRatio = 0.00005d + 0.00003d * Math.log10(1.0d + stat);
		return flat + Math.max(0.0d, maximumChakra) * reserveRatio;
	}

	public static int getChakraRegenLockTicks(EntityPlayer player) {
		return Math.max(20, 100 - (int)Math.round(10.0d * Math.log10(1.0d + getStat(player, 5))));
	}

	public static double getTaijutsuDamageBonus(EntityPlayer player) {
		return getEffectiveAttackBonus(player) * 0.55d
		 + progressive(getStat(player, 0), 0.02d, 0.72d);
	}

	public static String getStatEffectText(EntityPlayer player, int stat) {
			switch (stat) {
			case 0:
				return String.format(Locale.ROOT, "Move +%.3f | competitive dodge", getMovementBonus(player));
			case 1:
				return String.format(Locale.ROOT, "Attack +%.1f effective | melee/Taijutsu", getEffectiveAttackBonus(player));
			case 2:
				return String.format(Locale.ROOT, "Defense %.1f | reduction %.1f%%", getResistanceRating(player),
				 getResistanceReduction(player) * 100.0d);
			case 3:
				return String.format(Locale.ROOT, "Effective HP %.0f | visible %.0f", getEffectiveMaxHealth(player),
				 getDisplayedMaxHealth(player));
			case 4:
				return String.format(Locale.ROOT, "Maximum chakra +%.0f | faster casting", getChakraBonus(player));
			case 5:
				double maximum = Chakra.isStaminaMode(player)
				 ? PlayerTracker.getBattleXp(player) * 0.35d + 120.0d + getStaminaBonus(player)
				 : PlayerTracker.getBattleXp(player) * 0.5d + getChakraBonus(player);
				return String.format(Locale.ROOT, "Regen +%.1f/s | combat delay %.1fs", getSpiRegenBonus(player, maximum) * 20.0d,
				 getChakraRegenLockTicks(player) / 20.0d);
			default:
				return "";
		}
	}

	public static boolean canRegenerateChakra(EntityPlayer player) {
		return player.world.getTotalWorldTime() >= getStatsTag(player).getLong(CHAKRA_REGEN_LOCK_UNTIL);
	}

	public static float getAffinityJutsuXpModifier(EntityPlayer player, ItemJutsu.JutsuEnum.Type type) {
		return hasAffinity(player, type) ? 1.35f : 1.0f;
	}

	public static void setPoints(EntityPlayer player, long points) {
		long actual = Math.max(getSpentPoints(player), Math.max(0L, Math.min(MAX_TOTAL_POINTS, points)));
		NBTTagCompound tag = getStatsTag(player);
		tag.setLong(POINTS, actual);
		tag.removeTag(LEGACY_POINT_LIMIT);
		tag.setInteger(DATA_VERSION, CURRENT_DATA_VERSION);
		syncToClient(player);
	}

	public static void addPoints(EntityPlayer player, long amount) {
		long current = getPoints(player);
		long target;
		if (amount > 0L && current > MAX_TOTAL_POINTS - amount) target = MAX_TOTAL_POINTS;
		else if (amount < 0L && current < -amount) target = 0L;
		else target = current + amount;
		setPoints(player, target);
	}

	/**
	 * Legacy compatibility: point capacity no longer exists separately. Setting
	 * it now sets the single total allocation pool.
	 */
	@Deprecated
	public static void setPointLimit(EntityPlayer player, long limit) {
		setPoints(player, limit);
	}

	public static void setPersonalStatLimit(EntityPlayer player, int limit) {
		NBTTagCompound tag = getStatsTag(player);
		if (limit <= 0) tag.removeTag(PERSONAL_STAT_LIMIT);
		else tag.setInteger(PERSONAL_STAT_LIMIT, Math.min(MAX_STAT, limit));
		clampStatsToLimit(player);
		ensurePointBudgetCoversSpent(player);
		applyAttributes(player);
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
		clampStatsToLimit(player);
		ensurePointBudgetCoversSpent(player);
		applyAttributes(player);
		syncToClient(player);
	}

	public static void setRankLimit(World world, String rank, int limit) {
		RankLimitData data = RankLimitData.get(world);
		data.setLimit(normalizeRank(rank), Math.max(1, Math.min(MAX_STAT, limit)));
		data.markDirty();
	}

	public static void refresh(EntityPlayer player) {
		clampStatsToLimit(player);
		ensurePointBudgetCoversSpent(player);
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
			getStatsTag(player).setInteger(STAT_KEYS[stat], Math.max(0, Math.min(getStatLimit(player), value)));
			ensurePointBudgetCoversSpent(player);
			applyAttributes(player);
			syncToClient(player);
		}
	}

	public static void setSharinganStage(EntityPlayer player, int stage) {
		stage = Math.max(0, Math.min(3, stage));
		getStatsTag(player).setInteger(SHARINGAN_STAGE, stage);
		getStatsTag(player).setLong(SPECIAL_SPENT, stage);
		if (stage == 1) {
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemSharinganTomoe1.helmet));
		} else if (stage == 2) {
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemSharinganTomoe2.helmet));
		} else if (stage == 3) {
			player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(ItemSharinganTomoe3.helmet));
		}
		ensurePointBudgetCoversSpent(player);
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
		 + "\nRank: " + getRank(player) + " (rank cap " + getRankStatLimit(player) + ")"
		 + "\nPersonal stat cap: " + (getPersonalStatLimit(player) > 0 ? getPersonalStatLimit(player) : "rank default")
		 + "\nEffective stat cap: " + getStatLimit(player)
		 + "\nClan: " + getClan(player)
		 + "\nAffinity: " + getAffinity(player)
			 + "\nStat points: " + getAvailablePoints(player) + " available / " + getPoints(player) + " total"
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

	public static boolean isValidRank(String rank) {
		if (rank == null) return false;
		String clean = rank.trim();
		if (clean.equalsIgnoreCase("chunnin") || clean.equalsIgnoreCase("jounin")) return true;
		for (String entry : RANKS) if (entry.equalsIgnoreCase(clean)) return true;
		return false;
	}

	private static void clampStatsToLimit(EntityPlayer player) {
		NBTTagCompound tag = getStatsTag(player);
		int limit = getStatLimit(player);
		for (int i = 0; i < STAT_KEYS.length; i++) {
			int value = Math.max(0, Math.min(limit, tag.getInteger(STAT_KEYS[i])));
			if (tag.getInteger(STAT_KEYS[i]) != value) tag.setInteger(STAT_KEYS[i], value);
		}
	}

	private static void migratePlayerData(EntityPlayer player) {
		NBTTagCompound tag = getStatsTag(player);
		int version = tag.getInteger(DATA_VERSION);
		if (version >= CURRENT_DATA_VERSION) return;
		if (version < 2 && !tag.hasKey(SPECIAL_SPENT)) {
			tag.setLong(SPECIAL_SPENT, Math.max(0, Math.min(3, tag.getInteger(SHARINGAN_STAGE))));
		}
		long oldPoints = Math.max(0L, Math.min(MAX_TOTAL_POINTS, tag.getLong(POINTS)));
		long oldLimit = Math.max(0L, Math.min(MAX_TOTAL_POINTS, tag.getLong(LEGACY_POINT_LIMIT)));
		long usableBudget = oldLimit > 0L ? Math.min(oldPoints, oldLimit) : oldPoints;
		tag.setLong(POINTS, Math.max(getSpentPoints(player), usableBudget));
		tag.removeTag(LEGACY_POINT_LIMIT);
		tag.setInteger(DATA_VERSION, CURRENT_DATA_VERSION);
	}

	private static void ensurePointBudgetCoversSpent(EntityPlayer player) {
		long spent = Math.min(MAX_TOTAL_POINTS, getSpentPoints(player));
		NBTTagCompound tag = getStatsTag(player);
		if (getPoints(player) < spent) tag.setLong(POINTS, spent);
		tag.removeTag(LEGACY_POINT_LIMIT);
		tag.setInteger(DATA_VERSION, CURRENT_DATA_VERSION);
	}

	private static NBTTagCompound getStatsTag(EntityPlayer player) {
		NBTTagCompound data = player.getEntityData();
		if (!data.hasKey(ROOT, 10)) {
			data.setTag(ROOT, new NBTTagCompound());
		}
		return data.getCompoundTag(ROOT);
	}

	private static boolean upgrade(EntityPlayer player, int stat, int requestedAmount) {
		if (stat < 0 || stat >= STAT_KEYS.length || requestedAmount == 0 || getAvailablePoints(player) <= 0L) {
			return false;
		}
		NBTTagCompound tag = getStatsTag(player);
		int value = tag.getInteger(STAT_KEYS[stat]);
		long room = (long)getStatLimit(player) - value;
		long wanted = requestedAmount < 0 ? Long.MAX_VALUE : requestedAmount;
		int amount = (int)Math.min(Integer.MAX_VALUE, Math.min(room, Math.min(wanted, getAvailablePoints(player))));
		if (amount <= 0) return false;
		tag.setInteger(STAT_KEYS[stat], value + amount);
		syncToClient(player);
		applyAttributes(player);
		return true;
	}

	private static boolean upgradeSharingan(EntityPlayer player) {
		if (!getClan(player).equalsIgnoreCase("Uchiha") || getAvailablePoints(player) <= 0L || getSharinganStage(player) >= 3) {
			return false;
		}
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
		tag.setLong(POINTS, message.points);
		tag.removeTag(LEGACY_POINT_LIMIT);
		tag.setLong(SPECIAL_SPENT, message.specialSpent);
		tag.setString(CLAN, message.clan);
		tag.setString(AFFINITY, message.affinity);
		tag.setString(RANK, message.rank);
		tag.setInteger(SYNCED_RANK_LIMIT, message.rankLimit);
		if (message.personalStatLimit > 0) tag.setInteger(PERSONAL_STAT_LIMIT, message.personalStatLimit);
		else tag.removeTag(PERSONAL_STAT_LIMIT);
		tag.setInteger(SHARINGAN_STAGE, message.sharinganStage);
		for (int i = 0; i < STAT_KEYS.length; i++) {
			tag.setInteger(STAT_KEYS[i], message.stats[i]);
		}
	}

	private static void applyAttributes(EntityPlayer player) {
		applyModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, SPEED_UUID, "narutomod.stats.speed", getMovementBonus(player));
		applyModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, STRENGTH_UUID, "narutomod.stats.strength", getStrengthAttackBonus(player));
		applyModifier(player, SharedMonsterAttributes.ARMOR, RESIST_UUID, "narutomod.stats.resistance", 0.0d);
		applyModifier(player, SharedMonsterAttributes.MAX_HEALTH, HEALTH_UUID, "narutomod.stats.health", getHealthBonus(player));
	}

	private static double progressive(long stat, double scale, double exponent) {
		return scale * Math.pow(Math.max(0.0d, (double)stat), exponent);
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
			if (attacker != null && event.getSource().getImmediateSource() == attacker
			 && "player".equals(event.getSource().getDamageType())) {
				amount *= (float)getMeleeOverflowMultiplier(attacker);
			}
			if (defender != null && !event.getSource().isDamageAbsolute()) {
				amount *= (float)getResistanceDamageMultiplier(defender);
				amount *= (float)getHealthDamageMultiplier(defender);
				EntityPlayer speedSource = attacker;
				double attackerSpeed = speedSource != null ? Math.pow(Math.max(0, getStat(speedSource, 0)), 0.45d) : 0d;
				double defenderSpeed = Math.pow(Math.max(0, getStat(defender, 0)), 0.45d);
				boolean dodgeable = event.getSource().isProjectile() || attacker != null;
				double advantage = Math.max(0.0d, defenderSpeed - attackerSpeed);
				double dodgeChance = dodgeable ? 0.20d * advantage / (advantage + 25.0d) : 0d;
				if (dodgeChance > 0d && defender.getRNG().nextDouble() < dodgeChance) {
					event.setCanceled(true);
					return;
				}
			}
			event.setAmount(Math.max(0.001f, amount));
		}

		@SubscribeEvent
		public void onDamaged(LivingDamageEvent event) {
			if (!event.getEntityLiving().world.isRemote && event.getEntityLiving() instanceof EntityPlayer && event.getAmount() > 0f) {
				EntityPlayer player = (EntityPlayer)event.getEntityLiving();
				getStatsTag(player).setLong(CHAKRA_REGEN_LOCK_UNTIL, player.world.getTotalWorldTime() + getChakraRegenLockTicks(player));
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
				migratePlayerData(event.player);
				clampStatsToLimit(event.player);
				ensurePointBudgetCoversSpent(event.player);
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
				migratePlayerData(event.player);
				clampStatsToLimit(event.player);
				ensurePointBudgetCoversSpent(event.player);
				applyAttributes(event.player);
				syncToClient(event.player);
			}
		}
	}

	public static class UpgradeMessage implements IMessage {
		private int stat;
		private int amount;
		public UpgradeMessage() { }
		public UpgradeMessage(int statIn) { this(statIn, 1); }
		public UpgradeMessage(int statIn, int amountIn) { this.stat = statIn; this.amount = amountIn; }
		public static class Handler implements IMessageHandler<UpgradeMessage, IMessage> {
			@Override public IMessage onMessage(UpgradeMessage message, MessageContext context) {
				EntityPlayerMP player = context.getServerHandler().player;
				player.getServerWorld().addScheduledTask(() -> {
					if (message.stat == 6) upgradeSharingan(player);
					else upgrade(player, message.stat, message.amount);
				});
				return null;
			}
		}
		@Override public void toBytes(ByteBuf buf) { buf.writeInt(this.stat); buf.writeInt(this.amount); }
		@Override public void fromBytes(ByteBuf buf) { this.stat = buf.readInt(); this.amount = buf.readInt(); }
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
		private long points, specialSpent;
		private int sharinganStage, rankLimit, personalStatLimit;
		private int[] stats = new int[STAT_KEYS.length];
		private String clan = "None";
		private String affinity = "None";
		private String rank = "None";
		public SyncMessage() { }
		public SyncMessage(EntityPlayer player) {
			this.points = getPoints(player);
			this.specialSpent = getStatsTag(player).getLong(SPECIAL_SPENT);
			this.clan = getClan(player);
			this.affinity = getAffinity(player);
			this.rank = getRank(player);
			this.rankLimit = getRankStatLimit(player);
			this.personalStatLimit = getPersonalStatLimit(player);
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
			buf.writeLong(this.points);
			buf.writeLong(this.specialSpent);
			buf.writeInt(this.sharinganStage);
			buf.writeInt(this.rankLimit);
			buf.writeInt(this.personalStatLimit);
			for (int stat : this.stats) buf.writeInt(stat);
			ByteBufUtils.writeUTF8String(buf, this.clan);
			ByteBufUtils.writeUTF8String(buf, this.affinity);
			ByteBufUtils.writeUTF8String(buf, this.rank);
		}
		@Override public void fromBytes(ByteBuf buf) {
			this.points = buf.readLong();
			this.specialSpent = buf.readLong();
			this.sharinganStage = buf.readInt();
			this.rankLimit = buf.readInt();
			this.personalStatLimit = buf.readInt();
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
			boolean hadAllLimits = true;
			for (int i = 0; i < RANKS.size() && i < this.limits.length; i++) {
				String key = "limit_" + RANKS.get(i);
				if (nbt.hasKey(key)) {
					this.limits[i] = Math.max(1, Math.min(MAX_STAT, nbt.getInteger(key)));
				} else hadAllLimits = false;
			}
			if (!hadAllLimits || Arrays.equals(this.limits, PREVIOUS_DEFAULT_RANK_LIMITS) || Arrays.equals(this.limits, LEGACY_DEFAULT_RANK_LIMITS)) {
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
