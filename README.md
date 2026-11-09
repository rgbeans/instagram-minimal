# Instagram Minimal

Private experimental Android patch fork of [FeurStagramPaulo](https://github.com/leparlon/FeurStagramPaulo), derived from Feurstagram. The GPLv3 license and upstream NOTICE are preserved, including inside the APK.

A signed ARM64 APK has been built from Meta-signed Instagram **454.0.0.54.77**, build **386014758**. Its package is `com.instagram.android.minimal`, so it installs alongside official Instagram. Android 9 or newer is required.

**Status: experimental; the current build needs signed-in acceptance testing.** See [the test report](docs/TEST_REPORT.md) for tested behavior and limitations.

## Implemented changes

- Force feed, Explore media, Reels and Stories blocking, including preferences restored from older installations.
- Hide Home, Reels and Create navigation, profile media tabs/grids, story trays/highlights, and known shared-media cards.
- Apply fragment guards before drawing, including cached media viewers. Restrict native typeahead result rows to account rows.
- Sanitize Instagram media URLs in incoming intents, outgoing activity launches and embedded web navigation; preserve account and DM links.
- Preserve account-row click and accessibility state when search result views are recycled.
- Open Instagram's Notifications from the bell beside Compose in Messages. Long-press Messages for settings.
- Hide native post, Reel and Story cards in DMs while preserving directly sent photos and ordinary attachments.
- Allow screenshots and recording in the inbox and ordinary chats. Preserve capture protection for disappearing chats and protected media viewers.
- Keep Notes, DMs, direct photo views, account search entry, profiles, and account management available by design.
- Block known nonessential logging/analytics and media endpoints while allowing messaging uploads, read receipts, Notes, user lookup, friendships, push registration, login and security notices.
- Disable upstream promotional/update prompts and omit the updater install permission patch.

UI selectors and module names target the inspected release. Account/server-dependent layouts need live device testing. GraphQL, native networking and Meta-side logging have not been exhaustively audited; zero telemetry is not claimed.

## Build

Tested on Linux with JDK **21.0.13**, Gradle **9.5.0**, Android platform/build-tools **36 / 36.0.0**, Kotlin compiler **2.4.21**, Morphe Desktop **1.11.0** (patcher 1.6.0), and APKEditor **1.4.9**.

1. Install JDK 21 and Android command-line tools; set `JAVA_HOME` and `ANDROID_HOME`. Accept the Android SDK licenses, then install:
   ```sh
   sdkmanager 'platform-tools' 'platforms;android-36' 'build-tools;36.0.0'
   ```
2. Fetch pinned public patching/compiler tools. GitHub Packages credentials are not needed:
   ```sh
   bash scripts/bootstrap-tools.sh
   export KOTLIN_HOME="$PWD/.toolchain/kotlinc"
   ```
3. Restore the private `signing/` backup, or provide your own PKCS12 keystore via `MINIMAL_KEYSTORE`, `MINIMAL_KEY_ALIAS`, and `MINIMAL_KEYSTORE_PASSWORD` or `MINIMAL_KEYSTORE_PASSWORD_FILE`. Keep the signing key out of Git.
4. Supply a stock Instagram APK or split bundle of the supported version:
   ```sh
   ./build.sh instagram.xapk
   ```
   The input verifier checks Instagram/Meta signing certificates before any split merge. Failed patches abort the build. The output is `instagram-minimal.apk`; signing verifies ZIP integrity, APK signatures and 16 KiB ZIP alignment. Set `GRADLE_BIN` to use an existing Gradle launcher.

The build compiles the Java extension with the Android SDK and the Kotlin patches against the pinned public CLI. This replaces the upstream authenticated Maven plugin dependency while retaining the Gradle entry point and wrapper.

## Tests

```sh
./gradlew build
bash scripts/test-apk.sh
# With a booted Android device/emulator connected over ADB:
bash scripts/run-android-tests.sh
```

CI repeats the build, runs the policy and Android guard checks, compares its DEX hashes with the delivered APK, and attempts signed-out navigation on Android 15. See the test report for the latest results.

The policy suite runs without an account. Android guard tests use an isolated fixture app with real Android Views and Intents. Full DMs, Notes, account search, follows, notifications, profile editing, media bypass routes, and login/2FA require a signed-in acceptance pass.

## Attribution

Based on FeurStagramPaulo / Feurstagram. Built using Morphe, with upstream approaches adapted from Piko. See `LICENSE` and `NOTICE`. The patch/extension license does not relicense Meta's proprietary Instagram base application.
