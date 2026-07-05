package com.fioiu8.devinfo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.fioiu8.devinfo.model.InfoCategory

/**
 * 返回分类对应的图标
 */
fun categoryIcon(category: InfoCategory): ImageVector {
    return when (category) {
        InfoCategory.IDENTIFIERS -> Icons.Outlined.Fingerprint
        InfoCategory.DEVICE -> Icons.Outlined.PhoneAndroid
        InfoCategory.SYSTEM -> Icons.Outlined.Memory
        InfoCategory.LOCALE -> Icons.Outlined.Language
        InfoCategory.DISPLAY -> Icons.Outlined.Screenshot
        InfoCategory.STORAGE -> Icons.Outlined.SdStorage
        InfoCategory.BATTERY -> Icons.Outlined.BatteryChargingFull
        InfoCategory.NETWORK -> Icons.Outlined.SignalCellularAlt
        InfoCategory.APP -> Icons.Outlined.Apps
    }
}

/**
 * 返回数据项 key 对应的图标，无法匹配时返回 Info 图标
 */
fun itemIcon(key: String): ImageVector {
    return when (key) {
        "ANDROID_ID" -> Icons.Outlined.Fingerprint
        "序列号" -> Icons.Outlined.Tag
        "品牌" -> Icons.Outlined.Badge
        "制造商" -> Icons.Outlined.PrecisionManufacturing
        "型号" -> Icons.Outlined.PhoneAndroid
        "产品" -> Icons.Outlined.Inventory2
        "设备" -> Icons.Outlined.Devices
        "主板" -> Icons.Outlined.DeveloperBoard
        "硬件" -> Icons.Outlined.Memory
        "引导程序" -> Icons.Outlined.RestartAlt
        "构建ID" -> Icons.Outlined.Build
        "标签" -> Icons.AutoMirrored.Outlined.Label
        "时间" -> Icons.Outlined.Schedule
        "类型" -> Icons.Outlined.Category
        "CPU架构" -> Icons.Outlined.DeveloperBoard
        "CPU核心数" -> Icons.Outlined.Memory
        "SDK版本" -> Icons.Outlined.Api
        "Android版本" -> Icons.Outlined.Android
        "安全补丁" -> Icons.Outlined.Security
        "基带版本" -> Icons.Outlined.SettingsInputAntenna
        "语言" -> Icons.Outlined.Language
        "国家" -> Icons.Outlined.Public
        "时区" -> Icons.Outlined.Schedule
        "屏幕DPI" -> Icons.Outlined.GridOn
        "屏幕宽度", "屏幕高度" -> Icons.Outlined.AspectRatio
        "刷新率" -> Icons.Outlined.Refresh
        "字体缩放" -> Icons.Outlined.FormatSize
        "总内存", "可用内存" -> Icons.Outlined.Memory
        "存储总量", "可用存储" -> Icons.Outlined.Storage
        "电池电量" -> Icons.Outlined.BatteryStd
        "充电状态" -> Icons.Outlined.Bolt
        "NFC功能" -> Icons.Outlined.Nfc
        "摄像头数量" -> Icons.Outlined.CameraAlt
        "蓝牙状态" -> Icons.Outlined.Bluetooth
        "网络类型" -> Icons.Outlined.Wifi
        "运营商", "SIM卡状态" -> Icons.Outlined.SimCard
        "包名" -> Icons.Outlined.Android
        "应用版本名" -> Icons.Outlined.Info
        "应用版本码" -> Icons.Outlined.Pin
        else -> Icons.Outlined.Info
    }
}
