# The worn bag's panel on the inventory screen, photographed — 0.4.0, 2026-09-25

D-0027 shipped the panel unphotographed. Rusty asked for it photographed before release: the
survival inventory screen with an Expedition bag and a Basic bag, at 720p and at the 240-row
minimum, the recipe book closed and open, judged at 2x against the recipe book, the Curios
button and EMI's panels. The capture found two defects and one EMI rule; D-0028 records the
layout that replaced D-0027's.

## Harness

Backpacks+ has no booth; its client harness is the network fixture (`runNetworkServer` and
`runNetworkDriver` on Xephyr `:7` under llvmpipe, driven by `devtools/network_control.py`). It
gained:

- driver op `inventory` (the inventory key through `KeyMapping.click`), `recipeBook` (the
  book's own button through the screen's click path, so EMI's takeover of it runs too), and
  the state fields `screen`, `guiLeft`, `guiTop`, `recipeBook`;
- server op `wear` (a bag of the named tier worn, its mounts and a few cells filled);
- `devtools/capture_inventory.py`: both tiers at 1280x720 GUI scale 2 (640x360), 1280x720
  scale 3 (427x240, a friend's screen), 1080x720 scale 2 (540x360, just under the width the
  book needs beside the panel) and 960x720 scale 3 (320x240, the minimum), the book closed and
  open; the book's state is read and set, never assumed, since the server keeps it with the
  player. A suffix names a run with EMI on the classpath (`-PtestMountMods=<dir with the EMI
  jar>`).
- The fixture's vehicle helper linked against Vanilla Wheels, a compile-only dependency, and
  crashed the server on join without the vehicle jar; both callers now check the mod is loaded.

## What the first photographs showed (the shipped 0.4.0 code)

- **The open book painted over the panel at every width** ([640 wide](inventory-panel/before-640x360-book-open.png)):
  vanilla stands the book exactly where D-0027 put the panel, and the bag's item icons floated
  on the book's page. Not the narrow edge case D-0027 supposed.
- **Under 540 wide the book's button sat 57 pixels left of the screen**, over the player model
  ([427 wide](inventory-panel/before-427x240-book-closed.png)): D-0027 shifted the screen after
  vanilla had built the button from the unshifted position.
- The Curios button sits inside the vanilla frame at the player box's corner and was never in
  the panel's way.

## The layout now (D-0028)

`domain.InventoryPanel`, pinned by seven JUnit tests: a worn bag makes the vanilla layout 180
wider, the screen and the book shift right by 90, the open book stands beside the panel from
589 wide and takes the room below that (the panel yields, vanilla's own layout, the button
reachable), and under 360 the panel is not shown.

- [640, book closed](inventory-panel/inventory-expedition-640x360-book-closed.png): panel and
  screen centered as one; [the seam at 2x](inventory-panel/seam-panel-screen-640-closed.png):
  the four-pixel gap, the Curios button, the book's button in place.
- [640, book open](inventory-panel/inventory-expedition-640x360-book-open.png): panel, the
  book's tabs, the book, the screen; [the seam at 2x](inventory-panel/seam-panel-tabs-640-open.png):
  the tabs clear of the panel by the gap. The first cut of the rule put the panel against the
  book's body and the tabs, which hang 30 pixels left of it, overlapped the panel's last column.
- [427, closed](inventory-panel/inventory-expedition-427x240-book-closed.png): the panel with
  35 pixels to the edge, the screen 36; [427, open](inventory-panel/inventory-expedition-427x240-book-open.png):
  the panel yields, vanilla's book beside the screen, the button reachable. The first cut
  overlaid the book on the screen here as vanilla does under 379, but vanilla's overlay mode
  makes the button unreachable while the book is open, which would have stranded a bag-wearer
  with a book they could not close.
- [540, open](inventory-panel/inventory-expedition-540x360-book-open.png): the same yield one
  pixel under the beside width.
- [320](inventory-panel/inventory-expedition-320x240-book-closed.png): no panel; vanilla's own
  narrow mode.
- [Basic, 640, closed](inventory-panel/inventory-basic-640x360-book-closed.png) and
  [open](inventory-panel/inventory-basic-640x360-book-open.png): the one-row panel, sized to
  its tier.
- The book's open state is read from the player's recipe book, not the component: the
  component copies it only when laid out, so the first build read a stale value at init and
  produced a book laid out for the widened width over a screen laid out for the plain one.

## EMI

EMI takes over the book's button (its craftables toggle), so with EMI the vanilla book never
opens ([640](inventory-panel/inventory-expedition-640x360-book-open-emi.png): the right index
switched to craftables, the layout unchanged). EMI lays its sidebars out from the vanilla
screen's rectangle; a plugin declares the panel's column as an exclusion area.

- With the pack's configuration (favorites on the left, transparent theme) the panel was never
  painted over ([640](inventory-panel/inventory-expedition-640x360-book-closed-emi.png),
  [427](inventory-panel/inventory-expedition-427x240-book-closed-emi.png)).
- With the item index on the left and a themed sidebar, the Expedition panel pushed EMI above
  it but the Basic panel did not ([before](inventory-panel/before-emi-index-basic-640x360.png)):
  EMI shrinks a sidebar away from an excluded rectangle only when it covers a third of the
  sidebar's height or two thirds of its width, and merely skips entries under a smaller one,
  which its background then paints over. The plugin now excludes the panel's column at the
  screen's height: [Expedition](inventory-panel/emi-index-expedition-640x360.png) and
  [Basic](inventory-panel/emi-index-basic-640x360.png) at 640, EMI to the left of the panel at
  full height; [Basic at 540](inventory-panel/emi-index-basic-540x360.png), EMI above it.

## Build

`./gradlew clean build` on 2026-09-25: 35 JUnit (28 before; `InventoryPanelTest` adds seven)
and all 42 required GameTests; `build -PtestCurios`: all 48, after `SyncGameTests` learned to
build its mock with Curios' channels declared before the player is placed (Curios syncs its
own state to any joining player and threw for the framework's mock, which had made the Curios
variant red since that test was written). Jar `backpacksplus-0.4.0.jar` sha1
`569337ae3fb7aa4a5a404c45b3138807903d8b5d` (177597 bytes).

Not verified: Rusty's own 688-wide screen (the rule is pinned there by JUnit and the 640 photos
cover the same branch); the panel with a bag put on or taken off while the screen is open,
which lays the screen out again through `rebuildWidgets` (exercised by no photo); a live
second player.
