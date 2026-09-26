# Curse Mark of Heaven assets

These are player overlays for the 1.12.2 mod, not a replacement player model.
The player supplies their own skin, clothing, body, and hair.

- `heaven_stage1.png` / `_slim.png`: transparent black Stage 1 flame marks.
- `heaven_ignition.png` / `_slim.png`: the same marks in a warm orange-red ignition pass.
- `heaven_stage2.png` / `_slim.png`: transparent dark Stage 2 marks with the bridge-of-nose star/cross.
- `curse_mark_heaven_wings.bbmodel`: editable Blockbench `modded_entity` rig for the Stage 2 hand-shaped wings. It contains 84 accessory cubes, no player body, and no Sasuke hair.
- `heaven_wings.png`: the matching 256×256 accessory atlas for a future Java `ModelRenderer` export.

The PNGs use a 128×128 doubled Minecraft skin layout so the overlay can be rendered over a normal or slim player model with a small positive shell/inflate. The runtime copies are under `src/main/resources/assets/narutomod/textures/cursemark/`.

The model and texture design follows the three-tomoe neck origin, flame-like Stage 1 spread, dark Stage 2 mark, nose star, and webbed claw-wing silhouette described on the [Cursed Seal of Heaven wiki page](https://naruto.fandom.com/wiki/Cursed_Seal_of_Heaven).
