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
			send(sender, "Set " + player.getName() + " points to " + args[1]);
		}
	}

	public static class SetStatCapCommand extends AdminCommand {
		@Override public String getName() { return "setstatcap"; }
		@Override public String getUsage(ICommandSender sender) { return "/setstatcap <player> <amount>"; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 2) throw new CommandException(getUsage(sender));
			EntityPlayerMP player = target(server, sender, args);
			PlayerStats.setPointLimit(player, parseInt(args[1]));
			send(sender, "Set " + player.getName() + " point cap to " + args[1]);
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
			PlayerStats.setRank(player, args[1]);
			send(sender, "Set " + player.getName() + " rank to " + PlayerStats.getRank(player));
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
					send(sender, "Set " + player.getName() + " " + keys[i] + " to " + args[2]);
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
			return "/rpstats <checksheet|points|maxpoints|clan|rank|ranklimit|affinity|stat|sharingan|clans|ranks|affinities> ...";
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
				return getListOfStringsMatchingLastWord(args, "checksheet", "points", "maxpoints", "clan", "rank", "ranklimit", "affinity", "stat", "sharingan", "clans", "ranks", "affinities");
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
				send(sender, "Set " + target.getName() + " stat points to " + args[2]);
			} else if ("maxpoints".equals(args[0]) && args.length >= 3) {
				PlayerStats.setPointLimit(target, parseInt(args[2]));
				send(sender, "Set " + target.getName() + " stat point cap to " + args[2]);
			} else if ("clan".equals(args[0]) && args.length >= 3) {
				PlayerStats.setClan(target, args[2]);
				send(sender, "Set " + target.getName() + " clan to " + PlayerStats.getClan(target));
			} else if ("rank".equals(args[0]) && args.length >= 3) {
				PlayerStats.setRank(target, args[2]);
				send(sender, "Set " + target.getName() + " rank to " + PlayerStats.getRank(target));
			} else if ("affinity".equals(args[0]) && args.length >= 3) {
				PlayerStats.setAffinity(target, args[2]);
				send(sender, "Set " + target.getName() + " affinity to " + PlayerStats.getAffinity(target));
			} else if ("stat".equals(args[0]) && args.length >= 4) {
				String[] keys = PlayerStats.getStatKeys();
				for (int i = 0; i < keys.length; i++) {
					if (keys[i].equalsIgnoreCase(args[2])) {
						PlayerStats.setStat(target, i, parseInt(args[3]));
						send(sender, "Set " + target.getName() + " " + keys[i] + " to " + args[3]);
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
