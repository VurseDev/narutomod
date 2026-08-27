package net.narutomod.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.narutomod.Particles;
import net.narutomod.entity.EntitySweep;
import net.narutomod.procedure.ProcedureCameraShake;

/** Distinct, server-synchronized presentation for the custom jutsu set. */
public final class CustomJutsuEffects {
	private CustomJutsuEffects() { }

	public static void onCast(ItemJutsu.JutsuEnum jutsu, EntityLivingBase caster, float power) {
		if (jutsu == null || caster == null || caster.world.isRemote || !jutsu.usesCustomBalance()
		 || jutsu.getType() == ItemJutsu.JutsuEnum.Type.RAITON
		 || jutsu.getType() == ItemJutsu.JutsuEnum.Type.TAIJUTSU
		 || jutsu.getType() == ItemJutsu.JutsuEnum.Type.INTON) {
			return;
		}
		String name = jutsu.unlocalizedName;
		Vec3d eyes = caster.getPositionEyes(1.0f);
		Vec3d look = caster.getLookVec().normalize();
		if ("ninken_companion".equals(name)) {
			seal(caster.world, caster.getPositionVector(), 42, 34);
			smoke(caster.world, caster.getPositionVector().addVector(0d, 0.7d, 0d), 0xD8F2EEE6, 70, 0.85d, 0.10d, 34, 7);
			vanilla(caster.world, EnumParticleTypes.HEART, caster.posX, caster.posY + 1.0d, caster.posZ, 9, 0.8d, 0.7d, 0.8d, 0.04d);
			caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundEvents.ENTITY_WOLF_SHAKE, SoundCategory.PLAYERS, 1.0f, 0.9f);
		} else if ("retsudo_tensho".equals(name)) {
			Vec3d center = caster.getPositionVector().add(look.scale(6d));
			sphere(caster.world, center, 4.8f, 13, 0x9067442A);
			smoke(caster.world, center.addVector(0d, 0.35d, 0d), 0xC06D4B31, 90, 3.8d, 0.18d, 38, 9);
			sonic(caster.world, center.addVector(0d, 0.4d, 0d), new Vec3d(0d, 0.16d, 0d), 0x506F4BFF, 20, 10);
			shake(caster.world, center, 28d, 11, 2.0f);
		} else if ("fire_phoenix".equals(name)) {
			flame(caster.world, eyes.add(look.scale(0.8d)), 0xE8FF5A00, 55, 0.7d, 0.10d, 26);
			sweep(caster, eyes, caster.rotationYaw - 32f, caster.rotationPitch, -28f, 0xC8FF6A00, 4.2f);
			sweep(caster, eyes, caster.rotationYaw + 32f, caster.rotationPitch, 28f, 0xC8FFB020, 4.2f);
			caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.4f, 0.72f);
		} else if ("housenka".equals(name)) {
			flameCone(caster, 7, 0xD8FF5A00, 0.42d);
			sweep(caster, eyes, caster.rotationYaw, caster.rotationPitch, 0f, 0xB8FF6A00, 2.6f);
			caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.18f);
		} else if ("housenka_tsumabeni".equals(name)) {
			flameCone(caster, 12, 0xE8FF8A10, 0.60d);
			sweep(caster, eyes, caster.rotationYaw - 18f, caster.rotationPitch, -14f, 0xC8FFD050, 3.2f);
			sweep(caster, eyes, caster.rotationYaw + 18f, caster.rotationPitch, 14f, 0xC8FF4A00, 3.2f);
			vanilla(caster.world, EnumParticleTypes.CRIT, eyes.x, eyes.y, eyes.z, 30, 0.65d, 0.45d, 0.65d, 0.14d);
		} else if ("crow_clone".equals(name)) {
			sphere(caster.world, caster.getPositionVector().addVector(0d, 1d, 0d), 3.5f, 16, 0xA018101C);
			smoke(caster.world, caster.getPositionVector().addVector(0d, 1d, 0d), 0xE0100C16, 100, 1.5d, 0.13d, 45, 10);
			vanilla(caster.world, EnumParticleTypes.SPELL_WITCH, caster.posX, caster.posY + 1d, caster.posZ, 35, 1.5d, 1d, 1.5d, 0.08d);
		} else if ("crow_trap_clone".equals(name)) {
			seal(caster.world, caster.getPositionVector(), 34, 45);
			smoke(caster.world, caster.getPositionVector().addVector(0d, 0.9d, 0d), 0xD822162C, 65, 0.8d, 0.08d, 40, 7);
			vanilla(caster.world, EnumParticleTypes.ENCHANTMENT_TABLE, caster.posX, caster.posY + 1d, caster.posZ, 45, 1.5d, 1.2d, 1.5d, 0.35d);
		} else if ("explosive_clone".equals(name)) {
			seal(caster.world, caster.getPositionVector(), 38, 30);
			smoke(caster.world, caster.getPositionVector().addVector(0d, 0.8d, 0d), 0xE8F0ECE4, 90, 1.15d, 0.13d, 35, 9);
			vanilla(caster.world, EnumParticleTypes.LAVA, caster.posX, caster.posY + 0.9d, caster.posZ, 18, 0.8d, 0.7d, 0.8d, 0.05d);
			caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.PLAYERS, 0.65f, 1.35f);
		} else if ("shuriken_shadow_clone".equals(name)) {
			sweep(caster, eyes, caster.rotationYaw - 22f, caster.rotationPitch, -18f, 0xB8D8E4F0, 3.0f);
			sweep(caster, eyes, caster.rotationYaw + 22f, caster.rotationPitch, 18f, 0xB8FFFFFF, 3.0f);
			vanilla(caster.world, EnumParticleTypes.CRIT, eyes.x, eyes.y, eyes.z, 45, 1.1d, 0.7d, 1.1d, 0.22d);
			caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.3f, 0.72f);
		} else if ("fire_rasengan".equals(name)) {
			Vec3d hand = eyes.add(look.scale(0.75d)).addVector(0d, -0.3d, 0d);
			sphere(caster.world, hand, 1.7f + Math.min(1.3f, power * 0.18f), 14, 0xC0FF5A00);
			flame(caster.world, hand, 0xE8FF7A00, 65, 0.65d, 0.10d, 25);
			sonic(caster.world, hand, look.scale(0.08d), 0x70FFB030, 12, 8);
		} else if ("sensorial_jutsu".equals(name)) {
			Vec3d center = caster.getPositionVector().addVector(0d, 0.9d, 0d);
			concentric(caster.world, center, 14f + power * 7f, 56, 0x6040D8FF);
			smoke(caster.world, center, 0xA040D8FF, 55, 1.4d, 0.06d, 42, 6);
			caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.9f, 0.72f);
			shake(caster.world, center, 16d, 8, 0.45f);
		} else if ("chakra_pulse".equals(name)) {
			Vec3d center = caster.getPositionVector().addVector(0d, 1d, 0d);
			sphere(caster.world, center, 4.0f, 12, 0xA040B8FF);
			for (int i = 0; i < 8; i++) {
				double a = Math.PI * 2d * i / 8d;
				sonic(caster.world, center, new Vec3d(Math.cos(a) * 0.15d, 0.02d, Math.sin(a) * 0.15d), 0x7040D8FF, 12, 7);
			}
			shake(caster.world, center, 18d, 7, 1.2f);
		} else if ("water_clone".equals(name)) {
			seal(caster.world, caster.getPositionVector(), 36, 34);
			water(caster.world, caster.getPositionVector().addVector(0d, 0.8d, 0d), 0xC060C8FF, 80, 1.0d, 0.10d, 36);
			vanilla(caster.world, EnumParticleTypes.WATER_BUBBLE, caster.posX, caster.posY + 0.8d, caster.posZ, 35, 0.9d, 0.8d, 0.9d, 0.08d);
		} else if ("mizuame_nabara".equals(name)) {
			water(caster.world, eyes.add(look.scale(1.0d)), 0xD8E7D28D, 60, 0.55d, 0.10d, 34);
			vanilla(caster.world, EnumParticleTypes.SLIME, eyes.x + look.x, eyes.y + look.y, eyes.z + look.z, 30, 0.65d, 0.4d, 0.65d, 0.10d);
		} else if ("water_wall".equals(name)) {
			Vec3d front = caster.getPositionVector().add(look.scale(3d));
			water(caster.world, front.addVector(0d, 1.4d, 0d), 0xC050BFFF, 120, 2.4d, 0.28d, 34);
			sphere(caster.world, front.addVector(0d, 1.0d, 0d), 3.2f, 10, 0x6050BFFF);
			shake(caster.world, front, 18d, 5, 0.65f);
		} else if ("water_prison_trap".equals(name)) {
			seal(caster.world, caster.getPositionVector(), 40, 42);
			water(caster.world, caster.getPositionVector().addVector(0d, 1d, 0d), 0xC060B8FF, 75, 1.15d, 0.08d, 42);
			vanilla(caster.world, EnumParticleTypes.WATER_BUBBLE, caster.posX, caster.posY + 1d, caster.posZ, 50, 1.2d, 1d, 1.2d, 0.05d);
		}
	}

	public static void onGenjutsu(EntityLivingBase caster, EntityLivingBase target, int type, float mastery) {
		if (caster == null || target == null || caster.world.isRemote) return;
		Vec3d center = target.getPositionVector().addVector(0d, target.height * 0.55d, 0d);
		if (type == 0) {
			sphere(caster.world, center, 2.4f, 18, 0x802E1648);
			vanilla(caster.world, EnumParticleTypes.SPELL_WITCH, center.x, center.y, center.z, 48, 0.8d, 0.9d, 0.8d, 0.08d);
			shakeTarget(target, 8, 0.8f);
		} else if (type == 1) {
			concentric(caster.world, center, 3.5f, 36, 0x704B2A8B);
			vanilla(caster.world, EnumParticleTypes.ENCHANTMENT_TABLE, center.x, center.y, center.z, 80, 1.4d, 1.2d, 1.4d, 0.45d);
			shakeTarget(target, 14, 1.3f + mastery);
		} else if (type == 2) {
			smoke(caster.world, center, 0xD8450810, 90, 1.0d, 0.04d, 48, 9);
			sweep(caster, center, target.rotationYaw + 180f, 0f, -25f, 0xC8A00010, 4.2f);
			sweep(caster, center, target.rotationYaw + 180f, 0f, 25f, 0xC8500010, 4.2f);
			shakeTarget(target, 18, 2.5f + mastery * 1.5f);
		} else if (type == 3) {
			for (int i = 0; i < 4; i++) sweep(caster, center, target.rotationYaw + i * 45f, 0f, -55f + i * 35f, 0xD8D01820, 5.0f);
			sonic(caster.world, center, new Vec3d(0d, 0.08d, 0d), 0x80C01018, 18, 9);
			vanilla(caster.world, EnumParticleTypes.DAMAGE_INDICATOR, center.x, center.y, center.z, 35, 1.0d, 1.1d, 1.0d, 0.18d);
			shakeTarget(target, 16, 4.0f + mastery * 2.0f);
		} else {
			sphere(caster.world, center, 3.2f, 20, 0xA0200808);
			flame(caster.world, center, 0xE8C01800, 100, 1.2d, 0.13d, 42);
			smoke(caster.world, center, 0xE8100808, 65, 1.0d, 0.06d, 50, 10);
			shakeTarget(target, 14, 2.5f + mastery * 1.5f);
		}
	}

	public static void taijutsu(String name, EntityLivingBase caster, EntityLivingBase target, float power, boolean hit) {
		if (caster == null || caster.world.isRemote) return;
		Vec3d origin = hit && target != null
		 ? target.getPositionVector().addVector(0d, target.height * 0.55d, 0d)
		 : caster.getPositionEyes(1f).add(caster.getLookVec().scale(1.2d));
		if ("leaf_whirlwind".equals(name)) {
			sweep(caster, origin, caster.rotationYaw - 18f, 4f, -12f, 0xC8A8FF58, 3.2f + power);
			sonic(caster.world, origin, caster.getLookVec().scale(0.10d), 0x60B8FF70, 10, 6);
			vanilla(caster.world, EnumParticleTypes.CLOUD, origin.x, origin.y, origin.z, 22, 0.65d, 0.35d, 0.65d, 0.10d);
		} else if ("leaf_hurricane".equals(name)) {
			for (int i = 0; i < 3; i++) sweep(caster, origin, caster.rotationYaw + i * 35f - 35f, 0f, i * 35f - 35f, 0xC8C8FF58, 4.0f + power);
			sphere(caster.world, origin, 2.6f, 8, 0x50A8E850);
			vanilla(caster.world, EnumParticleTypes.SWEEP_ATTACK, origin.x, origin.y, origin.z, 12, 1.0d, 0.55d, 1.0d, 0d);
			shake(caster.world, origin, 16d, hit ? 6 : 3, hit ? 1.4f : 0.5f);
		} else if ("dynamic_entry".equals(name)) {
			Vec3d look = caster.getLookVec().normalize();
			for (int i = 1; i <= 4; i++) sonic(caster.world, caster.getPositionEyes(1f).add(look.scale(i * 1.2d)), look.scale(0.16d), 0x70FFFFFF, 8 + i * 2, 5 + i);
			sweep(caster, origin, caster.rotationYaw, caster.rotationPitch, 90f, 0xD8FFFFFF, 4.5f + power);
			vanilla(caster.world, EnumParticleTypes.CLOUD, caster.posX, caster.posY + 0.2d, caster.posZ, 45, 0.9d, 0.25d, 0.9d, 0.18d);
			shake(caster.world, origin, 22d, hit ? 10 : 4, hit ? 2.8f : 0.8f);
		} else if ("primary_lotus".equals(name)) {
			for (int i = 0; i < 4; i++) sweep(caster, origin.addVector(0d, i * 0.45d - 0.7d, 0d), caster.rotationYaw + i * 35f, -25f + i * 18f, i * 45f, 0xC8E8E8E8, 4.2f + i * 0.35f);
			vanilla(caster.world, EnumParticleTypes.CLOUD, origin.x, origin.y, origin.z, 75, 1.15d, 1.5d, 1.15d, 0.20d);
			sphere(caster.world, origin, 3.4f, 10, 0x60FFFFFF);
			shake(caster.world, origin, 28d, hit ? 15 : 6, hit ? 4.2f : 1.2f);
		} else if ("lion_combo".equals(name)) {
			for (int i = 0; i < 5; i++) sweep(caster, origin, caster.rotationYaw - 40f + i * 20f, -20f + i * 10f, -50f + i * 25f, 0xD8FFD060, 3.2f + i * 0.25f);
			vanilla(caster.world, EnumParticleTypes.CRIT, origin.x, origin.y, origin.z, 65, 1.0d, 1.0d, 1.0d, 0.24d);
			sonic(caster.world, origin, caster.getLookVec().scale(0.12d), 0x70FFD070, 14, 8);
			shake(caster.world, origin, 20d, hit ? 10 : 4, hit ? 2.6f : 0.8f);
		} else if ("peregrine_falcon_drop".equals(name)) {
			for (int i = 0; i < 5; i++) sweep(caster, origin.addVector(0d, 1.5d - i * 0.5d, 0d),
			 caster.rotationYaw + i * 28f, 65f, i * 42f, 0xC8E8E8E8, 3.4f + i * 0.3f);
			vanilla(caster.world, EnumParticleTypes.CLOUD, origin.x, origin.y, origin.z, 90, 1.6d, 0.35d, 1.6d, 0.22d);
			sphere(caster.world, origin, 4.3f, 10, 0x70D8D0C8);
			shake(caster.world, origin, 30d, hit ? 16 : 6, hit ? 4.4f : 1.0f);
		} else if ("drunken_fist".equals(name)) {
			for (int i = 0; i < 4; i++) sweep(caster, origin.addVector((i - 1.5d) * 0.2d, i * 0.15d, 0d),
			 caster.rotationYaw - 45f + i * 30f, -18f + i * 12f, -55f + i * 35f, 0xA0D8E8A0, 2.8f + i * 0.25f);
			vanilla(caster.world, EnumParticleTypes.SPELL_MOB, caster.posX, caster.posY + 1d, caster.posZ, 32, 0.8d, 0.9d, 0.8d, 0.08d);
			concentric(caster.world, caster.getPositionVector().addVector(0d, 0.8d, 0d), 2.8f, 22, 0x50D8E8A0);
		} else if ("drunken_fist_hit".equals(name)) {
			sweep(caster, origin, caster.rotationYaw + (caster.getRNG().nextBoolean() ? 35f : -35f), 5f,
			 caster.getRNG().nextBoolean() ? 70f : -70f, 0xC8E8F0C0, 3.3f);
			vanilla(caster.world, EnumParticleTypes.CRIT, origin.x, origin.y, origin.z, 25, 0.55d, 0.55d, 0.55d, 0.18d);
			shake(caster.world, origin, 14d, 5, 0.8f);
		} else if ("leaf_drop".equals(name)) {
			sweep(caster, origin.addVector(0d, 0.7d, 0d), caster.rotationYaw, 72f, 90f, 0xD8D8C898, 4.8f + power);
			for (int i = 0; i < 8; i++) sonic(caster.world, origin,
			 new Vec3d(Math.cos(Math.PI * 2d * i / 8d) * 0.16d, 0.03d, Math.sin(Math.PI * 2d * i / 8d) * 0.16d),
			 0x80B89870, 14, 8);
			vanilla(caster.world, EnumParticleTypes.CLOUD, origin.x, origin.y, origin.z, 75, 1.4d, 0.3d, 1.4d, 0.18d);
			shake(caster.world, origin, 26d, hit ? 13 : 9, hit ? 3.4f : 2.2f);
		}
	}

	public static void impact(World world, Vec3d center, int color, float radius, int shakeTicks, float shakeScale) {
		if (world == null || world.isRemote) return;
		sphere(world, center, radius, 12, color);
		smoke(world, center, color, 70, radius * 0.45d, 0.12d, 32, 8);
		shake(world, center, 36d, shakeTicks, shakeScale);
	}

	private static void flameCone(EntityLivingBase caster, int points, int color, double spread) {
		Vec3d eyes = caster.getPositionEyes(1f);
		Vec3d look = caster.getLookVec().normalize();
		for (int i = 1; i <= points; i++) {
			Vec3d p = eyes.add(look.scale(0.45d * i));
			flame(caster.world, p, color, 7, spread * i / points, 0.04d, 20);
		}
	}

	private static void sweep(EntityLivingBase caster, Vec3d position, float yaw, float pitch, float roll, int color, float scale) {
		EntitySweep.Base effect = new EntitySweep.Base(caster, color, scale);
		effect.setLocationAndAngles(position.x, position.y, position.z, yaw, pitch);
		effect.rotationRoll = roll;
		caster.world.spawnEntity(effect);
	}

	private static void smoke(World world, Vec3d p, int color, int count, double spread, double speed, int life, int scale) {
		Particles.spawnParticle(world, Particles.Types.SMOKE, p.x, p.y, p.z, count, spread, spread * 0.7d, spread,
		 0d, speed, 0d, color, life, scale, 0xF0);
	}

	private static void flame(World world, Vec3d p, int color, int count, double spread, double speed, int life) {
		Particles.spawnParticle(world, Particles.Types.FLAME, p.x, p.y, p.z, count, spread, spread, spread,
		 0d, speed, 0d, color, life);
	}

	private static void water(World world, Vec3d p, int color, int count, double spread, double speed, int life) {
		Particles.spawnParticle(world, Particles.Types.WATER_SPLASH, p.x, p.y, p.z, count, spread, spread * 0.7d, spread,
		 0d, speed, 0d, color, life);
	}

	private static void sphere(World world, Vec3d p, float radius, int life, int color) {
		Particles.spawnParticle(world, Particles.Types.EXPANDING_SPHERE, p.x, p.y, p.z, 1,
		 0d, 0d, 0d, 0d, 0d, 0d, Math.max(4, (int)(radius * 10f)), life, color);
	}

	private static void concentric(World world, Vec3d p, float radius, int life, int color) {
		Particles.spawnParticle(world, Particles.Types.CONCENTRIC_SPHERES, p.x, p.y, p.z, 1,
		 0d, 0d, 0d, 0d, 0d, 0d, 128, Math.max(8, (int)(radius * 10f)), life, color);
	}

	private static void seal(World world, Vec3d p, int size, int life) {
		Particles.spawnParticle(world, Particles.Types.SEAL_FORMULA, p.x, p.y + 0.015d, p.z, 1,
		 0d, 0d, 0d, 0d, 0d, 0d, size, 0, life);
	}

	private static void sonic(World world, Vec3d p, Vec3d motion, int color, int age, int scale) {
		Particles.spawnParticle(world, Particles.Types.SONIC_BOOM, p.x, p.y, p.z, 1,
		 0d, 0d, 0d, motion.x, motion.y, motion.z, color, age, scale);
	}

	private static void vanilla(World world, EnumParticleTypes type, double x, double y, double z, int count,
	 double xOff, double yOff, double zOff, double speed) {
		if (world instanceof WorldServer) {
			((WorldServer)world).spawnParticle(type, x, y, z, count, xOff, yOff, zOff, speed);
		}
	}

	private static void shake(World world, Vec3d p, double range, int ticks, float scale) {
		ProcedureCameraShake.sendToClients(world.provider.getDimension(), p.x, p.y, p.z, range, ticks, scale);
	}

	private static void shakeTarget(EntityLivingBase target, int ticks, float scale) {
		if (target instanceof EntityPlayer) ProcedureCameraShake.sendToClient((EntityPlayer)target, ticks, scale);
		else shake(target.world, target.getPositionVector(), 12d, ticks, scale);
	}
}
