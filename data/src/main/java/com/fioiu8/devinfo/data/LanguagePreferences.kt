/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data

import android.content.Context
import com.fioiu8.devinfo.data.AppLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Language preferences storage, following the same pattern as [ThemePreferences].
 * Uses SharedPreferences for persistence and exposes via StateFlow.
 */
class LanguagePreferences(context: Context) : BasePreferences<String>(context, PREFS_NAME) {

    private val appLanguagePreference =
        enumPreference(
            key = KEY_APP_LANGUAGE,
            defaultValue = AppLanguage.SYSTEM,
        )

    private val customLocaleTagPreference =
        stringPreference(
            key = KEY_CUSTOM_LOCALE,
            defaultValue = "",
            isValid = PreferenceValidators::isValidLocaleTag,
        )

    val appLanguage: Flow<AppLanguage> = appLanguagePreference.flow

    val customLocaleTag: Flow<String> = customLocaleTagPreference.flow

    /**
     * 同步读取当前已持久化的语言。
     *
     * Activity 首帧必须在组合期就拿到真实取值：若只给 Flow 一个默认的初始值，首帧会用
     * 默认值、随后才被 Flow 的真值改写，把 UI 层的 key（这里用于选 ViewModel）带偏一次。
     */
    val currentAppLanguage: AppLanguage
        get() = appLanguagePreference.snapshot

    val currentCustomLocaleTag: String
        get() = customLocaleTagPreference.snapshot

    fun setAppLanguage(language: AppLanguage) {
        appLanguagePreference.set(language)
    }

    fun setCustomLocaleTag(tag: String): Boolean {
        val normalizedTag = PreferenceValidators.normalizedLocaleTag(tag) ?: return false
        customLocaleTagPreference.set(normalizedTag)
        return true
    }

    fun getEffectiveLocaleTag(): String? {
        val lang = appLanguagePreference.snapshot
        return if (lang == AppLanguage.CUSTOM) {
            val tag = customLocaleTagPreference.snapshot
            PreferenceValidators.normalizedLocaleTag(tag)
        } else {
            lang.localeTag
        }
    }

    private companion object {
        const val PREFS_NAME = "devinfo_language_prefs"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_CUSTOM_LOCALE = "custom_locale_tag"
    }
}
