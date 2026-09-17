"""Capture real, unmodified gameplay gestures for the review, then close both clients."""
from pathlib import Path
import json
import time
from network_control import ROOT, command, read, player, obj

def ready() -> None:
    start=time.time()
    deadline=time.monotonic()+90
    while time.monotonic()<deadline:
        if all((ROOT/f'{r}-state.json').is_file() and (ROOT/f'{r}-state.json').stat().st_mtime>=start and read(r).get('connected') for r in ('driver','observer')):
            if all(n in obj(read('server').get('players',{})) for n in ('QuickDriver','QuickViewer')): return
        time.sleep(.2)
    raise TimeoutError('Clients not ready')

def lap(role: str, steps: int) -> None:
    command(role,'g',down=True)
    for _ in range(steps): command(role,'scroll',delta=-1)
    command(role,'g',down=False)

def wait_record(role: str) -> None:
    deadline=time.monotonic()+40
    while read(role).get('framesRemaining',0)!=0:
        if time.monotonic()>deadline: raise TimeoutError('Recording stalled')
        time.sleep(.2)

ready()
evidence: dict[str,object]={}
try:
    command('server','seed')
    command('server','held',item='minecraft:air')
    command('server','position',target='QuickViewer',x=-2.5,z=-4)
    command('observer','hud',width=1280,height=720,arm='RIGHT',attack='HOTBAR',scale=2)
    command('observer','view',camera='FIRST_PERSON',fov=35,hideGui=True,yaw=-32,pitch=10)
    command('driver','hud',width=960,height=640,arm='RIGHT',attack='HOTBAR',scale=2)
    command('driver','view',camera='THIRD_PERSON_BACK',fov=50,hideGui=False,yaw=0,pitch=10)
    command('driver','focus')
    time.sleep(2)
    command('observer','capture',name='backpack-final-worn.png')
    command('observer','record',name='backpack-final-rear',frames=240)
    time.sleep(.8)
    lap('driver',1)
    time.sleep(.35)
    assert obj(player('server')['held'])['item']=='minecraft:diamond_pickaxe'
    assert player('observer')['action']=='DRAW'
    evidence['draw_received']=True
    time.sleep(1.5)
    lap('driver',5)
    time.sleep(.35)
    assert obj(player('server')['held'])['item']=='minecraft:air'
    assert player('observer')['action']=='STOW'
    evidence['stow_received']=True
    time.sleep(1.5)
    command('driver','b')
    time.sleep(2)
    assert player('server')['menu']=='BackpackMenu'
    opened=player('observer')['openedAt']
    assert isinstance(opened,int) and opened>=0
    command('driver','menuClick',slot=0,button=0,type='QUICK_MOVE')
    time.sleep(.35)
    assert player('observer')['action']=='RETRIEVE'
    evidence['retrieve_received']=True
    time.sleep(.75)
    command('driver','close')
    wait_record('observer')
    print('Rear draw/stow/open/retrieve capture passed',flush=True)

    command('server','seed')
    command('server','held',item='minecraft:air')
    command('server','position',target='QuickViewer',x=2.5,z=4)
    command('observer','view',camera='FIRST_PERSON',fov=35,hideGui=True,yaw=148,pitch=10)
    command('observer','record',name='backpack-final-front',frames=250)
    time.sleep(.75)
    command('driver','b')
    time.sleep(2)
    command('observer','capture',name='backpack-final-open.png')
    command('driver','menuClick',slot=0,button=0,type='QUICK_MOVE')
    time.sleep(1)
    command('driver','close')
    time.sleep(1.5)
    command('server','holdBag')
    time.sleep(1)
    command('observer','capture',name='backpack-final-held.png')
    command('driver','use')
    time.sleep(2)
    assert player('server')['menu']=='BackpackMenu'
    opened=player('observer')['openedAt']
    assert isinstance(opened,int) and opened>=0
    command('observer','capture',name='backpack-final-held-open.png')
    evidence['held_menu_opens']=True
    command('driver','close')
    wait_record('observer')
    evidence['fresh_animations_view']=player('observer')
    print('Front opening and mounted side-carry capture passed',flush=True)

    # Reverse roles: the plain client observes the other real player's wide skin and left main arm.
    command('server','seed',target='QuickViewer')
    command('server','held',target='QuickViewer',item='minecraft:air')
    command('server','position',target='QuickDriver',x=-2.5,z=-4)
    command('observer','hud',width=960,height=640,arm='LEFT',attack='HOTBAR',scale=2)
    command('observer','view',camera='THIRD_PERSON_BACK',fov=50,hideGui=False,yaw=0,pitch=10)
    command('driver','hud',width=1280,height=720,arm='RIGHT',attack='HOTBAR',scale=2)
    command('driver','view',camera='FIRST_PERSON',fov=35,hideGui=True,yaw=-32,pitch=10)
    command('observer','focus')
    time.sleep(2)
    command('driver','capture',name='backpack-final-wide.png')
    command('driver','record',name='backpack-final-wide',frames=180)
    time.sleep(.75)
    lap('observer',1)
    time.sleep(.35)
    assert obj(player('server','QuickViewer')['held'])['item']=='minecraft:diamond_pickaxe'
    evidence['wide_left_draw']=True
    time.sleep(1.5)
    command('observer','b')
    time.sleep(2)
    assert player('server','QuickViewer')['menu']=='BackpackMenu'
    command('observer','close')
    wait_record('driver')
    evidence['plain_client_view']=player('driver','QuickViewer')
    assert obj(evidence['fresh_animations_view'])['skinModel']=='SLIM'
    assert obj(evidence['plain_client_view'])['skinModel']=='WIDE'
    print('Wide/left-arm capture without animation mods passed',flush=True)
    (Path(__file__).parent/'verification/animation-evidence.json').write_text(json.dumps(evidence,indent=2)+'\n')
finally:
    for role in ('driver','observer','server'): command(role,'quit')
