package com.deepseekclone.app.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.ui.theme.LocalExtendedColors

/**
 * 模型与思考等级面板：官方把「深度思考」收进模型选择里，
 * 这里提供 模型（快速/专家）× 思考强度（不思考/快速/标准/深度）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    currentModel: String,
    currentEffort: String,
    onSelectModel: (String) -> Unit,
    onSelectEffort: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 24.dp)) {
            SectionLabel("模型")
            OptionRow(
                title = "快速模式",
                subtitle = "deepseek-flash · DeepSeek-V4.1-Flash-1017",
                selected = !currentModel.startsWith("deepseek-v4-pro"),
                onClick = { onSelectModel("deepseek-flash") },
            )
            OptionRow(
                title = "专家模式",
                subtitle = "deepseek-v4-pro · DeepSeek-V4-Pro-0813",
                selected = currentModel.startsWith("deepseek-v4-pro"),
                onClick = { onSelectModel("deepseek-v4-pro") },
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel("思考等级")
            listOf(
                Triple("none", "不思考", "直接回答，最快"),
                Triple("low", "快速思考", "少量推理，适合简单问题"),
                Triple("high", "标准思考", "默认强度，兼顾速度与质量"),
                Triple("max", "深度思考", "最大化推理，复杂问题更可靠"),
            ).forEach { (key, title, subtitle) ->
                OptionRow(
                    title = title,
                    subtitle = subtitle,
                    selected = currentEffort == key,
                    onClick = { onSelectEffort(key) },
                )
            }
            Text(
                text = "思考等级会以 output_config.effort 传给模型；选择「不思考」等价关闭思考模式。",
                fontSize = 12.sp,
                color = extended.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = LocalExtendedColors.current.textSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = extended.textSecondary,
            )
        }
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
