package net.narutomod.item;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
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
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
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

	public static final ItemJutsu.JutsuEnum LEAF_WHIRLWIND = new ItemJutsu.JutsuEnum(0, "leaf_whirlwind", 'D', 35d, new StrikeJutsu(4.0d, 3.0f, 55, 1.0f, 0.6d, 0.18d, 0x80D8FF70, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP));
	public static final ItemJutsu.JutsuEnum LEAF_HURRICANE = new ItemJutsu.JutsuEnum(1, "leaf_hurricane", 'C', 55d, new HurricaneJutsu());
	public static final ItemJutsu.JutsuEnum DYNAMIC_ENTRY = new ItemJutsu.JutsuEnum(2, "dynamic_entry", 'C', 65d, new DynamicEntryJutsu());
	public static final ItemJutsu.JutsuEnum PRIMARY_LOTUS = new ItemJutsu.JutsuEnum(3, "primary_lotus", 'B', 95d, new PrimaryLotusJutsu());
	public static final ItemJutsu.JutsuEnum LION_COMBO = new ItemJutsu.JutsuEnum(4, "lion_combo", 'B', 85d, new LionComboJutsu());

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
	}

	public static class RangedItem extends ItemJutsu.Base {
		public RangedItem() {
			super(ItemJutsu.JutsuEnum.Type.TAIJUTSU, LEAF_WHIRLWIND, LEAF_HURRICANE, DYNAMIC_ENTRY, PRIMARY_LOTUS, LION_COMBO);
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
		protected final double range;
		protected final float baseDamage;
		protected final long cooldown;
		protected final float exhaustion;
		protected final double forward;
		protected final double lift;
		protected final int color;
		protected final SoundEvent sound;

		StrikeJutsu(double range, float baseDamage, long cooldown, float exhaustion, double forward, double lift, int color, SoundEvent sound) {
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
			if (target == null) {
				this.dash(entity, power * 0.45d);
				this.fx(entity, false);
				this.cooldown(stack, entity);
				return true;
			}
			float damage = this.damage(entity, power);
			target.hurtResistantTime = 0;
			target.attackEntityFrom(this.source(entity), damage);
			this.push(entity, target, power);
			this.fx(entity, true);
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
			return entity instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer)entity) : DamageSource.causeMobDamage(entity);
		}

		protected float damage(EntityLivingBase entity, float power) {
			float stats = entity instanceof EntityPlayer ? (float)(PlayerStats.getStat((EntityPlayer)entity, 1) * 0.045d + PlayerStats.getStat((EntityPlayer)entity, 0) * 0.025d) : 0f;
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

		protected void fx(EntityLivingBase entity, boolean hit) {
			entity.swingArm(EnumHand.MAIN_HAND);
			entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, this.sound, SoundCategory.PLAYERS, hit ? 1.2f : 0.7f, hit ? 0.85f : 1.2f);
			if (hit) {
				entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.25f, 1.8f);
				entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.7f);
			}
			Particles.spawnParticle(entity.world, Particles.Types.SMOKE, entity.posX, entity.posY + 0.9d, entity.posZ,
			 hit ? 70 : 28, 0.55d, 0.45d, 0.55d, 0d, 0.08d, 0d, this.color, 28, 6, 0xF0, entity.getEntityId());
			for (int i = 0; i < (hit ? 34 : 12); i++) {
				entity.world.spawnParticle(EnumParticleTypes.CRIT, entity.posX + (entity.getRNG().nextDouble() - 0.5d) * 1.2d,
				 entity.posY + 0.6d + entity.getRNG().nextDouble() * 1.0d, entity.posZ + (entity.getRNG().nextDouble() - 0.5d) * 1.2d,
				 (entity.getRNG().nextDouble() - 0.5d) * 0.25d, 0.12d, (entity.getRNG().nextDouble() - 0.5d) * 0.25d);
			}
			if (hit) {
				for (int i = 0; i < 12; i++) {
					entity.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, entity.posX + (entity.getRNG().nextDouble() - 0.5d) * 1.4d,
					 entity.posY + 0.8d + entity.getRNG().nextDouble() * 0.7d, entity.posZ + (entity.getRNG().nextDouble() - 0.5d) * 1.4d,
					 (entity.getRNG().nextDouble() - 0.5d) * 0.2d, 0.08d, (entity.getRNG().nextDouble() - 0.5d) * 0.2d);
				}
			}
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
			super(4.5d, 4.5f, 80, 1.4f, 0.95d, 0.35d, 0x90B8FF70, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
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
			super(8.0d, 6.0f, 120, 2.0f, 1.45d, 0.25d, 0x90FFFFFF, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK);
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			this.dash(entity, 1.2d * power);
			return super.createJutsu(stack, entity, power);
		}

		@Override public float getPowerupDelay() { return 26.0f; }
		@Override public float getMaxPower() { return 1.6f; }
	}

	private static class PrimaryLotusJutsu extends StrikeJutsu {
		PrimaryLotusJutsu() {
			super(3.6d, 8.0f, 220, 3.5f, 0.55d, 0.9d, 0xA0E0E0E0, SoundEvents.ENTITY_GENERIC_EXPLODE);
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
			super(4.0d, 7.0f, 180, 2.8f, 0.75d, 0.55d, 0x90FFD070, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT);
		}

		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			EntityLivingBase target = this.getTarget(entity, this.range + power);
			boolean used = super.createJutsu(stack, entity, power);
			if (used && target != null) {
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
}
