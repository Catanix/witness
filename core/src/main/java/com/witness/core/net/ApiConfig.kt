package com.witness.core.net

/** Конфигурация провайдера ASR/LLM. Пользователь меняет её в настройках телефона. */
data class ApiConfig(
    val asrUrl: String = "",
    val asrModel: String = "",
    val llmUrl: String = "",
    val llmModel: String = "",
    val apiKey: String = "",
    val asrKey: String = "",
    val llmKey: String = "",
) {
    /** У каждого сервиса свой ключ; legacy apiKey — fallback для старых конфигов. */
    val asrApiKey: String get() = asrKey.ifBlank { apiKey }
    val llmApiKey: String get() = llmKey.ifBlank { apiKey }

    val isComplete: Boolean
        get() = asrUrl.isNotBlank() && asrModel.isNotBlank() &&
                llmUrl.isNotBlank() && llmModel.isNotBlank() &&
                asrApiKey.isNotBlank() && llmApiKey.isNotBlank()

    companion object {
        /** Дефолты из сборки (dev-билд — пресет из .env.dev; git-билд — пустые). */
        fun fromBuild(
            asrUrl: String, asrModel: String,
            llmUrl: String, llmModel: String, apiKey: String,
        ) = ApiConfig(asrUrl, asrModel, llmUrl, llmModel, apiKey)

        /**
         * Определяем протокол ASR по URL:
         *  - DashScope-роут (.../aigc/multimodal-generation/generation) — Qwen Cloud Token Plan;
         *  - иначе OpenAI-совместимый /chat/completions с input_audio (mimo и др.).
         */
        fun isDashScope(asrUrl: String): Boolean =
            asrUrl.contains("multimodal-generation")
    }
}
