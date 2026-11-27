package com.witness.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.witness.core.audio.WavRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Запись на часах в foreground-сервисе: экран гаснет — запись продолжается. */
class WearRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.witness.wear.START"
        const val ACTION_PAUSE = "com.witness.wear.PAUSE"
        const val ACTION_RESUME = "com.witness.wear.RESUME"
        const val ACTION_STOP_SAVE = "com.witness.wear.STOP_SAVE"
        const val ACTION_DISCARD = "com.witness.wear.DISCARD"
        private const val CHANNEL = "wear_rec"
        private const val NOTIF_ID = 3001

        val recorder = MutableStateFlow<WavRecorder?>(null)
        val saving = MutableStateFlow(false)
        val lastSaved = MutableStateFlow(false)

        fun send(ctx: Context, action: String) {
            val i = Intent(ctx, WearRecordingService::class.java).setAction(action)
            if (action == ACTION_START) ContextCompat.startForegroundService(ctx, i) else ctx.startService(i)
        }
    }

    private var wav: WavRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (wav == null) {
                    val r = WavRecorder()
                    if (r.start()) {
                        wav = r
                        recorder.value = r
                        lastSaved.value = false
                        val pm = getSystemService(PowerManager::class.java)
                        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "witness:rec").apply { acquire(60 * 60 * 1000L) }
                        ensureChannel()
                        startForeground(NOTIF_ID, buildNotif())
                    } else stopSelf()
                }
            }
            ACTION_PAUSE -> wav?.pause()
            ACTION_RESUME -> wav?.resume()
            ACTION_STOP_SAVE -> {
                val r = wav ?: run { stopSelf(); return START_NOT_STICKY }
                if (saving.value) return START_NOT_STICKY
                saving.value = true
                scope.launch {
                    val ts = System.currentTimeMillis()
                    val id = "watch_$ts"
                    val dir = File(filesDir, "audio").apply { mkdirs() }
                    val dur = withContext(Dispatchers.IO) { r.stop(File(dir, "$id.wav")) }
                    if (dur > 0) {
                        SyncQueue.add(this@WearRecordingService, id)
                        SyncQueue.flush(this@WearRecordingService)
                        lastSaved.value = true
                    }
                    finish()
                }
            }
            ACTION_DISCARD -> {
                val r = wav ?: run { stopSelf(); return START_NOT_STICKY }
                if (saving.value) return START_NOT_STICKY
                saving.value = true
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val tmp = File(cacheDir, "discard.wav")
                        r.stop(tmp)
                        tmp.delete()
                    }
                    finish()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun finish() {
        wav = null
        recorder.value = null
        saving.value = false
        runCatching { wakeLock?.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        wav = null
        recorder.value = null
        runCatching { wakeLock?.release() }
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.w_live), NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        }
    }

    private fun buildNotif(): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_wear_notif)
            .setContentTitle(getString(R.string.w_live))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
}
