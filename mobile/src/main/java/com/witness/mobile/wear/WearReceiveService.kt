package com.witness.mobile.wear

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.witness.core.data.Recording
import com.witness.core.data.Source
import com.witness.mobile.Processing
import java.io.File
import java.io.FileOutputStream

class WearReceiveService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type != DataEvent.TYPE_CHANGED) continue
            val item = e.dataItem
            val path = item.uri.path ?: continue
            if (!path.startsWith("/recording/")) continue
            try {
                val map = DataMapItem.fromDataItem(item).dataMap
                val asset = map.getAsset("audio") ?: continue
                val id = path.removePrefix("/recording/")
                val dur = map.getLong("duration", 0L).toInt()
                val ts = map.getLong("ts", System.currentTimeMillis())
                val dir = File(filesDir, "audio").apply { mkdirs() }
                val out = File(dir, "$id.wav")
                val resp = Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset))
                resp.inputStream.use { input ->
                    FileOutputStream(out).use { input.copyTo(it) }
                }
                resp.release()
                Processing.start(this, Recording(id, out.absolutePath, ts, dur, Source.WATCH))
                Tasks.await(Wearable.getDataClient(this).deleteDataItems(item.uri))
                Log.i("WearReceive", "received $id ($dur s)")
            } catch (ex: Exception) {
                Log.e("WearReceive", "receive failed", ex)
            }
        }
    }
}
