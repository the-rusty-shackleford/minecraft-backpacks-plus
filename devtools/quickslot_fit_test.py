"""Capture the approved companion fit using two actual clients on an isolated display.

Partitions: visible/hidden Curios, chest/held/inventory-only bag, back/hip/bowl items,
elytra, both skin widths/main arms and resource reload. Focus only the off-screen X server.
"""
from pathlib import Path
import json
import time
from network_control import command, read, player, obj
from lifecycle_test import wait_for

OUT = Path(__file__).resolve().parent / 'verification/quickslot-fit.json'
checks: list[str] = []


def note(text: str) -> None:
    checks.append(text)
    OUT.write_text(json.dumps({'checks': checks}, indent=2) + '\n')
    print(text, flush=True)


def capture(name: str) -> None:
    time.sleep(0.8)
    command('observer', 'capture', name='quick-fit-' + name + '.png')


def quick(item: str) -> None:
    command('server', 'quick', item='minecraft:' + item)
    wait_for(lambda: obj(player('observer')['quick']).get('item') == 'minecraft:' + item, 'observer received Quick Slot stack')


def toggle() -> None:
    command('driver', 'focus')
    command('driver', 'curios')
    time.sleep(0.3)
    command('driver', 'curiosToggle')
    command('driver', 'close')
    command('observer', 'focus')


def main() -> None:
    command('server', 'life', action='setup', route='curios', keep=False, curse=False)
    command('server', 'mounts', items=['minecraft:air'] * 4)
    command('server', 'position', target='QuickViewer', x=-4, z=-0.5)
    command('observer', 'view', camera='FIRST_PERSON', fov=40, hideGui=True, yaw=-83, pitch=10)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=40, hideGui=True, yaw=0, pitch=0)
    command('observer', 'focus')
    for item in ('apple', 'lantern', 'potion', 'shears', 'diamond_sword', 'mushroom_stew'):
        quick(item)
        capture('curios-' + item)
    note('Captured small hip items, hidden sword and permanently hidden bowl with visible Curios bag')
    quick('apple')
    command('server', 'curiosControl', mode='armor', enabled=True)
    capture('armor-apple')
    command('driver', 'focus')
    command('driver', 'movement', crouch=True)
    time.sleep(0.5)
    command('observer', 'capture', name='quick-fit-crouch-apple.png')
    command('driver', 'movement')
    command('observer', 'focus')
    note('Captured chestplate and actual crouch input without changing the approved item scale')
    command('server', 'curiosControl', mode='elytra', enabled=True)
    for item in ('apple', 'lantern'):
        quick(item)
        capture('elytra-' + item)
    light = obj(player('observer')['light'])
    note('Captured hidden Quick Slot items with elytra; observer light report: ' + json.dumps(light, sort_keys=True))
    command('server', 'curiosControl', mode='elytra', enabled=False)
    quick('diamond_sword')
    toggle()
    wait_for(lambda: player('observer').get('bagVisible') is False, 'actual Curios visibility toggle')
    capture('hidden-bag-sword')
    quick('apple')
    capture('hidden-bag-apple')
    toggle()
    wait_for(lambda: player('observer').get('bagVisible') is True, 'actual Curios visibility restored')
    capture('reload-before')
    command('observer', 'placementData', enabled=True)
    wait_for(lambda: read('observer').get('reloading') is False, 'placement resource reload', 90)
    capture('reload-override')
    toggle()
    capture('override-hidden-bag')
    toggle()
    command('observer', 'placementData', enabled=False)
    wait_for(lambda: read('observer').get('reloading') is False, 'placement resource removal', 90)
    capture('reload-restored')
    note('Captured actual visibility toggle and with_backpack override enable/disable through resource reload')
    command('server', 'seed')
    command('server', 'mounts', items=['minecraft:air'] * 4)
    quick('diamond_sword')
    capture('chest-sword-hidden')
    command('server', 'pocketBag')
    capture('inventory-only-sword')
    command('server', 'unPocketBag')
    command('server', 'holdBag')
    capture('held-bag-sword')
    note('Captured chest, inventory-only and carried bag: only a visibly worn bag covers the back')
    command('server', 'wearBag')
    command('server', 'mounts', items=['minecraft:diamond_sword', 'minecraft:diamond_pickaxe', 'minecraft:torch', 'minecraft:lantern'])
    quick('apple')
    capture('independent-mounts')
    before = obj(player('server')['bag']).copy()
    command('driver', 'focus')
    command('driver', 'h')
    wait_for(lambda: obj(player('server')['quick']).get('item') == 'minecraft:air', 'native H drew apple')
    assert obj(player('server')['bag']) == before
    command('driver', 'h')
    wait_for(lambda: obj(player('server')['quick']).get('item') == 'minecraft:apple', 'native H stowed apple')
    assert obj(player('server')['bag']) == before
    note('Native H draw/stow leaves all four backpack mounts, bag identity and revision untouched')
    note('Capture run complete; screenshots require visual review')


if __name__ == '__main__':
    main()
