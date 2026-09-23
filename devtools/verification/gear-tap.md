# Release G commits the highlight without a scroll — 0.2.2, 2026-09-23

Rusty, on 0.2.1: "When you hold G and let go, you expect it to swap with the highlighted
item but it doesn't. You have to scroll one click for that to happen." The cause was
`GearClient`'s `moved` flag: the release committed only after a wheel movement, which
D-0023 had written in as an accident guard. D-0024 replaces it: releasing G commits the
highlighted choice, the highlight opens on the last swap or the first mount, and a bag
deposit is never the default. The rule is a pure state machine (`domain.GearGesture`) and
a pure default (`GearChoices.defaultIndex`); the client reads the step.

- `./gradlew --offline --no-watch-fs check`, JUnit: 20 tests (13 before). Six new in
  `GearGestureTest` (a hold released with or without the wheel moving commits; a press
  and release inside one tick is a tap; a press with nothing to browse opens nothing and
  does not linger; a broken selection cancels and the held key opens nothing until
  released; the wheel opens the gesture before a tick sees the key; a reset drops the
  gesture) and one in `GearChoicesTest` (the highlight starts on the remembered swap if
  offered, else the first mount, never on a deposit; nothing to choose from is refused).
- `./gradlew --offline --no-watch-fs runGameTestServer`: all 35 required real-server
  GameTests passed (the plain set; the Curios and pack-mount variants were not rerun,
  since nothing server-side or in admission changed). The first two attempts died at mod
  load with NightConfig's `FileWatcher$WatchingException`: the desktop was at the
  inotify instance ceiling (128) with Rusty's own client running. The run directory's
  `fml.toml` now has `disableConfigWatcher = true`, the loader's documented switch;
  config hot-reload is irrelevant to the tests and the production jar is unaffected.
- The existing two-client network checks (`devtools/network_test.py`) drive the gesture
  through the key mapping's own down state and a `+1/-1` scroll when the target is
  already highlighted, so they remain valid under the new rule.
- Not verified: the tap in a real client. The native check needs a second Minecraft
  client on this desktop, which the same ceiling and Rusty's one-client rule forbid
  while they are playing; it is the thing to do before the release go, or Rusty tries
  it on their own client from the pack.
