package com.witness.mobile.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.witness.core.data.Recording
import com.witness.core.pipeline.Stage
import com.witness.mobile.MainActivity
import com.witness.mobile.R

object Notifications {
    private const val ID_RECEIVED = 1001
    private const val ID_PROCESSING = 1002
    private const val ID_DONE = 1003
    private const val ID_FAILED = 1004

    fun post(context: Context, title: String, text: String, id: Int = ID_DONE) {
        val pi = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, "witness")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, n)
    }

    fun stage(context: Context, stage: Stage, rec: Recording?) {
        val name = rec?.title?.ifBlank { null } ?: context.getString(R.string.untitled)
        when (stage) {
            Stage.RECEIVED -> post(context, context.getString(R.string.notif_received_watch), name, ID_RECEIVED)
            Stage.TRANSCRIBING -> post(context, context.getString(R.string.notif_transcribing), name, ID_PROCESSING)
            Stage.ANALYZING -> post(context, context.getString(R.string.notif_analyzing), name, ID_PROCESSING)
            Stage.DONE -> post(context, context.getString(R.string.notif_done), name, ID_DONE)
            Stage.FAILED -> post(context, context.getString(R.string.notif_failed), rec?.error ?: "", ID_FAILED)
        }
    }
}
