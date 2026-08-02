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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fioiu8.devinfo.BuildConfig
import com.fioiu8.devinfo.ModuleExportHelper
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.UpdateState
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.model.PaletteStyle
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.BlurredBar
import com.fioiu8.devinfo.ui.rememberBlurBackdrop
import com.fioiu8.devinfo.ui.screen.about.AboutScreen
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

internal val LocalFloatingNavigationContentPadding = staticCompositionLocalOf { 0.dp }

/** App-owned settings and callbacks required by the main UI. */
data class MainScreenSettings(
    val deviceId: String,
    val themeMode: ThemeMode,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val themeColor: ThemeColor,
    val onThemeColorChange: (ThemeColor) -> Unit,
    val uiStyle: UiStyle,
    val onUiStyleChange: (UiStyle) -> Unit,
    val checkUpdate: Boolean,
    val onCheckUpdateChange: (Boolean) -> Unit,
    val paletteStyle: PaletteStyle,
    val onPaletteStyleChange: (PaletteStyle) -> Unit,
    val colorSpec: com.fioiu8.devinfo.model.ColorSpec,
    val onColorSpecChange: (com.fioiu8.devinfo.model.ColorSpec) -> Unit,
    val enableBlur: Boolean,
    val onEnableBlurChange: (Boolean) -> Unit,
    val enableFloatingBottomBar: Boolean,
    val onEnableFloatingBottomBarChange: (Boolean) -> Unit,
    val enableFloatingBottomBarBlur: Boolean,
    val onEnableFloatingBottomBarBlurChange: (Boolean) -> Unit,
    val pageScale: Float,
    val onPageScaleChange: (Float) -> Unit,
    val enablePredictiveBack: Boolean,
    val onEnablePredictiveBackChange: (Boolean) -> Unit,
    val appLanguage: AppLanguage,
    val customLocaleTag: String,
    val onAppLanguageChange: (AppLanguage) -> Unit,
    val onCustomLocaleTagChange: (String) -> Unit
)

/** Renders navigation and UI effects while [MainViewModel] owns data coordination. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    settings: MainScreenSettings,
    exportHelper: ModuleExportHelper
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val deviceInfoItems by viewModel.deviceInfoItems.collectAsStateWithLifecycle()
    val isDeviceInfoLoading by viewModel.isDeviceInfoLoading.collectAsStateWithLifecycle()
    val overviewSnapshot by viewModel.overviewSnapshot.collectAsStateWithLifecycle()
    val isOverviewLoading by viewModel.isOverviewLoading.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val releaseInfo by viewModel.releaseInfo.collectAsStateWithLifecycle()

    var selectedIndex by rememberSaveable { mutableIntStateOf(INFO_TAB_INDEX) }
    var showDetailsPage by remember { mutableStateOf(false) }
    var detailCategory by remember { mutableStateOf(InfoCategory.DEVICE) }
    var showAboutPage by remember { mutableStateOf(false) }
    var isAboutVisible by remember { mutableStateOf(false) }
    var showThemeSettingsPage by remember { mutableStateOf(false) }
    var isThemeSettingsVisible by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val useNavigationRail = configuration.screenWidthDp >= TABLET_NAVIGATION_RAIL_MIN_WIDTH_DP
    val navigationRailStartInsets =
        WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Start)
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val aboutOffsetX = remember { Animatable(0f) }
    val themeSettingsOffsetX = remember { Animatable(0f) }
    val detailOffsetX = remember { Animatable(0f) }
    val alreadyLatestMessage = stringResource(R.string.already_latest)
    val exportFailedLabel = stringResource(R.string.export_failed)
    val materialSnackbarHostState = remember { SnackbarHostState() }
    val miuixSnackbarHostState = remember { MiuixSnackbarHostState() }
    val showMessage = rememberDevInfoMessageHandler(
        materialHostState = materialSnackbarHostState,
        miuixHostState = miuixSnackbarHostState,
    )

    // SAF 导出 — 用户选择保存位置后将 ZIP 写入 ContentResolver 提供的输出流
    val saveExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult // 用户取消
        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    exportHelper.exportModuleToStream(
                        deviceId = settings.deviceId,
                        itemsState = deviceInfoItems,
                        outputStream = outputStream,
                        onSuccess = {
                            scope.launch {
                                exportedFileUri = uri
                                showExportSuccessDialog = true
                            }
                        },
                        onError = { error ->
                            showMessage("$exportFailedLabel: $error")
                        }
                    )
                } ?: run {
                    showMessage("$exportFailedLabel: 无法打开文件")
                }
            } catch (e: Exception) {
                showMessage("$exportFailedLabel: ${e.message}")
            }
        }
    }

    val snackbarBottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            if (useNavigationRail) 16.dp else 92.dp
    val floatingNavigationContentPadding = if (
        !useNavigationRail && settings.enableFloatingBottomBar
    ) {
        88.dp
    } else {
        0.dp
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onForegroundChanged(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.onForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.onForegroundChanged(true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onForegroundChanged(false)
        }
    }

    LaunchedEffect(selectedIndex, viewModel) {
        viewModel.onInfoTabChanged(selectedIndex == INFO_TAB_INDEX)
    }

    LaunchedEffect(showAboutPage) {
        if (showAboutPage) {
            isAboutVisible = true
            aboutOffsetX.snapTo(screenWidthPx)
            aboutOffsetX.animateTo(0f, animationSpec = tween(ABOUT_ANIMATION_DURATION_MS))
        }
    }

    LaunchedEffect(showThemeSettingsPage) {
        if (showThemeSettingsPage) {
            isThemeSettingsVisible = true
            themeSettingsOffsetX.snapTo(screenWidthPx)
            themeSettingsOffsetX.animateTo(0f, animationSpec = tween(ABOUT_ANIMATION_DURATION_MS))
        }
    }

    LaunchedEffect(showDetailsPage) {
        if (!showDetailsPage) {
            detailOffsetX.snapTo(0f)
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            UpdateState.UP_TO_DATE -> {
                showMessage(alreadyLatestMessage)
                viewModel.resetUpdateState()
            }

            UpdateState.NEW_VERSION_AVAILABLE,
            UpdateState.ERROR -> showUpdateDialog = true

            else -> Unit
        }
    }

    fun dismissAboutPage() {
        scope.launch {
            aboutOffsetX.animateTo(screenWidthPx, animationSpec = tween(ABOUT_ANIMATION_DURATION_MS))
            showAboutPage = false
            isAboutVisible = false
        }
    }

    fun dismissThemeSettingsPage() {
        scope.launch {
            themeSettingsOffsetX.animateTo(
                screenWidthPx,
                animationSpec = tween(ABOUT_ANIMATION_DURATION_MS),
            )
            showThemeSettingsPage = false
            isThemeSettingsVisible = false
        }
    }

    val navigationItems = listOf(
        MainNavigationItem(
            label = stringResource(R.string.nav_info),
            selectedIcon = Icons.Filled.Description,
            unselectedIcon = Icons.Outlined.Description
        ),
        MainNavigationItem(
            label = stringResource(R.string.nav_settings),
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    fun selectNavigationItem(index: Int) {
        selectedIndex = index
        if (index == SETTINGS_TAB_INDEX) showDetailsPage = false
    }

    val topBarTitle = when {
        selectedIndex == SETTINGS_TAB_INDEX -> stringResource(R.string.title_settings)
        showDetailsPage -> stringResource(R.string.title_device_details)
        else -> stringResource(R.string.title_device_info)
    }
    val showTopBarBackButton = selectedIndex == INFO_TAB_INDEX && showDetailsPage

    DevInfoFeedbackScope(
        materialHostState = materialSnackbarHostState,
        miuixHostState = miuixSnackbarHostState,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MainRootScaffold(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                MainNavigationRail(
                    items = navigationItems,
                    selectedIndex = selectedIndex,
                    onItemSelected = ::selectNavigationItem,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            MainScaffold(
                modifier = Modifier.weight(1f),
                consumedStartInsets = if (useNavigationRail) {
                    navigationRailStartInsets
                } else {
                    WindowInsets(0, 0, 0, 0)
                },
                title = topBarTitle,
                showBackButton = showTopBarBackButton,
                onBack = { showDetailsPage = false },
                items = navigationItems,
                selectedIndex = selectedIndex,
                onItemSelected = ::selectNavigationItem,
                showBottomBar = !useNavigationRail,
                enableBlur = settings.enableBlur,
                enableFloatingBottomBar = settings.enableFloatingBottomBar,
                enableFloatingBottomBarBlur = settings.enableFloatingBottomBarBlur,
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CompositionLocalProvider(
                        LocalFloatingNavigationContentPadding provides floatingNavigationContentPadding,
                    ) {
                        AnimatedContent(
                            modifier = Modifier.offset {
                                IntOffset(
                                    x = if (showDetailsPage) detailOffsetX.value.roundToInt() else 0,
                                    y = 0
                                )
                            },
                            targetState = selectedIndex to showDetailsPage,
                            transitionSpec = {
                                val direction = if (
                                    targetState.first > initialState.first ||
                                    targetState.second && !initialState.second
                                ) {
                                    FORWARD_DIRECTION
                                } else {
                                    BACKWARD_DIRECTION
                                }
                                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { direction * it / 5 })
                                    .togetherWith(
                                        fadeOut(tween(160)) +
                                            slideOutHorizontally(tween(180)) { -direction * it / 5 }
                                    )
                            },
                            label = "mainNavigationTransition"
                        ) { pageState ->
                            when (pageState.first) {
                            INFO_TAB_INDEX -> {
                                if (pageState.second) {
                                    DeviceInfoPage(
                                        deviceId = settings.deviceId,
                                        itemsState = deviceInfoItems,
                                        isLoading = isDeviceInfoLoading,
                                        overviewSnapshot = overviewSnapshot,
                                        onRefresh = viewModel::refreshAndAwait,
                                        initialCategory = detailCategory
                                    )
                                } else {
                                    DeviceInfoOverviewPage(
                                        itemsState = deviceInfoItems,
                                        isLoading = isDeviceInfoLoading,
                                        isOverviewLoading = isOverviewLoading,
                                        snapshot = overviewSnapshot,
                                        onRefresh = viewModel::refreshAndAwait,
                                        onOpenDetails = { category ->
                                            detailCategory = category
                                            showDetailsPage = true
                                        }
                                    )
                                }
                            }

                                SETTINGS_TAB_INDEX -> {
                                val languageOptions = AppLanguage.entries.map { language ->
                                    stringResource(language.displayNameResId)
                                }
                                SettingsPage(
                                    versionName = viewModel.appVersionName,
                                    versionCode = viewModel.appVersionCode,
                                    uiStyle = settings.uiStyle,
                                    onUiStyleChange = settings.onUiStyleChange,
                                    onThemeSettingsClick = { showThemeSettingsPage = true },
                                    onExportClick = { showExportDialog = true },
                                    onAboutClick = { showAboutPage = true },
                                    appLanguage = settings.appLanguage,
                                    checkUpdate = settings.checkUpdate,
                                    onCheckUpdateChange = settings.onCheckUpdateChange,
                                    languageOptions = languageOptions,
                                    onLanguageChange = { index ->
                                        settings.onAppLanguageChange(AppLanguage.entries[index])
                                    },
                                    customLocaleTag = settings.customLocaleTag,
                                    onCustomLocaleTagChange = settings.onCustomLocaleTagChange
                                )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isAboutVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(aboutOffsetX.value.roundToInt(), 0) }
            ) {
                AboutScreen(
                    versionName = viewModel.appVersionName,
                    onBack = { dismissAboutPage() }
                )
            }
        }

        if (isThemeSettingsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(themeSettingsOffsetX.value.roundToInt(), 0) },
            ) {
                ThemeSettingsPage(
                    themeMode = settings.themeMode,
                    onThemeModeChange = settings.onThemeModeChange,
                    themeColor = settings.themeColor,
                    onThemeColorChange = settings.onThemeColorChange,
                    paletteStyle = settings.paletteStyle,
                    onPaletteStyleChange = settings.onPaletteStyleChange,
                    colorSpec = settings.colorSpec,
                    onColorSpecChange = settings.onColorSpecChange,
                    enableBlur = settings.enableBlur,
                    onEnableBlurChange = settings.onEnableBlurChange,
                    enableFloatingBottomBar = settings.enableFloatingBottomBar,
                    onEnableFloatingBottomBarChange = settings.onEnableFloatingBottomBarChange,
                    enableFloatingBottomBarBlur = settings.enableFloatingBottomBarBlur,
                    onEnableFloatingBottomBarBlurChange = settings.onEnableFloatingBottomBarBlurChange,
                    pageScale = settings.pageScale,
                    onPageScaleChange = settings.onPageScaleChange,
                    enablePredictiveBack = settings.enablePredictiveBack,
                    onEnablePredictiveBackChange = settings.onEnablePredictiveBackChange,
                    onBack = ::dismissThemeSettingsPage,
                )
            }
        }
            }

            DevInfoSnackbarHost(
                materialHostState = materialSnackbarHostState,
                miuixHostState = miuixSnackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = snackbarBottomPadding),
            )
        }

    PredictiveBackHandler(
        enabled = showThemeSettingsPage || showAboutPage || showDetailsPage,
        onBack = { progress ->
            val dismissThemeSettings = showThemeSettingsPage
            val dismissAbout = !dismissThemeSettings && showAboutPage
            val dismissDetails = !dismissThemeSettings && !dismissAbout && showDetailsPage
            try {
                progress.collect { event ->
                    when {
                        dismissThemeSettings -> themeSettingsOffsetX.snapTo(event.progress * screenWidthPx)
                        dismissAbout -> aboutOffsetX.snapTo(event.progress * screenWidthPx)
                        dismissDetails -> detailOffsetX.snapTo(event.progress * screenWidthPx)
                    }
                }
                when {
                    dismissThemeSettings -> {
                        themeSettingsOffsetX.animateTo(
                            screenWidthPx,
                            animationSpec = tween(PREDICTIVE_BACK_ANIMATION_DURATION_MS),
                        )
                        showThemeSettingsPage = false
                        isThemeSettingsVisible = false
                    }

                    dismissAbout -> {
                        aboutOffsetX.animateTo(
                            screenWidthPx,
                            animationSpec = tween(PREDICTIVE_BACK_ANIMATION_DURATION_MS)
                        )
                        showAboutPage = false
                        isAboutVisible = false
                    }

                    dismissDetails -> {
                        detailOffsetX.animateTo(
                            screenWidthPx,
                            animationSpec = tween(PREDICTIVE_BACK_ANIMATION_DURATION_MS)
                        )
                        showDetailsPage = false
                    }
                }
            } catch (_: CancellationException) {
                when {
                    dismissThemeSettings -> scope.launch {
                        themeSettingsOffsetX.animateTo(
                            0f,
                            animationSpec = tween(PREDICTIVE_BACK_ANIMATION_DURATION_MS),
                        )
                    }

                    dismissAbout -> scope.launch {
                        aboutOffsetX.animateTo(
                            0f,
                            animationSpec = tween(PREDICTIVE_BACK_ANIMATION_DURATION_MS)
                        )
                    }

                    dismissDetails -> scope.launch {
                        detailOffsetX.animateTo(
                            0f,
                            animationSpec = tween(PREDICTIVE_BACK_ANIMATION_DURATION_MS)
                        )
                    }
                }
            }
        }
    )

    UpdateAvailableDialog(
        show = showUpdateDialog,
        info = releaseInfo,
        isError = updateState == UpdateState.ERROR,
        currentVersion = BuildConfig.VERSION_NAME,
        onDownload = {
            openUrl(
                context = context,
                url = releaseInfo?.htmlUrl ?: RELEASES_URL,
                onFailure = showMessage,
            )
            showUpdateDialog = false
            viewModel.resetUpdateState()
        },
        onRetry = {
            showUpdateDialog = false
            viewModel.retryUpdateCheck()
        },
        onDismiss = {
            showUpdateDialog = false
            viewModel.resetUpdateState()
        }
    )

    ExportConfirmDialog(
        show = showExportDialog,
        onConfirm = {
            showExportDialog = false
            saveExportLauncher.launch("DevInfo_${Build.MODEL}.zip")
        },
        onDismiss = { showExportDialog = false }
    )

    ExportSuccessDialog(
        show = showExportSuccessDialog,
        fileUri = exportedFileUri,
        onDismiss = { showExportSuccessDialog = false }
    )
    }
}

private typealias MainNavigationItem = DevInfoNavigationItem

@Composable
private fun MainRootScaffold(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val contentStateHolder = rememberSaveableStateHolder()

    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> {
            Box(modifier = modifier) {
                contentStateHolder.SaveableStateProvider(MAIN_CONTENT_STATE_KEY) {
                    content()
                }
            }
        }

        UiStyle.MIUIX -> {
            MiuixScaffold(
                modifier = modifier,
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    contentStateHolder.SaveableStateProvider(MAIN_CONTENT_STATE_KEY) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    modifier: Modifier,
    consumedStartInsets: WindowInsets,
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    showBottomBar: Boolean,
    enableBlur: Boolean,
    enableFloatingBottomBar: Boolean,
    enableFloatingBottomBarBlur: Boolean,
    content: @Composable (PaddingValues) -> Unit
) {
    val topBar: @Composable () -> Unit = {
        MainTopBar(
            title = title,
            showBackButton = showBackButton,
            onBack = onBack
        )
    }

    val standardBottomBar: @Composable () -> Unit = {
        if (showBottomBar) {
            DevInfoNavigationBar(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = onItemSelected,
            )
        }
    }

    Box(modifier = modifier.consumeWindowInsets(consumedStartInsets)) {
        when (LocalUiStyle.current) {
            UiStyle.MATERIAL3 -> {
                // Material3：保留原始 Scaffold + bottomBar slot，不干涉，保留原始悬浮样式
                if (enableFloatingBottomBar) {
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
                    val blurBackdrop = rememberBlurBackdrop(
                        enableBlur = enableBlur,
                        surfaceColor = surfaceColor,
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier),
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = topBar,
                                containerColor = MaterialTheme.colorScheme.background,
                                content = content,
                            )
                        }
                        if (showBottomBar) {
                            FloatingMainNavigationBar(
                                items = items,
                                selectedIndex = selectedIndex,
                                onItemSelected = onItemSelected,
                                blurBackdrop = blurBackdrop,
                                enableBlur = enableBlur && enableFloatingBottomBarBlur,
                            )
                        }
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = topBar,
                        bottomBar = standardBottomBar,
                        containerColor = MaterialTheme.colorScheme.background,
                        content = content,
                    )
                }
            }

            UiStyle.MIUIX -> {
                // Miuix：使用 Box + blur 叠加，实现悬浮毛玻璃效果
                if (enableFloatingBottomBar) {
                    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer
                    val blurBackdrop = rememberBlurBackdrop(
                        enableBlur = enableBlur,
                        surfaceColor = surfaceColor,
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier),
                        ) {
                            MiuixScaffold(
                                topBar = topBar,
                                popupHost = {},
                                content = content,
                            )
                        }
                        if (showBottomBar) {
                            FloatingMainNavigationBar(
                                items = items,
                                selectedIndex = selectedIndex,
                                onItemSelected = onItemSelected,
                                blurBackdrop = blurBackdrop,
                                enableBlur = enableBlur && enableFloatingBottomBarBlur,
                            )
                        }
                    }
                } else {
                    MiuixScaffold(
                        topBar = topBar,
                        bottomBar = standardBottomBar,
                        popupHost = {},
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FloatingMainNavigationBar(
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    blurBackdrop: LayerBackdrop?,
    enableBlur: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(
                bottom = 12.dp +
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        DevInfoFloatingNavigationBar(
            items = items,
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected,
            glassEffect = enableBlur,
            blurBackdrop = blurBackdrop,
        )
    }
}

@Composable
private fun MainTopBar(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit
) {
    Column {
        when (LocalUiStyle.current) {
            UiStyle.MATERIAL3 -> {
                MaterialMainTopBar(
                    title = title,
                    showBackButton = showBackButton,
                    onBack = onBack
                )
            }

            UiStyle.MIUIX -> {
                MiuixMainTopBar(
                    title = title,
                    showBackButton = showBackButton,
                    onBack = onBack
                )
            }
        }

        if (!BuildConfig.IS_OFFICIAL) {
            TestVersionWarningCard(
                versionName = BuildConfig.VERSION_NAME,
                buildType = BuildConfig.BUILD_TYPE_NAME,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialMainTopBar(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun MiuixMainTopBar(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit
) {
    MiuixTopAppBar(
        title = title,
        navigationIcon = {
            if (showBackButton) {
                MiuixIconButton(onClick = onBack) {
                    MiuixIcon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = stringResource(R.string.back),
                        tint = MiuixTheme.colorScheme.onBackground
                    )
                }
            }
        },
        color = MiuixTheme.colorScheme.surface
    )
}

@Composable
private fun MainNavigationRail(
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialMainNavigationRail(items, selectedIndex, onItemSelected, modifier)
        UiStyle.MIUIX -> MiuixMainNavigationRail(items, selectedIndex, onItemSelected, modifier)
    }
}

@Composable
private fun MaterialMainNavigationRail(
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Spacer(modifier = Modifier.weight(1f))
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MiuixMainNavigationRail(
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MiuixNavigationRail(
        modifier = modifier.fillMaxHeight(),
        color = MiuixTheme.colorScheme.surface
    ) {
        Spacer(modifier = Modifier.weight(1f))
        items.forEachIndexed { index, item ->
            MiuixNavigationRailItem(
                icon = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                label = item.label,
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun openUrl(
    context: Context,
    url: String,
    onFailure: (String) -> Unit,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: Exception) {
        onFailure(context.getString(R.string.cannot_open_link))
    }
}

private const val INFO_TAB_INDEX = 0
private const val SETTINGS_TAB_INDEX = 1
private const val MAIN_CONTENT_STATE_KEY = "main_content"
private const val FORWARD_DIRECTION = 1
private const val BACKWARD_DIRECTION = -1
private const val TABLET_NAVIGATION_RAIL_MIN_WIDTH_DP = 600
private const val ABOUT_ANIMATION_DURATION_MS = 300
private const val PREDICTIVE_BACK_ANIMATION_DURATION_MS = 200
private const val RELEASES_URL = "https://github.com/FIOIU8/DevInfo/releases"
