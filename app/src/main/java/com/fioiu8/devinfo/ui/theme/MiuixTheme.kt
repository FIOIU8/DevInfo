package com.fioiu8.devinfo.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
internal fun MiuixDevInfoTheme(
    themeMode: ThemeMode,
    themeColor: ThemeColor,
    darkTheme: Boolean,
    materialColorScheme: ColorScheme?,
    materialOverscrollFactory: OverscrollFactory?,
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

    MiuixTheme(controller = controller) {
        val miuixIndication = LocalIndication.current
        val resolvedMaterialColorScheme =
            materialColorScheme ?: MiuixTheme.colorScheme.toMaterialColorScheme(darkTheme)
        val overscrollFactory =
            if (materialColorScheme != null) materialOverscrollFactory else LocalOverscrollFactory.current
        MaterialTheme(
            colorScheme = resolvedMaterialColorScheme,
            typography = Typography,
        ) {
            val indication =
                if (materialColorScheme == null) miuixIndication else LocalIndication.current
            CompositionLocalProvider(
                LocalIndication provides indication,
                LocalOverscrollFactory provides overscrollFactory,
                content = content,
            )
        }
    }
}

private fun ThemeMode.toMiuixColorSchemeMode(): ColorSchemeMode = when (this) {
    ThemeMode.SYSTEM -> ColorSchemeMode.System
    ThemeMode.LIGHT -> ColorSchemeMode.Light
    ThemeMode.DARK -> ColorSchemeMode.Dark
    ThemeMode.DYNAMIC_SYSTEM -> ColorSchemeMode.MonetSystem
    ThemeMode.DYNAMIC_LIGHT -> ColorSchemeMode.MonetLight
    ThemeMode.DYNAMIC_DARK -> ColorSchemeMode.MonetDark
}

private fun Colors.toMaterialColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = primaryVariant,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = primaryVariant,
        onTertiary = onPrimaryVariant,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceSecondary,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = dividerLine,
        scrim = Color.Black,
        surfaceDim = surface,
        surfaceBright = background,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}
