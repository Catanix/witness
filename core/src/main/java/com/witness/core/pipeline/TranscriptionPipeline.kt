package com.witness.core.pipeline

import android.content.Context
import com.witness.core.data.Recording
import com.witness.core.data.RecordingsStore
import com.witness.core.data.Segment
import com.witness.core.data.SettingsRepo
import com.witness.core.data.Status
import com.witness.core.net.AsrClient
import com.witness.core.net.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

enum class Stage { RECEIVED, TRANSCRIBING, ANALYZING, DONE, FAILED }

object TranscriptionPipeline {
    suspend fun run(
        context: Context,
        rec: Recording,
        onStage: (Stage, Recording?) -> Unit = { _, _ -> },
    ): Recording {
        val store = RecordingsStore.get(context)
        store.upsert(rec.copy(status = Status.TRANSCRIBING))
        onStage(Stage.TRANSCRIBING, rec)
        val settings = SettingsRepo(context)
        val cfg = settings.config()
        if (!cfg.isComplete) {
            val f = rec.copy(status = Status.FAILED, error = "API config is empty — set it in Settings")
            store.upsert(f); onStage(Stage.FAILED, f); return f
        }
        return try {
            val bytes = withContext(Dispatchers.IO) { File(rec.path).readBytes() }
            val asr = AsrClient(cfg).transcribe(bytes)
            if (asr.text.isBlank()) {
                val f = rec.copy(status = Status.FAILED, error = "Empty transcript")
                store.upsert(f); onStage(Stage.FAILED, f); return f
            }
            store.upsert(rec.copy(transcript = asr.text))
            onStage(Stage.ANALYZING, rec)
            val lang = settings.lang.first()
            val llm = LlmClient(cfg).analyze(asr.text, lang)
            // таймкоды реплик берём из ASR-предложений (beginMs), если число совпадает
            val starts = asr.sentences.map { it.beginMs ?: 0L }
            val done = rec.copy(
                status = Status.DONE,
                title = llm.title.ifBlank { "Untitled" },
                summary = llm.summary,
                points = llm.points,
                tasks = llm.tasks,
                segments = llm.segments.mapIndexed { i, s ->
                    Segment(s.speaker, s.text, if (i < starts.size) starts[i] else 0L)
                },
                transcript = asr.text,
            )
            store.upsert(done); onStage(Stage.DONE, done)
            done
        } catch (e: Exception) {
            val f = rec.copy(status = Status.FAILED, error = e.message ?: "Unknown error")
            store.upsert(f); onStage(Stage.FAILED, f); f
        }
    }
}
