package net.narutomod.entity;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;

import net.narutomod.Chakra;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.JutsuEffectSounds;
import net.narutomod.JutsuVisualEffects;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.item.ItemJutsu;
import net.narutomod.item.ItemFuton;
import net.narutomod.item.ItemRaiton;
import net.narutomod.procedure.ProcedureCameraShake;
import net.narutomod.procedure.ProcedureRenderView;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;

import javax.annotation.Nullable;

/**
 * A separate, high-commitment Chidori variant inspired by NydoMod's layered
 * Raiton presentation.  Gameplay remains server-side; the client renderer is
 * only the hand-held core and channel envelope.
 */
@ElementsNarutomodMod.ModElement.Tag
public class EntityChidoriRaikiri extends ElementsNarutomodMod.ModElement {
	public static final int ENTITYID = 516;
	public static final double CHAKRA_USAGE = 220d;
	private static final int CHARGE_TICKS = 12;
	private static final int DASH_TICKS = 9;
	/* Nydo's impact preset lives for 26 ticks; keep the entity alive for the
	 * full burst instead of cutting the effect off after the old 8-tick tail. */
	private static final int AFTERIMAGE_TICKS = 26;

	public EntityChidoriRaikiri(ElementsNarutomodMod instance) {
		super(instance, 1032);
	}

	@Override
	public void initElements() {
		elements.entities.add(() -> EntityEntryBuilder.create().entity(EC.class)
			.id(new ResourceLocation("narutomod", "chidori_raikiri"), ENTITYID)
			.name("chidori_raikiri").tracker(96, 1, true).build());
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void preInit(FMLPreInitializationEvent event) {
		RenderingRegistry.registerEntityRenderingHandler(EC.class,
			renderManager -> new RenderRaikiri(renderManager));
	}

	public static class EC extends Entity implements ItemJutsu.IJutsu {
		private static final DataParameter<Integer> OWNER_ID = EntityDataManager.createKey(EC.class, DataSerializers.VARINT);
		private static final DataParameter<Integer> PHASE = EntityDataManager.createKey(EC.class, DataSerializers.VARINT);
		private static final DataParameter<Float> POWER = EntityDataManager.createKey(EC.class, DataSerializers.FLOAT);
		private static final DataParameter<Integer> AGE = EntityDataManager.createKey(EC.class, DataSerializers.VARINT);
		private static final DataParameter<Integer> IMPACT_START = EntityDataManager.createKey(EC.class, DataSerializers.VARINT);
		private static final DataParameter<Float> IMPACT_X = EntityDataManager.createKey(EC.class, DataSerializers.FLOAT);
		private static final DataParameter<Float> IMPACT_Y = EntityDataManager.createKey(EC.class, DataSerializers.FLOAT);
		private static final DataParameter<Float> IMPACT_Z = EntityDataManager.createKey(EC.class, DataSerializers.FLOAT);
		private EntityLivingBase owner;
		private EntityLivingBase target;
		private int dashAge;
		private boolean hasHit;
		private float damage;

		public EC(World world) {
			super(world);
			this.setSize(0.12f, 0.12f);
			this.ignoreFrustumCheck = true;
			this.isImmuneToFire = true;
		}

		protected EC(EntityLivingBase ownerIn, float powerIn) {
			this(ownerIn.world);
			this.owner = ownerIn;
			this.getDataManager().set(OWNER_ID, Integer.valueOf(ownerIn.getEntityId()));
			this.getDataManager().set(POWER, Float.valueOf(powerIn));
			this.damage = computeDamage(ownerIn, powerIn);
			this.setPosition(ownerIn.posX, ownerIn.posY, ownerIn.posZ);
		}

		@Override
		protected void entityInit() {
			this.getDataManager().register(OWNER_ID, Integer.valueOf(0));
			this.getDataManager().register(PHASE, Integer.valueOf(0));
			this.getDataManager().register(POWER, Float.valueOf(1.0f));
			this.getDataManager().register(AGE, Integer.valueOf(0));
			this.getDataManager().register(IMPACT_START, Integer.valueOf(-1));
			this.getDataManager().register(IMPACT_X, Float.valueOf(0.0f));
			this.getDataManager().register(IMPACT_Y, Float.valueOf(0.0f));
			this.getDataManager().register(IMPACT_Z, Float.valueOf(0.0f));
		}

		@Override
		public ItemJutsu.JutsuEnum.Type getJutsuType() {
			return ItemJutsu.JutsuEnum.Type.RAITON;
		}

		@Nullable
		public EntityLivingBase getOwner() {
			if (this.owner != null) {
				return this.owner;
			}
			Entity entity = this.world.getEntityByID(this.getDataManager().get(OWNER_ID).intValue());
			return entity instanceof EntityLivingBase ? (EntityLivingBase)entity : null;
		}

		public int getPhase() {
			return this.getDataManager().get(PHASE).intValue();
		}

		public float getPower() {
			return this.getDataManager().get(POWER).floatValue();
		}

		private static float computeDamage(EntityLivingBase entity, float power) {
			float levelBonus = entity instanceof EntityPlayer
				? (float)Math.min(18.0d, PlayerTracker.getNinjaLevel((EntityPlayer)entity) * 0.30d) : 0.0f;
			return 24.0f + Math.min(18.0f, power * 6.0f) + levelBonus;
		}

		private Vec3d handPosition() {
			EntityLivingBase living = this.getOwner();
			if (living == null) {
				return this.getPositionVector().addVector(0d, 1.0d, 0d);
			}
			Vec3d look = living.getLookVec().normalize();
			return living.getPositionEyes(1.0f).add(look.scale(0.62d)).addVector(0d, -0.28d, 0d);
		}

		@Override
		public void onUpdate() {
			EntityLivingBase living = this.getOwner();
			int age = this.getDataManager().get(AGE).intValue();
			this.getDataManager().set(AGE, Integer.valueOf(age + 1));
			if (living == null || !living.isEntityAlive()) {
				this.setDead();
				return;
			}
			this.owner = living;
			if (!this.world.isRemote) {
				this.tickServer(living, age);
			} else {
				this.setPosition(living.posX, living.posY, living.posZ);
				// Continuous filaments follow the local hand, without spawning network entities.
				if (this.getPhase() == 0 && age % 2 == 0) {
					this.chargeFx(this.handPosition(), age);
				} else if (this.getPhase() == 1) {
					this.dashFx(living);
				}
			}
		}

		private void tickServer(EntityLivingBase living, int age) {
			int phase = this.getPhase();
			if (phase == 0) {
				this.setPosition(living.posX, living.posY, living.posZ);
				if (age == 0) {
					ProcedureSync.EntityNBTTag.setAndSync(living, NarutomodModVariables.forceBowPose, true);
					playSoundAt(living, "narutomod:chidori", 1.25f, 1.0f);
					JutsuEffectSounds.charge(this.world, this.handPosition());
					ProcedureRenderView.setFOV(living, 18, 92f);
				}
				if (age >= CHARGE_TICKS) {
					this.getDataManager().set(PHASE, Integer.valueOf(1));
					this.dashAge = 0;
					RayTraceResult result = ProcedureUtils.objectEntityLookingAt(living, 18d, 2.0d);
					this.target = result != null && result.entityHit instanceof EntityLivingBase
						&& result.entityHit != living ? (EntityLivingBase)result.entityHit : null;
					JutsuEffectSounds.discharge(this.world, this.handPosition());
					JutsuVisualEffects.ring(this.world, living.getPositionVector().addVector(0d, 0.08d, 0d), 1.2f, 8, 0x53AEFF);
				}
			} else if (phase == 1) {
				this.tickDash(living);
			} else {
				this.setPosition(living.posX, living.posY, living.posZ);
				if (age >= CHARGE_TICKS + DASH_TICKS + AFTERIMAGE_TICKS) {
					this.setDead();
				}
			}
		}

		private void tickDash(EntityLivingBase living) {
			this.setPosition(living.posX, living.posY, living.posZ);
			Vec3d direction;
			if (this.target != null && this.target.isEntityAlive()) {
				direction = this.target.getPositionEyes(1.0f).subtract(living.getPositionEyes(1.0f)).normalize();
			} else {
				direction = living.getLookVec().normalize();
			}
			ProcedureUtils.setVelocity(living, direction.x * 1.85d, direction.y * 1.35d, direction.z * 1.85d);
			if (!this.hasHit && this.target != null && this.target.isEntityAlive()
				&& living.getDistance(this.target) <= 3.3d) {
				this.impact(living, this.target);
				return;
			}
			if (++this.dashAge >= DASH_TICKS) {
				this.impact(living, null);
			}
		}

		private void dashFx(EntityLivingBase living) {
			Vec3d hand = this.handPosition();
			Vec3d direction = new Vec3d(living.motionX, living.motionY, living.motionZ);
			direction = direction.lengthSquared() > 0.01d ? direction.normalize() : living.getLookVec();
			JutsuVisualEffects.bolt(this.world, hand, hand.add(direction.scale(1.25d)), 0.070f, 3, 0x87DFFF);
			JutsuVisualEffects.bolt(this.world, hand.subtract(direction.scale(1.8d)), hand, 0.036f, 4, 0x368CFF);
			if ((this.getDataManager().get(AGE).intValue() & 3) == 0) {
				JutsuVisualEffects.burst(this.world, hand, 0.28f, 0xB5EEFF);
			}
		}

		private void chargeFx(Vec3d hand, int age) {
			Vec3d look = this.getOwner() != null ? this.getOwner().getLookVec().normalize() : new Vec3d(0d, 0d, 1d);
			float buildup = Math.min(1f, age / (float)CHARGE_TICKS);
			for (int i = 0; i < 2; i++) {
				double angle = age * 0.83d + i * Math.PI;
				Vec3d end = hand.add(look.scale(0.25d + buildup * 0.35d)).addVector(
					Math.cos(angle) * (0.3d + buildup * 0.35d), Math.sin(angle) * 0.45d,
					Math.sin(angle + 0.9d) * 0.4d);
				JutsuVisualEffects.bolt(this.world, end, hand, 0.024f + buildup * 0.025f, 3, 0x61CFFF);
			}
			if (age % 4 == 0) {
				JutsuVisualEffects.burst(this.world, hand, 0.18f + buildup * 0.16f, 0xBDEFFF);
			}
		}

		private void impact(EntityLivingBase living, @Nullable EntityLivingBase victim) {
			this.hasHit = true;
			Vec3d center = victim != null ? victim.getPositionEyes(1.0f)
				: living.getPositionEyes(1.0f).add(living.getLookVec().scale(1.4d));
			this.getDataManager().set(IMPACT_START, Integer.valueOf(this.getDataManager().get(AGE).intValue()));
			this.getDataManager().set(IMPACT_X, Float.valueOf((float)center.x));
			this.getDataManager().set(IMPACT_Y, Float.valueOf((float)center.y));
			this.getDataManager().set(IMPACT_Z, Float.valueOf((float)center.z));
			if (victim != null) {
				victim.hurtResistantTime = 0;
				EntityLightningArc.onStruck(victim, ItemJutsu.causeJutsuDamage(this, living), this.damage, 75, false);
				ProcedureUtils.pushEntity(living, victim, 0.45d, 0.18f);
			}
			JutsuVisualEffects.burst(this.world, center, victim != null ? 1.4f : 0.8f, 0x9DDDFF);
			JutsuVisualEffects.ring(this.world, center.addVector(0d, -0.6d, 0d), victim != null ? 2.8f : 1.4f, 12, 0x449CFF);
			for (int i = 0; i < 6; i++) {
				double angle = Math.PI * 2d * i / 6d;
				double radius = 1.0d + this.rand.nextDouble() * (victim != null ? 1.9d : 0.8d);
				Vec3d end = center.addVector(Math.cos(angle) * radius,
					(this.rand.nextDouble() - 0.5d) * 1.8d,
					Math.sin(angle) * radius);
				JutsuVisualEffects.bolt(this.world, center, end, 0.055f, 8, 0x68B8FF);
			}
			ProcedureCameraShake.sendToClients(this.world.provider.getDimension(), center.x, center.y, center.z, 24d, 9, victim != null ? 2.2f : 0.9f);
			if (victim != null) JutsuEffectSounds.pillarImpact(this.world, center);
			else JutsuEffectSounds.chainSnap(this.world, center);
			this.getDataManager().set(PHASE, Integer.valueOf(2));
			this.dashAge = 0;
			living.motionX *= 0.25d;
			living.motionY *= 0.25d;
			living.motionZ *= 0.25d;
		}

		private void playSoundAt(EntityLivingBase entity, String id, float volume, float pitch) {
			playSoundAt(entity.getPositionVector(), id, volume, pitch);
		}

		private void playSoundAt(Vec3d position, String id, float volume, float pitch) {
			SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(id));
			if (sound != null) {
				this.world.playSound(null, position.x, position.y, position.z, sound, SoundCategory.PLAYERS, volume, pitch);
			}
		}

		@Override
		public void setDead() {
			if (!this.world.isRemote && this.owner != null) {
				ProcedureSync.EntityNBTTag.removeAndSync(this.owner, NarutomodModVariables.forceBowPose);
				ItemJutsu.IJutsuCallback.JutsuData data = ItemRaiton.CHIDORI_RAIKIRI.jutsu.getData(this.owner);
				if (data != null && data.entity == this) {
					data.stack.getTagCompound().removeTag(Jutsu.ID_KEY);
				}
			}
			super.setDead();
		}

		@Override
		public boolean canBeCollidedWith() {
			return false;
		}

		@Override
		protected void readEntityFromNBT(NBTTagCompound compound) {
		}

		@Override
		protected void writeEntityToNBT(NBTTagCompound compound) {
		}

		public static class Jutsu implements ItemJutsu.IJutsuCallback {
			private static final String ID_KEY = "ChidoriRaikiriEntityIdKey";

			@Override
			public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
				if (entity.world.isRemote || entity.isRiding()) {
					return false;
				}
				if (this.isActivated(entity)) {
					return false;
				}
				if (ItemFuton.CHAKRAFLOW.jutsu.isActivated(entity)) {
					ItemFuton.CHAKRAFLOW.jutsu.deactivate(entity);
				}
				EC raikiri = new EC(entity, Math.max(1.0f, Math.min(3.0f, power)));
				entity.world.spawnEntity(raikiri);
				if (!stack.hasTagCompound()) {
					stack.setTagCompound(new NBTTagCompound());
				}
				stack.getTagCompound().setInteger(ID_KEY, raikiri.getEntityId());
				return true;
			}

			@Override
			public float getBasePower() {
				return 1.0f;
			}

			@Override
			public float getPowerupDelay() {
				return 42.0f;
			}

			@Override
			public float getMaxPower() {
				return 3.0f;
			}

			@Override
			public boolean isActivated(ItemStack stack) {
				return stack.hasTagCompound() && stack.getTagCompound().hasKey(ID_KEY);
			}

			@Override
			public boolean isActivated(EntityLivingBase entity) {
				return this.getData(entity) != null;
			}

			@Override
			public void deactivate(EntityLivingBase entity) {
				ItemJutsu.IJutsuCallback.JutsuData data = this.getData(entity);
				if (data != null) {
					data.entity.setDead();
					data.stack.getTagCompound().removeTag(ID_KEY);
				}
			}

			@Override
			@Nullable
			public ItemJutsu.IJutsuCallback.JutsuData getData(EntityLivingBase entity) {
				if (entity instanceof EntityPlayer) {
					ItemStack stack = ProcedureUtils.getMatchingItemStack((EntityPlayer)entity, ItemRaiton.block);
					if (stack != null && stack.hasTagCompound() && stack.getTagCompound().hasKey(ID_KEY)) {
						Entity found = entity.world.getEntityByID(stack.getTagCompound().getInteger(ID_KEY));
						return found instanceof EC ? new ItemJutsu.IJutsuCallback.JutsuData(found, stack) : null;
					}
				}
				return null;
			}
		}
	}

	@SideOnly(Side.CLIENT)
	public static class RenderRaikiri extends Render<EC> {
		public RenderRaikiri(RenderManager manager) {
			super(manager);
			this.shadowSize = 0.0f;
		}

		@Override
		public boolean shouldRender(EC entity, net.minecraft.client.renderer.culling.ICamera camera,
				double camX, double camY, double camZ) {
			return true;
		}

		@Override
		public void doRender(EC entity, double x, double y, double z, float yaw, float partialTicks) {
			// The shared effect renderer draws the local hand filaments and synchronized impact.
		}

		@Override
		protected ResourceLocation getEntityTexture(EC entity) {
			return null;
		}
	}
}
