package com.fioiu8.devinfo

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fioiu8.devinfo.core.model.UiStyle
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
    fun persistedUiStyleCanSwitchFromMiuixToMaterial3() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteSharedPreferences("devinfo_theme_prefs")
        try {
            val seeded = context.getSharedPreferences("devinfo_theme_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(ThemePreferences.KEY_UI_STYLE, UiStyle.MIUIX.name)
                .commit()
            assertTrue(seeded)
            val preferences = ThemePreferences.getInstance(context)

            assertEquals(UiStyle.MIUIX, preferences.uiStyle.first())

            preferences.setUiStyle(UiStyle.MATERIAL3)
            assertEquals(UiStyle.MATERIAL3, preferences.uiStyle.first())
        } finally {
            context.deleteSharedPreferences("devinfo_theme_prefs")
        }
    }
}
