/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val LocalMaterialSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }

/** Provides the app-wide transient-message host to every DevInfo page. */
@Composable
fun DevInfoFeedbackScope(
    materialHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMaterialSnackbarHostState provides materialHostState,
        content = content,
    )
}

/** Returns a main-thread-safe callback for short, non-blocking feedback. */
@Composable
fun rememberDevInfoMessageHandler(
    materialHostState: SnackbarHostState? = LocalMaterialSnackbarHostState.current,
): (String) -> Unit {
    val scope = rememberCoroutineScope()

    return remember(materialHostState, scope) {
        { message ->
            scope.launch {
                materialHostState?.showSnackbar(message)
            }
        }
    }
}

/**
 * Snackbar host that lets the user flick a transient message away with a horizontal swipe.
 *
 * The swipe state is keyed by the current [SnackbarData], so each new message starts from the
 * settled position without resetting the state by hand.
 */
@Composable
fun DevInfoSnackbarHost(
    materialHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SwipeDismissSnackbarHost(hostState = materialHostState, modifier = modifier)
}

@Composable
private fun SwipeDismissSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier,
) {
    val currentData = hostState.currentSnackbarData

    key(currentData) {
        val dismissState = rememberSwipeToDismissBoxState()

        SwipeToDismissBox(
            modifier = modifier,
            state = dismissState,
            backgroundContent = {},
            onDismiss = { currentData?.dismiss() },
        ) {
            SnackbarHost(
                hostState = hostState,
                snackbar = { data -> MaterialDevInfoSnackbar(data) },
            )
        }
    }
}

@Composable
private fun MaterialDevInfoSnackbar(data: SnackbarData) = Snackbar(snackbarData = data)

/** M3 Expressive 形状变形加载指示器。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DevInfoLoadingIndicator(modifier: Modifier = Modifier) {
    LoadingIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Material 3 下拉刷新容器。
 */
@Composable
fun DevInfoPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = rememberPullToRefreshState(),
        modifier = modifier,
    ) {
        content()
    }
}

data class DevInfoNavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Switch with an icon inside the thumb, following the expressive Material 3 switch shape.
 */
@Composable
fun DevInfoExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        thumbContent = { SwitchThumb(checked = checked) },
    )
}

@Composable
private fun SwitchThumb(checked: Boolean) {
    val tint = if (checked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Icon(
        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
}

/**
 * Settings row that shows the current choice on the trailing edge and opens a menu with every
 * option. The menu is anchored to the card's bottom-end corner so it expands below the whole
 * row instead of covering the selected value.
 *
 * 行本身复用 [SegmentedRow]，不再单独实现一套 Card + ListItem，因此设置页只有一种行样式。
 */
@Composable
fun DevInfoSegmentedDropdownItem(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))

    Box(modifier = modifier) {
        SegmentedRow(
            title = title,
            supporting = summary,
            leadingIcon = icon,
            trailingValue = items.getOrNull(safeIndex).orEmpty(),
            showChevron = true,
            onClick = { expanded = true },
        )
        if (expanded) {
            SelectionMenu(
                items = items,
                selectedIndex = safeIndex,
                onDismiss = { expanded = false },
                onSelect = { index ->
                    onItemSelected(index)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun BoxScope.SelectionMenu(
    items: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    // A one-pixel anchor at the card's lower end keeps the standard dropdown below the row.
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(1.dp),
    ) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    leadingIcon = if (index == selectedIndex) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}
