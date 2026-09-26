package net.narutomod;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.*;
import net.narutomod.item.ItemJutsu;

/** Small presentation messages. Damage, targets and resource spending stay in the jutsu callbacks. */
@ElementsNarutomodMod.ModElement.Tag
public final class JutsuVisualEffects extends ElementsNarutomodMod.ModElement {
    public static final int BOLT=0, BURST=1, RING=2, WATER=3, WIND=4, FIRE=5, CAST=6, EARTH=7, LIGHTNING_BIND=8;
    public JutsuVisualEffects(ElementsNarutomodMod elements){super(elements,1145);}
    @Override public void preInit(FMLPreInitializationEvent event){elements.addNetworkMessage(Message.Handler.class,Message.class,Side.CLIENT);}
    @Override @SideOnly(Side.CLIENT) public void init(FMLInitializationEvent event){MinecraftForge.EVENT_BUS.register(new net.narutomod.client.ClientJutsuVfx());}
    public static void bolt(World w,Vec3d a,Vec3d b,float width,int life,int color){emit(w,BOLT,a,b,width,life,color,0,0);}
    public static void burst(World w,Vec3d p,float scale,int color){emit(w,BURST,p,p,scale,16,color,0,0);}
    public static void ring(World w,Vec3d p,float radius,int life,int color){emit(w,RING,p,p,radius,life,color,0,0);}
    public static void ribbon(World w,Vec3d a,Vec3d b,float width,int life,int color,int kind){emit(w,kind,a,b,width,life,color,0,0);}
    public static void lightningBind(EntityLivingBase target,int life){
        net.minecraft.util.math.AxisAlignedBB box=target.getEntityBoundingBox();
        Vec3d center=new Vec3d((box.minX+box.maxX)*.5,box.minY,(box.minZ+box.maxZ)*.5);
        float height=FourPillarPolicy.height(box.maxY-box.minY);
        int radius=Math.round(FourPillarPolicy.radius(Math.max(box.maxX-box.minX,box.maxZ-box.minZ))*1000);
        emit(target.world,LIGHTNING_BIND,center,center,height,life,0x73CFFF,target.getEntityId(),radius);
    }
    public static int color(ItemJutsu.JutsuEnum.Type type){
        switch(type){case RAITON:return 0x73CFFF;case KATON:return 0xFF8A25;case SUITON:return 0x68CFFF;
            case DOTON:return 0xC1A170;case FUTON:return 0xCBF8E7;default:return 0x86E9EF;}
    }
    public static void cast(ItemJutsu.JutsuEnum jutsu,EntityLivingBase caster){
        if(jutsu.getType()==ItemJutsu.JutsuEnum.Type.DOTON||jutsu.getType()==ItemJutsu.JutsuEnum.Type.FUTON||jutsu.getType()==ItemJutsu.JutsuEnum.Type.SUITON)return;
        if(jutsu.getType()==ItemJutsu.JutsuEnum.Type.INTON||jutsu.getType()==ItemJutsu.JutsuEnum.Type.TAIJUTSU)return;
        emit(caster.world,CAST,caster.getPositionVector(),caster.getLookVec(),1,14,color(jutsu.getType()),caster.getEntityId(),jutsu.getType().ordinal());
    }
    /** Replaces the default forty-smoke-particles-per-tick charge only for our custom elemental techniques. */
    public static boolean charging(ItemStack stack,EntityLivingBase caster,float power){
        ItemJutsu.JutsuEnum jutsu=ItemJutsu.getCurrentJutsu(stack);
        if(jutsu==null||!jutsu.usesCustomBalance()||jutsu.getType()!=ItemJutsu.JutsuEnum.Type.RAITON)return false;
        if(!caster.world.isRemote&&jutsu.getType()==ItemJutsu.JutsuEnum.Type.RAITON){
            long now=caster.world.getTotalWorldTime(),next=caster.getEntityData().getLong("JutsuElectricChargeSoundUntil");
            if(now>=next){JutsuEffectSounds.charge(caster.world,caster.getPositionVector());caster.getEntityData().setLong("JutsuElectricChargeSoundUntil",now+36);}
        }
        // Active-item state already reaches observers; charge arcs are generated locally in the client tick handler.
        return true;
    }
    private static void emit(World w,int kind,Vec3d a,Vec3d b,float scale,int life,int color,int source,int element){
        if(w==null||!finite(a)||!finite(b)||!Float.isFinite(scale))return;
        Message m=new Message();m.kind=kind;m.dimension=w.provider.getDimension();m.from=a;m.to=b;
        m.scale=Math.max(.008f,Math.min(kind==BOLT?1f:kind==LIGHTNING_BIND?96f:12f,scale));m.life=Math.max(2,Math.min(160,life));
        m.color=color&0xFFFFFF;m.source=source;m.element=element;m.seed=w.rand.nextInt();
        if(!m.valid())return;
        if(w.isRemote)ClientBridge.receive(m);
        else {
            Vec3d mid=kind==BOLT||kind==WATER||kind==WIND||kind==EARTH?a.add(b).scale(.5):a;
            NarutomodMod.PACKET_HANDLER.sendToAllAround(m,new NetworkRegistry.TargetPoint(m.dimension,mid.x,mid.y,mid.z,96));
        }
    }
    public static boolean finite(Vec3d p){return p!=null&&Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z)
        &&Math.abs(p.x)<=30000000&&Math.abs(p.y)<=30000000&&Math.abs(p.z)<=30000000;}
    @SideOnly(Side.CLIENT) private static final class ClientBridge {
        static void receive(Message m){net.narutomod.client.ClientJutsuVfx.accept(m);}
    }
    public static final class Message implements IMessage {
        public int kind,dimension,life,color,source,element,seed;
        public float scale;
        public Vec3d from=Vec3d.ZERO,to=Vec3d.ZERO;
        public boolean valid(){return kind>=0&&kind<=LIGHTNING_BIND&&finite(from)&&finite(to)&&Float.isFinite(scale)&&scale>0&&scale<=(kind==LIGHTNING_BIND?96:12)&&life>=2&&life<=160
            &&(kind!=LIGHTNING_BIND||scale>=2&&life>=20&&(element==0||element>=2000&&element<=64000))
            &&(kind==CAST?element>=0&&element<ItemJutsu.JutsuEnum.Type.values().length:from.squareDistanceTo(to)<=96*96);}
        @Override public void toBytes(ByteBuf b){b.writeByte(kind);b.writeInt(dimension);b.writeInt(life);b.writeInt(color);b.writeInt(source);b.writeInt(element);b.writeInt(seed);b.writeFloat(scale);
            b.writeDouble(from.x);b.writeDouble(from.y);b.writeDouble(from.z);b.writeDouble(to.x);b.writeDouble(to.y);b.writeDouble(to.z);}
        @Override public void fromBytes(ByteBuf b){kind=b.readUnsignedByte();dimension=b.readInt();life=b.readInt();color=b.readInt();source=b.readInt();element=b.readInt();seed=b.readInt();scale=b.readFloat();
            from=new Vec3d(b.readDouble(),b.readDouble(),b.readDouble());to=new Vec3d(b.readDouble(),b.readDouble(),b.readDouble());}
        public static final class Handler implements IMessageHandler<Message,IMessage>{
            @Override @SideOnly(Side.CLIENT) public IMessage onMessage(Message m,MessageContext c){net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(()->{if(m.valid())ClientBridge.receive(m);});return null;}
        }
    }
}
