package net.narutomod;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.nbt.*;
import net.narutomod.entity.EntityEdoTensei.Sequence;

/** Dependency-free regression executable; run using tools/edo/verify.gradle. */
public final class EdoPrototypeChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID(), first = UUID.randomUUID();
        EdoSoulRegistry archive = new EdoSoulRegistry();
        archive.complete(alice, first, 1);
        archive.complete(alice, first, 1);
        check(archive.souls(alice).tagCount() == 1, "duplicate completion must not award twice");
        check(archive.souls(bob).tagCount() == 0, "owner isolation");
        for (int i = 1; i < 513; i++) archive.complete(alice, UUID.randomUUID(), i % 3);
        archive.complete(bob, UUID.randomUUID(), 2);
        check(archive.souls(alice).tagCount() == 513, "roster must grow past inventory and byte-index limits");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(archive.writeToNBT(new NBTTagCompound()), bytes);
        EdoSoulRegistry restored = new EdoSoulRegistry();
        restored.readFromNBT(CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray())));
        check(restored.souls(alice).tagCount() == 513, "NBT roundtrip preserves all souls");
        check(restored.souls(bob).tagCount() == 1, "NBT roundtrip preserves separate owners");
        check(restored.souls(alice).getCompoundTagAt(0).getUniqueId("Soul").equals(first), "stable soul identity");
        check(restored.souls(alice).getCompoundTagAt(512).getString("Name").equals("Test Soul 513"), "stable numbering");
        restored.complete(alice, first, 0);
        check(restored.souls(alice).tagCount() == 513, "idempotency survives reload");
        check(restored.souls(alice).getCompoundTagAt(0).getInteger("Mob") == 1, "repeat ritual cannot change mob");

        check(Sequence.rise(0) < -2.7f, "coffin begins underground");
        check(Sequence.rise(60) == 0, "coffin is fully raised at 3 seconds");
        check(Sequence.lid(64) == 0 && Sequence.lid(84) == 1, "lid timing");
        check(Sequence.sink(52) == 0 && Sequence.sink(92) < -2, "body sinks below ground");
        float previous = Sequence.rise(0);
        for (int tick = 1; tick <= 60; tick++) {
            check(Sequence.rise(tick) >= previous, "rise must not reverse");
            previous = Sequence.rise(tick);
        }
        Path root = Paths.get("src/main/resources/assets/narutomod");
        JsonObject model;
        try (Reader r = Files.newBufferedReader(root.resolve("models/custom/edo_tensei_coffin.json"), StandardCharsets.UTF_8)) {
            model = new JsonParser().parse(r).getAsJsonObject();
        }
        int cubes=0, doors=0, faces=0;
        for (JsonElement element : model.getAsJsonArray("elements")) {
            JsonObject cube = element.getAsJsonObject(); cubes++;
            if (cube.get("name").getAsString().matches("^(lid|trim|channel|plaque|binding|fastener)_.*")) doors++;
            check(!cube.has("rotation"), "renderer assumes current unrotated exported cubes");
            for (Map.Entry<String,JsonElement> entry : cube.getAsJsonObject("faces").entrySet()) {
                faces++;
                JsonObject f = entry.getValue().getAsJsonObject();
                check(f.get("texture").getAsString().equals("#0"), "all faces use the existing coffin texture");
                for (JsonElement uv : f.getAsJsonArray("uv")) check(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 16, "UV out of bounds");
            }
        }
        check(cubes == 68 && doors > 0 && doors < cubes, "full original mesh with detachable lid");
        check(faces == cubes * 6, "all six faces retained");
        java.awt.image.BufferedImage texture = ImageIO.read(root.resolve("textures/blocks/edo_tensei_coffin.png").toFile());
        check(texture.getWidth() == 256 && texture.getHeight() == 256, "original 256x256 texture");
        try (Reader r = Files.newBufferedReader(root.resolve("sounds.json"), StandardCharsets.UTF_8)) {
            JsonObject sounds = new JsonParser().parse(r).getAsJsonObject();
            check(sounds.has("kuchiyosenojutsu") && sounds.has("poof"), "Naruto sounds exist");
        }
        for (String lang : new String[]{"en_us", "pt_br"}) {
            String contents = new String(Files.readAllBytes(root.resolve("lang/"+lang+".lang")), StandardCharsets.UTF_8);
            check(contents.contains("item.summoning_souls.name="), "localized soul item");
            check(contents.contains("item.scroll_edo_tensei.name="), "localized scroll");
        }
        java.awt.image.BufferedImage preview = new java.awt.image.BufferedImage(900,900,java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = preview.createGraphics();
        graphics.setColor(new java.awt.Color(132,155,98)); graphics.fillRect(0,0,900,900);
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new java.awt.Color(7,5,8));
        java.awt.geom.Path2D.Double completeSeal = new java.awt.geom.Path2D.Double();
        for (double[] q : new net.narutomod.client.RenderEdoTensei.SealGeometry().quads()) {
            java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
            for (int i=0;i<8;i+=2) {
                check(Math.abs(q[i]) < 3.4 && Math.abs(q[i+1]) < 3.4, "seal fits checked ground area");
                if(i==0) path.moveTo(450+q[i]*125,450+q[i+1]*125);
                else path.lineTo(450+q[i]*125,450+q[i+1]*125);
            }
            path.closePath(); completeSeal.append(path, false);
        }
        graphics.fill(completeSeal);
        graphics.dispose();
        Path output = Paths.get("build/reports/edo/ritual-seal.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(preview,"png",output.toFile());
        System.out.println("PASS: " + checks + " checks; 513-soul archive roundtrip, owner isolation, timeline, "
            + cubes + " textured cubes (" + doors + " lid parts), sounds and localization.");
    }
}
