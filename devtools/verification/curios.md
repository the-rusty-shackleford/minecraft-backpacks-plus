# Curios compatibility verification — 2026-09-17

Release **HELD**. Curios API **9.5.1+1.21.1**, NeoForge **21.1.248**, Java 21.
See the [actual third-person preview](../curios-preview/index.html).

## Implemented behavior

All three tiers support the functional Curios `back` slot, including simultaneous chest
armor. Backpacks+ supplies its own player-slot assignment and item tag; it does not rely
on Sophisticated Backpacks creating that slot. Curios stays optional and is not bundled.
Without it, the native chest and hand-carried routes remain available.

As Rusty selected, the Curios bag wins over a Backpacks+ chest bag. The first active back
slot is used if a datapack grants multiple slots. B, the gear bar, mount exchanges, menu
edits, opening poses and mounted light all use the same server-selected bag. The original
Quick Slot stays independent. No other mod's containers or files are changed.

Curios' visibility toggle hides the bag, mounts and their reaching/opening gestures for
both players, while storage and light remain functional. An occupied cosmetic back slot
also suppresses the functional bag's render. Cosmetic slots do not provide bag storage.
The existing player layer renders the bag once; no duplicate Curios renderer is added.

Protocol 3 synchronizes worn source and visibility. A menu binds to the exact source
stack, identity and revision. Normal unequipping is refused while that menu is open;
external replacement, removal or deactivation invalidates it. Curios owns serialization
and its normal configured death/drop behavior.

Curios also binds G by default. The approved gear gesture owns a matching key/modifier
while a bag is worn, before Curios' tick handler can open its inventory. No saved binding
is rewritten. The native inventory button, rebound Curios keys, and no-bag G behavior
remain available. No new mixin was needed.

## Evidence

- Before implementation, the added real-backend test failed because this mod did not
  assign the Curios back slot. The previous code also hard-coded inventory cell 38.
- **13 JUnit tests and all 33 required dedicated-server GameTests pass with Curios.**
  Five new tests cover all tiers, actual slot/capability admission, Curios priority,
  shown/hidden/inactive sources, exact-source replacement/removal, mount transactions,
  open-menu unequip locking and rejecting cosmetic/other slots. The protocol test checks
  that hidden Curios equipment survives the actual registry-backed network codec.
- The final normal build also passes all **28 required GameTests without Curios**
  on the runtime classpath, including the final input-compatibility helper. The 13
  JUnit results remain green. The reused test world logs its expected missing-Curios
  warning after the earlier optional-mod run; startup and every required test pass.
- [Real two-client checks](curios-network.json) equip every tier using native Curios menu
  clicks, compare the server and remote snapshots, use B and G, shift-click storage,
  replace an open bag, exercise the native render toggle, and compare armor/no-armor
  views. They also verify mounted torch light, light removal on moving it into ordinary
  storage, exact save/relog, leaving/re-entering tracking range, full-capacity atomic
  refusal and fallback to the untouched chest bag.
- [No-Curios client/server evidence](curios-absent.json) verifies startup, normal bag
  rendering, worn B opening, real item transfers and hand-carried right-click opening.
  The check also omits Luminance. The original seven profile jars are backed up and
  restored byte-for-byte after this run.
- The first programmatic G test did not cover Curios' competing click handler. A later
  [real X11 G press reproduced the conflict](curios-key-before.json). The final keyboard
  check exercises the actual shared input path, scroll/release swap, physical B, and
  native Curios G opening without a backpack. This corrects that harness blind spot.
- The reusable keyboard gate is `curios_keys_test.py`, using `native_keys.py` on X11.
  Strict mypy passes for all three Curios verification scripts. The reused test clients are muted; test code,
  evidence and external API jars are excluded from the production artifact.
- Final host-process inspection confirms no Minecraft test clients or servers remain.
  The temporary absence-test profile changes have been restored; production is untouched.

The Curios API is used through its documented [optional item capability](https://docs.illusivesoulworks.com/curios/items/curio-creation)
and [player slot assignment](https://docs.illusivesoulworks.com/curios/slots/entity-register),
checked against the exact installed source and binary. Full Curios classes are used only
by development fixtures for native UI operations; production compiles against its API jar.

## Remaining release gates

This is the Curios equipment/access integration. Sophisticated Backpacks migration,
original Quick Slot dynamic lighting, final armor/elytra/cape poses, and the complete
death/travel/shader matrix remain separate gates. Curios' standard death policy is not
overridden here; a full death/keepInventory/Vanishing acceptance run is still required
before release. Existing Refined Tools unused model warnings remain in the test profiles.
No live pack, production server, tag, push or release is authorized by this work.
