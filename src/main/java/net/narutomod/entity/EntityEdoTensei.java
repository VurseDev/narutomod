package net.narutomod.entity;

import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.narutomod.EdoSoulRegistry;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.Particles;
import net.narutomod.item.ItemJutsu;
import net.narutomod.item.ItemSummoningSouls;
import net.narutomod.item.ItemDnaSample;
import net.minecraftforge.items.ItemHandlerHelper;

/** A saved, server-timed presentation entity; preview bodies never enter the server entity list. */
@ElementsNarutomodMod.ModElement.Tag
public class EntityEdoTensei extends ElementsNarutomodMod.ModElement {
    public static final int ID = 9340;
    public EntityEdoTensei(ElementsNarutomodMod instance) { super(instance, 1031); }
    @Override public void initElements() {
        elements.entities.add(() -> EntityEntryBuilder.create().entity(Sequence.class)
            .id(new ResourceLocation("narutomod:edo_tensei_sequence"), ID).name("edo_tensei_sequence").tracker(96, 1, false).build());
        elements.entities.add(() -> EntityEntryBuilder.create().entity(EntityEdoReanimation.class)
            .id(new ResourceLocation("narutomod:edo_reanimation"), 9341).name("edo_reanimation").tracker(96, 2, true).build());
    }
    @Override public void preInit(FMLPreInitializationEvent event) { new Renderer().register(); }
    public static class Renderer extends EntityRendererRegister {
        @Override @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
        public void register() { net.narutomod.client.RenderEdoTensei.register(); net.narutomod.client.RenderEdoReanimation.register(); }
    }

    public static boolean begin(EntityPlayer caster, boolean ritual, NBTTagCompound soul) {
        World world = caster.world;
        if (world.isRemote) return false;
        EdoSoulRegistry archive=EdoSoulRegistry.get(world);
        ItemStack sampleStack=ItemStack.EMPTY;
        UUID sample=null;
        if(ritual) {
            // Explicit donor choice: hold their sample in the offhand while casting the ritual.
            sampleStack=caster.getHeldItemOffhand();
            sample=ItemDnaSample.sampleId(sampleStack);
            soul=sample==null?new NBTTagCompound():archive.getSample(sample);
            if(!soul.hasUniqueId("Player")) {
                caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_dna_required"),true);return false;
            }
            for(int i=0;i<archive.souls(caster.getUniqueID()).tagCount();i++) {
                NBTTagCompound existing=archive.souls(caster.getUniqueID()).getCompoundTagAt(i);
                if(existing.hasUniqueId("Player")&&existing.getUniqueId("Player").equals(soul.getUniqueId("Player"))) {
                    caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_known",soul.getString("Name")),true);return false;
                }
            }
        } else {
            if(soul==null || !soul.hasUniqueId("Player")) {
                caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_legacy"),true);return false;
            }
            if(archive.active(caster.getUniqueID(),soul.getUniqueId("Soul"))!=null) {
                caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_active"),true);return false;
            }
        }
        for (Sequence seq : world.getEntities(Sequence.class, e -> !e.isDead)) {
            if (caster.getUniqueID().equals(seq.owner)) {
                caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_busy"), true);
                return false;
            }
        }
        Vec3d eyes = caster.getPositionEyes(1f);
        RayTraceResult aim = world.rayTraceBlocks(eyes, eyes.add(caster.getLookVec().scale(8)), false, true, false);
        Vec3d front = caster.getPositionVector().add(Vec3d.fromPitchYaw(0, caster.rotationYaw).scale(4));
        RayTraceResult ground = aim != null && aim.sideHit == net.minecraft.util.EnumFacing.UP ? aim
            : world.rayTraceBlocks(front.addVector(0, 2, 0), front.addVector(0, -6, 0), false, true, false);
        if (ground == null || ground.sideHit != net.minecraft.util.EnumFacing.UP) return groundFailure(caster);
        BlockPos floor = ground.getBlockPos();
        Vec3d center = new Vec3d(floor.getX() + .5, floor.getY() + 1, floor.getZ() + .5);
        if (eyes.distanceTo(center) > 9 || world.rayTraceBlocks(eyes, center.addVector(0, .25, 0), false, true, false) != null)
            return groundFailure(caster);
        // Flat support gives the seal a stable plane, and leaves room for the full falling lid.
        int radius = 3;
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            BlockPos pos = floor.add(x, 0, z);
            if (!world.isBlockLoaded(pos) || !world.getBlockState(pos).isTopSolid()) return groundFailure(caster);
        }
        AxisAlignedBB space = new AxisAlignedBB(center.x - radius - .4, center.y + .05, center.z - radius - .4,
            center.x + radius + .4, center.y + 2.9, center.z + radius + .4);
        if (!world.getCollisionBoxes(null, space).isEmpty()) return groundFailure(caster);
        Sequence seq = new Sequence(world);
        seq.owner = caster.getUniqueID();
        seq.soul = soul.copy();
        seq.sample=sample;
        seq.syncIdentity();
        seq.getDataManager().set(Sequence.RITUAL, ritual);
        seq.setLocationAndAngles(center.x, center.y, center.z, caster.rotationYaw + 180, 0);
        if(ritual && !archive.claimSample(sample,seq.owner,seq.getUniqueID())) {
            caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_dna_used"),true);return false;
        }
        if(ritual) {sampleStack.shrink(1);caster.inventory.markDirty();}
        if (!world.spawnEntity(seq) || seq.isDead) {
            // Consume into escrow first: even a spawn-event cancellation cannot refund a sample twice.
            seq.setDead();
            return false;
        }
        caster.swingArm(EnumHand.MAIN_HAND);
        seq.playNaruto("kuchiyosenojutsu", 1f);
        return true;
    }

    private static boolean groundFailure(EntityPlayer player) {
        player.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_ground"), true);
        return false;
    }

    public static boolean cancelSummon(EntityPlayer player,UUID soulId) {
        boolean canceled=false;
        for(WorldServer world:player.world.getMinecraftServer().worlds) {
            for(Sequence seq:world.getEntities(Sequence.class,e->!e.isDead)) {
                if(!seq.ritual() && player.getUniqueID().equals(seq.owner)
                    && soulId.equals(seq.soul.getUniqueId("Soul"))) {seq.smoke();seq.setDead();canceled=true;}
            }
        }
        return canceled;
    }

    public static class Sequence extends Entity implements ItemJutsu.IJutsu {
        private static final DataParameter<Boolean> RITUAL = EntityDataManager.createKey(Sequence.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Integer> AGE = EntityDataManager.createKey(Sequence.class, DataSerializers.VARINT);
        private static final DataParameter<Integer> MOB = EntityDataManager.createKey(Sequence.class, DataSerializers.VARINT);
        private static final DataParameter<NBTTagCompound> IDENTITY = EntityDataManager.createKey(Sequence.class, DataSerializers.COMPOUND_TAG);
        private UUID owner;
        private UUID sample;
        private NBTTagCompound soul = new NBTTagCompound();
        private boolean released;
        public Sequence(World world) {
            super(world);
            setSize(7.0f, 3.0f);
            setNoGravity(true);
            isImmuneToFire = true;
            ignoreFrustumCheck = true;
        }
        @Override protected void entityInit() {
            dataManager.register(RITUAL, false);
            dataManager.register(AGE, 0);
            dataManager.register(MOB, 0);
            dataManager.register(IDENTITY,new NBTTagCompound());
        }
        @Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.NINJUTSU; }
        public boolean ritual() { return dataManager.get(RITUAL); }
        public int age() { return dataManager.get(AGE); }
        public int mob() { return dataManager.get(MOB); }
        public NBTTagCompound identity() { return dataManager.get(IDENTITY); }
        private void syncIdentity() {
            NBTTagCompound visible=new NBTTagCompound();
            if(soul.hasUniqueId("Player"))visible.setUniqueId("Player",soul.getUniqueId("Player"));
            visible.setString("Name",soul.getString("Name"));visible.setTag("Profile",soul.getCompoundTag("Profile").copy());
            dataManager.set(IDENTITY,visible);
        }
        public boolean showBody() { return ritual() ? age() >= 20 && age() < 96 : age() < 120; }
        public static float smooth(float t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
        public static float rise(float age) { return -2.8f * (1 - smooth(age / 60f)); }
        public static float lid(float age) { return smooth((age - 64) / 20f); }
        public static float sink(float age) { return -2.1f * smooth((age - 52) / 40f); }

        @Override public void onUpdate() {
            super.onUpdate();
            if (world.isRemote) return;
            if (owner == null) { setDead(); return; }
            EntityPlayer caster = world.getPlayerEntityByUUID(owner);
            // Interrupted rituals award nothing; the player can perform another after returning.
            if (caster == null || !caster.isEntityAlive() || caster.getDistance(this) > 96) { setDead(); return; }
            int age = age() + 1;
            dataManager.set(AGE, age);
            if (ritual()) {
                if(sample==null || !soul.hasUniqueId("Player")) {setDead();return;}
                if (age == 20) playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, .7f, .7f);
                if (age >= 52 && age <= 90 && age % 6 == 0) dirt(.8);
                if (age == 96) smoke();
                if (age >= 110) {
                    EdoSoulRegistry.get(world).complete(owner, getUniqueID(), soul);
                    sample=null; // Completion retains the archive claim; never refund a consumed registration.
                    ItemSummoningSouls.deliver(caster);
                    caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_complete"), true);
                    setDead();
                }
            } else {
                if (age <= 60 && age % 6 == 0) {
                    dirt(1.0);
                    if (age % 12 == 0) playSound(SoundEvents.BLOCK_GRAVEL_BREAK, .65f, .65f);
                }
                if (age == 64) playSound(SoundEvents.BLOCK_WOOD_BREAK, .9f, .6f);
                if (age == 84) { playSound(SoundEvents.BLOCK_WOOD_FALL, 1.2f, .65f); dirt(1.5); }
                if (age >= 120 && !released) {
                    if(!soul.hasUniqueId("Player")) {setDead();return;}
                    EntityEdoReanimation mob = new EntityEdoReanimation(world);
                    mob.setUniqueId(UUID.nameUUIDFromBytes(("edo:"+getUniqueID()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    UUID active=EdoSoulRegistry.get(world).active(owner,soul.getUniqueId("Soul"));
                    if(active!=null) {
                        if(active.equals(mob.getUniqueID())) {released=true;return;}
                        caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_active"),true);setDead();return;
                    }
                    mob.configure(owner,soul);
                    Vec3d forward = Vec3d.fromPitchYaw(0, rotationYaw);
                    mob.setLocationAndAngles(posX + forward.x * 1.05, posY + .1, posZ + forward.z * 1.05, rotationYaw, 0);
                    mob.enablePersistence();
                    mob.getEntityData().setUniqueId("EdoCaster", owner);
                    if (soul.hasUniqueId("Soul")) mob.getEntityData().setUniqueId("EdoSoul", soul.getUniqueId("Soul"));
                    if (!world.getCollisionBoxes(mob, mob.getEntityBoundingBox()).isEmpty()
                        || !EdoSoulRegistry.get(world).activate(owner,soul.getUniqueId("Soul"),mob.getUniqueID())) {
                        smoke();
                        caster.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_blocked"), true);
                        setDead();
                        return;
                    }
                    mob.bind();
                    if(!world.spawnEntity(mob)) {mob.setDead();smoke();setDead();return;}
                    released = true;
                }
                if (age == 144) smoke();
                if (age >= 150) setDead();
            }
        }
        private void dirt(double spread) {
            ((WorldServer)world).spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + .08, posZ,
                20, spread, .12, spread, .09, Block.getStateId(world.getBlockState(new BlockPos(posX, posY - 1, posZ))));
        }
        private void smoke() {
            Particles.spawnParticle(world, Particles.Types.SMOKE, posX, posY + 1, posZ, 70,
                .85, .8, .85, 0, .1, 0, 0xE8EEECE4, 28, 8, 0xF0);
            ((WorldServer)world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, posX, posY + .8, posZ, 40, .9, .8, .9, .08);
            playNaruto("poof", 1f);
        }
        private void playNaruto(String name, float volume) {
            SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod", name));
            if (sound != null) world.playSound(null, posX, posY, posZ, sound, SoundCategory.PLAYERS, volume, 1f);
        }
        @Override public void setDead() {
            if(!world.isRemote && !isDead && sample!=null && owner!=null) {
                EdoSoulRegistry archive=EdoSoulRegistry.get(world);
                if(archive.releaseSample(sample,owner,getUniqueID())) {
                    ItemStack refund=ItemDnaSample.create(sample,archive.getSample(sample));
                    EntityPlayer caster=world.getMinecraftServer().getPlayerList().getPlayerByUUID(owner);
                    if(caster!=null)ItemHandlerHelper.giveItemToPlayer(caster,refund);
                    else entityDropItem(refund,0);
                }
                sample=null;
            }
            super.setDead();
        }
        @Override protected void readEntityFromNBT(NBTTagCompound nbt) {
            owner = nbt.hasUniqueId("Owner") ? nbt.getUniqueId("Owner") : null;
            dataManager.set(RITUAL, nbt.getBoolean("Ritual"));
            dataManager.set(AGE, Math.max(0, nbt.getInteger("Age")));
            dataManager.set(MOB, Math.floorMod(nbt.getInteger("Mob"), 3));
            soul = nbt.getCompoundTag("Soul");
            sample=nbt.hasUniqueId("Sample")?nbt.getUniqueId("Sample"):null;
            syncIdentity();
            released = nbt.getBoolean("Released");
        }
        @Override protected void writeEntityToNBT(NBTTagCompound nbt) {
            if (owner != null) nbt.setUniqueId("Owner", owner);
            nbt.setBoolean("Ritual", ritual()); nbt.setInteger("Age", age()); nbt.setInteger("Mob", mob());
            nbt.setTag("Soul", soul); nbt.setBoolean("Released", released);
            if(sample!=null)nbt.setUniqueId("Sample",sample);
        }
    }
}
