package com.fioiu8.devinfo.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.fioiu8.devinfo.BuildConfig
import com.fioiu8.devinfo.DeviceInfoCollector
import com.fioiu8.devinfo.ModuleExportHelper
import com.fioiu8.devinfo.UpdateChecker
import com.fioiu8.devinfo.UpdateState
import com.fioiu8.devinfo.model.ItemWithVisibility
import com.fioiu8.devinfo.model.MountThemeColor
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.AppLanguage
import com.fioiu8.devinfo.model.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 主屏幕 — 双标签布局（设备信息 / 设置），协调更新检查、导出对话框和关于页面。
 *
 * @param deviceId 持久化设备唯一标识
 * @param themeMode 当前主题模式
 * @param onThemeModeChange 主题模式变更回调
 * @param mountThemeColor 当前自定义主题色
 * @param onMountThemeColorChange 主题色变更回调
 * @param useMountTheme 是否启用自定义主题色
 * @param onUseMountThemeChange 自定义主题色开关回调
 * @param exportHelper 模块导出工具实例
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    deviceId: String,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    mountThemeColor: MountThemeColor,
    onMountThemeColorChange: (MountThemeColor) -> Unit,
    useMountTheme: Boolean,
    onUseMountThemeChange: (Boolean) -> Unit,
    exportHelper: ModuleExportHelper,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    languageOptions: List<String> = emptyList(),
    onLanguageChange: (Int) -> Unit = {},
    customLocaleTag: String = "",
    onCustomLocaleTagChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val collector = remember { DeviceInfoCollector(context) }
    val updateChecker = remember { UpdateChecker(context) }
    val scope = rememberCoroutineScope()
    var selectedIndex by remember { mutableIntStateOf(0) }
    val itemsState = remember { mutableStateListOf<ItemWithVisibility>() }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // 关于页面 — 预测性返回动画状态
    var showAboutPage by remember { mutableStateOf(false) }
    var isAboutVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val aboutOffsetX = remember { Animatable(screenWidthPx) }

    // 关于页面入场动画
    LaunchedEffect(showAboutPage) {
        if (showAboutPage) {
            isAboutVisible = true
            aboutOffsetX.snapTo(screenWidthPx)
            aboutOffsetX.animateTo(0f, animationSpec = tween(300))
        }
    }

    // 关于页面退场动画（返回按钮或预测返回提交后调用）
    fun dismissAboutPage() {
        scope.launch {
            aboutOffsetX.animateTo(screenWidthPx, animationSpec = tween(300))
            showAboutPage = false
            isAboutVisible = false
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportedFilePath by remember { mutableStateOf("") }

    // Update — 使用 collectAsState 响应式订阅 StateFlow
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateChecked by remember { mutableStateOf(false) }

    val updateState by updateChecker.state.collectAsState()
    val releaseInfo by updateChecker.releaseInfo.collectAsState()

    val themeOptions = ThemeMode.entries.map { it.displayName }
    val languageOptionsList = AppLanguage.entries.map { lang -> stringResource(lang.displayNameResId) }
    val isDynamicMode = themeMode.isDynamic

    LaunchedEffect(Unit) {
        loadDeviceInfo(collector, itemsState)
        isLoading = false
    }

    LaunchedEffect(Unit) {
        if (!updateChecked) {
            updateChecked = true
            updateChecker.check(BuildConfig.VERSION_NAME)
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            UpdateState.UP_TO_DATE -> {
                Toast.makeText(context, context.getString(R.string.already_latest), Toast.LENGTH_SHORT).show()
                updateChecker.reset()
            }
            UpdateState.NEW_VERSION_AVAILABLE -> showUpdateDialog = true
            UpdateState.ERROR -> showUpdateDialog = true
            else -> {}
        }
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            itemsState.clear()
            loadDeviceInfo(collector, itemsState)
        }
    }

    // 主界面内容（始终渲染，确保关于页面滑入时无白屏闪烁、
    // 预测性返回预览时能看到底层内容）
    val tabs = listOf(stringResource(R.string.nav_info), stringResource(R.string.nav_settings))
    val selectedIcons = listOf(Icons.Filled.Description, Icons.Filled.Settings)
    val unselectedIcons = listOf(Icons.Outlined.Description, Icons.Outlined.Settings)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (selectedIndex == 1) stringResource(R.string.title_settings) else stringResource(R.string.title_device_info),
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (!BuildConfig.IS_OFFICIAL) {
                        TestVersionWarningCard(
                            versionName = BuildConfig.VERSION_NAME,
                            buildType = BuildConfig.BUILD_TYPE_NAME,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { i, title ->
                        NavigationBarItem(
                            selected = selectedIndex == i,
                            onClick = { selectedIndex = i },
                            icon = {
                                Icon(
                                    imageVector = if (selectedIndex == i) selectedIcons[i] else unselectedIcons[i],
                                    contentDescription = title
                                )
                            },
                            label = { Text(text = title, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (selectedIndex) {
                    0 -> DeviceInfoPage(
                        deviceId = deviceId,
                        itemsState = itemsState,
                        isLoading = isLoading,
                        onRefresh = { refreshTrigger++ }
                    )
                    1 -> SettingsPage(
                        versionName = collector.getAppVersionName(),
                        versionCode = collector.getAppVersionCode(),
                        themeMode = themeMode,
                        themeOptions = themeOptions,
                        onThemeChange = { index -> onThemeModeChange(ThemeMode.entries[index]) },
                        mountThemeColor = mountThemeColor,
                        mountColorOptions = MountThemeColor.entries,
                        selectedMountColorIndex = MountThemeColor.entries.indexOf(mountThemeColor),
                        onMountColorChange = { index ->
                            onMountThemeColorChange(MountThemeColor.entries[index])
                            onUseMountThemeChange(true)
                        },
                        isDynamicMode = isDynamicMode,
                        useMountTheme = useMountTheme,
                        onExportClick = { showExportDialog = true },
                        onAboutClick = { showAboutPage = true },
                        appLanguage = appLanguage,
                        languageOptions = languageOptionsList,
                        onLanguageChange = { index -> onLanguageChange(index) },
                        customLocaleTag = customLocaleTag,
                        onCustomLocaleTagChange = { tag -> onCustomLocaleTagChange(tag) }
                    )
                }
            }
        }

        // 关于页面 — 覆盖在主界面之上，offset 由预测返回手势或入场动画驱动
        // 主界面始终在下层渲染，避免动画过程中出现白屏闪烁
        if (isAboutVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(aboutOffsetX.value.roundToInt(), 0) }
            ) {
                AboutPage(
                    versionName = collector.getAppVersionName(),
                    onBack = { dismissAboutPage() }
                )
            }
        }
    }

    // 预测性返回手势（Android 14+ 系统级预测性返回动画）
    // PredictiveBackHandler 需无条件调用；enabled 参数控制是否拦截
    PredictiveBackHandler(
        enabled = showAboutPage,
        onBack = { progress ->
            try {
                progress.collect { event ->
                    // 手势进度驱动关于页面实时跟随手指滑动
                    aboutOffsetX.snapTo(event.progress * screenWidthPx)
                }
                // 手势提交 → 继续动画到完全滑出
                scope.launch {
                    aboutOffsetX.animateTo(screenWidthPx, animationSpec = tween(200))
                    showAboutPage = false
                    isAboutVisible = false
                }
            } catch (_: CancellationException) {
                // 手势取消（用户滑回）→ 动画回到原位
                scope.launch {
                    aboutOffsetX.animateTo(0f, animationSpec = tween(200))
                }
            }
        }
    )

    // Dialogs
    UpdateAvailableDialog(
        show = showUpdateDialog,
        info = releaseInfo,
        isError = updateState == UpdateState.ERROR,
        currentVersion = BuildConfig.VERSION_NAME,
        onDownload = {
            openUrl(context, releaseInfo?.htmlUrl ?: "https://github.com/FIOIU8/DevInfo/releases")
            showUpdateDialog = false
            updateChecker.reset()
        },
        onRetry = {
            showUpdateDialog = false
            scope.launch { updateChecker.check(BuildConfig.VERSION_NAME) }
        },
        onDismiss = {
            showUpdateDialog = false
            updateChecker.reset()
        }
    )

    ExportConfirmDialog(
        show = showExportDialog,
        onConfirm = {
            exportHelper.exportModule(
                deviceId = deviceId, itemsState = itemsState,
                onSuccess = { path ->
                    exportedFilePath = path
                    showExportSuccessDialog = true
                },
                onError = { error ->
                    Toast.makeText(context, context.getString(R.string.export_failed) + ": $error", Toast.LENGTH_SHORT).show()
                }
            )
        },
        onDismiss = { showExportDialog = false }
    )

    ExportSuccessDialog(
        show = showExportSuccessDialog,
        filePath = exportedFilePath,
        onDismiss = { showExportSuccessDialog = false }
    )
}

/** 从 DeviceInfoCollector 加载设备信息并执行逐项淡入动画 */
private suspend fun loadDeviceInfo(
    collector: DeviceInfoCollector,
    itemsState: MutableList<ItemWithVisibility>
) {
    val infoList = collector.collectDeviceInfo()
    itemsState.addAll(infoList.map { item -> ItemWithVisibility(item, mutableStateOf(false)) })
    itemsState.forEach { item -> delay(30); item.visible.value = true }
}

/** 通过 Intent 打开外部链接，失败时弹出 Toast 提示 */
private fun openUrl(context: android.content.Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    catch (_: Exception) { Toast.makeText(context, context.getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show() }
}
