package net.narutomod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Explicitly retired item IDs only. Other missing content still gets Forge's normal warning. */
@Mod.EventBusSubscriber(modid = "narutomod")
public final class RemovedCursedSeals {
    private static final Set<String> RETIRED = new HashSet<>(Arrays.asList(
        "curse_mark_heaven", "curse_mark_earth", "curse_mark_jirobo", "curse_mark_kidomaru", "curse_mark_tayuya",
        "curse_mark_sakon_ukon", "curse_mark_animal", "curse_mark_prisoners", "curse_mark_guren_team", "curse_mark_iburi"));
    public static boolean retired(String namespace, String path) { return "narutomod".equals(namespace) && RETIRED.contains(path); }
    @SubscribeEvent public static void missing(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (retired(mapping.key.getResourceDomain(), mapping.key.getResourcePath())) mapping.ignore();
        }
    }
}
