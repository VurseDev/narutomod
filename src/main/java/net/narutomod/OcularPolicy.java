package net.narutomod;

import java.util.UUID;
import net.narutomod.item.ItemSharingan;

/** Rules shared by the authoritative surgery and its tests. No creative-mode XP bypass. */
public final class OcularPolicy {
    public static final int REQUIRED_HEALING_XP=3000, CHANNEL_TICKS=100, RECOVERY_TICKS=200, SESSION_TICKS=1200;
    public static final double CHAKRA_COST=100d;
    private OcularPolicy(){}
    public static boolean qualified(double xp){return Double.isFinite(xp)&&xp>=REQUIRED_HEALING_XP;}
    public static boolean serverOwnedTag(String tag){return tag!=null&&(OcularState.KEY.equals(tag)||tag.startsWith("Ocular"));}
    public static boolean validLayout(int left,int right,int size){return size>0&&left>=0&&right>=0&&left<size&&right<size&&(left==0||left!=right);}
    public static boolean same(OcularState.Eye a,OcularState.Eye b){return a==null?b==null:b!=null&&a.id.equals(b.id);}
    private static boolean sameForm(OcularState.Eye a,OcularState.Eye b){return same(a,b)&&(a==null||net.minecraft.item.ItemStack.areItemStacksEqual(a.stack,b.stack));}
    public static boolean changed(OcularState old,OcularState.Eye left,OcularState.Eye right){return !sameForm(old.eyes[0],left)||!sameForm(old.eyes[1],right);}
    public static boolean canDeactivate(boolean sharingan,boolean uchiha){return !sharingan||uchiha;}
    /** Upkeep per second when this eye is active and uncovered; the menu uses the same values. */
    public static double upkeep(OcularState.Eye eye,UUID wearer){
        if(eye==null||ItemSharingan.isBlinded(eye.stack))return 0d;
        boolean own=eye.donor.equals(wearer);
        return eye.rinnegan()?(own?20d:40d):eye.byakugan()?(own?10d:20d):eye.mangekyo()?(own?10d:30d):eye.sharingan()?(own?2.5d:10d):0d;
    }
    public static boolean abilitiesAvailable(long recoveryEnd,long now,boolean surgeryBusy){return !surgeryBusy&&recoveryEnd<=now;}
}
