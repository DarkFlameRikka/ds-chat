package com.deepseekclone.app.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.components.SimpleTopBar
import com.deepseekclone.app.ui.theme.LocalExtendedColors
import kotlinx.coroutines.launch

/** 字体大小页：预览区 + 跟随系统开关 + 分档滑杆（对齐官方 2.8.x）。 */
@Composable
fun FontSizeScreen(
    settings: com.deepseekclone.app.data.settings.SettingsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val extended = LocalExtendedColors.current
    val stepKey by settings.fontStep.collectAsState(initial = "2")
    val followSystem by settings.followSystemFont.collectAsState(initial = false)
    val step = (stepKey.toIntOrNull() ?: 2).coerceIn(0, FONT_LABELS.lastIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(title = Zh.fontSize, onBack = onBack)
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = extended.cardBg,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                // 预览卡片
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = extended.cardBg,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, extended.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Surface(shape = RoundedCornerShape(14.dp), color = extended.chipBg) {
                                Text(
                                    text = "预览文字大小",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "你可以拖动滑块来调整字体大小。",
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "如果在使用过程中存在问题或建议，可反馈给 DeepSeek 团队。",
                            fontSize = 17.sp,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("跟随系统", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = followSystem,
                        onCheckedChange = { enabled -> scope.launch { settings.setFollowSystemFont(enabled) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp),
                ) {
                    Text(
                        text = FONT_LABELS[step],
                        fontSize = 13.sp,
                        color = extended.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    StepSlider(
                        steps = FONT_LABELS.size,
                        value = step,
                        enabled = !followSystem,
                        onChange = { index ->
                            scope.launch { settings.setFontStep(index.toString()) }
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    Text("A", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "字号会同步应用到聊天正文与界面文字。",
                    fontSize = 12.sp,
                    color = extended.textSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

val FONT_LABELS = listOf("小", "较小", "标准", "较大", "大")
val FONT_SCALES = listOf(0.85f, 0.92f, 1.0f, 1.10f, 1.22f)

fun fontLabelOf(step: String): String {
    val index = (step.toIntOrNull() ?: 2).coerceIn(0, FONT_LABELS.lastIndex)
    return FONT_LABELS[index]
}

fun fontScaleOf(step: String): Float {
    val index = (step.toIntOrNull() ?: 2).coerceIn(0, FONT_LABELS.lastIndex)
    return FONT_SCALES[index]
}


/** 官方样式的分档滑杆：细灰轨道 + 刻度 + 白色圆形滑块。 */
@Composable
private fun StepSlider(
    steps: Int,
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxIndex = (steps - 1).coerceAtLeast(1)
    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .semantics {
                contentDescription = "字体大小滑块"
                stateDescription = FONT_LABELS[value.coerceIn(0, FONT_LABELS.lastIndex)]
                setProgress { target ->
                    onChange((target * maxIndex).roundToInt().coerceIn(0, maxIndex))
                    true
                }
            }
            .pointerInput(enabled, steps) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val stepPx = w / maxIndex
                    onChange((offset.x / stepPx).roundToInt().coerceIn(0, maxIndex))
                }
            }
            .pointerInput(enabled, steps) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val w = size.width.toFloat()
                    val stepPx = w / maxIndex
                    onChange((change.position.x / stepPx).roundToInt().coerceIn(0, maxIndex))
                }
            },
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val stepPx = widthPx / maxIndex
        val trackColor = Color(0xFFDDDDDD)
        val knobRadius = 11.dp
        fun indexAt(x: Float): Int = (x / stepPx).roundToInt().coerceIn(0, maxIndex)
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val cy = size.height / 2f
            drawLine(trackColor, Offset(0f, cy), Offset(size.width, cy), 1.5.dp.toPx())
            repeat(steps) { index ->
                val x = stepPx * index
                drawLine(
                    color = trackColor,
                    start = Offset(x, cy - 7.dp.toPx()),
                    end = Offset(x, cy + 7.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            val knobX = (stepPx * value).coerceIn(knobRadius.toPx(), size.width - knobRadius.toPx())
            drawCircle(Color.White.copy(alpha = if (enabled) 1f else 0.6f), knobRadius.toPx(), Offset(knobX, cy))
            drawCircle(trackColor, knobRadius.toPx(), Offset(knobX, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
    }
}
