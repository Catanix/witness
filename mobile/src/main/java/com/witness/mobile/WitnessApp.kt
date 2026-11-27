package com.witness.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.witness.core.data.RecordingsStore
import com.witness.core.data.SettingsRepo
import com.witness.core.net.ApiConfig

class WitnessApp : Application() {
    lateinit var store: RecordingsStore
    lateinit var settings: SettingsRepo

    override fun onCreate() {
        super.onCreate()
        SettingsRepo.preset = ApiConfig.fromBuild(
            BuildConfig.WITNESS_ASR_URL, BuildConfig.WITNESS_ASR_MODEL,
            BuildConfig.WITNESS_LLM_URL, BuildConfig.WITNESS_LLM_MODEL,
            BuildConfig.WITNESS_API_KEY,
        )
        store = RecordingsStore.get(this)
        settings = SettingsRepo(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("witness", getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }
}
