package com.fioiu8.devinfo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
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
    val keyColor = themeColor.color.takeIf {
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
