package net.narutomod.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.narutomod.MedicalSurgery;
import net.narutomod.NarutomodMod;
import net.narutomod.OcularPolicy;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Medical dossier. Choices are previews until the server accepts the procedure. */
@SideOnly(Side.CLIENT)
public final class GuiOcularSurgery extends GuiScreen {
    private static final int WIDTH = 596, HEIGHT = 378, PAGE_SIZE = 5;
    private static final int INK = 0xFF293F3C, MUTED = 0xFF63716A;
    private static final int JADE = 0xFF285F53, GOLD = 0xFF9A7540, RED = 0xFF943D32;
    private static final int PAPER = 0xFFEDE3CA, LIGHT = 0xFFF6EEDC, RULE = 0xFFC2BDA5;
    private final UUID session;
    private final String patient;
    private final boolean self;
    private final boolean consent;
    private final String surgeon;
    private final List<EyeOption> options = new ArrayList<>();
    private final int originalLeft, originalRight;
    private int left, right, selectedSocket, page;
    private int originX, originY;
    private float scale = 1f;
    private boolean submitted, reviewed;

    public GuiOcularSurgery(NBTTagCompound data) {
        session = data.getUniqueId("Session");
        patient = data.getString("Patient");
        self = data.getBoolean("Self");
        consent = data.getBoolean("Consent");
        surgeon = data.getString("Surgeon");
        NBTTagList labels = data.getTagList("Options", 8);
        NBTTagList details = data.getTagList("Details", 10);
        for (int i = 0; i < Math.min(160, labels.tagCount()); i++) {
            options.add(new EyeOption(labels.getStringTagAt(i), details.getCompoundTagAt(i), i));
        }
        if (options.isEmpty()) options.add(new EyeOption("", new NBTTagCompound(), 0));
        originalLeft = left = clamp(data.getInteger("Left"));
        originalRight = right = clamp(data.getInteger("Right"));
    }

    private int clamp(int value) { return Math.max(0, Math.min(value, options.size() - 1)); }
    private int choice() { return selectedSocket == 0 ? left : right; }
    private int otherChoice() { return selectedSocket == 0 ? right : left; }
    private int pages() { return (options.size() + PAGE_SIZE - 1) / PAGE_SIZE; }
    private boolean changed() { return left != originalLeft || right != originalRight; }
    private boolean sameIdentity(int a, int b) {
        return a != 0 && b != 0 && options.get(a).identity != null && options.get(a).identity.equals(options.get(b).identity);
    }
    private boolean valid() { return OcularPolicy.validLayout(left, right, options.size()) && !sameIdentity(left, right) && changed(); }
    private static String tr(String key, Object... args) { return I18n.format("gui.narutomod.surgery." + key, args); }

    @Override
    public void initGui() {
        scale = Math.max(0.1f, Math.min(1f, Math.min((width - 12f) / WIDTH, (height - 12f) / HEIGHT)));
        originX = Math.round((width - WIDTH * scale) / 2f);
        originY = Math.round((height - HEIGHT * scale) / 2f);
        buttonList.clear();
        buttonList.add(new DossierButton(0, 18, 78, 172, 126, "", 0));
        buttonList.add(new DossierButton(1, 202, 78, 172, 126, "", 0));
        if (!consent) {
            for (int row = 0; row < PAGE_SIZE; row++) buttonList.add(new DossierButton(10 + row, 390, 94 + row * 30, 188, 29, "", 0));
            buttonList.add(new DossierButton(20, 390, 249, 28, 20, "<", 1));
            buttonList.add(new DossierButton(21, 550, 249, 28, 20, ">", 1));
            buttonList.add(new DossierButton(31, 130, 340, 116, 24, tr("reset"), 1));
        }
        buttonList.add(new DossierButton(30, 18, 340, consent ? 228 : 104, 24, tr(consent ? "decline" : "cancel"), 1));
        buttonList.add(new DossierButton(32, 350, 340, 228, 24, tr(consent ? "approve" : self ? "confirm_self" : "request_approval"), 2));
        refreshButtons();
    }

    private void refreshButtons() {
        for (GuiButton button : buttonList) {
            if (button.id >= 10 && button.id < 15) {
                int index = page * PAGE_SIZE + button.id - 10;
                button.visible = index < options.size();
                button.enabled = button.visible && (index == 0 || index != otherChoice() && !sameIdentity(index, otherChoice()));
            } else if (button.id == 20) button.enabled = page > 0;
            else if (button.id == 21) button.enabled = page + 1 < pages();
            else if (button.id == 31) button.enabled = changed();
            else if (button.id == 32) {
                button.enabled = consent || valid();
                button.displayString = tr(consent ? "approve" : self ? "confirm_self" : "request_approval");
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (consent) {
            if (button.id == 30 || button.id == 32) {
                submitted = true;
                NarutomodMod.PACKET_HANDLER.sendToServer(new MedicalSurgery.ConsentMessage(session, button.id == 32));
                mc.displayGuiScreen(null);
            }
            return;
        }
        if (button.id < 2) {
            selectedSocket = button.id;
            page = choice() / PAGE_SIZE;
        } else if (button.id >= 10 && button.id < 15) {
            int value = page * PAGE_SIZE + button.id - 10;
            if (value >= options.size() || value != 0 && (value == otherChoice() || sameIdentity(value, otherChoice()))) return;
            if (selectedSocket == 0) left = value; else right = value;
            reviewed = false;
        } else if (button.id == 20) page--;
        else if (button.id == 21) page++;
        else if (button.id == 31) {
            left = originalLeft; right = originalRight; page = choice() / PAGE_SIZE; reviewed = false;
        } else if (button.id == 30) {
            submitted = true;
            NarutomodMod.PACKET_HANDLER.sendToServer(new MedicalSurgery.ChoiceMessage(session, left, right, true));
            mc.displayGuiScreen(null);
        } else if (button.id == 32 && valid()) {
            submitted = true;
            NarutomodMod.PACKET_HANDLER.sendToServer(new MedicalSurgery.ChoiceMessage(session, left, right, false));
            mc.displayGuiScreen(null);
        }
        refreshButtons();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(localX(mouseX), localY(mouseY), mouseButton);
    }
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(localX(mouseX), localY(mouseY), state);
    }
    private int localX(int value) { return (int)Math.floor((value - originX) / scale); }
    private int localY(int value) { return (int)Math.floor((value - originY) / scale); }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void onGuiClosed() {
        if (!submitted) {
            if (consent) NarutomodMod.PACKET_HANDLER.sendToServer(new MedicalSurgery.ConsentMessage(session, false));
            else NarutomodMod.PACKET_HANDLER.sendToServer(new MedicalSurgery.ChoiceMessage(session, 0, 0, true));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int mx = localX(mouseX), my = localY(mouseY);
        GlStateManager.pushMatrix();
        GlStateManager.translate(originX, originY, 0);
        GlStateManager.scale(scale, scale, 1);
        drawDossier();
        drawSocket(0, 18, mx, my);
        drawSocket(1, 202, mx, my);
        if (consent) drawConsentSummary(); else drawOptions(mx, my);
        drawPreview();
        refreshButtons();
        super.drawScreen(mx, my, partialTicks);
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawDossier() {
        drawRect(-4, 4, WIDTH + 5, HEIGHT + 6, 0x66000000);
        drawRect(0, 0, WIDTH, HEIGHT, INK);
        drawRect(3, 3, WIDTH - 3, HEIGHT - 3, PAPER);
        drawRect(3, 3, WIDTH - 3, 56, JADE);
        drawRect(3, 56, WIDTH - 3, 59, GOLD);
        // Ruled paper continues the visual language of the existing stats dossier.
        for (int y = 63; y < HEIGHT - 4; y += 8) drawRect(5, y, WIDTH - 5, y + 1, 0x05725C3C);
        drawRect(16, 16, 34, 34, 0xFFB3C9AD);
        drawRect(23, 19, 27, 31, JADE);
        drawRect(19, 23, 31, 27, JADE);
        text(tr("division"), 43, 14, 0xFFBBD2BF);
        GlStateManager.pushMatrix();
        GlStateManager.translate(42, 29, 0);
        GlStateManager.scale(1.45f, 1.45f, 1);
        text(tr(consent ? "approval_title" : "title"), 0, 0, 0xFFF5EDD6);
        GlStateManager.popMatrix();
        text(tr("patient"), 414, 16, 0xFFBBD2BF);
        text(fit(patient, 158), 414, 31, 0xFFF5EDD6);
        text(tr(consent ? "proposed_layout" : "select_socket"), 18, 66, JADE);
        text(tr(consent ? "approval" : "choose_eye"), 390, 66, JADE);
        drawRect(382, 78, 383, 275, RULE);
        text(fit(tr(consent ? "consent_warning" : "inventory_hint"), 560), 18, 274, consent ? RED : MUTED);
        drawRect(18, 284, 578, 285, RULE);
        metric(18, tr("cost"), tr("chakra", (int)OcularPolicy.CHAKRA_COST));
        metric(210, tr("procedure"), tr("seconds", OcularPolicy.CHANNEL_TICKS / 20));
        metric(402, tr("recovery"), tr("seconds", OcularPolicy.RECOVERY_TICKS / 20));
        text(tr("escape"), 263, 349, MUTED);
        text(tr("anatomical"), 18, 367, MUTED);
        rightText(tr("xp", OcularPolicy.REQUIRED_HEALING_XP), 578, 367, MUTED);
    }

    private void drawSocket(int socket, int x, int mx, int my) {
        EyeOption eye = options.get(socket == 0 ? left : right);
        boolean selected = socket == selectedSocket;
        boolean hover = mx >= x && mx < x + 172 && my >= 78 && my < 204;
        int border = selected ? JADE : hover ? GOLD : RULE;
        drawRect(x, 78, x + 172, 204, border);
        drawRect(x + 1, 79, x + 171, 203, LIGHT);
        drawRect(x + 1, 79, x + 171, 100, selected ? JADE : 0xFFDBDDC8);
        text(tr(socket == 0 ? "left" : "right"), x + 10, 85, selected ? LIGHT : INK);
        rightText(consent ? "[=]" : selected ? "[+]" : "[ ]", x + 161, 85, selected && !consent ? LIGHT : MUTED);
        drawRect(x + 57, 105, x + 115, 151, 0xFFE2D9BF);
        drawRect(x + 60, 108, x + 112, 148, 0xFFF9F3E5);
        drawEye(eye, x + 70, 112, 32);
        centered(fit(eye.name(), 156), x + 86, 157, eye.empty ? RED : INK);
        centered(tr(eye.empty ? "no_sight" : eye.foreign ? "transplanted" : "native"), x + 86, 171, eye.empty ? RED : MUTED);
        String condition = eye.blinded ? "blinded" : eye.lockedActive ? "cover_required" : eye.kind.equals("normal") ? "natural_sight" : eye.empty ? "choose_replacement" : "toggle_available";
        centered(fit(tr(condition), 156), x + 86, 188, eye.lockedActive || eye.blinded ? RED : JADE);
    }

    private void drawOptions(int mx, int my) {
        text(tr(selectedSocket == 0 ? "assign_left" : "assign_right"), 390, 81, MUTED);
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = page * PAGE_SIZE + row, y = 94 + row * 30;
            if (index >= options.size()) break;
            EyeOption eye = options.get(index);
            boolean current = index == choice(), assigned = index != 0 && index == otherChoice();
            boolean hover = mx >= 390 && mx < 578 && my >= y && my < y + 29;
            drawRect(390, y, 578, y + 29, current ? JADE : hover && !assigned ? 0xFFD5DDC6 : 0xFFE3DDC5);
            if (current) drawRect(390, y, 393, y + 29, GOLD);
            drawEye(eye, 399, y + 5, 18);
            text(fit(eye.name(), 143), 425, y + 5, current ? LIGHT : assigned ? MUTED : INK);
            String source = assigned ? tr("assigned_other") : eye.empty ? tr("remove_eye") : tr("source_" + eye.source);
            text(fit(source, 143), 425, y + 17, current ? 0xFFC6D8C5 : MUTED);
        }
        centered(tr("page", page + 1, pages()), 484, 255, MUTED);
    }

    private void drawConsentSummary() {
        text(tr("requested_by"), 398, 91, MUTED);
        text(fit(surgeon, 168), 398, 106, INK);
        drawRect(398, 125, 562, 126, RULE);
        fontRenderer.drawSplitString(tr("patient_review"), 398, 140, 168, INK);
        fontRenderer.drawSplitString(tr("approval_locked"), 398, 205, 168, MUTED);
    }

    private void drawPreview() {
        text(tr("preview"), 18, 216, JADE);
        EyeOption a = options.get(left), b = options.get(right);
        text(tr("left_short"), 18, 233, MUTED);
        text(fit(tr("skill_" + a.skill), 286), 64, 233, INK);
        text(tr("right_short"), 18, 247, MUTED);
        text(fit(tr("skill_" + b.skill), 286), 64, 247, INK);
        text(fit(tr("upkeep", format(a.upkeep + b.upkeep)), 355), 18, 265, GOLD);
        String status;
        if (consent) status = tr("consent_status");
        else if (!OcularPolicy.validLayout(left, right, options.size()) || sameIdentity(left, right)) status = tr("duplicate");
        else if (!changed()) status = tr("unchanged");
        else if (left == 0 && right == 0) status = tr("blind_warning");
        else if (left == 0 || right == 0) status = tr("empty_warning");
        else status = tr(self ? "reviewed_self" : "reviewed_other");
        int statusColor = changed() && (left == 0 || right == 0) ? RED : consent ? JADE : MUTED;
        drawRect(18, 314, 578, 334, changed() && (left == 0 || right == 0) ? 0x1A943D32 : 0x12285F53);
        fontRenderer.drawSplitString(status, 24, 318, 548, statusColor);
    }

    private void metric(int x, String label, String value) {
        text(label, x, 288, MUTED);
        text(value, x, 301, INK);
        if (x < 402) drawRect(x + 178, 289, x + 179, 309, RULE);
    }
    private static String format(double n) { return n == (int)n ? Integer.toString((int)n) : String.format(java.util.Locale.ROOT, "%.1f", n); }
    private void text(String value, int x, int y, int color) { fontRenderer.drawString(value, x, y, color); }
    private void centered(String value, int x, int y, int color) { text(value, x - fontRenderer.getStringWidth(value) / 2, y, color); }
    private void rightText(String value, int x, int y, int color) { text(value, x - fontRenderer.getStringWidth(value), y, color); }
    private String fit(String value, int max) { return fontRenderer.getStringWidth(value) <= max ? value : fontRenderer.trimStringToWidth(value, max - 9) + "..."; }

    private void drawEye(EyeOption eye, int x, int y, int size) {
        if (eye.empty) {
            drawRect(x + 1, y + size / 2 - 1, x + size - 1, y + size / 2 + 1, MUTED);
            return;
        }
        mc.getTextureManager().bindTexture(eye.texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
        GlStateManager.disableBlend();
    }

    private static final class EyeOption {
        final boolean empty, foreign, lockedActive, blinded;
        final String label, kind, source, skill;
        final UUID identity;
        final int donorSide;
        final double upkeep;
        final ResourceLocation texture;
        EyeOption(String fallback, NBTTagCompound detail, int index) {
            empty = index == 0;
            label = detail.hasKey("Name", 8) ? detail.getString("Name") : fallback.replaceFirst("^(Current: |Inventory #[0-9]+: )", "").replaceFirst(" \\[donor.*$", "");
            kind = detail.hasKey("Kind", 8) ? detail.getString("Kind") : empty ? "empty" : "normal";
            source = detail.hasKey("Source", 8) ? detail.getString("Source") : "installed";
            skill = detail.hasKey("Skill", 8) ? detail.getString("Skill") : empty ? "none" : "sight";
            donorSide = detail.getInteger("DonorSide");
            foreign = detail.getBoolean("Foreign");
            lockedActive = detail.getBoolean("LockedActive");
            blinded = detail.getBoolean("Blinded");
            upkeep = Math.max(0, detail.getDouble("Upkeep"));
            identity = detail.hasUniqueId("Identity") ? detail.getUniqueId("Identity") : null;
            String resource = detail.getString("Texture");
            texture = new ResourceLocation(resource.startsWith("narutomod:textures/blocks/") && resource.endsWith(".png")
                ? resource : "narutomod:textures/blocks/normal_eyes.png");
        }
        String name() { return empty ? tr("empty") : label; }
    }

    private static final class DossierButton extends GuiButton {
        private final int style;
        DossierButton(int id, int x, int y, int width, int height, String text, int style) {
            super(id, x, y, width, height, text); this.style = style;
        }
        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            if (style == 0) return;
            int fill = !enabled ? 0xFFD8D5BF : style == 2 ? hovered ? 0xFF387E68 : JADE : hovered ? 0xFFD5DDC6 : LIGHT;
            drawRect(x, y, x + width, y + height, enabled ? JADE : RULE);
            drawRect(x + 1, y + 1, x + width - 1, y + height - 1, fill);
            int color = !enabled ? MUTED : style == 2 ? LIGHT : INK;
            mc.fontRenderer.drawString(displayString, x + (width - mc.fontRenderer.getStringWidth(displayString)) / 2, y + (height - 8) / 2, color);
        }
    }
}
