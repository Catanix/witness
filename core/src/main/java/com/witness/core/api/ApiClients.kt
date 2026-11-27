package com.witness.core.api

/** Результат транскрибации: текст + предложения с таймкодами (мс). */
data class AsrSentence(val sentenceId: Int, val beginMs: Long, val endMs: Long, val text: String)
data class AsrResult(val text: String, val sentences: List<AsrSentence>, val durationSec: Int)

interface AsrClient {
    suspend fun transcribe(audioDataUri: String, format: String, sampleRate: Int, languageHints: List<String>): AsrResult
}

interface LlmClient {
    suspend fun complete(system: String, user: String): String
}

/**
 * Провайдеро-независимая конфигурация (Qwen для тестов, но работает с разными).
 */
data class ApiConfig(
    val baseUrl: String,
    val token: String,
    val asrModel: String,
    val llmModel: String,
    val protocol: String = "auto", // auto | dashscope | openai
)
