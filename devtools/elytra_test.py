"""Real-client elytra gate on an isolated off-screen display.

Partitions: worn/held bag, elytra on/off, normal B menu and release-G swap,
local/remote snapshots and light, actual fall-flying input. Review captures separately.
"""
from pathlib import Path
import json
import time
from network_control import command, read, player, obj
from network_test import wait_for, browse

OUT = Path(__file__).parent / 'verification/elytra.json'
checks: list[str] = []


def note(text: str) -> None:
    checks.append(text)
    OUT.write_text(json.dumps({'checks': checks}, indent=2) + '\n')
    print(text, flush=True)


def capture(name: str) -> None:
    time.sleep(0.65)
    command('observer', 'capture', name='elytra-' + name + '.png')


def main() -> None:
    wait_for(lambda: len(obj(read('server')['players'])) == 2, 'Both clients joined', 90)
    command('server', 'life', action='setup', route='curios', keep=False, curse=False)
    command('server', 'mounts', items=['minecraft:diamond_sword', 'minecraft:diamond_pickaxe', 'minecraft:torch', 'minecraft:lantern'])
    command('server', 'quick', item='minecraft:apple')
    command('server', 'position', target='QuickViewer', x=2.5, z=-4)
    command('observer', 'hud', width=1280, height=720, arm='RIGHT', attack='HOTBAR', scale=2)
    command('observer', 'view', camera='FIRST_PERSON', fov=40, hideGui=True, yaw=32, pitch=10)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50, hideGui=False, yaw=0, pitch=0)
    command('driver', 'focus')
    command('driver', 'movement', forward=True)
    time.sleep(.5)
    command('driver', 'movement')
    command('server', 'position', target='QuickDriver', x=0, z=0)
    command('observer', 'focus')
    capture('before')
    before = obj(player('server')['bag'])
    command('server', 'curiosControl', mode='elytra', enabled=True)
    capture('hidden')
    for role in ('driver', 'observer'):
        wait_for(lambda: obj(player(role)['syncedBag']) == before, 'Bag remains synced with elytra')
        assert player(role)['bagVisible'] is True, 'Elytra must not mutate Curios visibility'
        light = obj(player(role)['light'])
        assert light['held'] == 0 and light['sources'] == [15] and light['worldBlockLight'] == 0
    note('Both clients retain exact bag contents and mounted light 15 with elytra; empty hand and world light 0')
    command('driver', 'focus')
    command('driver', 'b')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu', 'B opens hidden worn bag')
    capture('open-hidden')
    command('driver', 'close')
    browse(1)
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:diamond_sword', 'G draws hidden mount')
    wait_for(lambda: obj(player('observer')['held'])['item'] == 'minecraft:diamond_sword', 'Remote held sword arrived')
    capture('drawn')
    browse(1)
    wait_for(lambda: obj(player('server')['held'])['item'] == 'minecraft:air', 'G stows hidden mount')
    assert obj(player('server')['quick'])['item'] == 'minecraft:apple'
    note('Native B opens the bag and release-G draws/stows a mount with elytra; standalone Quick Slot unchanged')
    command('server', 'curiosControl', mode='elytra', enabled=False)
    capture('restored')
    note('Captured immediate worn-bag/mount restoration after removing elytra')
    command('server', 'curiosControl', mode='elytra', enabled=True)
    command('driver', 'focus')
    command('server', 'position', target='QuickDriver', x=0, y=82, z=0)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=60, hideGui=True, yaw=0, pitch=-15)
    command('driver', 'movement', jump=True)
    wait_for(lambda: player('server').get('fallFlying') is True, 'Native jump input starts fall flight', 5)
    command('driver', 'movement')
    assert player('driver').get('fallFlying') is True
    command('driver', 'capture', name='elytra-flight.png')
    note('Actual jump input starts server-confirmed fall flight; local third-person capture recorded')
    command('server', 'seed')
    command('server', 'holdBag')
    command('server', 'quick', item='minecraft:air')
    command('server', 'curiosControl', mode='elytra', enabled=True)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50, hideGui=False, yaw=0, pitch=0)
    command('server', 'position', target='QuickViewer', x=-4, z=-.5)
    command('observer', 'view', camera='FIRST_PERSON', fov=40, hideGui=True, yaw=-83, pitch=10)
    command('driver', 'focus')
    command('driver', 'movement', forward=True)
    time.sleep(.5)
    command('driver', 'movement')
    command('server', 'position', target='QuickDriver', x=0, z=0)
    capture('held')
    command('driver', 'use')
    wait_for(lambda: player('server')['menu'] == 'BackpackMenu', 'Right-click opens carried bag with elytra')
    capture('held-open')
    command('driver', 'close')
    note('Carried bag still opens through normal right-click with elytra; carry/open captures recorded')
    note('Runtime assertions passed; screenshots require visual inspection')


if __name__ == '__main__':
    main()
