# Released and deployed — 2026-09-18

Backpacks+ 0.1.0 and Quick Slot 0.1.0 are published and deployed together in client/server
pack **1.38.0**, under Rusty's explicit release and server-local snapshot approval.

## Artifacts

| Release | Source commit | Jar SHA-256 |
|---|---|---|
| [minecraft-quickslot 0.1.0](https://github.com/the-rusty-shackleford/minecraft-quickslot/releases/tag/v0.1.0) | `c2b71c9f00ec65521594420ca05170be8f40e62f` | `2515666d2f299fbec846e74fc1f02db7318cef8cf1bde6bd28b6fa4a832b8539` |
| [minecraft-backpacks-plus 0.1.0](https://github.com/the-rusty-shackleford/minecraft-backpacks-plus/releases/tag/v0.1.0) | `51a13fc1f867c7c80f591e1f99b902b723b61cb6` | `d1ff72ac63e48ed72389a5ff4074672928adc80809a86bbdb5ba325c96fba89b` |

Published jars were downloaded and matched to the clean-build SHA-1/SHA-512 values.
Both real mrpack archives were inspected: exact download paths, URLs, hashes, sizes and
client/server selection match the source. The only changed override is the client
Entity Culling vehicle tick whitelist; all other override bytes are unchanged.

- Client pack SHA-1: `507b009d846b1b35318ce80a6906f890aaecfefc`.
- Server pack SHA-1: `c7c0beb774befeb161cce423c8aa9ad25ff6a68a`.

## Cutover and verification

- Fresh RCON confirmed zero players before shutdown. The stopped world, installed mods
  and current pack files have a private server-local rollback snapshot. The Hub's old
  pack source and archives are also retained server-side. No fresh player saves were
  exported; the operator first passed a synthetic native-NBT preflight on the server.
- All four fresh saves passed native NBT round-trip and source/output checksum checks.
  Two populated Stowed holsters moved to Quick Slot with raw item components retained.
  Every source hash was rechecked before installation. Prepared current saves also became
  the fallback saves; original current/fallback files remain in the rollback snapshot.
- Stowed, Sophisticated Backpacks and unused Sophisticated Core are absent from the
  running server and both new pack lists. SB SavedData was archived outside the active
  world. Per the approved policy, old bags and contents receive no replacements or
  recovery items. Native loading discards unknown bag stacks when saved players/chunks
  are loaded; unloaded region bytes are not eagerly rewritten.
- Curios, Roofs and Dusty Decorations remain installed. Both production mod hashes match
  the releases. Mod Hub reports the server matches the published pack. RCON reports
  **20 TPS** after startup; there were no players connected during this health check.
- The existing stopped Prism profile was updated, including its previously older
  vehicle/Luminance jars and the pack's existing disabled-mod entries. All 113 published
  mod-directory files (including disabled files) match their hashes. Personal options,
  resource packs and local saves were not changed. The personal client was not launched;
  runtime acceptance used the separate full-pack two-client fixture.
- All owned test clients/server exited, their original isolated-profile files were
  restored, and the private off-screen display shut down. No desktop mouse grab occurred.

The final server emits the expected missing-mod notices for the three retirements.
Existing Homesteads/Subwild recipe errors, Tectonic resource-condition errors and optional
client-class mixin warnings also appear in the previous pre-cutover startup log. They are
not new Quick Slot/Backpacks+ failures. No new errors name either released mod.

See [release validation](release-readiness.md) for the 39 JUnit/55 GameTest results,
real two-client tests, actual shader/animation captures, and limits. Physical gamepad
hardware and the previously deferred 4–8-player exploration check remain untested.
Live player login after cutover has not been observed; preserve this distinction from
native copied-save checks and the completed synthetic lifecycle/registry rehearsal.
