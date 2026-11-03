/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main
import com.fioiu8.devinfo.feature.main.R
import com.fioiu8.devinfo.ui.DevInfoExpressiveSwitch
import com.fioiu8.devinfo.ui.DevInfoSegmentedDropdownItem
import com.fioiu8.devinfo.ui.displayNameResId

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults as MaterialCardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onBack: () -> Unit,
) {
    val selectedThemeIndex = themeMode.baseIndex
    val themeTabs = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    MaterialScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MaterialTopAppBar(
                title = {
                    MaterialText(
                        text = stringResource(R.string.theme_settings_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    MaterialIconButton(onClick = onBack) {
                        MaterialIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
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
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeTabs.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedThemeIndex == index,
                            onClick = { onThemeModeChange(themeMode.withBaseIndex(index)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = themeTabs.size,
                            ),
                            label = { MaterialText(label) },
                        )
                    }
                }
            }

            item {
                MaterialCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = MaterialCardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MaterialIcon(
                                imageVector = Icons.Rounded.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                MaterialText(
                                    text = stringResource(R.string.theme_monet),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                MaterialText(
                                    text = stringResource(R.string.theme_monet_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DevInfoExpressiveSwitch(
                                checked = themeMode.isDynamic,
                                onCheckedChange = { enabled ->
                                    onThemeModeChange(themeMode.withMonet(enabled))
                                },
                            )
                        }

                    }
                }
            }

            if (themeMode.isDynamic) {
                item {
                    MaterialCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = MaterialCardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                MaterialThemeSwitchGroup(
                    enablePredictiveBack = enablePredictiveBack,
                    onEnablePredictiveBackChange = onEnablePredictiveBackChange,
                )
            }
            item {
                MaterialPageScaleCard(
                    pageScale = pageScale,
                    onPageScaleChange = onPageScaleChange,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
            if (themeMode.isDynamic) {
                item {
                    DevInfoSegmentedDropdownItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = stringResource(R.string.theme_palette_style),
                        summary = PaletteStyle.entries[PaletteStyle.entries.indexOf(paletteStyle).coerceAtLeast(0)].name,
                        items = PaletteStyle.entries.map { it.name },
                        selectedIndex = PaletteStyle.entries.indexOf(paletteStyle).coerceAtLeast(0),
                        onItemSelected = { index ->
                            PaletteStyle.entries.getOrNull(index)?.let(onPaletteStyleChange)
                        },
                    )
                }
                item {
                    DevInfoSegmentedDropdownItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = stringResource(R.string.theme_color_spec),
                        summary = com.fioiu8.devinfo.core.model.ColorSpec.entries[com.fioiu8.devinfo.core.model.ColorSpec.entries.indexOf(colorSpec).coerceAtLeast(0)].name,
                        items = com.fioiu8.devinfo.core.model.ColorSpec.entries.map { it.name },
                        selectedIndex = com.fioiu8.devinfo.core.model.ColorSpec.entries.indexOf(colorSpec).coerceAtLeast(0),
                        onItemSelected = { index ->
                            com.fioiu8.devinfo.core.model.ColorSpec.entries.getOrNull(index)?.let(onColorSpecChange)
                        },
                    )
                }
            }

        }
    }
}

@Composable
private fun MaterialThemeSwitchGroup(
    enablePredictiveBack: Boolean,
    onEnablePredictiveBackChange: (Boolean) -> Unit,
) {
    MaterialCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = MaterialCardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column {
            MaterialThemeSwitchRow(
                title = stringResource(R.string.theme_predictive_back),
                summary = stringResource(R.string.theme_predictive_back_summary),
                checked = enablePredictiveBack,
                onCheckedChange = onEnablePredictiveBackChange,
            )
        }
    }
}

@Composable
private fun MaterialThemeSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MaterialText(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            MaterialText(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DevInfoExpressiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun MaterialPageScaleCard(
    pageScale: Float,
    onPageScaleChange: (Float) -> Unit,
) {
    var sliderValue by remember(pageScale) { mutableFloatStateOf(pageScale) }

    MaterialCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = MaterialCardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MaterialText(
                        text = stringResource(R.string.theme_page_scale),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    MaterialText(
                        text = stringResource(R.string.theme_page_scale_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MaterialText(
                    text = "${(sliderValue * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MaterialSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onPageScaleChange(sliderValue) },
                valueRange = 0.8f..1.1f,
                modifier = Modifier.fillMaxWidth(),
            )
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
    MaterialCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp),
        shape = RoundedCornerShape(16.dp),
        colors = MaterialCardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
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
