# Mounted light and lamp scale verification — 2026-09-17

Release **HELD**. Local development builds and isolated profiles only. The original
backpack materials, recipes, dyes and approved weapon/tool placements are preserved.
See the [actual before/after preview](../lighting-preview/index.html).

## Reproduction and implementation

Before the change, a mounted torch produced no dynamic source and left the closed stone
room dark. Holding the same torch produced level 14 and lit the floor. Evidence:
[baseline](light-baseline.json) and the matching photographs in the preview.

The installed backend is **Luminance 1.1.0**, identified from its `neoforge.mods.toml`
inside Vanilla Wheels 1.7.0. Backpacks+ registers its public interpolated player provider
and uses its existing `Providers.luminanceOf` lookup. No backend source, other-mod mixin,
lighting engine, invisible blocks or new network messages were added. Dry/wet brightness
is cached against immutable bag contents and invalidated on resource reload. Ordinary
storage does not contribute. Equipped and hand-carried mount contents do.

Torches now target 10/16 block visible height; lanterns target 11/16 including the handle.
The old generic 0.26-block limit included transparent sprite padding, making the visible
items especially small. Known vanilla padding is corrected, lamps stay upright, and
their back faces are seated at the outer pocket. Resource-pack item models remain in use;
three-dimensional models are measured and fitted. Other item scales are unchanged.

## Checks run in this session

- Java 21 build: **13 JUnit tests and all 28 required dedicated-server GameTests passed**.
  Lighting itself is gated by real clients, not by a simulated backend in a unit test.
- [19 real-client lighting checks](lighting-plain.json), with two clients on the dedicated
  test server: normal/soul torch and lantern brightness equal held brightness, including
  the same 49 terrain-light samples for the remote observer. Levels are 14/10/15/10.
- Held and mounted lights take the maximum. An equally bright held lantern leaves one
  effective level-15 source. Real G draw/stow keeps brightness; real menu clicks moving
  a torch into ordinary storage clear its source and terrain field.
- A bag in player inventory stays dark; carrying it by its handle retains mount light.
  Luminance's held-item switch and master switch were exercised.
- Actual water extinguishes normal/soul torches and leaves lanterns lit. A temporary
  resource definition changes a torch to 9 and Farmer's Delight apple cider to 11.
  Removing that definition clears the unchanged cider mount's light immediately.
- Save/relog and leaving/re-entering tracking range restore the remote source.
  Actual world block light stays **0** throughout the dark-room checks.
- [Shader evidence](lighting-shader.json): Iris **1.8.14-beta.1**, Sodium
  **0.8.13-beta.2**, Complementary Unbound **r5.8.1**. Iris's API confirms the shader pack
  is active. The shader's handheld-light effect is disabled; the remote player's mounted
  lamp still lights actual terrain. Matching empty/torch/lantern photographs demonstrate it.
- Final size captures cover normal/soul variants, paired lanterns, side contact, mixed
  lamps with Refined Tools weapons/tools, and hand-carried ground clearance. The shader
  captures are remote third person; the absence test also captures local third person.
- [Four no-Luminance checks](lighting-absent.json): the reused driver connects to a
  minimal dedicated server with no lighting backend, renders the bag and larger lamps,
  opens a worn bag with B, transfers the torch using real menu clicks, and opens a
  hand-carried bag with right-click. No missing-class error.
- [All 13 existing two-client input/storage checks](lighting-input-regression.json)
  passed, including independent Quick Slot, focus cancellation, furnace admission,
  shulker refusal, source locking, full-capacity atomic refusal, relog and tracking.
  The harness was corrected to account for pre-existing bread when merging, tolerate
  the real disconnect timeout, and avoid running/closing clients merely on import.
- Strict mypy passed for the five lighting/control/capture scripts.

The two existing muted client profiles were reused. Temporary shader files and light
definitions were removed, and all seven driver-profile jars were restored byte-for-byte
after the absence test. Host process inspection confirmed no test clients or test servers
remain. No live pack, production server, release tag or remote was changed.

## Limits and next gates

This completes backpack-mounted light and the requested lamp enlargement. It does not
complete the separate original Quick Slot light integration, Curios equipment routing,
Sophisticated Backpacks migration, or the remaining all-pose/armor/death/travel acceptance.
The tested shader is Complementary; BSL was not tested. Custom packs with unusual sprite
padding may need additional item placement tuning. The hand-carried photo checks ground
clearance; it is not evidence of a newly validated opening animation.

Existing Refined Tools unused crossbow/spear model warnings, shader pack GLSL warnings
and Sodium's non-optimized bounds-measurement consumer notice remain visible in test logs.
Large fixture teleports can generate movement warnings; these are not a vehicle-regression
test. They did not invalidate the recorded lighting/menu checks.

For manual review: compare torch and lantern size in the preview, then wear a lit bag in
a cave, swap its lamp with G, and move it into ordinary storage. Watch the ground light
and another player's view. Review all three backpack tiers and any custom lamp model in
your preferred resource pack before final visual acceptance.

Curios back-slot support and its mounted-light checks were subsequently completed in
[curios.md](curios.md); that equipment route is no longer pending.
