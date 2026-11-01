/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fioiu8.devinfo.core.model

/** Hardware detail pages shown by the horizontally paged details screen. */
enum class HardwareDetailPage {
    DEVICE,
    DISPLAY,
    SOC_STORAGE,
    BATTERY_NETWORK,
    CAMERA,
    SENSORS,
    MORE
}

/** Maps the existing category entry to the page that contains its information. */
fun InfoCategory.hardwareDetailPage(): HardwareDetailPage = when (this) {
    InfoCategory.DEVICE, InfoCategory.IDENTIFIERS -> HardwareDetailPage.DEVICE
    InfoCategory.DISPLAY -> HardwareDetailPage.DISPLAY
    InfoCategory.SYSTEM, InfoCategory.STORAGE -> HardwareDetailPage.SOC_STORAGE
    InfoCategory.BATTERY, InfoCategory.NETWORK -> HardwareDetailPage.BATTERY_NETWORK
    InfoCategory.LOCALE, InfoCategory.APP -> HardwareDetailPage.MORE
}

/** Maps each collected item exactly once, splitting sensor and camera inventory into their pages. */
fun DeviceInfoItem.hardwareDetailPage(): HardwareDetailPage = when {
    key == "device_soc_manufacturer" || key == "device_soc_model" -> HardwareDetailPage.SOC_STORAGE
    key == "system_sensor_count" -> HardwareDetailPage.SENSORS
    key == "network_camera_count" -> HardwareDetailPage.CAMERA
    category == InfoCategory.DEVICE || category == InfoCategory.IDENTIFIERS -> HardwareDetailPage.DEVICE
    category == InfoCategory.DISPLAY -> HardwareDetailPage.DISPLAY
    category == InfoCategory.STORAGE || key.startsWith("system_cpu_") -> HardwareDetailPage.SOC_STORAGE
    category == InfoCategory.BATTERY -> HardwareDetailPage.BATTERY_NETWORK
    category == InfoCategory.NETWORK -> HardwareDetailPage.BATTERY_NETWORK
    else -> HardwareDetailPage.MORE
}

/** Camera characteristics that Android exposes without opening the camera. */
data class CameraHardwareInfo(
    val id: String,
    val characteristicsAvailable: Boolean,
    val lensFacing: Int?,
    val focalLengthsMm: List<Float>,
    val physicalWidthMm: Float?,
    val physicalHeightMm: Float?,
    val pixelWidth: Int?,
    val pixelHeight: Int?,
    val sensorOrientationDegrees: Int?
)

/** Static fields for one system sensor. */
data class SensorHardwareInfo(
    val name: String,
    val vendor: String,
    val type: Int,
    val typeName: String,
    val maxRange: Float?,
    val resolution: Float?,
    val powerMilliAmps: Float?,
    val minDelayMicros: Int?
)

/** Static hardware information for the camera and sensor detail pages. */
data class HardwareDetailsSnapshot(
    val cameras: List<CameraHardwareInfo> = emptyList(),
    val sensors: List<SensorHardwareInfo> = emptyList(),
    val cameraAccessAvailable: Boolean = true,
    val sensorAccessAvailable: Boolean = true
)

enum class MotionSensorType {
    ACCELEROMETER,
    GYROSCOPE
}

/** One timestamped three-axis sensor reading. */
data class MotionSensorSample(
    val timestampMillis: Long,
    val type: MotionSensorType,
    val x: Float,
    val y: Float,
    val z: Float
)

/** Latest motion readings and a bounded history for the live sensor page. */
data class MotionSensorSnapshot(
    val accelerometer: MotionSensorSample? = null,
    val gyroscope: MotionSensorSample? = null,
    val samples: List<MotionSensorSample> = emptyList()
)

/** Keeps only recent motion samples and imposes a hard capacity limit. */
class MotionSensorSampleBuffer(
    private val windowMillis: Long = WINDOW_MILLIS,
    private val maxSamples: Int = MAX_SAMPLES
) {
    private val samples = ArrayDeque<MotionSensorSample>()

    init {
        require(windowMillis > 0)
        require(maxSamples > 0)
    }

    fun add(sample: MotionSensorSample) {
        samples.addLast(sample)
        val oldestAllowed = sample.timestampMillis - windowMillis
        while (samples.isNotEmpty() && samples.first().timestampMillis < oldestAllowed) {
            samples.removeFirst()
        }
        while (samples.size > maxSamples) samples.removeFirst()
    }

    fun snapshot(): MotionSensorSnapshot {
        val values = samples.toList()
        return MotionSensorSnapshot(
            accelerometer = values.lastOrNull { it.type == MotionSensorType.ACCELEROMETER },
            gyroscope = values.lastOrNull { it.type == MotionSensorType.GYROSCOPE },
            samples = values
        )
    }

    fun clear() = samples.clear()

    private companion object {
        const val WINDOW_MILLIS = 10_000L
        const val MAX_SAMPLES = 1_200
    }
}
