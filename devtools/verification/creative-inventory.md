# Creative inventory access — 2026-09-18

Local follow-up to released Backpacks+ 0.1.0; not published or deployed.

Rusty wants Creative to support obtaining and testing everything without gathering
materials, ammunition or fuel. Backpacks+ registered all three items but never
contributed them to a Creative category, so the Creative search tab also omitted them.
The fix puts Basic, Reinforced and Expedition in Tools & Utilities and search.
Survival recipes and unlocks retain their existing behavior. This change does not
implement ingredient-free crafting tables.

## Verification in this session

- Added a real-server regression check that builds the game's Creative tabs and
  checks every registered Backpacks+ item in both Tools & Utilities and search,
  with operator permissions disabled and enabled. Before the fix, it failed on
  Basic Backpack; the other 28 GameTests passed.
- After the registration fix, `./gradlew --no-watch-fs build` passed all 29 server
  GameTests. All 13 JUnit tests also ran and passed.
- Started the isolated complete pack 1.39.0 server and one muted client, using the
  changed Backpacks+ source. Iris/Complementary shaders were enabled.
- Cleared the test player's inventory, opened the normal Creative inventory using
  the inventory key, selected search and typed `backpack`. All three tiers appeared.
- Used native mouse clicks to take each item into the hotbar. The dedicated server
  reported exactly one `backpacksplus:basic_backpack`, one
  `backpacksplus:reinforced_backpack` and one `backpacksplus:expedition_backpack`,
  in hotbar slots 0, 1 and 2. No crafting, ingredients or give commands were used.
- Viewed the [actual client capture](creative-inventory.png). Shut down the isolated
  client and server afterwards. Production pack, client and server are unchanged.

Scratch logs and command evidence: `/tmp/codex-creative-20260918/`.
