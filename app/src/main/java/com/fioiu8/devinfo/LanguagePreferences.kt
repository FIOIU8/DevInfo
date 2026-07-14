package com.fioiu8.devinfo

import android.content.Context
import com.fioiu8.devinfo.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Language preferences storage, following the same pattern as [ThemePreferences].
 * Uses SharedPreferences for persistence and exposes via StateFlow.
 */
class LanguagePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(loadLanguage())
    val appLanguage: Flow<AppLanguage> = _appLanguage.asStateFlow()

    private val _customLocaleTag = MutableStateFlow(loadCustomLocaleTag())
    val customLocaleTag: Flow<String> = _customLocaleTag.asStateFlow()

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _appLanguage.value = language
    }

    fun setCustomLocaleTag(tag: String) {
        prefs.edit().putString(KEY_CUSTOM_LOCALE, tag).apply()
        _customLocaleTag.value = tag
    }

    fun getAppLanguageSnapshot(): AppLanguage = _appLanguage.value

    fun getCustomLocaleTagSnapshot(): String = _customLocaleTag.value

    fun getEffectiveLocaleTag(): String? {
        val lang = _appLanguage.value
        return if (lang == AppLanguage.CUSTOM) {
            val tag = _customLocaleTag.value
            if (tag.isBlank()) null else tag
        } else {
            lang.localeTag
        }
    }

    private fun loadLanguage(): AppLanguage {
        val name = prefs.getString(KEY_APP_LANGUAGE, null) ?: return AppLanguage.SYSTEM
        return AppLanguage.entries.firstOrNull { it.name == name } ?: AppLanguage.SYSTEM
    }

    private fun loadCustomLocaleTag(): String {
        return prefs.getString(KEY_CUSTOM_LOCALE, "") ?: ""
    }

    private companion object {
        const val PREFS_NAME = "devinfo_language_prefs"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_CUSTOM_LOCALE = "custom_locale_tag"
    }
}
