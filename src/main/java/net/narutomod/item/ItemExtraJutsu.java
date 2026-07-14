package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.texture.TextureMap;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.Particles;
import net.narutomod.block.BlockWaterStill;
import net.narutomod.entity.EntityClone;
import net.narutomod.entity.EntityCrow;
import net.narutomod.entity.EntityExplosiveClone;
import net.narutomod.entity.EntityNinjaMob;
import net.narutomod.event.EventSetBlocks;
import net.narutomod.potion.PotionHeaviness;
import net.narutomod.potion.PotionParalysis;
import net.narutomod.procedure.ProcedureAoeCommand;
import net.narutomod.procedure.ProcedureUtils;

import java.util.HashMap;
import java.util.Map;

@ElementsNarutomodMod.ModElement.Tag
public class ItemExtraJutsu extends ElementsNarutomodMod.ModElement {
	private static final int WATER_CLONE_ID = 9310;
	private static final int LIGHTNING_CLONE_ID = 9311;
	private static final int GUIDED_SHURIKEN_ID = 9312;
	private static final int GUIDED_SENBON_ID = 9313;
	private static final int CROW_TRAP_CLONE_ID = 9314;
	private static final int FIRE_DRAGON_HEAD_ID = 9315;
	private static final int WATER_PRISON_TRAP_CLONE_ID = 9316;
	private static final int WATER_PRISON_TRAP_ID = 9317;

	public ItemExtraJutsu(ElementsNarutomodMod instance) {
		super(instance, 1009);
	}

	@Override
	public void initElements() {
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityWaterClone.class)
		 .id(new ResourceLocation("narutomod", "water_clone"), WATER_CLONE_ID).name("water_clone").tracker(64, 3, true).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityLightningClone.class)
		 .id(new ResourceLocation("narutomod", "lightning_clone"), LIGHTNING_CLONE_ID).name("lightning_clone").tracker(64, 3, true).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityGuidedShuriken.class)
		 .id(new ResourceLocation("narutomod", "guided_shadow_shuriken"), GUIDED_SHURIKEN_ID).name("guided_shadow_shuriken").tracker(64, 2, true).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityGuidedChidoriSenbon.class)
		 .id(new ResourceLocation("narutomod", "guided_chidori_senbon"), GUIDED_SENBON_ID).name("guided_chidori_senbon").tracker(64, 2, true).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityCrowTrapClone.class)
		 .id(new ResourceLocation("narutomod", "crow_trap_clone"), CROW_TRAP_CLONE_ID).name("crow_trap_clone").tracker(64, 3, true).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityFireDragonHead.class)
		 .id(new ResourceLocation("narutomod", "fire_dragon_head"), FIRE_DRAGON_HEAD_ID).name("fire_dragon_head").tracker(64, 3, true).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityWaterPrisonTrapClone.class)
		 .id(new ResourceLocation("narutomod", "water_prison_trap_clone"), WATER_PRISON_TRAP_CLONE_ID).name("water_prison_trap_clone").tracker(64, 3, true).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityWaterPrisonTrap.class)
		 .id(new ResourceLocation("narutomod", "water_prison_trap"), WATER_PRISON_TRAP_ID).name("water_prison_trap").tracker(64, 3, true).build());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void preInit(FMLPreInitializationEvent event) {
		RenderingRegistry.registerEntityRenderingHandler(EntityWaterClone.class,
		 renderManager -> EntityClone.ClientRLM.getInstance().new RenderClone(renderManager));
		RenderingRegistry.registerEntityRenderingHandler(EntityLightningClone.class,
		 renderManager -> EntityClone.ClientRLM.getInstance().new RenderClone(renderManager));
		RenderingRegistry.registerEntityRenderingHandler(EntityGuidedShuriken.class,
		 renderManager -> new RenderSnowball<EntityGuidedShuriken>(renderManager, ItemShuriken.block, Minecraft.getMinecraft().getRenderItem()));
		RenderingRegistry.registerEntityRenderingHandler(EntityGuidedChidoriSenbon.class,
		 renderManager -> new RenderChidoriSenbon(renderManager));
		RenderingRegistry.registerEntityRenderingHandler(EntityCrowTrapClone.class,
		 renderManager -> EntityClone.ClientRLM.getInstance().new RenderClone(renderManager));
		RenderingRegistry.registerEntityRenderingHandler(EntityFireDragonHead.class,
		 renderManager -> new RenderFireDragonHead(renderManager));
		RenderingRegistry.registerEntityRenderingHandler(EntityWaterPrisonTrapClone.class,
		 renderManager -> EntityClone.ClientRLM.getInstance().new RenderClone(renderManager));
	}

	private static EntityLivingBase getLookTarget(EntityLivingBase entity, double range, double grow) {
		RayTraceResult hit = ProcedureUtils.objectEntityLookingAt(entity, range, grow);
		return hit != null && hit.entityHit instanceof EntityLivingBase && !hit.entityHit.equals(entity)
		 ? (EntityLivingBase)hit.entityHit : null;
	}

	private static Vec3d guidedDirection(Entity projectile, EntityLivingBase target, double turnStrength) {
		if (target == null || !target.isEntityAlive()) {
			return new Vec3d(projectile.motionX, projectile.motionY, projectile.motionZ).normalize();
		}
		Vec3d current = new Vec3d(projectile.motionX, projectile.motionY, projectile.motionZ).normalize();
		Vec3d wanted = target.getPositionEyes(1f).subtract(projectile.getPositionVector()).normalize();
		return current.scale(1.0d - turnStrength).add(wanted.scale(turnStrength)).normalize();
	}

	private static float masteryRatio(ItemStack stack, ItemJutsu.JutsuEnum jutsu) {
		if (!(stack.getItem() instanceof ItemJutsu.Base)) {
			return 0f;
		}
		ItemJutsu.Base item = (ItemJutsu.Base)stack.getItem();
		int required = Math.max(1, item.getRequiredXp(stack, jutsu));
		return MathHelper.clamp(((float)item.getJutsuXp(stack, jutsu) - (float)required) / ((float)required * 2.0f), 0f, 1f);
	}

	public static class WaterCloneJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.isSneaking()) {
				entity.world.spawnEntity(new EntityWaterClone(entity));
				return true;
			}
			for (EntityWaterClone clone : entity.world.getEntities(EntityWaterClone.class, e -> e.isEntityAlive() && entity.equals(e.getSummoner()))) {
				clone.setDead();
			}
			return false;
		}
	}

	public static class WaterPrisonTrapJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				for (EntityWaterPrisonTrapClone clone : entity.world.getEntities(EntityWaterPrisonTrapClone.class,
						e -> e.isEntityAlive() && entity.equals(e.getSummoner()))) {
					clone.setDead();
				}
				float mastery = masteryRatio(stack, ItemSuiton.WATERPRISONTRAP);
				entity.world.spawnEntity(new EntityWaterPrisonTrapClone(entity, mastery));
				entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
				 SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 1.0f, 0.85f);
			}
			return true;
		}
		@Override public float getBasePower() { return 1.0f; }
		@Override public float getPowerupDelay() { return 15.0f; }
		@Override public float getMaxPower() { return 1.0f; }
	}

	public static class CrowCloneJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				int xp = stack.getItem() instanceof ItemJutsu.Base ? ((ItemJutsu.Base)stack.getItem()).getJutsuXp(stack, ItemNinjutsu.CROWCLONE) : 1250;
				int mastery = Math.max(0, Math.min(1250, xp - 1250));
				int invisTicks = 100 + mastery / 10;
				entity.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, invisTicks, 0, false, false));
				spawnCrows(entity.world, entity.posX, entity.posY, entity.posZ, 14, entity.getRNG());
				for (EntityLivingBase target : entity.world.getEntitiesWithinAABB(EntityLivingBase.class, entity.getEntityBoundingBox().grow(5d))) {
					if (!target.equals(entity)) {
						target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 0, false, false));
					}
				}
				entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
				 SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:crow_call")), SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
			return true;
		}
	}

	public static class CrowTrapCloneJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				for (EntityCrowTrapClone clone : entity.world.getEntities(EntityCrowTrapClone.class,
						e -> e.isEntityAlive() && entity.equals(e.getSummoner()))) {
					clone.dismissSilently();
					clone.setDead();
				}
				entity.world.spawnEntity(new EntityCrowTrapClone(entity));
			}
			return true;
		}
	}

	public static class ExplosiveCloneJutsu implements ItemJutsu.IJutsuCallback {
		private final EntityExplosiveClone.EC.Jutsu delegate = new EntityExplosiveClone.EC.Jutsu();
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			return this.delegate.createJutsu(stack, entity, power);
		}
	}

	public static class ShurikenShadowCloneJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				Vec3d look = entity.getLookVec();
				EntityLivingBase target = getLookTarget(entity, 36d, 4d);
				for (int i = 0; i < 14; i++) {
					double side = (i - 6.5d) * 0.28d;
					double up = ((i % 5) - 2) * 0.18d;
					EntityGuidedShuriken shuriken = new EntityGuidedShuriken(entity.world, entity, target);
					shuriken.setPosition(entity.posX + side * Math.cos(Math.toRadians(entity.rotationYaw)),
					 entity.posY + entity.getEyeHeight() - 0.1d + up,
					 entity.posZ + side * Math.sin(Math.toRadians(entity.rotationYaw)));
					Vec3d dir = look.addVector((entity.getRNG().nextDouble() - 0.5d) * 0.75d, up * 0.6d,
					 (entity.getRNG().nextDouble() - 0.5d) * 0.75d).normalize();
					shuriken.shoot(dir.x, dir.y, dir.z, 1.7f, 0.0f);
					shuriken.setDamage(4.0d + power);
					shuriken.pickupStatus = EntityArrow.PickupStatus.DISALLOWED;
					entity.world.spawnEntity(shuriken);
				}
			}
			return true;
		}
	}

	public static class HousenkaJutsu implements ItemJutsu.IJutsuCallback {
		private final boolean shuriken;
		public HousenkaJutsu(boolean shurikenIn) {
			this.shuriken = shurikenIn;
		}
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				if (!this.shuriken) {
					EntityFireDragonHead dragon = new EntityFireDragonHead(entity, MathHelper.clamp(power, 1.0f, 5.0f));
					entity.world.spawnEntity(dragon);
					entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
					 SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:suiton_suiryuudan")), SoundCategory.PLAYERS, 3.0f, 0.75f);
					return true;
				}
				Vec3d look = entity.getLookVec();
				EntityLivingBase target = getLookTarget(entity, 32d, 4d);
				int count = this.shuriken ? 12 : 7;
				for (int i = 0; i < count; i++) {
					double side = (i - (count - 1) * 0.5d) * (this.shuriken ? 0.22d : 0.16d);
					double up = ((i % 4) - 1.5d) * 0.14d;
					EntityGuidedShuriken arrow = new EntityGuidedShuriken(entity.world, entity, this.shuriken ? target : null);
					arrow.setPosition(entity.posX + side * Math.cos(Math.toRadians(entity.rotationYaw)),
					 entity.posY + entity.getEyeHeight() - 0.1d + up,
					 entity.posZ + side * Math.sin(Math.toRadians(entity.rotationYaw)));
					arrow.setFire(this.shuriken ? 10 : 6);
					arrow.setDamage(this.shuriken ? 6.0d + power : 4.0d + power * 0.5d);
					arrow.pickupStatus = EntityArrow.PickupStatus.DISALLOWED;
					Vec3d dir = look.addVector((entity.getRNG().nextDouble() - 0.5d) * (this.shuriken ? 0.45d : 0.35d),
					 up * 0.5d, (entity.getRNG().nextDouble() - 0.5d) * (this.shuriken ? 0.45d : 0.35d)).normalize();
					arrow.shoot(dir.x, dir.y, dir.z, this.shuriken ? 2.0f : 1.7f, 0f);
					entity.world.spawnEntity(arrow);
				}
			}
			return true;
		}
		@Override public float getBasePower() { return this.shuriken ? 1.0f : 0.9f; }
		@Override public float getPowerupDelay() { return this.shuriken ? 15.0f : 150.0f; }
		@Override public float getMaxPower() { return this.shuriken ? 3.0f : 5.0f; }
	}

	public static class StickySyrupJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				RayTraceResult hit = ProcedureUtils.objectEntityLookingAt(entity, 18d, 2.5d);
				Vec3d center = hit != null && hit.entityHit != null ? hit.entityHit.getPositionVector() : entity.getPositionVector().add(entity.getLookVec().scale(8d));
				entity.world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_SLIME_PLACE, SoundCategory.PLAYERS, 1.0f, 0.55f);
				ProcedureAoeCommand.set(entity.world, center.x, center.y, center.z, 0d, 4.0d).exclude(entity)
				 .damageEntities(ItemJutsu.causeJutsuDamage(entity, entity), 1.0f);
				for (EntityLivingBase target : entity.world.getEntitiesWithinAABB(EntityLivingBase.class,
						entity.getEntityBoundingBox().offset(center.subtract(entity.getPositionVector())).grow(4d, 1.5d, 4d))) {
					if (!target.equals(entity)) {
						target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 120, 4, false, false));
						target.addPotionEffect(new PotionEffect(PotionHeaviness.potion, 80, 1, false, false));
					}
				}
				Particles.spawnParticle(entity.world, Particles.Types.WATER_SPLASH, center.x, center.y + 0.1d, center.z,
				 80, 3d, 0.1d, 3d, 0d, 0.05d, 0d, 0xAAE6D08A, 40);
				for (int i = 0; i < 50; i++) {
					entity.world.spawnParticle(EnumParticleTypes.SLIME, center.x + (entity.getRNG().nextDouble() - 0.5d) * 6d,
					 center.y + 0.05d, center.z + (entity.getRNG().nextDouble() - 0.5d) * 6d, 0d, 0.02d, 0d);
				}
			}
			return true;
		}
	}

	public static class WaterWallJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				Vec3d look = entity.getLookVec();
				Vec3d forward = new Vec3d(look.x, 0d, look.z);
				if (forward.lengthVector() < 0.01d) {
					forward = new Vec3d(-Math.sin(Math.toRadians(entity.rotationYaw)), 0d, Math.cos(Math.toRadians(entity.rotationYaw)));
				}
				forward = forward.normalize();
				Vec3d side = new Vec3d(-forward.z, 0d, forward.x);
				int halfWidth = Math.max(2, Math.min(6, 2 + (int)(power * 1.35f)));
				int height = Math.max(3, Math.min(8, 3 + (int)(power * 1.1f)));
				BlockPos center = new BlockPos(entity.posX + look.x * 3d, entity.posY, entity.posZ + look.z * 3d);
				Map<BlockPos, net.minecraft.block.state.IBlockState> blocks = new HashMap<>();
				for (int x = -halfWidth; x <= halfWidth; x++) {
					for (int y = 0; y <= height; y++) {
						BlockPos pos = new BlockPos(center.getX() + side.x * x, center.getY() + y, center.getZ() + side.z * x);
						if (entity.world.isAirBlock(pos) || entity.world.getBlockState(pos).getBlock().isReplaceable(entity.world, pos)) {
							blocks.put(pos, BlockWaterStill.block.getDefaultState());
						}
					}
				}
				if (!blocks.isEmpty()) {
					entity.world.playSound(null, center, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 1.4f, 0.65f);
					new EventSetBlocks(entity.world, blocks, 0, 80 + (int)(power * 20f), false, false);
				}
			}
			return true;
		}
		@Override public float getBasePower() { return 1.0f; }
		@Override public float getPowerupDelay() { return 20.0f; }
		@Override public float getMaxPower() { return 4.0f; }
	}

	public static class ChidoriSenbonJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				Vec3d look = entity.getLookVec();
				float mastery = masteryRatio(stack, ItemRaiton.CHIDORISENBON);
				entity.world.playSound(null, entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ,
				 SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.PLAYERS, 0.35f, 1.8f);
				for (int i = 0; i < 8; i++) {
					double side = (i - 3.5d) * 0.13d;
					double up = ((i % 4) - 1.5d) * 0.10d;
					EntityGuidedChidoriSenbon senbon = new EntityGuidedChidoriSenbon(entity.world, entity, null);
					senbon.setMastery(mastery);
					senbon.setPosition(entity.posX + side * Math.cos(Math.toRadians(entity.rotationYaw)),
					 entity.posY + entity.getEyeHeight() - 0.1d + up,
					 entity.posZ + side * Math.sin(Math.toRadians(entity.rotationYaw)));
					Vec3d dir = look.addVector((entity.getRNG().nextDouble() - 0.5d) * 0.55d, up * 0.5d,
					 (entity.getRNG().nextDouble() - 0.5d) * 0.55d).normalize();
					senbon.shoot(dir.x, dir.y, dir.z, 1.75f, 0f);
					senbon.setDamage(3.0d + power);
					senbon.pickupStatus = EntityArrow.PickupStatus.DISALLOWED;
					entity.world.spawnEntity(senbon);
				}
			}
			return true;
		}
	}

	public static class RetsudoTenshoJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.world.isRemote) {
				Vec3d center = entity.getPositionVector().add(entity.getLookVec().scale(6d));
				entity.world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 1.5f, 0.45f);
				entity.world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.45f, 0.55f);
				ProcedureAoeCommand.set(entity.world, center.x, center.y, center.z, 0d, 4.5d).exclude(entity)
				 .damageEntities(ItemJutsu.causeJutsuDamage(entity, entity), 10f + power * 2f);
				for (EntityLivingBase target : entity.world.getEntitiesWithinAABB(EntityLivingBase.class,
						entity.getEntityBoundingBox().offset(center.subtract(entity.getPositionVector())).grow(4.5d, 2d, 4.5d))) {
					if (!target.equals(entity)) {
						target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 2, false, false));
						target.motionY += 0.35d;
					}
				}
				for (int i = 0; i < 60; i++) {
					entity.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, center.x + (entity.getRNG().nextDouble() - 0.5d) * 8d,
					 center.y + 0.2d, center.z + (entity.getRNG().nextDouble() - 0.5d) * 8d, 0d, 0.2d, 0d,
					 net.minecraft.block.Block.getStateId(Blocks.DIRT.getDefaultState()));
				}
			}
			return true;
		}
	}

	public static class EntityWaterClone extends EntityClone.Base implements ItemJutsu.IJutsu {
		private int life = 1200;
		private boolean releasedWater;
		public EntityWaterClone(World world) {
			super(world);
			this.moveHelper = new EntityNinjaMob.MoveHelper(this);
			this.tasks.addTask(2, new EntityClone.AIFollowSummoner(this, 0.8d, 3.0F));
		}
		public EntityWaterClone(EntityLivingBase summoner) {
			super(summoner);
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30d);
			this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(6d);
			this.setHealth(this.getMaxHealth());
			this.moveHelper = new EntityNinjaMob.MoveHelper(this);
			this.tasks.addTask(2, new EntityClone.AIFollowSummoner(this, 0.8d, 3.0F));
		}
		@Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.SUITON; }
		@Override public void onUpdate() {
			super.onUpdate();
			if (!this.world.isRemote && this.ticksExisted > this.life) this.setDead();
		}
		@Override public void setDead() {
			if (!this.world.isRemote && !this.releasedWater) {
				this.releasedWater = true;
				EntityLivingBase summoner = this.getSummoner();
				ProcedureAoeCommand.set(this, 0d, 3d).exclude(summoner)
				 .damageEntities(ItemJutsu.causeJutsuDamage(this, summoner), 2f);
				for (EntityLivingBase target : this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(3d, 1d, 3d))) {
					if (!target.equals(summoner)) target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 2, false, false));
				}
				Particles.spawnParticle(this.world, Particles.Types.WATER_SPLASH, this.posX, this.posY + 0.5d, this.posZ,
				 80, 1.5d, 0.4d, 1.5d, 0d, 0.1d, 0d, 0xAA6AD1FF, 40);
				Map<BlockPos, net.minecraft.block.state.IBlockState> puddle = new HashMap<>();
				BlockPos base = new BlockPos(this.posX, this.posY, this.posZ);
				for (BlockPos pos : new BlockPos[] { base, base.north() }) {
					if (this.world.isAirBlock(pos) || this.world.getBlockState(pos).getBlock().isReplaceable(this.world, pos)) {
						puddle.put(pos, BlockWaterStill.block.getDefaultState());
					}
				}
				if (!puddle.isEmpty()) {
					new EventSetBlocks(this.world, puddle, 0, 80, false, false);
				}
			}
			super.setDead();
		}
	}

	public static class EntityWaterPrisonTrapClone extends EntityWaterClone {
		private EntityLivingBase lastAttacker;
		private boolean triggered;
		private float mastery;

		public EntityWaterPrisonTrapClone(World world) {
			super(world);
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(18d);
			this.setHealth(this.getMaxHealth());
		}

		public EntityWaterPrisonTrapClone(EntityLivingBase summoner, float masteryIn) {
			super(summoner);
			this.mastery = MathHelper.clamp(masteryIn, 0f, 1f);
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(18d + 12d * this.mastery);
			this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(3d + 4d * this.mastery);
			this.setHealth(this.getMaxHealth());
		}

		@Override
		public boolean attackEntityFrom(DamageSource source, float amount) {
			if (source.getTrueSource() instanceof EntityLivingBase) {
				this.lastAttacker = (EntityLivingBase)source.getTrueSource();
			}
			boolean ret = super.attackEntityFrom(source, amount);
			if (!this.world.isRemote && ret && amount > 0f && this.getHealth() <= 0f) {
				this.triggerPrison();
			}
			return ret;
		}

		@Override
		public void setDead() {
			if (!this.world.isRemote && this.isEntityAlive()) {
				this.triggerPrison();
			}
			super.setDead();
		}

		private void triggerPrison() {
			if (!this.triggered && this.lastAttacker != null && !this.lastAttacker.equals(this.getSummoner())
			 && ItemJutsu.canTarget(this.lastAttacker)) {
				this.triggered = true;
				int duration = 100 + (int)(100f * this.mastery);
				this.world.spawnEntity(new EntityWaterPrisonTrap(this.world, this.getSummoner(), this.lastAttacker, duration, this.mastery));
				this.world.playSound(null, this.lastAttacker.posX, this.lastAttacker.posY, this.lastAttacker.posZ,
				 SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 1.8f, 0.55f);
			}
		}

		@Override public void writeEntityToNBT(NBTTagCompound compound) {
			super.writeEntityToNBT(compound);
			compound.setFloat("mastery", this.mastery);
		}

		@Override public void readEntityFromNBT(NBTTagCompound compound) {
			super.readEntityFromNBT(compound);
			this.mastery = compound.getFloat("mastery");
		}
	}

	public static class EntityWaterPrisonTrap extends Entity {
		private EntityLivingBase caster;
		private EntityLivingBase target;
		private int casterId = -1;
		private int targetId = -1;
		private int duration = 100;
		private float mastery;
		private AxisAlignedBB prisonBox;

		public EntityWaterPrisonTrap(World world) {
			super(world);
			this.setSize(0.1f, 0.1f);
			this.noClip = true;
		}

		public EntityWaterPrisonTrap(World world, EntityLivingBase casterIn, EntityLivingBase targetIn, int durationIn, float masteryIn) {
			this(world);
			this.caster = casterIn;
			this.target = targetIn;
			this.casterId = casterIn != null ? casterIn.getEntityId() : -1;
			this.targetId = targetIn.getEntityId();
			this.duration = durationIn;
			this.mastery = MathHelper.clamp(masteryIn, 0f, 1f);
			this.setPosition(targetIn.posX, targetIn.posY, targetIn.posZ);
			this.buildPrison();
		}

		@Override protected void entityInit() { }

		private EntityLivingBase getTarget() {
			if (this.target == null && this.targetId >= 0) {
				Entity entity = this.world.getEntityByID(this.targetId);
				this.target = entity instanceof EntityLivingBase ? (EntityLivingBase)entity : null;
			}
			return this.target;
		}

		private EntityLivingBase getCaster() {
			if (this.caster == null && this.casterId >= 0) {
				Entity entity = this.world.getEntityByID(this.casterId);
				this.caster = entity instanceof EntityLivingBase ? (EntityLivingBase)entity : null;
			}
			return this.caster;
		}

		private void buildPrison() {
			EntityLivingBase trapped = this.getTarget();
			if (trapped == null || this.world.isRemote) return;
			double r = 1.65d + 0.35d * this.mastery;
			this.prisonBox = new AxisAlignedBB(trapped.posX - r, trapped.posY - 0.25d, trapped.posZ - r,
			 trapped.posX + r, trapped.posY + trapped.height + 0.75d, trapped.posZ + r);
			Map<BlockPos, net.minecraft.block.state.IBlockState> water = new HashMap<>();
			BlockPos min = new BlockPos(this.prisonBox.minX, this.prisonBox.minY, this.prisonBox.minZ);
			BlockPos max = new BlockPos(this.prisonBox.maxX, this.prisonBox.maxY, this.prisonBox.maxZ);
			Vec3d center = new Vec3d(trapped.posX, trapped.posY + trapped.height * 0.5d, trapped.posZ);
			for (BlockPos pos : BlockPos.getAllInBoxMutable(min, max)) {
				Vec3d p = new Vec3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d);
				if (p.distanceTo(center) <= r + 0.25d
				 && (this.world.isAirBlock(pos) || this.world.getBlockState(pos).getBlock().isReplaceable(this.world, pos))) {
					water.put(pos.toImmutable(), BlockWaterStill.block.getDefaultState());
				}
			}
			if (!water.isEmpty()) {
				new EventSetBlocks(this.world, water, 0, this.duration + 10, false, false);
			}
		}

		@Override
		public void onUpdate() {
			if (this.world.isRemote) {
				for (int i = 0; i < 6; i++) {
					this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE,
					 this.posX + (this.rand.nextDouble() - 0.5d) * 3d,
					 this.posY + this.rand.nextDouble() * 2.2d,
					 this.posZ + (this.rand.nextDouble() - 0.5d) * 3d, 0d, 0.03d, 0d);
				}
				return;
			}
			EntityLivingBase trapped = this.getTarget();
			if (trapped == null || !trapped.isEntityAlive() || this.ticksExisted > this.duration) {
				this.setDead();
				return;
			}
			trapped.setPositionAndUpdate(this.posX, this.posY, this.posZ);
			ProcedureUtils.setVelocity(trapped, 0d, 0d, 0d);
			trapped.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 8, 10, false, false));
			trapped.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 8, 10, false, false));
			trapped.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 8, 4, false, false));
			trapped.fallDistance = 0f;
			if (this.ticksExisted % 20 == 0) {
				trapped.attackEntityFrom(ItemJutsu.causeJutsuDamage(this, this.getCaster()), 1.5f + this.mastery * 2.0f);
				net.narutomod.Chakra.pathway(trapped).consume(8d + 12d * this.mastery);
				Particles.spawnParticle(this.world, Particles.Types.WATER_SPLASH, this.posX, this.posY + 1d, this.posZ,
				 60, 1.2d, 0.8d, 1.2d, 0d, 0.05d, 0d, 0xAA6AD1FF, 30);
			}
		}

		@Override
		public void setDead() {
			if (!this.world.isRemote && this.prisonBox != null) {
				for (BlockPos pos : BlockPos.getAllInBoxMutable(new BlockPos(this.prisonBox.minX, this.prisonBox.minY, this.prisonBox.minZ),
						new BlockPos(this.prisonBox.maxX, this.prisonBox.maxY, this.prisonBox.maxZ))) {
					if (this.world.getBlockState(pos).getBlock() == BlockWaterStill.block) {
						this.world.setBlockToAir(pos);
					}
				}
			}
			super.setDead();
		}

		@Override protected void readEntityFromNBT(NBTTagCompound compound) {
			this.ticksExisted = compound.getInteger("ticks");
			this.duration = compound.getInteger("duration");
			this.mastery = compound.getFloat("mastery");
			this.casterId = compound.getInteger("casterId");
			this.targetId = compound.getInteger("targetId");
		}

		@Override protected void writeEntityToNBT(NBTTagCompound compound) {
			compound.setInteger("ticks", this.ticksExisted);
			compound.setInteger("duration", this.duration);
			compound.setFloat("mastery", this.mastery);
			compound.setInteger("casterId", this.casterId);
			compound.setInteger("targetId", this.targetId);
		}
	}

	public static class EntityCrowTrapClone extends EntityClone.Base implements ItemJutsu.IJutsu {
		private EntityLivingBase lastAttacker;
		private boolean burst;
		private boolean silentDismiss;

		public EntityCrowTrapClone(World world) {
			super(world);
			this.setNoAI(false);
			this.moveHelper = new EntityNinjaMob.MoveHelper(this);
			this.tasks.addTask(2, new EntityClone.AIFollowSummoner(this, 0.8d, 3.0F));
		}

		public EntityCrowTrapClone(EntityLivingBase summoner) {
			super(summoner);
			this.setNoAI(false);
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(8d);
			this.setHealth(this.getMaxHealth());
			this.moveHelper = new EntityNinjaMob.MoveHelper(this);
			this.tasks.addTask(2, new EntityClone.AIFollowSummoner(this, 0.8d, 3.0F));
		}

		@Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.NINJUTSU; }

		@Override
		public boolean attackEntityFrom(DamageSource source, float amount) {
			if (source.getTrueSource() instanceof EntityLivingBase) {
				this.lastAttacker = (EntityLivingBase)source.getTrueSource();
			}
			boolean ret = super.attackEntityFrom(source, amount);
			if (!this.world.isRemote && ret && amount > 0f && this.getHealth() <= 0f) {
				this.burstCrows();
			}
			return ret;
		}

		public void dismissSilently() {
			this.silentDismiss = true;
		}

		@Override
		public void setDead() {
			if (!this.world.isRemote && !this.silentDismiss) {
				this.burstCrows();
			}
			super.setDead();
		}

		private void burstCrows() {
			if (!this.burst) {
				this.burst = true;
				spawnCrows(this.world, this.posX, this.posY, this.posZ, 18, this.rand);
				if (this.lastAttacker != null && !this.lastAttacker.equals(this.getSummoner())) {
					this.lastAttacker.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0, false, false));
				}
				this.world.playSound(null, this.posX, this.posY, this.posZ,
				 SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:crow_call")), SoundCategory.PLAYERS, 1.0f, 0.9f);
			}
		}
	}

	public static class EntityLightningClone extends EntityClone.Base implements ItemJutsu.IJutsu {
		private int life = 900;
		public EntityLightningClone(World world) {
			super(world);
			this.moveHelper = new EntityNinjaMob.MoveHelper(this);
			this.tasks.addTask(2, new EntityClone.AIFollowSummoner(this, 0.9d, 3.0F));
		}
		public EntityLightningClone(EntityLivingBase summoner) {
			super(summoner);
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20d);
			this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.65d);
			this.setHealth(this.getMaxHealth());
			this.moveHelper = new EntityNinjaMob.MoveHelper(this);
			this.tasks.addTask(2, new EntityClone.AIFollowSummoner(this, 0.9d, 3.0F));
		}
		@Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.RAITON; }
		@Override public void onUpdate() {
			super.onUpdate();
			if (!this.world.isRemote && this.ticksExisted > this.life) this.setDead();
		}
		@Override public boolean attackEntityFrom(DamageSource source, float amount) {
			boolean ret = super.attackEntityFrom(source, amount);
			if (!this.world.isRemote && amount > 0f && this.getHealth() <= 0f) shock();
			return ret;
		}
		@Override public void setDead() {
			if (!this.world.isRemote && this.isEntityAlive()) shock();
			super.setDead();
		}
		private void shock() {
			EntityLivingBase summoner = this.getSummoner();
			ProcedureAoeCommand.set(this, 0d, 4d).exclude(summoner)
			 .damageEntities(ItemJutsu.causeJutsuDamage(this, summoner), 8f);
			for (EntityLivingBase target : this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(4d))) {
				if (!target.equals(summoner)) target.addPotionEffect(new PotionEffect(PotionParalysis.potion, 50, 0, false, false));
			}
			Particles.spawnParticle(this.world, Particles.Types.SMOKE, this.posX, this.posY + 1d, this.posZ,
			 35, 0.7d, 0.8d, 0.7d, 0d, 0.03d, 0d, 0x70D8FFFF, 20);
		}
	}

	public static class LightningCloneJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!entity.isSneaking()) {
				entity.world.spawnEntity(new EntityLightningClone(entity));
				return true;
			}
			for (EntityLightningClone clone : entity.world.getEntities(EntityLightningClone.class, e -> e.isEntityAlive() && entity.equals(e.getSummoner()))) {
				clone.setDead();
			}
			return false;
		}
	}

	private static void spawnCrows(World world, double x, double y, double z, int count, java.util.Random rand) {
		for (int i = 0; i < count; i++) {
			EntityCrow.EntityCustom crow = new EntityCrow.EntityCustom(world);
			crow.setPosition(x + (rand.nextDouble() - 0.5d) * 3d,
			 y + 0.5d + rand.nextDouble() * 2d,
			 z + (rand.nextDouble() - 0.5d) * 3d);
			world.spawnEntity(crow);
		}
	}

	public static class EntityGuidedShuriken extends ItemShuriken.EntityArrowCustom {
		private EntityLivingBase target;

		public EntityGuidedShuriken(World world) {
			super(world);
			this.setNoGravity(true);
		}

		public EntityGuidedShuriken(World world, EntityLivingBase shooter, EntityLivingBase targetIn) {
			super(world, shooter);
			this.target = targetIn;
			this.setNoGravity(true);
		}

		@Override
		public void onUpdate() {
			if (!this.world.isRemote && this.ticksExisted > 80) {
				this.setDead();
				return;
			}
			if (!this.inGround && this.target != null && this.ticksExisted > 4) {
				Vec3d dir = guidedDirection(this, this.target, 0.16d);
				double speed = Math.max(1.25d, new Vec3d(this.motionX, this.motionY, this.motionZ).lengthVector());
				this.motionX = dir.x * speed;
				this.motionY = dir.y * speed;
				this.motionZ = dir.z * speed;
			}
			super.onUpdate();
			if (this.isBurning()) {
				Particles.spawnParticle(this.world, Particles.Types.FLAME, this.posX, this.posY, this.posZ,
				 3, 0.08d, 0.08d, 0.08d, 0d, 0d, 0d, 0xffff5500, 12);
			}
		}
	}

	public static class EntityGuidedChidoriSenbon extends ItemSenbon.EntityArrowCustom {
		private float acceleration = 0.012f;
		private double maxSpeed = 2.35d;

		public EntityGuidedChidoriSenbon(World world) {
			super(world);
			this.setNoGravity(true);
		}

		public EntityGuidedChidoriSenbon(World world, EntityLivingBase shooter, EntityLivingBase targetIn) {
			super(world, shooter);
			this.setNoGravity(true);
		}

		public void setMastery(float mastery) {
			this.acceleration = 0.010f + MathHelper.clamp(mastery, 0f, 1f) * 0.008f;
			this.maxSpeed = 2.2d + MathHelper.clamp(mastery, 0f, 1f) * 0.45d;
		}

		@Override
		public void onUpdate() {
			if (!this.world.isRemote && this.ticksExisted > 65) {
				this.setDead();
				return;
			}
			if (!this.inGround) {
				Vec3d dir = new Vec3d(this.motionX, this.motionY, this.motionZ).normalize();
				double speed = Math.min(this.maxSpeed, Math.max(1.35d, new Vec3d(this.motionX, this.motionY, this.motionZ).lengthVector()) + this.acceleration);
				this.motionX = dir.x * speed;
				this.motionY = dir.y * speed;
				this.motionZ = dir.z * speed;
				this.rotationYaw = (float)(MathHelper.atan2(this.motionX, this.motionZ) * (180d / Math.PI));
				this.rotationPitch = (float)(MathHelper.atan2(this.motionY, MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ)) * (180d / Math.PI));
			}
			super.onUpdate();
			Particles.spawnParticle(this.world, Particles.Types.SMOKE, this.posX, this.posY, this.posZ,
			 1, 0.02d, 0.02d, 0.02d, 0d, 0d, 0d, 0x50D8FFFF, 8);
			if (this.world.isRemote && this.ticksExisted % 2 == 0) {
				this.world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, this.posX - this.motionX * 0.15d, this.posY - this.motionY * 0.15d,
				 this.posZ - this.motionZ * 0.15d, 0d, 0d, 0d);
			}
		}

		@Override
		protected void arrowHit(EntityLivingBase entity) {
			super.arrowHit(entity);
			entity.attackEntityFrom(ItemJutsu.causeJutsuDamage(this, this.shootingEntity), 3.0f);
			entity.addPotionEffect(new PotionEffect(PotionParalysis.potion, 35, 0, false, false));
		}
	}

	public static class EntityFireDragonHead extends net.narutomod.entity.EntityWaterDragon.EC {
		private float damage = 9.0f;

		public EntityFireDragonHead(World world) {
			super(world);
			this.isImmuneToFire = true;
		}

		public EntityFireDragonHead(EntityLivingBase shooter, float power) {
			super(shooter, power);
			this.isImmuneToFire = true;
			this.damage = 20.0f * power;
		}

		@Override
		public void onUpdate() {
			super.onUpdate();
			if (this.world.isRemote) {
				for (int i = 0; i < 4; i++) {
					this.world.spawnParticle(EnumParticleTypes.FLAME, this.posX + (this.rand.nextDouble() - 0.5d) * this.width,
					 this.posY + this.rand.nextDouble() * this.height, this.posZ + (this.rand.nextDouble() - 0.5d) * this.width,
					 -this.motionX * 0.05d, 0.02d, -this.motionZ * 0.05d);
				}
			}
		}

		@Override
		public void renderParticles() {
			Particles.spawnParticle(this.world, Particles.Types.FLAME, this.posX, this.posY + this.height * 0.5d, this.posZ,
			 18, this.width * 0.35d, this.height * 0.25d, this.width * 0.35d, 0d, 0.03d, 0d, 0xAAFF5A00, 22);
		}

		@Override
		protected void onImpact(RayTraceResult result) {
			if (result.entityHit != null && result.entityHit.equals(this.shootingEntity)) {
				return;
			}
			if (!this.world.isRemote) {
				float size = this.getEntityScale();
				ProcedureAoeCommand.set(this, 0d, 3.0d).exclude(this.shootingEntity)
				 .damageEntities(ItemJutsu.causeJutsuDamage(this, this.shootingEntity).setFireDamage(), this.damage).setFire(8);
				this.world.newExplosion(this.shootingEntity, this.posX, this.posY, this.posZ,
				 5.0F * size, false,
				 net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this.shootingEntity));
				this.setDead();
			}
		}

		@Override
		public ItemJutsu.JutsuEnum.Type getJutsuType() {
			return ItemJutsu.JutsuEnum.Type.KATON;
		}
	}

	@SideOnly(Side.CLIENT)
	public static class RenderFireDragonHead extends Render<EntityFireDragonHead> {
		private final ResourceLocation texture = new ResourceLocation("narutomod:textures/dragon_blue.png");
		private final ResourceLocation smokeTexture = new ResourceLocation("narutomod:textures/gas256.png");
		private final net.narutomod.entity.EntityWaterDragon.Renderer.ModelDragonHead model =
		 new net.narutomod.entity.EntityWaterDragon.Renderer().new ModelDragonHead();

		public RenderFireDragonHead(net.minecraft.client.renderer.entity.RenderManager renderManager) {
			super(renderManager);
			this.shadowSize = 0.1F;
		}

		@Override
		public boolean shouldRender(EntityFireDragonHead entity, net.minecraft.client.renderer.culling.ICamera camera,
				double camX, double camY, double camZ) {
			return true;
		}

		@Override
		public void doRender(EntityFireDragonHead entity, double x, double y, double z, float yaw, float pt) {
			float age = entity.ticksExisted + pt;
			float f1 = -entity.prevRotationYaw - MathHelper.wrapDegrees(entity.rotationYaw - entity.prevRotationYaw) * pt;
			float f2 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * pt;
			float scale = entity.getEntityScale();
			this.model.setRotationAngles(0f, 0f, age, 0f, 0f, 0.0625F, entity);
			GlStateManager.pushMatrix();
			GlStateManager.translate((float)x, (float)y + scale, (float)z);
			GlStateManager.rotate(f1, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(f2 - 180F, 1.0F, 0.0F, 0.0F);
			GlStateManager.scale(scale, scale, scale);
			GlStateManager.enableBlend();
			GlStateManager.disableCull();
			GlStateManager.disableLighting();
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
			this.bindTexture(this.smokeTexture);
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.translate(0.0F, age * 0.015F, 0.0F);
			GlStateManager.matrixMode(5888);
			GlStateManager.color(1.0F, 0.22F + 0.12F * MathHelper.sin(age * 0.45F), 0.0F, 0.82F);
			this.model.render(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F * 0.99F);
			GlStateManager.matrixMode(5890);
			GlStateManager.loadIdentity();
			GlStateManager.matrixMode(5888);
			this.bindEntityTexture(entity);
			GlStateManager.color(1.0F, 0.45F, 0.05F, 0.92F);
			this.model.render(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableLighting();
			GlStateManager.enableCull();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(EntityFireDragonHead entity) {
			return this.texture;
		}
	}

	@SideOnly(Side.CLIENT)
	public static class RenderChidoriSenbon extends Render<EntityGuidedChidoriSenbon> {
		private final RenderItem itemRenderer;

		public RenderChidoriSenbon(net.minecraft.client.renderer.entity.RenderManager renderManager) {
			super(renderManager);
			this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
		}

		@Override
		public void doRender(EntityGuidedChidoriSenbon entity, double x, double y, double z, float entityYaw, float partialTicks) {
			GlStateManager.pushMatrix();
			GlStateManager.translate((float)x, (float)y, (float)z);
			GlStateManager.enableRescaleNormal();
			GlStateManager.rotate(ProcedureUtils.interpolateRotation(entity.prevRotationYaw, entity.rotationYaw, partialTicks) - 90.0F, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 0.0F, 0.0F, 1.0F);
			GlStateManager.scale(0.7F, 0.7F, 0.7F);
			this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			this.itemRenderer.renderItem(new ItemStack(ItemSenbon.block != null ? ItemSenbon.block : Items.ARROW), ItemCameraTransforms.TransformType.GROUND);
			GlStateManager.disableRescaleNormal();
			GlStateManager.popMatrix();
			super.doRender(entity, x, y, z, entityYaw, partialTicks);
		}

		@Override
		protected ResourceLocation getEntityTexture(EntityGuidedChidoriSenbon entity) {
			return TextureMap.LOCATION_BLOCKS_TEXTURE;
		}
	}
}
