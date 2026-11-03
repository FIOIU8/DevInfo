/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
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
