
package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.world.World;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;

import net.narutomod.NarutomodMod;
import net.narutomod.entity.EntityMindTransfer;
import net.narutomod.entity.EntityShadowImitation;
import net.narutomod.entity.EntityTailedBeast;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.potion.PotionParalysis;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.EntityNinjaMob;

import io.netty.buffer.ByteBuf;

@ElementsNarutomodMod.ModElement.Tag
public class ItemInton extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:inton")
	public static final Item block = null;
	public static final int ENTITYID = 172;
	private static final String GENJUTSU_ROOT = "NarutomodActiveGenjutsu";
	public static final ItemJutsu.JutsuEnum GENJUTSU = new ItemJutsu.JutsuEnum(0, "genjutsu", 'B', 300d, new Genjutsu());
	public static final ItemJutsu.JutsuEnum MBTRANSFER = new ItemJutsu.JutsuEnum(1, "mind_transfer", 'C', 300d, new EntityMindTransfer.EC.Jutsu());
	public static final ItemJutsu.JutsuEnum SHADOW_IMITATION = new ItemJutsu.JutsuEnum(2, "shadow_imitation", 'B', 50d, new EntityShadowImitation.EC.Jutsu());
	public static final ItemJutsu.JutsuEnum FALSE_OPENING = new ItemJutsu.JutsuEnum(3, "false_opening", 'C', 80d, new FalseOpening()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum MEMORY_FRACTURE = new ItemJutsu.JutsuEnum(4, "memory_fracture", 'B', 120d, new SharinganGenjutsu(1)).withCustomBalance();
	public static final ItemJutsu.JutsuEnum MURDER_INTENT = new ItemJutsu.JutsuEnum(5, "murder_intent", 'B', 130d, new SharinganGenjutsu(2)).withCustomBalance();
	public static final ItemJutsu.JutsuEnum ILLUSIONARY_EXECUTION = new ItemJutsu.JutsuEnum(6, "illusionary_execution", 'A', 180d, new SharinganGenjutsu(3)).withCustomBalance();
	public static final ItemJutsu.JutsuEnum BURNING_COFFIN = new ItemJutsu.JutsuEnum(7, "burning_coffin", 'A', 170d, new SharinganGenjutsu(4)).withCustomBalance();

	public ItemInton(ElementsNarutomodMod instance) {
		super(instance, 441);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new RangedItem(GENJUTSU, MBTRANSFER, SHADOW_IMITATION, FALSE_OPENING, MEMORY_FRACTURE, MURDER_INTENT, ILLUSIONARY_EXECUTION, BURNING_COFFIN));
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		elements.addNetworkMessage(ClientGenjutsuMessage.Handler.class, ClientGenjutsuMessage.class, Side.CLIENT);
		elements.addNetworkMessage(GenjutsuCastMessage.Handler.class, GenjutsuCastMessage.class, Side.CLIENT);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new net.narutomod.client.ClientGenjutsuOverlay());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(block, 0, new ModelResourceLocation("narutomod:inton", "inventory"));
	}

	public static class RangedItem extends ItemJutsu.Base {
		public RangedItem(ItemJutsu.JutsuEnum... list) {
			super(ItemJutsu.JutsuEnum.Type.INTON, list);
			this.setUnlocalizedName("inton");
			this.setRegistryName("inton");
			this.setCreativeTab(TabModTab.tab);
		}
	}

	public static class Genjutsu implements ItemJutsu.IJutsuCallback {
		private final double maxRange;
		private final int duration;
		private final int cooldown = 1200;

		public Genjutsu() {
			this(16.0d, 200);
		}

		public Genjutsu(double range, int durationIn) {
			this.maxRange = range;
			this.duration = durationIn;
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			Entity target = ProcedureUtils.objectEntityLookingAt(entity, this.maxRange).entityHit;
			if (target instanceof EntityLivingBase && this.createJutsu(entity, (EntityLivingBase)target, this.duration)) {				
				if (stack != null && entity instanceof EntityPlayer) {
					ItemJutsu.setCurrentJutsuCooldown(stack, (EntityPlayer)entity, this.cooldown);
				}
				return true;
			}
			return false;
		}

		public static boolean createJutsu(EntityLivingBase entity, EntityLivingBase target, int durationIn) {
			if (canTargetBeAffected(entity, target)) {
				entity.world.playSound(null, target.posX, target.posY, target.posZ,
				  SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:genjutsu")), SoundCategory.NEUTRAL, 1f, 1f);
				target.addPotionEffect(new PotionEffect(PotionParalysis.potion, durationIn, 1, false, false));
				target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, durationIn + 40, 0, false, true));
				target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, durationIn, 0, false, true));
				markGenjutsu(entity, target, durationIn, 0);
				if (target instanceof EntityPlayerMP) {
					ProcedureSync.MobAppearanceParticle.send((EntityPlayerMP)target, entity.getEntityId());
				}
				target.setRevengeTarget(entity);
				return true;
			}
			return false;			
		}

		public static boolean canTargetBeAffected(EntityLivingBase caster, EntityLivingBase target) {
			if (target instanceof EntityTailedBeast.Base && !ItemSharingan.wearingAny(caster)) {
				return false;
			} else {
				ItemStack stack = ProcedureUtils.getMatchingItemStack(target, ItemNinjutsu.block);
				if (stack != null && ItemNinjutsu.isJutsuEnabled(stack, ItemNinjutsu.BUGSWARM)) {
					return false;
				}
			}
			return true;
		}
	}

	private static EntityLivingBase getTarget(EntityLivingBase entity, double range, double grow) {
		RayTraceResult hit = ProcedureUtils.objectEntityLookingAt(entity, range, grow);
		return hit != null && hit.entityHit instanceof EntityLivingBase && !hit.entityHit.equals(entity) ? (EntityLivingBase)hit.entityHit : null;
	}

	private static float mastery(ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
		if (!(stack.getItem() instanceof ItemJutsu.Base)) return 0f;
		ItemJutsu.Base item = (ItemJutsu.Base)stack.getItem();
		int required = Math.max(1, item.getRequiredXp(stack, jutsu));
		return net.minecraft.util.math.MathHelper.clamp(((float)item.getJutsuXp(stack, jutsu) - required) / (required * 2.0f), 0f, 1f);
	}

	public static class FalseOpening implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			EntityLivingBase target = getTarget(entity, 18d, 2.0d);
			if (target == null || !Genjutsu.canTargetBeAffected(entity, target)) return false;
			float mastery = mastery(stack, FALSE_OPENING);
			int duration = 80 + (int)(mastery * 80f);
			markGenjutsu(entity, target, duration, 0);
			entity.world.playSound(null, target.posX, target.posY, target.posZ,
			 SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:genjutsu")), SoundCategory.PLAYERS, 1.0f, 1.15f);
			net.narutomod.GenjutsuSession.begin(entity, target, 0, duration);
			CustomJutsuEffects.onGenjutsu(entity, target, 0, mastery);
			return true;
		}
	}

	public static class SharinganGenjutsu implements ItemJutsu.IJutsuCallback {
		private final int type;
		SharinganGenjutsu(int typeIn) {
			this.type = typeIn;
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!ItemSharingan.wearingAny(entity)) return false;
			EntityLivingBase target = getTarget(entity, 22d, 2.5d);
			if (target == null || !Genjutsu.canTargetBeAffected(entity, target)) return false;
			float mastery = mastery(stack, this.jutsu());
			int duration = net.narutomod.GenjutsuSession.boundedDuration(this.type, 100 + (int)(mastery * 100f));
			// The affected player hears one private cue at their screen; nearby observers hear the caster.
			entity.world.playSound(target instanceof EntityPlayer ? (EntityPlayer)target : null,
			 entity.posX, entity.posY, entity.posZ,
			 SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:sharingansfx")), SoundCategory.PLAYERS, 0.85f, 1.0f);
			if (this.type == 1) {
				if (target instanceof EntityPlayer) {
					((EntityPlayer)target).sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("message.narutomod.memory_fracture"), true);
				}
			} else if (this.type == 2) {
				if (entity.getRNG().nextFloat() < 0.45f + mastery * 0.35f) {
					target.resetActiveHand();
				}
			} else if (this.type == 3) {
				target.attackEntityFrom(DamageSource.MAGIC, 7f + mastery * 7f);
			} else {
				// Illusory flames must not ignite the real body or terrain.
				target.attackEntityFrom(DamageSource.MAGIC, 3f + mastery * 3f);
				Chakra.pathway(target).consume(10d + mastery * 25d);
			}
			CustomJutsuEffects.onGenjutsu(entity, target, this.type, mastery);
			markGenjutsu(entity, target, duration, this.type);
			net.narutomod.GenjutsuSession.begin(entity, target, this.type, duration);
			target.setRevengeTarget(entity);
			return true;
		}

		private ItemJutsu.JutsuEnum jutsu() {
			return this.type == 1 ? MEMORY_FRACTURE : this.type == 2 ? MURDER_INTENT
			 : this.type == 3 ? ILLUSIONARY_EXECUTION : BURNING_COFFIN;
		}
	}

	private static double genjutsuStrength(EntityLivingBase caster) {
		if (caster instanceof EntityPlayer) {
			return PlayerTracker.getBattleXp((EntityPlayer)caster);
		}
		if (caster instanceof EntityNinjaMob.Base) {
			double level = ((EntityNinjaMob.Base)caster).getNinjaLevel();
			return level * level;
		}
		return 2500d;
	}

	private static void markGenjutsu(EntityLivingBase caster, EntityLivingBase target, int duration, int type) {
		if (target == null || target.world.isRemote) return;
		NBTTagCompound tag = target.getEntityData().getCompoundTag(GENJUTSU_ROOT);
		tag.setDouble("strength", genjutsuStrength(caster));
		tag.setInteger("type", type);
		tag.setLong("expires", target.world.getTotalWorldTime() + Math.max(20, duration));
		tag.setString("caster", caster == null ? "Unknown" : caster.getName());
		target.getEntityData().setTag(GENJUTSU_ROOT, tag);
	}

	public static boolean chakraPulseBreak(EntityPlayer player) {
		if (player == null || player.world.isRemote) return false;
		NBTTagCompound tag = player.getEntityData().getCompoundTag(GENJUTSU_ROOT);
		boolean activeTag = tag.hasKey("strength") && player.world.getTotalWorldTime() <= tag.getLong("expires") + 20;
		boolean hasEffects = player.isPotionActive(PotionParalysis.potion) || player.isPotionActive(MobEffects.NAUSEA)
		 || player.isPotionActive(MobEffects.BLINDNESS) || player.isPotionActive(MobEffects.WEAKNESS)
		 || player.isPotionActive(MobEffects.SLOWNESS) || player.isPotionActive(MobEffects.MINING_FATIGUE);
		if (!activeTag && !hasEffects) {
			player.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "No active genjutsu to break."), true);
			return false;
		}
		double defenderXp = PlayerTracker.getBattleXp(player);
		double casterXp = activeTag ? Math.max(0d, tag.getDouble("strength")) : 1000d;
		if (defenderXp < casterXp * 1.10d && defenderXp < casterXp + 500d) {
			player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_PURPLE + "Your Chakra Pulse failed to overpower the genjutsu."), true);
			return false;
		}
		if (net.narutomod.GenjutsuSession.active(player)) {
			net.narutomod.GenjutsuSession.clear(player);
		} else {
		player.removePotionEffect(PotionParalysis.potion);
		player.removePotionEffect(MobEffects.NAUSEA);
		player.removePotionEffect(MobEffects.BLINDNESS);
		player.removePotionEffect(MobEffects.WEAKNESS);
		player.removePotionEffect(MobEffects.SLOWNESS);
		player.removePotionEffect(MobEffects.MINING_FATIGUE);
		player.extinguish();
		}
		player.getEntityData().removeTag(GENJUTSU_ROOT);
		if (player instanceof EntityPlayerMP) {
			ClientGenjutsuMessage.clear((EntityPlayerMP)player);
		}
		player.world.playSound(null, player.posX, player.posY, player.posZ,
		 SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:chakraflow")), SoundCategory.PLAYERS, 0.9f, 1.25f);
		player.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + "Chakra Pulse broke the genjutsu."), true);
		return true;
	}

	public static class GenjutsuCastMessage implements IMessage {
		private int caster, type;
		public GenjutsuCastMessage() { }
		private GenjutsuCastMessage(int casterIn, int typeIn) { caster = casterIn; type = typeIn; }
		public static void send(EntityLivingBase caster, int type) {
			NarutomodMod.PACKET_HANDLER.sendToAllAround(new GenjutsuCastMessage(caster.getEntityId(), type),
			 new net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint(caster.dimension, caster.posX, caster.posY, caster.posZ, 48));
		}
		@Override public void toBytes(ByteBuf buf) { buf.writeInt(caster); buf.writeInt(type); }
		@Override public void fromBytes(ByteBuf buf) { caster = buf.readInt(); type = buf.readInt(); }
		public static class Handler implements IMessageHandler<GenjutsuCastMessage, IMessage> {
			@Override @SideOnly(Side.CLIENT)
			public IMessage onMessage(GenjutsuCastMessage msg, MessageContext ctx) {
				net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() ->
				 net.narutomod.client.ClientGenjutsuOverlay.cast(msg.caster, msg.type));
				return null;
			}
		}
	}

	public static class ClientGenjutsuMessage implements IMessage {
		private int type;
		private int ticks;
		private int caster = -1;
		public ClientGenjutsuMessage() { }
		public ClientGenjutsuMessage(int typeIn, int ticksIn) {
			this.type = typeIn;
			this.ticks = ticksIn;
		}
		public static void send(EntityPlayerMP player, int typeIn, int ticksIn) {
			NarutomodMod.PACKET_HANDLER.sendTo(new ClientGenjutsuMessage(typeIn, ticksIn), player);
		}
		public static void send(EntityPlayerMP player, int typeIn, int ticksIn, int casterIn) {
			ClientGenjutsuMessage message = new ClientGenjutsuMessage(typeIn, ticksIn);
			message.caster = casterIn;
			NarutomodMod.PACKET_HANDLER.sendTo(message, player);
		}
		public static void clear(EntityPlayerMP player) {
			NarutomodMod.PACKET_HANDLER.sendTo(new ClientGenjutsuMessage(-1, 0), player);
		}
		public void toBytes(ByteBuf buf) {
			buf.writeInt(this.type);
			buf.writeInt(this.ticks);
			buf.writeInt(this.caster);
		}
		public void fromBytes(ByteBuf buf) {
			this.type = buf.readInt();
			this.ticks = buf.readInt();
			this.caster = buf.readInt();
		}
		public static class Handler implements IMessageHandler<ClientGenjutsuMessage, IMessage> {
			@SideOnly(Side.CLIENT)
			@Override
			public IMessage onMessage(ClientGenjutsuMessage message, MessageContext context) {
				net.narutomod.client.ClientGenjutsuOverlay.handleMessage(message.type, message.ticks, message.caster);
				return null;
			}
		}
	}
}
