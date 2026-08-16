package com.fioiu8.devinfo.feature.main.screen.about
import com.fioiu8.devinfo.feature.main.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import com.fioiu8.devinfo.ui.CustomMiuixIcons
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun AboutScreenMiuix(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    var logoHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val scrollProgress by remember {
        derivedStateOf {
            if (logoHeightPx <= 0) {
                0f
            } else {
                val index = lazyListState.firstVisibleItemIndex
                val offset = lazyListState.firstVisibleItemScrollOffset
                if (index > 0) 1f else (offset.toFloat() / logoHeightPx).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = state.title,
                scrollBehavior = topAppBarScrollBehavior,
                color = if (scrollProgress == 1f) colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                titleColor = colorScheme.onSurface.copy(
                    alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
                ),
                defaultWindowInsetsPadding = false,
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        val layoutDirection = LocalLayoutDirection.current
                        Icon(
                            modifier = Modifier.graphicsLayer {
                                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                            },
                            imageVector = CustomMiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        val scrollPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
        )
        val logoPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 40.dp,
            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
        )

        // Logo area (positioned behind the lazy column)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { logoHeightPx = it.size.height }
                    .padding(
                        top = logoPadding.calculateTopPadding() + 52.dp,
                        start = logoPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = logoPadding.calculateEndPadding(LocalLayoutDirection.current),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                )
                Text(
                    modifier = Modifier.padding(top = 12.dp, bottom = 5.dp),
                    text = state.appName,
                    color = colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.onSurfaceVariantSummary,
                    text = state.versionName,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            // Scrollable content
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = scrollPadding.calculateTopPadding(),
                    start = scrollPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = scrollPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
                overscrollEffect = null,
            ) {
                // Transparent spacer matching logo height
                item(key = "logoSpacer") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { logoHeightPx.toDp() }),
                        contentAlignment = Alignment.TopCenter,
                        content = { },
                    )
                }

                item(key = "about") {
                    Column(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .padding(bottom = innerPadding.calculateBottomPadding() + 12.dp),
                    ) {
                        Card(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            colors = CardDefaults.defaultColors(
                                colorScheme.surfaceContainer,
                                androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        ) {
                            state.links.forEach {
                                ArrowPreference(
                                    title = it.fullText,
                                    onClick = { actions.onOpenLink(it.url) },
                                )
                            }
                        }
                        Spacer(
                            Modifier.height(
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                        WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                            )
                        )
                    }
                }
            }
        }
    }
}
