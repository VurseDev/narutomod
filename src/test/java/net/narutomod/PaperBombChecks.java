package net.narutomod;

import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import net.minecraft.item.*;
import net.minecraft.init.Bootstrap;

/** Regression executable covering actual ammunition mutation, fixed budgets and packaged resources. */
public final class PaperBombChecks {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        check(!PaperBombPolicy.valid(-1)&&!PaperBombPolicy.valid(4),"reject invalid modes");
        int[] bombs={3,4,6,5},cooldowns={160,360,440,320};double[] chakra={60,100,160,130};
        for(int mode=0;mode<4;mode++){
            check(PaperBombPolicy.bombs(mode)==bombs[mode],"fixed ammunition "+mode);
            check(PaperBombPolicy.cooldown(mode)==cooldowns[mode],"fixed cooldown "+mode);
            check(PaperBombPolicy.chakra(mode)==chakra[mode],"base chakra "+mode);
            check(PaperBombPolicy.damage(mode,1,0)<PaperBombPolicy.damage(mode,0,0),"second tag attenuated");
            check(PaperBombPolicy.damage(mode,2,0)<PaperBombPolicy.damage(mode,1,0),"later tags attenuated");
            float last=Float.MAX_VALUE;
            for(int step=0;step<=100;step++){
                float damage=PaperBombPolicy.damage(mode,0,step*.1);
                check(damage>=0&&damage<=last,"monotone nonnegative blast falloff");last=damage;
            }
        }
        check(PaperBombPolicy.inCone(1)&&PaperBombPolicy.inCone(.5)&&!PaperBombPolicy.inCone(.499)&&!PaperBombPolicy.inCone(-1),"cone front/edge/back");
        Bootstrap.register();
        // Exercise the actual wire codec used by multiplayer and ReplayMod, not just particle rendering.
        net.minecraft.network.PacketBuffer packetBuffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
        try {
            int itemId=Item.getIdFromItem(net.minecraft.init.Items.PAPER);
            net.minecraft.network.play.server.SPacketParticles sent=new net.minecraft.network.play.server.SPacketParticles(
                net.minecraft.util.EnumParticleTypes.ITEM_CRACK,false,1,2,3,.3f,.3f,.3f,.12f,8,
                net.narutomod.entity.EntityPaperBombCast.debrisArguments(itemId));
            sent.writePacketData(packetBuffer);
            net.minecraft.network.play.server.SPacketParticles received=new net.minecraft.network.play.server.SPacketParticles();
            received.readPacketData(packetBuffer);
            check(received.getParticleArgs().length==net.minecraft.util.EnumParticleTypes.ITEM_CRACK.getArgumentCount(),"complete debris particle payload");
            check(received.getParticleArgs()[0]==itemId&&received.getParticleArgs()[1]==0,"debris item ID and metadata roundtrip");
            check(received.getParticleCount()==8&&packetBuffer.readableBytes()==0,"complete explosion packet consumed");
        } finally {packetBuffer.release();}
        Item bomb=net.minecraft.init.Items.PAPER,unrelated=net.minecraft.init.Items.STICK;
        for(int amount:bombs){
            ItemStack a=new ItemStack(bomb,2),b=new ItemStack(bomb,4),other=new ItemStack(unrelated,9);
            List<ItemStack> slots=Arrays.asList(a,other,ItemStack.EMPTY,b);
            check(PaperBombAmmo.count(slots,bomb)==6,"split-stack count");
            check(!PaperBombAmmo.consume(slots,bomb,7),"shortage fails");
            check(a.getCount()==2&&b.getCount()==4&&other.getCount()==9,"failure leaves every slot unchanged");
            check(PaperBombAmmo.consume(slots,bomb,amount),"valid debit");
            check(PaperBombAmmo.count(slots,bomb)==6-amount,"exact debit across slots");
            check(other.getCount()==9,"never consume unrelated items");
        }
        List<ItemStack> one=Arrays.asList(new ItemStack(bomb,6));
        check(!PaperBombAmmo.consume(one,bomb,-1)&&!PaperBombAmmo.consume(one,bomb,0),"invalid debit rejected");
        check(PaperBombAmmo.consume(one,bomb,6)&&!PaperBombAmmo.consume(one,bomb,6),"cannot debit same supply twice");
        Path assets=Paths.get("src/main/resources/assets/narutomod");
        String[] names={"explosive_art","scroll_tag_volley","scroll_snare_circuit","scroll_seeking_tag_swarm","scroll_breaching_seal"};
        for(String name:names){
            JsonObject model=new JsonParser().parse(new String(Files.readAllBytes(assets.resolve("models/item/"+name+".json")),StandardCharsets.UTF_8)).getAsJsonObject();
            for(Map.Entry<String,JsonElement> entry:model.getAsJsonObject("textures").entrySet())
                check(Files.exists(assets.resolve("textures/"+entry.getValue().getAsString().split(":")[1]+".png")),"texture exists "+name);
            String parent=model.get("parent").getAsString();
            if(parent.startsWith("narutomod:"))check(Files.exists(assets.resolve("models/"+parent.substring(10)+".json")),"model parent exists "+name);
            for(String lang:Arrays.asList("en_us","pt_br")){
                String text=new String(Files.readAllBytes(assets.resolve("lang/"+lang+".lang")),StandardCharsets.UTF_8);
                check(text.contains("item."+name+".name="),"localized item "+name+" "+lang);
            }
        }
        checks+=PaperBombRuntimeChecks.run();
        checks+=PaperBombPlacementChecks.run();
        System.out.println("Paper bomb checks passed: "+checks);
    }
}
