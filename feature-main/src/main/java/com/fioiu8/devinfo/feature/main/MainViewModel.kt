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

import androidx.annotation.MainThread
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fioiu8.devinfo.core.model.CpuCoreMetric
import com.fioiu8.devinfo.core.model.CpuUsageSample
import com.fioiu8.devinfo.core.model.CpuUsageReading
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import com.fioiu8.devinfo.core.model.LiveHardwareSnapshot
import com.fioiu8.devinfo.core.model.OverviewSnapshot
import com.fioiu8.devinfo.core.model.SecuritySnapshot
import com.fioiu8.devinfo.core.model.UpdateState
import com.fioiu8.devinfo.data.BatteryObserver
import com.fioiu8.devinfo.data.CpuUsageSampler
import com.fioiu8.devinfo.data.DeviceInfoCollector
import com.fioiu8.devinfo.data.GitHubClient
import com.fioiu8.devinfo.data.LiveHardwareMonitor
import com.fioiu8.devinfo.data.ThemePreferences
import com.fioiu8.devinfo.data.UpdateChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class MonitorMode {
    STOPPED,
    LOW_FREQUENCY,
    ACTIVE
}

fun monitorModeFor(
    isForeground: Boolean,
    isOverviewVisible: Boolean
): MonitorMode = when {
    !isForeground -> MonitorMode.STOPPED
    isOverviewVisible -> MonitorMode.ACTIVE
    else -> MonitorMode.LOW_FREQUENCY
}

/** Owns device data loading and foreground-only hardware monitoring for the main screen. */
class MainViewModel(
    private val collector: DeviceInfoCollector,
    private val cpuUsageSampler: CpuUsageSampler,
    private val liveHardwareMonitor: LiveHardwareMonitor,
    private val batteryObserver: BatteryObserver,
    private val updateChecker: UpdateChecker,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val reloadMutex = Mutex()
    private val refreshMutex = Mutex()

    @Immutable
    data class MainUiState(
        val deviceInfoItems: List<ItemWithVisibility> = emptyList(),
        val isDeviceInfoLoading: Boolean = true,
        val overviewSnapshot: OverviewSnapshot = OverviewSnapshot(),
        val isOverviewLoading: Boolean = true,
        val isRootModeEnabled: Boolean = false,
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val updateState: StateFlow<UpdateState> = updateChecker.state
    val releaseInfo: StateFlow<GitHubClient.ReleaseInfo?> = updateChecker.releaseInfo

    val appVersionName: String by lazy(collector::getAppVersionName)
    val appVersionCode: Long by lazy(collector::getAppVersionCode)

    private var refreshJob: Job? = null
    private var batteryJob: Job? = null
    private var dynamicMetricsJob: Job? = null
    private var hardwareMonitoringJob: Job? = null
    private var hardwareMonitoringGeneration = 0L
    private var isForeground = false
    private var isInfoTabSelected = true
    private val _monitorMode = MutableStateFlow(MonitorMode.STOPPED)
    val monitorMode: StateFlow<MonitorMode> = _monitorMode.asStateFlow()

    private var hasStarted = false

    init {
        // 延迟到首帧渲染后再启动数据加载
    }

    /** 由 UI 层在首次组合后调用，触发数据加载和更新检查 */
    fun onFirstComposition() {
        if (hasStarted) return
        hasStarted = true
        refresh()
        checkForUpdates()
    }

    /** Starts a refresh in the ViewModel scope, sharing an existing refresh when one is underway. */
    fun refresh() {
        viewModelScope.launch { startRefreshIfNeeded() }
    }

    /** Lets pull-to-refresh wait for ViewModel-owned work without owning its lifetime. */
    suspend fun refreshAndAwait() {
        startRefreshIfNeeded().join()
    }

    @MainThread
    fun onForegroundChanged(foreground: Boolean) {
        isForeground = foreground
        updateMonitoring()
    }

    /** 检测 Root 权限是否可用 */
    suspend fun checkRootAvailable(): Boolean {
        return try {
            withContext(Dispatchers.IO) { collector.isRootAvailable() }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 尝试启用 Root 监控模式。
     * @return Root 模式是否成功启用
     */
    suspend fun enableRootMode(): Boolean {
        val metrics = try {
            withContext(Dispatchers.IO) { collector.getCpuCoreMetricsWithRoot() }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
        if (metrics.isEmpty()) return false
        _uiState.update { it.copy(isRootModeEnabled = true) }
        val overallUsage = metrics.mapNotNull { it.usagePercent }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
        val cpuReading = CpuUsageReading(metrics, overallUsage)
        updateOverview { snapshot -> snapshot.withCpuUsageReading(cpuReading) }
        if (dynamicMetricsJob?.isActive == true) startCpuSampling()
        return true
    }

    @MainThread
    fun onInfoTabChanged(selected: Boolean) {
        isInfoTabSelected = selected
        updateMonitoring()
    }

    @Volatile private var isUserInitiatedUpdateCheck = false

    fun retryUpdateCheck() {
        isUserInitiatedUpdateCheck = true
        checkForUpdates()
    }

    /** 读取并清除“本次更新检查是否由用户手动触发”的标记 */
    fun consumeUserInitiatedUpdateCheck(): Boolean =
        isUserInitiatedUpdateCheck.also { isUserInitiatedUpdateCheck = false }

    fun resetUpdateState() {
        updateChecker.reset()
    }

    private suspend fun startRefreshIfNeeded(): Job = refreshMutex.withLock {
        refreshJob?.takeIf { it.isActive }?.let { return it }

        viewModelScope.launch(start = CoroutineStart.DEFAULT) {
            reloadDeviceInfo()
        }.also { refreshJob = it }
    }

    private suspend fun reloadDeviceInfo() {
        reloadMutex.withLock {
            // 仅首次加载显示全屏加载态；下拉刷新时保留旧内容，避免列表清空、
            // 滚动位置与选中状态丢失
            val isInitialLoad = _uiState.value.deviceInfoItems.isEmpty()
            if (isInitialLoad) {
                _uiState.update {
                    it.copy(
                        isDeviceInfoLoading = true,
                        isOverviewLoading = true,
                    )
                }
            }

            try {
                coroutineScope {
                    // 概览采集读取 /proc、/sys 并可能派生 top 子进程，走 IO 调度器
                    val overviewJob = async(Dispatchers.IO) { loadOverviewSnapshot() }
                    val deviceInfoJob = async { loadDeviceInfo(publishBatches = isInitialLoad) }
                    overviewJob.await()
                    deviceInfoJob.await()
                }
            } finally {
                _uiState.update {
                    it.copy(isDeviceInfoLoading = false, isOverviewLoading = false)
                }
            }
        }
    }

    private suspend fun loadDeviceInfo(publishBatches: Boolean) {
        withContext(Dispatchers.IO) {
            // Accumulate into a single buffer and publish in batches. This avoids
            // the previous per-item `delay` (which pushed full-load time to ~3s) and
            // the O(n^2) `list + item` copy that reallocated the whole list each item.
            // Entrance animation is handled by the UI layer (AnimatedVisibility), so
            // no artificial delay is needed here.
            val buffer = ArrayList<ItemWithVisibility>(EXPECTED_ITEM_COUNT)
            collector.collectDeviceInfo { item ->
                buffer.add(ItemWithVisibility(item = item, visible = true))
                if (publishBatches && buffer.size % ITEM_BATCH_SIZE == 0) {
                    val snapshot = buffer.toList()
                    withContext(Dispatchers.Main.immediate) {
                        _uiState.update { it.copy(deviceInfoItems = snapshot) }
                    }
                }
            }
            val finalSnapshot = buffer.toList()
            withContext(Dispatchers.Main.immediate) {
                _uiState.update { it.copy(deviceInfoItems = finalSnapshot) }
            }
        }
    }

    private suspend fun loadOverviewSnapshot() {
        val hardware = runCatching { liveHardwareMonitor.snapshot() }.getOrDefault(LiveHardwareSnapshot())
        val cpuFrequency = runCatching { collector.getCpuFrequency() }.getOrNull()
        val coreMetrics = try {
            collector.getCpuCoreMetrics()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
        val overallUsage = coreMetrics.mapNotNull { it.usagePercent }
            .average()
            .toFloat()
            .takeIf { !it.isNaN() }
            ?: try {
                collector.getCpuUsagePercent()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        val cpuReading = CpuUsageReading(coreMetrics, overallUsage)
        val dynamicMetrics = readDynamicMetrics()
        updateOverview { snapshot ->
            // 基于旧快照 copy 而非新建，保住 cpuUsageHistory 等字段——
            // 否则每次刷新 CPU 趋势图都会塌缩为单点
            snapshot.copy(
                hardware = hardware,
                cpuFrequency = cpuFrequency,
                cpuCoreMetrics = coreMetrics,
                securityPatch = dynamicMetrics.securityPatch,
                lockScreenEnabled = dynamicMetrics.lockScreenEnabled,
                usbDebuggingEnabled = dynamicMetrics.usbDebuggingEnabled
            ).withCpuUsageReading(cpuReading).withDynamicMetrics(dynamicMetrics)
        }
    }

    private suspend fun refreshDynamicMetrics() {
        reloadMutex.withLock {
            val dynamicMetrics = readDynamicMetrics()
            updateOverview { snapshot -> snapshot.withDynamicMetrics(dynamicMetrics) }
        }
    }

    private suspend fun readDynamicMetrics(): DynamicMetrics = withContext(Dispatchers.IO) {
        val security = runCatching { collector.getSecuritySnapshot() }.getOrDefault(SecuritySnapshot(null, null, null))
        DynamicMetrics(
            gpuFrequency = runCatching { collector.getGpuFrequency() }.getOrNull(),
            gpuUsage = runCatching { collector.getGpuUsagePercent() }.getOrNull(),
            memoryPercent = runCatching { collector.getMemoryUsagePercent() }.getOrNull(),
            storagePercent = runCatching { collector.getStorageUsagePercent() }.getOrNull(),
            securityPatch = security.securityPatch,
            lockScreenEnabled = security.lockScreenEnabled,
            usbDebuggingEnabled = security.usbDebuggingEnabled
        )
    }

    private fun updateMonitoring() {
        val mode = monitorModeFor(isForeground, isInfoTabSelected)
        if (_monitorMode.value == mode) return
        _monitorMode.value = mode

        when (mode) {
            MonitorMode.STOPPED -> {
                stopBatteryObserver()
                stopHardwareMonitoring()
                stopDynamicMonitoring()
            }

            MonitorMode.LOW_FREQUENCY -> {
                startBatteryObserver()
                startHardwareMonitoring(mode)
                stopDynamicMonitoring()
            }

            MonitorMode.ACTIVE -> {
                startBatteryObserver()
                startHardwareMonitoring(mode)
                startDynamicMonitoring()
            }
        }
    }

    private fun startBatteryObserver() {
        if (batteryJob?.isActive == true) return

        batteryJob = viewModelScope.launch {
            while (isActive) {
                try {
                    batteryObserver.batteryState.collect { state ->
                        updateOverview { snapshot ->
                            snapshot.copy(
                                batteryLevel = state.level,
                                batteryCharging = state.isCharging
                            )
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    updateOverview { snapshot -> snapshot.copy(batteryLevel = null, batteryCharging = false) }
                    delay(BATTERY_RETRY_INTERVAL_MS)
                }
            }
        }
    }

    private fun stopBatteryObserver() {
        batteryJob?.cancel()
        batteryJob = null
    }

    private fun startDynamicMonitoring() {
        if (dynamicMetricsJob?.isActive == true) return

        startCpuSampling()
        dynamicMetricsJob = viewModelScope.launch {
            while (isActive) {
                delay(ACTIVE_REFRESH_INTERVAL_MS)
                refreshDynamicMetrics()
            }
        }
    }

    private fun stopDynamicMonitoring() {
        dynamicMetricsJob?.cancel()
        dynamicMetricsJob = null
        cpuUsageSampler.stop()
    }

    private fun startCpuSampling() {
        cpuUsageSampler.start(useRoot = _uiState.value.isRootModeEnabled) { reading ->
            updateOverview { snapshot -> snapshot.withCpuUsageReading(reading) }
        }
    }

    private fun startHardwareMonitoring(mode: MonitorMode) {
        if (hardwareMonitoringJob?.isActive == true) stopHardwareMonitoring()

        val generation = ++hardwareMonitoringGeneration
        liveHardwareMonitor.start(collectMotion = mode == MonitorMode.ACTIVE)
        val interval = if (mode == MonitorMode.ACTIVE) ACTIVE_REFRESH_INTERVAL_MS else LOW_FREQUENCY_REFRESH_INTERVAL_MS
        hardwareMonitoringJob = viewModelScope.launch {
            while (isActive) {
                val hardware = try {
                    withContext(Dispatchers.IO) {
                        liveHardwareMonitor.snapshot(includeStorageReadSpeed = mode == MonitorMode.ACTIVE)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
                if (generation != hardwareMonitoringGeneration) break
                hardware?.let {
                    updateOverview { snapshot -> snapshot.copy(hardware = hardware) }
                }
                delay(interval)
            }
        }
    }

    private fun stopHardwareMonitoring() {
        hardwareMonitoringGeneration++
        hardwareMonitoringJob?.cancel()
        hardwareMonitoringJob = null
        liveHardwareMonitor.stop()
    }

    private fun checkForUpdates() {
        if (!BuildConfig.IS_OFFICIAL) return
        if (!themePreferences.getCheckUpdateSnapshot()) return

        viewModelScope.launch {
            updateChecker.check(BuildConfig.VERSION_NAME)
        }
    }

    @MainThread
    private fun updateOverview(transform: (OverviewSnapshot) -> OverviewSnapshot) {
        _uiState.update {
            it.copy(overviewSnapshot = transform(it.overviewSnapshot))
        }
    }

    override fun onCleared() {
        isForeground = false
        updateMonitoring()
        super.onCleared()
    }

    companion object {
        private const val ACTIVE_REFRESH_INTERVAL_MS = 3_000L
        private const val LOW_FREQUENCY_REFRESH_INTERVAL_MS = 15_000L
        private const val BATTERY_RETRY_INTERVAL_MS = 10_000L

        /** Publish loaded items in batches so the UI updates a few times, not once per item. */
        private const val ITEM_BATCH_SIZE = 8

        /** Rough initial capacity for the item buffer to avoid ArrayList regrowth. */
        private const val EXPECTED_ITEM_COUNT = 110

        fun factory(
            collector: DeviceInfoCollector,
            cpuUsageSampler: CpuUsageSampler,
            liveHardwareMonitor: LiveHardwareMonitor,
            batteryObserver: BatteryObserver,
            updateChecker: UpdateChecker,
            themePreferences: ThemePreferences,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return MainViewModel(
                    collector = collector,
                    cpuUsageSampler = cpuUsageSampler,
                    liveHardwareMonitor = liveHardwareMonitor,
                    batteryObserver = batteryObserver,
                    updateChecker = updateChecker,
                    themePreferences = themePreferences
                ) as T
            }
        }
    }
}

private data class DynamicMetrics(
    val gpuFrequency: String?,
    val gpuUsage: Float?,
    val memoryPercent: Float?,
    val storagePercent: Float?,
    val securityPatch: String?,
    val lockScreenEnabled: Boolean?,
    val usbDebuggingEnabled: Boolean?
)

private fun OverviewSnapshot.withDynamicMetrics(metrics: DynamicMetrics): OverviewSnapshot = copy(
    gpuFrequency = metrics.gpuFrequency,
    gpuUsage = metrics.gpuUsage,
    memoryPercent = metrics.memoryPercent,
    storagePercent = metrics.storagePercent,
    securityPatch = metrics.securityPatch,
    lockScreenEnabled = metrics.lockScreenEnabled,
    usbDebuggingEnabled = metrics.usbDebuggingEnabled
)

private fun OverviewSnapshot.withCpuUsageReading(reading: CpuUsageReading): OverviewSnapshot {
    val currentMetrics = reading.coreMetrics.ifEmpty { cpuCoreMetrics }
    val perCoreValues = if (reading.coreMetrics.isNotEmpty()) {
        reading.coreMetrics.associate { metric ->
            metric.index to (metric.usagePercent ?: -1f)
        }
    } else {
        emptyMap()
    }
    val values = perCoreValues.ifEmpty {
        reading.overallUsage?.let { mapOf(OVERALL_CPU_KEY to it) }.orEmpty()
    }
    val history = if (values.isEmpty()) {
        cpuUsageHistory
    } else {
        (cpuUsageHistory + CpuUsageSample(System.currentTimeMillis(), values)).takeLast(MAX_CPU_HISTORY_SIZE)
    }

    return copy(
        cpuCoreMetrics = currentMetrics,
        cpuUsage = reading.overallUsage,
        cpuUsageHistory = history
    )
}

private const val OVERALL_CPU_KEY = -1
private const val MAX_CPU_HISTORY_SIZE = 20
