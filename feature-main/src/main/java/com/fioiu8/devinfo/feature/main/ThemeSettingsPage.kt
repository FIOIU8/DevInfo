/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main
import com.fioiu8.devinfo.feature.main.R
import com.fioiu8.devinfo.ui.DevInfoExpressiveSwitch
import com.fioiu8.devinfo.ui.DevInfoSegmentedControl
import com.fioiu8.devinfo.ui.DevInfoSegmentedDropdownItem
import com.fioiu8.devinfo.ui.SegmentedGroup
import com.fioiu8.devinfo.ui.SegmentedRow
import com.fioiu8.devinfo.ui.SegmentedSliderRow
import com.fioiu8.devinfo.ui.displayNameResId
import com.fioiu8.devinfo.ui.kit.liquid.LiquidGlassCard
import com.fioiu8.devinfo.ui.motion.rememberSystemAnimationsEnabled

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.core.model.MotionLevel
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.ui.theme.color
import com.fioiu8.devinfo.ui.theme.displayNameResId

/** 圆形颜色按钮：选中状态同时体现在边框粗细与文字颜色上。 */
@Composable
private fun MaterialColorCircle(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            // 单选语义：读屏软件才能感知选中状态，而非仅靠视觉边框
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        ColorSwatch(color = color, selected = selected)
        Spacer(Modifier.height(4.dp))
        MaterialText(
            text = label,
            fontSize = 10.sp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
) {
    val strokeWidth = if (selected) 3.dp else 1.dp
    val strokeColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(strokeWidth, strokeColor, CircleShape),
    )
}

/** Theme settings rendered with the active UI component system. */
@Composable
fun ThemeSettingsPage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    colorSpec: com.fioiu8.devinfo.core.model.ColorSpec,
    onColorSpecChange: (com.fioiu8.devinfo.core.model.ColorSpec) -> Unit,
    pageScale: Float,
    onPageScaleChange: (Float) -> Unit,
    enablePredictiveBack: Boolean,
    onEnablePredictiveBackChange: (Boolean) -> Unit,
    motionLevel: MotionLevel,
    onMotionLevelChange: (MotionLevel) -> Unit,
    pressAnimation: Boolean,
    onPressAnimationChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    MaterialThemeSettingsPage(
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        themeColor = themeColor,
        onThemeColorChange = onThemeColorChange,
        paletteStyle = paletteStyle,
        onPaletteStyleChange = onPaletteStyleChange,
        colorSpec = colorSpec,
        onColorSpecChange = onColorSpecChange,
        pageScale = pageScale,
        onPageScaleChange = onPageScaleChange,
        enablePredictiveBack = enablePredictiveBack,
        onEnablePredictiveBackChange = onEnablePredictiveBackChange,
        motionLevel = motionLevel,
        onMotionLevelChange = onMotionLevelChange,
        pressAnimation = pressAnimation,
        onPressAnimationChange = onPressAnimationChange,
        onBack = onBack,
    )
}

@Composable
private fun MaterialThemeSettingsPage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    colorSpec: com.fioiu8.devinfo.core.model.ColorSpec,
    onColorSpecChange: (com.fioiu8.devinfo.core.model.ColorSpec) -> Unit,
    pageScale: Float,
    onPageScaleChange: (Float) -> Unit,
    enablePredictiveBack: Boolean,
    onEnablePredictiveBackChange: (Boolean) -> Unit,
    motionLevel: MotionLevel,
    onMotionLevelChange: (MotionLevel) -> Unit,
    pressAnimation: Boolean,
    onPressAnimationChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val selectedThemeIndex = themeMode.baseIndex
    val themeTabs = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    DevInfoPageScaffold(
        title = stringResource(R.string.theme_settings_title),
        showBackButton = true,
        onBack = onBack,
    ) { innerPadding ->
        val systemAnimationsEnabled = rememberSystemAnimationsEnabled()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                MaterialThemePreviewCard(themeMode = themeMode, themeColor = themeColor)
            }

            item {
                SegmentedGroup(title = stringResource(R.string.category_appearance)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DevInfoSegmentedControl(
                            options = themeTabs,
                            selectedIndex = selectedThemeIndex,
                            onSelected = { index ->
                                onThemeModeChange(themeMode.withBaseIndex(index))
                            },
                        )
                    }
                    SegmentedRow(
                        title = stringResource(R.string.theme_monet),
                        supporting = stringResource(R.string.theme_monet_summary),
                        leadingIcon = Icons.Rounded.Wallpaper,
                        trailing = {
                            DevInfoExpressiveSwitch(
                                checked = themeMode.isDynamic,
                                onCheckedChange = { enabled ->
                                    onThemeModeChange(themeMode.withMonet(enabled))
                                },
                            )
                        },
                    )
                    if (themeMode.isDynamic) {
                        DevInfoSegmentedDropdownItem(
                            icon = Icons.Rounded.Wallpaper,
                            title = stringResource(R.string.theme_palette_style),
                            summary = PaletteStyle.entries[
                                PaletteStyle.entries.indexOf(paletteStyle).coerceAtLeast(0)
                            ].name,
                            items = PaletteStyle.entries.map { it.name },
                            selectedIndex = PaletteStyle.entries.indexOf(paletteStyle).coerceAtLeast(0),
                            onItemSelected = { index ->
                                PaletteStyle.entries.getOrNull(index)?.let(onPaletteStyleChange)
                            },
                        )
                        DevInfoSegmentedDropdownItem(
                            icon = Icons.Rounded.Wallpaper,
                            title = stringResource(R.string.theme_color_spec),
                            summary = com.fioiu8.devinfo.core.model.ColorSpec.entries[
                                com.fioiu8.devinfo.core.model.ColorSpec.entries
                                    .indexOf(colorSpec).coerceAtLeast(0)
                            ].name,
                            items = com.fioiu8.devinfo.core.model.ColorSpec.entries.map { it.name },
                            selectedIndex = com.fioiu8.devinfo.core.model.ColorSpec.entries
                                .indexOf(colorSpec).coerceAtLeast(0),
                            onItemSelected = { index ->
                                com.fioiu8.devinfo.core.model.ColorSpec.entries
                                    .getOrNull(index)?.let(onColorSpecChange)
                            },
                        )
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            MaterialText(
                                text = stringResource(R.string.theme_key_color),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ThemeColor.entries.forEach { color ->
                                    MaterialColorCircle(
                                        color = color.color,
                                        label = stringResource(color.displayNameResId()),
                                        selected = themeColor == color,
                                        onClick = { onThemeColorChange(color) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SegmentedGroup(title = stringResource(R.string.theme_motion_level)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DevInfoSegmentedControl(
                            options = MotionLevel.entries.map { stringResource(it.labelRes()) },
                            selectedIndex = MotionLevel.entries.indexOf(motionLevel),
                            onSelected = { index ->
                                MotionLevel.entries.getOrNull(index)?.let(onMotionLevelChange)
                            },
                        )
                        MaterialText(
                            text = stringResource(
                                if (systemAnimationsEnabled) {
                                    R.string.theme_motion_level_summary
                                } else {
                                    R.string.motion_system_disabled
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SegmentedRow(
                        title = stringResource(R.string.settings_press_animation),
                        supporting = stringResource(R.string.settings_press_animation_summary),
                        trailing = {
                            DevInfoExpressiveSwitch(
                                checked = pressAnimation,
                                onCheckedChange = onPressAnimationChange,
                            )
                        },
                    )
                }
            }

            item {
                SegmentedGroup(title = stringResource(R.string.settings_group_navigation)) {
                    SegmentedRow(
                        title = stringResource(R.string.theme_predictive_back),
                        supporting = stringResource(R.string.theme_predictive_back_summary),
                        trailing = {
                            DevInfoExpressiveSwitch(
                                checked = enablePredictiveBack,
                                onCheckedChange = onEnablePredictiveBackChange,
                            )
                        },
                    )
                }
            }

            item {
                SegmentedGroup(title = stringResource(R.string.settings_group_display)) {
                    SegmentedSliderRow(
                        title = stringResource(R.string.theme_page_scale),
                        supporting = stringResource(R.string.theme_page_scale_summary),
                        value = pageScale,
                        valueRange = 0.8f..1.1f,
                        valueFormatter = { value -> "${(value * 100).toInt()}%" },
                        onValueChangeFinished = onPageScaleChange,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MaterialThemePreviewCard(
    themeMode: ThemeMode,
    themeColor: ThemeColor,
) {
    val accent = if (themeMode.isDynamic && themeColor != ThemeColor.DEFAULT) {
        themeColor.color
    } else {
        MaterialTheme.colorScheme.primary
    }
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(42.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    MaterialText(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    MaterialText(
                        text = stringResource(R.string.theme_preview_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(accent),
                )
            }
        }
    }
}


private val ThemeMode.baseIndex: Int
    get() = when (this) {
        ThemeMode.SYSTEM, ThemeMode.DYNAMIC_SYSTEM -> 0
        ThemeMode.LIGHT, ThemeMode.DYNAMIC_LIGHT -> 1
        ThemeMode.DARK, ThemeMode.DYNAMIC_DARK -> 2
    }

private fun ThemeMode.withBaseIndex(index: Int): ThemeMode = when (index.coerceIn(0, 2)) {
    0 -> if (isDynamic) ThemeMode.DYNAMIC_SYSTEM else ThemeMode.SYSTEM
    1 -> if (isDynamic) ThemeMode.DYNAMIC_LIGHT else ThemeMode.LIGHT
    else -> if (isDynamic) ThemeMode.DYNAMIC_DARK else ThemeMode.DARK
}

private fun ThemeMode.withMonet(enabled: Boolean): ThemeMode = when (baseIndex) {
    0 -> if (enabled) ThemeMode.DYNAMIC_SYSTEM else ThemeMode.SYSTEM
    1 -> if (enabled) ThemeMode.DYNAMIC_LIGHT else ThemeMode.LIGHT
    else -> if (enabled) ThemeMode.DYNAMIC_DARK else ThemeMode.DARK
}

private fun MotionLevel.labelRes(): Int = when (this) {
    MotionLevel.FULL -> R.string.motion_full
    MotionLevel.STANDARD -> R.string.motion_standard
    MotionLevel.MINIMAL -> R.string.motion_minimal
    MotionLevel.OFF -> R.string.motion_off
}
