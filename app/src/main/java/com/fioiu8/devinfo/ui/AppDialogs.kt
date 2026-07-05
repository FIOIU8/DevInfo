package com.fioiu8.devinfo.ui

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
                text = "导出模块",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "即将导出当前设备信息为改机型模块",
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
                            label = "导出格式",
                            value = "ZIP 压缩包"
                        )
                        InfoRow(
                            label = "保存位置",
                            value = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            ).absolutePath
                        )
                        InfoRow(
                            label = "文件名",
                            value = "${android.os.Build.MODEL}.zip"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "确认导出吗？",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认导出")
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

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                text = "导出成功",
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
                    text = "文件已保存至：",
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
                                Toast.makeText(context, "路径已复制", Toast.LENGTH_SHORT).show()
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
                    text = "长按路径可复制",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
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
                    text = "将会打开外部浏览器",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("打开")
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
                text = if (isError) "检查更新失败" else "发现新版本",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isError) {
                    Text(
                        text = "无法连接到 GitHub，请检查网络后重试",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "当前版本: $currentVersion",
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
                        text = "当前版本: $currentVersion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (info.body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Text(
                                text = info.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp),
                                maxLines = 12,
                                overflow = TextOverflow.Ellipsis
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
                    Text("重试")
                }
                TextButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("手动前往")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("稍后")
                }
            }
        },
        confirmButton = {
            if (isError) {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            } else {
                TextButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("前往下载")
                }
            }
        }
    )
}
