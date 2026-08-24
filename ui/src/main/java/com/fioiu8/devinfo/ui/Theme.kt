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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.core.model.UiStyle

val LocalUiStyle = staticCompositionLocalOf { UiStyle.MATERIAL3 }
private val LocalDevInfoDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isInDarkTheme(): Boolean = LocalDevInfoDarkTheme.current

// 静态 fallback — 用于非动态模式或 Android 12 以下
private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF74F8E5),
    secondary = BlueGrey80,
    onSecondary = Color(0xFF1B3530),
    secondaryContainer = Color(0xFF324B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Sky80,
    onTertiary = Color(0xFF143349),
    tertiaryContainer = Color(0xFF2C4960),
    onTertiaryContainer = Color(0xFFCBE6FF),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE0E3E1),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4946),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceDim = Color(0xFF101413),
    surfaceBright = Color(0xFF363A38),
    surfaceContainerLowest = Color(0xFF0B0F0E),
    surfaceContainerLow = Color(0xFF191C1B),
    surfaceContainer = Color(0xFF1D201F),
    surfaceContainerHigh = Color(0xFF272B29),
    surfaceContainerHighest = Color(0xFF323634)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF74F8E5),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF06201C),
    tertiary = Sky40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCBE6FF),
    onTertiaryContainer = Color(0xFF001E30),
    background = Color(0xFFF8FAF8),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF8FAF8),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C5),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceDim = Color(0xFFD8DBD9),
    surfaceBright = Color(0xFFF8FAF8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F4F2),
    surfaceContainer = Color(0xFFECEEEB),
    surfaceContainerHigh = Color(0xFFE6E9E7),
    surfaceContainerHighest = Color(0xFFE0E3E1)
)

/**
 * DevInfo 主题 — 支持完整的 Material 动态颜色（Android 12+）和手动浅色/深色。
 *
 * @param themeMode 主题模式
 * @param content Composable 内容
 */
@Composable
fun DevInfoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeColor: ThemeColor = ThemeColor.DEFAULT,
    uiStyle: UiStyle = UiStyle.MATERIAL3,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT, ThemeMode.DYNAMIC_LIGHT -> false
        ThemeMode.DARK, ThemeMode.DYNAMIC_DARK -> true
        ThemeMode.SYSTEM, ThemeMode.DYNAMIC_SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val view = LocalView.current
    SideEffect {
        val window = (view.context.findActivity() ?: context.findActivity())?.window
            ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val colorScheme: ColorScheme = when {
        // Android 12+ 动态颜色
        themeMode.isDynamic && themeColor != ThemeColor.DEFAULT -> {
            if (darkTheme) themedDarkColorScheme(themeColorToColor(themeColor)) else themedLightColorScheme(themeColorToColor(themeColor))
        }

        themeMode.isDynamic -> {
            val dynamicScheme = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            dynamicScheme
        }

        // 静态 fallback
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalUiStyle provides uiStyle,
        LocalDevInfoDarkTheme provides darkTheme,
    ) {
        when (uiStyle) {
            UiStyle.MIUIX -> MiuixDevInfoTheme(
                themeMode = themeMode,
                themeColor = themeColor,
                darkTheme = darkTheme,
                content = content,
            )

            UiStyle.MATERIAL3 -> MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content,
            )
        }
    }
}

private fun themedLightColorScheme(seed: Color): ColorScheme {
    val primaryContainer = lerp(seed, Color.White, 0.82f)
    return lightColorScheme(
        primary = seed,
        onPrimary = onColorFor(seed),
        primaryContainer = primaryContainer,
        onPrimaryContainer = seed,
        secondary = lerp(seed, BlueGrey40, 0.45f),
        tertiary = lerp(seed, Sky40, 0.45f),
        secondaryContainer = lerp(seed, Color.White, 0.86f),
        tertiaryContainer = lerp(seed, Color.White, 0.86f)
    )
}

private fun themedDarkColorScheme(seed: Color): ColorScheme {
    val primary = lerp(seed, Color.White, 0.45f)
    return darkColorScheme(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = lerp(seed, Color.Black, 0.55f),
        onPrimaryContainer = Color.White,
        secondary = lerp(seed, BlueGrey80, 0.45f),
        tertiary = lerp(seed, Sky80, 0.45f),
        secondaryContainer = lerp(seed, Color.Black, 0.58f),
        tertiaryContainer = lerp(seed, Color.Black, 0.58f)
    )
}

private fun onColorFor(color: Color): Color = if (color.luminance() > 0.4f) Color.Black else Color.White

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
