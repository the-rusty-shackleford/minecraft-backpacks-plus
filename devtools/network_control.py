"""Typed control client for the isolated real Minecraft server/client fixtures."""
from pathlib import Path
import json
import sys
import time
from typing import cast

ROOT = Path(__file__).resolve().parents[1] / 'run/network-control'

def obj(value: object) -> dict[str, object]:
    if not isinstance(value, dict) or not all(isinstance(key, str) for key in value):
        raise TypeError('Expected JSON object')
    return cast(dict[str, object], value)

def read(role: str) -> dict[str, object]:
    path = ROOT / f'{role}-state.json'
    return obj(json.loads(path.read_text())) if path.exists() else {}

def command(role: str, op: str, **fields: object) -> dict[str, object]:
    ROOT.mkdir(parents=True, exist_ok=True)
    path = ROOT / f'{role}-command.json'
    previous = obj(json.loads(path.read_text())) if path.exists() else {}
    prior = previous.get('seq', 0)
    assert isinstance(prior, int)
    sequence = prior + 1
    fields.update(seq=sequence, op=op)
    tmp = path.with_suffix('.tmp')
    tmp.write_text(json.dumps(fields))
    tmp.replace(path)
    if op == 'quit':
        return {}
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        state = read(role)
        if state.get('error'):
            raise RuntimeError(str(state['error']))
        current = state.get('seq', 0)
        if isinstance(current, int) and current >= sequence:
            return state
        time.sleep(0.1)
    raise TimeoutError(f'{role} did not acknowledge {op}')

def player(role: str, name: str = 'QuickDriver') -> dict[str, object]:
    return obj(obj(read(role)['players'])[name])

if __name__ == '__main__':
    role = sys.argv[1]
    if len(sys.argv) > 2:
        fields = obj(json.loads(sys.argv[3])) if len(sys.argv) > 3 else {}
        result = command(role, sys.argv[2], **fields)
    else:
        result = read(role)
    print(json.dumps(result, indent=2))
