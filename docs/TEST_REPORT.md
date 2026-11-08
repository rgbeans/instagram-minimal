# Instagram Minimal — test report

Build date: November 7, 2026 (America/Toronto). **Experimental signed APK; full app operation is not verified.**

| Check | Result |
|---|---|
| Stock input | Instagram 454.0.0.54.77, build 386014758; Instagram/Meta signer certificates verified before merging |
| Gradle build | Passed with JDK 21, Gradle 9.5.0 and Android SDK 36, including an offline rebuild |
| Policy regression tests | **69 passed**: content URLs/endpoints blocked; messaging, Notes, account search, uploads, follows, login and push endpoints allowed |
| Android guard fixture | **18 passed** on Android 11/API 30: media cards/tabs hidden, Notes and direct-photo views visible, intent redirection, old preference enforcement |
| Patching | **9 applied, 0 failed**; 748 incoming intent reads guarded |
| APK DEX structure | Passed: 31 DEX files, 222,132 unique classes, no duplicates; injected extension calls resolve; required network and fragment hooks present |
| Package integrity | ZIP CRC check, APK v3 signature verification and 16 KiB ZIP alignment passed; upstream LICENSE/NOTICE retained byte-for-byte |
| Full APK install/launch | **Inconclusive.** The software emulator timed out during installation, left the package frozen, and showed System UI ANRs; Android refused to start the app process |
| Live account features | **Not verified:** login/2FA, DMs, Notes, notifications/push, account search results, follows, profile/account editing and every media bypass route |
| Telemetry | Known rules tested; no comprehensive decrypted traffic/native-network audit. Complete telemetry elimination is **not claimed** |

The emulator had no KVM acceleration and used ARM64 translation. Its isolated guard test app ran successfully; that does not establish that the full Instagram APK launches or works. The failed full-app launch is not reported as a pass, and no normal login screen was observed.

Package: `com.instagram.android.minimal` · ARM64 · Android 9+ · 162,771,048 bytes.

APK SHA-256:
`7ea8d30760195a5e77ad9706b41e8839259baa00d373799b55fccd824af550f3`

Signing certificate SHA-256:
`4c7594fdd01305694065a530398610a255678df9bc9f36e0dcebc36288018de4`

Source: https://github.com/rgbeans/instagram-minimal
