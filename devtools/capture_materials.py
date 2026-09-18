"""Exercise real client crafting and capture the approved materials in third person.

Requires the existing muted server, actor and observer profiles. This only seeds recipe
ingredients; outputs are taken through the real client menu/network path. Shutdown is
explicit so an interrupted capture can be inspected before closing its clients.
"""
# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
from collections.abc import Callable
from pathlib import Path
import json
import time
from typing import cast
from capture_mounts import ready, resources
from network_control import command, obj, player, read

TIERS = ('basic', 'reinforced', 'expedition')
DYES = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
        'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')
EVIDENCE: list[dict[str, object]] = []

def checkpoint(checks: list[str]) -> None:
    """Effects: retains completed real-client evidence even if a user closes a client."""
    evidence = {'checks': checks, 'crafts': EVIDENCE}
    (Path(__file__).parent / 'verification/crafting-network.json').write_text(json.dumps(evidence, indent=2) + '\n')

def wait_for(check: Callable[[], bool]) -> None:
    deadline = time.monotonic() + 20
    while time.monotonic() < deadline:
        if check():
            return
        time.sleep(.15)
    raise TimeoutError('Client/server condition not reached')

def slots(role: str) -> list[object]:
    value = player(role)['slots']
    assert isinstance(value, list)
    return cast(list[object], value)

def bag(role: str) -> dict[str, object]:
    tracked = obj(read(role).get('players', {}))
    actor = obj(tracked.get('QuickDriver', {}))
    return obj(actor.get('bag' if role == 'server' else 'syncedBag', {}))

def craft(recipe: str, dye: str, tier: str, screenshot: bool = False) -> dict[str, object]:
    """Effects: uses actual result pickup and hotbar placement, then wears that output."""
    command('server', 'craftSetup', recipe=recipe, dye=dye)
    expected = f'backpacksplus:{tier}_backpack'
    wait_for(lambda: player('driver')['menu'] == 'CraftingMenu'
             and obj(slots('driver')[0]).get('item') == expected)
    if screenshot:
        command('driver', 'capture', name=f'material-recipe-{tier}.png')
    command('driver', 'menuClick', slot=0, button=0, type='PICKUP')
    wait_for(lambda: obj(player('server')['cursor']).get('item') == expected)
    command('driver', 'menuClick', slot=37, button=0, type='PICKUP')
    wait_for(lambda: obj(player('server')['held']).get('item') == expected)
    command('driver', 'close')
    wait_for(lambda: player('server')['menu'] == 'InventoryMenu')
    command('server', 'wearBag')
    wait_for(lambda: bag('server').get('item') == expected and bag('observer') == bag('server'))
    result = bag('server')
    EVIDENCE.append({'recipe': recipe, 'dye': dye, 'bag': result})
    checkpoint(['real result clicks', 'remote color and contents synchronization'])
    return result

def capture(tier: str, color: str) -> None:
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50, hideGui=True, yaw=0, pitch=0)
    command('server', 'position', target='QuickViewer', x=2.5, z=-4)
    time.sleep(.5)
    command('observer', 'view', camera='FIRST_PERSON', fov=35, hideGui=True, yaw=32, pitch=10)
    time.sleep(.8)
    command('observer', 'capture', name=f'material-{tier}-{color}.png')

def retained(old: dict[str, object], new: dict[str, object], stage: int) -> None:
    assert old['id'] == new['id'] and old.get('name') == new.get('name')
    before = old['cells']; after = new['cells']
    assert isinstance(before, list) and isinstance(after, list)
    storage = (9, 18)[stage]; target = (18, 36)[stage]
    assert before[:storage] == after[:storage]
    assert before[storage] == after[target]
    if stage == 0:
        assert before[10] == after[20]
        assert obj(after[19])['item'] == 'minecraft:air'
    else:
        assert before[19:21] == after[37:39]
        assert obj(after[39])['item'] == 'minecraft:air'

def run() -> None:
    ready()
    command('observer', 'hud', width=1280, height=900, arm='RIGHT', attack='HOTBAR', scale=2)
    command('driver', 'hud', width=1280, height=900, arm='RIGHT', attack='HOTBAR', scale=2)
    command('server', 'position', target='QuickViewer', x=4, z=-4)
    command('server', 'seed', target='QuickDriver')
    resources(True)
    old: dict[str, object] = {}
    for stage, tier in enumerate(TIERS):
        result = craft(tier, 'none', tier)
        if stage:
            retained(old, result, stage - 1)
        # Hide mounts for the material comparison without altering stored items.
        command('server', 'mounts', items=['minecraft:air'] * (stage + 2))
        capture(tier, 'natural')
        command('server', 'craftFill')
        wait_for(lambda: bag('observer') == bag('server'))
        old = bag('server')
    print('Full-bag upgrade chain retained storage, mounts, UUID and name through real crafting clicks.', flush=True)
    for color in ('red', 'blue', 'green'):
        for tier in TIERS:
            result = craft(tier, color if tier == 'basic' else 'none', tier, screenshot=color == 'red')
            assert 'color' in result
            capture(tier, color)
        print(f'{color} canvas captured at all three tiers, with inherited color.', flush=True)
    command('server', 'craftFill')
    old = bag('server')
    for dye in DYES:
        result = craft('recolor', dye, 'expedition')
        assert result['id'] == old['id'] and result['cells'] == old['cells'] and result['name'] == old['name']
    before = bag('server')
    command('server', 'save')
    command('driver', 'disconnect')
    wait_for(lambda: not read('driver').get('connected'))
    command('driver', 'join')
    wait_for(lambda: bool(read('driver').get('connected')))
    wait_for(lambda: bag('observer') == before and bag('server') == before)
    command('server', 'position', target='QuickViewer', x=250, z=0)
    wait_for(lambda: not bag('observer'))
    command('server', 'position', target='QuickViewer', x=2.5, z=-4)
    wait_for(lambda: bag('observer') == before)
    checkpoint(['real result clicks', 'full storage and mount upgrades', 'all sixteen recolors',
                'remote color and contents synchronization', 'save/relog', 'tracking range leave and return'])
    print('All 16 recolors preserve the full bag; relog and tracking return match the server.', flush=True)

if __name__ == '__main__':
    run()
