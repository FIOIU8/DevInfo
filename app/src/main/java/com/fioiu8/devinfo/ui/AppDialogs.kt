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

import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 导出确认对话框 — 展示导出摘要（格式、路径、文件名）。
 *
 * @param show 是否显示
 * @param onConfirm 确认导出回调
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun ExportConfirmDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixActionDialog(
            title = stringResource(R.string.export_title),
            message = stringResource(R.string.export_confirm_text),
            confirmLabel = stringResource(R.string.confirm_export),
            onConfirm = onConfirm,
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = onDismiss,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(com.fioiu8.devinfo.R.string.export_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(com.fioiu8.devinfo.R.string.export_confirm_text),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        InfoRow(
                            label = stringResource(com.fioiu8.devinfo.R.string.export_format_label),
                            value = stringResource(com.fioiu8.devinfo.R.string.export_format_value)
                        )
                        InfoRow(
                            label = stringResource(com.fioiu8.devinfo.R.string.export_save_location),
                            value = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            ).absolutePath
                        )
                        InfoRow(
                            label = stringResource(com.fioiu8.devinfo.R.string.export_filename),
                            value = "${android.os.Build.MODEL}.zip"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(com.fioiu8.devinfo.R.string.export_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.fioiu8.devinfo.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(com.fioiu8.devinfo.R.string.confirm_export))
            }
        }
    )
}

/**
 * 导出成功对话框 — 展示保存路径，支持长按复制。
 *
 * @param show 是否显示
 * @param filePath 导出文件完整路径
 * @param onDismiss 关闭对话框回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExportSuccessDialog(
    show: Boolean,
    filePath: String,
    onDismiss: () -> Unit
) {
    if (!show) return

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixExportSuccessDialog(filePath = filePath, onDismiss = onDismiss)
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val pathCopiedMessage = stringResource(com.fioiu8.devinfo.R.string.path_copied)
    val showMessage = rememberDevInfoMessageHandler()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(com.fioiu8.devinfo.R.string.export_success),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(com.fioiu8.devinfo.R.string.export_saved_to),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(filePath))
                                showMessage(pathCopiedMessage)
                            }
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Text(
                        text = filePath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(com.fioiu8.devinfo.R.string.export_long_press_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.fioiu8.devinfo.R.string.confirm))
            }
        }
    )
}

/**
 * 外部链接确认对话框 — 打开外链前请求用户确认。
 *
 * @param show 是否显示
 * @param title 对话框标题
 * @param description 链接描述
 * @param onConfirm 确认打开外部链接回调
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun ExternalLinkConfirmDialog(
    show: Boolean,
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixActionDialog(
            title = title,
            message = "$description\n\n${stringResource(R.string.external_link_open_browser)}",
            confirmLabel = stringResource(R.string.open),
            onConfirm = onConfirm,
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = onDismiss,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(com.fioiu8.devinfo.R.string.external_link_open_browser),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.fioiu8.devinfo.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(com.fioiu8.devinfo.R.string.open))
            }
        }
    )
}

/**
 * 更新通知对话框 — 显示新版本内容或错误信息。
 *
 * @param show 是否显示
 * @param info 新版本 Release 信息，error 时为 null
 * @param isError 是否为检查失败的错误状态
 * @param currentVersion 当前应用版本
 * @param onDownload 跳转到下载页面
 * @param onRetry 重试检查更新
 * @param onDismiss 关闭对话框
 */
@Composable
fun UpdateAvailableDialog(
    show: Boolean,
    info: com.fioiu8.devinfo.GitHubClient.ReleaseInfo?,
    isError: Boolean,
    currentVersion: String,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        val title = stringResource(if (isError) R.string.update_check_failed else R.string.update_found)
        val message = if (isError) {
            stringResource(R.string.update_network_error)
        } else {
            listOfNotNull(
                info?.let { "${it.name} (${it.tagName})" },
                info?.body?.takeIf { it.isNotBlank() },
            ).joinToString("\n\n")
        }
        MiuixActionDialog(
            title = title,
            message = message,
            confirmLabel = stringResource(if (isError) R.string.retry else R.string.go_to_download),
            onConfirm = if (isError) onRetry else onDownload,
            dismissLabel = stringResource(if (isError) R.string.close else R.string.later),
            onDismiss = onDismiss,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(
                    if (isError) {
                        com.fioiu8.devinfo.R.string.update_check_failed
                    } else {
                        com.fioiu8.devinfo.R.string.update_found
                    }
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isError) {
                    Text(
                        text = stringResource(com.fioiu8.devinfo.R.string.update_network_error),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(com.fioiu8.devinfo.R.string.current_version) + ": $currentVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (info != null) {
                    Text(
                        text = "${info.name} (${info.tagName})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(com.fioiu8.devinfo.R.string.current_version) + ": $currentVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (info.body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // 可滚动的 Markdown 渲染区域
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            MarkdownText(markdown = info.body)
                        }
                    }

                }
            }
        },
        dismissButton = {
            if (isError) {
                TextButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(com.fioiu8.devinfo.R.string.retry))
                }
                TextButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(com.fioiu8.devinfo.R.string.manual_go))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(com.fioiu8.devinfo.R.string.later))
                }
            }
        },
        confirmButton = {
            if (isError) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(com.fioiu8.devinfo.R.string.close))
                }
            } else {
                TextButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(com.fioiu8.devinfo.R.string.go_to_download))
                }
            }
        }
    )
}

@Composable
private fun MiuixActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (message.isNotBlank()) {
                MiuixText(text = message)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                MiuixTextButton(text = dismissLabel, onClick = onDismiss)
                MiuixTextButton(text = confirmLabel, onClick = onConfirm)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiuixExportSuccessDialog(
    filePath: String,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val showMessage = rememberDevInfoMessageHandler()
    val pathCopiedMessage = stringResource(R.string.path_copied)

    WindowDialog(
        show = true,
        title = stringResource(R.string.export_success),
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MiuixText(text = stringResource(R.string.export_saved_to))
            MiuixCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            clipboardManager.setText(AnnotatedString(filePath))
                            showMessage(pathCopiedMessage)
                        },
                    ),
            ) {
                MiuixText(
                    text = filePath,
                    modifier = Modifier.padding(14.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MiuixText(text = stringResource(R.string.export_long_press_hint))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                MiuixTextButton(
                    text = stringResource(R.string.confirm),
                    onClick = onDismiss,
                )
            }
        }
    }
}
