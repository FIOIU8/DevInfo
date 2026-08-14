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

package com.fioiu8.devinfo.ui.theme

import androidx.compose.ui.graphics.Color
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.ui.R

val Teal80 = Color(0xFF53DBC7)
val BlueGrey80 = Color(0xFFB0CCC6)
val Sky80 = Color(0xFFADCBE5)

val Teal40 = Color(0xFF006A60)
val BlueGrey40 = Color(0xFF4A635F)
val Sky40 = Color(0xFF456179)

/**
 * Maps ThemeColor enum to actual Color values for Material You theming.
 */
fun themeColorToColor(themeColor: ThemeColor): Color {
    return when (themeColor) {
        ThemeColor.DEFAULT -> Color(0xFF6750A4)
        ThemeColor.RED -> Color(0xFFB3261E)
        ThemeColor.ORANGE -> Color(0xFF9A4600)
        ThemeColor.YELLOW -> Color(0xFFFBC02D)
        ThemeColor.GREEN -> Color(0xFF386A20)
        ThemeColor.TEAL -> Color(0xFF006A6A)
        ThemeColor.CYAN -> Color(0xFF0097A7)
        ThemeColor.BLUE -> Color(0xFF1976D2)
        ThemeColor.INDIGO -> Color(0xFF303F9F)
        ThemeColor.DEEP_PURPLE -> Color(0xFF512DA8)
        ThemeColor.PURPLE -> Color(0xFF6750A4)
        ThemeColor.PINK -> Color(0xFF9C3D6D)
        ThemeColor.BROWN -> Color(0xFF5D4037)
        ThemeColor.BLUE_GREY -> Color(0xFF455A64)
        ThemeColor.SAKURA -> Color(0xFFF8BBD0)
        ThemeColor.DARK -> Color(0xFF415F91)
    }
}

/** Extension property to get Color from ThemeColor */
val ThemeColor.color: Color
    get() = themeColorToColor(this)

/** Extension function to get display name resource ID from ThemeColor */
fun ThemeColor.displayNameResId(): Int = when (this) {
    ThemeColor.DEFAULT -> R.string.mount_color_default
    ThemeColor.RED -> R.string.mount_color_red
    ThemeColor.ORANGE -> R.string.mount_color_orange
    ThemeColor.YELLOW -> R.string.mount_color_yellow
    ThemeColor.GREEN -> R.string.mount_color_green
    ThemeColor.TEAL -> R.string.mount_color_teal
    ThemeColor.CYAN -> R.string.mount_color_cyan
    ThemeColor.BLUE -> R.string.mount_color_blue
    ThemeColor.INDIGO -> R.string.mount_color_indigo
    ThemeColor.DEEP_PURPLE -> R.string.mount_color_deep_purple
    ThemeColor.PURPLE -> R.string.mount_color_purple
    ThemeColor.PINK -> R.string.mount_color_pink
    ThemeColor.BROWN -> R.string.mount_color_brown
    ThemeColor.BLUE_GREY -> R.string.mount_color_blue_grey
    ThemeColor.SAKURA -> R.string.mount_color_sakura
    ThemeColor.DARK -> R.string.mount_color_dark
}
