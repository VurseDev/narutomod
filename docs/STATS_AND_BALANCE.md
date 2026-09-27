# Stats, resource economy and a coherent next balance pass

Source audit: **27 September 2026**, current dirty 1.12.2 tree based on `f828586`. Read [AI_HANDOFF](../AI_HANDOFF.md) first. Sections 1–5 describe implementation; section 6 is a **new proposal, not implemented or approved by this document**. No gameplay values were changed in preparing this handoff.

## 1. Which calculation is actually running?

[StatsPolicy](../src/main/java/net/narutomod/StatsPolicy.java) is a new untracked source dependency of the current tree. [ModConfig](../src/main/java/net/narutomod/ModConfig.java) defaults `BETTER_STAT_CURVES=true`, copied into `StatsPolicy.ENABLED` during [PlayerStats.preInit](../src/main/java/net/narutomod/PlayerStats.java). This is not a per-world “new characters only” switch. Existing characters get new derived curves when enabled; stat keys and data version **5** are unchanged. No runtime refresh or server-to-client synchronization of this flag was found in the audit. Match configuration and restart when testing it.

Disabling the flag restores the legacy player-effect curves and makes the new jutsu damage multiplier one. It does **not** undo the new Sharingan Copy behavior or unscaled Bloodline cooldown setters. Save and compare configs alongside JAR hashes.

The older [rebalance proposal](stats_and_jutsu_economy_rebalance.md) is not a trustworthy implementation report. Its claims of P1–P4 completion, universal legacy conversion, v6 migration, reduced XP contributions and global jutsu scaling disagree with this working tree. The original jutsu enums were **not** broadly normalized. Many modified item files differ only in line endings; inspect with `git diff --ignore-space-at-eol` before interpreting a large diff as gameplay work.

### Persistent quantities must not be confused

- Six allocated stats: Speed, Strength, Resistance, Health, Chakra, SPI; nonnegative integer allocations, up to 100,000,000 technical maximum per stat.
- RP point reserve/budget and special progression spending: separate long-valued accounting. Rank/personal caps limit allocation; they do not generate points.
- Ninja/Battle XP: `PlayerTracker.getBattleXp`, normally capped at 100,000 by its progression path.
- Individual Jutsu XP: item/technique progression and mastery; not Ninja XP or RP points.
- Current chakra/stamina versus maximum resource: live numeric resource state, not the allocated Chakra stat.
- Effective HP versus displayed vanilla HP: different above the display cap, with damage compensation described below.

## 2. Exact enabled curves

Let `F(s)=(max(s,0)/250)^0.6` and `X=clamp(BattleXP,0,100000)`. All damage numbers use Minecraft health units (two HP per ordinary heart). All times are game ticks; `/20` assumes 20 TPS, not guaranteed wall-clock time.

| Effect | Current equation / unit |
| --- | --- |
| Movement attribute addition | `min(0.15, 0.025*F(Speed))`, attribute operation 0. Vanilla player base is 0.1, so addition 0.025 is +25%, not 0.025 blocks/tick. Other modifiers still apply. |
| Effective attack bonus | `2.5*F(Strength)`; visible attribute bonus capped at 2,000. |
| Nonabsolute damage multiplier from Resistance | `1/(1+0.14*F(Resistance))`. It affects more than physical melee. |
| Effective max HP | `20 + 8*F(Health) + 0.005*X`. XP contributes up to **500**, not 200. |
| Max chakra | `150 + 800*F(Chakra) + 0.5*X`. XP contributes up to **50,000**, not 30. |
| Max stamina, actual caller | `120 + 5*(Speed+Strength+Resistance+Health)^0.78 + 0.35*BattleXP`; legacy formula preserved. This caller does not route through `StatsPolicy.staminaPool`. |
| SPI regeneration **bonus per second** | `maxPool * min(0.06, 0.012+0.012*F(SPI))`. This is not total regeneration. |
| Lock after accepted damage | `max(10, round(40-8*F(SPI)))` ticks. This does not remove the separate stationary requirement. |
| Taijutsu bonus | `1.4*F(Strength) + 0.8*F(Speed)`. |
| Available jutsu damage factor `g` | `1+0.10*F(Chakra)`; applied only where called, currently Twin/Company blast helper. |

At ten times a stat, its unbounded `F` contribution grows about 3.981 times. Movement deliberately saturates at +150%; the implemented resistance curve approaches complete mitigation at extreme values. Do not claim every displayed effect grows forever without a plateau.

### Computed examples

Each row independently sets the relevant stat to `s`; it is a **curve comparison, not a legal six-stat build with a given point budget**. SPI example uses the same `s` and the XP-zero chakra pool. Rounded values are not storage precision.

| s | Move bonus | Attack bonus | Damage taken factor | HP: XP 0 / 100k | Chakra: XP 0 / 100k | SPI bonus/s, XP 0 | Damage lock |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 0 | 0% | 0 | 1.000 | 20 / 520 | 150 / 50,150 | 1.80 | 40t |
| 100 | 14.43% | 1.44 | 0.925 | 24.62 / 524.62 | 611.66 / 50,611.66 | 11.58 | 35t |
| 250 | 25.00% | 2.50 | 0.877 | 28 / 528 | 950 / 50,950 | 22.80 | 32t |
| 600 | 42.27% | 4.23 | 0.809 | 33.53 / 533.53 | 1,502.75 / 51,502.75 | 48.53 | 26t |
| 1,200 | 64.07% | 6.41 | 0.736 | 40.50 / 540.50 | 2,200.38 / 52,200.38 | 94.08 | 19t |
| 2,500 | 99.53% | 9.95 | 0.642 | 51.85 / 551.85 | 3,334.86 / 53,334.86 | 199.33 | 10t |
| 100,000 | 150% | 91.03 | 0.164 | 311.29 / 811.29 | 29,279.03 / 79,279.03 | 1,756.74 | 10t |
| 1,000,000 | 150% | 362.39 | 0.047 | 1,179.65 / 1,679.65 | 116,114.75 / 166,114.75 | 6,966.88 | 10t |
| 100,000,000 | 150% | 5,743.49 | 0.00310 | 18,399.17 / 18,899.17 | 1,838,067.37 / 1,888,067.37 | 110,284.04 | 10t |

### Damage application and health compression

`PlayerStats.PlayerHook.onHurt` first applies melee overflow only for direct player damage of type `player`: `(4+effectiveAttackBonus)/(4+visibleAttackBonus)`. For a player defender and **nonabsolute** damage it applies Resistance, then `min(1, displayedHP/effectiveHP)` where displayed HP caps at 1,000, then comparative Speed dodge. This emulates large effective health without asking vanilla HUD/attributes to represent it all.

Dodge uses `a=max(0, SpeedDefender^0.45-SpeedAttacker^0.45)` and probability `0.20*a/(a+25)`. It only considers eligible projectile/player-source damage; an unrecognized nonplayer attacker's Speed is zero. It is not the old 60% Sharingan dodge. Other armor, absorption, event priorities and damage-source flags still matter; never calculate final damage by pretending this hook is the entire Forge pipeline.

Absolute damage bypasses Resistance, effective-health compression and this dodge branch. Healing is not automatically converted by the inverse compression factor. The 1.12.2 merge must explicitly preserve or redesign these semantics; simply copying the max-HP formula changes survivability.

### Actual regeneration, not just the policy helper

In [Chakra.Player.update](../src/main/java/net/narutomod/Chakra.java), at nominal 20 TPS:

```text
totalPassivePerSecond = (20*baseRegen + SPIbonusPerSecond + 0.02*foodSaturation)
                        * TenketsuRegenMultiplier
baseRegen = ModConfig.CHAKRA_REGEN_RATE        # default 0.006 per tick
baseRegen *= 1.35 only in stamina mode
```

The player must be grounded, horizontally motionless and not swinging for **more than 40 ticks**, and past the accepted-damage lock. Moving/airborne/swinging resets the stationary counter. Separate all-players-asleep recovery adds 0.6 per tick. Therefore “regeneration is capped at 6% total” and “regen begins while moving after the SPI lock” are both false.

At Chakra/SPI 2,500 and XP 100k, SPI alone grants about **3,187.98/s while eligible**. Perfect Susanoo costs 115/s. This is a real sustain mismatch to playtest; the older claim that maxed SPI cannot sustain Perfect is unsupported. Simultaneously, XP-zero Chakra 250 has only 950 capacity, less than nine seconds of Perfect upkeep even before entry cost, if eligibility were granted. Compare actual eligible builds/XP, not those two extremes as equivalent characters.

## 3. Legacy curve reference (flag off / pre-rebalance baseline)

These formulas explain preserved v3 behavior and offer a regression oracle, not a recommendation to silently revert the current tree:

```text
movementAdd = 0.012*log10(1+Speed) + 0.00015*Speed^0.45
effectiveAttackBonus = 0.04*Strength^0.80
resistanceRating = 0.12*Resistance^0.75
resistanceDamageFactor = 1/sqrt(1+resistanceRating/100)
effectiveHP = 20 + 0.005*BattleXP + 0.24*Health^0.80
maxChakra = 0.5*BattleXP + 6*ChakraStat^0.80
maxStamina = 0.35*BattleXP + 120 + 5*(Speed+Strength+Resistance+Health)^0.78
SPIbonusPerTick = 0.0025*SPI^0.65 + maxPool*(0.00005+0.00003*log10(1+SPI))
damageRegenLockTicks = max(20,100-round(10*log10(1+SPI)))
taijutsuBonus = 0.55*effectiveAttackBonus + 0.02*Speed^0.72
```

The visible attack/HP caps, compression and comparative dodge already existed. Attribute updates run on server refresh/login/respawn, not every client frame. A client-only stat change cannot legitimately grant gameplay power.

## 4. Shared charging, mastery, resource cost and cooldown

Source: [ItemJutsu](../src/main/java/net/narutomod/item/ItemJutsu.java), [ProcedureUtils](../src/main/java/net/narutomod/procedure/ProcedureUtils.java), [StatsPolicy](../src/main/java/net/narutomod/StatsPolicy.java).

For an eligible learned technique with required XP `R` and learned XP `J`, mastery is `m=clamp((J-R)/(2R),0,1)`. It reaches one at three times the requirement. Rank defaults D/C/B/A/S are 100/150/200/250/400 required XP; explicit overrides and non-affinity multipliers (2.5 where used) matter. Lookup can search matching main/offhand items. Never use total Ninja XP as individual mastery.

The shared charge path is:

```text
chakraModifier = 1/(0.684 + 0.01*sqrt(max(currentResource,maxResource)))
chargeModifier = chakraModifier * currentJutsuXpModifier
power = min(callbackBase + heldTicks/(callbackDelay*chargeModifier), actualMaxPower)
actualMaxPower = min(callbackMax, currentResource/baseCost)  # positive-cost case
```

The Jutsu XP modifier is based on required/current learned XP, with special unlearned/owner handling. The affordability check at execution uses the full cost below; a simple resource/base-power cap does not guarantee the later cast is affordable. Zero-delay callbacks retain their base power behavior. Pool changes therefore affect charge speed and some legacy cooldowns even when base costs were untouched.

### Custom-balance cost, already implemented

Let `B` be the technique's own base cost, `M` maximum pathway resource, `p` charge power and `m` mastery. The helper does **not** replace every `B` by a rank-standard number.

```text
fixed = B*(1-0.35*m)
poolSurcharge = max(0,M-500)*rankRatio*(1-0.80*m)
raw = (fixed+poolSurcharge)*max(1,p)
ceiling = M*(noviceFraction+(masterFraction-noviceFraction)*m)
cost = max(fixed, min(raw, max(fixed,ceiling)))
actualPayment = cost * TenketsuCostMultiplier
```

| Rank | Pool ratio | Novice ceiling | Master ceiling | Minimum cooldown |
| --- | ---: | ---: | ---: | ---: |
| D | 0.010 | 12% | 4% | 80t / 4s |
| C | 0.015 | 18% | 6% | 140t / 7s |
| B | 0.025 | 25% | 8% | 240t / 12s |
| A | 0.035 | 32% | 11% | 360t / 18s |
| S | 0.050 | 40% | 15% | 600t / 30s |

The fixed minimum can exceed the percentage ceiling; it remains unaffordable if the player lacks enough resource. Failed callback execution does not receive normal success payment/cooldown. Resource mode must match: Taijutsu uses the stamina pathway, normal ninjutsu chakra; do not introduce duplicate “combat stamina.”

Original non-custom techniques still use their base × power × mastery-cost modifier (plus Tenketsu), with their original cooldown behavior. Their mastery-cost factor is `0.70+0.30*clamp(currentJutsuXpModifier,0.3334,1)`. The extracted `StatsPolicy.rankBaseCost` helper is **not** evidence that all enums were changed to D30/C55/B90/A140/S200.

Twin/Company/Throw now use unscaled callback cooldowns 280/420/180 ticks, after which custom floors yield **360/420/240** (18/21/12 seconds). Turning off better stat curves does not revert these setters. Elsewhere entity-scaled and unscaled setters coexist; inspect each call before presenting a callback number as effective cooldown.

### Actual cost examples: Twin Flame Dragons

Base 140, rank A, full power 2.8, no Tenketsu modifier. These are formula evaluations, not predictions that every player can unlock/cast the technique.

| Chakra stat / XP | Pool | Tap novice | Full novice | Full master |
| --- | ---: | ---: | ---: | ---: |
| 250 / 0 | 950 | 155.75 | 304.00 | 104.50 |
| 2,500 / 0 | 3,334.86 | 239.22 | 669.82 | 310.36 |
| 250 / 100k | 50,950 | 1,905.75 | 5,336.10 | 1,243.62 |
| 2,500 / 100k | 53,334.86 | 1,989.22 | 5,569.82 | 1,290.36 |

At 100k XP the resource difference between the two Chakra allocations is relatively small. That is a consequence of retaining the original XP share, not a table error. Mastery reduces both fixed and pool portions substantially; never use base 140 as the displayed final price for every character.

## 5. Damage coverage and remaining inconsistencies

- Twin per dragon: `(8+10*p)*(1+0.25*m)*g*falloff`; Company bolt: `(6+7*p)*(1+0.25*m)*g*falloff`. `g` is the enabled Chakra factor. Only `BloodlineTechniques.blast` calls it.
- Clone Throw still `(6+7*p)*(1+0.25*m)`, without `g`. Four-Pillar keeps its dedicated XP/mastery formula, maximum 30.375, without `g`. Phoenix remains `12+8*p`. There is **no universal stat-scaled ninjutsu hook**.
- Twin's two impacts are a total attack budget, not a single-hit figure: at power 2.8/mastery 1, center damage is 47.5 **per dragon before `g`**, or 95 if both eligible hits land. With Chakra 2,500, that is about 132.82 combined raw damage. Burn and terrain are additional effects. Armor, immunity windows, water/cover/substitution and falloff change actual outcome.
- Paper-bomb repeated-hit attenuation, same-tick aggregation and accepted-only effects remain independent policies. Do not multiply each tag, the aggregate, and the shared damage hook again.
- Sharingan Copy now invokes custom cost, but cooldown is written on the single-use copied stack just before consumption. It is not a persistent per-player cooldown. `onUsingTick` recreates a temporary original stack and forwards channel callbacks; some can charge/execute during holding. Release failure can preserve the copy. Test copied channel abilities, not just one-shot release.
- The Chakra tooltip still reports the old `6*stat^0.8` bonus while the enabled maximum uses `150+800F+0.5X`. Fix UI through the same authoritative calculation in a future code task.
- Existing rank-stat defaults and point budgets are different controls. All 18 registered stat-command handlers remain; only an already-unregistered class was removed. `/rpstats` accepts more subcommands than usage/tab completion advertises. The proposed consolidated command syntax was not implemented.

## 6. Proposed next calculation and balance contract — NOT IMPLEMENTED

This proposal intentionally keeps the current approved XP shares and original jutsu baseline. Reducing XP contribution, converting every original jutsu or changing named ability identity is a separate user decision. First implement reliable wiring/telemetry, then tune combat; formula monotonicity alone does not establish balanced gameplay.

### 6.1 Keep meaningful progression; bound compounding mitigation

Keep `F`, current effective HP, chakra/stamina pools, attack, movement cap and Taijutsu curve. Replace only the resistance multiplier in an opt-in balance revision with:

```text
resistanceReduction = 0.60*F(Resistance)/(F(Resistance)+4)
damageTakenFactor = 1-resistanceReduction
```

It remains strictly improving, never reaches immunity, and preserves near-Genin behavior while avoiding double-unbounded survivability from both HP and Resistance. At very large equal stats, current effective HP grows like `F` while current mitigation adds another factor like `F`; attacker damage grows only like `F`. This increasingly lengthens equal-power fights. Bounded Resistance removes that extra unbounded factor.

| Resistance | Current damage factor | Proposed factor |
| ---: | ---: | ---: |
| 250 | 0.877 | 0.880 |
| 600 | 0.809 | 0.822 |
| 1,200 | 0.736 | 0.766 |
| 2,500 | 0.642 | 0.701 |
| 100,000 | 0.164 | 0.459 |
| 1,000,000 | 0.047 | 0.416 |
| 100,000,000 | 0.00310 | 0.401 |

This is a balance change, particularly for staff-granted extreme builds, so it requires approval and migration notes even though raw allocations do not change. Armor must have its own audited cap/order: stacking bounded RP resistance with another mod's near-immunity still creates a problem.

### 6.2 One damage pipeline, explicitly scoped

Introduce a damage descriptor for **opted-in custom techniques**, not all legacy damage: technique ID, owner UUID, cast ID, rank, charge, mastery snapshot, source category, direct/AoE/DoT phase and `statsAlreadyApplied` state. Resolve one authoritative owner and apply the Chakra factor once. Remove local application from Twin/Company if moved into this dispatcher. Keep Four-Pillar's existing XP/mastery calculation as an explicit override until deliberately retuned; do not square mastery/XP factors.

Recommended sequence is validate cast/target → raw technique budget → charge/mastery/stat contribution once → per-target/falloff/phase budget → Forge attack/damage path → accepted-hit secondary effects. Do **not** apply armor manually then let Forge apply it again. Use one canonical cast ID to test no replay/duplicate packets; never grant damage from the client render timeline.

Use consistent stat factor `1+0.10F(Chakra)` for ordinary opted-in ninjutsu, and existing Strength/Speed path for named Taijutsu. Summon attacks need explicit owner-vs-donor rules: Edo currently uses donor XP; do not accidentally scale by the caster and donor twice. Paper bombs should remain their tested raw/attenuated model until their total budget is reapproved.

### 6.3 Budget whole attacks and control, not just first hits

Define configuration for `maxPrimaryDamagePerCast`, `maxTotalDamagePerVictim`, `maxControlTicks` and burn/DoT allowance. Multi-hit abilities divide a budget rather than receiving the full rank budget per projectile. A candidate full-charge novice reference budget at XP-zero, before stat factor/armor is D 10, C 16, B 26, A 40, S 60 total primary damage. These are **starting test targets**, not automatic replacements for the existing highly visible Twin attack. A high-investment ultimate can declare an exception with more cost, telegraph and cooldown.

Example proposed ordinary A two-impact spell: total reference 40, split 20+20; mastery factor up to 1.25, stat factor at Chakra 2,500 about 1.398, giving about 69.91 combined center damage before defenses. Current Twin is about 132.82 under those same power/mastery/stats assumptions. Adopting the ordinary budget would be a substantial nerf, not a harmless refactor—either approve that change or classify Twin as an expensive exception.

Do not make control “free”: long restraint, homing, unavoidable follow-ups, cover destruction and area all consume budget. Start with jutsu-specific, short recovery/escape windows; do not revive generic M1/parry to solve this. Preserve substitution/water/LOS counters. Measure damage and control against actual player build distributions and summons, not a naked 20-HP zombie alone.

### 6.4 Stop unlimited passive sustain without deleting XP progression

Proposed resource rules (optional revision):

1. Keep maximum-pool formulas and custom cost curve initially; show actual predicted charge cost in UI. Clamp/validate finite inputs server-side.
2. Add an explicit combat inactivity gate, initially 100 ticks since an accepted hostile hit dealt or received. Existing stationary >40 ticks and Tenketsu restrictions also remain. Starting a cast exits meditation; raw key input alone must not authorize refill.
3. During an active upkeep transformation/summon, cap **all passive refill eligible for that mode** to `min(calculatedRefill, 0.25*totalUpkeepPerSecond)`; no independent sleep/food bypass. Without an active upkeep drain, retain the calculated out-of-combat refill initially. Active healing items/techniques need separate budgets and costs rather than being mislabeled passive refill.
4. Make this an opt-in policy and disclose it to players. A character may choose to dismiss a summon to meditate; each summon remains meaningful resource commitment.

For `M=53,334.86`, Perfect drain 115/s and a 25%-drain refill cap, net drain is at least 86.25/s while eligible: a theoretical maximum of about **618.38 seconds** from a full pool before entry/other casts. If combat disables refill, the same pool lasts about **463.78 seconds**. These are still long; if the RP server wants 30–90 second transformations, choose a separate percentage-pool upkeep or ability duration cap explicitly. Do not pretend the regen change alone creates short fights.

### 6.5 Point budgets and telemetry

Suggested **staff guidance only**: None 200 total points; Genin 500; Chunin 900; Jonin 1,500; Hokage 3,000, with existing per-stat caps. At Genin, six stats of 250 would cost 1,500—not the suggested 500-point budget. Compare equal-budget builds (e.g. 250 Strength + 150 Speed + 100 Health against 250 Chakra + 150 SPI + 100 Health), then compare their rank/XP/mastery progression separately.

Record controlled duel metrics: effective HP, raw/accepted damage per cast, damage after each hook, resource before/after, charge time, time to first response, control uptime, substitution outcomes, healing/regen/upkeep and approximate time to defeat. Start targets only after baseline capture; avoid a universal TTK claim across unequal RP ranks.

### 6.6 Implementation and proof order

1. Add semantic tests for current wiring: flag on/off, server/client agreement, real NBT migration, maximum resource vs tooltip, accepted-hit-only effects, original techniques unchanged.
2. Introduce a balance version/config synced from the server; pure policy helpers receive sanitized finite values. Do not overload the existing stat-allocation data version for a derived-formula toggle without explaining it.
3. If persistence shape changes, use distinct compounds such as `stats.chakraPoints` and `resources.chakraCurrent`; provide an idempotent, backed-up migration. Do not infer whether a long is double bits from its magnitude alone.
4. Add shared custom damage ownership/once-only dispatch, then test Twin/Company/Clone/Four-Pillar/paper-bomb exceptions and copied channel abilities. No global legacy conversion by accident.
5. Trial bounded Resistance and sustain policy separately, with configurable rollback. Clamp retained current resource to new max; define whether HP changes preserve percentage or absolute health, and test no free heal on login/config toggle.
6. Test at stats 0, each rank cap, 100k, 1M and 100M; XP 0/40k/100k; mastery 0/0.5/1; minimum/full charge; finite bounds, monotonicity, legal budgets and all damage-source types. Test cooldown persistence across duplicate items, relog, dimension change and copies.
7. Run two-client PvP and dedicated-server tests: low/high FPS, latency, armor from the other mod, healing, abs/void damage, Susanoo rider, Edo reform, multiple summons and shutdown/reload. Rebalance NPC/healing/mission reward assumptions only after obtaining results.

The purpose is a port that can explain its numbers and preserve RP choices—not an untested promise that every combination of extreme stats is already fair.
