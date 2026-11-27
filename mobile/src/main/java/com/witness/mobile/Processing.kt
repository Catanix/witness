package com.witness.mobile

import android.content.Context
import com.witness.core.data.Recording
import com.witness.core.data.Source
import com.witness.core.pipeline.Stage
import com.witness.core.pipeline.TranscriptionPipeline
import com.witness.mobile.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Processing {
    fun start(context: Context, rec: Recording) {
        val app = context.applicationContext as WitnessApp
        CoroutineScope(Dispatchers.IO).launch {
            app.store.upsert(rec)
            if (rec.source != Source.PHONE) Notifications.stage(app, Stage.RECEIVED, rec)
            TranscriptionPipeline.run(app, rec) { stage, r -> Notifications.stage(app, stage, r) }
        }
    }
}
