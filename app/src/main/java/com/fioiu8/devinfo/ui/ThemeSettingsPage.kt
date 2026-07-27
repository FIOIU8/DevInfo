package com.fioiu8.devinfo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** Miuix Theme Settings interaction adapted from KernelSU-Style-UI-Kit. */
@Composable
fun ThemeSettingsPage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeColor: ThemeColor,
    onThemeColorChange: (ThemeColor) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val selectedThemeIndex = themeMode.baseIndex
    val themeTabs = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    val colorItems = ThemeColor.entries.map { stringResource(it.displayNameResId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.theme_settings_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        val layoutDirection = LocalLayoutDirection.current
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.graphicsLayer {
                                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                            },
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Spacer(Modifier.height(28.dp))
                ThemePreviewCard(themeMode = themeMode, themeColor = themeColor)
                Spacer(Modifier.height(56.dp))

                TabRow(
                    tabs = themeTabs,
                    selectedTabIndex = selectedThemeIndex,
                    onTabSelected = { index ->
                        onThemeModeChange(themeMode.withBaseIndex(index))
                    },
                    height = 48.dp,
                )

                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.theme_monet),
                        summary = stringResource(R.string.theme_monet_summary),
                        checked = themeMode.isDynamic,
                        onCheckedChange = { enabled ->
                            onThemeModeChange(themeMode.withMonet(enabled))
                        },
                        startAction = {
                            Icon(
                                imageVector = Icons.Rounded.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        },
                    )

                    AnimatedVisibility(visible = themeMode.isDynamic) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.theme_key_color),
                            items = colorItems,
                            selectedIndex = ThemeColor.entries.indexOf(themeColor).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                ThemeColor.entries.getOrNull(index)?.let(onThemeColorChange)
                            },
                            startAction = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(themeColor.color),
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    themeMode: ThemeMode,
    themeColor: ThemeColor,
) {
    val accent = if (themeMode.isDynamic && themeColor != ThemeColor.DEFAULT) {
        themeColor.color
    } else {
        MiuixTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.theme_preview_summary),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                )
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(accent),
                )
            }
        }
    }
}

private val ThemeMode.baseIndex: Int
    get() = when (this) {
        ThemeMode.SYSTEM, ThemeMode.DYNAMIC_SYSTEM -> 0
        ThemeMode.LIGHT, ThemeMode.DYNAMIC_LIGHT -> 1
        ThemeMode.DARK, ThemeMode.DYNAMIC_DARK -> 2
    }

private fun ThemeMode.withBaseIndex(index: Int): ThemeMode = when (index.coerceIn(0, 2)) {
    0 -> if (isDynamic) ThemeMode.DYNAMIC_SYSTEM else ThemeMode.SYSTEM
    1 -> if (isDynamic) ThemeMode.DYNAMIC_LIGHT else ThemeMode.LIGHT
    else -> if (isDynamic) ThemeMode.DYNAMIC_DARK else ThemeMode.DARK
}

private fun ThemeMode.withMonet(enabled: Boolean): ThemeMode = when (baseIndex) {
    0 -> if (enabled) ThemeMode.DYNAMIC_SYSTEM else ThemeMode.SYSTEM
    1 -> if (enabled) ThemeMode.DYNAMIC_LIGHT else ThemeMode.LIGHT
    else -> if (enabled) ThemeMode.DYNAMIC_DARK else ThemeMode.DARK
}
