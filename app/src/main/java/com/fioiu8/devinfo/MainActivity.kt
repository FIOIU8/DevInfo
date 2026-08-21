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

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fioiu8.devinfo.data.BatteryObserver
import com.fioiu8.devinfo.data.DeviceIdManager
import com.fioiu8.devinfo.data.DeviceInfoCollector
import com.fioiu8.devinfo.data.LanguagePreferences
import com.fioiu8.devinfo.data.LiveHardwareMonitor
import com.fioiu8.devinfo.data.ModuleExportHelper
import com.fioiu8.devinfo.data.ThemePreferences
import com.fioiu8.devinfo.data.UpdateChecker
import com.fioiu8.devinfo.data.AppLanguage
import com.fioiu8.devinfo.data.CpuUsageSampler
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.feature.main.MainScreen
import com.fioiu8.devinfo.feature.main.MainScreenSettings
import com.fioiu8.devinfo.feature.main.MainViewModel
import com.fioiu8.devinfo.ui.theme.DevInfoTheme

class MainActivity : ComponentActivity() {

    // ThemePreferences 是单例，不应在 Activity 销毁时 close，否则 recreate() 后监听器失效
    // LanguagePreferences 不是单例，需要在 onDestroy 中 close
    private var languagePrefs: LanguagePreferences? = null
    private var attachLanguagePrefs: LanguagePreferences? = null

    override fun onDestroy() {
        super.onDestroy()
        // themePrefs 是单例，跨 Activity 生命周期，不在这里 close
        languagePrefs?.close()
        attachLanguagePrefs?.close()
    }

    override fun attachBaseContext(base: Context) {
        val prefs = LanguagePreferences(base)
        attachLanguagePrefs = prefs
        val tag = prefs.getEffectiveLocaleTag()
        val newBase = if (tag != null) {
            // Recreate the base context so Android resolves resources in the selected locale.
            val locale = Locale.forLanguageTag(tag)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            base.createConfigurationContext(config)
        } else {
            base
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest theme owns the system splash window before this callback runs.
        // 必须在 setContentView 之前调用
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.iconView.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(180L)
                .withEndAction { splashScreenView.remove() }
                .start()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val localizedAppContext = applicationContext.createConfigurationContext(
            Configuration(resources.configuration),
        )

        // 模块导出助手
        val exportHelper = ModuleExportHelper(localizedAppContext)
        val themePrefs = ThemePreferences.getInstance(this)
        val languagePrefs = LanguagePreferences(this).also { this.languagePrefs = it }
        val appContext = applicationContext
        val collector = DeviceInfoCollector(localizedAppContext)
        val mainViewModelFactory = MainViewModel.factory(
            collector = collector,
            cpuUsageSampler = CpuUsageSampler(collector),
            liveHardwareMonitor = LiveHardwareMonitor(appContext),
            batteryObserver = BatteryObserver(appContext),
            updateChecker = UpdateChecker(appContext),
            themePreferences = themePrefs,
        )

        setContent {
            // 设备 ID 首次读取涉及 SharedPreferences 磁盘加载与 ANDROID_ID binder
            // 调用，移出主线程 onCreate，在组合后异步加载
            var deviceId by remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                deviceId = withContext(Dispatchers.IO) {
                    try {
                        DeviceIdManager(this@MainActivity).getOrCreateDeviceId()
                    } catch (e: Exception) {
                        getString(R.string.device_id_fetch_failed, e.message.orEmpty())
                    }
                }
            }
            val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle()
            val themeColor by themePrefs.themeColor.collectAsStateWithLifecycle()
            val uiStyle by themePrefs.uiStyle.collectAsStateWithLifecycle()
            val appLanguage by languagePrefs.appLanguage.collectAsStateWithLifecycle(initialValue = AppLanguage.SYSTEM)
            val customLocaleTag by languagePrefs.customLocaleTag.collectAsStateWithLifecycle(initialValue = "")
            val viewModelKey = "main:$appLanguage:$customLocaleTag:${resources.configuration.locales.toLanguageTags()}"
            val mainViewModel: MainViewModel = viewModel(
                key = viewModelKey,
                factory = mainViewModelFactory,
            )
            val checkUpdate by themePrefs.checkUpdate.collectAsStateWithLifecycle(initialValue = true)
            val paletteStyle by themePrefs.paletteStyle.collectAsStateWithLifecycle(initialValue = PaletteStyle.DEFAULT)
            val colorSpec by themePrefs.colorSpec.collectAsStateWithLifecycle(initialValue = com.fioiu8.devinfo.core.model.ColorSpec.DEFAULT)
            val enableBlur by themePrefs.enableBlur.collectAsStateWithLifecycle(initialValue = false)
            val enableFloatingBottomBar by themePrefs.enableFloatingBottomBar.collectAsStateWithLifecycle(initialValue = true)
            val enableFloatingBottomBarBlur by themePrefs.enableFloatingBottomBarBlur.collectAsStateWithLifecycle(initialValue = true)
            val pageScale by themePrefs.pageScale.collectAsStateWithLifecycle(initialValue = 1f)
            val enablePredictiveBack by themePrefs.enablePredictiveBack.collectAsStateWithLifecycle(initialValue = true)
            val systemDensity = LocalDensity.current
            val scaledDensity = remember(systemDensity, pageScale) {
                Density(
                    density = systemDensity.density * pageScale,
                    fontScale = systemDensity.fontScale,
                )
            }
            val mainScreenSettings = remember(
                deviceId,
                themeMode,
                themeColor,
                uiStyle,
                appLanguage,
                customLocaleTag,
                checkUpdate,
                paletteStyle,
                colorSpec,
                enableBlur,
                enableFloatingBottomBar,
                enableFloatingBottomBarBlur,
                pageScale,
                enablePredictiveBack,
            ) {
                MainScreenSettings(
                    deviceId = deviceId,
                    themeMode = themeMode,
                    onThemeModeChange = themePrefs::setThemeMode,
                    themeColor = themeColor,
                    onThemeColorChange = themePrefs::setThemeColor,
                    uiStyle = uiStyle,
                    onUiStyleChange = themePrefs::setUiStyle,
                    checkUpdate = checkUpdate,
                    onCheckUpdateChange = themePrefs::setCheckUpdate,
                    paletteStyle = paletteStyle,
                    onPaletteStyleChange = themePrefs::setPaletteStyle,
                    colorSpec = colorSpec,
                    onColorSpecChange = themePrefs::setColorSpec,
                    enableBlur = enableBlur,
                    onEnableBlurChange = themePrefs::setEnableBlur,
                    enableFloatingBottomBar = enableFloatingBottomBar,
                    onEnableFloatingBottomBarChange = themePrefs::setEnableFloatingBottomBar,
                    enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                    onEnableFloatingBottomBarBlurChange = themePrefs::setEnableFloatingBottomBarBlur,
                    pageScale = pageScale,
                    onPageScaleChange = themePrefs::setPageScale,
                    enablePredictiveBack = enablePredictiveBack,
                    onEnablePredictiveBackChange = themePrefs::setEnablePredictiveBack,
                    appLanguage = appLanguage,
                    customLocaleTag = customLocaleTag,
                    onAppLanguageChange = { selected ->
                        languagePrefs.setAppLanguage(selected)
                        if (!selected.isCustom) recreate()
                    },
                    onCustomLocaleTagChange = { tag ->
                        if (languagePrefs.setCustomLocaleTag(tag)) {
                            languagePrefs.setAppLanguage(AppLanguage.CUSTOM)
                            recreate()
                        }
                    }
                )
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                DevInfoTheme(themeMode = themeMode, themeColor = themeColor, uiStyle = uiStyle) {
                    MainScreen(
                        viewModel = mainViewModel,
                        settings = mainScreenSettings,
                        exportHelper = exportHelper
                    )
                }
            }
        }
    }
}
