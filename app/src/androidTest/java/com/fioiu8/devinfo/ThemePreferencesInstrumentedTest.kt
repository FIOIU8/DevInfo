/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fioiu8.devinfo.core.model.MotionLevel
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.data.ThemePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePreferencesInstrumentedTest {

    @Test
    fun legacyUiStyleValueIsPreservedAndIgnored() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // clear() 会清空值但保留 SharedPreferences 实例；deleteSharedPreferences
        // 会让 ThemePreferences 单例持有的实例失效，影响同一进程内的后续测试。
        context.getSharedPreferences("devinfo_theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        try {
            val seeded = context.getSharedPreferences("devinfo_theme_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("ui_style", "MIUIX")
                .commit()
            assertTrue(seeded)
            val preferences = ThemePreferences.getInstance(context)

            preferences.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, preferences.themeMode.first())
            assertEquals(
                "MIUIX",
                context.getSharedPreferences("devinfo_theme_prefs", Context.MODE_PRIVATE)
                    .getString("ui_style", null),
            )
        } finally {
            context.getSharedPreferences("devinfo_theme_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    @Test
    fun motionLevelPersistsAndInvalidValuesFallBackToStandard() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = ThemePreferences.getInstance(context)
        try {
            preferences.setMotionLevel(MotionLevel.FULL)
            assertEquals(MotionLevel.FULL, preferences.motionLevel.first())

            context.getSharedPreferences("devinfo_theme_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("motion_level", "NOT_A_LEVEL")
                .commit()

            // 偏好变更通过监听器异步回传到 StateFlow，等待其落到默认值。
            withTimeout(2_000) {
                preferences.motionLevel.first { it == MotionLevel.STANDARD }
            }
        } finally {
            preferences.setMotionLevel(MotionLevel.STANDARD)
        }
        Unit
    }
}
