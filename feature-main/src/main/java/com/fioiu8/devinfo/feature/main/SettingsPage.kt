/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main
import com.fioiu8.devinfo.ui.DevInfoExpressiveSwitch
import com.fioiu8.devinfo.ui.DevInfoSegmentedDropdownItem
import com.fioiu8.devinfo.ui.SegmentedGroup
import com.fioiu8.devinfo.ui.SegmentedRow
import com.fioiu8.devinfo.feature.main.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info

import androidx.compose.material.icons.outlined.Palette

import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.fioiu8.devinfo.data.PreferenceValidators
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.data.AppLanguage

/** Settings entry page. Theme controls live on [ThemeSettingsPage]. */
@Composable
fun SettingsPage(
    versionName: String,
    versionCode: Long,
    onThemeSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onAboutClick: () -> Unit,
    onRootHelpClick: () -> Unit,
    checkUpdate: Boolean,
    onCheckUpdateChange: (Boolean) -> Unit,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    languageOptions: List<String> = emptyList(),
    onLanguageChange: (Int) -> Unit = {},
    customLocaleTag: String = "",
    onCustomLocaleTagChange: (String) -> Boolean = { false },
) {
    MaterialSettingsPage(
        versionName = versionName,
        versionCode = versionCode,
        onThemeSettingsClick = onThemeSettingsClick,
        onExportClick = onExportClick,
        onAboutClick = onAboutClick,
        onRootHelpClick = onRootHelpClick,
        checkUpdate = checkUpdate,
        onCheckUpdateChange = onCheckUpdateChange,
        appLanguage = appLanguage,
        languageOptions = languageOptions,
        onLanguageChange = onLanguageChange,
        customLocaleTag = customLocaleTag,
        onCustomLocaleTagChange = onCustomLocaleTagChange,
    )
}

@Composable
private fun MaterialSettingsPage(
    versionName: String,
    versionCode: Long,
    onThemeSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onAboutClick: () -> Unit,
    onRootHelpClick: () -> Unit,
    checkUpdate: Boolean,
    onCheckUpdateChange: (Boolean) -> Unit,
    appLanguage: AppLanguage,
    languageOptions: List<String>,
    onLanguageChange: (Int) -> Unit,
    customLocaleTag: String,
    onCustomLocaleTagChange: (String) -> Boolean,
) {

    var showCustomLocaleDialog by rememberSaveable { mutableStateOf(false) }
    var customLocaleInput by rememberSaveable { mutableStateOf(customLocaleTag) }
    var customLocaleError by rememberSaveable { mutableStateOf(false) }
    if (showCustomLocaleDialog) {
        AlertDialog(
            onDismissRequest = { showCustomLocaleDialog = false },
            title = { Text(stringResource(R.string.custom_locale_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.custom_locale_hint))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customLocaleInput,
                        onValueChange = {
                            customLocaleInput = it
                            customLocaleError = false
                        },
                        label = { Text(stringResource(R.string.custom_locale_title)) },
                        placeholder = { Text(stringResource(R.string.custom_locale_placeholder)) },
                        singleLine = true,
                        isError = customLocaleError,
                        supportingText = if (customLocaleError) {
                            { Text(stringResource(R.string.custom_locale_invalid)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomLocaleDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!PreferenceValidators.isValidLocaleTag(customLocaleInput)) {
                            customLocaleError = true
                        } else if (onCustomLocaleTagChange(customLocaleInput.trim())) {
                            showCustomLocaleDialog = false
                        } else {
                            // 偏好层拒绝（例如存储写入失败）时保持对话框打开并给出反馈，
                            // 不再静默关闭让用户以为已生效。
                            customLocaleError = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.custom_locale_apply))
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 12.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SegmentedGroup(title = stringResource(R.string.settings_category_app)) {
                SegmentedRow(
                    title = stringResource(R.string.settings_check_update),
                    supporting = stringResource(R.string.settings_check_update_summary),
                    leadingIcon = Icons.Outlined.Update,
                    trailing = {
                        DevInfoExpressiveSwitch(
                            checked = checkUpdate,
                            onCheckedChange = onCheckUpdateChange,
                        )
                    },
                )
            }
        }
        item {
            SegmentedGroup(title = stringResource(R.string.category_appearance)) {
                SegmentedRow(
                    title = stringResource(R.string.theme_settings_title),
                    supporting = stringResource(R.string.theme_settings_summary),
                    leadingIcon = Icons.Outlined.Palette,
                    showChevron = true,
                    onClick = onThemeSettingsClick,
                )
            }
        }
        item {
            SegmentedGroup(title = stringResource(R.string.category_language)) {
                MaterialLanguageDropdown(
                    icon = Icons.Outlined.Translate,
                    title = stringResource(R.string.language_setting),
                    summary = stringResource(R.string.language_setting_summary),
                    items = languageOptions,
                    selectedIndex = AppLanguage.entries.indexOf(appLanguage),
                    onSelectedIndexChange = { index ->
                        val selected = AppLanguage.entries.getOrNull(index)
                            ?: return@MaterialLanguageDropdown
                        if (selected.isCustom) {
                            customLocaleInput = customLocaleTag
                            showCustomLocaleDialog = true
                        } else {
                            onLanguageChange(index)
                        }
                    },
                )
            }
        }
        item {
            SegmentedGroup(title = stringResource(R.string.category_tools)) {
                SegmentedRow(
                    title = stringResource(R.string.export_tool),
                    supporting = stringResource(R.string.export_summary),
                    leadingIcon = Icons.Outlined.FileDownload,
                    showChevron = true,
                    onClick = onExportClick,
                )
                SegmentedRow(
                    title = stringResource(R.string.root_help_title),
                    supporting = stringResource(R.string.root_help_settings_summary),
                    leadingIcon = Icons.Outlined.Security,
                    showChevron = true,
                    onClick = onRootHelpClick,
                )
            }
        }
        item {
            SegmentedGroup(title = stringResource(R.string.category_about)) {
                SegmentedRow(
                    title = stringResource(R.string.about_app),
                    supporting = stringResource(R.string.about_summary),
                    leadingIcon = Icons.Outlined.Info,
                    showChevron = true,
                    onClick = onAboutClick,
                )
            }
        }
        item {
            Text(
                text = "${stringResource(R.string.copyright)} · ${stringResource(R.string.version)} $versionName ($versionCode)",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}


@Composable
private fun MaterialLanguageDropdown(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    DevInfoSegmentedDropdownItem(
        icon = icon,
        title = title,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = onSelectedIndexChange,
        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
