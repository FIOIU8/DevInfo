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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.model.ItemWithVisibility
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分类卡片每页显示的最大条目数 */
private const val ITEMS_PER_PAGE = 8

private fun List<ItemWithVisibility>.pageCount(): Int =
    ((size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)

/**
 * 设备信息页 — 分类浏览 + 下拉刷新。
 *
 * @param deviceId 设备唯一标识
 * @param itemsState 已加载的设备信息列表（含可见性状态）
 * @param isLoading 是否正在首次加载
 * @param onRefresh 下拉刷新回调
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceInfoPage(
    deviceId: String,
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    overviewSnapshot: OverviewSnapshot,
    onRefresh: suspend () -> Unit,
    initialCategory: InfoCategory = InfoCategory.DEVICE
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixDeviceInfoPage(
            itemsState = itemsState,
            isLoading = isLoading,
            overviewSnapshot = overviewSnapshot,
            onRefresh = onRefresh,
            initialCategory = initialCategory,
        )
        return
    }

    val resources = LocalResources.current
    val clipboardManager = LocalClipboardManager.current
    val showMessage = rememberDevInfoMessageHandler()
    val categories = InfoCategory.entries
    val itemsByCategory = remember(itemsState) {
        itemsState.groupBy { it.item.category }
    }
    val pageCountsByCategory = remember(itemsByCategory) {
        itemsByCategory.mapValues { (_, categoryItems) -> categoryItems.pageCount() }
    }
    val onItemCopy: (ItemWithVisibility) -> Unit = remember(clipboardManager, resources, showMessage) {
        { item ->
            clipboardManager.setText(AnnotatedString(item.item.value))
            val itemLabel = resources.getString(item.item.keyResId)
            showMessage(
                resources.getString(
                    com.fioiu8.devinfo.R.string.copied_to_clipboard,
                    itemLabel,
                ),
            )
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember(initialCategory) {
        mutableIntStateOf(categories.indexOf(initialCategory).coerceAtLeast(0))
    }
    var previousCategoryIndex by remember { mutableIntStateOf(0) }
    var currentPage by remember(selectedCategoryIndex) { mutableIntStateOf(0) }
    val pullToRefreshState = rememberPullToRefreshState()

    val storagePercent = overviewSnapshot.storagePercent
    val memoryPercent = overviewSnapshot.memoryPercent

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    if (isLoading && itemsState.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DevInfoLoadingIndicator()
        }
    } else {
        val selectedCategory = categories[selectedCategoryIndex]
        val selectedCategoryItems = itemsByCategory[selectedCategory].orEmpty()
        val totalPages = selectedCategoryItems.pageCount()

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = 12.dp + LocalFloatingNavigationContentPadding.current,
                    start = 0.dp,
                    end = 0.dp,
                )
            ) {
                // Category Tab Row
                item {
                    CategoryTabRow(
                        categories = categories,
                        selectedIndex = selectedCategoryIndex,
                        onCategorySelected = { newIndex ->
                            previousCategoryIndex = selectedCategoryIndex
                            selectedCategoryIndex = newIndex
                        }
                    )
                }

                // Category Hero Card with directional animation + pagination
                item {
                    // 超出页码范围时自动修正
                    LaunchedEffect(selectedCategoryIndex, totalPages) {
                        if (currentPage >= totalPages) currentPage = 0
                    }
                    val animDirection = if (selectedCategoryIndex > previousCategoryIndex) 1 else -1
                    AnimatedContent(
                        targetState = selectedCategory to currentPage,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { animDirection * it / 4 })
                                .togetherWith(fadeOut() + slideOutHorizontally { -animDirection * it / 4 })
                        },
                        label = "categoryPageSwitch"
                    ) { (category, page) ->
                        val visibleCategoryItems = itemsByCategory[category].orEmpty()
                        val visibleTotalPages = pageCountsByCategory[category] ?: 1
                        CategoryCard(
                            category = category,
                            items = visibleCategoryItems,
                            currentPage = page,
                            totalPages = visibleTotalPages,
                            onPageChange = { currentPage = it },
                            onItemCopy = onItemCopy,
                            storagePercent = storagePercent,
                            memoryPercent = memoryPercent,
                            batteryLevel = overviewSnapshot.batteryLevel,
                            batteryCharging = overviewSnapshot.batteryCharging,
                            cpuCoreMetrics = overviewSnapshot.cpuCoreMetrics
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }
    }
}

@Composable
private fun MiuixDeviceInfoPage(
    itemsState: List<ItemWithVisibility>,
    isLoading: Boolean,
    overviewSnapshot: OverviewSnapshot,
    onRefresh: suspend () -> Unit,
    initialCategory: InfoCategory,
) {
    val resources = LocalResources.current
    val clipboardManager = LocalClipboardManager.current
    val showMessage = rememberDevInfoMessageHandler()
    val scope = rememberCoroutineScope()
    val categories = InfoCategory.entries
    var selectedCategoryIndex by remember(initialCategory) {
        mutableIntStateOf(categories.indexOf(initialCategory).coerceAtLeast(0))
    }
    var isRefreshing by remember { mutableStateOf(false) }
    val itemsByCategory = remember(itemsState) {
        itemsState.groupBy { it.item.category }
    }
    val selectedCategory = categories[selectedCategoryIndex]
    val selectedItems = itemsByCategory[selectedCategory].orEmpty()

    if (isLoading && itemsState.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 12.dp,
            end = 12.dp,
            bottom = 12.dp + LocalFloatingNavigationContentPadding.current,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MiuixTabRow(
                tabs = categories.map { stringResource(it.displayNameResId) },
                selectedTabIndex = selectedCategoryIndex,
                onTabSelected = { selectedCategoryIndex = it },
            )
        }
        item {
            MiuixCategoryCard(
                category = selectedCategory,
                items = selectedItems,
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        try { onRefresh() } finally { isRefreshing = false }
                    }
                },
                onItemClick = { item ->
                    clipboardManager.setText(AnnotatedString(item.item.value))
                    showMessage(
                        resources.getString(
                            R.string.copied_to_clipboard,
                            resources.getString(item.item.keyResId),
                        ),
                    )
                },
                overviewSnapshot = overviewSnapshot,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiuixCategoryCard(
    category: InfoCategory,
    items: List<ItemWithVisibility>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onItemClick: (ItemWithVisibility) -> Unit,
    overviewSnapshot: OverviewSnapshot,
) {
    val resources = LocalResources.current
    val totalPages = ((items.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)
    var currentPage by remember(category) { mutableIntStateOf(0) }

    LaunchedEffect(category, totalPages) {
        if (currentPage >= totalPages) currentPage = 0
    }

    val pagedItems = remember(items, currentPage) {
        items.drop(currentPage * ITEMS_PER_PAGE).take(ITEMS_PER_PAGE)
    }

    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 16.dp,
    ) {
        Column {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiuixIcon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MiuixText(
                        text = stringResource(category.displayNameResId),
                        style = MiuixTheme.textStyles.subtitle,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    MiuixText(
                        text = if (isRefreshing) {
                            stringResource(R.string.overview_loading)
                        } else {
                            stringResource(category.descriptionResId)
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
                MiuixCard(
                    insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    cornerRadius = 8.dp,
                    colors = MiuixCardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    MiuixText(
                        text = stringResource(R.string.items_count, items.size),
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
            }

            // ── Progress sections for STORAGE / BATTERY / SYSTEM ──
            val showProgress = category == InfoCategory.STORAGE ||
                category == InfoCategory.BATTERY ||
                (category == InfoCategory.SYSTEM && overviewSnapshot.cpuCoreMetrics.isNotEmpty())

            if (showProgress) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (category) {
                        InfoCategory.STORAGE -> {
                            MiuixStorageProgressSection(
                                overviewSnapshot.storagePercent,
                                overviewSnapshot.memoryPercent
                            )
                        }
                        InfoCategory.BATTERY -> {
                            overviewSnapshot.batteryLevel?.let { level ->
                                MiuixBatteryProgressSection(
                                    level.toFloat(),
                                    overviewSnapshot.batteryCharging
                                )
                            }
                        }
                        InfoCategory.SYSTEM -> {
                            if (overviewSnapshot.cpuCoreMetrics.isNotEmpty()) {
                                MiuixCpuCoreSection(overviewSnapshot.cpuCoreMetrics)
                            }
                        }
                        else -> { }
                    }
                }
            }

            // ── Data items (paginated) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                pagedItems.forEach { item ->
                    key(item.item.key) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onLongClick = { onItemClick(item) },
                                    onClick = { onItemClick(item) }
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfoRow(
                                label = resources.getString(item.item.keyResId) + ":",
                                value = item.item.value,
                                icon = item.item.icon
                            )
                        }
                    }
                }
                if (pagedItems.isEmpty()) {
                    MiuixText(
                        text = stringResource(R.string.no_data),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // ── Page navigation ──
            if (totalPages > 1) {
                MiuixPageNavigationRow(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPrevious = { if (currentPage > 0) currentPage-- },
                    onNext = { if (currentPage < totalPages - 1) currentPage++ },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MiuixPageNavigationRow(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixBasicComponent(
            title = stringResource(R.string.previous_page),
            startAction = {
                MiuixIcon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.previous_page),
                    modifier = Modifier.size(18.dp),
                    tint = if (currentPage > 0) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceSecondary
                )
            },
            onClick = {
                if (currentPage > 0) onPrevious()
            },
        )

        MiuixText(
            text = stringResource(R.string.page_info, currentPage + 1, totalPages),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )

        MiuixBasicComponent(
            title = stringResource(R.string.next_page),
            endActions = {
                MiuixIcon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.next_page),
                    modifier = Modifier.size(18.dp),
                    tint = if (currentPage < totalPages - 1) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurfaceSecondary
                )
            },
            onClick = {
                if (currentPage < totalPages - 1) onNext()
            },
        )
    }
}

@Composable
private fun MiuixStorageProgressSection(storagePct: Float?, memoryPct: Float?) {
    val animatedStoragePct by animateFloatAsState(
        storagePct?.coerceIn(0f, 100f) ?: 0f, tween(700), label = "miuixStorageProgress"
    )
    val animatedMemoryPct by animateFloatAsState(
        memoryPct?.coerceIn(0f, 100f) ?: 0f, tween(700), label = "miuixMemoryProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        storagePct?.let {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiuixText(
                        text = stringResource(R.string.overview_storage),
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    MiuixText(
                        text = "%.1f%%".format(animatedStoragePct),
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.SemiBold,
                        color = progressColor(animatedStoragePct)
                    )
                }
                Spacer(Modifier.height(6.dp))
                MiuixLinearProgressIndicator(
                    progress = animatedStoragePct / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = progressColor(animatedStoragePct),
                        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh
                    ),
                    height = 8.dp
                )
            }
        }
        memoryPct?.let {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiuixText(
                        text = stringResource(R.string.overview_memory),
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    MiuixText(
                        text = "%.1f%%".format(animatedMemoryPct),
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.SemiBold,
                        color = progressColor(animatedMemoryPct)
                    )
                }
                Spacer(Modifier.height(6.dp))
                MiuixLinearProgressIndicator(
                    progress = animatedMemoryPct / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = progressColor(animatedMemoryPct),
                        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh
                    ),
                    height = 8.dp
                )
            }
        }
    }
}

@Composable
private fun MiuixBatteryProgressSection(level: Float, charging: Boolean) {
    val animatedLevel by animateFloatAsState(
        level.coerceIn(0f, 100f), tween(700), label = "miuixBatteryProgress"
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MiuixText(
                text = stringResource(R.string.overview_battery),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface
            )
            MiuixText(
                text = "${animatedLevel.toInt()}%",
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold,
                color = batteryColor(animatedLevel.toInt())
            )
        }
        Spacer(Modifier.height(6.dp))
        MiuixLinearProgressIndicator(
            progress = animatedLevel / 100f,
            modifier = Modifier.fillMaxWidth(),
            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                foregroundColor = batteryColor(animatedLevel.toInt()),
                backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh
            ),
            height = 8.dp
        )
        if (charging) {
            Spacer(Modifier.height(4.dp))
            MiuixText(
                text = stringResource(R.string.status_charging),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MiuixCpuCoreSection(metrics: List<com.fioiu8.devinfo.CpuCoreMetric>) {
    val metricRows = remember(metrics) { metrics.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MiuixText(
            text = stringResource(R.string.overview_cpu_cores_detail),
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )
        metricRows.forEach { rowMetrics ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowMetrics.forEach { metric -> MiuixCpuCoreItem(metric, Modifier.weight(1f)) }
                if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiuixCpuCoreItem(metric: com.fioiu8.devinfo.CpuCoreMetric, modifier: Modifier) {
    val usage = metric.usagePercent
    val animatedUsage by animateFloatAsState((usage ?: 0f).coerceIn(0f, 100f), tween(650), label = "miuixCpuCore")
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            androidx.compose.material3.CircularProgressIndicator(
                progress = { animatedUsage / 100f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 5.dp,
                color = coreUsageColor(animatedUsage),
                trackColor = MiuixTheme.colorScheme.surfaceContainerHigh
            )
            MiuixText(
                text = usage?.let { "${animatedUsage.toInt()}%" } ?: "--",
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            MiuixText(
                text = "CPU${metric.index}",
                style = MiuixTheme.textStyles.main,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface
            )
            metric.frequency?.let {
                MiuixText(
                    text = it,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(
    items: List<ItemWithVisibility>,
    storagePercent: Float,
    memoryPercent: Float,
    batteryLevel: Int,
    batteryCharging: Boolean,
    isLoading: Boolean
) {
    val manufacturer = items.valueFor(R.string.device_manufacturer)
    val model = items.valueFor(R.string.device_model)
    val deviceName = listOfNotNull(manufacturer, model).joinToString(" ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = categoryIcon(InfoCategory.DEVICE),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.overview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = deviceName.ifBlank { stringResource(R.string.overview_loading) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OverviewMetric(stringResource(R.string.overview_storage), storagePercent, progressColor(storagePercent))
            Spacer(modifier = Modifier.height(10.dp))
            OverviewMetric(stringResource(R.string.overview_memory), memoryPercent, progressColor(memoryPercent))
            Spacer(modifier = Modifier.height(10.dp))
            OverviewMetric(stringResource(R.string.overview_battery), batteryLevel.toFloat(), batteryColor(batteryLevel), batteryCharging)
        }
    }
}

@Composable
private fun OverviewMetric(
    label: String,
    percent: Float,
    color: Color,
    charging: Boolean = false
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = tween(700),
        label = "overviewProgress"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.width(76.dp)
        )
        LinearProgressIndicator(
            progress = { animatedPercent / 100f },
            modifier = Modifier
                .weight(1f)
                .height(7.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
        )
        Text(
            text = if (charging) "${animatedPercent.toInt()}%" else "${animatedPercent.toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

private fun List<ItemWithVisibility>.valueFor(keyResId: Int): String? =
    firstOrNull { it.item.keyResId == keyResId }?.item?.value

// ── Category Tab Row ──
/** Official Material 3 tabs keep each detail category separate and scannable. */
@Composable
private fun CategoryTabRow(
    categories: List<InfoCategory>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp,
        divider = {}
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onCategorySelected(index) },
                text = { Text(stringResource(category.displayNameResId)) }
            )
        }
    }
}

// ── Category Hero Card ──
/** 分类信息卡片，包含 Header + 进度条（存储/电池）+ 分页数据项列表，长按复制 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryCard(
    category: InfoCategory,
    items: List<ItemWithVisibility>,
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onItemCopy: (ItemWithVisibility) -> Unit,
    storagePercent: Float? = null,
    memoryPercent: Float? = null,
    batteryLevel: Int? = null,
    batteryCharging: Boolean = false,
    cpuCoreMetrics: List<com.fioiu8.devinfo.CpuCoreMetric> = emptyList()
) {
    // 分页切片
    val pagedItems = remember(items, currentPage) {
        items.drop(currentPage * ITEMS_PER_PAGE).take(ITEMS_PER_PAGE)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(category.displayNameResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(category.descriptionResId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = stringResource(com.fioiu8.devinfo.R.string.items_count, items.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ── Progress bars for STORAGE & BATTERY ──
            when (category) {
                InfoCategory.STORAGE -> if (storagePercent != null || memoryPercent != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        StorageProgressSection(storagePercent, memoryPercent)
                    }
                }
                InfoCategory.BATTERY -> if (batteryLevel != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BatteryProgressSection(batteryLevel, batteryCharging)
                    }
                }
                InfoCategory.SYSTEM -> if (cpuCoreMetrics.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        CpuCoreDetailSection(cpuCoreMetrics)
                    }
                }
                else -> { /* 无进度条 */ }
            }

            // Divider between progress and data rows (only for STORAGE/BATTERY)
            if (category == InfoCategory.STORAGE || category == InfoCategory.BATTERY ||
                (category == InfoCategory.SYSTEM && cpuCoreMetrics.isNotEmpty())
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Data items (paginated)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                pagedItems.forEachIndexed { index, item ->
                    key(item.item.key) {
                        AnimatedVisibility(
                            visible = item.visible.value,
                            enter = fadeIn(tween(220, delayMillis = index * 40)) +
                                slideInHorizontally(tween(220, delayMillis = index * 40))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onLongClick = { onItemCopy(item) },
                                        onClick = {}
                                    )
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InfoRow(
                                    label = stringResource(item.item.keyResId) + ":",
                                    value = item.item.value,
                                    icon = item.item.icon
                                )
                            }
                        }
                    }
                }
            }

            if (pagedItems.isEmpty()) {
                Text(
                    text = stringResource(com.fioiu8.devinfo.R.string.no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Page navigation
            if (totalPages > 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                PageNavigationRow(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPrevious = { if (currentPage > 0) onPageChange(currentPage - 1) },
                    onNext = { if (currentPage < totalPages - 1) onPageChange(currentPage + 1) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CpuCoreDetailSection(metrics: List<com.fioiu8.devinfo.CpuCoreMetric>) {
    val metricRows = remember(metrics) { metrics.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(com.fioiu8.devinfo.R.string.overview_cpu_cores_detail),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        metricRows.forEach { rowMetrics ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowMetrics.forEach { metric -> CpuCoreDetailItem(metric, Modifier.weight(1f)) }
                if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CpuCoreDetailItem(metric: com.fioiu8.devinfo.CpuCoreMetric, modifier: Modifier) {
    val usage = metric.usagePercent
    val animatedUsage by animateFloatAsState((usage ?: 0f).coerceIn(0f, 100f), tween(650), label = "cpuCoreUsage")
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            CircularProgressIndicator(
                progress = { animatedUsage / 100f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 5.dp,
                color = coreUsageColor(animatedUsage),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(usage?.let { "${animatedUsage.toInt()}%" } ?: "--", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text("CPU${metric.index}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            metric.frequency?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun coreUsageColor(usage: Float): Color = when {
    usage < 50f -> Color(0xFF4CAF50)
    usage < 80f -> Color(0xFFFFA726)
    else -> Color(0xFFEF5350)
}

// ── Page Navigation Row ──
/** 上一页/下一页导航条 */
@Composable
private fun PageNavigationRow(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = currentPage > 0
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(com.fioiu8.devinfo.R.string.previous_page),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(stringResource(com.fioiu8.devinfo.R.string.previous_page))
        }

        Text(
            text = stringResource(
                com.fioiu8.devinfo.R.string.page_info,
                currentPage + 1,
                totalPages
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextButton(
            onClick = onNext,
            enabled = currentPage < totalPages - 1
        ) {
            Text(stringResource(com.fioiu8.devinfo.R.string.next_page))
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(com.fioiu8.devinfo.R.string.next_page),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Storage Progress Section ──
/** 存储与内存使用百分比进度条 */
@Composable
private fun StorageProgressSection(storagePct: Float?, memoryPct: Float?) {
    val animatedStoragePct by animateFloatAsState(storagePct?.coerceIn(0f, 100f) ?: 0f, tween(700), label = "storageProgress")
    val animatedMemoryPct by animateFloatAsState(memoryPct?.coerceIn(0f, 100f) ?: 0f, tween(700), label = "memoryProgress")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        storagePct?.let {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(com.fioiu8.devinfo.R.string.overview_storage),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.1f%%".format(animatedStoragePct),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = progressColor(animatedStoragePct)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedStoragePct / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = progressColor(animatedStoragePct),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
        memoryPct?.let {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(com.fioiu8.devinfo.R.string.overview_memory),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.1f%%".format(animatedMemoryPct),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = progressColor(animatedMemoryPct)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedMemoryPct / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = progressColor(animatedMemoryPct),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

// ── Battery Progress Section ──
/** 电池电量进度条 */
@Composable
private fun BatteryProgressSection(level: Int, isCharging: Boolean) {
    val animatedLevel by animateFloatAsState(level.coerceIn(0, 100).toFloat(), tween(700), label = "batteryProgress")
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(com.fioiu8.devinfo.R.string.overview_battery) +
                    if (isCharging) " (${stringResource(com.fioiu8.devinfo.R.string.status_charging)})" else "",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${animatedLevel.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = batteryColor(animatedLevel.toInt())
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedLevel / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = batteryColor(animatedLevel.toInt()),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
    }
}

// ── 存储/内存进度条颜色：低 → 绿，高 → 红（高使用率 = 需要关注） ──
/** 根据使用百分比返回进度条颜色 */
@Composable
private fun progressColor(percent: Float): Color {
    return when {
        percent < 50f -> Color(0xFF4CAF50) // 绿 — 充足
        percent < 80f -> Color(0xFFFFA726) // 橙 — 中度
        else -> Color(0xFFEF5350)            // 红 — 高使用率
    }
}

// ── 电池进度条颜色：高 → 绿，中 → 黄，低 → 红（高电量 = 好） ──
/** 根据电池电量返回进度条颜色 */
@Composable
private fun batteryColor(level: Int): Color {
    return when {
        level > 50 -> Color(0xFF4CAF50) // 绿 — 充足
        level > 20 -> Color(0xFFFFA726) // 橙 — 中等
        else -> Color(0xFFEF5350)        // 红 — 低电量
    }
}
