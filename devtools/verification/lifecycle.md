# Real-client lifecycle checks — 2026-09-18

Two actual clients connected to the reused isolated dedicated server. Stowed, SB and
Core were absent. Curios and the normal Quick Slot companion were present; the observer
kept its existing Fresh Animations/EMF/NEA stack. Production and personal profiles were
not used. Both test clients were muted.

The first run reproduced stale backpack displays after a real respawn. Native Minecraft
reuses the player's entity ID; the replacement server-side Session restarted its sequence
counter at zero. Both clients correctly rejected the lower counter as stale, leaving the
HUD/body snapshot behind the actual inventory. `GearSync.clone` now carries only the
sequence into the new session, never equipment references, open menus or actions.

The exact two-client regression then passed all 13 recorded checks in `lifecycle.json`:

- Chest and Curios routes, each with all four keepInventory/Vanishing combinations.
  Actual death and native client respawn preserve or discard the whole bag and the
  independent Quick Slot correctly. Drops appear once, with exact item components.
- Named/dyed Expedition bag, first/last storage cells, all four mounts, named/damaged
  weapon and named/damaged Quick Slot shears. Server comparisons use complete native
  ItemStack component equality; both clients receive the matching bag UUID, revision,
  cells, name and dye. One Curios death occurs while its real B-opened menu is open.
- Nether and End round trips for each equipment route. The observer follows between
  dimensions and sees the restored bag snapshot; no contents or Quick Slot data change.
- Actual disconnect/relogin and observer tracking exit/reentry for both routes.

`devtools/lifecycle_test.py` drives normal client input/respawn packets. Its server adapter
only seeds fixtures and causes native deaths/dimension transitions. It never substitutes
a network, inventory or persistence backend. This is deterministic lifecycle coverage,
not a manual portal traversal, controller session or completed pose/animation review.

A fixture restart attempt initially hit the world lock because its prior server had not
yet closed after client disconnect. No world was forced open or lock removed. The empty
server was stopped, its process exit verified, and then the same world restarted normally.
Release remains HELD.
