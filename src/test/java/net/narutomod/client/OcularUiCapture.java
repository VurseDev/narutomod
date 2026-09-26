package net.narutomod.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.client.resources.data.TextureMetadataSectionSerializer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import sun.misc.Unsafe;

/**
 * Development-only visual fixture. Renders the production GuiScreen through
 * Minecraft's actual FontRenderer, TextureManager and LWJGL in an offscreen
 * OpenGL context. It does not start a world, connect to a server, or submit UI
 * choices. This is renderer/layout evidence, not a live multiplayer test.
 */
public final class OcularUiCapture {
    private static final int BUFFER_WIDTH = 1600;
    private static final int BUFFER_HEIGHT = 1000;

    private OcularUiCapture() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected project directory, Minecraft client jar, output directory");
        }
        File project = new File(args[0]);
        File output = new File(args[2]);
        if (!output.isDirectory() && !output.mkdirs()) {
            throw new IllegalStateException("Cannot create capture directory: " + output);
        }
        Pbuffer context = new Pbuffer(BUFFER_WIDTH, BUFFER_HEIGHT, new PixelFormat(), null, null);
        try {
            context.makeCurrent();
            Minecraft minecraft = prepareMinecraft(project, new File(args[1]));
            OpenGlHelper.initializeTextures();
            System.out.println("Ocular UI capture renderer: " + GL11.glGetString(GL11.GL_RENDERER));
            capture(minecraft, fixture(1, 2, false), 1440, 900, 2, false, output, "01-overview");
            capture(minecraft, fixture(3, 4, false), 1440, 900, 2, true, output, "02-selected-eyes");
            capture(minecraft, fixture(1, 2, true), 1280, 800, 2, false, output, "03-self-surgery");
            capture(minecraft, fixture(0, 0, false), 1440, 900, 2, true, output, "04-empty-sockets");
            capture(minecraft, fixture(3, 4, false), 960, 540, 2, true, output, "05-compact");
            NBTTagCompound approval=fixture(3,4,false);approval.setBoolean("Consent",true);approval.setString("Surgeon","Tsunade Senju");
            capture(minecraft, approval, 1440, 900, 2, false, output, "06-patient-approval");
            System.out.println("Captured actual GuiOcularSurgery renderer (isolated fixture, no live world): " + output.getAbsolutePath());
        } finally {
            context.destroy();
        }
    }

    private static Minecraft prepareMinecraft(File project, File clientJar) throws Exception {
        Minecraft minecraft = (Minecraft) unsafe().allocateInstance(Minecraft.class);
        setField(Minecraft.class, null, "instance", minecraft);
        minecraft.gameSettings = new GameSettings();
        minecraft.gameSettings.guiScale = 2;
        MetadataSerializer metadata = new MetadataSerializer();
        metadata.registerMetadataSectionType(new TextureMetadataSectionSerializer(), TextureMetadataSection.class);
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(metadata);
        resources.reloadResourcePack(new FileResourcePack(clientJar));
        resources.reloadResourcePack(new FolderResourcePack(new File(project, "src/main/resources")));
        setField(Minecraft.class, minecraft, "mcResourceManager", resources);
        LanguageManager languages = new LanguageManager(metadata, "en_us");
        languages.onResourceManagerReload(resources);
        setField(Minecraft.class, minecraft, "mcLanguageManager", languages);
        minecraft.renderEngine = new TextureManager(resources);
        minecraft.fontRenderer = new FontRenderer(minecraft.gameSettings,
                new ResourceLocation("textures/font/ascii.png"), minecraft.renderEngine, false);
        minecraft.fontRenderer.onResourceManagerReload(resources);
        return minecraft;
    }

    private static NBTTagCompound fixture(int left, int right, boolean self) {
        NBTTagCompound data = new NBTTagCompound();
        data.setUniqueId("Session", UUID.fromString("36edaf04-092f-4000-8000-000000000001"));
        data.setString("Patient", "Kakashi Hatake");
        data.setUniqueId("PatientId", UUID.fromString("36edaf04-092f-4000-8000-000000000002"));
        data.setBoolean("NativeUchiha", false);
        data.setInteger("Left", left);
        data.setInteger("Right", right);
        data.setBoolean("Self", self);
        NBTTagList options = new NBTTagList();
        for (String label : new String[] { "Empty socket", "Current left: Sharingan (3 tomoe)",
                "Current right: Normal eye", "Inventory: Sharingan (3 tomoe) - left",
                "Inventory: Byakugan - right", "Inventory: Normal eye - left" }) {
            options.appendTag(new NBTTagString(label));
        }
        data.setTag("Options", options);
        NBTTagList details = new NBTTagList();
        details.appendTag(detail("Empty socket", "empty", "empty", -1, false, 0, "", "none"));
        details.appendTag(detail("Sharingan / 3 tomoe", "sharingan", "installed", 0, true, 1.0,
                "sharingan_3_tomoe", "copy"));
        details.appendTag(detail("Normal eye", "normal", "installed", 1, false, 0,
                "normal_eyes_black", "sight"));
        details.appendTag(detail("Sharingan / 3 tomoe", "sharingan", "patient", 0, true, 1.0,
                "sharingan_3_tomoe", "copy"));
        details.appendTag(detail("Byakugan", "byakugan", "medic", 1, true, 0.5,
                "byakugan", "vision"));
        details.appendTag(detail("Normal eye / amber", "normal", "inventory", 0, true, 0,
                "normal_eyes_amber", "sight"));
        data.setTag("Details", details);
        return data;
    }

    private static NBTTagCompound detail(String name, String kind, String source, int donorSide,
            boolean foreign, double upkeep, String texture, String skill) {
        NBTTagCompound detail = new NBTTagCompound();
        detail.setString("Name", name);
        detail.setString("Kind", kind);
        detail.setString("Source", source);
        detail.setInteger("DonorSide", donorSide);
        detail.setBoolean("Foreign", foreign);
        detail.setDouble("Upkeep", upkeep);
        detail.setBoolean("LockedActive", "sharingan".equals(kind));
        detail.setString("Texture", texture.isEmpty() ? "" : "narutomod:textures/blocks/" + texture + ".png");
        detail.setString("Skill", skill);
        detail.setBoolean("Blinded", false);
        if (!"empty".equals(kind)) detail.setUniqueId("Identity",UUID.nameUUIDFromBytes((name+source+donorSide).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return detail;
    }

    private static void capture(Minecraft minecraft, NBTTagCompound data, int pixelWidth, int pixelHeight,
            int scale, boolean reviewed, File output, String name) throws Exception {
        minecraft.displayWidth = pixelWidth;
        minecraft.displayHeight = pixelHeight;
        NBTTagCompound initial = data.copy();
        if (!data.getBoolean("Consent")) {
            initial.setInteger("Left", 1);
            initial.setInteger("Right", 2);
        }
        GuiOcularSurgery screen = new GuiOcularSurgery(initial);
        minecraft.currentScreen = screen;
        screen.setWorldAndResolution(minecraft, pixelWidth / scale, pixelHeight / scale);
        if (!data.getBoolean("Consent")) {
            setField(GuiOcularSurgery.class, screen, "left", data.getInteger("Left"));
            setField(GuiOcularSurgery.class, screen, "right", data.getInteger("Right"));
        }
        if (reviewed) setField(GuiOcularSurgery.class, screen, "reviewed", true);
        GlStateManager.viewport(0, 0, pixelWidth, pixelHeight);
        GlStateManager.clearColor(0.018f, 0.025f, 0.032f, 1.0f);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, screen.width, screen.height, 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0f, 0.0f, -2000.0f);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.color(1, 1, 1, 1);
        screen.drawScreen(-100, -100, 0.0f);
        GL11.glFinish();
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            throw new IllegalStateException("OpenGL error after drawing " + name + ": " + error);
        }
        ByteBuffer pixels = BufferUtils.createByteBuffer(pixelWidth * pixelHeight * 4);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, pixelWidth, pixelHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < pixelHeight; y++) {
            for (int x = 0; x < pixelWidth; x++) {
                int offset = (y * pixelWidth + x) * 4;
                int red = pixels.get(offset) & 255;
                int green = pixels.get(offset + 1) & 255;
                int blue = pixels.get(offset + 2) & 255;
                image.setRGB(x, pixelHeight - y - 1, red << 16 | green << 8 | blue);
            }
        }
        File target = new File(output, name + ".png");
        ImageIO.write(image, "png", target);
        System.out.println(target.getAbsolutePath() + " (" + screen.width + "x" + screen.height
                + " GUI units, " + scale + "x scale)");
        // Never call onGuiClosed(): production close handlers send cancellation packets.
        minecraft.currentScreen = null;
    }

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
