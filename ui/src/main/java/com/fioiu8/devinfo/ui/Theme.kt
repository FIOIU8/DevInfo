/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.fioiu8.devinfo.core.model.ColorSpec
import com.fioiu8.devinfo.core.model.MotionLevel
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.core.model.effectiveMotionLevel
import com.fioiu8.devinfo.ui.motion.LocalDevInfoMotionLevel
import com.fioiu8.devinfo.ui.motion.LocalDevInfoMotionScheme
import com.fioiu8.devinfo.ui.motion.LocalDevInfoPressAnimationEnabled
import com.fioiu8.devinfo.ui.motion.rememberSystemAnimationsEnabled

/** 当前是否处于深色配色。图表等非 Compose 组件的自绘色需要按明暗切换。 */
private val LocalDevInfoDarkTheme = staticCompositionLocalOf { false }

/**
 * 是否启用液态玻璃表面。
 *
 * 关闭后 [com.fioiu8.devinfo.ui.DevInfoCard] 会退回纯 Material 3 色调表面
 * （tonal surface + M3 shape），此时界面不含任何自绘玻璃层。
 */
val LocalDevInfoGlassEnabled = staticCompositionLocalOf { true }

@Composable
fun isInDarkTheme(): Boolean = LocalDevInfoDarkTheme.current

// 静态 fallback — 用于非动态模式或用户选择固定主题色
/**
 * Material 3 Expressive 形状尺度。分组容器（24dp）与组内行（20dp）由
 * [com.fioiu8.devinfo.ui.SegmentedGroup] 组合 `largeIncreased` / `large` token，
 * 并通过 4dp 内边距保证嵌套圆角同心。
 */
private val DevInfoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(36.dp),
)

/**
 * 按动效等级选择 M3 Expressive 的运动方案。
 *
 * material3 1.5.0 没有 `MotionScheme.none()`；[MotionLevel.OFF] 只能让应用自己的
 * 路由/入场过渡静止，Switch、Ripple 等组件内部动效仍由 Material 控制。
 */
private fun motionSchemeFor(level: MotionLevel): MotionScheme = when (level) {
    MotionLevel.FULL -> MotionScheme.expressive()
    MotionLevel.STANDARD, MotionLevel.MINIMAL, MotionLevel.OFF -> MotionScheme.standard()
}

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
 * DevInfo 主题 — 完整的 Material 3 Expressive 配色、形状、类型与动效。
 *
 * 动态模式下配色由种子色经 HCT 色板算法生成，[paletteStyle] 选择生成策略，
 * [colorSpec] 选择对比度档位；固定模式下使用内置的青绿品牌配色。
 *
 * [motionLevel] 控制 M3 Expressive 动效强度；系统「移除动画」会强制折算为
 * [MotionLevel.OFF]，并在回到前台时重新读取。[glassEnabled] 控制液态玻璃表面，
 * 关闭后全部卡片退回 M3 色调表面。
 *
 * @param themeMode 主题模式
 * @param themeColor 动态模式下的种子色
 * @param paletteStyle 动态模式下的色板生成策略
 * @param colorSpec 动态模式下的对比度档位
 * @param pressAnimationEnabled 是否启用按压缩放反馈
 * @param glassEnabled 是否启用液态玻璃表面
 * @param content Composable 内容
 */
@Composable
fun DevInfoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeColor: ThemeColor = ThemeColor.DEFAULT,
    paletteStyle: PaletteStyle = PaletteStyle.DEFAULT,
    colorSpec: ColorSpec = ColorSpec.DEFAULT,
    motionLevel: MotionLevel = MotionLevel.STANDARD,
    pressAnimationEnabled: Boolean = true,
    glassEnabled: Boolean = true,
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

    val colorScheme: ColorScheme = remember(context, themeMode, themeColor, paletteStyle, colorSpec, darkTheme) {
        when {
            // 动态模式：种子色来自用户选择，或来自系统动态色的主色
            themeMode.isDynamic -> {
                val seed = if (themeColor == ThemeColor.DEFAULT) {
                    if (darkTheme) {
                        dynamicDarkColorScheme(context).primary
                    } else {
                        dynamicLightColorScheme(context).primary
                    }
                } else {
                    themeColorToColor(themeColor)
                }
                materialColorScheme(
                    seed = seed,
                    dark = darkTheme,
                    paletteStyle = paletteStyle,
                    colorSpec = colorSpec,
                )
            }

            // 固定模式：内置品牌配色
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val systemAnimationsEnabled = rememberSystemAnimationsEnabled()
    val effectiveMotion = effectiveMotionLevel(motionLevel, systemAnimationsEnabled)
    val motionScheme = remember(effectiveMotion) { motionSchemeFor(effectiveMotion) }

    CompositionLocalProvider(
        LocalDevInfoDarkTheme provides darkTheme,
        LocalDevInfoGlassEnabled provides glassEnabled,
        LocalDevInfoMotionLevel provides effectiveMotion,
        LocalDevInfoMotionScheme provides motionScheme,
        LocalDevInfoPressAnimationEnabled provides pressAnimationEnabled,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            shapes = DevInfoShapes,
            typography = DevInfoTypography,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
