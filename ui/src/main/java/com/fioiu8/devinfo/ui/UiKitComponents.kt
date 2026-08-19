/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.fioiu8.devinfo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.core.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.SnackbarHost as MiuixSnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LocalMaterialSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }
private val LocalMiuixSnackbarHostState = staticCompositionLocalOf<MiuixSnackbarHostState?> { null }

/** Provides the app-wide transient-message host to every DevInfo page. */
@Composable
fun DevInfoFeedbackScope(
    materialHostState: SnackbarHostState,
    miuixHostState: MiuixSnackbarHostState,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMaterialSnackbarHostState provides materialHostState,
        LocalMiuixSnackbarHostState provides miuixHostState,
        content = content,
    )
}

/** Returns a main-thread-safe callback for short, non-blocking feedback. */
@Composable
fun rememberDevInfoMessageHandler(
    materialHostState: SnackbarHostState? = LocalMaterialSnackbarHostState.current,
    miuixHostState: MiuixSnackbarHostState? = LocalMiuixSnackbarHostState.current,
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val uiStyle = LocalUiStyle.current

    return remember(materialHostState, miuixHostState, scope, uiStyle) {
        { message ->
            scope.launch {
                when (uiStyle) {
                    UiStyle.MATERIAL3 -> materialHostState?.showSnackbar(message)
                    UiStyle.MIUIX -> miuixHostState?.showSnackbar(message)
                }
            }
        }
    }
}

/**
 * Adapted from KernelSU-Style-UI-Kit's SnackBarHost so app feedback can be dismissed by swipe.
 */
@Composable
fun DevInfoSnackbarHost(
    materialHostState: SnackbarHostState,
    miuixHostState: MiuixSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialDevInfoSnackbarHost(
            hostState = materialHostState,
            modifier = modifier,
        )

        UiStyle.MIUIX -> MiuixSnackbarHost(
            state = miuixHostState,
            modifier = modifier,
        )
    }
}

@Composable
private fun MaterialDevInfoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        backgroundContent = {},
        onDismiss = { hostState.currentSnackbarData?.dismiss() },
    ) {
        SnackbarHost(
            hostState = hostState,
            snackbar = { data -> MaterialDevInfoSnackbar(data) },
        )
    }

    LaunchedEffect(hostState.currentSnackbarData) {
        if (hostState.currentSnackbarData != null) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
}

@Composable
private fun MaterialDevInfoSnackbar(data: SnackbarData) = Snackbar(
    snackbarData = data,
    shape = RoundedCornerShape(16.dp),
    containerColor = MaterialTheme.colorScheme.inverseSurface,
    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
)

@Composable
fun DevInfoNavigationBar(
    items: List<DevInfoNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected,
            modifier = modifier,
        )

        UiStyle.MIUIX -> MiuixNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected,
            modifier = modifier,
        )
    }
}

@Composable
private fun MaterialNavigationBar(
    items: List<DevInfoNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(item.label) },
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
            )
        }
    }
}

@Composable
private fun MiuixNavigationBar(
    items: List<DevInfoNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    MiuixNavigationBar(
        modifier = modifier,
        color = MiuixTheme.colorScheme.surface,
    ) {
        items.forEachIndexed { index, item ->
            MiuixNavigationBarItem(
                modifier = Modifier.weight(1f),
                icon = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                label = item.label,
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
            )
        }
    }
}

/** Uses the component system selected by the user for initial data loading. */
@Composable
fun DevInfoLoadingIndicator(modifier: Modifier = Modifier) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> CircularProgressIndicator(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
        )

        UiStyle.MIUIX -> InfiniteProgressIndicator(
            modifier = modifier,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

data class DevInfoNavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** Material switch adapted from KernelSU-Style-UI-Kit's expressive switch. */
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
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

/** Material settings row adapted from KernelSU-Style-UI-Kit's segmented dropdown item. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevInfoSegmentedDropdownItem(
    icon: ImageVector,
    title: String,
    summary: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            ListItem(
                headlineContent = { Text(title) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                leadingContent = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                },
                supportingContent = {
                    if (summary.isNotBlank()) {
                        Text(text = summary)
                    }
                },
                trailingContent = {
                    Text(
                        text = items.getOrNull(safeIndex).orEmpty(),
                        color = valueColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(0.3f),
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        }
        if (expanded) {
            // A compact anchor at the card's lower end makes the standard M3 menu expand
            // below the whole setting row instead of covering its value text.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(1.dp),
            ) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { expanded = false },
                ) {
                    items.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            leadingIcon = if (index == safeIndex) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            } else null,
                            onClick = {
                                onItemSelected(index)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
