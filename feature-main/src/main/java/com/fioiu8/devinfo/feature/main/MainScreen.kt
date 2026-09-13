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

package com.fioiu8.devinfo.feature.main
import com.fioiu8.devinfo.ui.DevInfoFeedbackScope
import com.fioiu8.devinfo.core.model.CpuUsageSample
import com.fioiu8.devinfo.feature.main.R
import com.fioiu8.devinfo.ui.DevInfoExpressiveSwitch
import com.fioiu8.devinfo.ui.DevInfoLoadingIndicator
import com.fioiu8.devinfo.ui.DevInfoNavigationBar
import com.fioiu8.devinfo.ui.DevInfoNavigationItem
import com.fioiu8.devinfo.ui.DevInfoSegmentedDropdownItem
import com.fioiu8.devinfo.ui.DevInfoSnackbarHost
import com.fioiu8.devinfo.ui.MarkdownText
import com.fioiu8.devinfo.ui.TestVersionWarningCard
import com.fioiu8.devinfo.ui.rememberDevInfoMessageHandler

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fioiu8.devinfo.feature.main.BuildConfig
import com.fioiu8.devinfo.data.GitHubClient
import com.fioiu8.devinfo.data.ModuleExportHelper
import com.fioiu8.devinfo.core.model.UpdateState
import com.fioiu8.devinfo.data.AppLanguage
import com.fioiu8.devinfo.core.model.InfoCategory
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.core.model.UiStyle
import com.fioiu8.devinfo.ui.BlurredBar
import com.fioiu8.devinfo.ui.rememberBlurBackdrop
import com.fioiu8.devinfo.feature.main.screen.about.AboutScreen
import com.fioiu8.devinfo.ui.kit.FloatingBottomBar
import com.fioiu8.devinfo.ui.kit.FloatingBottomBarItem
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import com.fioiu8.devinfo.ui.theme.isInDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavController
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.window.WindowDialog
import com.fioiu8.devinfo.ui.CustomMiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val colorSpec: com.fioiu8.devinfo.core.model.ColorSpec,
    val onColorSpecChange: (com.fioiu8.devinfo.core.model.ColorSpec) -> Unit,
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val releaseInfo by viewModel.releaseInfo.collectAsStateWithLifecycle()

    // 首帧渲染后再启动数据加载，避免阻塞首屏
    LaunchedEffect(Unit) {
        viewModel.onFirstComposition()
    }

    val navController = rememberNavController<MainRoute>(MainRoute.Overview)
    val navigator = remember(navController) { MainNavigator(navController) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showExportSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var exportedFileUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var showDownloadConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showRootRequiredDialog by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val useNavigationRail = configuration.screenWidthDp >= TABLET_NAVIGATION_RAIL_MIN_WIDTH_DP
    val navigationRailStartInsets =
        WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Start)
    val alreadyLatestMessage = stringResource(R.string.already_latest)
    val exportFailedLabel = stringResource(R.string.export_failed)
    val cannotOpenFileMessage = stringResource(R.string.cannot_open_file)
    val exportPartialFileWarning = stringResource(R.string.export_failed_partial_file)
    val exportFileName = remember { ModuleExportHelper.createExportFileName(android.os.Build.MODEL) }
    val materialSnackbarHostState = remember { SnackbarHostState() }
    val miuixSnackbarHostState = remember { MiuixSnackbarHostState() }
    val showMessage = rememberDevInfoMessageHandler(
        materialHostState = materialSnackbarHostState,
        miuixHostState = miuixSnackbarHostState,
    )

    // SAF 导出 — 用户选择保存位置后将 ZIP 写入 ContentResolver 提供的输出流。
    //
    // 失败时不删除目标文档：CreateDocument 在用户选择已存在文件并确认覆盖时返回的
    // 正是原文件 URI，删除它会连同用户原有内容一起丢失，且 SAF 没有回收站可以恢复。
    // 因此失败只做提示，并告知保存位置可能残留不完整文件。
    val saveExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult // 用户取消
        fun reportExportFailure(reason: String) {
            showMessage("$exportFailedLabel: $reason. $exportPartialFileWarning")
        }
        scope.launch(Dispatchers.IO) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    reportExportFailure(cannotOpenFileMessage)
                    return@launch
                }
                outputStream.use { stream ->
                    exportHelper.exportModuleToStream(
                        deviceId = settings.deviceId,
                        itemsState = uiState.deviceInfoItems,
                        outputStream = stream,
                        policy = com.fioiu8.devinfo.core.model.ModuleExportPolicy.MINIMAL,
                        onSuccess = {
                            scope.launch {
                                exportedFileUri = uri
                                showExportSuccessDialog = true
                            }
                        },
                        onError = { error -> reportExportFailure(error) }
                    )
                }
            } catch (e: Exception) {
                reportExportFailure(e.message ?: exportFailedLabel)
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

    LaunchedEffect(navigator.isOverviewVisible, viewModel) {
        viewModel.onOverviewVisibilityChanged(navigator.isOverviewVisible)
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            UpdateState.UP_TO_DATE -> {
                // “已是最新版本”提示仅在用户手动触发检查时弹出，
                // 避免每次冷启动自动检查都打扰
                if (viewModel.consumeUserInitiatedUpdateCheck()) {
                    showMessage(alreadyLatestMessage)
                }
                viewModel.resetUpdateState()
            }

            UpdateState.NEW_VERSION_AVAILABLE,
            UpdateState.ERROR -> showUpdateDialog = true

            else -> Unit
        }
    }

    fun onRootFabClick(
        rootEnabledMsg: String,
        rootFailedMsg: String,
    ) {
        if (uiState.isRootModeEnabled) return
        scope.launch {
            val hasRoot = withContext(Dispatchers.IO) { viewModel.checkRootAvailable() }
            if (!hasRoot) {
                showRootRequiredDialog = true
                return@launch
            }
            val success = withContext(Dispatchers.IO) { viewModel.enableRootMode() }
            showMessage(if (success) rootEnabledMsg else rootFailedMsg)
        }
    }

    val navInfoLabel = stringResource(R.string.nav_info)
    val navSettingsLabel = stringResource(R.string.nav_settings)
    val navigationItems = remember(navInfoLabel, navSettingsLabel) {
        listOf(
            MainNavigationItem(
                label = navInfoLabel,
                selectedIcon = Icons.Filled.Description,
                unselectedIcon = Icons.Outlined.Description
            ),
            MainNavigationItem(
                label = navSettingsLabel,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings
            )
        )
    }

    val selectedIndex = navigator.selectedTabIndex
    val rootEnabledMsg = stringResource(R.string.root_mode_enabled)
    val rootFailedMsg = stringResource(R.string.root_mode_failed)

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
                            onItemSelected = navigator::selectTab,
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }

                    MainNavigationHost(
                        modifier = Modifier.weight(1f),
                        consumedStartInsets = if (useNavigationRail) {
                            navigationRailStartInsets
                        } else {
                            WindowInsets(0, 0, 0, 0)
                        },
                        navController = navController,
                        navigator = navigator,
                        viewModel = viewModel,
                        uiState = uiState,
                        settings = settings,
                        items = navigationItems,
                        showBottomBar = !useNavigationRail,
                        floatingNavigationContentPadding = floatingNavigationContentPadding,
                        rootFabBottomPadding = snackbarBottomPadding,
                        onExportClick = { showExportDialog = true },
                        onRootFabClick = {
                            onRootFabClick(rootEnabledMsg, rootFailedMsg)
                        },
                        enablePredictiveBack = settings.enablePredictiveBack,
                    )
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
    }

    UpdateAvailableDialog(
        show = showUpdateDialog,
        info = releaseInfo,
        isError = updateState == UpdateState.ERROR,
        currentVersion = BuildConfig.VERSION_NAME,
        onDownload = {
            // 关闭更新提示，弹出二级确认；用户需在确认对话框中再次点击才真正跳转浏览器
            showUpdateDialog = false
            showDownloadConfirmDialog = true
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

    RootRequiredDialog(
        show = showRootRequiredDialog,
        onDismiss = { showRootRequiredDialog = false },
        onConfirm = {
            showRootRequiredDialog = false
            scope.launch {
                val success = withContext(Dispatchers.IO) { viewModel.enableRootMode() }
                showMessage(if (success) rootEnabledMsg else rootFailedMsg)
            }
        }
    )

    // 下载二次确认：用户必须在弹出对话框中再次点击"确认下载"才会跳转浏览器。
    // 这样设计是为了避免单次点击立即触发离开应用的行为（用户的实际意图可能是查看 release notes）。
    DownloadConfirmDialog(
        show = showDownloadConfirmDialog,
        onConfirm = {
            showDownloadConfirmDialog = false
            openUrl(
                context = context,
                url = releaseInfo?.htmlUrl ?: GitHubClient.RELEASES_URL,
                onFailure = showMessage,
            )
            viewModel.resetUpdateState()
        },
        onDismiss = { showDownloadConfirmDialog = false },
    )

    ExportConfirmDialog(
        show = showExportDialog,
        fileName = exportFileName,
        onConfirm = {
            showExportDialog = false
            saveExportLauncher.launch(exportFileName)
        },
        onDismiss = { showExportDialog = false }
    )

    ExportSuccessDialog(
        show = showExportSuccessDialog,
        fileUri = exportedFileUri,
        onDismiss = { showExportSuccessDialog = false }
    )
}

@Composable
private fun MainNavigationHost(
    modifier: Modifier,
    consumedStartInsets: WindowInsets,
    navController: NavController,
    navigator: MainNavigator,
    viewModel: MainViewModel,
    uiState: MainViewModel.MainUiState,
    settings: MainScreenSettings,
    items: List<MainNavigationItem>,
    showBottomBar: Boolean,
    floatingNavigationContentPadding: androidx.compose.ui.unit.Dp,
    rootFabBottomPadding: androidx.compose.ui.unit.Dp,
    onExportClick: () -> Unit,
    onRootFabClick: () -> Unit,
    enablePredictiveBack: Boolean,
) {
    val hostModifier = modifier
        .fillMaxHeight()
        .consumeWindowInsets(consumedStartInsets)
        .clipToBounds()
    val hostBackground = when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> MaterialTheme.colorScheme.background
        UiStyle.MIUIX -> MiuixTheme.colorScheme.background
    }

    if (enablePredictiveBack) {
        NavDisplay(
            navController = navController,
            modifier = hostModifier,
            onBack = { navigator.pop() },
            transition = NavTransitions.MiuixDefault,
            effects = NavDisplayEffects(
                dimAmount = 0f,
                blockInputDuringTransition = true,
                backdropColor = hostBackground,
            ),
        ) {
            entry<MainRoute.Overview>(swipeDismiss = NavSwipeDirection.None) {
                MainRouteContent(
                    route = MainRoute.Overview,
                    navigator = navigator,
                    viewModel = viewModel,
                    uiState = uiState,
                    settings = settings,
                    items = items,
                    showBottomBar = showBottomBar,
                    floatingNavigationContentPadding = floatingNavigationContentPadding,
                    rootFabBottomPadding = rootFabBottomPadding,
                    onExportClick = onExportClick,
                    onRootFabClick = onRootFabClick,
                )
            }
            entry<MainRoute.Settings>(swipeDismiss = NavSwipeDirection.None) {
                MainRouteContent(
                    route = MainRoute.Settings,
                    navigator = navigator,
                    viewModel = viewModel,
                    uiState = uiState,
                    settings = settings,
                    items = items,
                    showBottomBar = showBottomBar,
                    floatingNavigationContentPadding = floatingNavigationContentPadding,
                    rootFabBottomPadding = rootFabBottomPadding,
                    onExportClick = onExportClick,
                    onRootFabClick = onRootFabClick,
                )
            }
            entry<MainRoute.Details>(swipeDismiss = NavSwipeDirection.None) { route ->
                MainRouteContent(
                    route = route,
                    navigator = navigator,
                    viewModel = viewModel,
                    uiState = uiState,
                    settings = settings,
                    items = items,
                    showBottomBar = showBottomBar,
                    floatingNavigationContentPadding = floatingNavigationContentPadding,
                    rootFabBottomPadding = rootFabBottomPadding,
                    onExportClick = onExportClick,
                    onRootFabClick = onRootFabClick,
                )
            }
            entry<MainRoute.ThemeSettings>(swipeDismiss = NavSwipeDirection.None) {
                MainRouteContent(
                    route = MainRoute.ThemeSettings,
                    navigator = navigator,
                    viewModel = viewModel,
                    uiState = uiState,
                    settings = settings,
                    items = items,
                    showBottomBar = showBottomBar,
                    floatingNavigationContentPadding = floatingNavigationContentPadding,
                    rootFabBottomPadding = rootFabBottomPadding,
                    onExportClick = onExportClick,
                    onRootFabClick = onRootFabClick,
                )
            }
            entry<MainRoute.About>(swipeDismiss = NavSwipeDirection.None) {
                MainRouteContent(
                    route = MainRoute.About,
                    navigator = navigator,
                    viewModel = viewModel,
                    uiState = uiState,
                    settings = settings,
                    items = items,
                    showBottomBar = showBottomBar,
                    floatingNavigationContentPadding = floatingNavigationContentPadding,
                    rootFabBottomPadding = rootFabBottomPadding,
                    onExportClick = onExportClick,
                    onRootFabClick = onRootFabClick,
                )
            }
        }
    } else {
        BackHandler(enabled = navController.backStack.size > 1) {
            navigator.pop()
        }
        val stateHolder = rememberSaveableStateHolder()
        val currentRoute = navigator.currentRoute
        Box(modifier = hostModifier) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = { mainRouteTransition(initialState, targetState) },
                label = "mainNavigationTransition",
            ) { route ->
                stateHolder.SaveableStateProvider(route) {
                    MainRouteContent(
                        route = route,
                        navigator = navigator,
                        viewModel = viewModel,
                        uiState = uiState,
                        settings = settings,
                        items = items,
                        showBottomBar = showBottomBar,
                        floatingNavigationContentPadding = floatingNavigationContentPadding,
                        rootFabBottomPadding = rootFabBottomPadding,
                        onExportClick = onExportClick,
                        onRootFabClick = onRootFabClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainRouteContent(
    route: MainRoute,
    navigator: MainNavigator,
    viewModel: MainViewModel,
    uiState: MainViewModel.MainUiState,
    settings: MainScreenSettings,
    items: List<MainNavigationItem>,
    showBottomBar: Boolean,
    floatingNavigationContentPadding: androidx.compose.ui.unit.Dp,
    rootFabBottomPadding: androidx.compose.ui.unit.Dp,
    onExportClick: () -> Unit,
    onRootFabClick: () -> Unit,
) {
    when (route) {
        MainRoute.Overview -> MainAppScaffold(
            title = stringResource(R.string.overview_title),
            showBackButton = false,
            onBack = {},
            items = items,
            selectedIndex = route.rootTabIndex(),
            showBottomBar = showBottomBar,
            enableBlur = settings.enableBlur,
            enableFloatingBottomBar = settings.enableFloatingBottomBar,
            enableFloatingBottomBarBlur = settings.enableFloatingBottomBarBlur,
            floatingNavigationContentPadding = floatingNavigationContentPadding,
            showRootFab = false,
            isRootModeEnabled = uiState.isRootModeEnabled,
            rootFabBottomPadding = rootFabBottomPadding,
            onRootFabClick = onRootFabClick,
            onItemSelected = navigator::selectTab,
        ) {
            DeviceInfoOverviewPage(
                itemsState = uiState.deviceInfoItems,
                isLoading = uiState.isDeviceInfoLoading,
                isOverviewLoading = uiState.isOverviewLoading,
                snapshot = uiState.overviewSnapshot,
                onRefresh = { viewModel.refreshAndAwait() },
                onOpenDetails = navigator::openDetails,
            )
        }

        MainRoute.Settings -> {
            val languageOptions = AppLanguage.entries.map { language ->
                stringResource(language.displayNameResId)
            }
            MainAppScaffold(
                title = stringResource(R.string.title_settings),
                showBackButton = false,
                onBack = {},
                items = items,
                selectedIndex = route.rootTabIndex(),
                showBottomBar = showBottomBar,
                enableBlur = settings.enableBlur,
                enableFloatingBottomBar = settings.enableFloatingBottomBar,
                enableFloatingBottomBarBlur = settings.enableFloatingBottomBarBlur,
                floatingNavigationContentPadding = floatingNavigationContentPadding,
                showRootFab = true,
                isRootModeEnabled = uiState.isRootModeEnabled,
                rootFabBottomPadding = rootFabBottomPadding,
                onRootFabClick = onRootFabClick,
                onItemSelected = navigator::selectTab,
            ) {
                SettingsPage(
                    versionName = viewModel.appVersionName,
                    versionCode = viewModel.appVersionCode,
                    uiStyle = settings.uiStyle,
                    onUiStyleChange = settings.onUiStyleChange,
                    onThemeSettingsClick = navigator::openThemeSettings,
                    onExportClick = onExportClick,
                    onAboutClick = navigator::openAbout,
                    appLanguage = settings.appLanguage,
                    checkUpdate = settings.checkUpdate,
                    onCheckUpdateChange = settings.onCheckUpdateChange,
                    languageOptions = languageOptions,
                    onLanguageChange = { index ->
                        settings.onAppLanguageChange(AppLanguage.entries[index])
                    },
                    customLocaleTag = settings.customLocaleTag,
                    onCustomLocaleTagChange = settings.onCustomLocaleTagChange,
                )
            }
        }

        is MainRoute.Details -> MainAppScaffold(
            title = stringResource(R.string.title_device_details),
            showBackButton = true,
            onBack = { navigator.pop() },
            items = items,
            selectedIndex = route.rootTabIndex(),
            showBottomBar = showBottomBar,
            enableBlur = settings.enableBlur,
            enableFloatingBottomBar = settings.enableFloatingBottomBar,
            enableFloatingBottomBarBlur = settings.enableFloatingBottomBarBlur,
            floatingNavigationContentPadding = floatingNavigationContentPadding,
            showRootFab = false,
            isRootModeEnabled = uiState.isRootModeEnabled,
            rootFabBottomPadding = rootFabBottomPadding,
            onRootFabClick = onRootFabClick,
            onItemSelected = navigator::selectTab,
        ) {
            DeviceInfoPage(
                deviceId = settings.deviceId,
                itemsState = uiState.deviceInfoItems,
                isLoading = uiState.isDeviceInfoLoading,
                overviewSnapshot = uiState.overviewSnapshot,
                onRefresh = { viewModel.refreshAndAwait() },
                initialCategory = navigator.detailsCategory(route),
            )
        }

        MainRoute.ThemeSettings -> ThemeSettingsPage(
            uiStyle = settings.uiStyle,
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
            onBack = { navigator.pop() },
        )

        MainRoute.About -> AboutScreen(
            versionName = viewModel.appVersionName,
            onBack = { navigator.pop() },
        )
    }
}

@Composable
private fun MainAppScaffold(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    showBottomBar: Boolean,
    enableBlur: Boolean,
    enableFloatingBottomBar: Boolean,
    enableFloatingBottomBarBlur: Boolean,
    floatingNavigationContentPadding: androidx.compose.ui.unit.Dp,
    showRootFab: Boolean,
    isRootModeEnabled: Boolean,
    rootFabBottomPadding: androidx.compose.ui.unit.Dp,
    onRootFabClick: () -> Unit,
    onItemSelected: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        MainScaffold(
            modifier = Modifier.fillMaxSize(),
            consumedStartInsets = WindowInsets(0, 0, 0, 0),
            title = title,
            showBackButton = showBackButton,
            onBack = onBack,
            items = items,
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected,
            showBottomBar = showBottomBar,
            enableBlur = enableBlur,
            enableFloatingBottomBar = enableFloatingBottomBar,
            enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                CompositionLocalProvider(
                    LocalFloatingNavigationContentPadding provides floatingNavigationContentPadding,
                ) {
                    content()
                }
            }
        }

        if (showRootFab) {
            RootModeFab(
                isRootModeEnabled = isRootModeEnabled,
                onClick = onRootFabClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp)
                    .padding(bottom = rootFabBottomPadding + 56.dp),
            )
        }
    }
}

private fun mainRouteTransition(
    initialState: MainRoute,
    targetState: MainRoute,
): ContentTransform {
    val initialDepth = initialState.navigationDepth()
    val targetDepth = targetState.navigationDepth()
    val direction = when {
        targetDepth > initialDepth -> FORWARD_DIRECTION
        targetDepth < initialDepth -> BACKWARD_DIRECTION
        targetState == MainRoute.Settings && initialState == MainRoute.Overview -> FORWARD_DIRECTION
        targetState == MainRoute.Overview && initialState == MainRoute.Settings -> BACKWARD_DIRECTION
        else -> FORWARD_DIRECTION
    }
    return (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
        slideInHorizontally(
            animationSpec = tween(320, easing = FastOutSlowInEasing),
        ) { direction * it })
        .togetherWith(
            fadeOut(tween(180, easing = LinearOutSlowInEasing)) +
                slideOutHorizontally(
                    animationSpec = tween(240, easing = LinearOutSlowInEasing),
                ) { -direction * it / 4 },
        )
}

private typealias MainNavigationItem = DevInfoNavigationItem

@Composable
private fun MainRootScaffold(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL3 -> {
            Box(modifier = modifier) {
                content()
            }
        }

        UiStyle.MIUIX -> {
            MiuixScaffold(
                modifier = modifier,
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
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
            if (LocalUiStyle.current == UiStyle.MIUIX) {
                val surfaceColor = MiuixTheme.colorScheme.surface
                val blurBackdrop = rememberBlurBackdrop(
                    enableBlur = enableBlur,
                    surfaceColor = surfaceColor,
                )
                BlurredBar(
                    backdrop = blurBackdrop,
                    blurActive = enableBlur,
                    surfaceColor = surfaceColor,
                ) {
                    DevInfoNavigationBar(
                        items = items,
                        selectedIndex = selectedIndex,
                        onItemSelected = onItemSelected,
                    )
                }
            } else {
                DevInfoNavigationBar(
                    items = items,
                    selectedIndex = selectedIndex,
                    onItemSelected = onItemSelected,
                )
            }
        }
    }

    Box(modifier = modifier.consumeWindowInsets(consumedStartInsets)) {
        when (LocalUiStyle.current) {
            UiStyle.MATERIAL3 -> {
                // Material3：保留原始 Scaffold + bottomBar slot，不干涉，保留原始悬浮样式
                if (enableFloatingBottomBar) {
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
                    val blurBackdrop = rememberBlurBackdrop(
                        enableBlur = enableBlur || enableFloatingBottomBarBlur,
                        surfaceColor = surfaceColor,
                    )
                    val kitBackdrop: Backdrop = blurBackdrop ?: rememberLayerBackdrop {
                        drawRect(surfaceColor)
                        drawContent()
                    }
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
                                backdrop = kitBackdrop,
                                blurBackdrop = blurBackdrop,
                                enableBlur = enableFloatingBottomBarBlur,
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
                        enableBlur = enableBlur || enableFloatingBottomBarBlur,
                        surfaceColor = surfaceColor,
                    )
                    val kitBackdrop: Backdrop = blurBackdrop ?: rememberLayerBackdrop {
                        drawRect(surfaceColor)
                        drawContent()
                    }
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
                                backdrop = kitBackdrop,
                                blurBackdrop = blurBackdrop,
                                enableBlur = enableFloatingBottomBarBlur,
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
    backdrop: Backdrop,
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
        when (LocalUiStyle.current) {
            UiStyle.MATERIAL3 -> MaterialKitFloatingNavigationBar(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = onItemSelected,
                backdrop = backdrop,
                glassEffect = enableBlur,
                blurBackdrop = blurBackdrop,
            )

            UiStyle.MIUIX -> MiuixKitFloatingNavigationBar(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = onItemSelected,
                backdrop = backdrop,
                glassEffect = enableBlur,
                blurBackdrop = blurBackdrop,
            )
        }
    }
}

@Composable
private fun BoxScope.MaterialKitFloatingNavigationBar(
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    backdrop: Backdrop,
    glassEffect: Boolean,
    blurBackdrop: LayerBackdrop?,
) {
    val selectedIndexState = rememberUpdatedState(selectedIndex)
    val selectedIndexProvider = remember { { selectedIndexState.value } }

    // 底部定位与安全区留白由外层 FloatingMainNavigationBar 统一处理，
    // 此处再补一次会导致浮动栏悬浮过高（双重 12.dp + 双重导航栏 inset）
    FloatingBottomBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        selectedIndex = selectedIndexProvider,
        onSelected = onItemSelected,
        backdrop = backdrop,
        tabsCount = items.size,
        isBlurEnabled = glassEffect && blurBackdrop != null,
        surfaceColorOverride = MaterialTheme.colorScheme.surfaceContainer,
        accentColorOverride = MaterialTheme.colorScheme.primary,
        darkThemeOverride = isInDarkTheme(),
    ) {
        items.forEachIndexed { index, item ->
            FloatingBottomBarItem(
                modifier = Modifier.defaultMinSize(minWidth = 92.dp),
                onClick = { onItemSelected(index) },
            ) {
                Icon(
                    imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MiuixKitFloatingNavigationBar(
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    backdrop: Backdrop,
    glassEffect: Boolean,
    blurBackdrop: LayerBackdrop?,
) {
    val selectedIndexState = rememberUpdatedState(selectedIndex)
    val selectedIndexProvider = remember { { selectedIndexState.value } }

    // 同 MaterialKitFloatingNavigationBar：底部留白由外层统一处理
    FloatingBottomBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        selectedIndex = selectedIndexProvider,
        onSelected = onItemSelected,
        backdrop = backdrop,
        tabsCount = items.size,
        isBlurEnabled = glassEffect && blurBackdrop != null,
    ) {
        items.forEachIndexed { index, item ->
            FloatingBottomBarItem(
                modifier = Modifier.defaultMinSize(minWidth = 92.dp),
                onClick = { onItemSelected(index) },
            ) {
                MiuixIcon(
                    imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = MiuixTheme.colorScheme.onSurface,
                )
                MiuixText(
                    text = item.label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        }
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
    val navigationIcon: @Composable () -> Unit = {
        if (showBackButton) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        }
    }
    val titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
        )
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )

    TopAppBar(
        navigationIcon = navigationIcon,
        title = titleContent,
        colors = colors,
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
                        imageVector = CustomMiuixIcons.Back,
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

@Composable
private fun BoxScope.RootModeFab(
    isRootModeEnabled: Boolean,
    onClick: () -> Unit,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = if (isRootModeEnabled) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.surfaceVariant
            },
        ) {
            MiuixIcon(
                imageVector = Icons.Outlined.Build,
                contentDescription = stringResource(R.string.root_fab_desc),
            )
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = if (isRootModeEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = stringResource(R.string.root_fab_desc),
                tint = if (isRootModeEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun RootRequiredDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!show) return
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        WindowDialog(
            show = show,
            title = stringResource(R.string.root_required_title),
            onDismissRequest = onDismiss,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MiuixText(text = stringResource(R.string.root_required_message))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    MiuixTextButton(text = stringResource(R.string.cancel), onClick = onDismiss)
                    MiuixTextButton(
                        text = stringResource(R.string.root_fab_confirm),
                        onClick = onConfirm,
                    )
                }
            }
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.root_required_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.root_required_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.root_fab_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun openUrl(
    context: Context,
    url: String,
    onFailure: (String) -> Unit,
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        onFailure(context.getString(R.string.cannot_open_link))
    }
}

private const val FORWARD_DIRECTION = 1
private const val BACKWARD_DIRECTION = -1
private const val TABLET_NAVIGATION_RAIL_MIN_WIDTH_DP = 600

private val UriSaver = Saver<Uri?, String>(
    save = { it?.toString() },
    restore = { uri -> android.net.Uri.parse(uri) }
)
