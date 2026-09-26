package net.narutomod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Modern persistent player state for the 1.20.1 port.
 *
 * The 1.12 implementation stored most of this data in entity NBT and kept a
 * second client-side mirror.  In the port the persistent tag is the source of
 * truth and CoreNetwork sends a small snapshot to the client when it changes.
 */
public final class CoreData {
    public static final String ROOT = "narutomod_core";
    public static final String[] STAT_KEYS = {"speed", "strength", "resistance", "health", "chakra", "spi"};
    public static final String[] STAT_LABELS = {"Speed", "Strength", "Resistance", "Health", "Chakra Max", "SPI"};
    public static final long MAX_POINTS = 600_000_003L;
    public static final int MAX_STAT = 100_000_000;

    private static final int[] DEFAULT_RANK_LIMITS = {100, 300, 2_000, 20_000, 100_000};
    private static final String[] RANKS = {"None", "Genin", "Chunin", "Jonin", "Hokage"};

    private CoreData() {
    }

    public static CompoundTag tag(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT, new CompoundTag());
        }
        CompoundTag data = persistent.getCompound(ROOT);
        if (!data.contains("version")) {
            data.putInt("version", 1);
            data.putString("rank", "None");
            data.putString("clan", "None");
            data.putString("affinity", "None");
            data.putLong("points", 0L);
            data.putLong("chakra", 0L);
            data.putLong("battle_xp", 0L);
            persistent.put(ROOT, data);
        }
        return data;
    }

    public static CompoundTag snapshot(Player player) {
        return tag(player).copy();
    }

    public static void applySnapshot(Player player, CompoundTag snapshot) {
        player.getPersistentData().put(ROOT, snapshot.copy());
    }

    public static String statKey(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replace("_", "");
        switch (normalized) {
            case "speed":
            case "spd":
                return "speed";
            case "strength":
            case "str":
                return "strength";
            case "resistance":
            case "res":
            case "defense":
            case "def":
                return "resistance";
            case "health":
            case "hp":
                return "health";
            case "chakra":
            case "chakramax":
            case "cha":
                return "chakra";
            case "spi":
            case "spirit":
                return "spi";
            default:
                return null;
        }
    }

    public static int statIndex(String name) {
        String key = statKey(name);
        if (key == null) return -1;
        for (int i = 0; i < STAT_KEYS.length; i++) {
            if (STAT_KEYS[i].equals(key)) return i;
        }
        return -1;
    }

    public static long getStat(Player player, String stat) {
        String key = statKey(stat);
        return key == null ? 0L : Math.max(0L, tag(player).getLong(key));
    }

    public static void setStat(Player player, String stat, long value) {
        String key = statKey(stat);
        if (key == null) return;
        tag(player).putLong(key, Math.max(0L, Math.min(MAX_STAT, value)));
        clampToLimit(player);
    }

    public static long getPoints(Player player) {
        return Math.max(0L, tag(player).getLong("points"));
    }

    public static void setPoints(Player player, long value) {
        CompoundTag data = tag(player);
        data.putLong("points", Math.max(getSpent(player), Math.min(MAX_POINTS, Math.max(0L, value))));
    }

    public static void addPoints(Player player, long amount) {
        setPoints(player, getPoints(player) + amount);
    }

    public static long getSpent(Player player) {
        CompoundTag data = tag(player);
        long spent = 0L;
        for (String key : STAT_KEYS) spent += Math.max(0L, data.getLong(key));
        return spent;
    }

    public static long getAvailable(Player player) {
        return Math.max(0L, getPoints(player) - getSpent(player));
    }

    public static String getRank(Player player) {
        String rank = tag(player).getString("rank");
        return rank.isEmpty() ? "None" : normalizeRank(rank);
    }

    public static void setRank(Player player, String rank) {
        tag(player).putString("rank", normalizeRank(rank));
        clampToLimit(player);
    }

    public static String normalizeRank(String rank) {
        for (String value : RANKS) if (value.equalsIgnoreCase(rank)) return value;
        return "None";
    }

    public static int getRankLimit(Player player) {
        int index = 0;
        String rank = getRank(player);
        for (int i = 0; i < RANKS.length; i++) if (RANKS[i].equalsIgnoreCase(rank)) index = i;
        return DEFAULT_RANK_LIMITS[index];
    }

    public static int getLimit(Player player) {
        int personal = tag(player).getInt("personal_limit");
        return personal > 0 ? Math.min(MAX_STAT, personal) : getRankLimit(player);
    }

    public static void setPersonalLimit(Player player, int limit) {
        if (limit <= 0) tag(player).remove("personal_limit");
        else tag(player).putInt("personal_limit", Math.min(MAX_STAT, limit));
        clampToLimit(player);
    }

    public static void clampToLimit(Player player) {
        int limit = getLimit(player);
        CompoundTag data = tag(player);
        for (String key : STAT_KEYS) data.putLong(key, Math.min(limit, Math.max(0L, data.getLong(key))));
        setPoints(player, getPoints(player));
    }

    public static long getBattleXp(Player player) {
        return Math.max(0L, tag(player).getLong("battle_xp"));
    }

    public static void setBattleXp(Player player, long value) {
        tag(player).putLong("battle_xp", Math.max(0L, Math.min(100_000L, value)));
    }

    public static double maxChakra(Player player) {
        double battle = getBattleXp(player) * 0.5d;
        double stat = Math.sqrt(getStat(player, "chakra")) * 6.0d;
        return Math.max(120.0d, 120.0d + battle + stat);
    }

    public static double chakra(Player player) {
        return Math.max(0.0d, Double.longBitsToDouble(tag(player).getLong("chakra")));
    }

    public static void setChakra(Player player, double amount) {
        double clamped = Math.max(0.0d, Math.min(maxChakra(player), amount));
        tag(player).putLong("chakra", Double.doubleToLongBits(clamped));
    }

    public static boolean consumeChakra(Player player, double amount) {
        if (amount <= 0.0d) return true;
        double current = chakra(player);
        if (current < amount) return false;
        setChakra(player, current - amount);
        return true;
    }

    public static double spiRegenPerSecond(Player player) {
        double spi = getStat(player, "spi");
        return 1.0d + Math.log10(1.0d + spi) * 2.0d + Math.sqrt(spi) * 0.02d;
    }

    public static void ensureChakra(Player player) {
        CompoundTag data = tag(player);
        if (!data.getBoolean("chakra_initialized")) {
            setChakra(player, maxChakra(player));
            data.putBoolean("chakra_initialized", true);
        } else {
            setChakra(player, chakra(player));
        }
    }

    public static void sync(ServerPlayer player) {
        CoreNetwork.send(player);
    }
}
