/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui.motion

import android.animation.ValueAnimator
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 系统是否允许动画。
 *
 * [ValueAnimator.areAnimatorsEnabled] 已覆盖「动画时长比例 = 0」；再读一次
 * [Settings.Global.ANIMATOR_DURATION_SCALE] 是为了兼容部分修改过默认值的 ROM。
 */
fun isSystemAnimationsEnabled(context: Context): Boolean {
    if (!ValueAnimator.areAnimatorsEnabled()) return false
    val scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)
    return scale > 0f
}

/**
 * 组合内可观察的系统动画开关。
 *
 * 开发者选项通常要在应用回到前台后才生效，因此在 ON_RESUME 重新读取即可，
 * 不需要常驻 ContentObserver。
 */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember(context) { mutableStateOf(isSystemAnimationsEnabled(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = isSystemAnimationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return enabled
}
