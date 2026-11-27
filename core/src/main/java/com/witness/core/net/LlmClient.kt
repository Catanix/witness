package com.witness.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** LLM via an OpenAI-compatible /chat/completions endpoint. Output language follows the app setting. */
class LlmClient private constructor(private val cfg: ApiConfig, private val http: okhttp3.OkHttpClient) {

    constructor(cfg: ApiConfig) : this(cfg, AsrClient.defaultClient())

    /** Strict JSON: {title, summary, points[], tasks[{text,speaker}], segments:[{speaker, text}]}.
     *  [lang] is the interface language ("en"/"ru"); all text fields are produced in that language
     *  regardless of the transcript language. */
    suspend fun analyze(transcript: String, lang: String = "en"): LlmAnalysis = withContext(Dispatchers.IO) {
        val ru = lang.lowercase().startsWith("ru")
        val sp1 = if (ru) "Говорящий 1" else "Speaker 1"
        val sp2 = if (ru) "Говорящий 2" else "Speaker 2"
        val prompt = buildString {
            if (ru) {
                append("Ты обрабатываешь транскрипцию записи диктофона. По тексту диалога верни СТРОГО JSON без пояснений:\n")
                append("{\"title\": \"короткое оглавление (до 60 символов, без точки в конце)\", ")
                append("\"summary\": \"краткое описание: диалог был про то-то, суть такая-то, надо сделать то-то. Без анализа и воды, 2-4 предложения.\", ")
                append("\"points\": [\"ключевой тезис 1\", \"ключевой тезис 2\"], ")
                append("\"tasks\": [{\"text\": \"что сделать\", \"speaker\": \"$sp1\"}], ")
                append("\"segments\": [{\"speaker\": \"$sp1\"|\"$sp2\", \"text\": \"реплика\"}]}\n")
                append("points — 3-6 главных тезисов; tasks — только явно поставленные задачи/поручения (если нет — пустой массив). Не выдумывай имена говорящих.\n")
                append("Все текстовые поля пиши на русском языке, независимо от языка транскрипции.\n\n")
                append("Текст транскрипции:\n")
            } else {
                append("You process a voice-recorder transcript. From the dialogue text return STRICTLY JSON with no explanations:\n")
                append("{\"title\": \"short heading (up to 60 characters, no trailing period)\", ")
                append("\"summary\": \"brief description: the dialogue was about X, the point is Y, Z needs to be done. No analysis or filler, 2-4 sentences.\", ")
                append("\"points\": [\"key point 1\", \"key point 2\"], ")
                append("\"tasks\": [{\"text\": \"what to do\", \"speaker\": \"$sp1\"}], ")
                append("\"segments\": [{\"speaker\": \"$sp1\"|\"$sp2\", \"text\": \"line\"}]}\n")
                append("points — 3-6 main points; tasks — only explicitly assigned tasks (empty array if none). Do not invent speaker names.\n")
                append("Write every text field in English, regardless of the transcript language.\n\n")
                append("Transcript text:\n")
            }
            append(transcript)
        }
        val body = buildJsonObject {
            put("model", cfg.llmModel)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
        }
        val url = cfg.llmUrl.trimEnd('/') + "/chat/completions"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + cfg.llmApiKey)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) error("HTTP ${resp.code}: ${raw.take(300)}")
            val content = Json.parseToJsonElement(raw).jsonObject["choices"]!!
                .jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
            parseAnalysis(content, lang)
        }
    }

    private fun parseAnalysis(content: String, lang: String): LlmAnalysis {
        val ru = lang.lowercase().startsWith("ru")
        val fallbackSpeaker = if (ru) "Говорящий" else "Speaker"
        val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val o = Json.parseToJsonElement(cleaned).jsonObject
        val segments = o["segments"]?.jsonArray?.map {
            val so = it.jsonObject
            LlmSegment(
                speaker = so["speaker"]?.jsonPrimitive?.content ?: fallbackSpeaker,
                text = so["text"]?.jsonPrimitive?.content ?: "",
            )
        } ?: emptyList()
        val points = o["points"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val tasks = o["tasks"]?.jsonArray?.map {
            val to = it.jsonObject
            com.witness.core.data.TaskItem(
                text = to["text"]?.jsonPrimitive?.content ?: "",
                speaker = to["speaker"]?.jsonPrimitive?.content ?: "",
            )
        }?.filter { it.text.isNotBlank() } ?: emptyList()
        return LlmAnalysis(
            title = o["title"]?.jsonPrimitive?.content ?: "",
            summary = o["summary"]?.jsonPrimitive?.content ?: "",
            points = points,
            tasks = tasks,
            segments = segments,
        )
    }
}

data class LlmSegment(val speaker: String, val text: String)

data class LlmAnalysis(
    val title: String,
    val summary: String,
    val points: List<String> = emptyList(),
    val tasks: List<com.witness.core.data.TaskItem> = emptyList(),
    val segments: List<LlmSegment>,
)
