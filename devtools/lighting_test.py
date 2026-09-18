"""Real-client Luminance gate, partitioned into short resumable phases.

Partitions: held/mounted/ordinary storage/pocketed/hand-carried; one/multiple sources;
dry/wet; vanilla/data-defined mod item; enabled/disabled/reloaded; local/remote/retracked.
Requires the muted existing server and two clients. This uses actual backend sources,
published terrain light fields, menu-click packets and G input; it never fakes lighting.
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
MODE = 'plain'

def light(role: str = 'observer') -> dict[str, object]:
    return obj(player(role)['light'])

def level(role: str = 'observer') -> int:
    values = light(role)['sources']
    assert isinstance(values, list)
    return max(cast(list[int], values), default=0)

def expect(value: int) -> None:
    wait_for(lambda: all(level(role) == value for role in ('driver', 'observer')))
    # Allow the bounded engine sample and terrain rebuild to settle for photographs.
    time.sleep(.5)
    assert all(light(role)['worldBlockLight'] == 0 for role in ('driver', 'observer'))

def record(name: str) -> None:
    path = ROOT / f'verification/lighting-{MODE}.json'
    values = json.loads(path.read_text()) if path.exists() else []
    values.append({'check': name, 'shaders': bool(read('observer').get('shaders')),
                   'driver': light('driver'), 'observer': light()})
    path.write_text(json.dumps(values, indent=2) + '\n')
    print('PASS', name, flush=True)

def mounts(item: str = 'minecraft:air', second: str = 'minecraft:air') -> None:
    command('server', 'mounts', items=['minecraft:air', 'minecraft:air', item, second])

def photo(name: str) -> None:
    time.sleep(.8)
    command('observer', 'capture', name=f'light-{MODE}-{name}.png')

def setup() -> None:
    ready()
    command('server', 'seed', target='QuickViewer')
    command('server', 'mounts', target='QuickViewer', items=['minecraft:air'] * 4)
    command('server', 'seed')
    command('server', 'darkRoom')
    command('server', 'position', target='QuickViewer', x=66.5, z=-4)
    command('observer', 'hud', width=1280, height=900, arm='RIGHT', attack='HOTBAR', scale=2)
    command('observer', 'view', camera='FIRST_PERSON', fov=50, hideGui=True, yaw=32, pitch=15)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50, hideGui=True, yaw=0, pitch=0)
    mounts()
    expect(0)
    photo('empty')

def basics() -> None:
    setup()
    items = (('torch', 14), ('soul_torch', 10), ('lantern', 15), ('soul_lantern', 10))
    for item, expected in items:
        mounts()
        command('server', 'held', item=f'minecraft:{item}')
        expect(expected)
        field = light()['field']
        photo(f'held-{item}')
        mounts(f'minecraft:{item}')
        expect(expected)
        wait_for(lambda: light()['field'] == field)
        photo(f'mounted-{item}')
        record(f'{item}: local and remote mounted light equals held brightness and terrain field')
    mounts('minecraft:torch', 'minecraft:lantern')
    command('server', 'held', item='minecraft:lantern')
    expect(15)
    assert light()['sources'] == [15]
    record('Held and mounted lights take the maximum; equal sources are not duplicated')
    mounts('minecraft:torch')
    expect(14)
    browse(3)
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:torch')
    expect(14)
    record('Real G draw preserves brightness')
    browse(3)
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:air')
    expect(14)
    record('Real G stow restores the mounted source')
    command('driver', 'b')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu')
    command('driver', 'menuClick', slot=38, button=0, type='PICKUP')
    command('driver', 'menuClick', slot=2, button=0, type='PICKUP')
    command('driver', 'close')
    expect(0)
    assert all(value == 0 for value in cast(list[int], light()['field']))
    photo('removed')
    record('Moving the torch into ordinary storage through the real menu clears all light')

def carrying() -> None:
    mounts('minecraft:torch')
    expect(14)
    command('server', 'pocketBag')
    expect(0)
    record('A backpack inside player inventory emits no mounted light')
    command('server', 'unPocketBag')
    expect(14)
    command('server', 'holdBag')
    expect(14)
    record('A bag carried by its handle retains mounted light for both clients')
    command('server', 'wearBag')
    expect(14)
    for role in ('driver', 'observer'):
        command(role, 'lightConfig', held=False)
    expect(0)
    record('Luminance held-items setting disables mounted light')
    for role in ('driver', 'observer'):
        command(role, 'lightConfig', held=True, enabled=False)
    wait_for(lambda: all(value == 0 for value in cast(list[int], light()['field'])))
    record('Luminance master switch clears the terrain light field')
    for role in ('driver', 'observer'):
        command(role, 'lightConfig', enabled=True)
    expect(14)
    command('server', 'water', wet=True)
    wait_for(lambda: bool(light()['underwater']) and bool(light('driver')['underwater']))
    expect(0)
    record('Mounted torch goes out underwater according to Luminance data')
    mounts('minecraft:soul_torch')
    expect(0)
    mounts('minecraft:lantern')
    expect(15)
    record('Mounted lantern remains lit underwater, matching the backend rule')
    command('server', 'water', wet=False)
    wait_for(lambda: not light()['underwater'] and not light('driver')['underwater'])

def reload_data(enabled: bool) -> None:
    for role in ('driver', 'observer'):
        command(role, 'lightData', enabled=enabled)
    time.sleep(2)
    wait_for(lambda: not read('driver')['reloading'] and not read('observer')['reloading'])

def data_and_sync() -> None:
    reload_data(True)
    mounts('minecraft:torch')
    expect(9)
    record('Resource reload replaces cached torch luminance with the backend JSON definition')
    mounts('farmersdelight:apple_cider')
    expect(11)
    record('A real modded item follows its Luminance resource definition')
    # Keep the same equipped stack while removing definitions: this must invalidate cache.
    reload_data(False)
    expect(0)
    record('Removing the resource definition clears the unchanged modded-item mount')
    mounts('minecraft:torch')
    expect(14)
    command('server', 'save')
    command('driver', 'disconnect')
    wait_for(lambda: not read('driver')['connected'])
    command('driver', 'join')
    wait_for(lambda: bool(read('driver')['connected']) and 'QuickDriver' in obj(read('observer')['players']))
    expect(14)
    record('Relog restores the current mount light on both clients')
    command('server', 'position', target='QuickViewer', x=512, z=0)
    wait_for(lambda: 'QuickDriver' not in obj(read('observer')['players']))
    command('server', 'position', target='QuickViewer', x=66.5, z=-4)
    wait_for(lambda: 'QuickDriver' in obj(read('observer')['players']))
    expect(14)
    record('Leaving and returning to tracking range restores the remote light')

if __name__ == '__main__':
    phase = sys.argv[1]
    if len(sys.argv) > 2:
        MODE = sys.argv[2]
    {'basics': basics, 'carrying': carrying, 'data': data_and_sync}[phase]()
