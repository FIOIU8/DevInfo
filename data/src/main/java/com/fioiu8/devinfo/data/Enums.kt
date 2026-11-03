/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data

import com.fioiu8.devinfo.data.R

/**
 * App language selection mode.
 *
 * [localeTag] is null for SYSTEM (follow the system locale) and for CUSTOM (tag lives in
 * the custom locale preference). It carries a real BCP-47 tag for the fixed languages.
 */
enum class AppLanguage(
    val displayNameResId: Int,
    val localeTag: String?,
    val isCustom: Boolean = false
) {
    SYSTEM(R.string.language_system, null),
    SIMPLIFIED_CHINESE(R.string.language_chinese, "zh"),
    ENGLISH(R.string.language_english, "en"),
    JAPANESE(R.string.language_japanese, "ja"),
    CUSTOM(R.string.language_custom, null, isCustom = true);
}
