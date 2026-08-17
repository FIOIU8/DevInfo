package com.fioiu8.devinfo.ui.kit.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import com.fioiu8.devinfo.ui.kit.modifier.inspectDragGestures
import kotlin.math.abs

/**
 * 优化版 DampedDragAnimation — 保留 5 个 Animatable 但优化协程调度。
 * 改进：
 * - press/release 从 5 个独立协程 → 1 个 coroutineScope 内并发
 * - release 中 snapshotFlow 阻塞 → awaitFrame 轮询
 * - updateValue 中 velocity 更新不单独启动协程
 */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec =
        spring(1f, 1000f, visibilityThreshold)
    private val pressProgressAnimationSpec =
        spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec =
        spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    var velocity: Float = 0f
        private set

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            }
        ) { change, dragAmount ->
            val position = change.position
            val previousPosition = change.previousPosition
            val isInside = canDrag(position)
            val wasInside = canDrag(previousPosition)
            if (isInside && wasInside) {
                onDrag(size, dragAmount)
            }
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            // 单次启动，内部并发，减少调度开销
            kotlinx.coroutines.coroutineScope {
                launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
                launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
            }
        }
    }

    fun release() {
        animationScope.launch {
            // 用 awaitFrame 轮询替代 snapshotFlow.first()，避免 Flow 创建开销
            if (abs(value - targetValue) >= (valueRange.endInclusive - valueRange.start) * 0.025f) {
                while (abs(valueAnimation.value - valueAnimation.targetValue) >=
                    (valueRange.endInclusive - valueRange.start) * 0.025f
                ) {
                    awaitFrame()
                }
            }
            kotlinx.coroutines.coroutineScope {
                launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
                launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
            }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            // 启动 value 动画，completion 中直接更新 velocity（不额外启动协程）
            valueAnimation.animateTo(targetValue, valueAnimationSpec) {
                // 动画帧回调中直接同步更新 velocity
                velocityTracker.addPosition(
                    System.currentTimeMillis(),
                    Offset(valueAnimation.value, 0f)
                )
                val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
                // 帧回调非挂起上下文：直接同步更新 velocity 属性，
                // 避免每帧新启动协程以及多个 animateTo 在 velocity 上相互打断
                this@DampedDragAnimation.velocity = targetVelocity
            }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                valueAnimation.animateTo(targetValue, valueAnimationSpec)
                if (velocity != 0f) {
                    velocity = 0f
                }
                release()
            }
        }
    }
}
