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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.provider.Settings
import com.fioiu8.devinfo.core.model.LiveHardwareSnapshot
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
    private val registeredMotionSensors = mutableSetOf<Sensor>()
    private var motionRegistrationAttempted = false
    private var lastSectorsRead: Long? = null
    private var lastStorageSampleAt = 0L
    private val recentReadSpeeds = ArrayDeque<Int>()

    @Synchronized
    fun start(collectMotion: Boolean) {
        if (!collectMotion || motionRegistrationAttempted) return
        motionRegistrationAttempted = true
        motionSensors.forEach { sensor ->
            val registered = runCatching {
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL) == true
            }.getOrDefault(false)
            if (registered) registeredMotionSensors += sensor
        }
    }

    @Synchronized
    fun stop() {
        sensorManager?.unregisterListener(this)
        registeredMotionSensors.clear()
        motionRegistrationAttempted = false
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

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        val magnitude = sqrt(
            event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
        )
        val motionThreshold = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) 1.25f else 0.35f
        if ((event.sensor.type == Sensor.TYPE_ACCELEROMETER && kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH) > motionThreshold) ||
            (event.sensor.type == Sensor.TYPE_GYROSCOPE && magnitude > motionThreshold)
        ) {
            movingUntil = SystemClock.elapsedRealtime() + MOTION_HOLD_MS
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun readBrightnessPercent(): Int? = runCatching {
        val raw = Settings.System.getInt(
            appContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            -1
        )
        raw.takeIf { it >= 0 }?.let { (it / 255f * 100f).roundToInt().coerceIn(0, 100) }
    }.getOrNull()

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
        val WHITESPACE_SPLIT_REGEX = Regex("\\s+")
        val LOGICAL_DEVICE_PATTERN = Regex("^dm-\\d+$")
        val PHYSICAL_DEVICE_PATTERN = Regex("^(mmcblk\\d+|nvme\\d+n\\d+|sd[a-z]+)$")
    }
}
