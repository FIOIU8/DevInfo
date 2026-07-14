package com.fioiu8.devinfo.model

import androidx.compose.runtime.MutableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 设备信息分类
 */
enum class InfoCategory(
    val displayName: String,
    val displayNameResId: Int,
    val description: String,
    val descriptionResId: Int
) {
    DEVICE("设备", com.fioiu8.devinfo.R.string.category_device, "设备硬件与构建信息", com.fioiu8.devinfo.R.string.category_desc_device),
    IDENTIFIERS("标识符", com.fioiu8.devinfo.R.string.category_identifiers, "设备唯一标识信息", com.fioiu8.devinfo.R.string.category_desc_identifiers),
    SYSTEM("系统", com.fioiu8.devinfo.R.string.category_system, "操作系统与CPU信息", com.fioiu8.devinfo.R.string.category_desc_system),
    LOCALE("区域", com.fioiu8.devinfo.R.string.category_locale, "语言、国家与时区", com.fioiu8.devinfo.R.string.category_desc_locale),
    DISPLAY("显示", com.fioiu8.devinfo.R.string.category_display, "屏幕相关参数", com.fioiu8.devinfo.R.string.category_desc_display),
    STORAGE("存储", com.fioiu8.devinfo.R.string.category_storage, "内存与存储空间", com.fioiu8.devinfo.R.string.category_desc_storage),
    BATTERY("电池", com.fioiu8.devinfo.R.string.category_battery, "电池状态信息", com.fioiu8.devinfo.R.string.category_desc_battery),
    NETWORK("网络", com.fioiu8.devinfo.R.string.category_network, "网络连接与运营商", com.fioiu8.devinfo.R.string.category_desc_network),
    APP("应用", com.fioiu8.devinfo.R.string.category_app, "应用自身信息", com.fioiu8.devinfo.R.string.category_desc_app)
}

/**
 * MD3 主题模式
 */
enum class ThemeMode(val displayName: String, val displayNameResId: Int) {
    SYSTEM("跟随系统", com.fioiu8.devinfo.R.string.theme_mode_system),
    LIGHT("浅色模式", com.fioiu8.devinfo.R.string.theme_mode_light),
    DARK("深色模式", com.fioiu8.devinfo.R.string.theme_mode_dark),
    DYNAMIC_SYSTEM("动态颜色·跟随系统", com.fioiu8.devinfo.R.string.theme_mode_dynamic_system),
    DYNAMIC_LIGHT("动态颜色·浅色", com.fioiu8.devinfo.R.string.theme_mode_dynamic_light),
    DYNAMIC_DARK("动态颜色·深色", com.fioiu8.devinfo.R.string.theme_mode_dynamic_dark);

    val isDynamic: Boolean get() = name.startsWith("DYNAMIC")
}

/**
 * 自定义主题色选项
 */
enum class MountThemeColor(
    val displayName: String,
    val displayNameResId: Int,
    val color: Color,
    val description: String,
    val descriptionResId: Int
) {
    DEFAULT("默认", com.fioiu8.devinfo.R.string.mount_color_default, Color(0xFF4A90D9), "清新蓝色", com.fioiu8.devinfo.R.string.mount_color_desc_default),
    RED("红色", com.fioiu8.devinfo.R.string.mount_color_red, Color(0xFFE74C3C), "热情红色", com.fioiu8.devinfo.R.string.mount_color_desc_red),
    ORANGE("橙色", com.fioiu8.devinfo.R.string.mount_color_orange, Color(0xFFE67E22), "活力橙色", com.fioiu8.devinfo.R.string.mount_color_desc_orange),
    GREEN("绿色", com.fioiu8.devinfo.R.string.mount_color_green, Color(0xFF2ECC71), "自然绿色", com.fioiu8.devinfo.R.string.mount_color_desc_green),
    TEAL("青色", com.fioiu8.devinfo.R.string.mount_color_teal, Color(0xFF1ABC9C), "清新青色", com.fioiu8.devinfo.R.string.mount_color_desc_teal),
    PURPLE("紫色", com.fioiu8.devinfo.R.string.mount_color_purple, Color(0xFF9B59B6), "优雅紫色", com.fioiu8.devinfo.R.string.mount_color_desc_purple),
    PINK("粉色", com.fioiu8.devinfo.R.string.mount_color_pink, Color(0xFFE91E63), "甜美粉色", com.fioiu8.devinfo.R.string.mount_color_desc_pink),
    DARK("深色", com.fioiu8.devinfo.R.string.mount_color_dark, Color(0xFF34495E), "沉稳深色", com.fioiu8.devinfo.R.string.mount_color_desc_dark)
}

/**
 * 设备信息单项数据。
 *
 * @property key 信息项名称（中文回退）
 * @property keyResId 信息项名称对应的字符串资源ID，0=使用key
 * @property value 信息项值
 * @property category 所属分类
 */
data class DeviceInfoItem(
    val key: String,
    val keyResId: Int = 0,
    val value: String,
    val category: InfoCategory,
    val icon: ImageVector = Icons.Outlined.Info
)

/**
 * 带可见性状态的设备信息项，用于列表交错动画。
 *
 * @property item 设备信息数据
 * @property visible 控制该项是否可见（用于入场动画，初始为 false）
 */
data class ItemWithVisibility(val item: DeviceInfoItem, val visible: MutableState<Boolean>)

/**
 * 改机型模块导出所需精简信息。
 *
 * @property deviceId 持久化设备唯一标识
 * @property brand 品牌（如 Xiaomi）
 * @property manufacturer 制造商（如 Xiaomi）
 * @property model 型号（如 23127PN0CC）
 * @property device 设备代号（如 houji）
 * @property product 产品名（如 houji）
 * @property versionRelease Android 版本（如 15）
 * @property versionSdk SDK 版本号（如 35）
 * @property securityPatch 安全补丁日期（如 2025-06-05）
 * @property supportedAbis 所有支持的 ABI 列表
 * @property supported32BitAbis 32 位 ABI 列表
 * @property supported64BitAbis 64 位 ABI 列表
 */
data class MenuInfo(
    val deviceId: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val versionRelease: String,
    val versionSdk: String,
    val securityPatch: String,
    val supportedAbis: List<String>,
 val supported32BitAbis: List<String>,
 val supported64BitAbis: List<String>
)

/**
 * App language selection mode.
 * SYSTEM — follow device locale; CUSTOM — user inputs a custom locale tag.
 */
enum class AppLanguage(
    val displayNameResId: Int,
    val localeTag: String?,
    val isCustom: Boolean = false
) {
    SYSTEM(com.fioiu8.devinfo.R.string.language_system, null),
    SIMPLIFIED_CHINESE(com.fioiu8.devinfo.R.string.language_chinese, "zh"),
    ENGLISH(com.fioiu8.devinfo.R.string.language_english, "en"),
    JAPANESE(com.fioiu8.devinfo.R.string.language_japanese, "ja"),
    CUSTOM(com.fioiu8.devinfo.R.string.language_custom, "_custom", isCustom = true);
}
