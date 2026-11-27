package com.witness.mobile.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.core.content.ContextCompat
import com.witness.core.audio.WavRecorder
import com.witness.core.data.Recording
import com.witness.core.data.Source
import com.witness.mobile.MainActivity
import com.witness.mobile.Processing
import com.witness.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Запись живёт в foreground-сервисе: приложение можно свернуть,
 * в шторке остаётся control panel (таймер, пауза, маркер, стоп).
 */
class RecordingService : Service() {

    override fun attachBaseContext(base: Context) {
        val lang = base.getSharedPreferences("witness_locale", Context.MODE_PRIVATE).getString("lang", "en") ?: "en"
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    companion object {
        const val ACTION_START = "com.witness.record.START"
        const val ACTION_PAUSE = "com.witness.record.PAUSE"
        const val ACTION_RESUME = "com.witness.record.RESUME"
        const val ACTION_MARK = "com.witness.record.MARK"
        const val ACTION_STOP_SAVE = "com.witness.record.STOP_SAVE"
        const val ACTION_DISCARD = "com.witness.record.DISCARD"
        private const val CHANNEL = "recording"
        private const val NOTIF_ID = 2001

        /** Общее состояние для UI (экран записи подключается к живому сервису). */
        val recorder = MutableStateFlow<WavRecorder?>(null)
        val markers = MutableStateFlow<List<Int>>(emptyList())
        val saving = MutableStateFlow(false)

        fun send(ctx: Context, action: String) {
            val i = Intent(ctx, RecordingService::class.java).setAction(action)
            if (action == ACTION_START) ContextCompat.startForegroundService(ctx, i) else ctx.startService(i)
        }
    }

    private var wav: WavRecorder? = null
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
                        markers.value = emptyList()
                        ensureChannel()
                        startForeground(NOTIF_ID, buildNotif())
                    } else stopSelf()
                } else updateNotif()
            }
            ACTION_PAUSE -> { wav?.pause(); updateNotif() }
            ACTION_RESUME -> { wav?.resume(); updateNotif() }
            ACTION_MARK -> {
                val r = wav ?: return START_NOT_STICKY
                val s = r.elapsedSec.value.toInt()
                if (s !in markers.value) markers.value = markers.value + s
                updateNotif()
            }
            ACTION_STOP_SAVE -> {
                val r = wav ?: run { stopSelf(); return START_NOT_STICKY }
                if (saving.value) return START_NOT_STICKY
                saving.value = true
                scope.launch {
                    val ts = System.currentTimeMillis()
                    val outFile = File(File(filesDir, "audio").apply { mkdirs() }, "rec_$ts.wav")
                    val mk = markers.value
                    val dur = withContext(Dispatchers.IO) { r.stop(outFile) }
                    if (dur > 0) {
                        Processing.start(this@RecordingService, Recording("rec_$ts", outFile.absolutePath, ts, dur, Source.PHONE, markers = mk))
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        wav = null
        recorder.value = null
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.rec_channel), NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null) })
        }
    }

    private fun actionPi(action: String, req: Int): PendingIntent =
        PendingIntent.getService(this, req, Intent(this, RecordingService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun buildNotif(): Notification {
        val r = wav
        val paused = r?.paused?.value == true
        val elapsed = r?.elapsedSec?.value ?: 0L
        val mk = markers.value.size
        val open = PendingIntent.getActivity(this, 3001,
            Intent(this, MainActivity::class.java).putExtra("open_recording", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.rec_live))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setGroup("rec_panel")
            .setSortKey("0")
            .setStyle(MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (paused) {
            b.setUsesChronometer(false)
            b.setContentText(getString(R.string.recording_paused) + " · %02d:%02d".format(elapsed / 60, elapsed % 60) +
                (if (mk > 0) " · " + getString(R.string.rec_marks_count, mk) else ""))
        } else {
            b.setWhen(System.currentTimeMillis() - elapsed * 1000)
            b.setUsesChronometer(true)
            b.setShowWhen(true)
            b.setContentText(if (mk > 0) getString(R.string.rec_marks_count, mk) else getString(R.string.rec_state))
        }
        b.addAction(if (paused) R.drawable.ic_rec_play else R.drawable.ic_rec_pause,
            getString(if (paused) R.string.recording_resume else R.string.recording_pause),
            actionPi(if (paused) ACTION_RESUME else ACTION_PAUSE, 3002))
        b.addAction(R.drawable.ic_rec_flag, getString(R.string.rec_mark), actionPi(ACTION_MARK, 3003))
        b.addAction(R.drawable.ic_rec_stop, getString(R.string.recording_stop), actionPi(ACTION_STOP_SAVE, 3004))
        return b.build()
    }

    private fun updateNotif() {
        if (wav != null) getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif())
    }
}
