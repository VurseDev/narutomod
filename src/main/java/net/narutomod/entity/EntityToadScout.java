package net.narutomod.entity;

import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;

import java.util.Comparator;
import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class EntityToadScout extends ElementsNarutomodMod.ModElement {
	public static final int SCOUT_ENTITY_ID = 9318;
	public static final int GAMAKICHI_ENTITY_ID = 9319;

	public EntityToadScout(ElementsNarutomodMod instance) {
		super(instance, 1010);
	}

	@Override
	public void initElements() {
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityScout.class)
		 .id(new ResourceLocation("narutomod", "toad_scout"), SCOUT_ENTITY_ID).name("toad_scout")
		 .tracker(64, 3, true).egg(0x56643a, 0xd9bd62).build());
		this.elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityMiniGamakichi.class)
		 .id(new ResourceLocation("narutomod", "mini_gamakichi"), GAMAKICHI_ENTITY_ID).name("mini_gamakichi")
		 .tracker(64, 3, true).egg(0xd9782f, 0x355c88).build());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void preInit(FMLPreInitializationEvent event) {
		RenderingRegistry.registerEntityRenderingHandler(EntityScout.class,
		 renderManager -> new RenderSmallToad<EntityScout>(renderManager, new ModelSmallToad(false),
		  new ResourceLocation("narutomod:textures/toad_scout.png"), 0.35f));
		RenderingRegistry.registerEntityRenderingHandler(EntityMiniGamakichi.class,
		 renderManager -> new RenderSmallToad<EntityMiniGamakichi>(renderManager, new ModelSmallToad(true),
		  new ResourceLocation("narutomod:textures/mini_gamakichi.png"), 0.55f));
	}

	public static abstract class Base extends EntitySummonAnimal.Base {
		protected Base(World world, float width, float height) {
			super(world);
			this.setOGSize(width, height);
			this.stepHeight = 1.0f;
			this.setHealth(this.getMaxHealth());
		}

		protected Base(EntityLivingBase summoner, float width, float height) {
			super(summoner);
			this.setOGSize(width, height);
			this.stepHeight = 1.0f;
			this.setHealth(this.getMaxHealth());
		}

		protected abstract double summonHealth();
		protected abstract double summonDamage();
		protected abstract double detectionRange();

		@Override
		protected void applyEntityAttributes() {
			super.applyEntityAttributes();
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(this.summonHealth());
			this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(this.summonDamage());
			this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28d);
			this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(this.detectionRange() + 8d);
		}

		@Override
		protected void initEntityAI() {
			super.initEntityAI();
			this.tasks.addTask(1, new EntityAISwimming(this));
			this.tasks.addTask(2, new EntityAIAttackMelee(this, 1.1d, true));
			this.tasks.addTask(3, new AIFollowSummoner(this, 1.15d, 3.0f, 12.0f));
			this.tasks.addTask(4, new EntityAIWander(this, 0.8d));
			this.tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0f));
			this.tasks.addTask(6, new EntityAILookIdle(this));
			this.targetTasks.addTask(2, new AIDetectEnemiesThroughWalls(this));
		}

		@Override
		protected void jump() {
			super.jump();
			this.motionY = Math.max(this.motionY, 0.42d);
		}

		@Override
		public boolean canAttackClass(Class<? extends EntityLivingBase> cls) {
			return !Base.class.isAssignableFrom(cls) && super.canAttackClass(cls);
		}

		@Override
		public boolean isOnSameTeam(Entity entity) {
			return entity instanceof Base || super.isOnSameTeam(entity);
		}

		@Override
		public void setAttackTarget(EntityLivingBase target) {
			if (target == null || !this.isOnSameTeam(target)) {
				super.setAttackTarget(target);
			}
		}

		@Override
		public void onUpdate() {
			super.onUpdate();
			if (!this.world.isRemote) {
				EntityLivingBase owner = this.getSummoner();
				if (owner != null && this.getDistanceSq(owner) > 1024d) {
					this.setPositionAndUpdate(owner.posX + 1d, owner.posY, owner.posZ + 1d);
					this.getNavigator().clearPath();
				}
			}
		}
	}

	public static class EntityScout extends Base {
		public EntityScout(World world) {
			super(world, 0.9f, 0.7f);
		}

		public EntityScout(EntityLivingBase summoner) {
			super(summoner, 0.9f, 0.7f);
		}

		@Override protected double summonHealth() { return 20d; }
		@Override protected double summonDamage() { return 3d; }
		@Override protected double detectionRange() { return 8d; }
		@Override public boolean canSitOnShoulder() { return true; }
	}

	public static class EntityMiniGamakichi extends Base {
		public EntityMiniGamakichi(World world) {
			super(world, 1.35f, 1.05f);
		}

		public EntityMiniGamakichi(EntityLivingBase summoner) {
			super(summoner, 1.35f, 1.05f);
		}

		@Override protected double summonHealth() { return 40d; }
		@Override protected double summonDamage() { return 6d; }
		@Override protected double detectionRange() { return 12d; }
	}

	private static class AIFollowSummoner extends EntityAIBase {
		private final Base toad;
		private final PathNavigate navigator;
		private final double speed;
		private final float startDistance;
		private final float teleportDistance;
		private EntityLivingBase owner;
		private int recalcTimer;

		AIFollowSummoner(Base toadIn, double speedIn, float startDistanceIn, float teleportDistanceIn) {
			this.toad = toadIn;
			this.navigator = toadIn.getNavigator();
			this.speed = speedIn;
			this.startDistance = startDistanceIn;
			this.teleportDistance = teleportDistanceIn;
			this.setMutexBits(3);
		}

		@Override
		public boolean shouldExecute() {
			this.owner = this.toad.getSummoner();
			return this.owner != null && this.toad.getAttackTarget() == null
			 && this.toad.getDistanceSq(this.owner) > this.startDistance * this.startDistance;
		}

		@Override
		public boolean shouldContinueExecuting() {
			return this.owner != null && this.owner.isEntityAlive() && this.toad.getAttackTarget() == null
			 && this.toad.getDistanceSq(this.owner) > 4d;
		}

		@Override
		public void startExecuting() {
			this.recalcTimer = 0;
		}

		@Override
		public void resetTask() {
			this.owner = null;
			this.navigator.clearPath();
		}

		@Override
		public void updateTask() {
			if (this.owner == null || --this.recalcTimer > 0) {
				return;
			}
			this.recalcTimer = 10;
			double distanceSq = this.toad.getDistanceSq(this.owner);
			if (distanceSq > this.teleportDistance * this.teleportDistance) {
				this.toad.setPositionAndUpdate(this.owner.posX + 1d, this.owner.posY, this.owner.posZ + 1d);
				this.navigator.clearPath();
			} else {
				this.navigator.tryMoveToEntityLiving(this.owner, this.speed);
			}
		}
	}

	private static class AIDetectEnemiesThroughWalls extends EntityAIBase {
		private final Base toad;
		private EntityLivingBase target;

		AIDetectEnemiesThroughWalls(Base toadIn) {
			this.toad = toadIn;
			this.setMutexBits(1);
		}

		@Override
		public boolean shouldExecute() {
			if (this.toad.ticksExisted % 10 != 0 || this.toad.getAttackTarget() != null) {
				return false;
			}
			EntityLivingBase owner = this.toad.getSummoner();
			double range = this.toad.detectionRange();
			List<EntityLivingBase> candidates = this.toad.world.getEntitiesWithinAABB(EntityLivingBase.class,
			 this.toad.getEntityBoundingBox().grow(range), entity -> entity != null && entity.isEntityAlive()
			  && entity != this.toad && entity != owner && !this.toad.isOnSameTeam(entity)
			  && (entity instanceof IMob || owner != null && (entity == owner.getRevengeTarget() || entity == owner.getLastAttackedEntity())));
			if (candidates.isEmpty()) {
				return false;
			}
			candidates.sort(Comparator.comparingDouble(this.toad::getDistanceSq));
			this.target = candidates.get(0);
			return true;
		}

		@Override
		public void startExecuting() {
			this.toad.setAttackTarget(this.target);
		}
	}

	@SideOnly(Side.CLIENT)
	private static class RenderSmallToad<T extends Base> extends RenderLiving<T> {
		private final ResourceLocation texture;

		RenderSmallToad(RenderManager manager, ModelSmallToad model, ResourceLocation textureIn, float shadow) {
			super(manager, model, shadow);
			this.texture = textureIn;
		}

		@Override
		protected ResourceLocation getEntityTexture(T entity) {
			return this.texture;
		}
	}

	@SideOnly(Side.CLIENT)
	public static class ModelSmallToad extends EntityToad.ModelToad {
		private final ModelRenderer backTanto;
		private final boolean gamakichi;

		public ModelSmallToad(boolean gamakichiIn) {
			super();
			this.gamakichi = gamakichiIn;
			this.scale = gamakichiIn ? 0.92f : 0.62f;
			this.blade.showModel = false;

			// Gamabunta's full Blockbench rig is retained. Mini Gamakichi replaces the
			// hand-held sword with a short tanto carried diagonally across his back.
			this.backTanto = new ModelRenderer(this);
			this.backTanto.setRotationPoint(0f, -1.2f, 5.4f);
			this.setRotationAngle(this.backTanto, 0.15f, 0f, -0.6109f);
			this.backTanto.cubeList.add(new ModelBox(this.backTanto, 0, 62, -4f, -0.5f, -0.5f,
			 8, 1, 1, 0.12f, false));
			this.backTanto.cubeList.add(new ModelBox(this.backTanto, 18, 60, 3.35f, -1.25f, -0.5f,
			 1, 3, 1, 0.08f, false));
			this.backTanto.showModel = gamakichiIn;
			this.body.addChild(this.backTanto);
		}

		@Override
		public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
		 float netHeadYaw, float headPitch, float scaleFactor) {
			GlStateManager.pushMatrix();
			if (entity.onGround) {
				GlStateManager.translate(0f, MathHelper.sin(ageInTicks * 0.12f) * 0.10f, 0f);
			}
			super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
			GlStateManager.popMatrix();
		}

		@Override
		public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
		 float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
			this.head.rotationPointY = 11.58f;
			this.body.rotationPointY = 11.58f;
			this.head.rotateAngleY = netHeadYaw * 0.017453292f;
			this.head.rotateAngleX = headPitch * 0.017453292f;
			this.body.rotateAngleX = 0f;
			this.body.rotateAngleY = 0f;
			this.jaw.rotateAngleX = this.swingProgress > 0f ? 0.35f : 0.0873f;

			float eyeSway = MathHelper.sin(ageInTicks * 0.08f) * 0.06f;
			this.browRight.rotateAngleZ = 0.5672f + eyeSway;
			this.browLeft.rotateAngleZ = -0.5672f - eyeSway;

			float walk = MathHelper.cos(limbSwing * 0.6662f) * limbSwingAmount;
			this.armRight.rotateAngleX = -0.5236f + walk * 0.75f;
			this.armLeft.rotateAngleX = -0.5236f - walk * 0.75f;
			this.armRight.rotateAngleZ = 0.3491f;
			this.armLeft.rotateAngleZ = -0.3491f;
			this.forearmRight.rotateAngleZ = -0.5236f;
			this.forearmLeft.rotateAngleZ = 0.5236f;
			this.legRight.rotateAngleX = 0.2618f - walk * 0.55f;
			this.legLeft.rotateAngleX = 0.2618f + walk * 0.55f;
			this.legLowerRight.rotateAngleX = -0.5236f;
			this.legLowerLeft.rotateAngleX = -0.5236f;
			this.footRight.rotateAngleX = 0.2182f;
			this.footLeft.rotateAngleX = 0.2182f;

			if (this.swingProgress > 0f) {
				float attack = MathHelper.sin(this.swingProgress * (float)Math.PI);
				this.armRight.rotateAngleX -= attack * 1.4f;
				this.body.rotateAngleY = attack * 0.12f;
			}

			if (!entity.onGround) {
				this.body.rotateAngleX = -0.2618f;
				this.head.rotateAngleX -= 0.2618f;
				this.body.rotationPointY += 0.8f;
				this.head.rotationPointY += 0.8f;
				this.legRight.rotateAngleX = 1.35f;
				this.legLeft.rotateAngleX = 1.35f;
				this.legLowerRight.rotateAngleX = -1.65f;
				this.legLowerLeft.rotateAngleX = -1.65f;
				this.footRight.rotateAngleX = 1.10f;
				this.footLeft.rotateAngleX = 1.10f;
			}
		}
	}
}
