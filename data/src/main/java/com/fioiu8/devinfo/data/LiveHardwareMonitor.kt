/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.fioiu8.devinfo.core.model.LiveHardwareSnapshot
import com.fioiu8.devinfo.core.model.MotionSensorSample
import com.fioiu8.devinfo.core.model.MotionSensorSampleBuffer
import com.fioiu8.devinfo.core.model.MotionSensorSnapshot
import com.fioiu8.devinfo.core.model.MotionSensorType
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Collects short-lived hardware signals while the overview is visible. */
class LiveHardwareMonitor(context: Context) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val motionSensors = listOfNotNull(
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    )
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    @Volatile private var movingUntil = 0L
    @Volatile private var collectMotion = false
    @Volatile private var collectLiveReadings = false
    private val registeredMotionSensors = mutableSetOf<Sensor>()
    private var motionRegistrationAttempted = false
    private val motionSampleBuffer = MotionSensorSampleBuffer()
    private val _motionSensorSnapshot = MutableStateFlow(MotionSensorSnapshot())
    val motionSensorSnapshot: StateFlow<MotionSensorSnapshot> = _motionSensorSnapshot.asStateFlow()
    private var lastMotionUiUpdateMillis = 0L
    private var lastSectorsRead: Long? = null
    private var lastStorageSampleAt = 0L
    private val recentReadSpeeds = ArrayDeque<Int>()

    @Synchronized
    fun start(collectMotion: Boolean, collectLiveReadings: Boolean = false) {
        if (motionRegistrationAttempted && this.collectMotion == collectMotion &&
            this.collectLiveReadings == collectLiveReadings
        ) return
        sensorManager?.unregisterListener(this)
        registeredMotionSensors.clear()
        motionSampleBuffer.clear()
        _motionSensorSnapshot.value = MotionSensorSnapshot()
        lastMotionUiUpdateMillis = 0L
        this.collectMotion = collectMotion
        this.collectLiveReadings = collectLiveReadings
        motionRegistrationAttempted = true
        if (!collectMotion && !collectLiveReadings) return
        motionSensors.forEach { sensor ->
            val registered = runCatching {
                val samplingPeriod = if (collectLiveReadings) {
                    SensorManager.SENSOR_DELAY_GAME
                } else {
                    SensorManager.SENSOR_DELAY_NORMAL
                }
                sensorManager?.registerListener(this, sensor, samplingPeriod) == true
            }.getOrDefault(false)
            if (registered) registeredMotionSensors += sensor
        }
    }

    @Synchronized
    fun stop() {
        start(collectMotion = false, collectLiveReadings = false)
    }

    @Synchronized
    fun snapshot(includeStorageReadSpeed: Boolean = true): LiveHardwareSnapshot {
        val (instantSpeed, averageSpeed) = if (includeStorageReadSpeed) {
            readStorageSpeed()
        } else {
            null to null
        }
        return LiveHardwareSnapshot(
            // 语义是“设备是否具备运动传感器”而非“当前是否已注册监听”——
            // LOW_FREQUENCY 模式不注册监听，但不代表设备没有传感器
            motionAvailable = motionSensors.isNotEmpty(),
            moving = SystemClock.elapsedRealtime() < movingUntil,
            brightnessPercent = readBrightnessPercent(),
            storageReadSpeedMbps = instantSpeed,
            storageAverageReadSpeedMbps = averageSpeed,
            wifiRssiDbm = readWifiRssi()
        )
    }

    @Synchronized
    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        val magnitude = sqrt(
            event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
        )
        if (collectMotion) {
            val motionThreshold = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) 1.25f else 0.35f
            if ((event.sensor.type == Sensor.TYPE_ACCELEROMETER &&
                    kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH) > motionThreshold
                    ) ||
                (event.sensor.type == Sensor.TYPE_GYROSCOPE && magnitude > motionThreshold)
            ) {
                movingUntil = SystemClock.elapsedRealtime() + MOTION_HOLD_MS
            }
        }
        if (collectLiveReadings) {
            val type = when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> MotionSensorType.ACCELEROMETER
                Sensor.TYPE_GYROSCOPE -> MotionSensorType.GYROSCOPE
                else -> return
            }
            val timestampMillis = event.timestamp / NANOS_PER_MILLI
            motionSampleBuffer.add(
                MotionSensorSample(
                    timestampMillis = timestampMillis,
                    type = type,
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2]
                )
            )
            if (timestampMillis - lastMotionUiUpdateMillis >= MOTION_UI_UPDATE_INTERVAL_MS) {
                _motionSensorSnapshot.value = motionSampleBuffer.snapshot()
                lastMotionUiUpdateMillis = timestampMillis
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** 亮度量程随厂商变化，统一走 [readScreenBrightnessPercent] 归一化后再展示。 */
    private fun readBrightnessPercent(): Int? = readScreenBrightnessPercent(appContext)

    // minSdk 33 仍需支持 WIFI_RSSI，无新 API 替代
    @Suppress("DEPRECATION")
    private fun readWifiRssi(): Int? = runCatching {
        wifiManager?.connectionInfo?.rssi?.takeIf { it in -126..0 }
    }.getOrNull()

    private fun readStorageSpeed(): Pair<Int?, Int?> {
        val now = SystemClock.elapsedRealtime()
        val sectors = readSectors()
        if (sectors == null || lastSectorsRead == null || lastStorageSampleAt == 0L) {
            lastSectorsRead = sectors
            lastStorageSampleAt = now
            return null to recentReadSpeeds.averageOrNull()
        }

        val elapsed = now - lastStorageSampleAt
        val lastRead = lastSectorsRead ?: return null to recentReadSpeeds.averageOrNull()
        val delta = sectors - lastRead
        lastSectorsRead = sectors
        lastStorageSampleAt = now
        if (elapsed <= 0L || delta < 0L) return null to recentReadSpeeds.averageOrNull()

        val speed = (delta.toDouble() * 512 * 1_000 / elapsed / (1024 * 1024))
            .roundToInt()
            .coerceIn(0, 10_000)
        recentReadSpeeds.addLast(speed)
        while (recentReadSpeeds.size > 5) recentReadSpeeds.removeFirst()
        return speed to recentReadSpeeds.averageOrNull()
    }

    private fun readSectors(): Long? = runCatching {
        File("/proc/diskstats").useLines { lines ->
            val stats = lines.mapNotNull { line ->
                val fields = line.trim().split(WHITESPACE_SPLIT_REGEX)
                val device = fields.getOrNull(2) ?: return@mapNotNull null
                val sectors = fields.getOrNull(5)?.toLongOrNull() ?: return@mapNotNull null
                device to sectors
            }.toList()
            val logical = stats.filter { it.first.matches(LOGICAL_DEVICE_PATTERN) }
            val physical = stats.filter { it.first.matches(PHYSICAL_DEVICE_PATTERN) }
            val selected = logical.takeIf { entries -> entries.any { it.second > 0L } } ?: physical
            selected.sumOf { it.second }
        }
    }.getOrNull()

    private fun ArrayDeque<Int>.averageOrNull(): Int? = takeIf { it.isNotEmpty() }?.average()?.roundToInt()

    private companion object {
        const val MOTION_HOLD_MS = 1_500L
        const val MOTION_UI_UPDATE_INTERVAL_MS = 100L
        const val NANOS_PER_MILLI = 1_000_000L
        val WHITESPACE_SPLIT_REGEX = Regex("\\s+")
        val LOGICAL_DEVICE_PATTERN = Regex("^dm-\\d+$")
        val PHYSICAL_DEVICE_PATTERN = Regex("^(mmcblk\\d+|nvme\\d+n\\d+|sd[a-z]+)$")
    }
}
