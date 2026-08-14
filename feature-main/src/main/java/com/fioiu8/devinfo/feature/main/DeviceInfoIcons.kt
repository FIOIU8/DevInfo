/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.fioiu8.devinfo.feature.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.HdrOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.fioiu8.devinfo.core.model.InfoCategory

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
 * 返回数据项 key 对应的图标，无法匹配时返回 Info 图标。
 * [key] 是字符串资源名称（如 "device_android_id"），由 DeviceInfoItem.key 提供。
 */
fun itemIconByKey(key: String): ImageVector {
    return when (key) {
        "device_android_id", "device_serial" -> Icons.Outlined.Fingerprint
        "device_brand" -> Icons.Outlined.Badge
        "device_manufacturer" -> Icons.Outlined.PrecisionManufacturing
        "device_model" -> Icons.Outlined.PhoneAndroid
        "device_product" -> Icons.Outlined.Inventory2
        "device_device" -> Icons.Outlined.Devices
        "device_board", "system_cpu_arch" -> Icons.Outlined.DeveloperBoard
        "device_hardware", "system_cpu_cores" -> Icons.Outlined.Memory
        "device_bootloader" -> Icons.Outlined.RestartAlt
        "device_build_id", "device_build_display", "device_incremental",
        "system_kernel", "system_java_vm" -> Icons.Outlined.Build
        "device_fingerprint" -> Icons.Outlined.Fingerprint
        "device_host", "locale_country", "network_data_roaming" -> Icons.Outlined.Public
        "device_user" -> Icons.Outlined.Person
        "device_soc_manufacturer" -> Icons.Outlined.PrecisionManufacturing
        "device_soc_model" -> Icons.Outlined.DeveloperBoard
        "system_sdk_version", "system_abis_32", "system_abis_64",
        "app_target_sdk", "app_min_sdk" -> Icons.Outlined.Api
        "system_android_version", "system_google_play_services", "app_package" -> Icons.Outlined.Android
        "system_security_patch", "battery_health" -> Icons.Outlined.Security
        "system_baseband" -> Icons.Outlined.SettingsInputAntenna
        "system_uptime", "locale_timezone", "device_time" -> Icons.Outlined.Schedule
        "system_features" -> Icons.Outlined.Devices
        "system_opengl_version", "display_wide_color_gamut" -> Icons.Outlined.Palette
        "system_treble" -> Icons.Outlined.Shield
        "system_sensor_count" -> Icons.Outlined.Sensors
        "system_boot_time", "locale_timezone_offset", "locale_24_hour" -> Icons.Outlined.AccessTime
        "system_usb_debugging" -> Icons.Outlined.DeveloperMode
        "system_lock_screen" -> Icons.Outlined.Lock
        "locale_language", "locale_display_name", "locale_system_locales" -> Icons.Outlined.Language
        "locale_tag", "device_tags" -> Icons.AutoMirrored.Outlined.Label
        "locale_currency" -> Icons.Outlined.AttachMoney
        "display_dpi", "display_density" -> Icons.Outlined.GridOn
        "display_width", "display_height", "display_size" -> Icons.Outlined.AspectRatio
        "display_refresh_rate", "display_supported_refresh_rates", "app_last_update" -> Icons.Outlined.Refresh
        "display_font_scale" -> Icons.Outlined.FormatSize
        "display_orientation" -> Icons.Outlined.ScreenRotation
        "display_dark_mode" -> Icons.Outlined.DarkMode
        "display_hdr" -> Icons.Outlined.HdrOn
        "display_brightness" -> Icons.Outlined.BrightnessMedium
        "display_timeout" -> Icons.Outlined.Timer
        "storage_total_ram", "storage_available_ram", "storage_low_memory" -> Icons.Outlined.Memory
        "storage_total", "storage_available" -> Icons.Outlined.Storage
        "storage_memory_threshold", "storage_app_heap", "network_metered" -> Icons.Outlined.DataUsage
        "storage_internal_total", "storage_internal_available", "storage_removable" -> Icons.Outlined.SdStorage
        "storage_emulated", "battery_cycle_count" -> Icons.Outlined.Cached
        "battery_level_label", "battery_temperature", "battery_technology",
        "battery_charge_counter", "battery_capacity_design" -> Icons.Outlined.BatteryStd
        "battery_charging_state", "battery_voltage", "battery_current_now" -> Icons.Outlined.Bolt
        "battery_plug_type" -> Icons.Outlined.Power
        "battery_power_save" -> Icons.Outlined.EnergySavingsLeaf
        "network_nfc" -> Icons.Outlined.Nfc
        "network_camera_count" -> Icons.Outlined.CameraAlt
        "network_bluetooth_state" -> Icons.Outlined.Bluetooth
        "network_type", "network_wifi_enabled" -> Icons.Outlined.Wifi
        "network_operator", "network_sim_state", "network_sim_operator", "network_sim_country" -> Icons.Outlined.SimCard
        "network_phone_type" -> Icons.Outlined.Phone
        "network_vpn" -> Icons.Outlined.VpnKey
        "network_airplane_mode" -> Icons.Outlined.AirplanemodeActive
        "network_link_speed" -> Icons.Outlined.Speed
        "app_version_name" -> Icons.Outlined.Info
        "app_version_code" -> Icons.Outlined.Pin
        "app_first_install", "app_installer" -> Icons.Outlined.Download
        "app_installed_count" -> Icons.Outlined.Apps
        else -> Icons.Outlined.Info
    }
}
