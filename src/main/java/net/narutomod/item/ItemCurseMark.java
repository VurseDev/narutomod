package net.narutomod.item;

import java.util.List;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.model.ModelLoader;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.narutomod.Chakra;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.Particles;
import net.narutomod.creativetab.TabModTab;
import net.narutomod.procedure.ProcedureUtils;

/**
 * Orochimaru's cursed seals.  These deliberately use the same held-jutsu
 * vocabulary as Sage Mode, but are short high-risk transformations rather than
 * permanent stat sticks.  The seal item is also the mastery record and owner
 * record, which keeps a distributed RP item from becoming a reusable buff.
 */
@ElementsNarutomodMod.ModElement.Tag
public class ItemCurseMark extends ElementsNarutomodMod.ModElement {
	@ObjectHolder("narutomod:curse_mark_heaven") public static final Item HEAVEN = null;
	@ObjectHolder("narutomod:curse_mark_earth") public static final Item EARTH = null;
	@ObjectHolder("narutomod:curse_mark_jirobo") public static final Item JIROBO = null;
	@ObjectHolder("narutomod:curse_mark_kidomaru") public static final Item KIDOMARU = null;
	@ObjectHolder("narutomod:curse_mark_tayuya") public static final Item TAYUYA = null;
	@ObjectHolder("narutomod:curse_mark_sakon_ukon") public static final Item SAKON_UKON = null;
	@ObjectHolder("narutomod:curse_mark_animal") public static final Item ANIMAL = null;
	@ObjectHolder("narutomod:curse_mark_prisoners") public static final Item PRISONERS = null;
	@ObjectHolder("narutomod:curse_mark_guren_team") public static final Item GUREN_TEAM = null;
	@ObjectHolder("narutomod:curse_mark_iburi") public static final Item IBURI = null;

	private static final String STAGE = "CurseMarkStage";
	private static final String CORRUPTION = "CurseMarkCorruption";
	private static final String EXPIRES = "CurseMarkExpires";
	private static final String READY = "CurseMarkReady";
	private static final int STAGE_ONE_TICKS = 1200;
	private static final int STAGE_TWO_TICKS = 720;

	public static final ItemJutsu.JutsuEnum RELEASE = new ItemJutsu.JutsuEnum(0,
	 "jutsu.narutomod.curse_mark_release", 'B', 55d, new Release()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum AWAKEN = new ItemJutsu.JutsuEnum(1,
	 "jutsu.narutomod.curse_mark_second_state", 'A', 130d, new Awaken()).withCustomBalance();
	public static final ItemJutsu.JutsuEnum SIGNATURE = new ItemJutsu.JutsuEnum(2,
	 "jutsu.narutomod.curse_mark_signature", 'A', 90d, new Signature()).withCustomBalance();

	public ItemCurseMark(ElementsNarutomodMod instance) {
		super(instance, 1027);
	}

	@Override
	public void initElements() {
		for (Mark mark : Mark.values()) {
			this.elements.items.add(() -> new RangedItem(mark));
		}
	}

	@Override
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new EventHook());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModels(ModelRegistryEvent event) {
		for (Mark mark : Mark.values()) {
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("narutomod", mark.registryName));
			if (item != null) {
				ModelLoader.setCustomModelResourceLocation(item, 0,
					new ModelResourceLocation("narutomod:" + mark.registryName, "inventory"));
			}
		}
	}

	private static NBTTagCompound data(ItemStack stack) {
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		return stack.getTagCompound();
	}

	public static int getStage(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger(STAGE) : 0;
	}

	public static float getCorruption(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getFloat(CORRUPTION) : 0f;
	}

	private static void setStage(ItemStack stack, int stage, EntityLivingBase owner) {
		NBTTagCompound tag = data(stack);
		tag.setInteger(STAGE, stage);
		if (stage > 0) {
			tag.setLong(EXPIRES, owner.world.getTotalWorldTime() + (stage == 2 ? STAGE_TWO_TICKS : STAGE_ONE_TICKS));
		}
	}

	public static void deactivate(ItemStack stack, EntityLivingBase owner, boolean backlash) {
		if (getStage(stack) == 0) return;
		NBTTagCompound tag = data(stack);
		tag.setInteger(STAGE, 0);
		tag.removeTag(EXPIRES);
		if (!owner.world.isRemote) {
			burst(owner, 0xA025172A, 22, 1.15d);
			owner.world.playSound(null, owner.posX, owner.posY, owner.posZ,
				SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.7f, 0.55f);
			if (backlash && !(owner instanceof EntityPlayer && ((EntityPlayer)owner).isCreative())) {
				owner.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 600, 1, false, false));
				owner.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 300, 1, false, false));
				owner.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 180, 0, false, false));
			}
		}
	}

	private static void deactivateOtherMarks(EntityPlayer player, ItemStack except) {
		for (ItemStack candidate : player.inventory.mainInventory) {
			if (candidate != except && candidate.getItem() instanceof RangedItem) {
				deactivate(candidate, player, false);
			}
		}
		for (ItemStack candidate : player.inventory.offHandInventory) {
			if (candidate != except && candidate.getItem() instanceof RangedItem) {
				deactivate(candidate, player, false);
			}
		}
	}

	private static void burst(EntityLivingBase entity, int color, int count, double radius) {
		Particles.spawnParticle(entity.world, Particles.Types.SMOKE, entity.posX, entity.posY + entity.height * 0.55d, entity.posZ,
			count, radius, radius * 0.7d, radius, 0d, 0.055d, 0d, color, 28, 4, 0xF0, entity.getEntityId());
		Particles.spawnParticle(entity.world, Particles.Types.EXPANDING_SPHERE, entity.posX, entity.posY + entity.height * 0.5d, entity.posZ,
			1, 0d, 0d, 0d, 0d, 0d, 0d, Math.max(11, (int)(radius * 13d)), 9, color);
	}

	private static void line(EntityLivingBase caster, Vec3d from, Vec3d to, int color, int steps) {
		Vec3d delta = to.subtract(from);
		for (int i = 0; i <= steps; i++) {
			Vec3d point = from.add(delta.scale((double)i / (double)Math.max(1, steps)));
			Particles.spawnParticle(caster.world, Particles.Types.SMOKE, point.x, point.y, point.z, 2,
				0.06d, 0.06d, 0.06d, 0d, 0.012d, 0d, color, 13, 3, 0xF0);
		}
	}

	private static EntityLivingBase target(EntityLivingBase caster, double range, double grow) {
		RayTraceResult hit = ProcedureUtils.objectEntityLookingAt(caster, range, grow);
		return hit != null && hit.entityHit instanceof EntityLivingBase && ItemJutsu.canTarget(hit.entityHit)
			&& !hit.entityHit.equals(caster) ? (EntityLivingBase)hit.entityHit : null;
	}

	private static boolean hostile(EntityLivingBase caster, EntityLivingBase target) {
		return target != null && !target.equals(caster) && ItemJutsu.canTarget(target) && !caster.isOnSameTeam(target);
	}

	private static DamageSource source(EntityLivingBase caster) {
		return ItemJutsu.causeJutsuDamage(caster, caster);
	}

	public static class RangedItem extends ItemJutsu.Base {
		private final Mark mark;

		public RangedItem(Mark markIn) {
			super(ItemJutsu.JutsuEnum.Type.OTHER, RELEASE, AWAKEN, SIGNATURE);
			this.mark = markIn;
			this.setUnlocalizedName(markIn.registryName);
			this.setRegistryName(markIn.registryName);
			this.setCreativeTab(TabModTab.tab);
		}

		public Mark getMark() {
			return this.mark;
		}

		private void initialise(ItemStack stack) {
			NBTTagCompound tag = data(stack);
			if (!tag.getBoolean(READY)) {
				this.enableAllJutsus(stack, true);
				for (ItemJutsu.JutsuEnum jutsu : new ItemJutsu.JutsuEnum[]{RELEASE, AWAKEN, SIGNATURE}) {
					this.addJutsuXp(stack, jutsu, this.getRequiredXp(stack, jutsu));
				}
				tag.setBoolean(READY, true);
			}
		}

		@Override
		public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
			super.onUpdate(stack, world, entity, slot, selected);
			if (world.isRemote || !(entity instanceof EntityPlayer)) return;
			EntityPlayer player = (EntityPlayer)entity;
			this.initialise(stack);
			int stage = getStage(stack);
			if (stage <= 0) {
				if (player.ticksExisted % 20 == 0 && getCorruption(stack) > 0f) {
					data(stack).setFloat(CORRUPTION, Math.max(0f, getCorruption(stack) - 0.75f));
				}
				return;
			}
			NBTTagCompound tag = data(stack);
			if (tag.getLong(EXPIRES) <= world.getTotalWorldTime()) {
				deactivate(stack, player, false);
				player.sendStatusMessage(new TextComponentString("The " + this.mark.displayName + " recedes."), true);
				return;
			}
			if (player.ticksExisted % 20 == 0) {
				double drain = stage == 2 ? 64d : 28d;
				if (Chakra.pathway(player).getAmount() < drain) {
					deactivate(stack, player, true);
					player.sendStatusMessage(new TextComponentString("Your cursed seal has exhausted your chakra."), true);
					return;
				}
				Chakra.pathway(player).consume(drain);
				float corruption = getCorruption(stack) + (stage == 2 ? 6.5f : 2.0f);
				tag.setFloat(CORRUPTION, corruption);
				if (corruption >= 100f) {
					tag.setFloat(CORRUPTION, 70f);
					deactivate(stack, player, true);
					player.sendStatusMessage(new TextComponentString("The curse mark overwhelms your body."), true);
					return;
				}
				applyFormEffects(player, stage);
			}
			if (player.ticksExisted % 8 == 0) {
				burst(player, this.mark.color, stage == 2 ? 7 : 3, stage == 2 ? 0.9d : 0.45d);
			}
			if (stage == 2 && this.mark == Mark.HEAVEN && player.motionY < 0d && !player.onGround) {
				player.motionY *= 0.65d;
				player.fallDistance = Math.min(player.fallDistance, 1.5f);
			}
			if (stage == 2 && this.mark == Mark.KIDOMARU && player.collidedHorizontally && !player.onGround) {
				player.motionY = Math.max(player.motionY, 0.18d);
			}
		}

		private void applyFormEffects(EntityPlayer player, int stage) {
			int power = stage == 2 ? 1 : 0;
			player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 30, power + (this.mark == Mark.JIROBO ? 1 : 0), false, false));
			player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 30, power + (this.mark == Mark.ANIMAL ? 1 : 0), false, false));
			player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 30, power, false, false));
			if (stage == 2) player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 30, this.mark == Mark.ANIMAL ? 2 : 1, false, false));
			if (this.mark == Mark.EARTH) player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 30, power + 1, false, false));
			if (this.mark == Mark.IBURI && stage == 2) player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 30, 0, false, false));
		}

		@Override
		public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
			ItemStack stack = player.getHeldItem(hand);
			this.initialise(stack);
			if (!world.isRemote && this.getCurrentJutsu(stack) == RELEASE && getStage(stack) > 0) {
				deactivate(stack, player, false);
				player.sendStatusMessage(new TextComponentString("The " + this.mark.displayName + " is suppressed."), true);
				return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
			}
			return super.onItemRightClick(world, player, hand);
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void addInformation(ItemStack stack, World world, List<String> tip, ITooltipFlag advanced) {
			tip.add(TextFormatting.DARK_PURPLE + this.mark.lore);
			tip.add(TextFormatting.GRAY + "Stage I: 28 chakra/s | Stage II: 64 chakra/s");
			tip.add(TextFormatting.DARK_RED + "Corruption: " + String.format("%.0f", getCorruption(stack)) + "%");
			tip.add(TextFormatting.GRAY + "Release toggles the mark off. Stage II is short and unstable.");
			super.addInformation(stack, world, tip, advanced);
		}
	}

	public static class Release implements ItemJutsu.IJutsuCallback {
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!(stack.getItem() instanceof RangedItem) || !(entity instanceof EntityPlayer) || power < 1f) return false;
			EntityPlayer player = (EntityPlayer)entity;
			deactivateOtherMarks(player, stack);
			setStage(stack, 1, player);
			RangedItem mark = (RangedItem)stack.getItem();
			burst(player, mark.getMark().color, 42, 1.35d);
			player.world.playSound(null, player.posX, player.posY, player.posZ,
				SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.PLAYERS, 0.5f, 1.6f);
			player.sendStatusMessage(new TextComponentString(mark.getMark().displayName + " - Stage I released."), true);
			return true;
		}
		@Override public float getBasePower() { return 1f; }
		@Override public float getPowerupDelay() { return 14f; }
		@Override public float getMaxPower() { return 1.2f; }
	}

	public static class Awaken implements ItemJutsu.IJutsuCallback {
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase entity, float power) {
			if (!(stack.getItem() instanceof RangedItem) || getStage(stack) != 1 || power < 1.3f) return false;
			if (getCorruption(stack) >= 78f) {
				if (entity instanceof EntityPlayer) ((EntityPlayer)entity).sendStatusMessage(new TextComponentString("Your mark is too unstable for a second state."), true);
				return false;
			}
			setStage(stack, 2, entity);
			RangedItem mark = (RangedItem)stack.getItem();
			burst(entity, mark.getMark().color, 72, 2.0d);
			entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
				SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.45f, 1.5f);
			if (entity instanceof EntityPlayer) ((EntityPlayer)entity).sendStatusMessage(new TextComponentString(mark.getMark().secondState + " awakened."), true);
			return true;
		}
		@Override public float getBasePower() { return 1f; }
		@Override public float getPowerupDelay() { return 18f; }
		@Override public float getMaxPower() { return 1.5f; }
	}

	public static class Signature implements ItemJutsu.IJutsuCallback {
		@Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
			if (!(stack.getItem() instanceof RangedItem) || getStage(stack) == 0) return false;
			RangedItem item = (RangedItem)stack.getItem();
			Mark mark = item.getMark();
			EntityLivingBase target = target(caster, getStage(stack) == 2 ? 17d : 12d, 2.2d);
			Vec3d origin = caster.getPositionEyes(1f);
			if (mark == Mark.JIROBO || mark == Mark.TAYUYA || mark == Mark.PRISONERS) {
				for (EntityLivingBase victim : caster.world.getEntitiesWithinAABB(EntityLivingBase.class, caster.getEntityBoundingBox().grow(mark == Mark.TAYUYA ? 9d : 5d))) {
					if (!hostile(caster, victim)) continue;
					victim.hurtResistantTime = 0;
					victim.attackEntityFrom(source(caster), mark == Mark.JIROBO ? 7f + power * 2f : 4f + power);
					victim.addPotionEffect(new PotionEffect(mark == Mark.TAYUYA ? MobEffects.NAUSEA : MobEffects.SLOWNESS, 70, mark == Mark.JIROBO ? 1 : 2, false, false));
					if (mark == Mark.TAYUYA) victim.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100, 1, false, false));
					Vec3d away = victim.getPositionVector().subtract(caster.getPositionVector()).normalize();
					if (mark == Mark.JIROBO) ProcedureUtils.addVelocity(victim, away.x * 0.85d, 0.36d, away.z * 0.85d);
				}
				burst(caster, mark.color, 55, mark == Mark.TAYUYA ? 2.4d : 1.9d);
			} else if (mark == Mark.IBURI) {
				caster.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 180, 0, false, false));
				caster.addPotionEffect(new PotionEffect(MobEffects.SPEED, 180, 2, false, false));
				caster.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 90, 1, false, false));
				burst(caster, mark.color, 48, 1.8d);
			} else if (mark == Mark.GUREN_TEAM) {
				caster.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 120, 2, false, false));
				if (target != null) {
					line(caster, origin, target.getPositionEyes(1f), mark.color, 18);
					target.attackEntityFrom(source(caster), 7f + power * 2f);
					target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 70, 2, false, false));
				}
			} else if (target != null) {
				line(caster, origin, target.getPositionEyes(1f), mark.color, 20);
				target.hurtResistantTime = 0;
				float damage = 6f + power * 2.2f;
				if (mark == Mark.EARTH) damage += 3f;
				target.attackEntityFrom(source(caster), damage);
				if (mark == Mark.HEAVEN) {
					Vec3d pull = caster.getPositionVector().subtract(target.getPositionVector()).normalize();
					ProcedureUtils.addVelocity(target, pull.x * 0.9d, 0.18d, pull.z * 0.9d);
					target.addPotionEffect(new PotionEffect(MobEffects.POISON, 55, 0, false, false));
				} else if (mark == Mark.KIDOMARU) {
					target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 130, 3, false, false));
					target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 100, 1, false, false));
				} else if (mark == Mark.SAKON_UKON) {
					caster.heal(Math.min(7f, 2f + power * 1.6f));
					target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 1, false, false));
				} else if (mark == Mark.ANIMAL) {
					ProcedureUtils.addVelocity(caster, caster.getLookVec().x * 1.35d, 0.23d, caster.getLookVec().z * 1.35d);
					target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 50, 1, false, false));
				} else if (mark == Mark.PRISONERS) {
					target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 70, 0, false, false));
				}
				if (caster.world instanceof WorldServer) ((WorldServer)caster.world).spawnParticle(EnumParticleTypes.CRIT_MAGIC,
					target.posX, target.posY + target.height * 0.55d, target.posZ, 26, 0.45d, 0.45d, 0.45d, 0.12d);
			} else {
				if (caster instanceof EntityPlayer) ((EntityPlayer)caster).sendStatusMessage(new TextComponentString("No target for " + mark.signatureName + "."), true);
				return false;
			}
			caster.world.playSound(null, caster.posX, caster.posY, caster.posZ,
				mark == Mark.TAYUYA ? SoundEvents.BLOCK_NOTE_HARP : SoundEvents.ENTITY_IRONGOLEM_ATTACK,
				SoundCategory.PLAYERS, 1.0f, mark == Mark.TAYUYA ? 0.55f : 0.8f);
			item.setCurrentJutsuCooldown(stack, mark == Mark.TAYUYA ? 420L : 300L);
			return true;
		}
		@Override public float getBasePower() { return 1f; }
		@Override public float getPowerupDelay() { return 20f; }
		@Override public float getMaxPower() { return 1.8f; }
	}

	public static class EventHook {
		@SubscribeEvent
		public void onDeath(LivingDeathEvent event) {
			if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
			EntityPlayer player = (EntityPlayer)event.getEntityLiving();
			for (ItemStack stack : player.inventory.mainInventory) if (stack.getItem() instanceof RangedItem) deactivate(stack, player, false);
			for (ItemStack stack : player.inventory.offHandInventory) if (stack.getItem() instanceof RangedItem) deactivate(stack, player, false);
		}
	}

	public enum Mark {
		HEAVEN("curse_mark_heaven", "Cursed Seal of Heaven", "Sasuke / Anko: snake-limbed pursuit and winged second state.", "Heavenly Curse Mark: Second State", "Snake Limb", 0xB04A1737),
		EARTH("curse_mark_earth", "Cursed Seal of Earth", "Kimimaro: hardened body and deadly bone assault.", "Earth Curse Mark: Second State", "Clematis Bone Assault", 0xB0D5D0B8),
		JIROBO("curse_mark_jirobo", "Jirobo's Cursed Seal", "Sound Four: brute force, earth-shaking blows.", "Jirobo's Second State", "Earth Dome Smash", 0xB0794B24),
		KIDOMARU("curse_mark_kidomaru", "Kidomaru's Cursed Seal", "Sound Four: spider agility, wall scaling and binding webs.", "Kidomaru's Spider Second State", "Spider Web Bind", 0xB0553020),
		TAYUYA("curse_mark_tayuya", "Tayuya's Cursed Seal", "Sound Four: demonic flute pressure and battlefield genjutsu.", "Tayuya's Demonic Second State", "Demonic Flute Genjutsu", 0xB075235A),
		SAKON_UKON("curse_mark_sakon_ukon", "Sakon & Ukon's Cursed Seal", "Sound Four: parasitic assault that restores the user.", "Sakon & Ukon's Second State", "Parasitic Assimilation", 0xB0636831),
		ANIMAL("curse_mark_animal", "Animal Cursed Seal", "Mizuki: predatory speed, tiger-like transformation and pursuit.", "Animal Curse Mark: Tiger State", "Tiger Pursuit", 0xB0C6751D),
		PRISONERS("curse_mark_prisoners", "Prisoners' Cursed Seal", "Orochimaru's test-subject seal. A volatile resonant pulse.", "Prisoner Seal: Overdrive", "Seal Resonance", 0xB0661E38),
		GUREN_TEAM("curse_mark_guren_team", "Team Guren's Cursed Seal", "Orochimaru's experimental carrier seal with a crystallised defence.", "Carrier Seal: Second State", "Crystal Carrier Barrage", 0xB0B34A86),
		IBURI("curse_mark_iburi", "Iburi Clan Cursed Seal", "A smoke-bound seal: concealment, speed and unstable escape.", "Iburi Smoke Second State", "Smoke Body", 0xB06F6F78);

		public final String registryName;
		public final String displayName;
		public final String lore;
		public final String secondState;
		public final String signatureName;
		public final int color;
		Mark(String registryNameIn, String displayNameIn, String loreIn, String secondStateIn, String signatureNameIn, int colorIn) {
			this.registryName = registryNameIn;
			this.displayName = displayNameIn;
			this.lore = loreIn;
			this.secondState = secondStateIn;
			this.signatureName = signatureNameIn;
			this.color = colorIn;
		}
	}
}
