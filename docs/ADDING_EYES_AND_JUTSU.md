# Adding New Eyes And Jutsu

This mod is a Forge 1.12.2 / MCreator-style project. Build it with:

```powershell
.\gradlew.bat build
```

The finished jar is created at:

```text
build/libs/modid-1.0.jar
```

## Add A New Eye Recolor

Use this path when you only want a new eye look, like a recolored Sharingan, Byakugan, Rinnegan, or Tenseigan.

1. Pick an existing eye texture to copy from:

```text
src/main/resources/assets/narutomod/textures/blocks/sharingan.png
src/main/resources/assets/narutomod/textures/blocks/byakugan.png
src/main/resources/assets/narutomod/textures/blocks/rinnegan.png
src/main/resources/assets/narutomod/textures/blocks/tenseigan_item.png
src/main/resources/assets/narutomod/textures/blocks/mangekyosharingan_sasuke.png
src/main/resources/assets/narutomod/textures/blocks/mangekyosharingan_obito.png
src/main/resources/assets/narutomod/textures/blocks/mangekyosharingan_eterna.png
```

2. Copy it into the same folder and rename it. Example:

```text
src/main/resources/assets/narutomod/textures/blocks/my_blue_sharingan.png
```

3. Recolor the new PNG in Paint.NET, GIMP, Photoshop, Aseprite, or any PNG editor.

4. Copy this template model:

```text
src/main/resources/assets/narutomod/models/item/template_eyehelmet.json
```

Rename the copy to your item model name, for example:

```text
src/main/resources/assets/narutomod/models/item/my_blue_sharinganhelmet.json
```

5. Open the copied JSON and change only the texture line:

```json
"1": "narutomod:blocks/my_blue_sharingan"
```

Do not include `.png` in the texture path.

6. If you are using MCreator, add/import a new helmet/armor item and point its item model to your new JSON. If you are coding by hand, copy an existing eye item class such as:

```text
src/main/java/net/narutomod/item/ItemSharingan.java
src/main/java/net/narutomod/item/ItemByakugan.java
src/main/java/net/narutomod/item/ItemRinnegan.java
```

Then change the registry name, unlocalized name, model resource location, and any ability logic.

7. Build again:

```powershell
.\gradlew.bat build
```

## Add A New Jutsu

Use an existing jutsu as your base. The easiest examples are:

```text
src/main/java/net/narutomod/item/ItemKaton.java
src/main/java/net/narutomod/item/ItemRaiton.java
src/main/java/net/narutomod/item/ItemNinjutsu.java
src/main/java/net/narutomod/entity/EntityRasengan.java
src/main/java/net/narutomod/entity/EntityChidori.java
```

1. Add a new `ItemJutsu.JutsuEnum` entry in the item class you want. Example pattern from `ItemKaton.java`:

```java
public static final ItemJutsu.JutsuEnum MYJUTSU =
    new ItemJutsu.JutsuEnum(6, "my_jutsu", 'C', 30d, new MyJutsu());
```

2. Add the new enum to the item constructor list. Example:

```java
elements.items.add(() -> new RangedItem(GREATFIREBALL, GFANNIHILATION, HIDINGINASH, GREATFLAME, FLAMESLICE, BARRIER, MYJUTSU));
```

3. Make the jutsu behavior class. For a simple projectile, copy the inner `EntityBigFireball.Jutsu` pattern in `ItemKaton.java`. For a held/charge jutsu, copy the `EntityRasengan.EC.Jutsu` pattern.

4. Add a scroll/item model if the jutsu needs a visible scroll:

```text
src/main/resources/assets/narutomod/models/item/template_jutsu_scroll.json
```

Copy and rename it, then point its texture to a scroll texture under:

```text
src/main/resources/assets/narutomod/textures/blocks/
```

5. Add language/tooltip keys if your jutsu displays text. Search for the existing key name first:

```powershell
rg "katonfireball|rasengan|chidori" src/main/resources src/main/java
```

6. Build and test:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

## Quick Rule

For recolors, copy PNG + copy JSON.

For real new powers, copy an existing item/entity Java pattern, rename everything carefully, then build.
