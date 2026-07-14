package net.narutomod.item;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.PlayerStats;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.procedure.ProcedureOnLeftClickEmpty;
import net.narutomod.procedure.ProcedureUtils;

import java.util.List;
import java.util.UUID;

@ElementsNarutomodMod.ModElement.Tag
public class ItemInuzuka extends ElementsNarutomodMod.ModElement {
	@GameRegistry.ObjectHolder("narutomod:inuzuka")
	public static final Item block = null;
	private static final int NINKEN_ID = 9320;
	public static final ItemJutsu.JutsuEnum NINKEN = new ItemJutsu.JutsuEnum(0, "ninken_companion", 'D', 80d, new NinkenJutsu());

	public ItemInuzuka(ElementsNarutomodMod instance) {
		super(instance, 1013);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new RangedItem());
		elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityNinken.class)
		 .id(new ResourceLocation("narutomod", "ninken"), NINKEN_ID).name("ninken").tracker(64, 3, true).build());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void preInit(FMLPreInitializationEvent event) {
		RenderingRegistry.registerEntityRenderingHandler(EntityNinken.class,
		 renderManager -> new RenderWolf(renderManager));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(block, 0, new ModelResourceLocation("narutomod:inuzuka", "inventory"));
	}

	@Override
	public void init(net.minecraftforge.fml.common.event.FMLInitializationEvent event) {
		ProcedureOnLeftClickEmpty.addQualifiedItem(block, EnumHand.MAIN_HAND);
	}

	public static class RangedItem extends ItemJutsu.Base {
		public RangedItem() {
			super(ItemJutsu.JutsuEnum.Type.INUZUKA, NINKEN);
			this.setUnlocalizedName("inuzuka");
			this.setRegistryName("inuzuka");
			this.setCreativeTab(TabCustomTabs.jutsus);
		}

		@Override
		public net.minecraft.util.EnumActionResult canActivateJutsu(ItemStack stack, ItemJutsu.JutsuEnum jutsuIn, EntityPlayer entity) {
			if (!PlayerStats.getClan(entity).equalsIgnoreCase("Inuzuka") && !entity.isCreative()) {
				return net.minecraft.util.EnumActionResult.FAIL;
			}
			return super.canActivateJutsu(stack, jutsuIn, entity);
		}

		@SideOnly(Side.CLIENT)
		@Override
		public void addInformation(ItemStack itemstack, World world, List<String> list, ITooltipFlag flag) {
			super.addInformation(itemstack, world, list, flag);
			list.add(TextFormatting.GOLD + I18n.format("tooltip.narutomod.inuzuka.clan_only"));
		}
	}

	public static class NinkenJutsu implements ItemJutsu.IJutsuCallback {
		@Override
		public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!(entity instanceof EntityPlayer) || entity.world.isRemote) {
				return false;
			}
			EntityPlayer player = (EntityPlayer)entity;
			float mastery = 0f;
			if (stack.getItem() instanceof ItemJutsu.Base) {
				ItemJutsu.Base item = (ItemJutsu.Base)stack.getItem();
				int required = Math.max(1, item.getRequiredXp(stack, NINKEN));
				mastery = net.minecraft.util.math.MathHelper.clamp(((float)item.getJutsuXp(stack, NINKEN) - required) / (required * 2.0f), 0f, 1f);
			}
			EntityNinken dog = EntityNinken.getForOwner(player);
			if (dog == null) {
				dog = new EntityNinken(player.world, player, mastery);
				dog.setPosition(player.posX + 1.0d, player.posY, player.posZ + 1.0d);
				player.world.spawnEntity(dog);
			} else {
				dog.setMastery(mastery);
				dog.heal(10f + mastery * 20f);
				if (dog.getDistance(player) > 24d) {
					dog.setPositionAndUpdate(player.posX + 1.0d, player.posY, player.posZ + 1.0d);
				}
			}
			ItemJutsu.setCurrentJutsuCooldown(stack, entity, 300);
			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_WOLF_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return true;
		}
	}

	public static class EntityNinken extends EntityWolf implements ItemJutsu.IJutsu {
		private float mastery;

		public EntityNinken(World world) {
			super(world);
			this.enablePersistence();
		}

		public EntityNinken(World world, EntityPlayer owner, float masteryIn) {
			this(world);
			this.setTamedBy(owner);
			this.setCustomNameTag(owner.getName() + "'s Ninken");
			this.setMastery(masteryIn);
		}

		public static EntityNinken getForOwner(EntityPlayer owner) {
			UUID id = owner.getUniqueID();
			for (EntityNinken dog : owner.world.getEntities(EntityNinken.class, e -> e.isEntityAlive() && id.equals(e.getOwnerId()))) {
				return dog;
			}
			return null;
		}

		public void setMastery(float masteryIn) {
			this.mastery = net.minecraft.util.math.MathHelper.clamp(masteryIn, 0f, 1f);
			IAttributeInstance maxHealth = this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
			IAttributeInstance attack = this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
			IAttributeInstance speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
			maxHealth.setBaseValue(50d + 90d * this.mastery);
			attack.setBaseValue(4d + 10d * this.mastery);
			speed.setBaseValue(0.36d + 0.12d * this.mastery);
			if (this.getHealth() > this.getMaxHealth()) {
				this.setHealth(this.getMaxHealth());
			} else if (this.getHealth() < 1f) {
				this.setHealth(this.getMaxHealth());
			}
			this.setAIMoveSpeed((float)speed.getBaseValue());
		}

		@Override
		protected void applyEntityAttributes() {
			super.applyEntityAttributes();
			this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50d);
			this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4d);
			this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.36d);
			this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32d);
		}

		@Override
		public void onLivingUpdate() {
			super.onLivingUpdate();
			if (!this.world.isRemote) {
				EntityLivingBase owner = this.getOwner();
				if (owner == null || !owner.isEntityAlive()) {
					return;
				}
				if (this.getDistance(owner) > 48d) {
					this.setPositionAndUpdate(owner.posX + 1d, owner.posY, owner.posZ + 1d);
				}
				EntityLivingBase target = owner.getRevengeTarget() != null ? owner.getRevengeTarget() : owner.getLastAttackedEntity();
				if (target != null && target.isEntityAlive() && !this.isOnSameTeam(target) && this.getDistance(target) < 32d) {
					this.setAttackTarget(target);
				}
				if (this.mastery >= 0.65f && this.getAttackTarget() != null && this.ticksExisted % 20 == 0) {
					this.world.spawnParticle(EnumParticleTypes.CRIT, this.posX, this.posY + 0.8d, this.posZ, 0d, 0.05d, 0d);
				}
			}
		}

		@Override
		public boolean attackEntityAsMob(Entity entityIn) {
			boolean hit = super.attackEntityAsMob(entityIn);
			if (hit && entityIn instanceof EntityLivingBase) {
				EntityLivingBase target = (EntityLivingBase)entityIn;
				if (this.mastery >= 0.45f && this.rand.nextFloat() < this.mastery * 0.35f) {
					target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0, false, false));
					target.attackEntityFrom(DamageSource.causeMobDamage(this), 1.0f + this.mastery * 2.0f);
				}
				if (this.mastery >= 0.75f) {
					EntityLivingBase owner = this.getOwner();
					if (owner != null && owner.getDistance(target) < 5d) {
						Vec3d push = target.getPositionVector().subtract(owner.getPositionVector()).normalize().scale(0.25d);
						ProcedureUtils.addVelocity(target, push.x, 0.18d, push.z);
					}
				}
			}
			return hit;
		}

		@Override
		public boolean shouldAttackEntity(EntityLivingBase target, EntityLivingBase owner) {
			return target != null && owner != null && !target.equals(owner) && !this.isOnSameTeam(target);
		}

		@Override
		public boolean isOnSameTeam(Entity entityIn) {
			return entityIn.equals(this.getOwner()) || super.isOnSameTeam(entityIn);
		}

		@Override
		public void writeEntityToNBT(NBTTagCompound compound) {
			super.writeEntityToNBT(compound);
			compound.setFloat("mastery", this.mastery);
		}

		@Override
		public void readEntityFromNBT(NBTTagCompound compound) {
			super.readEntityFromNBT(compound);
			this.setMastery(compound.getFloat("mastery"));
		}

		@Override
		public ItemJutsu.JutsuEnum.Type getJutsuType() {
			return ItemJutsu.JutsuEnum.Type.INUZUKA;
		}
	}
}
