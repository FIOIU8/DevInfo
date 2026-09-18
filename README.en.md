# DevInfo

<p align="center">
  <a href="README.md">简体中文</a> |
  <a href="README.en.md">English</a> |
  <a href="README.ja.md">日本語</a>
</p>

| Material 3 Device information | Material 3 Details | Material 3 Settings |
| --- | --- | --- |
| <img src="md3_info.png" width="220" alt="Material 3 device information"> | <img src="md3_info_details.png" width="220" alt="Material 3 information details"> | <img src="md3_settings.png" width="220" alt="Material 3 settings"> |

| Miuix Device information | Miuix Details | Miuix Settings |
| --- | --- | --- |
| <img src="miuix_info.png" width="220" alt="Miuix device information"> | <img src="miuix_info_details.png" width="220" alt="Miuix information details"> | <img src="miuix_settings.png" width="220" alt="Miuix settings"> |

[![License](https://img.shields.io/github/license/FIOIU8/DevInfo?color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Release](https://img.shields.io/github/v/release/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

DevInfo is an Android device information viewer built with Kotlin and Jetpack Compose. It collects device data locally, provides a live hardware overview and categorized details, supports theme and language settings, and can export the information as a Magisk/KernelSU ZIP module.

## Features

### Device information and live overview

- Details are grouped into nine categories: device, identifiers, system, locale, display, storage, battery, network, and apps.
- Fields include Android version, SDK, ABI, kernel, security patch, display, memory/storage, battery, network, sensors, and app information.
- The overview provides static information, pull-to-refresh, and live metrics. CPU/GPU, memory, storage, battery, motion, brightness, storage read speed, and Wi-Fi signal are shown when supported.
- CPU statistics may be restricted in normal mode. Optional Root mode shows a risk notice first and only requests root permission after you confirm; when it fails, it explains the reason and offers in-app authorization help.

### Export and updates

- Export a ZIP-based Magisk/KernelSU module through the system file picker; the exported content is a fixed minimal set (brand, model, device, product name, Android version/SDK, and ABI lists).
- The module version and versionCode come from the app version. Flashing a module writes ro.product.* system properties and can change system behavior.
- Official builds can check GitHub Releases and show release notes. Network failures are reported as an error state.

### UI and localization

- Material 3 and Miuix UI styles.
- System, light, dark, and dynamic-color modes with theme colors, palette styles, page scale, blur, and floating navigation settings.
- Simplified Chinese, English, Japanese, and custom BCP-47 locale tags.
- Predictive back gestures on Android 14+, splash screens, and app-wide pull-to-refresh.

## Data availability and privacy

Readable fields depend on the manufacturer, Android version, permission policy, and `/proc`/`/sys` restrictions. Individual failures do not block the rest of the screen, and unavailable CPU utilization is never presented as `0%`.

The app has no custom backend and does not upload device information. Update checks contact the GitHub API; inspect exported ZIP contents before sharing or flashing. Root mode is optional.

The manifest declares `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, and `NFC`; NFC is an optional device feature. No Bluetooth permission is currently declared.

## Download and build

### Downloads

- Official releases: [Releases](https://github.com/FIOIU8/DevInfo/releases).
- Test builds: download workflow Artifacts from [Actions](https://github.com/FIOIU8/DevInfo/actions).

### Requirements

- Android Studio and Android SDK 37.
- JDK 21; `app`/`feature-main` target JVM 21, while `core`/`data`/`ui` remain on JVM 11.
- Minimum supported Android version: Android 13 (API 33).

### Local build

```bash
# Windows
gradlew.bat testDebugUnitTest
gradlew.bat ktlintCheck
gradlew.bat lintDebug
gradlew.bat assembleDebug

# Linux/macOS: replace gradlew.bat with ./gradlew
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Technology and structure

| Technology | Current configuration |
| --- | --- |
| Kotlin | 2.4.10 |
| Jetpack Compose | BOM 2026.08.00 |
| Miuix | 0.9.4-rc01 |
| Android Gradle Plugin | 9.1.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 33 (Android 13) |
| Java target / toolchain | `app`/`feature-main`: JVM 21; `core`/`data`/`ui`: JVM 11; JDK 21 |

```text
DevInfo/
├── app/                 # Android entry point, manifest, resources, build
├── core/                # Framework-independent models and CPU parsing
├── data/                # Collection, monitoring, preferences, updates, export
├── feature-main/        # Main, overview, details, settings, about
├── ui/                  # Compose/Miuix themes and shared components
├── .github/workflows/   # CI, builds, and releases
├── gradle/libs.versions.toml
└── LICENSE
```

`MainActivity` assembles dependencies. `MainViewModel` coordinates loading, refresh, and foreground monitoring, while screens collect `StateFlow`. Theme and language preferences use SharedPreferences; the update cache uses DataStore with a legacy SharedPreferences compatibility path. Page state is managed by `MainScreen`; Navigation Compose is not used.

## CI and releases

- `build.yml` runs tests, ktlint, Android lint, and builds for pushes to `main`, `dev`, or `test`, pull requests, and manual runs.
- Manual builds accept a version name and `debug`/`release` signing type and upload APK/build information Artifacts.
- `release.yml` supports pushes to `main`, `vMAJOR.MINOR.PATCH` tags, and manual releases from `main`.
- Releases pass tests, ktlint, and lint before using `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` to create a signed APK and Draft Release.

## Contributing

Read the implementation and tests before changing the relevant module, then run the applicable Gradle checks. Commit messages use `English prefix: Chinese description`, for example `docs: 更新开发文档`. Report issues through [Issues](https://github.com/FIOIU8/DevInfo/issues).

## License and links

This project is released under [GPL-3.0](LICENSE).

- [Source code](https://github.com/FIOIU8/DevInfo)
- [Releases](https://github.com/FIOIU8/DevInfo/releases)
- [Actions](https://github.com/FIOIU8/DevInfo/actions)
