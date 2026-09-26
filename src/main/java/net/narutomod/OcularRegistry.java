package net.narutomod;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.*;

/** Server-wide eye identity ledger: a duplicated jar or legacy progression token cannot create a second eye. */
public final class OcularRegistry extends WorldSavedData {
    private static final String NAME="narutomod_ocular_registry";
    private NBTTagCompound locations=new NBTTagCompound();
    private NBTTagCompound families=new NBTTagCompound();
    public OcularRegistry(){super(NAME);}public OcularRegistry(String name){super(name);}
    public static OcularRegistry get(World world){
        MapStorage storage=world.getMinecraftServer().getWorld(0).getMapStorage();
        OcularRegistry r=(OcularRegistry)storage.getOrLoadData(OcularRegistry.class,NAME);
        if(r==null){r=new OcularRegistry();storage.setData(NAME,r);}return r;
    }
    public boolean unclaimed(UUID id){return !locations.hasKey(id.toString());}
    public boolean inJar(UUID id){return "jar".equals(locations.getString(id.toString()));}
    public boolean installed(UUID id,UUID patient,int side){return (patient+":"+side).equals(locations.getString(id.toString()));}
    public void jar(UUID id){locations.setString(id.toString(),"jar");markDirty();}
    public void install(UUID id,UUID patient,int side){locations.setString(id.toString(),patient+":"+side);markDirty();}
    public String family(UUID player){return families.getString(player.toString());}
    public void family(UUID player,String family){if(family.isEmpty())families.removeTag(player.toString());else families.setString(player.toString(),family);markDirty();}
    public boolean bloodRelated(UUID a,UUID b){String f=family(a);return !a.equals(b)&&!f.isEmpty()&&f.equals(family(b));}
    @Override public void readFromNBT(NBTTagCompound n){locations=n.getCompoundTag("Locations");families=n.getCompoundTag("Families");}
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n){n.setTag("Locations",locations.copy());n.setTag("Families",families.copy());return n;}
}
