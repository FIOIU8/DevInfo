/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main
import com.fioiu8.devinfo.ui.theme.isInDarkTheme
import com.fioiu8.devinfo.ui.DevInfoLoadingIndicator
import com.fioiu8.devinfo.ui.DevInfoPullToRefresh
import com.fioiu8.devinfo.ui.DevInfoSegmentHeader
import com.fioiu8.devinfo.ui.SegmentedGroup
import com.fioiu8.devinfo.ui.SegmentedRow
import com.fioiu8.devinfo.ui.DevInfoAlertDialog
import com.fioiu8.devinfo.ui.DevInfoCard
import com.fioiu8.devinfo.ui.DevInfoCardLevel
import com.fioiu8.devinfo.ui.DevInfoDialogAction
import com.fioiu8.devinfo.ui.motion.DevInfoAnimatedValue
import com.fioiu8.devinfo.ui.motion.DevInfoStaggeredEntrance
import com.fioiu8.devinfo.ui.descriptionResId
import com.fioiu8.devinfo.ui.displayNameResId
import com.fioiu8.devinfo.feature.main.R

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.core.model.CpuCoreMetric
import com.fioiu8.devinfo.core.model.CpuUsageSample
import com.fioiu8.devinfo.core.model.LiveHardwareSnapshot
import com.fioiu8.devinfo.core.model.OverviewSnapshot
import com.fioiu8.devinfo.core.model.InfoCategory
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import com.fioiu8.devinfo.core.privacy.displayValue
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
    /**
     * 标题行右侧的状态短标签（例如电量卡的“充电中”）。
     * 不使用 [supportingTextResId]：那会新增一行文本，把该卡撑得比同排卡片高。
     */
    val badgeResId: Int? = null,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceInfoOverviewPage(
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    isOverviewLoading: Boolean,
    snapshot: OverviewSnapshot,
    maskSensitive: Boolean = true,
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
            val metricColumns = when {
                maxWidth < 320.dp -> 1
                compactLayout -> 2
                else -> 3
            }

            DevInfoPullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (compactLayout) 12.dp else 20.dp,
                        top = if (compactLayout) 12.dp else 20.dp,
                        end = if (compactLayout) 12.dp else 20.dp,
                        bottom = if (compactLayout) 12.dp else 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") {
                        DeviceOverviewHeader(itemsState)
                    }
                    item(key = "identity") {
                        DeviceIdentitySummaryCard(itemsState)
                    }
                    stickyHeader(key = "realtime_header") {
                        DevInfoSegmentHeader(stringResource(R.string.overview_realtime_title))
                    }
                    item(key = "realtime_metrics") {
                        OverviewMetricsSection(
                            metrics = metrics,
                            columns = metricColumns,
                            onOpenDetails = onOpenDetails,
                        )
                    }
                    metrics.firstOrNull { it.id == OverviewMetricId.REALTIME_CPU }
                        ?.takeIf { it.history.isNotEmpty() }
                        ?.let { cpuMetric ->
                            item(key = "cpu_trend") {
                                CpuTrendCard(metric = cpuMetric)
                            }
                        }
                    stickyHeader(key = "details_header") {
                        DevInfoSegmentHeader(stringResource(R.string.title_device_details))
                    }
                    item(key = "categories") {
                        CategorySummaryGroup(
                            summaries = categorySummaries,
                            maskSensitive = maskSensitive,
                            onOpenDetails = onOpenDetails,
                        )
                    }
                    stickyHeader(key = "sensors_header") {
                        DevInfoSegmentHeader(stringResource(R.string.overview_hardware_title))
                    }
                    item(key = "sensors") {
                        HardwareSensorsCard(
                            snapshot = snapshot.hardware,
                            stackValues = metricColumns == 1,
                        )
                    }
                    stickyHeader(key = "security_header") {
                        DevInfoSegmentHeader(stringResource(R.string.overview_security_title))
                    }
                    item(key = "security") {
                        SecuritySummaryCard(snapshot)
                    }
                    item(key = "bottom_spacer") {
                        Spacer(Modifier.height(4.dp))
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
            style = MaterialTheme.typography.headlineMediumEmphasized,
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
    DevInfoCard(
        level = DevInfoCardLevel.EMPHASIS,
        modifier = Modifier.fillMaxWidth(),
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
                    style = MaterialTheme.typography.titleLargeEmphasized,
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

/** 指标卡按固定列数排布；CPU 等小卡两两成行，避免整页重建网格。 */
@Composable
private fun OverviewMetricsSection(
    metrics: List<OverviewMetric>,
    columns: Int,
    onOpenDetails: (InfoCategory) -> Unit,
) {
    if (metrics.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (columns == 1) {
            metrics.forEachIndexed { index, metric ->
                DevInfoStaggeredEntrance(index = index) {
                    OverviewMetricCard(
                        metric = metric,
                        wideLayout = true,
                        onClick = { onOpenDetails(metric.category) },
                    )
                }
            }
        } else {
            // CPU 频率摘要行数不固定，单独占满一行；否则同排卡片会被拉到同一高度，
            // 较短的内存/存储卡下方出现大片空白。
            val wideCpuMetric = metrics.firstOrNull {
                it.id == OverviewMetricId.REALTIME_CPU && it.showFrequencySummary
            }
            val gridMetrics = if (wideCpuMetric != null) metrics - wideCpuMetric else metrics
            if (wideCpuMetric != null) {
                DevInfoStaggeredEntrance(index = 0) {
                    OverviewMetricCard(
                        metric = wideCpuMetric,
                        wideLayout = true,
                        onClick = { onOpenDetails(wideCpuMetric.category) },
                    )
                }
            }
            val entranceOffset = if (wideCpuMetric != null) 1 else 0
            gridMetrics.chunked(columns).forEachIndexed { rowIndex, rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowMetrics.forEachIndexed { columnIndex, metric ->
                        Box(modifier = Modifier.weight(1f)) {
                            DevInfoStaggeredEntrance(
                                index = entranceOffset + rowIndex * columns + columnIndex
                            ) {
                                OverviewMetricCard(
                                    metric = metric,
                                    // 多列时卡片宽度有限，CPU 仪表放到数值下方，避免横向溢出。
                                    wideLayout = false,
                                    onClick = { onOpenDetails(metric.category) },
                                )
                            }
                        }
                    }
                    repeat(columns - rowMetrics.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** 分类入口：一个分组容器 + 每类一张行卡片，无分割线。 */
@Composable
private fun CategorySummaryGroup(
    summaries: List<CategorySummaryData>,
    maskSensitive: Boolean,
    onOpenDetails: (InfoCategory) -> Unit,
) {
    if (summaries.isEmpty()) return

    SegmentedGroup {
        summaries.forEachIndexed { index, summary ->
            val preview = summary.preview
            DevInfoStaggeredEntrance(index = index) {
                SegmentedRow(
                    title = stringResource(summary.category.displayNameResId()),
                    supporting = preview?.let {
                        val value = if (maskSensitive) {
                            displayValue(it.item.value, it.item.key, revealed = false)
                        } else {
                            it.item.value
                        }
                        stringResource(it.item.keyResId) + " · " + value
                    },
                    leadingIcon = categoryIcon(summary.category),
                    trailingValue = stringResource(R.string.items_count, summary.count),
                    showChevron = true,
                    onClick = { onOpenDetails(summary.category) },
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
                badgeResId = if (snapshot.batteryCharging) R.string.status_charging else null,
                progress = safeLevel / 100f
            )
        )
    }
}

private fun formatPercent(value: Float): String = "%.1f%%".format(Locale.US, value.coerceIn(0f, 100f))

/**
 * 标题行右侧的状态短标签。
 *
 * 放在标题行而不是卡片正文里，是为了让状态文案不占用独立一行：
 * 同排卡片高度取各自内容高度，多一行文本会把其中一张卡撑高、与邻居错位。
 */
@Composable
private fun MetricHeaderBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
    DevInfoCard(
        level = DevInfoCardLevel.CARD,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp),
        onClick = onClick,
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
                metric.badgeResId?.let { badgeResId ->
                    Spacer(Modifier.width(6.dp))
                    MetricHeaderBadge(text = stringResource(badgeResId))
                }
                if (metric.category == InfoCategory.SYSTEM && !hasCpuGauge) {
                    // 整卡已经可点击，这里只做视觉提示，避免卡内再嵌一个点击目标。
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (wideLayout && hasCpuGauge) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashboardGauge(
                        progress = metric.progress,
                        value = displayValue,
                        modifier = Modifier.size(96.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        supportingText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (hasFrequencySummary) {
                            CpuFrequencySummary(metric.coreMetrics)
                        }
                    }
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

    DevInfoCard(
        level = DevInfoCardLevel.CARD,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overview_realtime_cpu),
                        style = MaterialTheme.typography.titleMediumEmphasized,
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

/** 连续且频率相同的核心合并成一行，避免概览卡被 8 行频率撑高。 */
internal data class CpuFrequencyRange(
    val startIndex: Int,
    val endIndex: Int,
    val frequency: String,
)

internal fun coreFrequencyRanges(metrics: List<CpuCoreMetric>): List<CpuFrequencyRange> {
    val result = mutableListOf<CpuFrequencyRange>()
    metrics.filter { it.frequency != null }
        .sortedBy { it.index }
        .forEach { core ->
            val frequency = core.frequency ?: return@forEach
            val last = result.lastOrNull()
            if (last != null && last.frequency == frequency && last.endIndex == core.index - 1) {
                result[result.lastIndex] = last.copy(endIndex = core.index)
            } else {
                result.add(CpuFrequencyRange(core.index, core.index, frequency))
            }
        }
    return result
}

@Composable
private fun CpuFrequencySummary(metrics: List<CpuCoreMetric>) {
    val ranges = remember(metrics) { coreFrequencyRanges(metrics) }
    if (ranges.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ranges.forEach { range ->
            Text(
                text = if (range.startIndex == range.endIndex) {
                    "CPU${range.startIndex} ${range.frequency}"
                } else {
                    "CPU${range.startIndex}–${range.endIndex} ${range.frequency}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                DevInfoAnimatedValue(
                    value = it,
                    style = if (metric.size == OverviewCardSize.LARGE) {
                        MaterialTheme.typography.headlineSmallEmphasized
                    } else {
                        MaterialTheme.typography.titleLargeEmphasized
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val indicatorColor = when {
        progress == null -> MaterialTheme.colorScheme.outline
        progress >= 0.9f -> MaterialTheme.colorScheme.error
        progress >= 0.75f -> MaterialTheme.colorScheme.tertiary
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
            style = MaterialTheme.typography.titleMediumEmphasized,
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

    val corePalette = rememberCpuLinePalette()
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
                cpuLineColor(coreIndex, corePalette)
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

/** 静态信息小卡片：图标 + 标题 + 值，点击跳转到对应分类详情 */
@Composable
private fun StaticInfoCard(card: StaticInfoCardData, onClick: () -> Unit) {
    DevInfoCard(
        level = DevInfoCardLevel.CARD,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp),
        onClick = onClick,
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
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * CPU 核心曲线色板，全部取自当前配色方案。
 *
 * 之前这里写死 16 个十六进制色值（明暗两套），自定义主题色下曲线与界面撞色或失去对比度；
 * 现在按角色取色，明暗与种子色切换都会自动跟随。
 */
@Composable
private fun rememberCpuLinePalette(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        listOf(
            scheme.primary,
            scheme.tertiary,
            scheme.secondary,
            scheme.error,
            scheme.primaryFixed,
            scheme.tertiaryFixed,
            scheme.secondaryFixed,
            scheme.outline,
        )
    }
}

private fun cpuLineColor(index: Int, palette: List<Color>): Color = palette[index % palette.size]

@Composable
private fun CpuTrendChart(
    history: List<CpuUsageSample>,
    selectedCore: Int?,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 104.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val corePalette = rememberCpuLinePalette()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(left, y),
                            end = androidx.compose.ui.geometry.Offset(right, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    // 使用缓存路径绘制
                    cachedPaths.forEach { (coreIndex, path, areaPath) ->
                        if (path != null && areaPath != null) {
                            val lineColor = if (coreIndex < 0) primaryColor else cpuLineColor(coreIndex, corePalette)
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
                                val lineColor = if (coreIndex < 0) primaryColor else cpuLineColor(coreIndex, corePalette)
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
    val corePalette = rememberCpuLinePalette()
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
            Surface(
                onClick = { onCoreSelected(core) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(7.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            drawCircle(if (core < 0) primaryColor else cpuLineColor(core, corePalette))
                        }
                    }
                    Text(
                        text = if (core < 0) "CPU" else "C$core${currentByCore[core]?.let { " ${it.roundToInt()}%" } ?: ""}",
                        style = if (isSelected) {
                            MaterialTheme.typography.labelSmallEmphasized
                        } else {
                            MaterialTheme.typography.labelSmall
                        }
                    )
                }
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
        Text(title, style = MaterialTheme.typography.labelMediumEmphasized)
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
            style = MaterialTheme.typography.labelMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HardwareSensorsCard(
    snapshot: LiveHardwareSnapshot,
    stackValues: Boolean
) {
    DevInfoCard(
        level = DevInfoCardLevel.CARD,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Refresh, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.overview_hardware_title), style = MaterialTheme.typography.titleMediumEmphasized)
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
            Text(value, style = MaterialTheme.typography.labelMediumEmphasized)
        }
    }
}

@Composable
private fun SecuritySummaryCard(snapshot: OverviewSnapshot) {
    val context = LocalContext.current
    var showUsbDialog by remember { mutableStateOf(false) }
    DevInfoCard(
        level = DevInfoCardLevel.CARD,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.overview_security_title), style = MaterialTheme.typography.titleMediumEmphasized)
            }
            SecurityRow(Icons.Outlined.CheckCircle, stringResource(R.string.security_patch_updated, snapshot.securityPatch ?: stringResource(R.string.status_unknown)), MaterialTheme.colorScheme.primary)
            SecurityRow(Icons.Outlined.Lock, when (snapshot.lockScreenEnabled) {
                true -> stringResource(R.string.security_lock_enabled)
                false -> stringResource(R.string.security_lock_disabled)
                null -> stringResource(R.string.security_lock_unknown)
            }, MaterialTheme.colorScheme.primary)
            Surface(
                onClick = { if (snapshot.usbDebuggingEnabled == true) showUsbDialog = true },
                enabled = snapshot.usbDebuggingEnabled == true,
                color = if (snapshot.usbDebuggingEnabled == true) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                shape = MaterialTheme.shapes.extraSmall,
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
        DevInfoAlertDialog(
            title = stringResource(R.string.security_usb_dialog_title),
            onDismissRequest = { showUsbDialog = false },
            icon = Icons.Outlined.ErrorOutline,
            iconTint = MaterialTheme.colorScheme.error,
            confirm = DevInfoDialogAction(
                label = stringResource(R.string.security_open_developer_options),
                onClick = {
                    showUsbDialog = false
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    }
                },
            ),
            dismiss = DevInfoDialogAction(
                label = stringResource(R.string.cancel),
                onClick = { showUsbDialog = false },
            ),
        ) {
            Text(stringResource(R.string.security_usb_dialog_text))
        }
    }
}

@Composable
private fun SecurityRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(19.dp), tint = color)
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}
