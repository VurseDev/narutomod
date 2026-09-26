# NydoMod 5.0 VF reverse-engineering report

## Scope and confidence

Inspected `C:\Users\ocaua\Desktop\NydoMod 5.0 VF.jar` as a compiled Forge 1.20.1 mod. The jar contains no Java/Kotlin source, mappings, or build files, so this report is based on class names, bytecode signatures/control flow, registries, resources, and the included README. The main runtime paths were traced from input through server logic to client rendering for representative jutsu, combat attacks, VFX, animations, and cinematics.

The findings below are high-confidence architecture and data-flow observations. Exact formulas for every jutsu cannot be recovered without the original source or a running instrumented server, but the extension points and most important behavior are visible.

## Package and asset map

| Area | Evidence in the jar |
| --- | --- |
| Mod/runtime | Forge 47+ / Minecraft 1.20.1, mod id `nydomod`, version `5.0` |
| Classes | 3,196 class files; `client` 866, `entity` 692, `item` 479, `combat` 273, `ai` 123 |
| Registries | 240 entity types, 352 items, 18 blocks, 10 effects, 24 particle types, 6 menus |
| Models | 36 Gecko/Bedrock geometry JSONs, 574 vanilla model JSONs, 2 OBJ/MTL sets |
| Textures/audio | 925 PNGs, 164 OGG sounds, 4 language files |
| Animation | 32 entity animation JSONs, 175 player animation JSONs |
| VFX | 203 composition JSONs and 80 reusable presets |
| Cinematics | 8 JSON scenes, including Sharingan reveal, Shisui, Tsukuyomi, and Kirin |
| Data | 27 advancements, 10 clan definitions, 35 recipes, 5 structures, custom dimensions for `kamuidimension` and `bijuu_mindscape` |

The jar is organized as a framework rather than one class per visual effect. Server systems select a logical ID; client registries resolve that ID into animation/VFX/rendering data.

## Runtime architecture

```text
Player input / item use
        |
        v
Server jutsu or NYCombatManager
        |
        +--> hand-sign session, stamina/chakra validation, cooldowns
        +--> authoritative hitbox/entity/action timeline
        +--> network packet (animation, state, VFX, cinematic)
        |
        v
Client PlayerAnimator / GeckoLib / NydoVfxEngine / cinematic director
        |
        +--> model/mesh layers, particles and custom geometry
        +--> camera, post-processing, HUD and sound
```

Forge mixins bind the framework into vanilla rendering and inventory behavior. Important mixins include the combat camera, custom blindness, combat hotbar, stable entity spawning, and the protected jutsu-pack hand.

## Jutsu execution pipeline

`ItemJutsu.Base` is the common entry point for elemental, taijutsu, genjutsu, senjutsu, and utility jutsu.

1. Right-click checks ownership, ninja status, enabled state, cooldown, biju/Hyuga restrictions, and the current jutsu-pack row.
2. A `HandSignPlan` is created. It can use physical signs, virtual signs from the jutsu pack, or Sharingan-copy signs. The plan has movement count, interval, animation speed, and start/complete timing.
3. While the item is held, the server tracks elapsed cast ticks and computes power from base power, charge delay, chakra modifier, and the jutsu's accumulated use XP.
4. PlayerAnimator packets play the corresponding hand-sign or charge animation. The callback can run per-tick effects and can opt into custom chakra handling.
5. On release, the server executes the callback only if the plan is complete and the player can afford the cost. It consumes chakra unless the callback explicitly handles it, ends the cast, awards use XP, applies exhaustion, and starts cooldowns.
6. The callback normally creates a server entity or changes a server state. That object then sends sound, VFX, animation, and cinematic packets to clients.

`ItemJutsu.JutsuEnum` stores rank, required XP, chakra usage, callback, base power, and power-up delay. The default rank XP values observed are D 100, C 150, B 200, A 250, and S 400; individual jutsu override their own chakra and delay.

Representative registration values recovered from bytecode:

| Jutsu | Rank | Base chakra | Callback | Notes |
| --- | --- | ---: | --- | --- |
| Chidori | A | 150 | `EntityChidori$EC$Jutsu` | charge-based |
| Lightning Wolf | C | 20 | `EntityRaitonWolf$EC$Jutsu` | tracking beast |
| False Darkness | B | 100 | `EntityFalseDarkness$EC$Jutsu` | ranged lightning |
| Black Panther | S | 50 | `EntityLightningPanther$EC$Jutsu` | server mob/AI |
| Kirin | S | 1,500 | `EntityKirin$EC$Jutsu` | target lock, cinematic launch, long cooldown |
| Water Dragon | — | registered value plus callback charge | `EntityWaterDragon$EC$Jutsu` | callback manually consumes twice the registered cost |

Important balancing implication: chakra is not necessarily consumed in one uniform place. A callback may declare custom consumption and add extra costs, as Water Dragon does. Cooldowns can also be set in callback code, as Kirin does. Any port or rebalance should inspect both the `JutsuEnum` registration and the callback.

### Representative jutsu traces

**Kirin.** The callback requires at least power 1, spawns Kirin at a locked target on the server, plays the Kirin launch cinematic, applies a 300-tick blindness effect, and sets a 3,600-tick cooldown. Its power-up delay is 400 ticks and its maximum power is 1. The projectile/entity then drives its own segments, impact, and return strokes.

**Water Dragon.** The callback requires full charge, the user to be on the ground, and the user not to be in water. It manually consumes two times the registered Water Dragon chakra cost, creates the persistent projectile, and delegates appearance to the water VFX composition. Its base power is 0.9, power-up delay 150, and max power 5.

**Rasengan/Rasenshuriken.** These are server entities implementing `IJutsu` and `NydoPersistentVfxSource`. They keep owner/target information, grow/charge timing, collision and damage logic, and replayable VFX state. Their VFX is not tied to a cosmetic entity tick loop.

## Combat system

The combat subsystem is independent from legacy item jutsu casting and is server authoritative.

`NYCombatManager` owns a per-player `NYCombatState`. That state tracks guard meter, stamina, combo index and windows, cooldown lanes, parry/guard-break stagger, held/charged attacks, buffered input, dash charges, lock-on, confirmed targets, style progression, and runtime style state.

Styles currently registered are Basic Taijutsu, Rock Lee, Naruto, Hyuga, Matatabi, Uchiha, Toad Sage, and Weapon. A style selects authored light/heavy/dash actions based on player state and weapon.

An authored `CombatActionDefinition` contains:

- startup, active, recovery, whiff recovery and counter windows;
- armor/poise windows, movement and rotation modes;
- resource policy and weapon policy;
- one or more hit groups, transitions, and a compiled event timeline.

The timeline emits animation start, resource consumption, sound, motion, rotation, poise open/close, hit-group open/feedback/close, transition windows, and recovery. `AttackExecutor` begins the action, activates it after validation, advances the timeline each server tick, resolves hit groups, applies reactions/impulses, and finishes it.

Hit groups are not simple radius checks. They have a shape (AABB, arc, cone, or line), start/end ticks, target cap, damage/poise multipliers, hitstun, juggle/bounce behavior, guard/knockback multipliers, block/parry rules, PvP/PvE multipliers, line-of-sight, and air-tech rules. Hit resolution includes temporal/lag-compensated positions.

Guard, poise, lock-on, prediction, camera punch, hit feedback, and state synchronization have separate managers and network messages. Client prediction is visual/input-side; the server remains the authority for outcomes.

The combat progression layer stores style XP/level, skill points, unlocked skills, per-style modifiers, and hit awards. It modifies damage, cooldown, action speed, precision, tracking, dash behavior, stamina, and regen with configured floors/caps.

The supplied Boundless/Strongest-derived animation resources are licensed/credited in `mods.toml`. They cover jab, hook, uppercut, kicks, rolls, grapple, suplex, and similar actions and are exposed through `CombatAnimationCatalog` and `PlayerAnimationCombatBridge`.

## VFX engine

The strongest part of the mod is its data-driven VFX layer. The server sends only a composition ID plus origin, direction, optional source/target entity IDs, seed, scale, and color override. The client resolves the ID, simulates, culls, LODs, and renders it. There is no cosmetic entity synchronization for each particle.

The included README describes the intended contract: the server triggers a composition; the client owns emission, simulation, curves, trails, LOD, culling, budgets, and render passes.

`NydoVfxDefinition` supports duration, priority, max distance, bounds, source following, source anchors, direction inheritance, first-person hiding, scale modes, emitters, declared capacities, and material counts. An emitter can define:

- geometry: billboard, directional billboard, cross, cube, ring, impact dome, streak, shockwave, chakra sphere/orbits, spiral vortex, futon shuriken, wind gust, lightning bolt, trail, flame/fire volumes, and water-specific helix/lance/ribbon/dragon-head shapes;
- spawn shape: point, sphere, ring, cone, box, perimeter, or line;
- speed, lifetime, gravity, drag, turbulence, vortex, rotation/spin, size/alpha/color curves, variation, atlas, near-fade, and trails;
- collision mode/response/padding/restitution and fluid/entity impact IDs;
- material texture, blending, shader, depth write, and fullbright.

`NydoVfxPresetResolver` expands reusable presets with inheritance, merge rules, cycle detection, and a maximum depth. `NydoCurve` samples fixed curves at runtime. `NydoVfxEngine` queues trigger packets, admits effects by priority/distance, simulates particles, records budgets/diagnostics, and dispatches custom geometry passes. `NydoVfxGeometryRenderer` contains the actual billboard, ring, shockwave, spiral, chakra, trail, fire, water, wind, lightning, and futon geometry paths.

VFX families include six chakra compositions, twelve fire, four gravity, eighteen lightning, fifty-eight Susanoo, four taijutsu, eleven water, eight wind, two cinematic effects, and a preset library. Example composition assets are:

- `vfx/nydochakra/rasengan.json`: layered follow-source charge with curves and colors;
- `vfx/nydochakra/rasengan_impact.json`: short spiral-vortex impact;
- `vfx/nydofire/fireball_flames.json`: flame flipbooks, tapered streaks, wake, rocks, smoke, shockwave, sparks, and heat halo;
- `vfx/nydowater/water_dragon.json`: dragon head/body, mist, foam, drops, streamers, and source-width scaling.

The command surface includes `/nydovfx stats`, `/nydovfx list`, `/nydovfx reset_stats`, `/nydovfx clear`, and `/test particles nydo effect <id> [scale]`.

Persistent VFX sources solve tracking problems: an entity implements `NydoPersistentVfxSource`, replays its current composition to a player who starts tracking it, and exposes owner, pose, anchor, direction, and active-effect state. This is why a late-joining/tracking client can still see a Rasengan or Water Dragon.

## Models and rendering

The jar uses three rendering families:

1. **GeckoLib/Bedrock geometry.** Entity geometry JSONs use Bedrock 1.12-style bones, cubes, UVs, pivots, and texture dimensions. Water Dragon and several Susanoo variants use GeckoLib models/controllers.
2. **Custom vanilla meshes/renderers.** Kirin loads `models/entity/kirin/kirin_nydo_mesh.json` into a custom mesh containing positions, normals, UVs, and indexed triangles. Rasengan and Kirin use custom entity renderers rather than a generic model.
3. **Layered player/entity rendering.** Susanoo and dojutsu layers render recursive bones, tints, chakra smoke/surface, flame, headwear, item layers, iris overlays, and eye glow separately.

The geometry set includes 512x512 high-resolution assets, plus 256, 128, 64, 96, 128x64, 32, and 16-pixel assets. The high-resolution models are concentrated in Susanoo/Yata-style rigs and other showcase entities, while smaller effects use simpler textures.

Water Dragon’s callback starts `NydoWaterVfx.WATER_DRAGON`, plays a timed sound sequence (`WATERBLAST`, `WATERFALL`, `WATERSTREAM`), and starts the impact composition on collision. Its legacy `renderParticles()` path is empty, showing that visual simulation was moved into the VFX engine.

## Player animations and camera work

Player animation JSONs use Bedrock-like animation data (`format_version` 1.8.0, often `geckolib_format_version` 2). They keyframe bone rotations/positions with easing and explicit lengths. The library contains:

- combat basic/core and every registered style;
- charge, Rasengan, Chidori, Rasenshuriken, Shinra, and hand-sign loops;
- licensed Boundless combat actions;
- Sharingan reveal, Shisui caster/target, and Tsukuyomi caster/victim cinematics.

`PlayerAnimationCombatBridge` validates an animation ID/length and sends it to the client. `NYCombatAnimationMessage` and `PlayerAnimationMessage` synchronize the selected animation and duration. Camera mixins add hand bob, punch displacement, combat camera motion, FOV/angle hooks, and custom blindness instead of relying on vanilla hurt-camera behavior.

## Cinematics and genjutsu

`NydoCinematic` can play for one player, caster/target pairs, or nearby observers. A `NydoCinematicMessage` carries cinematic ID, caster/target IDs, start tick, seed, or stop state.

`NydoCinematicDefinition` parses a scene schema with duration, priority, HUD/input lock, skippability, snapshot anchors, letterbox/fade, camera tracks, scene tracks, actor animation cues, panels, markers, actor roles, color grading, lens compression/breathing, and FOV/roll. `NydoCinematicDirector` interpolates keyframes (including Catmull-Rom), resolves actor anchors, takes over the camera, suppresses local input when requested, and restores the player at the end.

The Tsukuyomi scene is a real scripted sequence rather than a screen tint: it hides HUD, locks input, uses a red/negative/desaturated vignette and lens profile, orbits the target with camera keyframes, triggers victim/caster animations, replaces the world presentation, reveals the cross, runs torture/impact markers, and returns to reality. Kirin similarly has a multi-second charge scene with actor-relative camera tracks and storm/charge markers.

## Eyes, chakra, and progression systems

The eye system is already designed for implants and asymmetric eyes. `DojutsuSlotInventory` stores left/right slots, active/visual eye, enable state, normal-eye fallback, death extraction, PvP theft, and owner/tracking synchronization. `DojutsuEyeRenderEngine` renders base texture, overlay, iris, static/rotated iris, calibration, and visibility transitions. A player render layer can render each half independently, Tenseigan/forehead marks, cinematic iris spins, and glow.

`Chakra` owns authoritative max/current amount, consumption, addition, regeneration, client mirrors, HUD snapshots, and sync sequence. Jutsu power reads the chakra modifier, so high chakra can affect charge output unless a callback caps or replaces it.

Sage Mode and Senninka are separate state machines with reserve pools, upkeep, activation thresholds, mode-specific tick logic, HUD sync, and cleanup. Dojutsu fatigue tracks blindness debt, exhaustion stages, continuous wear, temporary debt, logout/death handling, and recovery. Sharingan progression stores tomoe/XP and emotional gates; Mangekyo abilities track strain, eye sacrifice, and ability-specific hooks.

`PlayerTracker` owns battle XP/ninja level and identity repair; server/client config controls max chakra, starting values, XP multipliers, Susanoo mastery/health, Amaterasu scaling, and biju settings. This separation is useful because chakra, combat progression, ninja XP, and dojutsu fatigue do not need to share one NBT counter.

## AI, missions, and supporting systems

The AI stack is a separate runtime with profiles, blackboards, threat memory, action controllers, movement/terrain schedulers, target scans, line-of-sight checks, and per-tick work budgets. Actions have windup/active/recovery, cooldown, priority, interruption, movement/aim rules, and markers. Jutsu descriptors add chakra cost, preferred range, utility, power, release markers, impact profiles, and use policy. The ninja combat brain can choose between taijutsu and jutsu while applying the same hit/impact pipeline.

Dialogue, mission, shop, clan, nature progression, biju, dimensions, and admin command services all communicate through explicit network messages and saved server data. This makes the mod closer to a game framework than a single-purpose Naruto content pack.

## What this means for our mod

The most portable ideas are:

1. Keep damage, chakra, cooldown, ownership, and hit confirmation server-side. Let clients own only animation, VFX simulation, camera, and cosmetic layers.
2. Replace per-tick cosmetic entities with one trigger packet and a persistent-source replay hook for effects that must survive tracking changes.
3. Describe complex attacks as timelines with startup/active/recovery and hit groups, rather than hard-coding one collision check in each entity.
4. Separate authored animation IDs and VFX IDs from gameplay callbacks. A jutsu callback should request `water_dragon` or `rasengan`, not construct every particle itself.
5. Use reusable VFX presets and strict JSON validation so new jutsu can be built by composition instead of Java changes.
6. Keep progression domains separate: chakra, ninja XP, combat/style XP, dojutsu fatigue, and mode reserves should sync independently.
7. Use cinematic scene definitions for genjutsu and major transformations; do not try to reproduce them with only particles and a camera shake.

## Limits and next step

This inspection proves the architecture and representative runtime behavior, but not every formula in every callback. The original source is not present, and many classes depend on Forge, GeckoLib, PlayerAnimator, BendyLib, and the game runtime. If an exact port is needed, the next useful step is to run the jar in a clean 1.20.1 instance with logging/instrumentation or decompile selected classes with a Java decompiler, then compare packet traces against our current implementation.
