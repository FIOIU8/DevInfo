/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * 对话框操作。主操作使用 filled tonal，次操作使用文本按钮，
 * 与 M3 Expressive 的层级一致。
 */
data class DevInfoDialogAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * 全应用统一的对话框。
 *
 * 之前每个弹窗都手写 `headlineSmall + FontWeight.Bold`，与类型阶梯里的 SemiBold
 * 叠加成双重加粗；主操作还全部是 `TextButton`，层级和次要操作分不开。这里把标题、
 * 图标、按钮样式统一交给 M3 组件默认值。
 *
 * @param title 标题，使用组件默认的 headlineSmall
 * @param onDismissRequest 点击遮罩或返回键
 * @param icon 可选图标
 * @param confirm 主操作，渲染为 filled tonal 按钮
 * @param dismiss 次操作，渲染为文本按钮
 * @param content 正文内容
 */
@Composable
fun DevInfoAlertDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    properties: DialogProperties = DialogProperties(),
    confirm: DevInfoDialogAction? = null,
    dismiss: DevInfoDialogAction? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint,
                )
            }
        },
        title = { Text(text = title) },
        text = { Column(content = content) },
        confirmButton = {
            if (confirm != null) {
                FilledTonalButton(
                    onClick = confirm.onClick,
                    enabled = confirm.enabled,
                ) {
                    Text(confirm.label)
                }
            }
        },
        dismissButton = {
            if (dismiss != null) {
                TextButton(
                    onClick = dismiss.onClick,
                    enabled = dismiss.enabled,
                ) {
                    Text(dismiss.label)
                }
            }
        },
    )
}
