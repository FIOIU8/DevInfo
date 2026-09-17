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
 *
 * [isDynamic] is an explicit flag instead of a `name.startsWith("DYNAMIC")` check, so
 * adding or renaming an entry cannot silently change which modes use dynamic color.
 */
enum class ThemeMode(val isDynamic: Boolean) {
    SYSTEM(isDynamic = false),
    LIGHT(isDynamic = false),
    DARK(isDynamic = false),
    DYNAMIC_SYSTEM(isDynamic = true),
    DYNAMIC_LIGHT(isDynamic = true),
    DYNAMIC_DARK(isDynamic = true),
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
    val visible: Boolean = false
)
