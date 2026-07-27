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
import com.fioiu8.devinfo.model.PaletteStyle
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.model.UiStyle
import kotlinx.coroutines.flow.StateFlow

/**
 * 简单的内存+SP 主题偏好存储。
 * 使用 SharedPreferences 实现持久化，通过 StateFlow 暴露当前值。
 */
class ThemePreferences(context: Context) : BasePreferences<String>(context, PREFS_NAME) {

    private val themeModePreference =
        enumPreference(
            key = KEY_THEME_MODE,
            defaultValue = ThemeMode.SYSTEM,
            values = ThemeMode.entries,
        )

    private val themeColorPreference =
        enumPreference(
            key = KEY_THEME_COLOR,
            defaultValue = ThemeColor.DEFAULT,
            values = ThemeColor.entries,
        )

    private val uiStylePreference =
        enumPreference(
            key = KEY_UI_STYLE,
            defaultValue = UiStyle.MIUIX,
            values = UiStyle.entries,
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
            values = PaletteStyle.entries,
        )

    private val colorSpecPreference =
        enumPreference(
            key = KEY_COLOR_SPEC,
            defaultValue = com.fioiu8.devinfo.model.ColorSpec.DEFAULT,
            values = com.fioiu8.devinfo.model.ColorSpec.entries,
        )

    private val enableBlurPreference =
        booleanPreference(key = KEY_ENABLE_BLUR, defaultValue = false)

    private val enableFloatingBottomBarPreference =
        booleanPreference(key = KEY_ENABLE_FLOATING_BOTTOM_BAR, defaultValue = true)

    private val enableFloatingBottomBarBlurPreference =
        booleanPreference(key = KEY_ENABLE_FLOATING_BOTTOM_BAR_BLUR, defaultValue = true)

    private val pageScalePreference =
        floatPreference(key = KEY_PAGE_SCALE, defaultValue = DEFAULT_PAGE_SCALE)

    private val enablePredictiveBackPreference =
        booleanPreference(key = KEY_ENABLE_PREDICTIVE_BACK, defaultValue = true)

    val themeMode: StateFlow<ThemeMode> = themeModePreference.flow

    val themeColor: StateFlow<ThemeColor> = themeColorPreference.flow

    val uiStyle: StateFlow<UiStyle> = uiStylePreference.flow

    val checkUpdate: StateFlow<Boolean> = checkUpdatePreference.flow
    val paletteStyle: StateFlow<PaletteStyle> = paletteStylePreference.flow
    val colorSpec: StateFlow<com.fioiu8.devinfo.model.ColorSpec> = colorSpecPreference.flow
    val enableBlur: StateFlow<Boolean> = enableBlurPreference.flow
    val enableFloatingBottomBar: StateFlow<Boolean> = enableFloatingBottomBarPreference.flow
    val enableFloatingBottomBarBlur: StateFlow<Boolean> = enableFloatingBottomBarBlurPreference.flow
    val pageScale: StateFlow<Float> = pageScalePreference.flow
    val enablePredictiveBack: StateFlow<Boolean> = enablePredictiveBackPreference.flow

    /** 设置并持久化主题模式 */
    fun setThemeMode(mode: ThemeMode) {
        themeModePreference.set(mode)
    }

    fun setThemeColor(color: ThemeColor) {
        themeColorPreference.set(color)
    }

    fun setUiStyle(style: UiStyle) {
        uiStylePreference.set(style)
    }

    fun setCheckUpdate(enabled: Boolean) {
        checkUpdatePreference.set(enabled)
    }

    fun setPaletteStyle(style: PaletteStyle) {
        paletteStylePreference.set(style)
    }

    fun setColorSpec(spec: com.fioiu8.devinfo.model.ColorSpec) {
        colorSpecPreference.set(spec)
    }

    fun setEnableBlur(enabled: Boolean) {
        enableBlurPreference.set(enabled)
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        enableFloatingBottomBarPreference.set(enabled)
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        enableFloatingBottomBarBlurPreference.set(enabled)
    }

    fun setPageScale(scale: Float) {
        pageScalePreference.set(scale.coerceIn(MIN_PAGE_SCALE, MAX_PAGE_SCALE))
    }

    fun setEnablePredictiveBack(enabled: Boolean) {
        enablePredictiveBackPreference.set(enabled)
    }

    // ── snapshot getters ──

    /** 同步读取当前主题模式（非响应式，用于组合上下文之外） */
    fun getThemeModeSnapshot(): ThemeMode = themeModePreference.snapshot

    fun getThemeColorSnapshot(): ThemeColor = themeColorPreference.snapshot

    fun getUiStyleSnapshot(): UiStyle = uiStylePreference.snapshot

    fun getCheckUpdateSnapshot(): Boolean = checkUpdatePreference.snapshot

    private companion object {
        const val PREFS_NAME = "devinfo_theme_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_UI_STYLE = "ui_style"
        const val KEY_CHECK_UPDATE = "check_update"
        const val KEY_PALETTE_STYLE = "palette_style"
        const val KEY_COLOR_SPEC = "color_spec"
        const val KEY_ENABLE_BLUR = "enable_blur"
        const val KEY_ENABLE_FLOATING_BOTTOM_BAR = "enable_floating_bottom_bar"
        const val KEY_ENABLE_FLOATING_BOTTOM_BAR_BLUR = "enable_floating_bottom_bar_blur"
        const val KEY_PAGE_SCALE = "page_scale"
        const val KEY_ENABLE_PREDICTIVE_BACK = "enable_predictive_back"
        const val MIN_PAGE_SCALE = 0.8f
        const val MAX_PAGE_SCALE = 1.1f
        const val DEFAULT_PAGE_SCALE = 1.0f
    }
}
