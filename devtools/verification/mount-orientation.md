# Mounted gear orientation review — 2026-09-17

Release HELD. [Before/after and side views](../orientation-preview/index.html).
The [motion clips](../animation-preview/index.html) were re-recorded with this correction.

## Change

Rusty reported an upside-down sword and a pickaxe head sticking out from the bag.
The original shared rotation put almost every large model broadside across the back.
The first correction inverted the sword but retained an outward lean; Rusty then asked
for the sword to sit flush. The final placement removes that lean and puts the blade's
broad face parallel to the bag's side. Clearance uses actual measured model thickness
and the Basic/Reinforced/Expedition side wall, rail or pocket surface respectively.

Tag-derived mounting directions distinguish blades, headed tools, shields and small
tools. Swords/tridents point down; 3-D pickaxe/axe/hoe heads and fishing rods turn along
the side. Shields face outward along the side. Shears/igniters show their broad face;
depth-oriented brushes turn upright. Bow, crossbow, shovel, mace, bottle, lantern and
food orientations retain their previous defaults. Flat tool sprites remain broadside
instead of receiving the 3-D headed-tool quarter-turn.

The actual NONE-context renderer, Refined Tools models, 0.92/0.26 diagonal fitting
limits and one-item display remain. Resource and tag reloads invalidate cached display
classification. The same placement applies to worn, opening and hand-carried bags.
No storage, swap, keybind, protocol or admission behavior changed.

## Executed this pass

- Reproduced and captured the original appearance before changing the renderer.
- Seven item sets captured on the final build, both with Refined Tools 3.0 and after
  disabling it through the client's real resource reload: diamond sword/pickaxe,
  axe/hoe, shovel/mace, trident/bow, shield/crossbow, fishing rod/iron sword and netherite
  sword/pickaxe. Small items included shears, flint and steel, brush, potion, honey bottle,
  milk bucket, torch, lantern, apple, pearl, ingot, carrot and sweet berries.
- Actual rear, left/right quarter and side views; reversed the sword/pickaxe mount
  positions. The final sword is straight and against the side pocket rather than leaning
  out; its broad face is intentionally seen from the side. The blade-down trident,
  side-facing shield and lengthwise brush were inspected too.
- Farmer's Delight 1.3.3 diamond knife and apple cider rendered; beef stew and vanilla
  mushroom stew remained hidden. The jar was staged only in the three isolated test
  profiles, with hashes retained in the session scratchpad.
- Two connected real clients: real G/wheel/release drew/stowed mounted gear, B opened
  the worn bag, shift-click produced RETRIEVE, and normal use opened the hand-carried
  bag. Both clients received the server state/actions. Reversed actor/viewer roles to
  capture slim/right and wide/left skins, with and without the animation mods.
- Observer: Fresh Animations 1.10.4, FA Player Extension 1.1, EMF 3.2.4, ETF 7.1,
  NEA 1.12.4, EMF Compat Core 1.1.2, NEA compat 1.2.0 and PlayerAnimator 2.0.4.
  The driver had no animation mods. Both used the existing muted profiles.
- `./gradlew --no-watch-fs build` passed: **10 JUnit and 19 dedicated-server GameTests**.
  `mypy --strict` passed for `capture_mounts.py` and `network_control.py`.
- The comparison page loaded all seven before/after/vanilla image sets in a real
  browser, with working tabs, no script errors and no overflow at 390px width.
- Both rendering clients and the isolated server exited. Host process inspection
  confirmed no Minecraft client remained. Quick Slot's three pre-existing documentation
  changes were preserved. No personal profile or production pack was changed.

## Limits and next visual work

This review used Expedition for runtime captures; the other tiers' side surfaces were
checked against their model geometry. The installed Galestone/Voidsteel 1.0.0 jars have
armor and material models, no tools to test. Create/Aquamirae/Alex's custom gear, the full
pose matrix and shader shadows were not exercised in this correction.

Refined Tools' existing invalid unused spear/crossbow resource warnings remain; no new
Backpacks+ renderer, measurement or animation-adapter failure appeared in the runs.
The overall backpack remains a prototype: original materials, visible attachment loops,
hand contacts and the reach paths still need their art/animation pass. This is not full
visual or release acceptance. Curios, lighting and safe legacy migration remain open.
