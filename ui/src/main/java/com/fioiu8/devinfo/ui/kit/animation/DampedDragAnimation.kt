package com.fioiu8.devinfo.ui.kit.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
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

/** 打包 5 个动画值为单一状态，减少 5 个 Animatable 为 1 个 */
data class DragState(
    val value: Float,
    val velocity: Float,
    val pressProgress: Float,
    val scaleX: Float,
    val scaleY: Float,
)

/** 5 维动画向量，用于 Animatable<DragState> 的底层存储 */
class DragStateVector(
    var v1: Float = 0f,
    var v2: Float = 0f,
    var v3: Float = 0f,
    var v4: Float = 0f,
    var v5: Float = 0f,
) : AnimationVector {
    override val size: Int = 5

    override operator fun get(index: Int): Float = when (index) {
        0 -> v1; 1 -> v2; 2 -> v3; 3 -> v4; 4 -> v5; else -> 0f
    }

    override operator fun set(index: Int, value: Float) {
        when (index) {
            0 -> v1 = value; 1 -> v2 = value; 2 -> v3 = value
            3 -> v4 = value; 4 -> v5 = value
        }
    }

    override fun reset() { v1 = 0f; v2 = 0f; v3 = 0f; v4 = 0f; v5 = 0f }

    override fun newInstance(): DragStateVector = DragStateVector()

    override fun toString(): String =
        "DragStateVector(v1=$v1, v2=$v2, v3=$v3, v4=$v4, v5=$v5)"
}

/** DragState ↔ DragStateVector 双向转换 */
private val DragStateConverter = object : TwoWayConverter<DragState, DragStateVector> {
    override val convertFromVector: (DragStateVector) -> DragState = {
        DragState(it.v1, it.v2, it.v3, it.v4, it.v5)
    }
    override val convertToVector: (DragState) -> DragStateVector = {
        DragStateVector(it.value, it.velocity, it.pressProgress, it.scaleX, it.scaleY)
    }
}

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

    private val animationSpec = spring(1f, 1000f, visibilityThreshold)
    private val pressSpec = spring(1f, 1000f, 0.001f)
    private val scaleSpec = spring(0.65f, 250f, 0.001f)
    private val velocitySpec = spring(0.5f, 300f, visibilityThreshold * 10f)

    private val animation = Animatable(
        DragState(
            value = initialValue,
            velocity = 0f,
            pressProgress = 0f,
            scaleX = initialScale,
            scaleY = initialScale
        ),
        DragStateConverter
    )

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = animation.value.value
    val targetValue: Float get() = animation.targetValue.value
    val pressProgress: Float get() = animation.value.pressProgress
    val scaleX: Float get() = animation.value.scaleX
    val scaleY: Float get() = animation.value.scaleY
    val velocity: Float get() = animation.value.velocity

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
            animation.animateTo(
                DragState(
                    value = animation.value.value,
                    velocity = animation.value.velocity,
                    pressProgress = 1f,
                    scaleX = pressedScale,
                    scaleY = pressedScale
                ),
                pressSpec
            )
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (abs(value - targetValue) >= (valueRange.endInclusive - valueRange.start) * 0.025f) {
                // 等待 value 接近 targetValue
                while (abs(animation.value.value - animation.targetValue.value) >=
                    (valueRange.endInclusive - valueRange.start) * 0.025f
                ) {
                    awaitFrame()
                }
            }
            animation.animateTo(
                DragState(
                    value = animation.value.value,
                    velocity = 0f,
                    pressProgress = 0f,
                    scaleX = initialScale,
                    scaleY = initialScale
                ),
                pressSpec
            )
        }
    }

    fun updateValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange)
        animationScope.launch {
            animation.suspendAndSetDragStateTarget(
                targetValue = clamped,
                velocity = animation.value.velocity,
                pressProgress = animation.value.pressProgress,
                scaleX = animation.value.scaleX,
                scaleY = animation.value.scaleY,
                spec = animationSpec,
                onVelocity = { updateVelocity() }
            )
        }
    }

    fun animateToValue(newValue: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val clamped = newValue.coerceIn(valueRange)
                animation.animateTo(
                    DragState(
                        value = clamped,
                        velocity = 0f,
                        pressProgress = animation.value.pressProgress,
                        scaleX = animation.value.scaleX,
                        scaleY = animation.value.scaleY
                    ),
                    animationSpec
                )
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch {
            animation.animateTo(
                DragState(
                    value = animation.value.value,
                    velocity = targetVelocity,
                    pressProgress = animation.value.pressProgress,
                    scaleX = animation.value.scaleX,
                    scaleY = animation.value.scaleY
                ),
                velocitySpec
            )
        }
    }
}

/** 扩展函数：在动画运行中仅更新部分字段，保持其他字段不变 */
private suspend fun Animatable<DragState, DragStateVector>.suspendAndSetDragStateTarget(
    targetValue: Float,
    velocity: Float,
    pressProgress: Float,
    scaleX: Float,
    scaleY: Float,
    spec: androidx.compose.animation.core.AnimationSpec<DragState>,
    onVelocity: () -> Unit,
) {
    animateTo(
        DragState(targetValue, velocity, pressProgress, scaleX, scaleY),
        spec,
        blocking = false
    )
    onVelocity()
}
