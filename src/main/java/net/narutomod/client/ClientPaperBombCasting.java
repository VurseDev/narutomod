package net.narutomod.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.*;
import net.narutomod.item.ItemExplosiveArt;

/** Uses synchronized active-item state, so observers see the seal sequence without extra packets. */
@SideOnly(Side.CLIENT)
public final class ClientPaperBombCasting {
    private static final java.lang.reflect.Field MAIN_MODEL=ReflectionHelper.findField(RenderLivingBase.class,"mainModel","field_77045_g");
    private final GenjutsuCastingModel standard=new GenjutsuCastingModel(false),slim=new GenjutsuCastingModel(true);
    private final Map<RenderPlayer,ModelPlayer> saved=new IdentityHashMap<>();
    @SubscribeEvent(priority=EventPriority.LOWEST) public void pose(RenderPlayerEvent.Pre event){
        EntityPlayer player=event.getEntityPlayer();
        if(!player.isHandActive()||player.getActiveItemStack().getItem()!=ItemExplosiveArt.block)return;
        ModelPlayer previous=event.getRenderer().getMainModel();
        if(previous.getClass()!=ModelPlayer.class)return; // Do not stack on an unrelated custom casting renderer.
        GenjutsuCastingModel model=player instanceof net.minecraft.client.entity.AbstractClientPlayer
            &&"slim".equals(((net.minecraft.client.entity.AbstractClientPlayer)player).getSkinType())?slim:standard;
        model.copyVisibility(previous);model.age=Math.min(16,player.getItemInUseMaxCount()+event.getPartialRenderTick());
        saved.put(event.getRenderer(),previous);set(event.getRenderer(),model);
    }
    @SubscribeEvent public void restore(RenderPlayerEvent.Post event){
        ModelPlayer model=saved.remove(event.getRenderer());if(model!=null)set(event.getRenderer(),model);
    }
    @SubscribeEvent public void end(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Map.Entry<RenderPlayer,ModelPlayer> entry:saved.entrySet())set(entry.getKey(),entry.getValue());saved.clear();
    }
    private static void set(RenderPlayer renderer,ModelPlayer model){try{MAIN_MODEL.set(renderer,model);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
}
