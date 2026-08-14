package com.fioiu8.devinfo.core.cpu

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuTopologyParserTest {

    @Test
    fun parsesContiguousCpuTopology() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), parseCpuIndexes("0-7"))
    }

    @Test
    fun parsesSparseCpuRanges() {
        assertEquals(listOf(0, 1, 2, 4, 6, 7), parseCpuIndexes("0-2,4,6-7"))
    }

    @Test
    fun rejectsMalformedCpuTopology() {
        assertEquals(listOf(0, 1, 2), parseCpuIndexes("0-2,4-1,invalid"))
    }

    @Test
    fun parsesAggregateProcStatFields() {
        val result = parseCpuTimes(listOf("10", "20", "30", "40", "50"))
        assertEquals(150L, result?.total)
        assertEquals(90L, result?.idle)
    }

    @Test
    fun rejectsMalformedProcStatFields() {
        assertEquals(null, parseCpuTimes(listOf("10", "bad", "30", "40", "50")))
        assertEquals(null, parseCpuTimes(listOf("10", "20", "30", "40")))
    }

    @Test
    fun formatsCommonCpuFrequencyUnits() {
        assertEquals("1800 MHz", formatCpuFrequency(1_800_000L))
        assertEquals("2.00 GHz", formatCpuFrequency(2_000_000_000L))
        assertEquals(null, formatCpuFrequency(0L))
    }

    @Test
    fun parsesProcUptimeValues() {
        val result = parseCpuUptime("1833.70 10432.17")
        assertEquals(1833L, result?.totalSeconds)
        assertEquals(10432L, result?.idleSeconds)
        assertEquals(null, parseCpuUptime("invalid"))
    }

    @Test
    fun calculatesOverallUsageFromProcUptime() {
        val usage = calculateCpuUsageFromUptime(
            first = CpuUptimeTimes(100L, 500L),
            second = CpuUptimeTimes(101L, 504L)
        )
        assertEquals(25f, usage ?: -1f, 0.001f)
    }

    @Test
    fun parsesTopCpuUsage() {
        assertEquals(65.5f, parseTopCpuUsage("CPU: 65.5%us") ?: -1f, 0.001f)
        assertEquals(null, parseTopCpuUsage("no cpu info"))
    }

    @Test
    fun parsesUsableTopCpuUsage() {
        assertEquals(45.2f, parseUsableTopCpuUsage("USR: 45.2%") ?: -1f, 0.001f)
        assertEquals(null, parseUsableTopCpuUsage("no cpu info"))
    }
}
