package com.fioiu8.devinfo

import com.fioiu8.devinfo.ui.MonitorMode
import com.fioiu8.devinfo.ui.monitorModeFor
import com.fioiu8.devinfo.cpuSamplingIntervalMs
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorModeTest {

    @Test
    fun `background always stops monitoring`() {
        assertEquals(MonitorMode.STOPPED, monitorModeFor(isForeground = false, isOverviewVisible = true))
        assertEquals(MonitorMode.STOPPED, monitorModeFor(isForeground = false, isOverviewVisible = false))
    }

    @Test
    fun `visible overview uses active monitoring`() {
        assertEquals(MonitorMode.ACTIVE, monitorModeFor(isForeground = true, isOverviewVisible = true))
    }

    @Test
    fun `other foreground pages use low frequency monitoring`() {
        assertEquals(MonitorMode.LOW_FREQUENCY, monitorModeFor(isForeground = true, isOverviewVisible = false))
    }

    @Test
    fun `cpu sampling backs off after repeated failures`() {
        assertEquals(2_000L, cpuSamplingIntervalMs(consecutiveFailures = 0))
        assertEquals(2_000L, cpuSamplingIntervalMs(consecutiveFailures = 2))
        assertEquals(5_000L, cpuSamplingIntervalMs(consecutiveFailures = 3))
    }
}
