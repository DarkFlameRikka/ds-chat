package com.deepseekclone.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.ui.components.SimpleTopBar
import com.deepseekclone.app.ui.theme.LocalExtendedColors

/** 申请 API Key 教程：7 步图文 + 底部跳转官方开放平台。 */
@Composable
fun ApiKeyGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val extended = LocalExtendedColors.current
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(title = "申请 API Key", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = extended.cardBg,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, extended.cardBorder),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3 分钟拿到 API Key",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "先在开放平台充值（API 按量计费），再创建并复制 API Key，回到本应用粘贴即可。",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = extended.textSecondary,
                    )
                }
            }

            STEPS.forEachIndexed { index, step ->
                StepCard(index = index + 1, step = step)
            }
            Spacer(Modifier.height(12.dp))
        }

        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding()) {
                Button(
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("跳转至官方地址申请 API Key", fontSize = 16.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "platform.deepseek.com/api_keys",
                    fontSize = 12.sp,
                    color = extended.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("即将跳转") },
            text = {
                Text(
                    "将打开浏览器访问 DeepSeek 开放平台（platform.deepseek.com/api_keys）申请 API Key。\n\n" +
                        "API 按量计费，请先完成充值；Key 只在创建时显示一次，请及时复制保存。",
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.deepseek.com/api_keys")),
                        )
                    }.onFailure {
                        android.widget.Toast.makeText(
                            context,
                            "未找到可用的浏览器，请手动访问 platform.deepseek.com/api_keys",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                }) { Text("确认跳转") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } },
        )
    }
}

private data class GuideStep(
    val title: String,
    val caption: String,
    val imageRes: Int,
)

private val STEPS = listOf(
    GuideStep(
        "充值余额",
        "打开 DeepSeek 开放平台，进入「用量信息」，点右侧「去充值」。",
        com.deepseekclone.app.R.drawable.apikey_step1,
    ),
    GuideStep(
        "选择充值金额",
        "选「在线充值」，点选金额（或自定义），选择支付宝 / 微信支付后点「去支付」。",
        com.deepseekclone.app.R.drawable.apikey_step2,
    ),
    GuideStep(
        "打开侧边栏",
        "充值完成后，点左上角菜单图标展开侧边栏。",
        com.deepseekclone.app.R.drawable.apikey_step3,
    ),
    GuideStep(
        "进入 API keys",
        "在侧边栏点「API keys」。",
        com.deepseekclone.app.R.drawable.apikey_step4,
    ),
    GuideStep(
        "创建 API Key",
        "在 API keys 页面点「创建 API key」。",
        com.deepseekclone.app.R.drawable.apikey_step5,
    ),
    GuideStep(
        "填写名称",
        "给 Key 起个名字（数字、字母、中文都可以），点「创建」。",
        com.deepseekclone.app.R.drawable.apikey_step6,
    ),
    GuideStep(
        "复制并保存",
        "创建成功后立即点「复制」——Key 只显示这一次，丢失需重新创建。回到本应用设置里粘贴即可。",
        com.deepseekclone.app.R.drawable.apikey_step7,
    ),
)

@Composable
private fun StepCard(index: Int, step: GuideStep) {
    val extended = LocalExtendedColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = extended.cardBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, extended.cardBorder),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$index",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = step.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = step.caption,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = extended.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Image(
                painter = painterResource(id = step.imageRes),
                contentDescription = step.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
            )
        }
    }
}
