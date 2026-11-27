package com.witness.wear

import android.app.Application

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncQueue.refreshCount(this)
        // авто-досылка при появлении связи — через WatchSyncService (onCapabilityChanged/onDataChanged)
        SyncQueue.flush(this)
    }
}
