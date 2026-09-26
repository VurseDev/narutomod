package net.narutomod;

/** Network-stable, deterministic hand-seal choreography. This class has no client/game dependencies. */
public final class SusanooCastProfile {
    public static final int IDLE = 0, PREPARE = 1, HOLD = 2, RELEASE = 3, CANCEL = 4;
    public static final int NEUTRAL = 0, TIGER = 1, RAM = 2, SNAKE = 3, BIRD = 4, CLASP = 5, OPEN_PALM = 6;
    public static final int FRONT_ARMS = 1, REAR_ARMS = 2, ALL_ARMS = 3;
    public static final int PREPARE_TICKS = 18, RELEASE_TICKS = 12, CANCEL_TICKS = 8;
    public static final int GENERIC = 0, FIRE = 1, WATER = 2, LIGHTNING = 3, EARTH = 4, WIND = 5, ILLUSION = 6, TEMPORAL = 7;
    private static final int[][] SEQUENCES = {
        {RAM, SNAKE, TIGER}, {SNAKE, RAM, TIGER}, {TIGER, SNAKE, BIRD}, {RAM, SNAKE, CLASP},
        {SNAKE, RAM, CLASP}, {RAM, BIRD, TIGER}, {SNAKE, TIGER, CLASP}, {RAM, SNAKE, TIGER}
    };

    private SusanooCastProfile() { }

    public static int sanitize(int profile) { return profile >= 0 && profile < SEQUENCES.length ? profile : GENERIC; }

    public static int primarySeal(int profile, int phase, float age) {
        if (phase == IDLE || phase == CANCEL) return NEUTRAL;
        if (phase == RELEASE) return OPEN_PALM;
        int[] sequence = SEQUENCES[sanitize(profile)];
        int step = phase == HOLD ? sequence.length - 1 : Math.max(0, Math.min(sequence.length - 1, (int)age / 6));
        return sequence[step];
    }

    public static int secondarySeal(int profile, int phase, float age) {
        if (phase == IDLE || phase == CANCEL) return NEUTRAL;
        if (phase == RELEASE) return OPEN_PALM;
        // Complementary sequences communicate Madara's two independently articulating bodies.
        int seal = primarySeal(profile, phase, age);
        return seal == TIGER ? RAM : seal == RAM ? SNAKE : seal == SNAKE ? BIRD : TIGER;
    }

    public static int duration(int phase) {
        return phase == PREPARE ? PREPARE_TICKS : phase == RELEASE ? RELEASE_TICKS : phase == CANCEL ? CANCEL_TICKS : 0;
    }

    public static float clamp01(float value) { return Math.max(0f, Math.min(1f, value)); }

    public static float blendWeight(int phase, float age) {
        if (phase == IDLE) return 0f;
        if (phase == PREPARE) return smooth(clamp01(age / 5f));
        if (phase == CANCEL) return 1f - smooth(clamp01(age / CANCEL_TICKS));
        if (phase == RELEASE) return 1f - smooth(clamp01((age - 5f) / 7f));
        return 1f;
    }

    public static float smooth(float value) { return value * value * (3f - 2f * value); }
}
