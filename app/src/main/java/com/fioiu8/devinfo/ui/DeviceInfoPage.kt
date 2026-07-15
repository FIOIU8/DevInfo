package com.fioiu8.devinfo.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.BatteryObserver
import com.fioiu8.devinfo.DeviceInfoCollector
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.InfoCategory
import com.fioiu8.devinfo.model.ItemWithVisibility

/** 分类卡片每页显示的最大条目数 */
private const val ITEMS_PER_PAGE = 8

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
    onRefresh: suspend () -> Unit
) {
    val ctx = LocalContext.current
    val resources = LocalResources.current
    val clipboardManager = LocalClipboardManager.current
    val collector = remember { DeviceInfoCollector(ctx) }
    val batteryObserver = remember { BatteryObserver(ctx) }
    val batteryState by batteryObserver.batteryState.collectAsState(
        initial = BatteryObserver.BatteryState(level = 100, isCharging = false)
    )

    var isRefreshing by remember { mutableStateOf(false) }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var previousCategoryIndex by remember { mutableIntStateOf(0) }
    var currentPage by remember(selectedCategoryIndex) { mutableIntStateOf(0) }
    val pullToRefreshState = rememberPullToRefreshState()
    val categories = InfoCategory.entries

    // Read these on recomposition so a refresh reflects the current device state.
    val storagePercent = collector.getStorageUsagePercent()
    val memoryPercent = collector.getMemoryUsagePercent()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            onRefresh()
            isRefreshing = false
        }
    }

    if (isLoading && itemsState.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val selectedCategory = categories[selectedCategoryIndex]

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
                    top = 12.dp, bottom = 12.dp, start = 0.dp, end = 0.dp
                )
            ) {
                item {
                    OverviewCard(
                        items = itemsState,
                        storagePercent = storagePercent,
                        memoryPercent = memoryPercent,
                        batteryLevel = batteryState.level,
                        batteryCharging = batteryState.isCharging,
                        isLoading = isLoading
                    )
                }

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
                    val categoryItems = itemsState.filter { it.item.category == selectedCategory }
                    val totalPages = ((categoryItems.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)
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
                        val visibleCategoryItems = itemsState.filter { it.item.category == category }
                        val visibleTotalPages = ((visibleCategoryItems.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE).coerceAtLeast(1)
                        CategoryCard(
                            category = category,
                            items = visibleCategoryItems,
                            currentPage = page,
                            totalPages = visibleTotalPages,
                            onPageChange = { currentPage = it },
                            onItemCopy = { item ->
                                clipboardManager.setText(AnnotatedString(item.item.value))
                                val itemLabel = resources.getString(item.item.keyResId)
                                Toast.makeText(
                                    ctx,
                                    resources.getString(
                                        com.fioiu8.devinfo.R.string.copied_to_clipboard,
                                        itemLabel
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            storagePercent = storagePercent,
                            memoryPercent = memoryPercent,
                            batteryLevel = batteryState.level,
                            batteryCharging = batteryState.isCharging
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
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
/** 水平滚动的分类标签行 */
@Composable
private fun CategoryTabRow(
    categories: List<InfoCategory>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories.size) { index ->
            val category = categories[index]
            val selected = selectedIndex == index
            FilterChip(
                selected = selected,
                onClick = { onCategorySelected(index) },
                label = {
                    Text(text = stringResource(category.displayNameResId), style = MaterialTheme.typography.labelMedium)
                },
                leadingIcon = {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
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
    storagePercent: Float = 0f,
    memoryPercent: Float = 0f,
    batteryLevel: Int = 100,
    batteryCharging: Boolean = false
) {
    // 分页切片
    val pagedItems = items.drop(currentPage * ITEMS_PER_PAGE).take(ITEMS_PER_PAGE)
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
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        
                        text = stringResource(com.fioiu8.devinfo.R.string.items_count, items.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                InfoCategory.STORAGE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        StorageProgressSection(storagePercent, memoryPercent)
                    }
                }
                InfoCategory.BATTERY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BatteryProgressSection(batteryLevel, batteryCharging)
                    }
                }
                else -> { /* 无进度条 */ }
            }

            // Divider between progress and data rows (only for STORAGE/BATTERY)
            if (category == InfoCategory.STORAGE || category == InfoCategory.BATTERY) {
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
private fun StorageProgressSection(storagePct: Float, memoryPct: Float) {
    val animatedStoragePct by animateFloatAsState(storagePct.coerceIn(0f, 100f), tween(700), label = "storageProgress")
    val animatedMemoryPct by animateFloatAsState(memoryPct.coerceIn(0f, 100f), tween(700), label = "memoryProgress")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 存储使用进度条
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
        // 内存使用进度条
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
