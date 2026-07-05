package com.fioiu8.devinfo

import android.content.Context
import com.fioiu8.devinfo.model.MountThemeColor
import com.fioiu8.devinfo.model.ThemeMode
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

    private val _mountThemeColor = MutableStateFlow(loadMountColor())
    val mountThemeColor: Flow<MountThemeColor> = _mountThemeColor.asStateFlow()

    private val _useMountTheme = MutableStateFlow(loadUseMountTheme())
    val useMountTheme: Flow<Boolean> = _useMountTheme.asStateFlow()

    /** 设置并持久化主题模式 */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    /** 设置并持久化自定义主题色 */
    fun setMountThemeColor(color: MountThemeColor) {
        prefs.edit().putString(KEY_MOUNT_COLOR, color.name).apply()
        _mountThemeColor.value = color
    }

    /** 设置是否使用自定义主题色的开关 */
    fun setUseMountTheme(use: Boolean) {
        prefs.edit().putBoolean(KEY_USE_MOUNT, use).apply()
        _useMountTheme.value = use
    }

    // ── snapshot getters ──

    /** 同步读取当前主题模式（非响应式，用于组合上下文之外） */
    fun getThemeModeSnapshot(): ThemeMode = _themeMode.value

    /** 同步读取当前主题色（非响应式） */
    fun getMountThemeColorSnapshot(): MountThemeColor = _mountThemeColor.value

    /** 同步读取自定义主题色开关状态（非响应式） */
    fun getUseMountThemeSnapshot(): Boolean = _useMountTheme.value

    // ── loading ──
    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
    }

    private fun loadMountColor(): MountThemeColor {
        val name = prefs.getString(KEY_MOUNT_COLOR, null) ?: return MountThemeColor.DEFAULT
        return MountThemeColor.entries.firstOrNull { it.name == name } ?: MountThemeColor.DEFAULT
    }

    private fun loadUseMountTheme(): Boolean = prefs.getBoolean(KEY_USE_MOUNT, false)

    private companion object {
        const val PREFS_NAME = "devinfo_theme_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_MOUNT_COLOR = "mount_color"
        const val KEY_USE_MOUNT = "use_mount"
    }
}
