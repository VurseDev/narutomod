# Custom jutsu, stable IDs, and asset map

Source audit: 2026-09-27, Forge 1.12.2 / Java 8, working tree based on `f828586`. This describes the actual working-tree implementation, including the uncommitted stat changes. It is a documentation handoff, not authorization to activate parked features or apply an older balance proposal. A release JAR is a separate snapshot; do not assume its contents match a dirty source tree.

## Scope and compatibility

The catalog below contains 48 active custom techniques in the requested elemental, Ninjutsu, Inton, Taijutsu and Inuzuka items, plus the four Explosive Art techniques referenced by the extra-scroll catalog: **52 techniques**. It excludes the original techniques preceding those custom indices and is not an inventory of every ocular, summoning or clan ability in the whole mod. For example, [ItemSummoningSouls.SUMMON](../src/main/java/net/narutomod/item/ItemSummoningSouls.java) is a separate active item technique at index 0; it is not the Ninjutsu Edo ritual.

An index here is the `ItemJutsu.JutsuEnum` index within its owning item, not a global ID, entity ID or GUI ID. Preserve item registry names, these indices, enum type ordering, existing NBT keys and scroll array order. The listed custom enums use `withCustomBalance()`; this does not mean all original techniques do. The common charge, resource, XP and cooldown pipeline is [ItemJutsu](../src/main/java/net/narutomod/item/ItemJutsu.java).

## Active technique catalog

Callback abbreviations in the tables:

- `Extra` = [ItemExtraJutsu](../src/main/java/net/narutomod/item/ItemExtraJutsu.java).
- `Canonical` = [ItemCanonicalJutsu](../src/main/java/net/narutomod/item/ItemCanonicalJutsu.java).
- `Bloodline` = [BloodlineTechniques](../src/main/java/net/narutomod/item/BloodlineTechniques.java).
- Unqualified callback names are nested in the owning item source linked above the table.

### Wind Release — [ItemFuton](../src/main/java/net/narutomod/item/ItemFuton.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 6 | `BEASTTEARINGGALEPALM` | Beast Tearing Gale Palm | `Canonical.BeastTearingGalePalm` |
| 7 | `FLOWERSCATTERINGDANCE` | Flower Scattering Dance | `Canonical.FlowerScatteringDance` |

### Lightning Release — [ItemRaiton](../src/main/java/net/narutomod/item/ItemRaiton.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 6 | `LIGHTNINGCLONE` | Lightning Clone | `Extra.LightningCloneJutsu` |
| 7 | `CHIDORISENBON` | Chidori Senbon | `Extra.ChidoriSenbonJutsu` |
| 8 | `WAVEINSPIRATION` | Wave of Inspiration | `Canonical.WaveOfInspiration` |
| 9 | `FOURPILLARBIND` | Four-Pillar Bind | `Canonical.FourPillarBind` |

Index 10, `CHIDORI_RAIKIRI`, is retired and excluded from the active item list. See the compatibility details below; its retained source and registry entries must not be mistaken for an active technique.

### Earth Release — [ItemDoton](../src/main/java/net/narutomod/item/ItemDoton.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 6 | `RETSUDOTENSHO` | Retsudo Tensho | `Extra.RetsudoTenshoJutsu` |
| 7 | `MUDWOLVES` | Mud Wolves | `Canonical.MudWolves` |
| 8 | `EARTHFLOWWAVE` | Earth Flow Wave | `Canonical.EarthFlowWave` |

### Water Release — [ItemSuiton](../src/main/java/net/narutomod/item/ItemSuiton.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 8 | `WATERCLONE` | Water Clone | `Extra.WaterCloneJutsu` |
| 9 | `MIZUAMENABARA` | Mizuame Nabara | `Extra.StickySyrupJutsu` |
| 10 | `WATERWALL` | Water Wall | `Extra.WaterWallJutsu` |
| 11 | `WATERPRISONTRAP` | Water Prison Trap | `Extra.WaterPrisonTrapJutsu` |
| 12 | `WATERWHIP` | Water Whip | `Canonical.WaterWhip` |
| 13 | `HIDINGINWATER` | Hiding in Water Technique | `Canonical.HidingInWater` |
| 14 | `WATERMIRROR` | Water Mirror Technique | `Canonical.WaterMirror` |
| 15 | `WATERBLADE` | Water Blade Technique | `Canonical.WaterBlade` |
| 16 | `EXPLOSIVEBUBBLES` | Bubbles Technique | `Canonical.ExplosiveBubbles` |
| 17 | `WATERFORMATIONPILLAR` | Water Formation Pillar | `Canonical.WaterFormationPillar` |

### Fire Release — [ItemKaton](../src/main/java/net/narutomod/item/ItemKaton.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 6 | `FIREPHOENIX` | Fire Phoenix | `ItemKaton.EntityFirePhoenix.Jutsu` |
| 7 | `HOUSENKA` | Housenka no Jutsu | `Extra.HousenkaJutsu(false)` |
| 8 | `HOUSENKATSUMABENI` | Housenka Tsumabeni | `Extra.HousenkaJutsu(true)` |
| 9 | `FLAMEWHIRLWIND` | Flame Whirlwind | `Canonical.FlameWhirlwind` |
| 10 | `TWINFLAMEDRAGONS` | Twin Flame Dragons | `Bloodline.TwinFlameDragons` |
| 11 | `FLAMECOMPANY` | Flame Company | `Bloodline.FlameCompanyJutsu` |

### Ninjutsu — [ItemNinjutsu](../src/main/java/net/narutomod/item/ItemNinjutsu.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 12 | `CROWCLONE` | Crow Clone Technique / Crow Escape scroll | `Extra.CrowCloneJutsu` |
| 13 | `CROWTRAPCLONE` | Crow Trap Clone | `Extra.CrowTrapCloneJutsu` |
| 14 | `EXPLOSIVECLONE` | Explosive Clone | `Extra.ExplosiveCloneJutsu`, delegating to `EntityExplosiveClone.EC.Jutsu` |
| 15 | `SHURIKENSHADOWCLONE` | Shuriken Shadow Clone | `Extra.ShurikenShadowCloneJutsu` |
| 16 | `FIRERASENGAN` | Fire Rasengan | `EntityRasengan.EC.FireJutsu` |
| 17 | `SENSORIAL` | Sensorial Jutsu | `SensorialJutsu` |
| 18 | `CHAKRAPULSE` | Chakra Pulse | `ChakraPulse` |
| 19 | `EDOTENSEI` | Edo Tensei: Ritual | `ItemSummoningSouls.Ritual` |
| 20 | `CLONETHROW` | Shadow Clone Throw | `Bloodline.CloneThrow` |

### Yin Release — [ItemInton](../src/main/java/net/narutomod/item/ItemInton.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 3 | `FALSE_OPENING` | False Opening | `FalseOpening` |
| 4 | `MEMORY_FRACTURE` | Memory Fracture | `SharinganGenjutsu(1)` |
| 5 | `MURDER_INTENT` | Murder Intent | `SharinganGenjutsu(2)` |
| 6 | `ILLUSIONARY_EXECUTION` | Illusionary Execution | `SharinganGenjutsu(3)` |
| 7 | `BURNING_COFFIN` | Burning Coffin | `SharinganGenjutsu(4)` |

Their private visions are client-rendered scenes, not new dimensions. Preserve [GenjutsuSession](../src/main/java/net/narutomod/GenjutsuSession.java), cleanup and the real victim's presence for other players. See [genjutsu presentation notes](genjutsu_anime_refresh.md).

### Taijutsu — [ItemTaijutsu](../src/main/java/net/narutomod/item/ItemTaijutsu.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 0 | `LEAF_WHIRLWIND` | Leaf Whirlwind | `StrikeJutsu("leaf_whirlwind", ...)` |
| 1 | `LEAF_HURRICANE` | Leaf Hurricane | `HurricaneJutsu` |
| 2 | `DYNAMIC_ENTRY` | Dynamic Entry | `DynamicEntryJutsu` |
| 3 | `PRIMARY_LOTUS` | Primary Lotus | `PrimaryLotusJutsu` |
| 4 | `LION_COMBO` | Lion Combo | `LionComboJutsu` |
| 5 | `PEREGRINE_FALCON_DROP` | Peregrine Falcon Drop | `PeregrineFalconDropJutsu` |
| 6 | `DRUNKEN_FIST` | Drunken Fist | `DrunkenFistJutsu` |
| 7 | `LEAF_DROP` | Leaf Drop | `LeafDropJutsu` |

These eight techniques remain active with the existing stamina-mode behavior. The item registers its `CombatHooks` and [CinematicTaijutsu](../src/main/java/net/narutomod/item/CinematicTaijutsu.java). Full server-driven cinematic sequences apply to Dynamic Entry, Primary Lotus and Lion Combo. Their client render skill IDs are 1, 2 and 3; brief pose IDs are Whirlwind 4, Hurricane 5, Falcon Drop 6 and Leaf Drop 7. Drunken Fist has its own buff/hit behavior. These render IDs are separate from technique indices.

### Inuzuka — [ItemInuzuka](../src/main/java/net/narutomod/item/ItemInuzuka.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 0 | `NINKEN` | Ninken Companion | `NinkenJutsu` |

### Explosive Art — [ItemExplosiveArt](../src/main/java/net/narutomod/item/ItemExplosiveArt.java)

| Index | Symbol | Technique | Callback |
| ---: | --- | --- | --- |
| 0 | `VOLLEY` | Tag Volley | `Cast(0)` |
| 1 | `CIRCUIT` | Snare Circuit | `Cast(1)` |
| 2 | `SWARM` | Seeking Tag Swarm | `Cast(2)` |
| 3 | `BREACH` | Breaching Seal | `Cast(3)` |

These consume actual explosive-tag inventory ammunition as well as chakra; preserve failed-spawn handling and no-terrain-damage policy. See [paper bomb mechanics](paper_bomb_jutsu.md).

## Canonical callback behavior verified in source

These are concise descriptions, not promises that every target/filter edge case has been playtested.

| Callback | Implemented behavior |
| --- | --- |
| `WaveOfInspiration` | Aimed lightning hit with up to four distinct chained targets, declining damage per jump and short paralysis. |
| `FourPillarBind` | Aimed accepted hit followed by a target-sized four-pillar visual and finite paralysis; rejected damage does not apply the restraint. |
| `WaterWhip` | Aimed damage, slowness and pull toward the caster. |
| `HidingInWater` | Requires water nearby; applies temporary invisibility, water breathing and speed. |
| `WaterMirror` | Sets a 45-tick, one-charge mirror state plus temporary resistance; damage handling is in `Canonical.Hooks`. |
| `WaterBlade` | Normal short-range slash or longer-range attack while sneaking, with separate damage/cooldown values. |
| `ExplosiveBubbles` | Spawns up to eight timed bubble entities, distributing targets among up to three enemies. |
| `WaterFormationPillar` | Extinguishes caster, adds resistance/fire resistance, and pushes nearby enemies outward. |
| `FlameWhirlwind` | Nearby fire damage, burning and outward knockback. |
| `BeastTearingGalePalm` | Aimed wind damage and strong directional knockback. |
| `FlowerScatteringDance` | Area damage, brief blindness and upward motion. |
| `MudWolves` | Up to three target damage/slow/weakness effects; this callback does not summon persistent wolf NPCs. |
| `EarthFlowWave` | Eight forward sample positions applying damage/knockback and moving the caster forward. |

The earth/wind/water presentation is the currently retained implementation. Do not reapply reverted visual work merely because a proposal or old build contains it.

## Extra learning scrolls: preserve all 29 positions

[ItemExtraJutsuScrolls.SCROLLS](../src/main/java/net/narutomod/item/ItemExtraJutsuScrolls.java) is a stable ordered table. [GuiScrollExtraJutsu](../src/main/java/net/narutomod/gui/GuiScrollExtraJutsu.java) uses `GUIID_BASE = 9300`, with `guiID = 9300 + scrollIndex`, currently through 9328. Scroll indices are not the technique indices above. Registry names below are in namespace `narutomod`.

| Scroll index | Registry path | Target symbol |
| ---: | --- | --- |
| 0 | `scroll_fire_rasengan` | `ItemNinjutsu.FIRERASENGAN` |
| 1 | `scroll_fire_phoenix` | `ItemKaton.FIREPHOENIX` |
| 2 | `scroll_housenka` | `ItemKaton.HOUSENKA` |
| 3 | `scroll_housenka_tsumabeni` | `ItemKaton.HOUSENKATSUMABENI` |
| 4 | `scroll_water_clone` | `ItemSuiton.WATERCLONE` |
| 5 | `scroll_water_prison_trap` | `ItemSuiton.WATERPRISONTRAP` |
| 6 | `scroll_mizuame_nabara` | `ItemSuiton.MIZUAMENABARA` |
| 7 | `scroll_water_wall` | `ItemSuiton.WATERWALL` |
| 8 | `scroll_crow_escape` | `ItemNinjutsu.CROWCLONE` |
| 9 | `scroll_crow_trap_clone` | `ItemNinjutsu.CROWTRAPCLONE` |
| 10 | `scroll_explosive_clone` | `ItemNinjutsu.EXPLOSIVECLONE` |
| 11 | `scroll_shuriken_shadow_clone` | `ItemNinjutsu.SHURIKENSHADOWCLONE` |
| 12 | `scroll_sensorial_jutsu` | `ItemNinjutsu.SENSORIAL` |
| 13 | `scroll_lightning_clone` | `ItemRaiton.LIGHTNINGCLONE` |
| 14 | `scroll_chidori_senbon` | `ItemRaiton.CHIDORISENBON` |
| 15 | `scroll_retsudo_tensho` | `ItemDoton.RETSUDOTENSHO` |
| 16 | `scroll_false_opening` | `ItemInton.FALSE_OPENING` |
| 17 | `scroll_memory_fracture` | `ItemInton.MEMORY_FRACTURE` |
| 18 | `scroll_murder_intent` | `ItemInton.MURDER_INTENT` |
| 19 | `scroll_illusionary_execution` | `ItemInton.ILLUSIONARY_EXECUTION` |
| 20 | `scroll_burning_coffin` | `ItemInton.BURNING_COFFIN` |
| 21 | `scroll_edo_tensei` | `ItemNinjutsu.EDOTENSEI` |
| 22 | `scroll_tag_volley` | `ItemExplosiveArt.VOLLEY` |
| 23 | `scroll_snare_circuit` | `ItemExplosiveArt.CIRCUIT` |
| 24 | `scroll_seeking_tag_swarm` | `ItemExplosiveArt.SWARM` |
| 25 | `scroll_breaching_seal` | `ItemExplosiveArt.BREACH` |
| 26 | `scroll_twin_flame_dragons` | `ItemKaton.TWINFLAMEDRAGONS` |
| 27 | `scroll_flame_company` | `ItemKaton.FLAMECOMPANY` |
| 28 | `scroll_clone_throw` | `ItemNinjutsu.CLONETHROW` |

This array is not the whole active technique catalog. For example, the canonical additions and Chakra Pulse have no entry in this table. Models for these registered scroll items live under `src/main/resources/assets/narutomod/models/item/`; their GUI icon textures are release-type icons or the explosive-tag texture, as declared in each `ScrollDef`.

## Supporting entity IDs

All registry paths below use the `narutomod` namespace. Keep these independent from GUI IDs even when the numbers overlap.

| Source | Numeric ID and registry path |
| --- | --- |
| `ItemExtraJutsu` | 9310 `water_clone`; 9311 `lightning_clone`; 9312 `guided_shadow_shuriken`; 9313 `guided_chidori_senbon`; 9314 `crow_trap_clone`; 9315 `fire_dragon_head`; 9316 `water_prison_trap_clone`; 9317 `water_prison_trap`; 9318 `explosive_water_bubble` |
| `ItemInuzuka` | 9320 `ninken` |
| `BloodlineTechniques` | 9381 `twin_flame_dragon`; 9382 `flame_company`; 9383 `flame_company_bolt`; 9384 `thrown_clone` |
| Retired `EntityChidoriRaikiri` compatibility registration | 516 `chidori_raikiri` |

## Current bloodline/stat delta versus the earlier v3 handoff

The [bloodline impact notes](bloodline_impact_fixes.md) describe the earlier `bloodlines-impact-v3-test.jar`. They remain useful for collision, visuals and survival behavior, but current uncommitted source adds a Chakra-stat multiplier to the shared **fire blast** helper and changes the three bloodline cooldown setter calls. Do not use the earlier release's testing history as proof that the dirty source has been tested or installed.

Let `p` be internal cast power, `m` be mastery clamped to 0..1, and `d/r` be the closest-hitbox distance divided by blast radius. The normal falloff is `f = 1 - 0.4*clamp(d/r,0,1)` (1 at center, 0.6 at edge). For a player owner and enabled [StatsPolicy](../src/main/java/net/narutomod/StatsPolicy.java), `g = 1 + 0.10*(max(ChakraStat,0)/250)^0.6`; for a nonplayer or disabled stat policy, `g = 1`. The mastery multiplier is captured at cast time; `g` is read when the fire blast resolves.

| Technique | Actual current damage before target defenses/hooks | Base chakra | Callback power / delay | Normal custom-pipeline cooldown |
| --- | --- | ---: | --- | --- |
| Twin Flame Dragons | Each dragon: `(8 + 10*p)*(1 + 0.25*m)*f*g` | 140, A | 1..2.8 / 24 | 360 ticks (18 s), from raw 280 plus A floor |
| Flame Company | Each bolt: `(6 + 7*p)*(1 + 0.25*m)*f*g`; three ammunition charges | 110, B | 1..2 / 24 | 420 ticks (21 s), above B floor |
| Shadow Clone Throw | `(6 + 7*p)*(1 + 0.25*m)` on its accepted direct hit | 45, B | 1..2 / 24 | 240 ticks (12 s), from raw 180 plus B floor |
| Four-Pillar Bind | `(6 + 12*c)*(1 + 0.35*sqrt(clamp(XP,0,100000)/100000))*(1 + 0.25*m)`, `c=clamp(p-1,0,1)` | 140, A | 1..2 / 32 | 520 ticks (26 s) |

Delay values are callback inputs to the existing training/chakra charging modifier, not guaranteed wall-clock charge duration. Available chakra can lower the reachable power below the callback cap. These caps and base costs did not change in the inspected item diff. Four-Pillar's formula remains in [FourPillarPolicy](../src/main/java/net/narutomod/FourPillarPolicy.java), with damage 6..30.375 before defenses, bind duration 60..110 ticks, and the previously established target-sized geometry. It does **not** receive `g`.

The shared `BloodlineTechniques.blast` currently has the only production call to `PlayerStats.getJutsuDamageMultiplier`. Consequently Twin Dragons and Company receive `g`, but Clone Throw, Four-Pillar and the rest of the custom jutsu do not acquire a universal damage multiplier through this change. Do not document the proposed universal hook or Twin damage `10 + 7*p` as implemented.

The three bloodline cooldown calls previously used `setCurrentJutsuCooldown(stack, caster, ticks)`, multiplying their raw duration by the item's chakra/training modifier before the rank floor. Current calls use the unscaled `setCurrentJutsuCooldown(stack, ticks)` with the same 280/420/180 raw durations. Other callbacks have not been universally converted by this diff.

The custom resource formula itself is unchanged, extracted into `StatsPolicy.customResourceCost`. For base cost `B`, maximum resource `M`, mastery `m` and power `p`:

```text
fixed = B*(1 - 0.35*m)
raw = (fixed + max(0,M-500)*poolRatio*(1 - 0.80*m))*max(1,p)
cap = max(0,M)*(noviceCap + (masterCap-noviceCap)*m)
cost = max(fixed, min(raw, max(fixed,cap)))
```

| Rank | Pool ratio | Novice cap | Master cap | Cooldown floor |
| --- | ---: | ---: | ---: | ---: |
| D | 0.010 | 0.12 | 0.04 | 80 ticks |
| C | 0.015 | 0.18 | 0.06 | 140 ticks |
| B | 0.025 | 0.25 | 0.08 | 240 ticks |
| A | 0.035 | 0.32 | 0.11 | 360 ticks |
| S | 0.050 | 0.40 | 0.15 | 600 ticks |

The Tenketsu cost multiplier and available-resource checks still apply outside this formula. A changed maximum chakra pool changes the result even though the cost formula is unchanged. Floors still apply only to `usesCustomBalance()` techniques in the normal `ItemJutsu.Base` pipeline. `StatsPolicy.rankBaseCost` defines proposed rank base numbers but has no production caller in this audit; original technique costs have not all been replaced with those numbers.

[ItemSharinganCopy](../src/main/java/net/narutomod/item/ItemSharinganCopy.java) now calculates copied-cast cost through the custom cost function using the caster's mastery and Tenketsu modifier, and writes/checks `CopyJutsuCooldownUntil`. Its successful cast still consumes the single-item copy stack. Therefore the stored expiry is not a demonstrated player-wide cooldown across newly created copies, nor does it preserve a callback duration longer than the rank floor. Treat that scope as a review/test question, not proof of a universal copy cooldown.

In `git diff --ignore-space-at-eol HEAD -- src/main/java/net/narutomod/item`, only `BloodlineTechniques`, `ItemJutsu` and `ItemSharinganCopy` had semantic changes at audit time. The many other modified item files were line-ending differences. In particular, `ItemExtraJutsu`, `ItemCanonicalJutsu` and the elemental enum catalogs do not contain the blanket new damage bands/caps claimed in the older rebalance proposal. Housenka still declares max power 5 (Tsumabeni 3), and Water Wall 4. [stats_and_jutsu_economy_rebalance.md](stats_and_jutsu_economy_rebalance.md) mixes a proposal and implementation claims which conflict with this source snapshot; its tables and completion labels are not authoritative current behavior.

## Active, parked and compatibility-only systems

- **Named Taijutsu is active.** Preserve the eight techniques and their server-driven choreography described above.
- **The general M1/combo/momentum/guard/parry/dash/skill-tree expansion is parked.** [The parked-work note](taijutsu-combat-tree-later.md) and [saved patch](taijutsu-combat-tree-expansion.patch) are future references. No extra guard/dash keys or tree commands belong in this handoff. Existing jutsu-specific knockback, paralysis or motion is not that global system.
- **Cursed seals are retired.** [ItemCurseMark](../src/main/java/net/narutomod/item/ItemCurseMark.java) deliberately has no `@ModElement.Tag`, so it registers no seal items or hooks. [RemovedCursedSeals](../src/main/java/net/narutomod/RemovedCursedSeals.java) ignores missing mappings for the ten exact retired `curse_mark_*` item IDs. Preserve that narrow save compatibility behavior; do not infer activation from translations or texture files.
- **Separate Chidori: Raikiri is disabled for normal play, with retained registrations.** `ItemRaiton.CHIDORI_RAIKIRI[10]` returns false and is absent from the active Raiton list. [ItemScrollChidoriRaikiri](../src/main/java/net/narutomod/item/ItemScrollChidoriRaikiri.java) remains registered for saves, hidden from creative tabs and unable to teach because its learning branch includes `&& false`. [EntityChidoriRaikiri](../src/main/java/net/narutomod/entity/EntityChidoriRaikiri.java) remains tagged/registered at entity ID 516. Do not delete these compatibility entries or restore the retired callback during a merge.

## Editable sources versus runtime assets

Runtime asset paths below are relative to [src/main/resources/assets/narutomod](../src/main/resources/assets/narutomod/). The authoring `models/` folder at repository root is separate. Editing an authoring file without updating its actual runtime export does not change the shipped game visual.

| Editable/reference source | Runtime asset and consumer | Merge caveat |
| --- | --- | --- |
| [models/madara](../models/madara/): `skeletal.bbmodel`, `humanoid.bbmodel`, `armored.bbmodel`, `perfect.bbmodel` | `models/custom/madara/*.json`, `textures/susanoo_madara_*.png`; [RenderSusanooMadara](../src/main/java/net/narutomod/client/RenderSusanooMadara.java), [MadaraModel](../src/main/java/net/narutomod/client/MadaraModel.java) | Current sources use native meshes and embedded textures/animation. Old `.mcp.json` files are superseded snapshots, not build inputs. Runtime uses baked `sealPoses`; a cube-only Blockbench audit is not a valid mesh rejection. |
| [models/madara/eyes](../models/madara/eyes/): `madara_ms.svg`, `madara_ems.svg` | Worn `textures/mangekyosharinganhelmet_madara{,_eternal}.png`; icons `textures/blocks/mangekyosharingan_madara{,_eternal}.png` | Regenerate through [build_eyes.cjs](../tools/madara/build_eyes.cjs); keep existing atlas UV placement and attribution. |
| [models/genjutsu/illusion_actor.bbmodel](../models/genjutsu/illusion_actor.bbmodel) | `models/custom/genjutsu_actor.json`, `textures/other/genjutsu_actor.png`; [GenjutsuStageRenderer](../src/main/java/net/narutomod/client/GenjutsuStageRenderer.java) fallback actor | Scene and casting animation lives in Java (`GenjutsuScene`, `GenjutsuCastingModel`, `ClientGenjutsuOverlay`), not Blockbench scene tracks. |
| [models/edo_tensei_coffin.bbmodel](../models/edo_tensei_coffin.bbmodel), legacy authoring JSON | `models/custom/edo_tensei_coffin.json`, `textures/blocks/edo_tensei_coffin.png`; [RenderEdoTensei](../src/main/java/net/narutomod/client/RenderEdoTensei.java) | Source retains 68 cubes/9 groups and embedded 256x256 texture. The static legacy export is scaled into 1.12.2 model-coordinate bounds. Its authoring README's old "future asset" wording does not mean the current Edo runtime is unimplemented. |
| [models/bloodlines/flame_company_wisp.bbmodel](../models/bloodlines/flame_company_wisp.bbmodel) | Current [item/RenderBloodlineTechniques](../src/main/java/net/narutomod/item/RenderBloodlineTechniques.java) uses `dragon_red.png`, `flames_red.png`, `fireball.png` and reused water-dragon geometry | This `.bbmodel` is an older design file, not loaded by the current dragon/orb renderer. |
| [models/cursemark/curse_mark_heaven_wings.bbmodel](../models/cursemark/curse_mark_heaven_wings.bbmodel) and seal textures | Shelved source/resources only | Do not register or activate them. |
| Inuzuka Java model/rendering in `ItemInuzuka` | `textures/akamaru_ninken.png`, wolf-based Ninken renderer | No Blockbench source is required for this runtime implementation. |
| Named Taijutsu / Four-Pillar Java presentation | Player pose hooks and procedural geometry/effects; Four-Pillar uses the vanilla stone texture | Their choreography and timing cannot be reconstructed from texture files alone. Preserve packet/server/client logic together. |

For Madara export and validation, follow [models/madara/README.md](../models/madara/README.md). Preserve [MADARA_EYES_CREDITS.txt](../src/main/resources/assets/narutomod/MADARA_EYES_CREDITS.txt): the adapted eye patterns carry CC BY-SA 3.0 attribution. That attribution is not a blanket license declaration for every third-party asset in a combined mod; retain each source's existing credits and verify redistribution terms before publishing.

This catalog was checked against source registrations, callbacks, runtime resource references and whitespace-insensitive diffs. It does not assert a successful live multiplayer test, installation, or byte-for-byte correspondence with any existing JAR.
