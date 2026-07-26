package com.fioiu8.devinfo

import android.content.Context
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import kotlinx.coroutines.flow.Flow

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

    val themeMode: Flow<ThemeMode> = themeModePreference.flow

    val themeColor: Flow<ThemeColor> = themeColorPreference.flow

    /** 设置并持久化主题模式 */
    fun setThemeMode(mode: ThemeMode) {
        themeModePreference.set(mode)
    }

    fun setThemeColor(color: ThemeColor) {
        themeColorPreference.set(color)
    }

    // ── snapshot getters ──

    /** 同步读取当前主题模式（非响应式，用于组合上下文之外） */
    fun getThemeModeSnapshot(): ThemeMode = themeModePreference.snapshot

    fun getThemeColorSnapshot(): ThemeColor = themeColorPreference.snapshot

    private companion object {
        const val PREFS_NAME = "devinfo_theme_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_COLOR = "theme_color"
    }
}
