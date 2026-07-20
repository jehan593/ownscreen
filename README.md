# OwnScreen

A fully offline Android screen-time tracker with manual app blocking, driven through
[OwnDroid](https://github.com/BinTianqi/OwnDroid)'s device-owner API. Nord color palette,
Martian Mono Nerd Font, Jetpack Compose UI, home-screen widget.

OwnScreen itself requests **no `INTERNET` permission** — all tracking, storage, and blocking
happen entirely on-device.

## How it works

- **Tracking**: a foreground service recomputes today's per-app foreground time from
  `UsageStatsManager` every 15–300 seconds (configurable), always freshly queried from local
  midnight and gated on the screen actually being on (so locked/screen-off time is never counted
  as usage) — never a stateful counter, so day rollovers and reboots self-heal automatically.
- **Blocking is manual, not automatic**: there are no daily limits. Open an app's detail screen
  and tap **Block** to immediately suspend it via OwnDroid — its icon stays visible, but opening
  it shows Android's built-in "app isn't available" message. A block persists until you
  explicitly unblock it; there's no automatic daily reset.
- **Unblocking has deliberate friction**: tapping **Unblock** doesn't unblock immediately — it
  shows a moderately-hard mental-math problem (not "2 + 2", not something you'd need a
  calculator for either) that has to be solved correctly first. A wrong answer swaps in a new
  problem rather than letting you keep guessing the same one.
- **Widget**: shows today's total and top apps, refreshed by the same service tick (with a
  30-minute AlarmManager fallback in case an aggressive OEM battery manager kills the service).

## One-time setup (required before blocking works)

OwnScreen cannot automate any of this itself — it's a device-level configuration step you do once,
outside the app:

1. **Install OwnDroid** on the same device (F-Droid or a GitHub release from
   [BinTianqi/OwnDroid](https://github.com/BinTianqi/OwnDroid)).
2. **Set OwnDroid as Device Owner.** This only works on a device with no existing Google/work
   account added and no other device-owner app set (a fresh device or one wiped back to that
   state). With USB debugging enabled, run from a computer:
   ```sh
   adb shell dpm set-device-owner com.bintianqi.owndroid/.Receiver
   ```
   This is a real device-management change — read what it does before running it, and know that
   removing a device owner later generally requires a factory reset on most ROMs.
3. **Open OwnDroid's own settings**, enable its API, and set an API key (any string you choose).
4. **Open OwnScreen**, grant Usage Access when prompted — tracking then starts automatically,
   no separate toggle needed. Then go to **Settings** and paste the same key into "OwnDroid API
   key".
5. From the dashboard or Settings' "All apps" list, tap an app → **Manage** → **Block**.

If OwnDroid isn't installed, blocking simply won't take effect (OwnDroid's broadcast receiver
won't be there to act on it) — a banner in Settings flags this.

## Building

This has actually been built and verified: both `./gradlew assembleDebug` and
`./gradlew assembleRelease` succeed. The Gradle wrapper (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`) is included and working, so no regeneration step is needed.

CI and any distributed build ship **release** (`app/build/outputs/apk/release/app-release.apk`) —
R8-minified and resource-shrunk, signed with the same committed `debug.keystore` as the debug
build (see `app/build.gradle.kts`) so in-place updates via Obtainium keep working. Use
`assembleDebug` (`app/build/outputs/apk/debug/app-debug.apk`) for local iteration only.

1. Open this `ownscreen/` folder in Android Studio (it will pick up the existing wrapper and
   sync automatically), **or** from a terminal with JDK 17 and the Android SDK installed:
   ```sh
   # local.properties needs sdk.dir pointed at your Android SDK if Android Studio hasn't
   # already created one for you, e.g.:
   echo "sdk.dir=/path/to/Android/sdk" > local.properties
   ./gradlew assembleRelease
   ```
2. Install/run on a device or emulator running Android 8.0 (API 26) or newer:
   `adb install app/build/outputs/apk/release/app-release.apk`. OwnDroid/device-owner setup only
   really makes sense on a physical device you control — not required just to try the
   tracking/dashboard/widget UI.

### Fonts

Martian Mono Nerd Font (`.ttf`, Regular + Medium — Bold isn't used anywhere and was dropped) is
bundled under `app/src/main/res/font/`, Latin-subsetted from the full
[Nerd Fonts](https://github.com/ryanoasis/nerd-fonts) release down to just the glyphs the app
actually renders (license in `MARTIAN_MONO_LICENSE.txt` at the project root). Home-screen widget
text falls back to the system monospace font — RemoteViews custom-font support is inconsistent
across launchers, so the in-app screens are where you'll see the real Martian Mono.

## Permissions

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` | Special app-op (granted via Settings, not a runtime dialog) needed to read per-app usage. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | The near-real-time tracking loop runs as a foreground service. |
| `POST_NOTIFICATIONS` | For the ongoing tracking notification (Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | Resumes tracking after a reboot if Usage Access has already been granted. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Backs an opt-in Settings button only — never requested silently. |
| `<queries>` scoped to `com.bintianqi.owndroid` | So OwnScreen can detect whether OwnDroid is installed, without the broader `QUERY_ALL_PACKAGES` permission. |

Deliberately not requested: `INTERNET`, `QUERY_ALL_PACKAGES`, any exact-alarm permission, `WAKE_LOCK`.
