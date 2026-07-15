package com.fioiu8.devinfo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fioiu8.devinfo.R

/** 纯色预览块，用于主题色选择器 */
@Composable
fun ColorPreview(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    cornerRadius: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color)
    )
}

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
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = when (valueAlignment) {
                Alignment.Start -> TextAlign.Start
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                null -> TextAlign.End
                else -> TextAlign.End
            },
            modifier = Modifier.weight(1f, fill = false)
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
    val bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    val textColor = MaterialTheme.colorScheme.error

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = stringResource(R.string.warning_icon_desc),
                modifier = Modifier.size(24.dp),
                tint = textColor
            )
            Column {
                Text(
                    text = stringResource(R.string.test_version_warning),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = stringResource(R.string.test_version_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}
