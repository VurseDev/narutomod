# Edo Tensei Coffin asset

This is a Blockbench-authored future asset for the Forge 1.12.2 version of Naruto Mod Ctrl-Z.

- `edo_tensei_coffin.bbmodel` is the editable native Blockbench project. It keeps the full 68-cube, 9-group hierarchy and the embedded 256×256 texture.
- `edo_tensei_coffin.json` is a legacy Java Block JSON export for the repository's existing model workflow.
- The full authoring silhouette is 18×38×12.25 Blockbench units. The JSON export is uniformly scaled to 32 units tall so every element stays inside Minecraft 1.12.2's `-16..32` model-coordinate limit.
- `lid_root` is separated from the shell, trim, plaques, and fasteners so a future entity or TESR renderer can animate the lid without rebuilding the mesh.
- The runtime model is `assets/narutomod/models/custom/edo_tensei_coffin.json`; its texture is `assets/narutomod/textures/blocks/edo_tensei_coffin.png`.

For a future summoning sequence, use an entity or TESR renderer for the lid movement, rising animation, purple particles, sound, and lighting. The supplied JSON is the static 1.12.2-compatible fallback model.
