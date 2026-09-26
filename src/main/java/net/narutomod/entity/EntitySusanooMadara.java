package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.Chakra;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.MadaraSusanooPolicy;
import net.narutomod.PlayerTracker;
import net.narutomod.SusanooCastController;
import net.narutomod.SusanooCastProfile;
import net.narutomod.item.ItemChokuto;
import net.narutomod.item.ItemJutsu;
import net.narutomod.item.ItemSharingan;
import net.narutomod.item.ItemShuriken;
import net.narutomod.procedure.ProcedureSusanoo;
import net.narutomod.procedure.ProcedureUtils;

/** Madara-specific progression stays in one entity, preserving rider, ownership and synchronized animation state. */
public class EntitySusanooMadara extends EntitySusanooBase {
    public enum Stage { RIBCAGE, SKELETAL, HUMANOID, LEGGED, ARMORED, PERFECT }
    private static final DataParameter<Integer> STAGE = integerKey();
    private static final DataParameter<Integer> PREVIOUS_STAGE = integerKey();
    private static final DataParameter<Integer> TRANSITION_START = integerKey();
    private static final DataParameter<Integer> CAST_PHASE = integerKey();
    private static final DataParameter<Integer> CAST_PROFILE = integerKey();
    private static final DataParameter<Integer> CAST_START = integerKey();
    private static final DataParameter<Integer> CAST_SEQUENCE = integerKey();
    private static final DataParameter<Integer> RECOVERY_PHASE = integerKey();
    private static final DataParameter<Integer> RECOVERY_AGE = integerKey();
    private static final DataParameter<Integer> ARM_MASK = integerKey();
    private static final DataParameter<Boolean> SHOW_SWORD = EntityDataManager.createKey(EntitySusanooMadara.class, DataSerializers.BOOLEAN);
    private static final String ARMOR_RECOVERY = "MadaraSusanooArmorRecovery";
    private UUID ownerUuid;
    private String castKey = "";
    private long armorBurstUntil;
    private long nextMagatamaTick;
    private EntitySusanooClothed.EntityMagatama bullet;

    private static DataParameter<Integer> integerKey() {
        return EntityDataManager.createKey(EntitySusanooMadara.class, DataSerializers.VARINT);
    }

    public EntitySusanooMadara(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
        setFlameColor(0x382869BE);
        applyStageDimensions();
    }

    /** Deliberately does not mount the owner; commitSpawn performs that only after validation and spawning succeed. */
    public EntitySusanooMadara(EntityPlayer owner) {
        this(owner.world);
        setLocationAndAngles(owner.posX, owner.posY, owner.posZ, owner.rotationYaw, 0);
        setOwnerPlayer(owner);
        playerXp = PlayerTracker.getBattleXp(owner);
        chakraUsageModifier = ProcedureUtils.isOriginalOwner(owner, net.narutomod.OcularAbilities.resolve(owner, "susanoo")) ? 1d : 2d;
        configureStageStats(false);
        dataManager.set(TRANSITION_START, now() - MadaraSusanooPolicy.TRANSITION_TICKS);
    }

    @Override protected void entityInit() {
        super.entityInit();
        dataManager.register(STAGE, 0);
        dataManager.register(PREVIOUS_STAGE, 0);
        dataManager.register(TRANSITION_START, -100);
        dataManager.register(CAST_PHASE, SusanooCastProfile.IDLE);
        dataManager.register(CAST_PROFILE, SusanooCastProfile.GENERIC);
        dataManager.register(CAST_START, 0);
        dataManager.register(CAST_SEQUENCE, 0);
        dataManager.register(RECOVERY_PHASE, SusanooCastProfile.HOLD);
        dataManager.register(RECOVERY_AGE, 0);
        dataManager.register(ARM_MASK, SusanooCastProfile.FRONT_ARMS);
        dataManager.register(SHOW_SWORD, false);
    }

    private int now() { return (int)world.getTotalWorldTime(); }
    private float elapsed(int timestamp, float partial) { return Math.max(0, now() - timestamp) + partial; }
    public int getStage() { return MadaraSusanooPolicy.clampStage(dataManager.get(STAGE)); }
    public int getPreviousStage() { return MadaraSusanooPolicy.clampStage(dataManager.get(PREVIOUS_STAGE)); }
    public float getVisualScale() { return MadaraSusanooPolicy.modelScale(getStage()); }
    public float getTransitionProgress(float partial) {
        return SusanooCastProfile.smooth(SusanooCastProfile.clamp01(elapsed(dataManager.get(TRANSITION_START), partial) / MadaraSusanooPolicy.TRANSITION_TICKS));
    }
    public boolean isTransitioning() { return getPreviousStage() != getStage() && getTransitionProgress(0) < 1; }
    public int getCastPhase() { return dataManager.get(CAST_PHASE); }
    public int getCastProfile() { return dataManager.get(CAST_PROFILE); }
    public int getCastSequence() { return dataManager.get(CAST_SEQUENCE); }
    public int getRecoveryPhase() { return dataManager.get(RECOVERY_PHASE); }
    public int getRecoveryAge() { return dataManager.get(RECOVERY_AGE); }
    public float getCastAge(float partial) { return elapsed(dataManager.get(CAST_START), partial); }
    public int getCastDuration() { return SusanooCastProfile.duration(getCastPhase()); }
    public int getSeal() { return SusanooCastProfile.primarySeal(getCastProfile(), getCastPhase(), getCastAge(0)); }
    public int getSecondarySeal() { return SusanooCastProfile.secondarySeal(getCastProfile(), getCastPhase(), getCastAge(0)); }
    public int getArmMask() { return dataManager.get(ARM_MASK); }
    public float getCastWeight(float partial) { return SusanooCastProfile.blendWeight(getCastPhase(), getCastAge(partial)); }
    public boolean isCasting() { return getCastPhase() != SusanooCastProfile.IDLE; }

    @Override protected void setOwnerPlayer(EntityLivingBase owner) {
        super.setOwnerPlayer(owner);
        ownerUuid = owner.getUniqueID();
    }

    @Override @Nullable public EntityLivingBase getOwnerPlayer() {
        EntityLivingBase owner = super.getOwnerPlayer();
        if (!world.isRemote && ownerUuid != null && (owner == null || !ownerUuid.equals(owner.getUniqueID()))) {
            EntityPlayer player = world.getPlayerEntityByUUID(ownerUuid);
            if (player != null) super.setOwnerPlayer(player);
            return player;
        }
        return owner;
    }

    public static boolean wearingMadara(EntityPlayer player) {
        ItemStack eyes = net.narutomod.OcularAbilities.resolve(player, "susanoo");
        return eyes.getItem() instanceof ItemSharingan.Base && ((ItemSharingan.Base)eyes.getItem()).getSubType() == ItemSharingan.Type.MADARA
            && ((ItemSharingan.Base)eyes.getItem()).isMangekyo() && !ItemSharingan.isBlinded(eyes);
    }

    private static boolean eternal(EntityPlayer player) {
        ItemStack eyes = net.narutomod.OcularAbilities.resolve(player, "susanoo");
        return eyes.getItem() instanceof ItemSharingan.Base && ((ItemSharingan.Base)eyes.getItem()).isEternal();
    }

    public boolean hasStageClearance(int stage) {
        float width = MadaraSusanooPolicy.width(stage), height = MadaraSusanooPolicy.height(stage);
        AxisAlignedBB box = new AxisAlignedBB(posX - width / 2, posY + .035, posZ - width / 2,
            posX + width / 2, posY + height, posZ + width / 2);
        if (box.minY < 0 || box.maxY >= world.getHeight() || !world.getWorldBorder().contains(box)
            || !world.isAreaLoaded(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) return false;
        return world.getCollisionBoxes(this, box).isEmpty();
    }

    public boolean tryUpgrade(EntityPlayer player) {
        if (world.isRemote || !player.equals(getOwnerPlayer()) || player.getRidingEntity() != this || !wearingMadara(player)
            || !player.isEntityAlive() || player.isSpectator() || isTransitioning() || isCasting()) return false;
        if (!net.narutomod.GenjutsuSession.canUse(player, null)) return false;
        int next = getStage() + 1;
        playerXp = PlayerTracker.getBattleXp(player);
        MadaraSusanooPolicy.LockReason lock = MadaraSusanooPolicy.lockReason(next, playerXp, eternal(player), player.isCreative());
        if (lock != MadaraSusanooPolicy.LockReason.NONE) {
            switch (lock) {
                case MAX_STAGE:
                    status(player, "message.narutomod.madara_stage_max");
                    break;
                case ETERNAL_REQUIRED:
                    status(player, net.narutomod.OcularSystem.enabled(player)
                        ? "message.narutomod.madara_perfect_implanted_ems"
                        : "message.narutomod.madara_perfect_ems");
                    break;
                case XP_REQUIRED:
                    status(player, "message.narutomod.madara_stage_xp", (long)MadaraSusanooPolicy.requiredXp(next), (long)Math.floor(playerXp));
                    break;
                default:
                    status(player, "message.narutomod.madara_stage_locked");
            }
            return false;
        }
        if (next == Stage.ARMORED.ordinal() && !eternal(player)
            && player.getEntityData().getLong(ARMOR_RECOVERY) > world.getTotalWorldTime()) {
            status(player, "message.narutomod.madara_armor_recovery");
            return false;
        }
        // Never charge or disturb the rider until the full new collision volume is valid.
        if (!hasStageClearance(next)) {
            status(player, "message.narutomod.madara_no_space");
            return false;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(ProcedureSusanoo.BASE_CHAKRA_USAGE)) return false;
        if (next == Stage.ARMORED.ordinal() && !eternal(player)) {
            armorBurstUntil = world.getTotalWorldTime() + MadaraSusanooPolicy.ARMORED_BURST_TICKS;
            player.getEntityData().setLong(ARMOR_RECOVERY, armorBurstUntil + MadaraSusanooPolicy.ARMORED_RECOVERY_TICKS);
        }
        changeStage(next);
        return true;
    }

    private void changeStage(int stage) {
        dataManager.set(PREVIOUS_STAGE, getStage());
        dataManager.set(TRANSITION_START, now());
        dataManager.set(STAGE, MadaraSusanooPolicy.clampStage(stage));
        configureStageStats(true);
        killBullet();
        net.minecraft.util.SoundEvent sound = net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:charging_chakra"));
        playSound(sound != null ? sound : SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, .65f, stage == Stage.PERFECT.ordinal() ? .75f : 1.05f);
        EntityLivingBase owner = getOwnerPlayer();
        if (owner != null) updatePassenger(owner);
    }

    private void configureStageStats(boolean retainHealthFraction) {
        float fraction = retainHealthFraction && getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1;
        int stage = getStage();
        double multiplier = stage == 5 ? 18 : stage == 4 ? 9 : stage == 3 ? 6 : stage == 2 ? 3 : stage == 1 ? 1.7 : 1;
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Math.max(100, Math.sqrt(Math.max(playerXp, 0))) * multiplier);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(stage == 0 ? 0 : Math.min(playerXp, 40000) * .003 * (stage >= 4 ? 1.15 : .75));
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(stage < 3 ? .1 : stage == 5 ? .32 : .22);
        getEntityAttribute(EntityPlayer.REACH_DISTANCE).setBaseValue(stage == 0 ? 0 : 4 + stage * 1.5);
        setHealth(Math.max(1, getMaxHealth() * fraction));
        chakraUsage = MadaraSusanooPolicy.chakraPerSecond(stage);
        applyStageDimensions();
    }

    private void applyStageDimensions() {
        double x = posX, y = posY, z = posZ;
        setSize(MadaraSusanooPolicy.width(getStage()), MadaraSusanooPolicy.height(getStage()));
        setPosition(x, y, z);
        stepHeight = Math.min(2, height / 3);
        getEntityData().setDouble("entityModelScale", getVisualScale());
    }

    @Override public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (STAGE.equals(key) && world.isRemote) applyStageDimensions();
    }

    @Override public double getMountedYOffset() {
        double previous = MadaraSusanooPolicy.riderHeight(getPreviousStage());
        return previous + (MadaraSusanooPolicy.riderHeight(getStage()) - previous) * getTransitionProgress(0);
    }

    public void beginCast(int profile, String key) {
        if (world.isRemote || getStage() == Stage.RIBCAGE.ordinal()) return;
        castKey = key;
        dataManager.set(CAST_PROFILE, SusanooCastProfile.sanitize(profile));
        dataManager.set(ARM_MASK, getStage() >= Stage.HUMANOID.ordinal() ? SusanooCastProfile.ALL_ARMS : SusanooCastProfile.FRONT_ARMS);
        dataManager.set(CAST_SEQUENCE, getCastSequence() + 1);
        phase(SusanooCastProfile.PREPARE);
        setShowSword(false);
        killBullet();
    }

    public void ensureCastProfile(int profile, String key) {
        if (world.isRemote || getStage() == Stage.RIBCAGE.ordinal()) return;
        if (!isCasting() || !castKey.equals(key)) beginCast(profile, key);
    }

    public void holdCast(int profile, String key) {
        ensureCastProfile(profile, key);
        if (getCastPhase() == SusanooCastProfile.PREPARE && getCastAge(0) >= SusanooCastProfile.PREPARE_TICKS) phase(SusanooCastProfile.HOLD);
    }

    public void finishCast(boolean success) {
        if (world.isRemote || getStage() == Stage.RIBCAGE.ordinal()) return;
        if (success) {
            phase(SusanooCastProfile.RELEASE);
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, .55f, .75f);
        } else if (getCastPhase() == SusanooCastProfile.PREPARE || getCastPhase() == SusanooCastProfile.HOLD) phase(SusanooCastProfile.CANCEL);
    }

    private void phase(int phase) {
        if (phase == SusanooCastProfile.CANCEL || phase == SusanooCastProfile.RELEASE) {
            dataManager.set(RECOVERY_PHASE, getCastPhase());
            dataManager.set(RECOVERY_AGE, (int)getCastAge(0));
        }
        dataManager.set(CAST_START, now());
        dataManager.set(CAST_PHASE, phase);
    }

    private void updateCast(EntityPlayer owner) {
        int phase = getCastPhase();
        if (phase == SusanooCastProfile.PREPARE || phase == SusanooCastProfile.HOLD) {
            ItemStack active = owner.getActiveItemStack();
            if (!owner.isHandActive() || active.isEmpty()
                || !castKey.startsWith(active.getItem().getRegistryName() + "/")) finishCast(false);
            else if (phase == SusanooCastProfile.PREPARE && getCastAge(0) >= SusanooCastProfile.PREPARE_TICKS) phase(SusanooCastProfile.HOLD);
        } else if ((phase == SusanooCastProfile.RELEASE || phase == SusanooCastProfile.CANCEL) && getCastAge(0) >= getCastDuration()) {
            phase(SusanooCastProfile.IDLE);
            castKey = "";
        }
    }

    @Override public boolean shouldShowSword() { return dataManager.get(SHOW_SWORD) && !isCasting() && getStage() >= Stage.HUMANOID.ordinal(); }
    @Override public void setShowSword(boolean show) { dataManager.set(SHOW_SWORD, show && !isCasting() && getStage() >= Stage.HUMANOID.ordinal()); }

    @Override public boolean attackEntityAsMob(Entity target) {
        // Additional limbs are presentation, never an implicit 4x attack multiplier.
        if (getStage() == Stage.RIBCAGE.ordinal() || isCasting() || isTransitioning()) return false;
        EntityLivingBase owner = getOwnerPlayer();
        if (owner == null || !net.narutomod.GenjutsuSession.canUse(owner, null)) return false;
        return super.attackEntityAsMob(target);
    }

    @Override protected void showHeldWeapons() {
        EntityLivingBase owner = getOwnerPlayer();
        if (world.isRemote || owner == null) return;
        setShowSword(owner.getHeldItemMainhand().getItem() == ItemChokuto.block);
        if (!isCasting() && owner.getHeldItemMainhand().getItem() == ItemShuriken.block && owner.isHandActive()) createBullet(Math.min(2, getVisualScale() * .3f));
        else killBullet();
    }

    @Override public void createBullet(float size) {
        EntityLivingBase owner = getOwnerPlayer();
        if (world.isRemote || owner == null || getStage() < Stage.HUMANOID.ordinal() || isCasting() || isTransitioning() || world.getTotalWorldTime() < nextMagatamaTick) return;
        if (bullet == null || bullet.isDead) {
            // Existing registered projectile, but the player remains the shooter for damage/PvP attribution.
            bullet = new EntitySusanooClothed.EntityMagatama(owner, 0xE02869BE, MathHelper.clamp(size, .8f, 2f));
            if (!world.spawnEntity(bullet)) bullet = null;
        }
    }

    @Override public void attackEntityRanged(double x, double y, double z) {
        tryAttackEntityRanged(x, y, z);
    }

    /** The shuriken item needs this result so rejected/cooldown-limited throws do not consume ammunition. */
    public boolean tryAttackEntityRanged(double x, double y, double z) {
        if (world.isRemote || isCasting() || isTransitioning() || getStage() < Stage.HUMANOID.ordinal()
            || world.getTotalWorldTime() < nextMagatamaTick) return false;
        EntityLivingBase owner = getOwnerPlayer();
        if (owner == null || owner.getRidingEntity() != this || !owner.isEntityAlive()
            || !net.narutomod.GenjutsuSession.canUse(owner, null)) return false;
        if (bullet == null) createBullet(Math.min(2, getVisualScale() * .3f));
        if (bullet == null) return false;
        if (!(owner instanceof EntityPlayer) || !((EntityPlayer)owner).isCreative()) {
            if (!Chakra.pathway(owner).consume(80d)) { killBullet(); return false; }
        }
        bullet.shoot(x, y, z, .99f, 0);
        bullet = null;
        nextMagatamaTick = world.getTotalWorldTime() + 30;
        swingArm(EnumHand.MAIN_HAND);
        return true;
    }

    @Override public void attackEntityWithRangedAttack(EntityLivingBase target, float factor) {
        Vec3d vec = target.getPositionEyes(1).subtract(getPositionEyes(1));
        attackEntityRanged(vec.x, vec.y, vec.z);
    }

    @Override public void killBullet() {
        if (bullet != null) { bullet.setDead(); bullet = null; }
    }

    @Override public void onLivingUpdate() {
        EntityLivingBase living = getOwnerPlayer();
        if (!world.isRemote && living instanceof EntityPlayer) {
            EntityPlayer owner = (EntityPlayer)living;
            if (!wearingMadara(owner) || owner.dimension != dimension) { setDead(); return; }
            // Allow the initial passenger restore window when loading a saved entity.
            if (ticksExisted > 20 && owner.getRidingEntity() != this) { setDead(); return; }
            if (owner.getRidingEntity() == this && ProcedureSusanoo.getSummonedSusanooId(owner) != getEntityId()) {
                ProcedureSusanoo.trackSummon(owner, this); // Entity IDs change after a saved world is reloaded.
            }
            if (getStage() == Stage.PERFECT.ordinal() && !eternal(owner)) { setDead(); return; }
            if (getStage() == Stage.ARMORED.ordinal() && !eternal(owner)
                && (armorBurstUntil <= 0 || world.getTotalWorldTime() >= armorBurstUntil)) {
                changeStage(Stage.LEGGED.ordinal());
                armorBurstUntil = 0;
                status(owner, "message.narutomod.madara_armor_expired");
            }
            updateCast(owner);
            showHeldWeapons();
            if (owner.swingProgressInt == -1 && !isCasting()) swingArm(EnumHand.MAIN_HAND);
        }
        super.onLivingUpdate();
    }

    @Override protected int getAuraParticleCount() {
        // Extra hands and a 16-block form must not multiply per-tick particle pressure for every observer.
        EntityPlayer viewer = world.getClosestPlayer(posX, posY + height * .5, posZ, 48, false);
        return viewer == null ? 0 : viewer.getDistanceSq(this) > 24 * 24 ? 1 : Math.min(5, 2 + getStage());
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (ownerUuid != null) tag.setUniqueId("MadaraOwner", ownerUuid);
        tag.setInteger("MadaraStage", getStage());
        tag.setDouble("MadaraBattleXp", playerXp);
        tag.setDouble("MadaraDrainModifier", chakraUsageModifier);
        tag.setLong("MadaraArmorBurstUntil", armorBurstUntil);
    }

    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        ownerUuid = tag.hasUniqueId("MadaraOwner") ? tag.getUniqueId("MadaraOwner") : null;
        dataManager.set(STAGE, MadaraSusanooPolicy.clampStage(tag.getInteger("MadaraStage")));
        dataManager.set(PREVIOUS_STAGE, getStage());
        dataManager.set(TRANSITION_START, now() - MadaraSusanooPolicy.TRANSITION_TICKS);
        playerXp = Math.max(0, tag.getDouble("MadaraBattleXp"));
        chakraUsageModifier = tag.getDouble("MadaraDrainModifier") == 1 ? 1 : 2;
        armorBurstUntil = tag.getLong("MadaraArmorBurstUntil");
        configureStageStats(true);
        phase(SusanooCastProfile.IDLE);
    }

    @Override public void setDead() {
        if (!isDead && !world.isRemote) {
            EntityLivingBase owner = getOwnerPlayer();
            if (owner instanceof EntityPlayer && ProcedureSusanoo.getSummonedSusanooId(owner) == getEntityId()) {
                ProcedureSusanoo.clearTrackedSummon((EntityPlayer)owner, this);
            }
        }
        killBullet();
        super.setDead();
    }

    private static void status(EntityPlayer player, String key, Object... arguments) {
        player.sendStatusMessage(new TextComponentTranslation(key, arguments), true);
    }

    @ElementsNarutomodMod.ModElement.Tag
    public static class Registration extends ElementsNarutomodMod.ModElement {
        public Registration(ElementsNarutomodMod instance) { super(instance, 1105); }
        @Override public void initElements() {
            elements.entities.add(() -> EntityEntryBuilder.create().entity(EntitySusanooMadara.class)
                .id(new ResourceLocation("narutomod:susanoo_madara"), 9350).name("susanoo_madara").tracker(128, 1, true).build());
        }
        @Override public void preInit(FMLPreInitializationEvent event) { new Renderer().register(); }
    }

    public static class Renderer extends EntityRendererRegister {
        @Override @SideOnly(Side.CLIENT) public void register() { net.narutomod.client.RenderSusanooMadara.register(); }
    }
}
