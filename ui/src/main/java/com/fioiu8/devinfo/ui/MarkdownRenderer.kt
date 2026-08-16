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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 简易 Markdown 渲染器。
 * 支持：# / ## / ### 标题、**粗体**、*斜体*、`行内代码`、
 * > 引用、--- 分隔线、1. 有序列表 / - 无序列表、普通段落。
 *
 * @param markdown 原始 Markdown 文本
 * @param modifier 修饰符
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    // 解析结果仅依赖 markdown 文本本身；不记忆的话对话框每次重组都会
    // 重新分词整个正文（parseBlocks 每行还要新建多个正则）
    val parsed = remember(markdown) { parseBlocks(markdown.lines()) }

    Column(modifier = modifier.fillMaxWidth()) {
        parsed.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> {
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Text(
                        text = parseInline(block.text),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleMedium
                            2 -> MaterialTheme.typography.titleSmall
                            else -> MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = when (block.level) {
                            1 -> FontWeight.Bold
                            2 -> FontWeight.SemiBold
                            else -> FontWeight.Medium
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                is MdBlock.Paragraph -> {
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    Text(
                        text = parseInline(block.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is MdBlock.BlockQuote -> {
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = parseInline(block.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                is MdBlock.Code -> {
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is MdBlock.HorizontalRule -> {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                is MdBlock.ListItem -> {
                    val prefix = if (block.ordered) "${block.index}." else "•"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = parseInline(block.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── 内联格式解析（**粗体**、*斜体*、`代码`） ──

/** 解析一行内的内联格式，返回 AnnotatedString */
@Composable
private fun parseInline(text: String) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            // **粗体**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // *斜体*（单星号，不与 ** 冲突时）
            text.startsWith("*", i) && !text.startsWith("**", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // `行内代码`
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.2f),
                            fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value - 1).sp
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

// ── 块级结构解析 ──

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class BlockQuote(val text: String) : MdBlock()
    data class Code(val text: String) : MdBlock()
    data object HorizontalRule : MdBlock()
    data class ListItem(val text: String, val ordered: Boolean, val index: Int) : MdBlock()
}

/** 将文本行解析为 MdBlock 列表 */
private fun parseBlocks(lines: List<String>): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i].trimEnd()

        // 空行
        if (line.isBlank()) {
            i++
            continue
        }

        // ### / ## / # 标题
        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(line.trimStart())
        if (headingMatch != null) {
            blocks.add(
                MdBlock.Heading(
                    level = headingMatch.groupValues[1].length,
                    text = headingMatch.groupValues[2]
                )
            )
            i++
            continue
        }

        // --- / *** 分隔线
        if (line.trimStart().matches(Regex("^[-*_]{3,}\\s*$"))) {
            blocks.add(MdBlock.HorizontalRule)
            i++
            continue
        }

        // > 引用
        if (line.trimStart().startsWith(">")) {
            val text = line.trimStart().removePrefix(">").trimStart()
            blocks.add(MdBlock.BlockQuote(text))
            i++
            continue
        }

        // ``` 代码块
        if (line.trimStart().startsWith("```")) {
            i++
            val codeLines = mutableListOf<String>()
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            i++ // skip closing ```
            blocks.add(MdBlock.Code(codeLines.joinToString("\n")))
            continue
        }

        // 有序列表：1. / 2.
        val orderedMatch = Regex("^(\\d+)\\.\\s+(.+)$").find(line.trimStart())
        if (orderedMatch != null) {
            blocks.add(
                MdBlock.ListItem(
                    text = orderedMatch.groupValues[2],
                    ordered = true,
                    index = orderedMatch.groupValues[1].toInt()
                )
            )
            i++
            continue
        }

        // 无序列表：- / *
        val unorderedMatch = Regex("^[-*]\\s+(.+)$").find(line.trimStart())
        if (unorderedMatch != null) {
            blocks.add(
                MdBlock.ListItem(
                    text = unorderedMatch.groupValues[1],
                    ordered = false,
                    index = 0
                )
            )
            i++
            continue
        }

        // 普通段落
        blocks.add(MdBlock.Paragraph(line.trimStart()))
        i++
    }

    return blocks
}
