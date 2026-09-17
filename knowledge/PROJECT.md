# Backpacks+

Repository `minecraft-backpacks-plus`; mod ID `backpacksplus`; namespace
`com.chunkworks.backpacksplus`. Minecraft 1.21.1, NeoForge 21.1.248, Java 21,
official Mojang mappings. Rusty authorized implementation after approving the persistent
gear bar (D-0009). Release remains HELD. No remote, tag, push or live pack change yet.

## Approved direction

- Separate rustic survival backpack mod, replacing SB only after verified lossless migration.
  Canvas/leather, practical straps/stitching, modest hardware. Supplied pictures establish
  mood, not a design to copy. Use the approved 80% SB comparison as a proportion reference.
- Basic / Reinforced / Expedition: 9 / 18 / 36 ordinary cells plus 2 / 3 / 4 mount cells.
  Mounts are long+small / long+long+small / long+long+small+small.
- Persistent gear bar beside the distinct original Quick Slot. Hold G, scroll, release
  to swap directly with the selected hotbar item. H remains original Quick Slot only,
  yielding to driving controls. Mount swaps never route through or displace Quick Slot.
- The bar stays horizontal, including at compact GUI sizes (D-0011). If it cannot fit
  beside Quick Slot, lift the complete mount row above the status icons, not into a column.
- An incompatible old hand item enters ordinary storage only if the whole stack fits;
  otherwise refuse the exchange. Current implementation uses normal item stack limits.
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
messages exist. The native chest equipment route is implemented first; Curios is pending.
Original compact cuboid models now replace the missing-model fallback, using referenced
vanilla materials for the first motion review. Dedicated mounts follow the worn/held bag;
real model measurements preserve Refined Tools proportions. The model and artwork have
not been accepted as final.

Rusty's mount review corrected the upside-down sword and outward-facing pickaxe head.
Blades now sit straight against the side, with clearance derived from model thickness
and the bag tier's surface. Headed tools, shields and small tools use distinct orientations;
the approved scale limits remain unchanged. See `devtools/orientation-preview/index.html`
and `devtools/verification/mount-orientation.md` for the actual comparison and audit.

Ordinary cells precede mounts in the vanilla CONTAINER item component. Replacement checks
every insertion; withdrawals of previously admitted items stay possible. Menus bind to an
exact stack/UUID/revision and lock their source cell. Overflow is refused and retained,
never truncated. This schema is not the Sophisticated Backpacks migration format.

Clients request bag UUID/revision, mount and selected hotbar index. The server chooses
the equipped source and moves actual server-owned stacks. Semantic action IDs describe
DRAW/STOW/EXCHANGE/OPEN/RETRIEVE/CLOSE, with server game time and visual before/after
stacks. Client layers now animate draw/stow/exchange, opening/retrieval/closing and top-strap
side carry. Protocol 2 adds validated worn opening and persistent held/worn menu state.
Two client-only vanilla injections handle arm setup and transient hand-item ownership;
there is no other-mod mixin. An isolated, guarded EMF Compat Core API adapter publishes
only this mod's owned arms and clears only its own named source.

Validation and remaining gates are recorded in `devtools/verification/first-build.md`.
The tests cover real server/menu behavior and separate two-client input/sync checks;
they do not establish finished artwork, animation or shader compatibility.

## Next work

Review the first actual motion captures in `devtools/animation-preview/index.html` and
`devtools/verification/first-motion.md`. The isolated observer uses Fresh Animations 1.10.4,
FA Player Extension 1.1, EMF 3.2.4/ETF 7.1, NEA 1.12.4, PlayerAnimator 2.0.4 and EMF Compat
Core 1.1.2. The driver provides the plain-client comparison. No personal profile changed.
Refine the reaching path, hand contacts, visible fastenings and original material textures
after Rusty's first motion review; do not treat this prototype as final visual acceptance.

Curios, final model/pose/armor/elytra behavior, mount eligibility,
recipes/materials, death-policy review, dyes and dynamic lighting remain. Original Quick
Slot lighting is also unfinished. Preserve its pre-existing documentation changes.

The live pack still uses SB and Stowed. SB contents live in external UUID-indexed SavedData;
renaming item IDs is not migration. Local reference capacities 27/45/54/81/108/120 exceed
the new working capacities: preserve overflow and unmapped data. Audit copies of real
world data before conversion or removal. Releases and production changes remain held.

Vault mirror and generated graph freshness are not assumed; verify before structural claims.
