/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * 回归测试：应用语言不是“跟随系统”时，主页仍然必须真正开始加载。
 *
 * 该缺陷的形态是 ViewModel 的 key 依赖语言，而语言在首帧只拿到默认值、
 * 随后才被偏好真值改写，于是同一次组合里换出了第二个 ViewModel：
 * 旧实例把数据加载完，新实例永远等不到首次加载回调，主页一直停在加载态。
 * 主页能出现“N details available”说明采集结果已发布，加载态已结束。
 */
@RunWith(AndroidJUnit4::class)
class LanguageOverrideLoadsOverviewTest {

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    /** 语言偏好必须写在 Activity 启动之前，否则首帧读到的还是上一次的值。 */
    private val overrideLanguageRule = TestRule { base: Statement, _: Description ->
        object : Statement() {
            override fun evaluate() {
                languagePrefs().edit().putString(KEY_APP_LANGUAGE, ENGLISH_LANGUAGE).commit()
                base.evaluate()
            }
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(overrideLanguageRule).around(composeTestRule)

    @After
    fun restoreSystemLanguage() {
        languagePrefs().edit().putString(KEY_APP_LANGUAGE, SYSTEM_LANGUAGE).commit()
    }

    @Test
    fun overviewFinishesLoadingWhenLanguageIsNotSystem() {
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule
                .onAllNodesWithText(DETAILS_AVAILABLE_LABEL, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun languagePrefs() =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getSharedPreferences(LANGUAGE_PREFS_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val LANGUAGE_PREFS_NAME = "devinfo_language_prefs"
        const val KEY_APP_LANGUAGE = "app_language"
        const val ENGLISH_LANGUAGE = "ENGLISH"
        const val SYSTEM_LANGUAGE = "SYSTEM"

        /** 概览“设备状态”卡片在英文下的尾标，仅在采集结果已发布时出现。 */
        const val DETAILS_AVAILABLE_LABEL = "details available"
    }
}
