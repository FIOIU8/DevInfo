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

package com.fioiu8.devinfo

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.util.UUID

class DeviceIdManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns an application-local identifier used for internal identification and export help.
     * This value must not be sent to a remote service.
     */
    fun getOrCreateDeviceId(): String {
        readSavedId(KEY_DEVICE_ID)?.let { return it }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }

        val deviceId = androidId?.trim()?.takeIf(::isUsableDeviceId) ?: generateUUID() ?: fallbackId
        runCatching { prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply() }
        return deviceId
    }

    private fun generateUUID(): String? {
        readSavedId(KEY_GENERATED_UUID)?.let { return it }

        return runCatching { UUID.randomUUID().toString() }.getOrNull()?.also { uuid ->
            runCatching { prefs.edit().putString(KEY_GENERATED_UUID, uuid).apply() }
        }
    }

    private fun readSavedId(key: String): String? =
        runCatching { prefs.getString(key, null) }
            .getOrNull()
            ?.trim()
            ?.takeIf(::isUsableDeviceId)

    private fun isUsableDeviceId(value: String): Boolean =
        value.isNotEmpty() && value.length <= MAX_ID_LENGTH && value != INVALID_ANDROID_ID

    private val fallbackId: String by lazy {
        runCatching { UUID.randomUUID().toString() }.getOrElse { context.packageName }
    }

    fun resetDeviceId() {
        runCatching { prefs.edit().remove(KEY_DEVICE_ID).apply() }
    }

    private companion object {
        const val PREFS_NAME = "device_prefs"
        const val KEY_DEVICE_ID = "device_unique_id"
        const val KEY_GENERATED_UUID = "generated_uuid"
        const val INVALID_ANDROID_ID = "9774d56d682e549c"
        const val MAX_ID_LENGTH = 128
    }
}
