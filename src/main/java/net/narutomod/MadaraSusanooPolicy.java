package net.narutomod;

/** Balance/geometry policy kept separate so advancement and animation boundaries can be tested headlessly. */
public final class MadaraSusanooPolicy {
    public enum LockReason { NONE, INVALID_STAGE, MAX_STAGE, ETERNAL_REQUIRED, XP_REQUIRED }
    public static final int STAGE_COUNT = 6, TRANSITION_TICKS = 24, ARMORED_BURST_TICKS = 400, ARMORED_RECOVERY_TICKS = 1200;
    private static final double[] XP = {2000, 5000, 10000, 20000, 30000, 40000};
    private static final float[] WIDTH = {2.4f, 3.2f, 4f, 4.8f, 5.6f, 8f};
    private static final float[] HEIGHT = {3f, 5f, 6f, 9f, 10f, 16f};
    private static final double[] CHAKRA = {30, 45, 60, 70, 85, 115};
    private static final String[] ASSETS = {"skeletal", "skeletal", "humanoid", "humanoid", "armored", "perfect"};

    private MadaraSusanooPolicy() { }
    public static int clampStage(int stage) { return Math.max(0, Math.min(STAGE_COUNT - 1, stage)); }
    public static double requiredXp(int stage) { return XP[clampStage(stage)]; }
    public static float width(int stage) { return WIDTH[clampStage(stage)]; }
    public static String assetName(int stage) { return ASSETS[clampStage(stage)]; }
    public static float height(int stage) { return HEIGHT[clampStage(stage)]; }
    public static double chakraPerSecond(int stage) { return CHAKRA[clampStage(stage)]; }
    public static float modelScale(int stage) { return height(stage) / (stage < 3 ? 1.25f : 2f); }
    public static double riderHeight(int stage) { return stage < 3 ? .35 : stage == 5 ? 12.4 : stage == 4 ? 5.2 : 4.5; }
    public static boolean canUnlock(int stage, double xp, boolean eternal, boolean creative) {
        return lockReason(stage, xp, eternal, creative) == LockReason.NONE;
    }
    public static LockReason lockReason(int stage, double xp, boolean eternal, boolean creative) {
        if (stage == STAGE_COUNT) return LockReason.MAX_STAGE;
        if (stage < 0 || stage > STAGE_COUNT) return LockReason.INVALID_STAGE;
        if (stage == STAGE_COUNT - 1 && !eternal) return LockReason.ETERNAL_REQUIRED;
        if (!creative && !(xp >= requiredXp(stage))) return LockReason.XP_REQUIRED;
        return LockReason.NONE;
    }
    public static float transitionProgress(long elapsed) {
        return SusanooCastProfile.smooth(SusanooCastProfile.clamp01((float)elapsed / TRANSITION_TICKS));
    }
}
