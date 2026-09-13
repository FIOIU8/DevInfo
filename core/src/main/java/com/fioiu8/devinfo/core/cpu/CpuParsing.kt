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

package com.fioiu8.devinfo.core.cpu

import java.util.Locale

/**
 * Pure CPU parsing utilities — no Android dependencies.
 *
 * This file is the single source of truth for the /proc, /sys and `top` parsing used by
 * the data module. The parsers tolerate the shapes real devices return, including
 * truncated field lists and vendor-specific frequency units.
 */

/** Delay between two CPU time samples for usage calculation (milliseconds). */
const val CPU_USAGE_SAMPLE_DELAY_MS = 180L

// 解析函数位于每 2 秒执行一次的采样热路径，正则提为顶层常量避免重复编译
private val WHITESPACE_SPLIT_REGEX = Regex("\\s+")

/**
 * `top -b` 的摘要行同时给出总占用与 idle 占用。只匹配 %cpu 无法判断该构建是否因
 * 系统限制把全部核心都报告为空闲，因此两个字段都要捕获。
 */
private val TOP_CPU_SUMMARY_REGEX = Regex(
    """^\s*(\d+(?:\.\d+)?)%cpu\b.*?(\d+(?:\.\d+)?)%idle\b"""
)

/** Represents parsed CPU time values from /proc/stat */
data class CpuTimes(
    val user: Long,
    val nice: Long,
    val system: Long,
    val idle: Long,
    val iowait: Long,
    val irq: Long,
    val softirq: Long
) {
    val total: Long get() = user + nice + system + idle + iowait + irq + softirq
}

/** Represents parsed uptime values from /proc/uptime */
data class CpuUptimeTimes(
    val totalSeconds: Double,
    val idleSeconds: Double
)

/**
 * Parse a comma-separated list of CPU indexes (e.g., "0-3,5,7-9").
 *
 * Invalid ranges ("4-1"), negative values and unparsable parts are dropped instead of
 * discarding the whole list, so a partially readable /sys node still yields a topology.
 */
fun parseCpuIndexes(value: String?): List<Int> {
    return value
        ?.split(',')
        ?.flatMap { part ->
            val bounds = part.trim().split('-', limit = 2).map(String::trim)
            when (bounds.size) {
                1 -> bounds.single().toIntOrNull()?.let(::listOf).orEmpty()
                2 -> {
                    val start = bounds[0].toIntOrNull()
                    val end = bounds[1].toIntOrNull()
                    if (start == null || end == null || start < 0 || end < start) {
                        emptyList()
                    } else {
                        (start..end).toList()
                    }
                }
                else -> emptyList()
            }
        }
        ?.distinct()
        ?.sorted()
        .orEmpty()
}

/**
 * Parse CPU time values from /proc/stat columns (after the "cpuN" prefix).
 *
 * Vendors can truncate the line, so only user/nice/system/idle are mandatory; the
 * interrupt counters fall back to 0 when absent. Returns null when a present field is
 * not numeric, so a malformed read is not mistaken for a valid sample.
 */
fun parseCpuTimes(fields: List<String>): CpuTimes? {
    if (fields.size < 5) return null
    val values = fields.map { it.toLongOrNull() ?: return null }
    return CpuTimes(
        user = values[0],
        nice = values[1],
        system = values[2],
        idle = values[3],
        iowait = values.getOrElse(4) { 0L },
        irq = values.getOrElse(5) { 0L },
        softirq = values.getOrElse(6) { 0L }
    )
}

/**
 * Parse /proc/uptime values.
 *
 * @param line raw first line of /proc/uptime, or null when the read failed.
 */
fun parseCpuUptime(line: String?): CpuUptimeTimes? {
    val fields = line?.trim()?.split(WHITESPACE_SPLIT_REGEX) ?: return null
    if (fields.size < 2) return null
    val uptime = fields[0].toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return null
    val idle = fields[1].toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return null
    return CpuUptimeTimes(totalSeconds = uptime, idleSeconds = idle)
}

/**
 * Calculate overall CPU usage from two /proc/uptime readings.
 *
 * /proc/uptime reports wall-clock totals while its idle counter is summed over all
 * cores, so the available CPU time is `elapsed * cpuCount`. Restricted ROMs that deny
 * /proc/stat keep this fallback meaningful.
 *
 * @param cpuCount number of online cores; a non-positive value yields null.
 */
fun calculateCpuUsageFromUptime(
    first: CpuUptimeTimes,
    second: CpuUptimeTimes,
    cpuCount: Int
): Float? {
    if (cpuCount <= 0) return null
    val elapsed = second.totalSeconds - first.totalSeconds
    val idle = second.idleSeconds - first.idleSeconds
    if (elapsed <= 0.0 || idle < 0.0) return null
    val available = elapsed * cpuCount
    return ((available - idle) / available * 100.0).toFloat().coerceIn(0f, 100f)
}

/**
 * Parse CPU usage from a `top -b -n 1` summary line.
 *
 * Returns null when the counters are missing or when idle exceeds total, which is what
 * restricted builds produce instead of a real reading.
 */
fun parseTopCpuUsage(line: String): Float? {
    val match = TOP_CPU_SUMMARY_REGEX.find(line) ?: return null
    val total = match.groupValues[1].toFloatOrNull() ?: return null
    val idle = match.groupValues[2].toFloatOrNull() ?: return null
    if (total <= 0f || idle < 0f || idle > total) return null
    return ((total - idle) / total * 100f).coerceIn(0f, 100f)
}

/**
 * Format a raw cpufreq/kgsl node value as MHz.
 *
 * Node units differ per vendor (kHz for cpufreq, Hz for kgsl), so the value is
 * normalized by magnitude. Non-positive input returns null rather than "0 MHz", because
 * an unreadable node must not look like a valid reading.
 */
fun formatCpuFrequency(raw: Long): String? {
    val mhz = when {
        raw >= 100_000_000L -> raw / 1_000_000f
        raw >= 1_000L -> raw / 1_000f
        else -> raw.toFloat()
    }
    return if (mhz > 0f) "%.0f MHz".format(Locale.US, mhz) else null
}
