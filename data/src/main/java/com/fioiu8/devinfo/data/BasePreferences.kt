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

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed state for a related group of settings.
 *
 * [T] is the setting-key type. Subclasses declare their keys and default values through the
 * typed helpers while this class keeps the persisted value and its in-memory StateFlow
 * synchronized.
 */
abstract class BasePreferences<T : CharSequence>(
    context: Context,
    preferenceName: String,
) : AutoCloseable {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    /** Tracks all registered listeners so they can be unregistered on close. */
    private val registeredListeners =
        mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    protected fun <V : Enum<V>> enumPreference(
        key: T,
        defaultValue: V,
        values: Iterable<V>,
    ): PersistentValue<V> =
        persistentValue(
            key = key,
            deserialize = { storedValue ->
                values.firstOrNull { it.name == storedValue } ?: defaultValue
            },
            serialize = { it.name },
        )

    /** Reads an enum without allowing malformed persisted data to escape to callers. */
    private inline fun <reified V : Enum<V>> readEnumOrDefault(
        key: String,
        defaultValue: V,
    ): V = enumValueOrDefault(readStringSafely(key), defaultValue)

    protected fun stringPreference(
        key: T,
        defaultValue: String,
        isValid: (String) -> Boolean = { true },
    ): PersistentValue<String> =
        persistentValue(
            key = key,
            deserialize = { it?.takeIf(isValid) ?: defaultValue },
            serialize = { it },
        )

    protected fun booleanPreference(
        key: T,
        defaultValue: Boolean,
    ): PersistentValue<Boolean> =
        persistentValue(
            key = key,
            deserialize = { it?.toBooleanStrictOrNull() ?: defaultValue },
            serialize = { it.toString() },
        )

    protected fun floatPreference(
        key: T,
        defaultValue: Float,
        isValid: (Float) -> Boolean = { it.isFinite() },
    ): PersistentValue<Float> =
        persistentValue(
            key = key,
            deserialize = { it?.toFloatOrNull()?.takeIf(isValid) ?: defaultValue },
            serialize = { it.toString() },
        )

    private fun <V : Any> persistentValue(
        key: T,
        deserialize: (String?) -> V,
        serialize: (V) -> String,
    ): PersistentValue<V> =
        PersistentValue(
            key = key,
            readValue = { deserialize(readStringSafely(key.toString())) },
            serialize = serialize,
        )

    private fun readStringSafely(key: String): String? =
        runCatching { preferences.getString(key, null) }.getOrNull()

    protected inner class PersistentValue<V : Any> internal constructor(
        private val key: T,
        readValue: () -> V,
        private val serialize: (V) -> String,
    ) {

        private val mutableValue = MutableStateFlow(readValue())

        private val preferenceListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key.toString()) {
                    mutableValue.value = readValue()
                }
            }

        init {
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
            registeredListeners.add(preferenceListener)
        }

        val flow: StateFlow<V> = mutableValue.asStateFlow()

        val snapshot: V
            get() = mutableValue.value

        fun set(value: V) {
            preferences.edit().putString(key.toString(), serialize(value)).commit()
        }
    }

    /**
     * Unregisters all preference listeners to prevent memory leaks.
     * Call this when the preferences instance is no longer needed (e.g. Activity.onDestroy).
     */
    override fun close() {
        registeredListeners.forEach { preferences.unregisterOnSharedPreferenceChangeListener(it) }
        registeredListeners.clear()
    }
}
