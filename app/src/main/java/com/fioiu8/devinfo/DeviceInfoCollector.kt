package com.fioiu8.devinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.DeviceInfoItem
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.ui.itemIconByResId
import java.io.File
import java.util.Locale
import java.util.TimeZone

private inline fun safeGet(default: String, block: () -> String): String {
    return try {
        block()
    } catch (_: Exception) {
        default
    }
}

data class CpuCoreMetric(
    val index: Int,
    val frequency: String?,
    val usagePercent: Float?
)

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
        return infoItemSuppliers().mapNotNull { supplier ->
            try {
                supplier()
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Reads one item at a time so the UI can publish successful results in source order. */
    suspend fun collectDeviceInfo(onItem: suspend (DeviceInfoItem) -> Unit) {
        infoItemSuppliers().forEach { supplier ->
            val item = try {
                supplier()
            } catch (_: Exception) {
                null
            }
            item?.let { onItem(it) }
        }
    }

    private fun infoItemSuppliers(): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.device_android_id, getAndroidIdSafe(), InfoCategory.IDENTIFIERS) },
        { infoItem(R.string.device_serial, getSerialNumberSafe(), InfoCategory.IDENTIFIERS) },
        { infoItem(R.string.device_brand, Build.BRAND, InfoCategory.DEVICE) },
        { infoItem(R.string.device_manufacturer, Build.MANUFACTURER, InfoCategory.DEVICE) },
        { infoItem(R.string.device_model, Build.MODEL, InfoCategory.DEVICE) },
        { infoItem(R.string.device_product, Build.PRODUCT, InfoCategory.DEVICE) },
        { infoItem(R.string.device_device, Build.DEVICE, InfoCategory.DEVICE) },
        { infoItem(R.string.device_board, Build.BOARD, InfoCategory.DEVICE) },
        { infoItem(R.string.device_hardware, Build.HARDWARE, InfoCategory.DEVICE) },
        { infoItem(R.string.device_bootloader, Build.BOOTLOADER, InfoCategory.DEVICE) },
        { infoItem(R.string.device_build_id, Build.ID, InfoCategory.DEVICE) },
        { infoItem(R.string.device_tags, Build.TAGS, InfoCategory.DEVICE) },
        { infoItem(R.string.device_time, Build.TIME.toString(), InfoCategory.DEVICE) },
        { infoItem(R.string.device_type, Build.TYPE, InfoCategory.DEVICE) },

        { infoItem(R.string.system_cpu_arch, Build.SUPPORTED_ABIS.joinToString(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_cpu_cores, Runtime.getRuntime().availableProcessors().toString(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_sdk_version, Build.VERSION.SDK_INT.toString(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_android_version, Build.VERSION.RELEASE, InfoCategory.SYSTEM) },
        { infoItem(R.string.system_security_patch, safeGet(statusUnknown) { Build.VERSION.SECURITY_PATCH }, InfoCategory.SYSTEM) },
        { infoItem(R.string.system_baseband, safeGet(statusUnknown) { Build.getRadioVersion() }, InfoCategory.SYSTEM) },
        { infoItem(R.string.system_uptime, getUptime(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_kernel, safeGet(statusUnknown) { System.getProperty("os.version") ?: statusUnknown }, InfoCategory.SYSTEM) },
        { infoItem(R.string.system_abis_32, Build.SUPPORTED_32_BIT_ABIS.joinToString(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_abis_64, Build.SUPPORTED_64_BIT_ABIS.joinToString(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_features, getDeviceFeatures(), InfoCategory.SYSTEM) },

        { infoItem(R.string.locale_language, Locale.getDefault().language, InfoCategory.LOCALE) },
        { infoItem(R.string.locale_country, Locale.getDefault().country, InfoCategory.LOCALE) },
        { infoItem(R.string.locale_timezone, TimeZone.getDefault().id, InfoCategory.LOCALE) },

        { infoItem(R.string.display_dpi, context.resources.displayMetrics.densityDpi.toString(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_density, formatDensity(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_width, context.resources.displayMetrics.widthPixels.toString(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_height, context.resources.displayMetrics.heightPixels.toString(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_size, getDisplaySize(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_refresh_rate, safeGet(statusUnknown) { context.display.refreshRate.toString() }, InfoCategory.DISPLAY) },
        { infoItem(R.string.display_font_scale, context.resources.configuration.fontScale.toString(), InfoCategory.DISPLAY) },

        { infoItem(R.string.storage_total_ram, getTotalMemory(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_available_ram, getAvailMemory(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_total, getTotalStorage(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_available, getFreeStorage(), InfoCategory.STORAGE) },

        { infoItem(R.string.battery_level_label, getBatteryLevel(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_charging_state, getBatteryCharging(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_temperature, getBatteryProperty(BatteryManager.EXTRA_TEMPERATURE), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_health, getBatteryHealth(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_voltage, getBatteryProperty(BatteryManager.EXTRA_VOLTAGE), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_technology, getBatteryTechnology(), InfoCategory.BATTERY) },

        { infoItem(R.string.network_nfc, if (NfcAdapter.getDefaultAdapter(context) != null) context.getString(R.string.status_supported) else context.getString(R.string.status_not_supported), InfoCategory.NETWORK) },
        { infoItem(R.string.network_camera_count, getCameraCount(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_bluetooth_state, getBluetoothState(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_type, getNetworkType(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_operator, getNetworkOperator(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_sim_state, getSimState(), InfoCategory.NETWORK) },

        { infoItem(R.string.app_package, context.packageName, InfoCategory.APP) },
        { infoItem(R.string.app_version_name, getAppVersionName(), InfoCategory.APP) },
        { infoItem(R.string.app_version_code, getAppVersionCode().toString(), InfoCategory.APP) }
    )

    private val statusUnknown: String
        get() = context.getString(R.string.status_unknown)

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

    fun getCpuFrequency(): String? = readFrequency(
        "/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq"
    )

    fun getCpuCoreMetrics(): List<CpuCoreMetric> = runCatching {
        val first = readCpuTimesByCore()
        if (first.isEmpty()) return@runCatching emptyList()
        Thread.sleep(180)
        val second = readCpuTimesByCore()
        second.keys.sorted().map { index ->
            val firstTimes = first[index]
            val secondTimes = second[index]
            val usage = if (firstTimes == null || secondTimes == null) {
                null
            } else {
                val totalDelta = secondTimes.total - firstTimes.total
                val idleDelta = secondTimes.idle - firstTimes.idle
                if (totalDelta <= 0L) null else {
                    ((totalDelta - idleDelta).toFloat() / totalDelta * 100f).coerceIn(0f, 100f)
                }
            }
            CpuCoreMetric(index, getCpuCoreFrequency(index), usage)
        }
    }.getOrDefault(emptyList())

    fun getGpuFrequency(): String? = readFrequency(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/devfreq/1c00000.qcom,kgsl-3d0/cur_freq",
        "/sys/class/devfreq/mali/cur_freq",
        "/sys/devices/platform/17000000.gpu/devfreq/17000000.gpu/cur_freq"
    )

    fun getCpuUsagePercent(): Float? = runCatching {
        val first = readCpuTimes() ?: return@runCatching null
        Thread.sleep(180)
        val second = readCpuTimes() ?: return@runCatching null
        val totalDelta = second.total - first.total
        val idleDelta = second.idle - first.idle
        if (totalDelta <= 0L) null else ((totalDelta - idleDelta).toFloat() / totalDelta * 100f).coerceIn(0f, 100f)
    }.getOrNull()

    fun getGpuUsagePercent(): Float? = readFirstLine(
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/devfreq/1c00000.qcom,kgsl-3d0/load",
        "/sys/class/devfreq/mali/load"
    )?.trim()?.removeSuffix("%")?.toFloatOrNull()?.coerceIn(0f, 100f)

    private fun readFrequency(vararg paths: String): String? {
        val raw = readFirstLine(*paths)?.trim()?.toLongOrNull() ?: return null
        val mhz = when {
            raw >= 100_000_000L -> raw / 1_000_000f
            raw >= 1_000L -> raw / 1_000f
            else -> raw.toFloat()
        }
        return if (mhz > 0f) "%.0f MHz".format(Locale.US, mhz) else null
    }

    private fun getCpuCoreFrequency(index: Int): String? = readFrequency(
        "/sys/devices/system/cpu/cpu${index}/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu${index}/cpufreq/cpuinfo_cur_freq",
        "/sys/devices/system/cpu/cpufreq/policy${index}/scaling_cur_freq"
    )

    private fun readFirstLine(vararg paths: String): String? = paths.firstNotNullOfOrNull { path ->
        runCatching {
            File(path).takeIf { it.isFile && it.canRead() }?.useLines { lines -> lines.firstOrNull() }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private data class CpuTimes(val total: Long, val idle: Long)

    private fun readCpuTimesByCore(): Map<Int, CpuTimes> {
        val result = mutableMapOf<Int, CpuTimes>()
        File("/proc/stat").takeIf { it.isFile && it.canRead() }?.useLines { lines ->
            lines.forEach { line ->
                val parts = line.trim().split(Regex("\\s+"))
                val index = parts.firstOrNull()?.removePrefix("cpu")?.toIntOrNull() ?: return@forEach
                val fields = parts.drop(1).mapNotNull { it.toLongOrNull() }
                if (fields.size >= 5) {
                    result[index] = CpuTimes(fields.sum(), fields[3] + fields[4])
                }
            }
        }
        return result
    }

    private fun readCpuTimes(): CpuTimes? {
        val fields = readFirstLine("/proc/stat")
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.takeIf { it.firstOrNull() == "cpu" }
            ?.drop(1)
            ?.mapNotNull { it.toLongOrNull() }
            ?: return null
        if (fields.size < 5) return null
        return CpuTimes(total = fields.sum(), idle = fields[3] + (fields.getOrNull(4) ?: 0L))
    }

    private fun getUptime(): String = safeGet(statusUnknown) {
        val totalMinutes = SystemClock.elapsedRealtime() / 60_000
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        "${days}d ${hours}h ${minutes}m"
    }

    private fun formatDensity(): String = safeGet(statusUnknown) {
        "%.2f".format(Locale.US, context.resources.displayMetrics.density)
    }

    private fun getDisplaySize(): String = safeGet(statusUnknown) {
        val configuration = context.resources.configuration
        "${configuration.screenWidthDp} x ${configuration.screenHeightDp} dp"
    }

    private fun getDeviceFeatures(): String = safeGet(statusUnknown) {
        val featureManager = context.packageManager
        buildList {
            if (featureManager.hasSystemFeature("android.hardware.camera")) add("Camera")
            if (featureManager.hasSystemFeature("android.hardware.nfc")) add("NFC")
            if (featureManager.hasSystemFeature("android.hardware.bluetooth")) add("Bluetooth")
            if (featureManager.hasSystemFeature("android.hardware.location.gps")) add("GPS")
            if (featureManager.hasSystemFeature("android.hardware.biometrics")) add("Biometrics")
        }.joinToString().ifBlank { statusUnknown }
    }

    private fun getBatteryLevel() = safeGet(context.getString(R.string.status_unknown)) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level !in 0..100) context.getString(R.string.status_unknown) else "$level%"
    }

    private fun getBatteryCharging() = safeGet(context.getString(R.string.status_unknown)) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        if (bm.isCharging) context.getString(R.string.status_charging) else context.getString(R.string.status_not_charging)
    }

    private fun getBatteryProperty(extraName: String): String = safeGet(statusUnknown) {
        val intent = getBatteryIntent() ?: return@safeGet statusUnknown
        val value = intent.getIntExtra(extraName, -1)
        if (value < 0) return@safeGet statusUnknown
        if (extraName == BatteryManager.EXTRA_TEMPERATURE) {
            "%.1f C".format(Locale.US, value / 10f)
        } else {
            "$value mV"
        }
    }

    private fun getBatteryHealth(): String = safeGet(statusUnknown) {
        val intent = getBatteryIntent() ?: return@safeGet statusUnknown
        when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.status_health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.status_health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.status_health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.status_health_over_voltage)
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.status_health_failure)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.status_health_cold)
            else -> statusUnknown
        }
    }

    private fun getBatteryTechnology(): String = safeGet(statusUnknown) {
        getBatteryIntent()
            ?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            ?.takeIf { it.isNotBlank() }
            ?: statusUnknown
    }

    private fun getBatteryIntent(): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    private fun getCameraCount() = safeGet(context.getString(R.string.status_unknown)) {
        val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cam.cameraIdList.size.toString()
    }

    private fun getNetworkType(): String = safeGet(context.getString(R.string.status_unknown)) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@safeGet context.getString(R.string.status_unknown)
        val nc = cm.getNetworkCapabilities(cm.activeNetwork)
            ?: return@safeGet context.getString(R.string.status_unknown)
        when {
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

    private fun infoItem(keyResId: Int, value: String, category: InfoCategory): DeviceInfoItem? {
        val normalizedValue = value.trim()
        if (normalizedValue.isBlank() || normalizedValue == statusUnknown) return null
        return DeviceInfoItem(
            key = context.resources.getResourceEntryName(keyResId),
            keyResId = keyResId,
            value = normalizedValue,
            category = category,
            icon = itemIconByResId(keyResId)
        )
    }
}
