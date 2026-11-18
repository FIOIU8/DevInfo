/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.fioiu8.devinfo.ui.kit.liquid.LiquidGlassCard
import com.fioiu8.devinfo.ui.motion.devInfoPressable
import com.fioiu8.devinfo.ui.theme.LocalDevInfoGlassEnabled

/**
 * 卡片在页面里的语义层级。
 *
 * 每个层级唯一对应一套「容器色 + 形状」，页面不再自行挑 surface 角色或圆角，
 * 这样同一层级的卡片在不同页面看起来才是同一个东西。
 */
enum class DevInfoCardLevel {
    /** 页面级内容卡，如指标卡、静态信息卡。 */
    CARD,

    /** 强调卡，用于页面主信息，如设备摘要、详情页 hero。 */
    EMPHASIS,

    /** 分组容器内的行，如设置项。 */
    ROW,

    /** 直接铺在页面上的信息行，如详情页字段。 */
    DETAIL_ROW,
}

/** 该层级是否允许使用液态玻璃表面。分组行始终是平面，避免一层玻璃套一层玻璃。 */
private val DevInfoCardLevel.supportsGlass: Boolean
    get() = this != DevInfoCardLevel.ROW

@Composable
private fun DevInfoCardLevel.containerColor() = when (this) {
    DevInfoCardLevel.CARD -> MaterialTheme.colorScheme.surfaceContainerHigh
    DevInfoCardLevel.EMPHASIS -> MaterialTheme.colorScheme.primaryContainer
    DevInfoCardLevel.ROW -> MaterialTheme.colorScheme.surfaceContainer
    DevInfoCardLevel.DETAIL_ROW -> MaterialTheme.colorScheme.surfaceContainerLow
}

@Composable
private fun DevInfoCardLevel.containerShape(): Shape = when (this) {
    DevInfoCardLevel.CARD, DevInfoCardLevel.ROW -> MaterialTheme.shapes.large
    DevInfoCardLevel.EMPHASIS -> MaterialTheme.shapes.largeIncreased
    DevInfoCardLevel.DETAIL_ROW -> MaterialTheme.shapes.medium
}

/**
 * 全应用唯一的卡片入口。
 *
 * 液态玻璃开启时使用玻璃表面；关闭时退回 Material 3 色调表面，两者共享同一套
 * 容器色、形状与点击语义，因此开关只改变材质，不改变布局。
 *
 * @param modifier 外部修饰符
 * @param level 语义层级，决定容器色与形状
 * @param onClick 提供时整卡可点击，并带按钮语义与按压反馈
 * @param content 卡片内容，垂直排列
 */
@Composable
fun DevInfoCard(
    modifier: Modifier = Modifier,
    level: DevInfoCardLevel = DevInfoCardLevel.CARD,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = level.containerShape()
    val containerColor = level.containerColor()
    val useGlass = level.supportsGlass && LocalDevInfoGlassEnabled.current
    val interactionSource = remember { MutableInteractionSource() }

    val cardModifier = if (onClick == null) {
        modifier
    } else {
        modifier
            // 先按卡片圆角裁剪，涟漪才不会画成矩形超出圆角。
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .devInfoPressable(interactionSource)
    }

    if (useGlass) {
        LiquidGlassCard(
            modifier = cardModifier,
            shape = shape,
            containerColor = containerColor,
            content = content,
        )
    } else {
        Surface(
            modifier = cardModifier,
            shape = shape,
            color = containerColor,
        ) {
            Column(content = content)
        }
    }
}
