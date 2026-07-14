package net.narutomod.item;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.world.World;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.Item;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.Minecraft;

import net.narutomod.entity.EntityEightTrigrams;
import net.narutomod.entity.EntityHakkeshoKeiten;
import net.narutomod.gui.overlay.OverlayByakuganView;
import net.narutomod.procedure.*;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.Chakra;
import net.narutomod.Particles;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.google.common.collect.Multimap;
import com.google.common.collect.Maps;

@ElementsNarutomodMod.ModElement.Tag
public class ItemByakugan extends ElementsNarutomodMod.ModElement {
	@ObjectHolder("narutomod:byakuganhelmet")
	public static final Item helmet = null;
	private static final String RINNESHARINGAN_KEY = NarutomodModVariables.RINNESHARINGAN_ACTIVATED;
	private static final String TENSEIGANEVOLVEDTIME = NarutomodModVariables.tenseiganEvolvedTime;
	private final UUID RINNESHARINGAN_MODIFIER = UUID.fromString("c69907b2-2687-47ab-aca0-49898cd38463");
	private static final double BYAKUGAN_CHAKRA_USAGE = 10d; //per half sec
	private static final double ROKUJUYONSHO_CHAKRA_USAGE = 100d;
	private static final double KAITEN_CHAKRA_USAGE = 5d; // per tick
	private static final double KUSHO_CHAKRA_USAGE = 0.5d; // x pressDuration
	private static final String TENKETSU_STACKS = "BlockedTenketsuStacks";
	private static final String TENKETSU_UNTIL = "BlockedTenketsuUntil";
	private static final int TENKETSU_MAX_STACKS = 8;
	
	public ItemByakugan(ElementsNarutomodMod instance) {
		super(instance, 98);
	}

	public static double getByakuganChakraUsage(EntityLivingBase entity) {
		ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		return stack.getItem() == helmet ? ((ItemDojutsu.Base)helmet).isOwner(stack, entity) ? BYAKUGAN_CHAKRA_USAGE 
		 : BYAKUGAN_CHAKRA_USAGE * 2 : (Double.MAX_VALUE * 0.001d);
	}

	public static double getRokujuyonshoChakraUsage(EntityLivingBase entity) {
		ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		return stack.getItem() == helmet && ((ItemDojutsu.Base)helmet).isOwner(stack, entity) ? ROKUJUYONSHO_CHAKRA_USAGE 
		 : (Double.MAX_VALUE * 0.001d);
	}

	public static double getKaitenChakraUsage(EntityLivingBase entity) {
		ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		return stack.getItem() == helmet && ((ItemDojutsu.Base)helmet).isOwner(stack, entity) ? KAITEN_CHAKRA_USAGE 
		 : (Double.MAX_VALUE * 0.001d);
	}

	public static double getKushoChakraUsage(EntityLivingBase entity) {
		ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
		return stack.getItem() == helmet && ((ItemDojutsu.Base)helmet).isOwner(stack, entity) ? KUSHO_CHAKRA_USAGE 
		 : (Double.MAX_VALUE * 0.001d);
	}

	public static int getBlockedTenketsuStacks(EntityLivingBase entity) {
		if (entity.world.getTotalWorldTime() > entity.getEntityData().getLong(TENKETSU_UNTIL)) {
			entity.getEntityData().removeTag(TENKETSU_STACKS);
			entity.getEntityData().removeTag(TENKETSU_UNTIL);
			return 0;
		}
		return Math.min(TENKETSU_MAX_STACKS, entity.getEntityData().getInteger(TENKETSU_STACKS));
	}

	public static double getTenketsuCostMultiplier(EntityLivingBase entity) {
		return 1.0d + getBlockedTenketsuStacks(entity) * 0.12d;
	}

	public static double getTenketsuRegenMultiplier(EntityLivingBase entity) {
		return Math.max(0.15d, 1.0d - getBlockedTenketsuStacks(entity) * 0.10d);
	}

	public static boolean canUseJutsuUnderTenketsu(EntityLivingBase entity, ItemJutsu.JutsuEnum jutsu) {
		return getBlockedTenketsuStacks(entity) < TENKETSU_MAX_STACKS || (jutsu.rank != 'A' && jutsu.rank != 'S');
	}

	private static void applyTenketsuHit(EntityLivingBase user, EntityLivingBase target, float mastery) {
		int stacks = Math.min(TENKETSU_MAX_STACKS, getBlockedTenketsuStacks(target) + 1);
		int duration = 120 + (int)(mastery * 120f);
		target.getEntityData().setInteger(TENKETSU_STACKS, stacks);
		target.getEntityData().setLong(TENKETSU_UNTIL, target.world.getTotalWorldTime() + duration);
		Chakra.pathway(target).consume(8d + stacks * 3d + mastery * 18d);
		target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 45 + (int)(mastery * 35f), Math.min(2, stacks / 3), false, false));
		target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 45 + (int)(mastery * 35f), Math.min(2, stacks / 4), false, false));
		if (stacks >= TENKETSU_MAX_STACKS) {
			target.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 70, 3, false, false));
			ProcedureUtils.setVelocity(target, 0d, Math.min(target.motionY, 0.05d), 0d);
		}
		target.attackEntityFrom(user instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer)user) : DamageSource.causeMobDamage(user),
		 1.2f + mastery * 1.6f);
	}

	private static void castTenketsuHari(EntityPlayer user, ItemStack stack) {
		if (user.world.isRemote) {
			return;
		}
		if (stack.hasTagCompound() && stack.getTagCompound().getLong("TenketsuHariCD") > user.world.getTotalWorldTime()) {
			user.sendStatusMessage(new TextComponentString(TextFormatting.AQUA + "Tenketsu Hari cooldown"), true);
			return;
		}
		double cost = 90d;
		if (!Chakra.pathway(user).consume(cost)) {
			return;
		}
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
		}
		float mastery = stack.hasTagCompound() ? (float)Math.min(1.0d, stack.getTagCompound().getDouble("ByakuganCount") / 5.0d) : 0f;
		int needles = 8 + (int)(mastery * 12f);
		double range = 18d + mastery * 10d;
		EntityLivingBase focusedTarget = null;
		RayTraceResult lookedAt = ProcedureUtils.objectEntityLookingAt(user, range, 2.5d);
		if (lookedAt != null && lookedAt.entityHit instanceof EntityLivingBase && !lookedAt.entityHit.equals(user)) {
			focusedTarget = (EntityLivingBase)lookedAt.entityHit;
		}
		user.world.playSound(null, user.posX, user.posY, user.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.9f, 1.8f);
		for (int i = 0; i < needles; i++) {
			double yaw = Math.toRadians(user.rotationYaw + (user.getRNG().nextDouble() - 0.5d) * (18d - mastery * 8d));
			double pitch = Math.toRadians(user.rotationPitch + (user.getRNG().nextDouble() - 0.5d) * (10d - mastery * 4d));
			Vec3d dir = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).normalize();
			Vec3d start = user.getPositionEyes(1f).add(dir.scale(0.8d));
			EntityLivingBase target = focusedTarget;
			double best = range;
			if (target != null) {
				best = Math.min(range, target.getDistance(user));
			}
			for (EntityLivingBase candidate : user.world.getEntitiesWithinAABB(EntityLivingBase.class,
					user.getEntityBoundingBox().grow(range, 4d, range))) {
				if (candidate.equals(user) || !candidate.isEntityAlive()) continue;
				Vec3d to = candidate.getPositionVector().addVector(0d, candidate.height * 0.5d, 0d).subtract(start);
				double distance = to.lengthVector();
				if (distance < best && to.normalize().dotProduct(dir) > 0.94d - mastery * 0.08d) {
					target = candidate;
					best = distance;
				}
			}
			Vec3d end = start.add(dir.scale(target != null ? best : range));
			Vec3d mid = start.add(end.subtract(start).scale(0.5d));
			Particles.spawnParticle(user.world, Particles.Types.SMOKE, mid.x, mid.y, mid.z,
			 6, 0.15d, 0.15d, 0.15d, dir.x * 0.05d, dir.y * 0.05d, dir.z * 0.05d, 0x8088E8FF, 15);
			if (target != null) {
				applyTenketsuHit(user, target, mastery);
				Particles.spawnParticle(user.world, Particles.Types.SMOKE, target.posX, target.posY + target.height * 0.5d, target.posZ,
				 10, 0.2d, 0.4d, 0.2d, 0d, 0.02d, 0d, 0x8088E8FF, 20);
			}
		}
		stack.getTagCompound().setLong("TenketsuHariCD", user.world.getTotalWorldTime() + 220 - (int)(mastery * 60f));
	}

	@Override
	public void initElements() {
		ItemArmor.ArmorMaterial enuma = EnumHelper.addArmorMaterial("BYAKUGAN", "narutomod:byakugan_", 25, new int[]{2, 5, 6, 15}, 0, null, 0.0F);
		
		this.elements.items.add(() -> new ItemDojutsu.Base(enuma) {
			@Override
			public ItemDojutsu.Type getType() {
				return ItemDojutsu.Type.BYAKUGAN;
			}
			
			@SideOnly(Side.CLIENT)
			@Override
			public ModelBiped getArmorModel(EntityLivingBase living, ItemStack stack, EntityEquipmentSlot slot, ModelBiped defaultModel) {
				ItemDojutsu.ClientModel.ModelHelmetSnug armorModel = (ItemDojutsu.ClientModel.ModelHelmetSnug)super.getArmorModel(living, stack, slot, defaultModel);
				armorModel.headwearHide = true;
				armorModel.onface.showModel = living.getEntityData().getBoolean("byakugan_activated") || EntityEightTrigrams.EntityCustom.isActivated(living)
				 || living.getRidingEntity() instanceof EntityHakkeshoKeiten.EntityCustom;
				armorModel.highlightHide = !armorModel.onface.showModel;
				return armorModel;
			}

			@Override
			public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
				return isRinnesharinganActivated(stack) 
				 ? "narutomod:textures/byakurinnesharingan_helmet.png" : "narutomod:textures/byakuganhelmet.png";
			}

			@Override
			public void onArmorTick(World world, EntityPlayer entity, ItemStack itemstack) {
				super.onArmorTick(world, entity, itemstack);
				this.isOwner(itemstack, entity);
				int x = (int) entity.posX;
				int y = (int) entity.posY;
				int z = (int) entity.posZ;
				HashMap<String, Object> $_dependencies = Maps.newHashMap();
				$_dependencies.put("entity", entity);
				$_dependencies.put("world", world);
				$_dependencies.put("itemstack", itemstack);
				ProcedureByakuganHelmetTickEvent.executeProcedure($_dependencies);
			}

			@Override
			public void onUpdate(ItemStack itemstack, World world, Entity entity, int par4, boolean par5) {
				super.onUpdate(itemstack, world, entity, par4, par5);
				if (!world.isRemote && entity instanceof EntityLivingBase && entity.ticksExisted % 20 == 0
				 && this.isOwner(itemstack, (EntityLivingBase)entity)
				 && itemstack.hasTagCompound() && itemstack.getTagCompound().hasKey(TENSEIGANEVOLVEDTIME)) {
					double d = itemstack.getTagCompound().getDouble(TENSEIGANEVOLVEDTIME) - 20d;
					itemstack.getTagCompound().setDouble(TENSEIGANEVOLVEDTIME, d);
					if (d <= 0.0d && entity instanceof EntityPlayerMP) {
						ItemStack oldstack = itemstack.copy();
						ItemStack newstack = new ItemStack(ItemTenseigan.helmet);
						((ItemDojutsu.Base)newstack.getItem()).setOwner(newstack, (EntityLivingBase)entity);
						newstack.getTagCompound().setDouble("ByakuganCount", itemstack.getTagCompound().getDouble("ByakuganCount"));
						((EntityPlayer)entity).inventory.setInventorySlotContents(getSlotId((EntityPlayer)entity, itemstack), newstack);
						oldstack.getTagCompound().removeTag("ByakuganCount");
						oldstack.getTagCompound().removeTag(TENSEIGANEVOLVEDTIME);
						ItemHandlerHelper.giveItemToPlayer((EntityPlayer)entity, oldstack);
						ProcedureUtils.grantAdvancement((EntityPlayerMP)entity, "narutomod:tenseigan_achieved", true);
					}
				}
			}

			@Override
			public void setOwner(ItemStack stack, EntityLivingBase entityIn) {
				super.setOwner(stack, entityIn);
				stack.getTagCompound().setDouble("ByakuganCount", 1.0d);
			}

			@Override
			public int getMaxDamage() {
				return 0;
			}

			@Override
			public boolean isDamageable() {
				return false;
			}

			@Override
			public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
				Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);
				if (slot == EntityEquipmentSlot.HEAD && isRinnesharinganActivated(stack)) {
					multimap.put(SharedMonsterAttributes.MAX_HEALTH.getName(),
					 new AttributeModifier(RINNESHARINGAN_MODIFIER, "byakurinnesharingan.maxhealth", 380d, 0));
				}
				return multimap;
			}

			@SideOnly(Side.CLIENT)
			@Override
			public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
				super.addInformation(stack, worldIn, tooltip, flagIn);
				if (isRinnesharinganActivated(stack)) {
					tooltip.add(TextFormatting.RED + I18n.translateToLocal("advancements.rinnesharinganactivated.title") + TextFormatting.WHITE);
					tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu1") + ": " + TextFormatting.GRAY + I18n.translateToLocal("tooltip.byakugan.jutsu1") + " (NXP:500)");
					tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu2") + ": " + TextFormatting.GRAY + I18n.translateToLocal("tooltip.byakurinnesharingan.jutsu2"));
					tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu3") + ": " + TextFormatting.GRAY + I18n.translateToLocal("entity.hakkeshokeiten.name") + " (NXP:1500)");
				} else {
					tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu1") + ": " + TextFormatting.GRAY + I18n.translateToLocal("tooltip.byakugan.jutsu1") + " (NXP:500)");
					if (Minecraft.getMinecraft().player != null && this.isOwner(stack, Minecraft.getMinecraft().player)) {
						tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu2") + ": " + TextFormatting.GRAY + I18n.translateToLocal("tooltip.byakugan.jutsu2") + " (NXP:1000)");
						tooltip.add(TextFormatting.ITALIC + "[Sneak] + " + I18n.translateToLocal("key.mcreator.specialjutsu2") + ": " + TextFormatting.GRAY + I18n.translateToLocal("tooltip.byakugan.tenketsu_hari"));
						tooltip.add(TextFormatting.ITALIC + I18n.translateToLocal("key.mcreator.specialjutsu3") + ": " + TextFormatting.GRAY + I18n.translateToLocal("entity.hakkeshokeiten.name") + " (NXP:1500)");
					}
				}
				if (stack.hasTagCompound()) {
					double d = stack.getTagCompound().getDouble(TENSEIGANEVOLVEDTIME);
					if (d > 0.0d) {
						tooltip.add(I18n.translateToLocal(TextFormatting.AQUA + I18n.translateToLocal("tooltip.byakugan.tenseigantime")
						 + (long)(d / 20d) + TextFormatting.WHITE));
					}
				}
			}

			@Override
			public boolean onJutsuKey1(boolean is_pressed, ItemStack stack, EntityPlayer entity) {
				Map<String, Object> $_dependencies = Maps.newHashMap();
				$_dependencies.put("is_pressed", is_pressed);
				$_dependencies.put("entity", entity);
				if (entity.isSneaking()) {
					ProcedureHakkeKusho.executeProcedure($_dependencies);
				} else {
					$_dependencies.put("x", (int)entity.posX);
					$_dependencies.put("y", (int)entity.posY);
					$_dependencies.put("z", (int)entity.posZ);
					$_dependencies.put("world", entity.world);
					ProcedureByakuganActivate.executeProcedure($_dependencies);
				}
				return true;
			}

			@Override
			public boolean onJutsuKey2(boolean is_pressed, ItemStack stack, EntityPlayer entity) {
				if (!is_pressed) {
					if (entity.isSneaking()) {
						castTenketsuHari(entity, stack);
						return true;
					}
					Map<String, Object> $_dependencies = Maps.newHashMap();
					$_dependencies.put("entity", entity);
					$_dependencies.put("world", entity.world);
					if (stack.hasTagCompound() && stack.getTagCompound().getBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED)) {
						ProcedureYomotsuHirasaka.executeProcedure($_dependencies);
					} else {
						ProcedureEightTrigrams64Palms.executeProcedure($_dependencies);
					}
				}
				return true;
			}

			@Override
			public boolean onJutsuKey3(boolean is_pressed, ItemStack stack, EntityPlayer entity) {
				Map<String, Object> $_dependencies = Maps.newHashMap();
				$_dependencies.put("is_pressed", is_pressed);
				$_dependencies.put("entity", entity);
				$_dependencies.put("world", entity.world);
				ProcedureHakkeshoKaiten.executeProcedure($_dependencies);
				return true;
			}

			@Override
			public boolean onSwitchJutsuKey(boolean is_pressed, ItemStack stack, EntityPlayer entity) {
				if (entity.getEntityData().getBoolean("byakugan_activated")) {
					if (is_pressed) {
						entity.getEntityData().setDouble("byakugan_fov", entity.getEntityData().getDouble("byakugan_fov") - 1);
						OverlayByakuganView.sendCustomData(entity, true, (float) entity.getEntityData().getDouble("byakugan_fov"));
					}
					return true;
				}
				return false;
			}
		}.setUnlocalizedName("byakuganhelmet").setRegistryName("byakuganhelmet").setCreativeTab(TabModTab.tab));
	}

	private static int getSlotId(EntityPlayer entity, ItemStack stack) {
		for (int i = 0; i < entity.inventory.getSizeInventory(); i++) {
			ItemStack stack1 = entity.inventory.getStackInSlot(i);
			if (stack != null && stack.equals(stack1)) {
				return i;
			}
		}
		return -1;
	}

	public static boolean wearingAny(EntityLivingBase entity) {
		return entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == helmet;
	}

	public static boolean isRinnesharinganActivated(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().getBoolean(RINNESHARINGAN_KEY);
	}

	public static boolean wearingRinnesharingan(EntityPlayer player) {
		ItemStack itemstack = player.inventory.armorInventory.get(3);
		return itemstack.getItem() == helmet && isRinnesharinganActivated(itemstack);
	}

	public static boolean hasRinnesharingan(EntityPlayer player) {
		ItemStack stack = ProcedureUtils.getItemStackIgnoreDurability(player.inventory, new ItemStack(helmet));
		return (stack != null && isRinnesharinganActivated(stack));
	}

	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(helmet, 0, new ModelResourceLocation("narutomod:byakuganhelmet", "inventory"));
	}
}
