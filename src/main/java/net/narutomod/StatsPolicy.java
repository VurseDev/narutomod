package net.narutomod;

/**
 * Pure math for the 2026-09 stats/jutsu economy rebalance (docs/stats_and_jutsu_economy_rebalance.md).
 * No Minecraft imports: every number the plan documents is verifiable headlessly, and tuning
 * happens here rather than in call sites. Callers gate on {@link #ENABLED}; when it is false the
 * legacy formulas in PlayerStats/Chakra/PlayerTracker apply unchanged.
 */
public final class StatsPolicy {
	/** Synced from ModConfig.BETTER_STAT_CURVES at preInit. Pure-math fixtures may flip this freely. */
	public static boolean ENABLED = true;
	/** Genin cap anchor: F(250)=1 so the curve is calibrated to the range that is actually played. */
	public static final double REFERENCE_STAT = 250.0d;
	public static final double STAT_EXPONENT = 0.6d;
	public static final double XP_CAP = 100000.0d;
	public static final double MOVE_SCALE = 0.025d;
	/** Physics sanity cap, not a math plateau: reached at Speed ~4953, above every rank cap. */
	public static final double MOVE_CAP = 0.15d;
	public static final double ATTACK_SCALE = 2.5d;
	public static final double RESIST_SCALE = 0.14d;
	public static final double HEALTH_SCALE = 8.0d;
	public static final double HEALTH_PER_XP = 0.005d;
	public static final double POOL_BASE = 150.0d;
	public static final double POOL_SCALE = 800.0d;
	/** Ninja XP keeps its ORIGINAL pool contribution (0.5x chakra / 0.35x stamina) per user direction. */
	public static final double CHAKRA_XP_SHARE = 0.5d;
	public static final double STAMINA_XP_SHARE = 0.35d;
	public static final double STAMINA_BASE = 120.0d;
	public static final double STAMINA_SCALE = 5.0d;
	public static final double STAMINA_EXPONENT = 0.78d;
	public static final double REGEN_BASE_FRACTION = 0.012d;
	public static final double REGEN_SPI_FRACTION = 0.012d;
	public static final double REGEN_MAX_FRACTION = 0.06d;
	public static final int REGEN_LOCK_BASE_TICKS = 40;
	public static final int REGEN_LOCK_MIN_TICKS = 10;
	public static final double REGEN_LOCK_SPI_SCALE = 8.0d;
	public static final double TAIJUTSU_STRENGTH_SCALE = 1.4d;
	public static final double TAIJUTSU_SPEED_SCALE = 0.8d;
	/** Jutsu damage scales with the Chakra stat so build choice reaches ninjutsu, not just taijutsu. */
	public static final double JUTSU_DAMAGE_FRACTION = 0.10d;

	/** Every 10x stats give ~3.98x effect, everywhere, with no plateau. */
	public static double statFactor(double stat) {
		return Math.pow(Math.max(stat, 0.0d) / REFERENCE_STAT, STAT_EXPONENT);
	}

	public static double xpFactor(double battleXp) {
		return Math.sqrt(Math.max(0.0d, Math.min(battleXp, XP_CAP)) / XP_CAP);
	}

	/** Attribute op-0 addition on MOVEMENT_SPEED (base 0.1), i.e. fraction of walk speed. */
	public static double movementAdd(double speedStat) {
		return Math.min(MOVE_CAP, MOVE_SCALE * statFactor(speedStat));
	}

	public static double attackBonus(double strengthStat) {
		return ATTACK_SCALE * statFactor(strengthStat);
	}

	public static double resistanceDamageFactor(double resistanceStat) {
		return 1.0d / (1.0d + RESIST_SCALE * statFactor(resistanceStat));
	}

	public static double maxHealth(double healthStat, double battleXp) {
		return 20.0d + HEALTH_SCALE * statFactor(healthStat)
		 + HEALTH_PER_XP * Math.max(0.0d, Math.min(battleXp, XP_CAP));
	}

	/** Stat term is intentionally LARGER than the legacy 6*stat^0.8 so investing the Chakra
	 *  stat visibly matters on top of Ninja XP's original 0.5*XP contribution. */
	public static double chakraPool(double chakraStat, double battleXp) {
		return CHAKRA_XP_SHARE * Math.max(0.0d, Math.min(battleXp, XP_CAP))
		 + POOL_BASE + POOL_SCALE * statFactor(chakraStat);
	}

	/** Legacy stamina pool preserved exactly (user kept the original XP share). */
	public static double staminaPool(double physicalStatSum, double battleXp) {
		return STAMINA_XP_SHARE * Math.max(0.0d, Math.min(battleXp, XP_CAP))
		 + STAMINA_BASE + STAMINA_SCALE * Math.pow(Math.max(physicalStatSum, 0.0d), STAMINA_EXPONENT);
	}

	public static double regenPerSecond(double spiStat, double pool) {
		return Math.min(REGEN_MAX_FRACTION * Math.max(pool, 0.0d),
		 Math.max(pool, 0.0d) * (REGEN_BASE_FRACTION + REGEN_SPI_FRACTION * statFactor(spiStat)));
	}

	public static int regenLockTicks(double spiStat) {
		return (int)Math.max(REGEN_LOCK_MIN_TICKS,
		 Math.round(REGEN_LOCK_BASE_TICKS - REGEN_LOCK_SPI_SCALE * statFactor(spiStat)));
	}

	public static double taijutsuBonus(double strengthStat, double speedStat) {
		return TAIJUTSU_STRENGTH_SCALE * statFactor(strengthStat) + TAIJUTSU_SPEED_SCALE * statFactor(speedStat);
	}

	public static double jutsuDamageMultiplier(double chakraStat) {
		return 1.0d + JUTSU_DAMAGE_FRACTION * statFactor(chakraStat);
	}

	/** Rank cooldown floors in ticks (docs §6); custom-balance jutsus pay at least this. */
	public static long getRankCooldownFloorTicks(char rank) {
		return rank == 'S' ? 600L : rank == 'A' ? 360L : rank == 'B' ? 240L
		 : rank == 'C' ? 140L : rank == 'D' ? 80L : 200L;
	}

	/** Base chakra cost per rank; original-mod jutsus are NOT touched (user direction 2026-09-26). */
	public static double rankBaseCost(char rank) {
		return rank == 'S' ? 200.0d : rank == 'A' ? 140.0d : rank == 'B' ? 90.0d
		 : rank == 'C' ? 55.0d : rank == 'D' ? 30.0d : 0.0d;
	}

	/** The ItemJutsu custom-balance cost curve, extracted verbatim so fixtures can verify it headlessly. */
	public static double customResourceCost(double baseCost, char rank, double maxResource,
	 float power, float masteryIn) {
		if (baseCost <= 0d) {
			return Math.max(0d, baseCost);
		}
		double mastery = Math.max(0.0d, Math.min((double)masteryIn, 1.0d));
		double excessResource = Math.max(0.0d, maxResource - 500.0d);
		double poolRatio = rank == 'S' ? 0.05d : rank == 'A' ? 0.035d : rank == 'B' ? 0.025d
		 : rank == 'C' ? 0.015d : rank == 'D' ? 0.01d : 0.02d;

		// Fixed costs fall to 65%; the large-pool surcharge falls to 20% at full mastery.
		double fixedEfficiency = 1.0d - 0.35d * mastery;
		double poolEfficiency = 1.0d - 0.80d * mastery;
		double charge = Math.max(1.0d, (double)power);
		double minimumCost = baseCost * fixedEfficiency;
		double calculatedCost = (minimumCost + excessResource * poolRatio * poolEfficiency) * charge;

		// Per-cast ceilings still make higher ranks meaningfully more expensive.
		double noviceCap = rank == 'S' ? 0.40d : rank == 'A' ? 0.32d : rank == 'B' ? 0.25d
		 : rank == 'C' ? 0.18d : rank == 'D' ? 0.12d : 0.22d;
		double masterCap = rank == 'S' ? 0.15d : rank == 'A' ? 0.11d : rank == 'B' ? 0.08d
		 : rank == 'C' ? 0.06d : rank == 'D' ? 0.04d : 0.07d;
		double maximumCost = Math.max(0.0d, maxResource) * (noviceCap + (masterCap - noviceCap) * mastery);
		return Math.max(minimumCost, Math.min(calculatedCost, Math.max(minimumCost, maximumCost)));
	}

	private StatsPolicy() {
	}
}
