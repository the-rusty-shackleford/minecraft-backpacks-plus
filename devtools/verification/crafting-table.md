# The worn bag at the crafting table, photographed and driven with EMI — 0.6.0, 2026-09-28

D-0033. Captured with `devtools/capture_table.py` on the network fixture: `runNetworkServer`
headless and without EMI (as the pack's server), `runNetworkDriver` on the desktop display through
`tools/booth/run_iconified.sh` (the window iconified the moment it mapped; Rusty's own client was
running, the driver's config watcher off). Nothing sent focus. The server op `table` places a
crafting table beside the player and opens it; the client op `tableBook` clicks the table's book
button; `emiCheck` and `emiFill` ask EMI, as its sidebar and fill button do.

## What the photographs show

- [Expedition, 640x360, book closed](crafting-table/table-expedition-640x360-book-closed.png): the
  panel left of the table, its top level with the screen's, the pair centred (the table at x 322,
  vanilla's 232 plus half a panel).
- [Expedition, 640x360, book open](crafting-table/table-expedition-640x360-book-open.png): panel,
  tabs, book and table side by side (the table at 399). The book outlines the cobblestone stairs,
  slab and wall as craftable: the only cobblestone is in the bag, so the book counts it on the
  client (Carried D-0004).
- [Expedition, 427x240, book closed](crafting-table/table-expedition-427x240-book-closed.png) and
  [book open](crafting-table/table-expedition-427x240-book-open.png): at the 240-row minimum the
  closed book leaves panel and table side by side; the open one takes the room and the panel
  yields, as on the inventory screen (D-0028).
- [Expedition, 320x240](crafting-table/table-expedition-320x240-book-closed.png): under 360 wide,
  no panel, the table at vanilla's place.
- [Basic, 640x360](crafting-table/table-basic-640x360-book-closed.png) and
  [427x240](crafting-table/table-basic-427x240-book-closed.png): the one-row bag, two mounts.

## With EMI on the client

- [640x360 with EMI's sidebars](crafting-table/table-expedition-640x360-before-fill-emi.png),
  [427x240](crafting-table/table-expedition-427x240-book-closed-emi.png) and
  [Basic at 320x240](crafting-table/table-basic-320x240-book-closed-emi.png): EMI's sidebars keep
  off the panel; at 320 there is no panel and EMI lays out as without the mod.
- EMI's answers with the Expedition bag worn, logs and cobblestone only in the bag:
  `oak_planks=true`, `furnace=true`, `stone_pickaxe=false` (no sticks anywhere).
- EMI's fill of the furnace ([after](crafting-table/table-expedition-640x360-after-fill-emi.png)):
  `filled=true`; the server then read the table's grid as eight cobblestone round an empty centre,
  a furnace in the result slot, and the bag's cobblestone cell at 56 of 64.

The first photographs showed 14 iron ingots in a bag seeded with 5 and a golden apple, planks and
coal in the hotbar: stacks left lying in the fixture's world from the chest captures, picked up on
arrival (D-0029). The later captures, after a fresh `wear`, show the seeded 5.

## Not verified

A player clicking EMI's fill button by hand (the fill ran through EMI's own fill path from the
fixture op); EMI's fill on the inventory screen's 2x2 grid from the bag (the handler is the same
code with EMI's inventory handler underneath; counted by nothing but reading); shift-click at the
table through the live client (covered by GameTests on the server); Rusty's GUI scale 5.
