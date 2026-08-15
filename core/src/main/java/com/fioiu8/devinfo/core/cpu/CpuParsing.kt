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
 */

/** Delay between two CPU time samples for usage calculation (milliseconds). */
const val CPU_USAGE_SAMPLE_DELAY_MS = 180L

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

/** Parse a comma-separated list of CPU indexes (e.g., "0-3,5,7-9") */
 fun parseCpuIndexes(value: String?): List<Int> {
    return value
        ?.split(',')
        ?.flatMap { part ->
            val range = part.trim().split('-')
            if (range.size == 2) {
                val start = range[0].toIntOrNull() ?: return@flatMap emptyList()
                val end = range[1].toIntOrNull() ?: return@flatMap emptyList()
                (start..end).toList()
            } else {
                listOfNotNull(range[0].trim().toIntOrNull())
            }
        }
        ?: emptyList()
}

/** Parse CPU time values from /proc/stat columns (after the "cpuN" prefix) */
 fun parseCpuTimes(parts: List<String>): CpuTimes? {
    if (parts.size < 7) return null
    return CpuTimes(
        user = parts[0].toLongOrNull() ?: return null,
        nice = parts[1].toLongOrNull() ?: return null,
        system = parts[2].toLongOrNull() ?: return null,
        idle = parts[3].toLongOrNull() ?: return null,
        iowait = parts[4].toLongOrNull() ?: 0L,
        irq = parts[5].toLongOrNull() ?: 0L,
        softirq = parts[6].toLongOrNull() ?: 0L
    )
}

/** Parse /proc/uptime values */
 fun parseCpuUptime(line: String): CpuUptimeTimes? {
    val parts = line.trim().split(Regex("\\s+"))
    if (parts.size < 2) return null
    return CpuUptimeTimes(
        totalSeconds = parts[0].toDoubleOrNull() ?: return null,
        idleSeconds = parts[1].toDoubleOrNull() ?: return null
    )
}

/** Calculate CPU usage percentage from two uptime readings */
 fun calculateCpuUsageFromUptime(
    first: CpuUptimeTimes,
    second: CpuUptimeTimes
): Float {
    val totalDelta = second.totalSeconds - first.totalSeconds
    val idleDelta = second.idleSeconds - first.idleSeconds
    if (totalDelta <= 0.0) return 0f
    return ((totalDelta - idleDelta) / totalDelta * 100.0).toFloat().coerceIn(0f, 100f)
}

/** Parse CPU usage from `top` command output */
 fun parseTopCpuUsage(output: String): Float? {
    val regex = Regex("(\\d+(?:\\.\\d+)?)%\\s*cpu", RegexOption.IGNORE_CASE)
    val match = regex.find(output) ?: return null
    return match.groupValues[1].toFloatOrNull()?.coerceIn(0f, 100f)
}

/** Parse usable CPU usage from `top` command output */
 fun parseUsableTopCpuUsage(output: String): Float? {
    val regex = Regex("(\\d+(?:\\.\\d+)?)%\\s*usr", RegexOption.IGNORE_CASE)
    val match = regex.find(output) ?: return null
    return match.groupValues[1].toFloatOrNull()?.coerceIn(0f, 100f)
}

/** Format CPU frequency from raw value (Hz) to human-readable string */
fun formatCpuFrequency(rawHz: Long): String {
    return when {
        rawHz >= 1_000_000_000 -> "%.2f GHz".format(Locale.US, rawHz / 1_000_000_000.0)
        rawHz >= 1_000_000 -> "%.0f MHz".format(Locale.US, rawHz / 1_000_000.0)
        rawHz >= 1_000 -> "%.0f kHz".format(Locale.US, rawHz / 1_000.0)
        else -> "$rawHz Hz"
    }
}
