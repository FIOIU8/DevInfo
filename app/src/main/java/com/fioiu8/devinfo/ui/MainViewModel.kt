package com.fioiu8.devinfo.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fioiu8.devinfo.BatteryObserver
import com.fioiu8.devinfo.BuildConfig
import com.fioiu8.devinfo.CpuCoreMetric
import com.fioiu8.devinfo.CpuUsageReading
import com.fioiu8.devinfo.CpuUsageSampler
import com.fioiu8.devinfo.DeviceInfoCollector
import com.fioiu8.devinfo.GitHubClient
import com.fioiu8.devinfo.LiveHardwareMonitor
import com.fioiu8.devinfo.LiveHardwareSnapshot
import com.fioiu8.devinfo.UpdateChecker
import com.fioiu8.devinfo.UpdateState
import com.fioiu8.devinfo.model.ItemWithVisibility
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Owns device data loading and foreground-only hardware monitoring for the main screen. */
class MainViewModel(
    private val collector: DeviceInfoCollector,
    private val cpuUsageSampler: CpuUsageSampler,
    private val liveHardwareMonitor: LiveHardwareMonitor,
    private val batteryObserver: BatteryObserver,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val reloadMutex = Mutex()
    private val overviewLock = Any()

    private val _deviceInfoItems = MutableStateFlow<List<ItemWithVisibility>>(emptyList())
    val deviceInfoItems: StateFlow<List<ItemWithVisibility>> = _deviceInfoItems.asStateFlow()

    private val _isDeviceInfoLoading = MutableStateFlow(true)
    val isDeviceInfoLoading: StateFlow<Boolean> = _isDeviceInfoLoading.asStateFlow()

    private val _overviewSnapshot = MutableStateFlow(OverviewSnapshot())
    val overviewSnapshot: StateFlow<OverviewSnapshot> = _overviewSnapshot.asStateFlow()

    private val _isOverviewLoading = MutableStateFlow(true)
    val isOverviewLoading: StateFlow<Boolean> = _isOverviewLoading.asStateFlow()

    val updateState: StateFlow<UpdateState> = updateChecker.state
    val releaseInfo: StateFlow<GitHubClient.ReleaseInfo?> = updateChecker.releaseInfo

    val appVersionName: String by lazy(collector::getAppVersionName)
    val appVersionCode: Long by lazy(collector::getAppVersionCode)

    private var refreshJob: Job? = null
    private var batteryJob: Job? = null
    private var dynamicMetricsJob: Job? = null
    private var hardwareMonitoringJob: Job? = null
    private var isForeground = false
    private var isInfoTabSelected = true

    init {
        refresh()
        checkForUpdates()
    }

    /** Starts a refresh in the ViewModel scope, sharing an existing refresh when one is underway. */
    fun refresh() {
        startRefreshIfNeeded()
    }

    /** Lets pull-to-refresh wait for ViewModel-owned work without owning its lifetime. */
    suspend fun refreshAndAwait() {
        startRefreshIfNeeded().join()
    }

    fun onForegroundChanged(foreground: Boolean) {
        isForeground = foreground
        updateMonitoring()
    }

    fun onInfoTabChanged(selected: Boolean) {
        isInfoTabSelected = selected
        updateMonitoring()
    }

    fun retryUpdateCheck() {
        checkForUpdates()
    }

    fun resetUpdateState() {
        updateChecker.reset()
    }

    private fun startRefreshIfNeeded(): Job {
        val existingJob = refreshJob
        if (existingJob?.isActive == true) return existingJob

        return viewModelScope.launch(start = CoroutineStart.DEFAULT) {
            reloadDeviceInfo()
        }.also { refreshJob = it }
    }

    private suspend fun reloadDeviceInfo() {
        reloadMutex.withLock {
            _isDeviceInfoLoading.value = true
            _isOverviewLoading.value = true
            _deviceInfoItems.value = emptyList()

            try {
                coroutineScope {
                    val overviewJob = async(Dispatchers.Default) { loadOverviewSnapshot() }
                    val deviceInfoJob = async { loadDeviceInfo() }
                    overviewJob.await()
                    deviceInfoJob.await()
                }
            } finally {
                _isDeviceInfoLoading.value = false
                _isOverviewLoading.value = false
            }
        }
    }

    private suspend fun loadDeviceInfo() {
        withContext(Dispatchers.Default) {
            collector.collectDeviceInfo { item ->
                withContext(Dispatchers.Main.immediate) {
                    _deviceInfoItems.value = _deviceInfoItems.value + ItemWithVisibility(
                        item = item,
                        visible = mutableStateOf(true)
                    )
                }
                delay(ITEM_APPEAR_DELAY_MS)
            }
        }
    }

    private suspend fun loadOverviewSnapshot() {
        val hardware = runCatching { liveHardwareMonitor.snapshot() }.getOrDefault(LiveHardwareSnapshot())
        updateOverview {
            OverviewSnapshot(
                batteryLevel = it.batteryLevel,
                batteryCharging = it.batteryCharging,
                hardware = hardware
            )
        }

        updateOverview { snapshot ->
            snapshot.copy(cpuFrequency = collector.getCpuFrequency())
        }

        val coreMetrics = collector.getCpuCoreMetrics()
        updateOverview { snapshot ->
            if (coreMetrics.isNotEmpty()) {
                snapshot.copy(cpuCoreMetrics = coreMetrics).withCpuUsageReading(coreMetrics)
            } else {
                snapshot.copy(cpuUsage = collector.getCpuUsagePercent())
            }
        }

        val dynamicMetrics = readDynamicMetrics()
        updateOverview { snapshot -> snapshot.withDynamicMetrics(dynamicMetrics) }
    }

    private suspend fun refreshDynamicMetrics() {
        reloadMutex.withLock {
            val dynamicMetrics = readDynamicMetrics()
            updateOverview { snapshot -> snapshot.withDynamicMetrics(dynamicMetrics) }
        }
    }

    private suspend fun readDynamicMetrics(): DynamicMetrics = withContext(Dispatchers.Default) {
        val security = collector.getSecuritySnapshot()
        DynamicMetrics(
            gpuFrequency = collector.getGpuFrequency(),
            gpuUsage = collector.getGpuUsagePercent(),
            memoryPercent = collector.getMemoryUsagePercent(),
            storagePercent = collector.getStorageUsagePercent(),
            securityPatch = security.securityPatch,
            lockScreenEnabled = security.lockScreenEnabled,
            usbDebuggingEnabled = security.usbDebuggingEnabled
        )
    }

    private fun updateMonitoring() {
        if (isForeground) {
            startBatteryObserver()
            startHardwareMonitoring()
        } else {
            stopBatteryObserver()
            stopHardwareMonitoring()
        }

        if (isForeground && isInfoTabSelected) {
            startDynamicMonitoring()
        } else {
            stopDynamicMonitoring()
        }
    }

    private fun startBatteryObserver() {
        if (batteryJob?.isActive == true) return

        batteryJob = viewModelScope.launch {
            batteryObserver.batteryState.collect { state ->
                updateOverview { snapshot ->
                    snapshot.copy(
                        batteryLevel = state.level,
                        batteryCharging = state.isCharging
                    )
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

        cpuUsageSampler.start { reading ->
            updateOverview { snapshot -> snapshot.withCpuUsageReading(reading) }
        }
        dynamicMetricsJob = viewModelScope.launch {
            while (isActive) {
                delay(DYNAMIC_REFRESH_INTERVAL_MS)
                refreshDynamicMetrics()
            }
        }
    }

    private fun stopDynamicMonitoring() {
        dynamicMetricsJob?.cancel()
        dynamicMetricsJob = null
        cpuUsageSampler.stop()
    }

    private fun startHardwareMonitoring() {
        if (hardwareMonitoringJob?.isActive == true) return

        liveHardwareMonitor.start()
        hardwareMonitoringJob = viewModelScope.launch {
            while (isActive) {
                val hardware = withContext(Dispatchers.Default) { liveHardwareMonitor.snapshot() }
                updateOverview { snapshot -> snapshot.copy(hardware = hardware) }
                delay(DYNAMIC_REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun stopHardwareMonitoring() {
        hardwareMonitoringJob?.cancel()
        hardwareMonitoringJob = null
        liveHardwareMonitor.stop()
    }

    private fun checkForUpdates() {
        if (!BuildConfig.IS_OFFICIAL) return

        viewModelScope.launch {
            updateChecker.check(BuildConfig.VERSION_NAME)
        }
    }

    private fun updateOverview(transform: (OverviewSnapshot) -> OverviewSnapshot) {
        synchronized(overviewLock) {
            _overviewSnapshot.value = transform(_overviewSnapshot.value)
        }
    }

    override fun onCleared() {
        isForeground = false
        updateMonitoring()
        super.onCleared()
    }

    companion object {
        private const val DYNAMIC_REFRESH_INTERVAL_MS = 2_000L
        private const val ITEM_APPEAR_DELAY_MS = 30L

        fun factory(
            collector: DeviceInfoCollector,
            cpuUsageSampler: CpuUsageSampler,
            liveHardwareMonitor: LiveHardwareMonitor,
            batteryObserver: BatteryObserver,
            updateChecker: UpdateChecker
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
                    updateChecker = updateChecker
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
    val values = if (reading.coreMetrics.isNotEmpty()) {
        reading.coreMetrics.mapNotNull { metric ->
            metric.usagePercent?.let { usage -> metric.index to usage }
        }.toMap()
    } else {
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

private fun OverviewSnapshot.withCpuUsageReading(metrics: List<CpuCoreMetric>): OverviewSnapshot {
    val overallUsage = metrics.mapNotNull { it.usagePercent }.average().toFloat().takeIf { !it.isNaN() }
    return withCpuUsageReading(CpuUsageReading(metrics, overallUsage))
}

private const val OVERALL_CPU_KEY = -1
private const val MAX_CPU_HISTORY_SIZE = 30
