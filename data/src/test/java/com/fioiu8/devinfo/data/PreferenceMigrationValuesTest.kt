package com.fioiu8.devinfo.data

import com.fioiu8.devinfo.data.readLegacyValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreferenceMigrationValuesTest {

    @Test
    fun `legacy update values accept their persisted types`() {
        val values = mapOf<String, Any?>(
            "last_check_time" to "42",
            "cached_tag" to "release-1",
        )

        assertEquals(42L, readLegacyValue(values, "last_check_time"))
        assertEquals("release-1", readLegacyValue(values, "cached_tag"))
    }

    @Test
    fun `legacy migration ignores malformed values`() {
        val values = mapOf<String, Any?>(
            "last_check_time" to "invalid",
            "cached_tag" to 42,
        )

        assertNull(readLegacyValue(values, "last_check_time"))
        assertNull(readLegacyValue(values, "cached_tag"))
        assertNull(readLegacyValue(values, "unknown"))
    }
}
