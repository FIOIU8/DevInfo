# DevInfo

<p align="center">
  <a href="README.md">简体中文</a> |
  <a href="README.en.md">English</a> |
  <a href="README.ja.md">日本語</a>
</p>

> 本项目及其文档部分由 AI 辅助生成和整理，代码、配置与发布内容仍需人工审核。

| Material 3 设备信息 | Material 3 信息详情 | Material 3 设置 |
| --- | --- | --- |
| <img src="md3_info.png" width="220" alt="Material 3 设备信息"> | <img src="md3_info_details.png" width="220" alt="Material 3 设备信息详情"> | <img src="md3_settings.png" width="220" alt="Material 3 设置"> |

| Miuix 设备信息 | Miuix 信息详情 | Miuix 设置 |
| --- | --- | --- |
| <img src="miuix_info.png" width="220" alt="Miuix 设备信息"> | <img src="miuix_info_details.png" width="220" alt="Miuix 设备信息详情"> | <img src="miuix_settings.png" width="220" alt="Miuix 设置"> |

[![License](https://img.shields.io/github/license/FIOIU8/DevInfo?color=blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Release](https://img.shields.io/github/v/release/FIOIU8/DevInfo?style=flat&logo=github)](https://github.com/FIOIU8/DevInfo/releases)

DevInfo 是一个使用 Kotlin 和 Jetpack Compose 编写的 Android 设备信息查看器。应用以“设备信息查看器”为定位，提供一次性设备信息采集、实时硬件概览、分类详情、主题与语言设置，以及设备信息模块导出等功能。

> 应用文案中的“设备信息查看器”来自 `app/src/main/res/values/strings.xml` 的 `footer_tag`，这里作为项目定位引用。

## 能做什么

### 设备信息

- 按九个分类展示信息：标识符、设备、系统、区域、显示、存储、电池、网络和应用。
- 采集 Android 版本、SDK、ABI、内核、安全补丁、屏幕参数、内存与存储、电池、网络、传感器以及应用安装信息等字段。
- 总览页聚合静态信息和实时指标，支持下拉刷新与分类详情浏览。
- CPU 总体/每核心占用率、核心频率、GPU 频率与占用率、内存/存储使用率、电池电量和充电状态会按设备能力显示。
- 实时硬件区域还会显示运动状态、屏幕亮度、存储读取速度和 Wi-Fi 信号（可用时）。

### 导出与更新

- 将当前设备信息导出为 ZIP 格式的 Magisk/KernelSU 机型模拟模块。
- 导出前可选择最小化策略；导出的 ZIP 可能包含设备信息，刷入模块可能改变系统行为，请在理解风险后使用。
- 通过 GitHub Releases API 检查更新，更新结果和发布说明在应用内展示；网络不可用时会显示错误状态。

### 界面与本地化

- Material 3 与 Miuix 两种 UI 风格。
- 系统、浅色、深色以及动态颜色模式；支持多种主题色、调色板风格、页面缩放、模糊和悬浮导航栏设置。
- 简体中文、英文、日文，以及自定义 BCP-47 locale 标签。
- 支持 Android 14+ 预测返回手势、深浅色 Splash 屏和全局下拉刷新。

## 数据可用性说明

设备厂商、Android 版本和权限策略会影响可读字段。应用对单项采集失败采用降级处理，缺失字段不会阻塞其余信息展示。

部分 Android 版本会限制 `/proc` 等 CPU 统计接口。此时 CPU 占用率会显示为不可用状态，应用仍会尽可能展示 CPU 拓扑和实时核心频率；不会把不可用数据伪装成 `0%`。

## 获取与构建

### 下载

- 正式版本：前往 [Releases](https://github.com/FIOIU8/DevInfo/releases)。
- 测试构建：前往 [Actions](https://github.com/FIOIU8/DevInfo/actions)，在工作流运行结果的 Artifacts 中下载 APK。

### 环境要求

- Android Studio（建议使用项目随附的 Android Studio JBR）。
- JDK 21；Kotlin 编译目标为 JVM 11。
- Android SDK 37。
- 最低支持 Android 13（API 33）。

### 本地构建

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

Debug APK 输出在 `app/build/outputs/apk/debug/`。首次构建需要 Gradle 下载依赖；网络或 JDK 配置问题会导致构建失败。

## 技术栈

| 项目 | 当前配置 |
| --- | --- |
| Kotlin | 2.4.0 |
| Jetpack Compose | BOM 2026.06.01 |
| Material 3 | 由 Compose BOM 管理 |
| Miuix | 0.9.2 |
| Android Gradle Plugin | 9.1.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 33（Android 13） |
| Java 编译兼容性 | Java 11；Gradle toolchain 21 |

## 项目结构

```text
DevInfo/
├── app/                 # Android 应用入口、Manifest、构建与打包配置
├── core/                # 纯 Kotlin 的模型、CPU 解析和跨模块基础逻辑
├── data/                # 设备信息采集、实时监控、偏好、更新和模块导出
├── feature-main/        # 主界面、总览、详情、设置和关于页面
├── ui/                  # Compose/Miuix 主题、通用组件和 Markdown 渲染
├── .github/workflows/   # CI 验证、APK 构建和 tag 发布流程
├── gradle/libs.versions.toml
├── LICENSE
└── README.md
```

数据层以 `DeviceInfoCollector` 为入口按分类采集信息；`OverviewSnapshot` 聚合实时指标供 Compose 页面渲染；`StateFlow`、DataStore 和 `callbackFlow` 分别用于状态协调、偏好持久化和电池广播桥接。模块导出逻辑集中在 `ModuleExportHelper`，并配有路径、转义和 ZIP 内容校验测试。

## 架构解析

### 模块依赖

```text
app
├── feature-main ── data ── core
│                 └─────── ui ── core
├── data
├── ui
└── core
```

- **`app`**：唯一的 Android application 模块。`MainActivity` 创建设备采集器、更新检查器、偏好仓库和导出助手，并将它们注入主功能模块。
- **`feature-main`**：面向用户的功能层，包含 `MainScreen`、`MainViewModel`、设备概览、分类详情、设置和关于页面。它依赖数据层提供的用例对象，不直接实现系统读取细节。
- **`data`**：Android 平台数据层。`DeviceInfoCollector` 按九类信息供应器采集设备数据；`LiveHardwareMonitor`、`CpuUsageSampler` 和 `BatteryObserver` 提供实时指标；偏好、更新检查和 ZIP 模块导出也集中在此层。
- **`core`**：跨模块的基础模型和无 UI 的解析逻辑，例如 `DeviceInfoItem`、`OverviewSnapshot`、CPU 时间解析和导出策略模型。
- **`ui`**：共享表现层，封装 Material 3/Miuix 主题、导航栏、反馈组件、模糊效果和 Markdown 渲染器。

### 主要数据流

```text
系统 API / sysfs / GitHub
          │
          ▼
       data 层
          │  DeviceInfoItem、OverviewSnapshot、StateFlow
          ▼
   MainViewModel（协调加载、刷新和错误状态）
          │
          ▼
 feature-main 页面 ──> ui 共享组件 ──> Compose 界面
```

1. `MainActivity` 组装依赖并启动 Compose 内容。
2. `MainViewModel` 在后台线程调用 `DeviceInfoCollector`，将静态信息和实时监控结果汇总为 `MainUiState` 与 `OverviewSnapshot`。
3. 页面通过生命周期感知的 `StateFlow` 收集状态；单项采集失败只影响该字段，加载和刷新状态仍会反馈给界面。
4. 主题、语言和更新开关由偏好仓库持久化，并以 `StateFlow` 驱动界面更新；电池广播通过 `callbackFlow` 转换为可观察数据。
5. 导出操作从页面发起，经 `ModuleExportHelper` 生成并校验 ZIP 内容，再交给系统文件选择器保存；更新操作由 `UpdateChecker` 调用 `GitHubClient`，并缓存检查结果。

### 设计取舍

- **快照式状态**：用 `OverviewSnapshot` 一次替换总览指标，避免 CPU、内存、电池等实时卡片分别更新造成短暂不一致。
- **能力优先的降级**：频率、占用率和传感器数据均允许为空；当 Android 限制 `/proc` 或 sysfs 访问时，界面保留可读取的拓扑和频率信息，并明确标记不可用状态。
- **边界清晰**：`core` 不依赖 Compose，`data` 不负责页面布局，`feature-main` 负责交互编排，便于对解析器、导出校验和偏好验证进行单元测试。

## 权限与隐私

Manifest 当前声明的权限为：

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 检查 GitHub Releases 更新 |
| `ACCESS_NETWORK_STATE` | 判断当前网络能力 |
| `ACCESS_WIFI_STATE` | 读取 Wi-Fi 开关和信号相关状态 |
| `NFC` | 判断设备是否支持 NFC |

应用会读取设备信息以完成界面展示和可选模块导出。导出的 ZIP 可能包含设备标识、构建指纹或安全补丁等字段，请在分享或刷入前检查内容。项目不在 README 中承诺云端上传设备信息；更新检查仅在启用时访问 GitHub Releases API。

## 持续集成与发布

- `build.yml` 在 `main`、`test` 的 push、目标分支 pull request 和手动触发时运行单元测试、ktlint、Android lint 与 Debug APK 构建。
- 手动工作流可传入版本名和签名类型（`debug` 或 `release`）；构建产物会上传到 Actions Artifacts。
- `release.yml` 由 `v*` tag 触发，先执行质量检查，再使用仓库 Secrets 签名并创建 Draft Release。
- Release 签名需要配置 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS` 和 `KEY_PASSWORD`；不要把 keystore 或密码提交到仓库。

## 贡献

1. Fork 项目并创建功能分支。
2. 修改代码或文档，并补充相应测试。
3. 本地运行 `testDebugUnitTest`、`ktlintCheck` 和 `lintDebug`。
4. 提交 Pull Request，并在描述中说明受影响模块和验证结果。

问题反馈请使用 [Issues](https://github.com/FIOIU8/DevInfo/issues)。

## 许可证

本项目基于 [GPL-3.0](LICENSE) 发布。

## 链接

- [源代码](https://github.com/FIOIU8/DevInfo)
- [Releases](https://github.com/FIOIU8/DevInfo/releases)
- [Actions](https://github.com/FIOIU8/DevInfo/actions)
- [Issues](https://github.com/FIOIU8/DevInfo/issues)
