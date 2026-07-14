package com.fioiu8.devinfo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.fioiu8.devinfo.R
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
 * 返回数据项 keyResId 对应的图标，无法匹配时返回 Info 图标
 */
fun itemIconByResId(keyResId: Int): ImageVector {
    return when (keyResId) {
        R.string.device_serial -> Icons.Outlined.Fingerprint
        R.string.device_brand -> Icons.Outlined.Badge
        R.string.device_manufacturer -> Icons.Outlined.PrecisionManufacturing
        R.string.device_model -> Icons.Outlined.PhoneAndroid
        R.string.device_product -> Icons.Outlined.Inventory2
        R.string.device_device -> Icons.Outlined.Devices
        R.string.device_board -> Icons.Outlined.DeveloperBoard
        R.string.device_hardware -> Icons.Outlined.Memory
        R.string.device_bootloader -> Icons.Outlined.RestartAlt
        R.string.device_build_id -> Icons.Outlined.Build
        R.string.device_tags -> Icons.AutoMirrored.Outlined.Label
        R.string.device_time -> Icons.Outlined.Schedule
        R.string.device_type -> Icons.Outlined.Category
        R.string.system_cpu_arch -> Icons.Outlined.DeveloperBoard
        R.string.system_cpu_cores -> Icons.Outlined.Memory
        R.string.system_sdk_version -> Icons.Outlined.Api
        R.string.system_android_version -> Icons.Outlined.Android
        R.string.system_security_patch -> Icons.Outlined.Security
        R.string.system_baseband -> Icons.Outlined.SettingsInputAntenna
        R.string.locale_language -> Icons.Outlined.Language
        R.string.locale_country -> Icons.Outlined.Public
        R.string.locale_timezone -> Icons.Outlined.Schedule
        R.string.display_dpi -> Icons.Outlined.GridOn
        R.string.display_width, R.string.display_height -> Icons.Outlined.AspectRatio
        R.string.display_refresh_rate -> Icons.Outlined.Refresh
        R.string.display_font_scale -> Icons.Outlined.FormatSize
        R.string.storage_total_ram, R.string.storage_available_ram -> Icons.Outlined.Memory
        R.string.storage_total, R.string.storage_available -> Icons.Outlined.Storage
        R.string.battery_level_label -> Icons.Outlined.BatteryStd
        R.string.battery_charging_state -> Icons.Outlined.Bolt
        R.string.network_nfc -> Icons.Outlined.Nfc
        R.string.network_camera_count -> Icons.Outlined.CameraAlt
        R.string.network_bluetooth_state -> Icons.Outlined.Bluetooth
        R.string.network_type -> Icons.Outlined.Wifi
        R.string.network_operator, R.string.network_sim_state -> Icons.Outlined.SimCard
        R.string.app_package -> Icons.Outlined.Android
        R.string.app_version_name -> Icons.Outlined.Info
        R.string.app_version_code -> Icons.Outlined.Pin
        else -> Icons.Outlined.Info
    }
}
