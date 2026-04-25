package com.fioiu8.devinfo

// 导入 Android 相关类
import android.app.ActivityManager // 用于获取内存信息
import android.content.Context // Android 上下文
import android.content.Intent // 用于启动其他 Activity 或打开链接
import android.hardware.camera2.CameraManager // 用于获取摄像头信息
import android.net.ConnectivityManager // 用于获取网络连接信息
import android.net.NetworkCapabilities // 用于判断网络具体类型（Wi-Fi、移动数据等）
import android.nfc.NfcAdapter // 用于检测 NFC 功能是否可用
import android.os.BatteryManager // 用于读取电池状态和电量
import android.os.Build // 获取设备硬件和系统版本信息
import android.os.Bundle // 用于 Activity 保存和恢复状态
import android.os.Environment // 用于获取存储目录路径
import android.provider.Settings // 用于获取系统设置值，如 Android ID
import android.telephony.TelephonyManager // 用于获取运营商和 SIM 卡信息
import android.widget.Toast // 显示短暂的提示消息
import androidx.activity.ComponentActivity // Jetpack Compose 的基础 Activity
import androidx.activity.compose.setContent // 在 Activity 中设置 Compose 内容
import androidx.activity.enableEdgeToEdge // 启用边到边显示（即内容延伸至状态栏和导航栏区域）
import androidx.compose.animation.* // 导入 Compose 动画相关的类和函数
import androidx.compose.foundation.ExperimentalFoundationApi // 标记使用了 Compose 中的非稳定 API (foundation)
import androidx.compose.foundation.Image // 用于显示图片的 Composable
import androidx.compose.foundation.background // 用于设置背景颜色的 Modifier
import androidx.compose.foundation.combinedClickable // 支持同时处理点击和长按事件
import androidx.compose.foundation.layout.* // 导入布局相关的元素，如 Box, Column, Row, Spacer 等
import androidx.compose.foundation.lazy.LazyColumn // 高效显示长列表的 Composable
import androidx.compose.foundation.lazy.itemsIndexed // 用于在 LazyColumn 中按索引生成项目
import androidx.compose.foundation.shape.RoundedCornerShape // 定义圆角形状
import androidx.compose.runtime.* // 导入 Compose 状态管理相关的注解和函数
import androidx.compose.ui.Alignment // 用于在父布局中定位子元素
import androidx.compose.ui.Modifier // Compose 中用于修饰 UI 元素的核心类
import androidx.compose.ui.draw.clip // 用于裁剪 UI 元素形状的 Modifier
import androidx.compose.ui.graphics.Color // 定义和使用颜色
import androidx.compose.ui.platform.LocalClipboardManager // 获取系统剪贴板的 Composable 实例
import androidx.compose.ui.platform.LocalContext // 获取当前 Composable 上下文的 Context
import androidx.compose.ui.res.painterResource // 从资源文件中加载图片
import androidx.compose.ui.text.AnnotatedString // 用于带有样式信息的字符串，设置剪贴板时需要
import androidx.compose.ui.text.font.FontWeight // 定义字体粗细
import androidx.compose.ui.unit.Dp // 表示密度无关像素的单位
import androidx.compose.ui.unit.dp // 将数值转换为密度无关像素的扩展属性
import androidx.compose.ui.unit.sp // 将数值转换为可缩放像素的单位
import com.fioiu8.devinfo.ui.theme.DevInfoTheme // 应用的基础主题
import kotlinx.coroutines.delay // 协程延迟函数，用于实现延时和动画效果
import java.util.* // 导入 Java 工具类，如 Locale, TimeZone
// 导入第三方 Miuix 组件库的各种组件和工具
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
import top.yukonga.miuix.kmp.basic.Card // miuix的card组件
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.mutableIntStateOf // 用于创建 Compose 状态的整型可变状态
import androidx.core.net.toUri // 将字符串转换为 Uri 对象的扩展函数
import androidx.compose.ui.zIndex

// 导航栏显示模式枚举，定义了底部导航栏的几种可选样式
enum class NavigationBarMode(val displayName: String) {
    FLOATING_ICON_ONLY("悬浮图标"),       // 只显示图标的悬浮导航栏
    FLOATING_ICON_TEXT("悬浮图标+文字"),   // 同时显示图标和文字的悬浮导航栏
    FLOATING_TEXT_ONLY("悬浮文字"),        // 只显示文字的悬浮导航栏
    FIXED_ICON_TEXT("固定图标+文字"),      // 固定位置，同时显示图标和文字
    FIXED_ICON_LABEL("固定图标+选中文字")  // 固定位置，图标常显，选中后显示文字标签
}

// 主题颜色枚举，定义了可供用户选择的 Monet 主题颜色选项
enum class MountThemeColor(
    val displayName: String, // 显示在界面上的颜色名称
    val color: Color,       // 对应的 Compose Color 对象
    val description: String  // 颜色的描述信息
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

// 一个用于显示颜色预览小方块的 Composable 组件
@Composable
fun ColorPreview(
    color: Color, // 需要预览的颜色
    modifier: Modifier = Modifier, // 可选的 Modifier，用于自定义样式和布局
    size: Dp = 24.dp, // 预览方块的大小，默认为 24dp
    cornerRadius: Dp = 6.dp // 方块的圆角大小，默认为 6dp
) {
    Box(
        modifier = modifier
            .size(size) // 应用指定的大小
            .clip(RoundedCornerShape(cornerRadius)) // 裁剪成圆角形状
            .background(color) // 设置背景颜色为该颜色
    )
}

class MainActivity : ComponentActivity() {

    // 延迟初始化模块导出助手，将在 onCreate 中实例化
    private lateinit var exportHelper: ModuleExportHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 启用边到边显示

        exportHelper = ModuleExportHelper(this) // 实例化导出助手

        // 尝试获取或生成设备唯一标识，失败时显示错误信息
        val deviceId = try {
            DeviceIdManager(this).getOrCreateDeviceId()
        } catch (e: Exception) {
            "获取失败: ${e.message}"
        }

        setContent {
            // 主题模式状态，默认为跟随系统
            var themeMode by remember { mutableStateOf(ColorSchemeMode.System) }
            // 自定义主题颜色状态，默认为 DEFAULT
            var mountThemeColor by remember { mutableStateOf(MountThemeColor.DEFAULT) }
            // 是否启用自定义主题颜色的状态，默认为不启用
            var useMountTheme by remember { mutableStateOf(false) }

            // 根据当前设置创建 ThemeController，它会响应 themeMode, mountThemeColor, useMountTheme 的变化
            val themeController = remember(themeMode, mountThemeColor, useMountTheme) {
                if (useMountTheme && themeMode.name.startsWith("Monet")) {
                    // 如果是 Monet 模式且启用了自定义主题，使用自定义的 keyColor 创建控制器
                    ThemeController(
                        colorSchemeMode = themeMode,
                        keyColor = mountThemeColor.color,
                        paletteStyle = ThemePaletteStyle.TonalSpot,
                        colorSpec = ThemeColorSpec.Spec2021
                    )
                } else {
                    // 否则，只使用基本的主题模式创建控制器
                    ThemeController(themeMode)
                }
            }

            // 应用基础主题 DevInfoTheme
            DevInfoTheme {
                // 应用第三方的 Miuix 主题，并传入我们创建的控制器
                MiuixTheme(controller = themeController) {
                    // 使用 Box 铺满整个屏幕，用于承载主界面和可能的对话框遮罩
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            deviceId = deviceId,
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it }, // 向上传递主题模式变化
                            mountThemeColor = mountThemeColor,
                            onMountThemeColorChange = { color ->
                                mountThemeColor = color
                                useMountTheme = true // 当手动更改颜色时，自动启用自定义主题
                            },
                            useMountTheme = useMountTheme,
                            onUseMountThemeChange = { useMountTheme = it }
                        )
                    }
                }
            }
        }
    }

    // 主屏幕 Composable
    @OptIn(ExperimentalFoundationApi::class) // 因为使用了 combinedClickable
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
        val context = LocalContext.current // 获取当前上下文
        var selectedIndex by remember { mutableIntStateOf(0) } // 当前选中的底部导航栏索引，0为“信息”，1为“设置”
        val itemsState = remember { mutableStateListOf<ItemWithVisibility>() } // 存储带有可见性状态的设备信息列表
        var isLoading by remember { mutableStateOf(true) } // 设备信息是否正在加载
        var refreshTrigger by remember { mutableIntStateOf(0) } // 刷新触发器，值变化时触发 LaunchedEffect 重新加载数据

        // 各种对话框的显示状态
        var showAboutDialog by remember { mutableStateOf(false) }
        var showExportDialog by remember { mutableStateOf(false) }
        var showExportSuccessDialog by remember { mutableStateOf(false) }
        var exportedFilePath by remember { mutableStateOf("") } // 存储导出成功后的文件路径

        // 导航栏模式，默认为固定图标+选中文字
        var navBarMode by remember { mutableStateOf(NavigationBarMode.FIXED_ICON_LABEL) }

        // 主题选项列表
        val themeOptions = listOf("跟随系统", "浅色模式", "深色模式", "Monet 跟随系统", "Monet 浅色", "Monet 深色")
        // 根据当前 themeMode 确定下拉框的选中索引
        val selectedThemeModeIndex = when (themeMode) {
            ColorSchemeMode.System -> 0
            ColorSchemeMode.Light -> 1
            ColorSchemeMode.Dark -> 2
            ColorSchemeMode.MonetSystem -> 3
            ColorSchemeMode.MonetLight -> 4
            ColorSchemeMode.MonetDark -> 5
            else -> 0
        }

        // 判断当前是否为 Monet 模式，用于决定是否显示“主题颜色”设置项
        val isMonetMode = themeMode.name.startsWith("Monet")

        // 获取导航栏模式选项的列表和当前选中项的索引
        val navBarModeOptions = NavigationBarMode.entries.map { it.displayName }
        val selectedNavBarModeIndex = NavigationBarMode.entries.indexOf(navBarMode)

        // 构建主题颜色下拉框的 SpinnerEntry 列表，每个条目包含颜色预览、名称和描述
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

        // 应用启动时加载设备信息
        LaunchedEffect(Unit) {
            loadDeviceInfo(context, itemsState)
            isLoading = false // 加载完成
        }

        // 当 refreshTrigger 变化时，触发刷新操作（除了第一次，即值为0时）
        LaunchedEffect(refreshTrigger) {
            if (refreshTrigger > 0) {
                itemsState.clear() // 清空旧数据
                loadDeviceInfo(context, itemsState) // 重新加载
            }
        }

        // 底部导航栏的标签和图标
        val tabs = listOf("信息", "设置")
        val icons = listOf(
            MiuixIcons.Info,
            MiuixIcons.Settings
        )

        // 主界面布局 Box
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                // 顶部应用栏，标题根据 selectedIndex 动态变化
                topBar = {
                    Column {
                        TopAppBar(
                            title = when (selectedIndex) {
                                0 -> "设备信息"
                                1 -> "设置"
                                else -> "设备信息"
                            }
                        )
                        // 警告卡片放在 TopAppBar 下方
                        if (!BuildConfig.IS_OFFICIAL) {
                            TestVersionWarningCard(
                                versionName = BuildConfig.VERSION_NAME,
                                buildType = BuildConfig.BUILD_TYPE_NAME,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                // 底部导航栏，根据 navBarMode 的值显示不同的样式
                bottomBar = {
                    when (navBarMode) {
                        NavigationBarMode.FLOATING_ICON_ONLY -> {
                            FloatingNavigationBar(
                                mode = FloatingNavigationBarDisplayMode.IconOnly
                            ) {
                                // 动态生成导航项
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
                        // ... 其他模式类似，只是模式和标签略有不同 ...
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
                // 根据 Scaffold 的内边距，避免内容被遮挡
                Box(modifier = Modifier.padding(paddingValues)) {
                    // 根据底部导航栏的选中项，显示不同页面
                    when (selectedIndex) {
                        0 -> DeviceInfoPage(
                            deviceId = deviceId,
                            itemsState = itemsState,
                            isLoading = isLoading,
                            onRefresh = { refreshTrigger++ } // 刷新时增加触发器
                        )
                        1 -> SettingsAndAboutPage(
                            selectedThemeMode = selectedThemeModeIndex,
                            themeOptions = themeOptions,
                            onThemeChange = { index: Int ->
                                // 将下拉框索引转换为主题模式枚举并向上传递
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
                            onExportClick = { showExportDialog = true }, // 显示导出确认对话框
                            onAboutClick = { showAboutDialog = true },   // 显示关于对话框
                            onSourceCodeClick = { openSourceCode(context) }, // 打开源码链接
                            onCommunityClick = { openCommunity(context) }   // 打开社区链接
                        )
                    }
                }
            }

            // 关于对话框
            AboutDialog(show = showAboutDialog, onDismiss = { showAboutDialog = false })

            // 导出确认对话框
            ExportConfirmDialog(
                show = showExportDialog,
                onConfirm = {
                    showExportDialog = false
                    // 执行导出操作
                    exportHelper.exportModule(
                        deviceId = deviceId,
                        itemsState = itemsState,
                        onSuccess = { path ->
                            exportedFilePath = path
                            showExportSuccessDialog = true // 导出成功，显示成功对话框
                        },
                        onError = { error ->
                            Toast.makeText(context, "导出失败: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onDismiss = { showExportDialog = false }
            )

            // 导出成功对话框
            ExportSuccessDialog(
                show = showExportSuccessDialog,
                filePath = exportedFilePath,
                onDismiss = { showExportSuccessDialog = false }
            )
        }
    }

    // 设置和关于页面的 Composable
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
                .padding(12.dp), // 列的外边距
            verticalArrangement = Arrangement.spacedBy(6.dp) // 项目之间的垂直间距
        ) {
            // “外观”分类标题
            item {
                CategoryHeader(title = "外观")
            }

            // 主题选择（深色模式）的下拉菜单
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp) // Card 内部无填充，让 Preference 撑满
                ) {
                    OverlayDropdownPreference(
                        title = "深色模式",
                        summary = themeOptions[selectedThemeMode], // 概要显示当前选中项
                        items = themeOptions,
                        selectedIndex = selectedThemeMode,
                        onSelectedIndexChange = onThemeChange
                    )
                }
            }

            // 主题颜色选择项，仅在 Monet 模式下可用
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
                // 非 Monet 模式下，显示一个不可用的 ArrowPreference，提示用户切换模式
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

            // 导航栏模式选择项
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

            // “工具”分类标题
            item {
                Spacer(modifier = Modifier.height(12.dp)) // 添加一点间距
                CategoryHeader(title = "工具")
            }

            // 导出模块项
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

            // “关于”部分的分类标题
            item {
                Spacer(modifier = Modifier.height(12.dp))
                CategoryHeader(title = "关于")
            }

            // 关于应用项
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

            // 查看源代码项
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

            // 加入社区项
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

            // 底部版本信息
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // 居中显示的文本
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

    // 加载设备信息的挂起函数，逐个添加并显示信息项
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

        // 将所有信息项转换为带可见性状态的 ItemWithVisibility 并加入到可观察列表
        itemsState.addAll(infoList.map { item ->
            ItemWithVisibility(item, mutableStateOf(false))
        })

        // 逐个显示信息项，产生逐条出现的动画效果
        itemsState.forEachIndexed { _, item ->
            delay(30) // 每个项目延迟30ms显示
            item.visible.value = true
        }
    }

    // 设备信息展示页面的 Composable
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DeviceInfoPage(
        deviceId: String,
        itemsState: List<ItemWithVisibility>,
        isLoading: Boolean, // 页面是否处于加载状态
        onRefresh: () -> Unit // 下拉刷新时触发的回调
    ) {
        val ctx = LocalContext.current
        val clipboardManager = LocalClipboardManager.current // 获取剪贴板管理器

        var isRefreshing by remember { mutableStateOf(false) } // 下拉刷新状态
        val pullToRefreshState = rememberPullToRefreshState() // 下拉刷新手势状态

        // 当 isRefreshing 变为 true 时，触发刷新操作
        LaunchedEffect(isRefreshing) {
            if (isRefreshing) {
                onRefresh()
                delay(500) // 模拟刷新延迟，让用户看到刷新动画
                isRefreshing = false // 结束刷新
            }
        }

        // 如果正在初次加载，显示加载指示器
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // 加载完成后，显示内容
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(0.dp)
            ) {
                // 下拉刷新容器
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = { isRefreshing = true },
                    pullToRefreshState = pullToRefreshState,
                    refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新...", "刷新成功"),
                    color = MiuixTheme.colorScheme.primary
                ) {
                    // 可滚动的信息列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp), // 列的内边距
                        verticalArrangement = Arrangement.spacedBy(6.dp) // 项目间距
                    ) {
                        // 第一项：设备唯一标识，支持长按复制
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
                                                // 长按复制 deviceId 到剪贴板
                                                clipboardManager.setText(AnnotatedString(deviceId))
                                                Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show()
                                            },
                                            onClick = {} // 单击无操作
                                        )
                                    )
                                }
                            }
                        }

                        // 逐个显示带索引的设备信息项，支持长按复制
                        itemsIndexed(itemsState) { _, currentItem ->
                            AnimatedVisibility(
                                visible = currentItem.visible.value, // 控制项目的可见性（实现逐个出现的动画）
                                enter = fadeIn() + slideInHorizontally() // 出现动画：淡入 + 水平滑入
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onLongClick = {
                                                // 长按复制该项的值
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
                                        horizontalArrangement = Arrangement.SpaceBetween // 键和值分布在两端
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

    // 一个用于在设置页面显示分类标题的 Composable
    @Composable
    fun CategoryHeader(title: String) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.primary, // 使用主题的主色调
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    // “关于”对话框 Composable
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
                    // 应用图标的圆形卡片
                    Card(
                        modifier = Modifier.size(80.dp),
                        cornerRadius = 40.dp, // 大圆角使其变为圆形
                        insideMargin = PaddingValues(0.dp),
                        pressFeedbackType = PressFeedbackType.Tilt // 按下时有倾斜的反馈效果
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
                                    .padding(12.dp) // 图标的内边距，使其不贴边
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 应用名称
                    Text(
                        text = "设备信息查看器",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // 应用版本
                    Text(
                        text = "版本 ${getAppVersionName(context)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 说明卡片
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

                    // 确定按钮，右对齐
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

    /**
     * 导出确认对话框
     *
     * 动态主题适配的核心原理：
     * 1. 通过 MiuixTheme.colorScheme 获取当前主题的语义化颜色令牌（Color Tokens）
     * 2. 使用语义化颜色名称（如 onSurface、onSurfaceSecondary、onPrimary）而非硬编码颜色值
     * 3. 颜色令牌会根据当前主题模式（浅色/深色/Monet）自动返回合适的 ARGB 颜色值
     * 4. 当用户切换主题时，Compose 会检测到 colorScheme 状态变化，自动重组（Recompose）所有使用这些颜色的 UI 组件
     * 5. 重组后的 UI 会使用新主题的颜色值，实现无需重启应用的实时主题切换
     *
     * 颜色令牌说明：
     * - onSurface: 绘制在表面容器上的主要文本颜色（高对比度）
     * - onSurfaceSecondary: 绘制在表面容器上的次要文本颜色（中等对比度，用于标签、辅助说明）
     * - onPrimary: 绘制在主色按钮/组件上的文本颜色（确保与主色背景形成足够对比度）
     *
     * Miuix 颜色系统特性：
     * - 浅色模式：自动使用浅色主题的颜色值（如深色文字、浅色背景）
     * - 深色模式：自动使用深色主题的颜色值（如浅色文字、深色背景）
     * - Monet 模式：基于系统壁纸动态生成主题色，所有语义化颜色会相应调整
     */
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

                // ========== 动态主题适配的关键代码 ==========
                // 获取当前主题的颜色方案（响应式状态）
                // colorScheme 是 Compose State 对象，其变化会自动触发 UI 重组
                val colorScheme = MiuixTheme.colorScheme

                // onSurface：表面上的主要文本颜色
                // 浅色主题 → 黑色/深灰色（高对比度）
                // 深色主题 → 白色/浅灰色（高对比度）
                val textColor = colorScheme.onSurface

                // onSurfaceSecondary：表面上的次要文本颜色
                // 浅色主题 → 半透明黑色（降低对比度，突出主要信息）
                // 深色主题 → 半透明白色（降低对比度）
                // 适用于：标签、摘要、辅助说明等非核心信息
                val secondaryTextColor = colorScheme.onSurfaceSecondary

                // onPrimary：主色调容器上的文本颜色
                // 用于确保按钮文字与背景有足够的可读性对比度
                // 浅色主题（primary 为蓝色）→ 白色文字
                // 深色主题（primary 为浅蓝）→ 深色文字
                val onPrimaryColor = colorScheme.onPrimary

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 主要说明文字使用高对比度的 onSurface
                    Text(
                        text = "即将导出当前设备信息为改机型模块",
                        fontSize = 14.sp,
                        color = textColor  // 主题切换时自动更新颜色值
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 导出信息卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(12.dp)
                        // Card 的背景色来自主题的 surface 色值
                        // 浅色主题：浅灰色/白色背景
                        // 深色主题：深色背景
                    ) {
                        Column {
                            // InfoRow 接收动态颜色参数
                            InfoRow(
                                label = "导出格式",
                                value = "ZIP 压缩包",
                                labelColor = secondaryTextColor,  // 标签使用次要颜色
                                valueColor = textColor            // 值使用主要颜色
                            )
                            InfoRow(
                                label = "保存位置",
                                value = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).absolutePath,
                                labelColor = secondaryTextColor,
                                valueColor = textColor,
                                valueAlignment = Alignment.End
                            )
                            InfoRow(
                                label = "文件名",
                                value = "${Build.MODEL}.zip",
                                labelColor = secondaryTextColor,
                                valueColor = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 确认提示文字
                    Text(
                        text = "确认导出吗？",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            text = "取消",
                            onClick = { dismiss?.invoke() }
                            // TextButton 内部使用主题的次要色/灰色系
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirm()
                                dismiss?.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(
                                color = colorScheme.primary  // 按钮背景使用主题主色
                            )
                        ) {
                            Text(
                                text = "确认导出",
                                color = onPrimaryColor  // 按钮文字使用 onPrimary
                                // 确保在任何主题下文字都清晰可读
                            )
                        }
                    }
                }
            }
        )
    }

    // 导出成功对话框 Composable
    @Composable
    fun ExportSuccessDialog(
        show: Boolean,
        filePath: String, // 导出文件的完整路径
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
                    // 成功图标
                    Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally),
                        tint = Color(0xFF4CAF50) // 绿色图标表示成功
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "文件已保存至：",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 显示文件路径的卡片，支持长按复制
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

                    // 确定按钮，右对齐
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onDismiss() // 调用 dismiss 回调
                                dismiss?.invoke() // 关闭对话框
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

    /**
     * 信息行组件
     *
     * 设计思路：
     * - 接收颜色参数，由调用方根据主题动态传入
     * - 不硬编码颜色值，保持组件的可复用性
     * - valueAlignment 参数控制值的对齐方式
     */
    @Composable
    private fun InfoRow(
        label: String,
        value: String,
        labelColor: Color = Color.Unspecified,  // 默认值，表示不指定颜色
        valueColor: Color = Color.Unspecified,
        valueAlignment: Alignment.Horizontal? = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签文字，使用传入的颜色（由主题提供）
            Text(
                text = label,
                fontSize = 13.sp,
                color = labelColor  // 当主题切换时，这里会跟随变为新主题的颜色
            )

            // 值文字，使用 weight 修饰符实现右对齐
            Text(
                text = value,
                fontSize = 13.sp,
                color = valueColor,  // 同样跟随主题动态变化
                textAlign = valueAlignment?.let {
                    when (it) {
                        Alignment.Start -> TextAlign.Start
                        Alignment.CenterHorizontally -> TextAlign.Center
                        Alignment.End -> TextAlign.End
                        else -> TextAlign.Start
                    }
                } ?: TextAlign.Start,
                // weight(1f) 让此文本占据剩余空间
                // 配合 textAlign = TextAlign.End 实现内容右对齐
                modifier = if (valueAlignment == Alignment.End) {
                    Modifier.weight(1f, fill = false)
                } else {
                    Modifier
                }
            )
        }
    }


    // ==================== 辅助方法 ====================

    // 打开项目源代码链接（GitHub）
    private fun openSourceCode(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/FIOIU8/DevInfo".toUri())
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    // 打开社区链接（酷安）
    private fun openCommunity(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, "https://www.coolapk.com/u/32334444".toUri())
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    // 安全地获取 Android ID
    private fun getAndroidIdSafe(context: Context): String {
        return safeGet {
            @Suppress("HardwareIds") // 忽略硬件 ID 警告
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            androidId ?: "未知"
        }
    }

    // 安全地获取设备序列号
    @Suppress("MissingPermission", "HardwareIds")
    private fun getSerialNumberSafe(): String {
        return safeGet {
            Build.getSerial()
        }
    }

    // 获取蓝牙状态（开启/关闭）
    private fun getBluetoothState(context: Context): String {
        return safeGet {
            val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            if (bluetoothManager?.adapter?.isEnabled == true) "开启" else "关闭"
        }
    }

    // 获取设备总内存（单位：MB）
    private fun getTotalMemory(context: Context): String {
        val mi = ActivityManager.MemoryInfo()
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(mi)
        return "${mi.totalMem / 1024 / 1024} MB"
    }

    // 获取设备可用内存（单位：MB）
    private fun getAvailMemory(context: Context): String {
        val mi = ActivityManager.MemoryInfo()
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(mi)
        return "${mi.availMem / 1024 / 1024} MB"
    }

    // 获取内部存储总空间（单位：GB）
    private fun getTotalStorage() =
        safeGet { "${Environment.getExternalStorageDirectory().totalSpace / 1024 / 1024 / 1024} GB" }

    // 获取内部存储可用空间（单位：GB）
    private fun getFreeStorage() =
        safeGet { "${Environment.getExternalStorageDirectory().freeSpace / 1024 / 1024 / 1024} GB" }

    // 获取电池电量百分比
    private fun getBatteryLevel(context: Context) = safeGet {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toString() + "%"
    }

    // 获取电池充电状态
    private fun getBatteryCharging(context: Context) = safeGet {
        val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        if (bm.isCharging) "充电中" else "未充电"
    }

    // 获取摄像头数量
    private fun getCameraCount(context: Context) = safeGet {
        val cam = context.getSystemService(CAMERA_SERVICE) as CameraManager
        cam.cameraIdList.size.toString()
    }

    // 获取当前网络类型（Wi-Fi、移动网络等）
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

    // 获取网络运营商名称
    private fun getNetworkOperator(context: Context) = safeGet {
        val tm = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        tm.networkOperatorName ?: "未知"
    }

    // 获取 SIM 卡状态
    @Suppress("DEPRECATION") // 忽略 TelephonyManager.simState 的弃用警告
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

    // 获取应用版本名
    private fun getAppVersionName(context: Context): String {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            p.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    // 获取应用版本码
    private fun getAppVersionCode(context: Context): Long {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            p.longVersionCode
        } catch (_: Exception) {
            1
        }
    }

    // 一个安全执行代码块的通用方法，捕获异常并返回默认值 “未知”
    private inline fun safeGet(default: String = "未知", block: () -> String): String {
        return try {
            block()
        } catch (_: Exception) {
            default
        }
    }
}

/**
 * 测试版本警告卡片
 * 背景半透明红色，文字纯红色
 */
@Composable
fun TestVersionWarningCard(
    modifier: Modifier = Modifier,
    versionName: String = "",
    buildType: String = "dev"
) {
    val warningBgColor = Color(0x1AFF0000)
    val warningTextColor = Color(0xFFE53935)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(warningBgColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "⚠️", fontSize = 16.sp, color = warningTextColor)
                Column {
                    Text(
                        text = "测试版本",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = warningTextColor
                    )
                    Text(
                        text = "当前为开发测试版，不建议在生产环境使用",
                        fontSize = 11.sp,
                        color = warningTextColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

// 用于存储设备信息项（键值对）的数据类
data class DeviceInfoItem(val key: String, val value: String)
// 用于在列表中绑定设备信息项和其可见性状态的数据类，以实现逐条显示动画
data class ItemWithVisibility(val item: DeviceInfoItem, val visible: MutableState<Boolean>)