# Legacy retirement rehearsal — 2026-09-18

Rusty explicitly chose to discard existing Sophisticated Backpacks bags and their contents
(D-0018/D-0019), with no free replacements. New Backpacks+ bags require crafting. Stowed
holsters remain separate and will move into Quick Slot with all their item data intact.
Release and production changes remain HELD.

The private approved local audit and four-player copied-save conversion completed. Two
holster stacks were preserved; no real inventory data enters this repository. The remote
read-only world scan completed across 5,187 region files and 1,435,822 chunks. Its 44
matching chunks contained legacy bag references in entities; no matching terrain chunks
were found. This running-server audit is not the stopped-server backup required at release.

`LegacyRetirement` is a development-only native server restart fixture. It seeds actual
SB bags in inventory, armor, Curios, a chest, armor-stand equipment and a dropped item.
The same saved world then loads without SB or Core. All usable bags disappear while
unrelated inventory/container/equipment items and the named/damaged Quick Slot stack remain.
The first over-strict assertion counted an already-discarded empty entity as a surviving
bag; a replay of the original saved world proved it was marked removed and contained air.
The final test rejects any live or recoverable legacy item. See `legacy-retirement.json`.

Replay only in a disposable `run/legacy-retirement` world with loopback binding:

1. Stage the exact installed SB/Core/Curios jars; run `runLegacyServer -PlegacyMode=seed`.
2. Check `legacy-seed-result.json`, keep a backup, then remove only that fixture's SB/Core jars.
3. Run `runLegacyServer -PlegacyMode=verify`; require `verified: true` in its result.

Both runs shut down automatically. The Gradle task fails on missing or unsuccessful
results because a server exception alone can still yield a successful launcher exit.
Curios stays installed. Installed client and server metadata show no other dependency
on Core; recheck before release. No production or personal-profile files were changed.

The complete copied-save utility instructions, checksums/refusal rules, fresh-backup
requirement and held deployment procedure live in the sibling Quick Slot store:
`knowledge/validation/legacy-cutover.md`. Native loading clears invalid stacks as their
players/chunks are loaded; this does not eagerly rewrite every unloaded region.
