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
import com.fioiu8.devinfo.ui.theme.isInDarkTheme
import com.fioiu8.devinfo.ui.DevInfoLoadingIndicator
import com.fioiu8.devinfo.ui.DevInfoPullToRefresh
import com.fioiu8.devinfo.ui.descriptionResId
import com.fioiu8.devinfo.ui.displayNameResId
import com.fioiu8.devinfo.ui.kit.liquid.LiquidGlassCard
import com.fioiu8.devinfo.feature.main.R

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.core.model.CpuCoreMetric
import com.fioiu8.devinfo.core.model.CpuUsageSample
import com.fioiu8.devinfo.core.model.LiveHardwareSnapshot
import com.fioiu8.devinfo.core.model.OverviewSnapshot
import com.fioiu8.devinfo.core.model.InfoCategory
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import java.util.Locale
import kotlin.math.roundToInt

private enum class OverviewCardSize(val span: Int) {
    SMALL(1),
    LARGE(2)
}

private enum class OverviewMetricId {
    REALTIME_CPU,
    GPU_FREQUENCY,
    GPU_USAGE,
    MEMORY,
    STORAGE,
    BATTERY
}

// OverviewSnapshot and CpuUsageSample moved to com.fioiu8.devinfo.core.model

@Immutable
private data class OverviewMetric(
    val id: OverviewMetricId,
    val title: String? = null,
    val titleResId: Int? = null,
    val value: String? = null,
    val category: InfoCategory,
    val icon: ImageVector,
    val size: OverviewCardSize,
    val supportingTextResId: Int? = null,
    val supportingTextSuffix: String? = null,
    val coreCount: Int? = null,
    val progress: Float? = null,
    val coreMetrics: List<CpuCoreMetric> = emptyList(),
    val showFrequencySummary: Boolean = false,
    val history: List<CpuUsageSample> = emptyList()
)

/** 从已采集信息项中挑选出的静态信息卡片数据 */
@Immutable
private data class StaticInfoCardData(
    val keyResId: Int,
    val value: String,
    val category: InfoCategory,
    val icon: ImageVector
)

@Immutable
private data class CategorySummaryData(
    val category: InfoCategory,
    val count: Int,
    val preview: ItemWithVisibility?
)

/** 概览页展示的静态信息项（按此顺序），值缺失时自动跳过 */
private val staticCardResIds = listOf(
    R.string.system_cpu_arch,
    R.string.system_lock_screen,
    R.string.device_model,
    R.string.system_android_version,
    R.string.system_security_patch,
    R.string.display_refresh_rate,
    R.string.system_sensor_count,
    R.string.network_type,
    R.string.battery_temperature,
    R.string.locale_timezone,
    R.string.system_boot_time,
    R.string.system_usb_debugging
)

private fun buildStaticInfoCards(items: List<ItemWithVisibility>): List<StaticInfoCardData> =
    staticCardResIds.mapNotNull { resId ->
        val item = items.firstOrNull { it.item.keyResId == resId }?.item ?: return@mapNotNull null
        StaticInfoCardData(resId, item.value, item.category, itemIconByKey(item.key))
    }

private fun buildCategorySummaries(items: List<ItemWithVisibility>): List<CategorySummaryData> =
    InfoCategory.entries.mapNotNull { category ->
        val categoryItems = items.filter { it.item.category == category }
        if (categoryItems.isEmpty()) {
            null
        } else {
            CategorySummaryData(
                category = category,
                count = categoryItems.size,
                preview = categoryItems.firstOrNull { it.item.value.isNotBlank() } ?: categoryItems.first()
            )
        }
    }

@Composable
fun DeviceInfoOverviewPage(
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    isOverviewLoading: Boolean,
    snapshot: OverviewSnapshot,
    onRefresh: suspend () -> Unit,
    onOpenDetails: (InfoCategory) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    val metrics = remember(snapshot) { buildOverviewMetrics(snapshot) }
    val categorySummaries = remember(itemsState) { buildCategorySummaries(itemsState) }

    if ((isLoading && itemsState.isEmpty()) || (isOverviewLoading && metrics.isEmpty())) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DevInfoLoadingIndicator()
        }
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactLayout = maxWidth < 600.dp
            val useSingleColumn = maxWidth < 320.dp

            DevInfoPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = if (useSingleColumn) {
                        GridCells.Fixed(1)
                    } else if (compactLayout) {
                        GridCells.Fixed(2)
                    } else {
                        GridCells.Adaptive(minSize = 148.dp)
                    },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (compactLayout) 12.dp else 20.dp,
                        top = if (compactLayout) 12.dp else 20.dp,
                        end = if (compactLayout) 12.dp else 20.dp,
                        bottom = if (compactLayout) 12.dp else 20.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 10.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compactLayout) 10.dp else 12.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        DeviceOverviewHeader(itemsState)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        DeviceIdentitySummaryCard(itemsState)
                    }
                    items(
                    items = metrics,
                        key = { metric -> metric.id },
                        span = { metric ->
                            GridItemSpan(
                                if (useSingleColumn) {
                                    maxLineSpan
                                } else {
                                    metric.size.span
                                }
                            )
                        }
                    ) { metric ->
                        OverviewMetricCard(
                            metric = metric,
                            wideLayout = !compactLayout,
                            onClick = { onOpenDetails(metric.category) }
                        )
                    }
                    metrics.firstOrNull { it.id == OverviewMetricId.REALTIME_CPU }
                        ?.takeIf { it.history.isNotEmpty() }
                        ?.let { cpuMetric ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                CpuTrendCard(metric = cpuMetric)
                            }
                        }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CategorySummaryList(
                            summaries = categorySummaries,
                            onOpenDetails = onOpenDetails,
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HardwareSensorsCard(
                            snapshot = snapshot.hardware,
                            stackValues = useSingleColumn
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SecuritySummaryCard(snapshot)
                    }
                }
            }
        }
    }
}

/** Device identity heading that follows the compact top app bar. */
@Composable
private fun DeviceOverviewHeader(items: List<ItemWithVisibility>) {
    val model = items.firstOrNull { it.item.key == "device_model" }?.item?.value
        ?: stringResource(R.string.status_unavailable)
    val subtitle = listOfNotNull(
        items.firstOrNull { it.item.key == "system_android_version" }?.item?.value?.let {
            "${stringResource(R.string.system_android_version)} $it"
        },
        items.firstOrNull { it.item.key == "system_cpu_arch" }?.item?.value?.let {
            "${stringResource(R.string.system_cpu_arch)} $it"
        },
    ).joinToString(" · ").ifBlank { stringResource(R.string.status_unavailable) }

    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = model,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Compact status summary inspired by the reference dashboard's health card. */
@Composable
private fun DeviceIdentitySummaryCard(items: List<ItemWithVisibility>) {
    val supportingText = stringResource(R.string.overview_device_summary_supporting, items.size)
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp, max = 132.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.overview_device_summary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun buildOverviewMetrics(
    snapshot: OverviewSnapshot
): List<OverviewMetric> = buildList {
    val availableCoreMetrics = snapshot.cpuCoreMetrics
    val currentUsage = snapshot.cpuUsage
        ?: availableCoreMetrics.mapNotNull { it.usagePercent }.average().toFloat().takeIf { !it.isNaN() }
    // 该卡必须无条件添加：若等 CPU 数据到达后再插入 index 0，
    // LazyGrid 会按 key 锚定滚动位置，把新卡排版到视口上方导致不可见
    add(
        OverviewMetric(
            id = OverviewMetricId.REALTIME_CPU,
            titleResId = R.string.overview_realtime_title,
            value = currentUsage?.let(::formatPercent),
            category = InfoCategory.SYSTEM,
            icon = itemIconByKey("system_cpu_arch"),
            size = OverviewCardSize.SMALL,
            supportingTextResId = when {
                currentUsage != null -> R.string.overview_realtime_cpu
                availableCoreMetrics.isNotEmpty() -> R.string.overview_cpu_frequency_monitor
                else -> R.string.overview_loading
            },
            supportingTextSuffix = snapshot.cpuFrequency.takeIf { currentUsage != null },
            coreCount = availableCoreMetrics.size.takeIf { it > 0 },
            progress = currentUsage?.div(100f),
            coreMetrics = availableCoreMetrics,
            showFrequencySummary = currentUsage == null && availableCoreMetrics.any { it.frequency != null },
            history = snapshot.cpuUsageHistory
        )
    )
    snapshot.gpuFrequency?.let {
        add(
            OverviewMetric(
                id = OverviewMetricId.GPU_FREQUENCY,
                titleResId = R.string.overview_gpu_frequency,
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
                id = OverviewMetricId.GPU_USAGE,
                titleResId = R.string.overview_gpu_usage,
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
                id = OverviewMetricId.MEMORY,
                titleResId = R.string.overview_memory,
                value = formatPercent(it),
                category = InfoCategory.STORAGE,
                icon = categoryIcon(InfoCategory.STORAGE),
                size = OverviewCardSize.SMALL,
                progress = it / 100f
            )
        )
    }
    snapshot.storagePercent?.let {
        add(
            OverviewMetric(
                id = OverviewMetricId.STORAGE,
                titleResId = R.string.overview_storage,
                value = formatPercent(it),
                category = InfoCategory.STORAGE,
                icon = categoryIcon(InfoCategory.STORAGE),
                size = OverviewCardSize.SMALL,
                progress = it / 100f
            )
        )
    }
    snapshot.batteryLevel?.let { level ->
        val safeLevel = level.coerceIn(0, 100)
        add(
            OverviewMetric(
                id = OverviewMetricId.BATTERY,
                titleResId = R.string.overview_battery,
                value = "$safeLevel%",
                category = InfoCategory.BATTERY,
                icon = categoryIcon(InfoCategory.BATTERY),
                size = OverviewCardSize.SMALL,
                supportingTextResId = if (snapshot.batteryCharging) R.string.status_charging else null,
                progress = safeLevel / 100f
            )
        )
    }
}

private fun formatPercent(value: Float): String = "%.1f%%".format(Locale.US, value.coerceIn(0f, 100f))

@Composable
private fun OverviewMetricCard(
    metric: OverviewMetric,
    wideLayout: Boolean,
    onClick: () -> Unit
) {
    val titleResId = metric.titleResId
    val title = if (metric.id == OverviewMetricId.REALTIME_CPU) {
        stringResource(R.string.overview_cpu_usage_short)
    } else if (titleResId == null) {
        metric.title.orEmpty()
    } else {
        stringResource(titleResId)
    }
    val supportingTextResId = metric.supportingTextResId
    val supportingText = if (metric.id == OverviewMetricId.REALTIME_CPU && metric.value != null) {
        listOfNotNull(
            metric.coreCount?.let { stringResource(R.string.overview_cpu_core_count, it) },
            metric.supportingTextSuffix,
        ).joinToString(" · ").ifBlank { null }
    } else if (supportingTextResId == null) {
        null
    } else {
        listOfNotNull(
            stringResource(supportingTextResId),
            metric.coreCount?.let { stringResource(R.string.overview_cpu_core_count, it) },
            metric.supportingTextSuffix
        ).joinToString(" · ")
    }
    val displayValue = metric.value ?: if (metric.id == OverviewMetricId.REALTIME_CPU) {
        stringResource(R.string.status_unavailable)
    } else {
        null
    }
    val hasCpuGauge = metric.id == OverviewMetricId.REALTIME_CPU
    val hasFrequencySummary = metric.showFrequencySummary
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasCpuGauge || hasFrequencySummary) {
                    Modifier.heightIn(min = 148.dp)
                } else {
                    Modifier.height(148.dp)
                }
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(if (hasCpuGauge) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (hasCpuGauge) 6.dp else 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metric.category == InfoCategory.SYSTEM && !hasCpuGauge) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.title_device_details)
                        )
                    }
                }
            }
            if (wideLayout && hasCpuGauge) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricValueBlock(
                        metric = metric,
                        displayValue = displayValue,
                        supportingText = supportingText,
                         modifier = Modifier.widthIn(min = 132.dp, max = 184.dp),
                         showValue = false,
                         showGauge = false,
                    )
                     DashboardGauge(
                         progress = metric.progress,
                         value = displayValue,
                         modifier = Modifier.size(104.dp),
                     )
                }
            } else {
                MetricValueBlock(
                    metric = metric,
                    displayValue = displayValue,
                    supportingText = supportingText,
                    showValue = !hasCpuGauge,
                    showGauge = !hasCpuGauge,
                )
                if (hasCpuGauge) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        DashboardGauge(
                            progress = metric.progress,
                            value = displayValue,
                            modifier = Modifier.size(if (wideLayout) 72.dp else 64.dp),
                        )
                    }
                }
                if (hasFrequencySummary) {
                    CpuFrequencySummary(metric.coreMetrics)
                }
            }
        }
    }
}

/** Historical CPU usage is intentionally separated from the current-value gauge. */
@Composable
private fun CpuTrendCard(metric: OverviewMetric) {
    var selectedCore by remember(metric.id) { mutableStateOf<Int?>(null) }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overview_realtime_cpu),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.overview_cpu_cores_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = metric.coreCount?.let { stringResource(R.string.overview_cpu_core_count, it) }
                        ?: stringResource(R.string.status_unavailable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CpuTrendChart(
                history = metric.history,
                selectedCore = selectedCore,
                chartHeight = 112.dp,
            )
            CpuTrendLegend(
                metric = metric,
                selectedCore = selectedCore,
                onCoreSelected = { core ->
                    selectedCore = core.takeUnless { it == selectedCore }
                },
            )
            CpuCoreSelectionSlot(
                metric = metric,
                selectedCore = selectedCore,
                reserveSpace = false,
            )
        }
    }
}

@Composable
private fun CpuFrequencySummary(metrics: List<CpuCoreMetric>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        metrics.forEach { metric ->
            metric.frequency?.let { frequency ->
                Text(
                    text = "CPU${metric.index} $frequency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MetricValueBlock(
    metric: OverviewMetric,
    displayValue: String?,
    supportingText: String?,
    modifier: Modifier = Modifier,
    showValue: Boolean = true,
    showGauge: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showGauge && metric.progress != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                DashboardGauge(
                    progress = metric.progress,
                    value = displayValue,
                    modifier = Modifier.size(if (metric.size == OverviewCardSize.LARGE) 112.dp else 76.dp),
                )
            }
        } else if (showValue) {
            displayValue?.let {
                Text(
                    text = it,
                    style = if (metric.size == OverviewCardSize.LARGE) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        metric.progress?.takeUnless { showGauge || metric.id == OverviewMetricId.REALTIME_CPU }?.let {
            LinearProgressIndicator(
                progress = { it.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/** Circular dashboard gauge used for the primary health metrics. */
@Composable
private fun DashboardGauge(
    progress: Float?,
    value: String?,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = when {
        progress == null -> MaterialTheme.colorScheme.outline
        progress >= 0.9f -> MaterialTheme.colorScheme.error
        progress >= 0.75f -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f
            val diameter = size.minDimension - strokeWidth
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                ),
            )
            progress?.coerceIn(0f, 1f)?.let {
                drawArc(
                    color = indicatorColor,
                    startAngle = 135f,
                    sweepAngle = 270f * it,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
        Text(
            text = value ?: stringResource(R.string.status_unavailable),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CpuCoreSelectionSlot(
    metric: OverviewMetric,
    selectedCore: Int?,
    reserveSpace: Boolean
) {
    if (selectedCore == null && !reserveSpace) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        selectedCore?.let { coreIndex ->
            val coreColor = if (coreIndex < 0) {
                MaterialTheme.colorScheme.primary
            } else {
                cpuLineColor(coreIndex, isInDarkTheme())
            }
            val selectedMetric = metric.coreMetrics.firstOrNull { it.index == coreIndex }
            CpuCoreSelectionDetails(
                coreIndex = coreIndex,
                metric = selectedMetric,
                usagePercent = selectedMetric?.usagePercent
                    ?: metric.history.lastOrNull()?.valuesByCore?.get(coreIndex),
                color = coreColor
            )
        }
    }
}

/** 首页分类入口：用一组紧凑列表行替代大量静态信息小卡片。 */
@Composable
private fun CategorySummaryList(
    summaries: List<CategorySummaryData>,
    onOpenDetails: (InfoCategory) -> Unit,
) {
    if (summaries.isEmpty()) return

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.title_device_details),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            summaries.forEachIndexed { index, summary ->
                val preview = summary.preview
                Card(
                    onClick = { onOpenDetails(summary.category) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = categoryIcon(summary.category),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(summary.category.displayNameResId()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(R.string.items_count, summary.count),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            preview?.let { item ->
                                Text(
                                    text = stringResource(item.item.keyResId) + " · " + item.item.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.title_device_details),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < summaries.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

/** 静态信息小卡片：图标 + 标题 + 值，点击跳转到对应分类详情 */
@Composable
private fun StaticInfoCard(card: StaticInfoCardData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(card.keyResId),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = card.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 明/暗两套核心曲线色板：暗色用更亮的 Material 色阶保证对比度
private val CPU_LINE_COLORS_LIGHT = listOf(
    0xFF2196F3, 0xFF009688, 0xFFFFA000, 0xFFEF5350,
    0xFFAB47BC, 0xFF7CB342, 0xFF00838F, 0xFFFF7043
)

private val CPU_LINE_COLORS_DARK = listOf(
    0xFF64B5F6, 0xFF4DB6AC, 0xFFFFD54F, 0xFFE57373,
    0xFFBA68C8, 0xFFAED581, 0xFF4DD0E1, 0xFFFF8A65
)

private fun cpuLineColor(index: Int, darkTheme: Boolean): Color =
    Color((if (darkTheme) CPU_LINE_COLORS_DARK else CPU_LINE_COLORS_LIGHT)[index % 8])

@Composable
private fun CpuTrendChart(
    history: List<CpuUsageSample>,
    selectedCore: Int?,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 104.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val darkTheme = isInDarkTheme()
    val coreIndexes = remember(history) {
        history.flatMap { it.valuesByCore.keys }.distinct().sorted()
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            // 波形是纯装饰绘制，具体数值由下方图例文本（C0 xx% 等）提供给读屏软件
            .clearAndSetSemantics { }
            .drawWithCache {
                // 缓存路径：仅在 size 或 history 变化时重建
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 8.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val plotWidth = (right - left).coerceAtLeast(1f)
                val plotHeight = (bottom - top).coerceAtLeast(1f)

                val cachedPaths = coreIndexes.map { coreIndex ->
                    val points = history.mapIndexedNotNull { pointIndex, sample ->
                        val x = if (history.size == 1) left + plotWidth / 2 else left + plotWidth * pointIndex / (history.size - 1)
                        val value = sample.valuesByCore[coreIndex]?.coerceIn(0f, 100f) ?: return@mapIndexedNotNull null
                        androidx.compose.ui.geometry.Offset(x, bottom - plotHeight * value / 100f)
                    }
                    val path = if (points.size > 1) {
                        androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEachIndexed { index, point ->
                                val previous = points[index]
                                val middleX = (previous.x + point.x) / 2f
                                cubicTo(middleX, previous.y, middleX, point.y, point.x, point.y)
                            }
                        }
                    } else null
                    val areaPath = if (points.size > 1) {
                        androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, bottom)
                            lineTo(points.first().x, points.first().y)
                            points.drop(1).forEachIndexed { index, point ->
                                val previous = points[index]
                                val middleX = (previous.x + point.x) / 2f
                                cubicTo(middleX, previous.y, middleX, point.y, point.x, point.y)
                            }
                            lineTo(points.last().x, bottom)
                            close()
                        }
                    } else null
                    Triple(coreIndex, path, areaPath)
                }

                onDrawWithContent {
                    // 绘制网格线
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = bottom - plotHeight * fraction
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.18f),
                            start = androidx.compose.ui.geometry.Offset(left, y),
                            end = androidx.compose.ui.geometry.Offset(right, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    // 使用缓存路径绘制
                    cachedPaths.forEach { (coreIndex, path, areaPath) ->
                        if (path != null && areaPath != null) {
                            val lineColor = if (coreIndex < 0) primaryColor else cpuLineColor(coreIndex, darkTheme)
                            val isMuted = selectedCore != null && selectedCore != coreIndex
                            drawPath(
                                path = areaPath,
                                color = lineColor.copy(
                                    alpha = when {
                                        isMuted -> 0.03f
                                        selectedCore == coreIndex -> 0.22f
                                        else -> 0.1f
                                    }
                                )
                            )
                            drawPath(
                                path = path,
                                color = lineColor.copy(alpha = if (isMuted) 0.3f else 1f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = if (selectedCore == coreIndex) 3.dp.toPx() else 2.25.dp.toPx(),
                                    pathEffect = if (coreIndex >= 0 && coreIndex % 2 == 1) {
                                        androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
                                    } else null
                                )
                            )
                        } else if (coreIndex == cachedPaths.firstOrNull()?.first) {
                            // points.size == 1 的情况
                            val points = history.mapIndexedNotNull { pointIndex, sample ->
                                val x = if (history.size == 1) left + plotWidth / 2 else left + plotWidth * pointIndex / (history.size - 1)
                                val value = sample.valuesByCore[coreIndex]?.coerceIn(0f, 100f) ?: return@mapIndexedNotNull null
                                androidx.compose.ui.geometry.Offset(x, bottom - plotHeight * value / 100f)
                            }
                            if (points.isNotEmpty()) {
                                val lineColor = if (coreIndex < 0) primaryColor else cpuLineColor(coreIndex, darkTheme)
                                drawCircle(
                                    color = lineColor.copy(alpha = if (selectedCore != null && selectedCore != coreIndex) 0.3f else 1f),
                                    radius = 3.5.dp.toPx(),
                                    center = points.single()
                                )
                            }
                        }
                    }
                }
            }
    ) {}
}

@Composable
private fun CpuTrendLegend(
    metric: OverviewMetric,
    selectedCore: Int?,
    onCoreSelected: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val darkTheme = isInDarkTheme()
    val currentByCore = remember(metric.coreMetrics) {
        metric.coreMetrics.associate { it.index to it.usagePercent }
    }
    val coreIndexes = remember(metric.history) {
        metric.history.flatMap { it.valuesByCore.keys }.distinct().sorted()
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        coreIndexes.forEach { core ->
            val isSelected = selectedCore == core
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .clickable { onCoreSelected(core) }
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(7.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawCircle(if (core < 0) primaryColor else cpuLineColor(core, darkTheme))
                    }
                }
                Text(
                    text = if (core < 0) "CPU" else "C$core${currentByCore[core]?.let { " ${it.roundToInt()}%" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CpuCoreSelectionDetails(
    coreIndex: Int,
    metric: CpuCoreMetric?,
    usagePercent: Float?,
    color: Color
) {
    val title = if (coreIndex < 0) "CPU" else "CPU$coreIndex"
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(50)),
        )
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = metric?.frequency ?: "--",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = usagePercent?.let(::formatPercent) ?: "--",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HardwareSensorsCard(
    snapshot: LiveHardwareSnapshot,
    stackValues: Boolean
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Refresh, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.overview_hardware_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            val sensorValues = listOf(
                Triple(Icons.Outlined.Refresh, stringResource(R.string.overview_motion), when {
                    !snapshot.motionAvailable -> stringResource(R.string.status_unavailable)
                    snapshot.moving -> stringResource(R.string.status_moving)
                    else -> stringResource(R.string.status_stationary)
                }),
                Triple(Icons.Outlined.Brightness6, stringResource(R.string.overview_brightness), snapshot.brightnessPercent?.let { "$it%" } ?: stringResource(R.string.status_unavailable)),
                Triple(Icons.Outlined.Storage, stringResource(R.string.overview_storage_read), snapshot.storageReadSpeedMbps?.let { "$it MB/s" } ?: stringResource(R.string.status_unavailable)),
                Triple(Icons.Outlined.Wifi, stringResource(R.string.overview_wifi_signal), snapshot.wifiRssiDbm?.let { "$it dBm" } ?: stringResource(R.string.status_unavailable))
            )
            if (stackValues) {
                sensorValues.forEach { (icon, label, value) ->
                    SensorValue(icon, label, value, Modifier.fillMaxWidth())
                }
            } else {
                sensorValues.chunked(2).forEach { rowValues ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowValues.forEach { (icon, label, value) ->
                            SensorValue(icon, label, value, Modifier.weight(1f))
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.overview_storage_average, snapshot.storageAverageReadSpeedMbps ?: 0).takeIf { snapshot.storageAverageReadSpeedMbps != null } ?: stringResource(R.string.overview_storage_average_unknown),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SensorValue(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SecuritySummaryCard(snapshot: OverviewSnapshot) {
    val context = LocalContext.current
    var showUsbDialog by remember { mutableStateOf(false) }
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.overview_security_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            SecurityRow(Icons.Outlined.CheckCircle, stringResource(R.string.security_patch_updated, snapshot.securityPatch ?: stringResource(R.string.status_unknown)), MaterialTheme.colorScheme.primary)
            SecurityRow(Icons.Outlined.Lock, when (snapshot.lockScreenEnabled) {
                true -> stringResource(R.string.security_lock_enabled)
                false -> stringResource(R.string.security_lock_disabled)
                null -> stringResource(R.string.security_lock_unknown)
            }, MaterialTheme.colorScheme.primary)
            Card(
                onClick = { if (snapshot.usbDebuggingEnabled == true) showUsbDialog = true },
                enabled = snapshot.usbDebuggingEnabled == true,
                colors = CardDefaults.cardColors(containerColor = if (snapshot.usbDebuggingEnabled == true) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(10.dp)
            ) {
                SecurityRow(Icons.Outlined.ErrorOutline, when (snapshot.usbDebuggingEnabled) {
                    true -> stringResource(R.string.security_usb_enabled)
                    false -> stringResource(R.string.security_usb_disabled)
                    null -> stringResource(R.string.security_usb_unknown)
                }, if (snapshot.usbDebuggingEnabled == true) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.padding(8.dp))
            }
        }
    }
    if (showUsbDialog) {
        AlertDialog(
            onDismissRequest = { showUsbDialog = false },
            icon = { Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.security_usb_dialog_title)) },
            text = { Text(stringResource(R.string.security_usb_dialog_text)) },
            dismissButton = { TextButton(onClick = { showUsbDialog = false }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = {
                TextButton(onClick = {
                    showUsbDialog = false
                    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                }) {
                    Icon(Icons.Outlined.OpenInBrowser, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.security_open_developer_options))
                }
            }
        )
    }
}

@Composable
private fun SecurityRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(19.dp), tint = color)
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}
