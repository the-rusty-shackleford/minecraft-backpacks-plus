"""Real full-pack checks; only synthetic test players and an isolated world are used.

Partitions: native poses, vanilla/custom/oversized items, armor/cape, either main arm,
repeated swaps and interrupted menus. Captures are evidence for visual review, not
automated assertions about pixel quality.
"""
import json
from pathlib import Path
import sys
import time

from network_control import command, obj, player, read
from network_test import browse, wait_for

HERE = Path(__file__).parent / "verification"
RESULTS: list[dict[str, object]] = json.loads((HERE / 'full-matrix.json').read_text()) if (HERE / 'full-matrix.json').exists() else []

def record(name: str) -> None:
    RESULTS.append({'check': name, 'server': player('server'), 'observer': player('observer')})
    (HERE / 'full-matrix.json').write_text(json.dumps(RESULTS, indent=2) + '\n')
    print('PASS', name, flush=True)

def photo(name: str) -> None:
    time.sleep(.6)
    command('observer', 'capture', name='release-full-' + name + '.png')

def position() -> None:
    command('server', 'position', target='QuickViewer', x=2.5, y=71, z=-4)
    command('observer', 'view', camera='FIRST_PERSON', fov=38, hideGui=True, yaw=32, pitch=10)

def poses() -> None:
    command('server', 'seed')
    command('server', 'mounts', items=['minecraft:diamond_pickaxe','minecraft:diamond_sword','minecraft:lantern','minecraft:apple'])
    command('observer', 'hud', width=1280, height=720, arm='RIGHT', attack='HOTBAR', scale=2)
    command('driver', 'hud', width=960, height=600, arm='RIGHT', attack='HOTBAR', scale=2)
    command('driver', 'view', camera='THIRD_PERSON_BACK', fov=50, hideGui=False, yaw=0, pitch=0)
    for pose in ('stand', 'boat', 'horse', 'sleep', 'swim'):
        try:
            command('server', 'release', action='pose', pose=pose)
        except RuntimeError as failure:
            if pose != 'sleep' or 'NOT_POSSIBLE_NOW' not in str(failure):
                raise
            # Vanilla updates its day/night brightness cache after the time-changing tick.
            time.sleep(.5)
            command('server', 'release', action='pose', pose=pose)
        position()
        if pose == 'swim':
            command('driver','focus')
            command('driver','movement',forward=True,sprint=True)
            wait_for(lambda: player('server')['pose'] == 'SWIMMING', 'Native swimming did not begin')
        photo(pose)
        record('native pose ' + pose)
        command('driver','movement')
    for mode in ('walk', 'sprint', 'crouch'):
        command('server','release',action='pose',pose='stand')
        position();command('driver','focus')
        command('driver','movement',forward=mode!='crouch',sprint=mode=='sprint',crouch=mode=='crouch')
        photo(mode);record('real movement ' + mode)
        command('driver','movement')
    command('server','release',action='pose',pose='stand')
    position()
    command('observer','cape',target='QuickDriver',enabled=True)
    wait_for(lambda: player('observer').get('cape') is True, 'Native cape descriptor not active')
    photo('cape');record('native CapeLayer with offline test cape')
    command('observer','cape',target='QuickDriver',enabled=False)

def models() -> None:
    command('server','life',action='setup',route='curios',keep=False,curse=False)
    position()
    for armor in ('minecraft:diamond_chestplate','alexsmobs:rocky_chestplate','galestone:galestone_chestplate','voidsteel:voidsteel_chestplate'):
        command('server','release',action='equipment',slot='CHEST',item=armor)
        photo(armor.replace(':','-'));record('actual armor ' + armor)
    sets = (
        ('custom', ['alexsmobs:skelewag_sword','alexsmobs:ghostly_pickaxe','create:wrench','farmersdelight:diamond_knife']),
        ('oversized', ['mowziesmobs:wrought_axe','minecraft:trident','minecraft:lantern','minecraft:torch']),
        ('special', ['minecraft:shield','minecraft:crossbow','minecraft:milk_bucket','minecraft:potion']),
    )
    for name, items in sets:
        command('server','mounts',items=items)
        photo(name);record('actual mounted models ' + name)

def interruptions() -> None:
    command('server','seed');position()
    for arm in ('LEFT','RIGHT'):
        command('driver','hud',arm=arm,attack='HOTBAR',scale=2)
        for slot in (1,2,3,4,4,3,2,1):
            browse(slot)
        command('driver','b')
        wait_for(lambda: player('server')['menu']=='BackpackMenu', 'B failed')
        command('driver','close')
        command('driver','b')
        wait_for(lambda: player('server')['menu']=='BackpackMenu', 'Reopening failed')
        photo('open-'+arm.lower())
        command('driver','menuClick',slot=0,button=0,type='QUICK_MOVE')
        command('driver','close')
        wait_for(lambda: player('server')['menu']=='InventoryMenu', 'Close failed')
        time.sleep(2)
        record('rapid swaps and interrupted opening ' + arm)

if __name__ == '__main__':
    globals()[sys.argv[1]]()
