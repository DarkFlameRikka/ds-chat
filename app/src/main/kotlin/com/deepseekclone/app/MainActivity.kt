package com.deepseekclone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.deepseekclone.app.ui.AppRoot
import com.deepseekclone.app.ui.theme.DeepSeekTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as App
        setContent {
            val mode by app.container.settingsRepository.darkTheme.collectAsState(initial = "light")
            val fontStep by app.container.settingsRepository.fontStep.collectAsState(initial = "2")
            val followSystemFont by app.container.settingsRepository.followSystemFont.collectAsState(initial = false)
            val dark = when (mode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val fontScale = if (followSystemFont) 1f else com.deepseekclone.app.ui.screens.settings.fontScaleOf(fontStep)
            DeepSeekTheme(darkTheme = dark, fontScale = fontScale) {
                AppRoot(container = app.container)
            }
        }
    }
}
