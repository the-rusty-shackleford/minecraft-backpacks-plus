"""Photograph the worn bag's panel beside a chest screen (D-0030).

Requires the muted network server (`runNetworkServer`) and the driver client
(`runNetworkDriver`). Captures a single and a double chest with the Expedition and the
Basic bag worn at 1280x720 GUI scale 2 (640x360), 1280x720 scale 3 (427x240, the
240-row minimum), and 960x720 scale 3 (320x240, under the width panel and screen need,
where the panel hides), then with no bag. Screenshots land in run/network-driver/screenshots.

Never sends the driver's `focus` op: it calls glfwFocusWindow, which raises an iconified
client over the desktop and takes the mouse (2026-09-27, over Rusty's game). Nothing here
needs focus: the server opens the chest and a capture reads the render target.
"""
import sys

from capture_inventory import ready, shot, wait_for
from network_control import command, read

SIZES: tuple[tuple[int, int, int, str], ...] = ((1280, 720, 2, '640x360'), (1280, 720, 3, '427x240'), (960, 720, 3, '320x240'))

def chest(double: bool, name: str) -> None:
    for width, height, scale, size in SIZES:
        command('driver', 'hud', width=width, height=height, arm='RIGHT', attack='HOTBAR', scale=scale)
        command('server', 'chest', double=double)
        wait_for(lambda: read('driver').get('screen') == 'ContainerScreen', 'The chest did not open')
        shot(f'chest-{name}-{"double" if double else "single"}-{size}.png')
        command('driver', 'close')
        wait_for(lambda: read('driver').get('screen') == '', 'The chest did not close')

def capture(suffix: str) -> None:
    ready()
    command('server', 'seed')
    for tier in ('expedition', 'basic'):
        command('server', 'wear', tier=tier)
        for double in (False, True):
            chest(double, tier + suffix)
    command('server', 'holdBag')  # the worn bag into the hotbar: none worn
    chest(False, 'none' + suffix)

if __name__ == '__main__':
    capture(sys.argv[1] if len(sys.argv) > 1 else '')
