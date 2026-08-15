package com.fioiu8.devinfo.core.cpu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val result = parseCpuTimes(listOf("10", "20", "30", "40", "50", "60", "70"))
        assertEquals(280L, result?.total) // 10+20+30+40+50+60+70
        assertEquals(40L, result?.idle)
    }

    @Test
    fun rejectsMalformedProcStatFields() {
        assertNull(parseCpuTimes(listOf("10", "bad", "30", "40", "50")))
        assertNull(parseCpuTimes(listOf("10", "20", "30", "40")))
    }

    @Test
    fun formatsCommonCpuFrequencyUnits() {
        assertEquals("1.80 GHz", formatCpuFrequency(1_800_000_000L))
        assertEquals("2.00 GHz", formatCpuFrequency(2_000_000_000L))
    }

    @Test
    fun formatsCpuFrequencyEdgeCases() {
        assertEquals("2 MHz", formatCpuFrequency(1_800_000L))  // 1.8M Hz -> 2 MHz (rounded)
        assertEquals("1 MHz", formatCpuFrequency(1_000_000L))  // 1M Hz -> 1 MHz
        assertEquals("1 kHz", formatCpuFrequency(1_000L))
        assertEquals("999 Hz", formatCpuFrequency(999L))
        assertEquals("0 Hz", formatCpuFrequency(0L))
    }

    @Test
    fun parsesProcUptimeValues() {
        val result = parseCpuUptime("1833.70 10432.17")
        assertNotNull(result)
        assertEquals(1833.70, result!!.totalSeconds, 0.001)
        assertEquals(10432.17, result!!.idleSeconds, 0.001)
        assertNull(parseCpuUptime("invalid"))
    }

    @Test
    fun calculatesOverallUsageFromProcUptime() {
        // totalDelta = 200 - 100 = 100
        // idleDelta = 150 - 100 = 50
        // usage = (100 - 50) / 100 * 100 = 50%
        val usage = calculateCpuUsageFromUptime(
            first = CpuUptimeTimes(100.0, 100.0),
            second = CpuUptimeTimes(200.0, 150.0)
        )
        assertEquals(50f, usage, 0.001f)
    }

    @Test
    fun calculatesCpuUsageHandlesZeroDelta() {
        val usage = calculateCpuUsageFromUptime(
            first = CpuUptimeTimes(100.0, 100.0),
            second = CpuUptimeTimes(100.0, 100.0)
        )
        assertEquals(0f, usage, 0.001f)
    }

    @Test
    fun parsesTopCpuUsage() {
        // Regex: (\d+(?:\.\d+)?)%\s*cpu
        val result = parseTopCpuUsage("65.5% cpu")
        assertEquals(65.5f, result ?: -1f, 0.001f)
        assertNull(parseTopCpuUsage("no cpu info"))
    }

    @Test
    fun parsesUsableTopCpuUsage() {
        // Regex: (\d+(?:\.\d+)?)%\s*usr
        val result = parseUsableTopCpuUsage("45.2% usr")
        assertEquals(45.2f, result ?: -1f, 0.001f)
        assertNull(parseUsableTopCpuUsage("no cpu info"))
    }

    @Test
    fun cpuTimesTotalCalculation() {
        val times = CpuTimes(
            user = 100L, nice = 50L, system = 200L,
            idle = 300L, iowait = 10L, irq = 5L, softirq = 5L
        )
        assertEquals(670L, times.total) // 100+50+200+300+10+5+5
    }

    @Test
    fun parseCpuTimesRequiresSevenFields() {
        assertNull(parseCpuTimes(listOf("1", "2", "3", "4", "5", "6")))
        assertNull(parseCpuTimes(listOf("1", "2", "3")))
    }
}
