/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fioiu8.devinfo

import java.util.Locale

internal inline fun <reified T : Enum<T>> enumValueOrDefault(
    storedValue: String?,
    defaultValue: T,
): T = storedValue?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: defaultValue

internal object PreferenceValidators {

    const val MIN_PAGE_SCALE = 0.8f
    const val MAX_PAGE_SCALE = 1.1f
    const val DEFAULT_PAGE_SCALE = 1.0f

    private val localeTagPattern = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")

    fun pageScaleOrDefault(value: Float): Float =
        value.takeIf { it.isFinite() && it in MIN_PAGE_SCALE..MAX_PAGE_SCALE } ?: DEFAULT_PAGE_SCALE

    fun isValidLocaleTag(tag: String): Boolean = normalizedLocaleTag(tag) != null

    fun normalizedLocaleTag(tag: String): String? {
        val normalized = tag.trim()
        if (normalized.isEmpty() || normalized.length > 64 || !localeTagPattern.matches(normalized)) {
            return null
        }

        val locale = Locale.forLanguageTag(normalized)
        return locale.takeUnless { it == Locale.ROOT || it.language.isEmpty() }?.toLanguageTag()
    }

    fun validLastCheckTimeOrZero(value: Long, now: Long): Long =
        value.takeIf { it in 0..now } ?: 0L
}
