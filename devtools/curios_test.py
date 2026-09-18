"""Real Curios 9.5.1 gate: native menu clicks, input, tracked renders and storage.

Partitions: all tiers; Curios/chest priority; visible/hidden; current/removed/replaced
source; armor/no armor; local/remote; relog/retracking. Uses the actual API and packets.
"""
# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import json
import sys
import time
from typing import cast
from capture_mounts import ready
from capture_materials import wait_for
from network_control import command, obj, player, read
from network_test import browse

ROOT = Path(__file__).parent

def bag(role: str = 'server') -> dict[str, object]:
    return obj(player(role)['bag' if role == 'server' else 'syncedBag'])

def array(value: object) -> list[object]:
    assert isinstance(value, list)
    return cast(list[object], value)

def record(name: str) -> None:
    path = ROOT / 'verification/curios-network.json'
    data = json.loads(path.read_text()) if path.exists() else []
    data.append({'check': name, 'server': player('server'), 'observer': player('observer')})
    path.write_text(json.dumps(data, indent=2) + '\n')
    print('PASS', name, flush=True)

def equip(tier: str) -> None:
    command('server', 'curiosPrepare', tier=tier)
    command('driver', 'curios')
    wait_for(lambda: 'backMenuSlot' in read('driver'))
    slot = read('driver')['backMenuSlot']
    assert isinstance(slot, int)
    command('driver', 'menuClick', slot=36, button=0, type='PICKUP')
    wait_for(lambda: obj(player('server')['cursor'])['item'] == f'backpacksplus:{tier}_backpack')
    command('driver', 'menuClick', slot=slot, button=0, type='PICKUP')
    wait_for(lambda: player('server')['source'] == 41 and bag().get('item') == f'backpacksplus:{tier}_backpack')
    wait_for(lambda: bag('observer') == bag())
    command('driver', 'close')

def photo(name: str) -> None:
    time.sleep(.7)
    command('observer', 'capture', name=f'curios-{name}.png')

def tiers() -> None:
    ready()
    for name in ('QuickDriver', 'QuickViewer'):
        command('server', 'seed', target=name)
    command('server', 'mounts', target='QuickViewer', items=['minecraft:air'] * 4)
    command('server', 'position', target='QuickViewer', x=2.5, z=-4)
    command('observer', 'hud', width=1280, height=900, arm='RIGHT', attack='HOTBAR', scale=2)
    command('observer', 'view', camera='FIRST_PERSON', fov=35, hideGui=True, yaw=32, pitch=10)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50, hideGui=False, yaw=0, pitch=0)
    for tier in ('basic', 'reinforced', 'expedition'):
        equip(tier)
        assert obj(obj(player('server')['curios'])['chest'])['item'] == 'minecraft:iron_chestplate'
        assert player('observer')['bagVisible'] is True
        photo(tier + '-armor')
        record(f'{tier}: real Curios clicks equip beside chest armor; remote bag and contents match')

def behavior() -> None:
    before = obj(player('server')['quick'])
    browse(1)
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:diamond_sword')
    assert obj(player('server')['quick']) == before
    browse(1)
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:air')
    record('G draws and stows the Curios mount; original Quick Slot remains independent')
    command('driver', 'b')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu')
    command('driver', 'menuClick', slot=0, button=0, type='QUICK_MOVE')
    wait_for(lambda: obj(array(bag()['cells'])[0])['item'] == 'minecraft:air')
    command('driver', 'close')
    record('B and real shift-click use the Curios bag storage')
    command('driver', 'b')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu')
    command('server', 'curiosControl', mode='replace')
    wait_for(lambda: player('server')['menu'] == 'InventoryMenu')
    record('Replacing the equipped source automatically closes the stale menu')
    command('server', 'curiosControl', mode='dual')
    wait_for(lambda: player('server')['source'] == 41)
    assert bag()['item'] == 'backpacksplus:expedition_backpack'
    command('driver', 'b')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu')
    assert len(array(player('server')['slots'])) == 76
    command('driver', 'close')
    record('Curios Expedition bag takes priority over a Basic chest-slot bag')

def visibility() -> None:
    command('server', 'curiosControl', mode='armor', enabled=True)
    command('driver', 'curios')
    wait_for(lambda: 'backMenuSlot' in read('driver'))
    command('driver', 'capture', name='curios-inventory.png')
    command('driver', 'curiosToggle')
    wait_for(lambda: player('observer')['bagVisible'] is False and player('driver')['bagVisible'] is False)
    command('driver', 'close')
    photo('hidden')
    assert bag()['item'] == 'backpacksplus:expedition_backpack'
    assert obj(player('observer')['light'])['sources'] == [14]
    command('driver', 'b')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu')
    photo('hidden-open')
    command('driver', 'close')
    record('Native render toggle hides bag and mounts remotely; storage and mounted torch light remain functional')
    command('driver', 'curios')
    wait_for(lambda: 'backMenuSlot' in read('driver'))
    command('driver', 'curiosToggle')
    wait_for(lambda: player('observer')['bagVisible'] is True)
    command('driver', 'close')
    command('server', 'curiosControl', mode='armor', enabled=False)
    photo('visible-no-armor')
    record('Native render toggle restores the bag; no armor and chest armor both render')
    command('driver', 'b')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu')
    command('driver', 'menuClick', slot=39, button=0, type='PICKUP')
    command('driver', 'menuClick', slot=2, button=0, type='PICKUP')
    command('driver', 'close')
    wait_for(lambda: obj(player('observer')['light'])['sources'] == [])
    record('Moving a Curios-mounted torch into ordinary storage clears its remote light')

def tracking() -> None:
    saved = bag()
    command('server', 'save')
    command('driver', 'disconnect')
    deadline = time.monotonic() + 60
    while 'QuickDriver' in obj(read('server').get('players', {})):
        if time.monotonic() > deadline:
            raise TimeoutError('Driver did not leave')
        time.sleep(.2)
    command('driver', 'join')
    wait_for(lambda: bool(read('driver')['connected']))
    wait_for(lambda: 'QuickDriver' in obj(read('observer')['players']) and bag('observer') == saved)
    assert bag() == saved
    record('Relog preserves the exact Curios bag identity, contents and mount state')
    command('server', 'position', target='QuickViewer', x=512, z=0)
    wait_for(lambda: 'QuickDriver' not in obj(read('observer')['players']))
    command('server', 'position', target='QuickViewer', x=2.5, z=-4)
    wait_for(lambda: 'QuickDriver' in obj(read('observer')['players']) and bag('observer') == saved)
    record('Leaving and re-entering tracking range restores the complete Curios bag snapshot')

if __name__ == '__main__':
    {'tiers': tiers, 'behavior': behavior, 'visibility': visibility, 'tracking': tracking}[sys.argv[1]]()
