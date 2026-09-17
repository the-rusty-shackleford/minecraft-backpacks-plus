# Persistent gear bar — visual refinement for D-0008

Update: D-0009 authorizes implementation after this study. The first functional build
implements the direct-swap/storage-fallback behavior below; actual client evidence and
remaining layout/animation gates are in `devtools/verification/first-build.md`.
The text below retains the context of the original design study.

The [interactive preview](../../devtools/interface-preview/index.html) shows the D-0008
interaction with the original Quick Slot and independent backpack mounts. The
[three-state comparison](../../devtools/interface-preview/persistent-states.png) shows no
backpack, an equipped Expedition pack, and holding G to browse its gear.

The original Quick Slot has a brass border; backpack mounts sit across a small divider.
Slots remain visible while idle, browsing, and after a swap. The hotbar stays centered,
the gear group stays opposite the offhand, and the hotbar attack indicator keeps its own
space. The left-arm setting mirrors that arrangement.

When the complete group cannot fit horizontally, the proposed compact layout places the
mounts directly above the Quick Slot, clear of health/hunger and hotbar cells. The preview
can force that layout for review; exact in-game dimensions are not yet approved or tested.

Hold G, scroll, and release to exchange the highlighted slot directly with the selected
hotbar slot. Normal scrolling over the game view selects the normal hotbar. H always
exchanges the standalone Quick Slot. Escape and focus loss cancel browsing. Holding and
releasing without browsing makes no exchange in this prototype; that detail is provisional.

The None/Basic/Reinforced/Expedition controls equip independent sample bags and retain each
bag's contents between changes. The player's Quick Slot and hotbar are also preserved.
Only Reset demo restores all sample items. This illustrates ownership; it does not create
Minecraft storage, change the existing attachment or modify any live inventory.

Proposed handling of the previous hand stack: return it to the vacated mount when it meets
that mount's category; otherwise store it in ordinary backpack storage. If neither can
accept it, cancel the entire exchange. An empty mount can accept an eligible held item;
an incompatible item cannot be stowed there. These rules and category/stack limits still
need interview review, including component-sensitive stacking and a full player inventory.
The standalone Quick Slot continues to allow ordinary two-way swaps with an empty side.

The browser validation exercised direct swaps without displacing Quick Slot, H behavior,
ordinary versus modified scrolling, unequip/re-equip retention, cancellation, full-storage
refusal, empty-slot swaps, tier counts, and right/left/compact HUD clearance. This is UI
prototype evidence only, not Minecraft runtime, compatibility or multiplayer validation.

The older [three-concept study](gear-access-concepts.md) remains historical context. Its
temporary overlay and route through Quick Slot no longer describe the chosen behavior.
