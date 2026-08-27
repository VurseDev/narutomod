package net.narutomod;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Server-hosted, client-cached ambient music for RP scenes. Only OGG files are accepted. */
@ElementsNarutomodMod.ModElement.Tag
public class AmbientMusicSystem extends ElementsNarutomodMod.ModElement {
	private static final Logger LOGGER = LogManager.getLogger("NarutomodMusic");
	public static final int CHUNK_SIZE = 24 * 1024;
	public static final long MAX_TRACK_BYTES = 128L * 1024L * 1024L;
	private static final int MAX_TRACKS = 512;
	private static final int CHUNKS_PER_TICK = 3;
	private static final Map<String, Track> TRACKS = new LinkedHashMap<>();
	private static final Map<UUID, Deque<Transfer>> TRANSFERS = new LinkedHashMap<>();
	private static Path musicFolder;
	private static String libraryId = "uninitialized";
	private static PlayMessage globalPlayback;

	public AmbientMusicSystem(ElementsNarutomodMod instance) {
		super(instance, 1012);
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		musicFolder = event.getModConfigurationDirectory().toPath().resolve("narutomod_server_music");
		elements.addNetworkMessage(ManifestMessage.Handler.class, ManifestMessage.class, Side.CLIENT);
		elements.addNetworkMessage(RequestTrackMessage.Handler.class, RequestTrackMessage.class, Side.SERVER);
		elements.addNetworkMessage(TrackChunkMessage.Handler.class, TrackChunkMessage.class, Side.CLIENT);
		elements.addNetworkMessage(PlayMessage.Handler.class, PlayMessage.class, Side.CLIENT);
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		if (event.getSide().isClient()) initClient();
	}

	@SideOnly(Side.CLIENT)
	private void initClient() {
		net.narutomod.client.ClientAmbientMusic.initialize();
	}

	@Override
	public void serverLoad(FMLServerStartingEvent event) {
		scanLibrary();
		event.registerServerCommand(new MusicCommand());
	}

	@SubscribeEvent
	public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.player instanceof EntityPlayerMP)) return;
		EntityPlayerMP player = (EntityPlayerMP)event.player;
		sendManifest(player);
		if (globalPlayback != null) NarutomodMod.PACKET_HANDLER.sendTo(globalPlayback, player);
	}

	@SubscribeEvent
	public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.player instanceof EntityPlayerMP) closeTransfers(event.player.getUniqueID());
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || TRANSFERS.isEmpty()) return;
		List<UUID> finishedPlayers = new ArrayList<>();
		for (Map.Entry<UUID, Deque<Transfer>> entry : new ArrayList<>(TRANSFERS.entrySet())) {
			EntityPlayerMP player = findPlayer(entry.getKey());
			if (player == null) {
				closeQueue(entry.getValue());
				finishedPlayers.add(entry.getKey());
				continue;
			}
			Deque<Transfer> queue = entry.getValue();
			for (int sent = 0; sent < CHUNKS_PER_TICK && !queue.isEmpty(); sent++) {
				Transfer transfer = queue.peekFirst();
				try {
					byte[] data = transfer.readChunk();
					if (data.length == 0) {
						transfer.close();
						queue.removeFirst();
						sent--;
						continue;
					}
					long offset = transfer.offset - data.length;
					NarutomodMod.PACKET_HANDLER.sendTo(new TrackChunkMessage(transfer.track, offset, data), player);
					if (transfer.offset >= transfer.track.size) {
						transfer.close();
						queue.removeFirst();
					}
				} catch (IOException ex) {
					LOGGER.warn("Unable to transfer music track {} to {}", transfer.track.id, player.getName(), ex);
					transfer.close();
					queue.removeFirst();
				}
			}
			if (queue.isEmpty()) finishedPlayers.add(entry.getKey());
		}
		for (UUID id : finishedPlayers) TRANSFERS.remove(id);
	}

	private static EntityPlayerMP findPlayer(UUID id) {
		MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
		return server == null ? null : server.getPlayerList().getPlayerByUUID(id);
	}

	public static synchronized int scanLibrary() {
		TRACKS.clear();
		try {
			Files.createDirectories(musicFolder);
			writeInstructions();
			libraryId = loadLibraryId();
			List<Path> paths;
			try (Stream<Path> stream = Files.list(musicFolder)) {
				paths = stream.filter(Files::isRegularFile)
				 .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
				 .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
				 .limit(MAX_TRACKS).collect(Collectors.toList());
			}
			for (Path path : paths) {
				long size = Files.size(path);
				if (size <= 0L || size > MAX_TRACK_BYTES) {
					LOGGER.warn("Skipping {}: track must be between 1 byte and {} MB", path.getFileName(), MAX_TRACK_BYTES / 1024L / 1024L);
					continue;
				}
				String filename = path.getFileName().toString();
				String display = filename.substring(0, filename.length() - 4);
				String baseId = safeId(display);
				String id = baseId;
				for (int suffix = 2; TRACKS.containsKey(id); suffix++) id = baseId + "_" + suffix;
				Track track = new Track(id, display, size, sha256(path), path);
				TRACKS.put(id, track);
			}
			LOGGER.info("Loaded {} server music tracks from {}", TRACKS.size(), musicFolder.toAbsolutePath());
		} catch (Exception ex) {
			LOGGER.error("Could not scan server music folder " + musicFolder, ex);
		}
		return TRACKS.size();
	}

	private static void writeInstructions() throws IOException {
		Path readme = musicFolder.resolve("README.txt");
		if (Files.exists(readme)) return;
		String text = "Narutomod server music library\r\n\r\n"
		 + "Place .ogg files directly in this folder, then run /rpmusic rescan.\r\n"
		 + "Use /rpmusic list to see the generated track IDs.\r\n"
		 + "Use /rpmusic play <track> [player|all] [fadeSeconds] [volume0-100] [loop].\r\n"
		 + "Use /rpmusic stop [player|all] [fadeSeconds].\r\n"
		 + "Players can use /music volume, /music mute, /music unmute, /music now, and /music cache.\r\n\r\n"
		 + "Only distribute audio that you have permission to share with connecting players.\r\n";
		Files.write(readme, text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
	}

	private static String loadLibraryId() throws IOException {
		Path marker = musicFolder.resolve(".library-id");
		if (Files.isRegularFile(marker)) {
			String id = new String(Files.readAllBytes(marker), StandardCharsets.UTF_8).trim();
			if (id.matches("[A-Za-z0-9-]{8,64}")) return id;
		}
		String id = UUID.randomUUID().toString();
		Files.write(marker, id.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		return id;
	}

	public static String safeId(String text) {
		String id = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_").replaceAll("^[_.]+|[_.]+$", "");
		if (id.isEmpty()) id = "track";
		return id.length() > 64 ? id.substring(0, 64) : id;
	}

	public static String sha256(Path path) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (java.io.InputStream input = Files.newInputStream(path)) {
			byte[] buffer = new byte[64 * 1024];
			for (int read; (read = input.read(buffer)) >= 0;) if (read > 0) digest.update(buffer, 0, read);
		}
		StringBuilder out = new StringBuilder(64);
		for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
		return out.toString();
	}

	private static void sendManifest(EntityPlayerMP player) {
		NarutomodMod.PACKET_HANDLER.sendTo(new ManifestMessage(libraryId, TRACKS.values()), player);
	}

	private static void sendManifest(Collection<EntityPlayerMP> players) {
		for (EntityPlayerMP player : players) sendManifest(player);
	}

	private static void requestTrack(EntityPlayerMP player, String id, String hash) {
		Track track = TRACKS.get(safeId(id));
		if (track == null || !track.hash.equalsIgnoreCase(hash)) return;
		Deque<Transfer> queue = TRANSFERS.computeIfAbsent(player.getUniqueID(), ignored -> new ArrayDeque<>());
		for (Transfer transfer : queue) if (transfer.track.id.equals(track.id) && transfer.track.hash.equals(track.hash)) return;
		queue.addLast(new Transfer(track));
	}

	private static void closeTransfers(UUID player) {
		Deque<Transfer> queue = TRANSFERS.remove(player);
		if (queue != null) closeQueue(queue);
	}

	private static void closeQueue(Deque<Transfer> queue) {
		for (Transfer transfer : queue) transfer.close();
		queue.clear();
	}

	private static void sendPlay(Collection<EntityPlayerMP> players, PlayMessage message) {
		for (EntityPlayerMP player : players) NarutomodMod.PACKET_HANDLER.sendTo(message, player);
	}

	public static final class Track {
		public final String id;
		public final String display;
		public final long size;
		public final String hash;
		public final Path path;
		private Track(String id, String display, long size, String hash, Path path) {
			this.id = id;
			this.display = display;
			this.size = size;
			this.hash = hash;
			this.path = path;
		}
	}

	private static final class Transfer {
		private final Track track;
		private FileChannel channel;
		private long offset;
		private Transfer(Track track) { this.track = track; }
		private byte[] readChunk() throws IOException {
			if (this.offset >= this.track.size) return new byte[0];
			if (this.channel == null) this.channel = FileChannel.open(this.track.path, StandardOpenOption.READ);
			int length = (int)Math.min(CHUNK_SIZE, this.track.size - this.offset);
			ByteBuffer buffer = ByteBuffer.allocate(length);
			int total = 0;
			while (buffer.hasRemaining()) {
				int read = this.channel.read(buffer);
				if (read < 0) break;
				total += read;
			}
			this.offset += total;
			if (total == length) return buffer.array();
			byte[] result = new byte[total];
			System.arraycopy(buffer.array(), 0, result, 0, total);
			return result;
		}
		private void close() {
			if (this.channel != null) try { this.channel.close(); } catch (IOException ignored) { }
			this.channel = null;
		}
	}

	public static class ManifestMessage implements IMessage {
		public String libraryId = "";
		public final List<TrackInfo> tracks = new ArrayList<>();
		public ManifestMessage() { }
		public ManifestMessage(String libraryId, Collection<Track> tracks) {
			this.libraryId = libraryId;
			for (Track track : tracks) this.tracks.add(new TrackInfo(track.id, track.display, track.size, track.hash));
		}
		@Override public void toBytes(ByteBuf buf) {
			ByteBufUtils.writeUTF8String(buf, this.libraryId);
			buf.writeInt(this.tracks.size());
			for (TrackInfo track : this.tracks) {
				ByteBufUtils.writeUTF8String(buf, track.id);
				ByteBufUtils.writeUTF8String(buf, track.display);
				buf.writeLong(track.size);
				ByteBufUtils.writeUTF8String(buf, track.hash);
			}
		}
		@Override public void fromBytes(ByteBuf buf) {
			this.libraryId = ByteBufUtils.readUTF8String(buf);
			int count = MathHelper.clamp(buf.readInt(), 0, MAX_TRACKS);
			for (int i = 0; i < count; i++) this.tracks.add(new TrackInfo(ByteBufUtils.readUTF8String(buf),
			 ByteBufUtils.readUTF8String(buf), buf.readLong(), ByteBufUtils.readUTF8String(buf)));
		}
		public static class Handler implements IMessageHandler<ManifestMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(ManifestMessage message, MessageContext context) {
				net.narutomod.client.ClientAmbientMusic.handleManifest(message);
				return null;
			}
		}
	}

	public static final class TrackInfo {
		public final String id;
		public final String display;
		public final long size;
		public final String hash;
		public TrackInfo(String id, String display, long size, String hash) {
			this.id = id;
			this.display = display;
			this.size = size;
			this.hash = hash;
		}
	}

	public static class RequestTrackMessage implements IMessage {
		private String id = "";
		private String hash = "";
		public RequestTrackMessage() { }
		public RequestTrackMessage(String id, String hash) { this.id = id; this.hash = hash; }
		@Override public void toBytes(ByteBuf buf) { ByteBufUtils.writeUTF8String(buf, this.id); ByteBufUtils.writeUTF8String(buf, this.hash); }
		@Override public void fromBytes(ByteBuf buf) { this.id = ByteBufUtils.readUTF8String(buf); this.hash = ByteBufUtils.readUTF8String(buf); }
		public static class Handler implements IMessageHandler<RequestTrackMessage, IMessage> {
			@Override public IMessage onMessage(RequestTrackMessage message, MessageContext context) {
				EntityPlayerMP player = context.getServerHandler().player;
				player.getServerWorld().addScheduledTask(() -> requestTrack(player, message.id, message.hash));
				return null;
			}
		}
	}

	public static class TrackChunkMessage implements IMessage {
		public String id = "";
		public String hash = "";
		public long totalSize;
		public long offset;
		public byte[] data = new byte[0];
		public TrackChunkMessage() { }
		public TrackChunkMessage(Track track, long offset, byte[] data) {
			this.id = track.id; this.hash = track.hash; this.totalSize = track.size; this.offset = offset; this.data = data;
		}
		@Override public void toBytes(ByteBuf buf) {
			ByteBufUtils.writeUTF8String(buf, this.id); ByteBufUtils.writeUTF8String(buf, this.hash);
			buf.writeLong(this.totalSize); buf.writeLong(this.offset); buf.writeInt(this.data.length); buf.writeBytes(this.data);
		}
		@Override public void fromBytes(ByteBuf buf) {
			this.id = ByteBufUtils.readUTF8String(buf); this.hash = ByteBufUtils.readUTF8String(buf);
			this.totalSize = buf.readLong(); this.offset = buf.readLong();
			int length = MathHelper.clamp(buf.readInt(), 0, CHUNK_SIZE);
			this.data = new byte[length]; buf.readBytes(this.data);
		}
		public static class Handler implements IMessageHandler<TrackChunkMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(TrackChunkMessage message, MessageContext context) {
				net.narutomod.client.ClientAmbientMusic.receiveChunk(message);
				return null;
			}
		}
	}

	public static class PlayMessage implements IMessage {
		public boolean stop;
		public String id = "";
		public int fadeTicks;
		public float volume = 1f;
		public boolean loop;
		public PlayMessage() { }
		public static PlayMessage play(String id, int fadeTicks, float volume, boolean loop) {
			PlayMessage message = new PlayMessage(); message.id = id; message.fadeTicks = fadeTicks;
			message.volume = volume; message.loop = loop; return message;
		}
		public static PlayMessage stop(int fadeTicks) {
			PlayMessage message = new PlayMessage(); message.stop = true; message.fadeTicks = fadeTicks; return message;
		}
		@Override public void toBytes(ByteBuf buf) {
			buf.writeBoolean(this.stop); ByteBufUtils.writeUTF8String(buf, this.id); buf.writeInt(this.fadeTicks);
			buf.writeFloat(this.volume); buf.writeBoolean(this.loop);
		}
		@Override public void fromBytes(ByteBuf buf) {
			this.stop = buf.readBoolean(); this.id = ByteBufUtils.readUTF8String(buf);
			this.fadeTicks = MathHelper.clamp(buf.readInt(), 0, 20 * 60); this.volume = MathHelper.clamp(buf.readFloat(), 0f, 1f); this.loop = buf.readBoolean();
		}
		public static class Handler implements IMessageHandler<PlayMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override public IMessage onMessage(PlayMessage message, MessageContext context) {
				net.narutomod.client.ClientAmbientMusic.handlePlay(message);
				return null;
			}
		}
	}

	private static class MusicCommand extends CommandBase {
		@Override public String getName() { return "rpmusic"; }
		@Override public String getUsage(ICommandSender sender) {
			return "/rpmusic <list|rescan|sync [player|all]|play <track> [player|all] [fadeSeconds] [volume0-100] [loop]|stop [player|all] [fadeSeconds]|now>";
		}
		@Override public int getRequiredPermissionLevel() { return 3; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return sender.canUseCommand(3, this.getName()); }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length == 0) throw new CommandException(getUsage(sender));
			String action = args[0].toLowerCase(Locale.ROOT);
			if ("list".equals(action)) {
				if (TRACKS.isEmpty()) send(sender, TextFormatting.YELLOW + "No .ogg tracks found in " + musicFolder.toAbsolutePath());
				else for (Track track : TRACKS.values()) send(sender, TextFormatting.AQUA + track.id + TextFormatting.GRAY + " - " + track.display + " (" + (track.size / 1024L) + " KB)");
			} else if ("rescan".equals(action)) {
				closeAllTransfers();
				int count = scanLibrary();
				sendManifest(server.getPlayerList().getPlayers());
				send(sender, TextFormatting.GREEN + "Rescanned " + count + " tracks in " + musicFolder.toAbsolutePath());
			} else if ("sync".equals(action)) {
				Collection<EntityPlayerMP> players = targets(server, sender, args.length > 1 ? args[1] : "all");
				sendManifest(players);
				send(sender, TextFormatting.GREEN + "Sent music manifest to " + players.size() + " player(s).");
			} else if ("play".equals(action)) {
				if (args.length < 2) throw new CommandException(getUsage(sender));
				Track track = TRACKS.get(safeId(args[1]));
				if (track == null) throw new CommandException("Unknown track: " + args[1] + ". Use /rpmusic list.");
				String selector = args.length > 2 ? args[2] : "all";
				int fadeTicks = args.length > 3 ? MathHelper.clamp((int)(parseDouble(args[3], 0d, 60d) * 20d), 0, 1200) : 40;
				float volume = args.length > 4 ? parseInt(args[4], 0, 100) / 100f : 1f;
				boolean loop = args.length > 5 && parseBoolean(args[5]);
				Collection<EntityPlayerMP> players = targets(server, sender, selector);
				PlayMessage message = PlayMessage.play(track.id, fadeTicks, volume, loop);
				sendPlay(players, message);
				if ("all".equalsIgnoreCase(selector) || "@a".equalsIgnoreCase(selector)) globalPlayback = message;
				send(sender, TextFormatting.GREEN + "Playing " + track.display + " for " + players.size() + " player(s).");
			} else if ("stop".equals(action)) {
				String selector = args.length > 1 ? args[1] : "all";
				int fadeTicks = args.length > 2 ? MathHelper.clamp((int)(parseDouble(args[2], 0d, 60d) * 20d), 0, 1200) : 40;
				Collection<EntityPlayerMP> players = targets(server, sender, selector);
				sendPlay(players, PlayMessage.stop(fadeTicks));
				if ("all".equalsIgnoreCase(selector) || "@a".equalsIgnoreCase(selector)) globalPlayback = null;
				send(sender, TextFormatting.YELLOW + "Stopping server music for " + players.size() + " player(s).");
			} else if ("now".equals(action)) {
				send(sender, globalPlayback == null ? "No global server track is active." : "Global track: " + globalPlayback.id + (globalPlayback.loop ? " (looping)" : ""));
			} else throw new CommandException(getUsage(sender));
		}

		private Collection<EntityPlayerMP> targets(MinecraftServer server, ICommandSender sender, String selector) throws CommandException {
			if ("all".equalsIgnoreCase(selector) || "@a".equalsIgnoreCase(selector)) return new ArrayList<>(server.getPlayerList().getPlayers());
			return Collections.singletonList(getPlayer(server, sender, selector));
		}

		private void send(ICommandSender sender, String text) { sender.sendMessage(new TextComponentString(text)); }

		@Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 1) return getListOfStringsMatchingLastWord(args, "list", "rescan", "sync", "play", "stop", "now");
			if (args.length == 2 && "play".equalsIgnoreCase(args[0])) return getListOfStringsMatchingLastWord(args, TRACKS.keySet());
			if ((args.length == 2 && ("sync".equalsIgnoreCase(args[0]) || "stop".equalsIgnoreCase(args[0])))
			 || (args.length == 3 && "play".equalsIgnoreCase(args[0]))) {
				List<String> names = new ArrayList<>(); names.add("all"); Collections.addAll(names, server.getOnlinePlayerNames());
				return getListOfStringsMatchingLastWord(args, names);
			}
			return Collections.emptyList();
		}
	}

	private static void closeAllTransfers() {
		for (Deque<Transfer> queue : TRANSFERS.values()) closeQueue(queue);
		TRANSFERS.clear();
	}
}
