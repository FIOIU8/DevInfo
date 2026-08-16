# 📱 DevInfo - デバイス情報ビューア

<p align="center">
  <a href="README.md">简体中文</a> | <a href="README.en.md">English</a> | <a href="README.ja.md">日本語</a>
</p>

> 🤖 このプロジェクトは AI の支援を受けて作成されています。

[![License](https://img.shields.io/github/license/FIOIU8/DevInfo?color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-11%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Stars](https://img.shields.io/github/stars/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/stargazers)
[![GitHub Release](https://img.shields.io/github/v/release/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

Kotlin と Jetpack Compose で構築した Android 向けデバイス情報ツールです。ハードウェア仕様、システム状態、ネットワーク情報、バッテリーデータを包括的に表示し、デバイス情報を Magisk / KernelSU モジュールとしてエクスポートできます。

## 📸 スクリーンショット

<p align="center">
  <img src="devinfo-overview.png" width="240" alt="CPU 波形を表示した概要ダッシュボード">
  &nbsp;&nbsp;
  <img src="devinfo-selected.png" width="240" alt="CPU コアの詳細">
</p>

## ✨ 主な機能

### 🔍 デバイス情報

- **幅広い情報収集** - 識別子、デバイス、システム、地域、表示、ストレージ、バッテリー、ネットワーク、アプリの 9 分類で、50 項目以上の情報を収集します。
- **リアルタイムダッシュボード** - CPU / GPU の周波数と使用率、メモリ / ストレージ使用量、バッテリー状態をリアルタイムに表示します。
- **マルチコア CPU 監視** - 各 CPU コアの周波数と使用率を Canvas 波形で可視化します。
- **ハードウェアセンサー** - 動作状態、画面の明るさ、リアルタイムのストレージ読み取り速度、Wi-Fi 信号強度を検出します。
- **セキュリティチェック** - セキュリティパッチの日付、ロック画面の認証状態、USB デバッグが有効な場合の警告を表示します。

### 🛠️ ツール機能

- **Magisk / KernelSU モジュールのエクスポート** - 現在のデバイス情報をフラッシュ可能な端末偽装モジュール ZIP として出力します。両環境用のインストールスクリプトを含みます。
- **自動アップデート確認** - GitHub Releases API を通じて新しいバージョンを確認します。キャッシュ時間は 12 時間です。
- **Markdown レンダリング** - Markdown 形式の更新履歴を表示できます。

### 🎨 UI と操作

- **Material 3 デザイン** - Android 12 以降の Material You 動的カラーと Material 3 Expressive API を含む Material 3 を使用します。
- **6 種類のテーマモード** - システムに合わせる、ライト、ダークの選択と、動的カラーの切り替えに対応します。
- **8 種類のテーマカラー** - 標準、赤、オレンジ、緑、青緑、紫、ピンク、ダークを選べます。
- **多言語対応** - 簡体字中国語、英語、日本語、およびカスタム locale 入力に対応します。
- **予測型戻るジェスチャー** - Android 14 以降のシステムレベルの戻るアニメーションをサポートします。
- **プル・トゥ・リフレッシュ** - アプリ全体で `PullToRefreshBox` をサポートします。
- **スプラッシュ画面** - ライト / ダークモードに対応します。

## 📥 ダウンロード

[![GitHub Release Download](https://img.shields.io/github/downloads/FIOIU8/DevInfo/total?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

- 🚀 **最新ビルド**: [Actions](https://github.com/FIOIU8/DevInfo/actions) を開き、最新のワークフローを選択して、**Artifacts** から APK をダウンロードします。
- 📦 **正式リリース**: [Releases](https://github.com/FIOIU8/DevInfo/releases) から正式版をダウンロードします。

## 📦 GitHub Actions による自動ビルド

### トリガー

| トリガー | ブランチ | 署名種別 | バージョン名 | Release の作成 |
|----------|----------|----------|--------------|----------------|
| `main` / `test` へのプッシュ | main, test | debug | `dev-{commit}` | なし |
| 手動実行 - debug | 任意 | debug | 任意 | 任意 |
| 手動実行 - release | 任意 | release | 任意 | 任意 |

### 手動ビルド

1. リポジトリの **Actions** を開き、**Build and Release** を選択します。
2. **Run workflow** をクリックし、バージョン名、署名種別、Release オプションを指定します。
3. ビルド完了後、Artifacts または Releases から APK を取得します。

### 🔐 Release 署名の設定

> ⚠️ debug ビルドにはこの設定は不要です。テスト用にそのままインストールできます。

Fork 後に release 署名を有効にするには、**Settings → Secrets and variables → Actions → Repository secrets** で次のシークレットを追加します。

| シークレット | 説明 |
|--------------|------|
| `KEYSTORE_BASE64` | Base64 エンコードしたキーストアファイル |
| `KEYSTORE_PASSWORD` | キーストアのパスワード |
| `KEY_ALIAS` | キーのエイリアス |
| `KEY_PASSWORD` | キーのパスワード |

```bash
# キーストアを生成します。
keytool -genkey -v -keystore release.keystore -alias devinfo -keyalg RSA -keysize 2048 -validity 10000

# Base64 エンコード (Windows PowerShell)。
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -FilePath release.keystore.base64 -NoNewline

# Base64 エンコード (Linux / macOS)。
base64 -w 0 release.keystore > release.keystore.base64
```

## 🛠️ 技術スタック

| 技術 | バージョン | 説明 |
|------|------------|------|
| Kotlin | 2.3.21 | 開発言語 |
| Jetpack Compose | BOM 2026.06.01 | 宣言型 UI |
| Material 3 | 1.5.0-alpha23 | MD3、Material You、Expressive API |
| AGP | 9.1.0 | Android ビルドプラグイン |
| compileSdk / targetSdk | 37 | Android SDK |
| minSdk | 30 (Android 11) | 最低サポートバージョン |

## 📂 プロジェクト構成

```text
DevInfo/
├── .github/workflows/build.yml          # CI/CD ワークフロー
├── app/src/main/java/com/fioiu8/devinfo/
│   ├── MainActivity.kt                  # エントリー Activity
│   ├── DeviceInfoCollector.kt           # デバイス情報収集の中心 (50 項目以上)
│   ├── LiveHardwareMonitor.kt           # リアルタイムハードウェアセンサー監視
│   ├── CpuUsageSampler.kt               # CPU 使用率サンプラー (2 秒間隔)
│   ├── BatteryObserver.kt               # バッテリー状態オブザーバー (callbackFlow)
│   ├── UpdateChecker.kt                 # GitHub 更新確認
│   ├── GitHubClient.kt                  # GitHub API クライアント
│   ├── DeviceIdManager.kt               # 一意なデバイス ID の管理
│   ├── ModuleExportHelper.kt            # Magisk モジュールのエクスポート
│   ├── ThemePreferences.kt              # テーマ設定の保存
│   ├── LanguagePreferences.kt           # 言語設定の保存
│   ├── model/AppModels.kt               # データモデル定義
│   └── ui/
│       ├── MainScreen.kt                # メイン画面 (ナビゲーションとレイアウト)
│       ├── DeviceInfoOverviewPage.kt    # 概要ダッシュボード (リアルタイム波形)
│       ├── DeviceInfoPage.kt            # 分類別詳細ブラウザー
│       ├── SettingsPage.kt              # 設定画面
│       ├── AboutPage.kt                 # このアプリについて
│       ├── MarkdownRenderer.kt          # Markdown レンダラー
│       ├── AppComponents.kt             # 共通 UI コンポーネント
│       ├── AppDialogs.kt                # ダイアログコンポーネント
│       └── theme/                       # Material 3 テーマ
├── app/src/main/res/
│   ├── values/strings.xml               # 簡体字中国語 (デフォルト)
│   ├── values-en/strings.xml            # 英語
│   └── values-ja/strings.xml            # 日本語
└── gradle/libs.versions.toml            # 依存関係のバージョンカタログ
```

## 🏗️ アーキテクチャ

- **Provider パターン** - `DeviceInfoCollector` は supplier 関数のリストを通じて項目ごとに情報を収集するため、1 項目の失敗が全体に影響しません。
- **リアクティブ状態** - `StateFlow` と `collectAsState` により、テーマ、言語、バッテリー状態などをリアルタイムに更新します。
- **callbackFlow** - `BatteryObserver` は `BroadcastReceiver` を Kotlin Flow に接続します。
- **Mutex による保護** - デバイス情報の同時再読み込みを防ぎます。
- **WeakReference** - `CpuUsageSampler` はメモリリーク防止のために弱参照を使用します。
- **Snapshot パターン** - `OverviewSnapshot` はすべてのリアルタイム指標を集約して一括置換し、状態の断片化を防ぎます。

## 🔐 権限

アプリが宣言するすべての `uses-feature` は `required="false"` に設定されています。任意機能に対応していない端末でも、アプリのインストールと実行が可能です。

| 権限 | 用途 |
|------|------|
| `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | ネットワーク種別と Wi-Fi 信号の確認 |
| `INTERNET` | 更新確認のための GitHub API 呼び出し |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | Bluetooth 状態の確認 |
| `NFC` | NFC 機能の確認 |
| `CAMERA` | カメラ数の確認 |
| `READ_PHONE_STATE` | 通信事業者情報 |
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*` | ストレージ情報の読み取り |

Android 12 以降では、`BLUETOOTH_SCAN` に位置情報権限は必要ありません。ただし、より低い API レベルで Bluetooth スキャンを行う場合は、位置情報権限が必要になることがあります。

## 🔗 リンク

| リソース | リンク |
|----------|--------|
| 📦 ソースコード | [Code](https://github.com/FIOIU8/DevInfo) |
| 🐛 問題の報告 | [Issues](https://github.com/FIOIU8/DevInfo/issues) |
| 🔀 プルリクエスト | [Pull Requests](https://github.com/FIOIU8/DevInfo/pulls) |
| 📦 リリース | [Releases](https://github.com/FIOIU8/DevInfo/releases) |
| 🔧 継続的インテグレーション | [Actions](https://github.com/FIOIU8/DevInfo/actions) |
| 📊 プロジェクトボード | [Projects](https://github.com/FIOIU8/DevInfo/projects) |

## 👥 コントリビューター

[![Contributors](https://contrib.rocks/image?repo=FIOIU8/DevInfo)](https://github.com/FIOIU8/DevInfo/graphs/contributors)

Issue とプルリクエストを歓迎します。

1. このリポジトリを Fork します。
2. 機能ブランチを作成します: `git checkout -b feature/AmazingFeature`。
3. 変更をコミットします: `git commit -m 'feat: Add some AmazingFeature'`。
4. ブランチをプッシュします: `git push origin feature/AmazingFeature`。
5. プルリクエストを作成します。

`feat:`、`fix:`、`docs:`、`style:`、`refactor:`、`perf:`、`test:`、`chore:`、`ci:` などの [Conventional Commits](https://www.conventionalcommits.org/) プレフィックスの利用を推奨します。

## 📜 ライセンス

このプロジェクトは [GNU General Public License v3.0 or later (GPL-3.0-or-later)](LICENSE) の下で公開されています。

## 🙏 謝辞

- [Material 3](https://developer.android.com/jetpack/compose/designsystems/material3) - Material Design 3 のデザインシステム
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - モダンな Android UI ツールキット
- [MIUIX](https://github.com/compose-miuix-ui/miuix) - UI
- [KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit) - UI フレームワーク
- すべてのコントリビューターとユーザー

---

> ⚠️ GitHub Actions により自動ビルドされたバージョンは、テスト用の開発ビルドです。正式版は [Releases](https://github.com/FIOIU8/DevInfo/releases) からダウンロードしてください。

<p align="center">
  <a href="https://github.com/FIOIU8/DevInfo">
    <img src="https://img.shields.io/badge/⭐_Star_Me-If_You_Like_This-FFD700?style=for-the-badge&logo=github" alt="Star Me">
  </a>
</p>

<p align="center">
  <a href="https://github.com/FIOIU8/DevInfo">
    <img src="https://github.com/FIOIU8/DevInfo/raw/main/app/src/main/res/drawable/ic_launcher_foreground.xml" width="100" alt="アプリアイコン">
  </a>
</p>
