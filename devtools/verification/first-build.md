# First functional build — 2026-09-17

Status: development only; release HELD. This is backend/input evidence, not approval of
the model, textures, third-person motion, Curios or shader compatibility.

The later [first motion checkpoint](first-motion.md) adds the original model and animation
preview. The missing-model and absent-renderer statements below describe this earlier build.

## Executed checks

- JDK-only JUnit: 6 passing tests for whole-stack destination policy, stale-intent
  authorization and stable semantic action IDs.
- NeoForge dedicated-server GameTests: 19 passing tests for actual menus, serialization,
  identity/revision, nesting, tiers, full capacity, mount exchanges and registry codecs.
- Two real Minecraft clients connected to a dedicated test server, reusing the existing
  muted Quick Slot driver/observer profiles. 13 integration assertions passed:
  - initial equipped-mount synchronization to both clients;
  - release-G exchanges actual hand/mount items;
  - original Quick Slot remains unchanged;
  - observer receives EXCHANGE action semantics;
  - focus-loss cancellation;
  - incompatible old hand stack goes into ordinary storage;
  - real menu accepts 16 ordinary furnaces;
  - real menu rejects a shulker without losing the cursor item;
  - opened source bag cannot move;
  - full bag refuses an incompatible exchange without mutation;
  - normal scrolling still selects the ordinary hotbar;
  - server save/relog retains exact UUID, revision and contents;
  - leaving/re-entering tracking range restores current equipment.

The controls were exercised through real key mappings, NeoForge wheel events and normal
Minecraft menu-click packets. Fixture commands only seed items/positions and collect
evidence; production swap/state packets perform the tested exchanges.

The first pass exposed a harness race: the client had reconnected before the server's
250 ms evidence report included it. Waiting for both reports fixed the test; the complete
rerun passed. A separate initial Java split-package error was corrected by keeping the
test driver's package separate from the production client package.

The real-server furnace regression caught Minecraft's default EMPTY CONTAINER component
on ordinary furnace items. Admission now allows that harmless default and rejects actual
retained contents; it does not instantiate arbitrary mod block entities to classify items.

## Visual inspection and limits

[Inventory menu](storage-menu.png), [right-hand compact bar](gear-right.png), and
[left-hand compact bar](gear-left.png) were inspected from real-client screenshots.
The gear images were refreshed after Rusty's D-0011 correction: the original in-game
vertical fallback was a mistake. Mounts now stay in one horizontal row, matching the
browser design. At 854×480 (427×240 GUI), the complete row lifts above the status icons;
at 1280×720 (640×360 GUI), it fits beside Quick Slot. Both arms were visually inspected:
[wide right](gear-wide-right.png), [wide left](gear-wide-left.png). The row clears the
hotbar, health/hunger and reserved hotbar attack-indicator space in all four views.
G selection highlights were visible during capture. The correction's build also passed
all 6 JUnit and 19 GameTests. The inventory drawing is a working layout, not finished
backpack artwork. The held bag's missing-model fallback is still visible in the menu.

The reused profiles include Refined Tools 3.0, Curios, SB/Core, Vanilla Wheels, Trailblazer
and Modefite, with Luminance bundled by Wheels. Stowed is absent from these isolated
profiles. No personal profile or live pack was changed. Refined Tools logged malformed
unused crossbow/spear models (missing rotation axis and missing models); this run does
not establish those models' compatibility. The tested tool icons rendered.

Fresh Animations/FA Player Extension/EMF/ETF/NEA were verified as enabled/installed in the
personal profile, but are not loaded in this minimal input test. The installed EMF Compat
Core 1.1.2 exposes source-priority pose save/clear APIs, a promising adapter surface.
No compatibility adapter, animation preview or shader validation is claimed yet.

## Next test gates

Third-person worn model and animated gear drawing/opening; both skins and all poses;
remaining G/H/Controlify conflicts and unusually tall status overlays; Curios routing and visibility;
death/keepInventory/vanishing/travel and server restart; optional-mod absence after client
integration; pack-specific nested-container formats; mounted/Quick Slot light and shaders;
lossless legacy migration with overflow recovery. Production changes stay held.

## Later crafting and material pass

See [crafting, dyes and restored materials](crafting-materials.md) for the current recipe
and dye checks. The original material textures are restored under D-0014. The historical
first-build limitations above describe that earlier test, not the completed crafting pass.

## Mounted light and larger lamps

The 2026-09-17 lighting/size pass is recorded in [mounted-lighting.md](mounted-lighting.md),
including real two-client, terrain, underwater, reload, shader and optional-dependency
absence checks. See [the visual comparison](../lighting-preview/index.html). Release HELD.

## Optional Curios equipment

The Curios back-slot, visibility and real G/B input checks are in [curios.md](curios.md).
All three tiers coexist with chest armor; Curios takes priority over a second chest bag.
Source identity, capacity, remote snapshots, relog and absence checks passed. Release HELD.

## Release-readiness checks

The later [lifecycle checks](lifecycle.md), [legacy retirement rehearsal](legacy-retirement.md)
and [Quick Slot/elytra fit](companion-fit.md) supersede those individual historical open
items above. Retirement now discards old bags and their contents under D-0019; Stowed
holsters are preserved separately. The final animation/full-pack review remains open.
