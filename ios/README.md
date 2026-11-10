# Instagram Minimal for iOS

This native preview is an arm64 dynamic library for a separately supplied iOS copy of Instagram. The build targets iOS 16 and later. It needs an iPhone acceptance test before it can be treated as a replacement for the Android edition.

| Surface | Preview behavior |
| --- | --- |
| Home | Returns an empty feed for the known list data source. Keeps the Home header so Instagram's Notifications and Messages controls remain available where Instagram places them. |
| Navigation | Filters Reels and Create from the known tab bar and disables the known swipe-to-tab feature. |
| Search | Hides the Explore grid and filters known media models from typeahead. Account rows and the search bar pass through. |
| Profiles and chats | Hides the known profile photo-grid cell and two known shared-media cell classes. Ordinary DM attachments and Notes are outside these hooks. |
| Media viewers and links | Rejects known post, Reel and Story viewer controllers when pushed or presented through the hooked UIKit methods. Filters media URLs through known app-delegate and in-app link methods. |

The private class names come from public iOS references. Some refer to older releases. A missing class or an incompatible method signature leaves that hook inactive. Profile highlights, newer DM card layouts, account-only search, and scene-based link handling require testing and may need additional hooks. This preview does not include an iOS analytics or telemetry filter. In-app Notifications and background push need separate checks after signing. The Android screenshot and recording patch has not been ported.

## Build

On a Mac with Xcode and the iOS SDK installed, run these commands from the repository root.

```bash
./ios/test.sh
./ios/build.sh
```

The output is `ios/build/InstagramMinimal.dylib`. For a cross-build, set `SDKROOT` and `IOS_CLANG` to absolute paths, and set `IOS_LINKER` when using a separate Mach-O linker. The preview was cross-built with Clang and LLD 19.1.7 and the iPhoneOS 16.2 SDK.

The dylib must be packaged with a compatible, user-supplied Instagram IPA and signed for the device. SCInsta's documented Instagram 418.2.0 target is a starting point for testing this preview. The source tree and preview binary contain no Instagram IPA or signing keys.

## Test status

The 39 host-side tests check media-route filtering and preservation of account, login, inbox, notification and ordinary profile URLs. An arm64 iOS dylib was built and inspected. No device installation or signed-in acceptance test has been performed.

## Attribution

The Home list, Explore grid, and navigation hooks are adapted from [SCInsta](https://github.com/SoCuul/SCInsta/tree/7bb71807190518ed45d21c716704c38e45a19253), copyright its contributors and based on [BHInstagram](https://github.com/BandarHL/BHInstagram) by BandarHL. The iOS preview is distributed under the repository's [GPLv3 license](../LICENSE). Class names and selectors for older surfaces were checked against [Instagram iOS headers](https://github.com/jengel3/Instagram-iOS-Headers/tree/97c84c0807188dd7d5f9dfdba55fa8e7205d540e).
