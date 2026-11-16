# Android TV screensaver

[Русский](README.ru.md) | [Project overview](../README.md)

A native screensaver and manual preview with clocks, charts, server temperature, disk and network metrics. Optional rates appear when enabled in the collector. Configure the endpoint and token after installing; no private credentials are embedded.

## Install the release

Download the [signed APK](https://github.com/ginginohf/living-room-server-dashboard/releases/download/v1.0.0/living-room-server-dashboard-1.0.0.apk), allow installation from your chosen file manager on the TV, then open the APK. Alternatively: `adb install living-room-server-dashboard-1.0.0.apk`. A metrics collector must run separately on your server; the APK is only the client.

This uses a release signing key, not a debug key. Android cannot update a debug-signed installation with the same application ID using a different certificate: save your endpoint/token, uninstall that debug build, then install the release. Future releases signed with this key can use `adb install -r`.

## Build and install

Requires JDK 17 and Android SDK 34. Set JAVA_HOME and ANDROID_HOME. The Gradle 8.9 wrapper downloads its distribution and verifies the configured SHA-256.

From the repository root:

```sh
cd android
./gradlew --no-daemon assembleDebug lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch the app (currently labeled “Домашний сервер”), enter the collector URL ending in `/metrics` and your `DASHBOARD_TOKEN`. Select the app as the screensaver in Android TV settings; the menu location depends on the device. The app does not replace the launcher. Remote-control activity exits the screensaver.

Use MENU in manual preview to reopen settings. If your remote has no MENU key, clear the app data and configure it again. To uninstall, first choose another system screensaver.

## Data and transport

Settings are stored in private SharedPreferences and app backup is disabled. This does not protect against root access to the television. HTTP is supported for a trusted LAN; use HTTPS to protect the token in transit. Supply your own signing setup for a release: `assembleRelease` produces an unsigned release APK.

Application ID: `local.homelab.dashboard`. This does not replace a differently identified private installation. Build and lint were checked; deployment on a physical television with this configuration has not been verified. Browser screenshots in the main README represent the separate browser client, not this Android layout.

Release 1.0.0 validation: signed APK installation, settings entry, HTTP metrics, PreviewActivity, DreamService activation and remote-button exit were checked on Android TV 12 ARM64 (1080p). No physical television was used.
