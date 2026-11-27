package com.witness.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** JSON-file backed list of recordings; survives restarts. */
class RecordingsStore private constructor(context: Context) {
    companion object {
        @Volatile private var instance: RecordingsStore? = null
        /** Единый instance на процесс — UI и пайплайн видят один StateFlow. */
        fun get(context: Context): RecordingsStore =
            instance ?: synchronized(this) {
                instance ?: RecordingsStore(context.applicationContext).also { instance = it }
            }
    }
    private val file = File(context.filesDir, "recordings.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val mutex = Mutex()
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<Recording>> = _items

    private fun load(): List<Recording> = try {
        if (file.exists()) json.decodeFromString<List<Recording>>(file.readText()) else emptyList()
    } catch (_: Exception) { emptyList() }

    private fun persist() { file.writeText(json.encodeToString(ListSerializer(Recording.serializer()), _items.value)) }

    suspend fun upsert(rec: Recording) = mutex.withLock {
        _items.value = listOf(rec) + _items.value.filter { it.id != rec.id }
        persist()
    }

    suspend fun update(id: String, transform: (Recording) -> Recording) = mutex.withLock {
        _items.value = _items.value.map { if (it.id == id) transform(it) else it }
        persist()
    }

    suspend fun delete(id: String) = mutex.withLock {
        _items.value.find { it.id == id }?.let { runCatching { File(it.path).delete() } }
        _items.value = _items.value.filter { it.id != id }
        persist()
    }

    fun get(id: String): Recording? = _items.value.find { it.id == id }
}
