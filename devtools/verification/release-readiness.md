# Coordinated release verification — 2026-09-18

Rusty explicitly authorized finishing the remaining work and releasing. D-0022 supersedes
the old hold. Rusty also approved the private server-local rollback snapshot. The
consistent snapshot is complete; four fresh player saves were verified and two Stowed
holsters prepared entirely on the server. No new player data was exported.

## Completed

- Clean Java 21 builds: Backpacks+ **13 JUnit and 33 real-server GameTests**, with Curios;
  Quick Slot **26 JUnit and 22 real-server GameTests**. Neither production jar contains
  development tests, private player saves or test-world structures.
- Recipes, safe upgrades, sixteen canvas dyes and original pixel materials: [crafting](crafting-materials.md).
  Curios routing, visibility, source locks and G priority: [Curios](curios.md).
- Mount reaches now target the long/small mount actually used. Retrieval displays the
  server's withdrawn stack in the free hand. Server inventory timing is unchanged.
  All four mounts, both arms, repeated swaps and early close/reopen were exercised.
  [Actual motion and full-pack captures](../release-preview/index.html).
- Exact pack 1.37.0 jars/overrides, minus Stowed/SB/Core, in two real clients and a dedicated
  server. Native G/H/B, menu admission/source locks, full-capacity refusal, focus-loss
  cancellation, relog and tracking reentry passed. Roofs and Dusty Decorations remain.
- Stand, walk, sprint, crouch, swim, sleep and vanilla boat/horse riding executed. Prior
  actual elytra checks remain valid. An offline cape descriptor exercises the native
  CapeLayer. Water particles obscure some swim frames; these are runtime coverage, not
  clean visual comparisons. The sleeping body lies low against the bed in this pack; the same pose occurs in
  the control capture with no backpack equipped.
- Curios with diamond, Alex's Mobs rocky, Galestone and Voidsteel chestplates; Create wrench,
  FD knife, Alex's Mobs custom sword/pickaxe, oversized Wrought Axe, trident, shield and
  crossbow. The installed Alex tools omit ordinary tool tags: real admission reproduced
  rejection, then vanilla tool-class fallbacks admitted and oriented them correctly.
  Explicit small-item tags still win. Actual final shader captures verify the correction.
- Full-pack Complementary rendering and mounted light: remote source 15 with empty hands,
  an illuminated terrain field and world block-light zero. Earlier [mount lighting](mounted-lighting.md)
  and companion Quick Slot lighting cover underwater/removal/max/reload/absence and shaders
  with shader-held-light disabled. [Lifecycle](lifecycle.md), [companion fit](companion-fit.md)
  and [retirement rehearsal](legacy-retirement.md) remain valid.
- Shipped vehicles, independent clients: moving lights, passengers, driver/passenger H
  priority, slab road, stationary contact, head-on contact and towing. The observer's
  trailer initially stayed at its old position with Entity Culling tick optimization.
  Adding only `vanillawheels:vehicle` to `tickCullingWhitelist` fixed it in the real observer.
  Include that targeted pack config correction; vehicle jars and handling remain unchanged.
  Fixture camera/heading/course corrections are not production fixes. Rusty closed the
  old movement-warning investigation; the approved cosmetic vehicle pass is already shipped.

## Limits and environment

Controlify is loaded and coexists with the actual keyboard/menu tests. No physical gamepad
is attached, so hardware controls are not claimed tested. The previously deferred 4–8
player/Distant Horizons exploration remains a group check. Software off-screen rendering
is not representative GPU performance evidence. Clients are muted and use a private
authenticated display, avoiding desktop mouse grabs.

Alex's Caves, Aquamirae, Supplementaries and Small Ships are absent from the actual pack;
they were not silently installed. Shippy Ships is installed but its specific vessel has
no dedicated pose capture; the recorded riding checks use a vanilla boat and horse.

Connector required its supported `connector.clean.path` option in the development launcher.
One full-pack startup failed inside Puzzle/MidnightLib with ConcurrentModificationException;
the unchanged retry loaded. Existing Subwild recipe warnings are external to these mods.
Some long-lived tool sessions received SIGTERM without a Minecraft crash; an explicitly
owned fixture process completed the remaining checks. Failed/aborted runs are not passes.

## Deployment

Both 0.1.0 releases are published and deployed with pack 1.38.0. The existing local client
is updated and the server is running at 20 TPS with matching jar hashes. The approved
server-local snapshot and fresh holster conversion completed; legacy mods are retired.
See the [deployment record](deployment.md) for artifact hashes, exact checks and limits.
