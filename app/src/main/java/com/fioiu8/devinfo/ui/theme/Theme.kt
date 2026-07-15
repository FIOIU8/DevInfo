package com.fioiu8.devinfo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode

// 静态 fallback — 用于非动态模式或 Android 12 以下
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
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
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT, ThemeMode.DYNAMIC_LIGHT -> false
        ThemeMode.DARK, ThemeMode.DYNAMIC_DARK -> true
        ThemeMode.SYSTEM, ThemeMode.DYNAMIC_SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme: ColorScheme = when {
        // Android 12+ 动态颜色
        themeMode.isDynamic && themeColor != ThemeColor.DEFAULT -> {
            if (darkTheme) themedDarkColorScheme(themeColor.color) else themedLightColorScheme(themeColor.color)
        }

        themeMode.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun themedLightColorScheme(seed: Color): ColorScheme {
    val primaryContainer = lerp(seed, Color.White, 0.82f)
    return lightColorScheme(
        primary = seed,
        onPrimary = onColorFor(seed),
        primaryContainer = primaryContainer,
        onPrimaryContainer = seed,
        secondary = lerp(seed, PurpleGrey40, 0.45f),
        tertiary = lerp(seed, Pink40, 0.45f),
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
        secondary = lerp(seed, PurpleGrey80, 0.45f),
        tertiary = lerp(seed, Pink80, 0.45f),
        secondaryContainer = lerp(seed, Color.Black, 0.58f),
        tertiaryContainer = lerp(seed, Color.Black, 0.58f)
    )
}

private fun onColorFor(color: Color): Color = if (color.luminance() > 0.4f) Color.Black else Color.White

// 保留兼容旧调用方（无参数 → 使用默认 SYSTEM + 动态色）
@Composable
@Suppress("unused")
fun DevInfoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val mode = if (dynamicColor) ThemeMode.DYNAMIC_SYSTEM else ThemeMode.SYSTEM
    // 忽略 darkTheme 参数，由 SYSTEM 模式自行判断
    DevInfoTheme(themeMode = mode, content = content)
}
