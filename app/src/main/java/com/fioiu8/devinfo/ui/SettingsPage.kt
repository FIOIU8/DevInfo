package com.fioiu8.devinfo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页面 — 主题、导出工具、关于入口。
 *
 * @param versionName 应用版本名
 * @param versionCode 应用版本号
 * @param uiStyle 当前界面风格
 * @param onUiStyleChange 界面风格切换回调
 * @param themeMode 当前主题模式
 * @param themeOptions 所有主题模式的展示名列表
 * @param onThemeChange 主题选中索引回调
 * @param onExportClick 导出模块点击回调
 * @param onAboutClick 关于页面点击回调
 */
@Composable
fun SettingsPage(
    versionName: String,
    versionCode: Long,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    themeMode: ThemeMode,
    themeOptions: List<String>,
    onThemeChange: (Int) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    onExportClick: () -> Unit,
    onAboutClick: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    languageOptions: List<String> = emptyList(),
    onLanguageChange: (Int) -> Unit = {},
    customLocaleTag: String = "",
    onCustomLocaleTagChange: (String) -> Unit = {}
) {

    var showCustomLocaleDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeColorDialog by rememberSaveable { mutableStateOf(false) }
    var customLocaleInput by rememberSaveable { mutableStateOf(customLocaleTag) }
    val uiStyleEntries = listOf(
        UiStyle.MATERIAL3 to stringResource(R.string.ui_style_material3),
        UiStyle.MIUIX to stringResource(R.string.ui_style_miuix)
    )
    val selectedUiStyleIndex = uiStyleEntries
        .indexOfFirst { (style, _) -> style == uiStyle }
        .coerceAtLeast(0)

    // Custom locale dialog
    if (showCustomLocaleDialog) {
        AlertDialog(
            onDismissRequest = { showCustomLocaleDialog = false },
            title = {
                Text(stringResource(R.string.custom_locale_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(stringResource(R.string.custom_locale_hint), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customLocaleInput,
                        onValueChange = { customLocaleInput = it },
                        label = { Text(stringResource(R.string.custom_locale_title)) },
                        placeholder = { Text(stringResource(R.string.custom_locale_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomLocaleDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCustomLocaleTagChange(customLocaleInput.trim())
                    showCustomLocaleDialog = false
                }) {
                    Text(stringResource(R.string.custom_locale_apply))
                }
            }
        )
    }

    ThemeColorPickerDialog(
        show = showThemeColorDialog,
        selectedColor = themeColor,
        onColorSelected = onThemeColorChange,
        onDismiss = { showThemeColorDialog = false }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── 外观 ──
        item { CategoryHeader(title = stringResource(R.string.category_appearance)) }

        item {
            DropdownPreferenceCard(
                icon = Icons.Outlined.Style,
                title = stringResource(R.string.ui_style),
                summary = uiStyleEntries[selectedUiStyleIndex].second,
                items = uiStyleEntries.map { (_, label) -> label },
                selectedIndex = selectedUiStyleIndex,
                onSelectedIndexChange = { index ->
                    uiStyleEntries.getOrNull(index)?.first?.let(onUiStyleChange)
                }
            )
        }

        item {
            ThemeSettingsCard(
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                themeColor = themeColor,
                onThemeColorClick = { showThemeColorDialog = true }
            )
        }

        // -- Language --
        item { Spacer(modifier = Modifier.height(8.dp)); CategoryHeader(title = stringResource(R.string.category_language)) }

        item {
            DropdownPreferenceCard(
                icon = Icons.Outlined.Translate,
                title = stringResource(R.string.language_setting),
                summary = languageOptions.getOrElse(AppLanguage.entries.indexOf(appLanguage)) { stringResource(R.string.language_system) },
                items = languageOptions,
                selectedIndex = AppLanguage.entries.indexOf(appLanguage),
                onSelectedIndexChange = { index ->
                    val selected = AppLanguage.entries[index]
                    if (selected.isCustom) {
                        customLocaleInput = customLocaleTag
                        showCustomLocaleDialog = true
                    } else {
                        onLanguageChange(index)
                    }
                }
            )
        }

        // ── 工具 ──
        item { Spacer(modifier = Modifier.height(8.dp)); CategoryHeader(title = stringResource(R.string.category_tools)) }

        item {
            ActionPreferenceCard(
                icon = Icons.Outlined.FileDownload,
                title = stringResource(R.string.export_tool),
                summary = stringResource(R.string.export_summary),
                onClick = onExportClick
            )
        }

        // ── 关于 ──
        item { Spacer(modifier = Modifier.height(8.dp)); CategoryHeader(title = stringResource(R.string.category_about)) }

        item {
            ActionPreferenceCard(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.about_app),
                summary = stringResource(R.string.about_summary),
                onClick = onAboutClick
            )
        }

        // Footer
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.footer_tag),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.version) + " $versionName ($versionCode)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.copyright),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
// ── Reusable MD3 Preference Components ──

/** Theme controls: system-follow, official light/dark button group, and dynamic color. */
@Composable
private fun ThemeSettingsCard(
    themeMode: ThemeMode,
    onThemeChange: (Int) -> Unit,
    themeColor: ThemeColor,
    onThemeColorClick: () -> Unit
) {
    val followsSystem = themeMode == ThemeMode.SYSTEM || themeMode == ThemeMode.DYNAMIC_SYSTEM
    val usesDynamicColor = themeMode.isDynamic
    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK, ThemeMode.DYNAMIC_DARK -> true
        ThemeMode.LIGHT, ThemeMode.DYNAMIC_LIGHT -> false
        else -> systemIsDark
    }

    fun updateTheme(
        followSystem: Boolean = followsSystem,
        dark: Boolean = isDark,
        dynamic: Boolean = usesDynamicColor
    ) {
        val nextMode = when {
            followSystem && dynamic -> ThemeMode.DYNAMIC_SYSTEM
            followSystem -> ThemeMode.SYSTEM
            dark && dynamic -> ThemeMode.DYNAMIC_DARK
            dark -> ThemeMode.DARK
            dynamic -> ThemeMode.DYNAMIC_LIGHT
            else -> ThemeMode.LIGHT
        }
        onThemeChange(ThemeMode.entries.indexOf(nextMode))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PreferenceSwitchRow(
                icon = Icons.Outlined.DarkMode,
                title = stringResource(R.string.theme_follow_system),
                summary = stringResource(R.string.theme_follow_system_summary),
                checked = followsSystem,
                checkedIcon = if (systemIsDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                uncheckedIcon = Icons.Filled.Close,
                onCheckedChange = { updateTheme(followSystem = it) }
            )

            AnimatedVisibility(
                visible = !followsSystem,
                enter = fadeIn() + expandVertically() + slideInVertically { -it / 2 },
                exit = fadeOut() + shrinkVertically() + slideOutVertically { -it / 2 }
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            stringResource(R.string.theme_mode_light),
                            stringResource(R.string.theme_mode_dark)
                        ).forEachIndexed { index, label ->
                            val buttonIsDark = index == 1
                            SegmentedButton(
                                selected = isDark == buttonIsDark,
                                onClick = { updateTheme(dark = buttonIsDark) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                icon = { SegmentedButtonDefaults.Icon(isDark == buttonIsDark) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PreferenceSwitchRow(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.theme_dynamic_color),
                summary = stringResource(R.string.theme_dynamic_color_summary),
                checked = usesDynamicColor,
                checkedIcon = Icons.Filled.Check,
                uncheckedIcon = Icons.Filled.Close,
                onCheckedChange = { updateTheme(dynamic = it) }
            )

            AnimatedVisibility(
                visible = usesDynamicColor,
                enter = fadeIn() + expandVertically() + slideInVertically { -it / 2 },
                exit = fadeOut() + shrinkVertically() + slideOutVertically { -it / 2 }
            ) {
                ThemeColorSelectorRow(
                    themeColor = themeColor,
                    onClick = onThemeColorClick,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeColorSelectorRow(
    themeColor: ThemeColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = themeColor.color,
            shape = RoundedCornerShape(6.dp)
        ) {}
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.theme_color),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(themeColor.displayNameResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    checkedIcon: ImageVector,
    uncheckedIcon: ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    imageVector = if (checked) checkedIcon else uncheckedIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/** 点击后触发操作的首选项卡片 */
@Composable
private fun ActionPreferenceCard(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/** 禁用状态的首选项卡片，仅展示信息不可点击 */
@Composable
private fun DisabledPreferenceCard(
    icon: ImageVector,
    title: String,
    summary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

/** 带下拉菜单的通用首选项卡片 */
@Composable
private fun DropdownPreferenceCard(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialDropdownPreferenceCard(
            icon = icon,
            title = title,
            summary = summary,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange
        )

        UiStyle.MIUIX -> MiuixDropdownPreferenceCard(
            icon = icon,
            title = title,
            summary = summary,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange
        )
    }
}

@Composable
private fun MaterialDropdownPreferenceCard(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                                )
                                if (index == selectedIndex) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectedIndexChange(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixDropdownPreferenceCard(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        OverlayDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            summary = summary,
            modifier = Modifier.fillMaxWidth(),
            startAction = {
                MiuixIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
            },
            showValue = false,
            onSelectedIndexChange = onSelectedIndexChange
        )
    }
}
