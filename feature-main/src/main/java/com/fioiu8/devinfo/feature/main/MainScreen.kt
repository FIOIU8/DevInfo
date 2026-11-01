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
import com.fioiu8.devinfo.feature.main.R
import com.fioiu8.devinfo.ui.DevInfoNavigationItem
import com.fioiu8.devinfo.ui.DevInfoSnackbarHost
import com.fioiu8.devinfo.ui.TestVersionWarningCard
import com.fioiu8.devinfo.ui.rememberDevInfoMessageHandler

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
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
import com.fioiu8.devinfo.core.root.RootAccess
import com.fioiu8.devinfo.data.AppLanguage
import com.fioiu8.devinfo.core.model.PaletteStyle
import com.fioiu8.devinfo.core.model.ThemeColor
import com.fioiu8.devinfo.core.model.ThemeMode
import com.fioiu8.devinfo.feature.main.screen.about.AboutScreen
import com.fioiu8.devinfo.feature.main.screen.roothelp.RootHelpScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** App-owned settings and callbacks required by the main UI. */
data class MainScreenSettings(
    val themeMode: ThemeMode,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val themeColor: ThemeColor,
    val onThemeColorChange: (ThemeColor) -> Unit,
    val checkUpdate: Boolean,
    val onCheckUpdateChange: (Boolean) -> Unit,
    val paletteStyle: PaletteStyle,
    val onPaletteStyleChange: (PaletteStyle) -> Unit,
    val colorSpec: com.fioiu8.devinfo.core.model.ColorSpec,
    val onColorSpecChange: (com.fioiu8.devinfo.core.model.ColorSpec) -> Unit,
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

    val routeStack = rememberSaveable(saver = MainRouteStackSaver) {
        mutableStateListOf(MainRoute.Overview)
    }
    val navigator = remember(routeStack) { MainNavigator(routeStack) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showExportSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var exportedFileUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var showDownloadConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showRootRequiredDialog by rememberSaveable { mutableStateOf(false) }

    // Root 授权的瞬态状态：它们只对「本次请求」有意义，进程或配置重建后重新发起比恢复更真实，
    // 因此用 remember 而不是 rememberSaveable（重建会取消请求，弹窗随之消失）。
    var rootRequestJob by remember { mutableStateOf<Job?>(null) }
    var rootRequestInFlight by remember { mutableStateOf(false) }
    var showRootLoadingDialog by remember { mutableStateOf(false) }
    var rootFailure by remember { mutableStateOf<RootAccess?>(null) }

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
    val showMessage = rememberDevInfoMessageHandler(
        materialHostState = materialSnackbarHostState,
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
                        itemsState = uiState.deviceInfoItems,
                        outputStream = stream,
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
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
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

    // Root 授权流程：FAB 只负责「先告知」，真正的请求由 startRootRequest 发起。
    // 失败弹窗的「重试」也直接走 startRootRequest，因此不会重复展示风险告知——用户在本轮流程里
    // 已经确认过一次。次序决策见交接文档 §4 #1（选「先告知再申请」）。
    fun startRootRequest() {
        if (rootRequestInFlight) return
        rootFailure = null
        rootRequestJob?.cancel()
        rootRequestJob = scope.launch {
            rootRequestInFlight = true
            try {
                val access = viewModel.requestRootMode()
                if (access == RootAccess.Granted) {
                    showMessage(rootEnabledMsg)
                } else {
                    rootFailure = access
                }
            } finally {
                rootRequestInFlight = false
            }
        }
    }

    fun onRootFabClick() {
        if (uiState.isRootModeEnabled) return
        showRootRequiredDialog = true
    }

    // 延迟显示加载弹窗：快路径（最坏 291 ms）不该闪一个又立刻消失的弹窗，依据见常量 KDoc。
    LaunchedEffect(rootRequestInFlight) {
        if (!rootRequestInFlight) {
            showRootLoadingDialog = false
            return@LaunchedEffect
        }
        delay(ROOT_LOADING_DIALOG_DELAY_MS)
        showRootLoadingDialog = true
    }

    DevInfoFeedbackScope(
        materialHostState = materialSnackbarHostState,
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
                        navigator = navigator,
                        viewModel = viewModel,
                        uiState = uiState,
                        settings = settings,
                        rootFabBottomPadding = snackbarBottomPadding,
                        onExportClick = { showExportDialog = true },
                        onRootFabClick = { onRootFabClick() },
                        enablePredictiveBack = settings.enablePredictiveBack,
                    )
                }
            }

            DevInfoSnackbarHost(
                materialHostState = materialSnackbarHostState,
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

    // 风险告知只负责「同意后才发起请求」：实际请求交给 startRootRequest。
    RootRequiredDialog(
        show = showRootRequiredDialog,
        onDismiss = { showRootRequiredDialog = false },
        onConfirm = {
            showRootRequiredDialog = false
            startRootRequest()
        }
    )

    // 加载弹窗与失败弹窗互斥：请求仍在进行时只可能是加载弹窗，结束后才轮到失败弹窗。
    RootRequestLoadingDialog(
        show = showRootLoadingDialog && rootRequestInFlight,
        onCancel = { rootRequestJob?.cancel() },
    )

    // 取消请求不是失败：协程被取消后 rootFailure 从未被赋值，所以这里不会弹失败弹窗。
    rootFailure?.let { failure ->
        if (!rootRequestInFlight) {
            RootRequestFailureDialog(
                access = failure,
                onRetry = { startRootRequest() },
                onOpenHelp = {
                    rootFailure = null
                    navigator.openRootHelp()
                },
                onDismiss = { rootFailure = null },
            )
        }
    }

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
    navigator: MainNavigator,
    viewModel: MainViewModel,
    uiState: MainViewModel.MainUiState,
    settings: MainScreenSettings,
    rootFabBottomPadding: androidx.compose.ui.unit.Dp,
    onExportClick: () -> Unit,
    onRootFabClick: () -> Unit,
    enablePredictiveBack: Boolean,
) {
    val hostModifier = modifier
        .fillMaxHeight()
        .consumeWindowInsets(consumedStartInsets)
        .background(MaterialTheme.colorScheme.background)
        .clipToBounds()
    // 所有路由入口渲染的是同一个内容函数，只有路由不同。此前把这一串参数在每个
    // entry 里各抄一遍，任何一处漏改都会让某个页面拿到过期的状态。
    val routeContent: @Composable (MainRoute) -> Unit = { route ->
        MainRouteContent(
            route = route,
            navigator = navigator,
            viewModel = viewModel,
            uiState = uiState,
            settings = settings,
            rootFabBottomPadding = rootFabBottomPadding,
            onExportClick = onExportClick,
            onRootFabClick = onRootFabClick,
        )
    }

    val canGoBack = navigator.backStack.size > 1
    if (enablePredictiveBack) {
        PredictiveBackHandler(enabled = canGoBack) { events ->
            events.collect { }
            navigator.pop()
        }
    } else {
        BackHandler(enabled = canGoBack) {
            navigator.pop()
        }
    }

    val stateHolder = rememberSaveableStateHolder()
    Box(modifier = hostModifier) {
        AnimatedContent(
            targetState = navigator.currentRoute,
            transitionSpec = { mainRouteTransition(initialState, targetState) },
            label = "mainNavigationTransition",
        ) { route ->
            // SaveableStateHolder keys must be Bundle-compatible on Android.
            stateHolder.SaveableStateProvider(route.toSavedKey()) {
                routeContent(route)
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
    rootFabBottomPadding: androidx.compose.ui.unit.Dp,
    onExportClick: () -> Unit,
    onRootFabClick: () -> Unit,
) {
    when (route) {
        MainRoute.Overview -> MainAppScaffold(
            title = stringResource(R.string.overview_title),
            showBackButton = false,
            onBack = {},
            onSettingsClick = navigator::openSettings,
            showRootFab = false,
            isRootModeEnabled = uiState.isRootModeEnabled,
            rootFabBottomPadding = rootFabBottomPadding,
            onRootFabClick = onRootFabClick,
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
                showBackButton = true,
                onBack = { if (!navigator.pop()) navigator.selectTab(INFO_TAB_INDEX) },
                showRootFab = false,
                isRootModeEnabled = uiState.isRootModeEnabled,
                rootFabBottomPadding = rootFabBottomPadding,
                onRootFabClick = onRootFabClick,
            ) {
                SettingsPage(
                    versionName = viewModel.appVersionName,
                    versionCode = viewModel.appVersionCode,
                    onThemeSettingsClick = navigator::openThemeSettings,
                    onExportClick = onExportClick,
                    onAboutClick = navigator::openAbout,
                    onRootHelpClick = navigator::openRootHelp,
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
            onSettingsClick = navigator::openSettings,
            showRootFab = false,
            isRootModeEnabled = uiState.isRootModeEnabled,
            rootFabBottomPadding = rootFabBottomPadding,
            onRootFabClick = onRootFabClick,
        ) {
            HardwareDetailsPage(
                itemsState = uiState.deviceInfoItems,
                isLoading = uiState.isDeviceInfoLoading,
                overviewSnapshot = uiState.overviewSnapshot,
                hardwareDetails = uiState.hardwareDetails,
                motionSensors = uiState.motionSensors,
                onRefresh = { viewModel.refreshAndAwait() },
                initialPage = navigator.detailsPage(route),
                onSensorPageVisibilityChanged = viewModel::onSensorsPageVisibilityChanged,
            )
        }

        MainRoute.ThemeSettings -> ThemeSettingsPage(
            themeMode = settings.themeMode,
            onThemeModeChange = settings.onThemeModeChange,
            themeColor = settings.themeColor,
            onThemeColorChange = settings.onThemeColorChange,
            paletteStyle = settings.paletteStyle,
            onPaletteStyleChange = settings.onPaletteStyleChange,
            colorSpec = settings.colorSpec,
            onColorSpecChange = settings.onColorSpecChange,
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

        MainRoute.RootHelp -> RootHelpScreen(
            onBack = { navigator.pop() },
        )
    }
}

@Composable
private fun MainAppScaffold(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    showRootFab: Boolean,
    isRootModeEnabled: Boolean,
    rootFabBottomPadding: androidx.compose.ui.unit.Dp,
    onRootFabClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        MainScaffold(
            modifier = Modifier.fillMaxSize(),
            consumedStartInsets = WindowInsets(0, 0, 0, 0),
            title = title,
            showBackButton = showBackButton,
            onBack = onBack,
            onSettingsClick = onSettingsClick,
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                content()
            }
        }

        if (showRootFab) {
            RootModeFab(
                isRootModeEnabled = isRootModeEnabled,
                onClick = onRootFabClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp)
                    .padding(bottom = rootFabBottomPadding),
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
    Box(modifier = modifier) {
        content()
    }
}

@Composable
private fun MainScaffold(
    modifier: Modifier,
    consumedStartInsets: WindowInsets,
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = modifier.consumeWindowInsets(consumedStartInsets)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MainTopBar(
                    title = title,
                    showBackButton = showBackButton,
                    onBack = onBack,
                    onSettingsClick = onSettingsClick
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}

@Composable
private fun MainTopBar(
    title: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)?
) {
    Column {
        MaterialMainTopBar(
            title = title,
            showBackButton = showBackButton,
            onBack = onBack,
            onSettingsClick = onSettingsClick,
        )

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
    onBack: () -> Unit,
    onSettingsClick: (() -> Unit)?
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
        actions = {
            onSettingsClick?.let { openSettings ->
                IconButton(onClick = openSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.nav_settings)
                    )
                }
            }
        },
        colors = colors,
    )
}

@Composable
private fun MainNavigationRail(
    items: List<MainNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialMainNavigationRail(items, selectedIndex, onItemSelected, modifier)
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
private fun BoxScope.RootModeFab(
    isRootModeEnabled: Boolean,
    onClick: () -> Unit,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return
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

@Composable
private fun RootRequiredDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!show) return
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
