# Crafting, dyes and restored materials — 2026-09-17

Rusty requested the original textures back while retaining recipes and dye mechanics
(D-0014). All nine natural models match commit `2c8ce85` exactly after excluding only
canvas tint indices and the dyed-item selection predicate: textures, face assignments,
UVs, cuboids, pivots and display transforms are identical. Natural items receive white
tint, preserving their original appearance. Explicitly dyed items use white wool or
white terracotta canvas; leather, straps, lining and fittings are unchanged.

The generated atlas and its generation helpers are removed from the project and built
jar. Both new-art directions are superseded. [Actual captures](../material-preview/index.html)
show natural, red, blue and green at every tier. The [original tier comparison](../tier-preview/index.html)
remains the reference for proportions, materials and mounted gear.

## Checks run

`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-watch-fs build` passed
after the restoration: **13 JUnit tests and 28 dedicated-server GameTests**.

The nine crafting GameTests cover all recipe/unlock loading, recipe network round trips,
all sixteen dyes, any wool color, both full-bag upgrades, unchanged stack components,
mount remapping, inventory 2x2 recoloring, real result pickup, shift-click capacity refusal,
normal ingredient consumption, malformed input/overflow refusal and registry save/load.
Tests use actual server recipe managers and menus. JDK-only tests partition the upgrade
cell mapping across both adjacent tiers and invalid inputs.

Two existing isolated Minecraft profiles were reused against the dedicated test server.
Actual client menu-click packets took the crafted results; fixtures supplied ingredients
only. The full natural upgrade chain preserved storage, mounts, UUID and custom name.
Natural and three dyed variants of all tiers were captured from the other player's view.
All sixteen recolors preserved the full bag, UUID and name. Save/relog restored the exact
server and observer state. Leaving tracking range removed the actor from the observer;
returning restored the exact bag. See [recorded crafting evidence](crafting-network.json).

The capture runner saves evidence after each completed craft, allowing interrupted runs
to resume without repeating captures or launching duplicate clients. A tool process was
interrupted during the sixteen-color cycle; the remaining checks resumed on the same
running clients from the saved checkpoint.

The original and restored natural captures were visually compared, including enlarged
canvas, pocket, strap and buckle surfaces. Recipe-screen captures verify that item icons
also select the dyed canvas model. The current preview's eight color/framing combinations
load all three captures; the mobile layout fits at 390 pixels with no script errors.
The four changed/new Python helpers pass `mypy --strict`.

## Limits and release gates

The observer had Fresh Animations/FA Player Extension, EMF/ETF, NEA and Refined Tools
active. **Shaders were off**; no Iris/Complementary/BSL pass is claimed. Existing Refined
Tools unused spear/crossbow model errors remain visible in the logs; no Backpacks+ model
load failure was observed. No personal profile, production server or live pack changed.

Curios routing, mounted/Quick Slot lighting, lossless Sophisticated Backpacks migration,
remaining pose/armor/death/travel checks and shader validation still block release.
Release remains **HELD**.
