# Verification, bug register and release checklist

Audit date: **27 September 2026**. Applies to the current 1.12.2 working tree, not automatically to every historical test JAR. Start with [AI_HANDOFF](../AI_HANDOFF.md). Findings below distinguish observed source defects, architectural risks and gameplay that simply remains untested. This documentation task did not implement the proposed fixes.

## 1. Evidence from this documentation pass

Java: local Temurin **8u472**, Gradle **4.9**, Forge **1.12.2-14.23.5.2855**, mappings `20171003-1.12`. Ran the normal `check` task plus all extended suites below, offline and without the daemon. Result: **BUILD SUCCESSFUL**, 50 seconds; 16 tasks executed, four up-to-date. Compilation was up-to-date; this was not a clean-room dependency download, new packaging pass or game launch.

| Task | Result reported by harness |
| --- | ---: |
| `verifyEdo` | 12,265 checks; includes 513-soul roundtrip, ownership, timeline, 68 textured cubes |
| `verifyEdoSouls` | 44 |
| `verifyEdoReanimation` | 15 |
| `verifyGenjutsuScenes` | 1,438,647 assertions |
| `verifyVisualRefresh` | 17,813 |
| `verifyMadaraTemporal` | 45 |
| `verifyMadaraSusanoo` | 108 |
| `verifyPhoenix` | 1,351 |
| `verifyLightningVfx` | 320,770; maximum 4,784 vertices |
| `checkBloodlineScrolls` | 160 |
| `checkBloodlineTechniques` | 42 |
| `checkMadaraModels` | 2,312,263 |
| `checkOcularSurgery` | 23,948 |
| `checkPaperBombs` | 503 |
| `checkStatsRebalance` | **582**, not the older document's 591 |
| `checkSusanooCombat` | 47 |

Large assertion counts include numeric/geometry sweeps, not millions of independent gameplay scenarios. Test bootstrap emitted Forge alternative-prefix warnings for synthetic registered items; no test failed. Gradle reported legacy deprecation warnings; upgrading Gradle casually is not a fix for a 1.12 toolchain.

No live two-client PvP, dedicated-server launch, production save migration, Blockbench MCP verification or sound listening was performed in this documentation pass. Prior Bloodlines work captured actual offscreen Java/OpenGL renderer images; that is useful visual evidence, not an end-to-end battle test. Current fixture previews are generated under `build/reports/`, which is ignored by Git.

### Reproduce the checks

PowerShell, Java 8 selected, from repository root:

```powershell
.\gradlew.bat -I tools/edo/verify.gradle -I tools/visuals/verify.gradle -I tools/madara/verify.gradle -I tools/phoenix/verify.gradle -I tools/vfx/verify.gradle verifyEdo verifyEdoSouls verifyEdoReanimation verifyGenjutsuScenes verifyVisualRefresh verifyMadaraTemporal verifyMadaraSusanoo verifyPhoenix verifyLightningVfx check --offline --no-daemon --console=plain
```

`--offline` requires an already populated dependency cache. On a fresh clone, resolve dependencies with network access first, record what resolved, then repeat reproducibly. This audit used the installed Gradle 4.9 distribution from the local wrapper cache; the portable command above uses the included wrapper. Bash equivalent uses `./gradlew`.

Normal packaging (not run as part of this documentation pass):

```powershell
.\gradlew.bat build --offline --no-daemon --console=plain
Get-FileHash .\build\libs\modid-1.0.jar -Algorithm SHA256
```

Verify the reobfuscation/build task graph and actual packaged classes/assets before calling it a release. Do not assume a successful `check` rebuilt the JAR. Preserve named old artifacts; do not overwrite a installed mod or a source-handoff snapshot to conduct a test.

Optional harnesses:

```powershell
.\gradlew.bat -I tools/stats/verify.gradle verifyStatsRebalance --offline --no-daemon
.\gradlew.bat -I tools/bloodlines/preview.gradle captureBloodlineRender --offline --no-daemon
.\gradlew.bat -I tools/ocular/preview.gradle captureOcularUi --offline --no-daemon
```

`checkStatsRebalance` is in normal `check`; `verifyStatsRebalance` only exists with its init script. Do not also import duplicate Madara verification scripts that declare the same tasks. Offscreen rendering depends on compatible graphics/context support and may not work on a headless server; it is optional, not a server runtime dependency.

### Identified artifacts, not assumed source equivalence

| File under `build/releases/` | Bytes | SHA-256 |
| --- | ---: | --- |
| `narutomod-ctrlz-0.3.2-beta-bloodlines-impact-v3-test.jar` | 32,971,264 | `4E8F4520E1246F3A62536C7C5B47E6475B97C86AD21A45A18CA609B74C2C876F` |
| `narutomod-ctrlz-0.3.2-beta-stats-rebalance.jar` | 32,973,671 | `5D9C2A74BFC53C9C30F67276254743CC41319787E1EC1E4FFE54038B2FB9D603` |

The second artifact is newer (September 26, 18:22 local filesystem time); `build/libs/modid-1.0.jar` had the same hash during this audit. It was not bytecode-diffed against every current source file, so its name/hash is identification, not proof every intended rebalance is included. Other older cursemark/combat/prototype JARs remain on disk; **do not install them merely because their timestamp/name seems relevant**. No JAR was copied into a launcher here.

## 2. Source-grounded bug and risk register

Priority is a suggested next-work order, not a claim that every risk has been reproduced in a live game. Each fix needs its own scoped code change and regression test.

| ID / priority | Evidence and impact | Recommended next action / acceptance |
| --- | --- | --- |
| AUTH-01 / high | [ElementalTraining](../src/main/java/net/narutomod/ElementalTraining.java) accepts client `success`/`perfect` after matching session/element, without replaying inputs or validating completion time in `finishTraining`. Client can claim progression. Tick cleanup checks expiry after an `isTraining` early return, making the expiry-cleanup branch unreachable at the boundary. | Server owns sequence, nonce, start/expiry and input timing; consume session once. Check expiry in completion and clean expired sessions independently of `isTraining`. Test forged/late/repeated completion, logout and movement. Do not trust a client-only minigame score. |
| EYE-01 / high review | [MedicalSurgery](../src/main/java/net/narutomod/MedicalSurgery.java) compares input state while intentionally ignoring volatile runtime fields, then commits selected session snapshots. Possible stale cooldown/strain/runtime rollback; not reproduced live here. | At atomic commit revalidate inventory identity, current sockets, ledger/revision/consent and qualified medic; merge permitted fresh volatile fields rather than restoring stale snapshots. Tests must exercise real `Session.complete`, not only pure inventory helpers. |
| EYE-02 / medium | Menu scanning stamps bare inventory eyes with UUIDs before consent; side provenance is reassigned to chosen socket. Older docs promised no mutation and immutable donor side. | Document actual side-neutral semantics (done in handoff). If transactional/no-mutation previews are desired, propose it separately. Verify cancel, full inventories, both inventories, duplicate IDs, extraction to medic and legacy split remainder ownership. |
| COPY-01 / high review | [ItemSharinganCopy](../src/main/java/net/narutomod/item/ItemSharinganCopy.java) writes a cooldown onto a copied stack immediately before consuming it. Tick callbacks receive fresh temporary original stacks; channel behavior is not normalized by that release cooldown. Existing acquisition cooldown is separate. | Decide intended acquisition/cast/channel cooldown policy. Enforce player+technique ledger if persistent cooldown is wanted. Test copied healing/other channels, failed release, duplicate copies, relog and dimension clocks; avoid charging twice. |
| STAT-01 / high balance | New damage factor is only consumed in [BloodlineTechniques.blast](../src/main/java/net/narutomod/item/BloodlineTechniques.java), not all ninjutsu. Enabling the flag still changes pools/charge behavior across other abilities. Large XP pools plus stationary regen can outpace fixed upkeep. | Preserve documented scope; add call-path tests and measured sustain cases. Approve the separate balance proposal before adding a global hook. Never apply the factor both locally and globally. |
| STAT-02 / medium | Enabled pool tooltip still uses old Chakra-stat bonus. Flag is copied once in preInit; no synchronization found. Data version remains 5, contradicting older migration notes. | Derive tooltip from same policy, synchronize balance revision/config from server, test mismatched clients and existing saves. Do not invent a v6 migration in documentation. |
| DAMAGE-01 / high integration | Absolute damage bypasses current Resistance/HP compression; older canonical callbacks may reset `hurtResistantTime` or apply status after rejected hits. New Bloodline/Four-Pillar safeguards are not universal. | Audit each shared damage path and source flag with the destination mod's armor/substitution/claims hooks. Scope any behavior change explicitly; test accepted vs canceled final damage, no forced iframe reset, nonplayer sources and healing conversion. |
| GEN-01 / medium review | [GenjutsuSession](../src/main/java/net/narutomod/GenjutsuSession.java) reacts to a hurt-stage event for physical break. Another later handler could cancel the hit; this could break an illusion on damage that never lands. | Decide whether “attempted hit” or “accepted physical hit” is intended; for the latter, use final accepted event semantics without breaking source-specific escape behavior. Test canceled/substituted and zero-damage hits, eight-tick grace, recasts and cleanup. |
| AOE-01 / medium review | [ProcedureAoeCommand](../src/main/java/net/narutomod/procedure/ProcedureAoeCommand.java) has shared mutable builder state. Nested damage-event procs can be reentrant, especially when merging another combat mod. No live failure proved here. | Move per-cast state into instance/local immutable context in a separately tested refactor. Test nested on-hit Flame Company and multiple casters same tick. |
| EDO-01 / medium acceptance | Archive/claim/active-ID behavior has fixtures but no end-to-end restart/crash recovery test in this pass. Unlimited distinct rosters are intentional; packet windows are bounded, archive growth still needs load testing. | Test interruption at each claim/award/spawn boundary, duplicate DNA, offline refund, cross-dimension/unloaded NPC and stale-ID deletion. Test large rosters without imposing a surprise gameplay cap. |
| FX-01 / high regression | Paper-bomb malformed ITEM_CRACK argument count previously disconnected clients; fire dragon model cast previously crashed saved worlds. Fixes are present and checked. | Keep protocol-specific particle layouts and non-water model branch. Reopen copied worlds containing old attacks, test with ReplayMod if used, and confirm completion cannot replay explosions. |
| WORLD-01 / medium integration | Twin terrain explosion uses Forge mobGriefing/block resistance; this is not the same permission path as paper-bomb grass clearing. | Test protection/claim mods and PvP-disabled servers. Do not promise that every claimed block is safe until tested with that mod's event path. |
| ADMIN-01 / high trust decision | Hokage rank can authorize mission/document admin operations in addition to operator permissions. Music sync has large per-track/per-player bounds. | Preserve or replace permissions deliberately after staff approval; test malicious client requests, aggregate transfer budgets and server lag. Hash checks detect corruption, not authorization. |
| COOLDOWN-01 / medium review | Many ability cooldowns live on item NBT; clocks use world game time. A normal stack cooldown does not inherently stop duplicate-stack, copied-item or cross-world clock bypass. | Specify per-player vs per-item intent. Add persistent UUID+technique cooldown only where desired, with tested expiry conversion and no blanket original-jutsu redesign. |
| DOC-01 / fixed in this handoff | Old notes contain incorrect extraction recipient, donor-side guarantee, stat curves/global-conversion status and prototype Edo limitations. | Use this entrypoint and exact source. Historical notes have warning banners; future changes must update the relevant versioned specification/tests. |

## 3. Live acceptance matrix — still required

Use a copied world, two ordinary player accounts, identical JAR/config on server and clients, plus a dedicated-server process. Include standard/slim skins, creative/survival/spectator, allies/enemies, PvP disabled/enabled, low particle settings and altered frame rate. Record expected/actual results and logs; don't just write “seems fine.”

| Area | Required scenarios and pass conditions |
| --- | --- |
| Baseline/non-operated eyes | Existing character can browse/confirm/toggle as before, H does nothing, no wheel appears; death/login/dimension does not silently opt into surgery. Normal black iris renders at correct head-local height. |
| Surgery | Self and other-player menus list both inventories; any eye can go in either socket; exact-layout consent; 3k XP gate, 100 success charge only, five-second channel/ten-second recovery; interrupts/refunds; extracted actual eye goes to medic; full inventories fail safely. Concurrent inventory moves and recasts cannot duplicate/lose eyes or restore spent cooldowns. |
| Advanced eyes | One eye/mixed types use correct socket skills/cost/cover; incompatible pair no Susanoo; family-recorded compatible donor MS yields recipient-family EMS; no unrelated power grant. Login/death/server restart preserves ledger identity. |
| Madara | Each stage XP gate, normal MS vs EMS Perfect, 40k threshold, armored burst/recovery, repeated G/H/switches; rider stays mounted; own jutsu clears own model while enemy armor remains hittable; extra-arm seals synchronized to observers. Temporal return never enters blocked/unloaded space or duplicates state. |
| Edo | Real donor death with/without keepInventory, blank/duplicate DNA rejection, trade/claim/interrupt/refund, smoke/sound/coffin timeline, roster owner isolation/window selection, one active NPC, recall/cancel/follow/guard/passive, reform/no loot, logout/distance/dimension/upkeep cleanup. Reopen saves at each timeline stage. |
| Genjutsu | Caster/victim skins, observers see actual body, Sharingan-required sound, FBO and fallback, no real Burning Coffin fire, escaped/expired/dead/logged-out victim regains camera/control, unrelated potions remain. Physical-hit event ordering and Chakra Pulse checked with other damage hooks. |
| Bloodline trio | Scrolls appear/learn in CUSTOM JUTSU, normal charge changes size/price, twin both impacts actually damage; correct owner/filter/cover/water/substitution; no dragon class cast on rejoin; company procs only accepted owner hits and not itself; clone physically flies/intercepts/hits and consumed miss has cost. Audio is audible and not layered every frame. |
| Phoenix/Four-Pillar | Phoenix full/tap size/speed/lock/counters and single damage; Four-Pillar charge 0/50/100, low-chakra cap, player and huge summon sizes, XP/mastery math, no cage after canceled damage, client geometry bounds and cleanup. |
| Paper bombs/earth | Exact ammo main/offhand including creative; insufficient ammo/spawn/placement fail without payment; circuits on grass/ferns/slabs/uneven ground, claim restrictions; safe terrain, no crop deletion, bounded active casts. ReplayMod/particle serialization no disconnect; no duplicate damage per tag tick. |
| Stats/resources | Existing v5 characters at each cap, legal equal-budget builds, flag/config agreement, charge/cost/tooltip match, regen stationary/damage/sleep gates, active upkeep, extreme stats, absolute damage/healing and duplicate attribute UUIDs. Original jutsu behavior compared to baseline. |
| RP services | Staff mission create/accept/progress/complete once, manual mission approval, bounty correct victim, unauthorized requests denied; document expiry survives restart; training forged/expired input addressed before hostile public-server rollout. |
| Music/assets/performance | Missing-cache transfer/reconnect/volume/mute/stop, invalid files and limits, no blocked server tick from many downloads; missing texture/sound checks, large battles FPS/network budget, low-FPS sound does not duplicate. |

## 4. Release and rollback checklist

1. Select one gameplay baseline and record source commit **plus dirty/untracked content**, configuration and hashes. The archived v3 and later stats JAR are not interchangeable.
2. Inspect `git status --short`, `git diff --ignore-space-at-eol`, and untracked required source. Never reset unrelated work to make the tree “clean.” Do not include logs/private worlds/local tool credentials in GitHub.
3. Keep namespace, technique/scroll slots, hidden retired entities/items and saved-data schema. Compile and run normal/extended checks. Run dedicated-server plus matrix above; attach evidence and unresolved cases.
4. Package a named reobfuscated test artifact in a separate output location; verify expected runtime classes/models/textures/sound definitions in the archive. Hash it. Avoid simultaneously loading an old and new NarutoMod JAR.
5. Back up the entire test world's playerdata and saved-data ledgers, not only inventory NBT. Maintain the old JAR and matching config with that backup.
6. Test migrations only on the copy first. An irreversible socket or schema conversion may require restoring the matching backup, not merely swapping back to an old binary.
7. User/admin chooses installation and publication. This handoff did neither. Report source changes, artifact hash, tests passed, gameplay not tested, migration requirements and known risks separately.
