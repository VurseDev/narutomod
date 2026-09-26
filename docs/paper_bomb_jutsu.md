# Explosive Art — first playable implementation (Minecraft 1.12.2)

Four original paper-bomb techniques, using the existing explosive tag item and artwork.
No changes to Susanoo models, Edo Tensei, curse marks, or genjutsu designs are included in this pass.

## Obtain and cast

The **Custom Jutsus** creative tab contains Explosive Art and four learning scrolls.
Survival players learn with the normal scroll GUI and follow the existing ninja training/XP requirements.
The first learned technique creates a player-owned Explosive Art item; subsequent scrolls add to it.
Learning the same technique twice does not consume another scroll. There are no new loot drops or recipes.

Select a technique using the usual jutsu selector. Hold use for at least **0.6 seconds**, then release.
Short releases cancel without spending resources. Paper rustling and a hand-seal pose telegraph casting.
The third-person animation keeps the player's own skin; first-person retains the existing jutsu use pose.

| Technique | Rank | Explosive tags | Base chakra | Cooldown |
| --- | --- | ---: | ---: | ---: |
| Tag Volley | C | 3 | 60 | 8 seconds |
| Snare Circuit | B | 4 | 100 | 18 seconds |
| Seeking Tag Swarm | A | 6 | 160 | 22 seconds |
| Breaching Seal | B | 5 | 130 | 16 seconds |

Chakra uses the existing custom-jutsu mastery, large-pool and tenketsu cost rules; the table shows base costs.
Mastery never reduces ammunition. Tags are counted across main inventory and offhand, not backpacks or armor slots.
Creative casts also require/consume ammunition. Chakra mode, not stamina mode, is required.

### Technique behavior

- **Tag Volley:** three swept-projectile tags fan out, flutter, then stick to a living target or solid surface.
  A visible one-second fuse follows attachment. Maximum flight: four seconds / 28 blocks from cast origin.
- **Snare Circuit:** aim at ground within ten blocks, or aim forward to snap to ground four blocks ahead.
  The four tags independently follow nearby ground heights (including one-block steps and slabs);
  the footprint no longer needs to be perfectly flat or completely empty. Unsupported cliff corners still reject placement.
  Four corner tags arm after one second. A valid target in the central four-by-four area triggers a warning;
  tags slide inward and burst in a staggered sequence. Maximum two active circuits per caster; untriggered circuits expire after 30 seconds.
- **Seeking Tag Swarm:** aim at a visible living target within 24 blocks. Six tags have limited turning speed.
  A tag permanently loses guidance when its target breaks line of sight, then follows a falling trajectory.
  Cover catches tags, and misses expire. Maximum one active swarm per caster.
- **Breaching Seal:** place a five-tag bundle on a solid surface within six blocks, or aim forward to snap to ground three blocks ahead.
  Its two-second fuse gives time to move.
  The blast has a five-block, 60-degree half-angle cone: outward from a wall, or forward along ground/ceiling.
  Stronger knockback and normal Minecraft shield/durability interaction; it does not destroy walls or bypass shields.

All explosions are **terrain-safe**: no block destruction, ignition, item drops, or chain-detonation of nearby tags.
Successful circuit/breach placement clears nearby tall grass and ferns (including double-height plants), without drops.
This clearing happens only after successful spawn and ammunition consumption, respects edit permissions and Forge block-break cancellation,
and leaves flowers, crops, grass soil blocks, and other solid blocks intact. Failed/canceled casts do not clear anything.
Visuals use the mod's original inked tag texture, hinged paper flutter, warm fuse pulses, smoke, embers and paper fragments.
Sounds reuse `paperflip` plus vanilla priming/explosion effects.

## Safety and balance

- Costs/ownership/cooldowns are checked on the server at cast start and release. The entity must spawn successfully before ammunition is debited; chakra is spent by the existing jutsu pipeline only after callback success.
- Invalid targets, invalid placement, short wind-up, canceled spawn and insufficient ammunition spend no tags/chakra.
  A successful cast that later misses, expires, or is interrupted is not refunded.
- Caster-side cooldowns prevent duplicate Explosive Art items from bypassing recovery. Their lifetime follows the player's existing Forge entity data (death may reset that data).
- The owner, their own Susanoo, friendly scoreboard teammates, owned summons/clones, friendly tameables, spectators, creative players and temporarily untargetable entities are excluded.
  Player targeting also respects server PvP and team friendly-fire rules. Custom RP alliances not represented by those existing APIs are not inferred.
- Solid cover blocks blast damage. Shield-facing calculations use the actual blast position.
- Subsequent tags from one cast deal 55%, then 30% of base damage, with distance falloff.
  Same-tick impacts are aggregated into one hit. Vanilla invulnerability frames remain intact; staggered hits may be absorbed by those frames.
- Active effects cancel when their owner dies, disconnects, leaves the dimension or moves over 48 blocks from cast origin.
  Paid effects are transient: reloading/unloading a saved effect cancels it rather than resurrecting a trap. There are no refunds on reload.
- Surface placement and grass clearing check vanilla edit/spawn protection; grass clearing also posts Forge block-break events.
  Damage goes through normal Forge living-damage hooks; third-party claim/region mods still need integration testing.

## IDs for testing

```text
/give @p narutomod:explosive_art
/give @p narutomod:explosive_tag 64
/give @p narutomod:scroll_tag_volley
/give @p narutomod:scroll_snare_circuit
/give @p narutomod:scroll_seeking_tag_swarm
/give @p narutomod:scroll_breaching_seal
```

Implementation: `item/ItemExplosiveArt.java`, `entity/EntityPaperBombCast.java`, `client/RenderPaperBombCast.java`,
`client/ClientPaperBombCasting.java`, `PaperBombAmmo.java`, `PaperBombPolicy.java`.
Scroll definitions append to the existing array to preserve previous IDs. Extra-scroll network actions now resolve
the sender's open container, avoiding the existing shared per-GUI map during simultaneous learning.

## Verification

Explosion hotfix: the original debris packet omitted the ITEM_CRACK metadata argument. This produced
`ArrayIndexOutOfBoundsException: 1` in `SPacketParticles.writePacketData`, including when ReplayMod
serialized an integrated-server packet. Debris now sends `[itemId, 0]`. The regression test reproduces
the original exception and verifies packet encode/decode after the fix. Gameplay values are unchanged.

`gradle checkPaperBombs` checks real stack consumption, wind-up boundaries, falloff, cone boundaries, swept collision/cover,
owner/ally/summon/untargetable filtering, reload cancellation, and both languages/model resources.
Placement regressions additionally cover forward-aim snapping, walls blocking fallback, one-block steps, slabs, unloaded chunks,
unsupported ground, double-grass clearing and protection of plants/solid blocks outside the grass allowlist.
`gradle build` also runs the existing Madara model and Susanoo combat regression executables.
Fixtures are not a substitute for a running Forge server/client.

Before an RP server rollout, test:

1. Two players learning the same and different scrolls simultaneously; offhand scrolls; repeated/forged clicks; full inventory.
2. All four techniques with exact ammunition, one tag short, split stacks and offhand tags; short release and low chakra.
3. Duplicate-item cooldowns, failed placement, an empty swarm target and canceled spawn events.
4. Moving targets, close walls, intervening allies, mounted Susanoo casts, protection plugins, and PvP disabled.
5. Circuit arming/expiry/limit, uneven ground and mining the supporting blocks; swarm cover escape and misses.
6. Shields facing toward/away from a breach; multiple tags striking together; verify terrain remains untouched.
7. Disconnect/death/dimension changes and chunk/world reloads with active tags.
8. First/third-person, both skin arm widths, armor, resource reload, late tracking clients and particle settings.

This is a test build. In-game visual quality, latency behavior, third-party compatibility and final damage tuning remain playtest items.
