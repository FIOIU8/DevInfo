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

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CpuUsageReading(
    val coreMetrics: List<CpuCoreMetric>,
    val overallUsage: Float?
)

/** Handler-driven sampler whose scheduled task cannot retain a disposed screen. */
class CpuUsageSampler(private val collector: DeviceInfoCollector) {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selfReference = WeakReference(this)
    private var onSample: ((CpuUsageReading) -> Unit)? = null
    private var running = false

    private val task: Runnable

    init {
        task = object : Runnable {
            override fun run() {
                val sampler = selfReference.get() ?: return
                if (!sampler.running) return
                sampler.scope.launch {
                    val reading = sampler.read()
                    withContext(Dispatchers.Main.immediate) {
                        sampler.onSample?.invoke(reading)
                        if (sampler.running) sampler.handler.postDelayed(sampler.task, SAMPLE_INTERVAL_MS)
                    }
                }
            }
        }
    }

    fun start(callback: (CpuUsageReading) -> Unit) {
        stop()
        running = true
        onSample = callback
        handler.post(task)
    }

    fun stop() {
        running = false
        onSample = null
        handler.removeCallbacks(task)
        scope.coroutineContext.cancelChildren()
    }

    private fun read(): CpuUsageReading {
        val cores = collector.getCpuCoreMetrics()
        return if (cores.isNotEmpty()) {
            CpuUsageReading(cores, cores.mapNotNull { it.usagePercent }.average().toFloat())
        } else {
            CpuUsageReading(emptyList(), collector.getCpuUsagePercent())
        }
    }

    private companion object {
        const val SAMPLE_INTERVAL_MS = 2_000L
    }
}
