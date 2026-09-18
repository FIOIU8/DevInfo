/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fioiu8.devinfo.feature.main

import com.fioiu8.devinfo.core.model.InfoCategory
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavKey

internal const val INFO_TAB_INDEX = 0
internal const val SETTINGS_TAB_INDEX = 1

/**
 * Routes rendered by the main page host.
 *
 * Each route is value-stable so Miuix Nav can restore its stack and saveable page state after
 * process recreation. Details stores the enum name instead of the enum itself so the core module
 * remains independent from the serialization runtime.
 */
@Serializable
internal sealed interface MainRoute : NavKey {
    @Serializable
    data object Overview : MainRoute

    @Serializable
    data object Settings : MainRoute

    @Serializable
    data class Details(val categoryName: String) : MainRoute

    @Serializable
    data object ThemeSettings : MainRoute

    @Serializable
    data object About : MainRoute

    @Serializable
    data object RootHelp : MainRoute
}

/** Owns route mutations so every navigation path has the same stack semantics. */
internal class MainNavigator(
    private val controller: NavController,
) {
    val backStack = controller.backStack

    val currentRoute: MainRoute
        get() = backStack.lastOrNull() as? MainRoute ?: MainRoute.Overview

    /** Returns the selected root tab even while a child route is visible. */
    val selectedTabIndex: Int
        get() = ((backStack.firstOrNull() as? MainRoute) ?: MainRoute.Overview).rootTabIndex()

    val isOverviewVisible: Boolean
        get() = currentRoute == MainRoute.Overview

    fun selectTab(index: Int) {
        val target = if (index == SETTINGS_TAB_INDEX) MainRoute.Settings else MainRoute.Overview
        popToRoot()
        if (currentRoute != target) controller.replace(target)
    }

    fun openDetails(category: InfoCategory) {
        val target = MainRoute.Details(category.name)
        if (currentRoute is MainRoute.Details) {
            if (currentRoute != target) controller.replace(target)
        } else {
            controller.push(target)
        }
    }

    fun openThemeSettings() {
        if (currentRoute != MainRoute.ThemeSettings) {
            if (selectedTabIndex != SETTINGS_TAB_INDEX || backStack.size != 1) {
                selectTab(SETTINGS_TAB_INDEX)
            }
            controller.push(MainRoute.ThemeSettings)
        }
    }

    fun openAbout() {
        if (currentRoute != MainRoute.About) {
            if (selectedTabIndex != SETTINGS_TAB_INDEX || backStack.size != 1) {
                selectTab(SETTINGS_TAB_INDEX)
            }
            controller.push(MainRoute.About)
        }
    }

    /**
     * 打开 Root 授权帮助页。
     *
     * 两个入口（设置页「工具」区、授权失败弹窗）都走这里：失败弹窗是从设置页的 Root FAB 触发的，
     * 但这里仍按 [openAbout] 的写法兜底切回设置页，避免未来从别处调用时把帮助页压在错误栈上。
     */
    fun openRootHelp() {
        if (currentRoute != MainRoute.RootHelp) {
            if (selectedTabIndex != SETTINGS_TAB_INDEX || backStack.size != 1) {
                selectTab(SETTINGS_TAB_INDEX)
            }
            controller.push(MainRoute.RootHelp)
        }
    }

    fun pop(): Boolean = controller.pop()

    fun detailsCategory(route: MainRoute.Details): InfoCategory =
        InfoCategory.entries.firstOrNull { it.name == route.categoryName } ?: InfoCategory.DEVICE

    private fun popToRoot() {
        val root = backStack.firstOrNull() ?: return
        controller.popUntil { it == root }
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
