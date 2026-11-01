package com.fioiu8.devinfo

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.data.ThemePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePreferencesInstrumentedTest {

    @Test
    fun legacyUiStyleValueIsPreservedAndIgnored() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteSharedPreferences("devinfo_theme_prefs")
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
            context.deleteSharedPreferences("devinfo_theme_prefs")
        }
    }
}
