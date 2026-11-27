package com.witness.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

/** PCM 16 kHz mono -> WAV. Amplitude + elapsed exposed as flows for the equalizer/timer. */
class WavRecorder {
    val amplitude = MutableStateFlow(0f)
    val elapsedSec = MutableStateFlow(0L)
    val running = MutableStateFlow(false)
    val paused = MutableStateFlow(false)

    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private val pcm = ByteArrayOutputStream()
    @Volatile private var capturing = false
    private var startedAt = 0L
    private var pausedAccum = 0L
    private var pauseStart = 0L

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return false
        val rec = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, minBuf * 4)
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }
        pcm.reset()
        record = rec
        capturing = true
        paused.value = false
        pausedAccum = 0
        startedAt = System.currentTimeMillis()
        rec.startRecording()
        running.value = true
        thread = Thread {
            val buf = ByteArray(minBuf)
            while (capturing) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    if (!paused.value) {
                        pcm.write(buf, 0, n)
                        var sum = 0.0
                        var i = 0
                        while (i + 1 < n) {
                            val s = (buf[i].toInt() and 0xff) or (buf[i + 1].toInt() shl 8)
                            sum += s.toDouble() * s
                            i += 2
                        }
                        val rms = sqrt(sum / (n / 2)) / 32768.0
                        amplitude.value = (rms * 6).toFloat().coerceIn(0.04f, 1f)
                    } else {
                        amplitude.value = 0.04f
                    }
                    // на паузе таймер заморожен на моменте pauseStart (иначе бежит вперёд и откатывается при resume)
                    val now = if (paused.value) pauseStart else System.currentTimeMillis()
                    elapsedSec.value = (now - startedAt - pausedAccum) / 1000
                }
            }
        }.also { it.start() }
        return true
    }

    fun pause() { if (!paused.value) { paused.value = true; pauseStart = System.currentTimeMillis() } }
    fun resume() { if (paused.value) { pausedAccum += System.currentTimeMillis() - pauseStart; paused.value = false } }

    /** Stops and writes a WAV file; returns duration in seconds (0 on failure). */
    fun stop(out: File): Int {
        capturing = false
        thread?.join(2000)
        record?.stop()
        record?.release()
        record = null
        running.value = false
        amplitude.value = 0f
        val data = pcm.toByteArray()
        if (data.isEmpty()) return 0
        val dur = data.size / (SAMPLE_RATE * 2)
        RandomAccessFile(out, "rw").use { f ->
            f.setLength(0)
            f.write(wavHeader(data.size))
            f.write(data)
        }
        elapsedSec.value = 0
        return dur
    }

    private fun wavHeader(dataLen: Int): ByteArray {
        val total = 36 + dataLen
        val h = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()); h.putInt(total); h.put("WAVE".toByteArray())
        h.put("fmt ".toByteArray()); h.putInt(16); h.putShort(1); h.putShort(1)
        h.putInt(SAMPLE_RATE); h.putInt(SAMPLE_RATE * 2); h.putShort(2); h.putShort(16)
        h.put("data".toByteArray()); h.putInt(dataLen)
        return h.array()
    }

    companion object { const val SAMPLE_RATE = 16000 }
}
