"""Verify Quick Slot through the real Luminance backend and two dedicated-server clients.

Partitions: held/quick/mount combinations, hidden block items, dry/wet, H exchanges,
local/remote, definitions/reload, each enable switch, removal, relog and tracking reentry.
The existing Backpacks+ fixture supplies observations; no lighting implementation is mocked.
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
from lighting_test import expect, light, mounts, reload_data

ROOT = Path(__file__).parent


def record(name: str) -> None:
    """Effects: appends real backend evidence without storing personal profile data."""
    path = ROOT / 'verification/quickslot-lighting.json'
    checks = json.loads(path.read_text()) if path.exists() else []
    checks.append({'check': name, 'shaders': bool(read('observer').get('shaders')),
                   'driver': light('driver'), 'observer': light()})
    path.write_text(json.dumps(checks, indent=2) + '\n')
    print('PASS', name, flush=True)


def quick(item: str) -> None:
    command('server', 'quick', item=item)
    wait_for(lambda: all(obj(player(role)['quick'])['item'] == item
                        for role in ('driver', 'observer')))


def photo(name: str) -> None:
    time.sleep(.6)
    command('observer', 'capture', name=f'quick-light-{name}.png')


def setup() -> None:
    ready()
    for who in ('QuickDriver', 'QuickViewer'):
        command('server', 'seed', target=who)
        command('server', 'mounts', target=who, items=['minecraft:air'] * 4)
    command('server', 'darkRoom')
    command('server', 'position', target='QuickViewer', x=66.5, z=-4)
    command('observer', 'hud', width=1280, height=900, arm='RIGHT', attack='HOTBAR', scale=2)
    command('observer', 'view', camera='FIRST_PERSON', fov=50, hideGui=True, yaw=32, pitch=15)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50, hideGui=True, yaw=0, pitch=0)
    expect(0)
    photo('empty')


def basics() -> None:
    setup()
    for item, level in (('torch', 14), ('soul_torch', 10), ('lantern', 15),
                        ('soul_lantern', 10), ('glowstone', 15)):
        quick('minecraft:air')
        command('server', 'held', item=f'minecraft:{item}')
        expect(level)
        field = light()['field']
        photo(f'held-{item}')
        command('server', 'held', item='minecraft:air')
        quick(f'minecraft:{item}')
        expect(level)
        wait_for(lambda: light()['field'] == field)
        photo(f'slotted-{item}')
        record(f'{item}: owner and observer Quick Slot light equals held brightness and terrain field')
    quick('minecraft:torch')
    command('driver', 'h')
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:torch')
    expect(14)
    assert light()['sources'] == [14]
    command('driver', 'h')
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:air')
    expect(14)
    assert light()['sources'] == [14]
    record('Real H draw/stow keeps brightness with no duplicate hand/Quick Slot source')
    command('server', 'held', item='minecraft:lantern')
    expect(15)
    assert light()['sources'] == [15]
    record('Brighter held lantern wins over the quick-slotted torch')
    mounts('minecraft:lantern')
    expect(15)
    field = light()['field']
    quick('minecraft:torch')
    expect(15)
    wait_for(lambda: light()['field'] == field)
    record('Backpack lantern plus Quick Slot torch uses the same maximum terrain brightness')
    mounts()
    quick('minecraft:torch')
    expect(14)
    quick('minecraft:air')
    expect(0)
    assert all(value == 0 for value in cast(list[int], light()['field']))
    photo('removed')
    record('Removing the Quick Slot item clears local/remote light and leaves real world light unchanged')


def rules() -> None:
    quick('minecraft:torch')
    expect(14)
    for role in ('driver', 'observer'):
        command(role, 'lightConfig', quick=False)
    expect(0)
    for role in ('driver', 'observer'):
        command(role, 'lightConfig', quick=True, held=False)
    expect(0)
    for role in ('driver', 'observer'):
        command(role, 'lightConfig', held=True, enabled=False)
    wait_for(lambda: all(value == 0 for value in cast(list[int], light()['field'])))
    for role in ('driver', 'observer'):
        command(role, 'lightConfig', enabled=True)
    expect(14)
    record('Quick Slot switch and Luminance held/master switches control the actual terrain light')
    command('server', 'water', wet=True)
    wait_for(lambda: bool(light()['underwater']) and bool(light('driver')['underwater']))
    quick('minecraft:torch')
    expect(0)
    quick('minecraft:soul_torch')
    expect(0)
    quick('minecraft:lantern')
    expect(15)
    record('Underwater torch/soul torch extinguish; lantern stays lit by the backend rules')
    command('server', 'water', wet=False)
    wait_for(lambda: not light()['underwater'] and not light('driver')['underwater'])
    quick('minecraft:torch')
    expect(14)
    reload_data(True)
    expect(9)
    record('Reloaded Luminance JSON changes the unchanged torch cache to brightness 9')
    quick('farmersdelight:apple_cider')
    expect(11)
    record('Modded drink uses the backend resource-defined item luminance')
    reload_data(False)
    expect(0)
    record('Removing its definition immediately clears an unchanged Quick Slot stack')


def persistence() -> None:
    quick('minecraft:torch')
    expect(14)
    command('server', 'save')
    command('driver', 'disconnect')
    wait_for(lambda: not read('driver')['connected'])
    command('driver', 'join')
    wait_for(lambda: bool(read('driver')['connected']) and 'QuickDriver' in obj(read('observer')['players']))
    expect(14)
    command('server', 'position', target='QuickViewer', x=512, z=0)
    wait_for(lambda: 'QuickDriver' not in obj(read('observer')['players']))
    command('server', 'position', target='QuickViewer', x=66.5, z=-4)
    wait_for(lambda: 'QuickDriver' in obj(read('observer')['players']))
    expect(14)
    record('Save/relog and tracking reentry restore the remote Quick Slot light')


if __name__ == '__main__':
    {'basics': basics, 'rules': rules, 'persistence': persistence}[sys.argv[1]]()
