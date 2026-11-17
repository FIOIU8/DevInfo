/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionLevelTest {

    @Test
    fun `system animations disabled forces off`() {
        MotionLevel.entries.forEach { level ->
            assertEquals(MotionLevel.OFF, effectiveMotionLevel(level, systemAnimationsEnabled = false))
        }
    }

    @Test
    fun `user level is kept when system animations are enabled`() {
        MotionLevel.entries.forEach { level ->
            assertEquals(level, effectiveMotionLevel(level, systemAnimationsEnabled = true))
        }
    }

    @Test
    fun `only off is not animated`() {
        assertTrue(MotionLevel.FULL.isAnimated)
        assertTrue(MotionLevel.STANDARD.isAnimated)
        assertTrue(MotionLevel.MINIMAL.isAnimated)
        assertFalse(MotionLevel.OFF.isAnimated)
    }
}
