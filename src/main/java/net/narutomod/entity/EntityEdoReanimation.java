package net.narutomod.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.narutomod.Chakra;
import net.narutomod.EdoSoulRegistry;
import net.narutomod.item.ItemJutsu;

/** A donor's archived identity, with independent server AI and no copied inventory. */
public class EntityEdoReanimation extends EntityCreature implements EntitySummonAnimal.ISummon, ItemJutsu.IJutsu {
    private static final DataParameter<NBTTagCompound> SOUL = EntityDataManager.createKey(EntityEdoReanimation.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Integer> REFORM = EntityDataManager.createKey(EntityEdoReanimation.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ORDER = EntityDataManager.createKey(EntityEdoReanimation.class, DataSerializers.VARINT);
    private boolean bound;

    public EntityEdoReanimation(World world) {
        super(world);
        setSize(.6f,1.8f);
        isImmuneToFire=true;
        experienceValue=0;
        enablePersistence();
        setCanPickUpLoot(false);
    }

    @Override protected void entityInit() {
        super.entityInit();
        dataManager.register(SOUL,new NBTTagCompound());
        dataManager.register(REFORM,0);
        dataManager.register(ORDER,0);
    }
    public void configure(UUID caster, NBTTagCompound snapshot) {
        NBTTagCompound data=snapshot.copy();
        data.removeTag("ActiveEntityMost");data.removeTag("ActiveEntityLeast");
        if(caster!=null)data.setUniqueId("Caster",caster);
        dataManager.set(SOUL,data);
        setCustomNameTag(data.getString("Name")+" (Edo Tensei)");
        double xp=data.getDouble("NinjaXp");
        double mastery=Math.sqrt(Double.isFinite(xp)?Math.max(0,Math.min(100000,xp)):0);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Math.min(80,20+mastery*.2));
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(Math.min(10,3+mastery*.025));
        setHealth(getMaxHealth());
    }
    public NBTTagCompound soul() { return dataManager.get(SOUL); }
    public UUID ownerId() { return soul().hasUniqueId("Caster")?soul().getUniqueId("Caster"):null; }
    public UUID soulId() { return soul().hasUniqueId("Soul")?soul().getUniqueId("Soul"):null; }
    public int reformTicks() { return dataManager.get(REFORM); }
    public int order() { return dataManager.get(ORDER); }
    public void bind() { bound=true; }
    @Override public EntityLivingBase getSummoner() { return ownerId()==null?null:world.getPlayerEntityByUUID(ownerId()); }
    @Override public ItemJutsu.JutsuEnum.Type getJutsuType() { return ItemJutsu.JutsuEnum.Type.NINJUTSU; }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(.29);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24);
    }
    @Override protected void initEntityAI() {
        tasks.addTask(0,new EntityAISwimming(this));
        tasks.addTask(1,new EntityAIAttackMelee(this,1.15,true));
        tasks.addTask(2,new EntityAIBase() {
            { setMutexBits(3); }
            public boolean shouldExecute() {
                EntityLivingBase caster=getSummoner();
                return order()!=1 && reformTicks()==0 && getAttackTarget()==null && caster!=null && getDistanceSq(caster)>9;
            }
            public void updateTask() {
                EntityLivingBase caster=getSummoner();
                if(caster!=null && ticksExisted%10==0)getNavigator().tryMoveToEntityLiving(caster,1.05);
            }
            public void resetTask() { getNavigator().clearPath(); }
        });
        tasks.addTask(5,new EntityAIWatchClosest(this,EntityPlayer.class,8));
    }
    private boolean friendly(Entity entity) {
        if(entity==null)return false;
        if(entity==this || entity.getUniqueID().equals(ownerId()))return true;
        if(entity instanceof EntityEdoReanimation && ownerId()!=null && ownerId().equals(((EntityEdoReanimation)entity).ownerId()))return true;
        EntityLivingBase caster=getSummoner();
        if(caster==null)return false;
        return caster.isOnSameTeam(entity) || entity instanceof EntitySummonAnimal.ISummon
            && caster.equals(((EntitySummonAnimal.ISummon)entity).getSummoner());
    }
    @Override public boolean isOnSameTeam(Entity entity) { return friendly(entity)||super.isOnSameTeam(entity); }
    private boolean canFight(EntityLivingBase target) {
        EntityLivingBase caster=getSummoner();
        return order()!=2 && reformTicks()==0 && target!=null && target.isEntityAlive() && !friendly(target)
            && ItemJutsu.canTarget(target) && getDistanceSq(target)<24*24
            && (!(target instanceof EntityPlayer) || caster instanceof EntityPlayer
                && !((EntityPlayer)target).isSpectator() && !((EntityPlayer)target).isCreative()
                && ((EntityPlayer)caster).canAttackPlayer((EntityPlayer)target));
    }
    @Override public void setAttackTarget(EntityLivingBase target) { super.setAttackTarget(canFight(target)?target:null); }
    @Override public boolean attackEntityAsMob(Entity target) {
        if(!(target instanceof EntityLivingBase)||!canFight((EntityLivingBase)target))return false;
        swingArm(EnumHand.MAIN_HAND);
        return target.attackEntityFrom(ItemJutsu.causeJutsuDamage(this,getSummoner()),(float)getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
    }
    @Override public boolean attackEntityFrom(DamageSource source,float amount) {
        if(world.isRemote)return false;
        if(source==DamageSource.OUT_OF_WORLD)return super.attackEntityFrom(source,amount);
        if(friendly(source.getTrueSource()) || reformTicks()>0 || isEntityInvulnerable(source))return false;
        if(amount>=getHealth() && amount>0) {
            setHealth(1);
            dataManager.set(REFORM,100);
            setNoAI(true);super.setAttackTarget(null);getNavigator().clearPath();
            puff();
            return true;
        }
        return super.attackEntityFrom(source,amount);
    }
    @Override public void onUpdate() {
        super.onUpdate();
        if(world.isRemote||isDead||!bound)return;
        EntityLivingBase caster=getSummoner();
        if(caster==null || !caster.isEntityAlive() || getDistanceSq(caster)>96*96
            || !getUniqueID().equals(EdoSoulRegistry.get(world).active(ownerId(),soulId()))) { dismiss();return; }
        if(ticksExisted%20==0 && caster instanceof EntityPlayer && !((EntityPlayer)caster).isCreative()
            && !Chakra.pathway(caster).consume(10d)) { dismiss();return; }
        int reform=reformTicks();
        if(reform>0) {
            motionX=motionZ=0;
            dataManager.set(REFORM,reform-1);
            if(reform%10==0)puff();
            if(reform==1) {setHealth(getMaxHealth());setNoAI(false);}
            return;
        }
        if(ticksExisted%10==0) {
            if(!canFight(getAttackTarget()))super.setAttackTarget(null);
            if(order()!=2 && getAttackTarget()==null) {
                EntityLivingBase target=caster.getRevengeTarget();
                if(!canFight(target))target=caster.getLastAttackedEntity();
                if(!canFight(target))target=getRevengeTarget();
                if(canFight(target))setAttackTarget(target);
            }
        }
        if(ticksExisted%40==0 && getHealth()<getMaxHealth())heal(1);
    }
    @Override protected boolean processInteract(EntityPlayer player,EnumHand hand) {
        if(!player.getUniqueID().equals(ownerId()) || !player.getHeldItem(hand).isEmpty())return false;
        if(!world.isRemote) {
            if(player.isSneaking())dismiss();
            else {
                dataManager.set(ORDER,(order()+1)%3);
                super.setAttackTarget(null);getNavigator().clearPath();
                player.sendStatusMessage(new TextComponentTranslation("message.narutomod.edo_order_"+order()),true);
            }
        }
        return true;
    }
    private void puff() {
        if(world instanceof WorldServer)((WorldServer)world).spawnParticle(EnumParticleTypes.CLOUD,posX,posY+.9,posZ,12,.3,.7,.3,.04);
    }
    public void dismiss() {
        if(isDead)return;
        if(!world.isRemote) {
            puff();
            SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:poof"));
            world.playSound(null,posX,posY,posZ,sound==null?SoundEvents.BLOCK_FIRE_EXTINGUISH:sound,SoundCategory.PLAYERS,.7f,1);
        }
        setDead();
    }
    @Override public void setDead() {
        if(!world.isRemote && bound && ownerId()!=null && soulId()!=null)
            EdoSoulRegistry.get(world).deactivate(ownerId(),soulId(),getUniqueID());
        super.setDead();
    }
    @Override protected boolean canDespawn() { return false; }
    @Override protected void dropLoot(boolean recentlyHit,int looting,DamageSource source) { }
    @Override public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        dataManager.set(SOUL,nbt.getCompoundTag("EdoSoul"));
        dataManager.set(REFORM,Math.max(0,Math.min(100,nbt.getInteger("Reform"))));
        dataManager.set(ORDER,Math.floorMod(nbt.getInteger("Order"),3));
        bound=nbt.getBoolean("Bound");
        setNoAI(reformTicks()>0);
    }
    @Override public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setTag("EdoSoul",soul().copy());nbt.setInteger("Reform",reformTicks());
        nbt.setInteger("Order",order());nbt.setBoolean("Bound",bound);
    }
}
