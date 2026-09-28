# The worn bag's panel on the creative inventory, photographed and driven — 0.5.1, 2026-09-28

D-0031. Captured with `devtools/capture_creative.py` on the network fixture: `runNetworkServer`
headless, `runNetworkDriver` on the desktop display through `tools/booth/run_iconified.sh` (the
window was iconified the moment it mapped; Rusty's client was not running). Nothing sent focus:
the driver's `focus` op now refuses unless `-Dbackpacksplus.fixture.focus=true`.

## What the photographs show

- [Inventory tab, 640x360](creative-panel/creative-inventory-expedition-640x360.png): the
  Expedition panel (four mounts, bread in cell 0) left of the creative inventory, the pair
  centred (the window at x 312, vanilla's 222 plus half a panel); the player's own inventory
  clean, no cell over the hotbar row; the page buttons moved with the window.
- [Building Blocks tab, 640x360](creative-panel/creative-blocks-expedition-640x360.png): no
  panel, the window at vanilla's centred 222.
- [Inventory tab at 427x240](creative-panel/creative-inventory-expedition-427x240.png) (206) and
  [at 688x288](creative-panel/creative-inventory-expedition-688x288.png) (336), Rusty's width at
  GUI scale 5, with their category tabs centred at 116 and 246.
- [Before](creative-panel/creative-live-before.png) and
  [after](creative-panel/creative-live-after.png) moving stacks through the screen's own mouse
  handling: sixteen furnaces from hotbar slot 1 into bag cell 5; the bread from cell 0, clicked
  on the panel's title with the bread on the cursor, then onto hotbar slot 4. The server's state
  afterwards: cell 5 sixteen furnaces, cell 0 empty, hotbar 1 empty, hotbar 4 eight bread. The
  title click did not throw it.
- [Survival inventory](creative-panel/survival-inventory-regression-640x360.png) and
  [a chest](creative-panel/chest-regression-640x360.png) afterwards in survival: both screens
  as in 0.5.0 (their mixins gained the click guard), the furnaces still in cell 5.

## Not verified

The hidden case under 379 wide: the capture asked for 1134x720 at GUI scale 3 and the window came
up at scale 2 (567 wide, the screen at 276 and 186 as the rule gives there), so no photograph of
it; the rule is pinned by JUnit at 378 and 379. EMI's exclusion on the creative screen (EMI was not
on the fixture's classpath). Middle-click copies and drags through the panel were not driven, only
single clicks.
