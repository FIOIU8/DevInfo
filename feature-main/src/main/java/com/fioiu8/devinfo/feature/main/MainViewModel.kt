/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import androidx.annotation.MainThread
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fioiu8.devinfo.core.model.CpuUsageSample
import com.fioiu8.devinfo.core.model.CpuUsageReading
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import com.fioiu8.devinfo.core.model.LiveHardwareSnapshot
import com.fioiu8.devinfo.core.model.HardwareDetailsSnapshot
import com.fioiu8.devinfo.core.model.MotionSensorSnapshot
import com.fioiu8.devinfo.core.model.OverviewSnapshot
import com.fioiu8.devinfo.core.model.SecuritySnapshot
import com.fioiu8.devinfo.core.model.UpdateState
import com.fioiu8.devinfo.core.root.RootAccess
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
    SENSORS,
    ACTIVE
}

fun monitorModeFor(
    isForeground: Boolean,
    isOverviewVisible: Boolean,
    isSensorsPageVisible: Boolean = false,
    isDataPageVisible: Boolean = true,
): MonitorMode = when {
    !isForeground -> MonitorMode.STOPPED
    !isDataPageVisible -> MonitorMode.STOPPED
    isOverviewVisible -> MonitorMode.ACTIVE
    isSensorsPageVisible -> MonitorMode.SENSORS
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
    private val dynamicMetricsMutex = Mutex()

    @Immutable
    data class MainUiState(
        val deviceInfoItems: List<ItemWithVisibility> = emptyList(),
        val isDeviceInfoLoading: Boolean = true,
        val isOverviewLoading: Boolean = true,
        val isRootModeEnabled: Boolean = false,
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Live metrics 与传感器快照刻意独立于 [uiState]。
     *
     * 传感器在详情页以 10 Hz 发布快照；若并入 uiState，收集整个状态的页面
     * （包括设置页）都会每 100 ms 重组。拆开后只有真正渲染该数据的页面订阅。
     */
    private val _overviewSnapshot = MutableStateFlow(OverviewSnapshot())
    val overviewSnapshot: StateFlow<OverviewSnapshot> = _overviewSnapshot.asStateFlow()

    /** 相机/传感器静态清单只在刷新时变化，与实时流分开可避免详情页重复重组。 */
    private val _hardwareDetails = MutableStateFlow(HardwareDetailsSnapshot())
    val hardwareDetails: StateFlow<HardwareDetailsSnapshot> = _hardwareDetails.asStateFlow()

    /** 运动传感器快照直接来自硬件监听器，ViewModel 不再复制。 */
    val motionSensors: StateFlow<MotionSensorSnapshot> = liveHardwareMonitor.motionSensorSnapshot

    private val _refreshFailed = MutableStateFlow(false)

    /** 最近一次刷新是否以可向用户说明的失败结束；UI 读取后调用 [consumeRefreshFailure]。 */
    val refreshFailed: StateFlow<Boolean> = _refreshFailed.asStateFlow()

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
    private var isDataPageVisible = true
    private var isOverviewVisible = true
    private var isSensorsPageVisible = false
    private val _monitorMode = MutableStateFlow(MonitorMode.STOPPED)
    val monitorMode: StateFlow<MonitorMode> = _monitorMode.asStateFlow()

    private var hasStarted = false

    init {
        // PackageManager 查询是阻塞调用；预取到 IO 线程，避免首次打开设置页时卡在主线程。
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { collector.getAppVersionName() }
            runCatching { collector.getAppVersionCode() }
        }
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

    /**
     * 用户主动发起的 Root 授权：向管理器请求一次 `su`，成功则直接启用增强 CPU 监控。
     *
     * 失败原因原样透传（见 [RootAccess] 的五态模型），因为 UI 需要按原因给出不同的下一步；
     * 这里没有「先探测再申请」的两段式——探测本身就是一次真实的 `su` 往返。
     *
     * 取消由调用方负责（用户点「取消」时取消承载本方法的协程），取消时 `su` 进程会被强杀，
     * 且本方法原样抛出 [CancellationException]，不把它当成失败原因。
     *
     * @return [RootAccess.Granted] 表示已启用 Root 监控模式；其它取值表示失败原因
     */
    suspend fun requestRootMode(): RootAccess {
        val access = try {
            withContext(Dispatchers.IO) { collector.requestRootAccess() }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return RootAccess.Unknown
        }
        if (access != RootAccess.Granted) return access
        return enableRootMode()
    }

    /**
     * 尝试用已获得的 Root 权限读取 CPU 数据并启用 Root 监控模式。
     *
     * @return 读取成功为 [RootAccess.Granted]；`su` 通了但读不到数据时无从判断原因（底层读取
     *   按契约返回空列表而不带原因），因此返回 [RootAccess.Unknown]。
     */
    suspend fun enableRootMode(): RootAccess {
        val metrics = try {
            withContext(Dispatchers.IO) { collector.getCpuCoreMetricsWithRoot() }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
        if (metrics.isEmpty()) return RootAccess.Unknown
        _uiState.update { it.copy(isRootModeEnabled = true) }
        val overallUsage = metrics.mapNotNull { it.usagePercent }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
        val cpuReading = CpuUsageReading(metrics, overallUsage)
        updateOverview { snapshot -> snapshot.withCpuUsageReading(cpuReading) }
        if (dynamicMetricsJob?.isActive == true) startCpuSampling()
        return RootAccess.Granted
    }

    /**
     * 路由可见性统一入口。
     *
     * 概览与详情渲染实时数据，设置/主题/关于不渲染任何采集结果；后者继续轮询只会
     * 白白唤醒硬件与电池监听，因此把「是否数据页」也纳入监控策略。
     */
    @MainThread
    fun onRouteVisibilityChanged(isDataPageVisible: Boolean, isOverviewVisible: Boolean) {
        this.isDataPageVisible = isDataPageVisible
        this.isOverviewVisible = isOverviewVisible
        if (!isDataPageVisible) isSensorsPageVisible = false
        updateMonitoring()
    }

    /** Starts live motion readings only while the sensor detail page is visible. */
    @MainThread
    fun onSensorsPageVisibilityChanged(visible: Boolean) {
        isSensorsPageVisible = visible
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
                val session = withContext(Dispatchers.IO) { collector.newCollectionSession() }
                coroutineScope {
                    // 概览采集读取 /proc、/sys 并可能派生 top 子进程，走 IO 调度器
                    val overviewJob = async(Dispatchers.IO) { loadOverviewSnapshot() }
                    val deviceInfoJob = async { loadDeviceInfo(session, publishBatches = isInitialLoad) }
                    val hardwareDetailsJob = async(Dispatchers.IO) {
                        collector.collectHardwareDetails(session)
                    }
                    overviewJob.await()
                    deviceInfoJob.await()
                    val hardwareDetails = hardwareDetailsJob.await()
                    withContext(Dispatchers.Main.immediate) {
                        _hardwareDetails.value = hardwareDetails
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // 刷新失败保留旧数据，只向 UI 暴露一个可提示的标记；
                // 否则异常会逃出 viewModelScope 并终止进程。
                _refreshFailed.value = true
            } finally {
                _uiState.update {
                    it.copy(isDeviceInfoLoading = false, isOverviewLoading = false)
                }
            }
        }
    }

    private suspend fun loadDeviceInfo(session: DeviceInfoCollector.CollectionSession, publishBatches: Boolean) {
        withContext(Dispatchers.IO) {
            // Accumulate into a single buffer and publish in batches. This avoids
            // the previous per-item `delay` (which pushed full-load time to ~3s) and
            // the O(n^2) `list + item` copy that reallocated the whole list each item.
            // Entrance animation is handled by the UI layer (AnimatedVisibility), so
            // no artificial delay is needed here.
            val buffer = ArrayList<ItemWithVisibility>(EXPECTED_ITEM_COUNT)
            collector.collectDeviceInfo(session) { item ->
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
        val cpuReading = try {
            collector.readCpuUsageReading(useRoot = _uiState.value.isRootModeEnabled)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            CpuUsageReading(emptyList(), null)
        }
        val coreMetrics = cpuReading.coreMetrics
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
        dynamicMetricsMutex.withLock {
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
        val mode = monitorModeFor(
            isForeground = isForeground,
            isDataPageVisible = isDataPageVisible,
            isOverviewVisible = isOverviewVisible,
            isSensorsPageVisible = isSensorsPageVisible,
        )
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

            MonitorMode.SENSORS -> {
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
                }
                // 无论正常结束还是异常都退避：flow 正常完成时立即重订阅会形成忙等。
                delay(BATTERY_RETRY_INTERVAL_MS)
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
            // 首次延迟一个周期：初始刷新已经读取过一轮动态指标。
            delay(ACTIVE_REFRESH_INTERVAL_MS)
            while (isActive) {
                val startedAt = android.os.SystemClock.uptimeMillis()
                refreshDynamicMetrics()
                val elapsed = android.os.SystemClock.uptimeMillis() - startedAt
                delay((ACTIVE_REFRESH_INTERVAL_MS - elapsed).coerceAtLeast(0L))
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
        val collectMotion = mode == MonitorMode.ACTIVE || mode == MonitorMode.SENSORS
        liveHardwareMonitor.start(
            collectMotion = collectMotion,
            collectLiveReadings = mode == MonitorMode.SENSORS
        )
        val interval = if (mode == MonitorMode.ACTIVE || mode == MonitorMode.SENSORS) {
            ACTIVE_REFRESH_INTERVAL_MS
        } else {
            LOW_FREQUENCY_REFRESH_INTERVAL_MS
        }
        hardwareMonitoringJob = viewModelScope.launch {
            while (isActive) {
                val startedAt = android.os.SystemClock.uptimeMillis()
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
                val elapsed = android.os.SystemClock.uptimeMillis() - startedAt
                delay((interval - elapsed).coerceAtLeast(0L))
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

    /** 读取并清除本次刷新失败标记；返回 true 表示 UI 应提示用户。 */
    fun consumeRefreshFailure(): Boolean = _refreshFailed.value.also { _refreshFailed.value = false }

    private fun updateOverview(transform: (OverviewSnapshot) -> OverviewSnapshot) {
        _overviewSnapshot.update(transform)
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
