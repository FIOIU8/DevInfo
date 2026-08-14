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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
internal fun MiuixDevInfoTheme(
    themeMode: ThemeMode,
    themeColor: ThemeColor,
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode = themeMode.toMiuixColorSchemeMode()
    val keyColor = themeColorToColor(themeColor).takeIf {
        themeMode.isDynamic && themeColor != ThemeColor.DEFAULT
    }
    val controller = remember(colorSchemeMode, keyColor, darkTheme) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = keyColor,
            isDark = darkTheme,
        )
    }

    MiuixTheme(controller = controller, content = content)
}

private fun ThemeMode.toMiuixColorSchemeMode(): ColorSchemeMode = when (this) {
    ThemeMode.SYSTEM -> ColorSchemeMode.System
    ThemeMode.LIGHT -> ColorSchemeMode.Light
    ThemeMode.DARK -> ColorSchemeMode.Dark
    ThemeMode.DYNAMIC_SYSTEM -> ColorSchemeMode.MonetSystem
    ThemeMode.DYNAMIC_LIGHT -> ColorSchemeMode.MonetLight
    ThemeMode.DYNAMIC_DARK -> ColorSchemeMode.MonetDark
}
