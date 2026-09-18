# Backpacks+

Repository `minecraft-backpacks-plus`; mod ID `backpacksplus`; namespace
`com.chunkworks.backpacksplus`. Minecraft 1.21.1, NeoForge 21.1.248, Java 21,
official Mojang mappings. Rusty authorized implementation after approving the persistent
gear bar (D-0009). Release 0.1.0 shipped in pack 1.38.0 under D-0022; see the deployment record.

## Approved direction

- Separate rustic survival backpack mod, replacing SB through the approved legacy discard and Stowed holster preservation (D-0019).
  Canvas/leather, practical straps/stitching, modest hardware. Supplied pictures establish
  mood, not a design to copy. Use the approved 80% SB comparison as a proportion reference.
- Basic / Reinforced / Expedition: 9 / 18 / 36 ordinary cells plus 2 / 3 / 4 mount cells.
  Mounts are long+small / long+long+small / long+long+small+small.
- Persistent gear bar beside the distinct original Quick Slot. Hold G, scroll, release
  to swap directly with the selected hotbar item. H remains original Quick Slot only,
  yielding to driving controls. Mount swaps never route through or displace Quick Slot.
- The bar stays horizontal, including at compact GUI sizes (D-0011). If it cannot fit
  beside Quick Slot, lift the complete mount row above the status icons, not into a column.
- D-0023 replaces the implicit storage fallback: incompatible mounts refuse the swap.
  Explicit G-menu bag actions stow a whole held or mounted stack only when it fits.
- No nested portable storage. Ordinary utility blocks including furnaces are permitted
  (D-0010); bags may go into chests. Keep D-0003's explicit chest/container prohibition.
- Curios equipment support; right-click opening while held; top-strap carry at the side.
- Early actual third-person draw/stow, taking the pack off, opening/retrieving and returning
  it. Preserve Refined Tools models and account for FA Player Extension/EMF/NEA.
- Keep Quick Slot's approved body placements, permanent bowl-meal hiding, single displayed
  item and all-item elytra hiding unless Rusty changes those rules.

## Current implementation

The three backpack items, immutable inline component storage, UUID/revision identity,
real inventory menu, mount transactions, first HUD/G adapter and server action/state
messages exist. The native chest route and optional Curios back slot are implemented (D-0017), with
Curios priority, cosmetic visibility and exact-source menu locking.
Original compact cuboid models retain their original vanilla pixel materials (D-0014).
Both generated atlas passes were reverted at Rusty's request. Natural bags use the exact
original material/UV mapping; dyes affect only the canvas. Dedicated mounts follow the worn/held bag;
real model measurements preserve Refined Tools proportions. The approved original pixel appearance and mount fit are retained.

D-0012's crafting is implemented: Basic leather/string/wool, iron-reinforced upgrades,
and Expedition iron-block/honeycomb/lead upgrades. Six shaped recipes cover optional dye,
and three shapeless recipes recolor with one dye, including in the inventory 2x2 grid.
Custom serializers retain standard recipe books, matching and result consumption while
preserving bag components and remapping existing mounts into the expanded inventory.
Minecraft's DYED_COLOR component is synchronized normally and colors only canvas quads.
All sixteen vanilla dyes are supported. See `devtools/verification/crafting-materials.md`
and `devtools/material-preview/index.html` for current evidence and captures.

Rusty's mount review corrected the upside-down sword and outward-facing pickaxe head.
Blades now sit straight against the side, with clearance derived from model thickness
and the bag tier's surface. Headed tools, shields and small tools use distinct orientations;
the approved scale limits remain unchanged. See `devtools/orientation-preview/index.html`
and `devtools/verification/mount-orientation.md` for the actual comparison and audit.
Torches and lanterns now use larger upright placements, matching the visible height of
their placed-block versions (D-0016). The original item/resource models remain in use.
See `devtools/lighting-preview/index.html` for actual before/after size and light captures.

Ordinary cells precede mounts in the vanilla CONTAINER item component. Replacement checks
every insertion; withdrawals of previously admitted items stay possible. Menus bind to an
exact stack/UUID/revision and lock their source cell. Overflow is refused and retained,
never truncated. This schema is not the Sophisticated Backpacks migration format.

Clients request bag UUID/revision, mount and selected hotbar index. The server chooses
the equipped source and moves actual server-owned stacks. Semantic action IDs describe
DRAW/STOW/EXCHANGE/OPEN/RETRIEVE/CLOSE, with server game time and visual before/after
stacks. Client layers now animate draw/stow/exchange, opening/retrieval/closing and top-strap
side carry. Protocol 3 adds the equipment source and cosmetic render visibility to validated worn
opening and persistent held/worn menu state.
Two client-only vanilla injections handle arm setup and transient hand-item ownership;
there is no other-mod mixin. An isolated, guarded EMF Compat Core API adapter publishes
only this mod's owned arms and clears only its own named source.

Current validation is recorded in `devtools/verification/release-readiness.md`;
`first-build.md` is the historical first checkpoint. The final record distinguishes
completed native, two-client and visual checks from hardware/group limitations.
The Curios checks and native G-key conflict correction are in `devtools/verification/curios.md`.

## Release status

Both 0.1.0 mods are published and deployed in pack 1.38.0 under D-0022.
The existing client is updated and the server is running with verified matching jars.
See [deployment](../devtools/verification/deployment.md).
The [release record](../devtools/verification/release-readiness.md) supersedes historical
open gates and records exact full-pack coverage, limitations and live cutover status.

Legacy bags/contents are discarded under D-0019. Preserve Stowed holsters through fresh
offline conversion. A separately approved private rollback snapshot precedes live changes.
The historical audit remains private scratch evidence and is never deployed or published.

Elytra hides worn bags/mounts while retaining controls, light and held bags. A visible bag
moves small Quick Slot items to the hip and hides large items. Lifecycle tests fixed the
native respawn sequence-reuse defect; repeated death/travel/relog/tracking checks passed.

Vault and graph freshness are not assumed. Reuse muted private-display profiles after
host-process inspection and stop them after checks; desktop focus is unnecessary.


## Unreleased follow-up — 2026-09-18

Rusty reiterated that Creative should allow freely obtaining and testing equipment
without consumables. Added all three backpack tiers to Tools & Utilities and Creative
search, correcting missing Creative registration. Survival crafting is unchanged;
ingredient-free crafting tables are not part of this fix. The regression failed before
the fix, then all 29 real-server GameTests passed. A full pack 1.39.0 client with shaders
found and retrieved all three bags through ordinary Creative search and clicks.
See [evidence](../devtools/verification/creative-inventory.md). Publication and deployment
are held pending an explicit release go.


## Dedicated Creative tabs — 2026-09-18, unreleased

Rusty requested a separate Creative inventory page for each item-adding mod, then
explicitly chose to group all vehicles in Vanilla Wheels.
The dedicated Backpacks+ Creative tab groups all three tiers, alongside their existing
Tools & Utilities and search entries.
No release or deployment is authorized by this follow-up.
Validation: 29 real-server GameTests and native full-pack Creative tab navigation/
item pickup passed; see [evidence](../devtools/verification/creative-tab.md).

## Release authorization — 2026-09-18

Rusty explicitly requested deployment: "Deploy it! I wanna play with it".
Version 0.1.1 is approved for publication and deployment in pack 1.39.1,
superseding the Creative-tab release hold above. Existing gameplay and world data
are preserved. Clean release builds and pack/hash verification gate deployment.

## Published release — 2026-09-18

Version 0.1.1 is published at
[GitHub Releases](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/releases/tag/v0.1.1)
and deployed to the server and Prism client in **pack 1.39.2**.
Clean release builds and real-server checks passed; the full-pack client verified
Creative tabs and item pickup. The downloaded release jar exactly matched the build.
The live server loaded the correct version and matched the published pack at 20 TPS.

Metals and Materials 1.0.2 is explicitly included in the pack: the first 1.39.1 startup
selected an older nested copy despite the updated Vanilla Wheels bundle. The 1.39.2
correction matches the directly installed materials jar used in full-pack testing.
Final startup verified all four updated mod versions; world, operators and DH settings
were preserved. This supersedes the historical release holds above.

## Explicit storage and hotbar HUD — 2026-09-18, unreleased

Version 0.2.0 implements [D-0023](decisions/D-0023.md). G rejects mount-size mismatches,
offers separate held/mounted "Put in bag" choices, and uses vanilla hotbar frames.
Protocol 4 requires matching client/server updates. Quick Slot 0.1.1 supplies its
matching HUD styling. Native reproduction caught the original behavior, and the
new mismatch regression failed before the fix. Final validation passed: 13 JUnit, 41 real-server checks including Curios, and
the complete-pack native interaction/visual checks under shaders. See
`devtools/verification/explicit-stowing.md`. Release remains held.


## Mount sizing audit — 2026-09-18, unreleased

Rusty reported rifles entering small mounts. The real server reproduced the failure:
Ranged Weapons Mod guns had no matching common or backpack mount tags and fell through
to small. Existing optional mount tags now put its rifle, scoped rifle, shotgun and
machine gun in long mounts, retaining the pistol in small mounts. The pack audit also
corrects Another Gun Mod long guns, Shield of the Deep, Mowzie's blowgun/sand rake and
Create's potato cannon/handheld worldshaper. Naga Fang Dagger and Block Factory's dagger
receive small overrides despite their sword inheritance. No admission algorithm, storage
schema or protocol changes. Existing misplaced items remain withdrawable and stowable.
The correction joins the held 0.2.0 follow-up; see
[verification](../devtools/verification/mount-sizing.md). No release go was given.


## Release authorization — 2026-09-18, pack 1.40.0

Rusty explicitly authorized publication and deployment of the backpack mount correction
and the coordinated Backpacks+ 0.2.0 / Quick Slot 0.1.1 update. This supersedes the
release holds for these two versions above. Clean builds and actual-item server checks
gate publication; a fresh empty-player check gates the production restart. Client
delivery is through Mod Hub; Rusty updates Prism themselves. The separate materials
packaging follow-up remains outside this release.
