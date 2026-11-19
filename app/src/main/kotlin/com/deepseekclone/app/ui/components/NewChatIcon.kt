package com.deepseekclone.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 官方「新建对话」图标：圆形描边 + 中间加号 + 左下角小尾巴（气泡形）。
 * Material 的 AddComment 是方形气泡，形状对不上，这里自绘。
 */
@Composable
fun NewChatCircleIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = 1.7.dp.toPx()
        val radius = size.minDimension / 2f - stroke
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        // 圆形主体（左下留缺口给尾巴）
        drawArc(
            color = tint,
            startAngle = 300f,
            sweepAngle = 330f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        // 左下角的小尾巴
        val tail = Path().apply {
            moveTo(center.x - radius * 0.55f, center.y + radius * 0.78f)
            lineTo(center.x - radius * 1.02f, center.y + radius * 1.02f)
            lineTo(center.x - radius * 0.12f, center.y + radius * 0.95f)
            close()
        }
        drawPath(tail, tint)

        // 中间的加号
        val arm = radius * 0.42f
        drawLine(
            color = tint,
            start = Offset(center.x - arm, center.y),
            end = Offset(center.x + arm, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(center.x, center.y - arm),
            end = Offset(center.x, center.y + arm),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
