package net.narutomod;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.narutomod.item.*;

/** Serialized per-player physical sockets. ItemStack payload retains donor ownership and eye XP. */
public final class OcularState {
    public static final String KEY="NarutomodOcularSockets";
    public final Eye[] eyes=new Eye[2];
    /** Recipient progression only, never an organ or an inventory item. */
    public final ItemStack[] nativeEyes={ItemStack.EMPTY,ItemStack.EMPTY};
    public int selected,revision;
    public String normalVariant="HAZEL";
    public long recoveringUntil,toggleUntil;
    public static final class Eye {
        public UUID id,donor;
        public int donorSide;
        public ItemStack stack=ItemStack.EMPTY;
        public boolean covered,active;
        public NBTTagCompound write(){
            NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Id",id);n.setUniqueId("Donor",donor);n.setInteger("DonorSide",donorSide);
            n.setTag("Stack",stack.writeToNBT(new NBTTagCompound()).copy());n.setBoolean("Covered",covered);n.setBoolean("Active",active);return n;
        }
        public static Eye read(NBTTagCompound n){
            if(!n.hasUniqueId("Id")||!n.hasUniqueId("Donor"))return null;
            Eye e=new Eye();e.id=n.getUniqueId("Id");e.donor=n.getUniqueId("Donor");e.donorSide=n.getInteger("DonorSide");
            e.stack=new ItemStack(n.getCompoundTag("Stack").copy());e.covered=n.getBoolean("Covered");e.active=n.getBoolean("Active");
            if(!supported(e.stack)||e.donorSide<0||e.donorSide>1)return null;
            e.stamp();return e;
        }
        private void stamp(){if(!stack.hasTagCompound())stack.setTagCompound(new NBTTagCompound());stack.getTagCompound().setUniqueId("OcularPhysicalId",id);}
        public boolean sharingan(){return stack.getItem() instanceof ItemSharingan.Base;}
        public boolean byakugan(){return stack.getItem()==ItemByakugan.helmet;}
        public boolean rinnegan(){return stack.getItem()==ItemRinnegan.helmet;}
        public boolean mangekyo(){return ItemSharingan.isMangekyo(stack);}
        public boolean eternal(){return ItemSharingan.isEternal(stack);}
        public ItemSharingan.Type family(){
            if(!sharingan())return ItemSharingan.Type.BASE;
            if(stack.hasTagCompound()&&stack.getTagCompound().hasKey("OcularAbilityFamily",8)){
                try{return ItemSharingan.Type.valueOf(stack.getTagCompound().getString("OcularAbilityFamily"));}catch(IllegalArgumentException ignored){}
            }
            return ((ItemSharingan.Base)stack.getItem()).getSubType();
        }
        public boolean usable(){return !covered&&active&&!ItemSharingan.isBlinded(stack);}
        public String label(){return stack.getDisplayName();}
    }
    public static boolean supported(ItemStack stack){
        return !stack.isEmpty()&&stack.getCount()==1&&(ItemNormalEyes.isNormalEyes(stack)||stack.getItem()==ItemByakugan.helmet&&!ItemByakugan.isRinnesharinganActivated(stack)
            ||stack.getItem() instanceof ItemSharingan.Base||stack.getItem()==ItemRinnegan.helmet&&!ItemByakugan.isRinnesharinganActivated(stack));
    }
    public static Eye original(ItemStack stack,UUID donor,int side){
        Eye e=new Eye();e.donor=donor;e.donorSide=side;
        e.id=UUID.nameUUIDFromBytes(("narutomod:physical-eye:"+donor+":"+side).getBytes(StandardCharsets.UTF_8));
        e.stack=stack.copy();e.stack.setCount(1);e.active=e.sharingan();e.stamp();return e;
    }
    public NBTTagCompound write(){
        NBTTagCompound n=new NBTTagCompound();n.setInteger("Version",2);n.setInteger("Selected",selected);n.setInteger("Revision",revision);
        n.setString("NormalVariant",normalVariant);
        n.setLong("Recovery",recoveringUntil);n.setLong("ToggleUntil",toggleUntil);
        for(int i=0;i<2;i++){
            if(eyes[i]!=null)n.setTag(i==0?"Left":"Right",eyes[i].write());
            if(!nativeEyes[i].isEmpty())n.setTag("Native"+i,nativeEyes[i].writeToNBT(new NBTTagCompound()).copy());
        }return n;
    }
    public static OcularState read(NBTTagCompound n){
        OcularState s=new OcularState();s.selected=n.getInteger("Selected")==1?1:0;s.revision=n.getInteger("Revision");
        if(n.hasKey("NormalVariant",8))s.normalVariant=n.getString("NormalVariant");
        s.recoveringUntil=n.getLong("Recovery");s.toggleUntil=n.getLong("ToggleUntil");
        s.eyes[0]=Eye.read(n.getCompoundTag("Left"));s.eyes[1]=Eye.read(n.getCompoundTag("Right"));
        for(int i=0;i<2;i++){ItemStack nativeEye=new ItemStack(n.getCompoundTag("Native"+i).copy());if(ItemSharingan.isMangekyo(nativeEye))s.nativeEyes[i]=nativeEye;}
        return s;
    }
    public boolean hasSight(){for(Eye e:eyes)if(e!=null&&!e.covered&&!ItemSharingan.isBlinded(e.stack))return true;return false;}
    public static boolean distinct(Eye left,Eye right){return left==null||right==null||!left.id.equals(right.id);}
}
