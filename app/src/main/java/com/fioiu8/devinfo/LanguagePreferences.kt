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
import com.fioiu8.devinfo.model.AppLanguage
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
            values = AppLanguage.entries,
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

    fun getAppLanguageSnapshot(): AppLanguage = appLanguagePreference.snapshot

    fun getCustomLocaleTagSnapshot(): String = customLocaleTagPreference.snapshot

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
