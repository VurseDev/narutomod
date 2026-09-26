package net.narutomod;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.player.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.narutomod.item.*;

/** Server-owned, unsaved illusion sessions. Never teleports the body or edits skills/XP/inventory. */
@Mod.EventBusSubscriber(modid="narutomod")
public final class GenjutsuSession {
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static final UUID SLOW = UUID.fromString("fc6bbb8f-d39f-4672-86ec-6a2b6a118c45");
    private static final UUID WEAK = UUID.fromString("d3ee0d18-87ea-40c6-8f02-b1c356b40ce4");
    private static final String TAG = "NarutomodActiveGenjutsu";
    private GenjutsuSession() { }
    private static final class Session {
        final EntityLivingBase target, caster;
        final int type, dimension;
        final long start, end;
        Session(EntityLivingBase c, EntityLivingBase t, int k, int length) {
            caster=c; target=t; type=k; dimension=t.dimension;
            start=t.world.getTotalWorldTime(); end=start+boundedDuration(k,length);
        }
    }
    public static int boundedDuration(int type,int duration) { return Math.max(20,Math.min(type>=3?120:200,duration)); }
    public static boolean expired(long now,long end,boolean targetAlive,boolean casterPresent,boolean sameWorld) {
        return now>=end || !targetAlive || !casterPresent || !sameWorld;
    }
    public static boolean blocks(int type,boolean chakraPulse) { return type>=2 && type<=4 && !chakraPulse; }
    public static boolean active(EntityLivingBase target) { return ACTIVE.containsKey(target.getUniqueID()); }
    public static int type(EntityLivingBase target) { Session s=ACTIVE.get(target.getUniqueID());return s==null?-1:s.type; }
    public static void begin(EntityLivingBase caster,EntityLivingBase target,int type,int duration) {
        if(target.world.isRemote || !target.isEntityAlive())return;
        // Recasts replace the presentation and owned modifiers, never stack restraints.
        removeModifiers(target);
        Session session=new Session(caster,target,type,duration); ACTIVE.put(target.getUniqueID(),session);
        attribute(target,SharedMonsterAttributes.MOVEMENT_SPEED,SLOW,type>=3?-.98:type==2?-.75:type==0?-.25:0);
        if(type==2 || type==3)attribute(target,SharedMonsterAttributes.ATTACK_DAMAGE,WEAK,-.35);
        if(type>=2)target.resetActiveHand();
        if(target instanceof EntityPlayerMP)ItemInton.ClientGenjutsuMessage.send((EntityPlayerMP)target,type,(int)(session.end-session.start),caster.getEntityId());
    }
    private static void attribute(EntityLivingBase target,IAttribute attribute,UUID id,double amount) {
        IAttributeInstance value=target.getEntityAttribute(attribute);
        if(value!=null && amount!=0)value.applyModifier(new AttributeModifier(id,"Temporary genjutsu",amount,2).setSaved(false));
    }
    private static void removeModifiers(EntityLivingBase target) {
        IAttributeInstance speed=target.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        IAttributeInstance attack=target.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if(speed!=null)speed.removeModifier(SLOW); if(attack!=null)attack.removeModifier(WEAK);
    }
    public static void clear(EntityLivingBase target) {
        Session old=ACTIVE.remove(target.getUniqueID()); removeModifiers(target);
        if(old!=null) {
            target.getEntityData().removeTag(TAG);
            if(target instanceof EntityPlayerMP)ItemInton.ClientGenjutsuMessage.clear((EntityPlayerMP)target);
        }
    }
    public static boolean canUse(EntityLivingBase target,ItemJutsu.JutsuEnum jutsu) {
        if(target.world.isRemote)return true;
        if(blocks(type(target),jutsu==ItemNinjutsu.CHAKRAPULSE)) {
            if(target instanceof EntityPlayer)((EntityPlayer)target).sendStatusMessage(new TextComponentTranslation("message.narutomod.genjutsu_restrained"),true);
            return false;
        }
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        for(Session s:new ArrayList<>(ACTIVE.values())) {
            boolean connected=!(s.caster instanceof EntityPlayerMP) || s.caster.getServer().getPlayerList().getPlayerByUUID(s.caster.getUniqueID())==s.caster;
            if(expired(s.target.world.getTotalWorldTime(),s.end,s.target.isEntityAlive(),s.caster.isEntityAlive() && connected,
                s.target.world==s.caster.world && s.target.dimension==s.dimension) || s.target.isDead || !s.target.addedToChunk)clear(s.target);
            else if(s.type>=3) {
                // The body remains a normal, damageable entity. Gravity, collisions and external knockback still work.
                s.target.motionX*=.1; s.target.motionZ*=.1;
                if(s.target instanceof EntityLiving)((EntityLiving)s.target).getNavigator().clearPath();
            }
        }
    }
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if(!event.getEntityPlayer().world.isRemote && type(event.getEntityPlayer())>=3)event.setCanceled(true);
    }
    @SubscribeEvent public static void mining(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        int type=type(event.getEntityPlayer());
        if(type>=3)event.setNewSpeed(0);else if(type==1)event.setNewSpeed(event.getNewSpeed()*.2f);
    }
    @SubscribeEvent public static void damage(LivingHurtEvent event) {
        if(event.getEntityLiving().world.isRemote || event.getAmount()<=0)return;
        Session s=ACTIVE.get(event.getEntityLiving().getUniqueID());
        // A physical hit is a second RP escape route. Initial mental damage is applied before begin().
        if(s!=null && s.target.world.getTotalWorldTime()-s.start>=8)clear(s.target);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) { if(!event.getEntityLiving().world.isRemote)endFor(event.getEntityLiving()); }
    private static void endFor(EntityLivingBase entity) {
        clear(entity);
        for(Session s:new ArrayList<>(ACTIVE.values()))if(s.caster==entity)clear(s.target);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { endFor(event.player); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { endFor(event.player); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        endFor(event.player); event.player.getEntityData().removeTag(TAG); removeModifiers(event.player);
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if(!event.getWorld().isRemote)for(Session s:new ArrayList<>(ACTIVE.values()))if(s.target.world==event.getWorld() || s.caster.world==event.getWorld())clear(s.target);
    }
}
