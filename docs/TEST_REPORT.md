# Instagram Minimal — chat update test report

This APK updates the previous installation using the same package and signing certificate.

| Check | Result |
|---|---|
| Build | Verified Meta-signed Instagram 454.0.0.54.77 before patching. Built with JDK 21.0.13, Gradle 9.5.0, Android SDK 36. Eleven patches applied; none failed. |
| Policy suite | 86 passed. |
| APK checks | 31 DEX files, 222,145 unique classes, all injected extension calls resolve. ZIP integrity, APK signature and 16 KiB alignment passed. |
| Notifications and chat hooks | The APK declares the `instagram://news` route and contains 71 chat card hooks and two secure-window hooks. |
| Licensing | Upstream LICENSE and NOTICE match the source files byte for byte. |
| Android 15 emulator | 43 Android guard checks passed. All 31 DEX hashes in the independently rebuilt APK match the delivered APK. The rebuild installed and reached the password field after selecting Wait once on a cold-start ANR dialog. The earlier stock control required the same recovery. |

The Android fixture exercises the Notifications bell placement and intent, post card hiding and recycling, and capture flags for ordinary chats and disappearing content. The Instagram startup check does not use account credentials. The live notifications list, server-provided chat layouts, capture on a physical phone, and other signed-in features still need a device check. Known endpoint rules were exercised; total telemetry elimination was not measured.

Package `com.instagram.android.minimal` · Instagram 454.0.0.54.77 / 386014758 · ARM64 · 163,074,152 bytes.

APK SHA-256: `887bb400b050d5502a685793177b8fff9454541e03042400cb32470964c3a975`

Signing certificate SHA-256: `4c7594fdd01305694065a530398610a255678df9bc9f36e0dcebc36288018de4`

[Source commit](https://github.com/rgbeans/instagram-minimal/commit/5f191ba6f10b4ce3341e2597f0200a3492467e48) · [Android runtime evidence](https://github.com/rgbeans/instagram-minimal/actions/runs/42540636765)
