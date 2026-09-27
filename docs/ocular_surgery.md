# Ocular surgery — historical feature guide

> **Current handoff:** see the ocular section in [`../AI_HANDOFF.md`](../AI_HANDOFF.md). In particular, extracted organs currently return to the medic/surgeon, inventory eyes are side-neutral, and present code does not preserve an immutable donor birth side as this older guide implied.

This is an opt-in system for Forge 1.12.2. **Use a copied world first.** Completing surgery converts that patient's legacy paired-eye equipment into persistent left/right sockets. There is no in-game conversion back to the legacy equipment system. Players who have never completed surgery use the original hotbar dojutsu selector: left/right arrows to browse, Enter to confirm and G to toggle.

## Included

- Ocular Surgery appended to the existing Medical Ninjutsu item; it unlocks at exactly **3,000 Healing Jutsu XP on the medic's owned item**. XP is a qualification, not a payment. Healing's training cap now reaches 3,000; its power/mastery cap remains unchanged.
- Sneak-cast for self-surgery, or aim at another player within 3 blocks. The dossier selects each anatomical socket independently and previews its skill and upkeep before submission.
- The menu lists compatible eyes from both the patient's and medic's inventories. Other players receive a read-only approval screen showing the exact layout; closing or declining cancels it. A pending invitation does not suppress either player's abilities. The legacy `/eyesurgery accept|decline <session>` command remains as a fallback.
- Five-second stationary channel, 100 chakra charged on successful completion, ten-second recovery. Damage, movement, leaving range, disconnecting, changing dimensions, changing eyes or switching away from the qualified medical item cancels without consuming implants or the surgery charge.
- Preserved eyes are obtained by extracting a consenting player's currently equipped eyes. Removed eyes return to the **patient's** inventory. A legacy paired-eye item is consumed only once; its unused physical half returns to the inventory owner as a preserved eye.
- Persistent donor UUID, physical donor side, item data/XP and unique eye identity. Reimplanting on the other side does not change physical donor provenance. An eye cannot occupy both sockets; duplicate jars and old progression items cannot mint a second copy of a claimed donor eye.
- Full-inventory checks before any charge or mutation. Socket state persists across death, respawn and login. Death does not automatically drop installed organs.
- Normal eye colors, Sharingan stages, Mangekyo/EMS, Byakugan and normal Rinnegan are supported. One eye alone works; two different supported eye types work. Covers and empty sockets render independently over the existing player skin using the mod's textures and eye fitting.
- Advanced techniques resolve against their physical socket rather than equipping a hidden duplicate. Sasuke-family eyes split Amaterasu/flame control, Obito-family eyes split ranged/self Kamui, Madara eyes retain time manipulation, and normal Rinnegan retains its Six Paths controls. Susanoo requires a compatible two-eye Mangekyo/EMS pair.
- EMS awakening requires an Uchiha recipient's own paired Mangekyo progression plus both Mangekyo eyes from one different, recorded blood relative. Operators manage RP family records with `/eyesurgery family <player|UUID> <family-id|none>`. The resulting EMS keeps the recipient's ability family and the donor organs' physical identities.
- Non-Uchiha Sharingan cannot deactivate: G/key 3 covers or uncovers it. Byakugan can deactivate. Covering stops that eye's skills and upkeep; insufficient chakra covers active dojutsu. Both covered/missing/blind eyes cause blindness.

## Controls

- Operated players: **G toggles the left socket; H toggles the right socket.** Shift+G or Shift+H focuses that side for side-specific jutsu keys without toggling it.
- Special jutsu keys operate the currently focused physical eye. Technique cycling is also routed through that eye.
- A transplanted Sharingan that the recipient cannot deactivate is covered/uncovered instead. Rinnegan is similarly covered to rest.
- Unoperated players: **left/right arrows browse the original hotbar selector, Enter confirms, and G toggles the confirmed dojutsu.** H does nothing. The optional wheel has been disabled; Shift+G behaves like G for these players.

Upkeep when active and uncovered, per eye per second: basic Sharingan 2.5 chakra for the original donor / 10 for a recipient; Mangekyo 10 / 30; Byakugan 10 / 20; Rinnegan 20 / 40. Normal eyes cost none. Creative mode skips upkeep, but not the 3,000-XP surgery qualification.

## Deliberate limits

Tenseigan and Rinnesharingan are rejected. Clan training (for example Gentle Fist) is not granted by installing an eye. Mixed donors never merge into EMS, and surgery never grants unrelated Mangekyo powers. No corpse/forced-extraction or RP medical-licensing system is included.

## Test procedure

1. Install the same test JAR on client and server, with only one narutomod JAR loaded. Keep the previous JAR and a full world/player-data backup.
2. Equip the Medical Ninjutsu item and select Healing. For an administrator's disposable test character, the existing `/addxp2jutsu 3000` command adds XP to the selected jutsu; leave any biju cloak first. Check the item tooltip reads 3,000 Healing XP, then cycle to Ocular Surgery.
3. Sneak-cast while standing still. Select one empty socket, confirm and wait. Verify exactly one preserved eye appears and one physical socket remains empty.
4. Test an extraction on a second consenting player, then implant an eye once from the patient's inventory and once from the medic's. Verify the patient's approval screen, left/right visuals, G/H controls and chakra drain.
5. Test Mangekyo side skills, Rinnegan paths and a compatible paired Susanoo. Configure two disposable Uchiha characters with the same family ID and verify EMS only after the recipient has their own paired Mangekyo progression and receives the relative's paired Mangekyo.
6. Repeat with full inventories, walking, taking damage, rejecting consent, repeated requests, menu changes, both empty sockets, death, relog and a dimension change. Failed procedures must leave eye identities/items and the surgery charge untouched.

## Verification and implementation entry points

`gradle build --no-daemon` includes `checkOcularSurgery`, exercising the real XP methods, basic/advanced eye NBT, EMS lineage, ledger transitions, dual-inventory preparation, consent/channel distinction and packet rejection/roundtrips, plus existing Susanoo and paper-bomb suites. `tools/ocular/preview.gradle` runs the actual GUI renderer offscreen; those captures are **not** live multiplayer tests.

Core files: `MedicalSurgery.java` (sessions/transactions/network/consent), `OcularState.java` (serialized sockets), `OcularRegistry.java` (world identity ledger), `OcularPolicy.java` (shared rules), `OcularSystem.java` (controls/upkeep/lifecycle), `item/ItemOcularGear.java`, `client/GuiOcularSurgery.java`, `client/OcularModel.java` and the targeted existing item/key/sync adapters.

Remaining acceptance work: live two-player surgery and actual player-model visual testing. The headless checks and UI fixture cannot establish those outcomes.
