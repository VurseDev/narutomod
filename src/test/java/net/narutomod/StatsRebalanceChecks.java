package net.narutomod;

/** Headless fixtures for docs/stats_and_jutsu_economy_rebalance.md: re-derives every documented
 *  number, sweeps monotonicity/bounds 1 -> 100M, and re-checks the cost-curve table on the new pools.
 *  2026-09-26 correction: Ninja XP keeps its ORIGINAL chakra/HP/stamina contributions and the
 *  chakra STAT term is larger than legacy so stat investment visibly matters. */
public final class StatsRebalanceChecks {
	private static int checks;
	private static void check(boolean condition, String name) {checks++; if(!condition) throw new AssertionError(name);}
	private static void close(double a,double b,String name) {check(Math.abs(a-b)<1e-9,name+": "+a+" vs "+b);}
	private static void near(double a,double b,String name) {check(Math.abs(a-b)<0.061,name+": "+a+" vs "+b);}

	private static final double[] STATS = {0d,100d,250d,600d,1200d,2500d,10000d,100000d,1000000d,100000000d};

	public static void main(String[] args) {
		factorAnchor();
		documentedTable();
		monotonicityAndBounds();
		economyTable();
		floorsAndBaseCosts();
		System.out.println("StatsRebalanceChecks: "+checks+" checks passed.");
	}

	private static void factorAnchor() {
		close(StatsPolicy.statFactor(250d),1.0d,"F(Genin cap)=1");
		close(StatsPolicy.statFactor(2500d),3.9810717055349722d,"F(Hokage cap)");
		close(StatsPolicy.statFactor(1000000d)/StatsPolicy.statFactor(100000d),Math.pow(10d,0.6d),"100k vs 1M keeps a 3.98x gap");
		close(StatsPolicy.statFactor(-5d),0.0d,"negative stats clamp to 0");
		close(StatsPolicy.xpFactor(0d),0.0d,"xpFactor 0");
		close(StatsPolicy.xpFactor(100000d),1.0d,"xpFactor cap");
		close(StatsPolicy.xpFactor(500000d),1.0d,"xpFactor clamps above cap");
	}

	/** The exact tables printed in docs section 4 (restored-XP economy). */
	private static void documentedTable() {
		double[] f      = {0d,0.5770799623628855d,1d,1.6909343439651996d,2.5629771980294445d,3.9810717055349722d,
		 9.146101038546526d,36.4112840605216d,144.95593273553908d,2297.396709994069d};
		double[] move   = {0d,0.014426999059072139d,0.025d,0.04227335859912999d,0.06407442995073612d,0.09952679263837431d,
		 0.15d,0.15d,0.15d,0.15d};
		double[] atk    = {0d,1.4426999059072139d,2.5d,4.227335859912999d,6.407442995073611d,9.95267926383743d,
		 22.865252596366314d,91.02821015130401d,362.3898318388477d,5743.491774985173d};
		double[] resist = {1d,0.9252481005353427d,0.8771929824561403d,0.8085833985907838d,0.7359343763747643d,
		 0.6421163997187551d,0.43850914609209346d,0.16399949454870735d,0.0469619590567939d,0.00309947255613023d};
		double[] pool   = {150d,611.6639698903084d,950d,1502.7474751721597d,2200.3817584235558d,3334.8573644279777d,
		 7466.880830837221d,29279.027248417282d,116114.74618843127d,1838067.3679952554d};
		for (int i=0;i<STATS.length;i++) {
			double s = STATS[i];
			close(StatsPolicy.statFactor(s),f[i],"F("+s+")");
			close(StatsPolicy.movementAdd(s),move[i],"move("+s+")");
			close(StatsPolicy.attackBonus(s),atk[i],"attack("+s+")");
			close(StatsPolicy.resistanceDamageFactor(s),resist[i],"resist("+s+")");
			close(StatsPolicy.chakraPool(s,0d),pool[i],"pool("+s+")");
		}
		// Ninja XP keeps its original 0.5x pool share: at 100k XP the pool gains +50,000 over the stat term.
		close(StatsPolicy.chakraPool(250d,100000d),950d+50000d,"pool Genin + XP100k (original 0.5x XP share)");
		close(StatsPolicy.maxHealth(250d,0d),28.0d,"HP Genin XP0");
		close(StatsPolicy.maxHealth(2500d,100000d),551.8485736442798d,"HP Hokage XP100k (original 0.005 share)");
		close(StatsPolicy.maxHealth(0d,100000d),520.0d,"HP 0-stat XP100k");
		close(StatsPolicy.staminaPool(1000d,0d),1213.8808119747764d,"stamina Genin (legacy formula)");
		close(StatsPolicy.staminaPool(10000d,0d),6711.283692782037d,"stamina Hokage (legacy formula)");
		close(StatsPolicy.regenPerSecond(250d,950d),22.8d,"regen Genin");
		close(StatsPolicy.regenPerSecond(2500d,3334.8573644279777d),199.33396391936554d,"regen Hokage");
		close(StatsPolicy.regenPerSecond(1000000d,116114.74618843127d),6966.884771305876d,"regen 1M capped at 6% of pool");
		check(StatsPolicy.regenPerSecond(100000000d,1838067.3679952554d)<=0.06d*1838067.3679952554d+1e-9,"6% pool/s cap binds at extreme");
		close(StatsPolicy.regenLockTicks(250d),32,"lock Genin");
		close(StatsPolicy.regenLockTicks(100d),35,"lock None");
		close(StatsPolicy.regenLockTicks(2500d),10,"lock Hokage floor");
		close(StatsPolicy.taijutsuBonus(250d,250d),2.2d,"taijutsu Genin");
		close(StatsPolicy.taijutsuBonus(2500d,2500d),8.75835775217694d,"taijutsu Hokage");
		close(StatsPolicy.jutsuDamageMultiplier(250d),1.1d,"jutsu mult Genin");
		close(StatsPolicy.jutsuDamageMultiplier(2500d),1.3981071705534973d,"jutsu mult Hokage");
	}

	private static void monotonicityAndBounds() {
		double s = 1.0d;
		while (s < 100000000.0d) {
			double next = s*1.3721d;
			check(StatsPolicy.movementAdd(next)>=StatsPolicy.movementAdd(s)-1e-12,"move monotone at "+s);
			check(StatsPolicy.attackBonus(next)>=StatsPolicy.attackBonus(s)-1e-9,"attack monotone at "+s);
			check(StatsPolicy.resistanceDamageFactor(next)<=StatsPolicy.resistanceDamageFactor(s)+1e-12,"resist monotone at "+s);
			check(StatsPolicy.resistanceDamageFactor(next)>0.0001d,"resist never reaches 0 at "+s);
			check(StatsPolicy.maxHealth(next,0d)>=StatsPolicy.maxHealth(s,0d)-1e-9,"HP monotone at "+s);
			check(StatsPolicy.chakraPool(next,0d)>=StatsPolicy.chakraPool(s,0d)-1e-9,"pool monotone at "+s);
			check(StatsPolicy.jutsuDamageMultiplier(next)>=StatsPolicy.jutsuDamageMultiplier(s)-1e-12,"jutsu mult monotone at "+s);
			check(Double.isFinite(StatsPolicy.statFactor(next))&&Double.isFinite(StatsPolicy.chakraPool(next,0d)),"finite at "+s);
			s = next;
		}
		check(StatsPolicy.movementAdd(100000000d)==StatsPolicy.MOVE_CAP,"move clamp binds at extreme");
		check(StatsPolicy.movementAdd(2500d)<StatsPolicy.MOVE_CAP,"no clamp inside rank range");
	}

	/** docs section 5: the custom-balance cost curve evaluated on the restored pools (XP = 0). */
	private static void economyTable() {
		double[] pools = {950d,1502.7474751721597d,2200.3817584235558d,3334.8573644279777d};
		char[] ranks = {'D','C','B','A','S'};
		double[] bases = {30d,55d,90d,140d,200d};
		double[] expectGenin = {34.5d,61.75d,101.25d,155.75d,222.5d};
		double[] expectHokage = {58.3d,97.5d,160.9d,239.2d,341.7d};
		double[] expectHokageM1 = {25.2d,44.3d,72.7d,110.8d,158.3d};
		for (int i=0;i<ranks.length;i++) {
			near(StatsPolicy.customResourceCost(bases[i],ranks[i],pools[0],1f,0f),expectGenin[i],"cost "+ranks[i]+" @Genin");
			near(StatsPolicy.customResourceCost(bases[i],ranks[i],pools[3],1f,0f),expectHokage[i],"cost "+ranks[i]+" @Hokage");
			near(StatsPolicy.customResourceCost(bases[i],ranks[i],pools[3],1f,1f),expectHokageM1[i],"cost "+ranks[i]+" @Hokage m1");
			check(StatsPolicy.customResourceCost(bases[i],ranks[i],pools[0],1f,0f)<=pools[0]*0.45d,"rank "+ranks[i]+" affordable at Genin");
		}
		check(Math.floor(950d/StatsPolicy.customResourceCost(140d,'A',950d,1f,0f))>=3d,"A-rank cast count Genin");
		check(Math.floor(3334.8573644279777d/StatsPolicy.customResourceCost(140d,'A',3334.8573644279777d,1f,0f))>=8d,"A-rank cast count Hokage");
		check(Math.floor(950d/StatsPolicy.customResourceCost(200d,'S',950d,1f,0f))>=2d,"S-rank cast count Genin");
	}

	private static void floorsAndBaseCosts() {
		char[] ranks = {'D','C','B','A','S'};
		long[] floors = {80L,140L,240L,360L,600L};
		double[] bases = {30d,55d,90d,140d,200d};
		for (int i=0;i<ranks.length;i++) {
			check(StatsPolicy.getRankCooldownFloorTicks(ranks[i])==floors[i],"floor "+ranks[i]);
			check(StatsPolicy.rankBaseCost(ranks[i])==bases[i],"base cost "+ranks[i]);
		}
		check(StatsPolicy.getRankCooldownFloorTicks('X')==200L,"unranked floor");
	}
}
