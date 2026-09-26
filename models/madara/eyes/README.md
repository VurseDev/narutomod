# Madara eyes

`madara_eyes_preview.png` shows the actual generated 64px inventory icons, SVG
patterns, and a flat offline preview of the worn eyes on a sample player face.
It is not an in-game screenshot.

The worn textures preserve the existing Sasuke Sharingan atlas at 2048 x 512.
Every pixel outside the emissive-iris island is unchanged: the existing eye whites,
eyelids, eyebrow details, model UV coordinates, and player eye-fitting controls
are reused. The two new irises occupy the exact existing 34 x 33 pixel bounds.

Editable patterns are `madara_ms.svg` and `madara_ems.svg`. The MS has three curved
tomoe blades and a central pupil. EMS retains those features and adds the three
radial bars from the eternal pattern. Their source illustrations and attribution
are recorded in `src/main/resources/assets/narutomod/MADARA_EYES_CREDITS.txt`, which
is packaged with the mod. The iris adaptations are CC BY-SA 3.0.

To regenerate the PNGs and preview, set `NODE_PATH` to a directory containing
`sharp` and run `node tools/madara/build_eyes.cjs` from the workspace. To verify
existing outputs without writing them, add `--check`.

Runtime textures:

- `textures/mangekyosharinganhelmet_madara.png`
- `textures/mangekyosharinganhelmet_madara_eternal.png`
- `textures/blocks/mangekyosharingan_madara.png`
- `textures/blocks/mangekyosharingan_madara_eternal.png`

Natural awakening uses the existing Sharingan battle-XP and nearby-death gates,
then selects Sasuke, Obito, or Madara with equal probability. Ownership is carried
forward, and the displayed name changes to the new Madara eye item. Medical
transplanting with Madara as the recipient (slot 0) preserves Madara's family
when producing EMS. Existing eye theft, death, and anti-cheat rules still apply.
