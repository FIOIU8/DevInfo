/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fioiu8.devinfo.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** SharedPreferences implementation retained as a rollback and compatibility path. */
class SharedPreferencesPreferenceRepository(
    context: Context,
    preferenceName: String,
) : PreferenceRepository {

    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    override suspend fun readLong(key: String): Long? = withContext(Dispatchers.IO) {
        runCatching { preferences.getLong(key, Long.MIN_VALUE) }
            .getOrNull()
            ?.takeUnless { it == Long.MIN_VALUE }
            ?: runCatching { preferences.getString(key, null)?.toLongOrNull() }.getOrNull()
    }

    override suspend fun readString(key: String): String? = withContext(Dispatchers.IO) {
        runCatching { preferences.getString(key, null) }.getOrNull()
    }

    override suspend fun writeLong(key: String, value: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            preferences.edit().putLong(key, value).apply()
            true
        }.getOrDefault(false)
    }

    override suspend fun writeString(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            preferences.edit().putString(key, value).apply()
            true
        }.getOrDefault(false)
    }
}

/**
 * DataStore-backed repository for the update cache. The legacy store is read and written as a
 * compatibility path until a later release can remove it after a verified rollback window.
 */
class DataStorePreferenceRepository(
    context: Context,
    private val legacyPreferenceName: String = LEGACY_PREFERENCE_NAME,
    dataStoreFileName: String = DATASTORE_FILE_NAME,
) : PreferenceRepository {

    private val legacyRepository = SharedPreferencesPreferenceRepository(context, legacyPreferenceName)

    private val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(dataStoreFileName) },
    )

    override suspend fun readLong(key: String): Long? =
        readDataStoreValue { preferences -> preferences[longPreferencesKey(key)] }
            ?: legacyRepository.readLong(key)

    override suspend fun readString(key: String): String? =
        readDataStoreValue { preferences -> preferences[stringPreferencesKey(key)] }
            ?: legacyRepository.readString(key)

    override suspend fun writeLong(key: String, value: Long): Boolean {
        val dataStoreResult = writeDataStoreValue { preferences ->
            preferences[longPreferencesKey(key)] = value
        }
        val legacyResult = legacyRepository.writeLong(key, value)
        return dataStoreResult && legacyResult
    }

    override suspend fun writeString(key: String, value: String): Boolean {
        val dataStoreResult = writeDataStoreValue { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
        val legacyResult = legacyRepository.writeString(key, value)
        return dataStoreResult && legacyResult
    }

    private val migrationMutex = Mutex()

    private suspend fun <T> readDataStoreValue(selector: (Preferences) -> T): T? {
        migrateLegacyValues()
        return readDataStoreSnapshot()?.let(selector)
    }

    private suspend fun migrateLegacyValues() {
        migrationMutex.withLock {
            val currentData = readDataStoreSnapshot() ?: return
            val legacyLastCheck = legacyRepository.readLong(KEY_LAST_CHECK)
            val legacyCachedTag = legacyRepository.readString(KEY_CACHED_TAG)
            if (currentData[longPreferencesKey(KEY_LAST_CHECK)] != null &&
                currentData[stringPreferencesKey(KEY_CACHED_TAG)] != null
            ) {
                return
            }

            runCatching {
                dataStore.edit { preferences ->
                    if (preferences[longPreferencesKey(KEY_LAST_CHECK)] == null) {
                        legacyLastCheck?.let { preferences[longPreferencesKey(KEY_LAST_CHECK)] = it }
                    }
                    if (preferences[stringPreferencesKey(KEY_CACHED_TAG)] == null) {
                        legacyCachedTag?.let { preferences[stringPreferencesKey(KEY_CACHED_TAG)] = it }
                    }
                }
            }
        }
    }

    private suspend fun readDataStoreSnapshot(): Preferences? =
        runCatching {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .first()
        }.getOrNull()

    private suspend fun writeDataStoreValue(update: (MutablePreferences) -> Unit): Boolean =
        runCatching {
            dataStore.edit { preferences -> update(preferences) }
            true
        }.getOrDefault(false)

    private companion object {
        const val LEGACY_PREFERENCE_NAME = "devinfo_update"
        const val DATASTORE_FILE_NAME = "devinfo_update.preferences_pb"
        const val KEY_LAST_CHECK = "last_check_time"
        const val KEY_CACHED_TAG = "cached_tag"
    }
}

internal fun readLegacyValue(values: Map<String, *>, key: String): Any? = when (key) {
    "last_check_time" -> when (val value = values[key]) {
        is Long -> value
        is String -> value.toLongOrNull()
        else -> null
    }

    "cached_tag" -> values[key] as? String
    else -> null
}
