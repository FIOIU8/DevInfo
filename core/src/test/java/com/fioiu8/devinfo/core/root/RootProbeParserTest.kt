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

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `su` 探测结果的判定。
 *
 * 每条断言的形状都来自真机观察：未授权时 KernelSU 让 `su` 对调用者不可见（ENOENT），
 * 已授权时才返回预期输出；被拒与其它失败必须分开，因为两者给用户的下一步动作不同。
 */
class RootProbeParserTest {

    @Test
    fun `expected output with zero exit code is granted`() {
        assertEquals(
            RootAccess.Granted,
            parseRootProbe(spawnFailed = false, timedOut = false, exitCode = 0, output = "test"),
        )
    }

    @Test
    fun `trailing newline and whitespace still count as granted`() {
        assertEquals(
            RootAccess.Granted,
            parseRootProbe(spawnFailed = false, timedOut = false, exitCode = 0, output = "test\n"),
        )
        assertEquals(
            RootAccess.Granted,
            parseRootProbe(spawnFailed = false, timedOut = false, exitCode = 0, output = "  test  "),
        )
    }

    @Test
    fun `missing su entry point is not treated as no root`() {
        // KernelSU/APatch 未授权时 `su` 对调用者不可见，spawn 直接抛 ENOENT。
        assertEquals(
            RootAccess.NoSuEntryPoint,
            parseRootProbe(spawnFailed = true, timedOut = false, exitCode = 0, output = ""),
        )
    }

    @Test
    fun `timed out while waiting for user confirmation`() {
        assertEquals(
            RootAccess.TimedOut,
            parseRootProbe(spawnFailed = false, timedOut = true, exitCode = 0, output = ""),
        )
    }

    @Test
    fun `denial hint in output is reported as denied`() {
        val hints = listOf(
            "su: permission denied",
            "Permission Denied",
            "not allowed",
            "拒绝授权",
            "未授权",
        )
        hints.forEach { hint ->
            assertEquals(
                "hint=$hint",
                RootAccess.Denied,
                parseRootProbe(spawnFailed = false, timedOut = false, exitCode = 1, output = hint),
            )
        }
    }

    @Test
    fun `failure without a denial hint stays unknown instead of being reported as denied`() {
        assertEquals(
            RootAccess.Unknown,
            parseRootProbe(
                spawnFailed = false,
                timedOut = false,
                exitCode = 1,
                output = "Segmentation fault",
            ),
        )
        assertEquals(
            RootAccess.Unknown,
            parseRootProbe(spawnFailed = false, timedOut = false, exitCode = 1, output = ""),
        )
    }

    @Test
    fun `zero exit code with unexpected output is unknown`() {
        assertEquals(
            RootAccess.Unknown,
            parseRootProbe(spawnFailed = false, timedOut = false, exitCode = 0, output = ""),
        )
        assertEquals(
            RootAccess.Unknown,
            parseRootProbe(
                spawnFailed = false,
                timedOut = false,
                exitCode = 0,
                output = "uid=0(root)",
            ),
        )
    }

    @Test
    fun `a process that never reported an outcome ignores exit code and output`() {
        // 进程没给出结论时，退出码与输出都不代表结果，判定必须由前两个标志决定。
        assertEquals(
            RootAccess.NoSuEntryPoint,
            parseRootProbe(spawnFailed = true, timedOut = true, exitCode = 1, output = "denied"),
        )
        assertEquals(
            RootAccess.TimedOut,
            parseRootProbe(spawnFailed = false, timedOut = true, exitCode = 1, output = "denied"),
        )
    }

    @Test
    fun `expected output is configurable`() {
        assertEquals(
            RootAccess.Granted,
            parseRootProbe(
                spawnFailed = false,
                timedOut = false,
                exitCode = 0,
                output = "ok",
                expected = "ok",
            ),
        )
    }
}
