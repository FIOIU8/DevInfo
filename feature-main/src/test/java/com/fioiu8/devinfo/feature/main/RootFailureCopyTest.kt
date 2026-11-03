/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import com.fioiu8.devinfo.core.root.RootAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootFailureCopyTest {

    private val failureStates = listOf(
        RootAccess.NoSuEntryPoint,
        RootAccess.Denied,
        RootAccess.TimedOut,
        RootAccess.Unknown,
    )

    @Test
    fun `granted maps to no failure copy`() {
        assertNull(rootFailureCopy(RootAccess.Granted))
    }

    @Test
    fun `every failure state maps to a copy`() {
        failureStates.forEach { state ->
            assertNotNull("$state 缺少失败文案", rootFailureCopy(state))
        }
    }

    @Test
    fun `titles and messages are pairwise distinct`() {
        val copies = failureStates.map { state ->
            requireNotNull(rootFailureCopy(state)) { "$state 缺少失败文案" }
        }

        // 相同的文案会让用户以为换了操作却没换结果，因此四态的标题与正文都必须两两不同。
        assertEquals(copies.size, copies.map { it.titleRes }.toSet().size)
        assertEquals(copies.size, copies.map { it.messageRes }.toSet().size)
    }

    @Test
    fun `loading dialog delay stays above the measured fast path`() {
        // 实测快路径最坏 291 ms（KernelSU 已授权：180 ms 采样间隔 + 2×su）。阈值低于它，
        // 成功路径就会闪一个又立刻消失的弹窗——这正是延迟阈值要避免的事。
        assertTrue(
            "ROOT_LOADING_DIALOG_DELAY_MS=$ROOT_LOADING_DIALOG_DELAY_MS 不高于实测快路径最坏值 291ms",
            ROOT_LOADING_DIALOG_DELAY_MS > 291L,
        )
    }
}
