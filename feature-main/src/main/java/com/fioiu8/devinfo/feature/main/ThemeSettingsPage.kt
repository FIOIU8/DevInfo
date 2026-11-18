/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import com.fioiu8.devinfo.feature.main.R
import com.fioiu8.devinfo.ui.DevInfoCard
import com.fioiu8.devinfo.ui.DevInfoCardLevel
import com.fioiu8.devinfo.ui.DevInfoExpressiveSwitch
import com.fioiu8.devinfo.ui.DevInfoSegmentedControl
import com.fioiu8.devinfo.ui.DevInfoSegmentedDropdownItem
import com.fioiu8.devinfo.ui.SegmentedGroup
import com.fioiu8.devinfo.ui.SegmentedRow
import com.fioiu8.devinfo.ui.SegmentedSliderRow
import com.fioiu8.devinfo.ui.displayNameResId
import com.fioiu8.devinfo.ui.motion.rememberSystemAnimationsEnabled

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.core.model.ColorSpec
import com.fioiu8.devinfo.core.model.MotionLevel
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.ui.theme.color
import com.fioiu8.devinfo.ui.theme.displayNameResId

/** 圆形颜色按钮：选中状态同时体现在边框粗细与文字颜色上。 */
@Composable
private fun ThemeColorCircle(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            // 单选语义：读屏软件才能感知选中状态，而非仅靠视觉边框
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.size(40.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color,
            border = androidx.compose.foundation.BorderStroke(
                width = if (selected) 3.dp else 1.dp,
                color = border,
            ),
            content = {},
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

/**
 * 主题设置页：外观、动效、导航与显示。
 *
 * 颜色风格与颜色标准只在动态模式下生效，因为它们描述的是「由种子色生成整套配色」
 * 的策略；固定模式使用内置品牌配色。
 */
@Composable
fun ThemeSettingsPage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    colorSpec: ColorSpec,
    onColorSpecChange: (ColorSpec) -> Unit,
    pageScale: Float,
    onPageScaleChange: (Float) -> Unit,
    enablePredictiveBack: Boolean,
    onEnablePredictiveBackChange: (Boolean) -> Unit,
    motionLevel: MotionLevel,
    onMotionLevelChange: (MotionLevel) -> Unit,
    pressAnimation: Boolean,
    onPressAnimationChange: (Boolean) -> Unit,
    glassEffect: Boolean,
    onGlassEffectChange: (Boolean) -> Unit,
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
                ThemePreviewCard()
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
                    SegmentedRow(
                        title = stringResource(R.string.theme_glass_effect),
                        supporting = stringResource(R.string.theme_glass_effect_summary),
                        trailing = {
                            DevInfoExpressiveSwitch(
                                checked = glassEffect,
                                onCheckedChange = onGlassEffectChange,
                            )
                        },
                    )
                    if (themeMode.isDynamic) {
                        DevInfoSegmentedDropdownItem(
                            icon = Icons.Rounded.Wallpaper,
                            title = stringResource(R.string.theme_palette_style),
                            summary = paletteStyle.name,
                            items = PaletteStyle.entries.map { it.name },
                            selectedIndex = PaletteStyle.entries.indexOf(paletteStyle).coerceAtLeast(0),
                            onItemSelected = { index ->
                                PaletteStyle.entries.getOrNull(index)?.let(onPaletteStyleChange)
                            },
                        )
                        DevInfoSegmentedDropdownItem(
                            icon = Icons.Rounded.Wallpaper,
                            title = stringResource(R.string.theme_color_spec),
                            summary = colorSpec.name,
                            items = ColorSpec.entries.map { it.name },
                            selectedIndex = ColorSpec.entries.indexOf(colorSpec).coerceAtLeast(0),
                            onItemSelected = { index ->
                                ColorSpec.entries.getOrNull(index)?.let(onColorSpecChange)
                            },
                        )
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            Text(
                                text = stringResource(R.string.theme_key_color),
                                style = MaterialTheme.typography.titleSmallEmphasized,
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
                                    ThemeColorCircle(
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
                        Text(
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

/**
 * 主题预览卡：用真实组件展示当前配色与形状，不再手绘色块。
 *
 * 开关的 `onCheckedChange` 为 null，因此只作展示，不会接受点击。
 */
@Composable
private fun ThemePreviewCard() {
    DevInfoCard(
        level = DevInfoCardLevel.EMPHASIS,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
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
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.theme_preview_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.theme_preview_toggle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                DevInfoExpressiveSwitch(
                    checked = true,
                    onCheckedChange = null,
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
