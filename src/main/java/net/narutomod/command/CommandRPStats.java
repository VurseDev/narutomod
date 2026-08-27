package net.narutomod.command;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.PlayerStats;

import java.util.ArrayList;
import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class CommandRPStats extends ElementsNarutomodMod.ModElement {
	public CommandRPStats(ElementsNarutomodMod instance) {
		super(instance, 1007);
	}

	@Override
	public void serverLoad(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandHandler());
		event.registerServerCommand(new StatLimitCommand());
		event.registerServerCommand(new StatPointsCommand());
		event.registerServerCommand(new SetStatCommand());
		event.registerServerCommand(new ChecksheetCommand());
		event.registerServerCommand(new SetStatPointsCommand());
		event.registerServerCommand(new SetStatCapCommand());
		event.registerServerCommand(new SetClanCommand());
		event.registerServerCommand(new SetRankCommand());
		event.registerServerCommand(new SetRankLimitCommand());
		event.registerServerCommand(new SetAffinityCommand());
		event.registerServerCommand(new AddAffinityCommand());
		event.registerServerCommand(new RemoveAffinityCommand());
		event.registerServerCommand(new SetRPStatCommand());
		event.registerServerCommand(new SetSharinganCommand());
		event.registerServerCommand(new ListClansCommand());
		event.registerServerCommand(new ListAffinitiesCommand());
		event.registerServerCommand(new ListRanksCommand());
	}

	private abstract static class AdminCommand extends CommandBase {
		@Override public int getRequiredPermissionLevel() { return 4; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return sender.canUseCommand(4, this.getName()); }
		@Override public boolean isUsernameIndex(String[] args, int index) { return index == 0; }
		protected EntityPlayerMP target(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 1) throw new CommandException(getUsage(sender));
			return getPlayer(server, sender, args[0]);
		}
		protected void send(ICommandSender sender, String text) {
			sender.sendMessage(new TextComponentString(text));
		}
		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 1) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
			return new ArrayList<>();
		}
	}

	/**
	 * Simple cap command:
	 * /statlimit rank Hokage 100000
	 * /statlimit player SomePlayer 120000
	 * /statlimit player SomePlayer reset
	 */
	public static class StatLimitCommand extends CommandBase {
		@Override public String getName() { return "statlimit"; }
		@Override public String getUsage(ICommandSender sender) {
			return "/statlimit <rank <rank> <amount> | player <player> <amount|reset>>";
		}
		@Override public int getRequiredPermissionLevel() { return 4; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
			return sender.canUseCommand(4, this.getName());
		}
		@Override public boolean isUsernameIndex(String[] args, int index) {
			return args.length > 0 && "player".equalsIgnoreCase(args[0]) && index == 1;
		}
		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 3) throw new CommandException(getUsage(sender));
			if ("rank".equalsIgnoreCase(args[0])) {
				if (!PlayerStats.isValidRank(args[1])) throw new CommandException("Unknown rank: " + args[1]);
				String rank = PlayerStats.normalizeRank(args[1]);
				PlayerStats.setRankLimit(server.getEntityWorld(), rank, parseInt(args[2]));
				for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
					PlayerStats.refresh(player);
				}
				sender.sendMessage(new TextComponentString(rank + " per-stat limit is now "
				 + PlayerStats.getRankLimit(server.getEntityWorld(), rank) + "."));
				return;
			}
			if ("player".equalsIgnoreCase(args[0])) {
				EntityPlayerMP player = getPlayer(server, sender, args[1]);
				boolean reset = "reset".equalsIgnoreCase(args[2]) || "rank".equalsIgnoreCase(args[2])
				 || "default".equalsIgnoreCase(args[2]);
				PlayerStats.setPersonalStatLimit(player, reset ? 0 : parseInt(args[2]));
				sender.sendMessage(new TextComponentString(player.getName() + " per-stat limit is now "
				 + PlayerStats.getStatLimit(player)
				 + (PlayerStats.getPersonalStatLimit(player) > 0 ? " (personal)." : " (from rank).")));
				return;
			}
			throw new CommandException(getUsage(sender));
		}
		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 1) return getListOfStringsMatchingLastWord(args, "rank", "player");
			if (args.length == 2 && "rank".equalsIgnoreCase(args[0])) {
				return getListOfStringsMatchingLastWord(args, PlayerStats.RANKS);
			}
			if (args.length == 2 && "player".equalsIgnoreCase(args[0])) {
				return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
			}
			if (args.length == 3 && "player".equalsIgnoreCase(args[0])) {
				return getListOfStringsMatchingLastWord(args, "reset");
			}
			return new ArrayList<>();
		}
	}

	/** Adds points to the player's single allocation pool. */
	public static class StatPointsCommand extends AdminCommand {
		@Override public String getName() { return "statpoints"; }
		@Override public String getUsage(ICommandSender sender) { return "/statpoints <player> <amount>"; }
		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			long before = PlayerStats.getPoints(player);
			PlayerStats.addPoints(player, parseInt(args[1]));
			long granted = PlayerStats.getPoints(player) - before;
			send(sender, (granted >= 0L ? "Granted " : "Removed ") + Math.abs(granted) + " stat points "
			 + (granted >= 0L ? "to " : "from ") + player.getName() + ". Available: "
			 + PlayerStats.getAvailablePoints(player) + " / " + PlayerStats.getPoints(player) + " total.");
		}
	}

	/** Directly assigns one custom stat without touching Ninja XP/Battle XP. */
	public static class SetStatCommand extends AdminCommand {
		@Override public String getName() { return "setstat"; }
		@Override public String getUsage(ICommandSender sender) {
			return "/setstat <player> <speed|strength|resistance|health|chakra|spi> <amount>";
		}
		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 3) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			int stat = findStat(args[1]);
			if (stat < 0) throw new CommandException("Unknown stat. Use: " + String.join(", ", PlayerStats.getStatKeys()));
			PlayerStats.setStat(player, stat, parseInt(args[2]));
			send(sender, "Set " + player.getName() + " " + PlayerStats.getStatKeys()[stat] + " to "
			 + PlayerStats.getStat(player, stat) + ". Available points: " + PlayerStats.getAvailablePoints(player)
			 + " / " + PlayerStats.getPoints(player) + " total.");
		}
		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 2) return getListOfStringsMatchingLastWord(args, PlayerStats.getStatKeys());
			return super.getTabCompletions(server, sender, args, pos);
		}
	}

	private static int findStat(String value) {
		if ("str".equalsIgnoreCase(value) || "strenght".equalsIgnoreCase(value)) value = "strength";
		else if ("res".equalsIgnoreCase(value)) value = "resistance";
		else if ("hp".equalsIgnoreCase(value)) value = "health";
		else if ("chakramax".equalsIgnoreCase(value) || "chakra_max".equalsIgnoreCase(value)) value = "chakra";
		String[] keys = PlayerStats.getStatKeys();
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].equalsIgnoreCase(value)) return i;
		}
		return -1;
	}

	public static class ChecksheetCommand extends AdminCommand {
		@Override public String getName() { return "checksheet"; }
		@Override public String getUsage(ICommandSender sender) { return "/checksheet <player>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			send(sender, TextFormatting.YELLOW + PlayerStats.getStatus(target(server, sender, args)));
		}
	}

	public static class SetStatPointsCommand extends AdminCommand {
		@Override public String getName() { return "setstatpoints"; }
		@Override public String getUsage(ICommandSender sender) { return "/setstatpoints <player> <amount>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.setPoints(player, parseInt(args[1]));
			send(sender, "Set " + player.getName() + " total stat points to " + PlayerStats.getPoints(player)
			 + " (available " + PlayerStats.getAvailablePoints(player) + ").");
		}
	}

	public static class SetStatCapCommand extends AdminCommand {
		@Override public String getName() { return "setstatcap"; }
		@Override public String getUsage(ICommandSender sender) { return "/setstatcap <player> <amount|0 to reset>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.setPersonalStatLimit(player, parseInt(args[1]));
			send(sender, "Set " + player.getName() + " personal stat cap to " + PlayerStats.getStatLimit(player)
			 + (PlayerStats.getPersonalStatLimit(player) > 0 ? " (override)" : " (rank default)"));
		}
	}

	public static class SetStatPointCapCommand extends AdminCommand {
		@Override public String getName() { return "setstatpointcap"; }
		@Override public String getUsage(ICommandSender sender) { return "/setstatpointcap <player> <amount>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.setPointLimit(player, parseInt(args[1]));
			send(sender, "Set " + player.getName() + " point capacity to " + PlayerStats.getPointLimit(player));
		}
	}

	public static class SetClanCommand extends AdminCommand {
		@Override public String getName() { return "setclan"; }
		@Override public String getUsage(ICommandSender sender) { return "/setclan <player> <clan>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.setClan(player, args[1]);
			send(sender, "Set " + player.getName() + " clan to " + PlayerStats.getClan(player));
		}
		@Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 2) return getListOfStringsMatchingLastWord(args, PlayerStats.CLANS);
			return super.getTabCompletions(server, sender, args, pos);
		}
	}

	public static class SetRankCommand extends AdminCommand {
		@Override public String getName() { return "setrank"; }
		@Override public String getUsage(ICommandSender sender) { return "/setrank <player> <None|Genin|Chunin|Jonin|Hokage>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			if (!PlayerStats.isValidRank(args[1])) throw new CommandException("Unknown rank: " + args[1]);
			PlayerStats.setRank(player, args[1]);
			send(sender, "Set " + player.getName() + " rank to " + PlayerStats.getRank(player)
			 + " (per-stat limit " + PlayerStats.getStatLimit(player) + ").");
		}
		@Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 2) return getListOfStringsMatchingLastWord(args, PlayerStats.RANKS);
			return super.getTabCompletions(server, sender, args, pos);
		}
	}

	public static class SetRankLimitCommand extends AdminCommand {
		@Override public String getName() { return "setranklimit"; }
		@Override public String getUsage(ICommandSender sender) { return "/setranklimit <rank> <statLimit>"; }
		@Override public boolean isUsernameIndex(String[] args, int index) { return false; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			if (!PlayerStats.isValidRank(args[0])) throw new CommandException("Unknown rank: " + args[0]);
			String rank = PlayerStats.normalizeRank(args[0]);
			PlayerStats.setRankLimit(server.getEntityWorld(), rank, parseInt(args[1]));
			for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
				PlayerStats.refresh(player);
			}
			send(sender, "Set " + rank + " stat limit to " + PlayerStats.getRankLimit(server.getEntityWorld(), rank));
		}
		@Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 1) return getListOfStringsMatchingLastWord(args, PlayerStats.RANKS);
			return new ArrayList<>();
		}
	}

	public static class SetAffinityCommand extends AdminCommand {
		@Override public String getName() { return "setaffinity"; }
		@Override public String getUsage(ICommandSender sender) { return "/setaffinity <player> <affinity>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.setAffinity(player, args[1]);
			send(sender, "Set " + player.getName() + " affinity to " + PlayerStats.getAffinity(player));
		}
		@Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 2) return getListOfStringsMatchingLastWord(args, PlayerStats.AFFINITIES);
			return super.getTabCompletions(server, sender, args, pos);
		}
	}

	public static class AddAffinityCommand extends SetAffinityCommand {
		@Override public String getName() { return "addaffinity"; }
		@Override public String getUsage(ICommandSender sender) { return "/addaffinity <player> <affinity>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.addAffinity(player, args[1]);
			send(sender, "Affinities for " + player.getName() + ": " + PlayerStats.getAffinity(player));
		}
	}

	public static class RemoveAffinityCommand extends SetAffinityCommand {
		@Override public String getName() { return "removeaffinity"; }
		@Override public String getUsage(ICommandSender sender) { return "/removeaffinity <player> <affinity>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.removeAffinity(player, args[1]);
			send(sender, "Affinities for " + player.getName() + ": " + PlayerStats.getAffinity(player));
		}
	}

	public static class SetRPStatCommand extends AdminCommand {
		@Override public String getName() { return "setrpstat"; }
		@Override public String getUsage(ICommandSender sender) { return "/setrpstat <player> <speed|strength|resistance|health|chakra|spi> <amount>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 3) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			String[] keys = PlayerStats.getStatKeys();
			for (int i = 0; i < keys.length; i++) {
				if (keys[i].equalsIgnoreCase(args[1])) {
					PlayerStats.setStat(player, i, parseInt(args[2]));
					send(sender, "Set " + player.getName() + " " + keys[i] + " to " + PlayerStats.getStat(player, i)
					 + " (effective cap " + PlayerStats.getStatLimit(player) + ")");
					return;
				}
			}
			throw new CommandException("Unknown stat.");
		}
		@Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 2) return getListOfStringsMatchingLastWord(args, PlayerStats.getStatKeys());
			return super.getTabCompletions(server, sender, args, pos);
		}
	}

	public static class SetSharinganCommand extends AdminCommand {
		@Override public String getName() { return "setsharingan"; }
		@Override public String getUsage(ICommandSender sender) { return "/setsharingan <player> <0|1|2|3>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.setSharinganStage(player, parseInt(args[1]));
			send(sender, "Set " + player.getName() + " Sharingan to " + PlayerStats.getSharinganStage(player) + " tomoe");
		}
	}

	public static class ListClansCommand extends CommandBase {
		@Override public String getName() { return "listclans"; }
		@Override public String getUsage(ICommandSender sender) { return "/listclans"; }
		@Override public int getRequiredPermissionLevel() { return 4; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return sender.canUseCommand(4, this.getName()); }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) { sender.sendMessage(new TextComponentString("Valid clans: " + String.join(", ", PlayerStats.CLANS))); }
	}

	public static class ListAffinitiesCommand extends CommandBase {
		@Override public String getName() { return "listaffinities"; }
		@Override public String getUsage(ICommandSender sender) { return "/listaffinities"; }
		@Override public int getRequiredPermissionLevel() { return 4; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return sender.canUseCommand(4, this.getName()); }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) { sender.sendMessage(new TextComponentString("Valid affinities: " + String.join(", ", PlayerStats.AFFINITIES))); }
	}

	public static class ListRanksCommand extends CommandBase {
		@Override public String getName() { return "listranks"; }
		@Override public String getUsage(ICommandSender sender) { return "/listranks"; }
		@Override public int getRequiredPermissionLevel() { return 4; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return sender.canUseCommand(4, this.getName()); }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
			String text = "";
			for (String rank : PlayerStats.RANKS) {
				text += (text.isEmpty() ? "" : ", ") + rank + "=" + PlayerStats.getRankLimit(server.getEntityWorld(), rank);
			}
			sender.sendMessage(new TextComponentString("Valid ranks/stat limits: " + text));
		}
	}

	public static class CommandHandler extends CommandBase {
		@Override
		public String getName() {
			return "rpstats";
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return "Simple stats: /statlimit, /statpoints, /setstat. Other RP data: /rpstats <checksheet|clan|rank|affinity|sharingan|clans|ranks|affinities>";
		}

		@Override
		public int getRequiredPermissionLevel() {
			return 4;
		}

		@Override
		public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
			return sender.canUseCommand(4, this.getName());
		}

		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 1) {
				return getListOfStringsMatchingLastWord(args, "checksheet", "clan", "rank", "affinity", "sharingan", "clans", "ranks", "affinities");
			}
			if (args.length == 2 && !"clans".equals(args[0]) && !"ranks".equals(args[0]) && !"affinities".equals(args[0]) && !"ranklimit".equals(args[0])) {
				return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
			}
			if (args.length == 2 && "ranklimit".equals(args[0])) {
				return getListOfStringsMatchingLastWord(args, PlayerStats.RANKS);
			}
			if (args.length == 3 && "clan".equals(args[0])) {
				return getListOfStringsMatchingLastWord(args, PlayerStats.CLANS);
			}
			if (args.length == 3 && "affinity".equals(args[0])) {
				return getListOfStringsMatchingLastWord(args, PlayerStats.AFFINITIES);
			}
			if (args.length == 3 && "rank".equals(args[0])) {
				return getListOfStringsMatchingLastWord(args, PlayerStats.RANKS);
			}
			if (args.length == 3 && "stat".equals(args[0])) {
				return getListOfStringsMatchingLastWord(args, PlayerStats.getStatKeys());
			}
			return new ArrayList<>();
		}

		@Override
		public boolean isUsernameIndex(String[] args, int index) {
			return index == 1;
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length == 0) {
				send(sender, getUsage(sender));
				return;
			}
			if ("clans".equals(args[0])) {
				send(sender, "Valid clans: " + String.join(", ", PlayerStats.CLANS));
				return;
			}
			if ("affinities".equals(args[0])) {
				send(sender, "Valid affinities: " + String.join(", ", PlayerStats.AFFINITIES));
				return;
			}
			if ("ranks".equals(args[0])) {
				String text = "";
				for (String rank : PlayerStats.RANKS) {
					text += (text.isEmpty() ? "" : ", ") + rank + "=" + PlayerStats.getRankLimit(server.getEntityWorld(), rank);
				}
				send(sender, "Valid ranks/stat limits: " + text);
				return;
			}
			if ("ranklimit".equals(args[0]) && args.length >= 3) {
				if (!PlayerStats.isValidRank(args[1])) throw new CommandException("Unknown rank: " + args[1]);
				String rank = PlayerStats.normalizeRank(args[1]);
				PlayerStats.setRankLimit(server.getEntityWorld(), rank, parseInt(args[2]));
				for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
					PlayerStats.refresh(player);
				}
				send(sender, "Set " + rank + " stat limit to " + PlayerStats.getRankLimit(server.getEntityWorld(), rank));
				return;
			}
			if (args.length < 2) {
				send(sender, getUsage(sender));
				return;
			}
			EntityPlayerMP target = getPlayer(server, sender, args[1]);
			if ("checksheet".equals(args[0])) {
				send(sender, PlayerStats.getStatus(target));
			} else if ("points".equals(args[0]) && args.length >= 3) {
				PlayerStats.setPoints(target, parseInt(args[2]));
				send(sender, "Set " + target.getName() + " total stat points to " + PlayerStats.getPoints(target)
				 + " (available " + PlayerStats.getAvailablePoints(target) + ").");
			} else if ("maxpoints".equals(args[0]) && args.length >= 3) {
				PlayerStats.setPointLimit(target, parseInt(args[2]));
				send(sender, "Set " + target.getName() + " stat point capacity to " + PlayerStats.getPointLimit(target));
			} else if ("statcap".equals(args[0]) && args.length >= 3) {
				PlayerStats.setPersonalStatLimit(target, parseInt(args[2]));
				send(sender, "Set " + target.getName() + " personal stat cap to " + PlayerStats.getStatLimit(target)
				 + (PlayerStats.getPersonalStatLimit(target) > 0 ? " (override)" : " (rank default)"));
			} else if ("clan".equals(args[0]) && args.length >= 3) {
				PlayerStats.setClan(target, args[2]);
				send(sender, "Set " + target.getName() + " clan to " + PlayerStats.getClan(target));
			} else if ("rank".equals(args[0]) && args.length >= 3) {
				if (!PlayerStats.isValidRank(args[2])) throw new CommandException("Unknown rank: " + args[2]);
				PlayerStats.setRank(target, args[2]);
				send(sender, "Set " + target.getName() + " rank to " + PlayerStats.getRank(target)
				 + " (per-stat limit " + PlayerStats.getStatLimit(target) + ").");
			} else if ("affinity".equals(args[0]) && args.length >= 3) {
				PlayerStats.setAffinity(target, args[2]);
				send(sender, "Set " + target.getName() + " affinity to " + PlayerStats.getAffinity(target));
			} else if ("stat".equals(args[0]) && args.length >= 4) {
				String[] keys = PlayerStats.getStatKeys();
				for (int i = 0; i < keys.length; i++) {
					if (keys[i].equalsIgnoreCase(args[2])) {
						PlayerStats.setStat(target, i, parseInt(args[3]));
						send(sender, "Set " + target.getName() + " " + keys[i] + " to " + PlayerStats.getStat(target, i)
						 + " (effective cap " + PlayerStats.getStatLimit(target) + ")");
						return;
					}
				}
				send(sender, "Unknown stat. Use: " + String.join(", ", keys));
			} else if ("sharingan".equals(args[0]) && args.length >= 3) {
				PlayerStats.setSharinganStage(target, parseInt(args[2]));
				send(sender, "Set " + target.getName() + " Sharingan to " + PlayerStats.getSharinganStage(target) + " tomoe");
			} else {
				send(sender, getUsage(sender));
			}
		}

		private static void send(ICommandSender sender, String text) {
			sender.sendMessage(new TextComponentString(text));
		}
	}
}
