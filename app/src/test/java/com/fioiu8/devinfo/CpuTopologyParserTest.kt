package com.fioiu8.devinfo

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuTopologyParserTest {

    @Test
    fun `parses a contiguous CPU topology`() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), parseCpuIndexes("0-7"))
    }

    @Test
    fun `parses sparse CPU ranges`() {
        assertEquals(listOf(0, 1, 2, 4, 6, 7), parseCpuIndexes("0-2,4,6-7"))
    }

    @Test
    fun `ignores malformed CPU ranges`() {
        assertEquals(listOf(0, 1, 2), parseCpuIndexes("0-2,4-1,invalid"))
    }

    @Test
    fun `parses aggregate proc stat fields without shifting positions`() {
        assertEquals(
            DeviceInfoCollector.CpuTimes(total = 150L, idle = 90L),
            parseCpuTimes(listOf("10", "20", "30", "40", "50"))
        )
    }

    @Test
    fun `rejects malformed proc stat fields`() {
        assertEquals(null, parseCpuTimes(listOf("10", "bad", "30", "40", "50")))
        assertEquals(null, parseCpuTimes(listOf("10", "20", "30", "40")))
    }

    @Test
    fun `formats common cpu frequency units`() {
        assertEquals("1800 MHz", formatCpuFrequency(1_800_000L))
        assertEquals("1800 MHz", formatCpuFrequency(1_800_000_000L))
        assertEquals(null, formatCpuFrequency(0L))
    }

    @Test
    fun `parses proc uptime values`() {
        assertEquals(CpuUptimeTimes(1833.70, 10432.17), parseCpuUptime("1833.70 10432.17"))
        assertEquals(null, parseCpuUptime("invalid"))
    }

    @Test
    fun `calculates overall usage from proc uptime`() {
        val usage = calculateCpuUsageFromUptime(
            first = CpuUptimeTimes(100.0, 500.0),
            second = CpuUptimeTimes(101.0, 504.5),
            cpuCount = 6
        )
        assertEquals(25f, usage ?: -1f, 0.001f)
    }
}
