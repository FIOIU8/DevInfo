package com.fioiu8.devinfo

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
) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

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

    protected fun stringPreference(
        key: T,
        defaultValue: String,
    ): PersistentValue<String> =
        persistentValue(
            key = key,
            deserialize = { it ?: defaultValue },
            serialize = { it },
        )

    private fun <V : Any> persistentValue(
        key: T,
        deserialize: (String?) -> V,
        serialize: (V) -> String,
    ): PersistentValue<V> =
        PersistentValue(
            key = key,
            initialValue = deserialize(preferences.getString(key.toString(), null)),
            serialize = serialize,
        )

    protected inner class PersistentValue<V : Any> internal constructor(
        private val key: T,
        initialValue: V,
        private val serialize: (V) -> String,
    ) {

        private val mutableValue = MutableStateFlow(initialValue)

        val flow: StateFlow<V> = mutableValue.asStateFlow()

        val snapshot: V
            get() = mutableValue.value

        fun set(value: V) {
            preferences.edit().putString(key.toString(), serialize(value)).apply()
            mutableValue.value = value
        }
    }
}
