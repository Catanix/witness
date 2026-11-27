package com.witness.core.data

import kotlinx.serialization.Serializable

@Serializable
enum class Source { WATCH, PHONE, IMPORT }

@Serializable
enum class Status { RECORDED, TRANSCRIBING, DONE, FAILED }

@Serializable
data class Segment(
    val speaker: String,
    val text: String,
    val startMs: Long = 0L,
)

@Serializable
data class TaskItem(
    val text: String,
    val speaker: String = "",
    val done: Boolean = false,
)

@Serializable
data class Recording(
    val id: String,
    val path: String,
    val createdAt: Long,
    val durationSec: Int = 0,
    val source: Source = Source.PHONE,
    val status: Status = Status.RECORDED,
    val title: String = "",
    val summary: String = "",
    val points: List<String> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val transcript: String = "",
    val segments: List<Segment> = emptyList(),
    val error: String = "",
    val markers: List<Int> = emptyList(),
)