/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import com.fioiu8.devinfo.core.model.DeviceInfoItem
import com.fioiu8.devinfo.core.model.InfoCategory
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenshotWarningTest {

    private val items = listOf(
        ItemWithVisibility(DeviceInfoItem("device_model", 0, "Pixel", InfoCategory.DEVICE)),
        ItemWithVisibility(DeviceInfoItem("device_android_id", 0, "abc", InfoCategory.IDENTIFIERS)),
        ItemWithVisibility(DeviceInfoItem("device_oaid", 0, "xyz", InfoCategory.IDENTIFIERS)),
    )

    @Test
    fun overviewListsSensitiveItems() {
        val keys = sensitiveItemsFor(MainRoute.Overview, items).map { it.key }
        assertEquals(listOf("device_android_id", "device_oaid"), keys)
    }

    @Test
    fun detailsListsSensitiveItems() {
        val keys = sensitiveItemsFor(MainRoute.Details("device"), items).map { it.key }
        assertEquals(listOf("device_android_id", "device_oaid"), keys)
    }

    @Test
    fun settingsPagesListNothing() {
        assertEquals(emptyList<String>(), sensitiveItemsFor(MainRoute.Settings, items).map { it.key })
        assertEquals(emptyList<String>(), sensitiveItemsFor(MainRoute.ThemeSettings, items).map { it.key })
        assertEquals(emptyList<String>(), sensitiveItemsFor(MainRoute.About, items).map { it.key })
        assertEquals(emptyList<String>(), sensitiveItemsFor(MainRoute.RootHelp, items).map { it.key })
    }
}
