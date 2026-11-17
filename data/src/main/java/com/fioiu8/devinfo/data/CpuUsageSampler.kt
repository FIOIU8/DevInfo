/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.fioiu8.devinfo.core.model.CpuUsageReading
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun cpuSamplingIntervalMs(consecutiveFailures: Int): Long =
    if (consecutiveFailures >= 3) 5_000L else 2_000L

/** 固定频率调度：扣除本次读取耗时，避免读得越慢周期越长的漂移。 */
fun cpuSamplingDelayMs(elapsedMs: Long, consecutiveFailures: Int): Long =
    (cpuSamplingIntervalMs(consecutiveFailures) - elapsedMs).coerceAtLeast(0L)

/** Handler-driven sampler whose scheduled task cannot retain a disposed screen. */
class CpuUsageSampler(private val collector: DeviceInfoCollector) {
    private val handler = Handler(Looper.getMainLooper())

    // 采样会派生 top 子进程并阻塞读取 /proc，必须使用 IO 调度器，
    // 避免饿死线程数有限的 Default 调度器
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selfReference = WeakReference(this)
    private var onSample: ((CpuUsageReading) -> Unit)? = null
    private var useRoot = false
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
                    val startedAt = SystemClock.uptimeMillis()
                    val reading = sampler.read()
                    val elapsed = SystemClock.uptimeMillis() - startedAt
                    withContext(Dispatchers.Main.immediate) {
                        if (!sampler.running || sampler.generation != runGeneration) return@withContext
                        if (reading != sampler.lastDeliveredReading) {
                            sampler.lastDeliveredReading = reading
                            runCatching { sampler.onSample?.invoke(reading) }
                        }
                        if (sampler.running) {
                            sampler.handler.postDelayed(
                                sampler.task,
                                sampler.nextDelay(reading, elapsed),
                            )
                        }
                    }
                }
            }
        }
    }

    fun start(useRoot: Boolean = false, callback: (CpuUsageReading) -> Unit) {
        stop()
        generation++
        running = true
        this.useRoot = useRoot
        consecutiveFailures = 0
        lastDeliveredReading = null
        onSample = callback
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        handler.post(task)
    }

    fun stop() {
        running = false
        generation++
        useRoot = false
        onSample = null
        lastDeliveredReading = null
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }

    private suspend fun read(): CpuUsageReading {
        return try {
            if (useRoot) {
                val rooted = collector.readCpuUsageReading(useRoot = true)
                if (rooted.coreMetrics.isNotEmpty() || rooted.overallUsage != null) {
                    rooted
                } else {
                    collector.readCpuUsageReading()
                }
            } else {
                collector.readCpuUsageReading()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            CpuUsageReading(emptyList(), null)
        }
    }

    private fun nextDelay(reading: CpuUsageReading, elapsedMs: Long): Long {
        if (reading.overallUsage == null && reading.coreMetrics.none { it.usagePercent != null }) {
            consecutiveFailures++
        } else {
            consecutiveFailures = 0
        }
        return cpuSamplingDelayMs(elapsedMs, consecutiveFailures)
    }

}
