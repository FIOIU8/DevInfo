package com.fioiu8.devinfo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar as MiuixFloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem as MiuixFloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
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

/** A compact floating navigation surface based on KernelSU-Style-UI-Kit's Miuix bottom bar. */
@Composable
fun DevInfoFloatingNavigationBar(
    items: List<DevInfoNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialFloatingNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected,
            modifier = modifier,
        )

        UiStyle.MIUIX -> MiuixNativeFloatingNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected,
            modifier = modifier,
        )
    }
}

@Composable
private fun MaterialFloatingNavigationBar(
    items: List<DevInfoNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val glassBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Row(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 14.dp, shape = shape, clip = false)
            .background(color = surfaceColor.copy(alpha = 0.82f), shape = shape)
            .border(width = 1.dp, color = glassBorder, shape = shape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            MaterialFloatingNavigationItem(
                item = item,
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MaterialFloatingNavigationItem(
    item: DevInfoNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "floatingNavigationIconColor",
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "floatingNavigationLabelColor",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            Color.Transparent
        },
        label = "floatingNavigationIndicatorColor",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(indicatorColor, RoundedCornerShape(24.dp))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = iconTint,
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = labelTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiuixNativeFloatingNavigationBar(
    items: List<DevInfoNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MiuixFloatingNavigationBar(
        modifier = modifier.widthIn(max = 520.dp),
        color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
        cornerRadius = 28.dp,
        horizontalOutSidePadding = 20.dp,
        shadowElevation = 10.dp,
        showDivider = true,
    ) {
        items.forEachIndexed { index, item ->
            MiuixFloatingNavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                label = item.label,
            )
        }
    }
}

/** Uses the component system selected by the user for initial data loading. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DevInfoLoadingIndicator(modifier: Modifier = Modifier) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> androidx.compose.material3.LoadingIndicator(
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val colors = ListItemDefaults.segmentedColors().copy(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        disabledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        supportingContentColor = MaterialTheme.colorScheme.outline,
    )

    Box(modifier = modifier) {
        SegmentedListItem(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1).copy(
                shape = MaterialTheme.shapes.large,
            ),
            colors = colors,
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
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(0.3f),
                )
            },
            content = { Text(title) },
        )
        if (expanded) {
            // A compact anchor at the card's lower end makes the standard M3 menu expand
            // below the whole setting row instead of covering its value text.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(1.dp),
            ) {
                DropdownMenuPopup(
                    expanded = true,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                        items.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                selected = index == safeIndex,
                                shapes = MenuDefaults.itemShape(index = index, count = items.size),
                                leadingIcon = {
                                    if (index == safeIndex) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                        )
                                    }
                                },
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
}
