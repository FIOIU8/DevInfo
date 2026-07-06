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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
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
 * DevInfo 主题 — 支持动态颜色（Android 12+）、自定义种子色、手动浅色/深色。
 *
 * @param themeMode 主题模式
 * @param seedColor 自定义种子颜色；非 null 时覆盖系统壁纸色的 primary token
 * @param content Composable 内容
 */
@Composable
fun DevInfoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT, ThemeMode.DYNAMIC_LIGHT -> false
        ThemeMode.DARK, ThemeMode.DYNAMIC_DARK -> true
        ThemeMode.SYSTEM, ThemeMode.DYNAMIC_SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme: ColorScheme = when {
        // Android 12+ 动态颜色
        themeMode.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            // 若有自定义种子色，覆盖 primary 及相关色
            if (seedColor != null) {
                dynamicScheme.copy(
                    primary = seedColor,
                    // 使用 seed 衍生 onPrimary（简单明暗判断）
                    onPrimary = onPrimaryFor(seedColor)
                )
            } else {
                dynamicScheme
            }
        }

        // 动态模式但系统 < Android 12 → 从 seedColor 生成近似方案
        themeMode.isDynamic && seedColor != null -> {
            if (darkTheme) darkColorScheme(
                primary = seedColor,
                onPrimary = onPrimaryFor(seedColor)
            )
            else lightColorScheme(
                primary = seedColor,
                onPrimary = onPrimaryFor(seedColor)
            )
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

/** 根据 primary 颜色亮度自动选择白/黑 onPrimary */
private fun onPrimaryFor(primary: Color): Color =
    if (primary.luminance() > 0.4f) Color.Black else Color.White

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
