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

package com.fioiu8.devinfo.core.model

/**
 * UI style selection — Material 3 or Miuix.
 */
enum class UiStyle {
    MATERIAL3,
    MIUIX,
}

/**
 * Theme mode options.
 * Display names resolved by UI layer via string key.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    DYNAMIC_SYSTEM,
    DYNAMIC_LIGHT,
    DYNAMIC_DARK;

    val isDynamic: Boolean get() = name.startsWith("DYNAMIC")
}

/**
 * Theme color options.
 * Display names resolved by UI layer via string key.
 */
enum class ThemeColor {
    DEFAULT,
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    TEAL,
    CYAN,
    BLUE,
    INDIGO,
    DEEP_PURPLE,
    PURPLE,
    PINK,
    BROWN,
    BLUE_GREY,
    SAKURA,
    DARK
}

/**
 * Material You palette style options.
 */
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

/**
 * Color specification version.
 */
enum class ColorSpec {
    DEFAULT,
    V0,
    V1,
}

/**
 * Device information categories.
 * Simple enum without UI dependencies — display names resolved by UI layer.
 */
enum class InfoCategory {
    DEVICE,
    IDENTIFIERS,
    SYSTEM,
    LOCALE,
    DISPLAY,
    STORAGE,
    BATTERY,
    NETWORK,
    APP
}

/**
 * Device info item — pure data, no UI dependencies.
 */
data class DeviceInfoItem(
    val key: String,
    val keyResId: Int,
    val value: String,
    val category: InfoCategory
)

/**
 * Visibility wrapper for animated list items.
 * Uses a simple boolean instead of Compose MutableState for framework independence.
 */
data class ItemWithVisibility(
    val item: DeviceInfoItem,
    var visible: Boolean = false
)

/**
 * Controls which optional fields can be included in an exported module.
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
 * Simplified device info for module export.
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
