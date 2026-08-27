package net.narutomod.client;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import net.narutomod.AmbientMusicSystem;
import net.narutomod.NarutomodMod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Receives, caches, and streams server-provided OGG music without modifying game resources. */
@SideOnly(Side.CLIENT)
public final class ClientAmbientMusic {
	private static final Logger LOGGER = LogManager.getLogger("NarutomodMusicClient");
	private static final ClientAmbientMusic INSTANCE = new ClientAmbientMusic();
	private static final String SOURCE = "narutomod_server_music";
	private final Map<String, AmbientMusicSystem.TrackInfo> manifest = new LinkedHashMap<>();
	private final Map<String, Download> downloads = new LinkedHashMap<>();
	private String libraryId = "";
	private Path cacheDir;
	private Path settingsFile;
	private boolean enabled = true;
	private float localVolume = 1f;
	private SoundSystem soundSystem;
	private String currentId = "";
	private String currentDisplay = "";
	private float currentGain;
	private float goalGain;
	private int fadeRemaining;
	private int playAge;
	private boolean stopping;
	private boolean looping;
	private AmbientMusicSystem.PlayMessage waitingForFile;
	private AmbientMusicSystem.PlayMessage afterFade;

	private ClientAmbientMusic() { }

	public static void initialize() {
		INSTANCE.settingsFile = Minecraft.getMinecraft().mcDataDir.toPath().resolve("config").resolve("narutomod_music_client.properties");
		INSTANCE.loadSettings();
		MinecraftForge.EVENT_BUS.register(INSTANCE);
		FMLCommonHandler.instance().bus().register(new DisconnectHook());
		ClientCommandHandler.instance.registerCommand(new LocalMusicCommand());
	}

	public static void handleManifest(AmbientMusicSystem.ManifestMessage message) {
		Minecraft.getMinecraft().addScheduledTask(() -> INSTANCE.applyManifest(message));
	}

	public static void receiveChunk(AmbientMusicSystem.TrackChunkMessage message) {
		INSTANCE.acceptChunk(message);
	}

	public static void handlePlay(AmbientMusicSystem.PlayMessage message) {
		Minecraft.getMinecraft().addScheduledTask(() -> INSTANCE.applyPlay(message));
	}

	private synchronized void applyManifest(AmbientMusicSystem.ManifestMessage message) {
		String safeLibrary = message.libraryId == null ? "" : message.libraryId.replaceAll("[^A-Za-z0-9-]", "");
		if (safeLibrary.length() < 8) {
			LOGGER.warn("Rejected invalid server music library id");
			return;
		}
		if (!safeLibrary.equals(this.libraryId)) {
			this.downloads.clear();
			this.manifest.clear();
			this.libraryId = safeLibrary;
			this.cacheDir = Minecraft.getMinecraft().mcDataDir.toPath().resolve("narutomod_music_cache").resolve(safeLibrary);
			try { Files.createDirectories(this.cacheDir); }
			catch (IOException ex) { LOGGER.error("Could not create server music cache " + this.cacheDir, ex); return; }
		}
		this.manifest.clear();
		for (AmbientMusicSystem.TrackInfo track : message.tracks) {
			if (valid(track)) this.manifest.put(track.id, track);
		}
		for (AmbientMusicSystem.TrackInfo track : this.manifest.values()) {
			if (!isCached(track)) request(track);
		}
		Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_AQUA
		 + "Server music library: " + this.manifest.size() + " track(s), missing files sync in the background."), false);
	}

	private boolean valid(AmbientMusicSystem.TrackInfo track) {
		return track != null && track.id != null && track.id.equals(AmbientMusicSystem.safeId(track.id))
		 && track.id.length() <= 64 && track.hash != null && track.hash.matches("[0-9a-fA-F]{64}")
		 && track.size > 0L && track.size <= AmbientMusicSystem.MAX_TRACK_BYTES;
	}

	private Path cachedFile(AmbientMusicSystem.TrackInfo track) {
		return this.cacheDir.resolve(track.id + "-" + track.hash.substring(0, 16).toLowerCase(Locale.ROOT) + ".ogg");
	}

	private boolean isCached(AmbientMusicSystem.TrackInfo track) {
		if (this.cacheDir == null) return false;
		Path file = cachedFile(track);
		try { return Files.isRegularFile(file) && Files.size(file) == track.size; }
		catch (IOException ignored) { return false; }
	}

	private void request(AmbientMusicSystem.TrackInfo track) {
		NarutomodMod.PACKET_HANDLER.sendToServer(new AmbientMusicSystem.RequestTrackMessage(track.id, track.hash));
	}

	private synchronized void acceptChunk(AmbientMusicSystem.TrackChunkMessage message) {
		AmbientMusicSystem.TrackInfo track = this.manifest.get(message.id);
		if (track == null || this.cacheDir == null || !track.hash.equalsIgnoreCase(message.hash)
		 || track.size != message.totalSize || message.data == null || message.data.length == 0
		 || message.data.length > AmbientMusicSystem.CHUNK_SIZE) return;
		try {
			Download download = this.downloads.get(track.id);
			if (download == null) {
				if (message.offset != 0L) return;
				Path part = this.cacheDir.resolve(track.id + "-" + track.hash.substring(0, 16).toLowerCase(Locale.ROOT) + ".part");
				Files.deleteIfExists(part);
				download = new Download(track, part);
				this.downloads.put(track.id, download);
			}
			if (message.offset != download.received || message.offset + message.data.length > track.size) return;
			try (RandomAccessFile output = new RandomAccessFile(download.part.toFile(), "rw")) {
				output.seek(message.offset);
				output.write(message.data);
			}
			download.received += message.data.length;
			if (download.received == track.size) finishDownload(download);
		} catch (Exception ex) {
			LOGGER.warn("Unable to cache server music track " + message.id, ex);
			Download failed = this.downloads.remove(message.id);
			if (failed != null) try { Files.deleteIfExists(failed.part); } catch (IOException ignored) { }
		}
	}

	private void finishDownload(Download download) throws Exception {
		this.downloads.remove(download.track.id);
		String actual = AmbientMusicSystem.sha256(download.part);
		if (!actual.equalsIgnoreCase(download.track.hash)) {
			Files.deleteIfExists(download.part);
			LOGGER.warn("Checksum rejected for server music track {}", download.track.id);
			return;
		}
		Path target = cachedFile(download.track);
		try {
			Files.move(download.part, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException ex) {
			Files.move(download.part, target, StandardCopyOption.REPLACE_EXISTING);
		}
		LOGGER.info("Cached server music track {}", download.track.id);
		Minecraft.getMinecraft().addScheduledTask(() -> {
			if (this.waitingForFile != null && this.waitingForFile.id.equals(download.track.id)) {
				AmbientMusicSystem.PlayMessage waiting = this.waitingForFile;
				this.waitingForFile = null;
				this.play(waiting);
			}
		});
	}

	private void applyPlay(AmbientMusicSystem.PlayMessage message) {
		if (message.stop) {
			this.waitingForFile = null;
			beginStop(message.fadeTicks, null);
			return;
		}
		if (!this.enabled) return;
		AmbientMusicSystem.TrackInfo track = this.manifest.get(message.id);
		if (track == null) return;
		if (!isCached(track)) {
			this.waitingForFile = copy(message);
			request(track);
			Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW
			 + "Downloading server track: " + track.display), true);
			return;
		}
		play(message);
	}

	private void play(AmbientMusicSystem.PlayMessage message) {
		if (!this.currentId.isEmpty()) {
			beginStop(message.fadeTicks, copy(message));
			return;
		}
		start(message);
	}

	private void start(AmbientMusicSystem.PlayMessage message) {
		AmbientMusicSystem.TrackInfo track = this.manifest.get(message.id);
		if (track == null || !isCached(track) || !this.enabled) return;
		SoundSystem system = getSoundSystem();
		if (system == null) {
			Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.RED + "Server music could not access Minecraft's sound system."));
			return;
		}
		try {
			system.stop(SOURCE);
			system.removeSource(SOURCE);
			Path file = cachedFile(track);
			system.newStreamingSource(true, SOURCE, file.toUri().toURL(), file.getFileName().toString(), message.loop,
			 0f, 0f, 0f, SoundSystemConfig.ATTENUATION_NONE, 0f);
			this.currentId = track.id;
			this.currentDisplay = track.display;
			this.looping = message.loop;
			this.playAge = 0;
			this.stopping = false;
			this.currentGain = message.fadeTicks > 0 ? 0f : message.volume;
			this.goalGain = message.volume;
			this.fadeRemaining = message.fadeTicks;
			system.setVolume(SOURCE, effectiveVolume(this.currentGain));
			system.play(SOURCE);
			Minecraft.getMinecraft().getSoundHandler().stop("", SoundCategory.MUSIC);
			Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_AQUA + "Now playing: " + track.display), true);
		} catch (Exception ex) {
			LOGGER.error("Unable to play cached server track " + track.id, ex);
			stopNow();
		}
	}

	private void beginStop(int fadeTicks, AmbientMusicSystem.PlayMessage next) {
		this.waitingForFile = null;
		this.afterFade = next;
		if (this.currentId.isEmpty() || fadeTicks <= 0) {
			stopNow();
			if (next != null) start(next);
			return;
		}
		this.stopping = true;
		this.goalGain = 0f;
		this.fadeRemaining = fadeTicks;
	}

	private void stopNow() {
		SoundSystem system = getSoundSystem();
		if (system != null) {
			try { system.stop(SOURCE); system.removeSource(SOURCE); }
			catch (Exception ignored) { }
		}
		this.currentId = "";
		this.currentDisplay = "";
		this.currentGain = 0f;
		this.goalGain = 0f;
		this.fadeRemaining = 0;
		this.playAge = 0;
		this.stopping = false;
		this.looping = false;
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || this.currentId.isEmpty()) return;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null || mc.world == null) { stopNow(); return; }
		this.playAge++;
		if (this.fadeRemaining > 0) {
			this.currentGain += (this.goalGain - this.currentGain) / this.fadeRemaining;
			this.fadeRemaining--;
		} else this.currentGain = this.goalGain;
		SoundSystem system = getSoundSystem();
		if (system == null) { stopNow(); return; }
		system.setVolume(SOURCE, effectiveVolume(this.currentGain));
		if ((this.playAge % 100) == 1) mc.getSoundHandler().stop("", SoundCategory.MUSIC);
		if (this.stopping && this.fadeRemaining <= 0) {
			AmbientMusicSystem.PlayMessage next = this.afterFade;
			this.afterFade = null;
			stopNow();
			if (next != null) start(next);
		} else if (!this.looping && this.playAge > 30 && !system.playing(SOURCE)) {
			stopNow();
		}
	}

	private float effectiveVolume(float serverGain) {
		float music = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC);
		return MathHelper.clamp(serverGain * this.localVolume * music, 0f, 1f);
	}

	private SoundSystem getSoundSystem() {
		if (this.soundSystem != null) return this.soundSystem;
		try {
			SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
			SoundManager manager = null;
			for (Field field : SoundHandler.class.getDeclaredFields()) {
				if (SoundManager.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					manager = (SoundManager)field.get(handler);
					break;
				}
			}
			if (manager != null) {
				for (Field field : SoundManager.class.getDeclaredFields()) {
					if (SoundSystem.class.isAssignableFrom(field.getType())) {
						field.setAccessible(true);
						this.soundSystem = (SoundSystem)field.get(manager);
						break;
					}
				}
			}
		} catch (Exception ex) {
			LOGGER.error("Could not access Minecraft SoundSystem", ex);
		}
		return this.soundSystem;
	}

	private AmbientMusicSystem.PlayMessage copy(AmbientMusicSystem.PlayMessage source) {
		return source.stop ? AmbientMusicSystem.PlayMessage.stop(source.fadeTicks)
		 : AmbientMusicSystem.PlayMessage.play(source.id, source.fadeTicks, source.volume, source.loop);
	}

	private void disconnect() {
		stopNow();
		this.afterFade = null;
		this.waitingForFile = null;
		this.downloads.clear();
		this.manifest.clear();
		this.libraryId = "";
		this.cacheDir = null;
	}

	private void loadSettings() {
		Properties properties = new Properties();
		try {
			if (Files.isRegularFile(this.settingsFile)) try (java.io.Reader reader = Files.newBufferedReader(this.settingsFile, StandardCharsets.UTF_8)) { properties.load(reader); }
			this.enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
			this.localVolume = MathHelper.clamp(Float.parseFloat(properties.getProperty("volume", "1.0")), 0f, 1f);
		} catch (Exception ex) {
			LOGGER.warn("Could not read client music settings", ex);
		}
	}

	private void saveSettings() {
		Properties properties = new Properties();
		properties.setProperty("enabled", Boolean.toString(this.enabled));
		properties.setProperty("volume", Float.toString(this.localVolume));
		try {
			Files.createDirectories(this.settingsFile.getParent());
			try (java.io.Writer writer = Files.newBufferedWriter(this.settingsFile, StandardCharsets.UTF_8,
			 StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				properties.store(writer, "Narutomod server music client settings");
			}
		} catch (IOException ex) { LOGGER.warn("Could not save client music settings", ex); }
	}

	private static final class Download {
		private final AmbientMusicSystem.TrackInfo track;
		private final Path part;
		private long received;
		private Download(AmbientMusicSystem.TrackInfo track, Path part) { this.track = track; this.part = part; }
	}

	private static final class DisconnectHook {
		@SubscribeEvent public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
			Minecraft.getMinecraft().addScheduledTask(INSTANCE::disconnect);
		}
	}

	private static final class LocalMusicCommand extends CommandBase {
		@Override public String getName() { return "music"; }
		@Override public String getUsage(ICommandSender sender) { return "/music <volume 0-100|mute|unmute|stop|now|cache>"; }
		@Override public int getRequiredPermissionLevel() { return 0; }
		@Override public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return true; }
		@Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (args.length == 0) throw new CommandException(getUsage(sender));
			String action = args[0].toLowerCase(Locale.ROOT);
			if ("volume".equals(action)) {
				if (args.length < 2) throw new CommandException(getUsage(sender));
				INSTANCE.localVolume = parseInt(args[1], 0, 100) / 100f;
				INSTANCE.saveSettings();
				send(sender, TextFormatting.AQUA + "Server music volume: " + Math.round(INSTANCE.localVolume * 100f) + "%");
			} else if ("mute".equals(action)) {
				INSTANCE.enabled = false; INSTANCE.stopNow(); INSTANCE.saveSettings();
				send(sender, TextFormatting.YELLOW + "Server music muted.");
			} else if ("unmute".equals(action)) {
				INSTANCE.enabled = true; INSTANCE.saveSettings();
				send(sender, TextFormatting.GREEN + "Server music enabled.");
			} else if ("stop".equals(action)) {
				INSTANCE.beginStop(30, null);
				send(sender, TextFormatting.YELLOW + "Stopped the current server track locally.");
			} else if ("now".equals(action)) {
				send(sender, INSTANCE.currentId.isEmpty() ? "No server track is playing." : "Now playing: " + INSTANCE.currentDisplay);
			} else if ("cache".equals(action)) {
				send(sender, "Server music cache: " + (INSTANCE.cacheDir == null ? "not connected" : INSTANCE.cacheDir.toAbsolutePath())
				 + " (" + INSTANCE.downloads.size() + " active download(s))");
			} else throw new CommandException(getUsage(sender));
		}
		private void send(ICommandSender sender, String text) { sender.sendMessage(new TextComponentString(text)); }
		@Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
			if (args.length == 1) return getListOfStringsMatchingLastWord(args, "volume", "mute", "unmute", "stop", "now", "cache");
			return Collections.emptyList();
		}
	}
}
