package com.fioiu8.devinfo.ui

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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.model.MountThemeColor
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.ThemeMode

/**
 * 设置页面 — 主题、导出工具、关于入口。
 *
 * @param versionName 应用版本名
 * @param versionCode 应用版本号
 * @param themeMode 当前主题模式
 * @param themeOptions 所有主题模式的展示名列表
 * @param onThemeChange 主题选中索引回调
 * @param mountThemeColor 当前主题色
 * @param mountColorOptions 所有可选主题色列表
 * @param selectedMountColorIndex 当前主题色选中索引
 * @param onMountColorChange 主题色选中索引回调
 * @param isDynamicMode 是否处于动态颜色模式
 * @param useMountTheme 是否启用自定义主题色
 * @param onExportClick 导出模块点击回调
 * @param onAboutClick 关于页面点击回调
 */
@Composable
fun SettingsPage(
    versionName: String,
    versionCode: Long,
    themeMode: ThemeMode,
    themeOptions: List<String>,
    onThemeChange: (Int) -> Unit,
    mountThemeColor: MountThemeColor,
    mountColorOptions: List<MountThemeColor>,
    selectedMountColorIndex: Int,
    onMountColorChange: (Int) -> Unit,
    isDynamicMode: Boolean,
    useMountTheme: Boolean,
    onExportClick: () -> Unit,
    onAboutClick: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    languageOptions: List<String> = emptyList(),
    onLanguageChange: (Int) -> Unit = {},
    customLocaleTag: String = "",
    onCustomLocaleTagChange: (String) -> Unit = {}
) {

    var showCustomLocaleDialog by remember { mutableStateOf(false) }
    var customLocaleInput by remember { mutableStateOf(customLocaleTag) }

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
                icon = Icons.Outlined.DarkMode,
                title = stringResource(R.string.theme_mode),
                summary = themeOptions[ThemeMode.entries.indexOf(themeMode)],
                items = themeOptions,
                selectedIndex = ThemeMode.entries.indexOf(themeMode),
                onSelectedIndexChange = onThemeChange
            )
        }

        if (isDynamicMode) {
            item {
                MountColorPreferenceCard(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.theme_color),
                    summary = if (useMountTheme) "已启用自定义颜色" else "选择应用的主题颜色",
                    colors = mountColorOptions,
                    selectedIndex = selectedMountColorIndex,
                    onSelectedIndexChange = onMountColorChange
                )
            }
        } else {
            item {
                DisabledPreferenceCard(
                    icon = Icons.Outlined.Palette,
                    title = "主题颜色",
                    summary = stringResource(R.string.theme_color_disabled)
                )
            }
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
    var expanded by remember { mutableStateOf(false) }

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

/** 带主题色预览的下拉首选项卡片 */
@Composable
private fun MountColorPreferenceCard(
    icon: ImageVector,
    title: String,
    summary: String,
    colors: List<MountThemeColor>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorPreview(
                            color = colors[selectedIndex].color,
                            size = 14.dp,
                            cornerRadius = 3.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                colors.forEachIndexed { index, entry ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorPreview(
                                    color = entry.color,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = entry.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
