package com.fioiu8.devinfo.feature.main

import androidx.compose.runtime.mutableStateListOf
import com.fioiu8.devinfo.core.model.InfoCategory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavKey

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
        assertEquals(MainRoute.Details(InfoCategory.NETWORK.name), navigator.currentRoute)
    }

    @Test
    fun `reselecting the current root tab is a no-op`() {
        val navigator = navigator()

        navigator.selectTab(INFO_TAB_INDEX)

        assertEquals(1, navigator.backStack.size)
        assertEquals(MainRoute.Overview, navigator.currentRoute)
    }

    @Test
    fun `invalid detail category falls back safely`() {
        val navigator = navigator()
        val route = MainRoute.Details("unknown-category")

        assertEquals(InfoCategory.DEVICE, navigator.detailsCategory(route))
    }

    @Test
    fun `routes round trip through serialization`() {
        val route = MainRoute.Details(InfoCategory.NETWORK.name)
        val json = Json

        val encoded = json.encodeToString(MainRoute.serializer(), route)
        val restored = json.decodeFromString(MainRoute.serializer(), encoded)

        assertEquals(route, restored)
    }

    @Test
    fun `root help route round trips through serialization`() {
        val json = Json

        val encoded = json.encodeToString(MainRoute.serializer(), MainRoute.RootHelp)
        val restored = json.decodeFromString(MainRoute.serializer(), encoded)

        assertEquals(MainRoute.RootHelp, restored)
    }

    private fun navigator(): MainNavigator {
        val stack = mutableStateListOf<NavKey>(MainRoute.Overview)
        return MainNavigator(NavController(stack))
    }
}
