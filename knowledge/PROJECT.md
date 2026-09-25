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
  to swap directly with the selected hotbar item; releasing commits the highlighted cell
  whether or not the wheel moved, and the highlight opens on the first backpack slot,
  never on a bag deposit (D-0024). G is the bag's key alone: no bag, nothing; the quick
  slot is never a G choice (D-0025). H remains original Quick Slot only, yielding to
  driving controls. Mount swaps never route through or displace Quick Slot.
- The bar stays one horizontal row beside the hotbar (D-0011, D-0025): where four mounts
  would overflow, the cells close up (24, 22, then 20 pixels apart) before the row lifts
  above the status icons, which now happens only at the minimum GUI width. The mounts never
  move when G goes down; the held-deposit cell goes at the row's end or one row up over the
  quick slot's column. The ammo counter of Ranged Weapons Mod reads
  `GearClient.browsingBottomRight()` to move out of a gesture's way.
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


## Published release — 2026-09-18, pack 1.40.0

Version 0.2.0 is [published](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/releases/tag/v0.2.0)
and deployed through Mod Hub in pack **1.40.0**. The downloaded GitHub asset and
installed server jar both match SHA-1 `40b6a169b1707d94bcc8f1135d9bff868b329df8`.
Clean builds passed; Backpacks+ additionally passed all 45 real-server tests with
the published dependency versions. Startup confirms both new mods; pack parity is
clean and the sampled server rate is 20 TPS. A fresh zero-player check preceded
the restart. Existing world selection, seed, operators and DH configuration were
preserved. Rusty updates Prism themselves; this release did not modify their client.
This supersedes the release holds and pending-publication statements for this version.


## Firearm orientation correction — 2026-09-18

Rusty reported that 0.2.0 put newly classified long guns across the back. Version 0.2.1
corrects their orientation: barrel-up, broad face against the bag's side, with measured
clearance and unchanged size limits. Covers the four RWM long guns and six AGM gun
sprites. All bag tiers, both mounts, swapped pairs, reference tools and carried bags
were checked in the real shader client, followed by a G/wheel/release draw/restow.
See [evidence](../devtools/verification/firearm-mounts.md). Automatic approval review requires a separate hotfix release go; publication and
deployment are held pending that answer. No other held mod release is included.


## Hotfix release authorization — 2026-09-18

Rusty explicitly approved publishing Backpacks+ 0.2.1 and deploying pack 1.40.1
after the firearm visual and regression checks. This supersedes the automatic-review
release hold above. Only this backpack hotfix is included; the other held releases
remain held. A fresh empty-player check gates the production restart.


## Release G commits without a scroll — 0.2.2 published, withdrawn; 0.2.3 (2026-09-23)

Rusty: holding and releasing G should swap with the highlighted cell, but a scroll was
needed first. D-0024: releasing G commits the highlighted choice whether or not the wheel
moved (a press and release inside one tick counts too); the highlight opens on the first
backpack slot, never on a bag deposit, so releasing without a scroll never puts anything
in the bag. 0.2.2 was published with the highlight opening on the last swap made; Rusty
caught it in the two minutes before the server restart ("you shouldn't have to scroll if
the item you want is in the first backpack slot, which is already selected"), the restart
was cancelled, and 0.2.3 opens on the first slot with nothing remembered. The rule is
`domain.GearGesture` plus `GearChoices.defaultIndex`, pinned by JUnit; the client reads
the step. Protocol 4 is unchanged. Also since 0.2.2:
the gear sync sends a payload only to a connection that negotiated its channel, so a
server player whose connection has not (a gametest's mock, a client mid-handshake) is
skipped instead of an exception aborting the login or tick event for every later
listener; found when Ranged Weapons Mod put this jar on its gametest server for its
loose-mode bag tests, pinned by a gametest here. 20 JUnit and the real-server GameTests
passed; the real-client tap check waits until a second client can run on this desktop.
See `devtools/verification/gear-tap.md`. Release awaits Rusty's go, together with Ranged
Weapons Mod 2.7.0.

## G for the bag alone; the four-mount row stays beside the hotbar — 0.3.0 (2026-09-23, evening)

Rusty, with 0.2.3 on the box: "G should not apply to Quickslot, only backpack slots. No
backpack, G does nothing." and "my friends backpack slot bar, when the largest backpack is
equipped with 4 slots, is not being shown in the bottom right like always but is being
shown in the compact way. That sucks. When you fix this, make sure it does not clash with
the ammo indicator UI." D-0025: G never offers the quick slot and does nothing without a
worn bag (`GearChoices.build` takes no quick-slot flag and refuses zero mounts; the QUICK
kind and the quick-slot swap through G are gone). The mount row is placed by the pure
`domain.GearLayout`: beside the hotbar at 24, 22 or 20 pixels a cell, whichever first fits
(four mounts at 1280 wide and GUI scale 3, or 1920 at scale 4 with the hotbar attack
indicator, missed by four to six pixels and lifted), lifting above the status icons only
at the 320-pixel minimum; the mounts never move when G goes down, the held-deposit cell
going to the row's end or one row up over the quick slot's column. Ranged Weapons Mod's
ammo counter now asks `GearClient.browsingBottomRight()` instead of recomputing the old
fit. 28 JUnit (7 new for the layout at the friend's widths, the minimum, both hands, every
width 320..1000), all 36 real-server gametests, jar `backpacksplus-0.3.0.jar` SHA-1
`6c2faee0f59831dfcfe7942a72ba3d03bb81e4c1` (157257 bytes). The corner was photographed through
Ranged Weapons Mod's HUD booth at 427 and 480 GUI pixels wide, browsing and idle, with the
counter clear of the row (`minecraft-ranged-weapons-mod/devtools/verification/release-2.7.1.md`).
Released with Redstonewall Jackson 0.2.0 and Ranged Weapons Mod 2.7.1 (the counter's side of
D-0025) in pack 1.58.0, below.

## Published and deployed — 2026-09-24 UTC, pack 1.58.0

Version 0.3.0 is [published](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/releases/tag/v0.3.0)
and deployed through Mod Hub in pack **1.58.0**, replacing 0.2.3 on the server. Release
asset, tested jar and installed server jar match SHA-1 `6c2faee0f59831dfcfe7942a72ba3d03bb81e4c1`.
Rusty: "go. 2 minute warning then restart server". Restart 00:16:11 UTC after the warning
(one player still on), ready at 00:16:32, the log noting "backpacksplus (version 0.2.3 ->
0.3.0)", 20 TPS, Mod Hub parity clean. Not seen on a live client yet: G without a bag, and
the four-mount row on the friend's own screen. The server repo's
`knowledge/releases/pack-1.58.0.md` has the whole deployment.

## Published hotfix — 2026-09-18, pack 1.40.1

Backpacks+ 0.2.1 is [published](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/releases/tag/v0.2.1)
and deployed through Mod Hub in **pack 1.40.1**. Release asset, tested jar and installed
server jar match SHA-1 `36881028ae418631612c7961b6b99e10593283d9`. Startup explicitly
loads 0.2.1, pack parity is clean, and the verified sample is 20 TPS. The restart
followed a fresh zero-player check; world selection, seed, operators and DH config
were preserved. Mod Hub's downloaded client archive matches verified staging and
contains the new jar. Rusty updates Prism themselves; no personal client was edited.
The previous hotfix release hold is superseded by the explicit approval and deployment.

## Published and deployed — 2026-09-23, pack 1.57.1

Version 0.2.3 is [published](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/releases/tag/v0.2.3)
and deployed through Mod Hub in pack **1.57.1**, replacing 0.2.1 on the server (0.2.2 was
published, assembled into 1.57.0, and withdrawn before any restart). Release asset, tested
jar and installed server jar match SHA-1 `e0fbcfd181273b78c7e004e5f136b493dc9d4d28`. Restart
23:06:21 UTC with nobody online after the warning; ready at 23:06:35, the log notes
"backpacksplus (version 0.2.1 -> 0.2.3)", 20 TPS, Mod Hub parity clean. The real-client
check of the G release is Rusty's own on the pack.

## Ammo from the bag — 2026-09-24, 0.3.1, unreleased

Rusty: "My bow shows as empty even though I have many arrows in my backpack." Bows and
crossbows now find projectiles in the worn bag and any carried bag after the hands and the
inventory, through NeoForge's `LivingGetProjectileEvent`; the weapon is handed a marked copy
and a mixin at `ProjectileWeaponItem.useAmmo` debits the bag when the shot is taken and drops
the marker (D-0026). `BagAmmo` in main, `ProjectileWeaponItemMixin`, four GameTests in
`AmmoGameTests`; 28 JUnit and 40 GameTests green; jar sha1 `f8eed7023d1c5f651e89b82e8836b2226c8744e5`.
Waits on Rusty's go.

## The worn bag on the inventory screen — 2026-09-24, 0.4.0, unreleased

Rusty: E with a bag equipped should show the bag beside the inventory with free movement.
Forty fixed slots over the live `WornBag` container hang on vanilla's `InventoryMenu` on both
sides (mixin at its constructor's tail; the first index read off the menu since Quick Slot
adds a slot after ours), inactive without a bag, sized to the worn tier; shift-click from the
inventory goes into the bag first; `InventoryScreenMixin` draws the panel to the left
(D-0027). Two GameTests in `InventoryGameTests`; 28 JUnit, 42 GameTests; jar sha1
`a182ae2779d0f97ad443454ae2e9255c47597e5d`. Carries 0.3.1's bag ammo (D-0026). Waits on
Rusty's go.
