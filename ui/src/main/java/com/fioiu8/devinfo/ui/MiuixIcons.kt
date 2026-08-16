package com.fioiu8.devinfo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自定义 Miuix 图标 — 替代 miuix-icons 库。
 * 仅包含项目实际使用的图标。
 */
object CustomMiuixIcons {
    val Back: ImageVector by lazy {
        ImageVector.Builder(
            name = "Back",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.41f, 7.41f)
                lineTo(14f, 6f)
                lineTo(8f, 12f)
                lineTo(14f, 18f)
                lineTo(15.41f, 16.59f)
                lineTo(10.83f, 12f)
                close()
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.14f, 12.94f)
                curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12.0f)
                curveTo(19.2f, 11.67f, 19.18f, 11.36f, 19.14f, 11.06f)
                lineTo(21.16f, 9.48f)
                curveTo(21.34f, 9.34f, 21.39f, 9.07f, 21.28f, 8.87f)
                lineTo(19.36f, 5.55f)
                curveTo(19.25f, 5.35f, 18.99f, 5.27f, 18.77f, 5.35f)
                lineTo(16.38f, 6.31f)
                curveTo(15.88f, 5.93f, 15.35f, 5.61f, 14.76f, 5.37f)
                lineTo(14.4f, 2.83f)
                curveTo(14.36f, 2.61f, 14.16f, 2.44f, 13.93f, 2.44f)
                lineTo(10.07f, 2.44f)
                curveTo(9.84f, 2.44f, 9.64f, 2.61f, 9.6f, 2.83f)
                lineTo(9.24f, 5.37f)
                curveTo(8.65f, 5.61f, 8.12f, 5.93f, 7.62f, 6.31f)
                lineTo(5.23f, 5.35f)
                curveTo(5.01f, 5.27f, 4.75f, 5.35f, 4.64f, 5.55f)
                lineTo(2.72f, 8.87f)
                curveTo(2.61f, 9.07f, 2.66f, 9.34f, 2.84f, 9.48f)
                lineTo(4.86f, 11.06f)
                curveTo(4.82f, 11.36f, 4.8f, 11.67f, 4.8f, 12.0f)
                curveTo(4.8f, 12.33f, 4.82f, 12.64f, 4.86f, 12.94f)
                lineTo(2.84f, 14.52f)
                curveTo(2.66f, 14.66f, 2.61f, 14.93f, 2.72f, 15.13f)
                lineTo(4.64f, 18.45f)
                curveTo(4.75f, 18.65f, 5.01f, 18.73f, 5.23f, 18.65f)
                lineTo(7.62f, 17.69f)
                curveTo(8.12f, 18.07f, 8.65f, 18.39f, 9.24f, 18.63f)
                lineTo(9.6f, 21.17f)
                curveTo(9.64f, 21.39f, 9.84f, 21.56f, 10.07f, 21.56f)
                lineTo(13.93f, 21.56f)
                curveTo(14.16f, 21.56f, 14.36f, 21.39f, 14.4f, 21.17f)
                lineTo(14.76f, 18.63f)
                curveTo(15.35f, 18.39f, 15.88f, 18.07f, 16.38f, 17.69f)
                lineTo(18.77f, 18.65f)
                curveTo(18.99f, 18.73f, 19.25f, 18.65f, 19.36f, 18.45f)
                lineTo(21.28f, 15.13f)
                curveTo(21.39f, 14.93f, 21.34f, 14.66f, 21.16f, 14.52f)
                close()
                moveTo(12.0f, 15.6f)
                curveTo(10.02f, 15.6f, 8.4f, 13.98f, 8.4f, 12.0f)
                curveTo(8.4f, 10.02f, 10.02f, 8.4f, 12.0f, 8.4f)
                curveTo(13.98f, 8.4f, 15.6f, 10.02f, 15.6f, 12.0f)
                curveTo(15.6f, 13.98f, 13.98f, 15.6f, 12.0f, 15.6f)
                close()
            }
        }.build()
    }
}
