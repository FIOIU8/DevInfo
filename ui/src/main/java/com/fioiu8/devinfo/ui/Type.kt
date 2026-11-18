/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.ui.theme

import androidx.compose.material3.Typography

/**
 * Material 3 Expressive 类型阶梯，直接采用库默认值。
 *
 * 之前这里逐条重写了 16 个基础样式并统一加 `SemiBold`，等于手工模拟强调样式：
 * 样式名仍是基础 token，语义错位；而库提供的 16 个 `*Emphasized` 变体因为没被覆盖，
 * 仍走库默认值，导致同一界面里出现两套字重规则。
 *
 * 现在基础样式保持 M3 规范字重，需要强调的标题/数值直接使用
 * `MaterialTheme.typography.titleLargeEmphasized` 这类 token，不再用
 * `fontWeight = FontWeight.Bold` 覆盖。
 */
val DevInfoTypography = Typography()
