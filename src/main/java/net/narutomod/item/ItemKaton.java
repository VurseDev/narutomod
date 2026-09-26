
package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.SoundEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelBox;

import net.narutomod.entity.EntityRendererRegister;
import net.narutomod.entity.EntityScalableProjectile;
import net.narutomod.entity.EntityHidingInAsh;
import net.narutomod.entity.EntityFirestream;
import net.narutomod.entity.EntityFlameSlice;
import net.narutomod.entity.EntityFlameFormation;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.procedure.ProcedureAoeCommand;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.NarutomodModVariables;
import net.narutomod.Particles;
import net.narutomod.ElementsNarutomodMod;

@ElementsNarutomodMod.ModElement.Tag
public class ItemKaton extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:katon")
	public static final Item block = null;
	public static final int ENTITYID = 123;
	public static final int PHOENIX_ENTITYID = 9123;
	//public static final int ENTITY2ID = 10123;
	public static final ItemJutsu.JutsuEnum GREATFIREBALL = new ItemJutsu.JutsuEnum(0, "katonfireball", 'C', 30d, new EntityBigFireball.Jutsu());
	public static final ItemJutsu.JutsuEnum GFANNIHILATION = new ItemJutsu.JutsuEnum(1, "tooltip.katon.annihilation", 'B', 50d, new EntityFirestream.EC.Jutsu1());
	public static final ItemJutsu.JutsuEnum HIDINGINASH = new ItemJutsu.JutsuEnum(2, "hiding_in_ash", 'B', 50d, new EntityHidingInAsh.EC.Jutsu());
	public static final ItemJutsu.JutsuEnum GREATFLAME = new ItemJutsu.JutsuEnum(3, "katonfirestream", 'C', 20d, new EntityFirestream.EC.Jutsu2());
	public static final ItemJutsu.JutsuEnum FLAMESLICE = new ItemJutsu.JutsuEnum(4, "flame_slice", 'D', 20d, new EntityFlameSlice.EC.Jutsu());
	public static final ItemJutsu.JutsuEnum BARRIER = new ItemJutsu.JutsuEnum(5, "flame_formation", 'B', 100d, new EntityFlameFormation.EC.Jutsu());
	public static final ItemJutsu.JutsuEnum FIREPHOENIX = new ItemJutsu.JutsuEnum(6, "fire_phoenix", 'B', 85d, new EntityFirePhoenix.Jutsu()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum HOUSENKA = new ItemJutsu.JutsuEnum(7, "housenka", 'D', 36d, new ItemExtraJutsu.HousenkaJutsu(false)).withCustomBalance();
	public static final ItemJutsu.JutsuEnum HOUSENKATSUMABENI = new ItemJutsu.JutsuEnum(8, "housenka_tsumabeni", 'C', 45d, new ItemExtraJutsu.HousenkaJutsu(true)).withCustomBalance();
	public static final ItemJutsu.JutsuEnum FLAMEWHIRLWIND = new ItemJutsu.JutsuEnum(9, "flame_whirlwind", 'B', 90d, new ItemCanonicalJutsu.FlameWhirlwind()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum TWINFLAMEDRAGONS = new ItemJutsu.JutsuEnum(10, "twin_flame_dragons", 'A', 140d, new BloodlineTechniques.TwinFlameDragons()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum FLAMECOMPANY = new ItemJutsu.JutsuEnum(11, "flame_company", 'B', 110d, new BloodlineTechniques.FlameCompanyJutsu()).withCustomBalance();

	public ItemKaton(ElementsNarutomodMod instance) {
		super(instance, 366);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new RangedItem(GREATFIREBALL, GFANNIHILATION, HIDINGINASH, GREATFLAME, FLAMESLICE, BARRIER, FIREPHOENIX, HOUSENKA, HOUSENKATSUMABENI, FLAMEWHIRLWIND, TWINFLAMEDRAGONS, FLAMECOMPANY));
		elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityBigFireball.class)
				.id(new ResourceLocation("narutomod", "katonfireball"), ENTITYID).name("katonfireball").tracker(64, 1, true).build());
		elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityFirePhoenix.class)
				.id(new ResourceLocation("narutomod", "fire_phoenix"), PHOENIX_ENTITYID).name("fire_phoenix").tracker(64, 1, true).build());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(block, 0, new ModelResourceLocation("narutomod:katon", "inventory"));
	}

	public static class RangedItem extends ItemJutsu.Base {
		public RangedItem(ItemJutsu.JutsuEnum... list) {
			super(ItemJutsu.JutsuEnum.Type.KATON, list);
			this.setRegistryName("katon");
			this.setUnlocalizedName("katon");
			this.setCreativeTab(TabModTab.tab);
			//this.defaultCooldownMap[GREATFIREBALL.index] = 0;
			//this.defaultCooldownMap[1] = 0;
		}
	}

	public static class EntityBigFireball extends EntityScalableProjectile.Base implements ItemJutsu.IJutsu {
		private float fullScale = 1f;
		private final int timeToFullscale = 20;
		private int explosionSize;
		private float damage;
		private boolean guided;
		private Entity target;
		
		public EntityBigFireball(World a) {
			super(a);
			this.setOGSize(0.8F, 0.8F);
		}

		public EntityBigFireball(EntityLivingBase shooter, float fullScale, boolean isGuided) {
			super(shooter);
			this.setOGSize(0.8F, 0.8F);
			this.fullScale = fullScale;
			this.explosionSize = Math.max((int)fullScale - 1, 0);
			this.damage = fullScale * 10.0f;
			this.guided = isGuided;
			//this.setEntityScale(0.1f);
			Vec3d vec3d = shooter.getLookVec();
			this.setPosition(shooter.posX + vec3d.x, shooter.posY + shooter.getEyeHeight() - 0.2d * fullScale + vec3d.y, shooter.posZ + vec3d.z);
		}

		@Override
		public ItemJutsu.JutsuEnum.Type getJutsuType() {
			return ItemJutsu.JutsuEnum.Type.KATON;
		}

		public void setDamage(float amount) {
			this.damage = amount;
		}

		@Override
		public boolean isImmuneToExplosions() {
			return true;
		}

		@Override
		protected void onImpact(RayTraceResult result) {
			if (result.typeOfHit == RayTraceResult.Type.BLOCK && this.fullScale >= 2.0f && this.ticksInAir < 15) {
				return;
			}
			if (!this.world.isRemote) {
				if (this.shootingEntity != null) {
					this.shootingEntity.getEntityData().setDouble(NarutomodModVariables.InvulnerableTime, 20d);
				}
				if (result.entityHit != null && (result.entityHit.equals(this.shootingEntity)
				 || (result.entityHit instanceof EntityBigFireball && ((EntityBigFireball)result.entityHit).shootingEntity == this.shootingEntity))) {
					return;
				}
				ProcedureAoeCommand.set(this, 0d, this.fullScale * 0.4f).exclude(this.shootingEntity)
				 .damageEntities(ItemJutsu.causeJutsuDamage(this, this.shootingEntity).setFireDamage(), this.damage).setFire(15);
				boolean flag = ForgeEventFactory.getMobGriefingEvent(this.world, this.shootingEntity);
				this.world.newExplosion(this.shootingEntity, this.posX, this.posY, this.posZ, this.explosionSize, flag, false);
				this.setDead();
			}
		}

		@Override
		public void renderParticles() {
			Particles.spawnParticle(this.world, Particles.Types.FLAME, this.posX, this.posY + (this.height / 2.0F), this.posZ,
			  (int)this.fullScale * 2, 0.3d * this.width, 0.3d * this.height, 0.3d * this.width, 0d, 0d, 0d,
			  0xffff0000|((0x40+this.rand.nextInt(0x80))<<8), 30);
		}

		@Override
		protected void checkOnGround() {
		}

		@Override
		public void onUpdate() {
			super.onUpdate();
			if (!this.world.isRemote && (this.ticksInAir > (this.guided ? 200 : 100) || this.isInWater())) {
				this.setDead();
			} else {
				if (!this.world.isRemote && this.ticksAlive <= this.timeToFullscale) {
					this.setEntityScale(1f + (this.fullScale - 1f) * this.ticksAlive / this.timeToFullscale);
				}
				if (this.guided && this.shootingEntity != null) {
					Vec3d vec;
					if (this.target == null) {
						this.target = this.shootingEntity instanceof EntityLiving ? ((EntityLiving)this.shootingEntity).getAttackTarget()
						 : ProcedureUtils.objectEntityLookingAt(this.shootingEntity, 50d, 3d, EntityBigFireball.class).entityHit;
						vec = this.target != null ? this.target.getPositionEyes(1f).subtract(this.getPositionVector())
						 : this.shootingEntity.getLookVec();
					} else {
						vec = this.target.getPositionEyes(1f).subtract(this.getPositionVector());
					}
					this.motionX *= 0.9D;
					this.motionY *= 0.9D;
					this.motionZ *= 0.9D;
					this.shoot(vec.x, vec.y, vec.z, 0.99f, 0f);
				}
				if (this.rand.nextFloat() <= 0.2f) {
					//this.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1, this.rand.nextFloat() + 0.5f);
					this.playSound(SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:flamethrow")),
					 this.fullScale >= 10.0f ? 5.0F : 1.0f, this.rand.nextFloat() * 0.5f + 0.6f);
				}
			}
		}

		public static class Jutsu implements ItemJutsu.IJutsuCallback {
			@Override
			public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
				if (power >= 0.5f) {
					this.createJutsu(entity, entity.getLookVec().x, entity.getLookVec().y, entity.getLookVec().z, power,
					 stack.getItem() instanceof RangedItem && ((RangedItem)stack.getItem()).getCurrentJutsuXpModifier(stack, entity) <= 0.5f);
					//if (entity instanceof EntityPlayer)
					//	ItemJutsu.setCurrentJutsuCooldown(stack, (EntityPlayer)entity, (long)(power * 80));
					return true;
				}
				return false;
			}

			public void createJutsu(EntityLivingBase entity, double x, double y, double z, float power) {
				this.createJutsu(entity, x, y, z, power, false);
			}

			public void createJutsu(EntityLivingBase entity, double x, double y, double z, float power, boolean isGuided) {
				EntityBigFireball entityarrow = new EntityBigFireball(entity, power, isGuided);
				entityarrow.shoot(x, y, z, 0.99f, 0);
				entity.world.spawnEntity(entityarrow);
			}

			@Override
			public float getBasePower() {
				return 0.5f;
			}
	
			@Override
			public float getPowerupDelay() {
				return 30.0f;
			}
	
			@Override
			public float getMaxPower() {
				return 10.0f;
			}
		}
	}

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		new Renderer().register();
	}

	public static class EntityFirePhoenix extends EntityScalableProjectile.Base implements ItemJutsu.IJutsu {
		private float damage;
		private float power;
		private EntityLivingBase target;
		private boolean acquired;
		private java.util.UUID savedCaster;
		private java.util.UUID savedTarget;

		public EntityFirePhoenix(World worldIn) {
			super(worldIn);
			this.setOGSize(1.2F, 0.8F);
			this.isImmuneToFire = true;
			this.setNoGravity(true);
		}

		public EntityFirePhoenix(EntityLivingBase shooter, float powerIn) {
			super(shooter);
			this.setOGSize(1.2F, 0.8F);
			this.power = net.narutomod.PhoenixFlight.power(powerIn);
			this.damage = 12.0f + this.power * 8.0f;
			this.setEntityScale(net.narutomod.PhoenixFlight.scale(this.power));
			Vec3d vec = shooter.getLookVec();
			this.setPosition(shooter.posX + vec.x, shooter.posY + shooter.getEyeHeight() - this.height * .5d + vec.y, shooter.posZ + vec.z);
			this.isImmuneToFire = true;
		}

		@Override
		public void shoot(double x, double y, double z, float ignoredSpeed, float inaccuracy) {
			// Base's no-gravity speed is a per-tick multiplier, NOT blocks/tick.
			// Keep its swept collision/movement, but own a bounded acceleration curve.
			this.haltMotion();
			this.setMotionFactor(1f);
			Vec3d velocity = net.narutomod.PhoenixFlight.velocity(new Vec3d(x, y, z), null, 0);
			this.motionX = velocity.x; this.motionY = velocity.y; this.motionZ = velocity.z;
		}

		private Vec3d center() { return this.getPositionVector().addVector(0, this.height * .5, 0); }
		private Vec3d center(Entity entity) { return entity.getPositionVector().addVector(0, entity.height * .5, 0); }
		private Vec3d targetPoint(Entity entity) {
			// A charged bird is taller than a player: aiming at their waist would plough into flat ground.
			return entity.getPositionVector().addVector(0, Math.max(entity.height * .5, this.height * .5 + .12), 0);
		}

		protected boolean canHit(Entity entity) {
			if (entity == this.shootingEntity || !ItemJutsu.canTarget(entity) || entity.noClip) return false;
			if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
				net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer)entity;
				if (player.isCreative() || (this.shootingEntity instanceof net.minecraft.entity.player.EntityPlayer
				    && !((net.minecraft.entity.player.EntityPlayer)this.shootingEntity).canAttackPlayer(player))) return false;
			}
			return this.shootingEntity == null || (!this.shootingEntity.isOnSameTeam(entity)
			    && !net.narutomod.SusanooCombat.isOwnSusanoo(entity, this.shootingEntity)
			    && !(entity instanceof EntityScalableProjectile.Base && ((EntityScalableProjectile.Base)entity).shootingEntity == this.shootingEntity)
			    && !(entity instanceof net.narutomod.entity.EntitySummonAnimal.ISummon
			        && ((net.narutomod.entity.EntitySummonAnimal.ISummon)entity).getSummoner() == this.shootingEntity));
		}

		protected void updateFlight() {
			if (this.target != null && (!canHit(this.target) || this.target.world != this.world
			    || this.getDistanceSq(this.target) > net.narutomod.PhoenixFlight.LEASH * net.narutomod.PhoenixFlight.LEASH)) {
				// Substitution/untargetability breaks this cast's lock permanently; never re-lock its victim.
				this.target = null;
			}
			if (!this.acquired && this.ticksAlive % 2 == 0) {
				double nearest = Double.MAX_VALUE;
				Vec3d forward = new Vec3d(this.motionX, this.motionY, this.motionZ);
				for (EntityLivingBase candidate : this.world.getEntitiesWithinAABB(EntityLivingBase.class,
				    this.getEntityBoundingBox().grow(net.narutomod.PhoenixFlight.LOCK_RANGE))) {
					Vec3d offset = targetPoint(candidate).subtract(center());
					if (canHit(candidate) && net.narutomod.PhoenixFlight.inAcquisitionCone(forward, offset)
					    && this.world.rayTraceBlocks(center(), targetPoint(candidate), true, false, false) == null
					    && offset.lengthSquared() < nearest) {
						this.target = candidate; nearest = offset.lengthSquared();
					}
				}
				if (this.target != null) this.acquired = true;
			}
			Vec3d velocity = net.narutomod.PhoenixFlight.velocity(new Vec3d(this.motionX, this.motionY, this.motionZ),
			    this.target == null ? null : targetPoint(this.target).subtract(center()), this.ticksAlive);
			this.motionX = velocity.x; this.motionY = velocity.y; this.motionZ = velocity.z;
		}

		@Override
		protected RayTraceResult forwardsRaycast(boolean includeEntities, boolean ignored, Entity excluded) {
			Vec3d start = center(), end = start.addVector(this.motionX, this.motionY, this.motionZ);
			net.minecraft.util.math.AxisAlignedBB swept = this.getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(.001);
			RayTraceResult closest = null;
			double distance = Double.MAX_VALUE;
			for (net.minecraft.util.math.BlockPos pos : net.minecraft.util.math.BlockPos.getAllInBoxMutable(
			    new net.minecraft.util.math.BlockPos(swept.minX, swept.minY, swept.minZ),
			    new net.minecraft.util.math.BlockPos(swept.maxX, swept.maxY, swept.maxZ))) {
				if (this.world.getBlockState(pos).getMaterial() != net.minecraft.block.material.Material.WATER) continue;
				Vec3d hit = net.narutomod.PhoenixFlight.contact(new net.minecraft.util.math.AxisAlignedBB(pos), start, end, this.width, this.height);
				if (hit != null && start.squareDistanceTo(hit) < distance) {
					closest = new RayTraceResult(hit, net.minecraft.util.EnumFacing.UP, pos.toImmutable());
					distance = start.squareDistanceTo(hit);
				}
			}
			for (net.minecraft.util.math.AxisAlignedBB box : this.world.getCollisionBoxes(null, swept)) {
				Vec3d hit = net.narutomod.PhoenixFlight.contact(box, start, end, this.width, this.height);
				if (hit != null && start.squareDistanceTo(hit) < distance) {
					closest = new RayTraceResult(hit, net.minecraft.util.EnumFacing.UP, new net.minecraft.util.math.BlockPos(box.minX, box.minY, box.minZ));
					distance = start.squareDistanceTo(hit);
				}
			}
			if (includeEntities) for (Entity entity : this.world.getEntitiesWithinAABBExcludingEntity(this, swept.grow(1))) {
				if (!entity.canBeCollidedWith() || !canHit(entity)) continue;
				Vec3d hit = net.narutomod.PhoenixFlight.contact(entity.getEntityBoundingBox(), start, end, this.width, this.height);
				if (hit != null && start.squareDistanceTo(hit) < distance) {
					closest = new RayTraceResult(entity, hit); distance = start.squareDistanceTo(hit);
				}
			}
			return closest;
		}

		protected void quenchPhoenix() {
			this.world.playSound(null, this.posX, this.posY, this.posZ, net.minecraft.init.SoundEvents.BLOCK_FIRE_EXTINGUISH,
			    net.minecraft.util.SoundCategory.PLAYERS, 1f, .85f);
			if (this.world instanceof net.minecraft.world.WorldServer) ((net.minecraft.world.WorldServer)this.world).spawnParticle(
			    net.minecraft.util.EnumParticleTypes.CLOUD, this.posX, this.posY + this.height * .5, this.posZ, 18, .45, .4, .45, .04);
			this.setDead();
		}

		@Override
		public ItemJutsu.JutsuEnum.Type getJutsuType() {
			return ItemJutsu.JutsuEnum.Type.KATON;
		}

		@Override
		protected void checkOnGround() {
		}

		@Override
		public void onUpdate() {
			if (!this.world.isRemote) {
				if (this.savedCaster != null && this.world instanceof net.minecraft.world.WorldServer) {
					Entity restored = ((net.minecraft.world.WorldServer)this.world).getEntityFromUuid(this.savedCaster);
					this.shootingEntity = restored instanceof EntityLivingBase ? (EntityLivingBase)restored : null;
					this.savedCaster = null;
				}
				if (this.savedTarget != null && this.world instanceof net.minecraft.world.WorldServer) {
					Entity restored = ((net.minecraft.world.WorldServer)this.world).getEntityFromUuid(this.savedTarget);
					this.target = restored instanceof EntityLivingBase ? (EntityLivingBase)restored : null;
					this.savedTarget = null;
				}
				if (this.ticksAlive >= net.narutomod.PhoenixFlight.LIFETIME || this.shootingEntity == null || !this.shootingEntity.isEntityAlive()) {
					this.setDead(); return;
				}
				if (this.isInWater()) { this.quenchPhoenix(); return; }
				this.updateFlight();
			}
			super.onUpdate();
			if (this.isDead) return;
			double horizontal = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
			if (horizontal > 0.001d || Math.abs(this.motionY) > 0.001d) {
				this.rotationYaw = (float)(MathHelper.atan2(this.motionX, this.motionZ) * (180D / Math.PI));
				this.rotationPitch = (float)(MathHelper.atan2(this.motionY, horizontal) * (180D / Math.PI));
			}
			if (!this.world.isRemote && this.ticksExisted % 8 == 0) {
				this.playSound(SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:flamethrow")), 0.8F, 1.2F);
			}
			Particles.spawnParticle(this.world, Particles.Types.FLAME, this.posX, this.posY + this.height * 0.5d, this.posZ,
			 8, 0.3d * this.width, 0.2d * this.height, 0.3d * this.width, 0d, 0.02d, 0d, 0xffff6600, 20);
		}

		@Override
		protected void onImpact(RayTraceResult result) {
			if (!this.world.isRemote && !this.isDead) {
				if (result.entityHit != null && !canHit(result.entityHit)) return;
				this.setPosition(result.hitVec.x, result.hitVec.y - this.height * .5, result.hitVec.z);
				if (result.typeOfHit == RayTraceResult.Type.BLOCK && this.world.getBlockState(result.getBlockPos()).getMaterial()
				    == net.minecraft.block.material.Material.WATER) { this.quenchPhoenix(); return; }
				Vec3d origin = center();
				double radius = Math.max(1.5d, this.power);
				for (EntityLivingBase victim : this.world.getEntitiesWithinAABB(EntityLivingBase.class,
				    new net.minecraft.util.math.AxisAlignedBB(origin, origin).grow(radius))) {
					if (!canHit(victim) || center(victim).squareDistanceTo(origin) > radius * radius
					    || this.world.rayTraceBlocks(origin, center(victim), true, false, false) != null) continue;
					// One damage event: replacement must not be followed by a second explosion hit or burn.
					if (victim.attackEntityFrom(ItemJutsu.causeJutsuDamage(this, this.shootingEntity).setFireDamage(), this.damage)
					    && ItemJutsu.canTarget(victim)) victim.setFire(10);
				}
				this.world.playSound(null, this.posX, this.posY, this.posZ, net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
				    net.minecraft.util.SoundCategory.PLAYERS, 1f, .9f);
				this.impactEffects();
				this.setDead();
			}
		}

		protected void impactEffects() {
			CustomJutsuEffects.impact(this.world, center(), 0xB8FF5A00, Math.max(2.4f, this.power * 1.25f), 12, 2.8f);
		}

		@Override
		protected void writeEntityToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
			super.writeEntityToNBT(nbt);
			nbt.setFloat("PhoenixPower", this.power);
			nbt.setBoolean("PhoenixAcquired", this.acquired);
			if (this.shootingEntity != null) nbt.setUniqueId("PhoenixCaster", this.shootingEntity.getUniqueID());
			else if (this.savedCaster != null) nbt.setUniqueId("PhoenixCaster", this.savedCaster);
			if (this.target != null) nbt.setUniqueId("PhoenixTarget", this.target.getUniqueID());
			else if (this.savedTarget != null) nbt.setUniqueId("PhoenixTarget", this.savedTarget);
		}

		@Override
		protected void readEntityFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
			super.readEntityFromNBT(nbt);
			this.power = net.narutomod.PhoenixFlight.power(nbt.getFloat("PhoenixPower"));
			this.damage = 12f + this.power * 8f;
			this.setEntityScale(net.narutomod.PhoenixFlight.scale(this.power));
			this.acquired = nbt.getBoolean("PhoenixAcquired");
			this.savedCaster = nbt.hasUniqueId("PhoenixCaster") ? nbt.getUniqueId("PhoenixCaster") : null;
			this.savedTarget = nbt.hasUniqueId("PhoenixTarget") ? nbt.getUniqueId("PhoenixTarget") : null;
			Vec3d direction = new Vec3d(this.motionX, this.motionY, this.motionZ);
			this.shoot(direction.x, direction.y, direction.z, 1f, 0f);
			this.setNoGravity(true);
		}

		public static class Jutsu implements ItemJutsu.IJutsuCallback {
			@Override
			public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
				if (entity.world.isRemote || !Float.isFinite(power) || power < 0.5f) {
					return false;
				}
				EntityFirePhoenix phoenix = new EntityFirePhoenix(entity, power);
				Vec3d vec = entity.getLookVec();
				phoenix.shoot(vec.x, vec.y, vec.z, 1f, 0.0f);
				return entity.world.spawnEntity(phoenix);
			}

			@Override
			public float getBasePower() {
				return 0.8f;
			}

			@Override
			public float getPowerupDelay() {
				return 35.0f;
			}

			@Override
			public float getMaxPower() {
				return 4.0f;
			}
		}
	}
	
	public static class Renderer extends EntityRendererRegister {
		@SideOnly(Side.CLIENT)
		@Override
		public void register() {
			RenderingRegistry.registerEntityRenderingHandler(EntityBigFireball.class, renderManager -> {
				return new RenderBigFireball(renderManager);
			});
			RenderingRegistry.registerEntityRenderingHandler(EntityFirePhoenix.class, renderManager -> {
				return new RenderFirePhoenix(renderManager);
			});
		}

		@SideOnly(Side.CLIENT)
		public class RenderFirePhoenix extends Render<EntityFirePhoenix> {
			private final ResourceLocation texture = new ResourceLocation("narutomod:textures/phantom1.png");
			private final ModelBase mainModel = new ModelPhoenixPhantom();

			public RenderFirePhoenix(RenderManager renderManagerIn) {
				super(renderManagerIn);
				this.shadowSize = 0.2f;
			}

			@Override
			public void doRender(EntityFirePhoenix entity, double x, double y, double z, float entityYaw, float partialTicks) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(x, y + entity.height * .5d, z);
				float scale = entity.getEntityScale();
				GlStateManager.rotate(180.0F - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
				GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
				GlStateManager.scale(scale * 0.45F, scale * 0.45F, scale * 0.45F);
				this.bindEntityTexture(entity);
				GlStateManager.enableBlend();
				GlStateManager.disableLighting();
				GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
				GlStateManager.color(1.0F, 0.34F + 0.12F * MathHelper.sin((entity.ticksExisted + partialTicks) * 0.45F), 0.02F, 0.82F);
				this.mainModel.setRotationAngles(0.0F, 0.0F, entity.ticksExisted + partialTicks, 0.0F, 0.0F, 0.0625F, entity);
				this.mainModel.render(entity, 0.0F, 0.0F, entity.ticksExisted + partialTicks, 0.0F, 0.0F, 0.0625F);
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				GlStateManager.enableLighting();
				GlStateManager.disableBlend();
				GlStateManager.popMatrix();
			}

			private void addQuad(BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
			 float x4, float y4, float z4, int r, int g, int b, int a) {
				buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
				buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
				buffer.pos(x3, y3, z3).color(r, g, b, a).endVertex();
				buffer.pos(x4, y4, z4).color(r, g, b, a).endVertex();
			}

			@Override
			protected ResourceLocation getEntityTexture(EntityFirePhoenix entity) {
				return this.texture;
			}
		}

		@SideOnly(Side.CLIENT)
		public class ModelPhoenixPhantom extends ModelBase {
			private final ModelRenderer body;
			private final ModelRenderer leftWingBody;
			private final ModelRenderer leftWing;
			private final ModelRenderer rightWingBody;
			private final ModelRenderer rightWing;
			private final ModelRenderer head;
			private final ModelRenderer tail;
			private final ModelRenderer tailtip;

			public ModelPhoenixPhantom() {
				textureWidth = 64;
				textureHeight = 64;
				body = new ModelRenderer(this);
				body.setRotationPoint(0.0F, 0.0F, 0.0F);
				body.cubeList.add(new ModelBox(body, 0, 8, -2.5F, -2.0F, -8.0F, 5, 3, 9, 0.0F, false));
				leftWingBody = new ModelRenderer(this);
				leftWingBody.setRotationPoint(2.5F, -2.0F, -8.0F);
				body.addChild(leftWingBody);
				setRotationAngle(leftWingBody, 0.0F, 0.0F, 0.0873F);
				leftWingBody.cubeList.add(new ModelBox(leftWingBody, 23, 12, 0.0F, 0.0F, 0.0F, 6, 2, 9, 0.0F, false));
				leftWing = new ModelRenderer(this);
				leftWing.setRotationPoint(6.0F, 0.0F, 0.0F);
				leftWingBody.addChild(leftWing);
				setRotationAngle(leftWing, 0.0F, 0.0F, 0.1745F);
				leftWing.cubeList.add(new ModelBox(leftWing, 16, 24, 0.0F, 0.0F, 0.0F, 13, 1, 9, 0.0F, false));
				rightWingBody = new ModelRenderer(this);
				rightWingBody.setRotationPoint(-2.5F, -2.0F, -8.0F);
				body.addChild(rightWingBody);
				setRotationAngle(rightWingBody, 0.0F, 0.0F, -0.0873F);
				rightWingBody.cubeList.add(new ModelBox(rightWingBody, 23, 12, -6.0F, 0.0F, 0.0F, 6, 2, 9, 0.0F, true));
				rightWing = new ModelRenderer(this);
				rightWing.setRotationPoint(-6.0F, 0.0F, 0.0F);
				rightWingBody.addChild(rightWing);
				setRotationAngle(rightWing, 0.0F, 0.0F, -0.1745F);
				rightWing.cubeList.add(new ModelBox(rightWing, 16, 24, -13.0F, 0.0F, 0.0F, 13, 1, 9, 0.0F, true));
				head = new ModelRenderer(this);
				head.setRotationPoint(0.5F, 1.0F, -7.0F);
				body.addChild(head);
				head.cubeList.add(new ModelBox(head, 0, 0, -4.0F, -2.0F, -5.0F, 7, 3, 5, 0.0F, false));
				tail = new ModelRenderer(this);
				tail.setRotationPoint(0.5F, -2.0F, 1.0F);
				body.addChild(tail);
				setRotationAngle(tail, -0.0873F, 0.0F, 0.0F);
				tail.cubeList.add(new ModelBox(tail, 3, 20, -2.0F, 0.0F, 0.0F, 3, 2, 6, 0.0F, false));
				tailtip = new ModelRenderer(this);
				tailtip.setRotationPoint(0.0F, 0.5F, 6.0F);
				tail.addChild(tailtip);
				setRotationAngle(tailtip, -0.0873F, 0.0F, 0.0F);
				tailtip.cubeList.add(new ModelBox(tailtip, 4, 29, -1.0F, 0.0F, 0.0F, 1, 1, 6, 0.0F, false));
			}

			@Override
			public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
				body.render(f5);
			}

			public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
				modelRenderer.rotateAngleX = x;
				modelRenderer.rotateAngleY = y;
				modelRenderer.rotateAngleZ = z;
			}

			@Override
			public void setRotationAngles(float f0, float f1, float ageInTicks, float f3, float f4, float f5, Entity entityIn) {
				float f = ((float)(entityIn.getEntityId() * 3) + ageInTicks) * 0.25F;
				this.leftWingBody.rotateAngleZ = MathHelper.cos(f) * 24.0F * ((float)Math.PI / 180F);
				this.leftWing.rotateAngleZ = this.leftWingBody.rotateAngleZ * 1.35F;
				this.rightWingBody.rotateAngleZ = -this.leftWingBody.rotateAngleZ;
				this.rightWing.rotateAngleZ = -this.leftWing.rotateAngleZ;
				this.tail.rotateAngleX = -(5.0F + MathHelper.cos(f * 2.0F) * 8.0F) * ((float)Math.PI / 180F);
				this.tailtip.rotateAngleX = -(5.0F + MathHelper.cos(f * 2.0F) * 10.0F) * ((float)Math.PI / 180F);
			}
		}

		@SideOnly(Side.CLIENT)
		public class RenderBigFireball extends Render<EntityBigFireball> {
			private final ResourceLocation texture = new ResourceLocation("narutomod:textures/fireball.png");
	
			public RenderBigFireball(RenderManager renderManagerIn) {
				super(renderManagerIn);
			}
	
			@Override
			public void doRender(EntityBigFireball entity, double x, double y, double z, float entityYaw, float partialTicks) {
				GlStateManager.pushMatrix();
				this.bindEntityTexture(entity);
				float scale = entity.getEntityScale();
				GlStateManager.translate(x, y + 0.375D * scale, z);
				GlStateManager.enableRescaleNormal();
				GlStateManager.scale(scale, scale, scale);
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder bufferbuilder = tessellator.getBuffer();
				GlStateManager.rotate(180F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
				GlStateManager.rotate((float) (this.renderManager.options.thirdPersonView == 2 ? -1 : 1) * -this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
				GlStateManager.rotate(30F * (partialTicks + entity.ticksExisted), 0.0F, 0.0F, 1.0F);
				GlStateManager.enableBlend();
				GlStateManager.disableLighting();
				OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
				bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
				bufferbuilder.pos(-0.375D, -0.375D, 0.0D).tex(0.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
				bufferbuilder.pos(0.375D, -0.375D, 0.0D).tex(1.0D, 1.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
				bufferbuilder.pos(0.375D, 0.375D, 0.0D).tex(1.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
				bufferbuilder.pos(-0.375D, 0.375D, 0.0D).tex(0.0D, 0.0D).normal(0.0F, 1.0F, 0.0F).endVertex();
				tessellator.draw();
				GlStateManager.enableLighting();
				GlStateManager.disableBlend();
				GlStateManager.disableRescaleNormal();
				GlStateManager.popMatrix();
				super.doRender(entity, x, y, z, entityYaw, partialTicks);
			}
	
			@Override
			protected ResourceLocation getEntityTexture(EntityBigFireball entity) {
				return this.texture;
			}
		}
	}
}
