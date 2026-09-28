"""Photograph and drive the worn bag's panel beside the crafting table (D-0033).

Requires the muted network server (`runNetworkServer`) and the driver client
(`runNetworkDriver`). Captures the Expedition and Basic tiers at 1280x720 GUI scale 2
(640x360: the open book beside the panel), scale 3 (427x240: the open book takes the room)
and 960x720 scale 3 (320x240: under the width panel and screen need, the panel hidden), the
recipe book closed and open. With EMI on the classpath (`-PtestMountMods=<dir with the EMI
jar>`, name the run with a suffix: `capture_table.py -emi`) the book's button toggles EMI's
craftables instead, and the run then asks EMI whether oak planks and a furnace can be made from
what the table offers (the logs and cobblestone are only in the bag), fills the furnace through
EMI's own fill path and reads the table's grid and the bag back from the server.
Screenshots land in run/network-driver/screenshots.

Never sends the driver's `focus` op: nothing here needs focus.
"""
import sys
import time

from capture_inventory import ready, shot, wait_for
from network_control import command, obj, read

SIZES: tuple[tuple[int, int, int, str], ...] = ((1280, 720, 2, '640x360'), (1280, 720, 3, '427x240'), (960, 720, 3, '320x240'))
PLAYER = 'QuickDriver'

def table() -> None:
    command('server', 'table')
    wait_for(lambda: read('driver').get('screen') == 'CraftingScreen', 'The crafting table did not open')

def book(open_: bool) -> None:
    if read('driver').get('recipeBook') == open_:
        return
    command('driver', 'tableBook')
    time.sleep(.5)
    if read('driver').get('recipeBook') != open_:
        print('recipe book stays', 'open' if not open_ else 'closed', 'at', read('driver').get('guiWidth'), flush=True)

def close() -> None:
    command('driver', 'close')
    wait_for(lambda: read('driver').get('screen') == '', 'The table did not close')

def server_player() -> dict[str, object]:
    return obj(obj(read('server').get('players', {})).get(PLAYER, {}))

def capture(suffix: str) -> None:
    ready()
    command('server', 'seed')
    time.sleep(6)  # the toasts fade before the first photo
    for tier in ('expedition', 'basic'):
        command('server', 'wear', tier=tier)
        for width, height, scale, size in SIZES:
            command('driver', 'hud', width=width, height=height, arm='RIGHT', attack='HOTBAR', scale=scale)
            time.sleep(.5)
            table()
            book(False)
            shot(f'table-{tier}-{size}-book-closed{suffix}.png')
            book(True)
            shot(f'table-{tier}-{size}-book-open{suffix}.png')
            book(False)
            close()
    if suffix:
        command('server', 'wear', tier='expedition')
        command('driver', 'hud', width=1280, height=720, arm='RIGHT', attack='HOTBAR', scale=2)
        time.sleep(.5)
        table()
        for recipe in ('minecraft:oak_planks', 'minecraft:furnace', 'minecraft:stone_pickaxe'):
            command('driver', 'emiCheck', recipe=recipe)
            time.sleep(.3)
            print('EMI', read('driver').get('emi'), flush=True)
        shot(f'table-expedition-640x360-before-fill{suffix}.png')
        command('driver', 'emiFill', recipe='minecraft:furnace')
        time.sleep(1.5)
        print('EMI', read('driver').get('emi'), flush=True)
        shot(f'table-expedition-640x360-after-fill{suffix}.png')
        p = server_player()
        slots = p.get('slots', [])
        print('server: menu', p.get('menu'), '| result', slots[0] if slots else None, '| grid', slots[1:10],
              '| bag cobblestone cell', obj(p.get('bag', {})).get('cells', [None] * 3)[2], flush=True)
        close()

if __name__ == '__main__':
    capture(sys.argv[1] if len(sys.argv) > 1 else '')
