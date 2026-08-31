package net.narutomod.item;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.narutomod.Particles;
import net.narutomod.entity.EntitySweep;
import net.narutomod.procedure.ProcedureCameraShake;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;

/**
 * Server-authoritative cinematic timing for the high-commitment taijutsu.
 *
 * The client still receives ordinary entity motion, sounds and particles, so
 * this works for every player without requiring another client-only animation
 * mod.  The routines deliberately retain control for a short, finite window:
 * a missed move still has the animation, but never leaves a player frozen.
 */
public final class CinematicTaijutsu {
	private static final String MODE = "NarutomodCinematicTaijutsuMode";
	private static final String TICK = "NarutomodCinematicTaijutsuTick";
	private static final String TARGET = "NarutomodCinematicTaijutsuTarget";
	private static final String POWER = "NarutomodCinematicTaijutsuPower";
	private static final String DAMAGE = "NarutomodCinematicTaijutsuDamage";
	private static final String X = "NarutomodCinematicTaijutsuX";
	private static final String Y = "NarutomodCinematicTaijutsuY";
	private static final String Z = "NarutomodCinematicTaijutsuZ";
	private static final String RENDER_SKILL = "NarutomodCinematicTaijutsuSkill";
	private static final String RENDER_TICK = "NarutomodCinematicTaijutsuRenderTick";
	private static final String FLASH = "NarutomodCinematicTaijutsuFlash";
	private static boolean registered;

	private CinematicTaijutsu() { }

	public static void register() {
		if (!registered) {
			registered = true;
			MinecraftForge.EVENT_BUS.register(new CinematicTaijutsu());
		}
	}

	public static boolean isCinematic(String name) {
		return "dynamic_entry".equals(name) || "primary_lotus".equals(name) || "lion_combo".equals(name);
	}

	/** Values mirrored to clients for the PlayerRender pose hook. */
	public static int getRenderSkill(Entity entity) {
		return entity == null ? 0 : entity.getEntityData().getInteger(RENDER_SKILL);
	}

	public static int getRenderTick(Entity entity) {
		return entity == null ? 0 : entity.getEntityData().getInteger(RENDER_TICK);
	}

	private static int skillId(String name) {
		return "dynamic_entry".equals(name) ? 1 : "primary_lotus".equals(name) ? 2 : "lion_combo".equals(name) ? 3
			: "leaf_whirlwind".equals(name) ? 4 : "leaf_hurricane".equals(name) ? 5
			: "peregrine_falcon_drop".equals(name) ? 6 : "leaf_drop".equals(name) ? 7 : 0;
	}

	/** Brief pose for the direct-hit moves that do not need a full movement sequence. */
	public static void flash(String name, EntityLivingBase caster) {
		if (caster == null || caster.world.isRemote || skillId(name) == 0 || isCinematic(name)) return;
		NBTTagCompound tag = caster.getEntityData();
		tag.setInteger(FLASH, 6);
		ProcedureSync.EntityNBTTag.setAndSync(caster, RENDER_SKILL, skillId(name));
		ProcedureSync.EntityNBTTag.setAndSync(caster, RENDER_TICK, 0);
	}

	public static void start(String name, EntityLivingBase caster, EntityLivingBase target, float power, float damage) {
		if (!(caster instanceof EntityPlayer) || caster.world.isRemote || !isCinematic(name)) return;
		NBTTagCompound tag = caster.getEntityData();
		tag.setString(MODE, name);
		tag.setInteger(TICK, 0);
		tag.setInteger(TARGET, target == null ? -1 : target.getEntityId());
		tag.setFloat(POWER, power);
		tag.setFloat(DAMAGE, damage);
		Vec3d origin = target != null ? target.getPositionVector() : caster.getPositionVector().add(caster.getLookVec().scale(8d));
		tag.setDouble(X, origin.x);
		tag.setDouble(Y, origin.y);
		tag.setDouble(Z, origin.z);
		ProcedureSync.EntityNBTTag.setAndSync(caster, RENDER_SKILL, skillId(name));
		ProcedureSync.EntityNBTTag.setAndSync(caster, RENDER_TICK, 0);
		caster.swingArm(EnumHand.MAIN_HAND);
		if ("dynamic_entry".equals(name)) {
			fxSmoke(caster.world, caster.getPositionVector(), 0xA0FFFFFF, 34, 0.75d);
			play(caster, "narutomod:howl_youth", 1.05f, 1.08f);
		} else if ("primary_lotus".equals(name)) {
			fxSweep(caster, caster.getPositionEyes(1f), caster.rotationYaw, -30f, 12f, 0xC8E8E8E8, 3.8f);
			play(caster, "narutomod:throwpunch", 0.85f, 0.78f);
		} else {
			fxSweep(caster, caster.getPositionEyes(1f), caster.rotationYaw, -18f, -22f, 0xD8FFD060, 3.5f);
			fxSmoke(caster.world, caster.getPositionVector(), 0xA0FFD060, 25, 0.6d);
		}
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player == null || event.player.world.isRemote) return;
		EntityPlayer player = event.player;
		NBTTagCompound tag = player.getEntityData();
		String mode = tag.getString(MODE);
		if (mode == null || mode.isEmpty()) {
			tickFlash(player);
			return;
		}
		int tick = tag.getInteger(TICK);
		float power = tag.getFloat(POWER);
		float damage = tag.getFloat(DAMAGE);
		Entity entity = player.world.getEntityByID(tag.getInteger(TARGET));
		EntityLivingBase target = entity instanceof EntityLivingBase && entity.isEntityAlive() && entity != player
			? (EntityLivingBase)entity : null;
		boolean done;
		if ("dynamic_entry".equals(mode)) {
			done = tickDynamicEntry(player, target, power, damage, tag, tick);
		} else if ("primary_lotus".equals(mode)) {
			done = tickPrimaryLotus(player, target, power, damage, tag, tick);
		} else if ("lion_combo".equals(mode)) {
			done = tickLionCombo(player, target, power, damage, tag, tick);
		} else {
			done = true;
		}
		if (done || tick >= 70) finish(player, target, mode);
		else {
			tag.setInteger(TICK, tick + 1);
			ProcedureSync.EntityNBTTag.setAndSync(player, RENDER_TICK, tick + 1);
		}
	}

	private static void tickFlash(EntityPlayer player) {
		NBTTagCompound tag = player.getEntityData();
		int ticks = tag.getInteger(FLASH);
		if (ticks <= 0) return;
		int age = 6 - ticks;
		if (ticks <= 1) {
			tag.removeTag(FLASH);
			ProcedureSync.EntityNBTTag.removeAndSync(player, RENDER_SKILL);
			ProcedureSync.EntityNBTTag.removeAndSync(player, RENDER_TICK);
		} else {
			tag.setInteger(FLASH, ticks - 1);
			ProcedureSync.EntityNBTTag.setAndSync(player, RENDER_TICK, age + 1);
		}
	}

	private static boolean tickDynamicEntry(EntityPlayer player, EntityLivingBase target, float power, float damage,
	 NBTTagCompound tag, int tick) {
		Vec3d end = target != null ? target.getPositionVector().addVector(0d, target.height * 0.52d, 0d)
			: new Vec3d(tag.getDouble(X), tag.getDouble(Y), tag.getDouble(Z));
		Vec3d current = player.getPositionVector();
		Vec3d delta = end.subtract(current);
		double distance = delta.lengthVector();
		Vec3d direction = distance < 0.02d ? player.getLookVec().normalize() : delta.normalize();
		Vec3d next = current.add(direction.scale(Math.min(distance, 1.3d + power * 0.25d)));
		move(player, next.x, next.y + (tick < 3 ? 0.14d : 0d), next.z);
		if (tick % 2 == 0) {
			fxSweep(player, next.addVector(0d, 0.8d, 0d), player.rotationYaw, player.rotationPitch, 90f,
				0xD8FFFFFF, 3.7f + power);
			fxSmoke(player.world, next, 0x70FFFFFF, 11, 0.32d);
		}
		if (tick < 7 && distance > 1.25d) return false;
		Vec3d impact = target != null ? target.getPositionVector().addVector(0d, target.height * 0.5d, 0d) : next;
		fxSweep(player, impact, player.rotationYaw, 4f, 92f, 0xE8FFFFFF, 5.0f + power);
		if (target != null) {
			hit(player, target, damage);
			ProcedureUtils.addVelocity(target, direction.x * 1.35d * power, 0.27d * power, direction.z * 1.35d * power);
			shake(player.world, impact, 20d, 9, 2.3f);
			player.world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
				SoundCategory.PLAYERS, 1.15f, 0.72f);
		} else {
			shake(player.world, impact, 12d, 4, 0.7f);
		}
		return true;
	}

	private static boolean tickPrimaryLotus(EntityPlayer player, EntityLivingBase target, float power, float damage,
	 NBTTagCompound tag, int tick) {
		double x = target != null ? target.posX : tag.getDouble(X);
		double z = target != null ? target.posZ : tag.getDouble(Z);
		double floor = groundY(player.world, x, Math.max(player.posY, target == null ? tag.getDouble(Y) : target.posY), z);
		if (tick == 0) {
			double high = Math.max(player.posY, target == null ? floor : target.posY);
			tag.setDouble(Y, Math.max(high + 5.6d + power * 0.8d, floor + 4.5d));
			fxSmoke(player.world, new Vec3d(x, floor, z), 0xA0E0E0E0, 35, 0.95d);
		}
		double apex = tag.getDouble(Y);
		if (tick <= 11) {
			double angle = tick * 0.95d;
			move(player, x + Math.cos(angle) * 0.72d, apex, z + Math.sin(angle) * 0.72d);
			if (target != null) {
				move(target, x, apex, z);
				if (tick == 1 || tick == 6) hit(player, target, damage * 0.18f);
			}
			spin(player, 48f);
			if (target != null) spin(target, 38f);
			if (tick % 2 == 0) {
				fxSweep(player, new Vec3d(x, apex + 0.45d, z), player.rotationYaw, -18f, tick * 27f,
					0xC8E8E8E8, 3.8f);
				fxSmoke(player.world, new Vec3d(x, apex + 0.5d, z), 0x70FFFFFF, 8, 0.42d);
			}
			return false;
		}
		int fallTick = tick - 12;
		double y = Math.max(floor, apex - fallTick * (0.78d + fallTick * 0.13d));
		double angle = tick * 0.82d;
		move(player, x + Math.cos(angle) * 0.5d, y, z + Math.sin(angle) * 0.5d);
		if (target != null) move(target, x, y, z);
		spin(player, 58f);
		if (target != null) spin(target, 48f);
		if (tick % 2 == 0) fxSmoke(player.world, new Vec3d(x, y + 0.3d, z), 0x90FFFFFF, 10, 0.45d);
		if (y > floor + 0.18d && tick < 42) return false;
		Vec3d impact = new Vec3d(x, floor + 0.05d, z);
		fxSweep(player, impact.addVector(0d, 0.72d, 0d), player.rotationYaw, 72f, 0f, 0xE8FFFFFF, 5.0f + power);
		impact(player.world, impact, 0xB8E8E8E8, 4.7f, 15, 4.0f);
		if (target != null) {
			move(target, x, floor, z);
			hit(player, target, damage * 0.66f);
			target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 48, 1, false, false));
			ProcedureUtils.addVelocity(target, 0d, 0.14d, 0d);
		}
		player.world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
			SoundCategory.PLAYERS, 0.9f, 0.84f);
		return true;
	}

	private static boolean tickLionCombo(EntityPlayer player, EntityLivingBase target, float power, float damage,
	 NBTTagCompound tag, int tick) {
		Vec3d center = target != null ? target.getPositionVector() : new Vec3d(tag.getDouble(X), tag.getDouble(Y), tag.getDouble(Z));
		double floor = groundY(player.world, center.x, Math.max(player.posY, center.y), center.z);
		if (tick == 0) {
			center = new Vec3d(center.x, Math.max(center.y, floor) + 2.2d + power * 0.5d, center.z);
			tag.setDouble(X, center.x); tag.setDouble(Y, center.y); tag.setDouble(Z, center.z);
			if (target != null) { move(target, center.x, center.y, center.z); hit(player, target, damage * 0.14f); }
			fxSmoke(player.world, center, 0x90FFD060, 24, 0.68d);
		}
		center = new Vec3d(tag.getDouble(X), tag.getDouble(Y), tag.getDouble(Z));
		if (tick < 15) {
			double orbit = tick * 0.75d;
			move(player, center.x - Math.cos(orbit) * 0.72d, center.y + 0.82d, center.z - Math.sin(orbit) * 0.72d);
			if (target != null) move(target, center.x, center.y, center.z);
			if (tick == 3 || tick == 6 || tick == 9 || tick == 12) {
				Vec3d strike = center.addVector(0d, 0.65d, 0d);
				fxSweep(player, strike, player.rotationYaw - 38f + tick * 8f, -20f, tick * 20f,
					0xD8FFD060, 3.3f + power * 0.25f);
				if (target != null) hit(player, target, damage * 0.09f);
				player.world.playSound(null, strike.x, strike.y, strike.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
					SoundCategory.PLAYERS, 0.85f, 0.9f + tick * 0.02f);
			}
			if (tick % 2 == 0) fxSmoke(player.world, center, 0x60FFD060, 6, 0.35d);
			return false;
		}
		Vec3d impact = new Vec3d(center.x, floor + 0.05d, center.z);
		move(player, impact.x - player.getLookVec().x * 0.8d, floor, impact.z - player.getLookVec().z * 0.8d);
		if (target != null) {
			move(target, impact.x, floor, impact.z);
			hit(player, target, damage * 0.50f);
			target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0, false, false));
			ProcedureUtils.addVelocity(target, player.getLookVec().x * 0.75d * power, 0.5d * power, player.getLookVec().z * 0.75d * power);
		}
		fxSweep(player, impact.addVector(0d, 0.7d, 0d), player.rotationYaw, 70f, 0f, 0xE8FFD060, 4.8f + power);
		impact(player.world, impact, 0xA0FFD060, 3.8f, 10, 2.5f);
		play(player, "narutomod:throwpunch", 1.0f, 0.92f);
		return true;
	}

	private static void finish(EntityPlayer player, EntityLivingBase target, String mode) {
		stopMotion(player);
		stopMotion(target);
		if ("primary_lotus".equals(mode)) {
			player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 0, false, false));
		} else if ("lion_combo".equals(mode)) {
			player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0, false, false));
		}
		NBTTagCompound tag = player.getEntityData();
		tag.removeTag(MODE); tag.removeTag(TICK); tag.removeTag(TARGET); tag.removeTag(POWER); tag.removeTag(DAMAGE);
		tag.removeTag(X); tag.removeTag(Y); tag.removeTag(Z);
		ProcedureSync.EntityNBTTag.removeAndSync(player, RENDER_SKILL);
		ProcedureSync.EntityNBTTag.removeAndSync(player, RENDER_TICK);
	}

	private static void hit(EntityLivingBase source, EntityLivingBase target, float damage) {
		if (target == null || !target.isEntityAlive() || damage <= 0f) return;
		target.hurtResistantTime = 0;
		DamageSource type = source instanceof EntityPlayer ? new net.minecraft.util.EntityDamageSource("narutomod.taijutsu", source)
			: DamageSource.causeMobDamage(source);
		target.attackEntityFrom(type, damage);
	}

	private static void move(EntityLivingBase entity, double x, double y, double z) {
		if (entity == null) return;
		entity.motionX = entity.motionY = entity.motionZ = 0d;
		entity.fallDistance = 0f;
		if (entity instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP)entity;
			player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
		} else {
			entity.setPositionAndUpdate(x, y, z);
		}
	}

	private static void stopMotion(EntityLivingBase entity) {
		if (entity != null) {
			entity.motionX = entity.motionY = entity.motionZ = 0d;
			entity.fallDistance = 0f;
		}
	}

	private static void spin(EntityLivingBase entity, float yaw) {
		if (entity == null) return;
		entity.rotationYaw += yaw;
		entity.renderYawOffset = entity.rotationYaw;
		entity.rotationYawHead = entity.rotationYaw;
	}

	private static double groundY(World world, double x, double y, double z) {
		BlockPos pos = new BlockPos(x, y, z);
		for (int i = 0; i < 18; i++) {
			if (!world.isAirBlock(pos) && world.isAirBlock(pos.up())) return pos.getY() + 1d;
			pos = pos.down();
		}
		return Math.floor(y);
	}

	private static void fxSweep(EntityLivingBase actor, Vec3d at, float yaw, float pitch, float roll, int color, float size) {
		EntitySweep.Base sweep = new EntitySweep.Base(actor, color, size);
		sweep.setLocationAndAngles(at.x, at.y, at.z, yaw, pitch);
		sweep.rotationRoll = roll;
		actor.world.spawnEntity(sweep);
	}

	private static void fxSmoke(World world, Vec3d at, int color, int count, double spread) {
		Particles.spawnParticle(world, Particles.Types.SMOKE, at.x, at.y + 0.25d, at.z, count,
			spread, spread * 0.55d, spread, 0d, 0.08d, 0d, color, 20, 5, 0xF0);
		if (world instanceof net.minecraft.world.WorldServer) {
			((net.minecraft.world.WorldServer)world).spawnParticle(EnumParticleTypes.CLOUD, at.x, at.y + 0.25d, at.z,
				Math.max(4, count / 4), spread, spread * 0.35d, spread, 0.08d);
		}
	}

	private static void impact(World world, Vec3d at, int color, float radius, int ticks, float scale) {
		Particles.spawnParticle(world, Particles.Types.EXPANDING_SPHERE, at.x, at.y + 0.4d, at.z,
			1, 0d, 0d, 0d, 0d, 0d, 0d, Math.max(18, (int)(radius * 11f)), 12, color);
		fxSmoke(world, at, color, 64, radius * 0.44d);
		shake(world, at, 32d, ticks, scale);
	}

	private static void shake(World world, Vec3d at, double range, int ticks, float scale) {
		ProcedureCameraShake.sendToClients(world.provider.getDimension(), at.x, at.y, at.z, range, ticks, scale);
	}

	private static void play(EntityLivingBase entity, String key, float volume, float pitch) {
		SoundEvent event = SoundEvent.REGISTRY.getObject(new ResourceLocation(key));
		if (event != null) entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, event, SoundCategory.PLAYERS, volume, pitch);
	}
}
