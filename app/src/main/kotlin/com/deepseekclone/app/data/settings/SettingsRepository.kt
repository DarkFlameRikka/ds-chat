package com.deepseekclone.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "deepseek_settings")

/** 用户本地设置：API Key、Base URL、模型、开关等。 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val THINKING = booleanPreferencesKey("thinking_enabled")
        val EFFORT = stringPreferencesKey("thinking_effort") // none | low | high | max
        val SEARCH = booleanPreferencesKey("search_enabled")
        val DARK_THEME = stringPreferencesKey("dark_theme") // light | dark | system（默认浅色）
        val FONT_SCALE = stringPreferencesKey("font_scale") // 兼容旧值
        val FONT_STEP = stringPreferencesKey("font_step") // 0..4
        val FOLLOW_SYS_FONT = booleanPreferencesKey("follow_system_font")
        val TTS_VOICE = stringPreferencesKey("tts_voice") // stella | tide
        val LANGUAGE = stringPreferencesKey("language") // zh
        val AUTO_READ = booleanPreferencesKey("auto_read_aloud")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[Keys.API_KEY] ?: "" }
    val baseUrl: Flow<String> = context.dataStore.data.map { it[Keys.BASE_URL] ?: "https://api.deepseek.com" }
    val model: Flow<String> = context.dataStore.data.map { it[Keys.MODEL] ?: "deepseek-chat" }
    val thinkingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.THINKING] ?: false }
    val effort: Flow<String> = context.dataStore.data.map { it[Keys.EFFORT] ?: "high" }
    val searchEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SEARCH] ?: false }
    val darkTheme: Flow<String> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: "light" }
    val fontScale: Flow<String> = context.dataStore.data.map { it[Keys.FONT_SCALE] ?: "standard" }
    val fontStep: Flow<String> = context.dataStore.data.map { it[Keys.FONT_STEP] ?: "2" }
    val followSystemFont: Flow<Boolean> = context.dataStore.data.map { it[Keys.FOLLOW_SYS_FONT] ?: false }
    val ttsVoice: Flow<String> = context.dataStore.data.map { it[Keys.TTS_VOICE] ?: "stella" }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "zh" }
    val autoReadAloud: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_READ] ?: false }

    suspend fun apiKeyFirst(): String = apiKey.first()
    suspend fun baseUrlFirst(): String = baseUrl.first()
    suspend fun modelFirst(): String = model.first()
    suspend fun thinkingFirst(): Boolean = thinkingEnabled.first()
    suspend fun searchFirst(): Boolean = searchEnabled.first()

    suspend fun setApiKey(value: String) = context.dataStore.edit { it[Keys.API_KEY] = value }
    suspend fun setBaseUrl(value: String) = context.dataStore.edit { it[Keys.BASE_URL] = value }
    suspend fun setModel(value: String) = context.dataStore.edit { it[Keys.MODEL] = value }
    suspend fun setThinkingEnabled(value: Boolean) = context.dataStore.edit { it[Keys.THINKING] = value }
    suspend fun setEffort(value: String) = context.dataStore.edit { it[Keys.EFFORT] = value }
    suspend fun effortFirst(): String = effort.first()
    suspend fun setSearchEnabled(value: Boolean) = context.dataStore.edit { it[Keys.SEARCH] = value }
    suspend fun setDarkTheme(value: String) = context.dataStore.edit { it[Keys.DARK_THEME] = value }
    suspend fun setFontScale(value: String) = context.dataStore.edit { it[Keys.FONT_SCALE] = value }
    suspend fun setFontStep(value: String) = context.dataStore.edit { it[Keys.FONT_STEP] = value }
    suspend fun setFollowSystemFont(value: Boolean) = context.dataStore.edit { it[Keys.FOLLOW_SYS_FONT] = value }
    suspend fun fontStepFirst(): String = fontStep.first()
    suspend fun followSystemFontFirst(): Boolean = followSystemFont.first()
    suspend fun setTtsVoice(value: String) = context.dataStore.edit { it[Keys.TTS_VOICE] = value }
    suspend fun setLanguage(value: String) = context.dataStore.edit { it[Keys.LANGUAGE] = value }
    suspend fun setAutoReadAloud(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_READ] = value }
    suspend fun ttsVoiceFirst(): String = ttsVoice.first()
}
