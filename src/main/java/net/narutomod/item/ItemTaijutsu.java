package net.narutomod.item;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.Particles;
import net.narutomod.PlayerStats;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.procedure.ProcedureAoeCommand;
import net.narutomod.procedure.ProcedureOnLeftClickEmpty;
import net.narutomod.procedure.ProcedureUtils;

import java.util.List;

@ElementsNarutomodMod.ModElement.Tag
public class ItemTaijutsu extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:taijutsu")
	public static final Item block = null;

	public static final ItemJutsu.JutsuEnum LEAF_WHIRLWIND = new ItemJutsu.JutsuEnum(0, "leaf_whirlwind", 'D', 35d, new StrikeJutsu("leaf_whirlwind", 4.0d, 3.0f, 55, 1.0f, 0.6d, 0.18d, 0x80D8FF70, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP)).withCustomBalance();
	public static final ItemJutsu.JutsuEnum LEAF_HURRICANE = new ItemJutsu.JutsuEnum(1, "leaf_hurricane", 'C', 55d, new HurricaneJutsu()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum DYNAMIC_ENTRY = new ItemJutsu.JutsuEnum(2, "dynamic_entry", 'C', 65d, new DynamicEntryJutsu()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum PRIMARY_LOTUS = new ItemJutsu.JutsuEnum(3, "primary_lotus", 'B', 95d, new PrimaryLotusJutsu()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum LION_COMBO = new ItemJutsu.JutsuEnum(4, "lion_combo", 'B', 85d, new LionComboJutsu()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum PEREGRINE_FALCON_DROP = new ItemJutsu.JutsuEnum(5, "peregrine_falcon_drop", 'B', 95d, new PeregrineFalconDropJutsu()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum DRUNKEN_FIST = new ItemJutsu.JutsuEnum(6, "drunken_fist", 'C', 75d, new DrunkenFistJutsu()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum LEAF_DROP = new ItemJutsu.JutsuEnum(7, "leaf_drop", 'C', 65d, new LeafDropJutsu()).withCustomBalance();
	private static final String DRUNKEN_FIST_UNTIL = "CanonicalDrunkenFistUntil";

	public ItemTaijutsu(ElementsNarutomodMod instance) {
		super(instance, 1012);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new RangedItem());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(block, 0, new ModelResourceLocation("narutomod:taijutsu", "inventory"));
	}

	@Override
	public void init(FMLInitializationEvent event) {
		ProcedureOnLeftClickEmpty.addQualifiedItem(block, EnumHand.MAIN_HAND);
		ProcedureOnLeftClickEmpty.addQualifiedItem(block, EnumHand.OFF_HAND);
		MinecraftForge.EVENT_BUS.register(new CombatHooks());
		// The beta's improved combat presentation is server-driven so every
		// connected player sees the same dash, lift, strikes and impact.
		CinematicTaijutsu.register();
	}

	public static class RangedItem extends ItemJutsu.Base {
		public RangedItem() {
			super(ItemJutsu.JutsuEnum.Type.TAIJUTSU, LEAF_WHIRLWIND, LEAF_HURRICANE, DYNAMIC_ENTRY, PRIMARY_LOTUS, LION_COMBO,
			 PEREGRINE_FALCON_DROP, DRUNKEN_FIST, LEAF_DROP);
			this.setUnlocalizedName("taijutsu");
			this.setRegistryName("taijutsu");
			this.setCreativeTab(TabCustomTabs.jutsus);
		}

		@Override
		public void onUpdate(ItemStack itemstack, World world, Entity entity, int slot, boolean selected) {
			super.onUpdate(itemstack, world, entity, slot, selected);
			if (!world.isRemote && entity instanceof EntityLivingBase && this.isOwner(itemstack, (EntityLivingBase)entity)
			 && !this.isAnyJutsuEnabled(itemstack)) {
				this.enableAllJutsus(itemstack, true);
			}
		}

		@SideOnly(Side.CLIENT)
		@Override
		public void addInformation(ItemStack itemstack, World world, List<String> list, ITooltipFlag flag) {
			super.addInformation(itemstack, world, list, flag);
			list.add(TextFormatting.YELLOW + I18n.format("tooltip.narutomod.taijutsu.stamina"));
		}
	}

	private static class StrikeJutsu implements ItemJutsu.IJutsuCallback {
		protected final String effectName;
		protected final double range;
		protected final float baseDamage;
		protected final long cooldown;
		protected final float exhaustion;
		protected final double forward;
		protected final double lift;
		protected final int color;
		protected final SoundEvent sound;

		StrikeJutsu(String effectNameIn, double range, float baseDamage, long cooldown, float exhaustion, double forward, double lift, int color, SoundEvent sound) {
			this.effectName = effectNameIn;
			this.range = range;
			this.baseDamage = baseDamage;
			this.cooldown = cooldown;
			this.exhaustion = exhaustion;
			this.forward = forward;
			this.lift = lift;
			this.color = color;
			this.sound = sound;
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (entity.world.isRemote || !(entity instanceof EntityPlayer)) {
				return false;
			}
			EntityPlayer player = (EntityPlayer)entity;
			EntityLivingBase target = this.getTarget(entity, this.range + power);
			if (CinematicTaijutsu.isCinematic(this.effectName)) {
				entity.swingArm(EnumHand.MAIN_HAND);
				entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, this.sound,
				 SoundCategory.PLAYERS, target != null ? 1.2f : 0.7f, target != null ? 0.85f : 1.2f);
				CinematicTaijutsu.start(this.effectName, entity, target, power, this.damage(entity, power));
				this.cooldown(stack, entity);
				return true;
			}
			if (target == null) {
				this.dash(entity, power * 0.45d);
				this.fx(entity, null, power, false);
				this.cooldown(stack, entity);
				return true;
			}
			float damage = this.damage(entity, power);
			target.hurtResistantTime = 0;
			target.attackEntityFrom(this.source(entity), damage);
			this.push(entity, target, power);
			this.fx(entity, target, power, true);
			this.cooldown(stack, entity);
			return true;
		}

		protected EntityLivingBase getTarget(EntityLivingBase entity, double range) {
			RayTraceResult hit = ProcedureUtils.objectEntityLookingAt(entity, range, 1.4d);
			if (hit != null && hit.entityHit instanceof EntityLivingBase && !hit.entityHit.equals(entity)) {
				return (EntityLivingBase)hit.entityHit;
			}
			Vec3d eye = entity.getPositionEyes(1.0f);
			Vec3d look = entity.getLookVec().normalize();
			EntityLivingBase best = null;
			double bestScore = 0.0d;
			for (EntityLivingBase target : entity.world.getEntitiesWithinAABB(EntityLivingBase.class, entity.getEntityBoundingBox().grow(range, 2.0d, range))) {
				if (target.equals(entity) || !target.isEntityAlive()) {
					continue;
				}
				Vec3d center = target.getPositionVector().addVector(0d, target.height * 0.5d, 0d);
				Vec3d toTarget = center.subtract(eye);
				double distance = toTarget.lengthVector();
				if (distance > range || distance <= 0.001d) {
					continue;
				}
				double dot = toTarget.normalize().dotProduct(look);
				if (dot < 0.2d && distance > 2.2d) {
					continue;
				}
				double score = (distance <= 2.2d ? 1.0d : dot) / Math.max(1.0d, distance);
				if (score > bestScore) {
					best = target;
					bestScore = score;
				}
			}
			return best;
		}

		protected DamageSource source(EntityLivingBase entity) {
			return entity instanceof EntityPlayer ? new EntityDamageSource("narutomod.taijutsu", entity) : DamageSource.causeMobDamage(entity);
		}

		protected float damage(EntityLivingBase entity, float power) {
			float stats = entity instanceof EntityPlayer ? (float)PlayerStats.getTaijutsuDamageBonus((EntityPlayer)entity) : 0f;
			return (this.baseDamage + stats) * (0.75f + 0.25f * power);
		}

		protected void push(EntityLivingBase entity, EntityLivingBase target, float power) {
			Vec3d look = entity.getLookVec().normalize();
			ProcedureUtils.addVelocity(target, look.x * this.forward * power, this.lift * power, look.z * this.forward * power);
		}

		protected void dash(EntityLivingBase entity, double scale) {
			Vec3d look = entity.getLookVec().normalize();
			ProcedureUtils.addVelocity(entity, look.x * scale, Math.max(0.05d, look.y * scale * 0.25d), look.z * scale);
		}

		protected void fx(EntityLivingBase entity, EntityLivingBase target, float power, boolean hit) {
			entity.swingArm(EnumHand.MAIN_HAND);
			CinematicTaijutsu.flash(this.effectName, entity);
			entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, this.sound, SoundCategory.PLAYERS, hit ? 1.2f : 0.7f, hit ? 0.85f : 1.2f);
			if (hit) {
				entity.world.playSound(null, target.posX, target.posY, target.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.25f, 1.8f);
				entity.world.playSound(null, target.posX, target.posY, target.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.7f);
			}
			CustomJutsuEffects.taijutsu(this.effectName, entity, target, power, hit);
		}

		protected void cooldown(ItemStack stack, EntityLivingBase entity) {
			if (stack.getItem() instanceof ItemJutsu.Base) {
				((ItemJutsu.Base)stack.getItem()).setCurrentJutsuCooldown(stack, this.cooldown);
			}
		}

		@Override public float getBasePower() { return 0.8f; }
		@Override public float getPowerupDelay() { return 18.0f; }
		@Override public float getMaxPower() { return 1.4f; }
	}

	private static class HurricaneJutsu extends StrikeJutsu {
		HurricaneJutsu() {
			super("leaf_hurricane", 4.5d, 4.5f, 80, 1.4f, 0.95d, 0.35d, 0x90B8FF70, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			boolean used = super.createJutsu(stack, entity, power);
			if (used) {
				ProcedureAoeCommand.set(entity, 0d, 3.2d).exclude(entity)
				 .damageEntities(this.source(entity), this.damage(entity, power) * 0.45f).knockback(0.7f);
			}
			return used;
		}
	}

	private static class DynamicEntryJutsu extends StrikeJutsu {
		DynamicEntryJutsu() {
			super("dynamic_entry", 8.0d, 6.0f, 120, 2.0f, 1.45d, 0.25d, 0x90FFFFFF, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!CinematicTaijutsu.isCinematic(this.effectName)) {
				this.dash(entity, 1.2d * power);
			}
			return super.createJutsu(stack, entity, power);
		}

		@Override public float getPowerupDelay() { return 26.0f; }
		@Override public float getMaxPower() { return 1.6f; }
	}

	private static class PrimaryLotusJutsu extends StrikeJutsu {
		PrimaryLotusJutsu() {
			super("primary_lotus", 3.6d, 8.0f, 220, 3.5f, 0.55d, 0.9d, 0xA0E0E0E0, SoundEvents.ENTITY_GENERIC_EXPLODE);
		}

		@Override
		protected void push(EntityLivingBase entity, EntityLivingBase target, float power) {
			Vec3d look = entity.getLookVec().normalize();
			ProcedureUtils.addVelocity(target, look.x * 0.35d, 1.05d * power, look.z * 0.35d);
			target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 45, 1, false, false));
		}

		@Override
		protected float damage(EntityLivingBase entity, float power) {
			return super.damage(entity, power) + 2.5f * power;
		}

		@Override public float getPowerupDelay() { return 40.0f; }
		@Override public float getMaxPower() { return 1.8f; }
	}

	private static class LionComboJutsu extends StrikeJutsu {
		LionComboJutsu() {
			super("lion_combo", 4.0d, 7.0f, 180, 2.8f, 0.75d, 0.55d, 0x90FFD070, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			EntityLivingBase target = this.getTarget(entity, this.range + power);
			boolean used = super.createJutsu(stack, entity, power);
			if (used && target != null && !CinematicTaijutsu.isCinematic(this.effectName)) {
				target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0, false, false));
				entity.world.playSound(null, target.posX, target.posY, target.posZ,
				 SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:throwpunch")), SoundCategory.PLAYERS, 1.0f, 1.1f);
				for (int i = 0; i < 18; i++) {
					entity.world.spawnParticle(EnumParticleTypes.CLOUD, target.posX + (entity.getRNG().nextDouble() - 0.5d),
					 target.posY + 0.5d + entity.getRNG().nextDouble(), target.posZ + (entity.getRNG().nextDouble() - 0.5d), 0d, 0.04d, 0d);
				}
			}
			return used;
		}

		@Override public float getPowerupDelay() { return 32.0f; }
		@Override public float getMaxPower() { return 1.7f; }
	}

	private static class PeregrineFalconDropJutsu extends StrikeJutsu {
		PeregrineFalconDropJutsu() {
			super("peregrine_falcon_drop", 5.0d, 9.0f, 320, 3.0f, 0.2d, -1.2d,
			 0xC0E8E8E8, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (entity.world.isRemote || !(entity instanceof EntityPlayer)) return false;
			EntityLivingBase target = this.getTarget(entity, this.range + power);
			if (target == null || (target.onGround && entity.onGround && target.posY >= entity.posY - 0.5d)) {
				((EntityPlayer)entity).sendStatusMessage(new net.minecraft.util.text.TextComponentString("Launch the target or attack from above first."), true);
				return false;
			}
			entity.setPositionAndUpdate(target.posX, target.posY + target.height + 0.35d, target.posZ);
			ProcedureUtils.setVelocity(entity, 0d, -1.35d, 0d);
			ProcedureUtils.setVelocity(target, 0d, -1.1d, 0d);
			target.hurtResistantTime = 0;
			target.attackEntityFrom(this.source(entity), this.damage(entity, power) + 3.0f * power);
			target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 55, 2, false, false));
			this.fx(entity, target, power, true);
			CustomJutsuEffects.impact(entity.world, target.getPositionVector(), 0xA0D8D0C8, 4.2f, 15, 4.0f);
			entity.world.playSound(null, target.posX, target.posY, target.posZ,
			 SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.9f, 0.62f);
			this.cooldown(stack, entity);
			return true;
		}

		@Override public float getPowerupDelay() { return 34.0f; }
		@Override public float getMaxPower() { return 1.8f; }
	}

	private static class DrunkenFistJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (entity.world.isRemote) return false;
			int duration = 180 + (int)(power * 25f);
			entity.getEntityData().setLong(DRUNKEN_FIST_UNTIL, entity.world.getTotalWorldTime() + duration);
			entity.addPotionEffect(new PotionEffect(MobEffects.SPEED, duration, 1, false, false));
			entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, duration, 0, false, false));
			entity.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, duration, 0, false, false));
			entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
			 SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0f, 0.72f);
			entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
			 SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8f, 0.55f);
			CustomJutsuEffects.taijutsu("drunken_fist", entity, null, power, false);
			if (stack.getItem() instanceof ItemJutsu.Base) ((ItemJutsu.Base)stack.getItem()).setCurrentJutsuCooldown(stack, 700);
			return true;
		}
		@Override public float getBasePower() { return 1.0f; }
		@Override public float getPowerupDelay() { return 18f; }
		@Override public float getMaxPower() { return 1.6f; }
	}

	private static class LeafDropJutsu extends StrikeJutsu {
		LeafDropJutsu() {
			super("leaf_drop", 5.5d, 7.0f, 240, 2.4f, 0.45d, -1.0d,
			 0xB0D8C898, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (entity.world.isRemote) return false;
			EntityLivingBase direct = this.getTarget(entity, this.range + power);
			if (entity.onGround) ProcedureUtils.addVelocity(entity, 0d, 0.28d, 0d);
			ProcedureUtils.addVelocity(entity, entity.getLookVec().x * 0.25d, -1.15d, entity.getLookVec().z * 0.25d);
			Vec3d impact = direct != null ? direct.getPositionVector() : entity.getPositionVector().add(entity.getLookVec().scale(2.2d));
			if (direct != null) {
				direct.hurtResistantTime = 0;
				direct.attackEntityFrom(this.source(entity), this.damage(entity, power) + 2f * power);
				ProcedureUtils.addVelocity(direct, 0d, -0.4d, 0d);
			}
			for (EntityLivingBase target : entity.world.getEntitiesWithinAABB(EntityLivingBase.class,
			 new net.minecraft.util.math.AxisAlignedBB(impact.x - 3d, impact.y - 1.5d, impact.z - 3d,
			 impact.x + 3d, impact.y + 2d, impact.z + 3d))) {
				if (target.equals(entity) || target.equals(direct) || entity.isOnSameTeam(target)) continue;
				target.attackEntityFrom(this.source(entity), this.damage(entity, power) * 0.35f);
				Vec3d away = target.getPositionVector().subtract(impact).normalize();
				ProcedureUtils.addVelocity(target, away.x * 0.55d, 0.25d, away.z * 0.55d);
			}
			this.fx(entity, direct, power, direct != null);
			CustomJutsuEffects.impact(entity.world, impact, 0xA0A08868, 4.6f, 13, 3.3f);
			entity.world.playSound(null, impact.x, impact.y, impact.z,
			 SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.75f, 0.68f);
			this.cooldown(stack, entity);
			return true;
		}

		@Override public float getPowerupDelay() { return 28f; }
		@Override public float getMaxPower() { return 1.7f; }
	}

	/** Drunken Fist improves committed attacks but never grants passive auto-dodge. */
	public static class CombatHooks {
		@SubscribeEvent
		public void onLivingHurt(LivingHurtEvent event) {
			if (!(event.getSource().getTrueSource() instanceof EntityLivingBase)) return;
			EntityLivingBase attacker = (EntityLivingBase)event.getSource().getTrueSource();
			NBTTagCompound data = attacker.getEntityData();
			if (data.getLong(DRUNKEN_FIST_UNTIL) < attacker.world.getTotalWorldTime()) return;
			event.setAmount(event.getAmount() * 1.12f);
			Vec3d side = new Vec3d(-attacker.getLookVec().z, 0d, attacker.getLookVec().x);
			double sway = (attacker.getRNG().nextDouble() - 0.5d) * 0.38d;
			ProcedureUtils.addVelocity(attacker, side.x * sway, 0.03d, side.z * sway);
			CustomJutsuEffects.taijutsu("drunken_fist_hit", attacker, event.getEntityLiving(), 1.0f, true);
		}
	}
}
