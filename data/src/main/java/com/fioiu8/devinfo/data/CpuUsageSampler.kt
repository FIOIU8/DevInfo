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

import android.os.Handler
import android.os.Looper
import com.fioiu8.devinfo.core.model.CpuUsageReading
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun cpuSamplingIntervalMs(consecutiveFailures: Int): Long =
    if (consecutiveFailures >= 3) 5_000L else 2_000L

/** Handler-driven sampler whose scheduled task cannot retain a disposed screen. */
class CpuUsageSampler(private val collector: DeviceInfoCollector) {
    private val handler = Handler(Looper.getMainLooper())

    // 采样会派生 top 子进程并阻塞读取 /proc，必须使用 IO 调度器，
    // 避免饿死线程数有限的 Default 调度器
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selfReference = WeakReference(this)
    private var onSample: ((CpuUsageReading) -> Unit)? = null
    @Volatile private var running = false
    @Volatile private var generation = 0L
    @Volatile private var consecutiveFailures = 0
    private var lastDeliveredReading: CpuUsageReading? = null

    private val task: Runnable

    init {
        task = object : Runnable {
            override fun run() {
                val sampler = selfReference.get() ?: return
                if (!sampler.running) return
                val runGeneration = sampler.generation
                sampler.scope.launch {
                    val reading = sampler.read()
                    withContext(Dispatchers.Main.immediate) {
                        if (!sampler.running || sampler.generation != runGeneration) return@withContext
                        if (reading != sampler.lastDeliveredReading) {
                            sampler.lastDeliveredReading = reading
                            runCatching { sampler.onSample?.invoke(reading) }
                        }
                        if (sampler.running) {
                            sampler.handler.postDelayed(sampler.task, sampler.nextDelay(reading))
                        }
                    }
                }
            }
        }
    }

    fun start(callback: (CpuUsageReading) -> Unit) {
        stop()
        generation++
        running = true
        consecutiveFailures = 0
        lastDeliveredReading = null
        onSample = callback
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        handler.post(task)
    }

    fun stop() {
        running = false
        generation++
        onSample = null
        lastDeliveredReading = null
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }

    private suspend fun read(): CpuUsageReading {
        val cores = runCatching { collector.getCpuCoreMetrics() }.getOrDefault(emptyList())
        return if (cores.isNotEmpty()) {
            val perCoreAverage = cores.mapNotNull { it.usagePercent }
                .average()
                .toFloat()
                .takeIf { !it.isNaN() }
            CpuUsageReading(
                cores,
                perCoreAverage ?: runCatching { collector.getCpuUsagePercent() }.getOrNull()
            )
        } else {
            CpuUsageReading(emptyList(), runCatching { collector.getCpuUsagePercent() }.getOrNull())
        }
    }

    private fun nextDelay(reading: CpuUsageReading): Long {
        if (reading.overallUsage == null && reading.coreMetrics.none { it.usagePercent != null }) {
            consecutiveFailures++
        } else {
            consecutiveFailures = 0
        }
        return cpuSamplingIntervalMs(consecutiveFailures)
    }

}
