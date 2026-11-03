/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import com.fioiu8.devinfo.core.model.HardwareDetailPage
import com.fioiu8.devinfo.core.model.InfoCategory
import com.fioiu8.devinfo.core.model.hardwareDetailPage

internal const val INFO_TAB_INDEX = 0
internal const val SETTINGS_TAB_INDEX = 1

/** Routes rendered by the main page host. */
internal sealed interface MainRoute {
    companion object

    data object Overview : MainRoute

    data object Settings : MainRoute

    data class Details(val categoryName: String) : MainRoute

    data object ThemeSettings : MainRoute

    data object About : MainRoute

    data object RootHelp : MainRoute
}

internal val MainRouteStackSaver = listSaver<SnapshotStateList<MainRoute>, String>(
    save = { routes -> routes.map(MainRoute::toSavedKey) },
    restore = { savedRoutes ->
        val routes = savedRoutes.mapNotNull(::mainRouteFromSavedKey).ifEmpty { listOf(MainRoute.Overview) }
        mutableStateListOf(*routes.toTypedArray())
    },
)

internal fun MainRoute.toSavedKey(): String = when (this) {
    MainRoute.Overview -> "overview"
    MainRoute.Settings -> "settings"
    is MainRoute.Details -> "details:$categoryName"
    MainRoute.ThemeSettings -> "theme-settings"
    MainRoute.About -> "about"
    MainRoute.RootHelp -> "root-help"
}

internal fun mainRouteFromSavedKey(key: String): MainRoute? = when {
    key == "overview" -> MainRoute.Overview
    key == "settings" -> MainRoute.Settings
    key.startsWith("details:") -> MainRoute.Details(key.removePrefix("details:"))
    key == "theme-settings" -> MainRoute.ThemeSettings
    key == "about" -> MainRoute.About
    key == "root-help" -> MainRoute.RootHelp
    else -> null
}

/** Owns route mutations so every navigation path has the same stack semantics. */
internal class MainNavigator(
    val backStack: SnapshotStateList<MainRoute>,
) {
    val currentRoute: MainRoute
        get() = backStack.lastOrNull() ?: MainRoute.Overview

    /** Returns the selected root tab even while a child route is visible. */
    val selectedTabIndex: Int
        get() = backStack.firstOrNull()?.rootTabIndex() ?: INFO_TAB_INDEX

    val isOverviewVisible: Boolean
        get() = currentRoute == MainRoute.Overview

    fun selectTab(index: Int) {
        val target = if (index == SETTINGS_TAB_INDEX) MainRoute.Settings else MainRoute.Overview
        backStack.clear()
        backStack.add(target)
    }

    fun openSettings() {
        if (currentRoute != MainRoute.Settings) backStack.add(MainRoute.Settings)
    }

    fun openDetails(category: InfoCategory) {
        val target = MainRoute.Details(category.hardwareDetailPage().name)
        if (currentRoute is MainRoute.Details) {
            backStack[backStack.lastIndex] = target
        } else {
            backStack.add(target)
        }
    }

    fun openThemeSettings() {
        if (currentRoute != MainRoute.ThemeSettings) {
            ensureSettingsRoot()
            backStack.add(MainRoute.ThemeSettings)
        }
    }

    fun openAbout() {
        if (currentRoute != MainRoute.About) {
            ensureSettingsRoot()
            backStack.add(MainRoute.About)
        }
    }

    fun openRootHelp() {
        if (currentRoute != MainRoute.RootHelp) {
            ensureSettingsRoot()
            backStack.add(MainRoute.RootHelp)
        }
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun detailsPage(route: MainRoute.Details): HardwareDetailPage =
        HardwareDetailPage.entries.firstOrNull { it.name == route.categoryName } ?: HardwareDetailPage.DEVICE

    private fun ensureSettingsRoot() {
        if (selectedTabIndex != SETTINGS_TAB_INDEX || backStack.size != 1) {
            selectTab(SETTINGS_TAB_INDEX)
        }
    }
}

internal fun MainRoute.rootTabIndex(): Int = when (this) {
    MainRoute.Settings,
    MainRoute.ThemeSettings,
    MainRoute.About,
    MainRoute.RootHelp -> SETTINGS_TAB_INDEX

    else -> INFO_TAB_INDEX
}

internal fun MainRoute.navigationDepth(): Int = when (this) {
    MainRoute.Overview,
    MainRoute.Settings -> 0

    is MainRoute.Details,
    MainRoute.ThemeSettings,
    MainRoute.About,
    MainRoute.RootHelp -> 1
}
