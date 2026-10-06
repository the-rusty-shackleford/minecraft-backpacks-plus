# Creative edits of the bag's cells under a real ping — 0.7.1, 2026-10-06

D-0036. Run with `devtools/creative_race.py 5 3 2 1` on the network fixture built with
`-PtestCurios`: `runNetworkServer` headless, `runNetworkDriver` on Xephyr with software rendering,
nothing focused. For the ping, the server's `run/network-server/server.properties` was moved to
port 25596 and a proxy on 25586 (where the driver connects) held every chunk 75 ms each way:
`devtools/lag_proxy.py 25586 25596 75`. The port goes back to 25586 afterwards.

Each period: `creativeRace` (a Reinforced bag in the Curios back slot: spider eye 7, bread 8,
cobblestone 64, torch 32, iron ingot 5, oak log 12, arrow 16, dirt 33 in cells 0 to 7; creative),
the creative screen on its inventory tab, `creativeBurst` of 32 left clicks (cell k to k+9 for the
eight stacks, then back), the screen closed, opened, closed; the server's count of every kind in
the bag and slots 1 to 45 against the start.

| clicks apart | 0.7.0 | revision | 0.7.1 | revision |
|---|---|---|---|---|
| 5 ticks | conserved | 1 → 33 | conserved | 1 → 33 |
| 3 ticks | arrow −16, bread −8, dirt −33, iron −5, spider eye −7, torch +64 | 1 → 96 | conserved | 1 → 33 |
| 2 ticks | conserved | 1 → 297 | conserved | 1 → 33 |
| 1 tick | all eight kinds gone, the bag empty | 1 → 166 | conserved | 1 → 33 |

Without the proxy, 0.7.0 conserved at all four periods (1 → 33 each).

## Not verified

Middle-click copies, right-click splits and drags through the panel, shift-clicks, and rounds
trips above 150 ms. The full pack (EMI's own clicks on the creative screen). A player on the box.
