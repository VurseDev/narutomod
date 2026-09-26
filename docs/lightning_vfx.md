# Lightning presentation pass

Four-Pillar Bind draws four temporary, stone-textured pillars. They emerge over
eight ticks, connect to the victim with narrow lightning arcs, then sink and fade
during the final twelve ticks of the bind. The cage is visual only: it does not
place blocks or create collisions. Its height and width now follow the target's
actual bounding box, including giant summons. The player-size cage keeps the
original four-pillar proportions; the vertex count remains constant as it grows.
Charging displays 0–100% on the action bar. Charge, Ninja XP and jutsu mastery
scale its single-hit damage. Canceled hits do not apply paralysis. Details and
formulas are in `merge_handoff.md`.

The existing rise, impact, and sustain sounds play in sequence. The server queues
the impact eight ticks after the rise, and quiet sustain cues during the bind.
Queues are bounded per world. Each client draws at most eight simultaneous cages
and 256 total transient effects; Minimal particles reduces the electrical detail.

Chidori Senbon retains its original needle model and combat rules, with local
white-blue trails, small hit sparks, and the electric snap sound. Lightning Clone
uses a radial discharge when its existing shock triggers. Luna's separate
Chidori/Raikiri addition stays disabled.

Earth, wind, and water use their restored presentation. The missing block-state
particle argument fix is retained.

The pre-wheel dōjutsu selector is restored for players who have not had surgery:
Left/Right arrows browse, Enter confirms, G toggles. Surgery continues to use its
existing G/H socket controls.

Madara Perfect Susanoo still requires 40,000 Ninja XP and Eternal Madara eyes.
`/addninjaxp` updates that same XP value. The upgrade now distinguishes missing EMS,
insufficient XP, and already being at Perfect. Operated players need both installed
eyes to satisfy the existing Eternal pair rule; an inventory eye alone does not
replace an installed eye.

## Verification

Run a normal build plus the additional fixtures:

    gradle -I tools/madara/verify.gradle -I tools/vfx/verify.gradle build verifyMadaraSusanoo verifyLightningVfx

The lightning fixture checks packet round trips, geometry limits, reduced-detail
budget, and animation endpoints, and renders the production geometry to
`build/reports/lightning/bind-preview.png`. This is an offline visual preview,
not a Minecraft screenshot. Live multiplayer, sound mixing, and GL presentation
still need an in-game check using the same new JAR on client and server.
