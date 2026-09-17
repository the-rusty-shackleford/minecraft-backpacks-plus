# Persistent gear bar prototype

Open `index.html` in a browser. It is self-contained and needs no server, dependencies,
network requests or Minecraft assets. `persistent-states.png` compares no backpack,
an equipped Expedition pack and browsing gear. Append `?sheet` to the HTML URL to view
that comparison as a page.

Hold **G**, scroll or use arrow keys, then release G to swap the highlighted gear slot
directly with the selected hotbar slot. **H** swaps only the original Quick Slot.
**Escape** cancels. Normal scrolling over the game view selects the ordinary hotbar.
On-screen buttons provide another way to try the interaction.

None/Basic/Reinforced/Expedition equips the corresponding sample bag while retaining each
bag's contents and the player's Quick Slot/hotbar. Reset demo restores all sample items.
Try the main-arm, attack-indicator and compact-layout controls to inspect HUD clearance.

This is an interaction prototype, not Minecraft code. The original SVG art and sample
inventory illustrate [D-0008](../../knowledge/decisions/D-0008.md) and the remaining layout
proposals in [the current study](../../knowledge/design/persistent-gear-bar.md).

`concepts.html` and `comparison.png` preserve the earlier three-concept comparison. Its
temporary overlays and routing of mounted items through Quick Slot are superseded.
