# Edo Tensei prototype — Minecraft Forge 1.12.2

Historical prototype notes. The current donor/DNA implementation and controls are documented in [edo_tensei_gameplay.md](edo_tensei_gameplay.md). New casts no longer register or summon random mobs.

Two separate casts are implemented. Real corpses, player skins/identities, reanimated-player abilities, immortality and command/control are deliberately not implemented yet.

## Trying it

Use a backed-up test world. The build is `build/libs/modid-1.0.jar`; replace the existing Naruto mod in a test instance, do not load two copies. This task does not overwrite the installed `.minecraft/mods` jar.

1. Get **Edo Tensei Ritual Scroll** from the existing Jutsus creative tab, or `/give @p narutomod:scroll_edo_tensei`. Learn it through the normal scroll UI. Survival uses the existing ninja/XP prerequisites; creative is useful for presentation testing, but the mod still requires chakra mode and sufficient chakra when executing casts.
2. Select **Edo Tensei: Ritual** on the regular Ninjutsu item. Aim at solid, flat ground within 8 blocks, with a clear 7 × 7 footprint and 3 blocks of headroom. Hold/release right-click as with other jutsus. If no suitable directly aimed block is found, it checks ground four blocks ahead.
3. A black ground seal draws in. A temporary test body appears, sinks into the centre, and disappears in smoke with the existing `poof` Naruto sound. At 5.5 seconds the completed ritual adds one soul and grants/updates **Summoning Souls**.
4. Hold **Summoning Souls**. Use the existing Cycle Jutsu key (default Up Arrow, selection changes on key release) to select the next soul. Sneak + Cycle selects the previous soul. Hover the item for its numbered list. Lists display eight entries at a time around the current selection, with a total count; cycling reaches every saved soul.
5. Cast with right-click on another clear patch. The existing `kuchiyosenojutsu` sound plays, the textured coffin rises with dirt and gravel sounds, its lid falls with a wood impact, and the selected test soul exits. The coffin disappears in smoke with `poof`, leaving the normal mob behind.
6. Repeat the ritual to add further souls. Each entry retains its randomly assigned test mob (villager, husk or witch) across subsequent summons. Use Normal difficulty for this prototype: hostile vanilla mobs disappear in Peaceful. They retain normal vanilla behaviour, including hostility; there are no commands or caster protections yet.

The Summoning Souls item must be earned by performing a ritual. An unbound creative-menu copy cannot cast. Items belonging to another player's UUID cannot cast or cycle, including in creative. If the item is lost, performing another ritual grants a replacement with the existing collection plus the newly completed soul.

## Timing and provisional balance

| Sequence | Timing at 20 ticks/second |
| --- | --- |
| Ritual seal draws in | 0–1 seconds |
| Test body shown | 1–4.8 seconds |
| Body sinks | 2.6–4.6 seconds |
| Ritual smoke and sound | 4.8 seconds |
| Soul awarded | 5.5 seconds |
| Coffin rises | 0–3 seconds |
| Lid falls | 3.2–4.2 seconds |
| Body exits / becomes real mob | 5–6 seconds |
| Coffin smoke and removal | 7.2–7.5 seconds |

Ritual: rank S, base chakra 180, existing 30-second custom cooldown floor. Summoning: rank B, base chakra 100, existing 12-second floor. Actual costs use the mod's existing mastery/pool balance. One presentation sequence per caster can run at a time. Souls are reusable, not consumed by summoning. There is no imposed roster-count limit, though storage and memory are naturally finite.

## Technical notes

- `EdoSoulRegistry` saves the owner-keyed roster in the overworld's `data/narutomod_edo_souls.dat`, shared across dimensions. Completed sequence UUIDs prevent duplicate awards on repeat completion. Item packets carry only the selected eight-entry window rather than the whole roster.
- `EntityEdoTensei.Sequence` owns the server timeline and saved phase. Clients receive phase/mob data; only the server awards souls and spawns the final mob. Logging out, dying, changing dimensions or moving more than 96 blocks away cancels a pending sequence without awarding an unfinished ritual. A previously released mob is not removed by that cancellation.
- The ritual does not delete terrain or consume nearby entities/items. Its body is a client-only visual placeholder.
- The coffin renderer reads all 68 original Blockbench-exported cubes with their per-face UV coordinates and the original 256 × 256 texture. Its 46 lid/decorative parts move together. The project contained no exported animation tracks, so the rise/lid/exit timing is implemented in Java.
- The ground seal is filled ribbon geometry: central black disk, uneven ink rings, three satellite glyphs and branching strokes, inspired by the supplied reference. It is not a claim to reproduce canonical lettering exactly.

## Verification

Build using Java 8 and Gradle 4.9:

```powershell
& 'C:\Users\ocaua\AppData\Local\Temp\gradle-4.9\bin\gradle.bat' -I tools/edo/verify.gradle verifyEdo build --no-daemon
```

The executable regression checks cover a 513-soul compressed NBT save/load, separate owner collections, duplicate-completion protection, persistent soul identities/types, animation endpoints and monotonic rise, original cube/UV/texture integrity, existing sounds, localization and seal bounds. It generates `build/reports/edo/ritual-seal.png` from the exact seal geometry for offline inspection.

The build and automated checks are not a live Minecraft playtest. Remaining manual checks: scroll learning and survival resource consumption; two-player ownership rejection; full-inventory reward pickup; reconnect and dimension persistence; late-joining observer animation sync; all four coffin orientations; audio/texture appearance; and blocked-exit handling. Test on a dedicated Forge server before a multiplayer release.

Reference consulted: [Narutopedia — Summoning: Impure World Reincarnation](https://naruto.fandom.com/wiki/Summoning%3A_Impure_World_Reincarnation).
