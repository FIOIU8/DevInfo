package com.fioiu8.devinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.BatteryObserver
import com.fioiu8.devinfo.DeviceInfoCollector
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.model.ItemWithVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class OverviewCardSize(val span: Int) {
    SMALL(1),
    LARGE(2)
}

private data class OverviewMetric(
    val title: String,
    val value: String,
    val category: InfoCategory,
    val icon: ImageVector,
    val size: OverviewCardSize,
    val supportingText: String? = null,
    val progress: Float? = null
)

private data class HardwareOverview(
    val cpuFrequency: String?,
    val gpuFrequency: String?,
    val cpuUsage: Float?,
    val gpuUsage: Float?
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceInfoOverviewPage(
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    onRefresh: suspend () -> Unit,
    onOpenDetails: (InfoCategory) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val collector = remember { DeviceInfoCollector(context) }
    val batteryObserver = remember { BatteryObserver(context) }
    val batteryState by batteryObserver.batteryState.collectAsState(
        initial = BatteryObserver.BatteryState(level = 100, isCharging = false)
    )
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    var cpuFrequency by remember { mutableStateOf<String?>(null) }
    var gpuFrequency by remember { mutableStateOf<String?>(null) }
    var cpuUsage by remember { mutableStateOf<Float?>(null) }
    var gpuUsage by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) {
        val hardware = withContext(Dispatchers.Default) {
            HardwareOverview(
                cpuFrequency = collector.getCpuFrequency(),
                gpuFrequency = collector.getGpuFrequency(),
                cpuUsage = collector.getCpuUsagePercent(),
                gpuUsage = collector.getGpuUsagePercent()
            )
        }
        cpuFrequency = hardware.cpuFrequency
        gpuFrequency = hardware.gpuFrequency
        cpuUsage = hardware.cpuUsage
        gpuUsage = hardware.gpuUsage
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    val storagePercent = collector.getStorageUsagePercent()
    val memoryPercent = collector.getMemoryUsagePercent()
    val metrics = buildOverviewMetrics(
        cpuFrequency = cpuFrequency,
        gpuFrequency = gpuFrequency,
        cpuUsage = cpuUsage,
        gpuUsage = gpuUsage,
        storagePercent = storagePercent,
        memoryPercent = memoryPercent,
        batteryLevel = batteryState.level,
        batteryCharging = batteryState.isCharging
    )

    if (isLoading && itemsState.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = if (screenWidthDp < 600) {
                    GridCells.Fixed(2)
                } else {
                    GridCells.Adaptive(minSize = 148.dp)
                },
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = metrics,
                    key = { metric -> metric.title },
                    span = { metric -> GridItemSpan(metric.size.span) }
                ) { metric ->
                    OverviewMetricCard(
                        metric = metric,
                        onClick = { onOpenDetails(metric.category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun buildOverviewMetrics(
    cpuFrequency: String?,
    gpuFrequency: String?,
    cpuUsage: Float?,
    gpuUsage: Float?,
    storagePercent: Float,
    memoryPercent: Float,
    batteryLevel: Int,
    batteryCharging: Boolean
): List<OverviewMetric> = buildList {
    cpuFrequency?.let {
        add(
            OverviewMetric(
                title = "CPU",
                value = it,
                category = InfoCategory.SYSTEM,
                icon = itemIconByResId(R.string.system_cpu_arch),
                size = OverviewCardSize.LARGE,
                supportingText = stringResourceValue(R.string.overview_cpu_frequency)
            )
        )
    }
    cpuUsage?.let {
        add(
            OverviewMetric(
                title = stringResourceValue(R.string.overview_cpu_usage),
                value = formatPercent(it),
                category = InfoCategory.SYSTEM,
                icon = itemIconByResId(R.string.system_cpu_cores),
                size = OverviewCardSize.SMALL,
                progress = it / 100f
            )
        )
    }
    gpuFrequency?.let {
        add(
            OverviewMetric(
                title = stringResourceValue(R.string.overview_gpu_frequency),
                value = it,
                category = InfoCategory.SYSTEM,
                icon = categoryIcon(InfoCategory.DISPLAY),
                size = OverviewCardSize.SMALL
            )
        )
    }
    gpuUsage?.let {
        add(
            OverviewMetric(
                title = stringResourceValue(R.string.overview_gpu_usage),
                value = formatPercent(it),
                category = InfoCategory.SYSTEM,
                icon = categoryIcon(InfoCategory.DISPLAY),
                size = OverviewCardSize.SMALL,
                progress = it / 100f
            )
        )
    }
    add(
        OverviewMetric(
            title = stringResourceValue(R.string.overview_memory),
            value = formatPercent(memoryPercent),
            category = InfoCategory.STORAGE,
            icon = categoryIcon(InfoCategory.STORAGE),
            size = OverviewCardSize.SMALL,
            progress = memoryPercent / 100f
        )
    )
    add(
        OverviewMetric(
            title = stringResourceValue(R.string.overview_storage),
            value = formatPercent(storagePercent),
            category = InfoCategory.STORAGE,
            icon = categoryIcon(InfoCategory.STORAGE),
            size = OverviewCardSize.SMALL,
            progress = storagePercent / 100f
        )
    )
    add(
        OverviewMetric(
            title = stringResourceValue(R.string.overview_battery),
            value = "${batteryLevel.coerceIn(0, 100)}%",
            category = InfoCategory.BATTERY,
            icon = categoryIcon(InfoCategory.BATTERY),
            size = OverviewCardSize.SMALL,
            supportingText = if (batteryCharging) stringResourceValue(R.string.status_charging) else null,
            progress = batteryLevel.coerceIn(0, 100) / 100f
        )
    )
}

@Composable
private fun stringResourceValue(resourceId: Int): String = stringResource(resourceId)

private fun formatPercent(value: Float): String = "%.1f%%".format(Locale.US, value.coerceIn(0f, 100f))

@Composable
private fun OverviewMetricCard(metric: OverviewMetric, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (metric.size == OverviewCardSize.LARGE) 156.dp else 132.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = metric.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.value,
                style = if (metric.size == OverviewCardSize.LARGE) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            metric.supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            metric.progress?.let {
                LinearProgressIndicator(
                    progress = { it.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
