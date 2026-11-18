/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fioiu8.devinfo.data.BatteryObserver
import com.fioiu8.devinfo.data.DeviceInfoCollector
import com.fioiu8.devinfo.data.LanguagePreferences
import com.fioiu8.devinfo.data.LiveHardwareMonitor
import com.fioiu8.devinfo.data.ModuleExportHelper
import com.fioiu8.devinfo.data.ThemePreferences
import com.fioiu8.devinfo.data.UpdateChecker
import com.fioiu8.devinfo.data.identifiers.AndroidCnOaidProvider
import com.fioiu8.devinfo.data.AppLanguage
import com.fioiu8.devinfo.data.CpuUsageSampler
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.core.model.MotionLevel
import com.fioiu8.devinfo.feature.main.MainScreen
import com.fioiu8.devinfo.feature.main.MainScreenSettings
import com.fioiu8.devinfo.feature.main.MainViewModel
import com.fioiu8.devinfo.ui.theme.DevInfoTheme

class MainActivity : ComponentActivity() {

    // ThemePreferences 是单例，不应在 Activity 销毁时 close，否则 recreate() 后监听器失效
    // LanguagePreferences 不是单例，需要在 onDestroy 中 close
    private var languagePrefs: LanguagePreferences? = null
    private var attachLanguagePrefs: LanguagePreferences? = null
    private val screenshotWarningState = mutableStateOf(false)
    private var screenshotGuard: SystemScreenshotGuard? = null

    override fun onDestroy() {
        super.onDestroy()
        // themePrefs 是单例，跨 Activity 生命周期，不在这里 close
        languagePrefs?.close()
        attachLanguagePrefs?.close()
    }

    override fun attachBaseContext(base: Context) {
        // 同一 Activity 实例可能被多次 attach；先释放上一个实例注册的
        // SharedPreferences 监听器，否则旧实例会被监听器强引用而无法回收。
        attachLanguagePrefs?.close()
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
        // 在首帧之前设置，避免启动画面或首屏内容短暂可被截图。
        if (themePrefs.getScreenshotProtectionSnapshot()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        val languagePrefs = LanguagePreferences(this).also { this.languagePrefs = it }
        val appContext = applicationContext
        val collector = DeviceInfoCollector(
            context = localizedAppContext,
            identifierProvider = AndroidCnOaidProvider(appContext),
        )
        val mainViewModelFactory = MainViewModel.factory(
            collector = collector,
            cpuUsageSampler = CpuUsageSampler(collector),
            liveHardwareMonitor = LiveHardwareMonitor(appContext),
            batteryObserver = BatteryObserver(appContext),
            updateChecker = UpdateChecker(
                context = localizedAppContext,
                isOfficialBuild = com.fioiu8.devinfo.feature.main.BuildConfig.IS_OFFICIAL,
            ),
            themePreferences = themePrefs,
        )

        setContent {
            val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle()
            val themeColor by themePrefs.themeColor.collectAsStateWithLifecycle()
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
            val pageScale by themePrefs.pageScale.collectAsStateWithLifecycle(initialValue = 1f)
            val enablePredictiveBack by themePrefs.enablePredictiveBack.collectAsStateWithLifecycle(initialValue = true)
            val motionLevel by themePrefs.motionLevel.collectAsStateWithLifecycle(initialValue = MotionLevel.STANDARD)
            val pressAnimation by themePrefs.pressAnimation.collectAsStateWithLifecycle(initialValue = true)
            val glassEffect by themePrefs.glassEffect.collectAsStateWithLifecycle(initialValue = true)
            val privacyMask by themePrefs.privacyMask.collectAsStateWithLifecycle(initialValue = true)
            val screenshotProtection by themePrefs.screenshotProtection.collectAsStateWithLifecycle(initialValue = true)
            val systemDensity = LocalDensity.current
            val scaledDensity = remember(systemDensity, pageScale) {
                Density(
                    density = systemDensity.density * pageScale,
                    fontScale = systemDensity.fontScale,
                )
            }
            val mainScreenSettings = remember(
                themeMode,
                themeColor,
                appLanguage,
                customLocaleTag,
                checkUpdate,
                paletteStyle,
                colorSpec,
                pageScale,
                enablePredictiveBack,
                motionLevel,
                pressAnimation,
                glassEffect,
                privacyMask,
                screenshotProtection,
            ) {
                MainScreenSettings(
                    themeMode = themeMode,
                    onThemeModeChange = themePrefs::setThemeMode,
                    themeColor = themeColor,
                    onThemeColorChange = themePrefs::setThemeColor,
                    checkUpdate = checkUpdate,
                    onCheckUpdateChange = themePrefs::setCheckUpdate,
                    paletteStyle = paletteStyle,
                    onPaletteStyleChange = themePrefs::setPaletteStyle,
                    colorSpec = colorSpec,
                    onColorSpecChange = themePrefs::setColorSpec,
                    pageScale = pageScale,
                    onPageScaleChange = themePrefs::setPageScale,
                    enablePredictiveBack = enablePredictiveBack,
                    onEnablePredictiveBackChange = themePrefs::setEnablePredictiveBack,
                    motionLevel = motionLevel,
                    onMotionLevelChange = themePrefs::setMotionLevel,
                    pressAnimation = pressAnimation,
                    onPressAnimationChange = themePrefs::setPressAnimation,
                    glassEffect = glassEffect,
                    onGlassEffectChange = themePrefs::setGlassEffect,
                    privacyMask = privacyMask,
                    onPrivacyMaskChange = themePrefs::setPrivacyMask,
                    screenshotProtection = screenshotProtection,
                    onScreenshotProtectionChange = themePrefs::setScreenshotProtection,
                    appLanguage = appLanguage,
                    customLocaleTag = customLocaleTag,
                    onAppLanguageChange = { selected ->
                        languagePrefs.setAppLanguage(selected)
                        if (!selected.isCustom) recreate()
                    },
                    onCustomLocaleTagChange = { tag ->
                        val applied = languagePrefs.setCustomLocaleTag(tag)
                        if (applied) {
                            languagePrefs.setAppLanguage(AppLanguage.CUSTOM)
                            recreate()
                        }
                        applied
                    }
                )
            }

            // 防截屏默认开启；关闭后才注册 API 34+ 的截图检测。
            LaunchedEffect(screenshotProtection) {
                if (screenshotProtection) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val guard = screenshotGuard ?: SystemScreenshotGuard(this@MainActivity) {
                        screenshotWarningState.value = true
                    }.also { screenshotGuard = it }
                    guard.setEnabled(enabled = !screenshotProtection)
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        screenshotGuard?.setEnabled(enabled = false)
                    }
                }
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                DevInfoTheme(
                    themeMode = themeMode,
                    themeColor = themeColor,
                    paletteStyle = paletteStyle,
                    colorSpec = colorSpec,
                    motionLevel = motionLevel,
                    pressAnimationEnabled = pressAnimation,
                    glassEnabled = glassEffect,
                ) {
                    MainScreen(
                        viewModel = mainViewModel,
                        settings = mainScreenSettings,
                        exportHelper = exportHelper,
                        showScreenshotWarning = screenshotWarningState.value,
                        onDismissScreenshotWarning = { screenshotWarningState.value = false },
                        onEnableScreenshotProtection = {
                            themePrefs.setScreenshotProtection(true)
                            screenshotWarningState.value = false
                        },
                    )
                }
            }
        }
    }
}
