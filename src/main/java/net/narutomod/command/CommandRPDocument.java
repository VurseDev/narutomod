package net.narutomod.command;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.PlayerStats;
import net.narutomod.item.ItemRPDocuments;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class CommandRPDocument extends ElementsNarutomodMod.ModElement {
	public CommandRPDocument(ElementsNarutomodMod instance) {
		super(instance, 1023);
	}

	@Override
	public void serverLoad(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandHandler());
	}

	public static class CommandHandler extends CommandBase {
		@Override
		public String getName() {
			return "rpdocument";
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return "/rpdocument <id|passport> <player> <village> [expiresDays]";
		}

		@Override
		public int getRequiredPermissionLevel() {
			return 0;
		}

		@Override
		public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
			if (sender.canUseCommand(4, this.getName())) {
				return true;
			}
			return sender.getCommandSenderEntity() instanceof EntityPlayerMP
				&& "Hokage".equals(PlayerStats.getRank((EntityPlayerMP)sender.getCommandSenderEntity()));
		}

		@Override
		public boolean isUsernameIndex(String[] args, int index) {
			return index == 1;
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length < 3) {
				throw new CommandException(getUsage(sender));
			}
			String kind = args[0].toLowerCase();
			if (!"id".equals(kind) && !"passport".equals(kind)) {
				throw new CommandException("Document type must be id or passport.");
			}
			EntityPlayerMP target = getPlayer(server, sender, args[1]);
			String village = args[2];
			int expiresDays = args.length >= 4 ? parseInt(args[3], 0) : 0;
			String issuer = sender.getName();
			ItemStack document = ItemRPDocuments.createDocument(kind, target, village, issuer, expiresDays);
			ItemHandlerHelper.giveItemToPlayer(target, document);
			sender.sendMessage(new TextComponentString("Issued " + kind + " document to " + target.getName() + " for " + village + "."));
			target.sendMessage(new TextComponentString("Your village document has been issued by " + issuer + "."));
		}

		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 1) {
				return getListOfStringsMatchingLastWord(args, Arrays.asList("id", "passport"));
			}
			if (args.length == 2) {
				return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
			}
			if (args.length == 3) {
				return getListOfStringsMatchingLastWord(args, Arrays.asList("Konoha", "Suna", "Kiri", "Kumo", "Iwa", "Ame", "Oto"));
			}
			return new ArrayList<>();
		}
	}
}
