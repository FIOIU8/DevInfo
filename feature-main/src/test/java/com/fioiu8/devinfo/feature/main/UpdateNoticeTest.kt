/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import com.fioiu8.devinfo.core.model.UpdateState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNoticeTest {

    @Test
    fun `already-latest notice only follows a manual up-to-date check`() {
        assertTrue(shouldNotifyAlreadyLatest(UpdateState.UP_TO_DATE, userInitiated = true))
        assertFalse(shouldNotifyAlreadyLatest(UpdateState.UP_TO_DATE, userInitiated = false))
        assertFalse(shouldNotifyAlreadyLatest(UpdateState.NEW_VERSION_AVAILABLE, userInitiated = true))
        assertFalse(shouldNotifyAlreadyLatest(UpdateState.ERROR, userInitiated = true))
    }
}
