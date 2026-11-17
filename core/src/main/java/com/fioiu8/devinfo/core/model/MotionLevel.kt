/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.core.model

/**
 * 用户可选的动效强度。
 *
 * - [FULL]：M3 Expressive 弹簧与容器变换
 * - [STANDARD]：较克制的弹簧与淡入淡出
 * - [MINIMAL]：仅短时透明度过渡，无位移/缩放
 * - [OFF]：无动画
 */
enum class MotionLevel {
    FULL,
    STANDARD,
    MINIMAL,
    OFF;

    val isAnimated: Boolean
        get() = this != OFF
}

/**
 * 系统「移除动画」必须覆盖用户设置：无障碍偏好优先于应用内选项。
 */
fun effectiveMotionLevel(level: MotionLevel, systemAnimationsEnabled: Boolean): MotionLevel =
    if (systemAnimationsEnabled) level else MotionLevel.OFF
