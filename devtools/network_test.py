"""Exercise Backpacks+ through two real clients and its dedicated server."""
from pathlib import Path
import json
import time
from collections.abc import Callable
from typing import cast
from network_control import command, obj, player, read

RESULTS: list[str] = []
OUT = Path(__file__).resolve().parents[1] / 'run/network-control'

def wait_for(predicate: Callable[[], bool], description: str, seconds: float = 20) -> None:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        try:
            if predicate():
                return
        except (KeyError, TypeError):
            pass
        time.sleep(0.15)
    raise AssertionError(description)

def check(value: bool, description: str) -> None:
    if not value:
        raise AssertionError(description)
    RESULTS.append(description)
    print('PASS', description, flush=True)

def item(value: object) -> str:
    result = obj(value)['item']
    assert isinstance(result, str)
    return result

def cell(role: str, index: int, synced: bool = False) -> dict[str, object]:
    cells = obj(player(role)['syncedBag' if synced else 'bag'])['cells']
    assert isinstance(cells, list)
    return obj(cells[index])

def slot(role: str, index: int) -> dict[str, object]:
    slots = player(role)['slots']
    assert isinstance(slots, list)
    return obj(cast(list[object], slots)[index])

def browse(target: int) -> None:
    command('driver', 'focus')
    command('driver', 'g', down=True)
    wait_for(lambda: read('driver').get('browsing') is True, 'G did not acquire wheel')
    selection = read('driver')['selection']
    assert isinstance(selection, int)
    steps = (target - selection) % 5
    if steps == 0:
        command('driver', 'scroll', delta=1)
        command('driver', 'scroll', delta=-1)
    else:
        for _ in range(steps):
            command('driver', 'scroll', delta=-1)
    command('driver', 'g', down=False)
    wait_for(lambda: read('driver').get('browsing') is False, 'G release did not finish')

def run() -> None:
    wait_for(lambda: len(obj(read('server')['players'])) == 2, 'Both clients did not join', 45)
    command('server', 'seed')
    command('server', 'position', target='QuickViewer', x=2, z=4)
    command('driver', 'focus')
    wait_for(lambda: item(cell('observer', 36, True)) == 'minecraft:diamond_pickaxe', 'Observer missing initial mount sync')
    check(item(cell('driver', 36, True)) == 'minecraft:diamond_pickaxe', 'Both clients receive the equipped backpack mounts')
    browse(1)
    wait_for(lambda: item(player('server')['held']) == 'minecraft:diamond_pickaxe', 'Direct G swap failed')
    check(item(cell('server', 36)) == 'minecraft:diamond_axe', 'Release G exchanges the selected mount directly with the actual hotbar item')
    check(item(player('server')['quick']) == 'minecraft:shears', 'Mount swap preserves the standalone Quick Slot')
    wait_for(lambda: item(cell('observer', 36, True)) == 'minecraft:diamond_axe', 'Observer missed changed mount')
    check(player('observer').get('action') == 'EXCHANGE', 'Other client receives the semantic EXCHANGE action')
    command('driver', 'g', down=True)
    command('driver', 'scroll', delta=-1)
    command('observer', 'focus')
    command('driver', 'g', down=False)
    time.sleep(0.4)
    check(item(player('server')['held']) == 'minecraft:diamond_pickaxe', 'Focus loss cancels a pending gear swap')
    command('driver', 'focus')
    command('server', 'held', item='minecraft:bread', count=5)
    browse(1)
    wait_for(lambda: item(player('server')['held']) == 'minecraft:diamond_axe', 'Fallback draw failed')
    check(item(cell('server', 0)) == 'minecraft:bread' and cell('server', 0)['count'] == 5, 'Incompatible hand stack is stored intact in ordinary storage')
    command('server', 'holdBag')
    wait_for(lambda: item(player('driver')['held']) == 'backpacksplus:expedition_backpack', 'Held bag not synced')
    command('driver', 'use')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu', 'Right-click did not open actual bag')
    command('driver', 'menuClick', slot=68, button=0, type='PICKUP')
    command('driver', 'menuClick', slot=1, button=0, type='PICKUP')
    wait_for(lambda: slot('server', 1)['count'] == 16, 'Furnace click insertion failed')
    check(item(slot('server', 1)) == 'minecraft:furnace', 'Real menu accepts ordinary furnaces')
    command('driver', 'menuClick', slot=69, button=0, type='PICKUP')
    command('driver', 'menuClick', slot=2, button=0, type='PICKUP')
    check(item(player('server')['cursor']) == 'minecraft:shulker_box', 'Real menu refuses shulker nesting without losing the cursor item')
    command('driver', 'menuClick', slot=69, button=0, type='PICKUP')
    command('driver', 'menuClick', slot=67, button=0, type='PICKUP')
    check(item(player('server')['held']) == 'backpacksplus:expedition_backpack', 'The opened backpack cannot be moved out of its source cell')
    command('driver', 'capture', name='backpacks-storage-menu.png')
    command('driver', 'close')
    command('server', 'wearBag')
    command('server', 'full')
    command('server', 'held', item='minecraft:apple', count=16)
    before = obj(player('server')['bag'])
    browse(2)
    time.sleep(0.3)
    check(obj(player('server')['bag']) == before and obj(player('server')['held'])['count'] == 16, 'Full storage refuses an incompatible mount exchange atomically')
    command('driver', 'scroll', delta=-1)
    wait_for(lambda: player('server')['selected'] == 1, 'Normal scroll did not select next hotbar cell')
    check(player('driver')['selected'] == 1, 'Ordinary scrolling still controls the nine-slot hotbar')
    command('driver', 'scroll', delta=1)
    wait_for(lambda: player('server')['selected'] == 0, 'Normal scroll did not return to slot zero')
    command('server', 'save')
    command('driver', 'disconnect')
    wait_for(lambda: len(obj(read('server')['players'])) == 1, 'Driver did not disconnect')
    command('driver', 'join')
    wait_for(lambda: item(cell('driver', 37, True)) == 'minecraft:diamond_sword', 'Relog lost mount state')
    wait_for(lambda: 'QuickDriver' in obj(read('server')['players']), 'Server report has not caught up with reconnect')
    check(obj(player('server')['bag']) == before, 'Relog/save-load preserves the exact bag identity, revision and contents')
    command('server', 'position', target='QuickViewer', x=512, z=0)
    wait_for(lambda: 'QuickDriver' not in obj(read('observer')['players']), 'Observer did not leave tracking range')
    command('server', 'position', target='QuickViewer', x=2, z=4)
    wait_for(lambda: item(cell('observer', 37, True)) == 'minecraft:diamond_sword', 'Retracking lost mount state')
    check(obj(player('observer')['syncedBag']) == before, 'Leaving and re-entering tracking range restores the full current snapshot')
    command('driver', 'focus')
    command('driver', 'hud', arm='RIGHT', attack='HOTBAR', scale=2)
    command('driver', 'capture', name='backpacks-gear-right.png')
    command('driver', 'hud', arm='LEFT', attack='HOTBAR', scale=3)
    command('driver', 'capture', name='backpacks-gear-left-compact.png')
    OUT.joinpath('network-results.json').write_text(json.dumps({'passed': RESULTS, 'server': read('server'), 'driver': read('driver'), 'observer': read('observer')}, indent=2))

try:
    run()
finally:
    for role in ('driver', 'observer', 'server'):
        try:
            command(role, 'quit')
        except (RuntimeError, TimeoutError):
            pass
