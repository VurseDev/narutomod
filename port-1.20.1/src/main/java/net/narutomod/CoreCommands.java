package net.narutomod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

/** Staff-facing commands for the first modern stats/chakra milestone. */
public final class CoreCommands {
    @SubscribeEvent
    public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("naruto")
                .then(Commands.literal("stats")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> show(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                        .executes(context -> showSelf(context.getSource())))
                .then(Commands.literal("setstat")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0, CoreData.MAX_STAT))
                                                .executes(context -> setStat(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "stat"),
                                                        LongArgumentType.getLong(context, "amount")))))))
                .then(Commands.literal("setpoints")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, CoreData.MAX_POINTS))
                                        .executes(context -> setPoints(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                LongArgumentType.getLong(context, "amount"))))))
                .then(Commands.literal("givepoints")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(-CoreData.MAX_POINTS, CoreData.MAX_POINTS))
                                        .executes(context -> givePoints(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                LongArgumentType.getLong(context, "amount"))))))
                .then(Commands.literal("setlimit")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("limit", IntegerArgumentType.integer(0, CoreData.MAX_STAT))
                                        .executes(context -> setLimit(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "limit"))))))
                .then(Commands.literal("setrank")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .executes(context -> setRank(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "rank"))))))
                .then(Commands.literal("setchakra")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, 9_000_000_000L))
                                        .executes(context -> setChakra(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                LongArgumentType.getLong(context, "amount")))))));
    }

    private static int showSelf(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        return show(source, player);
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " — rank " + CoreData.getRank(player)
                + ", points " + CoreData.getPoints(player) + " (available " + CoreData.getAvailable(player) + ")"), false);
        for (int i = 0; i < CoreData.STAT_KEYS.length; i++) {
            String key = CoreData.STAT_KEYS[i];
            String label = CoreData.STAT_LABELS[i];
            source.sendSuccess(() -> Component.literal("  " + label + ": " + CoreData.getStat(player, key)), false);
        }
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "  Chakra: %.0f / %.0f | SPI regen %.2f/s",
                CoreData.chakra(player), CoreData.maxChakra(player), CoreData.spiRegenPerSecond(player))), false);
        return 1;
    }

    private static int setStat(CommandSourceStack source, ServerPlayer player, String stat, long amount) {
        if (CoreData.statIndex(stat) < 0) {
            source.sendFailure(Component.literal("Unknown stat. Use speed, strength, resistance, health, chakra, or spi."));
            return 0;
        }
        CoreData.setStat(player, stat, amount);
        CoreData.sync(player);
        source.sendSuccess(() -> Component.literal("Set " + stat + " for " + player.getGameProfile().getName() + " to " + CoreData.getStat(player, stat)), true);
        return 1;
    }

    private static int setPoints(CommandSourceStack source, ServerPlayer player, long amount) {
        CoreData.setPoints(player, amount);
        CoreData.sync(player);
        source.sendSuccess(() -> Component.literal("Set stat points for " + player.getGameProfile().getName() + " to " + CoreData.getPoints(player)), true);
        return 1;
    }

    private static int givePoints(CommandSourceStack source, ServerPlayer player, long amount) {
        CoreData.addPoints(player, amount);
        CoreData.sync(player);
        source.sendSuccess(() -> Component.literal("Adjusted stat points for " + player.getGameProfile().getName() + " to " + CoreData.getPoints(player)), true);
        return 1;
    }

    private static int setLimit(CommandSourceStack source, ServerPlayer player, int limit) {
        CoreData.setPersonalLimit(player, limit);
        CoreData.sync(player);
        source.sendSuccess(() -> Component.literal("Set personal stat limit for " + player.getGameProfile().getName() + " to " + CoreData.getLimit(player)), true);
        return 1;
    }

    private static int setRank(CommandSourceStack source, ServerPlayer player, String rank) {
        CoreData.setRank(player, rank);
        CoreData.sync(player);
        source.sendSuccess(() -> Component.literal("Set rank for " + player.getGameProfile().getName() + " to " + CoreData.getRank(player)), true);
        return 1;
    }

    private static int setChakra(CommandSourceStack source, ServerPlayer player, long amount) {
        CoreData.setChakra(player, amount);
        CoreData.sync(player);
        source.sendSuccess(() -> Component.literal("Set chakra for " + player.getGameProfile().getName() + " to " + Math.round(CoreData.chakra(player))), true);
        return 1;
    }
}
