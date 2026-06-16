# Banner Overlay

A companion app for [Ingress Prime](https://ingress.com/) that keeps track of your current [Bannergress](https://bannergress.com/) banner and lets you launch missions without switching apps.

> **Note:** This is a personal fork of [bannergress/banner-overlay](https://github.com/bannergress/banner-overlay) with additional features. See [What's different](#whats-different-from-upstream) below.

---

## What it does

When you're walking a banner in Ingress, you normally have to switch between Ingress and Bannergress to track which mission you're on and find the next one. Banner Overlay eliminates that by showing a small widget on top of Ingress that tracks your progress and lets you launch the next mission in one tap.

## Features

- **Mission counter** — shows your current position in the banner (e.g. 3/18)
- **One-tap mission launch** — opens the next mission directly in Ingress
- **Hide/Show** — temporarily hide the overlay without stopping tracking
- **Notification controls** — move navigation buttons into the persistent notification instead of the on-screen overlay (useful if the overlay gets in the way)
- **Location-based step tracking** — highlights waypoints as you approach them
- **Works with Bannergress links and Intel Mission links**

## What's different from upstream

This fork adds two features not in the original app:

**Hide/Show overlay** — A Hide button in the persistent notification collapses the overlay off-screen while keeping location tracking and progress running. Tap Show to bring it back.

**Notification controls** — An optional setting (in Settings → Use notification controls) that moves the − / Next / + navigation buttons into the persistent notification and auto-hides the on-screen overlay. The overlay stays visible until you tap Start for the first time, so you can use +/− to pick your starting mission before launching.

## Requirements

- Android 8.0 (API 26) or higher
- [Ingress Prime](https://play.google.com/store/apps/details?id=com.nianticproject.ingress) installed
- Overlay permission (prompted on first launch)

## Installation

This app is not on the Play Store. You'll need to build it from source or sideload a release APK.

### Build from source

1. Clone this repo
2. Open in [Android Studio](https://developer.android.com/studio)
3. Connect your Android device with USB debugging enabled
4. Hit Run

### Sideload an APK

Download a release APK from the [Releases](../../releases) page, transfer it to your device, and install it. You may need to enable "Install from unknown sources" in your Android settings.

## How to use

1. Open Bannergress and find a banner you want to walk
2. Tap Share and choose **Banner Overlay** from the share sheet
3. The overlay appears on screen — tap **Start** (or use +/− to pick a starting mission, then tap Start)
4. Banner Overlay opens the first mission in Ingress
5. Complete the mission, then tap **Next** in the overlay (or notification) to advance and launch the next one
6. When you finish the last mission, tap **Close** to stop the session

You can drag the overlay anywhere on screen.

## Settings

Open the app from your launcher to access settings.

| Setting | Description |
|---|---|
| Use location | Enables GPS-based step proximity tracking and remaining distance display in the notification |
| Use notifications | Enables the persistent notification (required for notification controls) |
| Progress | Opens notification channel settings for the progress notification |
| Next step in range | Opens notification channel settings for step proximity alerts |
| Use notification controls | Moves − / Next / + buttons into the notification and hides the on-screen overlay after the first mission is started |
| Copy mission number as passphrase | Copies the current mission number to clipboard for passphrase missions |

## Permissions

| Permission | Why |
|---|---|
| Display over other apps | Required to show the overlay on top of Ingress |
| Location (fine) | Used for step proximity detection and remaining distance calculation |
| Post notifications | Required for the persistent progress notification (Android 13+) |
| Internet | Fetches banner and mission data from the Bannergress API |

## Building

Requirements: JDK 17+, Android Studio Meerkat or later.

```bash
git clone https://github.com/marcthayer/banner-overlay-mt.git
cd banner-overlay-mt
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Credits

Original app by the [Bannergress team](https://github.com/bannergress). This fork adds the hide/show and notification controls features.

## License

[MIT](LICENSE)
