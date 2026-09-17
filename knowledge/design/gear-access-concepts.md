# Gear access concepts — historical alternatives

Superseded in part by [D-0008](../decisions/D-0008.md): Rusty chose a persistent gear bar
with independent backpack quick slots and direct swaps on releasing G. The original
alternative study below is retained as context, not the current interface specification.
See the [current persistent-bar study](persistent-gear-bar.md).

During the design interview on 2026-09-17, Rusty requested a small handful of intuitive
interfaces with visuals. Their initial preference is holding a button to scroll through
a separate Quick Slot/backpack bar, then releasing to select. This is a design direction,
not approval of an exact layout, binding or item-exchange protocol.

The [original interactive study](../../devtools/interface-preview/concepts.html) and
[comparison image](../../devtools/interface-preview/comparison.png) illustrate three options:

| Option | Gesture | Benefit | Trade-off |
| --- | --- | --- | --- |
| A: Gear bar | Hold, scroll, release | Familiar hotbar presentation; keeps the center clear | Some selections require several scroll steps |
| B: Gear wheel | Hold, point, release | Items can be chosen by direction | Covers the center of the view while open |
| C: Pack map | Hold, point to a mount, release | Matches the item's physical mount on the pack | Smaller targets need more precise aiming |

Current recommendation: A for quick access, with the physical mount arrangement from C
in the ordinary backpack screen. All three remain proposals. Basic, Reinforced and
Expedition show the approved 2/3/4 mounts; ordinary storage uses the working 9/18/36 slots.
The quick-access study includes Quick Slot and dedicated mounts, not all ordinary contents.

The prototype offers two release behaviors for comparison:

- Ready the selected item in Quick Slot, then use H to exchange it with the selected hotbar item.
- Ready the item and immediately perform that H exchange, drawing it on release. The old
  hand item then occupies Quick Slot.

Proposed item exchange: the previous Quick Slot stack returns to the vacated mount when
eligible; otherwise it goes into ordinary backpack storage. If neither destination can
accept it, cancel the entire exchange. Never discard or duplicate a stack. Exact eligibility,
stack limits, component-sensitive stacking and server validation still require design review.
The browser demo illustrates the flow with a small fixed set of sample items; it is not a
Minecraft storage implementation or evidence for mod compatibility.

The gold outline is only a preview while browsing. Escape or losing focus cancels. Releasing
without changing the preview leaves items alone. H retains the approved swap behavior and
must yield to driving controls in game. G is a demo binding only; no final gear key is chosen.

The shared backpack-screen study positions long mounts beside a schematic bag, small mounts
near its pockets, and ordinary storage in a nine-column grid. Player inventory rows are
omitted from this focused study. Clicking a mount and selecting it exercises the same sample
item state as quick access; free inventory rearrangement is outside this prototype.

All bag art, terrain and item icons in the page are original interface illustrations, not
final models, Refined Tools replacements or in-game captures. No gameplay implementation,
release, pack update or removal of legacy mods is approved by producing these previews.
