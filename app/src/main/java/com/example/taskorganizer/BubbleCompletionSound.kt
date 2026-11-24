package com.example.taskorganizer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** Short, unobtrusive confirmation sound inspired by a small bubble popping. */
internal object BubbleCompletionSound {
    private const val SAMPLE_RATE = 22_050
    private const val DURATION_MS = 145

    fun play(context: Context) {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        Thread {
            val samples = bubbleSamples()
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull() ?: return@Thread
            try {
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(DURATION_MS.toLong() + 40L)
            } finally {
                track.release()
            }
        }.start()
    }

    private fun bubbleSamples(): ShortArray {
        val count = SAMPLE_RATE * DURATION_MS / 1_000
        return ShortArray(count) { index ->
            val progress = index.toFloat() / count
            val frequency = 260f + 260f * progress
            val envelope = sin(PI * progress).toFloat().let { it * it } * (1f - progress * 0.35f)
            val phase = 2.0 * PI * frequency * index / SAMPLE_RATE
            (sin(phase) * envelope * Short.MAX_VALUE * 0.19).toInt().toShort()
        }
    }
}
