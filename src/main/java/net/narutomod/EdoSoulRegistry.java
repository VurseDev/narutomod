package net.narutomod;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/** Server archive. Item stacks only carry a small display window, never the whole roster. */
public class EdoSoulRegistry extends WorldSavedData {
    private static final String KEY = "narutomod_edo_souls";
    private NBTTagCompound owners = new NBTTagCompound();
    private NBTTagCompound samples = new NBTTagCompound();

    public EdoSoulRegistry() { super(KEY); }
    public EdoSoulRegistry(String name) { super(name); }

    public static EdoSoulRegistry get(World world) {
        if (world.isRemote) throw new IllegalStateException("Soul archive is server-only");
        MapStorage storage = world.getMinecraftServer().getWorld(0).getMapStorage();
        EdoSoulRegistry data = (EdoSoulRegistry)storage.getOrLoadData(EdoSoulRegistry.class, KEY);
        if (data == null) {
            data = new EdoSoulRegistry();
            storage.setData(KEY, data);
            data.markDirty();
        }
        return data;
    }

    public NBTTagList souls(UUID owner) {
        String key = owner.toString();
        if (!owners.hasKey(key, 9)) owners.setTag(key, new NBTTagList());
        return owners.getTagList(key, 10);
    }

    /** Issue a server record at death. The physical item is only a reference to this snapshot. */
    public UUID issue(EntityPlayer donor) {
        if (donor == null || donor.world == null || donor.world.isRemote) return null;
        NBTTagCompound captured = new NBTTagCompound();
        captured.setUniqueId("Player", donor.getUniqueID());
        captured.setString("Name", donor.getName());
        captured.setTag("Profile", NBTUtil.writeGameProfile(new NBTTagCompound(), donor.getGameProfile()));
        double xp = PlayerTracker.getBattleXp(donor);
        // The legacy LOWEST-priority death listener may already have cleared Ninja XP.
        if (donor.getHealth() <= 0 && PlayerTracker.Deaths.hasRecentMatching(donor, 0)
            && PlayerTracker.Deaths.mostRecentTime(donor) == donor.world.getTotalWorldTime())
            xp = PlayerTracker.Deaths.getXpBeforeDeath(donor);
        captured.setDouble("NinjaXp", xp);
        UUID id = UUID.randomUUID();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setTag("Captured", snapshot(captured));
        samples.setTag(id.toString(), entry);
        markDirty();
        return id;
    }

    public NBTTagCompound getSample(UUID sample) {
        if (sample == null || !samples.hasKey(sample.toString(), 10)) return new NBTTagCompound();
        return snapshot(samples.getCompoundTag(sample.toString()).getCompoundTag("Captured"));
    }

    public boolean claimSample(UUID sample, UUID owner, UUID ritual) {
        if (sample == null || owner == null || ritual == null || getSample(sample).hasNoTags()) return false;
        NBTTagCompound entry = samples.getCompoundTag(sample.toString());
        if (entry.hasUniqueId("ClaimOwner") || entry.hasUniqueId("ClaimRitual"))
            return owner.equals(uuid(entry, "ClaimOwner")) && ritual.equals(uuid(entry, "ClaimRitual"));
        entry.setUniqueId("ClaimOwner", owner);
        entry.setUniqueId("ClaimRitual", ritual);
        markDirty();
        return true;
    }

    /** Only an unfinished claim may be returned; completing a ritual permanently spends its sample. */
    public boolean releaseSample(UUID sample, UUID owner, UUID ritual) {
        if (sample == null || owner == null || ritual == null || !samples.hasKey(sample.toString(), 10)) return false;
        NBTTagCompound entry = samples.getCompoundTag(sample.toString());
        if (!owner.equals(uuid(entry, "ClaimOwner")) || !ritual.equals(uuid(entry, "ClaimRitual")) || find(owner, ritual) != null) return false;
        removeUuid(entry, "ClaimOwner");
        removeUuid(entry, "ClaimRitual");
        markDirty();
        return true;
    }

    /** Repeated completion is harmless, and the archive never copies donor inventory or arbitrary player NBT. */
    public void complete(UUID owner, UUID ritual, NBTTagCompound captured) {
        if (owner == null || ritual == null || find(owner, ritual) != null) return;
        NBTTagCompound soul = snapshot(captured);
        if (soul.hasNoTags()) return;
        soul.setUniqueId("Soul", ritual);
        souls(owner).appendTag(soul);
        markDirty();
    }

    public boolean activate(UUID owner, UUID soul, UUID entity) {
        NBTTagCompound entry = find(owner, soul);
        if (entity == null || entry == null || !entry.hasUniqueId("Player")) return false;
        UUID current = uuid(entry, "ActiveEntity");
        if (current != null) return current.equals(entity);
        entry.setUniqueId("ActiveEntity", entity);
        markDirty();
        return true;
    }

    public UUID active(UUID owner, UUID soul) {
        NBTTagCompound entry = find(owner, soul);
        return entry == null ? null : uuid(entry, "ActiveEntity");
    }

    public boolean deactivate(UUID owner, UUID soul, UUID entity) {
        NBTTagCompound entry = find(owner, soul);
        if (entity == null || entry == null || !entity.equals(uuid(entry, "ActiveEntity"))) return false;
        removeUuid(entry, "ActiveEntity");
        markDirty();
        return true;
    }

    private NBTTagCompound find(UUID owner, UUID soul) {
        if (owner == null || soul == null) return null;
        NBTTagList list = owners.getTagList(owner.toString(), 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (soul.equals(uuid(entry, "Soul"))) return entry;
        }
        return null;
    }

    private static UUID uuid(NBTTagCompound tag, String key) { return tag.hasUniqueId(key) ? tag.getUniqueId(key) : null; }
    private static void removeUuid(NBTTagCompound tag, String key) {
        tag.removeTag(key + "Most");
        tag.removeTag(key + "Least");
    }

    private static NBTTagCompound snapshot(NBTTagCompound source) {
        if (source == null || !source.hasUniqueId("Player")) return new NBTTagCompound();
        UUID player = source.getUniqueId("Player");
        String name = source.getString("Name").replaceAll("[\\p{Cntrl}\\u00a7]", "").trim();
        if (name.length() > 64) name = name.substring(0, 64);
        if (name.isEmpty()) name = player.toString();
        GameProfile safeProfile = new GameProfile(player, name);
        NBTTagList textures = source.getCompoundTag("Profile").getCompoundTag("Properties").getTagList("textures", 10);
        // One signed texture property is enough; never persist arbitrary profile properties or unbounded payloads.
        for (int i = 0; i < Math.min(textures.tagCount(), 8); i++) {
            NBTTagCompound texture = textures.getCompoundTagAt(i);
            String value = texture.getString("Value"), signature = texture.getString("Signature");
            if (!value.isEmpty() && value.length() <= 8192 && !signature.isEmpty() && signature.length() <= 2048) {
                safeProfile.getProperties().put("textures", new Property("textures", value, signature));
                break;
            }
        }
        NBTTagCompound result = new NBTTagCompound();
        result.setUniqueId("Player", player);
        result.setString("Name", name);
        result.setTag("Profile", NBTUtil.writeGameProfile(new NBTTagCompound(), safeProfile));
        double xp = source.getDouble("NinjaXp");
        result.setDouble("NinjaXp", Double.isFinite(xp) ? Math.max(0, Math.min(100000, xp)) : 0);
        return result;
    }

    /** A sequence UUID makes completion idempotent if an entity is saved during its final tick. */
    public void complete(UUID owner, UUID ritual, int mob) {
        NBTTagList list = souls(owner);
        for (int i = 0; i < list.tagCount(); i++) {
            if (ritual.equals(list.getCompoundTagAt(i).getUniqueId("Soul"))) return;
        }
        NBTTagCompound soul = new NBTTagCompound();
        soul.setUniqueId("Soul", ritual);
        soul.setInteger("Mob", Math.floorMod(mob, 3));
        soul.setString("Name", "Test Soul " + (list.tagCount() + 1));
        list.appendTag(soul);
        markDirty();
    }

    @Override public void readFromNBT(NBTTagCompound nbt) {
        owners = nbt.getCompoundTag("Owners").copy();
        samples = nbt.getCompoundTag("Samples").copy();
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setTag("Owners", owners.copy());
        nbt.setTag("Samples", samples.copy());
        nbt.setInteger("Version", 2);
        return nbt;
    }
}
