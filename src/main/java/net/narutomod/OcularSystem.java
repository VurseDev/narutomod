package net.narutomod;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.item.*;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.gui.overlay.OverlayByakuganView;

/** The player NBT is authoritative; the head item is only a synchronized render/control projection. */
@ElementsNarutomodMod.ModElement.Tag
public class OcularSystem extends ElementsNarutomodMod.ModElement {
    private static final String COPY_CD="SharinganCopyCooldown";
    private static final java.util.Map<EntityPlayer,VisionGrant> VISION=new java.util.WeakHashMap<>();
    private static final class VisionGrant {
        final PotionEffect effect;long until;
        VisionGrant(PotionEffect effect,long until){this.effect=effect;this.until=until;}
    }
    public OcularSystem(ElementsNarutomodMod elements){super(elements,1140);}
    @Override public void init(FMLInitializationEvent e){MinecraftForge.EVENT_BUS.register(this);}
    public static boolean enabled(EntityLivingBase p){
        if(!(p instanceof EntityPlayer))return false;
        if(!p.world.isRemote)return p.getEntityData().hasKey(OcularState.KEY,10);
        ItemStack h=p.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        return h.getItem()==ItemOcularGear.SOCKETS&&h.hasTagCompound()&&h.getTagCompound().hasKey(OcularState.KEY,10);
    }
    public static OcularState state(EntityLivingBase p){
        NBTTagCompound n=p.getEntityData();
        if(p.world.isRemote){ItemStack h=p.getItemStackFromSlot(EntityEquipmentSlot.HEAD);n=h.hasTagCompound()?h.getTagCompound():new NBTTagCompound();}
        return OcularState.read(n.getCompoundTag(OcularState.KEY));
    }
    public static void save(EntityPlayer p,OcularState state){
        if(p.world.isRemote)return;
        state.revision++;p.getEntityData().setTag(OcularState.KEY,state.write());project(p,state);
    }
    private static void project(EntityPlayer p,OcularState state){
        ItemStack head=p.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if(head.getItem()!=ItemOcularGear.SOCKETS){
            p.setItemStackToSlot(EntityEquipmentSlot.HEAD,ItemStack.EMPTY);
            if(!head.isEmpty())ItemHandlerHelper.giveItemToPlayer(p,head); // Never erase displaced equipment.
            head=new ItemStack(ItemOcularGear.SOCKETS);p.setItemStackToSlot(EntityEquipmentSlot.HEAD,head);
        }
        NBTTagCompound n=new NBTTagCompound();n.setTag(OcularState.KEY,state.write());
        head.setTagCompound(n);p.inventory.markDirty();
    }
    public static void message(EntityPlayer p,String text){p.sendStatusMessage(new TextComponentString(text),true);}
    private static boolean ready(EntityPlayer p){
        if(p.world.isRemote||!enabled(p)||!p.isEntityAlive()||p.isSpectator())return false;
        if(MedicalSurgery.operating(p)||state(p).recoveringUntil>p.world.getTotalWorldTime()){
            message(p,"Your eyes are recovering / in surgery.");return false;
        }
        return true;
    }
    public static void select(EntityPlayer p){
        if(!ready(p))return;OcularState s=state(p);s.selected=1-s.selected;save(p,s);describe(p,s);
    }
    public static void focus(EntityPlayer p,int side){if(!ready(p)||side<0||side>1)return;OcularAbilities.cleanup(p);OcularState s=state(p);s.selected=side;save(p,s);describe(p,s);}
    public static void toggleSide(EntityPlayer p,int side){if(!ready(p)||side<0||side>1)return;OcularState s=state(p);if(s.toggleUntil>p.world.getTotalWorldTime())return;s.selected=side;save(p,s);toggle(p,false);}
    public static void toggle(EntityPlayer p,boolean cover){
        if(!ready(p))return;OcularState s=state(p);long now=p.world.getTotalWorldTime();
        if(s.toggleUntil>now)return;
        OcularAbilities.cleanup(p);
        OcularState.Eye e=s.eyes[s.selected];if(e==null){message(p,"That socket is empty.");return;}
        if(cover)e.covered=!e.covered;
        else if(e.sharingan()&&!OcularPolicy.canDeactivate(true,PlayerStats.getClan(p).equalsIgnoreCase("Uchiha")))e.covered=!e.covered;
        else if(e.rinnegan()){e.active=true;e.covered=!e.covered;}
        else if(e.sharingan()||e.byakugan()){e.active=!e.active;e.covered=false;}
        else e.covered=!e.covered;
        if(e.sharingan()&&!PlayerStats.getClan(p).equalsIgnoreCase("Uchiha"))e.active=true;
        s.toggleUntil=now+10;save(p,s);updateByakugan(p,s,false);describe(p,s);
        if(e.usable()&&(e.sharingan()||e.byakugan())){
            SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod",e.sharingan()?"sharingan_activate":"byakugan"));
            if(sound!=null)p.world.playSound(null,p.posX,p.posY,p.posZ,sound,SoundCategory.PLAYERS,0.8f,1f);
        }
    }
    private static void describe(EntityPlayer p,OcularState s){
        OcularState.Eye e=s.eyes[s.selected];message(p,(s.selected==0?"Left":"Right")+" socket: "+(e==null?"empty":e.stack.getDisplayName()+" / "+(e.covered?"covered":e.active?"active":"inactive")));
    }
    public static boolean key(EntityPlayer p,int key,boolean pressed){
        if(p.world.isRemote)return true;
        if(!ready(p)){OcularAbilities.cleanup(p);return true;}
        OcularState s=state(p);OcularState.Eye e=s.eyes[s.selected];
        if(OcularAbilities.key(p,s,key,pressed)){save(p,s);return true;}
        if(pressed||key!=1)return true;
        if(e!=null&&e.byakugan()){toggle(p,false);return true;}
        if(e!=null&&e.usable()&&e.stack.getItem()==ItemSharinganTomoe3.helmet){
            if(!e.stack.hasTagCompound())e.stack.setTagCompound(new NBTTagCompound());
            long shared=p.getEntityData().getLong("OcularCopyCooldown");
            e.stack.getTagCompound().setLong(COPY_CD,Math.max(shared,e.stack.getTagCompound().getLong(COPY_CD)));
            ItemSharinganCopy.attemptCopy(p,e.stack);
            p.getEntityData().setLong("OcularCopyCooldown",e.stack.getTagCompound().getLong(COPY_CD));save(p,s);
        }
        return true;
    }
    public static boolean switchTechnique(EntityPlayer p,boolean pressed){if(!ready(p))return true;OcularState s=state(p);OcularAbilities.switchTechnique(p,s,pressed);save(p,s);return true;}
    public static boolean activeSharingan(EntityLivingBase p){return active(p,false,false);}
    public static boolean activeByakugan(EntityLivingBase p){return active(p,true,false);}
    public static boolean threeTomoe(EntityLivingBase p){return active(p,false,true);}
    public static boolean activeMangekyo(EntityLivingBase p){
        if(!enabled(p))return false;OcularState s=state(p);
        if(!OcularPolicy.abilitiesAvailable(s.recoveringUntil,p.world.getTotalWorldTime(),!p.world.isRemote&&p instanceof EntityPlayer&&MedicalSurgery.operating((EntityPlayer)p)))return false;
        for(OcularState.Eye e:s.eyes)if(e!=null&&e.usable()&&e.mangekyo())return true;
        return false;
    }
    private static boolean active(EntityLivingBase p,boolean byakugan,boolean three){
        if(!enabled(p))return false;OcularState s=state(p);
        if(!OcularPolicy.abilitiesAvailable(s.recoveringUntil,p.world.getTotalWorldTime(),!p.world.isRemote&&p instanceof EntityPlayer&&MedicalSurgery.operating((EntityPlayer)p)))return false;
        for(OcularState.Eye e:s.eyes)if(e!=null&&e.usable()&&(byakugan?e.byakugan():e.sharingan())&&(!three||e.stack.getItem()==ItemSharinganTomoe3.helmet))return true;
        return false;
    }
    public static boolean allowsCopy(EntityPlayer p,ItemStack stack){
        if(!threeTomoe(p))return false;
        for(OcularState.Eye e:state(p).eyes)if(e!=null&&e.usable()&&e.stack.getItem()==stack.getItem()&&stack.hasTagCompound()
            &&stack.getTagCompound().hasUniqueId("OcularPhysicalId")&&e.id.equals(stack.getTagCompound().getUniqueId("OcularPhysicalId")))return true;
        return false;
    }
    public static void trainCopy(EntityPlayer p){
        OcularState s=state(p);
        for(OcularState.Eye e:s.eyes)if(e!=null&&e.usable()&&e.stack.getItem()==ItemSharinganTomoe3.helmet){ItemSharinganCopy.addCopyXp(e.stack,1);save(p,s);return;}
    }
    private static void updateByakugan(EntityPlayer p,OcularState s,boolean force){
        boolean active=OcularPolicy.abilitiesAvailable(s.recoveringUntil,p.world.getTotalWorldTime(),MedicalSurgery.operating(p));
        boolean any=false;for(OcularState.Eye e:s.eyes)if(e!=null&&e.byakugan()&&e.usable())any=true;
        active&=any;
        if(force||p.getEntityData().getBoolean("byakugan_activated")!=active){
            ProcedureSync.EntityNBTTag.setAndSync(p,"byakugan_activated",active);
            // Avoid the legacy overlay's false/false packet path, which changes the user's FOV.
            if(active||p.getEntityData().getBoolean("OcularByakuganSent"))OverlayByakuganView.sendCustomData(p,active,active?110f:0f);
            p.getEntityData().setBoolean("OcularByakuganSent",active);
        }
    }
    private static void updateVision(EntityPlayer p,boolean active){
        long now=p.world.getTotalWorldTime();PotionEffect current=p.getActivePotionEffect(MobEffects.NIGHT_VISION);
        VisionGrant grant=VISION.get(p);
        if(grant!=null&&(current!=grant.effect||current.getAmplifier()!=0||current.getIsAmbient()||current.doesShowParticles()
            ||current.getDuration()>Math.max(0,grant.until-now)+2)){VISION.remove(p);grant=null;}
        if(!active){
            if(grant!=null){p.removePotionEffect(MobEffects.NIGHT_VISION);VISION.remove(p);}
        }else if(current==null||grant!=null&&current.getDuration()<220){
            p.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION,240,0,false,false));
            VISION.put(p,new VisionGrant(p.getActivePotionEffect(MobEffects.NIGHT_VISION),now+240));
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void tick(TickEvent.PlayerTickEvent event){
        EntityPlayer p=event.player;if(event.phase!=TickEvent.Phase.START||p.world.isRemote||!enabled(p)||!p.isEntityAlive())return;
        OcularState s=state(p);boolean changed=false;
        changed|=OcularAbilities.tick(p,s);
        for(OcularState.Eye e:s.eyes)if(e!=null&&e.sharingan()&&!PlayerStats.getClan(p).equalsIgnoreCase("Uchiha")&&!e.active){e.active=true;changed=true;}
        // Projection copies are not organs and cannot be traded, banked or used to duplicate eyes.
        for(int i=0;i<p.inventory.mainInventory.size();i++)if(p.inventory.mainInventory.get(i).getItem()==ItemOcularGear.SOCKETS)p.inventory.mainInventory.set(i,ItemStack.EMPTY);
        for(int i=0;i<p.inventory.offHandInventory.size();i++)if(p.inventory.offHandInventory.get(i).getItem()==ItemOcularGear.SOCKETS)p.inventory.offHandInventory.set(i,ItemStack.EMPTY);
        if(p.ticksExisted%20==0){
            double cost=0;
            if(OcularPolicy.abilitiesAvailable(s.recoveringUntil,p.world.getTotalWorldTime(),MedicalSurgery.operating(p)))
                for(OcularState.Eye e:s.eyes)if(e!=null&&e.usable())cost+=OcularPolicy.upkeep(e,p.getUniqueID());
            if(cost>0&&!p.isCreative()&&!Chakra.pathway(p).consume(cost)){
                for(OcularState.Eye e:s.eyes)if(e!=null&&(e.sharingan()||e.byakugan()||e.rinnegan()))e.covered=true;
                OcularAbilities.cleanup(p);
                changed=true;message(p,"Not enough chakra; cover your implanted eyes to rest.");
            }
            if(!s.hasSight())p.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS,25,0,false,false));
        }
        if(changed)save(p,s);
        else {
            ItemStack head=p.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
            if(head.getItem()!=ItemOcularGear.SOCKETS||!head.hasTagCompound()||!s.write().equals(head.getTagCompound().getCompoundTag(OcularState.KEY)))project(p,s);
        }
        updateVision(p,activeSharingan(p)||!OcularAbilities.resolve(p,"rinnegan").isEmpty());
        if(ItemRinnegan.helmet instanceof ItemRinnegan.Base)((ItemRinnegan.Base)ItemRinnegan.helmet).onUpdatePost(p);
        updateByakugan(p,s,false);
    }
    private static void reconnect(EntityPlayer p){if(!p.world.isRemote&&enabled(p)){OcularAbilities.cleanup(p);project(p,state(p));updateByakugan(p,state(p),true);}}
    @SubscribeEvent public void logout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent e){if(enabled(e.player))OcularAbilities.cleanup(e.player);}
    @SubscribeEvent public void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e){reconnect(e.player);}
    @SubscribeEvent public void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent e){reconnect(e.player);}
    @SubscribeEvent public void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent e){reconnect(e.player);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void clone(PlayerEvent.Clone e){
        if(!e.getOriginal().world.isRemote&&enabled(e.getOriginal()))OcularAbilities.cleanup(e.getOriginal());
        if(e.getOriginal().getEntityData().hasKey(OcularState.KEY,10)){
            e.getEntityPlayer().getEntityData().setTag(OcularState.KEY,e.getOriginal().getEntityData().getCompoundTag(OcularState.KEY).copy());
            e.getEntityPlayer().getEntityData().setLong("OcularCopyCooldown",e.getOriginal().getEntityData().getLong("OcularCopyCooldown"));
        }
    }
    @SubscribeEvent public void drops(LivingDropsEvent e){
        if(e.getEntityLiving() instanceof EntityPlayer&&enabled(e.getEntityLiving()))e.getDrops().removeIf(drop->drop.getItem().getItem()==ItemOcularGear.SOCKETS);
    }
}
