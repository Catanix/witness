package com.witness.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Speaker(val id: String, val label: String)

@Serializable
data class Segment(
    val speakerId: String,
    val text: String,
    val beginMs: Long,
    val endMs: Long,
)

@Serializable
data class Transcript(
    val id: String,
    val recordingId: String,
    val title: String,
    val summary: String,
    val segments: List<Segment>,
)

enum class RecordingSource { PHONE, WATCH }
enum class RecordingStatus { RECORDING, PAUSED, TRANSFERRING, TRANSCRIBING, DONE, FAILED }
