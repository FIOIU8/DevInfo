package com.fioiu8.devinfo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/** Settings entry page. Theme controls live on [ThemeSettingsPage]. */
@Composable
fun SettingsPage(
    versionName: String,
    versionCode: Long,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    onThemeSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onAboutClick: () -> Unit,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    languageOptions: List<String> = emptyList(),
    onLanguageChange: (Int) -> Unit = {},
    customLocaleTag: String = "",
    onCustomLocaleTagChange: (String) -> Unit = {},
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixSettingsPage(
            versionName = versionName,
            versionCode = versionCode,
            uiStyle = uiStyle,
            onUiStyleChange = onUiStyleChange,
            onThemeSettingsClick = onThemeSettingsClick,
            onExportClick = onExportClick,
            onAboutClick = onAboutClick,
            appLanguage = appLanguage,
            languageOptions = languageOptions,
            onLanguageChange = onLanguageChange,
            customLocaleTag = customLocaleTag,
            onCustomLocaleTagChange = onCustomLocaleTagChange,
        )
        return
    }

    var showCustomLocaleDialog by rememberSaveable { mutableStateOf(false) }
    var customLocaleInput by rememberSaveable { mutableStateOf(customLocaleTag) }
    val uiStyleEntries = listOf(
        UiStyle.MIUIX to stringResource(R.string.ui_style_miuix),
        UiStyle.MATERIAL3 to stringResource(R.string.ui_style_material3),
    )
    val selectedUiStyleIndex = uiStyleEntries.indexOfFirst { it.first == uiStyle }.coerceAtLeast(0)

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
                        onValueChange = { customLocaleInput = it },
                        label = { Text(stringResource(R.string.custom_locale_title)) },
                        placeholder = { Text(stringResource(R.string.custom_locale_placeholder)) },
                        singleLine = true,
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
                        onCustomLocaleTagChange(customLocaleInput.trim())
                        showCustomLocaleDialog = false
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { CategoryHeader(stringResource(R.string.category_appearance)) }
        item {
            UiStyleDropdown(
                title = stringResource(R.string.ui_style),
                items = uiStyleEntries.map { it.second },
                selectedIndex = selectedUiStyleIndex,
                onSelectedIndexChange = { index ->
                    uiStyleEntries.getOrNull(index)?.first?.let(onUiStyleChange)
                },
            )
        }
        item {
            PreferenceAction(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.theme_settings_title),
                summary = stringResource(R.string.theme_settings_summary),
                onClick = onThemeSettingsClick,
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            CategoryHeader(stringResource(R.string.category_language))
        }
        item {
            DropdownPreference(
                icon = Icons.Outlined.Translate,
                title = stringResource(R.string.language_setting),
                summary = languageOptions.getOrElse(AppLanguage.entries.indexOf(appLanguage)) {
                    stringResource(R.string.language_system)
                },
                items = languageOptions,
                selectedIndex = AppLanguage.entries.indexOf(appLanguage),
                onSelectedIndexChange = { index ->
                    val selected = AppLanguage.entries.getOrNull(index) ?: return@DropdownPreference
                    if (selected.isCustom) {
                        customLocaleInput = customLocaleTag
                        showCustomLocaleDialog = true
                    } else {
                        onLanguageChange(index)
                    }
                },
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            CategoryHeader(stringResource(R.string.category_tools))
        }
        item {
            PreferenceAction(
                icon = Icons.Outlined.FileDownload,
                title = stringResource(R.string.export_tool),
                summary = stringResource(R.string.export_summary),
                onClick = onExportClick,
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            CategoryHeader(stringResource(R.string.category_about))
        }
        item {
            PreferenceAction(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.about_app),
                summary = stringResource(R.string.about_summary),
                onClick = onAboutClick,
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.footer_tag),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.version) + " $versionName ($versionCode)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.copyright),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MiuixSettingsPage(
    versionName: String,
    versionCode: Long,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    onThemeSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onAboutClick: () -> Unit,
    appLanguage: AppLanguage,
    languageOptions: List<String>,
    onLanguageChange: (Int) -> Unit,
    customLocaleTag: String,
    onCustomLocaleTagChange: (String) -> Unit,
) {
    var showCustomLocaleDialog by rememberSaveable { mutableStateOf(false) }
    var customLocaleInput by rememberSaveable { mutableStateOf(customLocaleTag) }
    val uiStyleEntries = listOf(
        UiStyle.MIUIX to stringResource(R.string.ui_style_miuix),
        UiStyle.MATERIAL3 to stringResource(R.string.ui_style_material3),
    )
    val selectedUiStyleIndex = uiStyleEntries.indexOfFirst { it.first == uiStyle }.coerceAtLeast(0)

    if (showCustomLocaleDialog) {
        WindowDialog(
            show = true,
            title = stringResource(R.string.custom_locale_title),
            onDismissRequest = { showCustomLocaleDialog = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiuixText(
                    text = stringResource(R.string.custom_locale_hint),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                BasicTextField(
                    value = customLocaleInput,
                    onValueChange = { customLocaleInput = it },
                    singleLine = true,
                    textStyle = TextStyle(color = MiuixTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MiuixTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(14.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    MiuixTextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { showCustomLocaleDialog = false },
                    )
                    MiuixTextButton(
                        text = stringResource(R.string.custom_locale_apply),
                        onClick = {
                            onCustomLocaleTagChange(customLocaleInput.trim())
                            showCustomLocaleDialog = false
                        },
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { MiuixCategoryHeader(stringResource(R.string.category_appearance)) }
        item {
            MiuixCard(modifier = Modifier.fillMaxWidth()) {
                OverlayDropdownPreference(
                    title = stringResource(R.string.ui_style),
                    items = uiStyleEntries.map { it.second },
                    selectedIndex = selectedUiStyleIndex,
                    onSelectedIndexChange = { index ->
                        uiStyleEntries.getOrNull(index)?.first?.let(onUiStyleChange)
                    },
                    showValue = true,
                    startAction = {
                        MiuixIcon(
                            imageVector = Icons.Outlined.Style,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.theme_settings_title),
                    summary = stringResource(R.string.theme_settings_summary),
                    onClick = onThemeSettingsClick,
                    startAction = {
                        MiuixIcon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                )
            }
        }

        item { MiuixCategoryHeader(stringResource(R.string.category_language)) }
        item {
            MiuixCard(modifier = Modifier.fillMaxWidth()) {
                OverlayDropdownPreference(
                    title = stringResource(R.string.language_setting),
                    summary = languageOptions.getOrElse(AppLanguage.entries.indexOf(appLanguage)) {
                        stringResource(R.string.language_system)
                    },
                    items = languageOptions,
                    selectedIndex = AppLanguage.entries.indexOf(appLanguage),
                    onSelectedIndexChange = { index ->
                        val selected = AppLanguage.entries.getOrNull(index) ?: return@OverlayDropdownPreference
                        if (selected.isCustom) {
                            customLocaleInput = customLocaleTag
                            showCustomLocaleDialog = true
                        } else {
                            onLanguageChange(index)
                        }
                    },
                    startAction = {
                        MiuixIcon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                )
            }
        }

        item { MiuixCategoryHeader(stringResource(R.string.category_tools)) }
        item {
            MiuixCard(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = stringResource(R.string.export_tool),
                    summary = stringResource(R.string.export_summary),
                    onClick = onExportClick,
                    startAction = {
                        MiuixIcon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                )
            }
        }

        item { MiuixCategoryHeader(stringResource(R.string.category_about)) }
        item {
            MiuixCard(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = stringResource(R.string.about_app),
                    summary = stringResource(R.string.about_summary),
                    onClick = onAboutClick,
                    startAction = {
                        MiuixIcon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MiuixText(
                    text = stringResource(R.string.footer_tag),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                MiuixText(
                    text = stringResource(R.string.version) + " $versionName ($versionCode)",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
                MiuixText(
                    text = stringResource(R.string.copyright),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun MiuixCategoryHeader(title: String) {
    MiuixText(
        text = title,
        color = MiuixTheme.colorScheme.primary,
        fontSize = 14.dp.value.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun UiStyleDropdown(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    StyleAwareDropdownPreference(
        icon = Icons.Outlined.Style,
        title = title,
        summary = items.getOrNull(selectedIndex).orEmpty(),
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = onSelectedIndexChange,
        showValue = true,
    )
}

@Composable
private fun StyleAwareDropdownPreference(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    showValue: Boolean = false,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialDropdownPreference(
            icon = icon,
            title = title,
            summary = summary,
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
        )

        UiStyle.MIUIX -> MiuixCard(modifier = Modifier.fillMaxWidth()) {
            OverlayDropdownPreference(
                title = title,
                summary = if (showValue) "" else summary,
                items = items,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = onSelectedIndexChange,
                modifier = Modifier.fillMaxWidth(),
                showValue = showValue,
                startAction = {
                    MiuixIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
            )
        }
    }
}

@Composable
private fun DropdownPreference(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    StyleAwareDropdownPreference(
        icon = icon,
        title = title,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = onSelectedIndexChange,
    )
}

@Composable
private fun MaterialDropdownPreference(
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
    )
}

@Composable
private fun PreferenceAction(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.MIUIX -> {
            MiuixCard(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = title,
                    summary = summary,
                    onClick = onClick,
                    startAction = {
                        MiuixIcon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                )
            }
        }

        UiStyle.MATERIAL3 -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
