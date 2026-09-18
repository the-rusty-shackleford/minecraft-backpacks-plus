# Companion fit and elytra — 2026-09-18

[Actual captures](../companion-preview/index.html). Decisions: Backpacks+ D-0020/D-0021
and Quick Slot D-0011. Production and personal profiles remain unchanged; release HELD.

Two reused muted clients connected to the dedicated test server. A private authenticated
Xvfb display kept focus and mouse capture off the desktop. The renderer processes' DISPLAY
values were checked at the host level. Future automated captures should use an isolated
display too; desktop focus must be called out before a visible check.

## Executed checks

- Visible Curios and chest bags hide Quick Slot back items. Small lower-back items move
  to the hip; the holder/item sit 0.18 blocks lower only while a pack is visible, so they
  clear its lower pocket. The earlier approved item scale and ordinary body fit remain.
- Actual Curios render-toggle input restores ordinary Quick Slot placement. An inventory-only
  or hand-carried bag does not cover the back. Bowl meals remain unconditionally hidden.
- Apple, potion, shears and lantern captures; real crouch input with a chestplate. Wide/left
  and slim/right skin/arm combinations were visually inspected. The dedicated bag mounts
  remain independent through native H draw/stow, including bag identity and revision.
- A temporary resource pack changed `with_backpack` tilt/scale, then actual resource reload
  restored the fit. Hidden-bag captures use the base transform. The override is test data.
- With elytra, the worn bag/mounts and their arm gestures are hidden. Both clients retain
  the exact bag snapshot and Curios visibility flag; mounted light is still 15 with empty
  hands and real world block light 0. B opens the menu and native G/wheel/release draws and
  stows the sword. The independent Quick Slot is unchanged. Removing elytra restores the bag.
- Actual jump input started server-confirmed fall flight; the local third-person capture
  shows clear wings. A held bag remains visible and opens through normal right-click.
  A follow-up remote draw capture waited for the native equipment update before taking
  the image; the original immediate server-ack capture preceded the observer's held update.
- Backpacks+ build with the actual Curios backend passed all 33 required GameTests. All
  13 JUnit tests passed. Quick Slot's companion build passed 26 JUnit and 22 GameTests.
  Both capture scripts passed `mypy --strict` (sandbox DNS failed; the host uv cache worked).

`quickslot_fit_test.py` covers the slim/right runtime sequence. The reversed wide/left
captures were made through the same fixture controls separately, not by that script.
`elytra_test.py` drives the elytra sequence. Their JSON files record runtime assertions;
visual claims above come from inspecting the actual captures, not those assertions alone.

## Limits still open

This pass has no shaders. It does not finish the broader pose, cape, bulky armor, oversized
item, full-pack or controller matrix. Previously recorded shader lighting checks remain
valid but do not stand in for backpack shadow/animation review. The earlier motion work
still needs final reach/hand-contact and retrieval review; this rule only prevents hidden
worn-gear gestures under elytra. Vehicle multiplayer and historical movement-warning work
remain separate follow-ups. No release or live legacy removal has occurred.

## Cleanup

Both clients and the empty isolated dedicated server exited normally after capture.
The owned off-screen X server was stopped; the temporary placement resource pack was
archived out of the test profile after being disabled. The six backed-up Sophisticated
Backpacks/Core jars were restored byte-for-byte to the three isolated profiles. This
restores the reusable fixtures, not production legacy bags. The local preview passed
image loading, all three tab controls and mobile-width checks in a headless browser.
Production jars contain no gametest classes, migration runner or private NBT files.
