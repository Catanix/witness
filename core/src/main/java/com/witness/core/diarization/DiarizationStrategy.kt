package com.witness.core.diarization

/** Сегмент диаризации: интервал и говорящий. */
data class DiarizedSegment(val beginMs: Long, val endMs: Long, val speaker: String)

/**
 * Стратегии: OnDeviceSherpa (pyannote на устройстве), LlmAttribution (фолбек),
 * NativeQwen (когда появится qwen-audio-3.1 в подписке).
 */
interface DiarizationStrategy {
    suspend fun diarize(wavPath: String, durationMs: Long): List<DiarizedSegment>
}
