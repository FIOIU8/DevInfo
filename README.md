# 设备信息查看器 (DevInfo)

一款基于 Kotlin Compose 和 Miuix 开发的 Android 设备信息查看工具，提供详细的硬件、系统、网络等信息展示。

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-Automated-blue)](https://github.com/FIOIU8/DevInfo/actions)

## 📱 功能特性

- 🔍 **设备信息查看** - 全面展示设备硬件信息（CPU、内存、存储、屏幕等）
- 📡 **系统信息** - Android 版本、SDK 版本、安全补丁等
- 🔋 **电池信息** - 电量百分比、充电状态
- 📶 **网络信息** - 网络类型、运营商、SIM 卡状态
- 🎨 **Miuix 风格 UI** - 支持浅色/深色模式、Monet 主题引擎
- 📤 **模块导出** - 导出设备信息为改造模块
- 🌐 **多语言支持** - 支持中英文

## 🚀 GitHub Actions 自动构建

本项目已集成 GitHub Actions CI/CD 流水线，支持自动化构建和发布。

### 触发方式

| 触发方式 | 签名类型 | 版本名 | 是否创建 Release |
|---------|---------|--------|-----------------|
| 推送到 `master`/`test` | debug | `dev-{commit}` | ❌ |
| 手动触发 - debug | debug | 自定义 | 可选 |
| 手动触发 - release | release | 自定义 | 可选 |

### 手动构建使用说明

1. 进入 GitHub 仓库 → **Actions** → **Build and Release**
2. 点击 **Run workflow**
3. 填写参数：
   - **Version name**: 版本号（如 `1.0.1`）
   - **Signature type**: `debug`（测试）或 `release`（正式）
   - **Create GitHub Release**: 是否自动创建 Release
   - **Release notes**: 更新内容（可选）
4. 点击 **Run workflow** 开始构建

### 🔐 配置正式签名（仅 release 构建需要）

> ⚠️ **注意**：如果只需要 debug 签名进行测试，可以跳过此步骤。debug 签名的 APK 可以直接安装使用。

如果您 Fork 了此仓库且需要使用 release 签名功能，需要在 GitHub Secrets 中配置以下密钥：

1. 进入仓库：**Settings** → **Secrets and variables** → **Actions** → **Repository secrets**
2. 点击 **New repository secret** 添加以下密钥：

| Secret 名称 | 说明 | 示例 |
|------------|------|------|
| `KEYSTORE_BASE64` | 密钥库文件的 Base64 编码 | （从 `release.keystore` 生成） |
| `KEYSTORE_PASSWORD` | 密钥库密码 | `your_password` |
| `KEY_ALIAS` | 密钥别名 | `devinfo` |
| `KEY_PASSWORD` | 密钥密码 | `your_key_password` |

**生成密钥库和 Base64 编码：**

```bash
# 1. 生成密钥库
keytool -genkey -v -keystore release.keystore -alias devinfo -keyalg RSA -keysize 2048 -validity 10000

# 2. Base64 编码（Linux/Mac）
base64 -w 0 release.keystore > release.keystore.base64

# 3. Base64 编码（Windows PowerShell）
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -FilePath release.keystore.base64 -NoNewline
```

## 📥 下载

- **最新构建**：前往 [Actions](https://github.com/FIOIU8/DevInfo/actions) 页面，点击最新的工作流，在 **Artifacts** 中下载 APK
- **正式发布**：前往 [Releases](https://github.com/FIOIU8/DevInfo/releases) 页面下载正式版本

> 💡 **提示**：Actions 中的构建可能包含未稳定的新功能，建议测试使用。

## 🛠️ 技术栈

- **UI 框架**：Jetpack Compose + [Miuix](https://github.com/yukonga/Miuix)
- **开发语言**：Kotlin
- **最低 SDK**：Android 8.0 (API 30)
- **构建工具**：Gradle Kotlin DSL
- **CI/CD**：GitHub Actions

## 📁 项目结构

```
DevInfo/
├── .github/workflows/     # GitHub Actions 工作流
├── app/
│   ├── src/main/java/     # 源代码
│   └── build.gradle.kts   # 模块构建配置
├── gradle/                # Gradle 配置
└── build.gradle.kts       # 项目构建配置
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

### Commit 规范

推荐使用以下前缀：

| 前缀 | 说明 |
|------|------|
| `feat:` | 新功能 |
| `fix:` | Bug 修复 |
| `docs:` | 文档更新 |
| `style:` | 代码格式调整 |
| `refactor:` | 代码重构 |
| `perf:` | 性能优化 |
| `test:` | 测试相关 |
| `chore:` | 构建/工具相关 |
| `ci:` | CI/CD 配置 |

## 📄 开源协议

本项目基于 MIT 协议开源，详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [Miuix](https://github.com/yukonga/Miuix) - 提供优秀的 Compose UI 组件库
- 所有贡献者和用户

---

**⚡ 注意**：通过 GitHub Actions 自动构建的版本为开发测试版，不建议在生产环境中使用。正式版本请从 Releases 页面下载。

<p align="center">
  <a href="https://github.com/FIOIU8/DevInfo/actions">
    <img src="https://github.com/FIOIU8/DevInfo/actions/workflows/build.yml/badge.svg" alt="Build Status">
  </a>
</p>
