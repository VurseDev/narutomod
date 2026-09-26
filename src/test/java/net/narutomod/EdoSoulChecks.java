package net.narutomod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.narutomod.item.ItemDnaSample;
import sun.misc.Unsafe;

/** Exercises real source capture, authoritative specimens, interruption refunds and persisted summon leases. */
public final class EdoSoulChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe)field.get(null);
    }
    private static final class Donor extends EntityPlayer {
        GameProfile profile;
        DataParameter<Float> healthKey;
        Donor() { super(null, null); }
        @Override public UUID getUniqueID() { return profile.getId(); }
        @Override public String getName() { return profile.getName(); }
        @Override public GameProfile getGameProfile() { return profile; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        void fixtureHealth(float value) { getDataManager().set(healthKey,value); }
        @Override public Team getTeam() { return null; }
    }
    private static final class DeathWorld extends WorldServer {
        GameRules rules;
        long time;
        DeathWorld() { super(null, null, null, 0, null); }
        @Override public long getTotalWorldTime() { return time; }
        @Override public GameRules getGameRules() { return rules; }
    }
    private static EdoSoulRegistry roundTrip(EdoSoulRegistry source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(source.writeToNBT(new NBTTagCompound()), bytes);
        EdoSoulRegistry result = new EdoSoulRegistry();
        result.readFromNBT(CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray())));
        return result;
    }

    public static void main(String[] arguments) throws Exception {
        Bootstrap.register();
        Donor donor = (Donor)unsafe().allocateInstance(Donor.class);
        DeathWorld world = (DeathWorld)unsafe().allocateInstance(DeathWorld.class);
        world.time = 99;
        world.rules = new GameRules();
        world.rules.addGameRule("keepNinjaXp", "false", GameRules.ValueType.BOOLEAN_VALUE);
        donor.world = world;
        Field managerField=Entity.class.getDeclaredField("dataManager");managerField.setAccessible(true);
        EntityDataManager manager=new EntityDataManager(donor);managerField.set(donor,manager);
        Field healthField=EntityLivingBase.class.getDeclaredField("HEALTH");healthField.setAccessible(true);
        donor.healthKey=(DataParameter<Float>)healthField.get(null);manager.register(donor.healthKey,20f);
        donor.profile = new GameProfile(UUID.randomUUID(), "CapturedPlayer");
        donor.profile.getProperties().put("textures", new Property("textures", "signed-texture", "source-signature"));
        donor.profile.getProperties().put("unrelated", new Property("unrelated", "private-value"));
        donor.getEntityData().setDouble(NarutomodModVariables.BATTLEXP, 55000);
        donor.getEntityData().setString("PrivateDonorData", "must-not-be-copied");

        UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), ritual = UUID.randomUUID();
        EdoSoulRegistry registry = new EdoSoulRegistry();
        UUID sample = registry.issue(donor);
        check(sample != null, "server capture issues a specimen identity");
        NBTTagCompound captured = registry.getSample(sample);
        check(captured.getUniqueId("Player").equals(donor.getUniqueID()), "actual donor UUID captured");
        check(captured.getString("Name").equals("CapturedPlayer") && captured.getDouble("NinjaXp") == 55000,
            "actual name and Ninja XP captured");
        check(captured.getKeySet().equals(new HashSet<>(Arrays.asList("PlayerMost", "PlayerLeast", "Name", "Profile", "NinjaXp"))),
            "snapshot contains only the permitted identity and XP fields");
        GameProfile profile = NBTUtil.readGameProfileFromNBT(captured.getCompoundTag("Profile"));
        check(profile != null && profile.getId().equals(donor.getUniqueID()) && profile.getProperties().get("textures").size() == 1,
            "source player profile retained for offline rendering");
        check(profile.getProperties().get("textures").iterator().next().getSignature().equals("source-signature")
            && !profile.getProperties().containsKey("unrelated"), "only the bounded signed skin property survives");
        PlayerTracker.Deaths.clear();
        donor.fixtureHealth(0);
        world.time = 0;
        check(registry.getSample(registry.issue(donor)).getDouble("NinjaXp") == 55000, "world tick zero does not invent a matching death record");
        world.time = 99;
        check(registry.getSample(registry.issue(donor)).getDouble("NinjaXp") == 55000, "death capture before the legacy listener uses current Ninja XP");
        PlayerTracker.Deaths.log(donor);
        check(PlayerTracker.getBattleXp(donor) == 0, "fixture exercises the real keepNinjaXp=false death reset");
        check(registry.getSample(registry.issue(donor)).getDouble("NinjaXp") == 55000, "death capture after the legacy reset preserves the same pre-death Ninja XP");
        donor.fixtureHealth(20); donor.getEntityData().setDouble(NarutomodModVariables.BATTLEXP, 15);
        check(registry.getSample(registry.issue(donor)).getDouble("NinjaXp") == 15, "a living player's capture does not reuse historical death XP");
        donor.fixtureHealth(0); world.time++;
        check(registry.getSample(registry.issue(donor)).getDouble("NinjaXp") == 15, "an older death record cannot override a later death's XP");
        donor.fixtureHealth(20); donor.getEntityData().setDouble(NarutomodModVariables.BATTLEXP, 55000);
        PlayerTracker.Deaths.clear();
        captured.setString("Name", "Tampered");
        check(registry.getSample(sample).getString("Name").equals("CapturedPlayer"), "sample reads cannot mutate the archive");
        check(registry.getSample(UUID.randomUUID()).hasNoTags() && registry.getSample(null).hasNoTags(), "unknown specimens rejected");
        check(!registry.claimSample(UUID.randomUUID(), owner, ritual), "invented physical specimen IDs cannot create an authoritative claim");

        check(registry.claimSample(sample, owner, ritual) && registry.claimSample(sample, owner, ritual), "identical claim retry is idempotent");
        check(!registry.claimSample(sample, other, ritual) && !registry.claimSample(sample, owner, UUID.randomUUID()), "copied samples cannot fund another ritual");
        check(!registry.getSample(sample).hasKey("ClaimOwnerMost"), "claim ownership stays server internal");
        check(!registry.releaseSample(sample, other, ritual) && !registry.releaseSample(sample, owner, UUID.randomUUID()), "unrelated interruption cannot refund a specimen");
        check(registry.releaseSample(sample, owner, ritual), "matching unfinished ritual can refund");
        UUID transferredRitual = UUID.randomUUID();
        check(registry.claimSample(sample, other, transferredRitual) && registry.releaseSample(sample, other, transferredRitual),
            "a refunded physical sample can be traded and claimed by a new caster");
        UUID replacement = UUID.randomUUID();
        check(registry.claimSample(sample, owner, replacement) && !registry.releaseSample(sample, owner, ritual), "stale interruption cannot release a newer claim");
        registry = roundTrip(registry);
        check(registry.claimSample(sample, owner, replacement) && !registry.claimSample(sample, other, replacement), "exclusive claim survives compressed save/load");
        captured = registry.getSample(sample);
        registry.complete(owner, replacement, captured);
        registry.complete(owner, replacement, captured);
        check(registry.souls(owner).tagCount() == 1 && registry.souls(other).tagCount() == 0, "real completion is idempotent and isolated by caster");
        check(!registry.releaseSample(sample, owner, replacement), "completed ritual cannot refund its consumed DNA");
        NBTTagCompound soul = registry.souls(owner).getCompoundTagAt(0);
        check(soul.getUniqueId("Player").equals(donor.getUniqueID()) && !soul.hasKey("Mob"), "real soul replaces the random mob payload");
        captured.setDouble("NinjaXp", 1);
        check(soul.getDouble("NinjaXp") == 55000, "completion stores a defensive source snapshot");

        UUID entity = UUID.randomUUID(), duplicate = UUID.randomUUID();
        check(registry.activate(owner, replacement, entity) && registry.activate(owner, replacement, entity), "one active instance may retry its own lease");
        check(!registry.activate(owner, replacement, duplicate) && !registry.activate(other, replacement, duplicate), "another instance or caster cannot duplicate a live soul");
        registry = roundTrip(registry);
        check(entity.equals(registry.active(owner, replacement)), "active summon ownership survives reload and chunk unload");
        check(!registry.deactivate(owner, replacement, duplicate) && registry.deactivate(owner, replacement, entity), "only matching active instance can release");
        check(registry.activate(owner, replacement, duplicate) && !registry.deactivate(owner, replacement, entity), "stale entity cannot dismiss a replacement summon");

        NBTTagCompound malformed = registry.getSample(sample);
        malformed.setDouble("NinjaXp", Double.NaN);
        char[] longName = new char[200]; Arrays.fill(longName, 'A'); malformed.setString("Name", new String(longName));
        malformed.setTag("Inventory", new NBTTagCompound());
        UUID bounded = UUID.randomUUID(); registry.complete(owner, bounded, malformed);
        NBTTagCompound sanitized = registry.souls(owner).getCompoundTagAt(1);
        check(sanitized.getString("Name").length() == 64 && sanitized.getDouble("NinjaXp") == 0 && !sanitized.hasKey("Inventory"),
            "oversized names, non-finite XP and unrelated NBT never enter real soul records");
        GameProfile untrustedSkin = new GameProfile(donor.getUniqueID(), "CapturedPlayer");
        untrustedSkin.getProperties().put("textures", new Property("textures", "unsigned-texture"));
        char[] hugeTexture = new char[8193]; Arrays.fill(hugeTexture, 'A');
        untrustedSkin.getProperties().put("textures", new Property("textures", new String(hugeTexture), "signature"));
        malformed.setTag("Profile", NBTUtil.writeGameProfile(new NBTTagCompound(), untrustedSkin));
        registry.complete(owner, UUID.randomUUID(), malformed);
        NBTTagCompound filteredSkin = registry.souls(owner).getCompoundTagAt(registry.souls(owner).tagCount() - 1).getCompoundTag("Profile");
        check(!filteredSkin.getCompoundTag("Properties").hasKey("textures"), "unsigned and oversized profile textures cannot enter the archive");
        for (double xp : new double[]{-10, 100001, Double.POSITIVE_INFINITY}) {
            malformed.setDouble("NinjaXp", xp); UUID next = UUID.randomUUID(); registry.complete(owner, next, malformed);
            NBTTagCompound latest = registry.souls(owner).getCompoundTagAt(registry.souls(owner).tagCount() - 1);
            check(latest.getDouble("NinjaXp") == (xp == 100001 ? 100000 : 0), "XP snapshot is finite and bounded");
        }
        UUID legacy = UUID.randomUUID(); registry.complete(owner, legacy, 1);
        check(!registry.activate(owner, legacy, UUID.randomUUID()), "old random test souls cannot become real player summons");
        check(roundTrip(registry).souls(owner).tagCount() == registry.souls(owner).tagCount(), "legacy and real rosters coexist after migration");
        EdoSoulRegistry original = new EdoSoulRegistry(); original.complete(other, legacy, 2);
        NBTTagCompound versionOne = original.writeToNBT(new NBTTagCompound());
        versionOne.removeTag("Samples"); versionOne.setInteger("Version", 1);
        EdoSoulRegistry migrated = new EdoSoulRegistry(); migrated.readFromNBT(versionOne);
        check(migrated.souls(other).tagCount() == 1 && migrated.getSample(sample).hasNoTags(), "version-one archives migrate without inventing donor identities");

        Item dna = new Item().setRegistryName("narutomod:dna_sample").setMaxStackSize(1);
        ForgeRegistry<Item> items = (ForgeRegistry<Item>)ForgeRegistries.ITEMS;
        boolean frozen = items.isLocked(); if (frozen) items.unfreeze();
        try { items.register(dna); } finally { if (frozen) items.freeze(); }
        Class.forName(ItemDnaSample.class.getName(), true, ItemDnaSample.class.getClassLoader());
        Field holder = ItemDnaSample.class.getDeclaredField("block"); Unsafe u = unsafe();
        u.putObject(u.staticFieldBase(holder), u.staticFieldOffset(holder), dna);
        ItemStack stack = ItemDnaSample.create(sample, registry.getSample(sample));
        check(sample.equals(ItemDnaSample.sampleId(stack)), "physical specimen round-trips its authoritative sample reference");
        check(!stack.getTagCompound().hasKey("Profile") && !stack.getTagCompound().hasKey("NinjaXp"), "tradeable item never carries trusted stats or skin payloads");
        ItemStack fake = new ItemStack(Items.PAPER); fake.setTagCompound(stack.getTagCompound().copy());
        check(ItemDnaSample.sampleId(fake) == null && ItemDnaSample.sampleId(ItemStack.EMPTY) == null, "NBT on arbitrary items is not DNA");
        stack.setCount(2); check(ItemDnaSample.sampleId(stack) == null, "stacked specimen references rejected");
        System.out.println("Edo real souls: " + checks + " checks passed.");
    }
}
