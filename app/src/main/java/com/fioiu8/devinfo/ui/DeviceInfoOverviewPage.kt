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

package com.fioiu8.devinfo.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.CpuCoreMetric
import com.fioiu8.devinfo.LiveHardwareSnapshot
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.model.ItemWithVisibility
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale
import kotlin.math.roundToInt

private enum class OverviewCardSize(val span: Int) {
    SMALL(1),
    LARGE(2)
}

private enum class OverviewMetricId {
    REALTIME_CPU,
    CPU_FREQUENCY,
    CPU_USAGE,
    GPU_FREQUENCY,
    GPU_USAGE,
    MEMORY,
    STORAGE,
    BATTERY
}

@Immutable
data class OverviewSnapshot(
    val cpuFrequency: String? = null,
    val gpuFrequency: String? = null,
    val cpuUsage: Float? = null,
    val gpuUsage: Float? = null,
    val cpuCoreMetrics: List<CpuCoreMetric> = emptyList(),
    val storagePercent: Float? = null,
    val memoryPercent: Float? = null,
    val batteryLevel: Int? = null,
    val batteryCharging: Boolean = false,
    val cpuUsageHistory: List<CpuUsageSample> = emptyList(),
    val securityPatch: String? = null,
    val lockScreenEnabled: Boolean? = null,
    val usbDebuggingEnabled: Boolean? = null,
    val hardware: LiveHardwareSnapshot = LiveHardwareSnapshot()
)

@Immutable
data class CpuUsageSample(
    val timestampMillis: Long,
    val valuesByCore: Map<Int, Float>
)

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
        StaticInfoCardData(resId, item.value, item.category, item.icon)
    }

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
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixDeviceInfoOverviewPage(
            itemsState = itemsState,
            isLoading = isLoading,
            isOverviewLoading = isOverviewLoading,
            snapshot = snapshot,
            onRefresh = onRefresh,
            onOpenDetails = onOpenDetails,
        )
        return
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale
    val compactLayout = screenWidthDp < 600
    val useSingleColumn = screenWidthDp < 360 || fontScale >= 1.3f
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    val metrics by remember(snapshot) {
        derivedStateOf { buildOverviewMetrics(snapshot) }
    }
    val staticCards by remember(itemsState) {
        derivedStateOf { buildStaticInfoCards(itemsState) }
    }

    if ((isLoading && itemsState.isEmpty()) || (isOverviewLoading && metrics.isEmpty())) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DevInfoLoadingIndicator()
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
                    bottom = (if (compactLayout) 12.dp else 20.dp) +
                        LocalFloatingNavigationContentPadding.current,
                ),
                horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactLayout) 10.dp else 12.dp)
            ) {
                items(
                    items = metrics,
                    key = { metric -> metric.id },
                    span = { metric ->
                        GridItemSpan(
                            if (useSingleColumn || (!compactLayout && metric.id == OverviewMetricId.REALTIME_CPU)) {
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
                items(
                    items = staticCards,
                    key = { card -> card.keyResId },
                    span = { GridItemSpan(1) }
                ) { card ->
                    StaticInfoCard(
                        card = card,
                        onClick = { onOpenDetails(card.category) }
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

@Composable
private fun MiuixDeviceInfoOverviewPage(
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    isOverviewLoading: Boolean,
    snapshot: OverviewSnapshot,
    onRefresh: suspend () -> Unit,
    onOpenDetails: (InfoCategory) -> Unit,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val metrics by remember(snapshot) {
        derivedStateOf { buildOverviewMetrics(snapshot) }
    }
    val staticCards by remember(itemsState) {
        derivedStateOf { buildStaticInfoCards(itemsState) }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    if ((isLoading && itemsState.isEmpty()) || (isOverviewLoading && metrics.isEmpty())) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
        }
        return
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale
    val compactLayout = screenWidthDp < 600
    val useSingleColumn = screenWidthDp < 360 || fontScale >= 1.3f

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
            bottom = (if (compactLayout) 12.dp else 20.dp) +
                LocalFloatingNavigationContentPadding.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactLayout) 10.dp else 12.dp)
    ) {
        items(
            items = metrics,
            key = { metric -> metric.id },
            span = { metric ->
                GridItemSpan(
                    if (useSingleColumn || (!compactLayout && metric.id == OverviewMetricId.REALTIME_CPU)) {
                        maxLineSpan
                    } else {
                        metric.size.span
                    }
                )
            }
        ) { metric ->
            MiuixMetricCard(
                metric = metric,
                onClick = { onOpenDetails(metric.category) }
            )
        }
        items(
            items = staticCards,
            key = { card -> card.keyResId },
            span = { GridItemSpan(1) }
        ) { card ->
            MiuixStaticInfoCard(
                card = card,
                onClick = { onOpenDetails(card.category) }
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            MiuixHardwareSensorsCard(
                snapshot = snapshot.hardware,
                stackValues = useSingleColumn
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            MiuixSecuritySummaryCard(snapshot)
        }
    }
}

@Composable
private fun MiuixMetricCard(
    metric: OverviewMetric,
    onClick: () -> Unit
) {
    val titleResId = metric.titleResId
    val title = if (titleResId == null) metric.title.orEmpty() else stringResource(titleResId)
    val supportingTextResId = metric.supportingTextResId
    val supportingText = if (supportingTextResId == null) {
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

    MiuixCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (metric.size == OverviewCardSize.LARGE) 144.dp else 116.dp)
            .clickable(onClick = onClick),
        insideMargin = PaddingValues(16.dp),
        cornerRadius = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiuixIcon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                MiuixText(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            displayValue?.let {
                MiuixText(
                    text = it,
                    style = if (metric.size == OverviewCardSize.LARGE) {
                        MiuixTheme.textStyles.headline1
                    } else {
                        MiuixTheme.textStyles.subtitle
                    },
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            supportingText?.let {
                MiuixText(
                    text = it,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            metric.progress?.let {
                MiuixLinearProgressIndicator(
                    progress = it.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = MiuixTheme.colorScheme.primary,
                        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
            if (metric.coreMetrics.isNotEmpty() && metric.history.isEmpty()) {
                metric.coreMetrics.forEach { coreMetric ->
                    MiuixCoreMetricRow(coreMetric)
                }
            }
        }
    }
}

@Composable
private fun MiuixCoreMetricRow(metric: CpuCoreMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiuixText(
            text = "CPU${metric.index}",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
        Spacer(Modifier.weight(1f))
        MiuixText(
            text = metric.usagePercent?.let { "%.0f%%".format(it) } ?: metric.frequency ?: "--",
            style = MiuixTheme.textStyles.main,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurface
        )
        metric.usagePercent?.let {
            metric.frequency?.let { frequency ->
                MiuixText(
                    text = frequency,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
        }
    }
}

@Composable
private fun MiuixStaticInfoCard(card: StaticInfoCardData, onClick: () -> Unit) {
    MiuixCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clickable(onClick = onClick),
        insideMargin = PaddingValues(14.dp),
        cornerRadius = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MiuixIcon(
                imageVector = card.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MiuixTheme.colorScheme.primary
            )
            MiuixText(
                text = stringResource(card.keyResId),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MiuixText(
                text = card.value,
                style = MiuixTheme.textStyles.subtitle,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiuixHardwareSensorsCard(
    snapshot: LiveHardwareSnapshot,
    stackValues: Boolean
) {
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        cornerRadius = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiuixIcon(
                    Icons.Outlined.Refresh,
                    null,
                    Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                MiuixText(
                    text = stringResource(R.string.overview_hardware_title),
                    style = MiuixTheme.textStyles.subtitle,
                    fontWeight = FontWeight.Bold
                )
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
                    MiuixSensorValue(icon, label, value, Modifier.fillMaxWidth())
                }
            } else {
                sensorValues.chunked(2).forEach { rowValues ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowValues.forEach { (icon, label, value) ->
                            MiuixSensorValue(icon, label, value, Modifier.weight(1f))
                        }
                    }
                }
            }
            MiuixText(
                text = stringResource(R.string.overview_storage_average, snapshot.storageAverageReadSpeedMbps ?: 0).takeIf { snapshot.storageAverageReadSpeedMbps != null } ?: stringResource(R.string.overview_storage_average_unknown),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
        }
    }
}

@Composable
private fun MiuixSensorValue(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        MiuixIcon(icon, null, Modifier.size(19.dp), tint = MiuixTheme.colorScheme.primary)
        Column {
            MiuixText(label, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MiuixText(value, style = MiuixTheme.textStyles.main, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MiuixSecuritySummaryCard(snapshot: OverviewSnapshot) {
    val context = LocalContext.current
    var showUsbDialog by remember { mutableStateOf(false) }
    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        cornerRadius = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiuixIcon(Icons.Outlined.CheckCircle, null, Modifier.size(24.dp), tint = MiuixTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                MiuixText(stringResource(R.string.overview_security_title), style = MiuixTheme.textStyles.subtitle, fontWeight = FontWeight.Bold)
            }
            MiuixSecurityRow(Icons.Outlined.CheckCircle, stringResource(R.string.security_patch_updated, snapshot.securityPatch ?: stringResource(R.string.status_unknown)), MiuixTheme.colorScheme.primary)
            MiuixSecurityRow(Icons.Outlined.Lock, when (snapshot.lockScreenEnabled) {
                true -> stringResource(R.string.security_lock_enabled)
                false -> stringResource(R.string.security_lock_disabled)
                null -> stringResource(R.string.security_lock_unknown)
            }, MiuixTheme.colorScheme.primary)
            MiuixCard(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(8.dp),
                cornerRadius = 10.dp,
                colors = MiuixCardDefaults.defaultColors(
                    color = if (snapshot.usbDebuggingEnabled == true) MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f) else MiuixTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                MiuixSecurityRow(Icons.Outlined.ErrorOutline, when (snapshot.usbDebuggingEnabled) {
                    true -> stringResource(R.string.security_usb_enabled)
                    false -> stringResource(R.string.security_usb_disabled)
                    null -> stringResource(R.string.security_usb_unknown)
                }, if (snapshot.usbDebuggingEnabled == true) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceSecondary)
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
private fun MiuixSecurityRow(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiuixIcon(icon, null, Modifier.size(19.dp), tint = color)
        MiuixText(text, style = MiuixTheme.textStyles.body2, color = color)
    }
}

private fun buildOverviewMetrics(
    snapshot: OverviewSnapshot
): List<OverviewMetric> = buildList {
    val availableCoreMetrics = snapshot.cpuCoreMetrics
    if (availableCoreMetrics.isNotEmpty() || snapshot.cpuUsageHistory.isNotEmpty() || snapshot.cpuUsage != null) {
        val currentUsage = snapshot.cpuUsage
            ?: availableCoreMetrics.mapNotNull { it.usagePercent }.average().toFloat().takeIf { !it.isNaN() }
        add(
            OverviewMetric(
                id = OverviewMetricId.REALTIME_CPU,
                titleResId = R.string.overview_realtime_title,
                value = currentUsage?.let(::formatPercent),
                category = InfoCategory.SYSTEM,
                icon = itemIconByResId(R.string.system_cpu_arch),
                size = OverviewCardSize.LARGE,
                supportingTextResId = if (currentUsage == null) {
                    R.string.overview_cpu_frequency_monitor
                } else {
                    R.string.overview_realtime_cpu
                },
                supportingTextSuffix = snapshot.cpuFrequency.takeIf { currentUsage != null },
                coreCount = availableCoreMetrics.size.takeIf { it > 0 },
                progress = currentUsage?.div(100f),
                coreMetrics = availableCoreMetrics,
                showFrequencySummary = currentUsage == null && availableCoreMetrics.any { it.frequency != null },
                history = snapshot.cpuUsageHistory
            )
        )
    } else {
        snapshot.cpuFrequency?.let {
            add(
                OverviewMetric(
                    id = OverviewMetricId.CPU_FREQUENCY,
                    title = "CPU",
                    value = it,
                    category = InfoCategory.SYSTEM,
                    icon = itemIconByResId(R.string.system_cpu_arch),
                    size = OverviewCardSize.LARGE,
                    supportingTextResId = R.string.overview_cpu_frequency
                )
            )
        }
        snapshot.cpuUsage?.let {
            add(
                OverviewMetric(
                    id = OverviewMetricId.CPU_USAGE,
                    titleResId = R.string.overview_cpu_usage,
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
    val title = if (titleResId == null) metric.title.orEmpty() else stringResource(titleResId)
    val supportingTextResId = metric.supportingTextResId
    val supportingText = if (supportingTextResId == null) {
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
    var selectedCore by remember(metric.id) { mutableStateOf<Int?>(null) }
    val hasCoreControls = metric.history.isNotEmpty()
    val hasFrequencySummary = metric.showFrequencySummary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasCoreControls || hasFrequencySummary) {
                    Modifier
                } else {
                    Modifier.height(if (metric.size == OverviewCardSize.LARGE) 148.dp else 124.dp)
                }
            )
            .then(if (hasCoreControls) Modifier else Modifier.clickable(onClick = onClick)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(if (hasCoreControls) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (hasCoreControls) 6.dp else 8.dp)
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
                if (hasCoreControls && !wideLayout) {
                    displayValue?.let { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
                if (metric.category == InfoCategory.SYSTEM) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.title_device_details)
                        )
                    }
                }
            }
            if (wideLayout && hasCoreControls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricValueBlock(
                        metric = metric,
                        displayValue = displayValue,
                        supportingText = supportingText,
                        modifier = Modifier.widthIn(min = 132.dp, max = 184.dp)
                    )
                    CpuTrendChart(
                        history = metric.history,
                        selectedCore = selectedCore,
                        modifier = Modifier.weight(1f),
                        chartHeight = 80.dp
                    )
                    Column(
                        modifier = Modifier.widthIn(min = 220.dp, max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CpuTrendLegend(
                            metric = metric,
                            selectedCore = selectedCore,
                            onCoreSelected = { core ->
                                selectedCore = core.takeUnless { it == selectedCore }
                            }
                        )
                        CpuCoreSelectionSlot(
                            metric = metric,
                            selectedCore = selectedCore,
                            reserveSpace = true
                        )
                    }
                }
            } else {
                MetricValueBlock(
                    metric = metric,
                    displayValue = displayValue,
                    supportingText = supportingText,
                    showValue = !hasCoreControls
                )
                if (hasCoreControls) {
                    CpuTrendChart(
                        history = metric.history,
                        selectedCore = selectedCore,
                        chartHeight = 80.dp
                    )
                    CpuTrendLegend(
                        metric = metric,
                        selectedCore = selectedCore,
                        onCoreSelected = { core ->
                            selectedCore = core.takeUnless { it == selectedCore }
                        }
                    )
                    CpuCoreSelectionSlot(
                        metric = metric,
                        selectedCore = selectedCore,
                        reserveSpace = false
                    )
                }
                if (hasFrequencySummary) {
                    CpuFrequencySummary(metric.coreMetrics)
                }
            }
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
    showValue: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showValue) {
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
            val coreColor = if (coreIndex < 0) MaterialTheme.colorScheme.primary else cpuLineColor(coreIndex)
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

private fun cpuLineColor(index: Int): Color = when (index % 8) {
    0 -> Color(0xFF2196F3)
    1 -> Color(0xFF009688)
    2 -> Color(0xFFFFA000)
    3 -> Color(0xFFEF5350)
    4 -> Color(0xFFAB47BC)
    5 -> Color(0xFF7CB342)
    6 -> Color(0xFF00838F)
    else -> Color(0xFFFF7043)
}

@Composable
private fun CpuTrendChart(
    history: List<CpuUsageSample>,
    selectedCore: Int?,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 104.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val coreIndexes = remember(history) {
        history.flatMap { it.valuesByCore.keys }.distinct().sorted()
    }
    Canvas(modifier = modifier.fillMaxWidth().height(chartHeight)) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val plotWidth = (right - left).coerceAtLeast(1f)
        val plotHeight = (bottom - top).coerceAtLeast(1f)
        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = bottom - plotHeight * fraction
            drawLine(
                color = Color.Gray.copy(alpha = 0.18f),
                start = androidx.compose.ui.geometry.Offset(left, y),
                end = androidx.compose.ui.geometry.Offset(right, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        coreIndexes.forEach { coreIndex ->
            val points = history.mapIndexedNotNull { pointIndex, sample ->
                val x = if (history.size == 1) left + plotWidth / 2 else left + plotWidth * pointIndex / (history.size - 1)
                val value = sample.valuesByCore[coreIndex]?.coerceIn(0f, 100f) ?: return@mapIndexedNotNull null
                androidx.compose.ui.geometry.Offset(x, bottom - plotHeight * value / 100f)
            }
            if (points.isNotEmpty()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEachIndexed { index, point ->
                        val previous = points[index]
                        val middleX = (previous.x + point.x) / 2f
                        cubicTo(middleX, previous.y, middleX, point.y, point.x, point.y)
                    }
                }
                val areaPath = androidx.compose.ui.graphics.Path().apply {
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
                val lineColor = if (coreIndex < 0) {
                    primaryColor
                } else {
                    cpuLineColor(coreIndex)
                }
                val isMuted = selectedCore != null && selectedCore != coreIndex
                if (points.size == 1) {
                    drawCircle(
                        color = lineColor.copy(alpha = if (isMuted) 0.3f else 1f),
                        radius = 3.5.dp.toPx(),
                        center = points.single()
                    )
                } else {
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
                            } else {
                                null
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CpuTrendLegend(
    metric: OverviewMetric,
    selectedCore: Int?,
    onCoreSelected: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
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
                        drawCircle(if (core < 0) primaryColor else cpuLineColor(core))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
