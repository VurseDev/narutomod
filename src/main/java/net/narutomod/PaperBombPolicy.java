package net.narutomod;

/** Fixed ammunition costs; power/mastery never multiply or waive ammunition. */
public final class PaperBombPolicy {
    public static final int VOLLEY=0, CIRCUIT=1, SWARM=2, BREACH=3;
    private static final int[] BOMBS={3,4,6,5}, COOLDOWN={160,360,440,320};
    private static final double[] CHAKRA={60,100,160,130};
    private PaperBombPolicy() { }
    public static boolean valid(int mode) { return mode>=0&&mode<4; }
    public static int bombs(int mode) { return BOMBS[mode]; }
    public static int cooldown(int mode) { return COOLDOWN[mode]; }
    public static double chakra(int mode) { return CHAKRA[mode]; }
    public static float damage(int mode,int previousHits,double distance) {
        float base=mode==BREACH?22:mode==CIRCUIT?9:mode==SWARM?7:10;
        return base*(previousHits==0?1:previousHits==1?.55f:.3f)*(float)Math.max(0,1-distance/(mode==BREACH?5:3.5));
    }
    public static boolean inCone(double dot) { return dot>=.5; }
}
