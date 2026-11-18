/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data

import android.content.Context
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.StateFlow

/**
 * 简单的内存+SP 主题偏好存储。
 * 使用 SharedPreferences 实现持久化，通过 StateFlow 暴露当前值。
 */
class ThemePreferences private constructor(context: Context) :
    BasePreferences<String>(context.applicationContext, PREFS_NAME) {

    private val themeModePreference =
        enumPreference(
            key = KEY_THEME_MODE,
            defaultValue = ThemeMode.SYSTEM,
        )

    private val themeColorPreference =
        enumPreference(
            key = KEY_THEME_COLOR,
            defaultValue = ThemeColor.DEFAULT,
        )

    private val checkUpdatePreference =
        booleanPreference(
            key = KEY_CHECK_UPDATE,
            defaultValue = true,
        )

    private val paletteStylePreference =
        enumPreference(
            key = KEY_PALETTE_STYLE,
            defaultValue = PaletteStyle.DEFAULT,
        )

    private val colorSpecPreference =
        enumPreference(
            key = KEY_COLOR_SPEC,
            defaultValue = com.fioiu8.devinfo.core.model.ColorSpec.DEFAULT,
        )

    private val pageScalePreference =
        floatPreference(
            key = KEY_PAGE_SCALE,
            defaultValue = PreferenceValidators.DEFAULT_PAGE_SCALE,
            isValid = { it in PreferenceValidators.MIN_PAGE_SCALE..PreferenceValidators.MAX_PAGE_SCALE },
        )

    private val enablePredictiveBackPreference =
        booleanPreference(key = KEY_ENABLE_PREDICTIVE_BACK, defaultValue = true)

    private val motionLevelPreference =
        enumPreference(
            key = KEY_MOTION_LEVEL,
            defaultValue = com.fioiu8.devinfo.core.model.MotionLevel.STANDARD,
        )

    private val pressAnimationPreference =
        booleanPreference(key = KEY_PRESS_ANIMATION, defaultValue = true)

    private val glassEffectPreference =
        booleanPreference(key = KEY_GLASS_EFFECT, defaultValue = true)

    private val privacyMaskPreference =
        booleanPreference(key = KEY_PRIVACY_MASK, defaultValue = true)

    private val screenshotProtectionPreference =
        booleanPreference(key = KEY_SCREENSHOT_PROTECTION, defaultValue = true)

    val themeMode: StateFlow<ThemeMode> = themeModePreference.flow

    val themeColor: StateFlow<ThemeColor> = themeColorPreference.flow

    val checkUpdate: StateFlow<Boolean> = checkUpdatePreference.flow
    val paletteStyle: StateFlow<PaletteStyle> = paletteStylePreference.flow
    val colorSpec: StateFlow<com.fioiu8.devinfo.core.model.ColorSpec> = colorSpecPreference.flow
    val pageScale: StateFlow<Float> = pageScalePreference.flow
    val enablePredictiveBack: StateFlow<Boolean> = enablePredictiveBackPreference.flow
    val motionLevel: StateFlow<com.fioiu8.devinfo.core.model.MotionLevel> = motionLevelPreference.flow
    val pressAnimation: StateFlow<Boolean> = pressAnimationPreference.flow

    /** 液态玻璃表面开关；关闭后卡片退回纯 Material 3 色调表面。 */
    val glassEffect: StateFlow<Boolean> = glassEffectPreference.flow
    val privacyMask: StateFlow<Boolean> = privacyMaskPreference.flow
    val screenshotProtection: StateFlow<Boolean> = screenshotProtectionPreference.flow

    /** 设置并持久化主题模式 */
    fun setThemeMode(mode: ThemeMode) {
        themeModePreference.set(mode)
    }

    fun setThemeColor(color: ThemeColor) {
        themeColorPreference.set(color)
    }

    fun setCheckUpdate(enabled: Boolean) {
        checkUpdatePreference.set(enabled)
    }

    fun setPaletteStyle(style: PaletteStyle) {
        paletteStylePreference.set(style)
    }

    fun setColorSpec(spec: com.fioiu8.devinfo.core.model.ColorSpec) {
        colorSpecPreference.set(spec)
    }

    fun setPageScale(scale: Float) {
        pageScalePreference.set(PreferenceValidators.pageScaleOrDefault(scale))
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        enablePredictiveBackPreference.set(enabled)
    }

    fun setMotionLevel(level: com.fioiu8.devinfo.core.model.MotionLevel) {
        motionLevelPreference.set(level)
    }

    fun setPressAnimation(enabled: Boolean) {
        pressAnimationPreference.set(enabled)
    }

    fun setGlassEffect(enabled: Boolean) {
        glassEffectPreference.set(enabled)
    }

    fun setPrivacyMask(enabled: Boolean) {
        privacyMaskPreference.set(enabled)
    }

    fun setScreenshotProtection(enabled: Boolean) {
        screenshotProtectionPreference.set(enabled)
    }

    // ── snapshot getters ──

    /** 同步读取当前主题模式（非响应式，用于组合上下文之外） */
    fun getThemeModeSnapshot(): ThemeMode = themeModePreference.snapshot

    fun getCheckUpdateSnapshot(): Boolean = checkUpdatePreference.snapshot

    fun getMotionLevelSnapshot(): com.fioiu8.devinfo.core.model.MotionLevel =
        motionLevelPreference.snapshot

    fun getScreenshotProtectionSnapshot(): Boolean = screenshotProtectionPreference.snapshot

    companion object {
        // 单例：避免每次 Activity 重建都新建实例并注册一组永不注销的
        // OnSharedPreferenceChangeListener，导致旧实例（及其 StateFlow）被
        // SharedPreferences 强引用而泄漏。实例随应用生命周期存在，符合主题存储的定位。
        @Volatile
        private var instance: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences =
            instance ?: synchronized(ThemePreferences::class.java) {
                instance ?: ThemePreferences(context.applicationContext).also {
                    instance = it
                }
            }

        private const val PREFS_NAME = "devinfo_theme_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_CHECK_UPDATE = "check_update"
        const val KEY_PALETTE_STYLE = "palette_style"
        const val KEY_COLOR_SPEC = "color_spec"
        const val KEY_PAGE_SCALE = "page_scale"
        const val KEY_ENABLE_PREDICTIVE_BACK = "enable_predictive_back"
        const val KEY_MOTION_LEVEL = "motion_level"
        const val KEY_PRESS_ANIMATION = "press_animation"
        const val KEY_GLASS_EFFECT = "glass_effect"
        const val KEY_PRIVACY_MASK = "privacy_mask"
        const val KEY_SCREENSHOT_PROTECTION = "screenshot_protection"
    }
}
