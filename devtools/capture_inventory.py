"""Photograph the worn bag's panel on the survival inventory screen (D-0027).

Requires the muted network server (`runNetworkServer`) and the driver client
(`runNetworkDriver`). Captures the Expedition and Basic tiers at 1280x720 with GUI
scale 2 (640x360 rows) and 3 (427x240, the 240-row minimum), the recipe book closed
and open. With EMI loaded the book's button toggles EMI's craftables instead of the
vanilla book, so the captures show EMI's panels; name that run with a suffix
(`capture_inventory.py -emi`). Screenshots land in run/network-driver/screenshots.
"""
import sys
import time
from collections.abc import Callable

from network_control import ROOT, command, obj, read

# Window size, GUI scale and the GUI rows that gives: 720p at scale 2 (the book stands beside the
# panel); 1280 wide at scale 3, a friend's screen (the book overlays); 540 wide, just under the
# width panel, book and screen need beside one another; 320 wide, the minimum, where the panel hides.
SIZES: tuple[tuple[int, int, int, str], ...] = ((1280, 720, 2, '640x360'), (1280, 720, 3, '427x240'), (1080, 720, 2, '540x360'), (960, 720, 3, '320x240'))
TIERS: tuple[str, ...] = ('expedition', 'basic')

def ready() -> None:
    start = time.time()
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        state = ROOT / 'driver-state.json'
        if state.exists() and state.stat().st_mtime >= start and read('driver').get('connected'):
            if 'QuickDriver' in obj(read('server').get('players', {})):
                return
        time.sleep(.2)
    raise TimeoutError('The driver client must be connected')

def wait_for(predicate: Callable[[], bool], description: str, seconds: float = 20) -> None:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if predicate():
            return
        time.sleep(.15)
    raise AssertionError(description)

def shot(name: str) -> None:
    time.sleep(.6)
    command('driver', 'capture', name=name)
    print('captured', name, read('driver').get('guiLeft'), read('driver').get('recipeBook'), flush=True)

def book(open_: bool) -> None:
    """Puts the recipe book in the wanted state through its button. The state outlives the client
    (the server keeps it with the player), so it is read, never assumed; where vanilla's narrow
    layout hides the button under the open book the click cannot land, and the state is reported."""
    if read('driver').get('recipeBook') == open_:
        return
    command('driver', 'recipeBook')
    time.sleep(.5)
    if read('driver').get('recipeBook') != open_:
        print('recipe book stays', 'open' if not open_ else 'closed', 'at', read('driver').get('guiWidth'), flush=True)

def capture(suffix: str) -> None:
    ready()
    command('server', 'seed')
    command('driver', 'focus')
    for tier in TIERS:
        command('server', 'wear', tier=tier)
        time.sleep(6)  # the recipe and chat toasts fade before the first photo
        for width, height, scale, size in SIZES:
            command('driver', 'hud', width=width, height=height, arm='RIGHT', attack='HOTBAR', scale=scale)
            command('driver', 'focus')
            time.sleep(.5)
            command('driver', 'inventory')
            wait_for(lambda: read('driver').get('screen') == 'InventoryScreen', 'The inventory did not open')
            book(False)
            shot(f'inventory-{tier}-{size}-book-closed{suffix}.png')
            book(True)
            shot(f'inventory-{tier}-{size}-book-open{suffix}.png')
            book(False)
            command('driver', 'close')
            wait_for(lambda: read('driver').get('screen') == '', 'The inventory did not close')

if __name__ == '__main__':
    capture(sys.argv[1] if len(sys.argv) > 1 else '')
