package com.deepseekclone.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepseekclone.app.ui.theme.LocalExtendedColors

/** 官方风格顶栏：左圆形菜单钮，中标题，右胶囊（语音/新建对话）。 */
@Composable
fun ChatTopBar(
    title: String,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    autoReadAloud: Boolean = false,
    onToggleAutoRead: () -> Unit = {},
) {
    val extended = LocalExtendedColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundSquareButton(onClick = onMenu) {
            TwoLineGlyph(tint = extended.iconTint)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .background(extended.cardBg, RoundedCornerShape(50))
                .border(0.8.dp, extended.cardBorder, RoundedCornerShape(50))
                .padding(horizontal = 5.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniIconButton(onClick = onToggleAutoRead) {
                Icon(
                    if (autoReadAloud) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                    contentDescription = if (autoReadAloud) "关闭自动朗读" else "开启自动朗读",
                    tint = if (autoReadAloud) MaterialTheme.colorScheme.primary else extended.iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            MiniIconButton(onClick = onNewChat) {
                NewChatCircleIcon(
                    tint = extended.iconTint,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/** 带返回键的简单顶栏（设置页、API Key 页）。 */
@Composable
fun SimpleTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val extended = LocalExtendedColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundSquareButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = extended.iconTint,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
fun RoundSquareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val extended = LocalExtendedColors.current
    Box(
        modifier = modifier
            .size(38.dp)
            .background(extended.cardBg, CircleShape)
            .border(1.dp, extended.cardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MiniIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 官方顶栏的「＝」两横线图标。 */
@Composable
fun TwoLineGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val stroke = size.height * 0.13f
        val radius = CornerRadius(stroke / 2f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, size.height * 0.30f),
            size = Size(size.width, stroke),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, size.height * 0.58f),
            size = Size(size.width, stroke),
            cornerRadius = radius,
        )
    }
}
