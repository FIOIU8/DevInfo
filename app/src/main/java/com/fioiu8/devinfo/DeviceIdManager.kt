package com.fioiu8.devinfo

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.util.UUID

class DeviceIdManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
    private val KEY_DEVICE_ID = "device_unique_id"

    /**
     * 获取或创建设备唯一标识
     * 优先使用 ANDROID_ID，如果失败则使用自定义生成的 UUID
     */
    fun getOrCreateDeviceId(): String {
        // 1. 首先尝试从 SharedPreferences 获取已保存的 ID
        prefs.getString(KEY_DEVICE_ID, null)?.let { savedId ->
            return savedId
        }

        // 2. 尝试获取 ANDROID_ID
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }

        // 3. 如果 ANDROID_ID 有效且不是已知的无效值，则使用它
        val deviceId = if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
            androidId
        } else {
            // 4. 否则生成一个 UUID 作为备选
            generateUUID()
        }

        // 5. 保存生成的 ID
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()

        return deviceId
    }

    /**
     * 生成唯一的 UUID
     */
    private fun generateUUID(): String {
        // 尝试从 SharedPreferences 获取已保存的 UUID
        prefs.getString("uuid", null)?.let { savedUUID ->
            return savedUUID
        }

        // 生成新的 UUID
        val uuid = UUID.randomUUID().toString()
        prefs.edit().putString("uuid", uuid).apply()
        return uuid
    }

    /**
     * 重置设备 ID（可选功能）
     */
    fun resetDeviceId() {
        prefs.edit().remove(KEY_DEVICE_ID).apply()
    }
}