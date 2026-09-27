package net.narutomod.item;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.block.material.Material;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import net.narutomod.ElementsNarutomodMod;
import net.narutomod.Particles;
import net.narutomod.SusanooCombat;
import net.narutomod.entity.EntityClone;
import net.narutomod.entity.EntityKageBunshin;
import net.narutomod.entity.EntityScalableProjectile;
import net.narutomod.entity.EntitySummonAnimal;
import net.narutomod.event.EventSphericalExplosion;
import net.narutomod.procedure.ProcedureCameraShake;
import net.narutomod.procedure.ProcedureUtils;

/** Three scroll techniques; server owns hit confirmation and all resource changes. */
@ElementsNarutomodMod.ModElement.Tag
public final class BloodlineTechniques extends ElementsNarutomodMod.ModElement {
    private static final int DRAGON_ID = 9381, COMPANY_ID = 9382, BOLT_ID = 9383, THROWN_ID = 9384;
    public BloodlineTechniques(ElementsNarutomodMod elements) { super(elements, 1151); }

    @Override public void initElements() {
        elements.entities.add(() -> EntityEntryBuilder.create().entity(TwinDragon.class)
            .id(new ResourceLocation("narutomod", "twin_flame_dragon"), DRAGON_ID).name("twin_flame_dragon").tracker(96, 1, true).build());
        elements.entities.add(() -> EntityEntryBuilder.create().entity(FlameCompany.class)
            .id(new ResourceLocation("narutomod", "flame_company"), COMPANY_ID).name("flame_company").tracker(80, 1, true).build());
        elements.entities.add(() -> EntityEntryBuilder.create().entity(FlameBolt.class)
            .id(new ResourceLocation("narutomod", "flame_company_bolt"), BOLT_ID).name("flame_company_bolt").tracker(80, 1, true).build());
        elements.entities.add(() -> EntityEntryBuilder.create().entity(ThrownClone.class)
            .id(new ResourceLocation("narutomod", "thrown_clone"), THROWN_ID).name("thrown_clone").tracker(80, 1, true).build());
    }

    @Override public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ConfirmedHit());
        if (event.getSide().isClient()) preInitClient(event);
    }

    @SideOnly(Side.CLIENT) private void preInitClient(FMLPreInitializationEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(TwinDragon.class, RenderBloodlineTechniques.Dragon::new);
        RenderingRegistry.registerEntityRenderingHandler(FlameCompany.class, RenderBloodlineTechniques.Company::new);
        RenderingRegistry.registerEntityRenderingHandler(FlameBolt.class, RenderBloodlineTechniques.Bolt::new);
        RenderingRegistry.registerEntityRenderingHandler(ThrownClone.class,
            manager -> EntityClone.ClientRLM.getInstance().new RenderClone<ThrownClone>(manager) {
                @Override protected void preRenderCallback(ThrownClone clone, float partial) {
                    super.preRenderCallback(clone, partial);
                    net.minecraft.client.renderer.GlStateManager.rotate(-57f + MathHelper.sin((clone.ticksExisted + partial) * .3f) * 7f, 1f, 0f, 0f);
                }
            });
    }

    private static boolean enemy(EntityLivingBase owner, EntityLivingBase target) {
        return owner != null && target != null && target != owner && target.isEntityAlive()
            && ItemJutsu.canTarget(target) && !owner.isOnSameTeam(target) && !target.isOnSameTeam(owner)
            && !SusanooCombat.isOwnSusanoo(target, owner)
            && !(target instanceof EntitySummonAnimal.ISummon && ((EntitySummonAnimal.ISummon)target).getSummoner() == owner)
            && !(target instanceof EntityPlayer && (((EntityPlayer)target).isCreative()
                || ((EntityPlayer)target).isSpectator() || owner instanceof EntityPlayer && !((EntityPlayer)owner).canAttackPlayer((EntityPlayer)target)))
            && !(target instanceof EntityClone._Base && ((EntityClone._Base)target).getSummoner() == owner);
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod", name));
    }

    private static void play(World world, Vec3d at, SoundEvent cue, float volume, float pitch) {
        if (cue != null) world.playSound(null, at.x, at.y, at.z, cue, SoundCategory.PLAYERS, volume, pitch);
    }

    private static void marker(World world, Vec3d at, int tick, float radius) {
        if (!(world instanceof WorldServer) || tick % 3 != 0) return;
        WorldServer server = (WorldServer)world;
        for (int i = 0; i < 20; i++) {
            double a = i * Math.PI * 2 / 20 + tick * .045;
            server.spawnParticle(EnumParticleTypes.FLAME, at.x + Math.cos(a) * radius, at.y + .08,
                at.z + Math.sin(a) * radius, 1, 0d, 0d, 0d, 0d, new int[0]);
        }
        for (int i = -2; i <= 2; i++)
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, at.x + i * .48, at.y + .12,
                at.z, 1, 0d, 0d, 0d, 0d, new int[0]);
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, double t) { return a.add(b.subtract(a).scale(t)); }

    private static float power(float value, float maximum) {
        return Float.isFinite(value) ? MathHelper.clamp(value, 1f, maximum) : 1f;
    }

    private static float mastery(ItemStack stack, ItemJutsu.JutsuEnum jutsu, EntityLivingBase caster) {
        return 1f + .25f * ItemJutsu.getJutsuMastery(stack, jutsu, caster);
    }

    // Swept hitboxes, not distance to an entity's feet. The same collision is
    // used by the fire shots and thrown clone; walls/water win when nearer.
    @Nullable static RayTraceResult firstHit(World world, Vec3d from, Vec3d to, EntityLivingBase owner, double padding) {
        RayTraceResult best = world.rayTraceBlocks(from, to, true, false, false);
        double distance = best != null ? from.squareDistanceTo(best.hitVec) : Double.MAX_VALUE;
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(from, to).grow(padding))) {
            if (!enemy(owner, victim)) continue;
            AxisAlignedBB box = victim.getEntityBoundingBox().grow(padding);
            RayTraceResult hit = box.contains(from) ? new RayTraceResult(from, null) : box.calculateIntercept(from, to);
            if (hit != null && from.squareDistanceTo(hit.hitVec) < distance) {
                distance = from.squareDistanceTo(hit.hitVec);
                best = new RayTraceResult(victim, hit.hitVec);
            }
        }
        return best;
    }

    private static Vec3d closest(Vec3d point, AxisAlignedBB box) {
        return new Vec3d(MathHelper.clamp(point.x, box.minX, box.maxX),
            MathHelper.clamp(point.y, box.minY, box.maxY), MathHelper.clamp(point.z, box.minZ, box.maxZ));
    }

    // One attributed hit per blast. Terrain destruction below must NOT add a
    // second vanilla explosion hit that bypasses a successful substitution.
    static int blast(Entity source, EntityLivingBase owner, float radius, float damage, int fireSeconds, float knockback) {
        if (source.world.isRemote || source.isDead) return 0;
        Vec3d origin = source.getPositionVector().addVector(0, .12, 0);
        int hits = 0;
        for (EntityLivingBase victim : source.world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(origin, origin).grow(radius))) {
            if (!enemy(owner, victim)) continue;
            AxisAlignedBB box = victim.getEntityBoundingBox();
            double distance = closest(origin, box).distanceTo(origin);
            if (distance > radius) continue;
            // Aim just inside the nearest face: works for players AND giant summons.
            Vec3d point = closest(origin, box).add(box.getCenter().subtract(closest(origin, box)).normalize().scale(.05));
            if (source.world.rayTraceBlocks(origin, point, true, false, false) != null) continue;
            float falloff = 1f - .4f * MathHelper.clamp((float)distance / radius, 0f, 1f);
            DamageSource damageSource = ItemJutsu.causeJutsuDamage(source, owner).setFireDamage().setExplosion();
            float statScale = owner instanceof EntityPlayer
                ? (float)net.narutomod.PlayerStats.getJutsuDamageMultiplier((EntityPlayer)owner) : 1f;
            if (!victim.attackEntityFrom(damageSource, damage * falloff * statScale) || !ItemJutsu.canTarget(victim)) continue;
            hits++;
            victim.setFire(fireSeconds);
            Vec3d away = box.getCenter().subtract(origin);
            if (away.x * away.x + away.z * away.z < .001) away = owner.getLookVec();
            victim.knockBack(source, knockback * falloff, -away.x, -away.z);
            victim.velocityChanged = true;
        }
        return hits;
    }

    private static void fireImpact(Entity source, EntityLivingBase owner, float radius, boolean terrain) {
        World world = source.world;
        Vec3d at = source.getPositionVector().addVector(0, .2, 0);
        // Reuse the mod's native animated fire, shockwave and resistance-aware
        // terrain explosion. mobGriefing governs the crater and ignition.
        Particles.spawnParticle(world, Particles.Types.EXPANDING_SPHERE, at.x, at.y, at.z,
            1, 0d, 0d, 0d, 0d, 0d, 0d, Math.round(radius * 12f), 12, 0xB0FF8A20);
        Particles.spawnParticle(world, Particles.Types.FLAME, at.x, at.y, at.z,
            35, radius * .4, radius * .2, radius * .4, 0d, .12d, 0d, 0xFFFF7800, 26);
        if (world instanceof WorldServer) {
            ((WorldServer)world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, at.x, at.y, at.z, 3, radius*.24, .25, radius*.24, 0d);
            ((WorldServer)world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, at.x, at.y, at.z, 28, radius*.35, .55, radius*.35, .05);
        }
        play(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, terrain ? 2.8f : 1.2f, terrain ? .72f : 1.15f);
        play(world, at, sound("flamethrow"), terrain ? 1.7f : .8f, .7f);
        ProcedureCameraShake.sendToClients(world.provider.getDimension(), at.x, at.y, at.z, terrain ? 32 : 18, terrain ? 12 : 5, terrain ? 1.6f : .45f);
        if (terrain) new EventSphericalExplosion(world, owner, MathHelper.floor(at.x), MathHelper.floor(at.y), MathHelper.floor(at.z),
            MathHelper.clamp(Math.round(radius * .6f), 2, 4), 0, true, .15f, true, false);
    }

    private static void quench(Entity source) {
        if (source.world instanceof WorldServer)
            ((WorldServer)source.world).spawnParticle(EnumParticleTypes.CLOUD, source.posX, source.posY, source.posZ, 20, .6, .5, .6, .04);
        play(source.world, source.getPositionVector(), SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.3f, .8f);
        source.setDead();
    }

    @Nullable private static EntityLivingBase resolve(World world, UUID id) {
        if (id == null || !(world instanceof WorldServer)) return null;
        Entity found = ((WorldServer)world).getEntityFromUuid(id);
        return found instanceof EntityLivingBase ? (EntityLivingBase)found : null;
    }

    public static final class TwinFlameDragons implements ItemJutsu.IJutsuCallback {
        @Override public float getBasePower() { return 1f; }
        @Override public float getPowerupDelay() { return 24f; }
        @Override public float getMaxPower() { return 2.8f; }
        @Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
            if (caster.world.isRemote || !Float.isFinite(power) || power < getBasePower()) return false;
            Vec3d eye = caster.getPositionEyes(1f);
            Vec3d end = eye.add(caster.getLookVec().scale(28));
            RayTraceResult block = caster.world.rayTraceBlocks(eye, end, false, true, false);
            RayTraceResult living = ProcedureUtils.objectEntityLookingAt(caster, 28, 1.25,
                false, false, e -> e instanceof EntityLivingBase && enemy(caster, (EntityLivingBase)e));
            Vec3d at = living != null && living.entityHit instanceof EntityLivingBase
                ? living.entityHit.getPositionVector() : block != null ? block.hitVec : end;
            if (at.distanceTo(caster.getPositionVector()) < 2 || at.distanceTo(caster.getPositionVector()) > 30) return false;
            if (block == null && (living == null || living.entityHit == null)) {
                BlockPos floor = new BlockPos(at);
                int top = ProcedureUtils.getTopSolidBlockY(caster.world, floor);
                at = new Vec3d(at.x, top + .1, at.z);
            }
            boolean spawned = false;
            for (int side : new int[] {-1, 1}) {
                Vec3d lateral = caster.getLookVec().crossProduct(new Vec3d(0, 1, 0)).normalize().scale(side * 1.3);
                Vec3d start = caster.getPositionVector().addVector(0, 2.5, 0).add(lateral);
                TwinDragon dragon = new TwinDragon(caster.world, caster, start, at, side, side < 0 ? 12 : 24, power);
                dragon.damageScale = mastery(stack, ItemKaton.TWINFLAMEDRAGONS, caster);
                spawned |= caster.world.spawnEntity(dragon);
            }
            if (!spawned) return false;
            play(caster.world, caster.getPositionVector(), sound("flamethrow"), 1.05f, .78f);
            // Unscaled setter so the rank floor always binds identically across the family.
            ItemJutsu.setCurrentJutsuCooldown(stack, 280);
            return true;
        }
    }

    public static final class FlameCompanyJutsu implements ItemJutsu.IJutsuCallback {
        @Override public float getBasePower() { return 1f; }
        @Override public float getPowerupDelay() { return 24f; }
        @Override public float getMaxPower() { return 2f; }
        @Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
            if (caster.world.isRemote || !Float.isFinite(power) || power < getBasePower()) return false;
            for (FlameCompany active : caster.world.getEntities(FlameCompany.class,
                e -> e.isEntityAlive() && caster.getUniqueID().equals(e.ownerId))) return false;
            FlameCompany company = new FlameCompany(caster.world, caster, power);
            company.damageScale = mastery(stack, ItemKaton.FLAMECOMPANY, caster);
            if (!caster.world.spawnEntity(company)) return false;
            play(caster.world, caster.getPositionVector(), sound("flamethrow"), .7f, 1.5f);
            ItemJutsu.setCurrentJutsuCooldown(stack, 420);
            return true;
        }
    }

    public static final class CloneThrow implements ItemJutsu.IJutsuCallback {
        @Override public float getBasePower() { return 1f; }
        @Override public float getPowerupDelay() { return 24f; }
        @Override public float getMaxPower() { return 2f; }
        @Override public boolean createJutsu(ItemStack stack, EntityLivingBase caster, float power) {
            if (caster.world.isRemote || !Float.isFinite(power) || power < getBasePower()) return false;
            EntityKageBunshin.EC chosen = null;
            double best = 100;
            EntityKageBunshin.EC original = EntityKageBunshin.getOriginalClone(caster);
            List<EntityKageBunshin.EC> clones = caster.world.getEntitiesWithinAABB(EntityKageBunshin.EC.class,
                caster.getEntityBoundingBox().grow(10));
            for (EntityKageBunshin.EC clone : clones) {
                double d = clone.getDistanceSq(caster);
                if (clone.isEntityAlive() && clone != original && clone.getSummoner() == caster && d < best) {
                    chosen = clone; best = d;
                }
            }
            if (chosen == null) {
                if (caster instanceof EntityPlayer)
                    ((EntityPlayer)caster).sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("message.narutomod.clone_throw_no_clone"), true);
                return false;
            }
            ThrownClone thrown = new ThrownClone(caster.world, caster, power);
            thrown.damageScale = mastery(stack, ItemNinjutsu.CLONETHROW, caster);
            Vec3d direction = caster.getLookVec().normalize();
            thrown.setPosition(caster.posX + direction.x, caster.posY + 1.1, caster.posZ + direction.z);
            double speed = .95 + .45 * (power - 1f);
            thrown.motionX = direction.x * speed;
            thrown.motionY = direction.y * speed + .12;
            thrown.motionZ = direction.z * speed;
            if (!caster.world.spawnEntity(thrown)) return false;
            chosen.consumeForThrow();
            play(caster.world, caster.getPositionVector(), sound("kagebunshin"), .95f, 1.25f);
            play(caster.world, caster.getPositionVector(), sound("windblast"), .7f, 1.2f);
            ItemJutsu.setCurrentJutsuCooldown(stack, 180);
            return true;
        }
    }

    public static final class TwinDragon extends Entity implements ItemJutsu.IJutsu {
        private static final DataParameter<Float> VISUAL_POWER = EntityDataManager.createKey(TwinDragon.class, DataSerializers.FLOAT);
        private static final DataParameter<Integer> CAST_DELAY = EntityDataManager.createKey(TwinDragon.class, DataSerializers.VARINT);
        private UUID ownerId;
        private Vec3d start = Vec3d.ZERO, target = Vec3d.ZERO;
        private int delay, side;
        private float power = 1f;
        private float damageScale = 1f;
        public TwinDragon(World world) { super(world); setSize(1.55f, 1.4f); noClip = true; isImmuneToFire = true; }
        TwinDragon(World world, EntityLivingBase owner, Vec3d start, Vec3d target, int side, int delay, float power) {
            this(world); this.ownerId=owner.getUniqueID(); this.start=start; this.target=target;
            this.side=side; this.delay=delay; this.power=power(power, 2.8f);
            dataManager.set(VISUAL_POWER, this.power);
            dataManager.set(CAST_DELAY, delay);
            setPosition(start.x, start.y, start.z);
        }
        @Override protected void entityInit() { dataManager.register(VISUAL_POWER,1f); dataManager.register(CAST_DELAY,12); }
        @Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.KATON; }
        @Override public boolean isImmuneToExplosions() { return true; }
        public float visualPower() { return dataManager.get(VISUAL_POWER); }
        public int getCastingDelay() { return dataManager.get(CAST_DELAY); }
        public float blastRadius() { return 4f + (power - 1f) * 1.15f; }
        @Override public void onUpdate() {
            if (isDead) return;
            super.onUpdate();
            if (world.isRemote) {
                if (ticksExisted > 1 && (motionX * motionX + motionY * motionY + motionZ * motionZ) > .015)
                    for (int i=0;i<3;i++) world.spawnParticle(EnumParticleTypes.FLAME,
                        posX + (rand.nextDouble()-.5)*.8, posY + rand.nextDouble()*.7,
                        posZ + (rand.nextDouble()-.5)*.8, -motionX*.06, .025, -motionZ*.06);
                return;
            }
            EntityLivingBase owner = resolve(world, ownerId);
            if (owner == null || !owner.isEntityAlive()) { setDead(); return; }
            if (isInWater()) { quench(this); return; }
            if (ticksExisted <= delay) { marker(world, target, ticksExisted, blastRadius()); return; }
            if (ticksExisted == delay + 1) play(world, getPositionVector(), sound("dragon_roar"), 1.05f, side < 0 ? .9f : 1.1f);
            double t = Math.min(1, (ticksExisted - delay) / 30.0);
            Vec3d from = getPositionVector();
            Vec3d lateral = new Vec3d(-(target.z-start.z), 0, target.x-start.x).normalize().scale(side*1.55*Math.sin(t*Math.PI));
            Vec3d next = lerp(start, target, t).add(lateral).addVector(0, Math.sin(t*Math.PI)*(6.5+power), 0);
            RayTraceResult obstruction = firstHit(world, from, next, owner, .85 + .3 * (power - 1));
            if (obstruction != null) {
                Vec3d at = obstruction.hitVec;
                if (obstruction.typeOfHit == RayTraceResult.Type.BLOCK) {
                    if (obstruction.sideHit != null) at = at.add(new Vec3d(obstruction.sideHit.getDirectionVec()).scale(.15));
                    setPosition(at.x, at.y, at.z);
                    if (world.getBlockState(obstruction.getBlockPos()).getMaterial() == Material.WATER) { quench(this); return; }
                } else setPosition(at.x, at.y, at.z);
                impact(owner); return;
            }
            motionX=next.x-from.x; motionY=next.y-from.y; motionZ=next.z-from.z;
            setPosition(next.x,next.y,next.z);
            rotationYaw = (float)(Math.atan2(motionZ,motionX)*180/Math.PI)-90;
            rotationPitch = (float)(-Math.atan2(motionY,Math.sqrt(motionX*motionX+motionZ*motionZ))*180/Math.PI);
            if (t >= 1) impact(owner);
        }
        private void impact(EntityLivingBase owner) {
            if (isDead) return;
            // Twelve-tick separation clears vanilla's damage window naturally.
            blast(this, owner, blastRadius(), (8f + power * 10f) * damageScale, 5, .55f + power * .25f);
            fireImpact(this, owner, blastRadius(), true);
            setDead();
        }
        @Override protected void readEntityFromNBT(NBTTagCompound tag) {
            ownerId=tag.hasUniqueId("Owner")?tag.getUniqueId("Owner"):null;
            start=new Vec3d(tag.getDouble("SX"),tag.getDouble("SY"),tag.getDouble("SZ"));
            target=new Vec3d(tag.getDouble("TX"),tag.getDouble("TY"),tag.getDouble("TZ"));
            delay=MathHelper.clamp(tag.getInteger("Delay"),0,24); side=tag.getInteger("Side") < 0 ? -1 : 1; power=power(tag.getFloat("Power"),2.8f);
            damageScale=power(tag.getFloat("DamageScale"),1.25f);
            // These are short-lived attack visuals, never persisted ammunition.
            // Old saves without Age are expired too, rather than replaying casts.
            ticksExisted=tag.hasKey("Age")?tag.getInteger("Age"):100;
            if (ticksExisted >= delay + 30) setDead();
            dataManager.set(VISUAL_POWER,power);
            dataManager.set(CAST_DELAY,delay);
        }
        @Override protected void writeEntityToNBT(NBTTagCompound tag) {
            if(ownerId!=null)tag.setUniqueId("Owner",ownerId);
            tag.setDouble("SX",start.x);tag.setDouble("SY",start.y);tag.setDouble("SZ",start.z);
            tag.setDouble("TX",target.x);tag.setDouble("TY",target.y);tag.setDouble("TZ",target.z);
            tag.setInteger("Delay",delay);tag.setInteger("Side",side);tag.setFloat("Power",power);
            tag.setInteger("Age",ticksExisted);tag.setFloat("DamageScale",damageScale);
        }
    }

    public static final class FlameCompany extends Entity implements ItemJutsu.IJutsu {
        private static final DataParameter<Integer> CHARGES = EntityDataManager.createKey(FlameCompany.class, DataSerializers.VARINT);
        private static final DataParameter<Float> VISUAL_POWER = EntityDataManager.createKey(FlameCompany.class, DataSerializers.FLOAT);
        UUID ownerId;
        private float power=1f;
        private float damageScale=1f;
        private long nextShot;
        public FlameCompany(World world) { super(world); setSize(.4f,.4f); noClip=true; isImmuneToFire=true; }
        FlameCompany(World world, EntityLivingBase owner, float power) {
            this(world); ownerId=owner.getUniqueID(); this.power=power(power,2f);
            dataManager.set(VISUAL_POWER,this.power);
            setPosition(owner.posX,owner.posY+1.5,owner.posZ);
        }
        @Override protected void entityInit() { dataManager.register(CHARGES,3); dataManager.register(VISUAL_POWER,1f); }
        @Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.KATON; }
        public float visualPower() { return dataManager.get(VISUAL_POWER); }
        public double orbitRadius() { return 1.25 + .25 * (visualPower()-1f); }
        public int charges() { return dataManager.get(CHARGES); }
        @Override public void onUpdate() {
            super.onUpdate();
            if(world.isRemote)return;
            EntityLivingBase owner=resolve(world,ownerId);
            if(owner==null || !owner.isEntityAlive() || owner.isInWater()
                || ticksExisted>200+Math.round((power-1f)*160f) || charges()<=0) {
                if(owner!=null && owner.isInWater()) play(world,getPositionVector(),SoundEvents.BLOCK_FIRE_EXTINGUISH,.9f,1f);
                setDead();return;
            }
            setPosition(owner.posX,owner.posY+1.5,owner.posZ);
        }
        void onConfirmedHit(EntityLivingBase target) {
            EntityLivingBase owner=resolve(world,ownerId);
            if(isDead || !enemy(owner,target) || owner.isInWater() || charges()<=0 || world.getTotalWorldTime()<nextShot || target.getDistanceSq(owner)>400) return;
            int index=3-charges();
            double angle=ticksExisted*.12 + index*Math.PI*2/3;
            Vec3d from=getPositionVector().addVector(Math.cos(angle)*orbitRadius(),.2,Math.sin(angle)*orbitRadius());
            FlameBolt bolt=new FlameBolt(world,owner,target,from,power);
            bolt.damageScale=damageScale;
            if(world.spawnEntity(bolt)) {
                dataManager.set(CHARGES,charges()-1);
                nextShot=world.getTotalWorldTime()+12;
                play(world,from,sound("flamethrow"),.56f,1.55f);
                if(charges()<=0)setDead();
            }
        }
        @Override protected void readEntityFromNBT(NBTTagCompound tag) {
            ownerId=tag.hasUniqueId("Owner")?tag.getUniqueId("Owner"):null;
            dataManager.set(CHARGES,MathHelper.clamp(tag.getInteger("Charges"),0,3));
            power=power(tag.getFloat("Power"),2f);nextShot=tag.getLong("NextShot");
            damageScale=power(tag.getFloat("DamageScale"),1.25f);
            dataManager.set(VISUAL_POWER,power);
            ticksExisted=tag.hasKey("Age")?tag.getInteger("Age"):400;
        }
        @Override protected void writeEntityToNBT(NBTTagCompound tag) {
            if(ownerId!=null)tag.setUniqueId("Owner",ownerId);
            tag.setInteger("Charges",charges());tag.setFloat("Power",power);tag.setLong("NextShot",nextShot);
            tag.setInteger("Age",ticksExisted);tag.setFloat("DamageScale",damageScale);
        }
    }

    public static final class ConfirmedHit {
        @SubscribeEvent public void hit(LivingDamageEvent event) {
            if(event.getEntityLiving().world.isRemote || event.getAmount()<=0 || event.getSource().getImmediateSource() instanceof FlameBolt) return;
            Entity attacker=event.getSource().getTrueSource();
            // Some older projectile jutsus report only their immediate source;
            // credit their real caster so those confirmed hits can trigger an orb.
            if(!(attacker instanceof EntityLivingBase)) {
                Entity projectile=event.getSource().getImmediateSource();
                if(projectile instanceof EntityArrow) attacker=((EntityArrow)projectile).shootingEntity;
                else if(projectile instanceof EntityThrowable) attacker=((EntityThrowable)projectile).getThrower();
                else if(projectile instanceof EntityFireball) attacker=((EntityFireball)projectile).shootingEntity;
                else if(projectile instanceof EntityScalableProjectile.Base)
                    attacker=((EntityScalableProjectile.Base)projectile).shootingEntity;
            }
            if(!(attacker instanceof EntityLivingBase)) return;
            EntityLivingBase owner=(EntityLivingBase)attacker;
            if(!enemy(owner,event.getEntityLiving())) return;
            for(FlameCompany company : owner.world.getEntities(FlameCompany.class,
                e -> e.isEntityAlive() && owner.getUniqueID().equals(e.ownerId))) company.onConfirmedHit(event.getEntityLiving());
        }
    }

    public static final class FlameBolt extends Entity implements ItemJutsu.IJutsu {
        private static final DataParameter<Float> VISUAL_POWER = EntityDataManager.createKey(FlameBolt.class, DataSerializers.FLOAT);
        private UUID ownerId,targetId;
        private float power=1f;
        private float damageScale=1f;
        public FlameBolt(World world) { super(world);setSize(.4f,.4f);noClip=true;isImmuneToFire=true; }
        FlameBolt(World world,EntityLivingBase owner,EntityLivingBase target,Vec3d from,float power) {
            this(world);ownerId=owner.getUniqueID();targetId=target.getUniqueID();this.power=power(power,2f);
            dataManager.set(VISUAL_POWER,this.power);
            setPosition(from.x,from.y,from.z);
        }
        @Override protected void entityInit() { dataManager.register(VISUAL_POWER,1f); }
        @Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.KATON; }
        @Override public boolean isImmuneToExplosions() { return true; }
        public float visualPower() { return dataManager.get(VISUAL_POWER); }
        @Override public void onUpdate() {
            if (isDead) return;
            super.onUpdate();
            if(world.isRemote){world.spawnParticle(EnumParticleTypes.FLAME,posX,posY,posZ,0,.01,0);return;}
            EntityLivingBase owner=resolve(world,ownerId), target=resolve(world,targetId);
            if(isInWater()) { quench(this); return; }
            if(owner==null || target==null || !enemy(owner,target) || ticksExisted>65) {
                setDead();return;
            }
            // Gather the fired orb for half a second: the melee hit's normal
            // invulnerability window expires before the follow-up fire lands.
            if(ticksExisted<=10) return;
            Vec3d from=getPositionVector(), towards=target.getEntityBoundingBox().getCenter().subtract(from);
            Vec3d step=towards.normalize().scale(Math.min(towards.lengthVector(), .7 + .25*(power-1)));
            RayTraceResult hit=firstHit(world,from,from.add(step),owner,.45+.2*(power-1));
            if(hit!=null){
                Vec3d at=hit.hitVec;
                if(hit.typeOfHit==RayTraceResult.Type.BLOCK && hit.sideHit!=null)
                    at=at.add(new Vec3d(hit.sideHit.getDirectionVec()).scale(.15));
                setPosition(at.x,at.y,at.z);
                if(hit.typeOfHit==RayTraceResult.Type.BLOCK && world.getBlockState(hit.getBlockPos()).getMaterial()==Material.WATER)
                    quench(this);
                else impact(owner);
                return;
            }
            motionX=step.x;motionY=step.y;motionZ=step.z;
            move(MoverType.SELF,motionX,motionY,motionZ);
        }
        private void impact(EntityLivingBase owner) {
            if(isDead)return;
            float radius=1.6f+.8f*(power-1f);
            blast(this,owner,radius,(6f+power*7f)*damageScale,3,.45f);
            fireImpact(this,owner,radius,false);
            setDead();
        }
        @Override protected void readEntityFromNBT(NBTTagCompound tag) {
            ownerId=tag.hasUniqueId("Owner")?tag.getUniqueId("Owner"):null;
            targetId=tag.hasUniqueId("Target")?tag.getUniqueId("Target"):null;
            power=power(tag.getFloat("Power"),2f);
            dataManager.set(VISUAL_POWER,power);
            damageScale=power(tag.getFloat("DamageScale"),1.25f);
            ticksExisted=tag.hasKey("Age")?tag.getInteger("Age"):66;
        }
        @Override protected void writeEntityToNBT(NBTTagCompound tag) {
            if(ownerId!=null)tag.setUniqueId("Owner",ownerId);
            if(targetId!=null)tag.setUniqueId("Target",targetId);
            tag.setFloat("Power",power);
            tag.setInteger("Age",ticksExisted);tag.setFloat("DamageScale",damageScale);
        }
    }

    public static final class ThrownClone extends EntityClone.Base implements ItemJutsu.IJutsu {
        private UUID ownerId;
        private float power=1f;
        private float damageScale=1f;
        public ThrownClone(World world) { super(world);setNoAI(true);noClip=true; }
        ThrownClone(World world,EntityLivingBase owner,float power) {
            super(owner);ownerId=owner.getUniqueID();this.power=power(power,2f);setNoAI(true);noClip=true;
            setSize(.65f,1.7f);
        }
        @Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.NINJUTSU; }
        // LivingEntity with noAI does not travel server-side in 1.12.2.
        // Own one swept move below; don't let AI/friction move it a second time.
        @Override public void travel(float strafe,float vertical,float forward) { }
        @Override public void onUpdate() {
            super.onUpdate();
            if(world.isRemote || isDead)return;
            EntityLivingBase owner=resolve(world,ownerId);
            if(owner==null || !owner.isEntityAlive() || ticksExisted>30 || isInWater()) { poof();return; }
            advanceThrow(owner);
        }
        private void advanceThrow(EntityLivingBase owner) {
            Vec3d from=getPositionVector().addVector(0,.8,0), to=from.addVector(motionX,motionY,motionZ);
            RayTraceResult hit=firstHit(world,from,to,owner,.5);
            double hitDistance=hit!=null?from.squareDistanceTo(hit.hitVec):Double.MAX_VALUE;
            Entity intercepted=null;
            Vec3d interception=null;
            for(Entity projectile:world.getEntitiesWithinAABB(Entity.class,new AxisAlignedBB(from,to).grow(.6))) {
                if(!interceptable(projectile,owner))continue;
                AxisAlignedBB box=projectile.getEntityBoundingBox().grow(.6);
                RayTraceResult contact=box.contains(from)?new RayTraceResult(from,null):box.calculateIntercept(from,to);
                if(contact!=null&&from.squareDistanceTo(contact.hitVec)<hitDistance){
                    hitDistance=from.squareDistanceTo(contact.hitVec);intercepted=projectile;interception=contact.hitVec;
                }
            }
            if(intercepted!=null){
                setPosition(interception.x,interception.y-.8,interception.z);
                intercepted.setDead();
                play(world,getPositionVector(),SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,1f,.78f);
                poof();return;
            }
            if(hit!=null){
                setPosition(hit.hitVec.x,hit.hitVec.y-.8,hit.hitVec.z);
                if(hit.entityHit instanceof EntityLivingBase){
                    EntityLivingBase target=(EntityLivingBase)hit.entityHit;
                    if(target.attackEntityFrom(ItemJutsu.causeJutsuDamage(this,owner),(6f+power*7f)*damageScale) && ItemJutsu.canTarget(target)){
                        target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,20+Math.round(power*5),3,false,true));
                        target.motionX*=.35;target.motionZ*=.35;target.velocityChanged=true;
                        play(world,getPositionVector(),SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,1.4f,.72f);
                        if(world instanceof WorldServer)((WorldServer)world).spawnParticle(EnumParticleTypes.CRIT,target.posX,target.posY+target.height*.5,target.posZ,20,.35,.45,.35,.12);
                    }
                }
                poof();return;
            }
            move(MoverType.SELF,motionX,motionY,motionZ);
            motionY-=.035;motionX*=.98;motionZ*=.98;
            rotationYaw+=24f;
        }
        private boolean interceptable(Entity projectile,EntityLivingBase owner) {
            if(projectile==this || projectile.isDead || !(projectile instanceof IProjectile)
                || projectile.width>1.15f || projectile.height>1.15f) return false;
            if(projectile instanceof EntityArrow) return ((EntityArrow)projectile).shootingEntity!=owner;
            if(projectile instanceof EntityThrowable) return ((EntityThrowable)projectile).getThrower()!=owner;
            if(projectile instanceof EntityFireball) return ((EntityFireball)projectile).shootingEntity!=owner;
            if(projectile instanceof EntityScalableProjectile.Base)
                return ((EntityScalableProjectile.Base)projectile).shootingEntity!=owner;
            return true;
        }
        private void poof() {
            if(world instanceof WorldServer)((WorldServer)world).spawnParticle(EnumParticleTypes.SMOKE_LARGE,posX,posY+1,posZ,24,.4,.6,.4,.05);
            play(world,getPositionVector(),sound("poof"),.85f,1f);
            setDead();
        }
        @Override public void readEntityFromNBT(NBTTagCompound tag) {
            super.readEntityFromNBT(tag);
            ownerId=tag.hasUniqueId("ThrowOwner")?tag.getUniqueId("ThrowOwner"):null;
            power=power(tag.getFloat("ThrowPower"),2f);
            damageScale=power(tag.getFloat("ThrowDamageScale"),1.25f);
            ticksExisted=tag.hasKey("ThrowAge")?tag.getInteger("ThrowAge"):31;
            // Summoner entity IDs are session-local. Never reload a copied
            // player inventory as an orphan living clone after a restart.
            setDead();
        }
        @Override public void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            if(ownerId!=null)tag.setUniqueId("ThrowOwner",ownerId);
            tag.setFloat("ThrowPower",power);
            tag.setFloat("ThrowDamageScale",damageScale);tag.setInteger("ThrowAge",ticksExisted);
        }
    }
}
