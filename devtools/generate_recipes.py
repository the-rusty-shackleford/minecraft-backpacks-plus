"""Emit the nine approved crafting recipes and their vanilla recipe-book unlocks."""
# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/data/backpacksplus'

def write(path: Path, data: object) -> None:
    """Effects: writes one complete generated JSON resource."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')

def generate() -> None:
    """Effects: emits regular/dyed crafting and recolor recipes for all three tiers."""
    recipes: tuple[tuple[str, list[str], dict[str, dict[str, str]], str], ...] = (
        ('basic', ['SLS', 'LWL', 'L L'], {'S': {'item': 'minecraft:string'}, 'L': {'item': 'minecraft:leather'}, 'W': {'tag': 'minecraft:wool'}}, 'minecraft:leather'),
        ('reinforced', ['ILI', 'SBS', 'I I'], {'I': {'item': 'minecraft:iron_ingot'}, 'L': {'item': 'minecraft:leather'}, 'S': {'item': 'minecraft:string'}, 'B': {'item': 'backpacksplus:basic_backpack'}}, 'backpacksplus:basic_backpack'),
        ('expedition', ['HLH', 'IBI', 'C C'], {'H': {'item': 'minecraft:honeycomb'}, 'L': {'item': 'minecraft:lead'}, 'I': {'item': 'minecraft:iron_block'}, 'B': {'item': 'backpacksplus:reinforced_backpack'}, 'C': {'item': 'minecraft:leather'}}, 'backpacksplus:reinforced_backpack'),
    )
    for tier, pattern, key, unlock in recipes:
        bag = f'backpacksplus:{tier}_backpack'
        for dyed in (False, True):
            name = tier + ('_dyed' if dyed else '')
            shaped = pattern.copy()
            ingredients = key.copy()
            if dyed:
                shaped[2] = shaped[2][0] + 'D' + shaped[2][2]
                ingredients['D'] = {'tag': 'c:dyes'}
            write(ROOT / f'recipe/{name}.json', {'type': 'backpacksplus:backpack_crafting', 'category': 'equipment', 'group': 'backpacksplus:' + tier, 'pattern': shaped, 'key': ingredients, 'result': {'id': bag, 'count': 1}})
        write(ROOT / f'recipe/{tier}_recolor.json', {'type': 'backpacksplus:backpack_dye', 'category': 'equipment', 'group': 'backpacksplus:recolor', 'ingredients': [{'item': bag}, {'tag': 'c:dyes'}], 'result': {'id': bag, 'count': 1}})
        write(ROOT / f'advancement/recipes/{tier}.json', {'parent': 'minecraft:recipes/root', 'criteria': {'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': unlock}]}}}, 'requirements': [['has_material']], 'rewards': {'recipes': [f'backpacksplus:{tier}', f'backpacksplus:{tier}_dyed', f'backpacksplus:{tier}_recolor']}})

if __name__ == '__main__':
    generate()
