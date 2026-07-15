package com.fioiu8.devinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.CpuCoreMetric
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.model.ItemWithVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import java.util.Locale

private enum class OverviewCardSize(val span: Int) {
    SMALL(1),
    LARGE(2)
}

data class OverviewSnapshot(
    val cpuFrequency: String? = null,
    val gpuFrequency: String? = null,
    val cpuUsage: Float? = null,
    val gpuUsage: Float? = null,
    val cpuCoreMetrics: List<CpuCoreMetric> = emptyList(),
    val storagePercent: Float? = null,
    val memoryPercent: Float? = null,
    val batteryLevel: Int? = null,
    val batteryCharging: Boolean = false
)

private data class OverviewMetric(
    val title: String,
    val value: String? = null,
    val category: InfoCategory,
    val icon: ImageVector,
    val size: OverviewCardSize,
    val supportingText: String? = null,
    val progress: Float? = null,
    val coreMetrics: List<CpuCoreMetric> = emptyList()
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceInfoOverviewPage(
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    isOverviewLoading: Boolean,
    snapshot: OverviewSnapshot,
    onRefresh: suspend () -> Unit,
    onOpenDetails: (InfoCategory) -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    val metrics = buildOverviewMetrics(
        snapshot = snapshot
    )

    if ((isLoading && itemsState.isEmpty()) || (isOverviewLoading && metrics.isEmpty())) {
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
    snapshot: OverviewSnapshot
): List<OverviewMetric> = buildList {
    if (snapshot.cpuCoreMetrics.isNotEmpty()) {
        add(
            OverviewMetric(
                title = "CPU",
                category = InfoCategory.SYSTEM,
                icon = itemIconByResId(R.string.system_cpu_arch),
                size = OverviewCardSize.LARGE,
                supportingText = snapshot.cpuFrequency?.let { stringResourceValue(R.string.overview_cpu_frequency) + ": $it" },
                coreMetrics = snapshot.cpuCoreMetrics
            )
        )
    } else {
        snapshot.cpuFrequency?.let {
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
        snapshot.cpuUsage?.let {
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
    }
    snapshot.gpuFrequency?.let {
        add(
            OverviewMetric(
                title = stringResourceValue(R.string.overview_gpu_frequency),
                value = it,
                category = InfoCategory.DISPLAY,
                icon = categoryIcon(InfoCategory.DISPLAY),
                size = OverviewCardSize.SMALL,
            )
        )
    }
    snapshot.gpuUsage?.let {
        add(
            OverviewMetric(
                title = stringResourceValue(R.string.overview_gpu_usage),
                value = formatPercent(it),
                category = InfoCategory.DISPLAY,
                icon = categoryIcon(InfoCategory.DISPLAY),
                size = OverviewCardSize.SMALL,
                progress = it / 100f
            )
        )
    }
    snapshot.memoryPercent?.let {
        add(
            OverviewMetric(
                title = stringResourceValue(R.string.overview_memory),
                value = formatPercent(it),
                category = InfoCategory.STORAGE,
                icon = categoryIcon(InfoCategory.STORAGE),
                size = OverviewCardSize.SMALL,
                progress = it / 100f
            )
        )
    }
    snapshot.storagePercent?.let {
        add(OverviewMetric(stringResourceValue(R.string.overview_storage), formatPercent(it), InfoCategory.STORAGE, categoryIcon(InfoCategory.STORAGE), OverviewCardSize.SMALL, progress = it / 100f))
    }
    snapshot.batteryLevel?.let { level ->
        val safeLevel = level.coerceIn(0, 100)
        add(OverviewMetric(stringResourceValue(R.string.overview_battery), "$safeLevel%", InfoCategory.BATTERY, categoryIcon(InfoCategory.BATTERY), OverviewCardSize.SMALL, if (snapshot.batteryCharging) stringResourceValue(R.string.status_charging) else null, safeLevel / 100f))
    }
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
            .then(if (metric.coreMetrics.isNotEmpty()) Modifier else Modifier.height(if (metric.size == OverviewCardSize.LARGE) 156.dp else 132.dp)),
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
            metric.value?.let {
                Text(
                    text = it,
                    style = if (metric.size == OverviewCardSize.LARGE) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            if (metric.coreMetrics.isNotEmpty()) {
                metric.coreMetrics.chunked(2).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMetrics.forEach { core ->
                            CoreMetricItem(core, Modifier.weight(1f))
                        }
                        if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreMetricItem(metric: CpuCoreMetric, modifier: Modifier = Modifier) {
    val usage = metric.usagePercent
    val animatedUsage by animateFloatAsState(
        targetValue = (usage ?: 0f).coerceIn(0f, 100f),
        animationSpec = tween(650),
        label = "cpuCoreUsage"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            CircularProgressIndicator(
                progress = { animatedUsage / 100f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 5.dp,
                color = coreUsageColor(animatedUsage),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = usage?.let { "${animatedUsage.toInt()}%" } ?: "-",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text("CPU${metric.index}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            metric.frequency?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun coreUsageColor(usage: Float): Color = when {
    usage < 50f -> Color(0xFF4CAF50)
    usage < 80f -> Color(0xFFFFA726)
    else -> Color(0xFFEF5350)
}
