package net.narutomod.item;

import java.util.List;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.*;
import net.narutomod.*;

@ElementsNarutomodMod.ModElement.Tag
public class ItemOcularGear extends ElementsNarutomodMod.ModElement {
    @ObjectHolder("narutomod:ocular_sockets")public static final Item SOCKETS=null;
    @ObjectHolder("narutomod:preserved_eye")public static final Item EYE=null;
    public ItemOcularGear(ElementsNarutomodMod elements){super(elements,1141);}
    @Override public void initElements(){
        ItemArmor.ArmorMaterial material=EnumHelper.addArmorMaterial("OCULAR_SOCKETS","narutomod:normal_eyes",0,new int[]{0,0,0,0},0,null,0);
        elements.items.add(()->new Sockets(material));
        elements.items.add(()->new Item(){
            @Override @SideOnly(Side.CLIENT)public void addInformation(ItemStack stack,World world,List<String> lines,ITooltipFlag flag){
                OcularState.Eye e=eye(stack);if(e!=null){lines.add(e.label());lines.add("Physical donor eye — medical surgery required.");}
                else lines.add("Invalid/unregistered specimen; cannot be implanted.");
            }
        }.setRegistryName("preserved_eye").setUnlocalizedName("preserved_eye").setMaxStackSize(1));
    }
    @Override @SideOnly(Side.CLIENT)public void registerModels(ModelRegistryEvent event){
        ModelLoader.setCustomModelResourceLocation(SOCKETS,0,new ModelResourceLocation("narutomod:ocular_sockets","inventory"));
        ModelLoader.setCustomModelResourceLocation(EYE,0,new ModelResourceLocation("narutomod:preserved_eye","inventory"));
    }
    public static OcularState.Eye eye(ItemStack stack){
        if(stack.isEmpty()||!stack.hasTagCompound())return null;
        if(stack.getItem()==EYE)return OcularState.Eye.read(stack.getTagCompound().getCompoundTag("Eye"));
        if(!OcularState.supported(stack)||!stack.getTagCompound().hasUniqueId("OcularPhysicalId")||!stack.getTagCompound().hasUniqueId("OcularDonor"))return null;
        OcularState.Eye e=new OcularState.Eye();e.id=stack.getTagCompound().getUniqueId("OcularPhysicalId");
        e.donor=stack.getTagCompound().getUniqueId("OcularDonor");e.stack=stack.copy();return e;
    }
    /** Actual item, with provenance stored invisibly. Retains the old method name for save compatibility callers. */
    public static ItemStack jar(OcularState.Eye eye){
        ItemStack stack=eye.stack.copy();stack.setCount(1);
        if(!stack.hasTagCompound())stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setUniqueId("OcularPhysicalId",eye.id);
        stack.getTagCompound().setUniqueId("OcularDonor",eye.donor);return stack;
    }
    public static class Sockets extends ItemDojutsu.Base {
        public Sockets(ItemArmor.ArmorMaterial material){super(material);setRegistryName("ocular_sockets");setUnlocalizedName("ocular_sockets");}
        @Override public boolean isValidArmor(ItemStack stack,EntityEquipmentSlot slot,Entity entity){return false;}
        @Override public boolean hasEffect(ItemStack stack){return false;}
        @Override public boolean onEntityItemUpdate(net.minecraft.entity.item.EntityItem entity){if(!entity.world.isRemote)entity.setDead();return true;}
        @Override public ItemDojutsu.Type getType(){return ItemDojutsu.Type.IMPLANTED;}
        @Override public void onArmorTick(World world,EntityPlayer player,ItemStack stack){} // Central controller owns upkeep, never the legacy foreign-item tick.
        @Override public void onUpdate(ItemStack stack,World world,Entity entity,int slot,boolean selected){}
        @Override public boolean onJutsuKey1(boolean pressed,ItemStack stack,EntityPlayer player){return OcularSystem.key(player,1,pressed);}
        @Override public boolean onJutsuKey2(boolean pressed,ItemStack stack,EntityPlayer player){return OcularSystem.key(player,2,pressed);}
        @Override public boolean onJutsuKey3(boolean pressed,ItemStack stack,EntityPlayer player){return OcularSystem.key(player,3,pressed);}
        @Override public boolean onSwitchJutsuKey(boolean pressed,ItemStack stack,EntityPlayer player){return OcularSystem.switchTechnique(player,pressed);}
        @Override public String getArmorTexture(ItemStack stack,Entity entity,EntityEquipmentSlot slot,String type){return "narutomod:textures/normal_eyes.png";}
        @Override @SideOnly(Side.CLIENT)public ModelBiped getArmorModel(EntityLivingBase e,ItemStack s,EntityEquipmentSlot slot,ModelBiped original){return net.narutomod.client.OcularModel.get(e);}
    }
}
