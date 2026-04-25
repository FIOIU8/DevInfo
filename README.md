# 📱 DevInfo - 设备信息查看器

[![版本](https://img.shields.io/badge/version-1.0-blue.svg)](https://github.com/FIOIU8/DevInfo/releases)
[![许可证](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-6.0%2B-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.00-blue.svg)](https://developer.android.com/jetpack/compose)

一款简洁、优雅、开源的 Android 设备信息查看工具，帮助你全面了解自己的设备。

[功能特点](#-功能特点) • [截图](#-截图) • [下载](#-下载) • [快速开始](#-快速开始) • [技术栈](#-技术栈) • [开源协议](#-开源协议)

---

## ✨ 功能特点

### 📊 全面的信息展示
展示 **40+ 项**设备详细信息，涵盖：

| 分类 | 信息项 |
|------|--------|
| **基本信息** | ANDROID_ID、序列号、品牌、型号、硬件、指纹等 15 项 |
| **系统信息** | Android 版本、SDK 版本、安全补丁、基带版本等 6 项 |
| **屏幕信息** | 分辨率、DPI、刷新率、字体缩放等 5 项 |
| **内存存储** | 总内存/可用内存、总存储/可用存储 |
| **电池信息** | 电量百分比、充电状态 |
| **硬件功能** | NFC、摄像头数量、蓝牙状态 |
| **网络信息** | 网络类型、运营商、SIM 卡状态 |

### 🎨 个性化定制
- **多种主题**：浅色模式、深色模式、跟随系统
- **Monet 主题**：Android 12+ 支持动态取色
- **8 种主题色**：默认、红色、橙色、绿色、青色、紫色、粉色、深色
- **5 种导航栏样式**：悬浮/固定、图标/文字自由组合

### 🔧 实用工具
- **一键导出**：导出设备信息为 ZIP 压缩包
- **长按复制**：任意信息项长按即可复制
- **下拉刷新**：实时更新设备状态

### 🔐 隐私安全
- **完全离线**：不联网，不上传任何数据
- **无广告**：纯净体验
- **开源透明**：代码完全公开

---

## 🚀 快速开始

### 方式一：直接安装 APK
1. 从 [Releases](https://github.com/FIOIU8/DevInfo/releases) 下载最新 APK
2. 在设备上允许“安装未知应用”
3. 点击安装即可使用

### 方式二：从源码构建

```bash
# 克隆项目
git clone https://github.com/FIOIU8/DevInfo.git
cd DevInfo

# 使用 Android Studio 打开项目
# 等待 Gradle 同步完成后，点击 Build → Build APK
