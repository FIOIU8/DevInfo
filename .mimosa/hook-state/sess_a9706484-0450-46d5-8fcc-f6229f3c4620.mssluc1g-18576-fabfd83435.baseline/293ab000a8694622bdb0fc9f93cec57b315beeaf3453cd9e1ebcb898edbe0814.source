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

package com.fioiu8.devinfo.data

import com.fioiu8.devinfo.data.R

// InfoCategory, ThemeMode, ThemeColor moved to com.fioiu8.devinfo.core.model

/**
 * App language selection mode.
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
    CUSTOM(R.string.language_custom, "_custom", isCustom = true);
}
