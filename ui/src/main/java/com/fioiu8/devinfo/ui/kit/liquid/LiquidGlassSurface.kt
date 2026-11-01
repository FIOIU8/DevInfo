/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.fioiu8.devinfo.ui.kit.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

private val LocalLiquidGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Provides a page background layer that glass surfaces can sample without sampling themselves. */
@Composable
fun LiquidGlassBackdropHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .layerBackdrop(backdrop),
        )
        CompositionLocalProvider(LocalLiquidGlassBackdrop provides backdrop) {
            content()
        }
    }
}

/** Material 3 card content with a translucent glass surface and an opaque fallback. */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.liquidGlassSurface(shape, containerColor),
        content = content,
    )
}

/** Material 3 icon button with a compact glass surface and the standard touch target. */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .liquidGlassSurface(
                    shape = CircleShape,
                    fallbackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}

/** Applies the shared liquid glass treatment while retaining a Material 3 fallback surface. */
@Composable
fun Modifier.liquidGlassSurface(
    shape: Shape,
    fallbackColor: Color,
): Modifier {
    val backdrop = LocalLiquidGlassBackdrop.current
    if (backdrop == null) {
        return clip(shape).background(fallbackColor, shape)
    }

    return clip(shape).drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(4.dp.toPx())
            lens(10.dp.toPx(), 12.dp.toPx())
        },
        highlight = {
            Highlight(
                width = 0.65.dp,
                blurRadius = 0.3.dp,
                alpha = 0.4f,
            )
        },
        shadow = { null },
        onDrawSurface = {
            drawRect(fallbackColor.copy(alpha = 0.84f))
        },
    )
}
