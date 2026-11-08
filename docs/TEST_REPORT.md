# Instagram Minimal — test report

Build date: November 7, 2026 (America/Toronto). Verification updated November 8, 2026 (UTC). **Experimental signed APK; real ARM64 startup and signed-in operation are not verified.**

| Check | Result |
|---|---|
| Stock input | Instagram 454.0.0.54.77, build 386014758; Instagram/Meta signer certificates verified before merging |
| Gradle build | Passed with JDK 21.0.13, Gradle 9.5.0 and Android SDK 36; repeated in GitHub Actions |
| Policy regression tests | **69 passed**: blocked content URLs/endpoints; allowed messaging, Notes, account search, uploads, follows, login and push endpoints |
| Android guard fixture | **18 passed**, repeated on KVM-accelerated Android 11/API 30: media cards/tabs hidden, Notes and direct-photo views visible, intent redirection, old preference enforcement |
| Patching | **9 applied, 0 failed**; 748 incoming intent reads guarded |
| APK DEX structure | Passed: 31 DEX files, 222,132 unique classes, no duplicates; injected extension calls resolve; required network and fragment hooks present |
| Independent CI rebuild | **All 31 DEX SHA-256 hashes exactly match the delivered APK.** CI uses a separate disposable signing key; the delivered signing key is unchanged |
| Package integrity | ZIP CRC check, APK v3 signature verification and 16 KiB ZIP alignment passed; upstream LICENSE/NOTICE retained byte-for-byte |
| Full APK installation | **Passed for the matching CI rebuild** on API 30/x86_64 with ARM64 translation |
| Full APK launch | **Failed on this emulator:** native SIGSEGV/SEGV_ACCERR in the Lacrima_startup thread. No normal login screen was verified |
| Unmodified stock control | **Also failed** on the same emulator configuration: Meta-signed, unpatched base plus density split installed successfully, then crashed with SIGSEGV/SEGV_ACCERR in Lacrima_startup |
| Live account features | **Not verified:** physical ARM64 startup, login/2FA, DMs, Notes, notifications/push, account search results, follows, profile/account editing and all media bypass routes |
| Telemetry | Known rules tested; no comprehensive decrypted traffic/native-network audit. Complete telemetry elimination is **not claimed** |

## Runtime evidence and scope

The emulator booted after build intermediates were removed to provide sufficient disk space. It used Android 11/API 30, x86_64, KVM and libndk_translation 0.2.2 for the ARM64 app. The patched APK installed successfully, but its process crashed during native startup. The native tombstone contained an unresolved frame at PC 0; no actionable native backtrace or Java verification error was captured.

A separate control installed the original Meta-signed APK and its density split without merging, resigning or applying patches. It also crashed in Lacrima_startup with the same signal/code. This establishes that this emulator does not provide a working baseline for the stock release. It does **not** prove that the delivered APK works on a physical ARM64 phone or that the patches introduce no additional regressions.

The 18 guard checks use an isolated fixture app with real Android Views and Intents. They do not exercise authenticated Instagram/server layouts. All live account features and complete media-route coverage still require acceptance testing on a working device.

- [Matching rebuild, 69 policy checks, 18 Android checks, installation and failed startup](https://github.com/rgbeans/instagram-minimal/actions/runs/42466963863)
- [Native startup log inspection](https://github.com/rgbeans/instagram-minimal/actions/runs/42467859987)
- [Unmodified stock control and native crash evidence](https://github.com/rgbeans/instagram-minimal/actions/runs/42468023249)

The stock-control workflow completed its diagnostic collection successfully; its recorded application startup result is **failed**, not passed. CI evidence artifacts are retained for seven days.

## Delivered APK identity

Package: `com.instagram.android.minimal` · ARM64 · Android 9+ · 162,771,048 bytes.

APK SHA-256:
`7ea8d30760195a5e77ad9706b41e8839259baa00d373799b55fccd824af550f3`

Signing certificate SHA-256:
`4c7594fdd01305694065a530398610a255678df9bc9f36e0dcebc36288018de4`

The signing backup remains private and is not committed to Git.

Source: https://github.com/rgbeans/instagram-minimal

Application source commit: https://github.com/rgbeans/instagram-minimal/commit/422d21511343228f72398961c24a8e6d523ee66a
