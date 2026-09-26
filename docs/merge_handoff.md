# NarutoMod 1.12.2 — implementation and merge handoff

Status: 2026-09-25. This document is for another AI working with this source tree and a second mod's source. It describes the current `narutomod` implementation, preservation constraints, assets, calculations, tests, and remaining live acceptance work. It is **not** permission to redesign or replace the user's surgery, dojutsu selection, Susanoo, or other established mechanics. The stat progression in the last section is a **proposal only**; it has not been applied to the mod.

## Baseline and non-negotiable merge rules

- Production target is **Minecraft Forge 1.12.2, Java 8, Gradle 4.9**. Runtime mod ID is `narutomod`, version `0.3.2-beta` in `NarutomodMod.java`. `build.gradle` still carries an old generic `version = "1.0"` / archive placeholder; identify a release by the packaged `@Mod` version and contents, not the Gradle filename. `port-1.20.1/` is a separate experimental scaffold, **not** part of this 1.12.2 release.
- This working tree is intentionally dirty and contains many new, untracked source files and assets. A merge based only on `git diff` or only on committed files will silently omit major systems. Include tracked changes **and** relevant untracked files; do not copy `build/`, `logs/`, caches, or prior release JARs as source.
- `docs/merge_source_manifest.md` lists 249 other changed/new files visible in the working tree for this handoff (excluding logs/build output), including dormant and nonproduction files marked in its preface. Use it as an audit checklist, not as an instruction to activate every listed file.
- Merge class behavior and registries deliberately. Preserve the `narutomod` namespace, existing item/entity registry names, `ItemJutsu.JutsuEnum` numeric positions, scroll ordering, existing NBT keys and saved-data formats. Do not renumber slots or blindly replace `ElementsNarutomodMod`, `NarutomodMod` packet setup, keybindings or client proxy with the other mod's equivalents. Resolve duplicate event subscriptions and packet registration explicitly. Put the **same newly built JAR** on client and dedicated server; never load two different NarutoMod versions together.
- Back up world, playerdata and server config before any merger test. Test conversion in a copied world, especially surgery sockets and Edo archive data. No installed `.minecraft/mods` JAR is changed by this handoff.
- Preserve asset attribution in `src/main/resources/assets/narutomod/MADARA_EYES_CREDITS.txt`. Confirm license/attribution before distributing a combined mod. Editable `models/` sources are not substitutes for their runtime exports under `src/main/resources/assets/narutomod/`.

## Feature and integration map

| System | What works now | Merge entry points / important assets | Boundary |
| --- | --- | --- | --- |
| Ocular surgery and sockets | Opt-in, per-socket self/consensual two-player surgery. 3,000 Healing Jutsu XP on the medic's medical item qualifies (not consumed); 100 chakra on success, five-second stationary channel and ten-second recovery. Patient or medic inventory can supply either socket. Extracted physical eyes retain donor identity and return to patient. Supported: normal colors including black, Sharingan/MS/EMS, Byakugan, normal Rinnegan. | `MedicalSurgery`, `OcularState`, `OcularRegistry`, `OcularPolicy`, `OcularSystem`, `OcularAbilities`, `OcularEvolution`, `item/ItemOcularGear`, `client/GuiOcularSurgery`, `client/OcularModel`; `models/item/{ocular_sockets,preserved_eye,normal_eyes_black}.json`; `textures/normal_eyes_black.png`. See `docs/ocular_surgery.md`. | Surgery is a permanent migration to socket state; there is no in-game reverse conversion. Do not replace preserved organs with generic placeholder eye tokens. Tenseigan/Rinnesharingan, involuntary surgery and automatic clan training are not implemented. Live two-player approval and rendering need confirmation. |
| Dojutsu controls | **Non-operated players keep the original selector**: Left/Right browse the hotbar choice, Enter confirms, G toggles. H is unused. Operated players use G/H for left/right sockets, Shift+G/H to focus side-specific jutsu. The experimental wheel is disabled. | `DojutsuControl`, `keybind/KeyBindingDojutsuControl`, `OcularSystem`, targeted `item/ItemDojutsu` and sync adapters. | Never introduce the wheel for non-operated players as an accidental side effect. Eye-in-inventory is not an installed socket. Non-Uchiha Sharingan must remain covered/uncovered rather than freely deactivated. |
| EMS and advanced eyes | Paired blood-relative donor MS plus recipient's own compatible Uchiha MS progression awakens EMS, preserving recipient ability family and physical donor-eye identities. Side-specific Sasuke/Obito/Madara/Rinnegan abilities follow socket side; compatible pair required for Susanoo. | `OcularEvolution`, `OcularAbilities`, `OcularRegistry`, Madara and existing eye item classes; `/eyesurgery family ...` RP record. | Never merge two arbitrary donor eyes into EMS or give a lone implanted eye paired Susanoo. |
| Madara MS/EMS and Susanoo | Dedicated six-stage Madara entity, time **movement** reversal, blue original-style geometry, server-synced two-faced/extra-arm hand seals, mount/collision and owner-projectile exclusions. Stage XP gates 2k/5k/10k/20k/30k/40k; Perfect requires **Eternal Madara pair + 40k Ninja XP**. Normal MS stops before Perfect; Armored has timed burst/recovery. | `MadaraSusanooPolicy`, `MadaraTemporalController`, `TemporalHistory`, `entity/EntitySusanooMadara`, `client/{MadaraModel,RenderSusanooMadara}`, `SusanooCastController`, `SusanooCombat`; editable `models/madara/{skeletal,humanoid,armored,perfect}.bbmodel`; runtime `models/custom/madara/*.json`, `textures/susanoo_madara_*.png`. See `models/madara/README.md`. | Stage widths: 2.4/3.2/4/4.8/5.6/8; heights: 3/5/6/9/10/16; upkeep chakra/s: 30/45/60/70/85/115. Time reversal is **not** health, inventory, XP or terrain rewind. Do not restore obsolete `.mcp.json` prototype geometry. Live multiplayer rider and seal syncing remain to be verified. |
| Genjutsu | Five established jutsu IDs/scrolls: False Opening, Memory Fracture, Murder Intent, Illusionary Execution, Burning Coffin. Private animated visions keep victim's real body present for other players; caster/victim skins, escape/cleanup and Sharingan sound on required casts. | `GenjutsuSession`, `client/{GenjutsuScene,GenjutsuStageRenderer,GenjutsuCastingModel,ClientGenjutsuOverlay}`, `models/genjutsu/illusion_actor.bbmodel`, runtime `models/custom/genjutsu_actor.json`, `textures/other/genjutsu_actor.png`. See `docs/genjutsu_anime_refresh.md`. | These are client-rendered stages, **not new dimensions**; FBO-disabled clients get lighter 2D fallback. No permanent control or skill/XP deletion. Live two-client audio/skin/FBO verification remains. |
| Edo Tensei donor loop | Actual survival death produces tradeable sealed DNA. Offhand sample + ritual consumes a claimed sample after sink/seal sequence, recording a caster-owned soul. Summoning Souls selects reusable donor and spawns player-skinned/name NPC from rising/falling coffin. Owner can cycle follow/guard/passive, recall/dismiss; one active NPC per soul. | `EdoSoulRegistry`, `item/{ItemDnaSample,ItemSummoningSouls}`, `entity/{EntityEdoTensei,EntityEdoReanimation}`, `client/{RenderEdoTensei,RenderEdoReanimation}`, editable `models/edo_tensei_coffin.bbmodel`, runtime `models/custom/edo_tensei_coffin.json`, `textures/blocks/edo_tensei_coffin.png`. See `docs/edo_tensei_gameplay.md`. | `narutomod_edo_souls.dat` archive v2 is server authority for sample claims and active IDs. One donor registration per caster, unlimited distinct donors. Old prototype souls cannot truthfully become donor NPCs. Donor's full jutsus, equipment, eye powers and sacrificial bodies are **not** implemented. `docs/edo_tensei_prototype.md` is historical only. |
| Paper bombs | Four scroll-learned Explosive Art techniques (Tag Volley, Snare Circuit, Seeking Tag Swarm, Breaching Seal), exact explosive-tag inventory ammo, chakra, cooldown and safe no-terrain-damage explosions. Grass/fern clearing makes circuit/breach usable on ordinary terrain. | `item/ItemExplosiveArt`, `PaperBomb{Ammo,Placement,Policy}`, `entity/EntityPaperBombCast`, `client/{RenderPaperBombCast,ClientPaperBombCasting}`, item/scroll models. See `docs/paper_bomb_jutsu.md` for ammo, costs, targeting and commands. | Ammo is from main/offhand, including creative; never spend on a failed spawn. The `ITEM_CRACK` debris packet must include **both** item ID and metadata (fixes `ArrayIndexOutOfBoundsException: 1`). Preserve source-to-victim/ally and no-PvP filtering. |
| Fire Phoenix | 0.8–4 charge scales the bird; starts at 0.22 block/tick, accelerates by 0.028/tick to 0.82; acquires within 8 blocks with forward LOS and pursues, while substitution and water walls counter it. Single impact damage event. | `PhoenixFlight`, `item/ItemKaton`, existing bird texture/model. See `docs/phoenix_balance_and_combat_depth.md` **implemented section only**. | Base damage remains `12 + 8*power`; no global homing change. The guard/parry/stagger plan later in that document was **explicitly canceled** and has no implementation. |
| Elemental presentation | Lightning Four-Pillar cage, Chidori Senbon sparks and Lightning Clone discharge, charge sounds, bounded packet/client rendering. Reverted earth/wind/water presentation remains; Luna's separate Chidori/Raikiri jutsu stays disabled. | `JutsuVisualEffects`, `JutsuEffectSounds`, `client/{ClientJutsuVfx,LightningBindVisual}`, `item/ItemCanonicalJutsu`, `item/ItemRaiton`; lightning textures/sounds below. See `docs/lightning_vfx.md`. | Do not re-enable disabled Raikiri merely because its source/scroll files are present. Do not reapply prior earth/wind/water VFX. Preserve corrected block-state particle arguments. |
| Retired cursed seals | Historic model/texture sources may remain on disk, but cursed seal gameplay was removed at user's request. | `RemovedCursedSeals`, dormant `models/cursemark/`, `textures/other/cursemark/`. | **Do not register or activate** those assets/items during merger. |

## Four-Pillar Bind — current final behavior and exact math

Registration remains `ItemRaiton.FOURPILLARBIND`: index **9**, rank **A**, base chakra **140**, `withCustomBalance()`. `ItemCanonicalJutsu.FourPillarBind` has existing charged base power 1 and max power 2, with delay 32 ticks. Its action-bar display converts internal power to 0–100%: `c = clamp(power - 1, 0, 1)` and `percent = round(100*c)`. A player with insufficient available chakra may be capped below internal power 2 by `ItemJutsu.Base.getMaxPower`; 100% is not guaranteed merely by holding longer.

Server raycast is 24 blocks. Only a valid hostile target and finite power are accepted; creative targets and forbidden player PvP are rejected. The cage center uses the target's bounding-box X/Z center and minimum Y. For target bounding-box height `h` and maximum horizontal width `w` (blocks):

```text
cageHeight = clamp(1.35*h + 1, 4.5, 96)
cageRadius = clamp(0.75*w + 1.3, 2, 64)
damage = (6 + 12*c) * (1 + 0.35*sqrt(clamp(NinjaXP,0,100000)/100000))
                    * (1 + 0.25*clamp(jutsuMastery,0,1))
bindTicks = 60 + round(50*c)
```

Damage spans 6 to **30.375** before armor/other damage hooks. XP is the caster's `PlayerTracker.getBattleXp`; jutsu mastery is `ItemJutsu.getJutsuMastery` (newly learned 0; reaches 1 at three times required jutsu XP). One initial server damage event is issued. If it is canceled/replaced/blocked or the target becomes untargetable, **no paralysis or cage** follows; an accepted cast is still on 520-tick cooldown. An accepted hit applies paralysis amplifier 1 for 60–110 ticks plus mining fatigue amplifier 3 for 80 ticks, four dirt bursts at the scaled pillar locations, rise/impact/sustain sounds and screen shake. This is existing jutsu-specific restraint, **not** the canceled generic combat stagger system.

Network payload `JutsuVisualEffects.Message` kind `LIGHTNING_BIND=8` keeps its existing layout. For that kind only, `scale` is cage height (2–96 accepted) and `element` stores `round(radius*1000)` (2,000–64,000 accepted). An old packet with `element=0` decodes to radius 2. Validation bounds values before rendering. Other VFX kinds keep their previous `scale` semantics. `LightningBindVisual` scales the same four-pillar geometry to target size without adding vertices; client budgets remain eight cages and 256 transient effects. Packet fields, kind ID and validation must be merged on **both** sides.

The normal custom-jutsu resource calculation is separate from the damage formula. For this A-rank technique, with `m=clamp(mastery,0,1)`, `M=max chakra`, and internal `power`:

```text
fixed = 140*(1 - 0.35*m)
pool = max(0,M-500)*0.035*(1-0.80*m)
raw = (fixed+pool)*max(1,power)
cap = M*(0.32+(0.11-0.32)*m)
chakraCost = max(fixed, min(raw, max(fixed,cap)))
```

The generic code applies any outside tenketsu/pathway modifiers and validates available resources. Other ranks have different ratios/caps in `ItemJutsu.getCustomResourceCost`; do not generalize these A-rank constants to all techniques. The manual bind cooldown is 520 ticks; its rank-based minimum does not reduce that.

## Existing stats and persistence (do not silently rewrite)

`PlayerStats.java` stores `NarutomodTrainingStats` NBT, data version 5, keys `speed`, `strength`, `resistance`, `health`, `chakra`, `spi`. Current default per-rank stat caps are 100/250/600/1200/2500 for None/Genin/Chunin/Jonin/Hokage (world and personal limits may override). Training points are budgeted separately. `PlayerTracker` caps Ninja/Battle XP at 100,000. Actual source formulas, where `s` is that individual stat and `XP` is Ninja XP:

```text
progressive(s,a,p)        = a * max(0,s)^p
movement attribute bonus = 0.012*log10(1+Speed) + 0.00015*Speed^0.45
strength attack bonus    = 0.04*Strength^0.80 (visible attribute capped at +2000;
                           melee overflow is handled separately)
resistance rating        = 0.12*Resistance^0.75
incoming damage factor   = 1/sqrt(1+resistanceRating/100)
effective maximum HP     = 20 + 0.005*XP + 0.24*Health^0.80
displayed maximum HP     = min(1000,effective maximum HP)
chakra maximum           = 0.5*XP + 6*Chakra^0.80
stamina-mode maximum     = 0.35*XP + 120 + 5*(Speed+Strength+Resistance+Health)^0.78
SPI regen bonus          = 0.0025*SPI^0.65
                           + maxChakra*(0.00005+0.00003*log10(1+SPI))
regen lock ticks         = max(20,100-round(10*log10(1+SPI)))
```

Displayed-health overflow is reconciled through `getHealthDamageMultiplier`, not by deleting underlying effective health. Damage, potion, clan, equipment, jutsu and Chakra pathway hooks can further modify a result. A merger must preserve NBT, point budgets, rank caps and server-authoritative attribute refresh, and test old characters in a copied world. Do not mistake the proposal below for current gameplay.

## Proposed replacement stat curve — document only, not in build

The user requested a calculation that scales sensibly for a future balancing pass, **without changing this mod now**. This is a coherent starting curve for the merger AI to present for approval, not an instruction to auto-apply it. Use `F(s)=sqrt(max(s,0)/250)` and `L=sqrt(clamp(XP,0,100000)/100000)`; all values are per-character, server-computed:

```text
HP                     = 20 + 8*F(Health) + 15*L
base melee damage      = 1 + 2.5*F(Strength) + 2*L
physical damage factor = 1/(1+0.18*F(Resistance))
maximum chakra         = 120 + 80*F(Chakra) + 180*L
chakra regen / second  = 1 + 0.75*F(SPI) + 0.002*maximumChakra
movement attribute add = min(0.065,0.012*F(Speed)+0.004*L)
maximum stamina        = 100 + 30*sqrt((Speed+Strength+Resistance+Health)/1000) + 80*L
```

Example values when all six stats are equal (`regen` is chakra per second; move add is an absolute Minecraft movement-attribute addition):

| Each stat / Ninja XP | HP | Melee | Physical hit factor | Max chakra | Regen/s | Move add |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 / 0 | 20.00 | 1.00 | 1.000 | 120.0 | 1.24 | 0.000 |
| 100 / 2,000 | 27.18 | 2.86 | 0.898 | 196.1 | 1.87 | 0.008 |
| 250 / 5,000 | 31.35 | 3.95 | 0.847 | 240.2 | 2.23 | 0.013 |
| 600 / 10,000 | 37.14 | 5.51 | 0.782 | 300.9 | 2.76 | 0.020 |
| 1,200 / 40,000 | 47.01 | 7.74 | 0.717 | 409.1 | 3.46 | 0.029 |
| 2,500 / 100,000 | 60.30 | 10.91 | 0.637 | 553.0 | 4.48 | 0.042 |

This deliberately has diminishing returns and no 50,000-chakra cliff, but **existing jutsu costs and sustained Susanoo/Edo upkeep are tuned for today's larger pools**. Before adoption, rescale costs/upkeep, NPC damage, healing and XP thresholds together; preserve stat/NBT identity and saved players, use a server-side migration/feature flag, and run monotonicity/finite/bounds tests at 0, all rank caps and 100,000 XP. Also evaluate player-vs-player time to defeat, one-cast affordability, sprint speed and chakra regeneration at multiple ranks. Never merge this table as code without user approval and a broad rebalance.

## Runtime assets and sounds

- Madara: editable `models/madara/*.bbmodel`; runtime `src/main/resources/assets/narutomod/models/custom/madara/*.json` and `textures/susanoo_madara_*.png`. Eye SVG sources/previews in `models/madara/eyes/`, runtime item/helmet textures under `textures/` and `textures/blocks/`, item models under `models/item/`. `MADARA_EYES_CREDITS.txt` must travel with the assets.
- Edo: editable `models/edo_tensei_coffin.bbmodel` (UV preview in `docs/`), runtime `models/custom/edo_tensei_coffin.json` and `textures/blocks/edo_tensei_coffin.png`; scene timing is Java renderer/entity logic. DNA icon currently simple paper art; do not present it as finalized donor art.
- Genjutsu: editable `models/genjutsu/illusion_actor.bbmodel`, runtime `models/custom/genjutsu_actor.json`, `textures/other/genjutsu_actor.png`; wooden/flame presentation also reuses original mod assets. Animated scene logic is Java, not a new animation runtime dependency.
- Lightning: `textures/raiton_{channel,channel_soft,ion_flipbook,ring,spark_flipbook}.png` plus `sounds/custom_jutsu/` and `sounds.json`. Four-Pillar geometry and animation are generated by `client/LightningBindVisual`; no separate Blockbench cage dependency.
- Paper bombs use the original tag artwork; new scroll/item models live in `models/item/`. Phoenix uses the existing bird renderer/texture rather than new model geometry. Normal black-eye item/overlay resources must be copied together. Cursed-seal source assets on disk are dormant.
- For every `sounds.json` entry, retain its referenced OGG and namespace spelling. Keep `en_us.lang` and `pt_br.lang` updates with code; omitted translations can make a complete system appear broken.

## Recommended merge order and conflict checklist

1. Inventory both source trees including untracked files and asset licensing. Make a world and playerdata backup; record each mod ID, registry name, item/entity ID, `JutsuEnum` index, packet discriminator and NBT root. Decide any renamed namespace *before* migration and write an explicit mapping; avoid renaming this mod when possible.
2. Merge core registration, side proxies and network messages **once**: `ElementsNarutomodMod`, `NarutomodMod`, `PlayerStats`, `Chakra`, `ItemJutsu`, keybinds, event listeners. Avoid duplicate key handlers, duplicate death-drop handlers and registering a client renderer on a server. Maintain numeric jutsu array positions/scroll append order and both language files.
3. Merge/save-test ocular and Edo persistence before effects. Eyes use physical socket identity and a registry ledger; Edo uses archive-v2 claims and active summon UUIDs. Never infer authority from item display names or only a client GUI. Recheck old player migration, death/respawn, relog, dimension transfer and unloaded chunks.
4. Merge entities and client visuals as pairs: Susanoo owner/passenger/projectile filtering; paper-bomb ownership and particle packet metadata; Edo coffin/NPC renderer and donor skins; genjutsu server sessions and private scene packets; Four-Pillar height/radius packet and matching renderer. Then copy all runtime textures/models/sounds and attribution.
5. Run offline fixtures and build; inspect the **reobfuscated** output rather than an ordinary dev JAR. Install one exact JAR per side in a copied 1.12.2 world. Live-test two-player surgery and controls, EMS/Perfect unlock, own/enemy Susanoo attacks, genjutsu observer/victim views, paper-bomb explosion/network, Edo DNA death/ritual/recall, Phoenix water/substitution, and a player-sized plus giant-summon Four-Pillar cage. Repeat with PvP off, latency and client minimal particles.

Useful fixture tasks (Java 8, Gradle 4.9, `--offline --no-daemon --console=plain`): `build` includes `checkOcularSurgery`, `checkPaperBombs` and established Madara checks. Additional init scripts provide `verifyEdo verifyEdoSouls verifyEdoReanimation` (`tools/edo/verify.gradle`), `verifyGenjutsuScenes verifyVisualRefresh` (`tools/visuals/verify.gradle`), `verifyMadaraTemporal verifyMadaraSusanoo verifyMadaraModels` (`tools/madara/verify.gradle`), `verifyPhoenix` (`tools/phoenix/verify.gradle`), and `verifyLightningVfx` (`tools/vfx/verify.gradle`). Offline previews in `build/reports/` validate geometry, **not** actual Minecraft rendering or multiplayer behavior.

Verification record for this handoff: all tasks above and `build` passed on 2026-09-25 with Java 8 / Gradle 4.9; the Four-Pillar fixture passed 320,770 checks and bounded the large-cage mesh to 4,784 vertices. Reobfuscated test artifact: `build/releases/narutomod-ctrlz-0.3.2-beta-four-pillar-handoff.jar` (32,945,982 bytes; SHA-256 `13BBBEDBD57B927E59C1ECF624358FAE3F025F3C455C1EA680FEFF235F27F508`). Seven representative class, model and language entries were checked inside the JAR. The artifact has **not** been installed or live-tested.

## Open acceptance items, not silent completion claims

- No dedicated-server/two-client playtest has been performed for this handoff. Rendering, skin signatures, synced animations, sounds, multiplayer cancellation, claims/protection compatibility and giant entity hitboxes still need real game verification. Automated tests can establish code/model invariants, not final appearance or balance.
- Edo does **not** copy donor jutsus, genetics/eye skills, gear, full attributes or sacrifice mechanics. Existing donor NPC and skin fallback are the playable first iteration. Its old random-mob prototype doc must not be used as the authoritative design.
- Eye surgery is functional by source/fixtures but still needs live consent/inventory test. The old selector must remain for unoperated players. The optional wheel and cursed seals must stay off; Luna's separate Raikiri remains disabled; reverted earth/wind/water VFX must stay reverted.
- Guard, parry, M1 chains, true stagger and armor break were explicitly canceled for this build. Do not merge placeholder combat code or infer that Four-Pillar paralysis implements those systems.
- The proposed stat curve above is documentation only. Current `PlayerStats`, Chakra pool and jutsu resource formulas are unchanged by this handoff.
