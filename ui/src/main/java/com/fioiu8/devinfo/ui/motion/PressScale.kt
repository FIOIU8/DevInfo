/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** 用户偏好：是否启用按压缩放。系统移除动画或动效等级为关闭时仍会被跳过。 */
val LocalDevInfoPressAnimationEnabled = staticCompositionLocalOf { true }

/**
 * M3 Expressive 风格的按压缩放反馈。
 *
 * 只做 `graphicsLayer` 缩放，不改变布局与命中区域；必须放在 `.clickable(...)` 之后，
 * 这样点击区域仍是原始尺寸，涟漪也照常绘制。动效关闭时直接返回原 Modifier，
 * 避免额外的状态订阅与图层。
 */
@Composable
fun Modifier.devInfoPressable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = LocalDevInfoMotionLevel.current.isAnimated &&
        LocalDevInfoPressAnimationEnabled.current,
    pressedScale: Float = 0.96f,
): Modifier {
    if (!enabled) return this

    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "devInfoPressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
