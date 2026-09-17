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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.core.model.UiStyle
import com.fioiu8.devinfo.data.GitHubClient
import com.fioiu8.devinfo.feature.main.UpdateAvailableDialog
import com.fioiu8.devinfo.ui.theme.DevInfoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 更新对话框的发布说明渲染。
 *
 * 更新检查只在官方构建里运行（BuildConfig.IS_OFFICIAL），因此 debug 构建无法通过
 * 正常流程弹出该对话框；这里直接组合对话框组件来覆盖渲染契约。
 */
@RunWith(AndroidJUnit4::class)
class UpdateDialogInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 正文里保留 Markdown 标记，用于确认渲染的是原始文本而不是解析结果。
    private val releaseNotes = "# 更新内容 **加粗**"

    private fun showDialog(uiStyle: UiStyle) {
        composeTestRule.setContent {
            DevInfoTheme(themeMode = ThemeMode.LIGHT, uiStyle = uiStyle) {
                UpdateAvailableDialog(
                    show = true,
                    info = GitHubClient.ReleaseInfo(
                        tagName = "v9.9.9",
                        name = "Release 9.9.9",
                        body = releaseNotes,
                        htmlUrl = "https://example.com/release",
                        downloadUrl = null,
                    ),
                    isError = false,
                    currentVersion = "1.0.0",
                    onDownload = {},
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun material3BranchRendersReleaseNotesAsPlainText() {
        showDialog(UiStyle.MATERIAL3)

        composeTestRule.onNodeWithText(releaseNotes).assertIsDisplayed()
    }

    @Test
    fun miuixBranchRendersReleaseNotesAsPlainText() {
        showDialog(UiStyle.MIUIX)

        // Miuix 分支把标题与正文合并成一段文本，因此按子串匹配。
        composeTestRule.onNodeWithText(releaseNotes, substring = true).assertExists()
    }
}
