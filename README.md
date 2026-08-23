# Home Player for Android

![License](https://img.shields.io/badge/license-MIT-green.svg)
![Android](https://img.shields.io/badge/platform-Android-3DDC84.svg)
![Min SDK](https://img.shields.io/badge/min%20SDK-26-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.01.01-4285F4.svg)
![Media3](https://img.shields.io/badge/Media3-1.5.1-orange.svg)
![Release](https://img.shields.io/badge/release-v1.0.0-informational.svg)

Home Player is a native Android client for a Web IPTV backend. It focuses on live TV playback, channel favorites and groups, NAS video browsing, and a clean player experience for phones.

> **⚠️ Not standalone**: Home Player is only a **frontend client**. It is useless without the [Web IPTV](https://github.com/zyongyuer1227/web_iptv) server running on your own device/NAS — it does not fetch or manage any live sources by itself. Downloading it alone has no meaning; please deploy and start the Web IPTV server **before** installing the app.

> **Companion app**: Home Player pairs with the open-source [Web IPTV](https://github.com/zyongyuer1227/web_iptv) server. Both are developed by the same author but **may not always be released in sync** — please refer to the author's announcements on the [Web IPTV Releases](https://github.com/zyongyuer1227/web_iptv/releases) page for release information.

## Features

- Multiple backend server profiles with health checks.
- Live TV channel browsing, search, source filtering, favorites, custom groups, and EPG lookup.
- NAS video browsing with quality selection and next-episode playback.
- Media3/ExoPlayer playback with HLS, direct stream, relay, TS/fMP4 fallback, and reconnect handling.
- Portrait playback, landscape fullscreen mode, status-bar safe layout, mute, seek, brightness and volume gestures.
- Local troubleshooting log with sensitive URL/token/path redaction.

## Requirements

- Android 8.0 or later.
- A compatible Web IPTV backend.
- Android Studio or JDK 17+ for building from source.
- Android SDK with compile SDK 36 installed.

## Build

Clone the repository and open it in Android Studio, or build from the command line:

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release Build

Release signing is intentionally configured through local-only properties. Copy `local.properties.example` to `local.properties`, update the values for your machine and keystore, then run:

```bash
./gradlew :app:assembleRelease
```

The release APK is generated at:

```text
app/build/outputs/apk/release/app-release.apk
```

Do not commit `local.properties`, keystores, or generated APK files. Publish APKs as release assets.

## Backend

The app expects a backend that exposes endpoints such as:

- `GET /api/health`
- `GET /api/sources`
- `GET /api/channels`
- `GET /api/channels/{id}/streaminfo`
- `GET /api/favorites`
- `POST /api/favorites`
- `DELETE /api/favorites/{id}`
- `GET /api/groups`
- `PUT /api/groups/{id}/items`
- `GET /api/nas/{sourceId}/browse`
- `GET /api/nas/{sourceId}/streaminfo`

The server can be configured inside the app using an address such as `http://192.168.1.100:8081` or a reverse-proxy path.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
