package net.narutomod;

import java.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.narutomod.MadaraTemporalController.EffectMessage;

/** Executable boundary checks; live collision and rider behavior also require a two-client play test. */
public final class MadaraTemporalChecks {
    private static int checks;
    private static void check(boolean value,String label) {checks++;if(!value)throw new AssertionError(label);}
    private static TemporalHistory.Frame frame(long tick,double x) {return new TemporalHistory.Frame(tick,x,64,0,90,5);}
    public static void main(String[] args) {
        check(MadaraTemporalSettings.MS_HISTORY_SECONDS==2 && MadaraTemporalSettings.EMS_HISTORY_SECONDS==4,"default MS/EMS history budgets");
        check(MadaraTemporalSettings.MS_ANCHOR_SECONDS==4 && MadaraTemporalSettings.EMS_ANCHOR_SECONDS==6,"default anchor lifetimes");
        check(MadaraTemporalSettings.MS_COOLDOWN_SECONDS==45 && MadaraTemporalSettings.EMS_COOLDOWN_SECONDS==35,"default cooldowns");
        check(MadaraTemporalSettings.MS_RETURN_DISTANCE==8 && MadaraTemporalSettings.EMS_RETURN_DISTANCE==12,"default spatial budgets");
        TemporalHistory ms=new TemporalHistory(40),ems=new TemporalHistory(80);
        for(int tick=0;tick<=120;tick++){ms.add(frame(tick,tick*.04));ems.add(frame(tick,tick*.04));}
        check(ms.size()==41 && ms.oldest().tick==80,"MS retains exactly two seconds including endpoints");
        check(ems.size()==81 && ems.oldest().tick==40,"EMS retains exactly four seconds including endpoints");
        check(ms.usable(120,121,8),"last pre-expiry tick usable");
        check(!ms.usable(120,120,8),"exact anchor expiry denied");
        check(!ms.usable(120,121,1),"distance beyond limit denied");
        ms.add(frame(120,999));ms.add(frame(100,999));
        check(ms.newest().x==4.8,"same-tick and out-of-order samples cannot replace history");
        ms.add(new TemporalHistory.Frame(121,Double.NaN,64,0,0,0));
        check(ms.size()==41 && ms.newest().tick==120,"non-finite recording rejected");
        TemporalHistory shortHistory=new TemporalHistory(40);
        shortHistory.add(frame(0,0));shortHistory.add(frame(5,8));
        check(!shortHistory.usable(5,80,8),"six-tick preparation minimum");
        shortHistory.add(frame(6,8));
        check(shortHistory.usable(6,80,8),"inclusive eight-block return limit");
        shortHistory.add(frame(7,8.0001));
        check(!shortHistory.usable(7,80,8),"strictly above distance rejected");
        List<TemporalHistory.Frame> path=ems.reverseSamples(16);
        check(path.size()==16,"bounded effect packet");
        check(path.get(0)==ems.newest() && path.get(15)==ems.oldest(),"reverse path retains both endpoints");
        for(int i=1;i<path.size();i++)check(path.get(i).tick<path.get(i-1).tick,"reverse chronology");
        TemporalHistory empty=new TemporalHistory(40);
        check(empty.reverseSamples(16).isEmpty() && !empty.usable(0,80,8),"empty history safe");
        empty.add(frame(1,1));check(empty.reverseSamples(16).size()==1,"single-frame visual safe");
        for(java.lang.reflect.Field field:TemporalHistory.Frame.class.getDeclaredFields()) {
            check(Arrays.asList("tick","x","y","z","yaw","pitch").contains(field.getName()),"record cannot carry inventory, health, chakra, cooldowns or world state");
        }
        UUID caster=UUID.fromString("6fc9a2c6-4240-4c6d-813a-41d50107400d");
        ByteBuf buffer=Unpooled.buffer();
        try {
            new EffectMessage(caster,0,1,40,path).toBytes(buffer);
            check(buffer.readableBytes()==24+16*32,"small fixed-format effect packet");
            EffectMessage copy=new EffectMessage();copy.fromBytes(buffer);
            check(copy.caster.equals(caster) && copy.frames.size()==16 && copy.kind==1,"caster identity and trail survive packet round-trip");
            check(copy.frames.get(15).x==ems.oldest().x,"destination preserved without lossy coordinate conversion");
        } finally {buffer.release();}
        buffer=Unpooled.buffer();
        try {
            new EffectMessage(caster,0,2,0,Collections.emptyList()).toBytes(buffer);
            EffectMessage clear=new EffectMessage();clear.fromBytes(buffer);check(clear.kind==2 && clear.frames.isEmpty(),"clear packet valid");
        } finally {buffer.release();}
        buffer=Unpooled.buffer();
        try {
            new EffectMessage(caster,0,1,40,path).toBytes(buffer);buffer.setByte(23,21);
            boolean rejected=false;try{new EffectMessage().fromBytes(buffer);}catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"oversized trail rejected before allocation");
        } finally {buffer.release();}
        buffer=Unpooled.buffer();
        try {
            new EffectMessage(caster,0,1,40,Collections.singletonList(new TemporalHistory.Frame(0,Double.NaN,0,0,0,0))).toBytes(buffer);
            boolean rejected=false;try{new EffectMessage().fromBytes(buffer);}catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"non-finite packet positions rejected");
        } finally {buffer.release();}
        System.out.println("PASS: "+checks+" Madara temporal boundary, snapshot and packet checks. Live collision/rider checks remain manual.");
    }
}
