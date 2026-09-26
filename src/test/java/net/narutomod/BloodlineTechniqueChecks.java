package net.narutomod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.narutomod.entity.EntitySummonAnimal;
import net.narutomod.entity.EntityWaterDragon;
import net.narutomod.item.BloodlineTechniques;
import net.narutomod.item.ItemJutsu;
import net.narutomod.item.ItemKaton;
import net.narutomod.item.ItemNinjutsu;
import sun.misc.Unsafe;

/** Production charge, collision and damage dispatch; fixtures bypass AI/server/GL startup only. */
public final class BloodlineTechniqueChecks {
    private static int checks;

    private static void check(boolean ok, String description) {
        checks++;
        if (!ok) throw new AssertionError(description);
    }

    private static <T> T fixture(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
    }

    private static class Actor extends EntityZombie {
        NBTTagCompound data;
        Entity ally;
        int hits, ignited, resistanceAtHit;
        float received;
        DamageSource damageSource;
        boolean acceptsDamage = true;
        Actor() { super((World)null); }
        @Override public boolean isEntityAlive() { return !isDead; }
        @Override public boolean canBeCollidedWith() { return true; }
        @Override public NBTTagCompound getEntityData() { return data; }
        @Override public boolean isOnSameTeam(Entity other) { return other == ally; }
        @Override public boolean attackEntityFrom(DamageSource source, float amount) {
            hits++;
            received += amount;
            damageSource = source;
            resistanceAtHit = hurtResistantTime;
            return acceptsDamage;
        }
        @Override public void setFire(int seconds) { ignited += seconds; }
        @Override public float getEyeHeight() { return height * .9f; }
        @Override public void knockBack(Entity source, float strength, double x, double z) {
            motionX -= x * strength; motionY = strength; motionZ -= z * strength;
        }
    }

    private static final class Summon extends Actor implements EntitySummonAnimal.ISummon {
        EntityLivingBase owner;
        @Override public EntityLivingBase getSummoner() { return owner; }
    }

    private static final class CollisionWorld extends World {
        List<EntityLivingBase> candidates;
        RayTraceResult block;
        boolean tracedLiquids;
        CollisionWorld() { super(null, null, null, null, false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean empty) { return true; }
        @Override public RayTraceResult rayTraceBlocks(Vec3d a, Vec3d b, boolean liquid, boolean ignore, boolean miss) {
            tracedLiquids |= liquid;
            return block;
        }
        @Override public boolean isMaterialInBB(AxisAlignedBB bounds, Material material) { return false; }
        @Override public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> type, AxisAlignedBB bounds) {
            List<T> matches = new ArrayList<>();
            for (EntityLivingBase candidate : candidates)
                if (type.isInstance(candidate) && bounds.intersects(candidate.getEntityBoundingBox()))
                    matches.add(type.cast(candidate));
            return matches;
        }
    }

    /** Supply deterministic training/resource limits; retain the actual shared ItemJutsu charge code. */
    private static final class ChargeItem extends ItemJutsu.Base {
        float modifier = 1f, resourceCap = Float.MAX_VALUE;
        final ItemJutsu.JutsuEnum technique;
        ChargeItem(ItemJutsu.JutsuEnum technique, ItemJutsu.JutsuEnum.Type type) {
            super(type, technique);
            this.technique = technique;
        }
        @Override public float getModifier(ItemStack stack, EntityLivingBase actor) { return modifier; }
        @Override public float getMaxPower(ItemStack stack, EntityLivingBase actor) {
            return Math.min(resourceCap, technique.jutsu.getMaxPower(stack, actor));
        }
    }

    private static Actor actor(CollisionWorld world, double x, double y, double z, float width, float height) throws Exception {
        Actor actor = fixture(Actor.class);
        initialize(actor, world, x, y, z, width, height);
        return actor;
    }

    private static void initialize(Actor actor, CollisionWorld world, double x, double y, double z, float width, float height) {
        actor.world = world;
        actor.data = new NBTTagCompound();
        actor.acceptsDamage = true;
        actor.width = width;
        actor.height = height;
        actor.posX = x; actor.posY = y; actor.posZ = z;
        actor.setEntityBoundingBox(new AxisAlignedBB(x-width*.5, y, z-width*.5, x+width*.5, y+height, z+width*.5));
    }

    private static Method method(String name, Class<?>... types) throws Exception {
        Method method = BloodlineTechniques.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }

    private static int blast(Entity effect, EntityLivingBase owner, float radius, float damage) throws Exception {
        return (Integer)method("blast", Entity.class, EntityLivingBase.class, float.class, float.class, int.class, float.class)
            .invoke(null, effect, owner, radius, damage, 3, .75f);
    }

    private static RayTraceResult firstHit(CollisionWorld world, Vec3d from, Vec3d to, EntityLivingBase owner) throws Exception {
        return (RayTraceResult)method("firstHit", World.class, Vec3d.class, Vec3d.class, EntityLivingBase.class, double.class)
            .invoke(null, world, from, to, owner, .15d);
    }

    private static RayTraceResult wall(double x, double y, double z) {
        return new RayTraceResult(new Vec3d(x,y,z), EnumFacing.WEST, new BlockPos(x,y,z));
    }

    private static void sharedCharging(Actor owner) throws Exception {
        ItemJutsu.JutsuEnum[] techniques = {ItemKaton.TWINFLAMEDRAGONS, ItemKaton.FLAMECOMPANY, ItemNinjutsu.CLONETHROW};
        for (ItemJutsu.JutsuEnum technique : techniques) {
            ChargeItem item = new ChargeItem(technique,
                technique == ItemNinjutsu.CLONETHROW ? ItemJutsu.JutsuEnum.Type.NINJUTSU : ItemJutsu.JutsuEnum.Type.KATON);
            ItemStack stack = new ItemStack(item);
            int duration = item.getMaxItemUseDuration(stack);
            float tap = item.getPower(stack, owner, duration);
            float held = item.getPower(stack, owner, duration - 10);
            check(held > tap, technique.unlocalizedName + " gains power through shared hold-to-charge path");
            check(item.getPower(stack, owner, 0) == technique.jutsu.getMaxPower(stack, owner),
                technique.unlocalizedName + " shared path caps power");
            item.modifier = .5f;
            check(item.getPower(stack, owner, duration - 10) > held,
                technique.unlocalizedName + " inherited training modifier speeds charging");
            item.resourceCap = tap + .1f;
            check(item.getPower(stack, owner, 0) <= item.resourceCap,
                technique.unlocalizedName + " inherited resource cap limits charge");
            check(technique.jutsu.getClass().getMethod("onUsingTick", ItemStack.class, EntityLivingBase.class, float.class)
                .getDeclaringClass() == ItemJutsu.IJutsuCallback.class,
                technique.unlocalizedName + " uses common charge presentation callback");
            check(!JutsuVisualEffects.charging(stack, owner, held),
                technique.unlocalizedName + " does not suppress original chakra charge visuals");
        }
    }

    private static void impactAndCover(CollisionWorld world, Actor owner) throws Exception {
        BloodlineTechniques.TwinDragon effect = new BloodlineTechniques.TwinDragon(null);
        effect.world = world;
        Actor target = actor(world, 0, 0, 0, .6f, 1.8f);
        world.candidates = Collections.singletonList(target);
        effect.setPosition(0, 1.62, 0);
        target.hurtResistantTime = 20;
        check(blast(effect, owner, 1.6f, 18f) == 1 && target.hits == 1 && target.received > 0,
            "fire arriving at player eye height actually dispatches damage");
        check(target.damageSource.getTrueSource() == owner && target.damageSource.getImmediateSource() == effect,
            "impact retains caster and projectile attribution");
        check(ItemJutsu.isDamageSourceJutsu(target.damageSource) && target.damageSource.isFireDamage()
            && target.damageSource.isExplosion(), "impact uses jutsu fire/explosion damage semantics");
        check(target.ignited > 0 && target.velocityChanged && target.motionY > 0,
            "accepted fire impact ignites and launches target");
        check(target.resistanceAtHit == 20, "blast preserves existing invulnerability instead of resetting it");

        target = actor(world, 0, 0, 0, 4f, 12f);
        world.candidates = Collections.singletonList(target);
        effect.setPosition(0, 10.8, 0);
        check(blast(effect, owner, 1.6f, 18f) == 1 && target.hits == 1,
            "fire arriving at tall summon's upper body damages its hitbox rather than checking feet distance");

        target = actor(world, 3, 0, 0, .6f, 1.8f);
        world.candidates = Collections.singletonList(target);
        effect.setPosition(0, 1, 0);
        world.block = wall(1.5, 1, 0);
        check(blast(effect, owner, 4f, 18f) == 0 && target.hits == 0, "solid cover prevents splash damage");
        world.block = null;
        check(blast(effect, owner, 4f, 18f) == 1 && target.hits == 1, "same target takes damage after cover is removed");

        target = actor(world, 7, 0, 0, .6f, 1.8f);
        world.candidates = Collections.singletonList(target);
        check(blast(effect, owner, 4f, 18f) == 0 && target.hits == 0, "outside blast radius is safe");
        target = actor(world, 0, 0, 0, .6f, 1.8f);
        target.acceptsDamage = false;
        world.candidates = Collections.singletonList(target);
        check(blast(effect, owner, 4f, 18f) == 0 && target.ignited == 0 && !target.velocityChanged,
            "rejected damage does not apply fire or knockback");
    }

    private static void friendliesAndSweeps(CollisionWorld world, Actor owner) throws Exception {
        Actor enemy = actor(world, 6, 0, 0, .6f, 1.8f);
        Actor ally = actor(world, 2, 0, 0, .6f, 1.8f);
        owner.ally = ally;
        Summon summon = fixture(Summon.class);
        initialize(summon, world, 3, 0, 0, .6f, 1.8f);
        summon.owner = owner;
        Actor intangible = actor(world, 4, 0, 0, .6f, 1.8f);
        intangible.data.setInteger("UntargetableTicks", 20);
        world.candidates = Arrays.asList(owner, ally, summon, intangible, enemy);
        Vec3d from = new Vec3d(0, .9, 0), to = new Vec3d(10, .9, 0);
        RayTraceResult hit = firstHit(world, from, to, owner);
        check(hit != null && hit.entityHit == enemy,
            "swept projectile passes caster, ally, owned summon and intangible body to hit enemy");
        world.block = wall(5, .9, 0);
        check(firstHit(world, from, to, owner) == world.block, "wall before target wins swept collision");
        world.block = wall(8, .9, 0);
        check(firstHit(world, from, to, owner).entityHit == enemy, "target before wall wins swept collision");
        world.tracedLiquids = false;
        world.block = wall(5, .9, 0);
        firstHit(world, from, to, owner);
        check(world.tracedLiquids, "fire collision ray includes water, not only solid blocks");
        world.block = null;
        BloodlineTechniques.TwinDragon effect = new BloodlineTechniques.TwinDragon(null);
        effect.world = world;
        effect.setPosition(4, .9, 0);
        check(blast(effect, owner, 8f, 18f) == 1 && enemy.hits == 1
            && owner.hits == 0 && ally.hits == 0 && summon.hits == 0 && intangible.hits == 0,
            "same owner/team/summon/immunity exclusions protect against explosion splash");
        owner.ally = null;
        ally.ally = owner;
        world.candidates = Collections.singletonList(ally);
        check(firstHit(world, from, to, owner) == null, "target-declared teammate is also protected");
        world.candidates = Collections.emptyList();
        check(firstHit(world, from, to, owner) == null, "empty trajectory misses cleanly");
    }

    private static void cloneMovement(CollisionWorld world, Actor owner) throws Exception {
        // The actual noClip move path requires no AI, chunks or renderer.
        BloodlineTechniques.ThrownClone clone = fixture(BloodlineTechniques.ThrownClone.class);
        clone.world = world; clone.noClip = true; clone.width = .65f; clone.height = 1.7f;
        clone.setPosition(0, 2, 0);
        clone.motionX = 1.4; clone.motionY = .1;
        world.candidates = Collections.emptyList(); world.block = null;
        Method advance = BloodlineTechniques.ThrownClone.class.getDeclaredMethod("advanceThrow", EntityLivingBase.class);
        advance.setAccessible(true);
        advance.invoke(clone, owner);
        check(Math.abs(clone.posX - 1.4) < .00001 && Math.abs(clone.posY - 2.1) < .00001,
            "AI-disabled clone actually moves once along its swept trajectory");
        clone.travel(1, 1, 1);
        check(Math.abs(clone.posX - 1.4) < .00001, "vanilla travel cannot double-move thrown clone");
    }

    private static void savedCastSafety() throws Exception {
        Method read = BloodlineTechniques.TwinDragon.class.getDeclaredMethod("readEntityFromNBT", NBTTagCompound.class);
        read.setAccessible(true);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Delay", 24); tag.setFloat("Power", 2.8f); tag.setInteger("Age", 25);
        BloodlineTechniques.TwinDragon dragon = new BloodlineTechniques.TwinDragon(null);
        read.invoke(dragon, tag);
        check(!dragon.isDead && dragon.ticksExisted == 25 && dragon.visualPower() == 2.8f,
            "loading active dragon retains progress and synchronized charged scale");
        check(dragon.blastRadius() > new BloodlineTechniques.TwinDragon(null).blastRadius(),
            "charged dragon expands actual damage area as well as visual size");
        tag.setInteger("Age", 54);
        read.invoke(dragon, tag);
        check(dragon.isDead, "completed saved dragon cannot replay an explosion");
        tag.removeTag("Age"); tag.setFloat("Power", Float.NaN);
        dragon = new BloodlineTechniques.TwinDragon(null);
        read.invoke(dragon, tag);
        check(dragon.isDead && Float.isFinite(dragon.visualPower()),
            "legacy saved attack and invalid power expire safely instead of crashing world");
    }

    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        BloodlineTechniques.TwinDragon dragon = new BloodlineTechniques.TwinDragon(null);
        EntityWaterDragon.Renderer.ModelDragonHead model = new EntityWaterDragon.Renderer().new ModelDragonHead();
        model.setRotationAngles(0, 0, 3, 0, 0, .0625f, dragon);
        check(true, "fire dragon safely reuses model without water-entity cast");
        CollisionWorld world = fixture(CollisionWorld.class);
        world.candidates = Collections.emptyList();
        Actor owner = actor(world, -1, 0, 0, .6f, 1.8f);
        sharedCharging(owner);
        impactAndCover(world, owner);
        friendliesAndSweeps(world, owner);
        cloneMovement(world, owner);
        savedCastSafety();
        System.out.println("Bloodline runtime regression: " + checks + " checks passed.");
    }
}
