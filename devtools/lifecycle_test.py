"""Actual two-client lifecycle test; no substitute inventory/network/storage backend.

Partitions: chest/Curios, drop/retain and Vanishing; full component preservation,
open-menu death, Nether/End travel, relog and tracking reentry.
"""
from pathlib import Path
import json
import time
from collections.abc import Callable
from network_control import command, read, obj, player

RESULT = Path(__file__).resolve().parent / 'verification/lifecycle.json'
checks: list[str] = []


def wait_for(test: Callable[[], bool], label: str, seconds: int = 60) -> None:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        try:
            if test():
                return
        except (KeyError, TypeError):
            pass
        time.sleep(0.1)
    raise AssertionError(label)


def life() -> dict[str, object]:
    return obj(read('server')['lifecycle'])


def note(label: str) -> None:
    checks.append(label)
    RESULT.parent.mkdir(parents=True, exist_ok=True)
    RESULT.write_text(json.dumps({'checks': checks}, indent=2) + '\n')
    print(label, flush=True)


def visible_matches(retained: bool) -> bool:
    for role in ('driver', 'observer'):
        view = player(role)
        bag = obj(view.get('syncedBag', {}))
        if bag.get('item') != ('backpacksplus:expedition_backpack' if retained else 'minecraft:air'):
            return False
        if retained:
            expected = obj(player('server')['bag'])
            if any(bag.get(key) != expected.get(key) for key in ('id', 'revision', 'cells', 'color', 'name')):
                return False
    return True


def setup(route: str, keep: bool = False, curse: bool = False) -> None:
    command('server', 'life', action='setup', route=route, keep=keep, curse=curse)
    command('server', 'life', target='QuickViewer', action='travel', dimension='minecraft:overworld', x=6)
    wait_for(lambda: life().get('bagIntact') is True and life().get('quickIntact') is True and visible_matches(True), 'seed synced complete bag')


def main() -> None:
    wait_for(lambda: len(obj(read('server').get('players', {}))) == 2, 'two clients connected', 120)
    if life().get('alive') is False:
        command('driver', 'respawn')
    for route in ('chest', 'curios'):
        for keep, curse in ((False, False), (False, True), (True, False), (True, True)):
            setup(route, keep, curse)
            if route == 'curios' and not keep and not curse:
                command('driver', 'focus')
                command('driver', 'b')
                wait_for(lambda: player('server').get('menu') == 'BackpackMenu', 'open bag before death')
            command('server', 'life', action='kill')
            wait_for(lambda: life().get('alive') is False, 'actual death')
            expected = 0 if keep or curse else 1
            state = life()
            assert all(state.get(key) == expected for key in ('bagDrops', 'quickDrops', 'intactBagDrops', 'intactQuickDrops')), (route, keep, curse, state)
            command('driver', 'respawn')
            wait_for(lambda: player('driver').get('alive') is True and life().get('alive') is True and visible_matches(keep), 'native respawn and both client snapshots')
            assert life().get('bagIntact') is keep and life().get('quickIntact') is keep, life()
            assert obj(player('driver')['quick']).get('item') == ('minecraft:shears' if keep else 'minecraft:air')
            note(f'{route}: death/respawn keepInventory={keep} Vanishing={curse}; exact contents/components and no duplicate drops')
        setup(route)
        for dimension in ('minecraft:the_nether', 'minecraft:overworld', 'minecraft:the_end', 'minecraft:overworld'):
            command('server', 'life', action='travel', dimension=dimension)
            wait_for(lambda: player('driver').get('dimension') == dimension and life().get('bagIntact') is True and life().get('quickIntact') is True, 'travel preserves exact items')
            command('server', 'life', target='QuickViewer', action='travel', dimension=dimension, x=6)
            wait_for(lambda: visible_matches(True), 'observer sees traveler after dimension change')
        note(f'{route}: Nether/End round trips preserve the bag, mounts, dye/name and Quick Slot on both clients')
        command('server', 'save')
        command('driver', 'disconnect')
        wait_for(lambda: read('driver').get('connected') is False, 'disconnect')
        command('driver', 'join')
        wait_for(lambda: life().get('bagIntact') is True and life().get('quickIntact') is True and visible_matches(True), 'relog retains exact saved items', 90)
        command('server', 'life', target='QuickViewer', action='travel', dimension='minecraft:overworld', x=1024)
        wait_for(lambda: 'QuickDriver' not in obj(read('observer').get('players', {})), 'tracking exit')
        command('server', 'life', target='QuickViewer', action='travel', dimension='minecraft:overworld', x=6)
        wait_for(lambda: visible_matches(True), 'tracking reentry')
        note(f'{route}: actual relog and tracking exit/reentry rebuild complete remote bag state')
    note('Lifecycle run complete; no production data used')


if __name__ == '__main__':
    main()
