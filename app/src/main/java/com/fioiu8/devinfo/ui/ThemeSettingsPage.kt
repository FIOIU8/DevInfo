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

package com.fioiu8.devinfo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults as MaterialCardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.PaletteStyle
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 圆形颜色按钮（参考 KernelSU-Style-UI-Kit 的 ColorButtonMaterial） */
@Composable
private fun ColorCircle(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(3.dp, borderColor, CircleShape)
                    else Modifier.border(1.dp, borderColor, CircleShape)
                ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
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

/** Theme settings rendered with the active UI component system. */
@Composable
fun ThemeSettingsPage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    colorSpec: com.fioiu8.devinfo.model.ColorSpec,
    onColorSpecChange: (com.fioiu8.devinfo.model.ColorSpec) -> Unit,
    onBack: () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialThemeSettingsPage(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            themeColor = themeColor,
            onThemeColorChange = onThemeColorChange,
            paletteStyle = paletteStyle,
            onPaletteStyleChange = onPaletteStyleChange,
            colorSpec = colorSpec,
            onColorSpecChange = onColorSpecChange,
            onBack = onBack,
        )

        UiStyle.MIUIX -> MiuixThemeSettingsPage(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            themeColor = themeColor,
            onThemeColorChange = onThemeColorChange,
            paletteStyle = paletteStyle,
            onPaletteStyleChange = onPaletteStyleChange,
            colorSpec = colorSpec,
            onColorSpecChange = onColorSpecChange,
            onBack = onBack,
        )
    }
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
    colorSpec: com.fioiu8.devinfo.model.ColorSpec,
    onColorSpecChange: (com.fioiu8.devinfo.model.ColorSpec) -> Unit,
    onBack: () -> Unit,
) {
    val selectedThemeIndex = themeMode.baseIndex
    val themeTabs = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    val colorItems = ThemeColor.entries.map { stringResource(it.displayNameResId) }

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
                                    ColorCircle(
                                        color = color.color,
                                        label = stringResource(color.displayNameResId),
                                        selected = themeColor == color,
                                        onClick = { onThemeColorChange(color) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            if (themeMode.isDynamic) {
                item {
                    DevInfoSegmentedDropdownItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = "色彩风格",
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
                        title = "色彩标准",
                        summary = com.fioiu8.devinfo.model.ColorSpec.entries[com.fioiu8.devinfo.model.ColorSpec.entries.indexOf(colorSpec).coerceAtLeast(0)].name,
                        items = com.fioiu8.devinfo.model.ColorSpec.entries.map { it.name },
                        selectedIndex = com.fioiu8.devinfo.model.ColorSpec.entries.indexOf(colorSpec).coerceAtLeast(0),
                        onItemSelected = { index ->
                            com.fioiu8.devinfo.model.ColorSpec.entries.getOrNull(index)?.let(onColorSpecChange)
                        },
                    )
                }
            }

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

/** Miuix theme settings interaction adapted from KernelSU-Style-UI-Kit. */
@Composable
private fun MiuixThemeSettingsPage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    paletteStyle: PaletteStyle,
    onPaletteStyleChange: (PaletteStyle) -> Unit,
    colorSpec: com.fioiu8.devinfo.model.ColorSpec,
    onColorSpecChange: (com.fioiu8.devinfo.model.ColorSpec) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val selectedThemeIndex = themeMode.baseIndex
    val themeTabs = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    val colorItems = ThemeColor.entries.map { stringResource(it.displayNameResId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.theme_settings_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        val layoutDirection = LocalLayoutDirection.current
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.graphicsLayer {
                                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                            },
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Spacer(Modifier.height(28.dp))
                MiuixThemePreviewCard(themeMode = themeMode, themeColor = themeColor)
                Spacer(Modifier.height(56.dp))

                TabRow(
                    tabs = themeTabs,
                    selectedTabIndex = selectedThemeIndex,
                    onTabSelected = { index ->
                        onThemeModeChange(themeMode.withBaseIndex(index))
                    },
                    height = 48.dp,
                )

                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.theme_monet),
                        summary = stringResource(R.string.theme_monet_summary),
                        checked = themeMode.isDynamic,
                        onCheckedChange = { enabled ->
                            onThemeModeChange(themeMode.withMonet(enabled))
                        },
                        startAction = {
                            Icon(
                                imageVector = Icons.Rounded.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        },
                    )

                    AnimatedVisibility(visible = themeMode.isDynamic) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.theme_key_color),
                            items = colorItems,
                            selectedIndex = ThemeColor.entries.indexOf(themeColor).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                ThemeColor.entries.getOrNull(index)?.let(onThemeColorChange)
                            },
                            startAction = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(themeColor.color),
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MiuixThemePreviewCard(
    themeMode: ThemeMode,
    themeColor: ThemeColor,
) {
    val accent = if (themeMode.isDynamic && themeColor != ThemeColor.DEFAULT) {
        themeColor.color
    } else {
        MiuixTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.theme_preview_summary),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
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
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
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
