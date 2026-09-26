package net.narutomod.item;

import java.util.List;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.ElementsNarutomodMod;
import net.narutomod.PaperBombPolicy;
import net.narutomod.block.BlockExplosiveTag;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.entity.EntityPaperBombCast;

@ElementsNarutomodMod.ModElement.Tag
public class ItemExplosiveArt extends ElementsNarutomodMod.ModElement {
    @ObjectHolder("narutomod:explosive_art") public static final Item block=null;
    public static final ItemJutsu.JutsuEnum VOLLEY=technique(0,"tag_volley",'C');
    public static final ItemJutsu.JutsuEnum CIRCUIT=technique(1,"snare_circuit",'B');
    public static final ItemJutsu.JutsuEnum SWARM=technique(2,"seeking_tag_swarm",'A');
    public static final ItemJutsu.JutsuEnum BREACH=technique(3,"breaching_seal",'B');
    public ItemExplosiveArt(ElementsNarutomodMod elements) { super(elements,1130); }
    private static ItemJutsu.JutsuEnum technique(int mode,String name,char rank) {
        return new ItemJutsu.JutsuEnum(mode,"jutsu.narutomod."+name,rank,PaperBombPolicy.chakra(mode),new Cast(mode)).withCustomBalance();
    }
    @Override public void initElements() { elements.items.add(ArtItem::new); }
    @Override @SideOnly(Side.CLIENT) public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(block,0,new ModelResourceLocation("narutomod:explosive_art","inventory"));
    }
    public static int count(EntityPlayer player) {
        return net.narutomod.PaperBombAmmo.count(ammunitionSlots(player),Item.getItemFromBlock(BlockExplosiveTag.block));
    }
    private static Iterable<ItemStack> ammunitionSlots(EntityPlayer player){
        return com.google.common.collect.Iterables.concat(player.inventory.mainInventory,player.inventory.offHandInventory);
    }
    public static boolean consume(EntityPlayer player,int amount) {
        if(!net.narutomod.PaperBombAmmo.consume(ammunitionSlots(player),Item.getItemFromBlock(BlockExplosiveTag.block),amount))return false;
        player.inventory.markDirty();return true;
    }
    public static class ArtItem extends ItemJutsu.Base {
        public ArtItem(){super(ItemJutsu.JutsuEnum.Type.NINJUTSU,VOLLEY,CIRCUIT,SWARM,BREACH);setRegistryName("explosive_art");setUnlocalizedName("explosive_art");setCreativeTab(TabCustomTabs.jutsus);}
        @Override public boolean isOwner(ItemStack stack,EntityLivingBase caster){return super.isOwner(stack,caster);}
        @Override public float getPower(ItemStack stack,EntityLivingBase caster,int timeLeft){return getMaxUseDuration()-timeLeft>=12?1:0;}
        @Override public void onUsingTick(ItemStack stack,EntityLivingBase caster,int timeLeft){
            super.onUsingTick(stack,caster,timeLeft);
            int elapsed=getMaxUseDuration()-timeLeft;
            if(!caster.world.isRemote&&(elapsed==4||elapsed==8||elapsed==12)){
                net.minecraft.util.SoundEvent sound=net.minecraft.util.SoundEvent.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("narutomod:paperflip"));
                if(sound!=null)caster.playSound(sound,.3f,1+elapsed*.025f);
            }
        }
        @Override public EnumActionResult canActivateJutsu(ItemStack stack,ItemJutsu.JutsuEnum jutsu,EntityPlayer player) {
            EnumActionResult result=super.canActivateJutsu(stack,jutsu,player);
            if(result!=EnumActionResult.SUCCESS)return result;
            if(count(player)<PaperBombPolicy.bombs(jutsu.index)) {
                if(!player.world.isRemote)player.sendStatusMessage(new TextComponentTranslation("message.narutomod.paper_cost",PaperBombPolicy.bombs(jutsu.index),count(player)),true);
                return EnumActionResult.FAIL;
            }
            if(!player.world.isRemote&&player.getEntityData().getLong("PaperBombCooldown"+jutsu.index)>player.world.getTotalWorldTime())return EnumActionResult.PASS;
            return result;
        }
        @Override protected boolean executeJutsu(ItemStack stack,EntityLivingBase caster,float power) {
            return power>0&&caster instanceof EntityPlayer&&!caster.world.isRemote
                &&canActivateJutsu(stack,getCurrentJutsu(stack),(EntityPlayer)caster)==EnumActionResult.SUCCESS
                &&super.executeJutsu(stack,caster,1);
        }
        @Override @SideOnly(Side.CLIENT) public void addInformation(ItemStack stack,World world,List<String> lines,ITooltipFlag flag) {
            super.addInformation(stack,world,lines,flag);
            int mode=getCurrentJutsu(stack).index;
            lines.add(new TextComponentTranslation("tooltip.narutomod.paper_cost",PaperBombPolicy.bombs(mode),PaperBombPolicy.chakra(mode),PaperBombPolicy.cooldown(mode)/20).getUnformattedText());
            lines.add(new TextComponentTranslation("tooltip.narutomod.paper_controls").getUnformattedText());
        }
    }
    public static class Cast implements ItemJutsu.IJutsuCallback {
        private final int mode;
        Cast(int mode){this.mode=mode;}
        @Override public float getMaxPower(){return 1;}
        @Override public boolean createJutsu(ItemStack stack,EntityLivingBase caster,float power) {
            if(power<=0||!(caster instanceof EntityPlayer)||caster.world.isRemote||stack.getItem()!=block)return false;
            EntityPlayer player=(EntityPlayer)caster;
            if(player.getEntityData().getLong("PaperBombCooldown"+mode)>player.world.getTotalWorldTime()||count(player)<PaperBombPolicy.bombs(mode))return false;
            EntityPaperBombCast cast=EntityPaperBombCast.prepare(player,mode);
            if(cast==null||!player.world.spawnEntity(cast))return false;
            if(!consume(player,PaperBombPolicy.bombs(mode))){cast.setDead();return false;}
            cast.clearPlacementGrass(player);
            player.getEntityData().setLong("PaperBombCooldown"+mode,player.world.getTotalWorldTime()+PaperBombPolicy.cooldown(mode));
            ItemJutsu.setCurrentJutsuCooldown(stack,PaperBombPolicy.cooldown(mode));
            caster.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
            cast.paperSound();return true;
        }
    }
}
