package net.narutomod;

import net.narutomod.item.ItemRyo100;
import net.narutomod.item.ItemRyo1000;
import net.narutomod.item.ItemRyo10000;
import net.narutomod.item.ItemRyo1M;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ElementsNarutomodMod.ModElement.Tag
public class MissionSystem extends ElementsNarutomodMod.ModElement {
	private static final String ROOT = "NarutomodMissionIntel";
	private static final String ACTIVE = "ActiveMission";
	private static final String REPUTATION = "VillageReputation";
	private static final String BOUNTY = "BingoBounty";
	private static final String INFAMY = "BingoInfamy";
	private static final String ACTIVE_BOUNTY_UUID = "ActiveBountyUuid";
	private static final String ACTIVE_BOUNTY_NAME = "ActiveBountyName";
	private static final String TAKEN_MISSIONS = "TakenMissions";
	private static final String LAST_SEEN = "LastSeen";
	private static final String SEP = "\u001f";
	private static final int TYPE_TRAVEL = 0;
	private static final int TYPE_HOSTILE_KILL = 1;
	private static final int TYPE_HUNT = 2;
	private static final int TYPE_STAFF_RP = 3;
	private static final String[] RANKS = {"D", "C", "B", "A", "S"};
	private static final MissionDef[][] MISSIONS = new MissionDef[][] {
		new MissionDef[0], new MissionDef[0], new MissionDef[0], new MissionDef[0], new MissionDef[0]
	};

	public MissionSystem(ElementsNarutomodMod instance) {
		super(instance, 1016);
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		elements.addNetworkMessage(RequestSyncMessage.Handler.class, RequestSyncMessage.class, Side.SERVER);
		elements.addNetworkMessage(ActionMessage.Handler.class, ActionMessage.class, Side.SERVER);
		elements.addNetworkMessage(SyncMessage.Handler.class, SyncMessage.class, Side.CLIENT);
		elements.addNetworkMessage(AdminActionMessage.Handler.class, AdminActionMessage.class, Side.SERVER);
		elements.addNetworkMessage(OpenAdminMessage.Handler.class, OpenAdminMessage.class, Side.CLIENT);
		elements.addNetworkMessage(AdminSyncMessage.Handler.class, AdminSyncMessage.class, Side.CLIENT);
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new PlayerHook());
	}

	@Override
	public void serverLoad(FMLServerStartingEvent event) {
		event.registerServerCommand(new AdminMissionCommand("adminmissions"));
		event.registerServerCommand(new AdminMissionCommand("rpadmin"));
	}

	@SideOnly(Side.CLIENT)
	public static void openClient(boolean bingoFirst) {
		net.narutomod.client.MissionClient.open(bingoFirst);
	}

	public static void requestClientSync(boolean bingoFirst) {
		NarutomodMod.PACKET_HANDLER.sendToServer(new RequestSyncMessage(bingoFirst));
	}

	private static TextComponentTranslation translated(TextFormatting color, String key, Object... args) {
		TextComponentTranslation component = new TextComponentTranslation(key, args);
		component.getStyle().setColor(color);
		return component;
	}

	private static void sendTranslated(ICommandSender sender, TextFormatting color, String key, Object... args) {
		sender.sendMessage(translated(color, key, args));
	}

	public static String[] getMissionRanks() {
		return RANKS.clone();
	}

	public static String getBuiltInMissionsPayload(String rank) {
		StringBuilder builder = new StringBuilder();
		MissionDef[] missions = MISSIONS[Math.max(0, Math.min(RANKS.length - 1, rankIndex(rank) - 1))];
		for (MissionDef mission : missions) {
			if (builder.length() > 0) builder.append(";");
			builder.append(mission.toPayload(0L));
		}
		return builder.toString();
	}

	private static boolean isStaff(MinecraftServer server, ICommandSender sender) {
		if (sender.canUseCommand(4, "adminmissions")) return true;
		if (sender instanceof EntityPlayer) {
			return "Hokage".equalsIgnoreCase(PlayerStats.getRank((EntityPlayer)sender));
		}
		return false;
	}

	private static boolean isStaff(EntityPlayerMP player) {
		return isStaff(player.mcServer, player);
	}

	private static void openAdmin(EntityPlayerMP player) {
		if (!isStaff(player)) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.admin_only");
			return;
		}
		NarutomodMod.PACKET_HANDLER.sendTo(new OpenAdminMessage(), player);
		sendAdminSync(player);
	}

	private static void sendAdminSync(EntityPlayerMP player) {
		AdminData data = AdminData.get(player.world);
		NarutomodMod.PACKET_HANDLER.sendTo(new AdminSyncMessage(
			data.listSummary("missions"), data.listSummary("bingo"), data.listSummary("clues"), data.listSummary("events"),
			data.listSummary("notes"), data.listSummary("arcs")), player);
	}

	public static class AdminMissionCommand extends CommandBase {
		private final String name;

		private AdminMissionCommand(String name) {
			this.name = name;
		}

		@Override public String getName() { return this.name; }
		@Override public String getUsage(ICommandSender sender) { return "/" + this.name + " [complete <player>]"; }
		@Override public int getRequiredPermissionLevel() { return 0; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return isStaff(server, sender); }
		@Override public List<String> getAliases() { return this.name.equals("adminmissions") ? Arrays.asList("rpmissionadmin") : new ArrayList<String>(); }

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length >= 2 && ("complete".equalsIgnoreCase(args[0]) || "completar".equalsIgnoreCase(args[0]))) {
				EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(args[1]);
				if (target == null) {
					throw new CommandException("Player nao encontrado: " + args[1]);
				}
				staffCompleteMission(server, sender, target);
				return;
			}
			if (!(sender instanceof EntityPlayerMP)) {
				throw new CommandException("Use /" + this.name + " complete <player> pelo console, ou entre no jogo para abrir o painel.");
			}
			openAdmin((EntityPlayerMP)sender);
		}
	}

	private static NBTTagCompound data(EntityPlayer player) {
		NBTTagCompound root = player.getEntityData().getCompoundTag(ROOT);
		player.getEntityData().setTag(ROOT, root);
		return root;
	}

	private static NBTTagCompound active(EntityPlayer player) {
		return data(player).getCompoundTag(ACTIVE);
	}

	private static void clearActive(EntityPlayer player) {
		data(player).removeTag(ACTIVE);
	}

	private static boolean hasTakenMission(EntityPlayer player, String id) {
		if (id == null || id.isEmpty()) return false;
		String taken = data(player).getString(TAKEN_MISSIONS);
		for (String entry : taken.split(";")) {
			if (entry.equalsIgnoreCase(id)) return true;
		}
		return false;
	}

	private static void markMissionTaken(EntityPlayer player, String id) {
		if (id == null || id.isEmpty() || hasTakenMission(player, id)) return;
		String taken = data(player).getString(TAKEN_MISSIONS);
		data(player).setString(TAKEN_MISSIONS, taken.isEmpty() ? id : taken + ";" + id);
	}

	private static int rankIndex(String rank) {
		if ("S".equalsIgnoreCase(rank)) return 5;
		if ("A".equalsIgnoreCase(rank)) return 4;
		if ("B".equalsIgnoreCase(rank)) return 3;
		if ("C".equalsIgnoreCase(rank)) return 2;
		if ("D".equalsIgnoreCase(rank)) return 1;
		if ("Hokage".equalsIgnoreCase(rank)) return 5;
		if ("Jonin".equalsIgnoreCase(rank) || "Jounin".equalsIgnoreCase(rank)) return 4;
		if ("Chunin".equalsIgnoreCase(rank) || "Chunnin".equalsIgnoreCase(rank)) return 3;
		if ("Genin".equalsIgnoreCase(rank)) return 2;
		return 1;
	}

	private static boolean canAcceptRank(EntityPlayer player, String missionRank) {
		return rankIndex(PlayerStats.getRank(player)) >= rankIndex(missionRank);
	}

	private static MissionDef missionAt(String rank, int index) {
		int rankIdx = Math.max(0, Math.min(RANKS.length - 1, rankIndex(rank) - 1));
		MissionDef[] list = MISSIONS[rankIdx];
		if (list.length == 0) return null;
		return list[Math.max(0, Math.min(list.length - 1, index))];
	}

	private static void acceptCustomMission(EntityPlayerMP player, String id) {
		MissionDef mission = AdminData.get(player.world).findMission(id);
		if (mission == null) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.not_published");
			syncTo(player, false);
			return;
		}
		if (!mission.canSee(player)) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.not_assigned");
			syncTo(player, false);
			return;
		}
		issueMission(player, mission);
	}

	private static void acceptMission(EntityPlayerMP player, String rank, int index) {
		MissionDef mission = missionAt(rank, index);
		if (mission == null) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.no_public");
			syncTo(player, false);
			return;
		}
		if (!canAcceptRank(player, mission.rank)) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.rank_low");
			return;
		}
		issueMission(player, mission);
	}

	private static void issueMission(EntityPlayerMP player, MissionDef mission) {
		if (data(player).hasKey(ACTIVE)) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.already_active");
			return;
		}
		if (hasTakenMission(player, mission.id)) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.already_taken");
			syncTo(player, false);
			return;
		}
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("id", mission.id);
		tag.setString("name", mission.name);
		tag.setString("rank", mission.rank);
		tag.setString("category", mission.category);
		tag.setString("objective", mission.objective);
		tag.setInteger("required", mission.required);
		tag.setInteger("progress", 0);
		tag.setInteger("reward", mission.reward);
		tag.setInteger("reputation", mission.reputation);
		tag.setInteger("timeLimit", mission.timeLimit);
		tag.setInteger("type", mission.type);
		tag.setString("customRewards", mission.customRewards);
		tag.setLong("acceptedAt", player.world.getTotalWorldTime());
		tag.setDouble("startX", player.posX);
		tag.setDouble("startZ", player.posZ);
		data(player).setTag(ACTIVE, tag);
		markMissionTaken(player, mission.id);
		sendTranslated(player, TextFormatting.GOLD, "message.narutomod.mission.issued", mission.name);
		syncTo(player, false);
	}

	private static void completeMission(EntityPlayerMP player) {
		NBTTagCompound tag = active(player);
		if (!tag.hasKey("id")) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.no_active_report");
			return;
		}
		if (tag.getInteger("type") == TYPE_STAFF_RP || tag.getInteger("required") <= 0) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.rp_only");
			syncTo(player, false);
			return;
		}
		updateTravelProgress(player, tag);
		if (tag.getInteger("progress") < tag.getInteger("required")) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.incomplete");
			syncTo(player, false);
			return;
		}
		if (remainingTicks(player, tag) <= 0) {
			sendTranslated(player, TextFormatting.RED, "message.narutomod.mission.expired_before_file");
			clearActive(player);
			syncTo(player, false);
			return;
		}
		finishMission(player, false);
	}

	private static void staffCompleteMission(MinecraftServer server, ICommandSender sender, EntityPlayerMP target) {
		NBTTagCompound tag = active(target);
		if (!tag.hasKey("id")) {
			sendTranslated(sender, TextFormatting.RED, "message.narutomod.mission.target_no_active", target.getName());
			return;
		}
		if (remainingTicks(target, tag) <= 0) {
			sendTranslated(sender, TextFormatting.RED, "message.narutomod.mission.target_expired", target.getName());
			clearActive(target);
			syncTo(target, false);
			return;
		}
		finishMission(target, true);
		sendTranslated(sender, TextFormatting.GREEN, "message.narutomod.mission.staff_completed", target.getName());
		if (sender instanceof EntityPlayerMP && sender != target) {
			syncTo((EntityPlayerMP)sender, false);
		}
	}

	private static void finishMission(EntityPlayerMP player, boolean byStaff) {
		NBTTagCompound tag = active(player);
		markMissionTaken(player, tag.getString("id"));
		int reward = tag.getInteger("reward");
		int reputation = tag.getInteger("reputation");
		giveRyo(player, reward);
		data(player).setInteger(REPUTATION, data(player).getInteger(REPUTATION) + reputation);
		sendTranslated(player, TextFormatting.GOLD, byStaff ? "message.narutomod.mission.finished_by_village" : "message.narutomod.mission.finished", tag.getString("id"), reward, reputation);
		if (!tag.getString("customRewards").isEmpty()) {
			sendTranslated(player, TextFormatting.LIGHT_PURPLE, "message.narutomod.mission.custom_rewards", tag.getString("customRewards"));
		}
		clearActive(player);
		syncTo(player, false);
	}

	private static void abandonMission(EntityPlayerMP player) {
		if (data(player).hasKey(ACTIVE)) {
			String name = active(player).getString("name");
			clearActive(player);
			sendTranslated(player, TextFormatting.GRAY, "message.narutomod.mission.abandoned", name);
		}
		syncTo(player, false);
	}

	private static void acceptBounty(EntityPlayerMP hunter, String targetName) {
		EntityPlayerMP target = hunter.mcServer.getPlayerList().getPlayerByUsername(targetName);
		NBTTagCompound entry = AdminData.get(hunter.world).findBingo(targetName);
		if ((target == null || data(target).getInteger(BOUNTY) <= 0) && entry == null) {
			sendTranslated(hunter, TextFormatting.RED, "message.narutomod.bingo.unavailable");
			syncTo(hunter, true);
			return;
		}
		if (target != null) data(hunter).setString(ACTIVE_BOUNTY_UUID, target.getUniqueID().toString());
		else data(hunter).removeTag(ACTIVE_BOUNTY_UUID);
		data(hunter).setString(ACTIVE_BOUNTY_NAME, target != null ? target.getName() : targetName);
		sendTranslated(hunter, TextFormatting.DARK_RED, "message.narutomod.bingo.hunt_authorized", target != null ? target.getName() : targetName);
		syncTo(hunter, true);
	}

	private static void completeBounty(EntityPlayerMP hunter, EntityPlayerMP target) {
		String uuid = data(hunter).getString(ACTIVE_BOUNTY_UUID);
		String activeName = data(hunter).getString(ACTIVE_BOUNTY_NAME);
		boolean matched = (!uuid.isEmpty() && uuid.equals(target.getUniqueID().toString())) || (!activeName.isEmpty() && activeName.equalsIgnoreCase(target.getName()));
		if (!matched) {
			if (data(target).getInteger(BOUNTY) <= 0) {
				addInfamy(hunter, 1);
			}
			return;
		}
		NBTTagCompound staffEntry = AdminData.get(hunter.world).findBingo(target.getName());
		int reward = Math.max(1000, Math.max(data(target).getInteger(BOUNTY), staffEntry == null ? 0 : staffEntry.getInteger("bounty")));
		giveRyo(hunter, reward);
		data(hunter).removeTag(ACTIVE_BOUNTY_UUID);
		data(hunter).removeTag(ACTIVE_BOUNTY_NAME);
		data(target).removeTag(BOUNTY);
		data(target).removeTag(INFAMY);
		sendTranslated(hunter, TextFormatting.DARK_RED, "message.narutomod.bingo.proof_accepted", reward, target.getName());
		sendTranslated(target, TextFormatting.GRAY, "message.narutomod.bingo.entry_removed");
		syncTo(hunter, true);
		syncTo(target, true);
	}

	private static void addInfamy(EntityPlayerMP player, int amount) {
		NBTTagCompound tag = data(player);
		int infamy = tag.getInteger(INFAMY) + amount;
		tag.setInteger(INFAMY, infamy);
		tag.setInteger(BOUNTY, Math.max(tag.getInteger(BOUNTY), 1200 + infamy * 1400));
		sendTranslated(player, TextFormatting.DARK_RED, "message.narutomod.bingo.marked");
	}

	private static void updateTravelProgress(EntityPlayer player, NBTTagCompound tag) {
		if (tag.getInteger("type") != TYPE_TRAVEL) return;
		double dx = player.posX - tag.getDouble("startX");
		double dz = player.posZ - tag.getDouble("startZ");
		int distance = (int)Math.sqrt(dx * dx + dz * dz);
		if (distance > tag.getInteger("progress")) {
			tag.setInteger("progress", Math.min(tag.getInteger("required"), distance));
		}
	}

	private static void incrementKillMission(EntityPlayerMP player, EntityLivingBase victim) {
		NBTTagCompound tag = active(player);
		if (!tag.hasKey("id")) return;
		int type = tag.getInteger("type");
		if (type != TYPE_HOSTILE_KILL && type != TYPE_HUNT) return;
		if (!(victim instanceof IMob) && !(victim instanceof EntityPlayer)) return;
		tag.setInteger("progress", Math.min(tag.getInteger("required"), tag.getInteger("progress") + 1));
		if (tag.getInteger("progress") >= tag.getInteger("required")) {
			player.sendStatusMessage(translated(TextFormatting.YELLOW, "message.narutomod.mission.objective_complete"), true);
		}
	}

	private static int remainingTicks(EntityPlayer player, NBTTagCompound tag) {
		return tag.getInteger("timeLimit") - (int)(player.world.getTotalWorldTime() - tag.getLong("acceptedAt"));
	}

	private static String activeMissionPayload(EntityPlayer player) {
		NBTTagCompound tag = active(player);
		if (!tag.hasKey("id")) return "";
		updateTravelProgress(player, tag);
		int remaining = Math.max(0, remainingTicks(player, tag) / 20);
		return tag.getString("id") + "|" + tag.getString("rank") + "|" + tag.getString("name") + "|" + tag.getString("category") + "|"
		 + tag.getString("objective") + "|" + tag.getInteger("progress") + "|" + tag.getInteger("required") + "|" + tag.getInteger("reward") + "|"
		 + tag.getInteger("reputation") + "|" + remaining;
	}

	private static String wantedPayload(MinecraftServer server) {
		StringBuilder builder = new StringBuilder();
		for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
			NBTTagCompound tag = data(player);
			int bounty = tag.getInteger(BOUNTY);
			if (bounty <= 0) continue;
			updateLastSeen(player);
			if (builder.length() > 0) builder.append(";");
			builder.append(player.getName()).append("|")
			 .append(threatRank(bounty)).append("|")
			 .append(bounty).append("|")
			 .append(tag.getString(LAST_SEEN)).append("|")
			 .append(player.isDead ? "Unknown" : "Active");
		}
		String staff = AdminData.get(server.getEntityWorld()).bingoPayload();
		if (!staff.isEmpty()) {
			if (builder.length() > 0) builder.append(";");
			builder.append(staff);
		}
		return builder.toString();
	}

	private static String threatRank(int bounty) {
		if (bounty >= 40000) return "S";
		if (bounty >= 18000) return "A";
		if (bounty >= 8000) return "B";
		if (bounty >= 3000) return "C";
		return "D";
	}

	private static void updateLastSeen(EntityPlayerMP player) {
		data(player).setString(LAST_SEEN, "Dim " + player.dimension + " / X " + (int)player.posX + " Y " + (int)player.posY + " Z " + (int)player.posZ);
	}

	private static void giveRyo(EntityPlayer player, int amount) {
		amount = Math.max(0, amount);
		amount = giveDenomination(player, ItemRyo1M.block, amount, 1000000);
		amount = giveDenomination(player, ItemRyo10000.block, amount, 10000);
		amount = giveDenomination(player, ItemRyo1000.block, amount, 1000);
		giveDenomination(player, ItemRyo100.block, amount, 100);
	}

	private static int giveDenomination(EntityPlayer player, Item item, int amount, int value) {
		if (item == null) return amount;
		int count = amount / value;
		amount %= value;
		while (count > 0) {
			int give = Math.min(64, count);
			ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(item, give));
			count -= give;
		}
		return amount;
	}

	private static String safe(String value) {
		return value == null ? "" : value.replace("|", "/").replace(";", ",").replace("\n", " ").replace("\r", " ");
	}

	private static int parseIntSafe(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	public static class AdminData extends WorldSavedData {
		private static final String NAME = "NarutomodMissionAdminRecords";
		private NBTTagCompound root = new NBTTagCompound();

		public AdminData() {
			super(NAME);
		}

		public AdminData(String name) {
			super(name);
		}

		public static AdminData get(World world) {
			AdminData data = (AdminData)world.getMapStorage().getOrLoadData(AdminData.class, NAME);
			if (data == null) {
				data = new AdminData();
				world.getMapStorage().setData(NAME, data);
			}
			return data;
		}

		@Override public void readFromNBT(NBTTagCompound nbt) { this.root = nbt.getCompoundTag("root"); }
		@Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) { nbt.setTag("root", this.root); return nbt; }

		private NBTTagList list(String key) {
			NBTTagList list = this.root.getTagList(key, 10);
			this.root.setTag(key, list);
			return list;
		}

		private int nextId() {
			int id = this.root.getInteger("nextId") + 1;
			this.root.setInteger("nextId", id);
			markDirty();
			return id;
		}

		private void add(String key, NBTTagCompound tag) {
			list(key).appendTag(tag);
			markDirty();
		}

		private NBTTagCompound entry(String title, String body, String meta) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "REC-" + nextId());
			tag.setString("title", safe(title));
			tag.setString("body", safe(body));
			tag.setString("meta", safe(meta));
			return tag;
		}

		public void addMission(World world, String payload) {
			String[] p = payload.split(SEP, -1);
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "VILA-" + nextId());
			tag.setString("name", safe(get(p, 0, "Untitled Mission")));
			tag.setString("description", safe(get(p, 1, "Missao criada pelo conselho da vila.")));
			tag.setString("rank", safe(get(p, 2, "C")).toUpperCase());
			tag.setInteger("reward", parseIntSafe(get(p, 3, "0"), 0));
			tag.setInteger("reputation", parseIntSafe(get(p, 4, "0"), 0));
			tag.setString("requiredRank", PlayerStats.normalizeRank(get(p, 5, "Genin")));
			int hours = Math.max(1, parseIntSafe(get(p, 6, "48"), 48));
			tag.setInteger("timeLimit", hours * 60 * 60 * 20);
			tag.setLong("expiresAt", world.getTotalWorldTime() + tag.getInteger("timeLimit"));
			tag.setString("targetPlayer", safe(get(p, 7, "")));
			tag.setString("targetArea", safe(get(p, 8, "")));
			tag.setString("assigned", safe(get(p, 9, "")));
			tag.setString("customRewards", safe(get(p, 10, "")));
			tag.setBoolean("emergency", Boolean.parseBoolean(get(p, 11, "false")));
			add("missions", tag);
		}

		public void addBingo(String payload) {
			String[] p = payload.split(SEP, -1);
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", "BINGO-" + nextId());
			tag.setString("name", safe(get(p, 0, "Shinobi Desconhecido")));
			tag.setString("crime", safe(get(p, 1, "Classificado")));
			tag.setString("threat", safe(get(p, 2, "B")).toUpperCase());
			tag.setInteger("bounty", parseIntSafe(get(p, 3, "0"), 0));
			tag.setString("lastSeen", safe(get(p, 4, "Desconhecido")));
			tag.setString("village", safe(get(p, 5, "Desconhecida")));
			tag.setString("notes", safe(get(p, 6, "")));
			tag.setString("status", safe(get(p, 7, "Ativo")));
			tag.setBoolean("secret", Boolean.parseBoolean(get(p, 8, "false")));
			add("bingo", tag);
		}

		public void addSimple(String key, String payload) {
			String[] p = payload.split(SEP, -1);
			add(key, entry(get(p, 0, "Untitled"), get(p, 1, ""), get(p, 2, "")));
		}

		public void markRogue(EntityPlayerMP player, String crime, int bounty, String notes) {
			NBTTagCompound pdata = data(player);
			pdata.setInteger(BOUNTY, Math.max(1000, bounty));
			pdata.setInteger(INFAMY, Math.max(1, pdata.getInteger(INFAMY)));
			String payload = player.getName() + SEP + crime + SEP + threatRank(bounty) + SEP + bounty + SEP
			 + "Dim " + player.dimension + " / X " + (int)player.posX + " Y " + (int)player.posY + " Z " + (int)player.posZ
			 + SEP + "Nukenin" + SEP + notes + SEP + "Ativo" + SEP + "false";
			addBingo(payload);
		}

		public MissionDef findMission(String id) {
			NBTTagList list = list("missions");
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound tag = list.getCompoundTagAt(i);
				if (tag.getString("id").equals(id)) return MissionDef.fromStaff(tag);
			}
			return null;
		}

		public NBTTagCompound findBingo(String name) {
			NBTTagList list = list("bingo");
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound tag = list.getCompoundTagAt(i);
				if (tag.getString("name").equalsIgnoreCase(name)) return tag;
			}
			return null;
		}

		public String customMissionsPayload(EntityPlayer player) {
			StringBuilder builder = new StringBuilder();
			NBTTagList list = list("missions");
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound tag = list.getCompoundTagAt(i);
				if (tag.getLong("expiresAt") > 0 && player.world.getTotalWorldTime() > tag.getLong("expiresAt")) continue;
				MissionDef mission = MissionDef.fromStaff(tag);
				if (!mission.canSee(player)) continue;
				if (hasTakenMission(player, mission.id)) continue;
				if (builder.length() > 0) builder.append(";");
				builder.append(mission.toPayload(player.world.getTotalWorldTime()));
			}
			return builder.toString();
		}

		public String bingoPayload() {
			StringBuilder builder = new StringBuilder();
			NBTTagList list = list("bingo");
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound tag = list.getCompoundTagAt(i);
				if (builder.length() > 0) builder.append(";");
				if (tag.getBoolean("secret")) {
					builder.append("Desconhecido|").append(tag.getString("threat")).append("|").append(tag.getInteger("bounty")).append("|Desconhecido|Classificado");
				} else {
					builder.append(safe(tag.getString("name"))).append("|").append(tag.getString("threat")).append("|").append(tag.getInteger("bounty")).append("|")
					 .append(safe(tag.getString("lastSeen"))).append("|").append(safe(tag.getString("status") + " - " + tag.getString("crime")));
				}
			}
			return builder.toString();
		}

		public String listSummary(String key) {
			StringBuilder builder = new StringBuilder();
			NBTTagList list = list(key);
			for (int i = Math.max(0, list.tagCount() - 8); i < list.tagCount(); i++) {
				NBTTagCompound tag = list.getCompoundTagAt(i);
				if (builder.length() > 0) builder.append("\n");
				if ("missions".equals(key)) {
					builder.append(tag.getString("id")).append(" | ").append(tag.getString("rank")).append(" | ").append(tag.getString("name")).append(" | ").append(tag.getString("assigned").isEmpty() ? "publica" : tag.getString("assigned"));
				} else if ("bingo".equals(key)) {
					builder.append(tag.getString("id")).append(" | ").append(tag.getString("threat")).append(" | ").append(tag.getString("name")).append(" | ").append(tag.getInteger("bounty")).append(" Ryo");
				} else {
					builder.append(tag.getString("id")).append(" | ").append(tag.getString("title")).append(" | ").append(tag.getString("meta"));
				}
			}
			return builder.toString();
		}

		private static String get(String[] values, int index, String fallback) {
			return index < values.length && values[index] != null && !values[index].isEmpty() ? values[index] : fallback;
		}
	}

	private static void syncTo(EntityPlayerMP player, boolean bingoFirst) {
		String active = activeMissionPayload(player);
		NBTTagCompound tag = data(player);
		String wanted = wantedPayload(player.mcServer);
		NarutomodMod.PACKET_HANDLER.sendTo(new SyncMessage(bingoFirst, PlayerStats.getRank(player), PlayerStats.getClan(player),
		 tag.getInteger(REPUTATION), tag.getInteger(BOUNTY), active, tag.getString(ACTIVE_BOUNTY_NAME), wanted,
		 AdminData.get(player.world).customMissionsPayload(player)), player);
	}

	private static class MissionDef {
		private final String id;
		private final String name;
		private final String rank;
		private final String category;
		private final String objective;
		private final int required;
		private final int reward;
		private final int reputation;
		private final int timeLimit;
		private final int type;
		private final String requiredRank;
		private final String assigned;
		private final String targetPlayer;
		private final String targetArea;
		private final String customRewards;
		private final boolean emergency;
		private final long expiresAt;

		private MissionDef(String id, String name, String rank, String category, String objective, int required, int reward, int reputation, int timeLimit, int type) {
			this(id, name, rank, category, objective, required, reward, reputation, timeLimit, type, "", "", "", "", "", false, 0L);
		}

		private MissionDef(String id, String name, String rank, String category, String objective, int required, int reward, int reputation, int timeLimit, int type,
				String requiredRank, String assigned, String targetPlayer, String targetArea, String customRewards, boolean emergency, long expiresAt) {
			this.id = id;
			this.name = name;
			this.rank = rank;
			this.category = category;
			this.objective = objective;
			this.required = required;
			this.reward = reward;
			this.reputation = reputation;
			this.timeLimit = timeLimit;
			this.type = type;
			this.requiredRank = requiredRank == null || requiredRank.isEmpty() ? rank : requiredRank;
			this.assigned = assigned == null ? "" : assigned;
			this.targetPlayer = targetPlayer == null ? "" : targetPlayer;
			this.targetArea = targetArea == null ? "" : targetArea;
			this.customRewards = customRewards == null ? "" : customRewards;
			this.emergency = emergency;
			this.expiresAt = expiresAt;
		}

		private static MissionDef fromStaff(NBTTagCompound tag) {
			String objective = tag.getString("description");
			if (!tag.getString("targetPlayer").isEmpty()) objective += " Alvo: " + tag.getString("targetPlayer") + ".";
			if (!tag.getString("targetArea").isEmpty()) objective += " Area: " + tag.getString("targetArea") + ".";
			objective += " Registro final somente pelo Kage/conselho da vila.";
			return new MissionDef(tag.getString("id"), tag.getString("name"), tag.getString("rank"), tag.getBoolean("emergency") ? "Alerta da Vila" : "Arco da Vila",
				objective, 1, tag.getInteger("reward"), tag.getInteger("reputation"), tag.getInteger("timeLimit"), TYPE_STAFF_RP,
				tag.getString("requiredRank"), tag.getString("assigned"), tag.getString("targetPlayer"), tag.getString("targetArea"),
				tag.getString("customRewards"), tag.getBoolean("emergency"), tag.getLong("expiresAt"));
		}

		private boolean canSee(EntityPlayer player) {
			if (this.expiresAt > 0 && player.world.getTotalWorldTime() > this.expiresAt) return false;
			if (!this.assigned.isEmpty()) {
				for (String name : this.assigned.split(",")) {
					if (name.trim().equalsIgnoreCase(player.getName())) return true;
				}
				return false;
			}
			return rankIndex(PlayerStats.getRank(player)) >= rankIndex(this.requiredRank);
		}

		private String toPayload(long worldTime) {
			int remaining = this.expiresAt > 0 ? Math.max(0, (int)((this.expiresAt - worldTime) / 20)) : this.timeLimit / 20;
			return safe(this.id) + "|" + safe(this.rank) + "|" + safe(this.name) + "|" + safe(this.category) + "|" + safe(this.objective) + "|"
			 + this.reward + "|" + this.reputation + "|" + safe(this.requiredRank) + "|" + remaining + "|" + safe(this.assigned) + "|"
			 + safe(this.customRewards) + "|" + this.emergency;
		}
	}

	public static class PlayerHook {
		@SubscribeEvent
		public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
			if (event.player instanceof EntityPlayerMP) {
				syncTo((EntityPlayerMP)event.player, false);
			}
		}

		@SubscribeEvent
		public void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
			if (event.getOriginal().getEntityData().hasKey(ROOT)) {
				event.getEntityPlayer().getEntityData().setTag(ROOT, event.getOriginal().getEntityData().getCompoundTag(ROOT).copy());
			}
		}

		@SubscribeEvent
		public void onTick(TickEvent.PlayerTickEvent event) {
			if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || event.player.ticksExisted % 20 != 0) return;
			NBTTagCompound tag = active(event.player);
			if (!tag.hasKey("id")) return;
			updateTravelProgress(event.player, tag);
			if (remainingTicks(event.player, tag) <= 0) {
				sendTranslated(event.player, TextFormatting.RED, "message.narutomod.mission.expired", tag.getString("name"));
				clearActive(event.player);
				if (event.player instanceof EntityPlayerMP) syncTo((EntityPlayerMP)event.player, false);
			}
		}

		@SubscribeEvent
		public void onDeath(LivingDeathEvent event) {
			if (event.getEntityLiving().world.isRemote) return;
			DamageSource source = event.getSource();
			if (!(source.getTrueSource() instanceof EntityPlayerMP)) return;
			EntityPlayerMP killer = (EntityPlayerMP)source.getTrueSource();
			incrementKillMission(killer, event.getEntityLiving());
			if (event.getEntityLiving() instanceof EntityPlayerMP) {
				completeBounty(killer, (EntityPlayerMP)event.getEntityLiving());
			}
			syncTo(killer, false);
		}
	}

	public static class RequestSyncMessage implements IMessage {
		private boolean bingoFirst;

		public RequestSyncMessage() {
		}

		public RequestSyncMessage(boolean bingoFirst) {
			this.bingoFirst = bingoFirst;
		}

		@Override public void toBytes(ByteBuf buf) { buf.writeBoolean(this.bingoFirst); }
		@Override public void fromBytes(ByteBuf buf) { this.bingoFirst = buf.readBoolean(); }

		public static class Handler implements IMessageHandler<RequestSyncMessage, IMessage> {
			@Override public IMessage onMessage(final RequestSyncMessage message, final MessageContext ctx) {
				ctx.getServerHandler().player.getServerWorld().addScheduledTask(new Runnable() {
					@Override public void run() {
						syncTo(ctx.getServerHandler().player, message.bingoFirst);
					}
				});
				return null;
			}
		}
	}

	public static class ActionMessage implements IMessage {
		private int action;
		private String rank;
		private int index;
		private String target;

		public ActionMessage() {
		}

		public ActionMessage(int action, String rank, int index, String target) {
			this.action = action;
			this.rank = rank;
			this.index = index;
			this.target = target == null ? "" : target;
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeInt(this.action);
			ByteBufUtils.writeUTF8String(buf, this.rank == null ? "" : this.rank);
			buf.writeInt(this.index);
			ByteBufUtils.writeUTF8String(buf, this.target == null ? "" : this.target);
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			this.action = buf.readInt();
			this.rank = ByteBufUtils.readUTF8String(buf);
			this.index = buf.readInt();
			this.target = ByteBufUtils.readUTF8String(buf);
		}

		public static class Handler implements IMessageHandler<ActionMessage, IMessage> {
			@Override public IMessage onMessage(final ActionMessage message, final MessageContext ctx) {
				ctx.getServerHandler().player.getServerWorld().addScheduledTask(new Runnable() {
					@Override public void run() {
						EntityPlayerMP player = ctx.getServerHandler().player;
						if (message.action == 0) acceptMission(player, message.rank, message.index);
						else if (message.action == 1) completeMission(player);
						else if (message.action == 2) abandonMission(player);
						else if (message.action == 3) acceptBounty(player, message.target);
						else if (message.action == 4) acceptCustomMission(player, message.target);
						else syncTo(player, false);
					}
				});
				return null;
			}
		}
	}

	public static class SyncMessage implements IMessage {
		private boolean bingoFirst;
		private String rank;
		private String clan;
		private int reputation;
		private int bounty;
		private String active;
		private String activeBounty;
		private String wanted;
		private String customMissions;

		public SyncMessage() {
		}

		public SyncMessage(boolean bingoFirst, String rank, String clan, int reputation, int bounty, String active, String activeBounty, String wanted, String customMissions) {
			this.bingoFirst = bingoFirst;
			this.rank = rank;
			this.clan = clan;
			this.reputation = reputation;
			this.bounty = bounty;
			this.active = active == null ? "" : active;
			this.activeBounty = activeBounty == null ? "" : activeBounty;
			this.wanted = wanted == null ? "" : wanted;
			this.customMissions = customMissions == null ? "" : customMissions;
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeBoolean(this.bingoFirst);
			ByteBufUtils.writeUTF8String(buf, this.rank == null ? "" : this.rank);
			ByteBufUtils.writeUTF8String(buf, this.clan == null ? "" : this.clan);
			buf.writeInt(this.reputation);
			buf.writeInt(this.bounty);
			ByteBufUtils.writeUTF8String(buf, this.active == null ? "" : this.active);
			ByteBufUtils.writeUTF8String(buf, this.activeBounty == null ? "" : this.activeBounty);
			ByteBufUtils.writeUTF8String(buf, this.wanted == null ? "" : this.wanted);
			ByteBufUtils.writeUTF8String(buf, this.customMissions == null ? "" : this.customMissions);
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			this.bingoFirst = buf.readBoolean();
			this.rank = ByteBufUtils.readUTF8String(buf);
			this.clan = ByteBufUtils.readUTF8String(buf);
			this.reputation = buf.readInt();
			this.bounty = buf.readInt();
			this.active = ByteBufUtils.readUTF8String(buf);
			this.activeBounty = ByteBufUtils.readUTF8String(buf);
			this.wanted = ByteBufUtils.readUTF8String(buf);
			this.customMissions = ByteBufUtils.readUTF8String(buf);
		}

		public static class Handler implements IMessageHandler<SyncMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(final SyncMessage message, MessageContext ctx) {
				net.narutomod.client.MissionClient.handleSync(message.bingoFirst, message.rank, message.clan, message.reputation,
				 message.bounty, message.active, message.activeBounty, message.wanted, message.customMissions);
				return null;
			}
		}
	}

	public static class OpenAdminMessage implements IMessage {
		@Override public void toBytes(ByteBuf buf) {}
		@Override public void fromBytes(ByteBuf buf) {}

		public static class Handler implements IMessageHandler<OpenAdminMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(OpenAdminMessage message, MessageContext ctx) {
				net.narutomod.client.MissionClient.openAdminFromPacket();
				return null;
			}
		}
	}

	public static class AdminActionMessage implements IMessage {
		private int action;
		private String payload;

		public AdminActionMessage() {}
		public AdminActionMessage(int action, String payload) {
			this.action = action;
			this.payload = payload == null ? "" : payload;
		}

		@Override public void toBytes(ByteBuf buf) {
			buf.writeInt(this.action);
			ByteBufUtils.writeUTF8String(buf, this.payload == null ? "" : this.payload);
		}

		@Override public void fromBytes(ByteBuf buf) {
			this.action = buf.readInt();
			this.payload = ByteBufUtils.readUTF8String(buf);
		}

		public static class Handler implements IMessageHandler<AdminActionMessage, IMessage> {
			@Override public IMessage onMessage(final AdminActionMessage message, final MessageContext ctx) {
				ctx.getServerHandler().player.getServerWorld().addScheduledTask(new Runnable() {
					@Override public void run() {
						EntityPlayerMP staff = ctx.getServerHandler().player;
						if (!isStaff(staff)) {
							sendTranslated(staff, TextFormatting.RED, "message.narutomod.mission.action_denied");
							return;
						}
						AdminData data = AdminData.get(staff.world);
						if (message.action == 0) {
							data.addMission(staff.world, message.payload);
							sendTranslated(staff, TextFormatting.GOLD, "message.narutomod.mission.order_published");
						} else if (message.action == 1) {
							data.addBingo(message.payload);
							String[] p = message.payload.split(SEP, -1);
							EntityPlayerMP target = staff.mcServer.getPlayerList().getPlayerByUsername(AdminData.get(p, 0, ""));
							if (target != null) data(target).setInteger(BOUNTY, parseIntSafe(AdminData.get(p, 3, "0"), 0));
							sendTranslated(staff, TextFormatting.DARK_RED, "message.narutomod.bingo.entry_added");
						} else if (message.action >= 2 && message.action <= 5) {
							String key = message.action == 2 ? "clues" : message.action == 3 ? "events" : message.action == 4 ? "notes" : "arcs";
							data.addSimple(key, message.payload);
							sendTranslated(staff, TextFormatting.YELLOW, "message.narutomod.mission.record_added", key);
						} else if (message.action == 6) {
							String[] p = message.payload.split(SEP, -1);
							EntityPlayerMP target = staff.mcServer.getPlayerList().getPlayerByUsername(AdminData.get(p, 0, ""));
							if (target != null) {
								PlayerStats.setRank(target, AdminData.get(p, 1, "Genin"));
								sendTranslated(staff, TextFormatting.GREEN, "message.narutomod.mission.promoted", target.getName(), PlayerStats.getRank(target));
							}
						} else if (message.action == 7) {
							String[] p = message.payload.split(SEP, -1);
							EntityPlayerMP target = staff.mcServer.getPlayerList().getPlayerByUsername(AdminData.get(p, 0, ""));
							if (target != null) {
								data.markRogue(target, AdminData.get(p, 1, "Declarado nukenin pela vila"), parseIntSafe(AdminData.get(p, 2, "5000"), 5000), AdminData.get(p, 3, ""));
								sendTranslated(staff, TextFormatting.DARK_RED, "message.narutomod.mission.nukenin", target.getName());
							}
						}
						for (EntityPlayerMP player : staff.mcServer.getPlayerList().getPlayers()) syncTo(player, false);
						sendAdminSync(staff);
					}
				});
				return null;
			}
		}
	}

	public static class AdminSyncMessage implements IMessage {
		private String missions;
		private String bingo;
		private String clues;
		private String events;
		private String notes;
		private String arcs;

		public AdminSyncMessage() {}
		public AdminSyncMessage(String missions, String bingo, String clues, String events, String notes, String arcs) {
			this.missions = missions == null ? "" : missions;
			this.bingo = bingo == null ? "" : bingo;
			this.clues = clues == null ? "" : clues;
			this.events = events == null ? "" : events;
			this.notes = notes == null ? "" : notes;
			this.arcs = arcs == null ? "" : arcs;
		}

		@Override public void toBytes(ByteBuf buf) {
			ByteBufUtils.writeUTF8String(buf, this.missions);
			ByteBufUtils.writeUTF8String(buf, this.bingo);
			ByteBufUtils.writeUTF8String(buf, this.clues);
			ByteBufUtils.writeUTF8String(buf, this.events);
			ByteBufUtils.writeUTF8String(buf, this.notes);
			ByteBufUtils.writeUTF8String(buf, this.arcs);
		}

		@Override public void fromBytes(ByteBuf buf) {
			this.missions = ByteBufUtils.readUTF8String(buf);
			this.bingo = ByteBufUtils.readUTF8String(buf);
			this.clues = ByteBufUtils.readUTF8String(buf);
			this.events = ByteBufUtils.readUTF8String(buf);
			this.notes = ByteBufUtils.readUTF8String(buf);
			this.arcs = ByteBufUtils.readUTF8String(buf);
		}

		public static class Handler implements IMessageHandler<AdminSyncMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(final AdminSyncMessage message, MessageContext ctx) {
				net.narutomod.client.MissionClient.handleAdminSync(message.missions, message.bingo, message.clues,
				 message.events, message.notes, message.arcs);
				return null;
			}
		}
	}

	private static List<String[]> parseWanted(String payload) {
		List<String[]> list = new ArrayList<String[]>();
		if (payload == null || payload.isEmpty()) return list;
		for (String entry : payload.split(";")) {
			String[] parts = entry.split("\\|", -1);
			if (parts.length >= 5) list.add(parts);
		}
		return list;
	}

}
