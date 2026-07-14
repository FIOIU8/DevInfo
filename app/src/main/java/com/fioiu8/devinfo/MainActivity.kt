package com.fioiu8.devinfo

import android.content.res.Configuration
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fioiu8.devinfo.model.MountThemeColor
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.ui.MainScreen
import com.fioiu8.devinfo.ui.theme.DevInfoTheme

class MainActivity : ComponentActivity() {


    /** Apply the selected app language locale to the activity configuration */
    private fun applyAppLanguage() {
        val tag = languagePrefs.getEffectiveLocaleTag() ?: return
        val locale = java.util.Locale.forLanguageTag(tag)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // 根据保存的主题模式选择浅色/深色启动屏主题
        val themePrefs = ThemePreferences(this)
        val languagePrefs = LanguagePreferences(this)
        // 鏍规嵁淇濆瓨鐨勮瑷�璁剧疆搴旂敤locale
        applyAppLanguage()

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

        setContent {
            val themeMode by themePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val mountColor by themePrefs.mountThemeColor.collectAsState(initial = MountThemeColor.DEFAULT)
            val useMount by themePrefs.useMountTheme.collectAsState(initial = false)

            // 动态颜色模式下，可选的自定义种子颜色
            val seedColor = if (useMount && themeMode.isDynamic) mountColor.color else null

            val appLanguage by languagePrefs.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
            val customLocaleTag by languagePrefs.customLocaleTag.collectAsState(initial = "")

            DevInfoTheme(
                themeMode = themeMode,
                seedColor = seedColor
            ) {
                MainScreen(
                    deviceId = deviceId,
                    themeMode = themeMode,
                    onThemeModeChange = { themePrefs.setThemeMode(it) },
                    mountThemeColor = mountColor,
                    onMountThemeColorChange = { color ->
                        themePrefs.setMountThemeColor(color)
                        themePrefs.setUseMountTheme(true)
                    },
                    useMountTheme = useMount,
                    onUseMountThemeChange = { themePrefs.setUseMountTheme(it) },
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
