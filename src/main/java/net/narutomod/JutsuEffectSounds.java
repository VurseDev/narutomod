package net.narutomod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Short original electric cues; server playback keeps observers and caster in sync. */
@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class JutsuEffectSounds {
    private enum Cue {
        CHARGE("charge", 0.58f, 6),
        DISCHARGE("discharge", 0.84f, 3),
        PILLAR_RISE("pillar_rise", 0.82f, 6),
        PILLAR_IMPACT("pillar_impact", 0.95f, 6),
        CHAIN_SNAP("chain_snap", 0.49f, 3),
        SUSTAIN("sustain", 0.29f, 12);

        final SoundEvent sound;
        final float volume;
        final int spacing;

        Cue(String suffix, float volume, int spacing) {
            ResourceLocation id = new ResourceLocation(NarutomodMod.MODID, "jutsu_lightning_" + suffix);
            this.sound = new SoundEvent(id).setRegistryName(id);
            this.volume = volume;
            this.spacing = spacing;
        }
    }

    // World keys are weak: disconnecting never leaves a dimension retained by audio state.
    private static final Map<World, Map<Long, Long>> RECENT = new WeakHashMap<>();
    private static final Map<World, List<Scheduled>> PENDING = new WeakHashMap<>();

    private static final class Scheduled {
        final Vec3d position;
        final Cue cue;
        final long due;
        Scheduled(Vec3d position, Cue cue, long due) { this.position=position;this.cue=cue;this.due=due; }
    }

    private JutsuEffectSounds() {}

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        for (Cue cue : Cue.values()) {
            event.getRegistry().register(cue.sound);
        }
    }

    public static void charge(World world, Vec3d position) { play(world, position, Cue.CHARGE); }
    public static void discharge(World world, Vec3d position) { play(world, position, Cue.DISCHARGE); }
    public static void pillarRise(World world, Vec3d position) { play(world, position, Cue.PILLAR_RISE); }
    public static void pillarImpact(World world, Vec3d position) { play(world, position, Cue.PILLAR_IMPACT); }
    public static void chainSnap(World world, Vec3d position) { play(world, position, Cue.CHAIN_SNAP); }
    public static void sustain(World world, Vec3d position) { play(world, position, Cue.SUSTAIN); }

    /** Rise first; the snap lands when the eight-tick pillar animation locks into place. */
    public static void pillarSequence(World world, Vec3d position, int life) {
        if (world == null || world.isRemote) return;
        pillarRise(world, position);
        List<Scheduled> queue = PENDING.computeIfAbsent(world, ignored -> new ArrayList<>());
        long now = world.getTotalWorldTime();
        if (queue.size() < 128) queue.add(new Scheduled(position, Cue.PILLAR_IMPACT, now + 8));
        for (int tick=32;tick<Math.min(life,160)-12 && queue.size()<128;tick+=30)
            queue.add(new Scheduled(position, Cue.SUSTAIN, now + tick));
    }

    @SubscribeEvent
    public static void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        List<Scheduled> queue = PENDING.get(event.world);
        if (queue == null) return;
        long now = event.world.getTotalWorldTime();
        for (Iterator<Scheduled> it=queue.iterator();it.hasNext();) {
            Scheduled sound=it.next();
            if (now>=sound.due) { it.remove(); if(now-sound.due<10)play(event.world,sound.position,sound.cue); }
        }
        if (queue.isEmpty()) PENDING.remove(event.world);
    }

    private static void play(World world, Vec3d position, Cue cue) {
        if (world == null || world.isRemote || position == null
                || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            return;
        }
        long now = world.getTotalWorldTime();
        Map<Long, Long> recent = RECENT.computeIfAbsent(world, ignored -> new HashMap<>());
        // Merge simultaneous nearby hits so a multi-target strike does not multiply its volume.
        long cell = new BlockPos(Math.floor(position.x / 8.0), Math.floor(position.y / 8.0),
                Math.floor(position.z / 8.0)).toLong();
        long key = cell ^ (0x9e3779b97f4a7c15L * (cue.ordinal() + 1L));
        Long last = recent.get(key);
        if (last != null && now >= last && now - last < cue.spacing) {
            return;
        }
        if (recent.size() > 256) {
            Iterator<Long> times = recent.values().iterator();
            while (times.hasNext()) {
                long time = times.next();
                if (now < time || now - time > 40) {
                    times.remove();
                }
            }
            if (recent.size() > 1024) {
                recent.clear();
            }
        }
        recent.put(key, now);
        float pitch = 0.96f + world.rand.nextFloat() * 0.08f;
        world.playSound(null, position.x, position.y, position.z, cue.sound,
                SoundCategory.PLAYERS, cue.volume, pitch);
    }
}
