# 📱 DevInfo - Device Information Viewer

<p align="center">
  <a href="README.md">简体中文</a> | <a href="README.en.md">English</a> | <a href="README.ja.md">日本語</a>
</p>

> 🤖 This project was created with AI assistance.

[![License](https://img.shields.io/github/license/FIOIU8/DevInfo?color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-11%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Stars](https://img.shields.io/github/stars/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/stargazers)
[![GitHub Release](https://img.shields.io/github/v/release/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

An Android device information tool built with Kotlin and Jetpack Compose. It presents hardware specifications, system status, network information, and battery data, and can export device information as Magisk or KernelSU modules.

## 📸 Screenshots

<p align="center">
  <img src="devinfo-overview.png" width="240" alt="Overview dashboard with CPU waveform">
  &nbsp;&nbsp;
  <img src="devinfo-selected.png" width="240" alt="CPU core details">
</p>

## ✨ Features

### 🔍 Device information

- **Comprehensive collection** - More than 50 device properties across nine categories: identifiers, device, system, regional settings, display, storage, battery, network, and apps.
- **Live dashboard** - Real-time CPU/GPU frequency and utilization, memory and storage usage, and battery status.
- **Multi-core CPU monitoring** - Per-core frequency and utilization, visualized with Canvas waveforms.
- **Hardware sensors** - Motion-state detection, screen brightness, live storage read speed, and Wi-Fi signal strength.
- **Security checks** - Security patch date, lock-screen credential status, and a warning when USB debugging is enabled.

### 🛠️ Tools

- **Magisk/KernelSU module export** - Export current device information as a flashable device-spoofing module ZIP, with installation scripts for both environments.
- **Automatic update checks** - Check for new versions through the GitHub Releases API, with a 12-hour cache.
- **Markdown rendering** - Render release notes written in Markdown.

### 🎨 Interface and interaction

- **Material 3 design** - Material 3 with Material You dynamic colors on Android 12+, including the Material 3 Expressive API.
- **Six theme modes** - Follow system, light, or dark, with a dynamic-color toggle.
- **Eight theme colors** - Default, red, orange, green, teal, purple, pink, and dark.
- **Multiple languages** - Simplified Chinese, English, Japanese, and a custom locale input.
- **Predictive back gesture** - Android 14+ system-level back animation support.
- **Pull to refresh** - App-wide `PullToRefreshBox` support.
- **Splash screen** - Adapts to light and dark modes.

## 📥 Download

[![GitHub Release Download](https://img.shields.io/github/downloads/FIOIU8/DevInfo/total?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

- 🚀 **Latest build**: Open [Actions](https://github.com/FIOIU8/DevInfo/actions), select the latest workflow run, and download the APK from **Artifacts**.
- 📦 **Stable release**: Download official versions from [Releases](https://github.com/FIOIU8/DevInfo/releases).

## 📦 GitHub Actions automated builds

### Triggers

| Trigger | Branch | Signing type | Version name | Create release |
|---------|--------|--------------|--------------|----------------|
| Push to `main` / `test` | main, test | debug | `dev-{commit}` | No |
| Manual trigger - debug | Any | debug | Custom | Optional |
| Manual trigger - release | Any | release | Custom | Optional |

### Manual builds

1. Open the repository's **Actions** page, then select **Build and Release**.
2. Click **Run workflow** and provide the version name, signing type, and release options.
3. When the build finishes, download the APK from Artifacts or Releases.

### 🔐 Configure release signing

> ⚠️ No setup is required for debug builds; they can be installed directly for testing.

After forking, add these repository secrets under **Settings → Secrets and variables → Actions → Repository secrets** to enable release signing:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

```bash
# Generate a keystore.
keytool -genkey -v -keystore release.keystore -alias devinfo -keyalg RSA -keysize 2048 -validity 10000

# Base64 encode it (Windows PowerShell).
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -FilePath release.keystore.base64 -NoNewline

# Base64 encode it (Linux/macOS).
base64 -w 0 release.keystore > release.keystore.base64
```

## 🛠️ Technology stack

| Technology | Version | Description |
|------------|---------|-------------|
| Kotlin | 2.3.21 | Development language |
| Jetpack Compose | BOM 2026.06.01 | Declarative UI |
| Material 3 | 1.5.0-alpha23 | MD3, Material You, and Expressive API |
| AGP | 9.1.0 | Android build plugin |
| compileSdk / targetSdk | 37 | Android SDK |
| minSdk | 30 (Android 11) | Minimum supported version |

## 📂 Project structure

```text
DevInfo/
├── .github/workflows/build.yml          # CI/CD workflow
├── app/src/main/java/com/fioiu8/devinfo/
│   ├── MainActivity.kt                  # Entry activity
│   ├── DeviceInfoCollector.kt           # Core device-information collector (50+ properties)
│   ├── LiveHardwareMonitor.kt           # Live hardware sensor monitor
│   ├── CpuUsageSampler.kt               # CPU utilization sampler (2-second interval)
│   ├── BatteryObserver.kt               # Battery status observer (callbackFlow)
│   ├── UpdateChecker.kt                 # GitHub version-update checker
│   ├── GitHubClient.kt                  # GitHub API client
│   ├── DeviceIdManager.kt               # Unique device identifier management
│   ├── ModuleExportHelper.kt            # Magisk module export
│   ├── ThemePreferences.kt              # Theme preference storage
│   ├── LanguagePreferences.kt           # Language preference storage
│   ├── model/AppModels.kt               # Data-model definitions
│   └── ui/
│       ├── MainScreen.kt                # Main screen (navigation and layout)
│       ├── DeviceInfoOverviewPage.kt    # Overview dashboard (live waveforms)
│       ├── DeviceInfoPage.kt            # Categorized detail browser
│       ├── SettingsPage.kt              # Settings page
│       ├── AboutPage.kt                 # About page
│       ├── MarkdownRenderer.kt          # Markdown renderer
│       ├── AppComponents.kt             # Shared UI components
│       ├── AppDialogs.kt                # Dialog components
│       └── theme/                       # Material 3 theme
├── app/src/main/res/
│   ├── values/strings.xml               # Simplified Chinese (default)
│   ├── values-en/strings.xml            # English
│   └── values-ja/strings.xml            # Japanese
└── gradle/libs.versions.toml            # Dependency version catalog
```

## 🏗️ Architecture

- **Provider pattern** - `DeviceInfoCollector` collects information item by item through a list of supplier functions, so a single failure does not affect the rest.
- **Reactive state** - `StateFlow` and `collectAsState` drive live updates for themes, language, battery status, and more.
- **callbackFlow** - `BatteryObserver` bridges `BroadcastReceiver` to Kotlin Flow.
- **Mutex protection** - Prevents concurrent device-information reloads.
- **WeakReference** - `CpuUsageSampler` uses weak references to prevent memory leaks.
- **Snapshot pattern** - `OverviewSnapshot` aggregates all live metrics and replaces them together to avoid fragmented state.

## 🔐 Permissions

Every `uses-feature` declared by the app is set to `required="false"`, so devices that do not support an optional feature can still install and run the app.

| Permission | Purpose |
|------------|---------|
| `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | Network type and Wi-Fi signal checks |
| `INTERNET` | GitHub API calls for update checks |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | Bluetooth status checks |
| `NFC` | NFC capability checks |
| `CAMERA` | Camera-count checks |
| `READ_PHONE_STATE` | Carrier information |
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*` | Storage-information reading |

On Android 12+, `BLUETOOTH_SCAN` does not require location permission. Location permission may still be needed for Bluetooth scanning on lower API levels.

## 🔗 Quick links

| Resource | Link |
|----------|------|
| 📦 Source code | [Code](https://github.com/FIOIU8/DevInfo) |
| 🐛 Report an issue | [Issues](https://github.com/FIOIU8/DevInfo/issues) |
| 🔀 Pull requests | [Pull Requests](https://github.com/FIOIU8/DevInfo/pulls) |
| 📦 Releases | [Releases](https://github.com/FIOIU8/DevInfo/releases) |
| 🔧 Continuous integration | [Actions](https://github.com/FIOIU8/DevInfo/actions) |
| 📊 Project board | [Projects](https://github.com/FIOIU8/DevInfo/projects) |

## 👥 Contributors

[![Contributors](https://contrib.rocks/image?repo=FIOIU8/DevInfo)](https://github.com/FIOIU8/DevInfo/graphs/contributors)

Issues and pull requests are welcome.

1. Fork this repository.
2. Create a feature branch: `git checkout -b feature/AmazingFeature`.
3. Commit your changes: `git commit -m 'feat: Add some AmazingFeature'`.
4. Push the branch: `git push origin feature/AmazingFeature`.
5. Open a pull request.

We recommend [Conventional Commits](https://www.conventionalcommits.org/) prefixes such as `feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `perf:`, `test:`, `chore:`, and `ci:`.

## 📜 License

This project is open source under the [MIT License](LICENSE).

## 🙏 Acknowledgments

- [Material 3](https://developer.android.com/jetpack/compose/designsystems/material3) - Material Design 3 design system
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- All contributors and users

---

> ⚠️ Versions built automatically by GitHub Actions are development builds intended for testing. Download official releases from [Releases](https://github.com/FIOIU8/DevInfo/releases).

<p align="center">
  <a href="https://github.com/FIOIU8/DevInfo">
    <img src="https://img.shields.io/badge/⭐_Star_Me-If_You_Like_This-FFD700?style=for-the-badge&logo=github" alt="Star Me">
  </a>
</p>

<p align="center">
  <a href="https://github.com/FIOIU8/DevInfo">
    <img src="https://github.com/FIOIU8/DevInfo/raw/main/app/src/main/res/drawable/ic_launcher_foreground.xml" width="100" alt="App Icon">
  </a>
</p>
