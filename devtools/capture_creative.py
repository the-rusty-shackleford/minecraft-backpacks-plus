"""Photograph and drive the worn bag's panel on the creative screen (D-0031).

Requires the muted network server (`runNetworkServer`) and the driver client
(`runNetworkDriver`). With the Expedition bag worn and the player in creative, captures the
inventory tab and a category tab at 1280x720 GUI scale 2 (640x360), 1280x720 scale 3
(427x240), 1376x576 scale 2 (688x288, Rusty's width at scale 5) and 1134x720 scale 3 (378x240,
one under the width panel and screen need, where the panel hides). Then, at 640x360, moves
stacks through the screen's own mouse handling and reads the server: the furnaces from hotbar
slot 1 into bag cell 5, the bread from bag cell 0 over the panel's title (which must not throw
it) onto hotbar slot 4. Screenshots land in run/network-driver/screenshots.

Never sends the driver's `focus` op: nothing here needs focus.
"""
import time

from capture_inventory import ready, shot, wait_for
from network_control import command, obj, read

SIZES: tuple[tuple[int, int, int, str], ...] = ((1280, 720, 2, '640x360'), (1280, 720, 3, '427x240'),
                                               (1376, 576, 2, '688x288'), (1134, 720, 3, '378x240'))
PLAYER = 'QuickDriver'

def open_tab(tab: str) -> None:
    if read('driver').get('screen') != 'CreativeModeInventoryScreen':
        command('driver', 'inventory')
        wait_for(lambda: read('driver').get('screen') == 'CreativeModeInventoryScreen', 'The creative screen did not open')
    command('driver', 'creativeTab', tab=tab)
    time.sleep(.4)

def server_player() -> dict[str, object]:
    return obj(obj(read('server').get('players', {})).get(PLAYER, {}))

def capture() -> None:
    ready()
    command('server', 'seed')
    command('server', 'gamemode', mode='creative')
    time.sleep(6)  # the toasts fade before the first photo
    for width, height, scale, size in SIZES:
        command('driver', 'hud', width=width, height=height, arm='RIGHT', attack='HOTBAR', scale=scale)
        time.sleep(.5)
        open_tab('minecraft:inventory')
        shot(f'creative-inventory-expedition-{size}.png')
        open_tab('minecraft:building_blocks')
        shot(f'creative-blocks-expedition-{size}.png')
        command('driver', 'close')
        wait_for(lambda: read('driver').get('screen') == '', 'The creative screen did not close')

    command('driver', 'hud', width=1280, height=720, arm='RIGHT', attack='HOTBAR', scale=2)
    time.sleep(.5)
    open_tab('minecraft:inventory')
    shot('creative-live-before.png')
    command('driver', 'creativeClick', slot=37)     # hotbar slot 1: sixteen furnaces onto the cursor
    command('driver', 'creativeClick', bagCell=5)   # into bag cell 5
    command('driver', 'creativeClick', bagCell=0)   # the bread from cell 0 onto the cursor
    left, top = int(read('driver').get('guiLeft', 0)), int(read('driver').get('guiTop', 0))
    command('driver', 'creativeClick', x=-180 + 100, y=8)  # the panel's title, between no cells: not outside
    time.sleep(.3)
    command('driver', 'creativeClick', slot=40)     # hotbar slot 4: the bread, if it is still on the cursor
    time.sleep(.8)
    shot('creative-live-after.png')
    command('driver', 'close')
    time.sleep(.8)
    p = server_player()
    cells = obj(p.get('bag', {})).get('cells', [])
    slots = p.get('slots', [])
    print('server: cell 5', cells[5] if len(cells) > 5 else None, '| cell 0', cells[0] if cells else None,
          '| hotbar 1', slots[37] if len(slots) > 40 else None, '| hotbar 4', slots[40] if len(slots) > 40 else None,
          '| guiLeft', left, 'guiTop', top, flush=True)

if __name__ == '__main__':
    capture()
