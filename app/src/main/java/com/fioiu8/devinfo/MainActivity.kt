package com.fioiu8.devinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fioiu8.devinfo.ui.theme.DevInfoTheme
import kotlinx.coroutines.delay
import java.util.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.ThemeColorSpec

import androidx.compose.runtime.mutableIntStateOf
import androidx.core.net.toUri

// 导航栏显示模式选项
enum class NavigationBarMode(val displayName: String) {
    FLOATING_ICON_ONLY("悬浮图标"),
    FLOATING_ICON_TEXT("悬浮图标+文字"),
    FLOATING_TEXT_ONLY("悬浮文字"),
    FIXED_ICON_TEXT("固定图标+文字"),
    FIXED_ICON_LABEL("固定图标+选中文字")
}

// 主题颜色枚举
enum class MountThemeColor(
    val displayName: String,
    val color: Color,
    val description: String
) {
    DEFAULT("默认", Color(0xFF4A90D9), "清新蓝色"),
    RED("红色", Color(0xFFE74C3C), "热情红色"),
    ORANGE("橙色", Color(0xFFE67E22), "活力橙色"),
    GREEN("绿色", Color(0xFF2ECC71), "自然绿色"),
    TEAL("青色", Color(0xFF1ABC9C), "清新青色"),
    PURPLE("紫色", Color(0xFF9B59B6), "优雅紫色"),
    PINK("粉色", Color(0xFFE91E63), "甜美粉色"),
    DARK("深色", Color(0xFF34495E), "沉稳深色")
}

// 简单的颜色预览 Composable（使用 Box + clip + background）
@Composable
fun ColorPreview(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    cornerRadius: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color)
    )
}

class MainActivity : ComponentActivity() {

    private lateinit var exportHelper: ModuleExportHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        exportHelper = ModuleExportHelper(this)

        val deviceId = try {
            DeviceIdManager(this).getOrCreateDeviceId()
        } catch (e: Exception) {
            "获取失败: ${e.message}"
        }

        setContent {
            // 主题模式状态
            var themeMode by remember { mutableStateOf(ColorSchemeMode.System) }
            // Mount 主题颜色状态
            var mountThemeColor by remember { mutableStateOf(MountThemeColor.DEFAULT) }
            // 是否使用 Mount 主题（当用户选择了 Mount 主题颜色时启用）
            var useMountTheme by remember { mutableStateOf(false) }

            // 根据当前设置创建 ThemeController
            val themeController = remember(themeMode, mountThemeColor, useMountTheme) {
                if (useMountTheme && themeMode.name.startsWith("Monet")) {
                    // 如果是 Monet 模式且启用了 Mount 主题，使用 keyColor
                    ThemeController(
                        colorSchemeMode = themeMode,
                        keyColor = mountThemeColor.color,
                        paletteStyle = ThemePaletteStyle.TonalSpot,
                        colorSpec = ThemeColorSpec.Spec2021
                    )
                } else {
                    // 否则只使用基本模式
                    ThemeController(themeMode)
                }
            }

            DevInfoTheme {
                MiuixTheme(controller = themeController) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            deviceId = deviceId,
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it },
                            mountThemeColor = mountThemeColor,
                            onMountThemeColorChange = { color ->
                                mountThemeColor = color
                                useMountTheme = true
                            },
                            useMountTheme = useMountTheme,
                            onUseMountThemeChange = { useMountTheme = it }
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainScreen(
        deviceId: String,
        themeMode: ColorSchemeMode,
        onThemeModeChange: (ColorSchemeMode) -> Unit,
        mountThemeColor: MountThemeColor,
        onMountThemeColorChange: (MountThemeColor) -> Unit,
        useMountTheme: Boolean,
        onUseMountThemeChange: (Boolean) -> Unit
    ) {
        val context = LocalContext.current
        var selectedIndex by remember { mutableIntStateOf(0) }
        val itemsState = remember { mutableStateListOf<ItemWithVisibility>() }
        var isLoading by remember { mutableStateOf(true) }
        var refreshTrigger by remember { mutableIntStateOf(0) }

        // 对话框状态
        var showAboutDialog by remember { mutableStateOf(false) }
        var showExportDialog by remember { mutableStateOf(false) }
        var showExportSuccessDialog by remember { mutableStateOf(false) }
        var exportedFilePath by remember { mutableStateOf("") }

        // 导航栏设置
        var navBarMode by remember { mutableStateOf(NavigationBarMode.FIXED_ICON_LABEL) }

        // 主题选择状态
        val themeOptions = listOf("跟随系统", "浅色模式", "深色模式", "Monet 跟随系统", "Monet 浅色", "Monet 深色")
        val selectedThemeModeIndex = when (themeMode) {
            ColorSchemeMode.System -> 0
            ColorSchemeMode.Light -> 1
            ColorSchemeMode.Dark -> 2
            ColorSchemeMode.MonetSystem -> 3
            ColorSchemeMode.MonetLight -> 4
            ColorSchemeMode.MonetDark -> 5
            else -> 0
        }

        // 检查当前是否为 Monet 模式
        val isMonetMode = themeMode.name.startsWith("Monet")

        // 导航栏模式选项
        val navBarModeOptions = NavigationBarMode.entries.map { it.displayName }
        val selectedNavBarModeIndex = NavigationBarMode.entries.indexOf(navBarMode)

        // Mount 主题颜色选项
        val mountColorOptions = MountThemeColor.entries.map { color ->
            SpinnerEntry(
                icon = {
                    ColorPreview(
                        color = color.color,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                title = color.displayName,
                summary = color.description
            )
        }
        val selectedMountColorIndex = MountThemeColor.entries.indexOf(mountThemeColor)

        LaunchedEffect(Unit) {
            loadDeviceInfo(context, itemsState)
            isLoading = false
        }

        LaunchedEffect(refreshTrigger) {
            if (refreshTrigger > 0) {
                itemsState.clear()
                loadDeviceInfo(context, itemsState)
            }
        }

        val tabs = listOf("信息", "设置")
        val icons = listOf(
            MiuixIcons.Info,
            MiuixIcons.Settings
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = when (selectedIndex) {
                            0 -> "设备信息"
                            1 -> "设置"
                            else -> "设备信息"
                        }
                    )
                },
                bottomBar = {
                    when (navBarMode) {
                        NavigationBarMode.FLOATING_ICON_ONLY -> {
                            FloatingNavigationBar(
                                mode = FloatingNavigationBarDisplayMode.IconOnly
                            ) {
                                tabs.forEachIndexed { i, title ->
                                    FloatingNavigationBarItem(
                                        selected = selectedIndex == i,
                                        onClick = { selectedIndex = i },
                                        icon = icons[i],
                                        label = title
                                    )
                                }
                            }
                        }
                        NavigationBarMode.FLOATING_ICON_TEXT -> {
                            FloatingNavigationBar(
                                mode = FloatingNavigationBarDisplayMode.IconAndText
                            ) {
                                tabs.forEachIndexed { i, title ->
                                    FloatingNavigationBarItem(
                                        selected = selectedIndex == i,
                                        onClick = { selectedIndex = i },
                                        icon = icons[i],
                                        label = title
                                    )
                                }
                            }
                        }
                        NavigationBarMode.FLOATING_TEXT_ONLY -> {
                            FloatingNavigationBar(
                                mode = FloatingNavigationBarDisplayMode.TextOnly
                            ) {
                                tabs.forEachIndexed { i, title ->
                                    FloatingNavigationBarItem(
                                        selected = selectedIndex == i,
                                        onClick = { selectedIndex = i },
                                        icon = icons[i],
                                        label = title
                                    )
                                }
                            }
                        }
                        NavigationBarMode.FIXED_ICON_TEXT -> {
                            NavigationBar(
                                mode = NavigationBarDisplayMode.IconAndText,
                                showDivider = false
                            ) {
                                tabs.forEachIndexed { i, title ->
                                    NavigationBarItem(
                                        selected = selectedIndex == i,
                                        onClick = { selectedIndex = i },
                                        icon = icons[i],
                                        label = title
                                    )
                                }
                            }
                        }
                        NavigationBarMode.FIXED_ICON_LABEL -> {
                            NavigationBar(
                                mode = NavigationBarDisplayMode.IconWithSelectedLabel,
                                showDivider = false
                            ) {
                                tabs.forEachIndexed { i, title ->
                                    NavigationBarItem(
                                        selected = selectedIndex == i,
                                        onClick = { selectedIndex = i },
                                        icon = icons[i],
                                        label = title
                                    )
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (selectedIndex) {
                        0 -> DeviceInfoPage(
                            deviceId = deviceId,
                            itemsState = itemsState,
                            isLoading = isLoading,
                            onRefresh = { refreshTrigger++ }
                        )
                        1 -> SettingsAndAboutPage(
                            selectedThemeMode = selectedThemeModeIndex,
                            themeOptions = themeOptions,
                            onThemeChange = { index: Int ->
                                val newMode = when (index) {
                                    0 -> ColorSchemeMode.System
                                    1 -> ColorSchemeMode.Light
                                    2 -> ColorSchemeMode.Dark
                                    3 -> ColorSchemeMode.MonetSystem
                                    4 -> ColorSchemeMode.MonetLight
                                    5 -> ColorSchemeMode.MonetDark
                                    else -> ColorSchemeMode.System
                                }
                                onThemeModeChange(newMode)
                            },
                            selectedNavBarMode = selectedNavBarModeIndex,
                            navBarModeOptions = navBarModeOptions,
                            onNavBarModeChange = { index ->
                                navBarMode = NavigationBarMode.entries[index]
                            },
                            mountColorOptions = mountColorOptions,
                            selectedMountColorIndex = selectedMountColorIndex,
                            onMountColorChange = { index ->
                                onMountThemeColorChange(MountThemeColor.entries[index])
                                onUseMountThemeChange(true)
                            },
                            isMonetMode = isMonetMode,
                            useMountTheme = useMountTheme,
                            onExportClick = { showExportDialog = true },
                            onAboutClick = { showAboutDialog = true },
                            onSourceCodeClick = { openSourceCode(context) },
                            onCommunityClick = { openCommunity(context) }
                        )
                    }
                }
            }

            // 对话框
            AboutDialog(show = showAboutDialog, onDismiss = { showAboutDialog = false })

            ExportConfirmDialog(
                show = showExportDialog,
                onConfirm = {
                    showExportDialog = false
                    exportHelper.exportModule(
                        deviceId = deviceId,
                        itemsState = itemsState,
                        onSuccess = { path ->
                            exportedFilePath = path
                            showExportSuccessDialog = true
                        },
                        onError = { error ->
                            Toast.makeText(context, "导出失败: $error", Toast.LENGTH_SHORT).show()
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
    }

    @Composable
    fun SettingsAndAboutPage(
        selectedThemeMode: Int,
        themeOptions: List<String>,
        onThemeChange: (Int) -> Unit,
        selectedNavBarMode: Int,
        navBarModeOptions: List<String>,
        onNavBarModeChange: (Int) -> Unit,
        mountColorOptions: List<SpinnerEntry>,
        selectedMountColorIndex: Int,
        onMountColorChange: (Int) -> Unit,
        isMonetMode: Boolean,
        useMountTheme: Boolean,
        onExportClick: () -> Unit,
        onAboutClick: () -> Unit,
        onSourceCodeClick: () -> Unit,
        onCommunityClick: () -> Unit
    ) {
        val context = LocalContext.current

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 外观配置
            item {
                CategoryHeader(title = "外观")
            }

            // 主题选择
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    OverlayDropdownPreference(
                        title = "深色模式",
                        summary = themeOptions[selectedThemeMode],
                        items = themeOptions,
                        selectedIndex = selectedThemeMode,
                        onSelectedIndexChange = onThemeChange
                    )
                }
            }

            // Mount 主题颜色选择 - 仅在 Monet 模式下可用
            if (isMonetMode) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(0.dp)
                    ) {
                        OverlaySpinnerPreference(
                            title = "主题颜色",
                            summary = if (useMountTheme) "已启用自定义颜色" else "选择应用的主题颜色",
                            items = mountColorOptions,
                            selectedIndex = selectedMountColorIndex,
                            onSelectedIndexChange = onMountColorChange
                        )
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(0.dp)
                    ) {
                        ArrowPreference(
                            title = "主题颜色",
                            summary = "请先切换到 Monet 模式",
                            enabled = false,
                            onClick = {}
                        )
                    }
                }
            }

            // 导航栏模式
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    OverlayDropdownPreference(
                        title = "导航栏样式",
                        summary = navBarModeOptions[selectedNavBarMode],
                        items = navBarModeOptions,
                        selectedIndex = selectedNavBarMode,
                        onSelectedIndexChange = onNavBarModeChange
                    )
                }
            }

            // 工具分类
            item {
                Spacer(modifier = Modifier.height(12.dp))
                CategoryHeader(title = "工具")
            }

            // 导出模块
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    ArrowPreference(
                        title = "导出模块",
                        summary = "导出当前设备信息为改机型模块",
                        onClick = onExportClick
                    )
                }
            }

            // 关于分类
            item {
                Spacer(modifier = Modifier.height(12.dp))
                CategoryHeader(title = "关于")
            }

            // 关于应用
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    ArrowPreference(
                        title = "关于 ${getAppVersionName(context)}",
                        summary = "查看应用信息",
                        onClick = onAboutClick
                    )
                }
            }

            // 查看源代码
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    ArrowPreference(
                        title = "查看源代码",
                        summary = "项目完全开源，欢迎 Star",
                        onClick = onSourceCodeClick
                    )
                }
            }

            // 加入社区
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    ArrowPreference(
                        title = "加入社区",
                        summary = "反馈问题、交流讨论",
                        onClick = onCommunityClick
                    )
                }
            }

            // 版本信息
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "设备信息查看器",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = "版本 ${getAppVersionName(context)} (${getAppVersionCode(context)})",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "© 2026 OIU0",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadDeviceInfo(context: Context, itemsState: MutableList<ItemWithVisibility>) {
        val infoList = mutableListOf<DeviceInfoItem>()

        // 基本信息
        infoList.add(DeviceInfoItem("ANDROID_ID", getAndroidIdSafe(context)))
        infoList.add(DeviceInfoItem("序列号", getSerialNumberSafe()))
        infoList.add(DeviceInfoItem("品牌", Build.BRAND))
        infoList.add(DeviceInfoItem("制造商", Build.MANUFACTURER))
        infoList.add(DeviceInfoItem("型号", Build.MODEL))
        infoList.add(DeviceInfoItem("产品", Build.PRODUCT))
        infoList.add(DeviceInfoItem("设备", Build.DEVICE))
        infoList.add(DeviceInfoItem("主板", Build.BOARD))
        infoList.add(DeviceInfoItem("硬件", Build.HARDWARE))
        infoList.add(DeviceInfoItem("引导程序", Build.BOOTLOADER))
        infoList.add(DeviceInfoItem("指纹", Build.FINGERPRINT))
        infoList.add(DeviceInfoItem("构建ID", Build.ID))
        infoList.add(DeviceInfoItem("标签", Build.TAGS))
        infoList.add(DeviceInfoItem("时间", Build.TIME.toString()))
        infoList.add(DeviceInfoItem("类型", Build.TYPE))

        // 系统信息
        infoList.add(DeviceInfoItem("CPU架构", Build.SUPPORTED_ABIS.joinToString()))
        infoList.add(DeviceInfoItem("CPU核心数", Runtime.getRuntime().availableProcessors().toString()))
        infoList.add(DeviceInfoItem("SDK版本", Build.VERSION.SDK_INT.toString()))
        infoList.add(DeviceInfoItem("Android版本", Build.VERSION.RELEASE))
        infoList.add(DeviceInfoItem("安全补丁", safeGet { Build.VERSION.SECURITY_PATCH }))
        infoList.add(DeviceInfoItem("基带版本", safeGet { Build.getRadioVersion() }))

        // 区域信息
        infoList.add(DeviceInfoItem("语言", Locale.getDefault().language))
        infoList.add(DeviceInfoItem("国家", Locale.getDefault().country))
        infoList.add(DeviceInfoItem("时区", TimeZone.getDefault().id))

        // 屏幕信息
        infoList.add(DeviceInfoItem("屏幕DPI", context.resources.displayMetrics.densityDpi.toString()))
        infoList.add(DeviceInfoItem("屏幕宽度", context.resources.displayMetrics.widthPixels.toString()))
        infoList.add(DeviceInfoItem("屏幕高度", context.resources.displayMetrics.heightPixels.toString()))
        infoList.add(DeviceInfoItem("刷新率", safeGet { context.display.refreshRate.toString() }))
        infoList.add(DeviceInfoItem("字体缩放", safeGet { context.resources.configuration.fontScale.toString() }))

        // 内存和存储
        infoList.add(DeviceInfoItem("总内存", getTotalMemory(context)))
        infoList.add(DeviceInfoItem("可用内存", getAvailMemory(context)))
        infoList.add(DeviceInfoItem("存储总量", getTotalStorage()))
        infoList.add(DeviceInfoItem("可用存储", getFreeStorage()))

        // 电池
        infoList.add(DeviceInfoItem("电池电量", getBatteryLevel(context)))
        infoList.add(DeviceInfoItem("充电状态", getBatteryCharging(context)))

        // 硬件功能
        infoList.add(DeviceInfoItem("NFC功能", if (NfcAdapter.getDefaultAdapter(context) != null) "支持" else "不支持"))
        infoList.add(DeviceInfoItem("摄像头数量", getCameraCount(context)))
        infoList.add(DeviceInfoItem("蓝牙状态", getBluetoothState(context)))
        // 网络
        infoList.add(DeviceInfoItem("网络类型", getNetworkType(context)))
        infoList.add(DeviceInfoItem("运营商", getNetworkOperator(context)))
        infoList.add(DeviceInfoItem("SIM卡状态", getSimState(context)))

        // 应用信息
        infoList.add(DeviceInfoItem("包名", context.packageName))
        infoList.add(DeviceInfoItem("应用版本名", getAppVersionName(context)))
        infoList.add(DeviceInfoItem("应用版本码", getAppVersionCode(context).toString()))

        itemsState.addAll(infoList.map { item ->
            ItemWithVisibility(item, mutableStateOf(false))
        })

        itemsState.forEachIndexed { _, item ->
            delay(30)
            item.visible.value = true
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DeviceInfoPage(
        deviceId: String,
        itemsState: List<ItemWithVisibility>,
        isLoading: Boolean,
        onRefresh: () -> Unit
    ) {
        val ctx = LocalContext.current
        val clipboardManager = LocalClipboardManager.current

        var isRefreshing by remember { mutableStateOf(false) }
        val pullToRefreshState = rememberPullToRefreshState()

        LaunchedEffect(isRefreshing) {
            if (isRefreshing) {
                onRefresh()
                delay(500)
                isRefreshing = false
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = { isRefreshing = true },
                    pullToRefreshState = pullToRefreshState,
                    refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新...", "刷新成功"),
                    color = MiuixTheme.colorScheme.primary
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 10.dp,
                                insideMargin = PaddingValues(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "设备唯一标识",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = deviceId,
                                        fontSize = 11.sp,
                                        modifier = Modifier.combinedClickable(
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(deviceId))
                                                Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show()
                                            },
                                            onClick = {}
                                        )
                                    )
                                }
                            }
                        }

                        itemsIndexed(itemsState) { _, currentItem ->
                            AnimatedVisibility(
                                visible = currentItem.visible.value,
                                enter = fadeIn() + slideInHorizontally()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(currentItem.item.value))
                                                Toast.makeText(ctx, "${currentItem.item.key} 已复制", Toast.LENGTH_SHORT).show()
                                            },
                                            onClick = {}
                                        ),
                                    cornerRadius = 10.dp,
                                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${currentItem.item.key}:",
                                            modifier = Modifier.weight(1f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = currentItem.item.value,
                                            modifier = Modifier.weight(1.5f),
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CategoryHeader(title: String) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    @Composable
    fun AboutDialog(show: Boolean, onDismiss: () -> Unit) {
        val context = LocalContext.current

        WindowDialog(
            show = show,
            title = "关于",
            onDismissRequest = onDismiss,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 应用图标
                    Card(
                        modifier = Modifier.size(80.dp),
                        cornerRadius = 40.dp,
                        insideMargin = PaddingValues(0.dp),
                        pressFeedbackType = PressFeedbackType.Tilt
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "应用图标",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "设备信息查看器",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "版本 ${getAppVersionName(context)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MiuixTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "这是一个完全开源的项目",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "使用者需遵守 MIT 协议",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "欢迎贡献代码和提出建议",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.primary
                            )
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun ExportConfirmDialog(
        show: Boolean,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        WindowDialog(
            show = show,
            title = "导出模块",
            onDismissRequest = onDismiss,
            content = {
                val dismiss = LocalDismissState.current

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "即将导出当前设备信息为改机型模块",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(12.dp)
                    ) {
                        Column {
                            InfoRow("导出格式", "ZIP 压缩包")
                            InfoRow("保存位置", Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            ).absolutePath)
                            InfoRow("文件名", "${Build.MODEL}.zip")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "确认导出吗？",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = { dismiss?.invoke() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirm()
                                dismiss?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.primary
                            )
                        ) {
                            Text("确认导出")
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun ExportSuccessDialog(
        show: Boolean,
        filePath: String,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current

        WindowDialog(
            show = show,
            title = "导出成功",
            onDismissRequest = onDismiss,
            content = {
                val dismiss = LocalDismissState.current

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally),
                        tint = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "文件已保存至：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(filePath))
                                    Toast.makeText(context, "路径已复制", Toast.LENGTH_SHORT).show()
                                }
                            ),
                        cornerRadius = 8.dp,
                        insideMargin = PaddingValues(12.dp)
                    ) {
                        Text(
                            text = filePath,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "长按路径可复制",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                dismiss?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.primary
                            )
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        )

    }

    @Composable
    fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }


    // ==================== 辅助方法 ====================

    private fun openSourceCode(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/FIOIU8/DevInfo".toUri())
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCommunity(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, "https://www.coolapk.com/u/32334444".toUri())
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    // Android ID
    private fun getAndroidIdSafe(context: Context): String {
        return safeGet {
            @Suppress("HardwareIds")
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            androidId ?: "未知"
        }
    }

    @Suppress("MissingPermission", "HardwareIds")
    private fun getSerialNumberSafe(): String {
        return safeGet {
            Build.getSerial()
        }
    }

    // 蓝牙
    private fun getBluetoothState(context: Context): String {
        return safeGet {
            val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            if (bluetoothManager?.adapter?.isEnabled == true) "开启" else "关闭"
        }
    }

    private fun getTotalMemory(context: Context): String {
        val mi = ActivityManager.MemoryInfo()
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(mi)
        return "${mi.totalMem / 1024 / 1024} MB"
    }

    private fun getAvailMemory(context: Context): String {
        val mi = ActivityManager.MemoryInfo()
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(mi)
        return "${mi.availMem / 1024 / 1024} MB"
    }

    private fun getTotalStorage() =
        safeGet { "${Environment.getExternalStorageDirectory().totalSpace / 1024 / 1024 / 1024} GB" }

    private fun getFreeStorage() =
        safeGet { "${Environment.getExternalStorageDirectory().freeSpace / 1024 / 1024 / 1024} GB" }

    private fun getBatteryLevel(context: Context) = safeGet {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toString() + "%"
    }

    private fun getBatteryCharging(context: Context) = safeGet {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        if (bm.isCharging) "充电中" else "未充电"
    }

    private fun getCameraCount(context: Context) = safeGet {
        val cam = context.getSystemService(CAMERA_SERVICE) as CameraManager
        cam.cameraIdList.size.toString()
    }

    private fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "未知"
        return when {
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "蓝牙"
            else -> "未知"
        }
    }

    private fun getNetworkOperator(context: Context) = safeGet {
        val tm = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        tm.networkOperatorName ?: "未知"
    }

    @Suppress("DEPRECATION")
    private fun getSimState(context: Context) = safeGet {
        val tm = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        when (tm.simState) {
            TelephonyManager.SIM_STATE_READY -> "就绪"
            TelephonyManager.SIM_STATE_ABSENT -> "无SIM卡"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "网络锁定"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "需要PIN"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "需要PUK"
            TelephonyManager.SIM_STATE_UNKNOWN -> "未知"
            TelephonyManager.SIM_STATE_NOT_READY -> "未就绪"
            TelephonyManager.SIM_STATE_PERM_DISABLED -> "永久禁用"
            TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "卡IO错误"
            TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "卡受限"
            else -> "未知状态"
        }
    }

    private fun getAppVersionName(context: Context): String {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            p.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    private fun getAppVersionCode(context: Context): Long {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            p.longVersionCode
        } catch (_: Exception) {
            1
        }
    }

    private inline fun safeGet(default: String = "未知", block: () -> String): String {
        return try {
            block()
        } catch (_: Exception) {
            default
        }
    }
}

data class DeviceInfoItem(val key: String, val value: String)
data class ItemWithVisibility(val item: DeviceInfoItem, val visible: MutableState<Boolean>)