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

package com.fioiu8.devinfo.model

import androidx.compose.runtime.MutableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 设备信息分类
 */
enum class InfoCategory(
    val displayNameResId: Int,
    val descriptionResId: Int
) {
    DEVICE(com.fioiu8.devinfo.R.string.category_device, com.fioiu8.devinfo.R.string.category_desc_device),
    IDENTIFIERS(com.fioiu8.devinfo.R.string.category_identifiers, com.fioiu8.devinfo.R.string.category_desc_identifiers),
    SYSTEM(com.fioiu8.devinfo.R.string.category_system, com.fioiu8.devinfo.R.string.category_desc_system),
    LOCALE(com.fioiu8.devinfo.R.string.category_locale, com.fioiu8.devinfo.R.string.category_desc_locale),
    DISPLAY(com.fioiu8.devinfo.R.string.category_display, com.fioiu8.devinfo.R.string.category_desc_display),
    STORAGE(com.fioiu8.devinfo.R.string.category_storage, com.fioiu8.devinfo.R.string.category_desc_storage),
    BATTERY(com.fioiu8.devinfo.R.string.category_battery, com.fioiu8.devinfo.R.string.category_desc_battery),
    NETWORK(com.fioiu8.devinfo.R.string.category_network, com.fioiu8.devinfo.R.string.category_desc_network),
    APP(com.fioiu8.devinfo.R.string.category_app, com.fioiu8.devinfo.R.string.category_desc_app)
}

/**
 * MD3 主题模式
 */
enum class ThemeMode(val displayNameResId: Int) {
    SYSTEM(com.fioiu8.devinfo.R.string.theme_mode_system),
    LIGHT(com.fioiu8.devinfo.R.string.theme_mode_light),
    DARK(com.fioiu8.devinfo.R.string.theme_mode_dark),
    DYNAMIC_SYSTEM(com.fioiu8.devinfo.R.string.theme_mode_dynamic_system),
    DYNAMIC_LIGHT(com.fioiu8.devinfo.R.string.theme_mode_dynamic_light),
    DYNAMIC_DARK(com.fioiu8.devinfo.R.string.theme_mode_dynamic_dark);

    val isDynamic: Boolean get() = name.startsWith("DYNAMIC")
}

enum class UiStyle {
    MATERIAL3,
    MIUIX,
}

enum class PaletteStyle {
    DEFAULT,
    TONAL_SPOT,
    VIBRANT,
    EXPRESSIVE,
    FIDELITY,
    CONTENT,
    NEUTRAL,
    MONOCHROME,
    RAINBOW,
    FRUIT_SALAD,
}

enum class ColorSpec {
    DEFAULT,
    V0,
    V1,
}

enum class ThemeColor(
    val displayNameResId: Int,
    val color: Color,
    val descriptionResId: Int
) {
    DEFAULT(com.fioiu8.devinfo.R.string.mount_color_default, Color(0xFF6750A4), com.fioiu8.devinfo.R.string.mount_color_desc_default),
    RED(com.fioiu8.devinfo.R.string.mount_color_red, Color(0xFFB3261E), com.fioiu8.devinfo.R.string.mount_color_desc_red),
    ORANGE(com.fioiu8.devinfo.R.string.mount_color_orange, Color(0xFF9A4600), com.fioiu8.devinfo.R.string.mount_color_desc_orange),
    YELLOW(com.fioiu8.devinfo.R.string.mount_color_yellow, Color(0xFFFBC02D), com.fioiu8.devinfo.R.string.mount_color_desc_yellow),
    GREEN(com.fioiu8.devinfo.R.string.mount_color_green, Color(0xFF386A20), com.fioiu8.devinfo.R.string.mount_color_desc_green),
    TEAL(com.fioiu8.devinfo.R.string.mount_color_teal, Color(0xFF006A6A), com.fioiu8.devinfo.R.string.mount_color_desc_teal),
    CYAN(com.fioiu8.devinfo.R.string.mount_color_cyan, Color(0xFF0097A7), com.fioiu8.devinfo.R.string.mount_color_desc_cyan),
    BLUE(com.fioiu8.devinfo.R.string.mount_color_blue, Color(0xFF1976D2), com.fioiu8.devinfo.R.string.mount_color_desc_blue),
    INDIGO(com.fioiu8.devinfo.R.string.mount_color_indigo, Color(0xFF303F9F), com.fioiu8.devinfo.R.string.mount_color_desc_indigo),
    DEEP_PURPLE(com.fioiu8.devinfo.R.string.mount_color_deep_purple, Color(0xFF512DA8), com.fioiu8.devinfo.R.string.mount_color_desc_deep_purple),
    PURPLE(com.fioiu8.devinfo.R.string.mount_color_purple, Color(0xFF6750A4), com.fioiu8.devinfo.R.string.mount_color_desc_purple),
    PINK(com.fioiu8.devinfo.R.string.mount_color_pink, Color(0xFF9C3D6D), com.fioiu8.devinfo.R.string.mount_color_desc_pink),
    BROWN(com.fioiu8.devinfo.R.string.mount_color_brown, Color(0xFF5D4037), com.fioiu8.devinfo.R.string.mount_color_desc_brown),
    BLUE_GREY(com.fioiu8.devinfo.R.string.mount_color_blue_grey, Color(0xFF455A64), com.fioiu8.devinfo.R.string.mount_color_desc_blue_grey),
    SAKURA(com.fioiu8.devinfo.R.string.mount_color_sakura, Color(0xFFF8BBD0), com.fioiu8.devinfo.R.string.mount_color_desc_sakura),
    DARK(com.fioiu8.devinfo.R.string.mount_color_dark, Color(0xFF415F91), com.fioiu8.devinfo.R.string.mount_color_desc_dark)
}

/**
 * 设备信息单项数据。
 *
 * @property key 信息项名称（中文回退）
 * @property keyResId 信息项名称对应的字符串资源ID，0=使用key
 * @property value 信息项值
 * @property category 所属分类
 */
data class DeviceInfoItem(
    val key: String,
    val keyResId: Int,
    val value: String,
    val category: InfoCategory,
    val icon: ImageVector = Icons.Outlined.Info
)

/**
 * 带可见性状态的设备信息项，用于列表交错动画。
 *
 * @property item 设备信息数据
 * @property visible 控制该项是否可见（用于入场动画，初始为 false）
 */
data class ItemWithVisibility(val item: DeviceInfoItem, val visible: MutableState<Boolean>)

/**
 * Controls which optional fields can be included in an exported module.
 * The default policy intentionally excludes identifiers and build details that
 * are not required for the module's device-property overrides.
 */
data class ModuleExportPolicy(
    val includeDeviceIdentifier: Boolean = false,
    val includeBuildFingerprint: Boolean = false,
    val includeSecurityPatch: Boolean = false,
) {
    companion object {
        val MINIMAL = ModuleExportPolicy()
    }
}

/**
 * 改机型模块导出所需精简信息。
 *
 * @property deviceId 持久化设备唯一标识
 * @property brand 品牌（如 Xiaomi）
 * @property manufacturer 制造商（如 Xiaomi）
 * @property model 型号（如 23127PN0CC）
 * @property device 设备代号（如 houji）
 * @property product 产品名（如 houji）
 * @property versionRelease Android 版本（如 15）
 * @property versionSdk SDK 版本号（如 35）
 * @property securityPatch 安全补丁日期（如 2025-06-05）
 * @property supportedAbis 所有支持的 ABI 列表
 * @property supported32BitAbis 32 位 ABI 列表
 * @property supported64BitAbis 64 位 ABI 列表
 */
data class MenuInfo(
    val deviceId: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val versionRelease: String,
    val versionSdk: String,
    val securityPatch: String,
    val supportedAbis: List<String>,
 val supported32BitAbis: List<String>,
 val supported64BitAbis: List<String>
)

/**
 * App language selection mode.
 * SYSTEM — follow device locale; CUSTOM — user inputs a custom locale tag.
 */
enum class AppLanguage(
    val displayNameResId: Int,
    val localeTag: String?,
    val isCustom: Boolean = false
) {
    SYSTEM(com.fioiu8.devinfo.R.string.language_system, null),
    SIMPLIFIED_CHINESE(com.fioiu8.devinfo.R.string.language_chinese, "zh"),
    ENGLISH(com.fioiu8.devinfo.R.string.language_english, "en"),
    JAPANESE(com.fioiu8.devinfo.R.string.language_japanese, "ja"),
    CUSTOM(com.fioiu8.devinfo.R.string.language_custom, "_custom", isCustom = true);
}
