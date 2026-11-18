/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main.screen.about

import com.fioiu8.devinfo.feature.main.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.feature.main.DevInfoPageScaffold
import com.fioiu8.devinfo.ui.SegmentedGroup
import com.fioiu8.devinfo.ui.SegmentedRow

/**
 * 关于页内容：应用标识、版本与链接列表。
 *
 * 文本全部走类型阶梯 token（含 emphasized 变体），底部留白只按导航栏 inset 计算。
 */
@Composable
fun AboutScreenContent(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    DevInfoPageScaffold(
        title = state.title,
        showBackButton = true,
        onBack = actions.onBack,
    ) { innerPadding ->
        val bottomInset = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = bottomInset,
            ),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                    )
                    Text(
                        text = state.appName,
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                    )
                    Text(
                        text = state.versionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SegmentedGroup {
                    state.links.forEach { linkInfo ->
                        SegmentedRow(
                            title = linkInfo.fullText,
                            showChevron = true,
                            onClick = { actions.onOpenLink(linkInfo.url) },
                        )
                    }
                }
            }
        }
    }
}
