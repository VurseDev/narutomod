package net.narutomod;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.narutomod.entity.EntitySusanooMadara;
import net.narutomod.item.ItemJutsu;

/** Animation-only bridge. It never invokes a callback, spends chakra, or changes a jutsu's execution time. */
public final class SusanooCastController {
    private SusanooCastController() { }

    private static EntitySusanooMadara mounted(EntityLivingBase caster) {
        if (caster == null || caster.world.isRemote || !(caster.getRidingEntity() instanceof EntitySusanooMadara)) return null;
        EntitySusanooMadara susanoo = (EntitySusanooMadara)caster.getRidingEntity();
        return caster.equals(susanoo.getOwnerPlayer()) && susanoo.isEntityAlive() ? susanoo : null;
    }

    private static int profile(ItemJutsu.JutsuEnum jutsu) {
        if (jutsu == null || jutsu.getType() == null) return SusanooCastProfile.GENERIC;
        switch (jutsu.getType()) {
            case KATON: case SHAKUTON: return SusanooCastProfile.FIRE;
            case SUITON: case HYOTON: return SusanooCastProfile.WATER;
            case RAITON: case RANTON: return SusanooCastProfile.LIGHTNING;
            case DOTON: case MOKUTON: return SusanooCastProfile.EARTH;
            case FUTON: return SusanooCastProfile.WIND;
            case INTON: case SHARINGAN: return SusanooCastProfile.ILLUSION;
            default: return SusanooCastProfile.GENERIC;
        }
    }

    public static String key(ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
        return stack.getItem().getRegistryName() + "/" + (jutsu == null ? "unknown" : jutsu.unlocalizedName);
    }

    private static boolean hasSeals(ItemJutsu.JutsuEnum jutsu) {
        return jutsu != null && jutsu.getType() != ItemJutsu.JutsuEnum.Type.TAIJUTSU;
    }

    public static void prepare(EntityLivingBase caster, ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
        EntitySusanooMadara susanoo = mounted(caster);
        if (susanoo != null && hasSeals(jutsu)) susanoo.beginCast(profile(jutsu), key(stack, jutsu));
    }

    public static void charge(EntityLivingBase caster, ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
        EntitySusanooMadara susanoo = mounted(caster);
        if (susanoo != null && hasSeals(jutsu)) susanoo.holdCast(profile(jutsu), key(stack, jutsu));
    }

    public static void completed(EntityLivingBase caster, ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
        EntitySusanooMadara susanoo = mounted(caster);
        if (susanoo != null && hasSeals(jutsu)) {
            susanoo.ensureCastProfile(profile(jutsu), key(stack, jutsu));
            susanoo.finishCast(true);
        }
    }

    /** A channeled callback may succeed every tick; keep its seal held until the real release. */
    public static void executed(EntityLivingBase caster, ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
        if (caster != null && caster.isHandActive()) {
            // Copied healing runs the original callback with a temporary stack, but the
            // active item remains the copied-jutsu scroll throughout the channel.
            ItemStack active = caster.getActiveItemStack();
            if (active == stack || active.getItem() instanceof net.narutomod.item.ItemSharinganCopy.CopiedJutsuItem) {
                charge(caster, active, jutsu);
                return;
            }
        }
        completed(caster, stack, jutsu);
    }

    public static void cancel(EntityLivingBase caster) {
        EntitySusanooMadara susanoo = mounted(caster);
        if (susanoo != null) susanoo.finishCast(false);
    }

    /** Eye-key abilities bypass ItemJutsu; call once after a successful ability, never to execute the ability. */
    public static void eyeRelease(EntityLivingBase caster, int animationProfile) {
        EntitySusanooMadara susanoo = mounted(caster);
        if (susanoo != null) {
            susanoo.ensureCastProfile(animationProfile, "eye");
            susanoo.finishCast(true);
        }
    }
}
