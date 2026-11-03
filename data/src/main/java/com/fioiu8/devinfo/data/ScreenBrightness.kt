/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data

import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * AOSP 的屏幕亮度量程。
 *
 * Android 没有公开"最大亮度设定值"的 API：`android.os.PowerManager` 不含亮度方法，
 * `DisplayManager.getBrightness(int, int)` 需要更新的版本且带额外的权限约束。因此这里
 * 只处理可以确认的 0..255 量程，其余情况一律判为不可用。
 */
private const val AOSP_MAX_BRIGHTNESS = 255

/**
 * 读取屏幕亮度百分比。
 *
 * [Settings.System.SCREEN_BRIGHTNESS] 是设备相关的原始量程（常见 0..255，也存在
 * 0..1023 / 0..2047 / 0..4095）。此前固定除以 255，在自定义量程设备上会得到超过 100%
 * 的结果（例如 802%），且与总览页做过钳制的实现互相矛盾。
 *
 * 量程无法确认时返回 null：宁可显示为不可用，也不给出错误的百分比。代价是在自定义量程
 * 的设备上该字段不再显示，这是有意为之的取舍。
 *
 * @return 0..100 的亮度百分比；读不到或量程超出已知范围时返回 null。
 */
internal fun readScreenBrightnessPercent(context: Context): Int? = runCatching {
    val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
    if (raw !in 0..AOSP_MAX_BRIGHTNESS) return@runCatching null
    (raw.toFloat() / AOSP_MAX_BRIGHTNESS * 100f).roundToInt().coerceIn(0, 100)
}.getOrNull()
