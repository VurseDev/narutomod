package net.narutomod.client;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.item.ItemJutsu;

/** Client-only hand-sign pose for the elemental items while they are charging. */
@SideOnly(Side.CLIENT)
public final class ClientElementalCasting {
    private static final Map<Integer, Release> RELEASES = new java.util.HashMap<>();
    private static net.minecraft.world.World releaseWorld;
    private static final class Release {
        final ItemJutsu.JutsuEnum.Type type; int age;
        Release(ItemJutsu.JutsuEnum.Type type) { this.type=type; }
    }
    public static void release(int entityId, int element) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world==null || element<0 || element>=ItemJutsu.JutsuEnum.Type.values().length)return;
        if(releaseWorld!=mc.world)RELEASES.clear();
        releaseWorld=mc.world;
        RELEASES.put(entityId,new Release(ItemJutsu.JutsuEnum.Type.values()[element]));
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(releaseWorld!=mc.world){RELEASES.clear();releaseWorld=mc.world;}
        if(!mc.isGamePaused())RELEASES.values().removeIf(r->++r.age>=12);
    }
    private static final java.lang.reflect.Field MAIN_MODEL =
        ReflectionHelper.findField(RenderLivingBase.class, "mainModel", "field_77045_g");
    private final ElementalCastingModel standard = new ElementalCastingModel(false);
    private final ElementalCastingModel slim = new ElementalCastingModel(true);
    private final Map<RenderPlayer, ModelPlayer> saved = new IdentityHashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void pose(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        Release release=RELEASES.get(player.getEntityId());
        if (!player.isHandActive() && release==null) return;
        net.minecraft.item.ItemStack active = player.getActiveItemStack();
        ItemJutsu.JutsuEnum jutsu = active.getItem() instanceof ItemJutsu.Base ? ItemJutsu.getCurrentJutsu(active) : null;
        if (release==null && (jutsu == null || !jutsu.usesCustomBalance())) return;
        ItemJutsu.JutsuEnum.Type type=release!=null?release.type:jutsu.getType();
        if(type==ItemJutsu.JutsuEnum.Type.DOTON || type==ItemJutsu.JutsuEnum.Type.FUTON || type==ItemJutsu.JutsuEnum.Type.SUITON)return;
        if(type==ItemJutsu.JutsuEnum.Type.INTON || type==ItemJutsu.JutsuEnum.Type.TAIJUTSU)return;
        ModelPlayer previous = event.getRenderer().getMainModel();
        if (previous == null || previous.getClass() != ModelPlayer.class) return;
        ElementalCastingModel model = player instanceof net.minecraft.client.entity.AbstractClientPlayer
            && "slim".equals(((net.minecraft.client.entity.AbstractClientPlayer) player).getSkinType()) ? slim : standard;
        model.copyVisibility(previous);
        model.configure(Math.min(24f, player.getItemInUseMaxCount() + event.getPartialRenderTick()),
            release==null?-1f:release.age+event.getPartialRenderTick(), type, player, false);
        saved.put(event.getRenderer(), previous);
        set(event.getRenderer(), model);
    }

    @SubscribeEvent
    public void restore(RenderPlayerEvent.Post event) {
        ModelPlayer model = saved.remove(event.getRenderer());
        if (model != null) set(event.getRenderer(), model);
    }

    @SubscribeEvent
    public void restoreCanceled(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Map.Entry<RenderPlayer, ModelPlayer> entry : saved.entrySet()) set(entry.getKey(), entry.getValue());
        saved.clear();
    }

    private static void set(RenderPlayer renderer, ModelPlayer model) {
        try { MAIN_MODEL.set(renderer, model); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
}
