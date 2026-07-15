package com.fioiu8.devinfo

import android.content.Context
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.model.ThemeColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 简单的内存+SP 主题偏好存储。
 * 使用 SharedPreferences 实现持久化，通过 StateFlow 暴露当前值。
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    private val _themeColor = MutableStateFlow(loadThemeColor())
    val themeColor: Flow<ThemeColor> = _themeColor.asStateFlow()

    /** 设置并持久化主题模式 */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setThemeColor(color: ThemeColor) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
        _themeColor.value = color
    }

    // ── snapshot getters ──

    /** 同步读取当前主题模式（非响应式，用于组合上下文之外） */
    fun getThemeModeSnapshot(): ThemeMode = _themeMode.value

    fun getThemeColorSnapshot(): ThemeColor = _themeColor.value

    // ── loading ──
    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
    }

    private fun loadThemeColor(): ThemeColor {
        val name = prefs.getString(KEY_THEME_COLOR, null) ?: return ThemeColor.DEFAULT
        return ThemeColor.entries.firstOrNull { it.name == name } ?: ThemeColor.DEFAULT
    }

    private companion object {
        const val PREFS_NAME = "devinfo_theme_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_COLOR = "theme_color"
    }
}
