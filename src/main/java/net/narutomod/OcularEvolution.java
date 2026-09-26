package net.narutomod;

import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.narutomod.item.*;
import net.narutomod.procedure.ProcedureUtils;

/** Surgery-only EMS evolution. Physical donor identity and recipient ability lineage are separate. */
public final class OcularEvolution {
    private OcularEvolution(){}
    public static boolean eligible(OcularState state,UUID recipient,boolean uchiha,OcularRegistry registry){
        OcularState.Eye a=state.eyes[0],b=state.eyes[1];
        if(!uchiha||a==null||b==null||!OcularState.distinct(a,b)||!a.mangekyo()||!b.mangekyo()
            ||a.eternal()||b.eternal()||!a.donor.equals(b.donor)||a.donorSide==b.donorSide
            ||!registry.bloodRelated(recipient,a.donor))return false;
        for(ItemStack nativeEye:state.nativeEyes)if(!ItemSharingan.isMangekyo(nativeEye)||!recipient.equals(ProcedureUtils.getOwnerId(nativeEye)))return false;
        return family(state.nativeEyes[0])==family(state.nativeEyes[1]);
    }
    private static ItemSharingan.Type family(ItemStack eye){
        if(eye.hasTagCompound()&&eye.getTagCompound().hasKey("OcularAbilityFamily",8)){
            try{return ItemSharingan.Type.valueOf(eye.getTagCompound().getString("OcularAbilityFamily"));}catch(IllegalArgumentException ignored){}
        }
        return ((ItemSharingan.Base)eye.getItem()).getSubType();
    }
    public static boolean evolve(OcularState state,UUID recipient,boolean uchiha,OcularRegistry registry){
        if(!eligible(state,recipient,uchiha,registry))return false;
        for(OcularState.Eye eye:state.eyes){
            ItemStack progression=state.nativeEyes[eye.donorSide];ItemSharingan.Type family=family(progression);
            ItemStack eternal=new ItemStack(family==ItemSharingan.Type.MADARA?ItemMangekyoSharinganMadaraEternal.helmet:ItemMangekyoSharinganEternal.helmet);
            NBTTagCompound data=progression.hasTagCompound()?progression.getTagCompound().copy():new NBTTagCompound();
            data.setString("OcularAbilityFamily",family.name());data.setUniqueId("OcularPhysicalId",eye.id);
            data.setUniqueId("OcularEvolvedFor",recipient);data.setBoolean("sharingan_blinded",false);
            // Recipient progression survives, but never reset any cooldown recorded on the donated eye.
            if(eye.stack.hasTagCompound())for(String key:eye.stack.getTagCompound().getKeySet())
                if(key.toLowerCase(java.util.Locale.ROOT).contains("cooldown")&&eye.stack.getTagCompound().hasKey(key,99))
                    data.setLong(key,Math.max(data.getLong(key),eye.stack.getTagCompound().getLong(key)));
            eternal.setTagCompound(data);eternal.setItemDamage(0);eye.stack=eternal;
        }
        return true;
    }
}
