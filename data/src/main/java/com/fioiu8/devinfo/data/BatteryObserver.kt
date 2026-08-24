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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 电池状态实时监听器。
 * 通过 BroadcastReceiver 监听 ACTION_BATTERY_CHANGED，暴露 StateFlow。
 */
class BatteryObserver(context: Context) {

    data class BatteryState(
        val level: Int,          // 0–100
        val isCharging: Boolean
    )

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "BatteryObserver"
    }

    val batteryState: Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                try {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                    if (scale <= 0 || level !in 0..scale) return
                    val pct = (level * 100) / scale
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                   status == BatteryManager.BATTERY_STATUS_FULL
                    trySend(BatteryState(pct.coerceIn(0, 100), charging))
                } catch (e: Exception) {
                    // 防御：畸形广播不应让 flow 关闭，仅忽略本次
                    Log.w(TAG, "parse battery broadcast failed", e)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // ACTION_BATTERY_CHANGED is delivered by the system and does not need an
        // externally addressable receiver. Keep the receiver private to the app.
        val registered = runCatching {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }.isSuccess
        if (!registered) {
            close(IllegalStateException("Battery receiver registration failed"))
            return@callbackFlow
        }

        // 立即发送一次当前值（sticky broadcast 在 registerReceiver 时就能拿到）
        // 但 callbackFlow 需要 emit；registerReceiver 的回调在 register 时就会触发 sticky intent
        // 所以不需要手动 emit

        awaitClose { runCatching { appContext.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()
}
