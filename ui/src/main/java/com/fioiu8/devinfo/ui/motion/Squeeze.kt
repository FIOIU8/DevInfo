/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private val SqueezeProgressSpec = spring(0.5f, 300f, 0.001f)
private val SqueezeOffsetSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

/**
 * 参考 AndroidLiquidGlass 目录页 LiquidButton 的长按挤压反馈。
 *
 * 按下时整体轻微膨胀（4dp 相对于高度），并跟随手指相对按下点的偏移做各向异性
 * 拉伸与 tanh 位移，形成果冻感；松手后回弹。只通过 `graphicsLayer` 改变绘制，
 * 命中区域保持原始尺寸，因此必须挂在 `clickable` 之后。
 */
@Composable
fun Modifier.devInfoSqueeze(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = LocalDevInfoMotionLevel.current.isAnimated &&
        LocalDevInfoPressAnimationEnabled.current,
    maxSqueeze: Dp = 4.dp,
): Modifier {
    if (!enabled) return this

    val maxSqueezePx = with(LocalDensity.current) { maxSqueeze.toPx() }
    val pressed by interactionSource.collectIsPressedAsState()
    val progress = remember { Animatable(0f, 0.001f) }
    val pointerOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val animationScope = rememberCoroutineScope()

    LaunchedEffect(pressed) {
        progress.animateTo(if (pressed) 1f else 0f, SqueezeProgressSpec)
    }

    return this
        .pointerInput(maxSqueezePx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val start = down.position
                animationScope.launch { pointerOffset.snapTo(Offset.Zero) }
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        animationScope.launch { pointerOffset.snapTo(change.position - start) }
                    }
                } finally {
                    animationScope.launch {
                        pointerOffset.animateTo(Offset.Zero, SqueezeOffsetSpec)
                    }
                }
            }
        }
        .graphicsLayer {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@graphicsLayer

            val press = progress.value
            val offset = pointerOffset.value
            val baseScale = lerp(1f, 1f + maxSqueezePx / height, press)
            val maxDragScale = maxSqueezePx / height
            val maxDimension = size.maxDimension

            translationX = maxDimension * tanh(0.05f * offset.x / maxDimension)
            translationY = maxDimension * tanh(0.05f * offset.y / maxDimension)

            val angle = atan2(offset.y, offset.x)
            scaleX = baseScale +
                maxDragScale * abs(cos(angle) * offset.x / maxDimension) *
                (width / height).coerceAtMost(1f)
            scaleY = baseScale +
                maxDragScale * abs(sin(angle) * offset.y / maxDimension) *
                (height / width).coerceAtMost(1f)
        }
}
