# Stats & jutsu economy rebalance — historical working proposal

> **Superseded as an implementation report:** use [`STATS_AND_BALANCE.md`](STATS_AND_BALANCE.md) for audited current formulas and the new clearly separated proposal. Several completion claims and example values below do not match the current source.

Status: 2026-09-26, updated same day. **P1–P4 are implemented and passing** (`gradle check` → `checkStatsRebalance` 591 checks + all pre-existing fixtures); the approved defaults were used for the five §11 decisions (chakra-stat damage scaling yes, movement clamp +150%, XP HP share 0.002, all legacy jutsus flipped at once, §6 special S-rank costs). **Nothing is live-tested yet** — the §10 P5 rebalance sweep (NPC damage, healing, rewards, XP thresholds) and the two-client acceptance pass remain open. Per the house rule in `docs/merge_handoff.md`: fixtures prove math, not gameplay; never claim silent completion.

## 1. Scope and non-goals

- In scope: the six RP stats (`PlayerStats`), the chakra/stamina pool (`Chakra`), jutsu chakra costs, jutsu cooldowns, jutsu damage normalization, and consolidation of the stat commands.
- **Non-goals:** no changes to animation code (the hand-written animation systems stay exactly as they are — see `memory`/audit note; they deliberately do not use GeckoLib), no GeckoLib runtime added, no `JutsuEnum` index renumbering, no NBT key changes, no registry renames, legacy command aliases stay registered for compatibility, curse marks stay retired, Luna's Raikiri stays disabled.
- Stat **spending stays capped**: rank per-stat caps + personal caps + a single point reserve, exactly as today. Only the *effect curves* and the economy change.

## 2. Diagnosis (audit 2026-09-26, all values computed from the working tree)

### 2.1 The six stats are nearly flat in the range that is actually played

Rank per-stat caps are None 100 / Genin 250 / Chunin 600 / Jonin 1200 / Hokage 2500 (`DEFAULT_RANK_LIMITS`). Effects at those caps, XP = 0:

| Stat value | Move speed bonus | Melee atk bonus | Incoming dmg factor | Max HP | Chakra pool (XP 0 / XP 100k) | Dodge vs a 100-Speed attacker |
|---|---|---|---|---|---|---|
| 100 | +25.2% | 1.6 | −1.8% | 29.6 | 239 / 50,239 | 0.0% |
| 250 | +30.6% | 3.3 | −3.6% | 39.9 | 497 / 50,497 | 2.8% |
| 600 | +36.0% | 6.7 | −6.6% | 60.1 | 1,002 / 51,002 | 5.7% |
| 1200 | +40.6% | 11.6 | −10.4% | 89.8 | 1,744 / 51,744 | 7.9% |
| 2500 | +45.9% | 20.9 | −16.2% | 145.5 | 3,137 / 53,137 | 10.2% |

- **Speed** is `0.012·log10(1+s) + 0.00015·s^0.45`: the log term dominates, so 25× the stat buys +20 points of percent. This is the "100k ≈ 1M" complaint in its purest form: Speed 100,000 → +8.7% total... measured from zero, i.e. a 1000× stat increase over Genin buys +15 points of percent.
- **Chakra pool** is `0.5·XP + 6·s^0.8`: at 100k Battle XP the pool is **94–97% Battle XP**. The Chakra stat is cosmetic for anyone who has fought.
- **Strength's** overflow multiplier `(4+eff)/(4+vis)` is exactly 1.0 below Strength ≈ 747,857 — dead code in all normal play (the visible-attribute cap 2000 is unreachable under every rank cap).
- **HP compression** (`displayed/effective` below the 1000 cap) is likewise dead below Health ≈ 32,652.
- **Resistance** tops out at −16.2% incoming damage at the Hokage cap; **SPI** affects only regen and the regen lock, and its regen bonus is a function of *pool size* (i.e. of XP), not of SPI.
- **Dodge** is fine (competitive advantage curve, 20% asymptote) — keep it.
- The `ARMOR` AttributeModifier slot is a permanent 0.0 no-op; `getAffinityJutsuXpModifier` (the intended 1.35× affinity XP) has zero callers.
- **Points come only from staff commands** — no gameplay path grants stat points. Genin parity is therefore a staff-granting convention, not a system property; the plan keeps it that way and adds recommended budgets (§9).

### 2.2 The jutsu economy is three different economies (184 JutsuEnums in 32 files)

- **Custom-balance jutsus** (`.withCustomBalance()`, the newer ~60): rank-capped cost curve, mastery discount, rank cooldown floors. Behaves.
- **Legacy jutsus** (~120): cost = `chakraUsage × power` with `power` capped by "how many casts' worth of chakra you hold" — so identical-rank jutsus on the same item can cost an order of magnitude differently, and cost correlates with pool size, not with the technique.
- **Cooldowns:** the rank floor (S 600t / A 360t / B 240t / C 140t / D 80t) applies **only** to custom-balance jutsus. **~80 legacy jutsus have no cooldown at all** — entire elements: all of Suiton's legacy set, Hyoton, Shakuton (all three S-ranks!), Bakuton, Jinton (600-chakra S-ranks, zero CD), Ranton, Futton, Yooton, Mokuton (most), Raiton's legacy trio, Katon's legacy six, Senjutsu (except two), Kekkei Mora, Tenseigan Chakra Mode, all four summons, Shikotsumyaku's toggles.
- **Zero/near-zero cost flags:** Kage Bunshin and Multi Clone (0 — saved from being free only by an entity-internal gate), Hiraishin 10 (S), all five Kekkei Mora at 10 (S) *including the 500-damage one*, Sage Mode 10 (S), Sand Levitation 0.25 (S), Healing 0.25 (A, per-tick), Puppet 0.5 (C), Crystal Thorns 2 (S), Finger Bones 5 (S).
- **Absurd cost flags:** Wood Buddha 5000, Eight Snakes/Gamarinsho 3000, Kirin 1500, Rasenshuriken 1000, Jinton 600 + reqXP 700, Wood Golem 1000 — with no cap because they are legacy (full charge ≈ whole pool by design).
- **Damage flags (the worst offenders):**
  - `Kekkei Mora "80 Gods"`: **flat 500 absolute, armor-bypassing, refireable every 4 ticks while channeling, 10 chakra, no cooldown.** The single most abusable number in the mod.
  - `Housenka (dragon)`: D-rank, 36 chakra, `20×power` up to ~100 damage **plus** a 5-block explosion — out-damages every A-rank at ¼ the cost.
  - No power cap → pool-scaled damage: Chidori Senbon (A, ~370 raw at large pools), Retsudo Tensho (C, ~100 AoE), Shuriken Shadow Clone (B), Housenka/Tsumabeni.
  - Flat, nothing-scaled: Chidori base 25 (scaled only by ninja level), Bracken Dance 20/spike, Kirin 100×size, clone death bursts, paper bombs (documented, leave).
  - Almost no jutsu scales with any stat. Only taijutsu (Strength/Speed) and Four-Pillar Bind (Battle XP + mastery) do.
  - **Sharingan Copy bypasses the whole economy**: copied jutsus cost `max(70% base, base×power)` with no rank cap, no pool surcharge, no mastery discount, and **no cooldown at all** (the callback cooldown is written to a discarded temp stack).
- Two cooldown-setter conventions coexist (entity-scaled vs unscaled), so the same rank floor binds differently for Bloodline jutsus vs canonical ones. Unify before retuning numbers.
- Documented intent vs code: everything the docs specify (bloodlines, Phoenix, paper bombs, Four-Pillar, genjutsu, taijutsu costs/cooldowns, Copy) **matches**. The undocumented legacy roster is where the chaos lives.

### 2.3 Commands: 18 registrations for ~8 functions

`/rpstats` already has subcommands for everything. Duplicates: set-stat ×3 (`/setstat`, `/setrpstat`, `/rpstats stat`), personal cap ×3, rank limit ×3, points ×2, clan/rank/affinity/sharingan/checksheet ×2 each, list ×3, plus one **unregistered dead command** (`/setstatpointcap`) and `/rpadmin` registered as an exact duplicate handler of `/adminmissions`.

## 3. Design goals (the user's requirements, restated as spec)

1. Stat building must be **meaningful**: each rank cap should be a clear power step, and build choice (which stat) must matter.
2. The curve must stay meaningful at extremes: 100k must clearly beat 2.5k, 1M must clearly beat 100k — never a plateau.
3. Keep capped spending (rank caps + point reserve). Genins get equal point budgets (staff convention, documented).
4. Every jutsu has a cooldown; chakra costs are rank-proportionate and bounded; damage sits in rank bands and scales with stats modestly.
5. One command surface; nothing that existing saves/worlds rely on breaks.

## 4. The proposed stat calculation (verified)

One shared growth shape, anchored on the **Genin cap** as the reference point:

```text
F(s) = (max(s, 0) / 250) ^ 0.6          # stat factor, F(250)=1, F(2500)≈3.98, F(100k)≈36.4
L    = sqrt(clamp(BattleXP, 0, 100000) / 100000)   # XP factor, 0..1
```

Every 10× stats ⇒ ~3.98× effect, everywhere, forever — meaningful at Genin and still differentiating at 1M (36.4 → 145, a 4× gap). Per attribute:

```text
Movement add (op 0 on base 0.1):  min(0.15, 0.025·F(Speed))
Melee attack bonus:               2.5·F(Strength)          [visible-cap 2000 + overflow kept as safety valve]
Incoming damage factor:           1 / (1 + 0.14·F(Resistance))
Effective max HP:                 20 + 8·F(Health) + 0.002·XP   [displayed cap 1000 + compression kept]
Chakra pool:                      150 + 350·F(Chakra) + 30·L
Stamina pool:                     100 + 0.9·(Speed+Strength+Resistance+Health)^0.75 + 15·L
Chakra regen per second:          pool · (0.012 + 0.012·F(SPI)), capped at 6% of pool
Regen lock:                       max(10, 40 − 8·F(SPI)) ticks
Taijutsu damage bonus:            1.4·F(Strength) + 0.8·F(Speed)
Jutsu damage multiplier (new):    1 + 0.10·F(Chakra)       [universal, see §7]
Dodge:                            unchanged (already good)
```

Verified table (XP = 0; HP second column at XP = 100,000; movement % relative to walk speed):

| Stat | F(s) | Move | Atk | Dmg taken | HP (XP0 / 100k) | Pool | Regen/s | Lock | Jutsu mult | Taijutsu bonus |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | 0.00 | +0% | 0.0 | 100% | 20.0 / 220.0 | 150 | 1.8 | 2.0 s | ×1.00 | 0.0 |
| 100 | 0.58 | +14.4% | 1.4 | −7.5% | 24.6 / 224.6 | 352 | 6.7 | 1.8 s | ×1.06 | 1.3 |
| 250 | 1.00 | +25.0% | 2.5 | −12.3% | 28.0 / 228.0 | 500 | 12.0 | 1.6 s | ×1.10 | 2.2 |
| 600 | 1.69 | +42.3% | 4.2 | −19.1% | 33.5 / 233.5 | 742 | 24.0 | 1.3 s | ×1.17 | 3.7 |
| 1200 | 2.56 | +64.1% | 6.4 | −26.4% | 40.5 / 240.5 | 1,047 | 44.8 | 1.0 s | ×1.26 | 5.6 |
| 2500 | 3.98 | +99.5% | 10.0 | −35.8% | 51.8 / 251.8 | 1,543 | 92.3 | 0.5 s | ×1.40 | 8.8 |
| 10,000 | 9.15 | +150% (clamp) | 22.9 | −56.1% | 93.2 / 293.2 | 3,351 | 201 | 0.5 s | ×1.91 | 20.1 |
| 100,000 | 36.4 | +150% | 91.0 | −83.6% | 311 / 511 | 12,894 | 774 | 0.5 s | ×4.64 | 80 |
| 1,000,000 | 145.0 | +150% | 362 | −95.3% | 1,180 / 1,380 | 50,885 | 3,053 | 0.5 s | ×15.5 | 319 |
| 100,000,000 | 2,297 | +150% | 5,744 | −99.7% | 18,399 (compression active) | 804,239 | 48,254 | 0.5 s | ×231 | 5,054 |

Sanity sweep (multiplicative march 1 → 100M): all curves monotone, resistance never reaches 0%, pool positive, PASS. Design notes:

- **Movement is the only clamped curve** (+150%, reached at stat ≈ 4,953 — above every rank cap). It is a physics simulation limit, not a math plateau; every other effect keeps growing so extreme builds still differentiate everywhere else.
- Each rank step is now a real jump: Genin→Chunin is −12%→−19% damage taken, +25%→+42% speed, ×1.10→×1.17 jutsu damage, pool 500→742.
- The dead mechanics become live again at their intended scale: compression engages around Health ≈ 516k, overflow far above that — safety valves, not core math.
- HP from Battle XP is reduced (0.005→0.002 coefficient) so the Health stat, not grind, drives survivability; XP still adds up to +200.
- Under the new pools the **existing** custom-balance cost curve finally behaves as designed (it was built assuming ~500+ pools). No cost-formula rewrite needed — see §6.

## 5. Why the chakra economy fixes itself under stat-driven pools

Existing cost curve, evaluated on the new pools (mastery 0 unless noted):

| Rank / base | Genin (500) | Chunin (742) | Jonin (1,047) | Hokage (1,543) | Hokage mastery 1 | Casts @Genin | Casts @Hokage |
|---|---|---|---|---|---|---|---|
| D / 30 | 30 | 32 | 36 | 40 | 22 | 16 | 38 |
| C / 55 | 55 | 59 | 63 | 71 | 39 | 9 | 21 |
| B / 90 | 90 | 96 | 104 | 116 | 64 | 5 | 13 |
| A / 140 | 140 | 149 | 159 | 177 | 98 | 3 | 8 |
| S / 200 | 200 | 212 | 227 | 252 | 140 | 2 | 6 |

The `pool − 500` surcharge threshold lands exactly at the Genin pool, so novices pay clean base costs, veterans pay a gentle surcharge capped per rank, and mastery halves everything. Battle XP's pool share drops to +30 — **the stat decides the pool now**. Upkeep coherence under the new pools: Edo Tensei 10/s is a light commitment at any rank; generic Susanoo 500 summon + stage upkeep 30–115/s becomes a genuine resource decision (a Genin sustains stage 0 for ~16 s; a Hokage sustains stage 0–1 with maxed SPI regen but never stage 5 indefinitely); ocular upkeep 2.5–40/s stays proportionate. No upkeep numbers need to change.

## 6. Per-rank cost and cooldown bands (the normalization policy)

**Base chakra per rank:** D 30 · C 55 · B 90 · A 140 · S 200. Utility/defensive jutsus may sit one band lower; summoning/upkeep jutsus declare their upkeep separately.
**Cooldown floors (already coded, extended to everyone):** D 4 s · C 7 s · B 12 s · A 18 s · S 30 s. Explicit callback cooldowns may exceed but never undercut the floor; the **unscaled** setter becomes the only convention (entity-scaled cooldowns are retired so floors always bind).

Conversion policy for the ~120 legacy jutsus:
1. Every `JutsuEnum` gets `.withCustomBalance()` and a base cost from the band (or a documented custom base for specials: Rasenshuriken stays a 400-cost S-rank "ultimate" tier, Kirin 300, Wood Buddha 400, Jinton 250 each — all now capped by the rank ceiling instead of eating the whole pool).
2. Zero-cost fixes: Kage Bunshin 90 (B), Multi Clone 140 (A), Hiraishin 140 (A — mobility, not damage), Sage Mode 200 (S toggle) — the internal entity gates stay as secondary limits.
3. Near-zero offenders move to band: Kekkei Mora set → 200 each (S), Crystal set → band by actual effect, Finger Bones 30 (D→refit as D), Sand Levitation 55 (C utility), Healing keeps per-tick costing but re-derived as `12 + 6·power` per second channeled.
4. Every flagged damage outlier is renormalized into the rank band (§7).

## 7. Damage normalization

**Target single-hit damage at full charge, mastery 0** (multi-hit totals ≤ 1.5× band; status effects are free):

| Rank | Band | Example tuning |
|---|---|---|
| D | 6–10 | Housenka dragon `7 + 3.5·power` (power cap 2), explosion size 2 |
| C | 10–16 | Water Whip keeps `4 + 1.5·power` ≈ 7→10, bumped base to 6 |
| B | 16–24 | Flame Whirlwind keeps `7 + 2.2·power` (cap 2.4) ≈ 12→17, bumped to 9 base |
| A | 24–36 | Twin Flame Dragons per dragon `10 + 7·power` (cap 2.8) ≈ 24→30 |
| S | 40–60 | "80 Gods" `45 + 8·power`, refire 20 ticks, cooldown 1200t |

- **Universal stat hook:** one `PlayerStats.getJutsuDamageMultiplier(entity)` = `1 + 0.10·F(Chakra)` applied in the shared damage helpers (`BloodlineTechniques.blast`, `ItemCanonicalJutsu`/`ItemExtraJutsu` hit paths, projectile impact paths) — never hand-sprinkled into callbacks. Chakra stat = jutsu power, Strength = physical, by design.
- **Power caps everywhere:** every multi-hit / no-cap callback gets an explicit `getMaxPower` (2.0–2.8 by rank) so pool size can no longer substitute for rank (fixes Chidori Senbon, Retsudo Tensho, Shuriken Shadow Clone, Housenka, Tsumabeni).
- **Copy fix:** copied jutsus go through `getCustomResourceCost` at the *user's* mastery with a cooldown = the source jutsu's floor. The 60 s copy window is already the spam limit; remove the free-cast bug.
- Chidori base 25 → `18 + 4·power × ninja-level multiplier` (keeps its identity); Bracken Dance 20 → `8 + 2·power` per spike; paper bombs unchanged (documented and correct).
- Four-Pillar Bind keeps its documented XP/mastery formula (it already matches the band).

## 8. Command consolidation

Canonical surface: **`/rpstats`** with subcommands `checksheet · points · statcap · ranklimit · clan · rank · affinity · stat · sharingan · clans · ranks · affinities`, plus **`/statpoints`** (add), **`/addninjaxp`**, **`/addxp2jutsu`**. Everything else becomes a thin alias delegation (house rule: aliases stay registered):

| Legacy command | Becomes |
|---|---|
| `/setstat`, `/setrpstat` | alias → `/rpstats stat` |
| `/setstatcap`, `/statlimit player` | alias → `/rpstats statcap` |
| `/setranklimit`, `/statlimit rank` | alias → `/rpstats ranklimit` |
| `/setstatpoints` | alias → `/rpstats points` |
| `/setclan` `/setrank` `/setsharingan` `/checksheet` | alias → `/rpstats …` |
| `/listclans` `/listranks` `/listaffinities` | alias → `/rpstats …` |
| `/setaffinity` / `/addaffinity` / `/removeaffinity` | alias → `/rpstats affinity set/add/remove` |
| `/rpadmin` | alias → `/adminmissions` |
| `/setstatpointcap` (unregistered dead class) | deleted |

Also: `/addninjaxp` usage message documents the `narutomod:ninjaachievement` requirement instead of failing silently.

## 9. Stat spending parity (RP guidance, unchanged mechanics)

Caps and the single point reserve stay exactly as they are. Recommended point budgets for staff (so all Genins build from the same wealth): None 200 · Genin 500 · Chunin 900 · Jonin 1,500 · Hokage 3,000 — with per-stat caps 100/250/600/1200/2500, a maxed Genin uses 1,500 of 6 stats, so 500 points forces real build choices (e.g. fast/squishy vs tanky/slow). Document this table in the staff docs; it is guidance, not code.

## 10. Implementation phases (each gated on approval, each with fixtures)

- **P1 — Policy class + fixtures (no behavior change):** `StatsPolicy` with every formula + constant; headless checks re-derifying every number in §4/§5 tables (monotonicity, bounds, cost table, band table) wired as `verifyStatsRebalance` in `tools/stats/verify.gradle`.
- **P2 — Stat curves:** swap `PlayerStats` formulas to `StatsPolicy`, dataVersion 6 migration (NBT keys unchanged — only derived values change), keep attribute UUIDs; feature flag `BETTER_STAT_CURVES` in `ModConfig` defaulting on for new worlds, off → legacy formulas.
- **P3 — Economy conversion:** flip legacy `JutsuEnum`s to `.withCustomBalance()` + band bases (append-only; no index changes), universal cooldown floor, unified unscaled setter, power caps, damage-band renormalization of the §7 outlier list, Copy-path fix. Per-item diff review against the audit tables.
- **P4 — Commands:** consolidation + aliases + dead-code removal.
- **P5 — Rebalance sweep:** re-tune NPC damage, healing throughput, mission rewards and XP thresholds that assumed old curves; verify the full jutsu roster against the bands; update `CHANGELOG_CUSTOM_PT_BR.md`.
- Verification ritual (house standard): Java 8 / Gradle 4.9, `gradlew --offline --no-daemon build verifyStatsRebalance`, reobfuscated test JAR, one identical JAR per side, **copied world**, live two-client pass covering: old-character migration, every rank cap, cast-cost feel per rank, Susanoo/Edo/ocular upkeep, Copy spam, 80-Gods fix, movement feel at Genin vs Hokage. Fixtures prove math, not gameplay — say so.

## 11. Open decisions (need the user's call before P2/P3)

1. **Jutsu damage scaling with the Chakra stat (×1.10–1.40 in rank range)** — recommended yes; alternative is scaling with Strength for a "physicalPower" flavor or no stat scaling at all.
2. **Movement clamp +150%** — recommended; alternative is unbounded (physics get silly above ~+150% anyway).
3. **Battle XP HP share** (proposed 0.002 → max +200 HP) vs current 0.005 (+500).
4. **Universal floor application to all ~120 legacy jutsus at once** (recommended — it is the actual fix) vs converting element-by-element over multiple releases.
5. **S-rank utility costs** (Hiraishin as A 140, Sage Mode toggle 200) — flavor calls.
