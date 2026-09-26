package net.narutomod.entity;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.*;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.*;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.*;
import net.narutomod.*;
import net.narutomod.item.ItemJutsu;

/** One bounded, server-authoritative cast, sharing its victim damage budget across all tags. */
public class EntityPaperBombCast extends Entity implements ItemJutsu.IJutsu {
    private static final DataParameter<NBTTagCompound> FRAME=EntityDataManager.createKey(EntityPaperBombCast.class,DataSerializers.COMPOUND_TAG);
    private UUID owner;
    private int mode,age;
    private Vec3d direction=Vec3d.ZERO;
    private EntityLivingBase target;
    private boolean triggered;
    private final List<Tag> tags=new ArrayList<>();
    private final Map<UUID,Integer> hits=new HashMap<>();
    private final Map<EntityLivingBase,Burst> pending=new LinkedHashMap<>();
    private final Set<BlockPos> placementGrass=new LinkedHashSet<>();
    private NBTTagCompound previousFrame=new NBTTagCompound();
    private int frameTick;

    @ElementsNarutomodMod.ModElement.Tag
    public static class Registration extends ElementsNarutomodMod.ModElement {
        public Registration(ElementsNarutomodMod elements){super(elements,1131);}
        @Override public void initElements(){elements.entities.add(()->EntityEntryBuilder.create().entity(EntityPaperBombCast.class)
            .id(new ResourceLocation("narutomod:paper_bomb_cast"),9360).name("paper_bomb_cast").tracker(64,2,false).build());}
        @Override public void preInit(FMLPreInitializationEvent event){new Renderer().register();}
    }
    public static class Renderer extends EntityRendererRegister {
        @Override @SideOnly(Side.CLIENT) public void register(){net.narutomod.client.RenderPaperBombCast.register();net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.narutomod.client.ClientPaperBombCasting());}
    }
    public EntityPaperBombCast(World world){super(world);setSize(.1f,.1f);setNoGravity(true);isImmuneToFire=true;ignoreFrustumCheck=true;}
    @Override public boolean isInRangeToRenderDist(double distanceSquared){return distanceSquared<64*64;}
    @Override protected void entityInit(){dataManager.register(FRAME,new NBTTagCompound());}
    @Override public ItemJutsu.JutsuEnum.Type getJutsuType(){return ItemJutsu.JutsuEnum.Type.NINJUTSU;}
    @Override protected void readEntityFromNBT(NBTTagCompound nbt){setDead();} // Paid transient effects never resume after reload.
    @Override protected void writeEntityToNBT(NBTTagCompound nbt){}
    public NBTTagCompound frame(){return dataManager.get(FRAME);}
    public NBTTagCompound previousFrame(){return previousFrame;}
    public float frameFraction(float partial){return MathHelper.clamp((ticksExisted-frameTick+partial)/2f,0,1);}
    @Override public void notifyDataManagerChange(DataParameter<?> key){
        if(FRAME.equals(key)&&world!=null&&world.isRemote){frameTick=ticksExisted;}
        super.notifyDataManagerChange(key);
    }
    private NBTTagCompound lastClientFrame;
    @Override public void onUpdate(){
        super.onUpdate();
        if(world.isRemote){
            if(lastClientFrame!=frame()){previousFrame=lastClientFrame==null?frame():lastClientFrame;lastClientFrame=frame();}
            return;
        }
        EntityPlayer caster=owner==null?null:world.getPlayerEntityByUUID(owner);
        if(caster==null||!caster.isEntityAlive()||caster.getDistance(this)>48||++age> (mode==PaperBombPolicy.CIRCUIT?600:140)){fizzle();return;}
        if(mode==PaperBombPolicy.CIRCUIT&&!triggered&&age>=20){
            for(EntityLivingBase victim:world.getEntitiesWithinAABB(EntityLivingBase.class,new AxisAlignedBB(posX-2,posY-1.25,posZ-2,posX+2,posY+3.25,posZ+2))){
                if(eligible(caster,victim)&&clear(getPositionVector().addVector(0,.25,0),victim.getPositionEyes(1))){
                    triggered=true;for(int i=0;i<tags.size();i++)tags.get(i).fuse=20+i*5;
                    playSound(SoundEvents.ENTITY_TNT_PRIMED,.8f,1.2f);break;
                }
            }
        }
        pending.clear();
        boolean alive=false;
        for(Tag tag:tags){
            if(tag.spent)continue;
            if(!world.isBlockLoaded(new BlockPos(tag.pos))){tag.spent=true;continue;}
            if(tag.attached!=null){
                if(!eligible(caster,tag.attached)||tag.attached.world!=world){tag.spent=true;continue;}
                tag.pos=tag.attached.getPositionVector().add(tag.offset);
            }
            if(tag.face!=null&&tag.support!=null&&world.getBlockState(tag.support).getCollisionBoundingBox(world,tag.support)==null){tag.spent=true;continue;}
            if(mode==PaperBombPolicy.CIRCUIT&&triggered){
                Vec3d inward=tag.pos.add(getPositionVector().subtract(tag.pos).scale(.045));
                RayTraceResult floor=PaperBombPlacement.ground(world,inward);
                if(floor!=null){
                    Vec3d grounded=floor.hitVec.addVector(0,.035,0);
                    if(clear(tag.pos.addVector(0,.08,0),grounded.addVector(0,.08,0))){tag.pos=grounded;tag.support=floor.getBlockPos();}
                }
            }
            if(tag.face==null&&tag.attached==null){fly(tag,caster);}
            if(tag.fuse>0){
                if(--tag.fuse==0){explode(tag,caster);tag.spent=true;}
                else if(age%4==0)particles(EnumParticleTypes.FLAME,tag.pos,1,.025,.005);
            }
            alive|=!tag.spent;
        }
        for(Map.Entry<EntityLivingBase,Burst> entry:pending.entrySet())applyBurst(entry.getKey(),entry.getValue(),caster);
        pending.clear();
        if(!alive){setDead();return;}
        if(age%2==0)sync();
    }

    public static EntityPaperBombCast prepare(EntityPlayer player,int mode){
        if(player.world.isRemote||!PaperBombPolicy.valid(mode))return null;
        World world=player.world;
        long active=world.getEntities(EntityPaperBombCast.class,e->!e.isDead&&player.getUniqueID().equals(e.owner)&&e.mode==mode).size();
        if(active>=(mode==PaperBombPolicy.CIRCUIT?2:1))return fail(player,"paper_busy");
        EntityPaperBombCast cast=new EntityPaperBombCast(world);cast.owner=player.getUniqueID();cast.mode=mode;cast.direction=player.getLookVec().normalize();
        Vec3d eye=player.getPositionEyes(1);
        cast.setPosition(eye.x,eye.y,eye.z);
        if(mode==PaperBombPolicy.SWARM){
            RayTraceResult hit=cast.trace(eye,eye.add(cast.direction.scale(24)),player);
            if(hit==null||!(hit.entityHit instanceof EntityLivingBase))return fail(player,"paper_target");
            cast.target=(EntityLivingBase)hit.entityHit;
        }
        if(mode==PaperBombPolicy.CIRCUIT||mode==PaperBombPolicy.BREACH){
            RayTraceResult hit=PaperBombPlacement.aim(world,eye,player.getPositionVector(),cast.direction,player.rotationYaw,mode==PaperBombPolicy.CIRCUIT);
            if(hit==null||hit.sideHit==null||(mode==PaperBombPolicy.CIRCUIT&&hit.sideHit!=EnumFacing.UP))return fail(player,"paper_ground");
            if(!cast.editable(player,hit.getBlockPos(),hit.sideHit))return fail(player,"paper_ground");
            Vec3d center=hit.hitVec.add(new Vec3d(hit.sideHit.getDirectionVec()).scale(.035));
            if(mode==PaperBombPolicy.BREACH){
                // Wall seals burst out of the wall, ground seals forward across the ground.
                cast.direction=hit.sideHit.getAxis().isHorizontal()?new Vec3d(hit.sideHit.getDirectionVec()):Vec3d.fromPitchYaw(0,player.rotationYaw);
            }
            cast.setPosition(center.x,center.y,center.z);
            int count=mode==PaperBombPolicy.CIRCUIT?4:1;
            for(int i=0;i<count;i++){
                Vec3d pos=mode==PaperBombPolicy.CIRCUIT?center.addVector(i%2==0?-2:2,0,i<2?-2:2):center;
                RayTraceResult surface=mode==PaperBombPolicy.CIRCUIT?PaperBombPlacement.ground(world,pos):hit;
                if(surface==null)return fail(player,"paper_ground");
                BlockPos floor=surface.getBlockPos();
                if(!cast.editable(player,floor,surface.sideHit))return fail(player,"paper_ground");
                pos=surface.hitVec.add(new Vec3d(surface.sideHit.getDirectionVec()).scale(.035));
                Tag tag=new Tag(pos,Vec3d.ZERO);tag.face=hit.sideHit;tag.support=floor;tag.fuse=mode==PaperBombPolicy.BREACH?40:-1;cast.tags.add(tag);
            }
            int radius=mode==PaperBombPolicy.CIRCUIT?2:1;
            BlockPos origin=new BlockPos(center);
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)for(int dy=-1;dy<=2;dy++){
                BlockPos grass=origin.add(dx,dy,dz);
                if(world.isBlockLoaded(grass)&&PaperBombPlacement.grass(world,grass))cast.placementGrass.add(grass);
            }
        }else{
            int count=PaperBombPolicy.bombs(mode);
            for(int i=0;i<count;i++){
                float spread=mode==PaperBombPolicy.VOLLEY?(i-1)*9:(i-(count-1)/2f)*6;
                Vec3d velocity=Vec3d.fromPitchYaw(player.rotationPitch,player.rotationYaw+spread).scale(mode==PaperBombPolicy.VOLLEY?.95:.65);
                // Start at the eyes: the first swept segment checks walls instead of spawning through them.
                cast.tags.add(new Tag(eye,velocity));
            }
        }
        cast.sync();return cast;
    }
    private static EntityPaperBombCast fail(EntityPlayer player,String key){player.sendStatusMessage(new TextComponentTranslation("message.narutomod."+key),true);return null;}
    private boolean editable(EntityPlayer player,BlockPos pos,EnumFacing face){
        return world.isBlockLoaded(pos)&&world.isBlockModifiable(player,pos)&&player.canPlayerEdit(pos,face,player.getHeldItemMainhand());
    }
    /** Called only after successful spawn and ammunition debit. Never harvests crops, flowers or ground blocks. */
    public void clearPlacementGrass(EntityPlayer player){
        if(world.isRemote)return;
        Set<BlockPos> remove=new LinkedHashSet<>();
        Set<BlockPos> examined=new HashSet<>();
        for(BlockPos pos:placementGrass){
            if(examined.contains(pos)||!PaperBombPlacement.grass(world,pos)||!editable(player,pos,EnumFacing.UP))continue;
            Set<BlockPos> plant=new LinkedHashSet<>();plant.add(pos);
            if(world.getBlockState(pos).getBlock()==net.minecraft.init.Blocks.DOUBLE_PLANT){
                BlockPos other=world.getBlockState(pos).getValue(net.minecraft.block.BlockDoublePlant.HALF)==net.minecraft.block.BlockDoublePlant.EnumBlockHalf.UPPER?pos.down():pos.up();
                if(world.isBlockLoaded(other)&&PaperBombPlacement.grass(world,other))plant.add(other);
            }
            examined.addAll(plant);
            boolean allowed=true;
            for(BlockPos part:plant)if(!editable(player,part,EnumFacing.UP)||net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.event.world.BlockEvent.BreakEvent(world,part,world.getBlockState(part),player))){allowed=false;break;}
            if(allowed)remove.addAll(plant);
        }
        // Remove both halves before neighbor notifications, so tall grass never drops seeds/items.
        List<BlockPos> validated=new ArrayList<>();
        for(BlockPos pos:remove)if(PaperBombPlacement.grass(world,pos))validated.add(pos);
        for(BlockPos pos:validated)world.setBlockState(pos,net.minecraft.init.Blocks.AIR.getDefaultState(),2);
        for(BlockPos pos:remove)world.notifyNeighborsOfStateChange(pos,net.minecraft.init.Blocks.AIR,false);
        placementGrass.clear();
    }
    public void paperSound(){
        SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("narutomod:paperflip"));
        if(sound!=null)playSound(sound,.8f,1.1f);
        if(mode==PaperBombPolicy.BREACH)playSound(SoundEvents.ENTITY_TNT_PRIMED,.8f,.9f);
    }

    public static boolean eligible(EntityPlayer caster,EntityLivingBase victim){
        if(victim==caster||!ItemJutsu.canTarget(victim)||caster.isOnSameTeam(victim)||victim.isOnSameTeam(caster)||SusanooCombat.isOwnSusanoo(victim,caster))return false;
        if(victim instanceof EntitySummonAnimal.ISummon){EntityLivingBase summoner=((EntitySummonAnimal.ISummon)victim).getSummoner();
            if(summoner==caster||(summoner!=null&&caster.isOnSameTeam(summoner)))return false;}
        if(victim instanceof EntityTameable){EntityLivingBase owner=((EntityTameable)victim).getOwner();if(owner==caster||(owner!=null&&caster.isOnSameTeam(owner)))return false;}
        if(victim instanceof EntityPlayer){EntityPlayer player=(EntityPlayer)victim;
            return !player.capabilities.isCreativeMode&&!player.isSpectator()&&caster.canAttackPlayer(player)
                &&caster.getServer()!=null&&caster.getServer().isPVPEnabled();}
        return true;
    }
    private boolean clear(Vec3d start,Vec3d end){return world.rayTraceBlocks(start,end,false,true,false)==null;}
    private RayTraceResult trace(Vec3d start,Vec3d end,EntityPlayer caster){
        RayTraceResult result=world.rayTraceBlocks(start,end,false,true,false);
        double nearest=result==null?start.squareDistanceTo(end):start.squareDistanceTo(result.hitVec);
        for(EntityLivingBase victim:world.getEntitiesWithinAABB(EntityLivingBase.class,new AxisAlignedBB(start,end).grow(.4))){
            if(!eligible(caster,victim)||!victim.canBeCollidedWith())continue;
            AxisAlignedBB box=victim.getEntityBoundingBox().grow(.15);
            RayTraceResult hit=box.calculateIntercept(start,end);
            Vec3d point=box.contains(start)?start:hit==null?null:hit.hitVec;
            if(point!=null&&start.squareDistanceTo(point)<nearest){nearest=start.squareDistanceTo(point);result=new RayTraceResult(victim,point);}
        }
        return result;
    }
    private void fly(Tag tag,EntityPlayer caster){
        if(age>80||tag.pos.distanceTo(getPositionVector())>28){tag.spent=true;return;}
        if(mode==PaperBombPolicy.SWARM&&!tag.lostTarget){
            if(target==null||!eligible(caster,target)||!clear(tag.pos,target.getPositionEyes(1)))tag.lostTarget=true;
            else tag.velocity=tag.velocity.scale(.86).add(target.getPositionEyes(1).subtract(tag.pos).normalize().scale(.14*.65)).normalize().scale(.65);
        }else if(mode==PaperBombPolicy.VOLLEY||tag.lostTarget)tag.velocity=tag.velocity.addVector(0,-.016,0);
        Vec3d next=tag.pos.add(tag.velocity);
        if(!world.isBlockLoaded(new BlockPos(next))){tag.spent=true;return;}
        RayTraceResult hit=trace(tag.pos,next,caster);
        if(hit==null)tag.pos=next;
        else{
            tag.pos=hit.hitVec;
            if(hit.entityHit instanceof EntityLivingBase){tag.attached=(EntityLivingBase)hit.entityHit;tag.offset=tag.pos.subtract(tag.attached.getPositionVector());}
            else{tag.face=hit.sideHit;tag.support=hit.getBlockPos();tag.pos=tag.pos.add(new Vec3d(tag.face.getDirectionVec()).scale(.035));}
            tag.fuse=21;playAt(tag.pos,SoundEvents.ENTITY_TNT_PRIMED,.35f,1.3f);
        }
        if(age%5==0)particles(EnumParticleTypes.CRIT,tag.pos,1,.02,.01);
    }
    private void explode(Tag tag,EntityPlayer caster){
        double radius=mode==PaperBombPolicy.BREACH?5:3.5;
        Vec3d center=tag.pos;
        particles(EnumParticleTypes.EXPLOSION_LARGE,center,2,.15,0);
        particles(EnumParticleTypes.SMOKE_LARGE,center,12,.45,.07);
        particles(EnumParticleTypes.FLAME,center,10,.3,.08);
        particles(EnumParticleTypes.CRIT,center,14,.4,.15);
        if(world instanceof WorldServer)((WorldServer)world).spawnParticle(EnumParticleTypes.ITEM_CRACK,center.x,center.y,center.z,8,.3,.3,.3,.12,
            debrisArguments(net.minecraft.item.Item.getIdFromItem(net.minecraft.item.Item.getItemFromBlock(net.narutomod.block.BlockExplosiveTag.block))));
        playAt(center,SoundEvents.ENTITY_GENERIC_EXPLODE,.9f,.9f+rand.nextFloat()*.3f);
        for(EntityLivingBase victim:world.getEntitiesWithinAABB(EntityLivingBase.class,new AxisAlignedBB(center,center).grow(radius))){
            if(!eligible(caster,victim))continue;
            Vec3d point=victim.getPositionVector().addVector(0,victim.height*.5,0),delta=point.subtract(center);
            double distance=delta.lengthVector();
            if(distance>=radius||(mode==PaperBombPolicy.BREACH&&!PaperBombPolicy.inCone(direction.dotProduct(delta.normalize()))))continue;
            // Attached tags can start just inside the hitbox; solid cover still always shields other victims.
            if(!clear(center,point))continue;
            int previous=hits.getOrDefault(victim.getUniqueID(),0);
            float damage=PaperBombPolicy.damage(mode,previous,distance);
            hits.put(victim.getUniqueID(),previous+1);
            Burst burst=pending.get(victim);
            if(burst==null){burst=new Burst(center);pending.put(victim,burst);}
            burst.damage+=damage;
        }
    }
    // 1.12.2 ITEM_CRACK packets require both item ID and metadata, even for metadata zero.
    public static int[] debrisArguments(int itemId){return new int[]{itemId,0};}
    private void applyBurst(EntityLivingBase victim,Burst burst,EntityPlayer caster){
        // Simultaneous tags form one attenuated hit. Never reset vanilla or another jutsu's i-frames.
        DamageSource source=new EntityDamageSourceIndirect(ItemJutsu.NINJUTSU_TYPE,this,caster){
            @Override public Vec3d getDamageLocation(){return burst.center;}
        }.setExplosion();
        if(victim.attackEntityFrom(source,burst.damage)){
            Vec3d delta=victim.getPositionVector().subtract(burst.center);
            victim.knockBack(this,mode==PaperBombPolicy.BREACH?1.1f:.25f,-delta.x,-delta.z);
        }
    }
    private static final class Burst {final Vec3d center;float damage;Burst(Vec3d center){this.center=center;}}
    private void playAt(Vec3d at,SoundEvent sound,float volume,float pitch){world.playSound(null,at.x,at.y,at.z,sound,SoundCategory.PLAYERS,volume,pitch);}
    private void particles(EnumParticleTypes type,Vec3d at,int count,double spread,double speed){if(world instanceof WorldServer)((WorldServer)world).spawnParticle(type,at.x,at.y,at.z,count,spread,spread,spread,speed);}
    private void fizzle(){for(Tag tag:tags)if(!tag.spent)particles(EnumParticleTypes.SMOKE_NORMAL,tag.pos,3,.08,.015);setDead();}
    private void sync(){
        NBTTagCompound nbt=new NBTTagCompound();nbt.setInteger("Mode",mode);nbt.setInteger("Age",age);nbt.setBoolean("Triggered",triggered);
        NBTTagList list=new NBTTagList();
        for(Tag tag:tags){NBTTagCompound n=new NBTTagCompound();n.setDouble("X",tag.pos.x-posX);n.setDouble("Y",tag.pos.y-posY);n.setDouble("Z",tag.pos.z-posZ);
            n.setDouble("VX",tag.velocity.x);n.setDouble("VY",tag.velocity.y);n.setDouble("VZ",tag.velocity.z);
            n.setInteger("Face",tag.face==null?-1:tag.face.getIndex());n.setInteger("Fuse",tag.fuse);n.setBoolean("Stuck",tag.face!=null||tag.attached!=null);n.setBoolean("Spent",tag.spent);list.appendTag(n);}
        nbt.setTag("Tags",list);dataManager.set(FRAME,nbt);
    }
    private static final class Tag {
        Vec3d pos,velocity,offset;EnumFacing face;BlockPos support;EntityLivingBase attached;int fuse=-1;boolean spent,lostTarget;
        Tag(Vec3d pos,Vec3d velocity){this.pos=pos;this.velocity=velocity;}
    }
}
