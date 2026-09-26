# Edo Tensei: donor souls

This replaces the random-mob cast with a first playable player-donor loop. It keeps
the existing ground seal, sinking body, coffin mesh, falling lid, dirt particles,
and Naruto summoning/poof sounds. It creates a separate NPC; the real player is not
teleported, controlled, or prevented from respawning.

## Play it

1. A non-creative, non-spectator player death drops a **Sealed DNA Sample**. The
   sample records that player's identity, signed skin profile when available,
   and pre-death Ninja XP. It is tradeable and does not replace their inventory
   drops. This also works with `keepInventory` enabled.
2. Put the chosen donor's sample in your **offhand**. Use the existing Edo Tensei
   Ritual on the Ninjutsu item, aiming at a flat, clear 7-by-7 ritual space.
3. The donor's preview body sinks into the seal. Completing the ritual consumes
   the sample and adds the donor's name to your owner-bound **Summoning Souls**.
   Interrupted rituals return the sample to the caster, or drop it at the ritual
   if the caster is offline. Each donor is registered once per caster; there is
   no hard roster-size limit.
4. Use the normal Cycle Jutsu key on Summoning Souls to select a donor, then
   cast. Their coffin rises and releases a reanimated NPC with their recorded
   name and player skin. Cached signed skins work while the donor is offline;
   missing/unavailable skin textures fall back to Minecraft's default skin.
5. **Empty-hand right-click the NPC** to cycle follow/defend, guard, and passive
   follow. Sneak + empty-hand right-click dismisses it.
6. **Sneak + right-click Summoning Souls** instantly recalls the selected soul
   or cancels its pending coffin sequence. Recall requires no chakra or cooldown,
   and can release an unloaded summon; that old instance dismisses on reload.

Samples are produced by actual deaths. A blank `/give` specimen is intentionally
unusable, and cloned specimen NBT cannot register a second ritual after its server
record has been spent. Use a backed-up test world and a real survival-mode death
to exercise capture.

## Initial gameplay rules

- One active NPC per registered soul per caster, including across dimensions and
  chunk unloads. The roster remains reusable after recalling or losing a summon.
- The summon defends its caster and can assist against the caster's recent target.
  It respects caster team protection and the existing player-PvP check.
- NPC health is `min(80, 20 + sqrt(NinjaXP) * 0.2)`; melee damage is
  `min(10, 3 + sqrt(NinjaXP) * 0.025)`. Captured XP is bounded to 0–100,000.
- Maintenance costs 10 chakra per second per active NPC in survival. Depleted
  chakra, caster death/logout/dimension departure, or a distance over 96 blocks
  dismisses it. Creative casters do not pay maintenance.
- A lethal ordinary hit starts five seconds of immobile reformation at 1 HP,
  then restores the body. Small passive healing applies outside reformation.
  Void/admin removal can still remove the entity. No inventory, armor, loot,
  or XP items are copied from the donor.
- Ritual/summon startup costs, cooldowns and mastery requirements use the existing
  jutsu system. Skin and name come from the server record, not trusted item text.

This implementation does **not** yet reproduce a donor's learned jutsus, eye
abilities, full RP attributes, or equipment. Sacrificial bodies, advanced orders,
sealing counters and skill-specific Edo combat are future work. The specimen uses
a simple paper icon for now.

## Save compatibility and testing

Archive version 2 preserves old test souls but marks them unusable for real
summoning; they have no truthful donor identity to migrate. Existing vanilla test
mobs already in a world are not deleted. Saved legacy ritual sequences without
DNA end without awarding new random souls.

The server keeps source snapshots, exclusive sample claims, and active summon
IDs in `narutomod_edo_souls.dat`. Items synchronize only an eight-name roster
window. Repeated ritual completion is idempotent, and stale entities cannot clear
a replacement summon's active record.

Run with Java 8 / Gradle 4.9:

    gradle -I tools/edo/verify.gradle verifyEdo verifyEdoSouls verifyEdoReanimation build

These fixtures cover source capture before/after the existing death XP reset,
profile filtering, specimen authenticity, interrupted/completed claims, saved
active instances, stale recall, NPC stats/team protection/reformation/save data,
and the existing coffin/seal/timeline assets. They do not replace a live two-client
test of skin loading, AI navigation, death-drop pickup, latency, or sound.

Use the same new mod JAR on the client and server. This work does not modify the
installed Minecraft JAR automatically.
