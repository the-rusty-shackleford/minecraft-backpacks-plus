"""Two actual clients exercise the shipped vehicle protocol; no handling changes.

Partitions: stationary/moving vehicle contact, passenger H priority, moving lights,
towing and a half-block road. Positions are sampled independently from each process.
"""
import json
from pathlib import Path
import sys
import time
from typing import cast

from network_control import command, obj, player, read
from network_test import wait_for

OUT=Path(__file__).parent/'verification/release-wheels-network.json'
evidence: list[dict[str,object]]=json.loads(OUT.read_text()) if OUT.exists() else []

def cars(role: str) -> dict[int,dict[str,object]]:
    values=read(role)['vehicles'];assert isinstance(values,list)
    result: dict[int,dict[str,object]]={}
    for v in values:
        car=obj(v);identifier=car['id'];assert isinstance(identifier,int)
        result[identifier]=car
    return result

def drive(seconds: float, moving: bool=False, turn: bool=False) -> list[dict[str,object]]:
    command('driver','focus')
    command('driver','movement',forward=True,left=turn)
    if moving:command('observer','movement',forward=True)
    samples: list[dict[str,object]]=[]
    deadline=time.monotonic()+seconds
    while time.monotonic()<deadline:
        samples.append({r:read(r)['vehicles'] for r in ('server','driver','observer')})
        time.sleep(.1)
    command('driver','movement')
    command('observer','movement')
    time.sleep(2)
    return samples

def report(name: str, samples: list[dict[str,object]]) -> None:
    evidence.append({'check':name,'samples':samples,'settled':{r:read(r)['vehicles'] for r in ('server','driver','observer')}})
    OUT.write_text(json.dumps(evidence,indent=2)+'\n')
    print('PASS',name,flush=True)

def main() -> None:
    command('server','seed');command('server','seed',target='QuickViewer')
    command('server','wheels',course='road',passenger='QuickViewer')
    wait_for(lambda: cast(int,player('driver')['vehicle'])>=0,'Driver did not board')
    vehicle=cast(int,player('driver')['vehicle'])
    wait_for(lambda: vehicle in cars('observer'),'Observer missed vehicle')
    quick=player('server')['quick'];held=player('server')['held']
    before=cars('server')[vehicle]['lights']
    command('driver','focus');command('driver','h')
    wait_for(lambda: cars('server')[vehicle]['lights']!=before,'Driving H did not toggle lights')
    assert player('server')['quick']==quick and player('server')['held']==held
    light=cars('server')[vehicle]['lights']
    wait_for(lambda: cars('observer')[vehicle]['lights']==light,'Passenger missed light sync')
    old=player('server','QuickViewer')['quick']
    command('observer','focus');command('observer','h')
    wait_for(lambda: player('server','QuickViewer')['held']==old,'Passenger H did not use Quick Slot')
    samples=drive(3)
    assert cars('server')[vehicle]['passengers']==2
    assert cast(float,cars('server')[vehicle]['z'])>5
    report('moving lamps, passenger seating, driver/passenger H priority and slab road',samples)
def remaining(courses: tuple[str,...]=('contact','moving','tow')) -> None:
    for course in courses:
        command('server','wheels',course=course)
        command('server','position',target='QuickViewer',x=24,z=8) if course!='moving' else None
        command('driver','view',camera='THIRD_PERSON_BACK',fov=60,hideGui=False,yaw=0,pitch=8)
        if course=='moving':command('observer','view',camera='THIRD_PERSON_BACK',fov=60,hideGui=False,yaw=180,pitch=8)
        time.sleep(.8)
        vehicle=cast(int,player('server')['vehicle'])
        initial=cars('server')[vehicle]
        samples=drive(3,moving=course=='moving',turn=False)
        current=cars('server')[vehicle]
        assert current['z']!=initial['z'] or current['x']!=initial['x']
        if course=='tow':
            assert cast(int,current['trailer'])>=0
            wait_for(lambda: cast(float,cars('server')[vehicle]['gap'])<.5, 'Tow linkage did not settle',20)
            trailer=cast(int,current['trailer'])
            wait_for(lambda: trailer in cars('observer') and all(abs(cast(float,cars('server')[trailer][a])-cast(float,cars('observer')[trailer][a]))<.75 for a in ('x','y','z')), 'Remote trailer did not converge',20)
        else:
            peers=[c for i,c in cars('server').items() if i!=vehicle and c['profile']==current['profile'] and cast(float,c['x'])>10]
            assert peers and cast(float,current['z'])<cast(float,peers[-1]['z'])
        wait_for(lambda: vehicle in cars('observer'),'Observer lost vehicle')
        wait_for(lambda: abs(cast(float,cars('server')[vehicle]['z'])-cast(float,cars('observer')[vehicle]['z']))<.75,
                 'Observer did not converge after coasting',20)
        report('independent observer '+course,samples)

if __name__=='__main__':
    main()
    remaining()
