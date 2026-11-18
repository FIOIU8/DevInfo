/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

// com.google.android.material 的 color.utilities 全体标了 @RestrictedApi(libGroup)，
// 因为官方只打算给自家组件内部使用。但它就是 Material 官方的 HCT 实现，
// Compose 侧没有等价的「按种子色生成整板 M3 配色」公开 API。
// 这里是有意识地依赖它：只用到纯函数式色板生成，不碰任何 View 组件。
@file:Suppress("RestrictedApi")

package com.fioiu8.devinfo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.fioiu8.devinfo.core.model.ColorSpec
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFidelity
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeRainbow
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant

/**
 * 使用 Material 的 HCT 色板算法把单个种子色展开成完整 M3 配色。
 *
 * 之前的手写 lerp 只覆盖 6 个角色，surface 系列仍是固定青绿，因此自定义主题色下
 * 容器层级不跟随种子色。这里改为按 Material 的 tone 规则生成全部 40 个角色。
 */

/**
 * [ColorSpec] 与对比度档位的对应关系。
 *
 * 说明：com.google.android.material 内置的 color.utilities 只实现 2021 规范，
 * 没有 Material 3 2025 规范的版本开关。因此这里把「颜色标准」映射为对比度档位：
 * `V0` 为标准对比度，`V1` 为增强对比度（0.35）。这保证两个选项都真实生效，
 * 而不是留在 UI 上点不动。
 */
private fun ColorSpec.contrastLevel(): Double = when (this) {
    ColorSpec.V1 -> 0.35
    ColorSpec.DEFAULT, ColorSpec.V0 -> 0.0
}

private fun PaletteStyle.toScheme(
    source: Hct,
    dark: Boolean,
    contrastLevel: Double,
): DynamicScheme = when (this) {
    PaletteStyle.DEFAULT, PaletteStyle.TONAL_SPOT -> SchemeTonalSpot(source, dark, contrastLevel)
    PaletteStyle.VIBRANT -> SchemeVibrant(source, dark, contrastLevel)
    PaletteStyle.EXPRESSIVE -> SchemeExpressive(source, dark, contrastLevel)
    PaletteStyle.FIDELITY -> SchemeFidelity(source, dark, contrastLevel)
    PaletteStyle.CONTENT -> SchemeContent(source, dark, contrastLevel)
    PaletteStyle.NEUTRAL -> SchemeNeutral(source, dark, contrastLevel)
    PaletteStyle.MONOCHROME -> SchemeMonochrome(source, dark, contrastLevel)
    PaletteStyle.RAINBOW -> SchemeRainbow(source, dark, contrastLevel)
    PaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(source, dark, contrastLevel)
}

/**
 * 由种子色、明暗、颜色风格和颜色标准生成完整的 Material 3 配色。
 *
 * @param seed 种子色，通常来自用户选择的主题色或系统动态色
 */
fun materialColorScheme(
    seed: Color,
    dark: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec,
): ColorScheme {
    val scheme = paletteStyle.toScheme(
        source = Hct.fromInt(seed.toArgb()),
        dark = dark,
        contrastLevel = colorSpec.contrastLevel(),
    )
    return scheme.toColorScheme()
}

/**
 * 把 [DynamicScheme] 的全部角色搬到 Compose 的 [ColorScheme]。
 *
 * 这些角色必须逐个显式搬运：`lightColorScheme()` / `darkColorScheme()` 的默认值来自
 * Material 基线配色，不填就等于把未覆盖的角色留在紫色基线上。
 */
private fun DynamicScheme.toColorScheme(): ColorScheme {
    val colors = MaterialDynamicColors()
    fun of(color: com.google.android.material.color.utilities.DynamicColor): Color =
        Color(color.getArgb(this))

    val values = ColorSchemeValues(
        primary = of(colors.primary()),
        onPrimary = of(colors.onPrimary()),
        primaryContainer = of(colors.primaryContainer()),
        onPrimaryContainer = of(colors.onPrimaryContainer()),
        inversePrimary = of(colors.inversePrimary()),
        secondary = of(colors.secondary()),
        onSecondary = of(colors.onSecondary()),
        secondaryContainer = of(colors.secondaryContainer()),
        onSecondaryContainer = of(colors.onSecondaryContainer()),
        tertiary = of(colors.tertiary()),
        onTertiary = of(colors.onTertiary()),
        tertiaryContainer = of(colors.tertiaryContainer()),
        onTertiaryContainer = of(colors.onTertiaryContainer()),
        background = of(colors.background()),
        onBackground = of(colors.onBackground()),
        surface = of(colors.surface()),
        onSurface = of(colors.onSurface()),
        surfaceVariant = of(colors.surfaceVariant()),
        onSurfaceVariant = of(colors.onSurfaceVariant()),
        surfaceTint = of(colors.surfaceTint()),
        inverseSurface = of(colors.inverseSurface()),
        inverseOnSurface = of(colors.inverseOnSurface()),
        error = of(colors.error()),
        onError = of(colors.onError()),
        errorContainer = of(colors.errorContainer()),
        onErrorContainer = of(colors.onErrorContainer()),
        outline = of(colors.outline()),
        outlineVariant = of(colors.outlineVariant()),
        scrim = of(colors.scrim()),
        surfaceBright = of(colors.surfaceBright()),
        surfaceDim = of(colors.surfaceDim()),
        surfaceContainer = of(colors.surfaceContainer()),
        surfaceContainerHigh = of(colors.surfaceContainerHigh()),
        surfaceContainerHighest = of(colors.surfaceContainerHighest()),
        surfaceContainerLow = of(colors.surfaceContainerLow()),
        surfaceContainerLowest = of(colors.surfaceContainerLowest()),
    )

    // 全部角色都已显式给出，light/dark 只决定初始基线，随后被 copy 全量覆盖。
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = values.primary,
        onPrimary = values.onPrimary,
        primaryContainer = values.primaryContainer,
        onPrimaryContainer = values.onPrimaryContainer,
        inversePrimary = values.inversePrimary,
        secondary = values.secondary,
        onSecondary = values.onSecondary,
        secondaryContainer = values.secondaryContainer,
        onSecondaryContainer = values.onSecondaryContainer,
        tertiary = values.tertiary,
        onTertiary = values.onTertiary,
        tertiaryContainer = values.tertiaryContainer,
        onTertiaryContainer = values.onTertiaryContainer,
        background = values.background,
        onBackground = values.onBackground,
        surface = values.surface,
        onSurface = values.onSurface,
        surfaceVariant = values.surfaceVariant,
        onSurfaceVariant = values.onSurfaceVariant,
        surfaceTint = values.surfaceTint,
        inverseSurface = values.inverseSurface,
        inverseOnSurface = values.inverseOnSurface,
        error = values.error,
        onError = values.onError,
        errorContainer = values.errorContainer,
        onErrorContainer = values.onErrorContainer,
        outline = values.outline,
        outlineVariant = values.outlineVariant,
        scrim = values.scrim,
        surfaceBright = values.surfaceBright,
        surfaceDim = values.surfaceDim,
        surfaceContainer = values.surfaceContainer,
        surfaceContainerHigh = values.surfaceContainerHigh,
        surfaceContainerHighest = values.surfaceContainerHighest,
        surfaceContainerLow = values.surfaceContainerLow,
        surfaceContainerLowest = values.surfaceContainerLowest,
    )
}

/** [DynamicScheme] 到 [ColorScheme] 的中间载体，避免两次构造 40 个具名参数。 */
private class ColorSchemeValues(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
)
