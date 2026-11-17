/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.core.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveValuesTest {

    @Test
    fun identifierKeysAreSensitive() {
        listOf(
            "device_android_id",
            "device_serial",
            "device_oaid",
            "device_aaid",
            "device_fingerprint",
        ).forEach { key ->
            assertTrue(key, SensitiveKeys.isSensitive(key))
        }
    }

    @Test
    fun ordinaryKeysAreNotSensitive() {
        assertFalse(SensitiveKeys.isSensitive("device_model"))
        assertFalse(SensitiveKeys.isSensitive("battery_level"))
    }

    @Test
    fun maskedValueHasFixedLengthRegardlessOfInput() {
        assertEquals(MASKED_VALUE, displayValue("a", "device_android_id", revealed = false))
        assertEquals(MASKED_VALUE, displayValue("x".repeat(64), "device_serial", revealed = false))
        assertEquals(MASKED_VALUE.length, "********".length)
    }

    @Test
    fun revealedValueIsReturnedAsIs() {
        assertEquals("abc123", displayValue("abc123", "device_android_id", revealed = true))
    }

    @Test
    fun nonSensitiveValueIsNeverMasked() {
        assertEquals("Pixel 9", displayValue("Pixel 9", "device_model", revealed = false))
    }
}
