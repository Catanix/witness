package com.witness.wear

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Очередь записей на часах. Сохранение НЕ ждёт телефон:
 * файл ложится в очередь и досылается автоматически при появлении связи.
 */
object SyncQueue {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sending = AtomicBoolean(false)
    val pendingCount = MutableStateFlow(0)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("witness_queue", Context.MODE_PRIVATE)

    fun ids(ctx: Context): List<String> =
        (prefs(ctx).getString("pending", "") ?: "").split(",").filter { it.isNotBlank() }

    fun refreshCount(ctx: Context) { pendingCount.value = ids(ctx).size }

    fun add(ctx: Context, id: String) {
        val p = prefs(ctx)
        p.edit().putString("pending", (p.getString("pending", "") ?: "") + "$id,").apply()
        refreshCount(ctx)
    }

    private fun remove(ctx: Context, id: String) {
        val p = prefs(ctx)
        p.edit().putString("pending", ids(ctx).filter { it != id }.joinToString(",") { "$it" } + if (ids(ctx).any { it != id }) "," else "").apply()
        refreshCount(ctx)
    }

    /** Пытается дослать всёpending; вызывается после сохранения, при появлении узлов и из listener-сервиса. */
    fun flush(ctx: Context) {
        val app = ctx.applicationContext
        if (!sending.compareAndSet(false, true)) return
        scope.launch {
            try {
                refreshCount(app)
                if (ids(app).isEmpty()) return@launch
                // кладём item в data layer даже офлайн: GMS хранит его локально
                // и автоматически синкает на телефон при появлении связи.
                for (id in ids(app)) {
                    val file = File(app.filesDir, "audio/$id.wav")
                    if (!file.exists()) { remove(app, id); continue }
                    val ts = id.removePrefix("watch_").toLongOrNull() ?: System.currentTimeMillis()
                    val dur = ((file.length() - 44) / 32000).toInt().coerceAtLeast(0)
                    val ok = runCatching {
                        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val req = PutDataMapRequest.create("/recording/$id").apply {
                            dataMap.putLong("ts", ts)
                            dataMap.putLong("duration", dur.toLong())
                            dataMap.putAsset("audio", Asset.createFromFd(pfd))
                        }.asPutDataRequest().setUrgent()
                        Tasks.await(Wearable.getDataClient(app).putDataItem(req))
                        pfd.close()
                    }.isSuccess
                    if (ok) {
                        file.delete()
                        remove(app, id)
                        Log.i("WearSync", "sent $id ($dur s)")
                    } else break  // порядок сохраняем, остальное дослится позже
                }
            } finally {
                sending.set(false)
                refreshCount(app)
            }
        }
    }
}
