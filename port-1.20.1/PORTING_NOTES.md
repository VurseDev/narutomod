# Naruto Mod 1.20.1 Port Notes

This folder is a clean Forge 1.20.1 workspace created from the Forge 47.3.0 MDK.

Current status:

- Forge 1.20.1 Gradle workspace exists.
- Mod id is `narutomod`.
- Java package root is `net.narutomod`.
- Legacy Java gameplay classes from 1.12.2 are not copied yet because the APIs changed too much to compile directly.
- A minimal creative tab and marker item are registered so the project can prove the modern mod loads.
- A modern client key mapping opens a placeholder V-key Training Stats screen.

Next porting steps:

1. Register modern items/blocks with `DeferredRegister`.
2. Convert legacy `.lang` translations to JSON.
3. Port chakra/player stats as modern player persistent data or capabilities.
4. Port key mappings and client screens.
5. Port dojutsu armor and render layers.
6. Port jutsu items and projectile entities one system at a time.
