/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.fioiu8.devinfo.core.model.MotionLevel

/** 当前生效的动效等级；系统「移除动画」会在主题层折算为 [MotionLevel.OFF]。 */
val LocalDevInfoMotionLevel = staticCompositionLocalOf { MotionLevel.STANDARD }

/**
 * 当前生效的 M3 Expressive 运动方案，供 `transitionSpec` 等非 Composable 调用点使用。
 *
 * material3 1.5.0 起 `MotionScheme` 已是公开 API，与 [MaterialTheme.motionScheme] 保持一致。
 */
val LocalDevInfoMotionScheme = staticCompositionLocalOf { MotionScheme.standard() }

/**
 * 手写的空间/效果曲线，仅用于列表入场与数值交叉淡入。
 *
 * 路由过渡已经改用 Material 的 [MotionScheme]；这里保留较小的弹簧规格，避免
 * 大段列表入场时出现过冲。
 */
private fun <T> expressiveSpatialSpec(): FiniteAnimationSpec<T> =
    spring(dampingRatio = 0.8f, stiffness = 380f)

private fun <T> expressiveEffectsSpec(): FiniteAnimationSpec<T> =
    tween(durationMillis = 320, easing = FastOutSlowInEasing)

private fun <T> expressiveFastEffectsSpec(): FiniteAnimationSpec<T> =
    tween(durationMillis = 160, easing = FastOutSlowInEasing)

private fun <T> standardEffectsSpec(): FiniteAnimationSpec<T> =
    tween(durationMillis = 200, easing = FastOutSlowInEasing)

/**
 * 路由切换动画。
 *
 * FULL 使用容器变换（缩放 + 淡入淡出 + 弹簧空间曲线）；STANDARD 保留方向感但幅度更小；
 * MINIMAL 只做 90ms 透明度过渡；OFF 直接切换。
 *
 * 该函数在 `transitionSpec` 中调用（非 Composable），因此 [MotionScheme] 由调用方从
 * [LocalDevInfoMotionScheme] 读取后传入。
 */
fun devInfoRouteTransform(
    forward: Boolean,
    level: MotionLevel,
    scheme: MotionScheme,
): ContentTransform {
    val direction = if (forward) 1 else -1

    return when (level) {
        MotionLevel.FULL -> (
            fadeIn(scheme.fastEffectsSpec()) +
                scaleIn(initialScale = 0.92f, animationSpec = scheme.defaultSpatialSpec())
            ).togetherWith(
                fadeOut(scheme.defaultEffectsSpec()) +
                    scaleOut(targetScale = 0.90f, animationSpec = scheme.defaultSpatialSpec())
            )

        MotionLevel.STANDARD -> (
            fadeIn(scheme.defaultEffectsSpec()) +
                slideInHorizontally(scheme.defaultSpatialSpec<IntOffset>()) { direction * it / 4 }
            ).togetherWith(
                fadeOut(scheme.defaultEffectsSpec()) +
                    // 1/8 宽度在 0.5x 系统动画下几乎不可见；提高到 1/3 让退出方向可感知。
                    slideOutHorizontally(scheme.defaultSpatialSpec<IntOffset>()) { -direction * it / 3 }
            )

        MotionLevel.MINIMAL -> fadeIn(tween(90)) togetherWith fadeOut(tween(90))

        MotionLevel.OFF -> EnterTransition.None togetherWith ExitTransition.None
    }
}

/**
 * 列表分段的入场动画。
 *
 * 只在 FULL/STANDARD 生效；[index] 用于形成轻微错峰，低档位直接渲染以避免无意义的
 * 组合开销。
 */
@Composable
fun DevInfoStaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val level = LocalDevInfoMotionLevel.current
    if (!level.isAnimated) {
        content()
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val delayMillis = if (level == MotionLevel.FULL) index.coerceIn(0, 6) * 24 else 0

    val enter = when (level) {
        MotionLevel.FULL -> fadeIn(
            tween(durationMillis = 220, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 220,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing,
            )
        ) { it / 10 }

        MotionLevel.STANDARD -> fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 12 }

        // 精简档只保留短时透明度过渡，不做任何位移。
        MotionLevel.MINIMAL -> fadeIn(tween(90))

        MotionLevel.OFF -> EnterTransition.None
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = ExitTransition.None,
    ) {
        content()
    }
}

/** 数值变化时的轻量交叉淡入；关闭动效时退回普通文本。 */
@Composable
fun DevInfoAnimatedValue(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val level = LocalDevInfoMotionLevel.current
    if (!level.isAnimated) {
        Text(
            text = value,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
        )
        return
    }

    val enterSpec = when (level) {
        MotionLevel.FULL -> expressiveEffectsSpec<Float>()
        MotionLevel.STANDARD -> standardEffectsSpec<Float>()
        MotionLevel.MINIMAL -> tween<Float>(90)
        MotionLevel.OFF -> tween<Float>(0)
    }
    val exitSpec = when (level) {
        MotionLevel.FULL -> expressiveFastEffectsSpec<Float>()
        MotionLevel.STANDARD -> standardEffectsSpec<Float>()
        MotionLevel.MINIMAL -> tween<Float>(90)
        MotionLevel.OFF -> tween<Float>(0)
    }

    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            fadeIn(enterSpec) togetherWith fadeOut(exitSpec)
        },
        label = "devInfoAnimatedValue",
    ) { text ->
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}
