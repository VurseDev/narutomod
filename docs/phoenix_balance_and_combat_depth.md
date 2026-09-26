# Phoenix balance and proposed combat depth

> Scope update (2026-09-25): the user explicitly canceled the guard/parry/stagger/combat-depth proposal below. Only the Fire Phoenix changes in the implemented section are part of this build. The proposal is archived design context, not an implementation or a merger requirement.

## Implemented: Fire Phoenix only

The old no-gravity projectile used `1.15` as a per-tick motion multiplier:
`velocity = (velocity + acceleration) * 1.15`. This was unbounded acceleration,
not a launch speed of 1.15 blocks/tick. Other projectile classes are untouched.

- Phoenix speed now starts at 0.22 blocks/tick, increases by 0.028 each tick,
  and caps at 0.82 (4.4 to 16.4 blocks/second at 20 TPS). Lifetime: six seconds.
- Charge 0.8–4 grows entity scale from 0.95–2.70, continuously. Bird render scale
  was corrected from 0.24 to 0.45 of entity scale so its wing span approximately
  matches the collision width (about 1.14–3.24 blocks). The wings still animate.
- Acquires the nearest valid living target within eight blocks in a forward
  120-degree cone, with line of sight. No targeting behind the bird or through
  walls. It retains that target rather than hopping between players.
- Strong near-target steering; still accelerates and turns rather than teleporting.
  Maximum tracking separation is 48 blocks. High-charge shots aim high enough
  that their taller body does not fly into flat ground to reach a player's waist.
- Substitution's existing `UntargetableTicks` breaks an acquired lock permanently
  for that cast. The projectile does not reacquire when those ticks expire.
- Swept-volume water collision includes the wings and detects a one-block-thick
  water wall, including a wall raised around an already moving projectile.
  Water extinguishes without impact damage, burning, or an explosion. Solid cover
  also remains a counter; no terrain phasing, homing pathfinding or guaranteed hit.
- Damage formula remains `12 + power * 8` (18.4–44 before existing defenses).
  Chakra, mastery, charge delay and cooldown definitions remain unchanged.
  Impact has one damage event per eligible victim, not direct damage plus a second
  vanilla explosion. Burn only follows accepted damage against a still-targetable
  victim. Splash respects solid/water line of sight and a 1.5–4-block radius.
- No new terrain destruction. Existing impact visuals/sound remain, plus water hiss.
- Caster, teammates, own summons/Susanoo, creative/spectator and untargetable
  players are excluded. Existing player attack permission is respected.
- Power, lifetime, caster UUID and acquired target/history survive entity NBT.
  A reloaded shot without its caster terminates safely rather than becoming ownerless.

Files: `item/ItemKaton.java`, `PhoenixFlight.java`. Tests:

    gradle -I tools/phoenix/verify.gradle verifyPhoenix build --offline --no-daemon

Headless fixtures exercise production charge/steering, swept water/solid collisions,
moving-target pursuit, lock break, target filters, one-hit/burn behavior and NBT.
They do not replace a two-client test of the actual replacement hook, water-wall
placement timing, render alignment, sound and network latency.

### Live acceptance pass

1. Compare tap, half and full charge side by side. Full-charge wings must visibly
   grow, with no tiny model inside a large invisible hitbox.
2. Aim near a moving survival player at 15–25 blocks; check the slow launch, buildup,
   acquisition inside eight blocks and pursuit of a sideways sprint.
3. Repeat on flat ground with full charge: the bird must reach the player rather
   than detonate at their feet prematurely. Check slopes/trees as normal cover.
4. Raise Water Wall between caster and victim, including late raises and oblique
   approaches. Check that the victim behind it receives neither burn nor damage.
5. Enable body replacement and wait its existing cooldown. On contact, ensure one
   substitution, no post-escape explosion damage/burn, and no reacquisition.
6. Test teams, own Susanoo, chunk reload and simultaneous birds. Check dedicated
   server and both ordinary/high-latency clients before declaring PvP tuned.

## Proposed only: add depth without replacing Minecraft/Naruto PvP

Recommendation: adopt the pressure/counterplay of Bloodlines, not its entire
combat controller. Keep free aim, vanilla reach, sprint/strafe/jump, hotbar jutsu,
chakra, environmental cover, substitutions and elemental counters.
Do not apply Phoenix-style homing to every jutsu: aimed bolts, pursuit attacks,
area denial and defensive techniques need different jobs.

These numbers are initial playtest proposals, **not claimed Bloodlines timings**.
The [official game page](https://www.roblox.com/games/5571328985/Bloodlines) identifies
the intended game. Its [linked reference board](https://trello.com/b/6qTU0St2/bloodlines-trello)
was not readable through the research tool, so exact live mechanics were not verified.

### Step 1 — optional test rules and attack classification

Add a server-controlled experimental rules flag, initially off, with a test arena
or consenting participants. Leave other players' existing combat unchanged.
Introduce a small `CombatState`/`CombatRules` layer with server tick deadlines,
posture, action phase and hit sequence. Classify attacks explicitly as melee,
small projectile, large elemental, grab, control, or environmental.
Never infer all rules from the generic `ninjutsu` damage string.

### Step 2 — M1 chains without an aim-lock fighting mode

Use ordinary left-click attacks with fists/eligible weapons for a short three-hit
sequence; reset after a miss, delay or target change. Keep manual aim and normal
reach. No pull-to-target, animation teleport or midair infinite combo.
Respect each weapon's existing attack-speed cooldown; spam clicks do not earn
extra hits or posture damage. The third confirmed hit pushes apart, giving space
for a jutsu or escape rather than restarting an unavoidable chain.
Mining, inventory interaction and right-click jutsu charging remain untouched.

### Step 3 — guard, perfect block and parry

Use one remappable guard input (do not commandeer right-click, G or H).
Holding it blocks a front-facing cone and spends posture; facing away does not.
The first 3–4 server ticks after guard begins are the proposed perfect-block window.
A successful perfect block against melee produces the parry: crisp impact sound,
sparks and a short attacker recovery. These are related outcomes, not two redundant
buttons. Missed parries have recovery; repeated tapping cannot refresh indefinitely.
Small weapon projectiles may be deflected with suitable gear. Large Phoenix/fire
waves are **not** universally negated with bare hands: keep water walls/substitution
and matching jutsu defenses valuable. Telegraph exceptional unblockable attacks.

### Step 4 — true stun, deliberately short

Successful light hits: initially 4 ticks (0.2 seconds) of actual action lock.
Parry/guard break: initially 8–12 ticks (0.4–0.6 seconds). Keep camera look, gravity
and externally caused knockback; lock voluntary movement/attacks/casting, not the
whole simulation. A light hit does not automatically guarantee the next hit.
Use a victim-wide stun budget and diminishing returns across ALL attackers, with a
short recovery immunity after the budget is used. Gang attacks must not reset it.
Death, logout and dimension changes clear transient state.
Approved substitution/escape moves remain explicit exceptions. Do not reuse
`PotionParalysis`: the existing substitution hook explicitly refuses that potion.

### Step 5 — posture and temporary armor break

Posture is a separate guard-resource meter, not the chakra bar. Heavy attacks and
particular taijutsu moves pressure it more; it recovers after pressure stops.
Zero posture causes a brief guard break. A distinct, clearly telegraphed armor-break
finisher could then reduce ordinary armor effectiveness by about 20% for 3 seconds,
non-stacking. Never delete gear, permanently reduce armor, or silently bypass
Susanoo, chakra cloaks, immunities, and named barrier mechanics. Give those their
own posture/resistance rules only in a later balancing pass.

### Step 6 — integration and rollout

- `item/ItemTaijutsu.java`: classify existing taijutsu and specify posture/stagger;
  preserve its current damage, chakra/stamina and cooldown calculations initially.
- `item/ItemJutsu.java`: enforce stun at BOTH cast start and execution/release,
  including direct keybind paths. Prevent charging during stun and releasing later
  as a bypass. Debit resources only for an accepted action.
- `PlayerInput.java` plus new client animation/guard HUD: presentation and responsive
  input only. The server validates attack state, facing, range, sequence and cooldown.
  Rate-limit packets; reject client-supplied hits, timestamps and stun durations.
- `procedure/ProcedureWhenPlayerAttcked.java`, `SusanooCombat.java`, ocular defensive
  hooks and `GenjutsuSession.java`: audit event order before integrating. Existing
  invulnerability, targetability, mount/barrier routing and canceled hits must not
  accidentally cause stun, armor break or free posture gain.
- `item/ItemNinjutsu.java` replacement hook: an explicit allowed escape, with its
  existing resource/cooldown rules. No global paralysis rewrite.
- Keep vanilla hit cooldowns/hurt immunity initially. If faster light chains later
  need changes, scope them to validated combo hits; never globally zero hurt resistance.
- Test duels before team fights, then ping/TPS variation, shields, water walls,
  eye defenses, Susanoo, surgery keybinds and simultaneous damage sources. Tune at
  normal and high ping; generous tells are preferable to trusting client hit claims.

Start with guard/parry + short stagger, then add chains, then armor break. Do not
ship all three layers alongside universal homing buffs: that would make it hard to
identify what improved PvP and what removed counterplay.
