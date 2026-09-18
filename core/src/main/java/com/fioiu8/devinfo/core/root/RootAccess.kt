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
 * 一次 `su` 调用的结果。
 *
 * 为什么需要这个模型：此前只有一个布尔结论，一切失败都被压成 `false`，信息在源头就丢了，
 * 而 UI 恰恰需要区分「su 不可用」与「明确被拒」——两者的下一步动作完全不同。
 *
 * 实测基线（KernelSU 3.2.5 / Android 15 / MuMu）：
 * - 未授权时 `su` 对未授权进程**不可见**，spawn 直接 ENOENT，12–19 ms 返回；
 * - 已授权时 `su -c` 往返 40–49 ms（max 72 ms），无「调用中弹窗」阶段。
 */
sealed interface RootAccess {

    /** `su` 以 0 退出且输出符合预期。 */
    data object Granted : RootAccess

    /**
     * 找不到可执行的 `su`（spawn 抛 ENOENT）。
     *
     * 注意这条**不能**读作「设备未 root」：KernelSU/APatch 按 UID 隐藏 `su`，未授权的
     * 进程连这个文件都看不到。所以它同时覆盖两种设备侧无法区分的情况——没装 Root 方案，
     * 以及装了但本应用未被授权。UI 必须据此给出「去管理器授权后重试」而不是断言原因。
     */
    data object NoSuEntryPoint : RootAccess

    /** `su` 已启动但以非 0 退出（典型为管理器中明确拒绝授权）。 */
    data object Denied : RootAccess

    /** 在等待用户在管理器弹窗中确认时超时。 */
    data object TimedOut : RootAccess

    /** 其它情况：I/O 失败、退出码与输出不匹配等。 */
    data object Unknown : RootAccess
}
