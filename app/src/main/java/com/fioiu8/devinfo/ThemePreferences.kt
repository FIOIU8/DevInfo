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

    val themeMode: StateFlow<ThemeMode> = themeModePreference.flow

    val themeColor: StateFlow<ThemeColor> = themeColorPreference.flow

    val uiStyle: StateFlow<UiStyle> = uiStylePreference.flow

    val checkUpdate: StateFlow<Boolean> = checkUpdatePreference.flow

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
    }
}
