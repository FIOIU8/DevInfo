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
}
