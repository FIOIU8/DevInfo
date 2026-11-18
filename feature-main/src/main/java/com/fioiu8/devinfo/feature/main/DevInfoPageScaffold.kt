/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fioiu8.devinfo.ui.TestVersionWarningCard
import com.fioiu8.devinfo.ui.kit.liquid.LiquidGlassBackdropHost
import com.fioiu8.devinfo.ui.kit.liquid.LiquidGlassIconButton

/**
 * 页面标题的共享元素修饰符。
 *
 * `MainNavigationHost` 在路由切换期间提供带 `sharedBounds` 的 Modifier，各页面骨架
 * 只负责把它挂到标题上，避免把 `SharedTransitionScope` 逐层传参。
 */
internal val LocalDevInfoPageTitleModifier = staticCompositionLocalOf<Modifier> { Modifier }

/**
 * 全应用统一的页面骨架。
 *
 * 顶栏保持紧凑，使每个路由只组合一个标题实例；共享元素过渡依赖“同一 key 在
 * 同一路由内只出现一次”，可折叠大标题会同时组合展开/收起两份标题，因此不使用。
 * 大标题感由概览页内容区的 headline 承担。
 */
@Composable
internal fun DevInfoPageScaffold(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    LiquidGlassBackdropHost(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                DevInfoTopBar(
                    title = title,
                    showBackButton = showBackButton,
                    onBack = onBack,
                    onSettingsClick = onSettingsClick,
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}

@Composable
private fun DevInfoTopBar(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)?,
) {
    Column {
        val titleContent: @Composable () -> Unit = {
            Text(
                text = title,
                modifier = LocalDevInfoPageTitleModifier.current,
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        }
        val navigationIcon: @Composable () -> Unit = {
            if (showBackButton) {
                LiquidGlassIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        }
        val actions: @Composable () -> Unit = {
            onSettingsClick?.let { openSettings ->
                LiquidGlassIconButton(onClick = openSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                    )
                }
            }
        }
        val colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        )

        TopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = { actions() },
            colors = colors,
        )

        if (!BuildConfig.IS_OFFICIAL) {
            TestVersionWarningCard(
                versionName = BuildConfig.VERSION_NAME,
                buildType = BuildConfig.BUILD_TYPE_NAME,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
