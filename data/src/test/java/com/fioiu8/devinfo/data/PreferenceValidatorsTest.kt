package com.fioiu8.devinfo.data

import com.fioiu8.devinfo.data.PreferenceValidators
import com.fioiu8.devinfo.data.enumValueOrDefault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceValidatorsTest {

    private enum class Mode {
        DEFAULT,
        ACTIVE,
    }

    @Test
    fun `unknown enum values use the default`() {
        assertEquals(Mode.DEFAULT, enumValueOrDefault(null, Mode.DEFAULT))
        assertEquals(Mode.DEFAULT, enumValueOrDefault("missing", Mode.DEFAULT))
        assertEquals(Mode.ACTIVE, enumValueOrDefault("ACTIVE", Mode.DEFAULT))
    }

    @Test
    fun `page scale rejects non finite and out of range values`() {
        assertEquals(PreferenceValidators.DEFAULT_PAGE_SCALE, PreferenceValidators.pageScaleOrDefault(Float.NaN))
        assertEquals(PreferenceValidators.DEFAULT_PAGE_SCALE, PreferenceValidators.pageScaleOrDefault(0.1f))
        assertEquals(PreferenceValidators.MIN_PAGE_SCALE, PreferenceValidators.pageScaleOrDefault(0.8f))
        assertEquals(PreferenceValidators.MAX_PAGE_SCALE, PreferenceValidators.pageScaleOrDefault(1.1f))
    }

    @Test
    fun `locale tags are parsed and normalized before storage`() {
        assertEquals("en-US", PreferenceValidators.normalizedLocaleTag(" en-us "))
        assertTrue(PreferenceValidators.isValidLocaleTag("ja"))
        assertTrue(PreferenceValidators.isValidLocaleTag("zh-CN"))
        assertFalse(PreferenceValidators.isValidLocaleTag(""))
        assertFalse(PreferenceValidators.isValidLocaleTag("not a locale"))
        assertFalse(PreferenceValidators.isValidLocaleTag("en_US"))
        assertFalse(PreferenceValidators.isValidLocaleTag("ko"))
    }

    @Test
    fun `invalid update timestamps mean unchecked`() {
        assertEquals(0L, PreferenceValidators.validLastCheckTimeOrZero(-1L, 1_000L))
        assertEquals(0L, PreferenceValidators.validLastCheckTimeOrZero(1_001L, 1_000L))
        assertEquals(500L, PreferenceValidators.validLastCheckTimeOrZero(500L, 1_000L))
    }
}
