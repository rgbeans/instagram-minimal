# Instagram Minimal

Experimental Android Instagram patch project based on [FeurStagramPaulo](https://github.com/leparlon/FeurStagramPaulo). Upstream license and NOTICE are retained.

## Status — not release-ready

The upstream Java extension, Kotlin bytecode patch modules, and text build scaffolding have been imported. The modified configuration forces feed, Explore, Reels and Stories blocking; Notes and Instants are kept visible; DM landing is the default; Reels and Create tabs are hidden; and upstream update prompts are disabled. Existing network blocking covers several telemetry and commerce endpoints.

**This repository does not contain an installable APK, and none of the behaviors has been device-tested.**

## Remaining blockers

1. Import `gradle/wrapper/gradle-wrapper.jar` (binary file omitted from connector-based import), or use a local compatible Gradle installation.
2. Supply a compatible legally obtained Instagram APK or APK bundle for patching. The proprietary Instagram APK is not included.
3. Install JDK 21, Android SDK, and the matching Morphe CLI and patcher dependencies; configure GitHub Packages access where required.
4. Run `./build.sh path/to/instagram.apk`, inspect `build/patch-report.json`, and sign the APK with your own key.
5. Verify DMs, Notes, account search, notifications, follows, profiles, login and deep links on a device.
6. Implement and verify profile post-grid hiding, account-only search results, media opened from DMs, notification links, and content shared via deep links.
7. Audit network calls to identify any nonessential telemetry not covered by existing rules. Complete elimination of Meta-side logging cannot be guaranteed.

## Privacy

The upstream project blocks known analytics endpoints, but Instagram still communicates with Meta to provide account and messaging functionality. Do not assume zero telemetry or zero tracking.

## Upstream attribution

Based on FeurStagramPaulo / Feurstagram. See `LICENSE` and `NOTICE`.
