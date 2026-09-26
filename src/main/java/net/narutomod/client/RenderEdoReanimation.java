package net.narutomod.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.narutomod.entity.EntityEdoReanimation;

/** Saved signed profiles allow a donor skin to render even when the player is offline. */
@SideOnly(Side.CLIENT)
public final class RenderEdoReanimation extends RenderLiving<EntityEdoReanimation> {
    private final ModelPlayer standard=new ModelPlayer(0,false), slim=new ModelPlayer(0,true);
    private final Map<String,Skin> skins=new LinkedHashMap<String,Skin>(32,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,Skin> entry) {return size()>256;}
    };
    private static final class Skin {
        ResourceLocation texture;
        boolean slim;
        Skin(UUID id) {texture=DefaultPlayerSkin.getDefaultSkin(id);slim="slim".equals(DefaultPlayerSkin.getSkinType(id));}
    }
    public static void register() {RenderingRegistry.registerEntityRenderingHandler(EntityEdoReanimation.class,RenderEdoReanimation::new);}
    public RenderEdoReanimation(RenderManager manager) {super(manager,new ModelPlayer(0,false),.4f);}
    private Skin skin(EntityEdoReanimation entity) {
        NBTTagCompound soul=entity.soul();
        UUID id=soul.hasUniqueId("Player")?soul.getUniqueId("Player"):new UUID(0,0);
        NBTTagCompound profileTag=soul.getCompoundTag("Profile");
        String key=id+":"+profileTag.hashCode();
        Skin skin=skins.get(key);
        if(skin==null) {
            skin=new Skin(id);skins.put(key,skin);
            GameProfile profile=NBTUtil.readGameProfileFromNBT(profileTag);
            if(profile!=null && profile.getId()!=null && profile.getId().equals(id)
                && profile.getProperties().containsKey("textures")) {
                final Skin result=skin;
                Minecraft.getMinecraft().getSkinManager().loadProfileTextures(profile,(type,location,texture)->{
                    if(type==MinecraftProfileTexture.Type.SKIN) {
                        result.texture=location;result.slim="slim".equals(texture.getMetadata("model"));
                    }
                },true);
            }
        }
        return skin;
    }
    @Override protected ResourceLocation getEntityTexture(EntityEdoReanimation entity) {return skin(entity).texture;}
    @Override public void doRender(EntityEdoReanimation entity,double x,double y,double z,float yaw,float partial) {
        mainModel=skin(entity).slim?slim:standard;
        try {super.doRender(entity,x,y,z,yaw,partial);}
        finally {GlStateManager.color(1,1,1,1);}
    }
    @Override protected void preRenderCallback(EntityEdoReanimation entity,float partial) {
        // Slight ash tint; saved player texture and its classic/slim proportions remain intact.
        GlStateManager.color(.86f,.89f,.87f,1);
        if(entity.reformTicks()>0) {
            float restore=1-entity.reformTicks()/100f;
            GlStateManager.scale(1,.25f+.75f*restore,1);
        }
    }
}
