/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.core.model

/**
 * CPU core metrics — index, current frequency, and usage percentage.
 */
data class CpuCoreMetric(
    val index: Int,
    val frequency: String?,
    val usagePercent: Float?
)

/**
 * Aggregated CPU usage reading across all cores.
 */
data class CpuUsageReading(
    val coreMetrics: List<CpuCoreMetric>,
    val overallUsage: Float?
)

/**
 * A timestamped CPU usage sample for chart rendering.
 */
data class CpuUsageSample(
    val timestampMillis: Long,
    val valuesByCore: Map<Int, Float>
)

/**
 * Security-related device state snapshot.
 */
data class SecuritySnapshot(
    val securityPatch: String?,
    val lockScreenEnabled: Boolean?,
    val usbDebuggingEnabled: Boolean?
)

/**
 * Hardware sensor snapshot for the overview dashboard.
 */
data class LiveHardwareSnapshot(
    val motionAvailable: Boolean = false,
    val moving: Boolean = false,
    val brightnessPercent: Int? = null,
    val storageReadSpeedMbps: Int? = null,
    val storageAverageReadSpeedMbps: Int? = null,
    val wifiRssiDbm: Int? = null
)

/**
 * Overview page snapshot — aggregated metrics for dashboard display.
 */
data class OverviewSnapshot(
    val cpuFrequency: String? = null,
    val gpuFrequency: String? = null,
    val cpuUsage: Float? = null,
    val gpuUsage: Float? = null,
    val cpuCoreMetrics: List<CpuCoreMetric> = emptyList(),
    val storagePercent: Float? = null,
    val memoryPercent: Float? = null,
    val batteryLevel: Int? = null,
    val batteryCharging: Boolean = false,
    val cpuUsageHistory: List<CpuUsageSample> = emptyList(),
    val securityPatch: String? = null,
    val lockScreenEnabled: Boolean? = null,
    val usbDebuggingEnabled: Boolean? = null,
    val hardware: LiveHardwareSnapshot = LiveHardwareSnapshot()
)
