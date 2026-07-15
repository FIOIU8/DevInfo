package com.fioiu8.devinfo

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.ui.MainScreen
import com.fioiu8.devinfo.ui.theme.DevInfoTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(base: Context) {
        val languagePrefs = LanguagePreferences(base)
        val tag = languagePrefs.getEffectiveLocaleTag()
        val newBase = if (tag != null) {
            // Recreate the base context so Android resolves resources in the selected locale.
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            base.createConfigurationContext(config)
        } else {
            base
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the selected light or dark splash theme.
        val themePrefs = ThemePreferences(this)
        val savedTheme = themePrefs.getThemeModeSnapshot()
        when (savedTheme) {
            ThemeMode.DARK, ThemeMode.DYNAMIC_DARK -> {
                setTheme(R.style.Theme_DevInfo_Splash_Dark)
            }
            else -> {
                setTheme(R.style.Theme_DevInfo_Splash)
            }
        }

        // 必须在 setContentView 之前调用
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 设备唯一标识
        val deviceId = try {
            DeviceIdManager(this).getOrCreateDeviceId()
        } catch (e: Exception) {
            "获取失败: ${e.message}"
        }

        // 模块导出助手
        val exportHelper = ModuleExportHelper(this)
        val languagePrefs = LanguagePreferences(this)

        setContent {
            val themeMode by themePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val themeColor by themePrefs.themeColor.collectAsState(initial = ThemeColor.DEFAULT)
            val appLanguage by languagePrefs.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
            val customLocaleTag by languagePrefs.customLocaleTag.collectAsState(initial = "")

            DevInfoTheme(themeMode = themeMode, themeColor = themeColor) {
                MainScreen(
                    deviceId = deviceId,
                    themeMode = themeMode,
                    onThemeModeChange = { themePrefs.setThemeMode(it) },
                    themeColor = themeColor,
                    onThemeColorChange = { themePrefs.setThemeColor(it) },
                    exportHelper = exportHelper,
                    appLanguage = appLanguage,
                    languageOptions = AppLanguage.entries.map { getString(it.displayNameResId) },
                    onLanguageChange = { index ->
                        val selected = AppLanguage.entries[index]
                        languagePrefs.setAppLanguage(selected)
                        if (!selected.isCustom) recreate()
                    },
                    customLocaleTag = customLocaleTag,
                    onCustomLocaleTagChange = { tag ->
                        languagePrefs.setCustomLocaleTag(tag)
                        languagePrefs.setAppLanguage(AppLanguage.CUSTOM)
                        recreate()
                    }
                )
            }
        }
    }
}
