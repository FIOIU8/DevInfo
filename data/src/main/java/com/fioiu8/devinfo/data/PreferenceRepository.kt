/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fioiu8.devinfo.data

/**
 * Typed storage boundary used by preference consumers. Implementations must return null for
 * unavailable or malformed values so storage failures do not escape into the UI layer.
 */
interface PreferenceRepository {
    suspend fun readLong(key: String): Long?

    suspend fun readString(key: String): String?

    /** Writes related values as one logical update where the backend supports it. */
    suspend fun writeBatch(values: Map<String, PreferenceValue>): Boolean
}

sealed interface PreferenceValue {
    data class LongValue(val value: Long) : PreferenceValue

    data class StringValue(val value: String) : PreferenceValue
}
