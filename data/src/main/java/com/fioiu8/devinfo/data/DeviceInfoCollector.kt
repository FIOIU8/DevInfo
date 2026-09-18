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

package com.fioiu8.devinfo.data
import com.fioiu8.devinfo.data.R

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Display
import com.fioiu8.devinfo.data.BuildConfig
import com.fioiu8.devinfo.core.cpu.CPU_USAGE_SAMPLE_DELAY_MS
import com.fioiu8.devinfo.core.cpu.CpuTimes
import com.fioiu8.devinfo.core.cpu.CpuUptimeTimes
import com.fioiu8.devinfo.core.cpu.calculateCpuUsageFromUptime
import com.fioiu8.devinfo.core.cpu.formatCpuFrequency
import com.fioiu8.devinfo.core.cpu.parseCpuIndexes
import com.fioiu8.devinfo.core.cpu.parseCpuTimes
import com.fioiu8.devinfo.core.cpu.parseCpuUptime
import com.fioiu8.devinfo.core.cpu.parseTopCpuUsage
import com.fioiu8.devinfo.core.model.CpuCoreMetric
import com.fioiu8.devinfo.core.root.ROOT_AUTHORIZATION_TIMEOUT_MS
import com.fioiu8.devinfo.core.root.ROOT_PROBE_COMMAND
import com.fioiu8.devinfo.core.root.RootAccess
import com.fioiu8.devinfo.core.root.parseRootProbe
import com.fioiu8.devinfo.core.model.SecuritySnapshot
import com.fioiu8.devinfo.core.model.DeviceInfoItem
import com.fioiu8.devinfo.core.model.InfoCategory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

// 热路径（每 2 秒采样）中逐行解析 /proc 使用，提为顶层常量避免每行重复分配
private val WHITESPACE_SPLIT_REGEX = Regex("\\s+")
private val CPU_POLICY_DIR_PATTERN = Regex("policy\\d+")

private inline fun safeGet(
    default: String,
    key: String = "unknown",
    block: () -> String,
): String {
    return try {
        block()
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            android.util.Log.w("DeviceInfoCollector", "Failed to collect $key", e)
        }
        default
    }
}

class DeviceInfoCollector(private val context: Context) {
    private val locale: Locale = context.resources.configuration.locales[0]

    // 写于 Default/IO 线程（loadDeviceInfo），读于主线程（appVersionName 懒加载）
    @Volatile private var cachedVersionName: String? = null
    @Volatile private var cachedVersionCode: Long? = null

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

    /**
     * 一次采集内的共享资源。电池广播、内存信息、网络能力、包信息这些 binder/IPC
     * 调用代价较高，整次采集只获取一次，既避免重复开销，也保证同一时间点的快照一致性。
     */
    private class CollectContext(
        val batteryIntent: Intent?,
        val memoryInfo: ActivityManager.MemoryInfo,
        val connectivityManager: ConnectivityManager?,
        val networkCapabilities: NetworkCapabilities?,
        val packageInfo: PackageInfo?,
    )

    private fun newCollectContext(): CollectContext {
        // 粘性广播：registerReceiver(null, filter) 返回最近一次系统广播，本次采集内的
        // 所有电池字段都从这份快照读取，后续广播更新不影响本次采集（符合预期）。
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val memoryInfo = ActivityManager.MemoryInfo().also {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCapabilities = connectivityManager?.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val packageInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        return CollectContext(batteryIntent, memoryInfo, connectivityManager, networkCapabilities, packageInfo)
    }

    suspend fun collectDeviceInfo(): List<DeviceInfoItem> = withContext(Dispatchers.IO) {
        val ctx = newCollectContext()
        infoItemSuppliers(ctx).mapNotNull { supplier ->
            try {
                supplier()
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Reads one item at a time so the UI can publish successful results in source order. */
    suspend fun collectDeviceInfo(onItem: suspend (DeviceInfoItem) -> Unit) = withContext(Dispatchers.IO) {
        val ctx = newCollectContext()
        infoItemSuppliers(ctx).forEach { supplier ->
            val item = try {
                supplier()
            } catch (_: Exception) {
                null
            }
            item?.let { onItem(it) }
        }
    }

    private fun infoItemSuppliers(ctx: CollectContext): List<() -> DeviceInfoItem?> = buildList {
        addAll(identifierInfoItemSuppliers())
        addAll(deviceInfoItemSuppliers())
        addAll(systemInfoItemSuppliers())
        addAll(localeInfoItemSuppliers())
        addAll(displayInfoItemSuppliers())
        addAll(storageInfoItemSuppliers(ctx))
        addAll(batteryInfoItemSuppliers(ctx))
        addAll(networkInfoItemSuppliers(ctx))
        addAll(appInfoItemSuppliers(ctx))
    }

    private fun identifierInfoItemSuppliers(): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.device_android_id, getAndroidIdSafe(), InfoCategory.IDENTIFIERS) },
        { infoItem(R.string.device_serial, getSerialNumberSafe(), InfoCategory.IDENTIFIERS) }
    )

    private fun deviceInfoItemSuppliers(): List<() -> DeviceInfoItem?> = listOf(
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
        { infoItem(R.string.device_build_display, Build.DISPLAY, InfoCategory.DEVICE) },
        { infoItem(R.string.device_incremental, Build.VERSION.INCREMENTAL, InfoCategory.DEVICE) },
        { infoItem(R.string.device_fingerprint, Build.FINGERPRINT, InfoCategory.DEVICE) },
        { infoItem(R.string.device_host, Build.HOST, InfoCategory.DEVICE) },
        { infoItem(R.string.device_user, Build.USER, InfoCategory.DEVICE) },
        { infoItem(R.string.device_soc_manufacturer, getSocManufacturer(), InfoCategory.DEVICE) },
        { infoItem(R.string.device_soc_model, getSocModel(), InfoCategory.DEVICE) }
    )

    private fun systemInfoItemSuppliers(): List<() -> DeviceInfoItem?> = listOf(
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
        { infoItem(R.string.system_java_vm, getJavaVmVersion(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_opengl_version, getOpenGlVersion(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_google_play_services, getGooglePlayServicesVersion(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_treble, getTrebleSupport(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_sensor_count, getSensorCount(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_boot_time, getBootTime(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_usb_debugging, getUsbDebuggingState(), InfoCategory.SYSTEM) },
        { infoItem(R.string.system_lock_screen, getLockScreenState(), InfoCategory.SYSTEM) }
    )

    private fun localeInfoItemSuppliers(): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.locale_language, locale.language, InfoCategory.LOCALE) },
        { infoItem(R.string.locale_country, locale.country, InfoCategory.LOCALE) },
        { infoItem(R.string.locale_timezone, TimeZone.getDefault().id, InfoCategory.LOCALE) },
        { infoItem(R.string.locale_display_name, locale.displayName, InfoCategory.LOCALE) },
        { infoItem(R.string.locale_tag, locale.toLanguageTag(), InfoCategory.LOCALE) },
        { infoItem(R.string.locale_timezone_offset, getTimezoneOffset(), InfoCategory.LOCALE) },
        { infoItem(R.string.locale_currency, getLocaleCurrency(), InfoCategory.LOCALE) },
        { infoItem(R.string.locale_system_locales, getSystemLocales(), InfoCategory.LOCALE) },
        { infoItem(R.string.locale_24_hour, get24HourFormat(), InfoCategory.LOCALE) }
    )

    private fun displayInfoItemSuppliers(): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.display_dpi, context.resources.displayMetrics.densityDpi.toString(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_density, formatDensity(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_width, context.resources.displayMetrics.widthPixels.toString(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_height, context.resources.displayMetrics.heightPixels.toString(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_size, getDisplaySize(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_refresh_rate, getRefreshRate(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_font_scale, context.resources.configuration.fontScale.toString(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_orientation, getOrientation(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_dark_mode, getDarkModeState(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_hdr, getHdrSupport(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_wide_color_gamut, getWideColorGamutSupport(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_brightness, getScreenBrightness(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_timeout, getScreenTimeout(), InfoCategory.DISPLAY) },
        { infoItem(R.string.display_supported_refresh_rates, getSupportedRefreshRates(), InfoCategory.DISPLAY) }
    )

    private fun storageInfoItemSuppliers(ctx: CollectContext): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.storage_total_ram, getTotalMemory(ctx.memoryInfo), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_available_ram, getAvailMemory(ctx.memoryInfo), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_low_memory, getLowMemoryState(ctx.memoryInfo), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_memory_threshold, getMemoryThreshold(ctx.memoryInfo), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_total, getTotalStorage(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_available, getFreeStorage(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_internal_total, getInternalTotalStorage(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_internal_available, getInternalFreeStorage(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_app_heap, getAppHeapLimit(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_emulated, getStorageEmulated(), InfoCategory.STORAGE) },
        { infoItem(R.string.storage_removable, getStorageRemovable(), InfoCategory.STORAGE) }
    )

    private fun batteryInfoItemSuppliers(ctx: CollectContext): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.battery_level_label, getBatteryLevel(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_charging_state, getBatteryCharging(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_temperature, getBatteryProperty(ctx.batteryIntent, BatteryManager.EXTRA_TEMPERATURE), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_health, getBatteryHealth(ctx.batteryIntent), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_voltage, getBatteryProperty(ctx.batteryIntent, BatteryManager.EXTRA_VOLTAGE), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_technology, getBatteryTechnology(ctx.batteryIntent), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_plug_type, getBatteryPlugType(ctx.batteryIntent), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_current_now, getBatteryCurrentNow(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_charge_counter, getBatteryChargeCounter(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_capacity_design, getBatteryDesignCapacity(), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_cycle_count, getBatteryCycleCount(ctx.batteryIntent), InfoCategory.BATTERY) },
        { infoItem(R.string.battery_power_save, getPowerSaveMode(), InfoCategory.BATTERY) }
    )

    private fun networkInfoItemSuppliers(ctx: CollectContext): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.network_nfc, if (NfcAdapter.getDefaultAdapter(context) != null) context.getString(R.string.status_supported) else context.getString(R.string.status_not_supported), InfoCategory.NETWORK) },
        { infoItem(R.string.network_camera_count, getCameraCount(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_bluetooth_state, getBluetoothState(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_type, getNetworkType(ctx.networkCapabilities), InfoCategory.NETWORK) },
        { infoItem(R.string.network_operator, getNetworkOperator(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_sim_state, getSimState(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_sim_operator, getSimOperator(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_sim_country, getSimCountry(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_phone_type, getPhoneType(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_wifi_enabled, getWifiEnabledState(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_metered, getNetworkMetered(ctx.connectivityManager), InfoCategory.NETWORK) },
        { infoItem(R.string.network_vpn, getVpnState(ctx.networkCapabilities), InfoCategory.NETWORK) },
        { infoItem(R.string.network_airplane_mode, getAirplaneModeState(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_data_roaming, getDataRoamingState(), InfoCategory.NETWORK) },
        { infoItem(R.string.network_link_speed, getNetworkLinkSpeed(ctx.networkCapabilities), InfoCategory.NETWORK) }
    )

    private fun appInfoItemSuppliers(ctx: CollectContext): List<() -> DeviceInfoItem?> = listOf(
        { infoItem(R.string.app_package, context.packageName, InfoCategory.APP) },
        { infoItem(R.string.app_version_name, getAppVersionName(), InfoCategory.APP) },
        { infoItem(R.string.app_version_code, getAppVersionCode().toString(), InfoCategory.APP) },
        { infoItem(R.string.app_first_install, getAppFirstInstallTime(ctx.packageInfo), InfoCategory.APP) },
        { infoItem(R.string.app_last_update, getAppLastUpdateTime(ctx.packageInfo), InfoCategory.APP) },
        { infoItem(R.string.app_target_sdk, getAppTargetSdk(), InfoCategory.APP) },
        { infoItem(R.string.app_min_sdk, getAppMinSdk(), InfoCategory.APP) },
        { infoItem(R.string.app_installer, getAppInstaller(), InfoCategory.APP) },
        { infoItem(R.string.app_installed_count, getVisibleInstalledAppCount(), InfoCategory.APP) }
    )

    private val statusUnknown: String = context.getString(R.string.status_unknown)

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

    private fun getTotalMemory(mi: ActivityManager.MemoryInfo): String {
        return "${mi.totalMem / 1024 / 1024} MB"
    }

    private fun getAvailMemory(mi: ActivityManager.MemoryInfo): String {
        return "${mi.availMem / 1024 / 1024} MB"
    }

    /** 返回内存使用百分比 [0, 100] */
    fun getMemoryUsagePercent(): Float? {
        return try {
            val mi = getMemoryInfo()
            if (mi.totalMem > 0) ((mi.totalMem - mi.availMem).toFloat() / mi.totalMem * 100f) else null
        } catch (_: Exception) {
            null
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
    fun getStorageUsagePercent(): Float? {
        return try {
            val total = Environment.getExternalStorageDirectory().totalSpace
            val free = Environment.getExternalStorageDirectory().freeSpace
            if (total > 0) ((total - free).toFloat() / total * 100f) else null
        } catch (_: Exception) {
            null
        }
    }

    fun getSecuritySnapshot(): SecuritySnapshot = SecuritySnapshot(
        securityPatch = runCatching { Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() } }.getOrNull(),
        lockScreenEnabled = runCatching {
            (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardSecure
        }.getOrNull(),
        usbDebuggingEnabled = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED) == 1
        }.getOrNull()
    )

    fun getCpuFrequency(): String? = readFrequency(*cpuFrequencyPaths(0).toTypedArray())

    suspend fun getCpuCoreMetrics(): List<CpuCoreMetric> {
        return try {
            val first = readCpuTimesByCore()
            if (first.isEmpty()) return getCpuCoreTopologyMetrics()
            delay(CPU_USAGE_SAMPLE_DELAY_MS)
            val second = readCpuTimesByCore()
            (first.keys + second.keys).toSortedSet().map { index ->
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
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            getCpuCoreTopologyMetrics()
        }
    }

    /**
     * 用户主动发起的授权请求：等待用户在管理器里操作期间可被取消。
     *
     * 它是唯一的 Root 探测入口——「已授权」这件事只能由一次真实的 `su` 往返证明，因此没有
     * 单独的「快速探测」方法：那只会多出一次无意义的 su 调用。
     *
     * 与 [runSu] 的两点差异都是有意的：
     * - 等待退出用轮询 + `ensureActive()`，因此协程取消在 [PROCESS_POLL_INTERVAL_MS] 内生效
     *   （`waitFor` 的阻塞调用无法被协程取消打断）；
     * - 超时用 [ROOT_AUTHORIZATION_TIMEOUT_MS]（30 秒）而不是 5 秒，因为这里可能一直在等用户
     *   去管理器里确认。
     *
     * 取消不算失败：进程在这里被强杀，[CancellationException] 原样向上抛，由调用方决定 UI 行为。
     */
    suspend fun requestRootAccess(timeoutMs: Long = ROOT_AUTHORIZATION_TIMEOUT_MS): RootAccess {
        val process = spawnSu(ROOT_PROBE_COMMAND)
            ?: return parseRootProbe(spawnFailed = true, timedOut = false, exitCode = -1, output = "")

        val lines = mutableListOf<String>()
        val readerDone = CountDownLatch(1)
        startOutputDrain(process, lines, readerDone)

        return try {
            val exited = awaitExitCancellable(process, timeoutMs)
            if (exited) {
                // 进程已退出，读取线程只剩收尾，无需再等一个完整超时
                readerDone.await(READER_DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            }
            parseRootProbe(
                spawnFailed = false,
                timedOut = !exited,
                exitCode = if (exited) process.exitValue() else -1,
                output = synchronized(lines) { lines.joinToString("\n") },
            )
        } finally {
            // 取消与超时都经过这里：离开前不给设备留下一个挂着的 su 进程
            if (process.isAlive) process.destroyForcibly()
        }
    }

    /**
     * 分片等待进程退出，使协程取消有明确上界。
     *
     * 每次只等一小段并在片段之间检查取消；若直接 `waitFor(整个超时)`，取消要等到它返回才可能
     * 被察觉，30 秒的授权上限会变成 30 秒不可中断的阻塞。
     *
     * @return true 表示进程已退出；false 表示到期限仍未退出
     */
    private suspend fun awaitExitCancellable(process: Process, timeoutMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
            var exited = false
            while (!exited) {
                val remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
                if (remainingMs <= 0L) break
                if (process.waitFor(minOf(PROCESS_POLL_INTERVAL_MS, remainingMs), TimeUnit.MILLISECONDS)) {
                    exited = true
                } else {
                    coroutineContext.ensureActive()
                }
            }
            exited
        }

    /** 一次 su 调用的原始观察结果，交 [parseRootProbe] 判定。 */
    private data class SuRunResult(
        val spawnFailed: Boolean,
        val timedOut: Boolean,
        val exitCode: Int,
        val lines: List<String>,
    ) {
        val output: String get() = lines.joinToString("\n")
    }

    /**
     * 以 root 执行一条命令并收集原始结果。
     *
     * 本方法不抛异常——失败以 [SuRunResult] 表达，保持调用方「读不到就返回空」的契约。
     *
     * 不可取消：调用方是已经授过权的内部读取（5 秒上限），不需要等用户操作。需要可取消的授权
     * 路径用 [requestRootAccess]。
     */
    private fun runSu(command: String, timeoutMs: Long): SuRunResult {
        val process = spawnSu(command)
            ?: return SuRunResult(spawnFailed = true, timedOut = false, exitCode = -1, lines = emptyList())

        val lines = mutableListOf<String>()
        val readerDone = CountDownLatch(1)
        val readerThread = startOutputDrain(process, lines, readerDone)

        return try {
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                readerThread.interrupt()
                SuRunResult(
                    spawnFailed = false,
                    timedOut = true,
                    exitCode = -1,
                    lines = synchronized(lines) { lines.toList() },
                )
            } else {
                // 进程已退出，读取线程只剩收尾，无需再等一个完整超时
                readerDone.await(READER_DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                SuRunResult(
                    spawnFailed = false,
                    timedOut = false,
                    exitCode = process.exitValue(),
                    lines = synchronized(lines) { lines.toList() },
                )
            }
        } catch (_: Exception) {
            SuRunResult(spawnFailed = false, timedOut = false, exitCode = -1, lines = emptyList())
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    /** 启动 `su -c <command>`；返回 null 表示进程未能启动（典型为 ENOENT）。 */
    private fun spawnSu(command: String): Process? = try {
        ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
    } catch (_: Exception) {
        // 典型为 ENOENT：su 不存在，或对调用者不可见（KernelSU 未授权时即如此）
        null
    }

    /**
     * 另起线程读取合并后的 stdout + stderr。
     *
     * 必须是独立线程：`readLine()`/`useLines()` 是阻塞调用，协程超时无法中断它们，su 挂起时
     * 会永久占住调用线程。行数上限 [MAX_ROOT_OUTPUT_LINES] 避免无界增长。
     */
    private fun startOutputDrain(
        process: Process,
        lines: MutableList<String>,
        done: CountDownLatch,
    ): Thread = Thread {
        runCatching {
            process.inputStream.bufferedReader().useLines { output ->
                output.forEach { line ->
                    synchronized(lines) {
                        if (lines.size < MAX_ROOT_OUTPUT_LINES) lines += line
                    }
                }
            }
        }
        done.countDown()
    }.apply {
        isDaemon = true
        start()
    }

    /** 通过 Root 权限读取 /proc/stat 并计算每核心占用率 */
    suspend fun getCpuCoreMetricsWithRoot(): List<CpuCoreMetric> {
        return try {
            val first = readCpuTimesByCoreWithRoot()
            if (first.isEmpty()) return emptyList()
            delay(CPU_USAGE_SAMPLE_DELAY_MS)
            val second = readCpuTimesByCoreWithRoot()
            (first.keys + second.keys).toSortedSet().map { index ->
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
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun readCpuTimesByCoreWithRoot(): Map<Int, CpuTimes> {
        val result = mutableMapOf<Int, CpuTimes>()
        readLinesWithRoot("/proc/stat").forEach { line ->
            val parts = line.trim().split(WHITESPACE_SPLIT_REGEX)
            val index = parts.firstOrNull()?.removePrefix("cpu")?.toIntOrNull() ?: return@forEach
            parseCpuTimes(parts.drop(1))?.let { result[index] = it }
        }
        return result
    }

    /** 通过 root 读取一个文件的内容行；读不到时返回空列表，失败原因由 [runSu] 记录。 */
    private fun readLinesWithRoot(path: String): List<String> =
        runSu("cat $path", ROOT_COMMAND_TIMEOUT_MS).lines

    fun getGpuFrequency(): String? = readFrequency(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/devfreq/1c00000.qcom,kgsl-3d0/cur_freq",
        "/sys/class/devfreq/mali/cur_freq",
        "/sys/devices/platform/17000000.gpu/devfreq/17000000.gpu/cur_freq"
    )

    suspend fun getCpuUsagePercent(): Float? {
        return try {
            val first = readCpuTimes()
            val firstUptime = if (first == null) readCpuUptime() else null
            if (first == null && firstUptime == null) {
                return readCpuUsageFromTop()
            }
            delay(CPU_USAGE_SAMPLE_DELAY_MS)
            if (first != null) {
                val second = readCpuTimes() ?: return null
                val totalDelta = second.total - first.total
                val idleDelta = second.idle - first.idle
                if (totalDelta <= 0L) null else {
                    ((totalDelta - idleDelta).toFloat() / totalDelta * 100f).coerceIn(0f, 100f)
                }
            } else {
                calculateCpuUsageFromUptime(
                    first = firstUptime ?: return null,
                    second = readCpuUptime() ?: return null,
                    cpuCount = cpuIndexes.size.coerceAtLeast(1)
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    fun getGpuUsagePercent(): Float? = readFirstLine(
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/devfreq/1c00000.qcom,kgsl-3d0/load",
        "/sys/class/devfreq/mali/load"
    )?.trim()?.removeSuffix("%")?.toFloatOrNull()?.coerceIn(0f, 100f)

    private fun readFrequency(vararg paths: String): String? {
        val raw = readFirstLine(*paths)?.trim()?.toLongOrNull() ?: return null
        return formatCpuFrequency(raw)
    }

    private fun getCpuCoreFrequency(index: Int): String? = readFrequency(*cpuFrequencyPaths(index).toTypedArray())

    /** Vendors expose frequency through either per-core nodes or policy nodes. */
    private val cpuFrequencyPathsCache = HashMap<Int, List<String>>()

    // CPU 拓扑在运行期不变；该函数原被每核每 2 秒调用一次，每次全量扫描
    // /sys/devices/system/cpu/cpufreq 并逐 policy 读 related_cpus，代价过高
    private fun cpuFrequencyPaths(index: Int): List<String> = synchronized(cpuFrequencyPathsCache) {
        cpuFrequencyPathsCache.getOrPut(index) { buildCpuFrequencyPaths(index) }
    }

    private fun buildCpuFrequencyPaths(index: Int): List<String> = buildList {
        add("/sys/devices/system/cpu/cpu${index}/cpufreq/scaling_cur_freq")
        add("/sys/devices/system/cpu/cpu${index}/cpufreq/cpuinfo_cur_freq")

        val policyRoot = File("/sys/devices/system/cpu/cpufreq")
        policyRoot.listFiles()
            ?.filter { it.name.matches(CPU_POLICY_DIR_PATTERN) }
            ?.sortedBy { it.name }
            ?.forEach { policy ->
                val related = readFirstLine(
                    File(policy, "related_cpus").path,
                    File(policy, "affected_cpus").path
                )
                if (parseCpuIndexes(related).contains(index)) {
                    add(File(policy, "scaling_cur_freq").path)
                    add(File(policy, "cpuinfo_cur_freq").path)
                }
            }
    }

    /**
     * Android vendors may deny third-party access to /proc/stat. Keep the core topology
     * and frequencies visible; overall usage is sampled separately through /proc/uptime.
     */
    private fun getCpuCoreTopologyMetrics(): List<CpuCoreMetric> {
        return cpuIndexes.map { index ->
            CpuCoreMetric(index = index, frequency = getCpuCoreFrequency(index), usagePercent = null)
        }
    }

    // CPU 拓扑在运行期不变；首次调用时计算并缓存（lazy 默认线程安全），
    // 避免每 2 秒采样都全量扫描 /sys/devices/system/cpu/online 与 cpufreq 目录。
    private val cpuIndexes: List<Int> by lazy { computeCpuIndexes() }

    private fun computeCpuIndexes(): List<Int> = parseCpuIndexes(
            readFirstLine("/sys/devices/system/cpu/online")
                ?: readFirstLine("/sys/devices/system/cpu/present")
        )
            .ifEmpty {
                runCatching {
                    File("/sys/devices/system/cpu")
                        .listFiles()
                        ?.mapNotNull { directory ->
                            directory.name
                                .removePrefix("cpu")
                                .toIntOrNull()
                        }
                        ?.sorted()
                        .orEmpty()
                }.getOrDefault(emptyList())
            }
            .ifEmpty {
                (0 until Runtime.getRuntime().availableProcessors().coerceAtLeast(1)).toList()
            }

    private fun readFirstLine(vararg paths: String): String? = paths.firstNotNullOfOrNull { path ->
        // Do not call isFile/canRead first: SELinux-backed proc/sysfs nodes can
        // reject those metadata checks while still allowing the actual read.
        // Note: ProcessBuilder-based shell fallback was attempted but the child
        // process inherits the app's SELinux context, so cat/top are equally denied.
        runCatching {
            File(path).bufferedReader().use { it.readLine() }
        }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun readLines(path: String): List<String> = runCatching {
        // Android 8.0+ SELinux denies untrusted_app direct read of /proc/stat;
        // the shell fallback was ineffective (child inherits app context).
        File(path).bufferedReader().useLines { it.toList() }
    }.getOrDefault(emptyList())

    private fun readCpuTimesByCore(): Map<Int, CpuTimes> {
        val result = mutableMapOf<Int, CpuTimes>()
        readLines("/proc/stat").forEach { line ->
            val parts = line.trim().split(WHITESPACE_SPLIT_REGEX)
            val index = parts.firstOrNull()?.removePrefix("cpu")?.toIntOrNull() ?: return@forEach
            parseCpuTimes(parts.drop(1))?.let { result[index] = it }
        }
        return result
    }

    private fun readCpuTimes(): CpuTimes? {
        val parts = readFirstLine("/proc/stat")?.split(WHITESPACE_SPLIT_REGEX) ?: return null
        if (parts.firstOrNull() != "cpu") return null
        return parseCpuTimes(parts.drop(1))
    }

    private fun readCpuUptime(): CpuUptimeTimes? = parseCpuUptime(readFirstLine("/proc/uptime"))

    /**
     * Some Android 15 vendor builds deny app access to all useful /proc CPU counters,
     * while still allowing the system top binary. Its summary line is scaled by the
     * online-core count, so derive a normalized percentage from total and idle time.
     */
    private fun readCpuUsageFromTop(): Float? = runCatching {
        val process = ProcessBuilder("top", "-b", "-n", "1", "-m", "1")
            .redirectErrorStream(true)
            .start()
        try {
            if (!process.waitFor(TOP_COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                return@runCatching null
            }
            process.inputStream.bufferedReader().useLines { lines ->
                // Android 15 can expose top while filtering its global counters. On affected
                // builds the summary is always entirely idle, which is not a valid reading.
                lines.mapNotNull(::parseTopCpuUsage).firstOrNull { it > 0f }
            }
        } finally {
            process.destroy()
        }
    }.getOrNull()

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

    private fun getBatteryProperty(batteryIntent: Intent?, extraName: String): String = safeGet(statusUnknown) {
        val intent = batteryIntent ?: return@safeGet statusUnknown
        val value = intent.getIntExtra(extraName, -1)
        if (value < 0) return@safeGet statusUnknown
        if (extraName == BatteryManager.EXTRA_TEMPERATURE) {
            "%.1f C".format(Locale.US, value / 10f)
        } else {
            "$value mV"
        }
    }

    private fun getBatteryHealth(batteryIntent: Intent?): String = safeGet(statusUnknown) {
        val intent = batteryIntent ?: return@safeGet statusUnknown
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

    private fun getBatteryTechnology(batteryIntent: Intent?): String = safeGet(statusUnknown) {
        batteryIntent
            ?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            ?.takeIf { it.isNotBlank() }
            ?: statusUnknown
    }

    private fun getCameraCount() = safeGet(context.getString(R.string.status_unknown)) {
        val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cam.cameraIdList.size.toString()
    }

    private fun getNetworkType(nc: NetworkCapabilities?): String = safeGet(statusUnknown) {
        val caps = nc ?: return@safeGet statusUnknown
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> context.getString(R.string.status_wifi)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> context.getString(R.string.status_cellular)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> context.getString(R.string.status_ethernet)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> context.getString(R.string.status_bluetooth)
            else -> context.getString(R.string.status_unknown)
        }
    }

    private fun getNetworkOperator() = safeGet(context.getString(R.string.status_unknown)) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tm.networkOperatorName ?: context.getString(R.string.status_unknown)
    }

    // minSdk 33 仍需支持，无新 API 替代
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

    // ── DEVICE 补充项 ──

    private fun getSocManufacturer(): String = safeGet(statusUnknown) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else statusUnknown
    }

    private fun getSocModel(): String = safeGet(statusUnknown) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else statusUnknown
    }

    // ── SYSTEM 补充项 ──

    private fun getJavaVmVersion(): String = safeGet(statusUnknown) {
        System.getProperty("java.vm.version") ?: statusUnknown
    }

    private fun getOpenGlVersion(): String = safeGet(statusUnknown) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.deviceConfigurationInfo.glEsVersion
    }

    private fun getGooglePlayServicesVersion(): String = safeGet(statusUnknown) {
        try {
            context.packageManager.getPackageInfo("com.google.android.gms", 0).versionName ?: statusUnknown
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            "not installed"
        }
    }

    private fun getTrebleSupport(): String = safeGet(statusUnknown) {
        val value = readSystemProperty("ro.treble.enabled") ?: return@safeGet statusUnknown
        if (value == "true") context.getString(R.string.status_supported) else context.getString(R.string.status_not_supported)
    }

    private fun getSensorCount(): String = safeGet(statusUnknown) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.getSensorList(Sensor.TYPE_ALL).size.toString()
    }

    private fun getBootTime(): String = safeGet(statusUnknown) {
        val bootMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(bootMillis))
    }

    private fun getUsbDebuggingState(): String = safeGet(statusUnknown) {
        val enabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        if (enabled) context.getString(R.string.status_enabled) else context.getString(R.string.status_disabled)
    }

    private fun getLockScreenState(): String = safeGet(statusUnknown) {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return@safeGet statusUnknown
        if (km.isKeyguardSecure) context.getString(R.string.status_enabled) else context.getString(R.string.status_disabled)
    }

    private fun readSystemProperty(name: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod("get", String::class.java)
        (method.invoke(null, name) as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    // ── LOCALE 补充项 ──

    private fun getTimezoneOffset(): String = safeGet(statusUnknown) {
        val offsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis())
        val totalMinutes = offsetMillis / 60_000
        val sign = if (totalMinutes >= 0) "+" else "-"
        val abs = kotlin.math.abs(totalMinutes)
        "UTC%s%02d:%02d".format(Locale.US, sign, abs / 60, abs % 60)
    }

    private fun getLocaleCurrency(): String = safeGet(statusUnknown) {
        val currency = Currency.getInstance(locale)
        "${currency.currencyCode} (${currency.symbol})"
    }

    private fun getSystemLocales(): String = safeGet(statusUnknown) {
        val locales = context.resources.configuration.locales
        (0 until locales.size()).joinToString { locales.get(it).toLanguageTag() }
    }

    private fun get24HourFormat(): String = safeGet(statusUnknown) {
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            context.getString(R.string.status_enabled)
        } else {
            context.getString(R.string.status_disabled)
        }
    }

    // ── DISPLAY 补充项 ──

    private fun getOrientation(): String = safeGet(statusUnknown) {
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> context.getString(R.string.status_landscape)
            Configuration.ORIENTATION_PORTRAIT -> context.getString(R.string.status_portrait)
            else -> statusUnknown
        }
    }

    private fun getDarkModeState(): String = safeGet(statusUnknown) {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            context.getString(R.string.status_enabled)
        } else {
            context.getString(R.string.status_disabled)
        }
    }

    private fun getHdrSupport(): String = safeGet(statusUnknown) {
        val display = defaultDisplay() ?: return@safeGet statusUnknown
        if (display.isHdr) context.getString(R.string.status_supported) else context.getString(R.string.status_not_supported)
    }

    private fun getWideColorGamutSupport(): String = safeGet(statusUnknown) {
        val display = defaultDisplay() ?: return@safeGet statusUnknown
        if (display.isWideColorGamut) context.getString(R.string.status_supported) else context.getString(R.string.status_not_supported)
    }

    private fun getScreenBrightness(): String = safeGet(statusUnknown) {
        readScreenBrightnessPercent(context)?.let { "$it%" } ?: statusUnknown
    }

    private fun getScreenTimeout(): String = safeGet(statusUnknown) {
        val millis = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        val seconds = millis / 1000
        if (seconds >= 60) "${seconds / 60} min" else "$seconds s"
    }

    private fun getSupportedRefreshRates(): String = safeGet(statusUnknown) {
        val rates = defaultDisplay()?.supportedModes
            ?.map { it.refreshRate }
            ?.distinct()
            ?.sorted()
            .orEmpty()
        if (rates.isEmpty()) statusUnknown
        else rates.joinToString { "%.0f Hz".format(Locale.US, it) }
    }

    private fun getRefreshRate(): String = safeGet(statusUnknown) {
        defaultDisplay()?.mode?.refreshRate
            ?.let { "%.0f Hz".format(Locale.US, it) }
            ?: statusUnknown
    }

    /**
     * 取默认 Display。
     *
     * 不能使用 Context.getDisplay()：本类的 Context 由
     * applicationContext.createConfigurationContext() 派生，不与任何显示关联，
     * 调用会抛 UnsupportedOperationException，使刷新率/HDR/广色域/支持刷新率
     * 四个字段永远读不到并静默消失。
     */
    private fun defaultDisplay(): Display? =
        (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.getDisplay(Display.DEFAULT_DISPLAY)

    // ── STORAGE 补充项 ──

    private fun getLowMemoryState(mi: ActivityManager.MemoryInfo): String = safeGet(statusUnknown) {
        if (mi.lowMemory) context.getString(R.string.status_yes) else context.getString(R.string.status_no)
    }

    private fun getMemoryThreshold(mi: ActivityManager.MemoryInfo): String = safeGet(statusUnknown) {
        "${mi.threshold / 1024 / 1024} MB"
    }

    private fun getInternalTotalStorage(): String = safeGet(statusUnknown) {
        "${Environment.getDataDirectory().totalSpace / 1024 / 1024 / 1024} GB"
    }

    private fun getInternalFreeStorage(): String = safeGet(statusUnknown) {
        "${Environment.getDataDirectory().freeSpace / 1024 / 1024 / 1024} GB"
    }

    private fun getAppHeapLimit(): String = safeGet(statusUnknown) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        "${am.memoryClass} MB / ${am.largeMemoryClass} MB"
    }

    private fun getStorageEmulated(): String = safeGet(statusUnknown) {
        if (Environment.isExternalStorageEmulated()) context.getString(R.string.status_yes) else context.getString(R.string.status_no)
    }

    private fun getStorageRemovable(): String = safeGet(statusUnknown) {
        if (Environment.isExternalStorageRemovable()) context.getString(R.string.status_yes) else context.getString(R.string.status_no)
    }

    // ── BATTERY 补充项 ──

    private fun getBatteryPlugType(batteryIntent: Intent?): String = safeGet(statusUnknown) {
        val intent = batteryIntent ?: return@safeGet statusUnknown
        when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.status_plugged_ac)
            BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.status_plugged_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.status_plugged_wireless)
            0 -> context.getString(R.string.status_unplugged)
            else -> statusUnknown
        }
    }

    private fun getBatteryCurrentNow(): String = safeGet(statusUnknown) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val microAmps = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (microAmps == Int.MIN_VALUE || microAmps == 0) return@safeGet statusUnknown
        "%.0f mA".format(Locale.US, kotlin.math.abs(microAmps) / 1000f)
    }

    private fun getBatteryChargeCounter(): String = safeGet(statusUnknown) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val microAmpHours = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        if (microAmpHours <= 0) return@safeGet statusUnknown
        "${microAmpHours / 1000} mAh"
    }

    private fun getBatteryDesignCapacity(): String = safeGet(statusUnknown) {
        readFirstLine("/sys/class/power_supply/battery/charge_full_design")
            ?.trim()?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { "${it / 1000} mAh" }
            ?: statusUnknown
    }

    private fun getBatteryCycleCount(batteryIntent: Intent?): String = safeGet(statusUnknown) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val count = batteryIntent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
            if (count > 0) return@safeGet count.toString()
        }
        readFirstLine("/sys/class/power_supply/battery/cycle_count")
            ?.trim()?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?.toString()
            ?: statusUnknown
    }

    private fun getPowerSaveMode(): String = safeGet(statusUnknown) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            ?: return@safeGet statusUnknown
        if (pm.isPowerSaveMode) context.getString(R.string.status_enabled) else context.getString(R.string.status_disabled)
    }

    // ── NETWORK 补充项 ──

    private fun getSimOperator(): String = safeGet(statusUnknown) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tm.simOperatorName?.takeIf { it.isNotBlank() } ?: statusUnknown
    }

    private fun getSimCountry(): String = safeGet(statusUnknown) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tm.simCountryIso?.takeIf { it.isNotBlank() }?.uppercase(Locale.US) ?: statusUnknown
    }

    // PHONE_TYPE_CDMA 为稳定常量，框架标记弃用但无替代值
    @Suppress("DEPRECATION")
    private fun getPhoneType(): String = safeGet(statusUnknown) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        when (tm.phoneType) {
            TelephonyManager.PHONE_TYPE_GSM -> "GSM"
            TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
            TelephonyManager.PHONE_TYPE_SIP -> "SIP"
            TelephonyManager.PHONE_TYPE_NONE -> context.getString(R.string.status_none)
            else -> statusUnknown
        }
    }

    private fun getWifiEnabledState(): String = safeGet(statusUnknown) {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return@safeGet statusUnknown
        if (wm.isWifiEnabled) context.getString(R.string.status_enabled) else context.getString(R.string.status_disabled)
    }

    private fun getNetworkMetered(cm: ConnectivityManager?): String = safeGet(statusUnknown) {
        val connectivityManager = cm ?: return@safeGet statusUnknown
        if (connectivityManager.isActiveNetworkMetered) context.getString(R.string.status_yes) else context.getString(R.string.status_no)
    }

    private fun getVpnState(nc: NetworkCapabilities?): String = safeGet(statusUnknown) {
        val caps = nc ?: return@safeGet statusUnknown
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            context.getString(R.string.status_enabled)
        } else {
            context.getString(R.string.status_disabled)
        }
    }

    private fun getAirplaneModeState(): String = safeGet(statusUnknown) {
        val enabled = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        if (enabled) context.getString(R.string.status_enabled) else context.getString(R.string.status_disabled)
    }

    private fun getDataRoamingState(): String = safeGet(statusUnknown) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (tm.isNetworkRoaming) context.getString(R.string.status_yes) else context.getString(R.string.status_no)
    }

    private fun getNetworkLinkSpeed(nc: NetworkCapabilities?): String = safeGet(statusUnknown) {
        val caps = nc ?: return@safeGet statusUnknown
        val down = caps.linkDownstreamBandwidthKbps / 1000
        val up = caps.linkUpstreamBandwidthKbps / 1000
        if (down <= 0 && up <= 0) return@safeGet statusUnknown
        val downStr = if (down > 0) "$down" else "?"
        val upStr = if (up > 0) "$up" else "?"
        "↓ $downStr Mbps / ↑ $upStr Mbps"
    }

    // ── APP 补充项 ──

    private fun getAppFirstInstallTime(p: PackageInfo?): String = safeGet(statusUnknown) {
        val pkg = p ?: return@safeGet statusUnknown
        SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(pkg.firstInstallTime))
    }

    private fun getAppLastUpdateTime(p: PackageInfo?): String = safeGet(statusUnknown) {
        val pkg = p ?: return@safeGet statusUnknown
        SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(Date(pkg.lastUpdateTime))
    }

    private fun getAppTargetSdk(): String = safeGet(statusUnknown) {
        context.applicationInfo.targetSdkVersion.toString()
    }

    private fun getAppMinSdk(): String = safeGet(statusUnknown) {
        context.applicationInfo.minSdkVersion.toString()
    }

    private fun getAppInstaller(): String = safeGet(statusUnknown) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            ?: context.getString(R.string.status_sideloaded)
    }

    /**
 * 读取当前应用"可见"的已安装包数量。
 *
 * 该方法并不等同于"设备总安装数"，原因：
 *  - Android 11 (API 30) 起引入 package visibility，未在 `<queries>` 中声明或通过
 *    [PackageManager.QUERY_ALL_PACKAGES] 获取权限的调用者，只能看到「对当前应用可见」的
 *    包。`getInstalledPackages(...)` 在 30+ 仍返回全集，但 11+ 的策略会决定调用方能感知
 *    哪些包「可见」，结果可能小于实际安装数。
 *  - 因此返回值仅适合用于"我应用能感知多少已安装包"这类业务判断；展示文案为「可见应用数」。
 *
 * 替代方案的取舍（均未在本项目采用，理由如下）：
 *  - 声明 `<queries>` 精确匹配：只能拿到声明范围内的包，仍不是总数。
 *  - 申请 `QUERY_ALL_PACKAGES`：能拿全部，但 Google Play 审核对非 launcher/杀毒/文件管理等
 *    场景严格，DevInfo 不属于允许该权限的类别。
 *  - 改用 [android.app.usage.UsageStatsManager] 统计有使用记录的包：能拿到更接近用户真实
 *    安装情况的子集，但需 `PACKAGE_USAGE_STATS` 特殊权限，且由用户在系统设置中手动授予，
 *    会破坏"零权限申请"的现状。
 */
private fun getVisibleInstalledAppCount(): String = safeGet(statusUnknown) {
    val count = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0)).size
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstalledPackages(0).size
    }
    count.toString()
}

    private fun infoItem(keyResId: Int, value: String, category: InfoCategory): DeviceInfoItem? {
        val normalizedValue = value.trim()
        if (normalizedValue.isBlank() || normalizedValue == statusUnknown) return null
        return DeviceInfoItem(
            key = context.resources.getResourceEntryName(keyResId),
            keyResId = keyResId,
            value = normalizedValue,
            category = category
        )
    }

    private companion object {
        const val TOP_COMMAND_TIMEOUT_MS = 1_500L

        /**
         * 已授权后读取 /proc、/sys 的上限。实测 su 往返 40–49 ms（max 72 ms），
         * 因此这里有数量级以上的余量。
         */
        const val ROOT_COMMAND_TIMEOUT_MS = 5_000L

        /** 进程已退出后等待读取线程收尾的时间。 */
        const val READER_DRAIN_TIMEOUT_MS = 1_000L

        /**
         * 可取消等待的检查间隔。
         *
         * 它是「点取消后多久真正停手」的上界：实测授权失败 5–19 ms、成功往返 40–49 ms，因此
         * 100 ms 的粒度不会让用户感到延迟，同时避免忙等。
         */
        const val PROCESS_POLL_INTERVAL_MS = 100L

        const val MAX_ROOT_OUTPUT_LINES = 512
    }
}
