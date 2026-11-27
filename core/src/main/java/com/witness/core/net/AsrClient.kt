package com.witness.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.concurrent.TimeUnit

data class AsrSentence(
    val text: String,
    val beginMs: Long? = null,
    val endMs: Long? = null,
)

data class AsrResult(
    val text: String,
    val sentences: List<AsrSentence>,
    val durationSec: Int?,
)

/**
 * ASR-клиент с двумя протоколами (auto по URL):
 *  - DashScope: POST {asrUrl}, body {model, input.messages[content: input_audio(data=data-URI)], parameters{format,sample_rate}}
 *    → output.text, output.sentence[] (begin_time/end_time мс), usage.duration;
 *  - OpenAI-совместимый: POST {asrUrl}/chat/completions, content=[{type:input_audio,input_audio:{data:base64,format}}]
 *    → choices[0].message.content (шлюз запрещает текстовые части в запросе).
 */
class AsrClient private constructor(private val cfg: ApiConfig, private val http: OkHttpClient) {

    constructor(cfg: ApiConfig) : this(cfg, defaultClient())

    suspend fun transcribe(wavBytes: ByteArray, format: String = "wav"): AsrResult =
        withContext(Dispatchers.IO) {
            if (ApiConfig.isDashScope(cfg.asrUrl)) transcribeDashScope(wavBytes, format)
            else transcribeOpenAi(wavBytes, format)
        }

    private fun transcribeDashScope(wav: ByteArray, format: String): AsrResult {
        val b64 = Base64.getEncoder().encodeToString(wav)
        val mime = if (format == "mp3") "audio/mpeg" else "audio/wav"
        val body = buildJsonObject {
            put("model", cfg.asrModel)
            putJsonObject("input") {
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "input_audio")
                                putJsonObject("input_audio") {
                                    put("data", "data:$mime;base64,$b64")
                                }
                            })
                        })
                    })
                })
            }
            putJsonObject("parameters") {
                put("format", format)
                put("sample_rate", "16000")
            }
        }
        val root = postJson(cfg.asrUrl, body.toString())
        val out = root["output"]!!.jsonObject
        val text = out["text"]?.jsonPrimitive?.content ?: ""
        val sentences = (out["sentence"] as? JsonObject)?.let { listOf(parseSentence(it)) }
            ?: (out["sentences"] as? kotlinx.serialization.json.JsonArray)?.map { parseSentence(it.jsonObject) }
            ?: emptyList()
        val duration = (root["usage"] as? JsonObject)?.get("duration")?.jsonPrimitive?.content?.toIntOrNull()
        return AsrResult(text, sentences, duration)
    }

    private fun parseSentence(o: JsonObject): AsrSentence = AsrSentence(
        text = o["text"]?.jsonPrimitive?.content ?: "",
        beginMs = o["begin_time"]?.jsonPrimitive?.content?.toLongOrNull(),
        endMs = o["end_time"]?.jsonPrimitive?.content?.toLongOrNull(),
    )

    private fun transcribeOpenAi(wav: ByteArray, format: String): AsrResult {
        val b64 = Base64.getEncoder().encodeToString(wav)
        val body = buildJsonObject {
            put("model", cfg.asrModel)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "input_audio")
                            putJsonObject("input_audio") {
                                put("data", b64)
                                put("format", format)
                            }
                        })
                    })
                })
            })
        }
        val url = cfg.asrUrl.trimEnd('/') + "/chat/completions"
        val resp = postJson(url, body.toString())
        val text = resp["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["text"]?.jsonPrimitive?.content
            ?: resp["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]?.jsonPrimitive?.content
            ?: ""
        return AsrResult(text, listOf(AsrSentence(text)), null)
    }

    private fun postJson(url: String, json: String): JsonObject {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + cfg.asrApiKey)
            .header("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) error("HTTP ${resp.code}: ${body.take(300)}")
            return Json.parseToJsonElement(body).jsonObject
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
