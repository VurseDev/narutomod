# Bloodline jutsu impact repair — 2026-09-26

Scope: Twin Flame Dragons, Flame Company and Shadow Clone Throw only. No changes to surgery, dojutsu selection, M1/parry, or existing elemental balance.

## Charging and resources

All three callbacks inherit `ItemJutsu.IJutsuCallback.onUsingTick` again. No percentage HUD, custom charging timer, new key, or separate charge packet. Holding/releasing the normal jutsu item uses `ItemJutsu.Base.getPower` and `onPlayerStoppedUsing`, including the existing chakra/training modifier, resource cap, mastery, cooldown and XP pipeline. Existing hand-sign presentation remains.

- Twin Dragons: power 1–2.8, power-up delay 24 (modified by the normal training/chakra calculation).
- Company and Clone Throw: power 1–2, delay 24.
- Damage is snapshotted at casting with a multiplier `1 + 0.25 * ItemJutsu.getJutsuMastery(...)`, using the existing learned/mastered range 0–1. This is not a replacement player-stat system; target resistance and other damage hooks still run normally.

## Gameplay repairs

Twin Dragons now sweep their flight paths against target hitboxes and cover instead of waiting for a tiny fixed-radius particle impact. Each dragon deals `(8 + 10 * power) * masteryMultiplier` base damage. Blast radius is `4 + 1.15 * (power - 1)` blocks. Damage falls to 60% at the radius edge, measured to the closest point of the target's hitbox so giant summons work. Both dragons travel for 30 ticks, with launch delays of 12 and 24 ticks. Normal same-point impacts are twelve ticks apart; no invulnerability reset. Different intervening obstacles can alter impact timing, and normal damage immunity is still respected.

The terrain blast reuses `EventSphericalExplosion`, radius 2–4 with block resistance and the existing Forge `mobGriefing` setting. It changes terrain/ignites fire but does not add a second entity damage pass. Victims receive one attributed jutsu/fire/explosion hit per dragon; accepted hits apply fire and knockback. Water quenches the attack.

Flame Company retains three ammunition charges and launches one orb per confirmed damaging attack. Its own bolts cannot recursively trigger it. The old final feet-distance check is gone: homing shots sweep actual hitboxes and aim at the torso. Orbs gather for ten ticks before flight so the triggering melee hit's normal damage window can expire. Shots remain limited by normal immunity, substitution, walls, water and team rules. Bolt damage is `(6 + 7 * power) * masteryMultiplier`; radius 1.6–2.4, same splash falloff. Small blasts do not dig craters. Company lasts 10–18 seconds, with a 12-tick firing cooldown and maximum targeting distance 20 blocks. Charging increases orb size, damage, blast radius, speed and duration, not ammunition.

Clone Throw previously disabled AI and then incorrectly relied on vanilla living travel, which skips movement for a no-AI entity in 1.12.2. It now owns one explicit server-side movement step and sweeps the full segment. It consumes one existing eligible owned shadow clone only after a successful spawn. Launch speed is 0.95–1.4 blocks/tick; damage `(6 + 7 * power) * masteryMultiplier`; an accepted hit briefly slows the target. It can intercept a small enemy projectile before a wall/target. Misses still spend the clone. It is not an explosive clone.

Damage excludes the caster, either-direction teammates, owned clones/summons/Susanoo, creative/spectator targets, denied PvP, and untargetable/substituted victims. A rejected hit applies no secondary burn, slow or knockback. KATON/NINJUTSU identification now participates in the mod's existing elemental/absorption interactions.

Short-lived attack progress is saved instead of restarting flight on reload. Legacy saved dragons without progress expire; completed casts cannot replay explosions. Thrown clone copies expire on load instead of becoming orphan player-inventory copies.

## Visuals

`RenderBloodlineTechniques` uses existing `dragon_red.png`, `flames_red.png`, and `fireball.png`. The dragon reuses the water dragon's head, UVs and segment geometry, with a separate 17-segment tapered articulated body. It does not cast a fire entity to `EntityWaterDragon.EC`, and water-dragon animation is unchanged. Head/body scale grows from 1.7 to 3.05 with charge, with flame mane, launch formation, corrected facing, and expanded render bounds. Company/bolts use larger animated native fireball cores and flame sheets, with projectile trails.

`models/bloodlines/flame_company_wisp.bbmodel` is an older design file, not the runtime renderer. It is not loaded by these jutsus; the runtime uses the Java renderer above.

## Verification and test procedure

Final build: `build/releases/narutomod-ctrlz-0.3.2-beta-bloodlines-impact-v3-test.jar`. Build/check succeeded with 42 Bloodline runtime checks and 160 scroll checks, plus the existing Madara/Susanoo, surgery, and paper-bomb suites. Actual offscreen dragon/orb renderer captures were generated and inspected; rectangular flame edges were corrected. The JAR was inspected for the three scrolls, native textures, and changed classes. It was **not** installed over the user's Prism JAR or tested in a live world by this change.

`checkBloodlineTechniques` exercises the real shared charge calculation, damage dispatch, player/tall-hitbox collisions, cover ordering, water-ray inclusion, friendly/untargetable exclusions, failed-hit secondary-effect suppression, clone movement and persisted dragon safety. It retains the previous world-crash model cast regression. Fixtures do not replace live multiplayer testing.

Optional `gradle -I tools/bloodlines/preview.gradle captureBloodlineRender --offline` captures the actual Java renderers and textures through an offscreen OpenGL context in `build/reports/bloodline-render`. Those images are isolated model previews, not in-game screenshots.

For Minecraft QA, back up the test world and install only the new Naruto test JAR (remove the previous Naruto JAR from that instance's mods folder). Do not modify the preserved handoff build. Scrolls remain in **Custom Jutsu**, under Twin Flame Dragons, Flame Company and Shadow Clone Throw; learn them through the same scroll system, then select them on Fire Release/Ninjutsu.

1. Compare a tap with a held/full charge on each jutsu; standard chakra charging feedback must appear.
2. Cast dragons toward a player-sized mob, a large summon, a wall, and water; check both impact timing, visible terrain blast with `mobGriefing=true`, no crater with it false, sound, and knockback.
3. Activate Company and land melee/ranged hits: a confirmed hit launches one orb, the bolt damages the target after its wind-up, and three bolts exhaust the cast. Check walls, water, allies and substitution.
4. Spawn a shadow clone and throw it at a distant target/projectile. Confirm visible travel, hit/interception, and clone consumption on a miss. No new clone should be awarded.
5. Reopen a test world containing a saved active dragon. Confirm no renderer cast crash or repeated saved explosion; also spawn the original Water Dragon as a regression check.
