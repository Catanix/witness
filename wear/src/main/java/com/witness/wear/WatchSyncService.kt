package com.witness.wear

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** Просыпается при изменении связности с телефоном и досылает очередь. */
class WatchSyncService : WearableListenerService() {
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        SyncQueue.flush(this)
    }

    override fun onDataChanged(events: DataEventBuffer) {
        // телефон подтвердил приём (удалил data item) — пробуем дослать остаток
        SyncQueue.flush(this)
    }
}
