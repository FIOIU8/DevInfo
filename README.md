# DevInfo

<p align="center">
  <a href="README.md">简体中文</a> |
  <a href="README.en.md">English</a> |
  <a href="README.ja.md">日本語</a>
</p>

| Material 3 设备信息 | Material 3 信息详情 | Material 3 设置 |
| --- | --- | --- |
| <img src="md3_info.png" width="220" alt="Material 3 设备信息"> | <img src="md3_info_details.png" width="220" alt="Material 3 信息详情"> | <img src="md3_settings.png" width="220" alt="Material 3 设置"> |

| Miuix 设备信息 | Miuix 信息详情 | Miuix 设置 |
| --- | --- | --- |
| <img src="miuix_info.png" width="220" alt="Miuix 设备信息"> | <img src="miuix_info_details.png" width="220" alt="Miuix 信息详情"> | <img src="miuix_settings.png" width="220" alt="Miuix 设置"> |

[![License](https://img.shields.io/github/license/FIOIU8/DevInfo?color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Release](https://img.shields.io/github/v/release/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

DevInfo 是一个使用 Kotlin 和 Jetpack Compose 编写的 Android 设备信息查看器。它在本地采集设备信息，提供实时硬件概览、分类详情、主题与语言设置，并可将信息导出为 Magisk/KernelSU ZIP 模块。

## 功能

### 设备信息与实时概览

- 详情页按设备、标识符、系统、区域、显示、存储、电池、网络、应用九类展示信息。
- 覆盖 Android 版本、SDK、ABI、内核、安全补丁、屏幕、内存/存储、电池、网络、传感器和应用信息等字段。
- 总览页提供静态信息、下拉刷新和实时指标；CPU/GPU、内存、存储、电池、运动、亮度、存储读速和 Wi-Fi 信号按设备能力显示。
- 普通模式下 CPU 统计可能受系统限制；可选 Root 模式会在用户确认后尝试读取更完整的 CPU 数据。

### 导出与更新

- 通过系统文件选择器导出 ZIP 格式的 Magisk/KernelSU 模块；导出内容固定为最小集合，仅包含品牌、型号、设备代号、产品名、Android 版本/SDK 与 ABI 列表。
- 模块的 version 与 versionCode 取自应用版本；刷入模块会写入 ro.product.* 等系统属性，可能改变系统行为。
- 正式构建可检查 GitHub Releases 更新并显示发布说明；网络异常会显示失败状态。

### 界面与本地化

- Material 3 与 Miuix 两种 UI 风格。
- 系统、浅色、深色和动态颜色模式，以及主题色、颜色风格、页面缩放、模糊和悬浮底栏设置。
- 简体中文、英文、日文和自定义 BCP-47 locale 标签。
- Android 14+ 预测返回手势、Splash 屏和全局下拉刷新。

## 数据可用性与隐私

厂商 ROM、Android 版本、权限策略以及 `/proc`/`/sys` 限制会影响字段可用性。单项采集失败不会阻塞其他信息；不可用的 CPU 使用率不会伪装成 `0%`。

应用没有自建后端或设备信息上传服务。更新检查访问 GitHub API；导出 ZIP 前请检查敏感字段，分享或刷入前确认风险。Root 模式不是应用运行的必要条件。

Manifest 当前声明 `INTERNET`、`ACCESS_NETWORK_STATE`、`ACCESS_WIFI_STATE` 和 `NFC`；NFC 特性为非必需。当前没有蓝牙权限声明。

## 下载与构建

### 下载

- 正式版本：[Releases](https://github.com/FIOIU8/DevInfo/releases)。
- 测试构建：[Actions](https://github.com/FIOIU8/DevInfo/actions) 中下载 workflow Artifact。

### 环境要求

- Android Studio 和 Android SDK 37。
- JDK 21；`app`/`feature-main` 使用 JVM 21，`core`/`data`/`ui` 保持 JVM 11。
- 最低支持 Android 13（API 33）。

### 本地构建

```bash
# Windows
gradlew.bat testDebugUnitTest
gradlew.bat ktlintCheck
gradlew.bat lintDebug
gradlew.bat assembleDebug

# Linux/macOS: replace gradlew.bat with ./gradlew
```

Debug APK 输出在 `app/build/outputs/apk/debug/`。

## 技术栈与结构

| 项目 | 当前配置 |
| --- | --- |
| Kotlin | 2.4.10 |
| Jetpack Compose | BOM 2026.08.00 |
| Miuix | 0.9.4-rc01 |
| Android Gradle Plugin | 9.1.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 33（Android 13） |
| Java 编译目标 / 工具链 | `app`/`feature-main`: JVM 21；`core`/`data`/`ui`: JVM 11；JDK 21 |

```text
DevInfo/
├── app/                 # Android 入口、Manifest、资源和构建
├── core/                # 无 UI 的模型与 CPU 解析
├── data/                # 采集、监控、偏好、更新和模块导出
├── feature-main/        # 主界面、总览、详情、设置和关于
├── ui/                  # Compose/Miuix 主题与共享组件
├── .github/workflows/   # CI、构建和 Release
├── gradle/libs.versions.toml
└── LICENSE
```

`MainActivity` 组装依赖，`MainViewModel` 协调加载、刷新和前台监控，页面通过 `StateFlow` 收集状态。主题/语言偏好使用 SharedPreferences；更新缓存使用 DataStore，并保留旧 SharedPreferences 兼容路径。页面状态由 `MainScreen` 管理，不使用 Navigation Compose。

## CI 与发布

- `build.yml` 在 `main`、`dev`、`test` 分支 push、目标分支 Pull Request 和手动运行时执行测试、ktlint、Android lint 与构建。
- 手动构建可输入版本名和 `debug`/`release` 签名类型，并上传 APK 与构建信息 Artifact。
- `release.yml` 支持 `main` push、`vMAJOR.MINOR.PATCH` tag 和从 `main` 发起的手动发布。
- Release 流程先通过测试、ktlint 和 lint，再使用 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 创建签名 APK 和 Draft Release。

## 贡献

请先阅读代码和测试，再修改对应模块，并运行相关 Gradle 检查。提交格式为 `英文前缀: 英文描述`（与 `AGENTS.md` §8 一致），例如 `docs: update contributor instructions`。问题请提交到 [Issues](https://github.com/FIOIU8/DevInfo/issues)。

## 许可证与链接

本项目基于 [GPL-3.0](LICENSE) 发布。

- [源代码](https://github.com/FIOIU8/DevInfo)
- [Releases](https://github.com/FIOIU8/DevInfo/releases)
- [Actions](https://github.com/FIOIU8/DevInfo/actions)
