/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import com.fioiu8.devinfo.core.model.CpuCoreMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuFrequencySummaryTest {

    private fun core(index: Int, frequency: String?) =
        CpuCoreMetric(index = index, frequency = frequency, usagePercent = null)

    @Test
    fun contiguousSameFrequencyIsMerged() {
        val ranges = coreFrequencyRanges(
            listOf(core(0, "1901 MHz"), core(1, "1901 MHz"), core(2, "1901 MHz"))
        )
        assertEquals(listOf(CpuFrequencyRange(0, 2, "1901 MHz")), ranges)
    }

    @Test
    fun differentFrequencyStartsNewRange() {
        val ranges = coreFrequencyRanges(
            listOf(core(0, "1901 MHz"), core(1, "2611 MHz"), core(2, "2611 MHz"))
        )
        assertEquals(
            listOf(
                CpuFrequencyRange(0, 0, "1901 MHz"),
                CpuFrequencyRange(1, 2, "2611 MHz"),
            ),
            ranges,
        )
    }

    @Test
    fun indexGapStartsNewRange() {
        val ranges = coreFrequencyRanges(
            listOf(core(0, "1901 MHz"), core(2, "1901 MHz"))
        )
        assertEquals(
            listOf(
                CpuFrequencyRange(0, 0, "1901 MHz"),
                CpuFrequencyRange(2, 2, "1901 MHz"),
            ),
            ranges,
        )
    }

    @Test
    fun coresWithoutFrequencyAreSkipped() {
        val ranges = coreFrequencyRanges(
            listOf(core(0, null), core(1, "2611 MHz"), core(2, null))
        )
        assertEquals(listOf(CpuFrequencyRange(1, 1, "2611 MHz")), ranges)
    }

    @Test
    fun emptyInputProducesNoRows() {
        assertTrue(coreFrequencyRanges(emptyList()).isEmpty())
    }
}
