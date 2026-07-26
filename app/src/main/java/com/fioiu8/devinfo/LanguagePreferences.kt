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
        )

    val appLanguage: Flow<AppLanguage> = appLanguagePreference.flow

    val customLocaleTag: Flow<String> = customLocaleTagPreference.flow

    fun setAppLanguage(language: AppLanguage) {
        appLanguagePreference.set(language)
    }

    fun setCustomLocaleTag(tag: String) {
        customLocaleTagPreference.set(tag)
    }

    fun getAppLanguageSnapshot(): AppLanguage = appLanguagePreference.snapshot

    fun getCustomLocaleTagSnapshot(): String = customLocaleTagPreference.snapshot

    fun getEffectiveLocaleTag(): String? {
        val lang = appLanguagePreference.snapshot
        return if (lang == AppLanguage.CUSTOM) {
            val tag = customLocaleTagPreference.snapshot
            if (tag.isBlank()) null else tag
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
