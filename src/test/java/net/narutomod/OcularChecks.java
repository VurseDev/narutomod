package net.narutomod;

import java.lang.reflect.Field;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import io.netty.buffer.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.narutomod.item.*;
import net.narutomod.procedure.ProcedureUtils;
import sun.misc.Unsafe;

/** Headless regression executable: actual XP methods, NBT, identity ledger, inventory planning and wire codecs. */
public final class OcularChecks {
    private static int checks;
    private static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    private static Unsafe unsafe()throws Exception{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);return (Unsafe)f.get(null);}
    private static void holder(Class<?> type,String name,Item value)throws Exception{
        Class.forName(type.getName(),true,type.getClassLoader()); // Match Forge: initialize before injecting @ObjectHolder fields.
        Field f=type.getDeclaredField(name);Unsafe u=unsafe();u.putObject(u.staticFieldBase(f),u.staticFieldOffset(f),value);
    }
    private static void register(Item... items){
        // Bootstrap locks the vanilla registry facade. Headless fixture registration must use Forge's registry.
        ForgeRegistry<Item> registry=(ForgeRegistry<Item>)ForgeRegistries.ITEMS;boolean frozen=registry.isLocked();
        if(frozen)registry.unfreeze();try{registry.registerAll(items);}finally{if(frozen)registry.freeze();}
    }
    private static class Player extends EntityPlayer {
        UUID id;Player(){super(null,null);}
        @Override public UUID getUniqueID(){return id;}
        @Override public String getName(){return "OcularTest";}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
    }
    private static Player player(UUID id)throws Exception{Player p=(Player)unsafe().allocateInstance(Player.class);p.id=id;return p;}
    public static void main(String[] args)throws Exception{
        for(double xp:new double[]{-1,0,749,1875,2999,2999.9,Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY})check(!OcularPolicy.qualified(xp),"unqualified XP "+xp);
        check(OcularPolicy.qualified(3000)&&OcularPolicy.qualified(3001),"exact 3,000 XP gate");
        for(String key:Arrays.asList(OcularState.KEY,"OcularCopyCooldown","OcularByakuganSent"))check(OcularPolicy.serverOwnedTag(key),"client cannot write/delete physical-eye state "+key);
        check(!OcularPolicy.serverOwnedTag("JutsuKey1Pressed"),"unrelated legacy input tags unchanged");
        for(int size=1;size<40;size++)for(int a=-1;a<=size;a++)for(int b=-1;b<=size;b++){
            boolean expected=a>=0&&b>=0&&a<size&&b<size&&(a==0||a!=b);
            check(OcularPolicy.validLayout(a,b,size)==expected,"bounded unique indices, empty may fill both");
        }
        check(!OcularPolicy.canDeactivate(true,false),"foreign Sharingan cannot turn off");
        check(OcularPolicy.canDeactivate(true,true)&&OcularPolicy.canDeactivate(false,false),"Uchiha Sharingan and Byakugan may toggle");
        check(!OcularPolicy.abilitiesAvailable(101,100,false),"recovery blocks powers and their upkeep");
        check(OcularPolicy.abilitiesAvailable(100,100,false),"powers become available at exact recovery boundary");
        check(!OcularPolicy.abilitiesAvailable(0,100,true)&&!OcularPolicy.abilitiesAvailable(101,100,true),"surgery suppresses powers, overlay and upkeep regardless of recovery");
        Bootstrap.register();
        consentAndPacketAuthorityChecks();
        Item basic=new ItemSharingan.Base(ItemArmor.ArmorMaterial.LEATHER).setRegistryName("narutomod:ocular_test_sharingan").setUnlocalizedName("ocular_test_sharingan");
        Item advanced=new ItemSharingan.Base(ItemArmor.ArmorMaterial.LEATHER){
            @Override public boolean isMangekyo(){return true;}
            @Override public ItemSharingan.Type getSubType(){return ItemSharingan.Type.AMATERASU;}
        }.setRegistryName("narutomod:ocular_test_ms");
        Item eternal=new ItemSharingan.Base(ItemArmor.ArmorMaterial.LEATHER){
            @Override public boolean isMangekyo(){return true;}
            @Override public boolean isEternal(){return true;}
            @Override public ItemSharingan.Type getSubType(){return ItemSharingan.Type.AMATERASU;}
        }.setRegistryName("narutomod:ocular_test_ems");
        Item rinnegan=new ItemRinnegan.Base(ItemArmor.ArmorMaterial.LEATHER).setRegistryName("narutomod:ocular_test_rinnegan");
        Item jar=new Item().setRegistryName("narutomod:preserved_eye").setMaxStackSize(1);
        register(basic,advanced,eternal,rinnegan,jar);
        holder(ItemOcularGear.class,"EYE",jar);holder(ItemSharinganTomoe3.class,"helmet",basic);
        holder(ItemMangekyoSharinganEternal.class,"helmet",eternal);holder(ItemRinnegan.class,"helmet",rinnegan);
        check(OcularState.supported(new ItemStack(basic)),"basic eye accepted");
        check(!OcularState.supported(new ItemStack(basic,2)),"physical eye payload cannot contain a stack of organs");
        check(OcularState.supported(new ItemStack(advanced))&&OcularState.supported(new ItemStack(rinnegan)),"Mangekyo and Rinnegan specimens accepted");
        check(!OcularState.supported(new ItemStack(Items.STICK))&&!OcularState.supported(ItemStack.EMPTY),"unrelated/empty items rejected");
        UUID donor=UUID.randomUUID(),recipient=UUID.randomUUID();ItemStack source=new ItemStack(basic);
        source.setTagCompound(new NBTTagCompound());source.getTagCompound().setInteger("SharinganCopyXp",321);
        net.narutomod.procedure.ProcedureUtils.setOriginalOwner(source,donor);
        ItemStack mutableMs=new ItemStack(advanced);ProcedureUtils.setOriginalOwner(mutableMs,donor);
        mutableMs.getTagCompound().setLong("CustomFireJutsuCooldown0",120L);
        ItemStack tickingMs=mutableMs.copy();tickingMs.getTagCompound().setLong("CustomFireJutsuCooldown0",480L);
        check(MedicalSurgery.sameSurgeryStack(mutableMs,tickingMs),"Mangekyo cooldown ticks do not cancel a surgery session");
        ItemStack changedFamily=mutableMs.copy();changedFamily.getTagCompound().setString("OcularAbilityFamily","KAMUI");
        check(!MedicalSurgery.sameSurgeryStack(mutableMs,changedFamily),"a changed Mangekyo ability family invalidates the snapshot");
        check(!MedicalSurgery.sameSurgeryStack(mutableMs,new ItemStack(eternal)),"a changed Mangekyo stage invalidates the snapshot");
        OcularState.Eye left=OcularState.original(source,donor,0),right=OcularState.original(source,donor,1);
        check(!left.id.equals(right.id),"two unique physical eyes");
        check(left.id.equals(OcularState.original(new ItemStack(advanced),donor,0).id),"legacy stage tokens cannot mint new physical identities");
        check(!left.id.equals(OcularState.original(source,recipient,0).id),"different donors have different identities");
        check(OcularState.distinct(left,right)&&!OcularState.distinct(left,left),"duplicate eye rejected");
        OcularState s=new OcularState();s.eyes[0]=left;s.eyes[1]=right;s.selected=1;s.revision=8;s.normalVariant="BLACK";s.recoveringUntil=200;s.toggleUntil=10;
        ItemStack nativeMs=new ItemStack(advanced);net.narutomod.procedure.ProcedureUtils.setOriginalOwner(nativeMs,recipient);
        s.nativeEyes[0]=nativeMs.copy();s.nativeEyes[1]=nativeMs.copy();
        right.covered=true;left.active=true;
        OcularState copy=OcularState.read(s.write());
        check(copy.selected==1&&copy.revision==8&&copy.recoveringUntil==200&&copy.toggleUntil==10&&copy.normalVariant.equals("BLACK"),"complete state roundtrip");
        check(copy.eyes[0].id.equals(left.id)&&copy.eyes[1].donorSide==1&&copy.eyes[1].covered,"physical side and cover persist");
        check(copy.eyes[0].stack.getTagCompound().getInteger("SharinganCopyXp")==321&&copy.eyes[0].donor.equals(donor),"donor XP and ownership persist");
        check(ItemSharingan.isMangekyo(copy.nativeEyes[0])&&recipient.equals(ProcedureUtils.getOwnerId(copy.nativeEyes[1])),"recipient Mangekyo progression snapshots survive serialization");
        copy.eyes[0].stack.getTagCompound().setInteger("SharinganCopyXp",123);
        check(left.stack.getTagCompound().getInteger("SharinganCopyXp")==321,"deserialization does not alias original item NBT");
        check(copy.hasSight(),"one uncovered eye grants sight");copy.eyes[0].covered=true;check(!copy.hasSight(),"both covered means no sight");
        copy.eyes[0]=null;copy.eyes[1]=null;check(!copy.hasSight(),"no eyes means no sight");
        check(!OcularPolicy.changed(s,left,right)&&OcularPolicy.changed(s,right,left)&&OcularPolicy.changed(s,null,right),"reject no-op, allow swap/extraction");
        ItemStack specimen=ItemOcularGear.jar(left);OcularState.Eye restored=ItemOcularGear.eye(specimen);
        check(restored!=null&&restored.id.equals(left.id)&&restored.donorSide==0,"preserved eye roundtrip");
        check(specimen.getItem()==left.stack.getItem(),"extraction returns the actual eye item");
        ItemStack otherEye=ItemOcularGear.jar(right);
        check(otherEye.getItem()==specimen.getItem()&&!ItemOcularGear.eye(otherEye).id.equals(restored.id),"two actual eyes retain distinct identities");
        check(ItemOcularGear.eye(new ItemStack(jar))==null,"blank /give jar cannot implant");
        NBTTagCompound corrupt=left.write();corrupt.removeTag("IdMost");check(OcularState.Eye.read(corrupt)==null,"missing identity rejected");
        corrupt=left.write();corrupt.setInteger("DonorSide",2);check(OcularState.Eye.read(corrupt)==null,"invalid physical donor side rejected");
        corrupt=left.write();corrupt.setTag("Stack",new ItemStack(advanced).writeToNBT(new NBTTagCompound()));check(OcularState.Eye.read(corrupt)!=null,"advanced NBT payload accepted without changing physical identity");
        corrupt=left.write();corrupt.setTag("Stack",new ItemStack(basic,2).writeToNBT(new NBTTagCompound()));check(OcularState.Eye.read(corrupt)==null,"stacked physical-eye NBT payload rejected");
        OcularRegistry ledger=new OcularRegistry();check(ledger.unclaimed(left.id)&&!ledger.inJar(left.id),"unclaimed is not a valid jar");
        ledger.install(left.id,recipient,0);check(ledger.installed(left.id,recipient,0)&&!ledger.installed(left.id,recipient,1)&&!ledger.unclaimed(left.id),"registry socket ownership exact");
        ledger.jar(left.id);check(ledger.inJar(left.id)&&!ledger.installed(left.id,recipient,0),"extraction transitions into jar");
        ledger.install(left.id,donor,1);check(!ledger.inJar(left.id)&&ledger.installed(left.id,donor,1),"a copied jar is invalid after implantation");
        OcularRegistry reloaded=new OcularRegistry();reloaded.readFromNBT(ledger.writeToNBT(new NBTTagCompound()).copy());
        check(reloaded.installed(left.id,donor,1),"registry survives save/reload");
        evolutionChecks(advanced,eternal);
        inventoryChecks(specimen);
        menuChecks(left,donor,recipient);
        xpChecks(donor,recipient);
        packetChecks();
        resources();
        System.out.println("Ocular surgery checks passed: "+checks+" (headless; live multiplayer/visual QA still required)");
    }
    private static void menuChecks(OcularState.Eye eye,UUID owner,UUID recipient)throws Exception{
        NBTTagCompound empty=MedicalSurgery.describeOption(null,-1,recipient,false);
        check(empty.getString("Kind").equals("empty")&&empty.getString("Source").equals("empty")&&empty.getString("Skill").equals("none"),"empty socket metadata is explicit");
        check(empty.getInteger("DonorSide")==-1&&!empty.getBoolean("Foreign")&&!empty.getBoolean("LockedActive")&&empty.getDouble("Upkeep")==0d&&empty.getString("Texture").isEmpty(),"empty socket never has donor or upkeep");
        NBTTagCompound own=MedicalSurgery.describeOption(eye,-1,owner,true);
        check(own.getString("Source").equals("installed")&&!own.getBoolean("Foreign")&&!own.getBoolean("LockedActive"),"installed native Sharingan metadata");
        check(own.getDouble("Upkeep")==2.5d&&OcularPolicy.upkeep(eye,owner)==2.5d,"UI and native Sharingan upkeep match");
        check(own.getString("Skill").equals("copy")&&own.getString("Texture").equals("narutomod:textures/blocks/sharingan_3_tomoe.png"),"three-tomoe copying preview is exact");
        NBTTagCompound foreign=MedicalSurgery.describeOption(eye,7,recipient,false);
        check(foreign.getString("Source").equals("inventory")&&foreign.getInteger("InventorySlot")==7&&foreign.getInteger("DonorSide")==eye.donorSide&&foreign.getBoolean("Foreign")&&foreign.getBoolean("LockedActive"),"transplanted Sharingan preserves provenance and non-Uchiha lock");
        check(foreign.getDouble("Upkeep")==10d&&OcularPolicy.upkeep(eye,recipient)==10d,"UI and transplanted Sharingan upkeep match");
        check(!MedicalSurgery.describeOption(eye,7,recipient,true).getBoolean("LockedActive"),"Uchiha may deactivate foreign Sharingan too");
        OcularState.Eye blind=OcularState.Eye.read(eye.write());blind.stack.getTagCompound().setBoolean("sharingan_blinded",true);
        NBTTagCompound unusable=MedicalSurgery.describeOption(blind,7,recipient,false);
        check(unusable.getBoolean("Blinded")&&unusable.getString("Skill").equals("none")&&unusable.getDouble("Upkeep")==0d,"blind eyes cannot advertise copying skills or charge upkeep");
        Item byakugan=new Item().setRegistryName("narutomod:ocular_test_byakugan");
        register(byakugan);holder(ItemByakugan.class,"helmet",byakugan);
        OcularState.Eye vision=OcularState.original(new ItemStack(byakugan),owner,1);
        NBTTagCompound byakuganInfo=MedicalSurgery.describeOption(vision,4,recipient,false);
        check(byakuganInfo.getString("Kind").equals("byakugan")&&byakuganInfo.getString("Skill").equals("vision")&&!byakuganInfo.getBoolean("LockedActive"),"Byakugan advertises toggleable vision only");
        check(OcularPolicy.upkeep(vision,owner)==10d&&byakuganInfo.getDouble("Upkeep")==20d,"native/transplanted Byakugan upkeep matches menu");
        for(NBTTagCompound detail:Arrays.asList(own,foreign,byakuganInfo))
            check(Files.exists(Paths.get("src/main/resources/assets/narutomod").resolve(detail.getString("Texture").split(":")[1])),"menu eye texture exists");
    }
    private static OcularState transplantState(Item advanced,UUID recipient,UUID donor){
        ItemStack donated=new ItemStack(advanced);ProcedureUtils.setOriginalOwner(donated,donor);
        ItemStack nativeMs=new ItemStack(advanced);ProcedureUtils.setOriginalOwner(nativeMs,recipient);
        OcularState state=new OcularState();state.eyes[0]=OcularState.original(donated,donor,0);state.eyes[1]=OcularState.original(donated,donor,1);
        state.nativeEyes[0]=nativeMs.copy();state.nativeEyes[1]=nativeMs.copy();return state;
    }
    private static void evolutionChecks(Item advanced,Item eternal){
        UUID recipient=UUID.randomUUID(),donor=UUID.randomUUID();OcularRegistry registry=new OcularRegistry();
        OcularState state=transplantState(advanced,recipient,donor);
        check(!OcularEvolution.eligible(state,recipient,true,registry),"foreign Mangekyo pair does not become EMS without an RP blood-family record");
        registry.family(recipient,"uchiha-a");registry.family(donor,"uchiha-a");
        UUID left=state.eyes[0].id,right=state.eyes[1].id;
        check(OcularEvolution.eligible(state,recipient,true,registry)&&OcularEvolution.evolve(state,recipient,true,registry),"related paired donor Mangekyo plus native progression awakens EMS");
        check(state.eyes[0].stack.getItem()==eternal&&state.eyes[1].stack.getItem()==eternal&&state.eyes[0].eternal(),"both sockets receive the matching Eternal Mangekyo form");
        check(state.eyes[0].id.equals(left)&&state.eyes[1].id.equals(right),"EMS evolution preserves each donated physical eye identity");
        check("AMATERASU".equals(state.eyes[0].stack.getTagCompound().getString("OcularAbilityFamily")),"EMS keeps the recipient's ability family instead of granting unrelated powers");
        OcularState mixed=transplantState(advanced,recipient,donor);mixed.eyes[1]=OcularState.original(new ItemStack(advanced),UUID.randomUUID(),1);
        check(!OcularEvolution.eligible(mixed,recipient,true,registry),"mixed donors never form EMS");
        OcularState noProgress=transplantState(advanced,recipient,donor);noProgress.nativeEyes[1]=ItemStack.EMPTY;
        check(!OcularEvolution.eligible(noProgress,recipient,true,registry),"donor eyes cannot bypass the recipient's own Mangekyo progression");
        OcularRegistry saved=new OcularRegistry();saved.family(recipient,"uchiha-a");saved.family(donor,"uchiha-a");
        OcularRegistry loaded=new OcularRegistry();loaded.readFromNBT(saved.writeToNBT(new NBTTagCompound()));
        check(loaded.bloodRelated(recipient,donor),"RP blood-family links survive world save/reload");
    }
    private static void inventoryChecks(ItemStack eye){
        List<ItemStack> full=Arrays.asList(new ItemStack(Items.STICK,64),eye.copy());
        List<ItemStack> room=Arrays.asList(new ItemStack(Items.DIAMOND,8),ItemStack.EMPTY);
        List<ItemStack> removed=Collections.singletonList(eye);
        MedicalSurgery.InventoryPlan self=MedicalSurgery.prepareInventories(full,full,Collections.singletonList(1),removed,true);
        check(self!=null&&self.surgeon==self.patient,"self-surgery uses the same prepared inventory");
        check(self.surgeon.get(0).getCount()==64&&self.surgeon.get(1).getItem()==eye.getItem(),"consumed slot reused for removed eye");
        check(full.get(1).getItem()==eye.getItem(),"preparation never mutates original inventory");
        MedicalSurgery.InventoryPlan other=MedicalSurgery.prepareInventories(full,room,Collections.singletonList(1),removed,false);
        check(other!=null&&other.surgeon.get(1).isEmpty()&&other.patient.get(1).getItem()==eye.getItem(),"donor consumed from medic, removed eye returned to patient");
        check(other.patient.get(0).getCount()==8&&room.get(1).isEmpty(),"unrelated items / patient original preserved");
        check(MedicalSurgery.prepareInventories(full,full,Collections.singletonList(1),removed,false)==null,"full patient inventory rejects operation");
        check(full.get(1).getItem()==eye.getItem()&&full.get(0).getCount()==64,"failed preparation consumes nothing");
        for(List<Integer> bad:Arrays.asList(Arrays.asList(1,1),Arrays.asList(-1),Arrays.asList(2),Arrays.asList(0)))
            check(MedicalSurgery.prepareInventories(full,room,bad,removed,false)==null,"duplicate/out of range/stacked input rejected");
        check(MedicalSurgery.prepareInventories(full,room,Collections.singletonList(1),Arrays.asList(eye,eye),false)==null,"partial insertion fails without partial commit");
        check(room.get(1).isEmpty(),"failed second insertion rolls back whole plan");
        List<ItemStack> medic=Arrays.asList(eye.copy(),ItemStack.EMPTY),patient=Arrays.asList(eye.copy(),ItemStack.EMPTY);
        MedicalSurgery.InventoryPlan sourced=MedicalSurgery.prepareInventories(medic,patient,Collections.singletonList(0),Collections.singletonList(0),
            Collections.singletonList(new ItemStack(Items.DIAMOND)),Collections.singletonList(new ItemStack(Items.EMERALD)),false);
        check(sourced!=null&&sourced.surgeon.get(0).getItem()==Items.DIAMOND&&sourced.patient.get(0).getItem()==Items.EMERALD,
            "medic and patient specimens are consumed independently and each unused half returns to its owner");
        check(medic.get(0).getItem()==eye.getItem()&&patient.get(0).getItem()==eye.getItem(),"dual-source planning remains atomic and never mutates live inventories");
    }
    @SuppressWarnings("unchecked")
    private static void consentAndPacketAuthorityChecks()throws Exception{
        net.minecraft.entity.player.EntityPlayerMP surgeon=(net.minecraft.entity.player.EntityPlayerMP)unsafe().allocateInstance(net.minecraft.entity.player.EntityPlayerMP.class);
        net.minecraft.entity.player.EntityPlayerMP patient=(net.minecraft.entity.player.EntityPlayerMP)unsafe().allocateInstance(net.minecraft.entity.player.EntityPlayerMP.class);
        Class<?> type=Class.forName("net.narutomod.MedicalSurgery$Session");Object session=unsafe().allocateInstance(type);
        for(String name:Arrays.asList("surgeon","patient")){Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(session,name.equals("surgeon")?surgeon:patient);}
        Field channel=type.getDeclaredField("channel");channel.setAccessible(true);
        Field field=MedicalSurgery.class.getDeclaredField("SESSIONS");field.setAccessible(true);Map<UUID,Object> sessions=(Map<UUID,Object>)field.get(null);
        UUID key=UUID.randomUUID();sessions.put(key,session);
        try{
            check(MedicalSurgery.busy(patient)&&MedicalSurgery.busy(surgeon),"invitation reserves both menus");
            check(!MedicalSurgery.operating(patient)&&!MedicalSurgery.operating(surgeon),"unaccepted invitation cannot suppress anyone's powers");
            check(OcularPolicy.abilitiesAvailable(0,100,MedicalSurgery.operating(patient)),"patient can still use eye abilities before consent");
            channel.setBoolean(session,true);check(MedicalSurgery.operating(patient)&&MedicalSurgery.operating(surgeon),"accepted channel suppresses powers");
        }finally{sessions.remove(key);}
        check(!MedicalSurgery.busy(patient)&&!MedicalSurgery.operating(patient),"closing session releases lock and abilities");
        net.narutomod.procedure.ProcedureSync.EntityNBTTag.ServerHandler handler=new net.narutomod.procedure.ProcedureSync.EntityNBTTag.ServerHandler();
        for(String tag:Arrays.asList(OcularState.KEY,"OcularCopyCooldown","OcularByakuganSent")){
            net.narutomod.procedure.ProcedureSync.EntityNBTTag[] attacks={
                new net.narutomod.procedure.ProcedureSync.EntityNBTTag(patient,tag),
                new net.narutomod.procedure.ProcedureSync.EntityNBTTag(patient,tag,1),
                new net.narutomod.procedure.ProcedureSync.EntityNBTTag(patient,tag,true)};
            for(net.narutomod.procedure.ProcedureSync.EntityNBTTag attack:attacks)
                check(handler.onMessage(attack,null)==null,"protected write/delete rejected before world mutation: "+tag);
        }
    }
    private static void xpChecks(UUID owner,UUID other)throws Exception{
        ItemIryoJutsu.RangedItem item=new ItemIryoJutsu.RangedItem(ItemIryoJutsu.HEALING,ItemIryoJutsu.POISONMIST,ItemIryoJutsu.MEDMODE,ItemIryoJutsu.POWERMODE,ItemIryoJutsu.SURGERY);
        Player medic=player(owner),stranger=player(other);ItemStack stack=new ItemStack(item);item.setOwner(stack,medic);
        item.enableJutsu(stack,ItemIryoJutsu.HEALING,true);item.setCurrentJutsu(stack,ItemIryoJutsu.HEALING);
        item.addJutsuXp(stack,ItemIryoJutsu.HEALING,2999);
        check(item.getJutsuXp(stack,ItemIryoJutsu.HEALING)==2999&&!item.qualifiedForSurgery(stack,medic),"training reaches 2,999 but gate stays closed");
        item.addCurrentJutsuXp(stack,1);check(item.qualifiedForSurgery(stack,medic),"normal healing completion XP reaches exact 3,000");
        check(!item.qualifiedForSurgery(stack,stranger)&&item.getJutsuXp(stack,ItemIryoJutsu.HEALING)==3000,"borrowed Medical Jutsu fails without losing XP");
        item.addCurrentJutsuXp(stack,5);check(item.getJutsuXp(stack,ItemIryoJutsu.HEALING)==3000,"healing training cap bounded");
        check(Math.abs(item.getCurrentJutsuXpModifier(stack,medic)-1f/3f)<.0001,"healing speed/mastery retains original maximum");
        item.addJutsuXp(stack,ItemIryoJutsu.POISONMIST,99999);
        check(item.getJutsuXp(stack,ItemIryoJutsu.POISONMIST)==item.getRequiredXp(stack,ItemIryoJutsu.POISONMIST)*3,"unrelated medical XP cap unchanged");
        item.setIsAffinity(stack,true);check(item.qualifiedForSurgery(stack,medic),"affinity does not rescale the exact XP threshold");
        item.setCurrentJutsu(stack,ItemIryoJutsu.SURGERY);check(item.getCurrentJutsuXpModifier(stack,medic)==1f,"zero-XP menu technique avoids 0/0 mastery");
        check(!new ItemIryoJutsu.SurgeryJutsu().createJutsu(stack,medic,1f),"menu callback itself never awards XP or charges resources");
        check(item.getJutsuXp(stack,ItemIryoJutsu.HEALING)==3000,"opening a menu cannot spend training XP");
    }
    private static void packetChecks(){
        UUID id=UUID.randomUUID();
        for(boolean cancel:new boolean[]{false,true}){
            MedicalSurgery.ChoiceMessage original=new MedicalSurgery.ChoiceMessage(id,37,-1,cancel),read=new MedicalSurgery.ChoiceMessage();
            ByteBuf b=Unpooled.buffer();try{original.toBytes(b);check(b.readableBytes()==25,"fixed-size client choice payload");read.fromBytes(b);check(read.id.equals(id)&&read.left==37&&read.right==-1&&read.cancel==cancel&&b.readableBytes()==0,"choice roundtrip; validation remains server-side");}finally{b.release();}
        }
        for(boolean accept:new boolean[]{false,true}){
            MedicalSurgery.ConsentMessage original=new MedicalSurgery.ConsentMessage(id,accept),read=new MedicalSurgery.ConsentMessage();
            ByteBuf b=Unpooled.buffer();try{original.toBytes(b);check(b.readableBytes()==17,"fixed-size patient consent payload");read.fromBytes(b);
                Field idField=MedicalSurgery.ConsentMessage.class.getDeclaredField("id"),acceptField=MedicalSurgery.ConsentMessage.class.getDeclaredField("accept");
                idField.setAccessible(true);acceptField.setAccessible(true);
                check(id.equals(idField.get(read))&&accept==(Boolean)acceptField.get(read)&&b.readableBytes()==0,"patient consent roundtrip");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}finally{b.release();}
        }
        NBTTagCompound n=new NBTTagCompound();n.setUniqueId("Session",id);n.setString("Patient","Test");
        NBTTagList list=new NBTTagList();list.appendTag(new NBTTagString("Empty"));n.setTag("Options",list);
        NBTTagList details=new NBTTagList();details.appendTag(MedicalSurgery.describeOption(null,-1,id,false));n.setTag("Details",details);
        n.setUniqueId("PatientId",id);n.setBoolean("NativeUchiha",true);
        MedicalSurgery.OpenMessage original=new MedicalSurgery.OpenMessage(n),read=new MedicalSurgery.OpenMessage();
        ByteBuf b=Unpooled.buffer();try{original.toBytes(b);read.fromBytes(b);check(n.equals(read.data)&&b.readableBytes()==0,"server menu NBT wire roundtrip");}finally{b.release();}
    }
    private static void resources()throws Exception{
        Path root=Paths.get("src/main/resources/assets/narutomod");
        for(String name:Arrays.asList("ocular_sockets","preserved_eye")){
            JsonObject model=new JsonParser().parse(new String(Files.readAllBytes(root.resolve("models/item/"+name+".json")),StandardCharsets.UTF_8)).getAsJsonObject();
            for(Map.Entry<String,JsonElement> texture:model.getAsJsonObject("textures").entrySet())check(Files.exists(root.resolve("textures/"+texture.getValue().getAsString().split(":")[1]+".png")),"item texture exists");
            for(String lang:Arrays.asList("en_us","pt_br")){
                String text=new String(Files.readAllBytes(root.resolve("lang/"+lang+".lang")),StandardCharsets.UTF_8);
                check(text.contains("item."+name+".name="),"localized item");check(text.contains("jutsu.narutomod.ocular_surgery="),"localized technique");
            }
        }
    }
}
