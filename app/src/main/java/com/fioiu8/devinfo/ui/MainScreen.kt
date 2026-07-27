package com.fioiu8.devinfo.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.PredictiveBackHandler
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
import com.fioiu8.devinfo.model.ThemeColor
import com.fioiu8.devinfo.model.ThemeMode
import com.fioiu8.devinfo.model.UiStyle
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** App-owned settings and callbacks required by the main UI. */
data class MainScreenSettings(
    val deviceId: String,
    val themeMode: ThemeMode,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val themeColor: ThemeColor,
    val onThemeColorChange: (ThemeColor) -> Unit,
    val uiStyle: UiStyle,
    val onUiStyleChange: (UiStyle) -> Unit,
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
    var exportedFilePath by remember { mutableStateOf("") }
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
    val snackbarBottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            if (useNavigationRail) 16.dp else 92.dp

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
                showBottomBar = !useNavigationRail
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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

        if (isAboutVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(aboutOffsetX.value.roundToInt(), 0) }
            ) {
                AboutPage(
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
            scope.launch(Dispatchers.IO) {
                exportHelper.exportModule(
                    deviceId = settings.deviceId,
                    itemsState = deviceInfoItems,
                    onSuccess = { path ->
                        scope.launch {
                            exportedFilePath = path
                            showExportSuccessDialog = true
                        }
                    },
                    onError = { error ->
                        showMessage("$exportFailedLabel: $error")
                    }
                )
            }
        },
        onDismiss = { showExportDialog = false }
    )

    ExportSuccessDialog(
        show = showExportSuccessDialog,
        filePath = exportedFilePath,
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
    content: @Composable (PaddingValues) -> Unit
) {
    val topBar: @Composable () -> Unit = {
        MainTopBar(
            title = title,
            showBackButton = showBackButton,
            onBack = onBack
        )
    }
    val bottomBar: @Composable () -> Unit = {
        if (showBottomBar) {
            Box(modifier = Modifier.fillMaxWidth()) {
                val bottomNavigationModifier = when (LocalUiStyle.current) {
                    UiStyle.MATERIAL3 -> Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp)
                        .padding(
                            bottom = 12.dp +
                                WindowInsets.navigationBars
                                    .asPaddingValues()
                                    .calculateBottomPadding(),
                        )

                    UiStyle.MIUIX -> Modifier
                }
                DevInfoFloatingNavigationBar(
                    items = items,
                    selectedIndex = selectedIndex,
                    onItemSelected = onItemSelected,
                    modifier = bottomNavigationModifier,
                )
            }
        }
    }
    Box(modifier = modifier.consumeWindowInsets(consumedStartInsets)) {
        when (LocalUiStyle.current) {
            UiStyle.MATERIAL3 -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = topBar,
                    bottomBar = bottomBar,
                    containerColor = MaterialTheme.colorScheme.background,
                    content = content,
                )
            }

            UiStyle.MIUIX -> {
                MiuixScaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = topBar,
                    bottomBar = bottomBar,
                    popupHost = {},
                    content = content,
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
