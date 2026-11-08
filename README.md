# Instagram Minimal (work in progress)

An experimental patch overlay for [FeurStagramPaulo](https://github.com/leparlon/FeurStagramPaulo).

## Current status
**Not a buildable fork yet.** This repository currently contains modified versions of two upstream Java files. They must be integrated with the upstream patcher and tested against a compatible Instagram APK.

## Target behavior
- Keep DMs, Notes, notifications, account search, follows, profiles, and settings.
- Remove the home feed, Reels, Stories, Explore recommendations, and post grids.
- Block entry points and deep links to posts, Reels, and Stories, including those arriving in messages, without breaking message delivery.
- Block nonessential analytics and telemetry while preserving network traffic essential to authentication, notifications and messaging.

## Initial modifications
- `Config.java`: fixed-on feed/Explore/Reels/Stories restrictions; Notes enabled; DMs landing; automatic upstream update checks disabled.
- `Block.java`: expanded Story network endpoint filters.

## Known gaps and cautions
- Search-account results without post grids **not implemented**.
- Post/Reel/Story deep-link handling **not implemented**.
- Complete upstream sources and build scripts **not imported**.
- No build, emulator run, or device validation.
- Instagram's endpoint and view structures change; a network-only approach cannot guarantee zero media surfaces.
- Existing settings UI may still display toggles that no longer change behavior.
- This is an unofficial Instagram modification and may violate Meta's terms.

## Source provenance
The two files are derived from the upstream repository. Preserve upstream licensing and notices when importing the full tree.
