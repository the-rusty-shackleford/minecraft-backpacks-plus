"""Add dyed variants while preserving the approved vanilla material models.

Natural bags retain their exact textures, UVs and geometry. Dyed canvas uses the
neutral counterpart of its original material; leather and hardware remain unchanged.
"""
# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import copy
import json
from typing import cast

ROOT = Path(__file__).resolve().parents[1]

def obj(value: object) -> dict[str, object]:
    if not isinstance(value, dict):
        raise TypeError('Expected model object')
    return cast(dict[str, object], value)

def build() -> None:
    """Requires original models; effects: adds canvas tint and dyed model variants."""
    folder = ROOT / 'src/main/resources/assets/backpacksplus/models/item'
    for tier in ('basic', 'reinforced', 'expedition'):
        for part in ('backpack', 'body', 'lid'):
            name = f'{tier}_{part}'
            path = folder / f'{name}.json'
            model = obj(json.loads(path.read_text()))
            model.pop('overrides', None)
            elements = model['elements']
            assert isinstance(elements, list)
            for element in elements:
                for value in obj(obj(element)['faces']).values():
                    face = obj(value)
                    if face['texture'] == '#canvas':
                        face['tintindex'] = 0
            canvas = 'minecraft:block/white_wool' if tier == 'basic' else 'minecraft:block/white_terracotta'
            if part == 'backpack':
                # A separate full model avoids inheriting its own item override.
                dyed = copy.deepcopy(model)
                obj(dyed['textures'])['canvas'] = canvas
                model['overrides'] = [{'predicate': {'backpacksplus:dyed': 1},
                                       'model': f'backpacksplus:item/{name}_dyed'}]
            else:
                dyed = {'parent': f'backpacksplus:item/{name}', 'textures': {'canvas': canvas}}
            path.write_text(json.dumps(model, indent=2) + '\n')
            (folder / f'{name}_dyed.json').write_text(json.dumps(dyed, indent=2) + '\n')

if __name__ == '__main__':
    build()
