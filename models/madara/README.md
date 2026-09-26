# Madara MS / EMS test build

Minecraft Forge 1.12.2, Java 8. The dedicated Madara entity contains six stages:
ribcage, skeletal, humanoid, legged, armored and Perfect. Normal MS stops at
Armored with its existing 20-second burst/recovery policy; Perfect requires EMS.

## Assets and animation

The four `.bbmodel` files in this folder are the current editable assets. They
use Blockbench's Generic Model format with native meshes and embedded textures
and animation clips. The older `.mcp.json` files are historical snapshots from
the superseded prototype, not build inputs.

Every model surface comes from the existing mod's actual ModelBox vertices and
UVs in `reference/`. Skeleton is uniformly normalized by 0.30; Clothed and Winged
retain their authored dimensions. Added rear arms/face use copied subtrees.
The Winged source supplies Perfect's armor, head and arms, without its wings.
Only one original crown/hair subtree is retained to avoid intersecting crowns.
Humanoid omits the Clothed helmet/spikes; Armored restores those original parts.

The original RGBA atlases receive a uniform blue tint. Their alpha, detail and
UV placement are retained. The earlier flat swatches and custom limb shapes
are no longer used. Thin original planes remain planes represented as meshes;
Blockbench MCP 0.6.1's cube-only audit reports `NO_CUBES` for this valid format.
It is not a full mesh validator. The generator checks the source geometry/UVs,
and the Java checks load the actual runtime models and reject cyclic parents.
That MCP version also undercounts rotation keyframes (`rotations` instead of
Blockbench's `rotation` property). The editable file contains the actual keys;
the runtime uses the baked `sealPoses` data and does not depend on that counter.

The original Perfect forearm surface is partitioned at elbow/wrist/finger
boundaries; its outside silhouette and UVs are preserved in the bind pose.
Other stages articulate their existing hand/finger parts. Offline pose fitting
places paired hands at the chest. These baked rotations are used at runtime,
with server-synchronized seal phases, eased transitions, complementary rear
seals, release/cancel recovery and weapon hiding during seals. Runtime rendering
requires neither GeckoLib nor a newer Minecraft animation library.

## Rebuild and checks

With the project's Java 8 and Gradle 4.9 configuration:

```text
gradle -I tools/madara/verify.gradle exportLegacySusanoo --offline --no-daemon
node tools/madara/build_assets.mjs
gradle -I tools/edo/verify.gradle -I tools/visuals/verify.gradle -I tools/madara/verify.gradle verifyEdo verifyGenjutsuScenes verifyVisualRefresh verifyMadaraTemporal verifyMadaraSusanoo build --offline --no-daemon
```

Normal `build` now includes `checkMadaraModels`. Textured stage and casting
previews are generated in `build/reports/madara/`. Eye validation uses
`tools/madara/build_eyes.cjs --check` with the bundled `sharp` dependency.

## In-game test

Use a creative test world and open terrain. Equip the Madara eyes from the Eyes
creative tab, or use:

```text
/give @p narutomod:mangekyosharinganmadarahelmet
/give @p narutomod:mangekyosharinganmadaraeternalhelmet
```

Use your configured Special Jutsu 1 for Temporal Anchor/Reversal, Special Jutsu
2 for Susanoo and Cycle Jutsu while mounted to advance the stage. Let each stage
transition finish before cycling again. Use EMS to reach Perfect. Cast an
elemental jutsu while mounted to test front/rear hand seals; cancel a charge and
check that the hands return to idle. Hold a Chokuto to show the original blade.

Temporal Reversal retraces movement only: it does not rewind inventory, health,
XP or terrain. Test a blocked destination, dismounting, death, reconnect and
world changes to exercise its rejection/cleanup paths.

Automated checks and Blockbench previews do not constitute a multiplayer playtest.
A second client should verify the real rider remains visible, cast synchronization,
collision clearance, projectiles and cleanup. That live test is still outstanding.

## Susanoo finishing pass

- Passenger mapping remains `riddenByEntities` / `field_184244_h` for 1.12.2.
- Removed the occupied-entity collision bypass. `SusanooCombat` excludes only
  the caster's own armor; enemy armor stays collidable. Targeted raycasts,
  scalable projectiles, spikes, vanilla-helper jutsus and vanilla projectile
  impact events use this rule. Impact retargeting retains intervening blocks.
- Owner-attributed damage cannot damage their own Susanoo. Existing enemy damage
  resistances are retained. Scalable collision ordering compares actual hit
  distances so a target behind a wall cannot replace the nearer block hit.
- Madara despawns after dismount (including creative), allowing twenty initial
  ticks for passenger restoration on load. Existing death/logout/dimension,
  chakra, eye eligibility and tracked-summon cleanup remain in effect.
- Cancel/release now synchronizes the preceding cast phase and age. Hand recovery
  begins at the pose actually reached, including an early interrupted wind-up.
  Existing geometry, UVs, textures, idle weapons and stage progression are reused.
- `gradle build` runs actual collision/intersection/event regression checks plus
  model matrix checks for early interruption continuity and final idle recovery.

All multiplayer participants must use the same rebuilt JAR because the entity's
synchronized animation fields changed. In a disposable two-client world, check
owner/enemy fireballs, water streams, beams and targeted jutsus at every stage;
repeat with a wall behind the armor. Observe early cancellation and weapon return.
Then test low chakra, dismount, death, logout/rejoin, dimension travel, occupied
upgrade space and timed MS armor expiry. These live checks have not been performed
by the automated regression suite.
