/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main

import android.content.ClipData
import android.hardware.camera2.CameraCharacteristics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.core.model.CameraHardwareInfo
import com.fioiu8.devinfo.core.model.DeviceInfoItem
import com.fioiu8.devinfo.core.model.HardwareDetailPage
import com.fioiu8.devinfo.core.model.HardwareDetailsSnapshot
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import com.fioiu8.devinfo.core.model.MotionSensorSample
import com.fioiu8.devinfo.core.model.MotionSensorSnapshot
import com.fioiu8.devinfo.core.model.MotionSensorType
import com.fioiu8.devinfo.core.model.OverviewSnapshot
import com.fioiu8.devinfo.core.model.SensorHardwareInfo
import com.fioiu8.devinfo.core.model.hardwareDetailPage
import com.fioiu8.devinfo.ui.DevInfoPullToRefresh
import com.fioiu8.devinfo.ui.kit.liquid.LiquidGlassCard
import com.fioiu8.devinfo.ui.rememberDevInfoMessageHandler
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue

private val hardwarePages = HardwareDetailPage.entries

@Composable
fun HardwareDetailsPage(
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    overviewSnapshot: OverviewSnapshot,
    hardwareDetails: HardwareDetailsSnapshot,
    motionSensors: MotionSensorSnapshot,
    initialPage: HardwareDetailPage,
    onRefresh: suspend () -> Unit,
    onSensorPageVisibilityChanged: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = hardwarePages.indexOf(initialPage).coerceAtLeast(0),
        pageCount = { hardwarePages.size }
    )
    val itemsByPage = remember(itemsState) {
        itemsState.groupBy { it.item.hardwareDetailPage() }
    }
    val pagerScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onSensorPageVisibilityChanged(hardwarePages[pagerState.currentPage] == HardwareDetailPage.SENSORS)
    }
    DisposableEffect(Unit) {
        onDispose { onSensorPageVisibilityChanged(false) }
    }

    Column(Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
            divider = {}
        ) {
            hardwarePages.forEachIndexed { index, page ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = stringResource(page.titleRes()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = hardwarePages[pageIndex]
            HardwareDetailPageContent(
                page = page,
                items = itemsByPage[page].orEmpty(),
                isLoading = isLoading,
                overviewSnapshot = overviewSnapshot,
                hardwareDetails = hardwareDetails,
                motionSensors = motionSensors,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun HardwareDetailPageContent(
    page: HardwareDetailPage,
    items: List<ItemWithVisibility>,
    isLoading: Boolean,
    overviewSnapshot: OverviewSnapshot,
    hardwareDetails: HardwareDetailsSnapshot,
    motionSensors: MotionSensorSnapshot,
    onRefresh: suspend () -> Unit
) {
    val clipboard = LocalClipboard.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val showMessage = rememberDevInfoMessageHandler()
    val itemList = remember(items) { items.map(ItemWithVisibility::item) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    DevInfoPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        modifier = Modifier.fillMaxSize()
    ) {
      LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 24.dp)
      ) {
        when (page) {
            HardwareDetailPage.DEVICE -> {
                item {
                    val model = itemList.firstValue("device_model")
                    val maker = itemList.firstValue("device_manufacturer")
                    DetailHero(
                        icon = Icons.Outlined.Memory,
                        title = model ?: stringResource(R.string.title_page_device),
                        value = maker ?: stringResource(R.string.status_unavailable),
                        supporting = stringResource(R.string.hardware_device_summary)
                    )
                }
                addInfoRows(this, itemList, resources, clipboard, scope, showMessage)
            }

            HardwareDetailPage.DISPLAY -> {
                item {
                    DetailHero(
                        icon = Icons.Outlined.Memory,
                        title = stringResource(R.string.title_page_display),
                        value = itemList.firstValue("display_size") ?: stringResource(R.string.status_unavailable),
                        supporting = itemList.firstValue("display_refresh_rate")
                            ?: stringResource(R.string.hardware_display_summary)
                    )
                }
                addInfoRows(this, itemList, resources, clipboard, scope, showMessage)
            }

            HardwareDetailPage.SOC_STORAGE -> {
                item {
                    DetailHero(
                        icon = Icons.Outlined.Memory,
                        title = itemList.firstValue("device_soc_model") ?: stringResource(R.string.title_page_soc),
                        value = overviewSnapshot.cpuFrequency ?: stringResource(R.string.status_unavailable),
                        supporting = stringResource(R.string.hardware_cpu_frequency)
                    )
                }
                item {
                    RuntimeMetricSection(
                        title = stringResource(R.string.hardware_cpu_usage),
                        percent = overviewSnapshot.cpuUsage
                    )
                }
                overviewSnapshot.gpuUsage?.let { percent ->
                    item {
                        RuntimeMetricSection(
                            title = stringResource(R.string.hardware_gpu_usage),
                            percent = percent
                        )
                    }
                }
                addInfoRows(this, itemList, resources, clipboard, scope, showMessage)
            }

            HardwareDetailPage.BATTERY_NETWORK -> {
                item {
                    val battery = itemList.firstValue("battery_level_label")
                        ?: stringResource(R.string.status_unavailable)
                    DetailHero(
                        icon = Icons.Outlined.Memory,
                        title = stringResource(R.string.title_page_battery_network),
                        value = battery,
                        supporting = itemList.firstValue("network_type")
                            ?: stringResource(R.string.hardware_network_summary)
                    )
                }
                overviewSnapshot.batteryLevel?.let { level ->
                    item { RuntimeMetricSection(stringResource(R.string.title_page_battery), level.toFloat()) }
                }
                addInfoRows(this, itemList, resources, clipboard, scope, showMessage)
            }

            HardwareDetailPage.CAMERA -> {
                item {
                    DetailHero(
                        icon = Icons.Outlined.CameraAlt,
                        title = stringResource(R.string.title_page_camera),
                        value = stringResource(
                            R.string.hardware_camera_count,
                            itemList.firstValue("network_camera_count")
                                ?: if (hardwareDetails.cameraAccessAvailable) {
                                    hardwareDetails.cameras.size.toString()
                                } else {
                                    stringResource(R.string.status_unavailable)
                                }
                        ),
                        supporting = if (hardwareDetails.cameraAccessAvailable) {
                            stringResource(R.string.hardware_camera_summary)
                        } else {
                            stringResource(R.string.hardware_camera_access_unavailable)
                        }
                    )
                }
                items(hardwareDetails.cameras, key = CameraHardwareInfo::id) { camera ->
                    CameraInfoCard(camera)
                }
                if (hardwareDetails.cameras.isEmpty() && hardwareDetails.cameraAccessAvailable) {
                    item { EmptyState(stringResource(R.string.hardware_no_cameras)) }
                }
                addInfoRows(this, itemList, resources, clipboard, scope, showMessage)
            }

            HardwareDetailPage.SENSORS -> {
                item {
                    DetailHero(
                        icon = Icons.Outlined.Sensors,
                        title = stringResource(R.string.title_page_sensors),
                        value = if (hardwareDetails.sensorAccessAvailable) {
                            stringResource(R.string.hardware_sensor_count, hardwareDetails.sensors.size)
                        } else {
                            stringResource(R.string.status_unavailable)
                        },
                        supporting = if (hardwareDetails.sensorAccessAvailable) {
                            stringResource(R.string.hardware_sensor_summary)
                        } else {
                            stringResource(R.string.hardware_sensor_access_unavailable)
                        }
                    )
                }
                item {
                    LiveMotionCard(motionSensors)
                }
                if (hardwareDetails.sensors.isEmpty()) {
                    item {
                        EmptyState(
                            stringResource(
                                if (hardwareDetails.sensorAccessAvailable) {
                                    R.string.hardware_no_sensors
                                } else {
                                    R.string.hardware_sensor_access_unavailable
                                }
                            )
                        )
                    }
                } else {
                    items(hardwareDetails.sensors, key = { "${it.type}:${it.name}:${it.vendor}" }) { sensor ->
                        SensorInfoCard(sensor)
                    }
                }
                addInfoRows(this, itemList, resources, clipboard, scope, showMessage)
            }

            HardwareDetailPage.MORE -> {
                item {
                    DetailHero(
                        icon = Icons.Outlined.Memory,
                        title = stringResource(R.string.title_page_more),
                        value = stringResource(R.string.items_count, itemList.size),
                        supporting = stringResource(R.string.hardware_more_summary)
                    )
                }
                addInfoRows(this, itemList, resources, clipboard, scope, showMessage)
            }
        }
          if (isLoading && itemList.isEmpty()) {
              item { EmptyState(stringResource(R.string.overview_loading)) }
          }
          item { Spacer(Modifier.height(4.dp)) }
      }
    }
}

private fun addInfoRows(
    scope: androidx.compose.foundation.lazy.LazyListScope,
    items: List<DeviceInfoItem>,
    resources: android.content.res.Resources,
    clipboard: androidx.compose.ui.platform.Clipboard,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    showMessage: (String) -> Unit
) {
    if (items.isEmpty()) return
    scope.item {
        Text(
            text = resources.getString(R.string.hardware_details_section),
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
    scope.items(items, key = DeviceInfoItem::key) { item ->
        InfoValueRow(
            label = resources.getString(item.keyResId),
            value = item.value,
            onCopy = {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, item.value)))
                }
                showMessage(resources.getString(R.string.copied_to_clipboard, resources.getString(item.keyResId)))
            }
        )
    }
}

@Composable
private fun DetailHero(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    supporting: String
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoValueRow(label: String, value: String, onCopy: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopy)
                .padding(start = 14.dp, top = 11.dp, end = 6.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.hardware_copy_value))
            }
        }
    }
}

@Composable
private fun RuntimeMetricSection(title: String, percent: Float?) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = percent?.let { "${it.toInt().coerceIn(0, 100)}%" }
                        ?: stringResource(R.string.status_unavailable),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            percent?.let {
                LinearProgressIndicator(
                    progress = { it.coerceIn(0f, 100f) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CameraInfoCard(camera: CameraHardwareInfo) {
    val characteristics = camera.characteristicsAvailable
    val unavailable = stringResource(R.string.status_unavailable)
    val facing = when (camera.lensFacing) {
        CameraCharacteristics.LENS_FACING_FRONT -> stringResource(R.string.hardware_camera_front)
        CameraCharacteristics.LENS_FACING_BACK -> stringResource(R.string.hardware_camera_back)
        CameraCharacteristics.LENS_FACING_EXTERNAL -> stringResource(R.string.hardware_camera_external)
        else -> unavailable
    }
    val focalLength = camera.focalLengthsMm.takeIf { it.isNotEmpty() }
        ?.joinToString { formatSensorValue(it) + " mm" } ?: unavailable
    val physicalSize = camera.physicalWidthMm?.let { width ->
        camera.physicalHeightMm?.let { height ->
            "${formatSensorValue(width)} × ${formatSensorValue(height)} mm"
        }
    } ?: unavailable
    val pixelSize = if (camera.pixelWidth != null && camera.pixelHeight != null) {
        "${camera.pixelWidth} × ${camera.pixelHeight}"
    } else {
        unavailable
    }
    HardwareDataCard(
        icon = Icons.Outlined.CameraAlt,
        title = stringResource(R.string.hardware_camera_id, camera.id),
        rows = listOf(
            stringResource(R.string.hardware_camera_facing) to facing,
            stringResource(R.string.hardware_camera_focal_length) to focalLength,
            stringResource(R.string.hardware_camera_sensor_size) to physicalSize,
            stringResource(R.string.hardware_camera_pixel_array) to pixelSize,
            stringResource(R.string.hardware_camera_orientation) to
                (camera.sensorOrientationDegrees?.let { "$it°" } ?: unavailable)
        ),
        supporting = if (characteristics) null else stringResource(R.string.hardware_camera_characteristics_unavailable)
    )
}

@Composable
private fun SensorInfoCard(sensor: SensorHardwareInfo) {
    val unavailable = stringResource(R.string.status_unavailable)
    HardwareDataCard(
        icon = Icons.Outlined.Sensors,
        title = sensor.name.ifBlank { sensor.typeName },
        rows = listOf(
            stringResource(R.string.hardware_sensor_type) to sensor.typeName,
            stringResource(R.string.hardware_sensor_vendor) to sensor.vendor.ifBlank { unavailable },
            stringResource(R.string.hardware_sensor_range) to (sensor.maxRange?.let(::formatSensorValue) ?: unavailable),
            stringResource(R.string.hardware_sensor_resolution) to (sensor.resolution?.let(::formatSensorValue) ?: unavailable),
            stringResource(R.string.hardware_sensor_power) to
                (sensor.powerMilliAmps?.let { "${formatSensorValue(it)} mA" } ?: unavailable),
            stringResource(R.string.hardware_sensor_rate) to
                (sensor.minDelayMicros?.let { if (it == 0) unavailable else "${1_000_000L / it} Hz" } ?: unavailable)
        )
    )
}

@Composable
private fun HardwareDataCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    rows: List<Pair<String, String>>,
    supporting: String? = null
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun LiveMotionCard(snapshot: MotionSensorSnapshot) {
    val accelerometer = snapshot.accelerometer
    val gyroscope = snapshot.gyroscope
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.hardware_sensor_live),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            MotionReadingRow(stringResource(R.string.hardware_sensor_accelerometer), accelerometer, "m/s²")
            MotionReadingRow(stringResource(R.string.hardware_sensor_gyroscope), gyroscope, "rad/s")
            SensorHistoryGraph(snapshot.samples)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SensorLegend(stringResource(R.string.hardware_axis_x), MaterialTheme.colorScheme.primary)
                SensorLegend(stringResource(R.string.hardware_axis_y), MaterialTheme.colorScheme.secondary)
                SensorLegend(stringResource(R.string.hardware_axis_z), MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun MotionReadingRow(label: String, sample: MotionSensorSample?, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        Text(
            text = sample?.let {
                "X ${formatSensorValue(it.x)}  ·  Y ${formatSensorValue(it.y)}  ·  Z ${formatSensorValue(it.z)} $unit"
            } ?: stringResource(R.string.status_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun SensorHistoryGraph(samples: List<MotionSensorSample>) {
    val gyroSamples = remember(samples) { samples.filter { it.type == MotionSensorType.GYROSCOPE } }
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    Canvas(Modifier.fillMaxWidth().height(136.dp)) {
        val top = 4.dp.toPx()
        val graphHeight = size.height - top * 2
        val middle = top + graphHeight / 2f
        drawLine(
            color = colors[0].copy(alpha = 0.25f),
            start = androidx.compose.ui.geometry.Offset(0f, middle),
            end = androidx.compose.ui.geometry.Offset(size.width, middle),
            strokeWidth = 1.dp.toPx()
        )
        if (gyroSamples.size < 2) return@Canvas
        val allValues = gyroSamples.flatMap { listOf(it.x, it.y, it.z) }
        val magnitude = maxOf(allValues.maxOrNull()?.absoluteValue ?: 1f, 0.05f)
        val axisValues = listOf(
            gyroSamples.map(MotionSensorSample::x),
            gyroSamples.map(MotionSensorSample::y),
            gyroSamples.map(MotionSensorSample::z)
        )
        axisValues.forEachIndexed { axis, values ->
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = size.width * index / (values.lastIndex).coerceAtLeast(1)
                val y = middle - (value / magnitude).coerceIn(-1f, 1f) * graphHeight / 2f
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, colors[axis], style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun SensorLegend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCircle(color) }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EmptyState(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun List<DeviceInfoItem>.firstValue(key: String): String? = firstOrNull { it.key == key }?.value

private fun HardwareDetailPage.titleRes(): Int = when (this) {
    HardwareDetailPage.DEVICE -> R.string.title_page_device
    HardwareDetailPage.DISPLAY -> R.string.title_page_display
    HardwareDetailPage.SOC_STORAGE -> R.string.title_page_soc
    HardwareDetailPage.BATTERY_NETWORK -> R.string.title_page_battery_network
    HardwareDetailPage.CAMERA -> R.string.title_page_camera
    HardwareDetailPage.SENSORS -> R.string.title_page_sensors
    HardwareDetailPage.MORE -> R.string.title_page_more
}

private fun formatSensorValue(value: Float): String = String.format(Locale.getDefault(), "%.2f", value)
