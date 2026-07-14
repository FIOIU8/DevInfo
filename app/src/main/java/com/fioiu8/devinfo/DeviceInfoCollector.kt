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
import com.fioiu8.devinfo.model.ItemWithVisibility
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.ui.itemIconByResId
import com.fioiu8.devinfo.model.InfoCategory
import java.util.Locale
import java.util.TimeZone

private inline fun safeGet(default: String, block: () -> String): String {
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

        list += infoItem(R.string.device_serial, getAndroidIdSafe(), InfoCategory.IDENTIFIERS)
        list += infoItem(R.string.device_serial, getSerialNumberSafe(), InfoCategory.IDENTIFIERS)
        list += infoItem(R.string.device_brand, Build.BRAND, InfoCategory.DEVICE)
        list += infoItem(R.string.device_manufacturer, Build.MANUFACTURER, InfoCategory.DEVICE)
        list += infoItem(R.string.device_model, Build.MODEL, InfoCategory.DEVICE)
        list += infoItem(R.string.device_product, Build.PRODUCT, InfoCategory.DEVICE)
        list += infoItem(R.string.device_device, Build.DEVICE, InfoCategory.DEVICE)
        list += infoItem(R.string.device_board, Build.BOARD, InfoCategory.DEVICE)
        list += infoItem(R.string.device_hardware, Build.HARDWARE, InfoCategory.DEVICE)
        list += infoItem(R.string.device_bootloader, Build.BOOTLOADER, InfoCategory.DEVICE)
        list += infoItem(R.string.device_build_id, Build.ID, InfoCategory.DEVICE)
        list += infoItem(R.string.device_tags, Build.TAGS, InfoCategory.DEVICE)
        list += infoItem(R.string.device_time, Build.TIME.toString(), InfoCategory.DEVICE)
        list += infoItem(R.string.device_type, Build.TYPE, InfoCategory.DEVICE)

        list += infoItem(R.string.system_cpu_arch, Build.SUPPORTED_ABIS.joinToString(), InfoCategory.SYSTEM)
        list += infoItem(R.string.system_cpu_cores, Runtime.getRuntime().availableProcessors().toString(), InfoCategory.SYSTEM)
        list += infoItem(R.string.system_sdk_version, Build.VERSION.SDK_INT.toString(), InfoCategory.SYSTEM)
        list += infoItem(R.string.system_android_version, Build.VERSION.RELEASE, InfoCategory.SYSTEM)
        list += infoItem(R.string.system_security_patch, safeGet(context.getString(R.string.status_unknown)) { Build.VERSION.SECURITY_PATCH }, InfoCategory.SYSTEM)
        list += infoItem(R.string.system_baseband, safeGet(context.getString(R.string.status_unknown)) { Build.getRadioVersion() }, InfoCategory.SYSTEM)

        list += infoItem(R.string.locale_language, Locale.getDefault().language, InfoCategory.LOCALE)
        list += infoItem(R.string.locale_country, Locale.getDefault().country, InfoCategory.LOCALE)
        list += infoItem(R.string.locale_timezone, TimeZone.getDefault().id, InfoCategory.LOCALE)

        val dm = context.resources.displayMetrics
        list += infoItem(R.string.display_dpi, dm.densityDpi.toString(), InfoCategory.DISPLAY)
        list += infoItem(R.string.display_width, dm.widthPixels.toString(), InfoCategory.DISPLAY)
        list += infoItem(R.string.display_height, dm.heightPixels.toString(), InfoCategory.DISPLAY)
        list += infoItem(R.string.display_refresh_rate, safeGet(context.getString(R.string.status_unknown)) { context.display.refreshRate.toString() }, InfoCategory.DISPLAY)
        list += infoItem(R.string.display_font_scale, safeGet(context.getString(R.string.status_unknown)) { context.resources.configuration.fontScale.toString() }, InfoCategory.DISPLAY)

        list += infoItem(R.string.storage_total_ram, getTotalMemory(), InfoCategory.STORAGE)
        list += infoItem(R.string.storage_available_ram, getAvailMemory(), InfoCategory.STORAGE)
        list += infoItem(R.string.storage_total, getTotalStorage(), InfoCategory.STORAGE)
        list += infoItem(R.string.storage_available, getFreeStorage(), InfoCategory.STORAGE)

        list += infoItem(R.string.battery_level_label, getBatteryLevel(), InfoCategory.BATTERY)
        list += infoItem(R.string.battery_charging_state, getBatteryCharging(), InfoCategory.BATTERY)

        list += infoItem(R.string.network_nfc, if (NfcAdapter.getDefaultAdapter(context) != null) context.getString(R.string.status_supported) else context.getString(R.string.status_not_supported), InfoCategory.NETWORK)
        list += infoItem(R.string.network_camera_count, getCameraCount(), InfoCategory.NETWORK)
        list += infoItem(R.string.network_bluetooth_state, getBluetoothState(), InfoCategory.NETWORK)
        list += infoItem(R.string.network_type, getNetworkType(), InfoCategory.NETWORK)
        list += infoItem(R.string.network_operator, getNetworkOperator(), InfoCategory.NETWORK)
        list += infoItem(R.string.network_sim_state, getSimState(), InfoCategory.NETWORK)

        list += infoItem(R.string.app_package, context.packageName, InfoCategory.APP)
        list += infoItem(R.string.app_version_name, getAppVersionName(), InfoCategory.APP)
        list += infoItem(R.string.app_version_code, getAppVersionCode().toString(), InfoCategory.APP)

        return list
    }

    private fun getAndroidIdSafe(): String = safeGet(context.getString(R.string.status_unknown)) {
        @Suppress("HardwareIds")
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: context.getString(R.string.status_unknown)
    }

    @Suppress("MissingPermission", "HardwareIds")
    private fun getSerialNumberSafe(): String = safeGet(context.getString(R.string.status_unknown)) { Build.getSerial() }

    private fun getBluetoothState(): String = safeGet(context.getString(R.string.status_unknown)) {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        if (bm?.adapter?.isEnabled == true) context.getString(R.string.status_enabled) else context.getString(R.string.status_disabled)
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

    private fun getTotalStorage() = safeGet(context.getString(R.string.status_unknown)) {
        "${Environment.getExternalStorageDirectory().totalSpace / 1024 / 1024 / 1024} GB"
    }

    private fun getFreeStorage() = safeGet(context.getString(R.string.status_unknown)) {
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

    private fun getBatteryLevel() = safeGet(context.getString(R.string.status_unknown)) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        "${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
    }

    private fun getBatteryCharging() = safeGet(context.getString(R.string.status_unknown)) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        if (bm.isCharging) context.getString(R.string.status_charging) else context.getString(R.string.status_not_charging)
    }

    private fun getCameraCount() = safeGet(context.getString(R.string.status_unknown)) {
        val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cam.cameraIdList.size.toString()
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return context.getString(R.string.status_unknown)
        return when {
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> context.getString(R.string.status_wifi)
            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> context.getString(R.string.status_cellular)
            nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> context.getString(R.string.status_ethernet)
            nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> context.getString(R.string.status_bluetooth)
            else -> context.getString(R.string.status_unknown)
        }
    }

    private fun getNetworkOperator() = safeGet(context.getString(R.string.status_unknown)) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tm.networkOperatorName ?: context.getString(R.string.status_unknown)
    }

    @Suppress("DEPRECATION")
    private fun getSimState() = safeGet(context.getString(R.string.status_unknown)) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        when (tm.simState) {
            TelephonyManager.SIM_STATE_READY -> context.getString(R.string.status_sim_ready)
            TelephonyManager.SIM_STATE_ABSENT -> context.getString(R.string.status_sim_absent)
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> context.getString(R.string.status_sim_network_locked)
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> context.getString(R.string.status_sim_pin_required)
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> context.getString(R.string.status_sim_puk_required)
            TelephonyManager.SIM_STATE_UNKNOWN -> context.getString(R.string.status_unknown)
            TelephonyManager.SIM_STATE_NOT_READY -> context.getString(R.string.status_sim_not_ready)
            TelephonyManager.SIM_STATE_PERM_DISABLED -> context.getString(R.string.status_sim_permanently_disabled)
            TelephonyManager.SIM_STATE_CARD_IO_ERROR -> context.getString(R.string.status_sim_io_error)
            TelephonyManager.SIM_STATE_CARD_RESTRICTED -> context.getString(R.string.status_sim_restricted)
            else -> context.getString(R.string.status_unknown_state)
        }
    }

    private fun infoItem(keyResId: Int, value: String, category: InfoCategory): DeviceInfoItem {
        return DeviceInfoItem(
            key = context.resources.getResourceEntryName(keyResId),
            keyResId = keyResId,
            value = value,
            category = category,
            icon = itemIconByResId(keyResId)
        )
    }
}
