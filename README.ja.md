# DevInfo

<p align="center">
  <a href="README.md">简体中文</a> |
  <a href="README.en.md">English</a> |
  <a href="README.ja.md">日本語</a>
</p>

| Material 3 デバイス情報 | Material 3 詳細 | Material 3 設定 |
| --- | --- | --- |
| <img src="md3_info.png" width="220" alt="Material 3 デバイス情報"> | <img src="md3_info_details.png" width="220" alt="Material 3 情報詳細"> | <img src="md3_settings.png" width="220" alt="Material 3 設定"> |

| Miuix デバイス情報 | Miuix 詳細 | Miuix 設定 |
| --- | --- | --- |
| <img src="miuix_info.png" width="220" alt="Miuix デバイス情報"> | <img src="miuix_info_details.png" width="220" alt="Miuix 情報詳細"> | <img src="miuix_settings.png" width="220" alt="Miuix 設定"> |

[![License](https://img.shields.io/github/license/FIOIU8/DevInfo?color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Release](https://img.shields.io/github/v/release/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

DevInfo は Kotlin と Jetpack Compose で作られた Android デバイス情報ビューアです。端末情報をローカルで収集し、リアルタイムのハードウェア概要、カテゴリ別の詳細、テーマと言語設定を提供します。情報を Magisk/KernelSU の ZIP モジュールとして書き出すこともできます。

## 機能

### デバイス情報とリアルタイム概要

- 詳細画面は、デバイス、識別子、システム、地域、ディスプレイ、ストレージ、バッテリー、ネットワーク、アプリの9カテゴリで構成されます。
- Android バージョン、SDK、ABI、カーネル、セキュリティパッチ、画面、メモリ/ストレージ、バッテリー、ネットワーク、センサー、アプリ情報を表示します。
- 概要画面は静的情報、プル・トゥ・リフレッシュ、リアルタイム指標を提供します。CPU/GPU、メモリ、ストレージ、バッテリー、動き、明るさ、ストレージ読み取り速度、Wi-Fi 信号は端末が対応する場合に表示されます。
- 通常モードでは CPU 統計が制限される場合があります。オプションの Root モードはユーザー確認後に、より詳細な CPU データの読み取りを試みます。

### エクスポートと更新

- システムのファイル選択画面を使い、任意フィールドを選択して Magisk/KernelSU の ZIP モジュールを書き出します。
- ZIP にはデバイス識別子、ビルドフィンガープリント、セキュリティパッチが含まれる場合があります。モジュールの適用はシステム動作を変更する可能性があります。
- 正式ビルドでは GitHub Releases を確認し、リリースノートを表示できます。ネットワーク障害はエラー状態として表示されます。

### UI とローカライズ

- Material 3 と Miuix の UI スタイル。
- システム、ライト、ダーク、動的カラー、テーマ色、カラースタイル、ページ倍率、ぼかし、フローティングナビゲーションの設定。
- 簡体字中国語、英語、日本語、カスタム BCP-47 locale タグ。
- Android 14 以降の予測型戻るジェスチャー、Splash 画面、アプリ全体のプル・トゥ・リフレッシュ。

## データ可用性とプライバシー

取得できる項目はメーカー、Android バージョン、権限ポリシー、`/proc`/`/sys` の制限によって異なります。個別の失敗は他の表示を妨げず、利用できない CPU 使用率を `0%` として表示することはありません。

本アプリに独自バックエンドやデバイス情報のアップロード機能はありません。更新確認では GitHub API にアクセスします。ZIP の共有・適用前に内容とリスクを確認してください。Root モードは必須ではありません。

Manifest が宣言するのは `INTERNET`、`ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE`、`NFC` です。NFC 機能は任意で、現在 Bluetooth 権限は宣言していません。

## ダウンロードとビルド

### ダウンロード

- 正式版：[Releases](https://github.com/FIOIU8/DevInfo/releases)。
- テスト版：[Actions](https://github.com/FIOIU8/DevInfo/actions) の workflow Artifact。

### 必要な環境

- Android Studio と Android SDK 37。
- JDK 21、`app`/`feature-main` のコンパイルターゲットは JVM 21、`core`/`data`/`ui` は JVM 11 のままです。
- 最低対応 Android は Android 13（API 33）です。

### ローカルビルド

```bash
# Windows
gradlew.bat testDebugUnitTest
gradlew.bat ktlintCheck
gradlew.bat lintDebug
gradlew.bat assembleDebug

# Linux/macOS: gradlew.bat を ./gradlew に置き換える
```

Debug APK は `app/build/outputs/apk/debug/` に出力されます。

## 技術スタックと構成

| 項目 | 現在の設定 |
| --- | --- |
| Kotlin | 2.4.10 |
| Jetpack Compose | BOM 2026.08.00 |
| Miuix | 0.9.4-rc01 |
| Android Gradle Plugin | 9.1.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 33（Android 13） |
| Java ターゲット / ツールチェーン | `app`/`feature-main`: JVM 21、`core`/`data`/`ui`: JVM 11、JDK 21 |

```text
DevInfo/
├── app/                 # Android エントリ、Manifest、リソース、ビルド
├── core/                # UI に依存しないモデルと CPU 解析
├── data/                # 収集、監視、設定、更新、モジュール出力
├── feature-main/        # メイン、概要、詳細、設定、About
├── ui/                  # Compose/Miuix テーマと共有コンポーネント
├── .github/workflows/   # CI、ビルド、Release
├── gradle/libs.versions.toml
└── LICENSE
```

`MainActivity` が依存関係を組み立て、`MainViewModel` がロード、更新、フォアグラウンド監視を調整します。画面は `StateFlow` を収集します。テーマと言語の設定は SharedPreferences、更新キャッシュは DataStore を使用し、旧 SharedPreferences 互換経路も保持します。画面状態は `MainScreen` が管理し、Navigation Compose は使用していません。

## CI とリリース

- `build.yml` は `main`、`dev`、`test` への push、Pull Request、手動実行でテスト、ktlint、Android lint、ビルドを実行します。
- 手動ビルドではバージョン名と `debug`/`release` 署名種別を指定でき、APK とビルド情報の Artifact をアップロードします。
- `release.yml` は `main` への push、`vMAJOR.MINOR.PATCH` tag、`main` からの手動リリースに対応します。
- テスト、ktlint、lint の後、`KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` を使って署名 APK と Draft Release を作成します。

## コントリビューション

変更前に実装とテストを確認し、対象モジュールだけを変更して必要な Gradle チェックを実行してください。コミット形式は `英文プレフィックス: 中国語の説明`（例：`docs: 更新开发文档`）です。問題は [Issues](https://github.com/FIOIU8/DevInfo/issues) へ報告してください。

## ライセンスとリンク

本プロジェクトは [GPL-3.0](LICENSE) で公開されています。

- [ソースコード](https://github.com/FIOIU8/DevInfo)
- [Releases](https://github.com/FIOIU8/DevInfo/releases)
- [Actions](https://github.com/FIOIU8/DevInfo/actions)
