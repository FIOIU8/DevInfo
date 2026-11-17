/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Android 14+ 的截图/录屏检测封装。
 *
 * 仅在关闭防截屏时注册：`FLAG_SECURE` 阻止捕获时系统不会回调，检测与防截屏
 * 天然互斥。实例只在 API 34+ 分支创建，避免旧系统加载新 API 类型。
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class SystemScreenshotGuard(
    private val activity: Activity,
    private val onCaptured: () -> Unit,
) {
    private var registered = false
    private var callback: Activity.ScreenCaptureCallback? = null

    fun setEnabled(enabled: Boolean) {
        if (enabled == registered) return
        if (enabled) {
            val captureCallback = callback
                ?: Activity.ScreenCaptureCallback { onCaptured() }.also { callback = it }
            activity.registerScreenCaptureCallback(activity.mainExecutor, captureCallback)
        } else {
            callback?.let(activity::unregisterScreenCaptureCallback)
        }
        registered = enabled
    }
}
