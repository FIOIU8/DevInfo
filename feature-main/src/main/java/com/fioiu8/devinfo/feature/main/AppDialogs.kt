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

package com.fioiu8.devinfo.feature.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.fioiu8.devinfo.data.GitHubClient
import com.fioiu8.devinfo.feature.main.R

/**
 * 导出确认对话框 — 展示导出摘要与风险提示，并延迟几秒才允许确认。
 *
 * @param show 是否显示
 * @param onConfirm 确认导出回调
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun ExportConfirmDialog(
    show: Boolean,
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(show) {
        countdown = 3
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
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
                text = stringResource(R.string.export_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.export_confirm_text),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.export_risk_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.export_filename) + ": " + fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.export_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                // 倒计时放在正文里、按钮文案保持恒定：否则按钮宽度会随「确认导出 (3) → 确认导出」
                // 变化而跳动。这行始终占位（结束后只剩空白），避免结束后弹窗高度突变。
                Text(
                    text = if (countdown > 0) {
                        stringResource(R.string.export_countdown_hint, countdown)
                    } else {
                        " "
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = countdown == 0
            ) {
                Text(stringResource(R.string.confirm_export))
            }
        }
    )
}

/**
 * 导出成功对话框 — 展示保存位置 URI，支持打开/分享。
 *
 * @param show 是否显示
 * @param fileUri 导出文件的 content URI
 * @param onDismiss 关闭对话框回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExportSuccessDialog(
    show: Boolean,
    fileUri: Uri?,
    onDismiss: () -> Unit
) {
    if (!show) return
    val context = LocalContext.current

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
                text = stringResource(R.string.export_success),
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
                    text = stringResource(R.string.export_saved_to),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            fileUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/zip")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        },
                        enabled = fileUri != null
                    ) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.open))
                    }
                    TextButton(
                        onClick = {
                            fileUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        },
                        enabled = fileUri != null
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.share))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
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
                    text = stringResource(R.string.external_link_open_browser),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.open))
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
    info: com.fioiu8.devinfo.data.GitHubClient.ReleaseInfo?,
    isError: Boolean,
    currentVersion: String,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
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
                        R.string.update_check_failed
                    } else {
                        R.string.update_found
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
                        text = stringResource(R.string.update_network_error),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.current_version) + ": $currentVersion",
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
                        text = stringResource(R.string.current_version) + ": $currentVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (info.body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Keep release notes as plain text to avoid introducing a parser.
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
                            Text(
                                text = info.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    Text(stringResource(R.string.retry))
                }
                TextButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.manual_go))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.later))
                }
            }
        },
        confirmButton = {
            if (isError) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            } else {
                TextButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.go_to_download))
                }
            }
        }
    )
}
/**
 * 下载二次确认对话框 — 用户在更新对话框点击"前往下载"后弹出。
 * 必须再次点击"确认下载"才会真正跳转浏览器离开应用，避免误触。
 *
 * @param show 是否显示
 * @param onConfirm 用户在确认对话框中再次点击"确认下载"的回调，触发跳转浏览器
 * @param onDismiss 用户关闭对话框（取消或返回）的回调
 */
@Composable
fun DownloadConfirmDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return

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
                text = stringResource(R.string.download_confirm_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.download_confirm_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
        Text(stringResource(R.string.download_confirm))
            }
        }
    )
}

// All app dialogs use the Material 3 implementation.
