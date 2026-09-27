# The worn bag's panel beside a chest, photographed — 0.5.0, 2026-09-27

D-0030. Captured with `devtools/capture_chest.py` on the network fixture: `runNetworkServer`
headless, `runNetworkDriver` on the desktop display through `tools/booth/run_iconified.sh`
(Rusty's own client was running; there is no Xvfb here). The server op `chest` places a single
or a double chest beside the player with five stacks and opens it for them.

The first live launch crashed at mod load: the chest screen's hook on vanilla's centred texture
position (a local-variable modifier) found no target. The gametest server loads no client mixins,
so only a real client could catch it. `@ModifyArg` on the two texture draws replaced it. The
first "double" photos were single chests: the fixture placed the two halves on the wrong axis.

## What the photographs show

- [Expedition, single chest, 640x360](chest-panel/chest-expedition-single-640x360.png): the
  panel left of the chest screen, its bottom level with the screen's, beside the inventory rows;
  the pair centred (the chest screen at x 322, vanilla's 232 plus half a panel); the chest's
  texture under its slots.
- [Expedition, double chest, 640x360](chest-panel/chest-expedition-double-640x360.png) and
  [at 427x240](chest-panel/chest-expedition-double-427x240.png), the 240-row minimum: the six-row
  "Large Chest" texture under its slots, the panel level with the screen's bottom.
- [Basic, single, 427x240](chest-panel/chest-basic-single-427x240.png) and
  [Basic, double, 640x360](chest-panel/chest-basic-double-640x360.png): the one-row bag's panel,
  two mounts.
- [Expedition at 320x240](chest-panel/chest-expedition-single-320x240.png): under 360 wide, no
  panel; the vanilla chest screen at vanilla's place (x 72).
- [No bag, 640x360](chest-panel/chest-none-single-640x360.png): the vanilla chest screen, centred.
- [Shift-clicks through the live client](chest-panel/live-shift-clicks.png)
  ([before](chest-panel/chest-live-before.png), [after](chest-panel/chest-live-after.png)): the
  bread in bag cell 0 went into the chest's first empty slot; the chest's 23 coal went to the
  inventory, onto the hotbar's 28 (51), none to the bag.

Seen incidentally: when the fixture replaced the first chests their contents dropped at the
player's feet and were picked up on the real server; the iron ingots stacked onto the worn bag's
iron (5 to 23) and the wheat, apples, coal and planks, kinds the bag did not hold, went to the
hotbar (D-0029).

## Not verified

EMI's exclusion on the chest screen (EMI was not on the fixture's classpath this run; the rule is
the inventory screen's, which was photographed with EMI for 0.4.0). Rusty's ultrawide at GUI
scale 5.
