# DevInfo

<p align="center">
  <a href="README.md">简体中文</a> |
  <a href="README.en.md">English</a> |
  <a href="README.ja.md">日本語</a>
</p>

> This project and parts of its documentation were generated and refined with AI assistance. Code, configuration, and releases remain subject to human review.

| Material 3 Device information | Material 3 Information details | Material 3 Settings |
| --- | --- | --- |
| <img src="md3_info.png" width="220" alt="Material 3 device information"> | <img src="md3_info_details.png" width="220" alt="Material 3 device information details"> | <img src="md3_settings.png" width="220" alt="Material 3 settings"> |

| Miuix Device information | Miuix Information details | Miuix Settings |
| --- | --- | --- |
| <img src="miuix_info.png" width="220" alt="Miuix device information"> | <img src="miuix_info_details.png" width="220" alt="Miuix device information details"> | <img src="miuix_settings.png" width="220" alt="Miuix settings"> |

[![License](https://img.shields.io/github/license/FIOIU8/DevInfo?color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Release](https://img.shields.io/github/v/release/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

DevInfo is an Android device information viewer built with Kotlin and Jetpack Compose. It collects device information, presents a live hardware overview and categorized details, supports theme and language preferences, and can export device information as a Magisk/KernelSU module.

## Features

### Device information

- Nine categories: identifiers, device, system, locale, display, storage, battery, network, and app information.
- Android version, SDK, ABI, kernel, security patch, display, memory/storage, battery, network, sensor, and installed-app fields.
- Overview dashboard with static cards, live metrics, pull-to-refresh, and categorized detail pages.
- CPU overall/per-core usage, core frequency, GPU frequency/usage, memory/storage usage, and battery state when supported by the device.
- Motion state, screen brightness, storage read speed, and Wi-Fi signal details when available.

### Export and updates

- Export current device information as a ZIP-based Magisk/KernelSU device-spoofing module.
- Choose a minimal export policy before writing the archive. The ZIP may contain device information and flashing it can affect system behavior.
- Check GitHub Releases for updates and render release notes in the app. Network failures are reported as an explicit error state.

### UI and localization

- Material 3 and Miuix UI styles.
- System, light, dark, and dynamic-color modes with theme colors, palette styles, page scale, blur, and floating navigation options.
- Simplified Chinese, English, Japanese, and custom BCP-47 locale tags.
- Predictive back gestures on Android 14+, light/dark splash screens, and app-wide pull-to-refresh.

## Data availability

Readable fields depend on the device manufacturer, Android version, and system restrictions. Individual collection failures are isolated so one unavailable field does not block the rest of the screen.

Some Android versions restrict `/proc` CPU statistics. In that case CPU utilization is shown as unavailable while CPU topology and live core frequency are retained when readable; unavailable telemetry is never presented as `0%`.

## Download and build

### Downloads

- Official releases: [Releases](https://github.com/FIOIU8/DevInfo/releases).
- Test builds: open [Actions](https://github.com/FIOIU8/DevInfo/actions) and download the APK from workflow Artifacts.

### Requirements

- Android Studio and Android SDK 37.
- JDK 21; Kotlin targets JVM 11.
- Minimum supported Android version: Android 13 (API 33).

### Local build

```bash
# Windows
gradlew.bat testDebugUnitTest
gradlew.bat ktlintCheck
gradlew.bat lintDebug
gradlew.bat assembleDebug

# Linux/macOS
./gradlew testDebugUnitTest
./gradlew ktlintCheck
./gradlew lintDebug
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. The first build requires Gradle dependency downloads.

## Technology stack

| Technology | Current configuration |
| --- | --- |
| Kotlin | 2.4.0 |
| Jetpack Compose | BOM 2026.06.01 |
| Material 3 | Managed by the Compose BOM |
| Miuix | 0.9.2 |
| Android Gradle Plugin | 9.1.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 33 (Android 13) |
| Java compatibility | Java 11; Gradle toolchain 21 |

## Project structure

```text
DevInfo/
├── app/                 # Application entry point, manifest, packaging
├── core/                # Models, CPU parsing, and framework-independent logic
├── data/                # Collection, monitoring, preferences, updates, export
├── feature-main/        # Main screen, overview, details, settings, about
├── ui/                  # Compose/Miuix theme, shared components, Markdown
├── .github/workflows/   # CI verification, APK builds, tag releases
├── gradle/libs.versions.toml
├── LICENSE
└── README.en.md
```

## Architecture

### Module dependencies

```text
app
├── feature-main ── data ── core
│                 └─────── ui ── core
├── data
├── ui
└── core
```

- **`app`** is the only Android application module. `MainActivity` assembles the collector, update checker, preference repositories, and export helper.
- **`feature-main`** owns user-facing screens and `MainViewModel`; it coordinates interactions without implementing platform reads.
- **`data`** contains `DeviceInfoCollector`, live hardware monitors, preferences, update checking, and module export.
- **`core`** contains shared models such as `DeviceInfoItem` and `OverviewSnapshot`, CPU parsers, and export policies.
- **`ui`** provides shared Material 3/Miuix themes, navigation, feedback, blur, and Markdown components.

### Main data flow

```text
Android APIs / sysfs / GitHub
             │
             ▼
          data layer
             │ DeviceInfoItem, OverviewSnapshot, StateFlow
             ▼
       MainViewModel
             │
             ▼
 feature-main screens ──> ui components ──> Compose UI
```

`MainActivity` injects dependencies, `MainViewModel` loads static and live data on background dispatchers, and screens collect lifecycle-aware `StateFlow` values. `OverviewSnapshot` replaces the complete overview state to keep live cards consistent. Preferences use observable flows, the battery broadcast is bridged through `callbackFlow`, `UpdateChecker` uses `GitHubClient` with cached state, and `ModuleExportHelper` validates ZIP entries before saving through the system picker.

## Permissions and privacy

The current manifests declare:

| Permission | Purpose |
| --- | --- |
| `INTERNET` | GitHub Releases update checks |
| `ACCESS_NETWORK_STATE` | Network capability checks |
| `ACCESS_WIFI_STATE` | Wi-Fi state and signal-related checks |
| `NFC` | NFC capability detection |

Device information is read for local display and optional module export. Exported ZIP files may contain identifiers, build fingerprints, or security-patch data; inspect them before sharing or flashing. Update checks contact GitHub only when enabled.

## CI and releases

- `build.yml` runs unit tests, ktlint, Android lint, and a debug APK build for pushes to `main`/`test`, pull requests, and manual runs.
- Manual runs accept a version name and `debug` or `release` signing type; artifacts are uploaded to Actions.
- `release.yml` supports pushes to `main`, `vMAJOR.MINOR.PATCH` tags, and manual dispatch. It publishes only after a version tag reaches `main`, then runs the quality gate, signs with repository Secrets, and creates a Draft Release; manual dispatch can create the tag from `main`.
- Release signing requires `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Never commit a keystore or password.

## Contributing

1. Fork the repository and create a feature branch.
2. Update code or documentation and add relevant tests.
3. Run `testDebugUnitTest`, `ktlintCheck`, and `lintDebug` locally.
4. Open a pull request describing affected modules and verification results.

Report issues through [Issues](https://github.com/FIOIU8/DevInfo/issues).

## License

This project is released under [GPL-3.0](LICENSE).

## Links

- [Source code](https://github.com/FIOIU8/DevInfo)
- [Releases](https://github.com/FIOIU8/DevInfo/releases)
- [Actions](https://github.com/FIOIU8/DevInfo/actions)
- [Issues](https://github.com/FIOIU8/DevInfo/issues)
