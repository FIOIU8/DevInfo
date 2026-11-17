/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.core.privacy

/**
 * 默认需要遮罩的敏感字段。
 *
 * key 由 [com.fioiu8.devinfo.data.DeviceInfoCollector] 通过资源名生成，例如
 * `device_android_id`；新增标识符时必须同步登记，避免新字段以明文出现。
 */
object SensitiveKeys {
    private val sensitiveKeys = setOf(
        "device_android_id",
        "device_serial",
        "device_oaid",
        "device_aaid",
        "device_fingerprint",
        "device_host",
        "device_user",
    )

    fun isSensitive(key: String): Boolean = key in sensitiveKeys
}

/** 固定长度遮罩，不泄露原值长度。 */
const val MASKED_VALUE: String = "********"

/**
 * 返回页面上应展示的值。
 *
 * 只有在用户主动揭示后才返回原值；揭示状态本身不持久化。
 */
fun displayValue(value: String, key: String, revealed: Boolean): String =
    if (!revealed && SensitiveKeys.isSensitive(key)) MASKED_VALUE else value
