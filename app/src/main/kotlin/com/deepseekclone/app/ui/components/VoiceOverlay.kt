package com.deepseekclone.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin

/**
 * 按住说话时的「局部」渐变面板（官方样式）：只覆盖屏幕下方约一半，
 * 上半屏仍可见当前会话内容；渐变以手指位置为中心，上缘圆角并淡出。
 */
@Composable
fun VoiceGradientOverlay(
    finger: Offset,
    cancelled: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "voice-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val base = if (cancelled) Color(0xFFE85C5C) else Color(0xFF1E6BFF)
    val light = if (cancelled) Color(0xFFFF9C9C) else Color(0xFF7FB2FF)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val panelHeightPx = with(density) { (maxHeight * 0.46f).toPx() }
        val panelTopPx = with(density) { maxHeight.toPx() } - panelHeightPx
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        ) {
            val localFinger = Offset(finger.x, (finger.y - panelTopPx).coerceIn(0f, panelHeightPx))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = if (finger == Offset.Zero) {
                    Offset(size.width / 2f, size.height * 0.85f)
                } else {
                    localFinger
                }
                // 上缘淡出 + 手指处径向高光
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to base.copy(alpha = 0f),
                        0.28f to base.copy(alpha = 0.55f),
                        0.6f to base,
                        1f to base,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        0f to light.copy(alpha = 0.9f),
                        0.45f to light.copy(alpha = 0.35f),
                        1f to light.copy(alpha = 0f),
                        center = center,
                        radius = size.width * 0.7f,
                    ),
                )
                if (finger != Offset.Zero) {
                    drawCircle(Color.White.copy(alpha = 0.28f), radius = 40.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = 0.14f), radius = 26.dp.toPx(), center = center)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (cancelled) "松开取消" else "松手发送，上滑取消",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(46.dp))
                Waveform(phase = phase, cancelled = cancelled)
            }
        }
    }
}

/** 录音波形：居中的一排竖条，按正弦相位起伏。 */
@Composable
private fun Waveform(phase: Float, cancelled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bars = 40
        repeat(bars) { index ->
            val distance = abs(index - bars / 2) / (bars / 2f)
            val amplitude = (1f - distance * 0.7f).coerceIn(0.25f, 1f)
            val wave = (sin(phase + index * 0.55f) * 0.5f + 0.5f)
            val barHeight = (6 + wave * amplitude * 40).dp
            Canvas(modifier = Modifier.height(barHeight).fillMaxWidth(0.009f)) {
                drawLine(
                    color = Color.White.copy(alpha = 0.55f + wave * 0.45f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = size.width,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

/** 转写中的轻提示层。 */
@Composable
fun TranscribingOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 8.dp,
        ) {
            Text(
                text = "正在识别语音…",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
