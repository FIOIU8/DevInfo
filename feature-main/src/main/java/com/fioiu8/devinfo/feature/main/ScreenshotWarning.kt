/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fioiu8.devinfo.core.model.DeviceInfoItem
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import com.fioiu8.devinfo.core.privacy.SensitiveKeys

/**
 * 截图发生时当前路由上可能存在风险的字段。
 *
 * 概览与详情会显示设备标识符；设置、主题、关于等页面不展示设备数据。
 */
internal fun sensitiveItemsFor(
    route: MainRoute,
    items: List<ItemWithVisibility>,
): List<DeviceInfoItem> = when (route) {
    MainRoute.Overview, is MainRoute.Details ->
        items.map(ItemWithVisibility::item).filter { SensitiveKeys.isSensitive(it.key) }

    MainRoute.Settings, MainRoute.ThemeSettings, MainRoute.About, MainRoute.RootHelp -> emptyList()
}

/**
 * 关闭防截屏后检测到截图时的风险提示。
 *
 * 只列出字段名，不回显值；用户可一键重新开启防截屏。
 */
@Composable
internal fun ScreenshotWarningDialog(
    show: Boolean,
    sensitiveLabels: List<String>,
    onEnableProtection: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.privacy_screenshot_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = if (sensitiveLabels.isEmpty()) {
                    stringResource(R.string.privacy_screenshot_message_none)
                } else {
                    stringResource(R.string.privacy_screenshot_message) +
                        "\n\n" +
                        sensitiveLabels.joinToString("\n") { "• $it" }
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onEnableProtection) {
                Text(stringResource(R.string.privacy_screenshot_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.privacy_screenshot_dismiss))
            }
        },
    )
}
