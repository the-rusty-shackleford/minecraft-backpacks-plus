# Backpacks+ engineering

Read `/home/rusty/Code/minecraft mods/AGENTS.md` and its routed shared rules.
Read `knowledge/PROJECT.md`, `knowledge/decisions/README.md` and relevant decisions
before nontrivial work. Never publish or deploy without Rusty's explicit go.

- Bump `event.registrar("N")` in `GearProtocol` in any release that changes a payload or
  the slot layout of a menu both sides build, which includes the bag's cells that
  `InventoryMenuMixin`, `ChestMenuMixin` and `CraftingMenuMixin` add: that version is the only thing NeoForge
  compares at login, so a stale client joins and is kicked with "Network Protocol Error"
  at its first chest (0.5.0 kept "4" over D-0030). Before tagging, diff `registrar(` and
  every menu or slot change since the last tag. Keep new channels required, never
  `.optional()`.
- Anything that counts, takes or gives a player's items goes through Carried
  (`com.chunkworks.carried.api.Carried`, D-0032), not through `BagContents` or the bag's
  component: Carried applies the order, the all-or-nothing takes and the open-screen writes that a
  direct edit would skip. Build and `publishToMavenLocal` `minecraft-carried` before this repo.
