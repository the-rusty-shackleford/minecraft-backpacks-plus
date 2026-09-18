# Backpack mount sizing — 2026-09-18

Local Backpacks+ 0.2.0 correction; publication and deployment remain held.

## Reproduction and cause

Ranged Weapons Mod's rifle, scoped rifle, shotgun and machine gun had neither
Backpacks+ mount tags nor the common weapon/tool tags used by `BagContents.longGear`.
All four therefore defaulted to small. The registry audit confirmed that classification
on actual registered items, and tests on the unchanged tags failed through both
`BackpackMenu.clicked` and `MountExchange.swap` (the server handler behind G swaps).
A third regression failed because a recovered rifle could be placed back into a
small mount. This was a classification omission, not a slot-index or render-scale defect.

The local pack-derived fixture exposed the same omission in additional long gear,
and the reverse problem for daggers inheriting sword behavior. The existing optional
mount tags are sufficient; admission code, rendering, protocol and storage remain unchanged.

## Corrected classifications

| Items | Mount |
| --- | --- |
| Ranged Weapons Mod: rifle, scoped rifle, shotgun, machine gun | Long |
| Another Gun Mod: rifle, scoped rifle, shotgun, auto-gun, machine gun, flamethrower, Soul of Mosin rifle | Long |
| Alex's Mobs: Shield of the Deep | Long |
| Mowzie's Mobs: blowgun, sand rake | Long |
| Create: potato cannon, handheld worldshaper | Long |
| Mowzie's Naga Fang Dagger; Block Factory's Bosses dagger | Small |
| Ranged Weapons Mod pistol; Another Gun Mod revolver | Small, explicitly retained |

The before/after registry comparison found **18 changed classifications**: 16 long
and two small. [Machine-readable changes](mount-sizing/classifications.json).
The dump covered 1,307 non-block registry entries; 48 representative non-block gear
items were checked against explicit expected sizes. The retained real-menu regression
adds torch and lantern for **50 gear items**, covering all three bag tiers. This is
not a physical measurement of every item or aircraft/building component in the pack.

## Checks run in this session

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --no-watch-fs build -PtestCurios`
  passed: **13 JUnit and 41 real-server GameTests**. Gun mods absent; optional tag
  entries do not make them dependencies.
- The same build with `-PtestMountMods=/tmp/codex-mount-sizing-20260918/mods` passed:
  **45 real-server GameTests**, including all four retained sizing tests and Curios.
  JUnit remained passing from the preceding build.
- Real Ranged Weapons Mod items exercise every tier, Survival/Creative, empty/occupied
  mounts, accepted and rejected exchanges, and ordinary menu clicks. Refusal preserves
  the entire bag, revision and held stack; accepted moves preserve names and damage.
- Guns saved in a small mount before this correction remain drawable and explicitly
  stowable, preserving their components. Once drawn, the long gun cannot reenter small.
- Compatibility fixtures used Ranged Weapons Mod 2.4.1, Another Gun Mod 1.5.4.2,
  Alex's Mobs 1.22.17, Mowzie's Mobs 1.8.2, Block Factory's Bosses 2.1.2,
  Create 6.0.10, Farmer's Delight 1.3.3 and Vanilla Wheels 1.7.2, with their dependencies.
- The built production jar contains the corrected tags, no test classes and no nested
  dependency jars. Whitespace/diff checks pass.

## Limits and environment

The first headless startup with Distant Horizons 3.3.1 failed before tests: its startup
listener casts `GameTestServer` to `DedicatedServer`. Only Distant Horizons was excluded
from the pack-derived headless fixture; every item mod remained. The completed check is
therefore not claimed as full-pack client validation. Existing third-party recipe errors
from Homesteads/SubWild and compatibility warnings remain visible in the fixture logs;
this sizing correction does not repair or validate those unrelated integrations.

No native keyboard/client or shader visual check was run this session. Host inspection
found Rusty's Prism Minecraft client running, so no second rendering client was launched
and the personal instance was left alone. All isolated GameTest servers exited. No live
server restart, pack assembly, push, tag or release occurred.

Private raw evidence and reproduction source: `/tmp/codex-mount-sizing-20260918/`.
The four retained regressions are in `src/gametest/.../MountSizingGameTests.java`,
registered when the real Ranged Weapons Mod is loaded. Supply actual dependency jars
through `testMountMods`; ordinary builds never bundle them.
