package net.narutomod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Lifecycle, regeneration, and clone handling for the modern core state. */
public final class CoreEvents {
    private static final String COMBAT_UNTIL = "combat_until";

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CoreData.ensureChakra(player);
            CoreData.sync(player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CoreData.ensureChakra(player);
            CoreData.sync(player);
        }
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CoreData.sync(player);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        if (oldPlayer.getPersistentData().contains(CoreData.ROOT)) {
            newPlayer.getPersistentData().put(CoreData.ROOT,
                    oldPlayer.getPersistentData().getCompound(CoreData.ROOT).copy());
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer target) {
            markCombat(target);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            markCombat(attacker);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        CoreData.ensureChakra(player);
        if (player.tickCount % 20 == 0) {
            CoreData.clampToLimit(player);
            long now = player.level().getGameTime();
            if (now >= CoreData.tag(player).getLong(COMBAT_UNTIL) && !player.isSprinting()) {
                CoreData.setChakra(player, CoreData.chakra(player) + CoreData.spiRegenPerSecond(player));
            }
            CoreData.sync(player);
        }
    }

    private static void markCombat(ServerPlayer player) {
        CoreData.tag(player).putLong(COMBAT_UNTIL, player.level().getGameTime() + 100L);
    }
}
