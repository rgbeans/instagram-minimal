# Instagram Minimal Notifications fix

The previous Notifications shortcut sent an Instagram deep link that could return without showing a screen. Both shortcuts now open the same native Notifications fragment as Instagram’s action bar. They use the current account session and the app’s own back stack. The user confirmed that post blocking worked on the previous build. This update changes only the notification entry points.

| Check | Result |
|---|---|
| Build | Meta-signed Instagram 454.0.0.54.77 verified before patching. Eleven patches applied successfully. |
| Policy checks | 86 passed. The rules allow the notification inbox endpoint and module. |
| Native notification route | Passed. Both shortcuts call the new route. The APK contains the same fragment transaction as the native action bar, and all its method references resolve. |
| APK checks | 31 DEX files, 222,145 unique classes, 71 shared card hooks, and two capture hooks. Signature, ZIP integrity, 16 KiB alignment, and licensing files verified. |
| Android 15 emulator | 43 Android guard checks passed. The independent rebuild matched all 31 DEX files in the delivered APK. The app installed and opened to a visible password field with no ANR recovery, relaunch, or captured crash. |

On the first run, the Android launcher stayed foreground after the explicit activity launch. The passing retry used Android’s launcher action and category with the same APK code. Unmodified Instagram reached sign-in after one Wait recovery on the stock control run.

The emulator checks signed-out startup and an isolated Android fixture. Opening the Notifications list with a signed-in account on the phone remains unverified.

The APK can be installed over the prior version. Package `com.instagram.android.minimal`, Instagram build 386014758, ARM64, 163,074,152 bytes.

APK SHA-256 `1db321056b80755348f4369dc610cebec24cbf9ba69ed6b121a5aaf281ce6eba`

Signing certificate SHA-256 `4c7594fdd01305694065a530398610a255678df9bc9f36e0dcebc36288018de4`

[Source commit](https://github.com/rgbeans/instagram-minimal/commit/fef832eae2ca7e6f06841fda934035b63ce250ea) · [Passing Android emulator run](https://github.com/rgbeans/instagram-minimal/actions/runs/42549629923) · [First startup attempt](https://github.com/rgbeans/instagram-minimal/actions/runs/42548204569) · [Unmodified Instagram control](https://github.com/rgbeans/instagram-minimal/actions/runs/42549629713)
