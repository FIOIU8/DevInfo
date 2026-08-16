package com.fioiu8.devinfo.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

/**
 * 创建一个可选的模糊背景层，供底部导航栏等元素采样。
 *
 * 当 blur 开启且设备支持 RenderEffect（API 31+）时，返回一个
 * [LayerBackdrop]：先以 surface 颜色铺底，再捕获子内容。
 * 否则返回 null，调用方应跳过模糊效果。
 */
@Composable
fun rememberBlurBackdrop(enableBlur: Boolean, surfaceColor: Color): LayerBackdrop? {
    if (!enableBlur || Build.VERSION.SDK_INT < 31) return null
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 毛玻璃容器 — 在内容外层应用 [textureBlur] 模糊效果。
 *
 * 从 [backdrop] 中采样已捕获的页面内容，应用高斯模糊后叠加
 * 87% 透明度的表面色，作为子组件的背景。子组件（导航栏图标/文字）
 * 绘制在模糊背景之上，形成真实的毛玻璃效果。
 *
 * @param backdrop 模糊采样的背景层（null 则跳过模糊）
 * @param blurActive 是否激活模糊
 * @param content 子内容
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    surfaceColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (blurActive && backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 15f,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = surfaceColor.copy(alpha = 0.87f)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}
