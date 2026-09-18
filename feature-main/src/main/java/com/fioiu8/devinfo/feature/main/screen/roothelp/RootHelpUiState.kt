/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.fioiu8.devinfo.feature.main.screen.roothelp

import androidx.compose.runtime.Immutable

/**
 * Root 授权帮助页的内容模型。
 *
 * 页面只讲「怎么在管理器里允许 DevInfo」，**不做任何外部跳转**：各家管理器界面差异太大，
 * 跳错地方比不跳更糟，而这里描述的步骤在它们的超级用户列表里都能走通。
 */
@Immutable
data class RootHelpUiState(
    val title: String,
    val intro: String,
    val sections: List<RootHelpSection>,
)

/** 一个管理器（KernelSU / Magisk / APatch）的步骤块。 */
@Immutable
data class RootHelpSection(
    val title: String,
    /** 有序步骤文案；序号由渲染层生成，文案里不含编号。 */
    val steps: List<String>,
    /** 可选提示，用于放置「管理器列表是缓存的」这类容易踩的坑。 */
    val note: String? = null,
)

/** 帮助页的回调集合。 */
@Immutable
data class RootHelpScreenActions(
    val onBack: () -> Unit,
)
