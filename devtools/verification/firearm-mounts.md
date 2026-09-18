# Firearm mount correction — 2026-09-18

Rusty reported the released long guns lying across the back. The actual 0.2.0
shader client reproduced that failure before the renderer changed:
[before](firearm-mounts/before.png), [corrected quarter view](firearm-mounts/after.png),
[corrected rear](firearm-mounts/machine-gun-shotgun-rear.png).

## Behavior

Ranged Weapons Mod rifle, scoped rifle, shotgun and machine gun now point barrel-up,
with their broad faces parallel to the bag sides. Another Gun Mod rifle, scoped rifle,
shotgun, machine gun, auto gun and flamethrower receive the corresponding sprite rotation.
The optional `backpacksplus:mounts/firearms` tag chooses this presentation; it does not
change mount admission. Its supported models point +X in 3-D or upper-left in flat
artwork. Geometry is still measured from the real NONE-context renderer and cached;
resource/tag reloads clear the classification. Original models and 0.92/0.26 diagonal
size limits are retained. Measured thickness sets the clearance against each tier's
side surface, with no outward lean. Storage, controls and protocol are unchanged.

## Executed validation

- Actual shader-enabled, muted client connected to the full-pack dedicated fixture.
  Complementary/Iris, Distant Horizons, Refined Tools and the published RWM 2.4.0 and
  AGM 1.5.4.2 jars were present. One rendering client at a time; no personal profile edits.
- All four RWM long guns on Basic, Reinforced and Expedition; both mounts, including
  reversed pairs on Expedition. Rear, quarters and side contact inspected as actual
  renders and enlarged crops. [Four guns](firearm-mounts/guns-sheet.png),
  [Reinforced](firearm-mounts/reinforced-sheet.png).
- All six AGM gun sprites inspected from both quarters; muzzles point upward and flat
  faces follow the sides. [AGM matrix](firearm-mounts/agm-sheet.png).
- Sword/pickaxe reference and hand-carried bag inspected. [Reference crops](firearm-mounts/held-reference-sheet.png),
  [full carried view](firearm-mounts/carried.png). The existing carry pose can occlude
  the inward mount in the player's leg and put a long stock against the floor; this
  correction does not redesign the approved hand/handle contact or carry clearance.
- G key mapping + real wheel event + release drew and restowed the actual rifle;
  authoritative inventory checked after each operation. [Restowed](firearm-mounts/restowed.png)
  and [runtime evidence](firearm-mounts/runtime.json).
- `uv run --no-project --with mypy mypy --strict devtools/capture_firearms.py devtools/network_control.py` passed.

The first low booth's nearby terrain LODs obscured later captures after clearing trees.
Moving the booth to an open platform at Y=150 resolved this while retaining shaders and
DH. The final comparison uses that unobstructed booth. Before/after camera and environment
are therefore different; the images demonstrate mounting direction, not pixel identity.
An invalid test-only FOV=25 attempt was corrected to the supported minimum of 30.
The recorded full-pack client contained no firearm measurement or renderer exception.

`devtools/capture_firearms.py` repeats the visual partitions with one detached test camera
observing the ordinary networked player; it does not replace production rendering.
General animation/body pose coverage is not claimed by this focused correction.

Clean `./gradlew --no-watch-fs clean build -PtestMountMods=<published server fixture>` passed: **13 JUnit and 45 actual-server GameTests**. The headless fixture excludes DH because of its previously reproduced GameTestServer cast failure; the visual dedicated server/client included DH. The final jar contains byte-identical renderer and firearm tag payloads to those inspected in the live client. Both private runtimes exited after an empty-player check.


## Released — 2026-09-18

Rusty explicitly approved the separate hotfix release. Backpacks+ **0.2.1** is
published and deployed in **pack 1.40.1**. The release asset and installed server jar
match SHA-1 `36881028ae418631612c7961b6b99e10593283d9`. The new startup loads
0.2.1, Mod Hub reports no differences, and RCON reports 20 TPS. The downloaded client
archive also matches staging. World/configuration remained intact, and the production
restart followed a fresh empty-player check. The separate carry-clearance observation
is tracked in [issue #1](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/issues/1).
