package net.narutomod;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.entity.EntitySusanooMadara;
import net.narutomod.item.ItemByakugan;
import net.narutomod.item.ItemSharingan;

/** Server-authoritative, position-only personal reversal. The world and combat ledger never rewind. */
@ElementsNarutomodMod.ModElement.Tag
@Mod.EventBusSubscriber(modid=NarutomodMod.MODID)
public final class MadaraTemporalController extends ElementsNarutomodMod.ModElement {
    private static final String COOLDOWN="NarutomodMadaraTemporalCooldownMs";
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();

    public MadaraTemporalController(ElementsNarutomodMod elements) { super(elements,1106); }
    @Override public void preInit(FMLPreInitializationEvent event) {
        elements.addNetworkMessage(EffectMessage.Handler.class,EffectMessage.class,Side.CLIENT);
    }
    @Override @SideOnly(Side.CLIENT) public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new net.narutomod.client.ClientTemporalEffects());
    }

    private static final class Session {
        final EntityPlayerMP player;
        final World world;
        final ItemStack eyes;
        final EntitySusanooMadara mount;
        final int stage;
        final long deadline;
        final boolean eternal;
        final double range, costMultiplier;
        final TemporalHistory history, mountHistory;
        Session(EntityPlayerMP player,ItemStack eyes,EntitySusanooMadara mount) {
            this.player=player;world=player.world;this.eyes=eyes;this.mount=mount;
            stage=mount==null?-1:mount.getStage();
            ItemSharingan.Base eye=(ItemSharingan.Base)eyes.getItem();eternal=eye.isEternal();
            costMultiplier=eye.isOwner(eyes,player) || player.isCreative()?1:3;
            range=eternal?MadaraTemporalSettings.EMS_RETURN_DISTANCE:MadaraTemporalSettings.MS_RETURN_DISTANCE;
            int seconds=eternal?MadaraTemporalSettings.EMS_HISTORY_SECONDS:MadaraTemporalSettings.MS_HISTORY_SECONDS;
            history=new TemporalHistory(seconds*20);mountHistory=new TemporalHistory(seconds*20);
            deadline=world.getTotalWorldTime()+20L*(eternal?MadaraTemporalSettings.EMS_ANCHOR_SECONDS:MadaraTemporalSettings.MS_ANCHOR_SECONDS);
            record();
        }
        void record() {
            long tick=world.getTotalWorldTime();history.add(frame(player,tick));
            if(mount!=null)mountHistory.add(frame(mount,tick));
        }
    }

    /** Called on the server by Madara eye ability key 1: first press records; next press reverses. */
    public static void toggle(EntityPlayer user) {
        if(!(user instanceof EntityPlayerMP) || user.world.isRemote)return;
        EntityPlayerMP player=(EntityPlayerMP)user;
        ItemStack eyes=activeEyes(player);
        if(eyes.isEmpty()) { cancel(player,false);status(player,"eyes");return; }
        if(!GenjutsuSession.canUse(player,null))return;
        if(ItemByakugan.getBlockedTenketsuStacks(player)>=8) {
            status(player,"tenketsu");return;
        }
        if(!player.isEntityAlive() || player.isSpectator() || player.isPlayerSleeping())return;
        Session session=ACTIVE.get(player.getUniqueID());
        if(session!=null) { reverse(session);return; }
        long remaining=cooldownRemaining(player,System.currentTimeMillis());
        if(remaining>0) {status(player,"cooldown",(remaining+999)/1000);return;}
        Entity riding=player.getRidingEntity();
        EntitySusanooMadara mount=riding instanceof EntitySusanooMadara?(EntitySusanooMadara)riding:null;
        if(riding!=null && mount==null || mount!=null && (mount.getOwnerPlayer()!=player || mount.isTransitioning()
            || mount.getPassengers().size()!=1 || mount.isRiding())) {
            status(player,"mount");return;
        }
        if(player.isBeingRidden()) {status(player,"passengers");return;}
        boolean eternal=((ItemSharingan.Base)eyes.getItem()).isEternal();
        double multiplier=((ItemSharingan.Base)eyes.getItem()).isOwner(eyes,player) || player.isCreative()?1:3;
        double cost=(eternal?MadaraTemporalSettings.EMS_ANCHOR_CHAKRA:MadaraTemporalSettings.MS_ANCHOR_CHAKRA)*multiplier;
        if(!spend(player,cost))return;
        session=new Session(player,eyes,mount);ACTIVE.put(player.getUniqueID(),session);
        int cooldown=eternal?MadaraTemporalSettings.EMS_COOLDOWN_SECONDS:MadaraTemporalSettings.MS_COOLDOWN_SECONDS;
        // Player NBT, not eye-stack NBT: swapping eyes, dying or restarting cannot reset the timer.
        // Wall-clock expiry deliberately continues while offline and across world/dimension clocks.
        player.getEntityData().setLong(COOLDOWN,System.currentTimeMillis()+cooldown*1000L);
        ((ItemSharingan.Base)eyes.getItem()).addOcularStrain(eyes,player,8);
        OcularAbilities.persistResolved(player,eyes);
        SusanooCastController.eyeRelease(player,SusanooCastProfile.TEMPORAL);
        emit(session,0,Collections.singletonList(frame(player,player.world.getTotalWorldTime())));
        sound(player,"narutomod:sharingansfx",.8f,1f);
        status(player,"anchor",eternal?MadaraTemporalSettings.EMS_HISTORY_SECONDS:MadaraTemporalSettings.MS_HISTORY_SECONDS);
    }

    private static ItemStack activeEyes(EntityPlayer player) {
        ItemStack stack=OcularAbilities.resolve(player,"madara");
        // Existing eye items are usable by direct equipment as well as DojutsuControl activation.
        if(!(stack.getItem() instanceof ItemSharingan.Base))return ItemStack.EMPTY;
        ItemSharingan.Base eye=(ItemSharingan.Base)stack.getItem();
        if(eye.getSubType()!=ItemSharingan.Type.MADARA || !eye.isMangekyo() || ItemSharingan.isBlinded(stack))return ItemStack.EMPTY;
        if(!player.isCreative() && !eye.isOwner(stack,player) && (player.experienceLevel<10 || PlayerTracker.getBattleXp(player)<300))return ItemStack.EMPTY;
        return stack;
    }

    private static boolean valid(Session s) {
        EntityPlayerMP p=s.player;
        return p.world==s.world && !p.isDead && p.isEntityAlive() && !p.isSpectator() && !p.isPlayerSleeping()
            && p.getServer()!=null && p.getServer().getPlayerList().getPlayerByUUID(p.getUniqueID())==p
            && OcularAbilities.samePhysicalEye(activeEyes(p),s.eyes) && p.getRidingEntity()==s.mount && !p.isBeingRidden()
            && (s.mount==null || !s.mount.isDead && s.mount.getOwnerPlayer()==p && s.mount.getStage()==s.stage
                && !s.mount.isTransitioning() && s.mount.getPassengers().size()==1 && !s.mount.isRiding());
    }

    private static void reverse(Session s) {
        EntityPlayerMP p=s.player;
        if(!valid(s) || s.world.getTotalWorldTime()>=s.deadline) {cancel(p,true);return;}
        s.record();
        if(!s.history.usable(s.world.getTotalWorldTime(),s.deadline,s.range)) {
            status(p,"range",(int)s.range);return;
        }
        TemporalHistory.Frame dest=s.history.oldest();
        TemporalHistory.Frame mountDest=s.mount==null?null:s.mountHistory.oldest();
        if(!safe(p,dest,s.mount) || s.mount!=null && (mountDest==null || !safe(s.mount,mountDest,p)
            || s.mountHistory.newest().distanceSquared(mountDest)>s.range*s.range)) {
            status(p,"blocked");return;
        }
        double cost=(s.eternal?MadaraTemporalSettings.EMS_REVERSAL_CHAKRA:MadaraTemporalSettings.MS_REVERSAL_CHAKRA)*s.costMultiplier;
        if(!spend(p,cost))return;
        List<TemporalHistory.Frame> trail=s.history.reverseSamples(16);
        ACTIVE.remove(p.getUniqueID());
        if(s.mount!=null) {
            s.mount.setPositionAndRotation(mountDest.x,mountDest.y,mountDest.z,mountDest.yaw,mountDest.pitch);
            stop(s.mount);s.mount.updatePassenger(p);
            p.connection.sendPacket(new SPacketMoveVehicle(s.mount));
        }
        // Authoritative teleport correction. No NBT restore, damage restore, invulnerability or fall reset.
        p.connection.setPlayerLocation(dest.x,dest.y,dest.z,dest.yaw,dest.pitch);
        stop(p);p.rotationYawHead=dest.yaw;p.renderYawOffset=dest.yaw;
        ItemStack eyes=activeEyes(p);
        if(eyes.isEmpty()){cancel(p,true);return;}
        ((ItemSharingan.Base)eyes.getItem()).addOcularStrain(eyes,p,18);
        OcularAbilities.persistResolved(p,eyes);
        SusanooCastController.eyeRelease(p,SusanooCastProfile.TEMPORAL);
        emit(s,1,trail);
        p.world.playSound(null,p.posX,p.posY,p.posZ,SoundEvents.ENTITY_ENDERMEN_TELEPORT,SoundCategory.PLAYERS,.7f,.7f);
        status(p,"reversed");
    }

    private static boolean safe(Entity entity,TemporalHistory.Frame dest,Entity excluded) {
        if(!dest.finite())return false;
        AxisAlignedBB box=entity.getEntityBoundingBox().offset(dest.x-entity.posX,dest.y-entity.posY,dest.z-entity.posZ);
        World world=entity.world;
        if(box.minY<0 || box.maxY>=world.getHeight())return false;
        BlockPos min=new BlockPos(box.minX,box.minY,box.minZ),max=new BlockPos(box.maxX,box.maxY,box.maxZ);
        if(!world.isAreaLoaded(min.add(-1,-1,-1),max.add(1,1,1))
            || !world.getWorldBorder().contains(min) || !world.getWorldBorder().contains(max))return false;
        if(!world.getCollisionBoxes(entity,box).isEmpty() || world.containsAnyLiquid(box))return false;
        for(Entity other:world.getEntitiesWithinAABBExcludingEntity(entity,box)) {
            if(other!=excluded && !other.isDead && other.canBeCollidedWith())return false;
        }
        return true;
    }

    private static void stop(Entity entity) {entity.motionX=entity.motionY=entity.motionZ=0;entity.velocityChanged=true;}
    private static TemporalHistory.Frame frame(Entity entity,long time) {
        return new TemporalHistory.Frame(time,entity.posX,entity.posY,entity.posZ,entity.rotationYaw,entity.rotationPitch);
    }
    private static boolean spend(EntityPlayer player,double cost) {
        return player.isCreative() || Chakra.pathway(player).consume(cost);
    }
    public static long cooldownRemaining(EntityPlayer player,long nowMillis) {
        return Math.max(0,player.getEntityData().getLong(COOLDOWN)-nowMillis);
    }
    private static void status(EntityPlayer player,String key,Object... arguments) {
        TextComponentTranslation message=new TextComponentTranslation("message.madara.temporal_"+key,arguments);
        message.getStyle().setColor(TextFormatting.AQUA);player.sendStatusMessage(message,true);
    }
    private static void sound(EntityPlayer player,String id,float volume,float pitch) {
        SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation(id));
        if(sound!=null)player.world.playSound(null,player.posX,player.posY,player.posZ,sound,SoundCategory.PLAYERS,volume,pitch);
    }
    private static void emit(Session s,int kind,List<TemporalHistory.Frame> frames) {
        EffectMessage message=new EffectMessage(s.player.getUniqueID(),s.player.dimension,kind,(int)Math.max(0,s.deadline-s.world.getTotalWorldTime()),frames);
        NarutomodMod.PACKET_HANDLER.sendToAllAround(message,new NetworkRegistry.TargetPoint(s.player.dimension,s.player.posX,s.player.posY,s.player.posZ,64));
    }
    private static void cancel(EntityPlayer player,boolean message) {
        Session s=ACTIVE.remove(player.getUniqueID());
        if(s!=null) {
            emit(s,2,Collections.emptyList());
            if(message)status(player,"released");
        }
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || event.player.world.isRemote)return;
        Session s=ACTIVE.get(event.player.getUniqueID());if(s==null)return;
        TemporalHistory.Frame previous=s.history.newest(),current=frame(s.player,s.world.getTotalWorldTime());
        // Discontinuous external teleports end the session instead of creating a cross-teleport checkpoint.
        if(!valid(s) || s.world.getTotalWorldTime()>=s.deadline || previous!=null && previous.distanceSquared(current)>64)cancel(event.player,true);
        else s.record();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {cancel(e.player,false);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {cancel(e.player,false);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {cancel(e.player,false);}
    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if(!e.getEntityLiving().world.isRemote && e.getEntityLiving() instanceof EntityPlayer)cancel((EntityPlayer)e.getEntityLiving(),false);
    }
    @SubscribeEvent public static void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone e) {
        long original=e.getOriginal().getEntityData().getLong(COOLDOWN);
        if(original>e.getEntityPlayer().getEntityData().getLong(COOLDOWN))e.getEntityPlayer().getEntityData().setLong(COOLDOWN,original);
        cancel(e.getOriginal(),false);
    }
    @SubscribeEvent public static void unload(WorldEvent.Unload e) {
        if(!e.getWorld().isRemote)for(Session s:new ArrayList<>(ACTIVE.values()))if(s.world==e.getWorld())cancel(s.player,false);
    }

    public static final class EffectMessage implements IMessage {
        public UUID caster;
        public int dimension,kind,ticks;
        public List<TemporalHistory.Frame> frames=Collections.emptyList();
        public EffectMessage() { }
        public EffectMessage(UUID caster,int dimension,int kind,int ticks,List<TemporalHistory.Frame> frames) {
            this.caster=caster;this.dimension=dimension;this.kind=kind;this.ticks=ticks;this.frames=frames;
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeLong(caster.getMostSignificantBits());buf.writeLong(caster.getLeastSignificantBits());
            buf.writeInt(dimension);buf.writeByte(kind);buf.writeShort(ticks);buf.writeByte(Math.min(20,frames.size()));
            for(int i=0;i<Math.min(20,frames.size());i++) {
                TemporalHistory.Frame f=frames.get(i);buf.writeDouble(f.x);buf.writeDouble(f.y);buf.writeDouble(f.z);buf.writeFloat(f.yaw);buf.writeFloat(f.pitch);
            }
        }
        @Override public void fromBytes(ByteBuf buf) {
            caster=new UUID(buf.readLong(),buf.readLong());dimension=buf.readInt();kind=buf.readUnsignedByte();ticks=buf.readUnsignedShort();
            int count=buf.readUnsignedByte();
            if(kind>2 || ticks>200 || count>20 || buf.readableBytes()!=count*32)throw new IllegalArgumentException("Invalid temporal effect packet");
            frames=new ArrayList<>(count);
            for(int i=0;i<count;i++) {
                TemporalHistory.Frame f=new TemporalHistory.Frame(i,buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readFloat(),buf.readFloat());
                if(!f.finite())throw new IllegalArgumentException("Non-finite temporal frame");frames.add(f);
            }
        }
        public static final class Handler implements IMessageHandler<EffectMessage,IMessage> {
            @Override @SideOnly(Side.CLIENT) public IMessage onMessage(EffectMessage message,MessageContext context) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(()->net.narutomod.client.ClientTemporalEffects.receive(message));return null;
            }
        }
    }
}
