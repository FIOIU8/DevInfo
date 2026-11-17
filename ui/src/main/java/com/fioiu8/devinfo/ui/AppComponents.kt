/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.core.model.InfoCategory
import com.fioiu8.devinfo.ui.motion.devInfoSqueeze

/**
 * Returns the string resource ID for an InfoCategory's display name.
 * This bridges the gap between the core enum (no R deps) and UI (needs strings).
 */
fun InfoCategory.displayNameResId(): Int = when (this) {
    InfoCategory.DEVICE -> R.string.category_device
    InfoCategory.IDENTIFIERS -> R.string.category_identifiers
    InfoCategory.SYSTEM -> R.string.category_system
    InfoCategory.LOCALE -> R.string.category_locale
    InfoCategory.DISPLAY -> R.string.category_display
    InfoCategory.STORAGE -> R.string.category_storage
    InfoCategory.BATTERY -> R.string.category_battery
    InfoCategory.NETWORK -> R.string.category_network
    InfoCategory.APP -> R.string.category_app
}

/** Returns the string resource ID for an InfoCategory's description */
fun InfoCategory.descriptionResId(): Int = when (this) {
    InfoCategory.DEVICE -> R.string.category_desc_device
    InfoCategory.IDENTIFIERS -> R.string.category_desc_identifiers
    InfoCategory.SYSTEM -> R.string.category_desc_system
    InfoCategory.LOCALE -> R.string.category_desc_locale
    InfoCategory.DISPLAY -> R.string.category_desc_display
    InfoCategory.STORAGE -> R.string.category_desc_storage
    InfoCategory.BATTERY -> R.string.category_desc_battery
    InfoCategory.NETWORK -> R.string.category_desc_network
    InfoCategory.APP -> R.string.category_desc_app
}

/**
 * 测试/非官方版本警告卡片，显示在 TopAppBar 下方。
 *
 * 使用 M3E 的色调容器与 Expressive 徽章形状；点按可展开/收起完整说明，
 * 并复用 AndroidLiquidGlass LiquidButton 的长按挤压反馈。
 *
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TestVersionWarningCard(
    modifier: Modifier = Modifier,
    versionName: String = "",
    buildType: String = "dev"
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val cardShape = MaterialTheme.shapes.largeIncreased
    val badgeShape = MaterialShapes.Cookie4Sided.toShape()
    val title = stringResource(R.string.test_version_warning)
    val description = stringResource(R.string.test_version_desc)
    val detail = stringResource(R.string.test_version_detail)
    val badgeDescription = stringResource(R.string.warning_icon_desc)
    val toggleLabel = stringResource(
        if (expanded) R.string.test_version_collapse else R.string.test_version_expand
    )
    // 单个旋转状态由外层驱动，两个布局里的箭头内容一致，sharedBounds 只负责移动。
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "testVersionChevron",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            // 不使用外层 clip：它会裁掉挤压时放大的边缘。
            // Surface 只负责内容圆角；共享元素放在 Surface 外，避免被卡片蒙版裁切。
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = toggleLabel,
            ) { expanded = !expanded }
            .devInfoSqueeze(interactionSource),
    ) {
        SharedTransitionLayout {
            val sharedScope = this
            // 共享元素放在 Surface 外部：Surface 的圆角蒙版只裁内容，
            // 不再把展开中的徽章/标题一刀切掉。
            Surface(
                shape = cardShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(tween(durationMillis = 180, delayMillis = 40)) togetherWith
                            fadeOut(tween(durationMillis = 90)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "testVersionExpand",
                ) { isExpanded ->
                    val animatedScope = this
                    val badgeModifier = with(sharedScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState("test-version-badge"),
                            animatedVisibilityScope = animatedScope,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.75f, stiffness = 500f)
                            },
                        )
                    }
                    val titleModifier = with(sharedScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState("test-version-title"),
                            animatedVisibilityScope = animatedScope,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = 450f)
                            },
                        )
                    }
                    val chevronModifier = with(sharedScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState("test-version-chevron"),
                            animatedVisibilityScope = animatedScope,
                            boundsTransform = { _, _ ->
                                spring(dampingRatio = 0.7f, stiffness = 500f)
                            },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = if (isExpanded) 14.dp else 12.dp,
                            ),
                        verticalAlignment = if (isExpanded) {
                            Alignment.Top
                        } else {
                            Alignment.CenterVertically
                        },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isExpanded) 48.dp else 44.dp)
                                .then(badgeModifier)
                                .background(MaterialTheme.colorScheme.errorContainer, badgeShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = badgeDescription,
                                modifier = Modifier.size(if (isExpanded) 26.dp else 24.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(titleModifier),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (versionName.isNotBlank()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ) {
                                        Text(
                                            text = "$versionName · $buildType",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isExpanded) {
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .then(chevronModifier)
                                .rotate(chevronRotation)
                                .align(
                                    if (isExpanded) Alignment.Top else Alignment.CenterVertically
                                ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
