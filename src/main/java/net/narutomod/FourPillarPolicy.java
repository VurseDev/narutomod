package net.narutomod;

/** Charge UI is 0..100; internal power remains 1..2 for the established chakra economy. */
public final class FourPillarPolicy {
    private FourPillarPolicy() { }
    public static float charge(float power) {return Float.isFinite(power)?Math.max(0,Math.min(1,power-1)):0;}
    public static int percent(float power) {return Math.round(charge(power)*100);}
    public static float damage(double ninjaXp,float mastery,float power) {
        double xp=Double.isFinite(ninjaXp)?Math.max(0,Math.min(100000,ninjaXp)):0;
        float m=Float.isFinite(mastery)?Math.max(0,Math.min(1,mastery)):0;
        return (float)((6+12*charge(power))*(1+.35*Math.sqrt(xp/100000))*(1+.25*m));
    }
    public static int duration(float power) {return 60+Math.round(50*charge(power));}
    public static float height(double height) {return (float)Math.max(4.5,Math.min(96,Double.isFinite(height)?height*1.35+1:4.5));}
    public static float radius(double width) {return (float)Math.max(2,Math.min(64,Double.isFinite(width)?width*.75+1.3:2));}
    public static float radiusFromPacket(int encoded) {return encoded==0?2:encoded/1000f;}
}
