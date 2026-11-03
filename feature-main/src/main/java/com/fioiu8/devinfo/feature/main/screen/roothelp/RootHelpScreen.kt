/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main.screen.roothelp

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fioiu8.devinfo.feature.main.R

/**
 * 应用内的 Root 授权帮助页。
 *
 * The state and its Material 3 rendering stay separate so translated help content can change
 * without mixing it into the layout.
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

    RootHelpScreenMaterial(state, actions)
}

/**
 * 把多行字符串资源切成步骤列表。
 *
 * 丢弃空行，这样译文里多余的空行不会渲染成一个空步骤。
 */
private fun String.toSteps(): List<String> =
    lines().map(String::trim).filter(String::isNotEmpty)
