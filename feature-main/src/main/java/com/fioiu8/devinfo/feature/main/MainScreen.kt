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
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fioiu8.devinfo.feature.main.BuildConfig
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
import kotlinx.coroutines.CancellationException
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
import top.yukonga.miuix.kmp.window.WindowDialog
import com.fioiu8.devinfo.ui.CustomMiuixIcons
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

    var selectedIndex by rememberSaveable { mutableIntStateOf(INFO_TAB_INDEX) }
    var showDetailsPage by rememberSaveable { mutableStateOf(false) }
    var detailCategory by rememberSaveable { mutableStateOf(InfoCategory.DEVICE) }
    var showAboutPage by rememberSaveable { mutableStateOf(false) }
    var isAboutVisible by rememberSaveable { mutableStateOf(false) }
    var showThemeSettingsPage by rememberSaveable { mutableStateOf(false) }
    var isThemeSettingsVisible by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showExportSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var exportedFileUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var showRootRequiredDialog by rememberSaveable { mutableStateOf(false) }

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
    val cannotOpenFileMessage = stringResource(R.string.cannot_open_file)
    val exportFileName = remember { ModuleExportHelper.createExportFileName(android.os.Build.MODEL) }
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
        fun removeFailedExport() {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    exportHelper.exportModuleToStream(
                        deviceId = settings.deviceId,
                        itemsState = uiState.deviceInfoItems,
                        outputStream = outputStream,
                        policy = com.fioiu8.devinfo.core.model.ModuleExportPolicy.MINIMAL,
                        onSuccess = {
                            scope.launch {
                                exportedFileUri = uri
                                showExportSuccessDialog = true
                            }
                        },
                        onError = { error ->
                            removeFailedExport()
                            showMessage("$exportFailedLabel: $error")
                        }
                    )
                } ?: run {
                    removeFailedExport()
                    showMessage("$exportFailedLabel: $cannotOpenFileMessage")
                }
            } catch (e: Exception) {
                removeFailedExport()
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

    LaunchedEffect(selectedIndex, showDetailsPage, showAboutPage, showThemeSettingsPage, viewModel) {
        viewModel.onInfoTabChanged(
            selectedIndex == INFO_TAB_INDEX &&
                !showDetailsPage &&
                !showAboutPage &&
                !showThemeSettingsPage
        )
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

    fun selectNavigationItem(index: Int) {
        selectedIndex = index
        if (index == SETTINGS_TAB_INDEX) showDetailsPage = false
    }

    val contentPage = when {
        selectedIndex == SETTINGS_TAB_INDEX -> MainContentPage.SETTINGS
        showDetailsPage -> MainContentPage.DETAILS
        else -> MainContentPage.INFO
    }
    val topBarTitle = when (contentPage) {
        MainContentPage.INFO -> stringResource(R.string.overview_title)
        MainContentPage.DETAILS -> stringResource(R.string.title_device_details)
        MainContentPage.SETTINGS -> stringResource(R.string.title_settings)
    }
    val showTopBarBackButton = contentPage == MainContentPage.DETAILS

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
                            onItemSelected = ::selectNavigationItem,
                            modifier = Modifier.fillMaxHeight(),
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
                                .padding(paddingValues),
                        ) {
                            CompositionLocalProvider(
                                LocalFloatingNavigationContentPadding provides floatingNavigationContentPadding,
                            ) {
                                AnimatedContent(
                                    modifier = Modifier.offset {
                                        IntOffset(
                                            x = if (contentPage == MainContentPage.DETAILS) {
                                                detailOffsetX.value.roundToInt()
                                            } else {
                                                0
                                            },
                                            y = 0,
                                        )
                                    },
                                    targetState = contentPage,
                                    transitionSpec = {
                                        val direction = when {
                                            targetState == MainContentPage.DETAILS &&
                                                initialState != MainContentPage.DETAILS -> FORWARD_DIRECTION

                                            initialState == MainContentPage.DETAILS &&
                                                targetState != MainContentPage.DETAILS -> BACKWARD_DIRECTION

                                            targetState.navigationOrder > initialState.navigationOrder -> FORWARD_DIRECTION
                                            else -> BACKWARD_DIRECTION
                                        }
                                        (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                                            slideInHorizontally(
                                                animationSpec = tween(320, easing = FastOutSlowInEasing),
                                            ) { direction * it / 5 })
                                            .togetherWith(
                                                fadeOut(tween(180, easing = LinearOutSlowInEasing)) +
                                                    slideOutHorizontally(
                                                        animationSpec = tween(240, easing = LinearOutSlowInEasing),
                                                    ) { -direction * it / 5 },
                                            )
                                    },
                                    label = "mainNavigationTransition",
                                ) { pageState ->
                                    when (pageState) {
                                        MainContentPage.INFO -> {
                                            DeviceInfoOverviewPage(
                                                itemsState = uiState.deviceInfoItems,
                                                isLoading = uiState.isDeviceInfoLoading,
                                                isOverviewLoading = uiState.isOverviewLoading,
                                                snapshot = uiState.overviewSnapshot,
                                                onRefresh = { viewModel.refreshAndAwait() },
                                                onOpenDetails = { category ->
                                                    detailCategory = category
                                                    showDetailsPage = true
                                                },
                                            )
                                        }

                                        MainContentPage.DETAILS -> {
                                            DeviceInfoPage(
                                                deviceId = settings.deviceId,
                                                itemsState = uiState.deviceInfoItems,
                                                isLoading = uiState.isDeviceInfoLoading,
                                                overviewSnapshot = uiState.overviewSnapshot,
                                                onRefresh = { viewModel.refreshAndAwait() },
                                                initialCategory = detailCategory,
                                            )
                                        }

                                        MainContentPage.SETTINGS -> {
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
                                                onCustomLocaleTagChange = settings.onCustomLocaleTagChange,
                                            )
                                        }
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
                        .offset { IntOffset(aboutOffsetX.value.roundToInt(), 0) },
                ) {
                    AboutScreen(
                        versionName = viewModel.appVersionName,
                        onBack = { dismissAboutPage() },
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
                        onBack = ::dismissThemeSettingsPage,
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

            RootModeFab(
                isRootModeEnabled = uiState.isRootModeEnabled,
                onClick = { onRootFabClick(rootEnabledMsg, rootFailedMsg) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp)
                    .padding(bottom = snackbarBottomPadding + 56.dp),
                isVisible = selectedIndex == SETTINGS_TAB_INDEX && !showThemeSettingsPage && !showAboutPage && !showDetailsPage,
            )
        }
    }

    val hasDismissablePage = showThemeSettingsPage || showAboutPage || showDetailsPage

    BackHandler(
        enabled = hasDismissablePage && !settings.enablePredictiveBack,
    ) {
        when {
            showThemeSettingsPage -> dismissThemeSettingsPage()
            showAboutPage -> dismissAboutPage()
            showDetailsPage -> scope.launch {
                detailOffsetX.animateTo(
                    screenWidthPx,
                    animationSpec = tween(PREDICTIVE_BACK_ANIMATION_DURATION_MS),
                )
                showDetailsPage = false
            }
        }
    }

    PredictiveBackHandler(
        enabled = hasDismissablePage && settings.enablePredictiveBack,
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

private const val INFO_TAB_INDEX = 0
private const val SETTINGS_TAB_INDEX = 1
private const val MAIN_CONTENT_STATE_KEY = "main_content"
private const val FORWARD_DIRECTION = 1
private const val BACKWARD_DIRECTION = -1
private const val TABLET_NAVIGATION_RAIL_MIN_WIDTH_DP = 600
private const val ABOUT_ANIMATION_DURATION_MS = 300
private const val PREDICTIVE_BACK_ANIMATION_DURATION_MS = 200
private const val RELEASES_URL = "https://github.com/FIOIU8/DevInfo/releases"

private enum class MainContentPage(val navigationOrder: Int) {
    INFO(navigationOrder = 0),
    DETAILS(navigationOrder = 1),
    SETTINGS(navigationOrder = 1),
}

private val UriSaver = Saver<Uri?, String>(
    save = { it?.toString() },
    restore = { uri -> android.net.Uri.parse(uri) }
)
