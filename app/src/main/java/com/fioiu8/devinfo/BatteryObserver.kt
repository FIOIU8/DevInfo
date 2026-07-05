package com.fioiu8.devinfo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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

    val batteryState: Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                val pct = if (scale > 0) (level * 100) / scale else 0
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                               status == BatteryManager.BATTERY_STATUS_FULL
                trySend(BatteryState(pct.coerceIn(0, 100), charging))
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        appContext.registerReceiver(receiver, filter)

        // 立即发送一次当前值（sticky broadcast 在 registerReceiver 时就能拿到）
        // 但 callbackFlow 需要 emit；registerReceiver 的回调在 register 时就会触发 sticky intent
        // 所以不需要手动 emit

        awaitClose { appContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
