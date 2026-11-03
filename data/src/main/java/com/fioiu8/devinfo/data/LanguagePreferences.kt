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
