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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.model.PaletteStyle
import com.fioiu8.devinfo.ui.MainScreen
import com.fioiu8.devinfo.ui.MainScreenSettings
import com.fioiu8.devinfo.ui.MainViewModel
import com.fioiu8.devinfo.ui.theme.DevInfoTheme

class MainActivity : ComponentActivity() {

    private var themePrefs: ThemePreferences? = null
    private var languagePrefs: LanguagePreferences? = null

    override fun onDestroy() {
        super.onDestroy()
        themePrefs?.close()
        languagePrefs?.close()
    }

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

        // 设备唯一标识
        val deviceId = try {
            DeviceIdManager(this).getOrCreateDeviceId()
        } catch (e: Exception) {
            getString(R.string.device_id_fetch_failed, e.message.orEmpty())
        }

        // 模块导出助手
        val exportHelper = ModuleExportHelper(this)
        val themePrefs = ThemePreferences(this).also { this.themePrefs = it }
        val languagePrefs = LanguagePreferences(this).also { this.languagePrefs = it }
        val appContext = applicationContext
        val collector = DeviceInfoCollector(appContext)
        val mainViewModelFactory = MainViewModel.factory(
            collector = collector,
            cpuUsageSampler = CpuUsageSampler(collector),
            liveHardwareMonitor = LiveHardwareMonitor(appContext),
            batteryObserver = BatteryObserver(appContext),
            updateChecker = UpdateChecker(appContext),
            themePreferences = themePrefs,
        )

        setContent {
            val themeMode by themePrefs.themeMode.collectAsState()
            val themeColor by themePrefs.themeColor.collectAsState()
            val uiStyle by themePrefs.uiStyle.collectAsState()
            val appLanguage by languagePrefs.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
            val customLocaleTag by languagePrefs.customLocaleTag.collectAsState(initial = "")
            val mainViewModel: MainViewModel = viewModel(factory = mainViewModelFactory)
            val checkUpdate by themePrefs.checkUpdate.collectAsState(initial = true)
            val paletteStyle by themePrefs.paletteStyle.collectAsState(initial = PaletteStyle.DEFAULT)
            val colorSpec by themePrefs.colorSpec.collectAsState(initial = com.fioiu8.devinfo.model.ColorSpec.DEFAULT)
            val enableBlur by themePrefs.enableBlur.collectAsState(initial = false)
            val enableFloatingBottomBar by themePrefs.enableFloatingBottomBar.collectAsState(initial = true)
            val enableFloatingBottomBarBlur by themePrefs.enableFloatingBottomBarBlur.collectAsState(initial = true)
            val pageScale by themePrefs.pageScale.collectAsState(initial = 1f)
            val enablePredictiveBack by themePrefs.enablePredictiveBack.collectAsState(initial = true)
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
