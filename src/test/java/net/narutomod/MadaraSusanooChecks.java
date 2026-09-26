package net.narutomod;

/** Deterministic policy/animation checks; these are not a substitute for a two-client in-game test. */
public final class MadaraSusanooChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(!MadaraSusanooPolicy.canUnlock(-1, 1e9, true, true), "negative stage rejected");
        check(!MadaraSusanooPolicy.canUnlock(6, 1e9, true, true), "stage beyond Perfect rejected");
        check(!MadaraSusanooPolicy.canUnlock(5, 1e9, false, false), "MS cannot unlock Perfect");
        check(!MadaraSusanooPolicy.canUnlock(5, 1e9, false, true), "creative MS does not silently become EMS");
        check(!MadaraSusanooPolicy.canUnlock(5, 39999, true, false), "EMS still needs mastery");
        check(MadaraSusanooPolicy.canUnlock(5, 40000, true, false), "mastered EMS permits Perfect");
        check(MadaraSusanooPolicy.lockReason(5, 100000, false, false) == MadaraSusanooPolicy.LockReason.ETERNAL_REQUIRED,
            "100,000 Ninja XP with non-eternal Madara eyes reports the eye requirement, not training");
        check(MadaraSusanooPolicy.lockReason(5, 100000, true, false) == MadaraSusanooPolicy.LockReason.NONE
            && MadaraSusanooPolicy.canUnlock(5, 100000, true, false), "100,000 Ninja XP and EMS unlock Perfect");
        check(MadaraSusanooPolicy.lockReason(5, 39999, true, false) == MadaraSusanooPolicy.LockReason.XP_REQUIRED,
            "undertrained EMS reports the Ninja XP requirement");
        check(MadaraSusanooPolicy.lockReason(5, 0, false, true) == MadaraSusanooPolicy.LockReason.ETERNAL_REQUIRED,
            "creative still reports the missing Eternal eyes");
        check(MadaraSusanooPolicy.lockReason(6, 100000, true, false) == MadaraSusanooPolicy.LockReason.MAX_STAGE,
            "upgrading an already Perfect Susanoo reports the maximum stage");
        check(MadaraSusanooPolicy.lockReason(-1, 100000, true, true) == MadaraSusanooPolicy.LockReason.INVALID_STAGE
            && MadaraSusanooPolicy.lockReason(7, 100000, true, true) == MadaraSusanooPolicy.LockReason.INVALID_STAGE,
            "invalid stages remain rejected independently of XP and creative mode");
        check(MadaraSusanooPolicy.canUnlock(4, 30000, false, false), "MS permits timed armored burst");
        for (int stage = 0; stage < MadaraSusanooPolicy.STAGE_COUNT; stage++) {
            check(!MadaraSusanooPolicy.canUnlock(stage, MadaraSusanooPolicy.requiredXp(stage) - 1, true, false), "stage XP boundary " + stage);
            check(MadaraSusanooPolicy.canUnlock(stage, MadaraSusanooPolicy.requiredXp(stage), true, false), "stage exact threshold " + stage);
            check(MadaraSusanooPolicy.height(stage) > MadaraSusanooPolicy.riderHeight(stage) + 1.8, "pilot stays inside collision volume " + stage);
            check(MadaraSusanooPolicy.chakraPerSecond(stage) >= 30, "no free sustained form " + stage);
            if (stage > 0) {
                check(MadaraSusanooPolicy.width(stage) >= MadaraSusanooPolicy.width(stage - 1), "monotonic collision width");
                check(MadaraSusanooPolicy.height(stage) >= MadaraSusanooPolicy.height(stage - 1), "monotonic collision height");
            }
        }
        check(MadaraSusanooPolicy.ARMORED_BURST_TICKS == 400, "MS armored burst is twenty seconds");
        check(MadaraSusanooPolicy.ARMORED_RECOVERY_TICKS > MadaraSusanooPolicy.ARMORED_BURST_TICKS, "burst is not endlessly retriggerable");
        check(MadaraSusanooPolicy.transitionProgress(-1) == 0, "transition clamps future epoch");
        check(MadaraSusanooPolicy.transitionProgress(0) == 0, "transition starts exactly zero");
        check(MadaraSusanooPolicy.transitionProgress(12) == .5f, "transition midpoint");
        check(MadaraSusanooPolicy.transitionProgress(24) == 1, "transition reaches exact completion");
        check(MadaraSusanooPolicy.transitionProgress(240) == 1, "late observer sees completed transition");
        for (int profile = 0; profile <= SusanooCastProfile.TEMPORAL; profile++) {
            check(SusanooCastProfile.primarySeal(profile, SusanooCastProfile.IDLE, 0) == SusanooCastProfile.NEUTRAL, "idle hands neutral");
            check(SusanooCastProfile.primarySeal(profile, SusanooCastProfile.RELEASE, 0) == SusanooCastProfile.OPEN_PALM, "release hands open");
            check(SusanooCastProfile.primarySeal(profile, SusanooCastProfile.CANCEL, 0) == SusanooCastProfile.NEUTRAL, "cancel not an attack");
            int finalSeal = SusanooCastProfile.primarySeal(profile, SusanooCastProfile.PREPARE, 17);
            check(SusanooCastProfile.primarySeal(profile, SusanooCastProfile.HOLD, 0) == finalSeal, "hold preserves final seal");
            check(SusanooCastProfile.primarySeal(profile, SusanooCastProfile.HOLD, 600) == finalSeal, "long charge never cycles back to start");
            int rear = SusanooCastProfile.secondarySeal(profile, SusanooCastProfile.HOLD, 600);
            check(rear != finalSeal && rear > 0 && rear <= SusanooCastProfile.CLASP, "rear pair has complementary seal");
        }
        check(SusanooCastProfile.sanitize(100) == SusanooCastProfile.GENERIC, "unknown packet profile has safe fallback");
        check(SusanooCastProfile.blendWeight(SusanooCastProfile.PREPARE, 0) == 0, "wind-up has no initial arm snap");
        check(SusanooCastProfile.blendWeight(SusanooCastProfile.PREPARE, 5) == 1, "wind-up reaches pose");
        check(SusanooCastProfile.blendWeight(SusanooCastProfile.HOLD, 10000) == 1, "charge pose persists");
        check(SusanooCastProfile.blendWeight(SusanooCastProfile.RELEASE, 12) == 0, "release restores stance");
        check(SusanooCastProfile.blendWeight(SusanooCastProfile.CANCEL, 8) == 0, "cancel restores stance");
        System.out.println("Madara Susanoo policy/choreography: " + checks + " checks passed.");
    }
}
