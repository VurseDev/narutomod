package net.narutomod;

import java.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.command.*;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.*;
import net.narutomod.item.*;
import net.narutomod.procedure.ProcedureUtils;

/** Server-owned sessions. The client submits indices, never eye NBT or inventory writes. */
@ElementsNarutomodMod.ModElement.Tag
public class MedicalSurgery extends ElementsNarutomodMod.ModElement {
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    public MedicalSurgery(ElementsNarutomodMod elements){super(elements,1142);}
    @Override public void init(FMLInitializationEvent e){MinecraftForge.EVENT_BUS.register(this);}
    @Override public void preInit(FMLPreInitializationEvent e){
        elements.addNetworkMessage(OpenMessage.Handler.class,OpenMessage.class,Side.CLIENT);
        elements.addNetworkMessage(ChoiceMessage.Handler.class,ChoiceMessage.class,Side.SERVER);
        elements.addNetworkMessage(ConsentMessage.Handler.class,ConsentMessage.class,Side.SERVER);
    }
    @Override public void serverLoad(FMLServerStartingEvent e){SESSIONS.clear();e.registerServerCommand(new ConsentCommand());}
    public static boolean busy(EntityPlayer p){for(Session s:SESSIONS.values())if(s.surgeon==p||s.patient==p)return true;return false;}
    /** Pending invitations reserve menus only; they must not disable a patient's abilities without consent. */
    public static boolean operating(EntityPlayer p){for(Session s:SESSIONS.values())if(s.channel&&(s.surgeon==p||s.patient==p))return true;return false;}
    public static boolean qualified(EntityPlayer p,ItemStack medical){
        return medical.getItem()==ItemIryoJutsu.block&&((ItemIryoJutsu.RangedItem)medical.getItem()).qualifiedForSurgery(medical,p);
    }
    public static void open(EntityPlayerMP surgeon){
        ItemStack medical=surgeon.getHeldItemMainhand();
        if(!qualified(surgeon,medical)){tell(surgeon,"Ocular Surgery requires 3,000 Healing Jutsu XP on your own Medical Jutsu item.");return;}
        EntityPlayerMP patient=null;
        if(surgeon.isSneaking())patient=surgeon;
        else {RayTraceResult r=ProcedureUtils.objectEntityLookingAt(surgeon,3d);if(r!=null&&r.entityHit instanceof EntityPlayerMP)patient=(EntityPlayerMP)r.entityHit;}
        if(patient==null){tell(surgeon,"Aim at a player within 3 blocks, or sneak-cast for self-surgery.");return;}
        if(busy(surgeon)||busy(patient)){tell(surgeon,"One of you already has a surgery menu or procedure open.");return;}
        Session s=new Session(surgeon,patient);
        String problem=s.valid(false);
        if(problem!=null){tell(surgeon,problem);return;}
        if(!s.load()){tell(surgeon,"Equip supported eyes or remove your helmet before surgery.");return;}
        if(!s.identitiesValid()){tell(surgeon,"An eye identity is already claimed elsewhere; this legacy item cannot create another physical eye.");return;}
        SESSIONS.put(s.id,s);NarutomodMod.PACKET_HANDLER.sendTo(new OpenMessage(s.menu()),surgeon);
    }
    private static void tell(EntityPlayer p,String text){p.sendStatusMessage(new TextComponentString(text),true);}
    private static final class Option {
        final OcularState.Eye eye;final int slot;final ItemStack snapshot;final EntityPlayerMP source;final boolean pair;
        Option(OcularState.Eye e,int inventorySlot,ItemStack stack){this(e,inventorySlot,stack,null,false);}
        Option(OcularState.Eye e,int inventorySlot,ItemStack stack,EntityPlayerMP owner,boolean paired){eye=e;slot=inventorySlot;snapshot=stack.copy();source=owner;pair=paired;}
    }

    /**
     * Compares a surgery input by physical identity and eye form, not by the
     * complete live ItemStack payload.  Dojutsu stacks are updated by their
     * normal tick (durability/strain and per-technique cooldown tags), so an
     * exact NBT comparison can report a false inventory swap while a menu is
     * open.  The item, owner, physical id, Mangekyo family/stage and count are
     * still authoritative; unrelated mutable runtime tags are deliberately
     * ignored.
     */
    static boolean sameSurgeryStack(ItemStack first,ItemStack second){
        if(first==second)return true;
        if(first==null||second==null)return false;
        if(first.isEmpty()||second.isEmpty())return first.isEmpty()&&second.isEmpty();
        if(first.getItem()!=second.getItem()||first.getCount()!=second.getCount())return false;
        OcularState.Eye firstJar=ItemOcularGear.eye(first),secondJar=ItemOcularGear.eye(second);
        if(firstJar!=null||secondJar!=null){
            return firstJar!=null&&secondJar!=null&&sameSurgeryEye(firstJar,secondJar);
        }
        if(OcularState.supported(first)||OcularState.supported(second))return sameSurgeryEyeForm(first,second);
        return ItemStack.areItemStacksEqual(first,second);
    }

    private static boolean sameSurgeryEye(OcularState.Eye first,OcularState.Eye second){
        return first!=null&&second!=null&&Objects.equals(first.id,second.id)
            &&Objects.equals(first.donor,second.donor)&&first.donorSide==second.donorSide
            &&sameSurgeryEyeForm(first.stack,second.stack);
    }

    private static UUID physicalId(ItemStack stack){
        return stack!=null&&stack.hasTagCompound()&&stack.getTagCompound().hasUniqueId("OcularPhysicalId")
            ?stack.getTagCompound().getUniqueId("OcularPhysicalId"):null;
    }

    /** Stable form fields for a supported legacy specimen or a serialized eye payload. */
    private static boolean sameSurgeryEyeForm(ItemStack first,ItemStack second){
        if(first==null||second==null||first.isEmpty()||second.isEmpty())return first==second||(first!=null&&second!=null&&first.isEmpty()&&second.isEmpty());
        if(first.getItem()!=second.getItem()||first.getCount()!=second.getCount())return false;
        if(!Objects.equals(ProcedureUtils.getOwnerId(first),ProcedureUtils.getOwnerId(second)))return false;
        if(!Objects.equals(physicalId(first),physicalId(second)))return false;
        if(first.getItem() instanceof ItemSharingan.Base){
            ItemSharingan.Base a=(ItemSharingan.Base)first.getItem(),b=(ItemSharingan.Base)second.getItem();
            if(a.isMangekyo()!=b.isMangekyo()||a.isEternal()!=b.isEternal()||a.getSubType()!=b.getSubType())return false;
            String af=familyTag(first),bf=familyTag(second);
            return Objects.equals(af,bf);
        }
        // Normal-eye colour is encoded by the item itself; Byakugan/Rinnegan
        // form is likewise item-defined (activated Rinnesharingan is not a
        // supported surgery input).
        return true;
    }

    private static String familyTag(ItemStack stack){
        return stack!=null&&stack.hasTagCompound()&&stack.getTagCompound().hasKey("OcularAbilityFamily",8)
            ?stack.getTagCompound().getString("OcularAbilityFamily"):"";
    }
    private static final class Session {
        final UUID id=UUID.randomUUID();final EntityPlayerMP surgeon,patient;final ItemStack medical;
        final int dimension;final long expires;final ItemStack oldHead;final boolean migrated;
        final List<Option> options=new ArrayList<>();OcularState before;
        int left,right;boolean proposed,channel;long finish;Vec3d surgeonStart,patientStart;float surgeonHealth,patientHealth;
        Session(EntityPlayerMP a,EntityPlayerMP b){
            surgeon=a;patient=b;medical=a.getHeldItemMainhand();dimension=a.dimension;
            expires=a.world.getTotalWorldTime()+OcularPolicy.SESSION_TICKS;
            oldHead=b.getItemStackFromSlot(EntityEquipmentSlot.HEAD).copy();migrated=OcularSystem.enabled(b);
        }
        boolean load(){
            before=new OcularState();
            before.normalVariant=patient.getEntityData().getString("NarutomodNormalEyesVariant");
            if(migrated)before=OcularSystem.state(patient);
            else if(!oldHead.isEmpty()){
                if(!OcularState.supported(oldHead)||oldHead.getCount()!=1)return false;
                UUID donor=ProcedureUtils.getOwnerId(oldHead);
                if(donor==null&&!ItemNormalEyes.isNormalEyes(oldHead))return false;
                if(donor==null)donor=patient.getUniqueID();
                OcularState.Eye physical=ItemOcularGear.eye(oldHead);
                if(physical!=null)before.eyes[0]=physical;
                else for(int i=0;i<2;i++)before.eyes[i]=OcularState.original(oldHead,donor,i);
            }
            options.add(new Option(null,-1,ItemStack.EMPTY));
            for(int i=0;i<2;i++)if(before.eyes[i]!=null){if(i==0)left=options.size();else right=options.size();options.add(new Option(before.eyes[i],-1,ItemStack.EMPTY));}
            Set<UUID> seen=new HashSet<>();for(OcularState.Eye e:before.eyes)if(e!=null)seen.add(e.id);
            OcularRegistry registry=OcularRegistry.get(surgeon.world);
            addInventory(patient,registry,seen);
            if(surgeon!=patient)addInventory(surgeon,registry,seen);
            for(int i=0;i<2;i++)rememberNative(before,before.eyes[i],patient.getUniqueID());
            return true;
        }
        void addInventory(EntityPlayerMP source,OcularRegistry registry,Set<UUID> seen){
            for(int slot=0;slot<source.inventory.mainInventory.size();slot++){
                ItemStack stack=source.inventory.mainInventory.get(slot);
                if(stack.isEmpty()||stack.getCount()!=1)continue;
                OcularState.Eye jar=ItemOcularGear.eye(stack);
                if(jar!=null&&stack.getItem()==ItemOcularGear.EYE){
                    stack=ItemOcularGear.jar(jar);source.inventory.mainInventory.set(slot,stack);source.inventory.markDirty();
                    jar=ItemOcularGear.eye(stack);
                }
                if(jar!=null){if((registry.inJar(jar.id)||registry.unclaimed(jar.id))&&seen.add(jar.id))options.add(new Option(jar,slot,stack,source,false));continue;}
                if(!OcularState.supported(stack))continue;
                UUID donor=ProcedureUtils.getOwnerId(stack);
                if(donor==null)donor=source.getUniqueID();
                OcularState.Eye eye=new OcularState.Eye();eye.id=UUID.randomUUID();eye.donor=donor;eye.stack=stack.copy();
                ProcedureUtils.setOriginalOwner(eye.stack,donor);
                // One inventory item is one freely assignable eye. Persist its identity
                // so reopening the menu cannot manufacture a new identity for a copy.
                ItemStack physical=ItemOcularGear.jar(eye);
                source.inventory.mainInventory.set(slot,physical);source.inventory.markDirty();
                registry.jar(eye.id);
                if(seen.add(eye.id))options.add(new Option(ItemOcularGear.eye(physical),slot,physical,source,false));
            }
        }
        boolean ownsBefore(UUID id){for(OcularState.Eye e:before.eyes)if(e!=null&&e.id.equals(id))return true;return false;}
        boolean sameExistingForm(OcularState.Eye eye){for(OcularState.Eye e:before.eyes)if(e!=null&&e.id.equals(eye.id)&&sameSurgeryStack(e.stack,eye.stack))return true;return false;}
        String valid(boolean checkState){
            if(!surgeon.isEntityAlive()||!patient.isEntityAlive()||surgeon.isSpectator()||patient.isSpectator()
                ||surgeon.connection==null||patient.connection==null||surgeon.hasDisconnected()||patient.hasDisconnected())return "Surgery cancelled: player unavailable.";
            if(surgeon.dimension!=dimension||patient.dimension!=dimension||surgeon.world!=patient.world
                ||surgeon.getDistanceSq(patient)>9||!surgeon.canEntityBeSeen(patient))return "Surgery cancelled: stay within 3 blocks and in sight.";
            if(surgeon.isRiding()||patient.isRiding()||surgeon.isInWater()||patient.isInWater()||!surgeon.onGround||!patient.onGround)
                return "Surgery requires both players unmounted on dry ground.";
            if(surgeon.hurtTime>0||patient.hurtTime>0||surgeon.getRevengeTimer()>surgeon.ticksExisted-100||patient.getRevengeTimer()>patient.ticksExisted-100)
                return "Surgery cancelled: leave combat and wait 5 seconds.";
            if(surgeon.world.getTotalWorldTime()>expires)return "Surgery session expired.";
            if(surgeon.getHeldItemMainhand()!=medical||!qualified(surgeon,medical)||ItemJutsu.getCurrentJutsu(medical)!=ItemIryoJutsu.SURGERY
                ||((ItemIryoJutsu.RangedItem)medical.getItem()).canActivateJutsu(medical,ItemIryoJutsu.SURGERY,surgeon)!=EnumActionResult.SUCCESS)
                return "Surgery cancelled: keep your qualified Medical Jutsu selected and usable.";
            if(Chakra.pathway(surgeon).getAmount()<OcularPolicy.CHAKRA_COST)return "Surgery requires 100 chakra (charged only on success).";
            if(OcularSystem.enabled(patient)&&OcularSystem.state(patient).recoveringUntil>patient.world.getTotalWorldTime())return "The patient's eyes are still recovering.";
            if(checkState){
                if(migrated!=OcularSystem.enabled(patient))return "Surgery cancelled: patient's eye state changed.";
                if(migrated){if(OcularSystem.state(patient).revision!=before.revision)return "Surgery cancelled: patient's eyes changed; reopen the menu.";}
                else if(!sameSurgeryStack(oldHead,patient.getItemStackFromSlot(EntityEquipmentSlot.HEAD)))return "Surgery cancelled: patient's equipped eyes changed.";
            }
            if(channel&&(surgeon.getPositionVector().squareDistanceTo(surgeonStart)>0.04||patient.getPositionVector().squareDistanceTo(patientStart)>0.04
                ||surgeon.getHealth()<surgeonHealth||patient.getHealth()<patientHealth))return "Surgery interrupted by movement or damage; nothing was consumed.";
            return null;
        }
        boolean identitiesValid(){
            OcularRegistry registry=OcularRegistry.get(surgeon.world);
            if(!OcularState.distinct(before.eyes[0],before.eyes[1]))return false;
            for(int i=0;i<2;i++)if(before.eyes[i]!=null&&!(migrated?registry.installed(before.eyes[i].id,patient.getUniqueID(),i):registry.unclaimed(before.eyes[i].id)||(ItemOcularGear.eye(oldHead)!=null&&registry.inJar(before.eyes[i].id))))return false;
            if(proposed)for(int index:new int[]{left,right}){
                Option o=options.get(index);
                if(o.slot>=0){
                    if(o.source==null||!sameSurgeryStack(o.snapshot,o.source.inventory.mainInventory.get(o.slot)))return false;
                    if(!o.pair&&!registry.inJar(o.eye.id)&&!registry.unclaimed(o.eye.id))return false;
                    if(o.pair&&!registry.unclaimed(o.eye.id)&&!(o.eye.donor.equals(patient.getUniqueID())&&ownsBefore(o.eye.id)))return false;
                    if(o.pair)for(int side=0;side<2;side++){
                        UUID id=OcularState.original(o.snapshot,o.eye.donor,side).id;
                        if(!registry.unclaimed(id)&&!ownsBefore(id))return false;
                    }
                }
            }
            return true;
        }
        NBTTagCompound menu(){
            NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Session",id);n.setString("Patient",patient.getName());
            n.setUniqueId("PatientId",patient.getUniqueID());
            boolean nativeUchiha="Uchiha".equalsIgnoreCase(PlayerStats.getClan(patient));n.setBoolean("NativeUchiha",nativeUchiha);
            n.setInteger("Left",left);n.setInteger("Right",right);n.setBoolean("Self",surgeon==patient);
            NBTTagList list=new NBTTagList(),details=new NBTTagList();
            for(Option o:options){
                list.appendTag(new NBTTagString(o.eye==null?"Empty socket":(o.slot<0?"Current: ":"Inventory #"+(o.slot+1)+": ")+o.eye.label()));
                NBTTagCompound detail=describeOption(o.eye,o.slot,patient.getUniqueID(),nativeUchiha);
                if(o.source!=null)detail.setString("Source",o.source==patient?"patient":"medic");
                if(o.eye!=null)detail.setUniqueId("Identity",o.eye.id);
                detail.setBoolean("Pair",o.pair);details.appendTag(detail);
            }
            n.setTag("Options",list);n.setTag("Details",details);return n;
        }
        String layout(){return "LEFT: "+label(left)+" | RIGHT: "+label(right);}
        String label(int i){return options.get(i).eye==null?"EMPTY":options.get(i).eye.label();}
        void start(){
            String problem=valid(true);if(problem!=null||!identitiesValid()){close(this,problem!=null?problem:"Surgery cancelled: eye inventory changed.");return;}
            channel=true;finish=surgeon.world.getTotalWorldTime()+OcularPolicy.CHANNEL_TICKS;
            surgeonStart=surgeon.getPositionVector();patientStart=patient.getPositionVector();surgeonHealth=surgeon.getHealth();patientHealth=patient.getHealth();
            OcularAbilities.cleanup(patient);
            tell(surgeon,"Surgery started — hold still for 5 seconds.");
            if(patient!=surgeon)tell(patient,"Surgery started — hold still. Move to cancel.");
        }
        void complete(){
            String problem=valid(true);if(problem!=null||!identitiesValid()){close(this,problem!=null?problem:"Surgery cancelled: an eye was moved or already used.");return;}
            OcularState.Eye a=options.get(left).eye,b=options.get(right).eye;
            if(!OcularState.distinct(a,b)){close(this,"The same physical eye cannot occupy both sockets.");return;}
            // Inventory preparation is pure. No chakra, item or socket is changed on failed capacity validation.
            List<Integer> medicSlots=new ArrayList<>(),patientSlots=new ArrayList<>();
            List<ItemStack> medicReturns=new ArrayList<>(),patientReturns=new ArrayList<>();
            List<OcularState.Eye> splitRemainders=new ArrayList<>();
            Set<String> consumed=new HashSet<>();
            for(int i:new int[]{left,right}){
                Option o=options.get(i);if(o.slot<0||!consumed.add(o.source.getUniqueID()+":"+o.slot))continue;
                (o.source==patient?patientSlots:medicSlots).add(o.slot);
                if(o.pair)for(int side=0;side<2;side++){
                    OcularState.Eye remainder=OcularState.original(o.snapshot,o.eye.donor,side);
                    if(!OcularPolicy.same(remainder,a)&&!OcularPolicy.same(remainder,b)&&!ownsBefore(remainder.id)){
                        splitRemainders.add(remainder);(o.source==patient?patientReturns:medicReturns).add(ItemOcularGear.jar(remainder));
                    }
                }
            }
            List<OcularState.Eye> removed=new ArrayList<>();
            for(OcularState.Eye eye:before.eyes)if(eye!=null&&!OcularPolicy.same(eye,a)&&!OcularPolicy.same(eye,b)){
                removed.add(eye);medicReturns.add(ItemOcularGear.jar(eye));
            }
            InventoryPlan inventory=prepareInventories(surgeon.inventory.mainInventory,patient.inventory.mainInventory,medicSlots,patientSlots,medicReturns,patientReturns,surgeon==patient);
            if(inventory==null){close(this,"Make inventory space for removed eyes and unused halves; nothing consumed.");return;}
            if(!Chakra.pathway(surgeon).consume(OcularPolicy.CHAKRA_COST)){close(this,"Surgery cancelled: not enough chakra.");return;}
            OcularRegistry registry=OcularRegistry.get(surgeon.world);
            for(OcularState.Eye e:removed)registry.jar(e.id);
            for(OcularState.Eye e:splitRemainders)registry.jar(e.id);
            OcularState after=OcularState.read(before.write());after.eyes[0]=a;after.eyes[1]=b;
            // Technique allocation follows the chosen socket, never an inventory-side label.
            if(a!=null)a.donorSide=0;
            if(b!=null)b.donorSide=1;
            rememberNative(after,a,patient.getUniqueID());rememberNative(after,b,patient.getUniqueID());
            boolean eternal=OcularEvolution.evolve(after,patient.getUniqueID(),"Uchiha".equalsIgnoreCase(PlayerStats.getClan(patient)),registry);
            for(int i=0;i<2;i++)if(after.eyes[i]!=null){
                OcularState.Eye eye=after.eyes[i];eye.covered=false;eye.active=eye.sharingan()||eye.rinnegan();
                registry.install(eye.id,patient.getUniqueID(),i);
            }
            putInventory(surgeon,inventory.surgeon);if(patient!=surgeon)putInventory(patient,inventory.patient);
            // The legacy pair is now the two serialized physical eyes, not a second transferable pair.
            patient.setItemStackToSlot(EntityEquipmentSlot.HEAD,ItemStack.EMPTY);
            after.recoveringUntil=patient.world.getTotalWorldTime()+OcularPolicy.RECOVERY_TICKS;
            OcularSystem.save(patient,after);
            surgeon.inventoryContainer.detectAndSendChanges();patient.inventoryContainer.detectAndSendChanges();
            SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:windecho"));
            if(sound!=null)patient.world.playSound(null,patient.posX,patient.posY,patient.posZ,sound,SoundCategory.PLAYERS,0.5f,1.4f);
            close(this,eternal?"Eternal Mangekyo awakened. Recovery: 10s. G: left eye / H: right eye.":"Surgery complete. Recovery: 10s. G: left eye / H: right eye.");
        }
    }
    private static void rememberNative(OcularState state,OcularState.Eye eye,UUID patient){
        if(eye!=null&&eye.donor.equals(patient)&&eye.mangekyo())state.nativeEyes[eye.donorSide]=eye.stack.copy();
    }
    /** Display-only, server-derived details; choices still contain only indices and the session UUID. */
    static NBTTagCompound describeOption(OcularState.Eye eye,int slot,UUID patient,boolean nativeUchiha){
        NBTTagCompound n=new NBTTagCompound();
        n.setString("Name",eye==null?"Empty socket":eye.stack.getDisplayName());
        n.setString("Source",eye==null?"empty":slot<0?"installed":"inventory");
        n.setInteger("InventorySlot",slot);n.setInteger("DonorSide",eye==null?-1:eye.donorSide);
        n.setBoolean("Foreign",eye!=null&&!eye.donor.equals(patient));
        n.setBoolean("LockedActive",eye!=null&&!OcularPolicy.canDeactivate(eye.sharingan(),nativeUchiha));
        n.setDouble("Upkeep",OcularPolicy.upkeep(eye,patient));
        String kind="empty",skill="none",texture="";
        if(eye!=null){
            if(eye.rinnegan()){kind="rinnegan";skill="rinnegan";texture="rinnegan";}
            else if(eye.byakugan()){kind="byakugan";skill="vision";texture="byakugan";}
            else if(eye.sharingan()){
                kind="sharingan";skill="perception";texture="sharingan";
                if(eye.stack.getItem()==ItemSharinganTomoe1.helmet)texture="sharingan_1_tomoe";
                else if(eye.stack.getItem()==ItemSharinganTomoe2.helmet)texture="sharingan_2_tomoe";
                else if(eye.stack.getItem()==ItemSharinganTomoe3.helmet){texture="sharingan_3_tomoe";skill="copy";}
                else if(eye.mangekyo()){
                    kind=eye.eternal()?"ems":"mangekyo";
                    if(eye.eternal())texture=eye.family()==ItemSharingan.Type.MADARA?"mangekyosharingan_madara_eternal":"mangekyosharingan_eterna";
                    else texture=eye.family()==ItemSharingan.Type.MADARA?"mangekyosharingan_madara"
                        :eye.family()==ItemSharingan.Type.KAMUI?"mangekyosharingan_obito"
                        :eye.family()==ItemSharingan.Type.AMATERASU?"mangekyosharingan_sasuke":"sharingan";
                    skill=eye.family()==ItemSharingan.Type.MADARA?"temporal":eye.family()==ItemSharingan.Type.KAMUI?(eye.donorSide==0?"kamui_ranged":"kamui_self")
                        :eye.family()==ItemSharingan.Type.AMATERASU?(eye.donorSide==0?"amaterasu":"flame_control"):"mangekyo";
                }
            }else{
                kind="normal";skill="sight";ItemNormalEyes.EyeColor color=ItemNormalEyes.getEyeColor(eye.stack);
                texture=color==null?"normal_eyes":color.registryName;
            }
            boolean blinded=ItemSharingan.isBlinded(eye.stack);n.setBoolean("Blinded",blinded);if(blinded)skill="none";
        }
        n.setString("Kind",kind);n.setString("Skill",skill);
        n.setString("Texture",texture.isEmpty()?"":"narutomod:textures/blocks/"+texture+".png");
        return n;
    }
    static final class InventoryPlan {
        final List<ItemStack> surgeon,patient;
        InventoryPlan(List<ItemStack> a,List<ItemStack> b){surgeon=a;patient=b;}
    }
    static InventoryPlan prepareInventories(List<ItemStack> surgeon,List<ItemStack> patient,List<Integer> consumed,List<ItemStack> removed,boolean self){
        return prepareInventories(surgeon,patient,consumed,Collections.emptyList(),Collections.emptyList(),removed,self);
    }
    static InventoryPlan prepareInventories(List<ItemStack> surgeon,List<ItemStack> patient,List<Integer> medicSlots,List<Integer> patientSlots,List<ItemStack> medicReturns,List<ItemStack> patientReturns,boolean self){
        List<ItemStack> a=copyInventory(surgeon),b=self?a:copyInventory(patient);
        if(!removeInputs(a,medicSlots)||!removeInputs(b,patientSlots))return null;
        for(ItemStack jar:medicReturns)if(!insert(a,jar.copy()))return null;
        for(ItemStack jar:patientReturns)if(!insert(b,jar.copy()))return null;
        return new InventoryPlan(a,b);
    }
    private static boolean removeInputs(List<ItemStack> inventory,List<Integer> slots){Set<Integer> seen=new HashSet<>();for(int slot:slots){if(slot<0||slot>=inventory.size()||!seen.add(slot)||inventory.get(slot).getCount()!=1)return false;inventory.set(slot,ItemStack.EMPTY);}return true;}
    private static List<ItemStack> copyInventory(List<ItemStack> inventory){List<ItemStack> copy=new ArrayList<>();for(ItemStack s:inventory)copy.add(s.copy());return copy;}
    static boolean insert(List<ItemStack> inventory,ItemStack eye){for(int i=0;i<inventory.size();i++)if(inventory.get(i).isEmpty()){inventory.set(i,eye);return true;}return false;}
    private static void putInventory(EntityPlayer p,List<ItemStack> copy){for(int i=0;i<copy.size();i++)p.inventory.mainInventory.set(i,copy.get(i));p.inventory.markDirty();}
    private static void close(Session s,String reason){SESSIONS.remove(s.id);if(reason!=null){tell(s.surgeon,reason);if(s.patient!=s.surgeon)tell(s.patient,reason);}}
    private static void choose(EntityPlayerMP p,ChoiceMessage m){
        Session s=SESSIONS.get(m.id);if(s==null||s.surgeon!=p)return;
        if(m.cancel){close(s,"Surgery cancelled; no changes made.");return;}
        if(s.proposed||!OcularPolicy.validLayout(m.left,m.right,s.options.size()))return;
        s.left=m.left;s.right=m.right;
        OcularState.Eye a=s.options.get(s.left).eye,b=s.options.get(s.right).eye;
        if(!OcularState.distinct(a,b)||!OcularPolicy.changed(s.before,a,b)){close(s,"No valid socket changes selected.");return;}
        s.proposed=true;String problem=s.valid(true);
        if(problem!=null||!s.identitiesValid()){close(s,problem!=null?problem:"Eye inventory changed; reopen the surgery menu.");return;}
        if(s.patient==s.surgeon)s.start();
        else {
            tell(s.surgeon,"Waiting for "+s.patient.getName()+" to approve this exact layout.");
            NBTTagCompound menu=s.menu();menu.setBoolean("Consent",true);menu.setString("Surgeon",s.surgeon.getName());
            NarutomodMod.PACKET_HANDLER.sendTo(new OpenMessage(menu),s.patient);
        }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Session s:new ArrayList<>(SESSIONS.values())){
            String problem=s.valid(true);if(problem!=null){close(s,problem);continue;}
            if(!s.channel)continue;
            if(s.surgeon.world.getTotalWorldTime()>=s.finish){s.complete();continue;}
            if(s.surgeon.ticksExisted%5==0){
                ((WorldServer)s.patient.world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,s.patient.posX,s.patient.posY+s.patient.getEyeHeight(),s.patient.posZ,3,0.2,0.08,0.2,0);
                if(s.surgeon.ticksExisted%20==0){String progress="Ocular surgery: "+((s.finish-s.surgeon.world.getTotalWorldTime()+19)/20)+"s — hold still";tell(s.surgeon,progress);if(s.patient!=s.surgeon)tell(s.patient,progress);}
            }
        }
    }
    public static final class ConsentCommand extends CommandBase {
        @Override public String getName(){return "eyesurgery";}
        @Override public String getUsage(ICommandSender sender){return "/eyesurgery <accept|decline> <session> | family <player|UUID> <family-id|none> (operator)";}
        @Override public int getRequiredPermissionLevel(){return 0;}
        @Override public void execute(MinecraftServer server,ICommandSender sender,String[] args)throws CommandException{
            if(args.length==3&&"family".equals(args[0])){
                if(!sender.canUseCommand(2,"eyesurgery"))throw new CommandException("commands.generic.permission");
                UUID player;try{player=UUID.fromString(args[1]);}catch(IllegalArgumentException notId){player=getPlayer(server,sender,args[1]).getUniqueID();}
                if(!args[2].matches("[a-zA-Z0-9_-]{1,40}"))throw new CommandException("Use a short family ID: letters, numbers, underscore or hyphen.");
                OcularRegistry.get(server.getWorld(0)).family(player,"none".equalsIgnoreCase(args[2])?"":args[2].toLowerCase(java.util.Locale.ROOT));
                sender.sendMessage(new TextComponentString("RP blood-family record updated."));return;
            }
            EntityPlayerMP p=getCommandSenderAsPlayer(sender);
            if(args.length!=2)throw new WrongUsageException(getUsage(sender));
            UUID id;try{id=UUID.fromString(args[1]);}catch(IllegalArgumentException bad){throw new CommandException("Invalid surgery session.");}
            Session s=SESSIONS.get(id);if(s==null||s.patient!=p)throw new CommandException("No pending surgery for you with that session.");
            if("decline".equals(args[0])){close(s,"Patient declined/cancelled surgery; no changes made.");return;}
            if(!"accept".equals(args[0]))throw new WrongUsageException(getUsage(sender));
            if(s.proposed&&!s.channel)s.start();
        }
    }
    private static void consent(EntityPlayerMP p,UUID id,boolean accept){Session s=SESSIONS.get(id);if(s==null||s.patient!=p||!s.proposed)return;if(!accept)close(s,"Patient cancelled surgery; no changes made.");else if(!s.channel)s.start();}
    public static final class ConsentMessage implements IMessage {
        UUID id=new UUID(0,0);boolean accept;public ConsentMessage(){}public ConsentMessage(UUID session,boolean yes){id=session;accept=yes;}
        @Override public void toBytes(ByteBuf b){b.writeLong(id.getMostSignificantBits());b.writeLong(id.getLeastSignificantBits());b.writeBoolean(accept);}
        @Override public void fromBytes(ByteBuf b){id=new UUID(b.readLong(),b.readLong());accept=b.readBoolean();}
        public static final class Handler implements IMessageHandler<ConsentMessage,IMessage>{public IMessage onMessage(ConsentMessage m,MessageContext c){EntityPlayerMP p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->consent(p,m.id,m.accept));return null;}}
    }
    public static final class OpenMessage implements IMessage {
        NBTTagCompound data=new NBTTagCompound();public OpenMessage(){}public OpenMessage(NBTTagCompound n){data=n;}
        @Override public void toBytes(ByteBuf b){ByteBufUtils.writeTag(b,data);}
        @Override public void fromBytes(ByteBuf b){data=ByteBufUtils.readTag(b);}
        public static final class Handler implements IMessageHandler<OpenMessage,IMessage>{
            @Override @SideOnly(Side.CLIENT)public IMessage onMessage(OpenMessage m,MessageContext c){
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(()->{
                    if(m.data!=null)net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new net.narutomod.client.GuiOcularSurgery(m.data));
                });return null;
            }
        }
    }
    public static final class ChoiceMessage implements IMessage {
        UUID id=new UUID(0,0);int left,right;boolean cancel;
        public ChoiceMessage(){}public ChoiceMessage(UUID session,int a,int b,boolean cancelled){id=session;left=a;right=b;cancel=cancelled;}
        @Override public void toBytes(ByteBuf b){b.writeLong(id.getMostSignificantBits());b.writeLong(id.getLeastSignificantBits());b.writeInt(left);b.writeInt(right);b.writeBoolean(cancel);}
        @Override public void fromBytes(ByteBuf b){id=new UUID(b.readLong(),b.readLong());left=b.readInt();right=b.readInt();cancel=b.readBoolean();}
        public static final class Handler implements IMessageHandler<ChoiceMessage,IMessage>{
            @Override public IMessage onMessage(ChoiceMessage m,MessageContext c){EntityPlayerMP p=c.getServerHandler().player;p.getServerWorld().addScheduledTask(()->choose(p,m));return null;}
        }
    }
}
