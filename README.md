# Backpacks+

Rustic survival backpacks for Minecraft 1.21.1 / NeoForge 21.1.x, with tools and weapons
mounted on the sides and small supplies carried on the outer face or hanging from straps.

**Unreleased development build.** Storage, a real inventory menu, direct mount swaps,
the first gear HUD and server action messages are implemented. Models, textures,
third-person animations, Curios, recipes and migration are unfinished. The bag item
currently displays Minecraft's missing-model fallback; this is a functional checkpoint.

Basic, Reinforced and Expedition have **9, 18 and 36** ordinary cells plus **2, 3 and 4**
dedicated mounts. Their mount splits are one long/one small, two long/one small, and two
of each. Contents travel with the bag item. Normal item stack limits apply.

Hold a bag and right-click to open its actual menu. Click, split and shift-click normally;
the opened bag stays locked in its source cell. Ordinary furnaces, smokers, hoppers and
similar utility blocks fit. Backpacks, chests, barrels, bundles, shulkers and retained
portable inventories are rejected. A backpack may itself be placed in a chest.

The first equipment route uses the chest slot. **Hold G and scroll, then release G** to
swap the highlighted gear cell with the selected hotbar item. **H remains original Quick
Slot only**. Mount swaps leave it untouched. An incompatible old hand item enters ordinary
storage only when it fits completely; otherwise neither item moves. Focus loss or a bag/
hotbar change cancels browsing. Holding and releasing without scrolling does nothing.

Development items: `/give @s backpacksplus:basic_backpack`, with `reinforced_backpack`
and `expedition_backpack` as the other IDs in that namespace. Recipes await the materials
review. This build does not migrate legacy data; keep SB and Stowed in existing worlds.

## Build and validation

Java 21, official Mojang mappings, NeoForge 21.1.248. Build the unreleased Quick Slot
companion in the adjacent `minecraft-quickslot` checkout first: compilation uses its
`build/libs/quickslot-0.1.0.jar`, without bundling it. The runtime companion is optional.

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-watch-fs build
```

This runs JDK-only JUnit and dedicated-server GameTests. Test code stays out of the
production jar. Development runs `networkServer`, `networkDriver` and `networkObserver`
support Gradle properties `testServerDir`, `testDriverDir`, `testObserverDir` to reuse
isolated profiles. Inspect host processes before launching and keep master volume zero.
See [validation and remaining gates](devtools/verification/first-build.md).

The protocol describes server-timed gear actions independently of any animation library.
Fresh Animations Player Extension / EMF Compat Core integration is being evaluated;
it is not yet implemented or runtime-verified.

The wrapper is from the official [NeoForge MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle);
TEMPLATE_LICENSE.txt retains its licence. JDK rules live in `src/domain`, game adapters in
`src/main`, and decisions in [knowledge](knowledge/PROJECT.md).

Copyright (C) 2026 Rusty Shackleford and nfx. AGPL-3.0-or-later; see LICENSE.
