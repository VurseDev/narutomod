package net.narutomod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.narutomod.creativetab.TabCustomTabs;
import net.narutomod.gui.GuiScrollExtraJutsu;
import net.narutomod.item.ItemExtraJutsuScrolls;
import net.narutomod.item.ItemJutsu;
import net.narutomod.item.ItemKaton;
import net.narutomod.item.ItemNinjutsu;

/** Guards the Creative-tab/scroll route used to teach the three bloodline techniques. */
public final class BloodlineScrollChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        ElementsNarutomodMod elements = new ElementsNarutomodMod();
        new TabCustomTabs(elements).initElements();
        new ItemExtraJutsuScrolls(elements).initElements();
        check(TabCustomTabs.jutsus != null, "Custom Jutsu tab initialized");
        check(elements.getItems().size() == ItemExtraJutsuScrolls.SCROLLS.length,
            "one registered item supplier per custom scroll");
        check(GuiScrollExtraJutsu.GUIID_LAST - GuiScrollExtraJutsu.GUIID_BASE + 1
            == ItemExtraJutsuScrolls.SCROLLS.length, "GUI range covers every custom scroll");

        Set<String> names = new HashSet<>();
        for (int i = 0; i < ItemExtraJutsuScrolls.SCROLLS.length; i++) {
            ItemExtraJutsuScrolls.ScrollDef def = ItemExtraJutsuScrolls.SCROLLS[i];
            check(names.add(def.registryName), "unique scroll registry name: " + def.registryName);
            Item item = elements.getItems().get(i).get();
            // A standalone JavaExec has no active Forge mod container; the normal
            // registration event supplies the narutomod namespace at runtime.
            check(item.getRegistryName().getResourcePath().equals(def.registryName),
                "registered scroll path: " + def.registryName);
            check(item.getCreativeTab() == TabCustomTabs.jutsus,
                "scroll appears in Custom Jutsu tab: " + def.registryName);
            NonNullList<ItemStack> creativeEntries = NonNullList.create();
            item.getSubItems(TabCustomTabs.jutsus, creativeEntries);
            check(creativeEntries.size() == 1 && creativeEntries.get(0).getItem() == item,
                "scroll contributes an inventory icon to Custom Jutsu: " + def.registryName);
            check(GuiScrollExtraJutsu.handles(GuiScrollExtraJutsu.GUIID_BASE + i),
                "scroll has GUI route: " + def.registryName);
        }
        verify("scroll_twin_flame_dragons", ItemKaton.TWINFLAMEDRAGONS, ItemExtraJutsuScrolls.Kind.KATON);
        verify("scroll_flame_company", ItemKaton.FLAMECOMPANY, ItemExtraJutsuScrolls.Kind.KATON);
        verify("scroll_clone_throw", ItemNinjutsu.CLONETHROW, ItemExtraJutsuScrolls.Kind.NINJUTSU);
        Path assets = Paths.get("src/main/resources/assets/narutomod");
        for (String name : new String[] {"scroll_twin_flame_dragons", "scroll_flame_company", "scroll_clone_throw"}) {
            check(Files.exists(assets.resolve("models/item/" + name + ".json")), "scroll model: " + name);
            for (String lang : new String[] {"en_us", "pt_br"}) {
                String text = new String(Files.readAllBytes(assets.resolve("lang/" + lang + ".lang")), StandardCharsets.UTF_8);
                check(text.contains("item." + name + ".name="), "scroll localization: " + lang + "/" + name);
            }
        }
        System.out.println("Bloodline scroll registration checks passed: " + checks);
    }

    private static void verify(String name, ItemJutsu.JutsuEnum jutsu, ItemExtraJutsuScrolls.Kind kind) {
        for (ItemExtraJutsuScrolls.ScrollDef def : ItemExtraJutsuScrolls.SCROLLS) {
            if (!def.registryName.equals(name)) continue;
            check(def.jutsu == jutsu && def.kind == kind, "scroll teaches intended jutsu: " + name);
            return;
        }
        throw new AssertionError("Missing Custom Jutsu scroll: " + name);
    }
}
