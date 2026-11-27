package com.witness.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.witness.core.net.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.witnessPrefs by preferencesDataStore("witness_settings")

class SettingsRepo(private val context: Context) {
    companion object {
        /** Дефолты сборки (dev-пресет). Задаются в WitnessApp до первого чтения. */
        var preset: ApiConfig = ApiConfig()
    }
    private val prefs get() = context.witnessPrefs
    private val kAsrUrl = stringPreferencesKey("asr_url")
    private val kAsrModel = stringPreferencesKey("asr_model")
    private val kLlmUrl = stringPreferencesKey("llm_url")
    private val kLlmModel = stringPreferencesKey("llm_model")
    private val kKey = stringPreferencesKey("api_key")
    private val kAsrKey = stringPreferencesKey("asr_key")
    private val kLlmKey = stringPreferencesKey("llm_key")
    private val kLang = stringPreferencesKey("lang")

    val lang: Flow<String> = prefs.data.map { it[kLang] ?: "en" }

    val config: Flow<ApiConfig> = prefs.data.map {
        ApiConfig(
            it[kAsrUrl] ?: preset.asrUrl,
            it[kAsrModel] ?: preset.asrModel,
            it[kLlmUrl] ?: preset.llmUrl,
            it[kLlmModel] ?: preset.llmModel,
            it[kKey] ?: preset.apiKey,
            it[kAsrKey] ?: it[kKey] ?: preset.apiKey,
            it[kLlmKey] ?: it[kKey] ?: preset.apiKey,
        )
    }

    suspend fun config(): ApiConfig = config.first()

    suspend fun save(asrUrl: String, asrModel: String, llmUrl: String, llmModel: String, key: String, asrKey: String = key, llmKey: String = key) {
        prefs.edit {
            it[kAsrUrl] = asrUrl; it[kAsrModel] = asrModel
            it[kLlmUrl] = llmUrl; it[kLlmModel] = llmModel; it[kKey] = key
            it[kAsrKey] = asrKey; it[kLlmKey] = llmKey
        }
    }

    suspend fun setLang(lang: String) { prefs.edit { it[kLang] = lang } }
}
