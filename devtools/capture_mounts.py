"""Capture real mounted-item orientation, resource reload and both backpack sides.

Requires the muted network server and two existing clients, with Farmer's Delight
in all three profiles. Follow with capture_motion.py to record real interactions.
No renderer or inventory transaction is substituted by this fixture.
"""
import time
from network_control import ROOT, command, obj, read

SETS: tuple[tuple[str, tuple[str, ...]], ...] = (
    ('sword-pickaxe', ('diamond_pickaxe', 'diamond_sword', 'apple', 'torch')),
    ('axe-hoe', ('diamond_axe', 'diamond_hoe', 'shears', 'flint_and_steel')),
    ('shovel-mace', ('diamond_shovel', 'mace', 'brush', 'potion')),
    ('trident-bow', ('trident', 'bow', 'lantern', 'honey_bottle')),
    ('shield-crossbow', ('shield', 'crossbow', 'milk_bucket', 'mushroom_stew')),
    ('fishing-sword', ('fishing_rod', 'iron_sword', 'ender_pearl', 'iron_ingot')),
    ('food', ('netherite_sword', 'netherite_pickaxe', 'carrot', 'sweet_berries')),
)

def ready() -> None:
    start = time.time()
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        if all((ROOT / f'{r}-state.json').exists()
               and (ROOT / f'{r}-state.json').stat().st_mtime >= start
               and read(r).get('connected') for r in ('driver', 'observer')):
            if all(n in obj(read('server').get('players', {})) for n in ('QuickDriver', 'QuickViewer')):
                return
        time.sleep(.2)
    raise TimeoutError('Both real clients must be connected')

def mounts(target: str, items: tuple[str, ...]) -> None:
    command('server', 'mounts', target=target,
            items=[i if ':' in i else 'minecraft:' + i for i in items])
    time.sleep(.7)

def resources(refined: bool) -> None:
    command('observer', 'resources', refined=refined)
    time.sleep(2)
    deadline = time.monotonic() + 60
    while read('observer').get('reloading'):
        if time.monotonic() > deadline:
            raise TimeoutError('Resource reload stalled')
        time.sleep(.2)

def side(name: str, x: float, z: float, yaw: float) -> None:
    command('server', 'position', target='QuickViewer', x=x, z=z)
    time.sleep(.5)
    command('observer', 'view', camera='FIRST_PERSON', fov=35,
            hideGui=True, yaw=yaw, pitch=10)
    time.sleep(.7)
    command('observer', 'capture', name=f'orientation-after-{name}.png')

def capture() -> None:
    ready()
    command('observer', 'hud', width=1280, height=720, arm='RIGHT', attack='HOTBAR', scale=2)
    command('observer', 'focus')
    command('server', 'position', target='QuickDriver', x=6, z=6)
    for refined in (True, False):
        resources(refined)
        command('server', 'seed', target='QuickViewer')
        time.sleep(.5)
        command('observer', 'view', camera='THIRD_PERSON_BACK', fov=30,
                hideGui=True, yaw=0, pitch=10)
        for name, items in SETS:
            mounts('QuickViewer', items)
            suffix = name if refined else 'vanilla-' + name
            command('observer', 'capture', name=f'orientation-after-{suffix}.png')
        print('Refined Tools' if refined else 'Vanilla', 'rear views captured', flush=True)
    resources(True)
    command('server', 'seed', target='QuickDriver')
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50,
            hideGui=True, yaw=0, pitch=0)
    mounts('QuickDriver', ('diamond_pickaxe', 'diamond_sword', 'brush', 'farmersdelight:diamond_knife'))
    side('threequarter-right', -2.5, -4, -32)
    side('threequarter-left', 2.5, -4, 32)
    side('sword-side', 4, 0, 90)
    mounts('QuickDriver', ('diamond_sword', 'diamond_pickaxe', 'shears', 'flint_and_steel'))
    side('reversed-mounts', -2.5, -4, -32)
    mounts('QuickDriver', ('shield', 'crossbow', 'farmersdelight:beef_stew', 'farmersdelight:apple_cider'))
    side('shield-left', 2.5, -4, 32)
    side('shield-right', -2.5, -4, -32)
    side('shield-side', -4, 0, -90)
    print('Side contact, reversed mounts and Farmer’s Delight captured', flush=True)

if __name__ == '__main__':
    capture()
