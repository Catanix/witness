package com.witness.mobile.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.witness.core.data.Recording
import com.witness.mobile.R
import java.io.File

/** Единая логика «поделиться» — используется и в карточке (Detail), и в меню по зажатию на главной. */
object ShareRecording {
    enum class Mode { BRIEF, FULL, MD }

    fun send(context: Context, rec: Recording, mode: Mode) {
        val intent = Intent(Intent.ACTION_SEND)
        if (mode == Mode.MD) {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val f = File(dir, "${rec.id}.md")
            f.writeText("# ${rec.title}\n\n${rec.summary}\n\n## Transcript\n\n${rec.transcript}\n")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
            intent.type = "text/markdown"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.type = "text/plain"
            intent.putExtra(
                Intent.EXTRA_TEXT,
                if (mode == Mode.BRIEF) rec.title + "\n\n" + rec.summary else rec.transcript,
            )
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)))
    }
}
