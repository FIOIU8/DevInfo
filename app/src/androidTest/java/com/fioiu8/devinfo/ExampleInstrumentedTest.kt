package com.fioiu8.devinfo

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.fioiu8.devinfo.ui.CpuUsageSample
import com.fioiu8.devinfo.ui.DeviceInfoOverviewPage
import com.fioiu8.devinfo.ui.OverviewSnapshot
import com.fioiu8.devinfo.ui.theme.DevInfoTheme
import com.fioiu8.devinfo.model.ThemeMode
import org.junit.Rule

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.fioiu8.devinfo", appContext.packageName)
    }

    @Test
    fun liveActivity_displaysEveryCoreOnNarrowScreen() {
        val coreMetrics = (0..15).map { index ->
            CpuCoreMetric(index = index, frequency = "1800 MHz", usagePercent = 80f)
        }
        val history = listOf(
            CpuUsageSample(
                timestampMillis = 1L,
                valuesByCore = coreMetrics.associate { it.index to (it.usagePercent ?: 0f) }
            )
        )

        composeTestRule.setContent {
            DevInfoTheme(themeMode = ThemeMode.LIGHT) {
                DeviceInfoOverviewPage(
                    itemsState = emptyList(),
                    isLoading = false,
                    isOverviewLoading = false,
                    snapshot = OverviewSnapshot(
                        cpuUsage = 80f,
                        cpuCoreMetrics = coreMetrics,
                        cpuUsageHistory = history
                    ),
                    onRefresh = {},
                    onOpenDetails = {}
                )
            }
        }

        composeTestRule.onNodeWithText("C0 80%").assertIsDisplayed()
        val lastCore = composeTestRule.onNodeWithText("C15 80%")
        lastCore.assertIsDisplayed()

        val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
        val lastCoreBounds = lastCore.fetchSemanticsNode().boundsInRoot
        assertTrue(lastCoreBounds.right <= rootBounds.right)
    }
}
