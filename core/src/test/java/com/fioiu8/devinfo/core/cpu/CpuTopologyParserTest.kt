package com.fioiu8.devinfo.core.cpu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 覆盖 core 中实际被 data 模块调用的 CPU 解析实现。
 *
 * 这些用例对应真实设备输入：截断的 /proc/stat 行、厂商自定义的 cpufreq 量程，
 * 以及受限构建下把全部核心报告为空闲的 top 摘要。改动解析逻辑必须先让本文件失败。
 */
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
    fun toleratesWhitespaceAndDuplicatesInCpuTopology() {
        assertEquals(listOf(0, 1, 2, 3), parseCpuIndexes(" 0-1 , 1-3 "))
    }

    @Test
    fun rejectsNegativeCpuIndexRange() {
        assertEquals(emptyList<Int>(), parseCpuIndexes("-1-3"))
    }

    @Test
    fun returnsEmptyTopologyForMissingInput() {
        assertEquals(emptyList<Int>(), parseCpuIndexes(null))
        assertEquals(emptyList<Int>(), parseCpuIndexes(""))
    }

    @Test
    fun parsesAggregateProcStatFields() {
        val result = parseCpuTimes(listOf("10", "20", "30", "40", "50", "60", "70"))
        assertEquals(280L, result?.total) // 10+20+30+40+50+60+70
        assertEquals(40L, result?.idle)
    }

    @Test
    fun defaultsMissingInterruptCountersToZero() {
        // 5 列是厂商截断 /proc/stat 后的最小可用形态
        val fiveFields = requireNotNull(parseCpuTimes(listOf("10", "20", "30", "40", "50")))
        assertEquals(50L, fiveFields.iowait)
        assertEquals(0L, fiveFields.irq)
        assertEquals(0L, fiveFields.softirq)

        val sixFields = requireNotNull(parseCpuTimes(listOf("10", "20", "30", "40", "50", "60")))
        assertEquals(60L, sixFields.irq)
        assertEquals(0L, sixFields.softirq)
    }

    @Test
    fun rejectsProcStatFieldsBelowMinimumColumnCount() {
        assertNull(parseCpuTimes(listOf("10", "20", "30", "40")))
        assertNull(parseCpuTimes(emptyList()))
    }

    @Test
    fun rejectsNonNumericProcStatFields() {
        assertNull(parseCpuTimes(listOf("10", "bad", "30", "40", "50")))
    }

    @Test
    fun formatsVendorFrequencyUnitsAsMhz() {
        // cpufreq 节点通常是 kHz，kgsl/mali 节点是 Hz，两者都要归一到 MHz
        assertEquals("1805 MHz", formatCpuFrequency(1_804_800L))
        assertEquals("587 MHz", formatCpuFrequency(587_000_000L))
        assertEquals("2400 MHz", formatCpuFrequency(2_400_000_000L))
        assertEquals("1 MHz", formatCpuFrequency(1_000L))
        assertEquals("999 MHz", formatCpuFrequency(999L))
    }

    @Test
    fun rejectsNonPositiveFrequencyInsteadOfReportingZero() {
        // 不可读的节点必须返回 null，而不是看起来有效的 "0 MHz"
        assertNull(formatCpuFrequency(0L))
        assertNull(formatCpuFrequency(-1L))
    }

    @Test
    fun parsesProcUptimeValues() {
        val result = parseCpuUptime("1833.70 10432.17")
        assertNotNull(result)
        assertEquals(1833.70, requireNotNull(result).totalSeconds, 0.001)
        assertEquals(10432.17, requireNotNull(result).idleSeconds, 0.001)
    }

    @Test
    fun rejectsMalformedProcUptimeInput() {
        assertNull(parseCpuUptime("invalid"))
        assertNull(parseCpuUptime(null))
        assertNull(parseCpuUptime("-1.0 5.0"))
        assertNull(parseCpuUptime("12.0"))
    }

    @Test
    fun calculatesOverallUsageFromProcUptime() {
        // elapsed = 100s，idle 增量 = 50s；单核可用时间为 100s → 50%
        val usage = calculateCpuUsageFromUptime(
            first = CpuUptimeTimes(100.0, 100.0),
            second = CpuUptimeTimes(200.0, 150.0),
            cpuCount = 1
        )
        assertEquals(50f, requireNotNull(usage), 0.001f)
    }

    @Test
    fun scalesUptimeUsageByCpuCount() {
        // 八核：可用时间 800s，其中 idle 50s → (800-50)/800 = 93.75%
        val usage = calculateCpuUsageFromUptime(
            first = CpuUptimeTimes(100.0, 100.0),
            second = CpuUptimeTimes(200.0, 150.0),
            cpuCount = 8
        )
        assertEquals(93.75f, requireNotNull(usage), 0.001f)
    }

    @Test
    fun rejectsUptimeUsageWithoutElapsedTimeOrCoreCount() {
        // 采样间隔为 0 或读数回退时不能给出 "0%" 这种看似有效的值
        assertNull(
            calculateCpuUsageFromUptime(
                first = CpuUptimeTimes(100.0, 100.0),
                second = CpuUptimeTimes(100.0, 100.0),
                cpuCount = 1
            )
        )
        assertNull(
            calculateCpuUsageFromUptime(
                first = CpuUptimeTimes(100.0, 100.0),
                second = CpuUptimeTimes(200.0, 150.0),
                cpuCount = 0
            )
        )
    }

    @Test
    fun clampsUptimeUsageWhenIdleExceedsAvailableTime() {
        val usage = calculateCpuUsageFromUptime(
            first = CpuUptimeTimes(0.0, 0.0),
            second = CpuUptimeTimes(10.0, 1000.0),
            cpuCount = 1
        )
        assertEquals(0f, requireNotNull(usage), 0.001f)
    }

    @Test
    fun parsesTopSummaryLineFromRealDeviceOutput() {
        // 取自 `top -b -n 1 -m 1` 的摘要行，总占用为 400%（4 核 × 100%）
        val line = "400%cpu  10%user   5%nice  20%sys  60%idle   0%iow   5%irq   0%sirq  0%host"
        assertEquals(85f, requireNotNull(parseTopCpuUsage(line)), 0.001f)
    }

    @Test
    fun reportsZeroForRestrictedTopSummary() {
        // Android 15 受限构建会把全部核心报告为空闲；解析结果是 0%，
        // 调用方（DeviceInfoCollector.readCpuUsageFromTop）据此丢弃该读数。
        val line = "400%cpu   0%user   0%nice   0%sys 400%idle   0%iow   0%irq   0%sirq  0%host"
        assertEquals(0f, requireNotNull(parseTopCpuUsage(line)), 0.001f)
    }

    @Test
    fun rejectsTopLinesWithoutUsableCounters() {
        // 只有 %cpu 或只有 %idle 都不足以判断真实占用
        assertNull(parseTopCpuUsage("no cpu info"))
        assertNull(parseTopCpuUsage("45.2% usr"))
        assertNull(parseTopCpuUsage("200%cpu  10%user"))
        assertNull(parseTopCpuUsage("100%cpu  20%sys 200%idle"))
    }

    @Test
    fun cpuTimesTotalCalculation() {
        val times = CpuTimes(
            user = 100L, nice = 50L, system = 200L,
            idle = 300L, iowait = 10L, irq = 5L, softirq = 5L
        )
        assertEquals(670L, times.total) // 100+50+200+300+10+5+5
    }
}
