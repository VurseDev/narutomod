# NarutoMod — final source and integration handoff

Audited **27 September 2026**. Target: **Minecraft Forge 1.12.2 / Java 8**. This is the entry point for an AI merging this project with another mod's source. It documents actual code, intentionally retired work, preservation requirements, risks and a proposed follow-up balance model. It is not authorization to implement every idea mentioned here.

## 1. Read this first

Read this document, [the technique/asset catalog](docs/CUSTOM_JUTSU_CATALOG.md), [stats and balance](docs/STATS_AND_BALANCE.md), then [verification and bug register](docs/HANDOFF_VERIFICATION.md). Source is authoritative when an older design document disagrees. The earlier [September 25 handoff](docs/merge_handoff.md) is historical, not a second current specification.

There are **three distinct baselines**:

| Baseline | What it represents |
| --- | --- |
| Git `f828586` | Named pre-rebalance backup commit. It contains the recent gameplay systems and Bloodlines work. A commit alone does not include subsequent working-tree edits. |
| Preserved `build/releases/narutomod-ctrlz-0.3.2-beta-bloodlines-impact-v3-test.jar` | September 26 test artifact, 32,971,264 bytes; SHA-256 `4E8F4520E1246F3A62536C7C5B47E6475B97C86AD21A45A18CA609B74C2C876F`. This is **not** proof that newer stat changes are packaged. `build/` is Git-ignored. |
| Current working tree | Includes a newer, partly implemented stat/economy revision: `StatsPolicy`, gated player curves, Bloodline blast scaling and Sharingan Copy changes. Several older proposal/status statements overstate what was implemented. See the exact formulas and coverage in the stats companion. |

This documentation pass does not alter gameplay, install a JAR, modify a world, commit or push. This handoff covers the **Forge 1.12.2 source and a 1.12.2 merge only**. “Port” below means behavior-preserving integration into the other 1.12.2 mod, not a Minecraft-version upgrade.

### Non-negotiable preservation rules

- Keep surgery opt-in. Players who have never completed surgery retain the old dojutsu selection and toggle behavior. Do not restore the experimental wheel.
- An inventory eye is **side-neutral**. Left/right belongs to the patient's anatomical socket. Return actual extracted eye items with their data, not generic donor placeholders.
- Keep normal black eyes, existing eye families, compatible-pair Susanoo requirements and server-authoritative consent/inventory checks.
- Keep the mod's shared charge/release path. Do not add a parallel charging timer/UI to Twin Flame Dragons, Flame Company or Clone Throw.
- Put custom learning scrolls in **CUSTOM JUTSU**, preserving scroll indices and learning-container routing.
- Preserve native NarutoMod visual style: blue Madara Susanoo geometry derived from existing models; existing fire textures and dragon geometry; readable, bounded effects and deliberate sounds.
- Do not restore reverted earth/wind/water VFX, cursed-seal gameplay, separate Luna Chidori/Raikiri, or the discarded generic M1/guard/parry system. Named Taijutsu techniques remain active.
- Never infer working gameplay from the mere presence of a `.java`, `.bbmodel`, texture, translation or old plan.
- Keep saved names, numeric technique indices, resource locations, NBT keys, packet compatibility and owner UUID semantics. When a change is unavoidable, supply a versioned migration and rollback test.

## 2. Repository and runtime architecture

| Area | Role / merge rule |
| --- | --- |
| [src/main/java/net/narutomod](src/main/java/net/narutomod) | Authoritative 1.12.2 gameplay, packet handlers, client code and generated legacy classes. A similarly named root-level file is not automatically compiled. |
| [src/main/resources/assets/narutomod](src/main/resources/assets/narutomod) | Packaged runtime models, textures, sounds, item definitions and language files. Case-sensitive resource names matter on Linux. |
| [models](models), [tools](tools) | Editable authoring sources and exporters/checkers. Exported runtime assets must travel with these. Local MCP endpoints/tool defaults are not portable project dependencies. |
| [src/test/java/net/narutomod](src/test/java/net/narutomod) | Headless policy/runtime fixtures and optional rendering harnesses. They are not a live multiplayer acceptance test. |
| [build.gradle](build.gradle), [mcreator.gradle](mcreator.gradle), [gradle/wrapper](gradle/wrapper) | Java 8, Gradle 4.9, Forge `1.12.2-14.23.5.2855`, snapshot mappings `20171003-1.12`. ForgeGradle `3.+` is dynamic: reproducibility needs dependency locking/pinning after validation. |

[NarutomodMod](src/main/java/net/narutomod/NarutomodMod.java) declares `narutomod`, runtime version `0.3.2-beta`, sided proxies and network channel `narutomod:a`. Gradle still uses generic archive name/version `modid-1.0`; that filename is not a meaningful gameplay release identifier.

[ElementsNarutomodMod](src/main/java/net/narutomod/ElementsNarutomodMod.java) discovers annotated `ModElement` classes, sorts them and drives initialization/registration. Packet discriminators depend on registration order; even a registration call without a side advances the counter. Do not merge by pasting in a second bootstrap, duplicate event registration or independently reordered packet table. Keep client render classes off the dedicated-server loading path.

[ItemJutsu](src/main/java/net/narutomod/item/ItemJutsu.java) is the shared contract: technique selection, learned XP, mastery, charge, affordability, cast callback, resource payment and cooldown. [ItemExtraJutsuScrolls](src/main/java/net/narutomod/item/ItemExtraJutsuScrolls.java) and [GuiScrollExtraJutsu](src/main/java/net/narutomod/gui/GuiScrollExtraJutsu.java) connect custom learning scrolls to those existing items. A new scroll is not a new independent combat framework.

## 3. Ocular surgery, regular eyes and dojutsu controls

Entry points: [MedicalSurgery](src/main/java/net/narutomod/MedicalSurgery.java), [OcularState](src/main/java/net/narutomod/OcularState.java), [OcularRegistry](src/main/java/net/narutomod/OcularRegistry.java), [OcularPolicy](src/main/java/net/narutomod/OcularPolicy.java), [OcularSystem](src/main/java/net/narutomod/OcularSystem.java), [OcularAbilities](src/main/java/net/narutomod/OcularAbilities.java), [OcularEvolution](src/main/java/net/narutomod/OcularEvolution.java), [ItemOcularGear](src/main/java/net/narutomod/item/ItemOcularGear.java), [GuiOcularSurgery](src/main/java/net/narutomod/client/GuiOcularSurgery.java), [DojutsuControl](src/main/java/net/narutomod/DojutsuControl.java).

### Player flow

1. Train **Healing Jutsu XP to 3,000 on the medic's owned medical item**. It is a qualification, not 3,000 chakra or a consumed XP payment. Creative does not bypass the qualification.
2. Sneak-cast for self-surgery, or target another player within three blocks. Choose each socket in the dossier. Compatible eye items from **either the patient or medic inventory** can supply either side.
3. Another patient approves the exact proposed layout. Closing/declining cancels; fallback `/eyesurgery accept|decline <session>` exists. Invitation alone must not suppress abilities.
4. Complete a five-second stationary channel. Damage, movement, range/dimension/logout, medical-item changes and relevant inventory/state changes can cancel it. Inventory returns must fit before commit.
5. Success costs 100 chakra, starts ten-second recovery and opts the patient into permanent socket state. There is no in-game reverse migration to the legacy equipment system.

**Actual extraction routing:** removed organs go to the **medic/surgeon**, not the patient. For self-surgery those are the same inventory. Split remainders from a legacy paired input return to that input's owner. The old surgery document's patient-return claim was wrong.

Bare compatible items can receive a generated physical identity when the menu scans them, before consent. Consequently cancellation means no completed transplant/resource payment, **not absolutely no item-NBT mutation**. A new extracted item keeps the actual eye item/payload; legacy wrapper formats are compatibility inputs only.

Normal colors are hazel/default, blue, green, grey, amber and black. Black uses the normal-eye rendering path with a dark iris treatment rather than adding a new power. Eye fitting retains per-player vertical adjustment (−2…2) and head-local rendering; test both standard and slim skins.

### Controls and capabilities

| Player state | Controls |
| --- | --- |
| Never operated | Left/right arrows browse original hotbar selector, Enter confirms, G toggles. H does nothing; experimental wheel disabled. |
| Socket patient | G left, H right; Shift+G/H changes the focused side for side-specific jutsu without toggling. |
| Transplanted Sharingan that cannot deactivate | Cover/uncover instead; an uncovered implant retains its identity/cost. Rinnegan is covered to rest. Byakugan can toggle. |

Skills/upkeep require the relevant active, uncovered socket. Empty, covered or blind eyes affect vision; both unavailable eyes produce blindness. Installing an eye does not grant unrelated clan training. Supported: regular eyes, Sharingan, MS/EMS, Byakugan, normal Rinnegan. Tenseigan/Rinnesharingan are rejected by this surgery system.

Per active eye, chakra/second: basic Sharingan **2.5 original / 10 recipient**, MS **10 / 30**, Byakugan **10 / 20**, Rinnegan **20 / 40**, normal **0**. Creative skips upkeep. These are per-eye rates, not pair totals.

Side adapters retain Sasuke Amaterasu/flame control, Obito ranged/self Kamui, Madara temporal abilities and Rinnegan Six Paths. A single implant or arbitrary mixed pair must not silently acquire a compatible paired Susanoo.

EMS awakening requires the Uchiha recipient's own paired MS progression plus both MS eyes from **one different, recorded blood relative**. The awakened result preserves the recipient's ability family and physical donor identities. Staff record RP relationships with `/eyesurgery family <player|UUID> <family-id|none>`; the game does not infer real family relationships.

### Persistence and integration hazards

- Player root `NarutomodOcularSockets`, schema version 2: `Left`/`Right` state, focus/revision, recovery/toggle timestamps, normal variant and native progression snapshots. Keep installed state through player cloning/respawn/login.
- Overworld saved-data ledger `narutomod_ocular_registry`: `Locations` maps physical UUID to jar or patient/socket location; `Families` stores RP ancestry groups. Preserve both with playerdata.
- Native migration can derive deterministic physical IDs; an unstamped inventory eye can acquire a random one. This is an identity/duplication ledger, **not cryptographic proof of provenance or a global two-eyes-per-donor guarantee**.
- `donorSide` is assigned to the chosen socket in the present commit path; do not advertise it as immutable birth-side provenance. The user explicitly wanted inventory eyes to be side-neutral.
- Session snapshots and mutable runtime eye fields need a fresh-state commit review. See the bug register; do not “fix” surgery by restoring the rejected donor-placeholder system.

## 4. Madara MS/EMS, Susanoo and time abilities

Main code: [MadaraSusanooPolicy](src/main/java/net/narutomod/MadaraSusanooPolicy.java), [EntitySusanooMadara](src/main/java/net/narutomod/entity/EntitySusanooMadara.java), [SusanooCombat](src/main/java/net/narutomod/SusanooCombat.java), [SusanooCastController](src/main/java/net/narutomod/SusanooCastController.java), [MadaraTemporalController](src/main/java/net/narutomod/MadaraTemporalController.java), [TemporalHistory](src/main/java/net/narutomod/TemporalHistory.java). Authoring/runtime map: [Madara models](models/madara/README.md).

Six progression stages use four model families; two early stages share skeletal geometry and two share humanoid geometry with distinct presentation/scale. Blue geometry, two-faced/extra-arm identity and seals follow existing NarutoMod Susanoo style, not the abandoned prototype parts.

| Stage index | Family | Ninja XP gate | Width × height, blocks | Chakra/s |
| --- | --- | ---: | ---: | ---: |
| 0 | Skeletal | 2,000 | 2.4 × 3 | 30 |
| 1 | Skeletal | 5,000 | 3.2 × 5 | 45 |
| 2 | Humanoid | 10,000 | 4 × 6 | 60 |
| 3 | Humanoid | 20,000 | 4.8 × 9 | 70 |
| 4 | Armored | 30,000 | 5.6 × 10 | 85 |
| 5 | Perfect | 40,000 | 8 × 16 | 115 |

**Perfect needs Eternal Madara eyes as well as XP.** Normal MS does not become Eternal because a command grants 100k XP. Creative bypasses XP, not the Eternal requirement. For surgery players, the eligible installed pair matters, not spare eyes sitting in inventory. Error messages distinguish the gate. Normal-MS armored use is a 400-tick burst with 1,200-tick recovery; stage transitions take 24 ticks.

The server starts seal choreography when the rider casts; extra arms use synchronized profiles/poses. Runtime consumes baked `sealPoses`, not arbitrary Blockbench animation playback. The rider remains mounted; shooter-aware filtering excludes the caster's own Susanoo from their jutsu collision/target selection. **Enemy Susanoo remains a valid obstruction/target**—never globally make all Susanoo intangible to projectiles.

Temporal manipulation is a game-inspired ability, not a claim that the manga revealed this MS power. It records bounded movement history/anchors and checks safe return positions. It does **not** rewind HP, inventory, blocks, XP or another player's world history. Preserve dimension, loaded-chunk, collision and rider safety.

Defaults in [MadaraTemporalSettings](src/main/java/net/narutomod/MadaraTemporalSettings.java): MS/EMS history 2/4 seconds, anchor 4/6 seconds, cooldown 45/35 seconds, maximum return 8/12 blocks, anchor cost 80/60, reversal 160/120. The `narutomod-madara-time` config requires restart. Existing eye strain/blindness integration remains relevant.

## 5. Edo Tensei — actual donor gameplay, not random mobs

Main code: [EdoSoulRegistry](src/main/java/net/narutomod/EdoSoulRegistry.java), [ItemDnaSample](src/main/java/net/narutomod/item/ItemDnaSample.java), [ItemSummoningSouls](src/main/java/net/narutomod/item/ItemSummoningSouls.java), [EntityEdoTensei](src/main/java/net/narutomod/entity/EntityEdoTensei.java), [EntityEdoReanimation](src/main/java/net/narutomod/entity/EntityEdoReanimation.java). Detailed companion: [Edo gameplay](docs/edo_tensei_gameplay.md).

1. A real survival-player death produces tradeable sealed DNA, including with keepInventory. Creative/spectator deaths are excluded. Capture donor identity and bounded Ninja XP before death XP-reset logic.
2. The caster holds DNA in the offhand and casts Edo Ritual onto a clear, flat 7×7 area. The server validates/claims the archived sample; a display name or `/give` blank item is not authentic DNA.
3. The ritual escrows the sample. It grows the ground seal, shows the donor, sinks it and ends in smoke/sound. Completion consumes the claim and adds the donor to the caster's roster. Interruptions refund the sample to the caster, or drop it if appropriate when offline. Claim replay must not duplicate it.
4. `Summoning Souls` is bound to that caster. Distinct donor count has no intended gameplay limit; the server stores the full roster and only synchronizes an eight-name window. Repeating the same donor does not multiply identical entries.
5. Selected souls rise in the authored coffin, with dirt, Kuchiyose sound, falling lid, reveal and later smoke/unsummon. The spawned actor is an **NPC using the donor's skin/name**, not the dead player's actual entity.
6. Empty-hand interaction cycles follow/guard/passive; sneak dismisses. Sneak-using the roster item recalls a soul or cancels its pending coffin without another cast payment/cooldown.

At 20 TPS the ritual sequence is seal growth ticks 0–20, donor preview 20–95, sink 52–92, smoke 96, award 110. Coffin: rise 0–60, lid fall 64–84, walk/reveal 100–120, NPC spawn 120, smoke 144, remove 150. Keep these as server tick state plus interpolated client visuals, not independent client timers granting rewards.

The archive is `narutomod_edo_souls.dat`, version 2, stored as world saved data. It records authentic samples, claims, owner rosters and active identities. One active NPC per caster/soul includes unloaded and cross-dimension records; a stale entity must not clear a newer entity's active UUID.

Current donor snapshot contains UUID, name, available signed GameProfile and XP clamped 0…100,000. NPC HP is `min(80, 20 + 0.2*sqrt(XP))`; attack is `min(10, 3 + 0.025*sqrt(XP))`. Upkeep is 10 chakra/s per active NPC (creative free). Owner death/logout/dimension departure, separation over 96 blocks or insufficient chakra ends it. Lethal ordinary damage reforms the NPC at one HP, immobile for five seconds, then restores it; void/admin removal still works. No ordinary loot or XP farm.

**Not implemented:** physical corpse placement/live sacrifice, donor inventory/equipment/full jutsu loadout/eye powers, comprehensive sealing counters, complex squad command UI. The real donor can respawn normally. Old prototype names cannot honestly be migrated to real donor identities; retain them as unusable legacy entries rather than inventing provenance. Already-existing old prototype random mobs are not retroactively deleted.

Future development should extend a versioned, allowlisted donor combat profile and authoritative server AI; do not serialize entire players or recreate unrestricted inventories to achieve it.

## 6. Genjutsu — anime-style private scenes

Main code: [GenjutsuSession](src/main/java/net/narutomod/GenjutsuSession.java), [GenjutsuScene](src/main/java/net/narutomod/client/GenjutsuScene.java), [GenjutsuStageRenderer](src/main/java/net/narutomod/client/GenjutsuStageRenderer.java), [GenjutsuCastingModel](src/main/java/net/narutomod/client/GenjutsuCastingModel.java), [ClientGenjutsuOverlay](src/main/java/net/narutomod/client/ClientGenjutsuOverlay.java), [ItemInton](src/main/java/net/narutomod/item/ItemInton.java). See [scene design](docs/genjutsu_anime_refresh.md).

| Technique | Presentation and gameplay identity |
| --- | --- |
| False Opening | Offset caster echoes, lavender spatial deception, inverted movement/mild slow, up to eight seconds. |
| Memory Fracture | Violet corridor and caster echoes, fatigue, roughly five–ten seconds. |
| Murder Intent | Looming caster/tomoe, slow/weakness and jutsu suppression, roughly five–ten seconds. |
| Illusionary Execution | Red restraint/cross scene; victim skin and caster blades; initial mental damage, short restraint/suppression, roughly five–six seconds. |
| Burning Coffin | Closing wood/coffin/flame illusion; initial mental effects and restraint, roughly five–six seconds. **No actual ignition from the illusion.** |

These are client-rendered private stages, **not dimensions or server teleports**. Observers still see the victim's actual body in the battlefield. The attacker in scenes uses the caster's skin when available, victim actors use the victim's skin, and standard/slim models must work. A packaged Blockbench actor is a fallback, not a replacement for every player's skin.

The private render target is width-bounded (1,280 maximum), with a lighter 2D fallback if framebuffer support is unavailable. Cast choreography lasts 31 ticks; Sharingan-required casts use the Sharingan sound. Schedule audio from tick events, not every render frame. Preserve readable transitions, not uncontrolled strobing.

Server sessions are temporary UUID-indexed state. Type ≥2 suppresses jutsu except Chakra Pulse; severe types ≥3 also restrict melee/mining and damp horizontal motion. Owned movement/weakness modifiers use dedicated UUIDs, so cleanup removes only this system's effects—not every potion another system applied.

Expiry, death, logout, dimension/world exit, recast and escape must clear camera/UI/input and server restrictions. Physical damage after an eight-tick grace period breaks the session; event ordering is a review item because the current hook is before final accepted damage. Chakra Pulse keeps its XP-strength escape logic and compatibility with older genjutsu. Never persist paralysis or delete learned abilities to simulate an illusion.

## 7. Custom combat techniques and presentation

The [full catalog](docs/CUSTOM_JUTSU_CATALOG.md) lists technique symbols, indices, callbacks, scroll registry names and runtime assets. This section explains the most failure-prone mechanisms. Numeric damage below is **before armor, resistance, effective-health compression and other event hooks**.

### Shared charging and targeting

The existing held-use/release path computes power from elapsed use ticks, chakra modifier, learned mastery and callback limits. It also caps power by available chakra; “hold long enough” is not a promise of full power with insufficient resources. Animation callbacks decorate that path, not replace it.

Owner/team/owned-clone/summon/Susanoo filtering, creative/spectator protection, player PvP permission and substitution untargetability must be preserved together. Validate both directions of team relationships where code does so. New Bloodline attacks and Four-Pillar emit secondary effects only after accepted damage and do not force hits by resetting immunity. That is **not universal legacy behavior**: older canonical callbacks such as Wave of Inspiration, Water Blade and Flower Scattering Dance need an explicit accepted-hit/immunity review during the merger, not a silent blanket rewrite.

### Twin Flame Dragons, Flame Company and Shadow Clone Throw

Source: [BloodlineTechniques](src/main/java/net/narutomod/item/BloodlineTechniques.java), [RenderBloodlineTechniques](src/main/java/net/narutomod/item/RenderBloodlineTechniques.java), [v3 repair notes](docs/bloodline_impact_fixes.md).

- **Twin Flame Dragons:** Katon index 10, A rank, base 140. Power 1…2.8. Two curved descents begin at ticks 12/24 and travel about 30 ticks; swept entity/block collision includes water. Each dragon's raw base damage is `(8 + 10*p)*(1 + 0.25*m)`, radius `4 + 1.15*(p-1)`. Distance is measured to target hitboxes, with edge falloff to 60%. Accepted hits burn five seconds and knock back. The current tree additionally multiplies this blast by the caster's enabled Chakra-stat damage factor; the preserved v3 JAR predates it.
- Twin's `EventSphericalExplosion` is for terrain/fire/particles, **not a second generic entity-damage pass**. Terrain radius rounds from 60% of attack radius (bounded 2…4), with block resistance and Forge mobGriefing checks, fire chance 0.15. This does not by itself prove compatibility with every claim/protection mod.
- **Flame Company:** Katon index 11, B rank, base 110, power 1…2. Three orbs remain for 200…360 ticks. Confirmed owner damage fires one, at most every 12 ticks, within 20 blocks. Its own bolts cannot recursively trigger more bolts. Water extinguishes the company. A bolt gathers for ten ticks, homes toward the torso at 0.7…0.95 blocks/tick, expires by 65 ticks and uses swept hitboxes. Base damage `(6+7*p)*(1+0.25*m)`, radius 1.6…2.4, same falloff, three-second accepted burn, no terrain blast. Current stat scaling is the same shared blast multiplier as Twin.
- **Clone Throw:** Ninjutsu index 20, B rank, base 45, power 1…2. It consumes one eligible owned existing shadow clone within ten blocks only after successful throw spawn. The projectile explicitly advances once per server tick because `noAI` disables ordinary travel; overridden vanilla travel must not also move it. Speed 0.95…1.4, gravity −0.035, damping 0.98, lifetime 30 ticks. Hit damage `(6+7*p)*(1+0.25*m)`, brief accepted slow and motion damping; it can intercept a small hostile projectile. Misses still consume the clone/cast. It is **not** an explosive clone. It currently does not receive the new Bloodline blast stat multiplier.

Rendering uses the native red dragon, flames and fireball textures, including a full articulated 17-segment dragon body and large head, not arbitrary colored spheres or vanilla fire quads. The fire dragon has a safe model branch instead of casting itself to `EntityWaterDragon`—the former cast was a world-crash source. Load old transient attacks conservatively; saved completed attacks must not replay explosions after reload.

### Fire Phoenix

[PhoenixFlight](src/main/java/net/narutomod/PhoenixFlight.java) and [ItemKaton](src/main/java/net/narutomod/item/ItemKaton.java): power 0.8…4 enlarges the bird; base damage `12+8*p`; speed starts 0.22 and accelerates 0.028/tick to 0.82. It acquires a visible forward target within eight blocks and pursues after closing. Substitution breaks targeting and water walls counter it; blocks, swept collision, lifetime and leash still constrain it. One impact damage event, not a duplicate explosion. Other projectiles did not all become globally homing.

### Four-Pillar Bind

[ItemCanonicalJutsu.FourPillarBind](src/main/java/net/narutomod/item/ItemCanonicalJutsu.java), [LightningBindVisual](src/main/java/net/narutomod/client/LightningBindVisual.java): Raiton index 9, A rank, base 140, normal internal power 1…2, charge delay 32. Its 0–100 display maps `c=clamp(p-1,0,1)`.

```text
height = clamp(1.35 * targetHeight + 1, 4.5, 96)
radius = clamp(0.75 * maxTargetHorizontalWidth + 1.3, 2, 64)
damage = (6 + 12*c) * (1 + 0.35*sqrt(clamp(NinjaXP,0,100000)/100000))
                       * (1 + 0.25*clamp(mastery,0,1))
durationTicks = 60 + round(50*c)
```

One valid hostile/PvP target within 24 blocks. Maximum formula damage is 30.375. A rejected damage event grants no paralysis/cage. Accepted hit applies paralysis amplifier 1 for the computed duration and fatigue amplifier 3 for 80 ticks. Explicit cooldown 520 ticks remains subject to its setter/shared policy; no hidden extra generic stagger.

Stone pillars rise for eight ticks and fade over 12, with bounded electrical arcs, rise/impact/sustain sound and shake. Geometry stays 4,784 vertices even for huge targets; these are rendered pillars, not world blocks. VFX packet kind 8 uses `scale=height`, `element=round(radius*1000)`; valid radius encoding 2,000…64,000, legacy zero means radius 2. Do not reinterpret other effect kinds. Client budgets: eight cages and 256 effects; lower particle settings reduce arc detail.

### Paper-bomb ninjutsu

Sources: [ItemExplosiveArt](src/main/java/net/narutomod/item/ItemExplosiveArt.java), [EntityPaperBombCast](src/main/java/net/narutomod/entity/EntityPaperBombCast.java), [PaperBombAmmo](src/main/java/net/narutomod/PaperBombAmmo.java), [PaperBombPlacement](src/main/java/net/narutomod/PaperBombPlacement.java), [PaperBombPolicy](src/main/java/net/narutomod/PaperBombPolicy.java). Full details: [paper-bomb guide](docs/paper_bomb_jutsu.md).

| Technique | Tags / base chakra | Explicit cooldown | Behavior |
| --- | ---: | ---: | --- |
| Tag Volley (C) | 3 / 60 | 160 ticks | Three swept fan throws, stick to blocks/entities, 21-tick fuse, bounded flight. |
| Snare Circuit (B) | 4 / 100 | 360 | Four forgiving terrain-probed corners, arms after one second, trigger/warning/inward blasts; maximum two per caster, 30-second expiry. |
| Seeking Tag Swarm (A) | 6 / 160 | 440 | Visible target within 24 blocks, limited turning; once LOS breaks it becomes ballistic, maximum one cast active. |
| Breaching Seal (B) | 5 / 130 | 320 | Surface within six blocks or forward fallback, five seals, two-second fuse, five-block cone (dot ≥0.5). Knockback, not automatic shield bypass. |

Uses normal Explosive Art item selection/held use (minimum 12 ticks), actual tags in main/offhand, including creative; backpacks are not searched. Validate spawn/placement and ammo before charging; failed casts spend neither, misses do not refund. Uses chakra, not a new generic combat stamina system.

Blast bases are 10/9/7/22 respectively. Per-target repeated-hit factors 1/0.55/0.3 and linear radius falloff constrain tag stacking; same-tick damage aggregates into one accepted hit and respects hurt immunity. Terrain remains intact: no ignition, block drops or explosive chain reaction. Only grass/ferns, including double plants, can be cleared after a committed placement, with edit/Forge break checks; do not erase crops, flowers or soil.

The prior disconnect was malformed particle data: `ITEM_CRACK` needs **item ID and metadata**, e.g. metadata zero, not a one-element array. `BLOCK_DUST` uses its own block-state-ID format. Do not generalize one particle type's payload to another. Preserve the earth-particle fix while retaining the reverted earth VFX style.

## 8. Earlier RP/progression systems that also belong in the merge

### Stats, sheet and training

[PlayerStats](src/main/java/net/narutomod/PlayerStats.java), [PlayerTracker](src/main/java/net/narutomod/PlayerTracker.java), [Chakra](src/main/java/net/narutomod/Chakra.java), [GuiPlayerStats](src/main/java/net/narutomod/client/GuiPlayerStats.java), [CommandRPStats](src/main/java/net/narutomod/command/CommandRPStats.java): six stats (Speed, Strength, Resistance, Health, Chakra, SPI), available/total points, +1/+10/MAX allocation, rank/clan/affinity dossier and Uchiha progression. Point spending is server-authoritative; points, Ninja XP and individual Jutsu XP are distinct quantities.

Default **per-stat** rank caps: None 100, Genin 250, Chunin 600, Jonin 1,200, Hokage 2,500. A cap does not grant that many points, XP or all six fully capped stats. Staff point reserves, per-player overrides and world rank limits remain separate. Technical stat maximum is 100,000,000; totals use long arithmetic. Stat root `NarutomodTrainingStats` remains data version **5**, including in the current stat revision.

Uchiha tomoe progression is handled in the sheet (each tomoe step costs one RP point); a retired large Sharingan auto-dodge bonus must not return. The smaller comparative Speed dodge remains. See [exact math and improvements](docs/STATS_AND_BALANCE.md) before porting any numerical change.

[ElementalTraining](src/main/java/net/narutomod/ElementalTraining.java) provides five nature-training scrolls, timed sequences and affinity acquisition. Typical awards are 25 XP, rising to 50 at 75% mastery, with a 1.2× perfect bonus and 2,500 element-XP cap. **Security gap:** current completion trusts client success/perfect flags after session matching; server replay/timing validation is not implemented. A declared 1.35× affinity-Jutsu-XP helper is not proof all XP award paths actually call it.

### Sharingan Copy

[ItemSharinganCopy](src/main/java/net/narutomod/item/ItemSharinganCopy.java): eligible owned three-tomoe Sharingan observes recent casts within 32 blocks (five-second observation window); attempt costs the larger of 150 chakra or 35% current chakra, with 400-second acquisition cooldown. Chance is 70%…90% by mastery. Copy rank unlocks D, then C/B/A at mastery 0.25/0.5/0.75; prohibited/S/bloodline abilities are excluded. Temporary owner-bound copy retains 60% observed Jutsu XP and a 60-second window, maximum one stored copy.

The current dirty tree changes copied-cast cost to the shared custom curve and writes a rank cooldown onto the copied item. That item is consumed on success; this is not a proven persistent player/source cooldown. Do not advertise complete anti-spam normalization. Acquisition cooldown is a separate pre-existing mechanism.

### Named Taijutsu and animal summons

[ItemTaijutsu](src/main/java/net/narutomod/item/ItemTaijutsu.java) and [CinematicTaijutsu](src/main/java/net/narutomod/item/CinematicTaijutsu.java) retain eight named skills: Leaf Whirlwind, Leaf Hurricane, Dynamic Entry, Primary Lotus, Lion Combo, Peregrine Falcon Drop, Drunken Fist, Leaf Drop. Their base costs are 35/55/65/95/85/95/75/65; ranks D/C/C/B/B/B/C/C. They use existing access/resource paths and authored player choreography, not the removed global M1/parry framework. Strength/Speed bonuses and jutsu-specific restraints still matter.

[ItemInuzuka](src/main/java/net/narutomod/item/ItemInuzuka.java): Ninken Companion, D rank/base 80, one owned dog, recast heals/updates rather than duplicates. Mastery drives HP 50…140, attack 4…14 and speed 0.36…0.48; follow/teleport/defend behaviors and native wolf-based visuals remain.

[EntityToadScout](src/main/java/net/narutomod/entity/EntityToadScout.java) and existing toad summoning provide charge tiers: below 2.5 scout (20 HP, 3 damage, sense radius 8); 2.5…5.9 mini-Gamakichi (40 HP, 6 damage, sense radius 12); ≥6 larger toad, ≥16 Gamabunta. Small scouts have follow/teleport and sensing rather than giant-summon combat stats.

### Missions, Bingo Book and RP documents

[MissionSystem](src/main/java/net/narutomod/MissionSystem.java), [MissionClient](src/main/java/net/narutomod/client/MissionClient.java), [ItemMissionPaperwork](src/main/java/net/narutomod/item/ItemMissionPaperwork.java), [ItemRPDocuments](src/main/java/net/narutomod/item/ItemRPDocuments.java): staff-authored D…S missions, travel/hostile-kill/hunt/manual-RP types, tracking, deadlines, rank/assignment constraints, Ryo/reputation rewards and Bingo targets. The automatic mission pool is empty; do not promise automatically generated content. Custom reward description text is not a universal executable reward engine.

Player data `NarutomodMissionIntel` includes active/taken missions, village reputation, bounty/infamy, target identity and last-seen information. World data `NarutomodMissionAdminRecords` stores staff records, notes/clues/arcs/events. Manual RP missions require staff completion; records alone do not implement automatic exams/mentoring.

`/adminmissions` and `/rpadmin` authorize operator level 4 **or Hokage rank**. That is actual server authority tied to an RP rank: review deliberately when merging another permissions system. Hunt resolution checks victim identity; bounty minima/rewards must be tested against the destination economy.

`/rpdocument <id|passport> <player> <village> [days]` issues owner-bound identity/passport snapshots with document ID, owner UUID/name, village/rank/clan, issuer and timestamps. `IssuedAt`/`ExpiresAt` are **wall-clock milliseconds**, unlike most combat tick deadlines. A snapshot is not an automatically updating permissions credential.

### Server-provided ambient music

[AmbientMusicSystem](src/main/java/net/narutomod/AmbientMusicSystem.java), [ClientAmbientMusic](src/main/java/net/narutomod/client/ClientAmbientMusic.java): server scans external `config/narutomod_server_music` OGGs, syncs a manifest and only missing cached files, and controls shared playback while clients retain local volume/mute. Those files are outside the packaged asset set; GitHub source upload does not supply them.

Server operator level 3: `/rpmusic list|rescan|sync|play|stop|now`. Client: `/music volume|mute|unmute|stop|now|cache`. Respect Minecraft music volume. Current bounds: 512 tracks, 128 MiB per track, chunks 24 KiB, up to three chunks per player per tick; SHA-256 and size validate cached data. These are large aggregate bandwidth/storage allowances, not evidence of a hardened public file service. Only distribute audio you have permission to use.

## 9. Retired work and unfinished intent

| Work | Correct treatment |
| --- | --- |
| Cursed Seal of Heaven overlays/wings | Shelved source/assets. `ItemCurseMark` has no discovery tag; keep `RemovedCursedSeals` missing-mapping compatibility. Do not reactivate. |
| Separate Luna Chidori/Raikiri | Disabled gameplay but retained registry compatibility. Raiton slot 10 is a false callback, excluded from active list; hidden scroll and entity ID 516 remain registered. Do not delete save-compatible stubs. Native Chidori and Chidori Senbon remain. |
| Generic M1/guard/parry/dash/tree | Removed/parked. Historical `.patch` and plans are not instructions to apply them. Active named Taijutsu is separate. |
| Experimental dojutsu wheel | Disabled; original selector required for unoperated players. |
| Earth/wind/water replacement VFX | Reverted by user; retain functional abilities and particle/crash fixes. Lightning/fire work is separate. |
| Edo corpse/sacrifice, complete donor combat loadout | Future work, not present simply because DNA/coffins exist. |
| Universal rebalance of every legacy jutsu | Not implemented in the current tree despite older proposal/status text. Do not silently normalize all original techniques during a merge. |

## 10. Step-by-step integration order

1. **Freeze inputs:** record both repositories' commits plus dirty/untracked manifests and runtime configs. Back up worlds/playerdata/registry ledgers. Confirm both inputs target Forge 1.12.2/Java 8 and whether the desired gameplay baseline is the preserved v3 build or current stat-edited source.
2. **Build each mod separately:** record exact toolchain, dependency resolution, mod IDs, namespaces and test results. Do not debug two unknown broken builds after merging them.
3. **Make a conflict map:** compare registries, packet discriminators, keybindings, entity IDs, creative tabs, attributes/modifier UUIDs, damage hooks, player-clone handlers, cooldown clocks, dimension assumptions and shared libraries. Define one owner for each shared service.
4. **Preserve data first:** retain the old namespace and keys or build explicit migration tables. Separate *stat Chakra* from *current chakra resource*. Migrate ocular ledger + socket NBT + item identities together; likewise Edo samples + claims + rosters + active NPC IDs. Avoid replayable migrations.
5. **Integrate foundational services:** stats/resource adapter, jutsu selection/charge/cost, owner/target eligibility, accepted-damage dispatch, safe entity lifecycle and networking. Do not multiply damage in both callback and central hook. Write adapter tests before ability-by-ability copies.
6. **Port legacy controls/eyes, then surgery:** validate untouched players first; then self/other surgery, consent races, inventories, death cloning, one/mixed eyes, EMS. Preserve rejected-feature exclusions.
7. **Port Madara and summons:** mount/attack collision first, then stage gating/seals/temporal safety. Edo authoritative archive before ritual/coffin/render polish. Use server ticks with synced visual progress.
8. **Port custom skills in dependency order:** shared scroll learning, particles, paper-bomb ammo/placement, Phoenix/Four-Pillar, genjutsu sessions, Bloodline techniques. Keep indices and serialize bounded state; test unloaded/reloaded attacks.
9. **Port RP services/assets:** named Taijutsu, ninken/toads, mission/document authority, training, server music. Use the asset map and resolve conflicts with the other 1.12.2 mod's render, packet and event systems. Keep attribution.
10. **Balance only in a named, separately approved pass:** use the stats companion's actual/proposed distinction; calibrate pools, damage, mitigation, sustain, cooldowns and summons together. Preserve original jutsu costs unless a broader change is explicitly chosen.
11. **Run automated checks and two-client copied-world acceptance:** see the verification matrix. Also launch a dedicated server to catch client-class loading. Compare regressions against both input builds.
12. **Package and hand off:** same reobfuscated JAR on server/clients, hashes, configs, migration/rollback instructions, known failures, test evidence and asset sources. Never call compilation alone “tested in PvP.”

### Pasteable instruction for the receiving AI

> Read `AI_HANDOFF.md`, `docs/CUSTOM_JUTSU_CATALOG.md`, `docs/STATS_AND_BALANCE.md` and `docs/HANDOFF_VERIFICATION.md` completely, then inspect both Forge 1.12.2 mods' source. Treat older plans as historical where current code disagrees. First report the input baselines and a compatibility/conflict map. Preserve original controls for non-surgery players, side-neutral physical eye items, opted-in socket mechanics, retired-feature exclusions, existing charging and custom-scroll placement. Preserve save identities and server authority. Distinguish built v3 behavior from later dirty stat work; do not silently implement the future balance proposal or restore canceled systems. Merge one subsystem at a time with adapters, migration tests and dedicated-server/client acceptance. Report exactly which tests ran and which gameplay remains unverified; do not overwrite installed worlds/JARs or claim donor skills or all-jutsu scaling without implementing and testing them.

## 11. Preparing this project for GitHub

Include Java/resources/tests, build wrapper/configuration, editable models/exporters, these docs and attribution. Untracked `StatsPolicy.java`, `StatsRebalanceChecks.java` and `tools/stats/` are required for the current source state; a tracked-only export can fail to compile. Inspect all other untracked files before staging; this handoff does not stage anything.

Exclude private world/player data, server credentials, logs, caches and local-only connection settings. `logs/` is **not** currently ignored. `.zcode/` and local assistant/tool state need review, not blind publication. Audit local MCP/tool defaults in `tools/madara/blockbench.mjs` and `tools/cursemark/build.mjs` for connection tokens/settings before making the repository public; do not paste credentials into issues or this document.

The old source manifest is only an earlier changed-file snapshot, not the complete current inventory. Assets from the original mod, anime references, NYDOMOD or other internet sources are not automatically licensed for redistribution. Preserve `MADARA_EYES_CREDITS.txt` and review third-party permissions. Source availability on GitHub is not an asset license. Upload test binaries separately only if desired and permitted; Git-ignored `build/releases/` does not appear in a normal source push.
