package net.narutomod.item;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.narutomod.Particles;
import net.narutomod.potion.PotionParalysis;
import net.narutomod.procedure.ProcedureCameraShake;
import net.narutomod.procedure.ProcedureUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Canon Naruto techniques selected for the RP expansion.  The callbacks keep
 * targeting server-authoritative while sharing one visual language so every
 * cast is readable in a crowded fight.
 */
public final class ItemCanonicalJutsu {
	private static final String WATER_MIRROR_UNTIL = "CanonicalWaterMirrorUntil";
	private static final String WATER_MIRROR_CHARGES = "CanonicalWaterMirrorCharges";

	private ItemCanonicalJutsu() { }

	private static EntityLivingBase target(EntityLivingBase caster, double range, double grow) {
		RayTraceResult hit = ProcedureUtils.objectEntityLookingAt(caster, range, grow);
		return hit != null && hit.entityHit instanceof EntityLivingBase && ItemJutsu.canTarget(hit.entityHit)
		 && !hit.entityHit.equals(caster) ? (EntityLivingBase)hit.entityHit : null;
	}

	private static boolean hostile(EntityLivingBase caster, EntityLivingBase other) {
		return other != null && !other.equals(caster) && ItemJutsu.canTarget(other) && !caster.isOnSameTeam(other);
	}

	private static List<EntityLivingBase> nearbyHostiles(EntityLivingBase caster, Vec3d center, double radius) {
		AxisAlignedBB box = new AxisAlignedBB(center.x - radius, center.y - radius, center.z - radius,
		 center.x + radius, center.y + radius, center.z + radius);
		List<EntityLivingBase> result = new ArrayList<>();
		for (EntityLivingBase living : caster.world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
			if (hostile(caster, living) && living.getPositionVector().distanceTo(center) <= radius) result.add(living);
		}
		result.sort(Comparator.comparingDouble(e -> e.getDistanceSq(center.x, center.y, center.z)));
		return result;
	}

	private static DamageSource source(EntityLivingBase caster) {
		return ItemJutsu.causeJutsuDamage(caster, caster);
	}

	private static void cooldown(ItemStack stack, long ticks) {
		if (stack.getItem() instanceof ItemJutsu.Base) {
			((ItemJutsu.Base)stack.getItem()).setCurrentJutsuCooldown(stack, ticks);
		}
	}

	private static void sound(EntityLivingBase caster, net.minecraft.util.SoundEvent sound, float volume, float pitch) {
		caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, sound, SoundCategory.PLAYERS, volume, pitch);
	}

	private static void line(EntityLivingBase caster, Vec3d from, Vec3d to, int color, int count) {
		Vec3d delta = to.subtract(from);
		for (int i = 0; i <= count; i++) {
			Vec3d p = from.add(delta.scale((double)i / (double)Math.max(1, count)));
			Particles.spawnParticle(caster.world, Particles.Types.SMOKE, p.x, p.y, p.z, 2,
			 0.08d, 0.08d, 0.08d, 0d, 0.01d, 0d, color, 14, 3, 0xF0);
		}
	}

	private static void waterBurst(World world, Vec3d p, double radius, int color) {
		Particles.spawnParticle(world, Particles.Types.WATER_SPLASH, p.x, p.y, p.z, 70,
		 radius, radius * 0.55d, radius, 0d, 0.12d, 0d, color, 28);
		Particles.spawnParticle(world, Particles.Types.EXPANDING_SPHERE, p.x, p.y, p.z, 1,
		 0d, 0d, 0d, 0d, 0d, 0d, Math.max(16, (int)(radius * 12d)), 12, color);
	}

	private static void vanilla(World world, EnumParticleTypes type, Vec3d p, int count, double spread, double speed) {
		if (world instanceof WorldServer) {
			((WorldServer)world).spawnParticle(type, p.x, p.y, p.z, count, spread, spread, spread, speed);
		}
	}

	private static void shake(World world, Vec3d p, double radius, int ticks, float strength) {
		ProcedureCameraShake.sendToClients(world.provider.getDimension(), p.x, p.y, p.z, radius, ticks, strength);
	}

	private abstract static class Charged implements ItemJutsu.IJutsuCallback {
		private final float max;
		private final float delay;
		Charged(float maxIn, float delayIn) { this.max = maxIn; this.delay = delayIn; }
		@Override public float getBasePower() { return 1.0f; }
		@Override public float getPowerupDelay() { return this.delay; }
		@Override public float getMaxPower() { return this.max; }
	}

	/** Darui-style conductive wave: simple aim, then short chain arcs. */
	public static class WaveOfInspiration extends Charged {
		public WaveOfInspiration() { super(2.2f, 24f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			EntityLivingBase first = target(caster, 14d + power * 2d, 2.0d);
			if (first == null) {
				if (caster instanceof EntityPlayer) ((EntityPlayer)caster).sendStatusMessage(new TextComponentString("No target in the conductive wave."), true);
				return false;
			}
			List<EntityLivingBase> struck = new ArrayList<>();
			struck.add(first);
			Vec3d last = caster.getPositionEyes(1f);
			EntityLivingBase current = first;
			for (int jump = 0; jump < 4 && current != null; jump++) {
				line(caster, last, current.getPositionEyes(1f), 0xB070D8FF, 10);
				current.hurtResistantTime = 0;
				current.attackEntityFrom(source(caster), (5.0f + power * 1.8f) * (1f - jump * 0.16f));
				current.addPotionEffect(new PotionEffect(PotionParalysis.potion, 16 + (int)(power * 5f), 0, false, false));
				last = current.getPositionEyes(1f);
				EntityLivingBase next = null;
				for (EntityLivingBase candidate : nearbyHostiles(caster, current.getPositionVector(), 5.5d)) {
					if (!struck.contains(candidate)) { next = candidate; break; }
				}
				current = next;
				if (current != null) struck.add(current);
			}
			sound(caster, SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.55f, 1.75f);
			vanilla(caster.world, EnumParticleTypes.CRIT_MAGIC, first.getPositionEyes(1f), 35, 0.55d, 0.18d);
			cooldown(stack, 260);
			return true;
		}
	}

	/** Four lightning columns converge on the aimed target and bind movement. */
	public static class FourPillarBind extends Charged {
		public FourPillarBind() { super(2.0f, 32f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			EntityLivingBase victim = target(caster, 24d, 2.5d);
			if (victim == null) return false;
			Vec3d center = victim.getPositionVector();
			for (int i = 0; i < 4; i++) {
				double a = Math.PI * 0.5d * i;
				Vec3d base = center.addVector(Math.cos(a) * 2d, 0d, Math.sin(a) * 2d);
				line(caster, base, base.addVector(0d, 4.5d, 0d), 0xC080E8FF, 13);
				line(caster, base.addVector(0d, 2.5d, 0d), center.addVector(0d, 1d, 0d), 0xD0B8F4FF, 8);
			}
			victim.attackEntityFrom(source(caster), 7f + power * 2f);
			victim.addPotionEffect(new PotionEffect(PotionParalysis.potion, 70 + (int)(power * 20f), 1, false, false));
			victim.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 80, 3, false, false));
			sound(caster, SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.8f, 1.25f);
			shake(caster.world, center, 20d, 9, 1.2f);
			cooldown(stack, 520);
			return true;
		}
	}

	public static class WaterWhip extends Charged {
		public WaterWhip() { super(2.0f, 22f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			EntityLivingBase victim = target(caster, 12d + power, 2d);
			if (victim == null) return false;
			line(caster, caster.getPositionEyes(1f).addVector(0d, -0.25d, 0d), victim.getPositionEyes(1f), 0xC050BFFF, 18);
			victim.attackEntityFrom(source(caster), 4f + power * 1.5f);
			victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 45 + (int)(power * 10f), 2, false, false));
			Vec3d pull = caster.getPositionVector().subtract(victim.getPositionVector()).normalize();
			ProcedureUtils.addVelocity(victim, pull.x * 0.8d, 0.18d, pull.z * 0.8d);
			waterBurst(caster.world, victim.getPositionEyes(1f), 0.8d, 0xC060C8FF);
			sound(caster, SoundEvents.ENTITY_PLAYER_SPLASH, 1.2f, 1.05f);
			cooldown(stack, 180);
			return true;
		}
	}

	public static class HidingInWater extends Charged {
		public HidingInWater() { super(1.0f, 12f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			if (!caster.isInWater() && !caster.world.containsAnyLiquid(caster.getEntityBoundingBox().grow(1.25d))) {
				if (caster instanceof EntityPlayer) ((EntityPlayer)caster).sendStatusMessage(new TextComponentString("You need nearby water to hide within it."), true);
				return false;
			}
			caster.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 180, 0, false, false));
			caster.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 220, 0, false, false));
			caster.addPotionEffect(new PotionEffect(MobEffects.SPEED, 180, 1, false, false));
			waterBurst(caster.world, caster.getPositionVector().addVector(0d, 0.8d, 0d), 1.2d, 0xA040A8D8);
			sound(caster, SoundEvents.ENTITY_PLAYER_SWIM, 1.0f, 0.75f);
			cooldown(stack, 300);
			return true;
		}
	}

	public static class WaterMirror extends Charged {
		public WaterMirror() { super(1.0f, 20f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			NBTTagCompound data = caster.getEntityData();
			data.setLong(WATER_MIRROR_UNTIL, caster.world.getTotalWorldTime() + 45L);
			data.setInteger(WATER_MIRROR_CHARGES, 1);
			caster.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 45, 1, false, false));
			Vec3d front = caster.getPositionEyes(1f).add(caster.getLookVec().scale(1.4d));
			waterBurst(caster.world, front, 1.8d, 0xB070D8FF);
			sound(caster, SoundEvents.BLOCK_GLASS_PLACE, 1.0f, 0.65f);
			cooldown(stack, 760);
			return true;
		}
	}

	public static class WaterBlade extends Charged {
		public WaterBlade() { super(2.2f, 20f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			boolean thrown = caster.isSneaking();
			double range = thrown ? 18d : 5d;
			EntityLivingBase victim = target(caster, range, thrown ? 1.4d : 2.0d);
			Vec3d start = caster.getPositionEyes(1f).addVector(0d, -0.25d, 0d);
			Vec3d end = victim != null ? victim.getPositionEyes(1f) : start.add(caster.getLookVec().scale(range));
			line(caster, start, end, 0xD070D8FF, thrown ? 28 : 13);
			if (victim != null) {
				victim.hurtResistantTime = 0;
				victim.attackEntityFrom(source(caster), (thrown ? 9f : 6f) + power * 2.2f);
				ProcedureUtils.addVelocity(victim, caster.getLookVec().x * 0.35d, thrown ? 0.08d : 0.22d, caster.getLookVec().z * 0.35d);
				waterBurst(caster.world, end, 0.9d, 0xD080E8FF);
			}
			sound(caster, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.1f, 1.45f);
			sound(caster, SoundEvents.ENTITY_PLAYER_SPLASH, 0.9f, thrown ? 1.25f : 0.85f);
			cooldown(stack, thrown ? 380 : 260);
			return true;
		}
	}

	public static class ExplosiveBubbles extends Charged {
		public ExplosiveBubbles() { super(2.0f, 26f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			List<EntityLivingBase> enemies = nearbyHostiles(caster, caster.getPositionVector().add(caster.getLookVec().scale(5d)), 10d);
			EntityLivingBase aimed = target(caster, 18d, 3d);
			if (aimed != null) { enemies.remove(aimed); enemies.add(0, aimed); }
			int count = Math.min(8, 4 + (int)power * 2);
			for (int i = 0; i < count; i++) {
				EntityLivingBase bubbleTarget = enemies.isEmpty() ? null : enemies.get(i % Math.min(3, enemies.size()));
				ItemExtraJutsu.EntityBubbleBomb bubble = new ItemExtraJutsu.EntityBubbleBomb(caster.world, caster, bubbleTarget, 18 + i * 3, 2.2f + power * 0.75f);
				double a = Math.PI * 2d * i / count;
				bubble.setPosition(caster.posX + Math.cos(a) * 1.2d, caster.posY + 0.7d + (i % 3) * 0.45d, caster.posZ + Math.sin(a) * 1.2d);
				caster.world.spawnEntity(bubble);
			}
			sound(caster, SoundEvents.ENTITY_PLAYER_SPLASH, 1.1f, 1.55f);
			cooldown(stack, 640);
			return true;
		}
	}

	public static class WaterFormationPillar extends Charged {
		public WaterFormationPillar() { super(1.5f, 26f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			Vec3d center = caster.getPositionVector().addVector(0d, 0.8d, 0d);
			caster.extinguish();
			caster.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 55 + (int)(power * 8f), 2, false, false));
			caster.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 90, 0, false, false));
			for (EntityLivingBase enemy : nearbyHostiles(caster, center, 5d)) {
				Vec3d away = enemy.getPositionVector().subtract(center).normalize();
				ProcedureUtils.addVelocity(enemy, away.x * 1.1d, 0.45d, away.z * 1.1d);
				enemy.extinguish();
			}
			for (int ring = 1; ring <= 3; ring++) {
				int points = 16 + ring * 8;
				for (int i = 0; i < points; i++) {
					double a = Math.PI * 2d * i / points;
					Vec3d p = center.addVector(Math.cos(a) * ring * 1.25d, (i % 5) * 0.45d, Math.sin(a) * ring * 1.25d);
					Particles.spawnParticle(caster.world, Particles.Types.WATER_SPLASH, p.x, p.y, p.z, 3,
					 0.12d, 0.35d, 0.12d, 0d, 0.18d, 0d, 0xC060C8FF, 24);
				}
			}
			waterBurst(caster.world, center, 4.5d, 0xA050BFFF);
			sound(caster, SoundEvents.ENTITY_PLAYER_SPLASH, 1.8f, 0.55f);
			shake(caster.world, center, 24d, 8, 1.3f);
			cooldown(stack, 600);
			return true;
		}
	}

	public static class FlameWhirlwind extends Charged {
		public FlameWhirlwind() { super(2.4f, 27f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			Vec3d center = caster.getPositionVector().addVector(0d, 0.9d, 0d);
			for (int i = 0; i < 70; i++) {
				double a = i * 0.48d;
				double r = 0.8d + i * 0.045d;
				Vec3d p = center.addVector(Math.cos(a) * r, (i % 18) * 0.12d, Math.sin(a) * r);
				Particles.spawnParticle(caster.world, Particles.Types.FLAME, p.x, p.y, p.z, 3,
				 0.08d, 0.1d, 0.08d, 0d, 0.09d, 0d, 0xE8FF5A00, 26);
			}
			for (EntityLivingBase enemy : nearbyHostiles(caster, center, 5.5d + power)) {
				enemy.attackEntityFrom(source(caster).setFireDamage(), 7f + power * 2.2f);
				enemy.setFire(5 + (int)power);
				Vec3d away = enemy.getPositionVector().subtract(center).normalize();
				ProcedureUtils.addVelocity(enemy, away.x * 0.8d, 0.35d, away.z * 0.8d);
			}
			sound(caster, SoundEvents.ENTITY_BLAZE_SHOOT, 1.5f, 0.72f);
			shake(caster.world, center, 25d, 10, 1.7f);
			cooldown(stack, 420);
			return true;
		}
	}

	public static class BeastTearingGalePalm extends Charged {
		public BeastTearingGalePalm() { super(2.2f, 21f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			EntityLivingBase victim = target(caster, 18d + power * 2d, 2.4d);
			Vec3d start = caster.getPositionEyes(1f);
			Vec3d end = victim != null ? victim.getPositionEyes(1f) : start.add(caster.getLookVec().scale(12d));
			line(caster, start, end, 0x90D8F8FF, 24);
			if (victim != null) {
				victim.attackEntityFrom(source(caster), 5f + power * 1.7f);
				Vec3d look = caster.getLookVec().normalize();
				ProcedureUtils.addVelocity(victim, look.x * (1.25d + power * 0.35d), 0.38d, look.z * (1.25d + power * 0.35d));
			}
			vanilla(caster.world, EnumParticleTypes.CLOUD, end, 55, 0.9d, 0.14d);
			sound(caster, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.3f, 0.62f);
			cooldown(stack, 200);
			return true;
		}
	}

	public static class FlowerScatteringDance extends Charged {
		public FlowerScatteringDance() { super(2.1f, 26f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			Vec3d center = caster.getPositionVector().add(caster.getLookVec().scale(4d)).addVector(0d, 1d, 0d);
			for (int i = 0; i < 90; i++) {
				double a = i * 0.73d;
				double r = 0.4d + (i % 22) * 0.22d;
				Vec3d p = center.addVector(Math.cos(a) * r, (i % 13) * 0.18d - 0.7d, Math.sin(a) * r);
				Particles.spawnParticle(caster.world, Particles.Types.SMOKE, p.x, p.y, p.z, 2,
				 0.08d, 0.08d, 0.08d, 0d, 0.05d, 0d, i % 2 == 0 ? 0xA0F090B0 : 0xA0FFE0F0, 30, 4, 0xF0);
			}
			for (EntityLivingBase enemy : nearbyHostiles(caster, center, 6d + power)) {
				enemy.hurtResistantTime = 0;
				enemy.attackEntityFrom(source(caster), 6f + power * 1.8f);
				enemy.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 36, 0, false, false));
				ProcedureUtils.addVelocity(enemy, 0d, 0.55d, 0d);
			}
			sound(caster, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.4f, 1.2f);
			shake(caster.world, center, 25d, 7, 1.0f);
			cooldown(stack, 360);
			return true;
		}
	}

	public static class MudWolves extends Charged {
		public MudWolves() { super(2.0f, 28f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			List<EntityLivingBase> enemies = nearbyHostiles(caster, caster.getPositionVector().add(caster.getLookVec().scale(5d)), 11d);
			EntityLivingBase aimed = target(caster, 20d, 3d);
			if (aimed != null) { enemies.remove(aimed); enemies.add(0, aimed); }
			if (enemies.isEmpty()) return false;
			int wolves = Math.min(3, enemies.size());
			for (int i = 0; i < wolves; i++) {
				EntityLivingBase victim = enemies.get(i);
				Vec3d start = caster.getPositionVector().addVector((i - 1) * 1.4d, 0.25d, 0d);
				line(caster, start, victim.getPositionVector().addVector(0d, 0.45d, 0d), 0xC06B4B2A, 18);
				victim.attackEntityFrom(source(caster), 6f + power * 1.8f);
				victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 70, 2, false, false));
				victim.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0, false, false));
				vanilla(caster.world, EnumParticleTypes.BLOCK_DUST, victim.getPositionVector().addVector(0d, 0.5d, 0d), 28, 0.55d, 0.10d);
			}
			sound(caster, SoundEvents.ENTITY_WOLF_GROWL, 1.25f, 0.7f);
			sound(caster, SoundEvents.BLOCK_GRAVEL_BREAK, 1.2f, 0.75f);
			cooldown(stack, 440);
			return true;
		}
	}

	public static class EarthFlowWave extends Charged {
		public EarthFlowWave() { super(2.1f, 22f); }
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (caster.world.isRemote) return false;
			Vec3d look = new Vec3d(caster.getLookVec().x, 0d, caster.getLookVec().z).normalize();
			Vec3d start = caster.getPositionVector();
			for (int step = 1; step <= 8; step++) {
				Vec3d p = start.add(look.scale(step * 1.3d));
				Particles.spawnParticle(caster.world, Particles.Types.SMOKE, p.x, p.y + 0.2d, p.z, 20,
				 0.8d, 0.18d, 0.8d, 0d, 0.08d, 0d, 0xC06B4B2A, 26, 6, 0xF0);
				for (EntityLivingBase enemy : nearbyHostiles(caster, p, 1.8d)) {
					if (enemy.hurtResistantTime <= 5) enemy.attackEntityFrom(source(caster), 3f + power);
					ProcedureUtils.addVelocity(enemy, look.x * 0.8d, 0.4d, look.z * 0.8d);
				}
			}
			ProcedureUtils.addVelocity(caster, look.x * (1.1d + power * 0.25d), 0.18d, look.z * (1.1d + power * 0.25d));
			sound(caster, SoundEvents.BLOCK_GRAVEL_BREAK, 1.5f, 0.55f);
			shake(caster.world, start, 22d, 8, 1.1f);
			cooldown(stack, 260);
			return true;
		}
	}

	/** One-use directional counter for Water Mirror. */
	public static class Hooks {
		@SubscribeEvent
		public void onLivingAttack(LivingAttackEvent event) {
			EntityLivingBase defender = event.getEntityLiving();
			NBTTagCompound data = defender.getEntityData();
			if (data.getInteger(WATER_MIRROR_CHARGES) <= 0
			 || data.getLong(WATER_MIRROR_UNTIL) < defender.world.getTotalWorldTime()
			 || "narutomod.water_mirror".equals(event.getSource().getDamageType())) return;
			Entity attackerEntity = event.getSource().getTrueSource();
			if (!(attackerEntity instanceof EntityLivingBase) || attackerEntity.equals(defender)) return;
			EntityLivingBase attacker = (EntityLivingBase)attackerEntity;
			Vec3d toward = attacker.getPositionEyes(1f).subtract(defender.getPositionEyes(1f)).normalize();
			Vec3d facing = defender.getLookVec().normalize();
			if (facing.dotProduct(toward) < -0.15d) return;
			event.setCanceled(true);
			data.setInteger(WATER_MIRROR_CHARGES, 0);
			float reflected = MathHelper.clamp(event.getAmount() * 0.65f, 2f, 12f);
			attacker.attackEntityFrom(new EntityDamageSource("narutomod.water_mirror", defender), reflected);
			Vec3d impact = defender.getPositionEyes(1f).add(defender.getLookVec().scale(1.2d));
			waterBurst(defender.world, impact, 2.0d, 0xD080E8FF);
			defender.world.playSound(null, defender.posX, defender.posY, defender.posZ,
			 SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2f, 0.72f);
			ProcedureCameraShake.sendToClients(defender.world.provider.getDimension(), impact.x, impact.y, impact.z, 20d, 9, 1.5f);
		}
	}
}
