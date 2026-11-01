package com.fioiu8.devinfo.feature.main

import androidx.compose.runtime.mutableStateListOf
import com.fioiu8.devinfo.core.model.HardwareDetailPage
import com.fioiu8.devinfo.core.model.InfoCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavigatorTest {

    @Test
    fun `selecting a root tab clears child routes`() {
        val navigator = navigator()

        navigator.openDetails(InfoCategory.BATTERY)
        navigator.selectTab(SETTINGS_TAB_INDEX)

        assertEquals(MainRoute.Settings, navigator.currentRoute)
        assertEquals(1, navigator.backStack.size)
        assertEquals(SETTINGS_TAB_INDEX, navigator.selectedTabIndex)
    }

    @Test
    fun `settings children push once and pop in reverse order`() {
        val navigator = navigator()

        navigator.openThemeSettings()
        navigator.openThemeSettings()
        navigator.pop()
        navigator.openAbout()

        assertEquals(MainRoute.About, navigator.currentRoute)
        assertEquals(2, navigator.backStack.size)
        assertTrue(navigator.pop())
        assertFalse(navigator.pop())
        assertEquals(MainRoute.Settings, navigator.currentRoute)
    }

    @Test
    fun `root help pushes once, keeps the settings tab and pops back`() {
        val navigator = navigator()

        navigator.openRootHelp()
        navigator.openRootHelp()

        assertEquals(MainRoute.RootHelp, navigator.currentRoute)
        assertEquals(2, navigator.backStack.size)
        assertEquals(SETTINGS_TAB_INDEX, navigator.selectedTabIndex)
        assertTrue(navigator.pop())
        assertFalse(navigator.pop())
        assertEquals(MainRoute.Settings, navigator.currentRoute)
    }

    @Test
    fun `repeated details clicks replace instead of duplicating`() {
        val navigator = navigator()

        navigator.openDetails(InfoCategory.BATTERY)
        navigator.openDetails(InfoCategory.BATTERY)
        navigator.openDetails(InfoCategory.NETWORK)

        assertEquals(2, navigator.backStack.size)
        assertEquals(MainRoute.Details("BATTERY_NETWORK"), navigator.currentRoute)
    }

    @Test
    fun `settings action pushes settings and back returns to the previous page`() {
        val navigator = navigator()
        navigator.openDetails(InfoCategory.DISPLAY)

        navigator.openSettings()

        assertEquals(MainRoute.Settings, navigator.currentRoute)
        assertTrue(navigator.pop())
        assertEquals(MainRoute.Details("DISPLAY"), navigator.currentRoute)
    }

    @Test
    fun `reselecting the current root tab is a no-op`() {
        val navigator = navigator()

        navigator.selectTab(INFO_TAB_INDEX)

        assertEquals(1, navigator.backStack.size)
        assertEquals(MainRoute.Overview, navigator.currentRoute)
    }

    @Test
    fun `invalid detail page falls back safely`() {
        val navigator = navigator()
        val route = MainRoute.Details("unknown-category")

        assertEquals(HardwareDetailPage.DEVICE, navigator.detailsPage(route))
    }

    @Test
    fun `routes round trip through save keys`() {
        val routes = listOf(
            MainRoute.Overview,
            MainRoute.Details("BATTERY_NETWORK"),
            MainRoute.Settings,
            MainRoute.ThemeSettings,
            MainRoute.About,
            MainRoute.RootHelp,
        )

        val restored = routes.map(MainRoute::toSavedKey).mapNotNull(::mainRouteFromSavedKey)

        assertEquals(routes, restored)
    }

    private fun navigator(): MainNavigator =
        MainNavigator(mutableStateListOf(MainRoute.Overview))
}
