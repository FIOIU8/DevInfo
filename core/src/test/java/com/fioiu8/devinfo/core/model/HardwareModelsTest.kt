/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HardwareModelsTest {

    @Test
    fun `existing categories select the expected starting page`() {
        assertEquals(HardwareDetailPage.DEVICE, InfoCategory.IDENTIFIERS.hardwareDetailPage())
        assertEquals(HardwareDetailPage.DISPLAY, InfoCategory.DISPLAY.hardwareDetailPage())
        assertEquals(HardwareDetailPage.SOC_STORAGE, InfoCategory.STORAGE.hardwareDetailPage())
        assertEquals(HardwareDetailPage.BATTERY_NETWORK, InfoCategory.NETWORK.hardwareDetailPage())
        assertEquals(HardwareDetailPage.MORE, InfoCategory.APP.hardwareDetailPage())
    }

    @Test
    fun `item routing preserves all original categories and splits new hardware pages`() {
        val items = InfoCategory.entries.mapIndexed { index, category ->
            DeviceInfoItem("item_$index", 0, "value", category)
        } + listOf(
            DeviceInfoItem("device_soc_model", 0, "soc", InfoCategory.DEVICE),
            DeviceInfoItem("system_sensor_count", 0, "sensor count", InfoCategory.SYSTEM),
            DeviceInfoItem("network_camera_count", 0, "camera count", InfoCategory.NETWORK)
        )
        val routed = items.groupBy { it.hardwareDetailPage() }.values.flatten()

        assertEquals(items.size, routed.size)
        assertEquals(items.toSet(), routed.toSet())
        assertEquals(HardwareDetailPage.SOC_STORAGE, items[items.size - 3].hardwareDetailPage())
        assertEquals(HardwareDetailPage.SENSORS, items[items.size - 2].hardwareDetailPage())
        assertEquals(HardwareDetailPage.CAMERA, items.last().hardwareDetailPage())
    }

    @Test
    fun `motion sample buffer retains only the configured recent window and capacity`() {
        val buffer = MotionSensorSampleBuffer(windowMillis = 1_000, maxSamples = 3)
        (0L..3L).forEach { second ->
            buffer.add(
                MotionSensorSample(
                    timestampMillis = second * 500,
                    type = if (second % 2 == 0L) MotionSensorType.GYROSCOPE else MotionSensorType.ACCELEROMETER,
                    x = second.toFloat(),
                    y = 0f,
                    z = 0f
                )
            )
        }

        val snapshot = buffer.snapshot()
        assertEquals(3, snapshot.samples.size)
        assertEquals(500L, snapshot.samples.first().timestampMillis)
        assertEquals(1_500L, snapshot.samples.last().timestampMillis)
        assertEquals(1_500L, snapshot.accelerometer?.timestampMillis)
    }
}
