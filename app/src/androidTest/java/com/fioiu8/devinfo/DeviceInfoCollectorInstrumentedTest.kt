/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fioiu8.devinfo.data.DeviceInfoCollector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the visible-app field is collected through the platform package manager. */
@RunWith(AndroidJUnit4::class)
class DeviceInfoCollectorInstrumentedTest {

    @Test
    fun reportsVisibleInstalledAppCount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val collector = DeviceInfoCollector(context)
        val items = runBlocking { collector.collectDeviceInfo() }
        val count = items.singleOrNull { it.key == "app_installed_count" }?.value?.toIntOrNull()

        // The app can always see its own package.
        assertTrue("Expected a positive visible-app count, got $count", count != null && count > 0)
    }
}
