"""Capture actual gun mounts with one muted, shader-enabled network client.

Requires the complete pack in the isolated server/driver profiles and a solid
platform at Y=150, above the forest LODs. The detached
camera observes the ordinary networked player; it never replaces item rendering.
Partitions: all bag tiers, both long mounts, 3-D/sprite guns, reference tools,
worn/hand-carried bags. Use --quick for the first Expedition inspection.
"""
import json
from pathlib import Path
import sys
import time
from network_control import command, read

VIEWS: dict[str, tuple[float, float, float]] = {
    "rear": (0, -3, 0),
    "left": (2.1, -2.6, 39),
    "right": (-2.1, -2.6, -39),
    "side-left": (3, -0.25, 85.2),
    "side-right": (-3, -0.25, -85.2),
}
GUNS = ("rifle", "scoped_rifle", "shotgun", "machine_gun")


def view(name: str, angle: str, *, height: float = 152.15, fov: int = 30) -> None:
    x, z, yaw = VIEWS[angle]
    command("driver", "orbit", x=x, y=height, z=z, yaw=yaw, pitch=0, fov=fov)
    time.sleep(1.2)
    command("driver", "capture", name=f"gun-after-{name}-{angle}.png")


def mount(tier: str, items: tuple[str, ...]) -> None:
    command("server", "release", action="equipment", slot="CHEST",
            item=f"backpacksplus:{tier}_backpack")
    slots = {"basic": 2, "reinforced": 3, "expedition": 4}[tier]
    command("server", "mounts", items=[*items, *["minecraft:air"] * (slots-len(items))])
    time.sleep(.7)


def capture(quick: bool) -> None:
    if not read("driver").get("connected") or not read("driver").get("shaders"):
        raise RuntimeError("The isolated real client must be connected with shaders")
    command("server", "seed")
    command("server", "position", x=0, y=151, z=0)
    command("driver", "hud", width=1280, height=720, arm="RIGHT", attack="HOTBAR", scale=2)
    command("driver", "view", camera="THIRD_PERSON_BACK", fov=30, hideGui=True, yaw=0, pitch=0)
    if quick:
        mount("expedition", tuple(f"rangedweaponsmod:{g}" for g in GUNS[:2]))
        for angle in VIEWS:
            view("expedition-rifles", angle)
        return
    for tier in ("basic", "reinforced", "expedition"):
        pairs: list[tuple[str, ...]] = [(g,) for g in GUNS] if tier == "basic" else [GUNS[:2], GUNS[2:]]
        if tier == "expedition":
            pairs.extend([tuple(reversed(GUNS[:2])), tuple(reversed(GUNS[2:]))])
        for pair in pairs:
            mount(tier, tuple(f"rangedweaponsmod:{g}" for g in pair))
            label = tier + "-" + "-".join(pair)
            for angle in (VIEWS if tier == "expedition" else ("left", "right", "side-left", "side-right")):
                view(label, angle)
            print(label, "captured", flush=True)
    for pair in (("rifle", "rifle_spyglass"), ("shotgun", "machine_gun"), ("auto_gun", "flame_thrower")):
        mount("expedition", tuple(f"anothergunmod:{g}" for g in pair))
        for angle in ("left", "right"):
            view("agm-" + "-".join(pair), angle)
    mount("expedition", ("minecraft:diamond_pickaxe", "minecraft:diamond_sword"))
    for angle in ("left", "right"):
        view("reference", angle)
    mount("expedition", ("rangedweaponsmod:rifle", "rangedweaponsmod:machine_gun"))
    command("server", "holdBag")
    time.sleep(1)
    for angle in ("left", "right"):
        view("carried", angle, height=151.7, fov=40)
    Path("run/network-control/firearms-evidence.json").write_text(json.dumps(read("driver"), indent=2)+"\n")
    print("Gun mount capture matrix complete", flush=True)


if __name__ == "__main__":
    capture("--quick" in sys.argv)
