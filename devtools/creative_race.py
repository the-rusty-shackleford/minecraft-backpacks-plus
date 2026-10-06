"""Edit the worn bag's cells on the creative screen as a quick hand does, and count what is left.

Bobandy_'s Reinforced bag, 2026-10-06: emptied and refilled with spider eyes during three minutes
in creative, its revision up 126. The creative screen is client-authoritative: after every click
and whenever it is laid out it sends the server each slot the client sees changed, and the client
sees the bag's cells through its copy of the bag, which every sync of the bag replaces whole.

Requires `runNetworkServer` and `runNetworkDriver` (with -PtestCurios for the back slot), joined
through `lag_proxy.py` for a player's ping: on localhost alone the answer to a click always lands
before the next, and 0.7.0 passed (D-0036). For each
period, the bag is reset to eight kinds of one stack each (`creativeRace`), the eight stacks are
moved one cell along and back with clicks that many client ticks apart (`creativeBurst`), the
screen is closed, opened and closed again, and the server's count of each kind is compared with
the start. A conserving bag shows no difference at any period.
"""
import collections
import sys
import time

from capture_inventory import ready, wait_for
from network_control import command, obj, read

PLAYER = 'QuickDriver'
PERIODS = tuple(int(p) for p in sys.argv[1:]) or (5, 3, 2, 1)

def server_player() -> dict[str, object]:
    return obj(obj(read('server').get('players', {})).get(PLAYER, {}))

def tally() -> tuple[collections.Counter[str], object]:
    """The server's count of each kind in the bag and the vanilla slots 1 to 45, and the bag's revision."""
    p = server_player()
    bag = obj(p.get('bag', {}))
    counts: collections.Counter[str] = collections.Counter()
    stacks = list(bag.get('cells', [])) + list(p.get('slots', []))[1:46]
    for stack in stacks:
        stack = obj(stack)
        if stack['item'] != 'minecraft:air':
            counts[str(stack['item'])] += int(stack['count'])
    return counts, bag.get('revision')

def open_inventory_tab() -> None:
    command('driver', 'inventory')
    wait_for(lambda: read('driver').get('screen') == 'CreativeModeInventoryScreen', 'The creative screen did not open')
    command('driver', 'creativeTab', tab='minecraft:inventory')
    time.sleep(.5)

def close() -> None:
    command('driver', 'close')
    wait_for(lambda: read('driver').get('screen') == '', 'The creative screen did not close')

def run(period: int) -> bool:
    command('server', 'creativeRace')
    time.sleep(1.5)
    before, rev0 = tally()
    open_inventory_tab()
    clicks = []
    for k in range(8):
        clicks += [{'bagCell': k}, {'bagCell': k + 9}]
    for k in range(8):
        clicks += [{'bagCell': k + 9}, {'bagCell': k}]
    command('driver', 'creativeBurst', clicks=clicks, period=period)
    wait_for(lambda: read('driver').get('burstRemaining') == 0, 'The burst did not finish', seconds=60)
    time.sleep(1.5)
    after_burst, rev1 = tally()
    close(); time.sleep(1); open_inventory_tab(); time.sleep(1); close(); time.sleep(1.5)
    after, rev2 = tally()
    def diff(a: collections.Counter[str], b: collections.Counter[str]) -> dict[str, int]:
        return {k: b[k] - a[k] for k in sorted(set(a) | set(b)) if b[k] != a[k]}
    print(f'period {period} ticks: {len(clicks)} clicks; revision {rev0} -> {rev1} (burst) -> {rev2} (reopen)', flush=True)
    print(f'  after the burst : {diff(before, after_burst) or "conserved"}', flush=True)
    print(f'  after reopening : {diff(before, after) or "conserved"}', flush=True)
    print(f'  bag cells now   : {[(i, c["item"].split(":")[1], c["count"]) for i, c in enumerate(obj(server_player().get("bag", {})).get("cells", [])) if c["item"] != "minecraft:air"]}', flush=True)
    return not diff(before, after)

if __name__ == '__main__':
    ready()
    results = {period: run(period) for period in PERIODS}
    command('server', 'gamemode', mode='survival')
    print('conserved at every period' if all(results.values()) else f'NOT conserved: {[p for p, ok in results.items() if not ok]}', flush=True)
