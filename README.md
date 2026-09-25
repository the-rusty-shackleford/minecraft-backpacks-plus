# Backpacks+

Rustic survival backpacks for Minecraft 1.21.1 / NeoForge 21.1.x, with tools and weapons
mounted on the sides and small supplies carried on the outer face or hanging from straps.

**Version 0.2.1.** Crafting and canvas dyes, Curios equipment, direct mount swaps,
the horizontal gear HUD, third-person carry/open/retrieval and optional Luminance lighting.
The approved original pixel materials and item scales are retained. See the
[actual motion and full-pack preview](devtools/release-preview/index.html) and
[release verification](devtools/verification/release-readiness.md).

Mounted swords now sit straight and blade-down against the bag's side; headed tools,
shields and small tools have their own orientations. The actual Refined Tools models
and existing scale limits are preserved. See the [before/after comparison](devtools/orientation-preview/index.html).

Mounted rifles, scoped rifles, shotguns and machine guns lie flat along the bag sides,
with barrels up and stocks down. This covers Ranged Weapons Mod and Another Gun Mod
long guns, including its auto gun and flamethrower. See the
[shader captures and verification](devtools/verification/firearm-mounts.md).

Basic, Reinforced and Expedition have **9, 18 and 36** ordinary cells plus **2, 3 and 4**
dedicated mounts. Their mount splits are one long/one small, two long/one small, and two
of each. Contents travel with the bag item. Normal item stack limits apply.

Long mounts take full-size weapons and tools: rifles (including scoped rifles), shotguns,
machine guns, bows, swords, shields and long-handled tools. Small mounts take sidearms,
knives/daggers, magazines, compact hand tools and supplies. The Ranged Weapons Mod pistol
and Another Gun Mod revolver remain small. Known untagged weapons from the installed
mods have explicit compatibility entries; see the [sizing audit](devtools/verification/mount-sizing.md).

Datapacks may classify additional gear through `backpacksplus:mounts/long` and
`backpacksplus:mounts/small`; the small tag takes precedence over broad weapon/tool tags.
Both menu insertion and G swaps use these rules. Items saved in a mount before a sizing
correction remain retrievable or explicitly stowable; the correction never deletes them.
Compare all three in the [in-game tier preview](devtools/tier-preview/index.html), with
matched rear/angled views, mounted gear, and full-player proportions.

**Creative inventory:** all three backpack tiers have their own **Backpacks+** Creative tab
and also appear in Tools & Utilities and search. See the
[full-pack tab check](devtools/verification/creative-tab.md).

## Crafting and color

In Creative, take any tier directly from the **Backpacks+** tab, or search for
**backpack** in the Creative inventory. Crafting and recipe unlocks are not needed
to obtain a bag for testing.

Use a normal crafting table. Each tier upgrades the previous bag, preserving its stored
items, mounted gear, custom name and other item data. You do not need to empty it first.

| Backpack | Ingredients, excluding optional dye |
|---|---|
| Basic | 5 leather, 2 string, 1 wool of any color |
| Reinforced | Basic backpack, 4 iron ingots, 1 leather, 2 string |
| Expedition | Reinforced backpack, 2 iron blocks, 2 leather, 2 honeycomb, 1 lead |

The [crafting guide](devtools/crafting-preview/index.html) shows each 3×3 arrangement.
Recipes also unlock in the vanilla recipe book when you obtain leather or the preceding
bag. Add any of the sixteen vanilla dyes in the bottom-center cell to choose the canvas
color. Leave that cell empty for natural Basic canvas or to retain the old color during
an upgrade. Undyed bags use their tier's natural palette.

Recolor an existing bag with **one backpack + one dye**, anywhere in a crafting grid,
including the inventory's 2×2 grid. One dye replaces the old color; colors do not mix.
Leather, straps, metal and lining remain their natural colors. New storage cells and
additional mounts start empty after an upgrade. Unsupported overflow refuses crafting
without removing items. Honeycomb is a crafting material, not a waterproofing mechanic.

## Using the backpack

Hold a bag and right-click, or press **B** with a bag equipped, to open its actual menu.
The open key is configurable; legacy backpack mods may also bind B during migration.
Click, split and shift-click normally;
the opened bag stays locked in its source cell. Ordinary furnaces, smokers, hoppers and
similar utility blocks fit. Backpacks, chests, barrels, bundles, shulkers and retained
portable inventories are rejected. A backpack may itself be placed in a chest.

Wear the bag in Curios’ **back** slot alongside a chestplate, or use the native chest slot.
Curios is optional; if both slots contain Backpacks+ bags, the Curios bag takes priority.
Its render toggle hides the bag and mounts while leaving storage, gear access and light
functional. The bag remains locked while its storage menu is open.

**Hold G, scroll to a gear cell, release G** to
swap it with the selected hotbar item. The highlight opens on the first backpack slot,
so for whatever is there you hold G and let go without touching the wheel. G is the
bag's key alone: without a worn backpack it does nothing, and the quick slot is never
among its choices. **H remains original Quick Slot only**. Mount swaps leave it untouched.
An incompatible mount turns red and explains which size it accepts; neither item moves.

While holding G, scroll to a **backpack icon** to deliberately put something in the bag.
Each occupied mount has its own arrow-linked action, labeled **Put mounted [item] in bag**.
A separate **Put held [item] in bag** action frees the selected hotbar slot. Release G to
commit. The entire stack must fit ordinary storage; otherwise nothing moves and the HUD
explains why. Stowing a mount leaves your held item and other mounts alone.

Focus loss, opening a menu, or a bag/hotbar/held-stack change cancels browsing. Releasing G
commits whatever is highlighted; a bag action is never highlighted until you scroll to it,
so a tap never puts anything in the bag. Item counts, names and other components survive
storage exactly as they do through the backpack menu.

Curios also defaults to G. While wearing a bag, the gear gesture takes priority when the
bindings match. Open Curios with its inventory-screen button or configure another key;
without a bag, Curios retains its G shortcut. Your saved key bindings are not rewritten.
See the [Curios compatibility checks](devtools/verification/curios.md).

The HUD uses the active resource pack's vanilla hotbar frames and selection border,
matching Quick Slot 0.1.1. The mounts always form one horizontal row beside the hotbar, on
your main hand's side. Where four mounts would run off the screen at a large GUI scale, the
row closes its cells up (touching, then sharing borders as the hotbar's do) rather than
moving; only at the smallest GUI width, where nothing fits beside the hotbar, does the row
sit above the status icons with its outer end aligned to Quick Slot. It never becomes a
vertical column. While you hold G with something in hand, the "put held item in bag" cell
appears at the end of the row, or one row up over the quick slot's column when the row is
full; the mounts themselves never move. Ranged Weapons Mod's ammo counter, which shares the
bottom-right corner, asks this mod whether a gesture is open there and moves up while one is.

**E shows the bag too.** With a bag worn, your own inventory screen (E) carries the bag's
cells in a panel to its left: the mounts across its top, the storage rows below, under the
bag's name. Click, drag and split between them as between any two inventories. Shift-click
from your inventory puts the item into the bag first (armor that would equip into an empty
slot still equips); shift-click from the bag brings it back. Take the bag off and the panel
goes. The bag's own screen (B) is unchanged.

**Arrows in the bag count.** A bow or crossbow with nothing to shoot in your hands or
inventory looks in your backpack, the worn one first and then any bag you carry, and takes
its arrows (or any projectile it accepts) from there, one shot at a time; the bag's count
falls as you shoot, and an arrow you pick back up is an ordinary arrow. Arrows in your
inventory are still used first. Infinity works off a bag's arrow without spending it.

Item IDs: `/give @s backpacksplus:basic_backpack`, with `reinforced_backpack`
and `expedition_backpack` as the other IDs in that namespace. For existing worlds, follow the coordinated [cutover procedure](devtools/verification/legacy-retirement.md).
Existing legacy bags and their contents will be discarded as requested. In Survival, new bags require crafting; Creative provides all three tiers directly.

When elytra is equipped, the worn backpack and its mounts are hidden to keep the wings
clear. B, G, storage and mounted lighting still work. A bag carried in hand stays visible;
removing elytra restores the worn model. See the [actual captures](devtools/companion-preview/index.html). The separate Quick Slot hides large back items
while a backpack is visible and moves small lower-back items to the hip.

## Mounted light sources

With Luminance 1.1+ installed, light-emitting items in the equipped backpack's mounts
light your surroundings and are visible to other clients. Mounts stay lit when you carry
the backpack in hand. Ordinary storage and bags tucked inside the player inventory do not
emit light. Torches, lanterns, soul variants and resource-defined modded items use the
same brightness and underwater rules as held items. Several lights use the brightest
value, without adding brightness together.

Luminance's settings control the feature; its master, held-item and entity-provider
switches apply. Without Luminance the backpack works normally and adds no dynamic light.
Light is visual only: it never changes world light or mob spawning. The companion Quick Slot also has verified held-equivalent dynamic lighting, including
remote players and shaders. See the [real-client lighting checks](devtools/verification/mounted-lighting.md).

## Build and validation

Java 21, official Mojang mappings, NeoForge 21.1.248. Build the Quick Slot
companion in the adjacent `minecraft-quickslot` checkout first: compilation uses its
`build/libs/quickslot-0.1.1.jar`, without bundling it. The runtime companion is optional.
Also build `minecraft-luminance` 1.1.0 in the adjacent checkout for its compile-only API
jar at `build/libs/luminance-1.1.0.jar`. Luminance remains optional at runtime and is not
bundled into Backpacks+. Curios 9.5.1’s API is resolved from its official Maven repository
for compilation only. Add `-PtestCurios` to run the dedicated tests against the full Curios
backend; it remains optional and unbundled.

For real-item mount compatibility tests, add `-PtestMountMods=/absolute/path/to/test-mods`.
That directory supplies actual runtime jars and their dependencies (including Ranged
Weapons Mod and Metals and Materials); none is packaged in Backpacks+. Loading Ranged
Weapons Mod enables four additional tests covering its guns, menu and G-swap paths,
legacy retrieval, and 50 representative gear items when their mods are present. Keep
Distant Horizons out of the headless GameTest fixture: version 3.3.1 expects a normal
dedicated server and fails before GameTests run. See the sizing audit for tested jars.

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-watch-fs build
```

This runs JDK-only JUnit and dedicated-server GameTests. Test code stays out of the
production jar. Development runs `networkServer`, `networkDriver` and `networkObserver`
support Gradle properties `testServerDir`, `testDriverDir`, `testObserverDir` to reuse
isolated profiles. Inspect host processes before launching and keep master volume zero.
See [current validation and remaining gates](devtools/verification/release-readiness.md).

Protocol version 4 describes server-timed gear actions independently of any animation
library, including persistent menu state, equipment source and render visibility for late viewers. Inventory changes happen on
the server immediately; the client gesture does not move items. Vanilla player/hand
rendering uses two small client-only `@Inject` hooks. No other mod is mixed into.
An isolated optional adapter publishes owned arm poses through EMF Compat Core's public
named-source API. It is checked with Fresh Animations Player Extension, EMF/ETF and NEA,
alongside a plain client without those mods. See the [motion test record](devtools/verification/first-motion.md)
for the original checkpoint; the current release record contains the later full-pack and shader checks.

The wrapper is from the official [NeoForge MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle);
TEMPLATE_LICENSE.txt retains its licence. JDK rules live in `src/domain`, game adapters in
`src/main`, and decisions in [knowledge](knowledge/PROJECT.md).

Copyright (C) 2026 Rusty Shackleford and nfx. AGPL-3.0-or-later; see LICENSE.

Backpacks+ 0.2.0 adds explicit storage requests and requires protocol 4 on both client
and server. It preserves existing backpack contents and item IDs.
