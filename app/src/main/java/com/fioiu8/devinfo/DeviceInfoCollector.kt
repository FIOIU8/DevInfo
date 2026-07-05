package com.fioiu8.devinfo

import android.app.ActivityManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.telephony.TelephonyManager
import com.fioiu8.devinfo.model.DeviceInfoItem
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.model.ItemWithVisibility
import java.util.Locale
import java.util.TimeZone

private inline fun safeGet(default: String = "未知", block: () -> String): String {
    return try {
        block()
    } catch (_: Exception) {
        default
    }
}

class DeviceInfoCollector(private val context: Context) {

    private var cachedVersionName: String? = null
    private var cachedVersionCode: Long? = null

    fun getAppVersionName(): String {
        if (cachedVersionName == null) cachePackageInfo()
        return cachedVersionName ?: "1.0.0"
    }

    fun getAppVersionCode(): Long {
        if (cachedVersionCode == null) cachePackageInfo()
        return cachedVersionCode ?: 1
    }

    private fun cachePackageInfo() {
        try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            cachedVersionName = p.versionName ?: "1.0.0"
            cachedVersionCode = p.longVersionCode
        } catch (_: Exception) {
            cachedVersionName = "1.0.0"
            cachedVersionCode = 1
        }
    }

    fun collectDeviceInfo(): List<DeviceInfoItem> {
        val list = mutableListOf<DeviceInfoItem>()

        list += DeviceInfoItem("ANDROID_ID", getAndroidIdSafe(), InfoCategory.IDENTIFIERS)
        list += DeviceInfoItem("序列号", getSerialNumberSafe(), InfoCategory.IDENTIFIERS)
        list += DeviceInfoItem("品牌", Build.BRAND, InfoCategory.DEVICE)
        list += DeviceInfoItem("制造商", Build.MANUFACTURER, InfoCategory.DEVICE)
        list += DeviceInfoItem("型号", Build.MODEL, InfoCategory.DEVICE)
        list += DeviceInfoItem("产品", Build.PRODUCT, InfoCategory.DEVICE)
        list += DeviceInfoItem("设备", Build.DEVICE, InfoCategory.DEVICE)
        list += DeviceInfoItem("主板", Build.BOARD, InfoCategory.DEVICE)
        list += DeviceInfoItem("硬件", Build.HARDWARE, InfoCategory.DEVICE)
        list += DeviceInfoItem("引导程序", Build.BOOTLOADER, InfoCategory.DEVICE)
        list += DeviceInfoItem("构建ID", Build.ID, InfoCategory.DEVICE)
        list += DeviceInfoItem("标签", Build.TAGS, InfoCategory.DEVICE)
        list += DeviceInfoItem("时间", Build.TIME.toString(), InfoCategory.DEVICE)
        list += DeviceInfoItem("类型", Build.TYPE, InfoCategory.DEVICE)

        list += DeviceInfoItem("CPU架构", Build.SUPPORTED_ABIS.joinToString(), InfoCategory.SYSTEM)
        list += DeviceInfoItem("CPU核心数", Runtime.getRuntime().availableProcessors().toString(), InfoCategory.SYSTEM)
        list += DeviceInfoItem("SDK版本", Build.VERSION.SDK_INT.toString(), InfoCategory.SYSTEM)
        list += DeviceInfoItem("Android版本", Build.VERSION.RELEASE, InfoCategory.SYSTEM)
        list += DeviceInfoItem("安全补丁", safeGet { Build.VERSION.SECURITY_PATCH }, InfoCategory.SYSTEM)
        list += DeviceInfoItem("基带版本", safeGet { Build.getRadioVersion() }, InfoCategory.SYSTEM)

        list += DeviceInfoItem("语言", Locale.getDefault().language, InfoCategory.LOCALE)
        list += DeviceInfoItem("国家", Locale.getDefault().country, InfoCategory.LOCALE)
        list += DeviceInfoItem("时区", TimeZone.getDefault().id, InfoCategory.LOCALE)

        val dm = context.resources.displayMetrics
        list += DeviceInfoItem("屏幕DPI", dm.densityDpi.toString(), InfoCategory.DISPLAY)
        list += DeviceInfoItem("屏幕宽度", dm.widthPixels.toString(), InfoCategory.DISPLAY)
        list += DeviceInfoItem("屏幕高度", dm.heightPixels.toString(), InfoCategory.DISPLAY)
        list += DeviceInfoItem("刷新率", safeGet { context.display.refreshRate.toString() }, InfoCategory.DISPLAY)
        list += DeviceInfoItem("字体缩放", safeGet { context.resources.configuration.fontScale.toString() }, InfoCategory.DISPLAY)

        list += DeviceInfoItem("总内存", getTotalMemory(), InfoCategory.STORAGE)
        list += DeviceInfoItem("可用内存", getAvailMemory(), InfoCategory.STORAGE)
        list += DeviceInfoItem("存储总量", getTotalStorage(), InfoCategory.STORAGE)
        list += DeviceInfoItem("可用存储", getFreeStorage(), InfoCategory.STORAGE)

        list += DeviceInfoItem("电池电量", getBatteryLevel(), InfoCategory.BATTERY)
        list += DeviceInfoItem("充电状态", getBatteryCharging(), InfoCategory.BATTERY)

        list += DeviceInfoItem("NFC功能", if (NfcAdapter.getDefaultAdapter(context) != null) "支持" else "不支持", InfoCategory.NETWORK)
        list += DeviceInfoItem("摄像头数量", getCameraCount(), InfoCategory.NETWORK)
        list += DeviceInfoItem("蓝牙状态", getBluetoothState(), InfoCategory.NETWORK)
        list += DeviceInfoItem("网络类型", getNetworkType(), InfoCategory.NETWORK)
        list += DeviceInfoItem("运营商", getNetworkOperator(), InfoCategory.NETWORK)
        list += DeviceInfoItem("SIM卡状态", getSimState(), InfoCategory.NETWORK)

        list += DeviceInfoItem("包名", context.packageName, InfoCategory.APP)
        list += DeviceInfoItem("应用版本名", getAppVersionName(), InfoCategory.APP)
        list += DeviceInfoItem("应用版本码", getAppVersionCode().toString(), InfoCategory.APP)

        return list
    }

    private fun getAndroidIdSafe(): String = safeGet {
        @Suppress("HardwareIds")
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "未知"
    }

    @Suppress("MissingPermission", "HardwareIds")
    private fun getSerialNumberSafe(): String = safeGet { Build.getSerial() }

    private fun getBluetoothState(): String = safeGet {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        if (bm?.adapter?.isEnabled == true) "开启" else "关闭"
    }

    private fun getTotalMemory(): String {
        val mi = getMemoryInfo()
        return "${mi.totalMem / 1024 / 1024} MB"
    }

    private fun getAvailMemory(): String {
        val mi = getMemoryInfo()
        return "${mi.availMem / 1024 / 1024} MB"
    }

    /** 返回内存使用百分比 [0, 100] */
    fun getMemoryUsagePercent(): Float {
        return try {
            val mi = getMemoryInfo()
            if (mi.totalMem > 0) ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100f) else 0f
        } catch (_: Exception) {
            0f
        }
    }

    /** 获取 ActivityManager.MemoryInfo，抽取内存相关方法的公共逻辑 */
    private fun getMemoryInfo(): ActivityManager.MemoryInfo {
        val mi = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return mi
    }

    private fun getTotalStorage() = safeGet {
        "${Environment.getExternalStorageDirectory().totalSpace / 1024 / 1024 / 1024} GB"
    }

    private fun getFreeStorage() = safeGet {
        "${Environment.getExternalStorageDirectory().freeSpace / 1024 / 1024 / 1024} GB"
    }

    /** 返回存储使用百分比 [0, 100] */
    fun getStorageUsagePercent(): Float {
        return try {
            val total = Environment.getExternalStorageDirectory().totalSpace
            val free = Environment.getExternalStorageDirectory().freeSpace
            if (total > 0) ((total - free).toFloat() / total * 100f) else 0f
        } catch (_: Exception) {
            0f
        }
    }

    private fun getBatteryLevel() = safeGet {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        "${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
    }

    private fun getBatteryCharging() = safeGet {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        if (bm.isCharging) "充电中" else "未充电"
    }

    private fun getCameraCount() = safeGet {
        val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cam.cameraIdList.size.toString()
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "未知"
        return when {
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "蓝牙"
            else -> "未知"
        }
    }

    private fun getNetworkOperator() = safeGet {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tm.networkOperatorName ?: "未知"
    }

    @Suppress("DEPRECATION")
    private fun getSimState() = safeGet {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
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
}
