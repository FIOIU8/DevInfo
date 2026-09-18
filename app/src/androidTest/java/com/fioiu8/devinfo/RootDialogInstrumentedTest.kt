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

package com.fioiu8.devinfo

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.core.model.UiStyle
import com.fioiu8.devinfo.core.root.RootAccess
import com.fioiu8.devinfo.feature.main.RootRequestFailureDialog
import com.fioiu8.devinfo.feature.main.RootRequestLoadingDialog
import com.fioiu8.devinfo.ui.theme.DevInfoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Root 授权弹窗在两套 UI 风格下的渲染与交互契约。
 *
 * 授权流程要靠真实 `su` 往返才能走完，真机路径本身不适合断言；这里直接组合弹窗组件覆盖两条
 * 关键契约：
 * - 加载弹窗**只能**用「取消」退出（返回键不得关掉它，否则用户会在不知道请求仍在进行的情况下
 *   把弹窗关掉）；
 * - 失败弹窗按 [RootAccess] 四态给出不同标题，且「重试 / 查看授权帮助 / 关闭」都真的回调。
 *
 * 文案从资源读取而不是硬编码，因此断言不受设备语言影响。
 */
@RunWith(AndroidJUnit4::class)
class RootDialogInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int): String = composeTestRule.activity.getString(id)

    /** 四态失败原因与各自标题的对应关系——测试侧的期望，与被测映射互相独立。 */
    private val failureTitles = listOf(
        RootAccess.NoSuEntryPoint to R.string.root_failure_nosu_title,
        RootAccess.Denied to R.string.root_failure_denied_title,
        RootAccess.TimedOut to R.string.root_failure_timeout_title,
        RootAccess.Unknown to R.string.root_failure_unknown_title,
    )

    /** 由测试线程切换到另一态失败原因；组合完成后才会被赋值。 */
    private var switchAccess: (RootAccess) -> Unit = {}

    // ---- 加载弹窗 ----

    private fun showLoadingDialog(uiStyle: UiStyle, onCancel: () -> Unit) {
        composeTestRule.setContent {
            DevInfoTheme(themeMode = ThemeMode.LIGHT, uiStyle = uiStyle) {
                var show by remember { mutableStateOf(true) }
                RootRequestLoadingDialog(
                    show = show,
                    onCancel = {
                        onCancel()
                        show = false
                    },
                )
            }
        }
    }

    private fun assertLoadingDialogBlocksBackPressAndCancels(uiStyle: UiStyle) {
        var cancelled = false
        showLoadingDialog(uiStyle, onCancel = { cancelled = true })

        composeTestRule.onNodeWithText(string(R.string.root_loading_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.root_loading_message)).assertIsDisplayed()

        // 真机的返回键先送给处于焦点的对话框窗口，所以这里也走输入注入。不要用
        // `activity.onBackPressedDispatcher.onBackPressed()`：那绕过窗口焦点，等于让 Activity
        // 自己处理返回，会把测试 Activity 直接结束掉（实测报「No compose hierarchies found」）。
        Espresso.pressBack()

        assertTrue(
            "返回键导致 Activity 结束，加载弹窗没拦住",
            !composeTestRule.activity.isFinishing,
        )
        composeTestRule.onNodeWithText(string(R.string.root_loading_title)).assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.cancel)).performClick()
        composeTestRule.waitForIdle()
        assertTrue("点「取消」后未回调 onCancel", cancelled)
        composeTestRule.onNodeWithText(string(R.string.root_loading_title)).assertDoesNotExist()
    }

    @Test
    fun material3LoadingDialogOnlyExitsThroughCancel() {
        assertLoadingDialogBlocksBackPressAndCancels(UiStyle.MATERIAL3)
    }

    @Test
    fun miuixLoadingDialogOnlyExitsThroughCancel() {
        assertLoadingDialogBlocksBackPressAndCancels(UiStyle.MIUIX)
    }

    // ---- 失败弹窗 ----

    private fun showFailureDialog(
        uiStyle: UiStyle,
        onRetry: () -> Unit = {},
        onOpenHelp: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            DevInfoTheme(themeMode = ThemeMode.LIGHT, uiStyle = uiStyle) {
                var access by remember { mutableStateOf<RootAccess>(RootAccess.NoSuEntryPoint) }
                switchAccess = { access = it }
                RootRequestFailureDialog(
                    access = access,
                    onRetry = onRetry,
                    onOpenHelp = onOpenHelp,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    /** 由测试线程切换到另一态失败原因；只在组合完成后可用。 */
    private fun switchTo(access: RootAccess) {
        composeTestRule.runOnUiThread { switchAccess(access) }
        composeTestRule.waitForIdle()
    }

    private fun assertFailureDialogShowsReasonSpecificTitle(uiStyle: UiStyle) {
        showFailureDialog(uiStyle)

        failureTitles.forEach { (access, titleRes) ->
            switchTo(access)
            composeTestRule.onNodeWithText(string(titleRes)).assertIsDisplayed()
        }
    }

    @Test
    fun material3FailureDialogShowsReasonSpecificTitle() {
        assertFailureDialogShowsReasonSpecificTitle(UiStyle.MATERIAL3)
    }

    @Test
    fun miuixFailureDialogShowsReasonSpecificTitle() {
        assertFailureDialogShowsReasonSpecificTitle(UiStyle.MIUIX)
    }

    private fun assertFailureDialogActionsInvokeCallbacks(uiStyle: UiStyle) {
        var retried = false
        var helped = false
        var closed = false
        showFailureDialog(
            uiStyle = uiStyle,
            onRetry = { retried = true },
            onOpenHelp = { helped = true },
            onDismiss = { closed = true },
        )

        composeTestRule.onNodeWithText(string(R.string.retry)).performClick()
        composeTestRule.onNodeWithText(string(R.string.root_failure_open_help)).performClick()
        composeTestRule.onNodeWithText(string(R.string.close)).performClick()
        composeTestRule.waitForIdle()

        assertTrue("「重试」未回调", retried)
        assertTrue("「查看授权帮助」未回调", helped)
        assertTrue("「关闭」未回调", closed)
    }

    @Test
    fun material3FailureDialogActionsInvokeCallbacks() {
        assertFailureDialogActionsInvokeCallbacks(UiStyle.MATERIAL3)
    }

    @Test
    fun miuixFailureDialogActionsInvokeCallbacks() {
        assertFailureDialogActionsInvokeCallbacks(UiStyle.MIUIX)
    }
}
