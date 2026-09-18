# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
"""Check the actual G/B key path, not a direct call into our key mapping."""
from pathlib import Path
import json
import time
from network_control import command, read, player, obj
from capture_mounts import ready
from capture_materials import wait_for
from curios_test import equip, record
from native_keys import key


def run() -> None:
    ready()
    command('server','seed')
    equip('expedition')
    command('driver','focus')
    try:
        key('g',True)
        wait_for(lambda:bool(read('driver')['browsing']))
        assert player('server')['menu']=='InventoryMenu'
        command('driver','scroll',delta=-1)
        wait_for(lambda:read('driver')['selection']==1)
    finally:
        key('g',False)
    wait_for(lambda:obj(player('server')['held'])['item']=='minecraft:diamond_sword')
    assert player('server')['menu']=='InventoryMenu'
    record('Physical G press, scroll and release draws the Curios-mounted sword without opening Curios')
    key('b',True);time.sleep(.1);key('b',False)
    wait_for(lambda:player('server')['menu']=='BackpackMenu')
    record('Physical B opens the Curios-worn backpack with Sophisticated Backpacks also installed')
    command('driver','close')
    command('driver','curios')
    wait_for(lambda:'backMenuSlot' in read('driver'))
    # Curios intentionally overlays its recipe book until any key closes that panel.
    key('Escape',True);time.sleep(.1);key('Escape',False);time.sleep(.3)
    if 'backMenuSlot' not in read('driver'):
        command('driver','curios')
        wait_for(lambda:'backMenuSlot' in read('driver'))
    command('driver','capture',name='curios-inventory.png')
    command('driver','close')
    command('server','curiosControl',mode='remove')
    wait_for(lambda:player('server')['source']==-1 and obj(player('driver')['syncedBag'])['item']=='minecraft:air')
    command('driver','focus')
    try:
        key('g',True)
        wait_for(lambda:player('server')['menu']=='CuriosContainer')
    finally:
        key('g',False)
    record('With no backpack equipped, physical G retains its native Curios inventory shortcut')
    command('driver','close')
    (Path(__file__).parent / 'verification/curios-key-after.json').write_text(json.dumps({'driver':read('driver'),'observer':read('observer')},indent=2)+'\n')
    print('PASS real keyboard priority, B opening and no-bag G fallback',flush=True)


if __name__ == "__main__":
    run()
