package net.narutomod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.narutomod.entity.EntityKingOfHell;
import net.narutomod.entity.EntityPretaShield;
import net.narutomod.entity.EntitySusanooBase;
import net.narutomod.item.*;
import net.narutomod.procedure.*;
import net.narutomod.world.WorldKamuiDimension;

/** Physical-eye ability routing. Never substitutes equipment or changes donor ownership. */
public final class OcularAbilities {
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    private static final Map<EntityPlayer, Hold[]> HELD = new WeakHashMap<>();
    private static final String KAMUI_CD = "OcularKamuiCooldown";

    private OcularAbilities() {}

    private static final class Context {
        final EntityPlayer player;
        final OcularState state;
        final OcularState.Eye eye;
        Context(EntityPlayer p, OcularState s, OcularState.Eye e) { player=p; state=s; eye=e; }
    }

    private static final class Hold {
        final UUID eye;
        final int dimension;
        long lastPulse, processed=-1;
        int ticks;
        boolean phasing, selfWarp, modeSet, failed;
        boolean oldEdit;
        UUID target;
        Vec3d safePosition;
        Hold(EntityPlayer p, OcularState.Eye e) { eye=e.id; dimension=p.dimension; lastPulse=p.world.getTotalWorldTime(); }
    }

    public static OcularState.Eye selectedEye(EntityLivingBase player) {
        if (!OcularSystem.enabled(player)) return null;
        Context context=CONTEXT.get();
        if (context!=null && context.player==player) return context.eye;
        OcularState state=OcularSystem.state(player);
        return state.eyes[state.selected];
    }

    private static boolean available(EntityLivingBase player, OcularState state) {
        return player.isEntityAlive() && !(player instanceof EntityPlayer && ((EntityPlayer)player).isSpectator())
            && OcularPolicy.abilitiesAvailable(state.recoveringUntil, player.world.getTotalWorldTime(),
                !player.world.isRemote && player instanceof EntityPlayer && MedicalSurgery.operating((EntityPlayer)player));
    }

    /** Head-dependent legacy helpers opt in here; untouched players retain their exact old stack. */
    public static ItemStack resolve(EntityLivingBase player, String kind) {
        if (!OcularSystem.enabled(player)) return player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        Context context=CONTEXT.get();
        OcularState state=context!=null && context.player==player ? context.state : OcularSystem.state(player);
        if (!available(player,state)) return ItemStack.EMPTY;
        if ("susanoo".equals(kind)) return susanooEye(state);
        if (context!=null && context.player==player)
            return matches(context.eye,kind) ? context.eye.stack : ItemStack.EMPTY;
        OcularState.Eye selected=state.eyes[state.selected];
        if (matches(selected,kind)) return selected.stack;
        for (OcularState.Eye eye:state.eyes) if (matches(eye,kind)) return eye.stack;
        return ItemStack.EMPTY;
    }

    /** True when the requested legacy ability is actually supplied; resolve() deliberately returns an untouched head stack for unoperated players. */
    public static boolean hasResolvedAbility(EntityLivingBase player,String kind) {
        ItemStack stack=resolve(player,kind);
        if (OcularSystem.enabled(player)) return !stack.isEmpty();
        if ("rinnegan".equals(kind)) return stack.getItem()==ItemRinnegan.helmet;
        if (!(stack.getItem() instanceof ItemSharingan.Base)) return false;
        ItemSharingan.Base eye=(ItemSharingan.Base)stack.getItem();
        if ("amaterasu".equals(kind)) return eye.canUseAmaterasu();
        if ("kamui".equals(kind)) return eye.canUseKamui();
        if ("madara".equals(kind)) return eye.isMangekyo()&&eye.getSubType()==ItemSharingan.Type.MADARA;
        return false;
    }

    private static boolean legacyMixed(OcularState.Eye eye) {
        return eye.stack.getItem()==ItemMangekyoSharinganEternal.helmet
            && (!eye.stack.hasTagCompound() || !eye.stack.getTagCompound().hasKey("OcularAbilityFamily",8));
    }

    private static boolean matches(OcularState.Eye eye, String kind) {
        if (eye==null || !eye.usable()) return false;
        if ("rinnegan".equals(kind)) return eye.rinnegan();
        if (!eye.mangekyo() || eye.stack.getItem()==ItemMangekyoSharinganObitoFire.helmet) return false;
        if ("madara".equals(kind)) return eye.family()==ItemSharingan.Type.MADARA;
        if (legacyMixed(eye)) return eye.donorSide==0 ? "amaterasu".equals(kind) : "kamui".equals(kind);
        return "amaterasu".equals(kind) && eye.family()==ItemSharingan.Type.AMATERASU
            || "kamui".equals(kind) && eye.family()==ItemSharingan.Type.KAMUI;
    }

    /** Two unrelated MS items are not a paired Susanoo; stage XP remains checked by the old controller. */
    public static ItemStack susanooEye(OcularState state) {
        OcularState.Eye left=state.eyes[0], right=state.eyes[1];
        if (left==null || right==null || !left.usable() || !right.usable()
            || !left.mangekyo() || !right.mangekyo() || !left.donor.equals(right.donor)
            || left.donorSide==right.donorSide || left.family()!=right.family()
            || left.stack.getItem()==ItemMangekyoSharinganObitoFire.helmet
            || right.stack.getItem()==ItemMangekyoSharinganObitoFire.helmet) return ItemStack.EMPTY;
        // A half-eternal pair must not bypass the normal maximum-stage restriction.
        return left.eternal() && !right.eternal() ? right.stack : left.stack;
    }

    public static boolean samePhysicalEye(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return false;
        if (first==second) return true;
        return first.getItem()==second.getItem() && first.hasTagCompound() && second.hasTagCompound()
            && first.getTagCompound().hasUniqueId("OcularPhysicalId") && second.getTagCompound().hasUniqueId("OcularPhysicalId")
            && first.getTagCompound().getUniqueId("OcularPhysicalId").equals(second.getTagCompound().getUniqueId("OcularPhysicalId"));
    }

    /** Used by continuous legacy controllers after mutating a decoded physical-eye payload. */
    public static void persistResolved(EntityPlayer player, ItemStack stack) {
        if (player.world.isRemote || !OcularSystem.enabled(player) || stack.isEmpty()) return;
        Context context=CONTEXT.get();
        if (context!=null && context.player==player) return; // key caller saves the same live state.
        OcularState state=OcularSystem.state(player);
        for (OcularState.Eye eye:state.eyes) if (eye!=null && samePhysicalEye(eye.stack,stack)) {
            eye.stack=stack.copy(); OcularSystem.save(player,state); return;
        }
    }

    public static boolean key(EntityPlayer player, OcularState state, int key, boolean pressed) {
        if (key<1 || key>3 || player.world.isRemote) return true;
        OcularState.Eye eye=state.eyes[state.selected];
        Hold[] holds=HELD.computeIfAbsent(player,p->new Hold[3]);
        Hold hold=holds[key-1];
        if (!available(player,state) || eye==null || !eye.usable() || !GenjutsuSession.canUse(player,null)
            || ItemByakugan.getBlockedTenketsuStacks(player)>=8) {
            cancelHeld(player); return true;
        }
        if (!eye.mangekyo() && !eye.rinnegan()) return false;
        long now=player.world.getTotalWorldTime();
        if (hold!=null && (!hold.eye.equals(eye.id) || hold.dimension!=player.dimension)) {
            cancelHeld(player); hold=null; holds=HELD.computeIfAbsent(player,p->new Hold[3]);
        }
        if (pressed) {
            if (hold==null) { hold=new Hold(player,eye); holds[key-1]=hold; }
            hold.lastPulse=now;
            if (hold.processed==now) return true; // At most one client pulse per server tick.
            hold.processed=now;
        } else if (hold==null) return true; // Release packets cannot create an uncharged technique.
        Context previous=CONTEXT.get();
        CONTEXT.set(new Context(player,state,eye));
        try {
            if (eye.rinnegan()) rinnegan(player,eye,key,pressed);
            else if (eye.stack.getItem()==ItemMangekyoSharinganObitoFire.helmet) fire(player,eye,key,pressed);
            else if (key==2) {
                if (!pressed) {
                    if (ProcedureSusanoo.isActivated(player) || !susanooEye(state).isEmpty()) ProcedureSusanoo.execute(player);
                    else OcularSystem.message(player,"Susanoo requires a matching donor pair and the usual battle XP.");
                }
            } else if (key==1) {
                if (matches(eye,"madara")) { if (!pressed) MadaraTemporalController.toggle(player); }
                else if (matches(eye,"amaterasu")) amaterasu(player,eye,pressed);
                else if (matches(eye,"kamui")) kamui(player,eye,hold,pressed);
            }
        } finally {
            if (!pressed) { restorePhase(player,hold); holds[key-1]=null; }
            if (previous==null) CONTEXT.remove(); else CONTEXT.set(previous);
        }
        return true;
    }

    private static Map<String,Object> dependencies(EntityPlayer p,boolean pressed) {
        Map<String,Object> data=new HashMap<>();
        data.put("entity",p); data.put("world",p.world); data.put("is_pressed",pressed);
        data.put("x",(int)p.posX); data.put("y",(int)p.posY); data.put("z",(int)p.posZ);
        return data;
    }

    private static void amaterasu(EntityPlayer player,OcularState.Eye eye,boolean pressed) {
        if (eye.donorSide==0) {
            // Ignition belongs to the donor's left eye, irrespective of its current socket.
            if (!player.isSneaking() || !pressed) {
                if (!pressed && player.isSneaking()) player.getEntityData().setBoolean("amaterasu_active",false);
                else ProcedureAmaterasu.executeProcedure(dependencies(player,pressed));
            }
        } else if (!pressed) {
            long now=player.world.getTotalWorldTime();
            if (player.getEntityData().getLong("OcularFlameControlCooldown")>now) return;
            RayTraceResult hit=ProcedureUtils.objectEntityLookingAt(player,30d);
            if (hit==null || hit.typeOfHit==RayTraceResult.Type.MISS) return;
            if (!spend(player,eye.donor.equals(player.getUniqueID()) ? 25d : 75d)) return;
            // Reuse the existing extinction effects, but do not clear a 30-block cube from one eye.
            if (hit.entityHit!=null) ProcedureAmaterasuExtinguishEntities.one(hit.entityHit);
            else if (hit.getBlockPos()!=null) {
                BlockPos center=hit.getBlockPos();
                for (BlockPos pos:BlockPos.getAllInBox(center.add(-2,-2,-2),center.add(2,2,2)))
                    if (player.world.isBlockLoaded(pos) && player.world.getBlockState(pos).getBlock()==net.narutomod.block.BlockAmaterasuBlock.block)
                        player.world.setBlockToAir(pos);
            }
            ((ItemSharingan.Base)eye.stack.getItem()).addOcularStrain(eye.stack,player,2);
            player.getEntityData().setLong("OcularFlameControlCooldown",now+40);
            sound(player,"sharingansfx",.65f);
        }
    }

    private static void fire(EntityPlayer player,OcularState.Eye eye,int key,boolean pressed) {
        if (eye.donorSide==0 && key!=1 || eye.donorSide==1 && key==1) return;
        int index=key==1?ItemKaton.GREATFIREBALL.index:key==2?ItemKaton.GFANNIHILATION.index:ItemKaton.HIDINGINASH.index;
        String cooldown="CustomFireJutsuCooldown"+index;
        long before=eye.stack.hasTagCompound()?eye.stack.getTagCompound().getLong(cooldown):0;
        ItemDojutsu.Base item=(ItemDojutsu.Base)eye.stack.getItem();
        if (key==1) item.onJutsuKey1(pressed,eye.stack,player);
        else if (key==2) item.onJutsuKey2(pressed,eye.stack,player);
        else item.onJutsuKey3(pressed,eye.stack,player);
        if (!pressed && eye.stack.hasTagCompound() && eye.stack.getTagCompound().getLong(cooldown)>before)
            ((ItemSharingan.Base)eye.stack.getItem()).addOcularStrain(eye.stack,player,2);
    }

    private static void rinnegan(EntityPlayer player,OcularState.Eye eye,int key,boolean pressed) {
        ItemDojutsu.Base item=(ItemDojutsu.Base)eye.stack.getItem();
        if (!eye.stack.hasTagCompound()) eye.stack.setTagCompound(new NBTTagCompound());
        if (!eye.stack.getTagCompound().hasKey("which_path")) eye.stack.getTagCompound().setDouble("which_path",0);
        if (key==1) item.onJutsuKey1(pressed,eye.stack,player);
        else if (key==2) {
            // The old Meteor/Outer callbacks dereference a miss; guard before entering them.
            int path=(int)eye.stack.getTagCompound().getDouble("which_path");
            if (path==1) {
                // An on-demand missile reuses the existing Asura projectile without overwriting armor/offhand gear.
                long now=player.world.getTotalWorldTime();
                if (!pressed && player.getEntityData().getLong("OcularAsuraCooldown")<=now
                    && spend(player,eye.donor.equals(player.getUniqueID())?20d:40d)) {
                    ItemAsuraCanon.EntityMissile.shoot(player);
                    player.swingArm(EnumHand.MAIN_HAND);
                    player.getEntityData().setLong("OcularAsuraCooldown",now+20);
                }
                return;
            }
            if (path==4 && !pressed && !eye.stack.getTagCompound().hasUniqueId("KoH_id")) {
                // The legacy Naraka constructor searches downward without a lower bound.
                // Refuse a void/unloaded location before calling it, rather than hanging a server.
                Vec3d ahead=player.getPositionEyes(1f).add(player.getLookVec().scale(4d));
                BlockPos floor=new BlockPos(ahead);
                if (floor.getY()<1 || floor.getY()>=player.world.getHeight()) return;
                while (floor.getY()>0 && player.world.isBlockLoaded(floor) && !player.world.getBlockState(floor).isTopSolid()) floor=floor.down();
                if (floor.getY()<=0 || !player.world.isBlockLoaded(floor) || !player.world.getBlockState(floor).isTopSolid()) return;
                BlockPos ceiling=floor.up();
                while (ceiling.getY()<player.world.getHeight() && player.world.isBlockLoaded(ceiling) && player.world.getBlockState(ceiling).isTopSolid()) ceiling=ceiling.up();
                if (ceiling.getY()>=player.world.getHeight() || !player.world.isBlockLoaded(ceiling)) return;
            }
            if (!pressed && (path==0 && player.isSneaking() || path==5)) {
                RayTraceResult hit=player.world.rayTraceBlocks(player.getPositionEyes(1f),
                    player.getPositionEyes(1f).add(player.getLookVec().scale(path==5?5:100)),false,false,true);
                if (hit==null || hit.getBlockPos()==null) return;
            }
            item.onJutsuKey2(pressed,eye.stack,player);
            if (player.getRidingEntity() instanceof EntityPretaShield.EntityCustom)
                player.getEntityData().setUniqueId("OcularPretaShield",player.getRidingEntity().getUniqueID());
        } else item.onJutsuKey3(pressed,eye.stack,player);
    }

    /** Power-cycle preserves the existing Rinnegan path menu and Susanoo upgrade control. */
    public static boolean switchTechnique(EntityPlayer player,OcularState state,boolean pressed) {
        if (player.world.isRemote || !available(player,state)) return false;
        OcularState.Eye eye=state.eyes[state.selected];
        if (eye==null || !eye.usable()) return false;
        Context old=CONTEXT.get(); CONTEXT.set(new Context(player,state,eye));
        try {
            if (eye.rinnegan()) return ((ItemDojutsu.Base)eye.stack.getItem()).onSwitchJutsuKey(pressed,eye.stack,player);
            if (player.getRidingEntity() instanceof EntitySusanooBase && !susanooEye(state).isEmpty()) {
                if (!pressed) ProcedureSusanoo.upgrade(player);
                return true;
            }
            return false;
        } finally { if (old==null) CONTEXT.remove(); else CONTEXT.set(old); }
    }

    private static void kamui(EntityPlayer player,OcularState.Eye eye,Hold hold,boolean pressed) {
        long now=player.world.getTotalWorldTime();
        if (hold.failed) return;
        if (player.getEntityData().getLong(KAMUI_CD)>now || player.isRiding() || player.isBeingRidden()) {
            abortKamui(player,hold); return;
        }
        if (!hold.modeSet) {
            hold.modeSet=true;
            hold.selfWarp=eye.donorSide==1 && player.isSneaking();
            if (eye.donorSide==0) {
                RayTraceResult hit=ProcedureUtils.objectEntityLookingAt(player,50d);
                if (hit!=null && hit.entityHit!=null) hold.target=hit.entityHit.getUniqueID();
            }
        }
        boolean phase=eye.donorSide==1 && !hold.selfWarp;
        Entity target=hold.selfWarp ? player : hold.target!=null && player.world instanceof WorldServer
            ? ((WorldServer)player.world).getEntityFromUuid(hold.target) : null;
        if (!phase && (target==null || target.isDead || target.isRiding() || target.isBeingRidden()
            || target!=player && (player.getDistanceSq(target)>2500 || !player.canEntityBeSeen(target)))) {
            abortKamui(player,hold); return;
        }
        if (pressed) {
            if (hold.ticks>=(phase?600:200)) {
                player.getEntityData().setLong(KAMUI_CD,now+100); abortKamui(player,hold); return;
            }
            double multiplier=eye.donor.equals(player.getUniqueID())?1d:3d;
            if (!spend(player,(phase?1d:20d)*multiplier)) { abortKamui(player,hold); return; }
            hold.ticks++; // Only successfully paid server ticks contribute to a warp charge.
            if (phase) {
                if (!hold.phasing) {
                    hold.oldEdit=player.capabilities.allowEdit;
                    hold.safePosition=player.getPositionVector(); hold.phasing=true;
                }
                if (player.world.getCollisionBoxes(player,player.getEntityBoundingBox()).isEmpty()) hold.safePosition=player.getPositionVector();
                player.capabilities.allowEdit=false;
                ProcedureOnLivingUpdate.setNoClip(player,true);
                ProcedureOnLivingUpdate.setUntargetable(player,3);
                ProcedureWhenPlayerAttcked.setInvulnerable(player,2);
                player.fallDistance=0;
                player.getEntityData().setBoolean("kamui_intangible",true);
                if (hold.ticks==1) { player.sendPlayerAbilities(); sound(player,"kamui",.7f); }
            } else {
                player.getEntityData().setBoolean("kamui_teleport",true);
                if (hold.ticks%5==1) Particles.spawnParticle(player.world,Particles.Types.PORTAL_SPIRAL,
                    target.posX,target.posY+target.height*.5,target.posZ,20,0d,0d,0d,0d,0d,0d,3,0x30000000,15);
                if (hold.ticks%60==1) sound(player,"kamui",.8f);
            }
            if (hold.ticks%6==1) ((ItemSharingan.Base)eye.stack.getItem()).addOcularStrain(eye.stack,player,3);
        } else {
            player.getEntityData().setBoolean("kamui_teleport",false);
            if (phase) {
                player.getEntityData().setLong(KAMUI_CD,now+Math.max(20,hold.ticks-400));
            } else {
                double distance=player.getDistance(target);
                int required=hold.selfWarp?40:(int)Math.max(20,Math.min(180,distance*target.getEntityBoundingBox().getAverageEdgeLength()
                    *Math.max(1d,2.01d-PlayerTracker.getNinjaLevel(player)/500.1d)));
                if (hold.ticks<required) { OcularSystem.message(player,"Kamui needs a longer charge."); return; }
                int dimension=target.dimension==WorldKamuiDimension.DIMID?0:WorldKamuiDimension.DIMID;
                player.getEntityData().setLong(KAMUI_CD,now+100);
                ProcedureKamuiTeleportEntity.eEntity(target,(int)target.posX,(int)target.posZ,dimension);
            }
        }
    }

    private static boolean spend(EntityPlayer player,double amount) {
        return player.isCreative() || Chakra.pathway(player).consume(amount);
    }

    private static void sound(EntityPlayer player,String name,float volume) {
        SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod",name));
        if (sound!=null) player.world.playSound(null,player.posX,player.posY,player.posZ,sound,SoundCategory.PLAYERS,volume,1f);
    }

    private static void restorePhase(EntityPlayer player,Hold hold) {
        if (hold==null || !hold.phasing) return;
        hold.phasing=false;
        ProcedureOnLivingUpdate.setNoClip(player,false);
        player.getEntityData().setBoolean("kamui_intangible",false);
        if (!player.capabilities.allowEdit && !player.isSpectator()) player.capabilities.allowEdit=hold.oldEdit;
        player.sendPlayerAbilities();
        if (hold.dimension==player.dimension && hold.safePosition!=null && player.isEntityAlive()
            && !player.world.getCollisionBoxes(player,player.getEntityBoundingBox()).isEmpty())
            player.setPositionAndUpdate(hold.safePosition.x,hold.safePosition.y,hold.safePosition.z);
    }

    private static void abortKamui(EntityPlayer player,Hold hold) {
        hold.failed=true;
        player.getEntityData().setBoolean("kamui_teleport",false);
        restorePhase(player,hold);
    }

    private static void cancelHeld(EntityPlayer player) {
        Hold[] holds=HELD.remove(player);
        if (holds!=null) for (Hold hold:holds) restorePhase(player,hold);
        player.getEntityData().setBoolean("kamui_teleport",false);
        player.getEntityData().setBoolean("amaterasu_active",false);
        player.getEntityData().setBoolean("was_pressed",false);
        player.getEntityData().setDouble("shinratensei_power",0);
        if (ProcedureBanShoTenin.isInUse(player)) ProcedureBanShoTenin.execute(false,player,null);
    }

    /** Cancel, never release-fire, when surgery, cover, a GUI, death or disconnect interrupts a hold. */
    public static void cleanup(EntityPlayer player) {
        if (!player.world.isRemote && OcularSystem.enabled(player)) cancelHeld(player);
    }

    /** Called once per server tick; true means the supplied physical-eye state needs saving. */
    public static boolean tick(EntityPlayer player,OcularState state) {
        if (player.world.isRemote) return false;
        boolean changed=false, active=available(player,state);
        Hold[] holds=HELD.get(player);
        if (holds!=null) for (Hold hold:holds) if (hold!=null) {
            OcularState.Eye selected=state.eyes[state.selected];
            if (!active || selected==null || !selected.usable() || !selected.id.equals(hold.eye)
                || hold.dimension!=player.dimension || player.world.getTotalWorldTime()-hold.lastPulse>4
                || player.getEntityData().getBoolean("hasAnyGuiOpen")) { cancelHeld(player); break; }
        }
        Hold[] current=HELD.get(player);
        Hold burning=current==null?null:current[0];
        if (active && player.ticksExisted%6==1) for (OcularState.Eye eye:state.eyes)
            if (eye!=null && eye.usable() && eye.mangekyo() && !eye.eternal()
                && (player.getEntityData().getBoolean("amaterasu_active") && burning!=null && burning.eye.equals(eye.id) && matches(eye,"amaterasu")
                    || ProcedureSusanoo.isActivated(player) && !susanooEye(state).isEmpty())) {
                ((ItemSharingan.Base)eye.stack.getItem()).addOcularStrain(eye.stack,player,3); changed=true;
            }
        boolean rinnegan=false;
        for (OcularState.Eye eye:state.eyes) if (eye!=null && eye.rinnegan()) {
            if (active && eye.usable()) rinnegan=true;
            if (eye.stack.hasTagCompound() && eye.stack.getTagCompound().hasUniqueId("KoH_id") && player.world instanceof WorldServer) {
                Entity king=((WorldServer)player.world).getEntityFromUuid(eye.stack.getTagCompound().getUniqueId("KoH_id"));
                if ((!active || !eye.usable()) && king instanceof EntityKingOfHell.EntityCustom
                    && ((EntityKingOfHell.EntityCustom)king).getSummoner()==player) king.setDead();
                if (!(king instanceof EntityKingOfHell.EntityCustom) || !king.isEntityAlive()
                    || ((EntityKingOfHell.EntityCustom)king).getSummoner()!=player) {
                    ProcedureUtils.removeUniqueIdTag(eye.stack,"KoH_id"); changed=true;
                }
            }
        }
        if (active) {
            boolean mangekyo=false;
            for (OcularState.Eye eye:state.eyes) if (eye!=null && eye.usable() && eye.mangekyo()) mangekyo=true;
            if (rinnegan) {
                player.fallDistance=0;
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED,3,4,false,false));
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE,3,2,false,false));
            } else if (mangekyo) player.addPotionEffect(new PotionEffect(MobEffects.SPEED,3,2,false,false));
        }
        if ((!active || susanooEye(state).isEmpty()) && ProcedureSusanoo.isActivated(player)) ProcedureSusanoo.execute(player);
        if (!rinnegan && player.getEntityData().hasUniqueId("OcularPretaShield")) {
            Entity shield=player.getRidingEntity();
            if (shield instanceof EntityPretaShield.EntityCustom && shield.getUniqueID().equals(player.getEntityData().getUniqueId("OcularPretaShield"))) shield.setDead();
            player.getEntityData().removeTag("OcularPretaShield");
        }
        return changed;
    }
}
