# First third-person motion preview — 2026-09-17

Development checkpoint under D-0009; release HELD. [Watch the actual captures](../animation-preview/index.html).
This is Rusty's early motion/proportion review, before final artwork or complete compatibility acceptance.

## Implemented

- Original compact Basic/Reinforced/Expedition cuboid models. They reference vanilla
  wool/terracotta/wood/metal materials for this prototype; no finished texture claim.
- Real side-mounted long gear and outer-face small items. Measure the actual item
  renderer in NONE context so Refined Tools' body models are used. Normalize the
  complete measured diagonal; keep one displayed item regardless of stack count.
- Server-timed draw/stow/exchange arm gesture with a visual hand/mount handoff.
  Inventory still changes immediately on the authoritative server.
- Configurable B to open the worn bag; right-click still opens the held bag. Actual
  menus preserve their source-cell lock and admission rules. Held bags hang from
  their top strap, with mounted contents still visible.
- Shoulder reach, forward opening, hinged flap, retrieval gesture and return motion.
  The supporting hand supplies the final bag position. Early close reverses from the
  current opening amount. Persistent held/worn menu state survives transient action expiry.
- Two vanilla client-only `@Inject` hooks: after player model setup, and selective
  suppression of the hand item currently owned by this layer. No `@Overwrite`, no
  other-mod mixin, no animation-dependent server transactions.
- Guarded EMF Compat Core public API adapter: named source, owned arms only, own-source
  cleanup. Reflected public handles resolve once; absent optional mods load no API classes.

## Executed checks

- Final production build: **10 JUnit and 19 real-server GameTests passed**. New JDK
  partitions cover oversized/translated model fitting, invalid renderer bounds,
  timeline endpoints and interrupted opening. Registry-backed protocol checks retain
  menu source/time and copy ownership.
- Two real connected clients, existing muted profiles reused. Actual G/wheel/release
  inputs drew and stowed the server's mounted pickaxe; observer received DRAW/STOW.
  Actual B opened the worn bag. Shift-click generated RETRIEVE on the remote client.
  Normal use opened the held bag; both clients received its persistent open state.
- An earlier pass left tracking range and returned with the menu still open, after the
  short action had expired: the observer recovered the open pose. That pass also
  exposed disappearing mounts during hand carry; the corrected capture keeps them.
- Slim/right-handed actor observed with Fresh Animations 1.10.4, FA Player Extension 1.1,
  EMF 3.2.4, ETF 7.1, NEA 1.12.4, EMF Compat Core 1.1.2, NEA compat 1.2.0 and
  PlayerAnimator 2.0.4. Refined Tools 3.0/Modefite models appear in the actual capture.
- Reversed roles: wide/left-handed actor observed by a plain client without the animation
  mods. Actual G draw and B opening worked; runtime skin identifiers are retained in
  [evidence](animation-evidence.json). This verifies both layer registrations and the
  optional-mod absence path; it does not approve every hand-contact pose.
- Inspected worn, open, carried and carried-open screenshots, plus cropped motion frames.
  Refined Tools' previously reported unused crossbow/spear model warnings remain external
  to this change. No renderer or EMF adapter failure occurred in the captured actions.
- `mypy --strict` passed for the saved capture helper and network control helper.
  Both rendering clients and the isolated server were closed after capture; a host-level
  process check found only the desktop X server and a Gradle daemon. Quick Slot's existing
  three documentation changes remain untouched. The observer's isolated test profile keeps
  its staged animation mods for future reuse; original options and added-file hashes are
  backed up in the session scratchpad, separate from the personal profile.

## Visual review and next checks

The compact body silhouette and Refined Tools models are now reviewable in-game. The
animation is an early gesture, not a finished articulated pickup. Across-body reach,
secondary/small mounts, weapon grip orientation, brief handoff scale changes and some
hand/flap intersections still need work. Retrieval currently gestures toward storage;
it does not yet animate each withdrawn cursor stack along a physical path. Visible
straps/loops and original material textures await the art pass. No final visual approval.

Test next: quick repeated exchanges and opening/closing, all mount positions, both arms,
walking/crouching/swimming/riding/sleeping, armor/capes/elytra, custom and oversized items,
resource reloads, offhand bags and combinations of held/worn bags. Check conflicting B
bindings during migration. Shader shadows and the full pack remain unverified.

Curios routing, Quick Slot placement on the new bag, dynamic lighting, recipes/materials,
death/travel review and lossless Sophisticated Backpacks migration remain open project
gates. SB/Stowed remain installed in the live pack. No release, pack assembly, personal
profile edit, legacy removal or production restart occurred.
