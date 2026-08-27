package net.narutomod;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;

/** Persistent, server-authoritative fitting values for Sharingan eye layers. */
@ElementsNarutomodMod.ModElement.Tag
public class EyeCustomization extends ElementsNarutomodMod.ModElement {
	private static final String HORIZONTAL = "NarutomodEyeHorizontal";
	private static final String VERTICAL = "NarutomodEyeVertical";
	private static final String DEPTH = "NarutomodEyeDepth";
	private static final String WIDTH = "NarutomodEyeWidth";
	private static final String HEIGHT = "NarutomodEyeHeight";

	public static final float MIN_HORIZONTAL = -2.0F;
	public static final float MAX_HORIZONTAL = 2.0F;
	public static final float MIN_VERTICAL = -2.0F;
	public static final float MAX_VERTICAL = 2.0F;
	public static final float MIN_DEPTH = -1.0F;
	public static final float MAX_DEPTH = 1.0F;
	public static final float MIN_SCALE = 0.70F;
	public static final float MAX_SCALE = 1.30F;

	public EyeCustomization(ElementsNarutomodMod instance) {
		super(instance, 1026);
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		elements.addNetworkMessage(SaveMessage.Handler.class, SaveMessage.class, Side.SERVER);
		elements.addNetworkMessage(SyncMessage.Handler.class, SyncMessage.class, Side.CLIENT);
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		if (event.getSide().isClient()) initializeClient();
	}

	@SideOnly(Side.CLIENT)
	private void initializeClient() {
		net.narutomod.client.GuiEyeCustomization.initialize();
	}

	public static float getHorizontal(Entity entity) {
		return get(entity, HORIZONTAL, 0.0F);
	}

	public static float getVertical(Entity entity) {
		return get(entity, VERTICAL, 0.0F);
	}

	public static float getDepth(Entity entity) {
		return get(entity, DEPTH, 0.0F);
	}

	public static float getWidth(Entity entity) {
		return get(entity, WIDTH, 1.0F);
	}

	public static float getHeight(Entity entity) {
		return get(entity, HEIGHT, 1.0F);
	}

	private static float get(Entity entity, String key, float defaultValue) {
		return entity != null && entity.getEntityData().hasKey(key) ? entity.getEntityData().getFloat(key) : defaultValue;
	}

	/** Used by the live editor and by client sync packets. */
	public static void setClientValues(Entity entity, float horizontal, float vertical, float depth, float width, float height) {
		if (entity == null) return;
		setRuntimeValues(entity, sanitizeHorizontal(horizontal), sanitizeVertical(vertical), sanitizeDepth(depth),
			sanitizeScale(width), sanitizeScale(height));
	}

	private static void setRuntimeValues(Entity entity, float horizontal, float vertical, float depth, float width, float height) {
		NBTTagCompound data = entity.getEntityData();
		data.setFloat(HORIZONTAL, horizontal);
		data.setFloat(VERTICAL, vertical);
		data.setFloat(DEPTH, depth);
		data.setFloat(WIDTH, width);
		data.setFloat(HEIGHT, height);
	}

	private static void applyServerValues(EntityPlayer player, float horizontal, float vertical, float depth, float width, float height) {
		horizontal = sanitizeHorizontal(horizontal);
		vertical = sanitizeVertical(vertical);
		depth = sanitizeDepth(depth);
		width = sanitizeScale(width);
		height = sanitizeScale(height);
		setRuntimeValues(player, horizontal, vertical, depth, width, height);

		NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		persisted.setFloat(HORIZONTAL, horizontal);
		persisted.setFloat(VERTICAL, vertical);
		persisted.setFloat(DEPTH, depth);
		persisted.setFloat(WIDTH, width);
		persisted.setFloat(HEIGHT, height);
		player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
	}

	private static void loadPersisted(EntityPlayer player) {
		NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		if (persisted.hasKey(HORIZONTAL) || persisted.hasKey(VERTICAL) || persisted.hasKey(DEPTH)
		 || persisted.hasKey(WIDTH) || persisted.hasKey(HEIGHT)) {
			applyServerValues(player,
				persisted.hasKey(HORIZONTAL) ? persisted.getFloat(HORIZONTAL) : 0.0F,
				persisted.hasKey(VERTICAL) ? persisted.getFloat(VERTICAL) : 0.0F,
				persisted.hasKey(DEPTH) ? persisted.getFloat(DEPTH) : 0.0F,
				persisted.hasKey(WIDTH) ? persisted.getFloat(WIDTH) : 1.0F,
				persisted.hasKey(HEIGHT) ? persisted.getFloat(HEIGHT) : 1.0F);
		} else {
			applyServerValues(player, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
		}
	}

	private static float sanitizeHorizontal(float value) {
		return sanitize(value, MIN_HORIZONTAL, MAX_HORIZONTAL, 0.0F);
	}

	private static float sanitizeVertical(float value) {
		return sanitize(value, MIN_VERTICAL, MAX_VERTICAL, 0.0F);
	}

	private static float sanitizeDepth(float value) {
		return sanitize(value, MIN_DEPTH, MAX_DEPTH, 0.0F);
	}

	private static float sanitizeScale(float value) {
		return sanitize(value, MIN_SCALE, MAX_SCALE, 1.0F);
	}

	private static float sanitize(float value, float min, float max, float fallback) {
		return Float.isNaN(value) || Float.isInfinite(value) ? fallback : MathHelper.clamp(value, min, max);
	}

	private static void syncTo(EntityPlayerMP receiver, EntityPlayer owner) {
		NarutomodMod.PACKET_HANDLER.sendTo(new SyncMessage(owner), receiver);
	}

	private static void syncToTrackingAndSelf(EntityPlayerMP owner) {
		NarutomodMod.PACKET_HANDLER.sendToAllTracking(new SyncMessage(owner), owner);
		syncTo(owner, owner);
	}

	@SubscribeEvent
	public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			loadPersisted(event.player);
			syncTo((EntityPlayerMP)event.player, event.player);
		}
	}

	@SubscribeEvent
	public void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
		EntityPlayer original = event.getOriginal();
		applyServerValues(event.getEntityPlayer(), getHorizontal(original), getVertical(original), getDepth(original),
			getWidth(original), getHeight(original));
	}

	@SubscribeEvent
	public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.player instanceof EntityPlayerMP) syncToTrackingAndSelf((EntityPlayerMP)event.player);
	}

	@SubscribeEvent
	public void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
		if (event.getEntityPlayer() instanceof EntityPlayerMP && event.getTarget() instanceof EntityPlayer) {
			syncTo((EntityPlayerMP)event.getEntityPlayer(), (EntityPlayer)event.getTarget());
		}
	}

	public static class SaveMessage implements IMessage {
		private float horizontal;
		private float vertical;
		private float depth;
		private float width;
		private float height;

		public SaveMessage() {
		}

		public SaveMessage(float horizontal, float vertical, float depth, float width, float height) {
			this.horizontal = horizontal;
			this.vertical = vertical;
			this.depth = depth;
			this.width = width;
			this.height = height;
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeFloat(this.horizontal);
			buf.writeFloat(this.vertical);
			buf.writeFloat(this.depth);
			buf.writeFloat(this.width);
			buf.writeFloat(this.height);
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			this.horizontal = buf.readFloat();
			this.vertical = buf.readFloat();
			this.depth = buf.readFloat();
			this.width = buf.readFloat();
			this.height = buf.readFloat();
		}

		public static class Handler implements IMessageHandler<SaveMessage, IMessage> {
			@Override
			public IMessage onMessage(SaveMessage message, MessageContext context) {
				EntityPlayerMP player = context.getServerHandler().player;
				player.getServerWorld().addScheduledTask(() -> {
					applyServerValues(player, message.horizontal, message.vertical, message.depth, message.width, message.height);
					syncToTrackingAndSelf(player);
				});
				return null;
			}
		}
	}

	public static class SyncMessage implements IMessage {
		private int entityId;
		private float horizontal;
		private float vertical;
		private float depth;
		private float width;
		private float height;

		public SyncMessage() {
		}

		public SyncMessage(EntityPlayer player) {
			this.entityId = player.getEntityId();
			this.horizontal = getHorizontal(player);
			this.vertical = getVertical(player);
			this.depth = getDepth(player);
			this.width = getWidth(player);
			this.height = getHeight(player);
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeInt(this.entityId);
			buf.writeFloat(this.horizontal);
			buf.writeFloat(this.vertical);
			buf.writeFloat(this.depth);
			buf.writeFloat(this.width);
			buf.writeFloat(this.height);
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			this.entityId = buf.readInt();
			this.horizontal = buf.readFloat();
			this.vertical = buf.readFloat();
			this.depth = buf.readFloat();
			this.width = buf.readFloat();
			this.height = buf.readFloat();
		}

		public static class Handler implements IMessageHandler<SyncMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override
			public IMessage onMessage(SyncMessage message, MessageContext context) {
				net.narutomod.client.GuiEyeCustomization.handleSync(message.entityId, message.horizontal,
					message.vertical, message.depth, message.width, message.height);
				return null;
			}
		}
	}
}
