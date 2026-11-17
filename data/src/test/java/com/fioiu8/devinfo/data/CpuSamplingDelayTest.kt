/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuSamplingDelayTest {

    @Test
    fun `delay subtracts the time already spent reading`() {
        assertEquals(2_000L, cpuSamplingDelayMs(elapsedMs = 0L, consecutiveFailures = 0))
        assertEquals(1_500L, cpuSamplingDelayMs(elapsedMs = 500L, consecutiveFailures = 0))
        assertEquals(0L, cpuSamplingDelayMs(elapsedMs = 2_500L, consecutiveFailures = 0))
    }

    @Test
    fun `repeated failures use the longer backoff`() {
        assertEquals(5_000L, cpuSamplingDelayMs(elapsedMs = 0L, consecutiveFailures = 3))
        assertEquals(4_000L, cpuSamplingDelayMs(elapsedMs = 1_000L, consecutiveFailures = 4))
    }
}
