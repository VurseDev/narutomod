package net.narutomod;

import net.minecraftforge.common.config.Config;

/** Separate config avoids altering unrelated mod settings. Restart after editing. */
@Config(modid=NarutomodMod.MODID, name="narutomod-madara-time")
@Config.RequiresMcRestart
public final class MadaraTemporalSettings {
    @Config.RangeInt(min=1,max=5) public static int MS_HISTORY_SECONDS=2;
    @Config.RangeInt(min=1,max=5) public static int EMS_HISTORY_SECONDS=4;
    @Config.RangeInt(min=2,max=10) public static int MS_ANCHOR_SECONDS=4;
    @Config.RangeInt(min=2,max=10) public static int EMS_ANCHOR_SECONDS=6;
    @Config.RangeInt(min=10,max=300) public static int MS_COOLDOWN_SECONDS=45;
    @Config.RangeInt(min=10,max=300) public static int EMS_COOLDOWN_SECONDS=35;
    @Config.RangeInt(min=1,max=24) public static int MS_RETURN_DISTANCE=8;
    @Config.RangeInt(min=1,max=24) public static int EMS_RETURN_DISTANCE=12;
    @Config.RangeDouble(min=1,max=5000) public static double MS_ANCHOR_CHAKRA=80;
    @Config.RangeDouble(min=1,max=5000) public static double MS_REVERSAL_CHAKRA=160;
    @Config.RangeDouble(min=1,max=5000) public static double EMS_ANCHOR_CHAKRA=60;
    @Config.RangeDouble(min=1,max=5000) public static double EMS_REVERSAL_CHAKRA=120;
    private MadaraTemporalSettings() { }
}
