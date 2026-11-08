# Instagram Minimal — test report

Updated November 8, 2026. **Signed APK delivered; signed-out UI verified. Authenticated features and physical ARM64 operation remain unverified.**

| Check | Result |
|---|---|
| Build and patching | Passed with JDK 21.0.13, Gradle 9.5.0, Android SDK 36; **9 patches applied, 0 failed** |
| Policy regression suite | **69 passed** |
| Android guard fixture | **27 passed** on Android 15/API 35, including account-only result filtering, recycled-row interaction/accessibility, malformed intent extras, media visibility, Notes/DM-photo visibility and mandatory preferences |
| APK structure | Passed: 31 DEX files, 222,132 unique classes, no duplicate classes, injected calls resolve; network/intent/fragment hooks present |
| Integrity and signing | ZIP CRC, APK signature and 16 KiB ZIP alignment passed; original signing certificate retained |
| CI rebuild comparison | **All 31 DEX hashes exactly match the delivered APK.** CI uses a separate disposable signing key |
| Installation and signed-out navigation | Matching rebuild installed, displayed the welcome UI, and navigated to a visible password field without entering credentials; no native crash captured |
| Cold startup | **One ANR dialog required selecting Wait.** Unmodified stock Instagram required the same recovery and then reached the login form. This is recorded as a limitation, not an ANR-free startup pass |
| Licensing | Upstream LICENSE/NOTICE preserved byte-for-byte inside the APK |
| Telemetry | Known endpoint rules tested; comprehensive traffic/native-network elimination is not verified |

The Android tests use an isolated fixture app with real Views and Intents. They do not establish that every server-dependent Instagram layout is filtered correctly.

The earlier Android 11 image crashed in native startup for both stock and patched Instagram. Android 15 avoids that captured native crash and provides a usable signed-out baseline after the recorded Wait recovery. Tests used an x86_64 emulator with ARM64 translation; no physical ARM64 device was connected.

## Changes in this build

- Recycled search rows retain native click, long-click and accessibility state when they become account results.
- Known blocked media URLs are sanitized before foreign extras are read, closing the malformed-extras bypass.

## Still unverified

Physical-device startup; authenticated login/2FA; DMs and attachments; Notes; account search results; follows/following; notifications/push; profile/account editing; all media routes through DMs, profiles, search and deep links. Complete telemetry elimination is not claimed.

## Evidence

- [69 policy checks, 27 Android checks, exact DEX comparison and patched login navigation](https://github.com/rgbeans/instagram-minimal/actions/runs/42522125392)
- [Unmodified stock control with the same cold-start recovery](https://github.com/rgbeans/instagram-minimal/actions/runs/42522125504)

Both runs recorded one Wait recovery, visible login UI, and no native crash. No account credentials were entered. Evidence artifacts are retained for seven days.

## APK identity

Package: `com.instagram.android.minimal` · Instagram 454.0.0.54.77/build 386014758 · ARM64 · Android 9+ · 162,771,048 bytes.

APK SHA-256:
`6668675a3afcd823a75110ec3de8813598957496ee5448d87735c2f6d14bd9d2`

Signing certificate SHA-256:
`4c7594fdd01305694065a530398610a255678df9bc9f36e0dcebc36288018de4`

[Application and test source](https://github.com/rgbeans/instagram-minimal/commit/731ac4d864be279846cb79b724e3236d6c33f14e). The private signing backup is unchanged and is not committed to Git.
