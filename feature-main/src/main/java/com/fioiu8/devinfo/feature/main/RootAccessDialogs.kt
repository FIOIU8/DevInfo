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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fioiu8.devinfo.core.model.UiStyle
import com.fioiu8.devinfo.core.root.RootAccess
import com.fioiu8.devinfo.ui.DevInfoLoadingIndicator
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 加载弹窗的延迟显示阈值。
 *
 * 依据是本机实测：KernelSU 上「已授权」的完整往返最坏 291 ms（真实工作是 180 ms 采样间隔 +
 * 2×su），而「未授权」的失败只要 5–19 ms。阈值放在最坏成功路径之上，两种常见结果都不会闪一个
 * 又立刻消失的弹窗；只有 Magisk 那类秒级等待（等待用户在管理器弹窗里操作）才会真正显示。
 *
 * 这也是**不给这 264 ms 加动画**的原因：失败路径 13 ms ≈ 0.78 帧，动画既改不了时间长度，
 * 又会在成功路径上凭空多出 16 帧的进度反馈。延迟阈值才是解。
 */
internal const val ROOT_LOADING_DIALOG_DELAY_MS = 400L

/** 一次 Root 失败对应的标题与正文资源。 */
@Immutable
internal data class RootFailureCopy(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
)

/**
 * 把 [RootAccess] 映射为一套失败文案；[RootAccess.Granted] 不是失败，返回 null。
 *
 * 四态必须给出**不同**的标题与正文：相同会让用户以为换了操作却没换结果。文案本身不得断言
 * 失败原因（尤其不能写「设备未 root」），因为 `su` 对自己不可见的场景在设备侧无法与「没装
 * Root 方案」区分。
 */
internal fun rootFailureCopy(access: RootAccess): RootFailureCopy? = when (access) {
    RootAccess.Granted -> null

    RootAccess.NoSuEntryPoint -> RootFailureCopy(
        titleRes = R.string.root_failure_nosu_title,
        messageRes = R.string.root_failure_nosu_message,
    )

    RootAccess.Denied -> RootFailureCopy(
        titleRes = R.string.root_failure_denied_title,
        messageRes = R.string.root_failure_denied_message,
    )

    RootAccess.TimedOut -> RootFailureCopy(
        titleRes = R.string.root_failure_timeout_title,
        messageRes = R.string.root_failure_timeout_message,
    )

    RootAccess.Unknown -> RootFailureCopy(
        titleRes = R.string.root_failure_unknown_title,
        messageRes = R.string.root_failure_unknown_message,
    )
}

/**
 * 授权等待弹窗。
 *
 * 刻意不可关闭：它出现时已经过了 [ROOT_LOADING_DIALOG_DELAY_MS]，说明请求确实停在等待用户
 * 操作上，此时误触背景或返回键关掉弹窗只会让用户不知道请求还在进行。唯一的出口是「取消」，
 * 由调用方取消协程并强杀 `su` 进程。
 *
 * @param show 是否显示
 * @param onCancel 用户点「取消」；调用方必须取消承载请求的协程
 */
@Composable
fun RootRequestLoadingDialog(
    show: Boolean,
    onCancel: () -> Unit,
) {
    if (!show) return
    val title = stringResource(R.string.root_loading_title)
    val message = stringResource(R.string.root_loading_message)
    val cancelLabel = stringResource(R.string.cancel)

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        // onDismissRequest 给空实现：Miuix 由回调驱动关闭，空回调即背景点击/返回键无效果
        WindowDialog(
            show = true,
            title = title,
            onDismissRequest = {},
            largeScreen = true,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RootLoadingBody(message)
                MiuixDialogActions(
                    listOf(MiuixDialogAction(label = cancelLabel, onClick = onCancel)),
                )
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = { RootLoadingBody(message) },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(cancelLabel)
            }
        },
    )
}

/**
 * 授权失败弹窗，按 [RootAccess] 的原因给文案，并提供「重试」与「查看授权帮助」。
 *
 * @param access 失败原因；[RootAccess.Granted] 时什么都不渲染
 * @param onRetry 重试授权（调用方不应重复展示风险告知：用户在本轮流程里已经确认过）
 * @param onOpenHelp 打开应用内的授权帮助页
 * @param onDismiss 关闭弹窗
 */
@Composable
fun RootRequestFailureDialog(
    access: RootAccess,
    onRetry: () -> Unit,
    onOpenHelp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val copy = rootFailureCopy(access) ?: return
    val title = stringResource(copy.titleRes)
    val message = stringResource(copy.messageRes)
    val retryLabel = stringResource(R.string.retry)
    val helpLabel = stringResource(R.string.root_failure_open_help)
    val closeLabel = stringResource(R.string.close)

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        WindowDialog(
            show = true,
            title = title,
            onDismissRequest = onDismiss,
            largeScreen = true,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiuixText(text = message, style = MiuixTheme.textStyles.body2)
                // 三个动作 → 竖排（见 MiuixDialogActions）；主要动作用强调色，其余保持默认中性。
                MiuixDialogActions(
                    listOf(
                        MiuixDialogAction(label = retryLabel, onClick = onRetry, isPrimary = true),
                        MiuixDialogAction(label = helpLabel, onClick = onOpenHelp),
                        MiuixDialogAction(label = closeLabel, onClick = onDismiss),
                    ),
                )
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onOpenHelp) {
                    Text(helpLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(closeLabel)
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(retryLabel)
            }
        },
    )
}

/** 弹窗正文：说明文字 + 统一的加载指示器（[DevInfoLoadingIndicator] 已按 UI 风格分派）。 */
@Composable
private fun RootLoadingBody(message: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (LocalUiStyle.current) {
            UiStyle.MATERIAL3 -> Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )

            UiStyle.MIUIX -> MiuixText(
                text = message,
                style = MiuixTheme.textStyles.body2,
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            DevInfoLoadingIndicator()
        }
    }
}
