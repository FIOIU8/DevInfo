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

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fioiu8.devinfo.core.model.UiStyle
import com.fioiu8.devinfo.feature.main.R
import com.fioiu8.devinfo.ui.theme.LocalUiStyle

/**
 * 应用内的 Root 授权帮助页。
 *
 * 与 `screen/about/` 保持同一套结构：一个组装状态的入口 + 两套风格实现，因此新增一节内容只需
 * 扩 [RootHelpUiState]，两套风格各自怎么排版由渲染层决定。
 *
 * 入口有两个：设置页「工具」区的一行，以及授权失败弹窗的「查看授权帮助」。
 */
@Composable
fun RootHelpScreen(
    onBack: () -> Unit,
) {
    val state = RootHelpUiState(
        title = stringResource(R.string.root_help_title),
        intro = stringResource(R.string.root_help_intro),
        sections = listOf(
            RootHelpSection(
                title = stringResource(R.string.root_help_ksu_title),
                steps = stringResource(R.string.root_help_ksu_steps).toSteps(),
                note = stringResource(R.string.root_help_ksu_note),
            ),
            RootHelpSection(
                title = stringResource(R.string.root_help_magisk_title),
                steps = stringResource(R.string.root_help_magisk_steps).toSteps(),
            ),
            RootHelpSection(
                title = stringResource(R.string.root_help_apatch_title),
                steps = stringResource(R.string.root_help_apatch_steps).toSteps(),
            ),
        ),
    )
    val actions = RootHelpScreenActions(onBack = onBack)

    when (LocalUiStyle.current) {
        UiStyle.MIUIX -> RootHelpScreenMiuix(state, actions)
        UiStyle.MATERIAL3 -> RootHelpScreenMaterial(state, actions)
    }
}

/**
 * 把多行字符串资源切成步骤列表。
 *
 * 丢弃空行，这样译文里多余的空行不会渲染成一个空步骤。
 */
private fun String.toSteps(): List<String> =
    lines().map(String::trim).filter(String::isNotEmpty)
