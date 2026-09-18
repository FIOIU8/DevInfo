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

package com.fioiu8.devinfo.core.root

/**
 * `su` 探测用的命令与期望输出。
 *
 * 保持历史上已经在用的形态（`su -c echo test`）：实测在已授权的 KernelSU 3.2.5 上返回
 * `test`，且这是各 root 方案共同支持的标准写法，不需要多形态协商。
 */
const val ROOT_PROBE_COMMAND = "echo test"

/** [ROOT_PROBE_COMMAND] 成功时的输出。 */
const val ROOT_PROBE_EXPECTED_OUTPUT = "test"

/**
 * 用户主动发起授权时等待管理器弹窗的上限。
 *
 * 这个值的职责**只是进程泄漏保护**，不是成功判据：授权请求可能一直等用户去管理器里操作，
 * 没有上限就会留一个永不退出的 `su` 进程。30 秒取自人类交互的常见上限，**不是本机实测**，
 * 待有 Magisk 类设备时复核。
 *
 * 与「已授权后读数据」用的 5 秒上限（`DeviceInfoCollector.ROOT_COMMAND_TIMEOUT_MS`）刻意分开：
 * 那条路径实测往返 40–49 ms，5 秒已有 70 倍余量，不需要等用户操作。
 */
const val ROOT_AUTHORIZATION_TIMEOUT_MS = 30_000L

/**
 * 拒绝授权时管理器可能输出的线索。
 *
 * 只是提示而非契约：各管理器的措辞不稳定，因此匹配不到时降级为 [RootAccess.Unknown]，
 * 不会把「未知失败」误报成「被拒绝」。
 */
private val DENIAL_HINT_REGEX = Regex(
    "denied|permission|not allowed|拒绝|未授权|没有权限",
    RegexOption.IGNORE_CASE,
)

/**
 * 把一次 `su` 调用的原始观察结果映射为 [RootAccess]。
 *
 * 纯函数：不触碰进程、Android API 或文件系统，因此五种结果都能在 JVM 上单测——这是它
 * 单独存在（而不写在 DeviceInfoCollector 里）的理由。
 *
 * 判定顺序即优先级：无法启动 > 超时 > 退出码 > 输出。前两者说明进程从未给出结论，此时
 * 退出码与输出都没有意义。
 *
 * @param spawnFailed 进程未能启动（典型为 ENOENT：`su` 不存在，或对调用者不可见）
 * @param timedOut 进程在超时前未退出
 * @param exitCode 进程退出码；[spawnFailed] 或 [timedOut] 为 true 时被忽略
 * @param output 合并后的 stdout + stderr
 * @param expected 成功时应看到的内容
 */
fun parseRootProbe(
    spawnFailed: Boolean,
    timedOut: Boolean,
    exitCode: Int,
    output: String,
    expected: String = ROOT_PROBE_EXPECTED_OUTPUT,
): RootAccess = when {
    spawnFailed -> RootAccess.NoSuEntryPoint
    timedOut -> RootAccess.TimedOut
    exitCode == 0 -> if (output.trim() == expected) RootAccess.Granted else RootAccess.Unknown
    DENIAL_HINT_REGEX.containsMatchIn(output) -> RootAccess.Denied
    else -> RootAccess.Unknown
}
