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

package com.fioiu8.devinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分类标题文本 */
@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

/**
 * 通用信息行组件（标签-值）。
 *
 * @param label 左侧标签
 * @param value 右侧值文本
 * @param labelColor 标签自定义颜色，默认跟随主题
 * @param valueColor 值文本自定义颜色，默认跟随主题
 * @param valueAlignment 值文本对齐方式，默认为右对齐
 * @param icon 标签左侧图标，可选
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    labelColor: Color = Color.Unspecified,
    valueColor: Color = Color.Unspecified,
    valueAlignment: Alignment.Horizontal? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = if (labelColor != Color.Unspecified) labelColor
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (labelColor != Color.Unspecified) labelColor
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (valueColor != Color.Unspecified) valueColor
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = when (valueAlignment) {
                Alignment.Start -> TextAlign.Start
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                null -> TextAlign.Start
                else -> TextAlign.Start
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 测试/非官方版本警告卡片，显示在 TopAppBar 下方。
 *
 * @param modifier 修饰符
 */
@Composable
fun TestVersionWarningCard(
    modifier: Modifier = Modifier,
    versionName: String = "",
    buildType: String = "dev"
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixCard(modifier = modifier.padding(horizontal = 12.dp)) {
            MiuixBasicComponent(
                title = stringResource(R.string.test_version_warning),
                summary = stringResource(R.string.test_version_desc),
                startAction = {
                    MiuixIcon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = stringResource(R.string.warning_icon_desc),
                        tint = MiuixTheme.colorScheme.error,
                    )
                },
            )
        }
        return
    }

    val bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = stringResource(R.string.warning_icon_desc),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.test_version_warning),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    if (versionName.isNotBlank()) {
                        Text(
                            text = "$versionName · $buildType",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.test_version_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
