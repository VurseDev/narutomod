# Naruto-style genjutsu refresh — Forge 1.12.2

This supersedes the recent minimalist horror overlays, retaining the five established jutsu IDs, scrolls and concepts. Black regular eyes, retired cursed seals and the Edo prototype are preserved.

## Presentation

| Jutsu | Private vision | Temporary gameplay |
| --- | --- | --- |
| False Opening | Two displaced caster-skin echoes, lavender interference, inverted movement | Mild speed penalty; up to 8 seconds with existing mastery |
| Memory Fracture | Repeating violet corridor, many copies of the caster, crystalline fracture lines | Slower mining; 5–10 seconds |
| Murder Intent | Looming caster beneath a rotating three-tomoe iris, converging pressure lines | Slower movement, reduced melee damage, jutsu suppression; 5–10 seconds |
| Illusionary Execution | Red-sky cross field, victim's skin bound to the central cross, caster-skin attackers and staggered blades | Mental damage on cast; restraint and jutsu suppression; 5–6 seconds |
| Burning Coffin | Victim inside a wooden coffin; caster at its side, hinged lid closes, animated Minecraft flames | Mental damage/chakra cost on cast; restraint and jutsu suppression; 5–6 seconds. No real fire |

These are **private rendered stages, not server dimensions**. No teleportation, replacement body, cloned inventory or hidden real entity is needed. Other players continue to see and interact with the victim's normal body in the real world. The actual world remains simulated. HUD/hotbar and Chakra Pulse remain accessible. FBO-disabled configurations retain the lighter 2D presentation but cannot display the full 3D stages.

The caster's skin and classic/slim model come from Minecraft's player skin system. The victim uses their own skin. A textured Blockbench biped is used only when the caster is not a client player or cannot be resolved. No external account or skin download service was added.

Casting has a 31-tick raise/seal/release/lower arm animation in third person, plus an ocular flare visible to observers and a first-person iris reveal. Model swaps are restored after rendering and at frame end. Sharingan-required casts reuse `narutomod:sharingansfx`, with one private victim cue and an observer cue at the caster. Existing vanilla/mod audio provides blade, impact, closing lid, flame, echo and release beats. Audio runs on ticks, not rendered frames. There is no recurring full-screen inversion or strobe.

## Ownership and cleanup

Server sessions are memory-only. Speed and attack modifiers use two dedicated UUIDs with `setSaved(false)`. Cleanup removes only those modifiers, not unrelated potions, equipment, learned jutsus, chakra capacity, XP or inventories. Existing one-time mental damage and chakra consumption remain normal combat costs, not permanent changes to player capabilities.

Chakra Pulse retains the existing strength check. A successful break clears the session immediately. A subsequent damaging hit after the initial 8-tick grace period is another escape route. Expiry, target/caster death, disconnect, dimension change and world unload terminate sessions. Recasting replaces rather than stacks them. Severe effects cap at six seconds; other effects cap at ten. The legacy base Genjutsu and its older potion-based escape path remain separate.

## Assets and implementation

- `models/genjutsu/illusion_actor.bbmodel`: editable Blockbench model with embedded 64×64 texture.
- `models/custom/genjutsu_actor.json` and `textures/other/genjutsu_actor.png` under mod resources: actual MCP-authored geometry/UV/texture export, consumed by the fallback renderer.
- Blockbench MCP `check_model`: 6 cubes, 7 groups, zero errors/warnings. No extra animation runtime/plugin dependency required. The connected MCP could not export directly to disk, so its returned geometry and original PNG were packaged into the editable model and runtime assets.
- Runtime animation lives in `GenjutsuScene`, `GenjutsuCastingModel` and `ClientGenjutsuOverlay`; the `.bbmodel` is an editable actor source, not a claim that runtime scene tracks were authored in Blockbench.
- Existing mod `textures/blocks/fire_layer_0.png`: all 32 frames reused without modifying the texture.
- Own framebuffer (maximum 1280 pixels wide) and independent projection: no clearing of Minecraft's main depth buffer, world/camera mutation or secondary world loading.

## Verification

Run with Java 8 / Gradle 4.9:

```text
gradle -I tools/edo/verify.gradle -I tools/visuals/verify.gradle verifyGenjutsuScenes verifyVisualRefresh verifyEdo build --offline --no-daemon
```

Checks cover timing/policy boundaries, escape allowance, scene budgets/finite geometry, caster/victim assignment, exported UV/texture bounds, packet layout, prior eye/seal checks and Edo regressions. `build/reports/genjutsu/scene-previews.png` is a software-rasterized preview of the real scene geometry with fallback skins; wood/stone are preview approximations and this is not an in-game screenshot.

Still requires a live two-client playtest: actual caster and victim skins including slim arms; first/third-person and GUI scaling; all five casts and audio; observer visibility; successful/failed Chakra Pulse; physical-hit escape; expiry; recast; target/caster death, disconnect and dimension changes; coexistence with other status modifiers and armor/rendering mods. Automated geometry/policy checks do not establish those integration results. The build output is not automatically installed over the user's mods JAR.

## Reference use

The anime concepts were informed by the supplied [overview of Naruto genjutsu](https://ovicio.com.br/naruto-todos-os-tipos-de-genjutsu/) and the [Portuguese Naruto wiki](https://naruto.fandom.com/pt-br/wiki/Genjutsu): sensory deception, eye-triggered illusions and subjective mental environments. The named custom jutsus remain adaptations, not claims of exact canonical techniques. The supplied YouTube video and forum page could not be retrieved in this session. No unlicensed third-party asset pack or new external sound recording was imported.
