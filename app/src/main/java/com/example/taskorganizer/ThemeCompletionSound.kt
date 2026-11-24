package com.example.taskorganizer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.taskorganizer.ui.theme.OrganizerTheme
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Small theme-specific confirmation sounds, generated locally with no audio files or network access. */
internal object ThemeCompletionSound {
    private const val sampleRate = 22_050

    fun play(context: Context, theme: OrganizerTheme) {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        Thread {
            val samples = samplesFor(theme)
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
                            .setSampleRate(sampleRate)
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
                Thread.sleep((samples.size * 1_000L / sampleRate) + 45L)
            } finally {
                track.release()
            }
        }.start()
    }

    private fun samplesFor(theme: OrganizerTheme): ShortArray = when (theme) {
        OrganizerTheme.CARTA_DORATA -> crumpledPaperSamples()
        OrganizerTheme.ORIGINAL_DARK -> windSamples()
        OrganizerTheme.AURORA_MIDNIGHT -> spaceSamples()
        OrganizerTheme.AURORA_NORDIC -> snowSamples()
        OrganizerTheme.SUNSET_GLOW, OrganizerTheme.SUNSET_GLOW_DARK -> bubbleSamples()
    }

    private fun bubbleSamples(): ShortArray = render(145) { t, _ ->
        val frequency = 260f + 260f * t
        val envelope = sin(PI * t).toFloat().let { it * it } * (1f - t * 0.35f)
        sin(2.0 * PI * frequency * t * 0.145).toFloat() * envelope * 0.19f
    }

    private fun crumpledPaperSamples(): ShortArray {
        var seed = 0x41A7
        return render(130) { t, _ ->
            seed = seed * 1_103_515_245 + 12_345
            val noise = ((seed ushr 16 and 0x7FFF) / 16_383.5f) - 1f
            val envelope = exp((-5.6f * t).toDouble()).toFloat() * (0.72f + 0.28f * sin(t * 32f))
            noise * envelope * 0.12f
        }
    }

    private fun windSamples(): ShortArray {
        var seed = 0x2569
        var smooth = 0f
        return render(245) { t, _ ->
            seed = seed * 1_664_525 + 1_013_904_223
            val noise = ((seed ushr 16 and 0x7FFF) / 16_383.5f) - 1f
            smooth = smooth * 0.91f + noise * 0.09f
            val envelope = sin(PI * t).toFloat().let { it * it }
            smooth * envelope * 0.16f
        }
    }

    private fun spaceSamples(): ShortArray = render(210) { t, _ ->
        val sweep = 170f + 640f * t * t
        val shimmer = sin(2.0 * PI * (sweep * 1.62f) * t * 0.210).toFloat() * 0.30f
        val main = sin(2.0 * PI * sweep * t * 0.210).toFloat()
        val envelope = sin(PI * t).toFloat().let { it * it }
        (main + shimmer) * envelope * 0.15f
    }

    private fun snowSamples(): ShortArray = render(170) { t, _ ->
        val first = sin(2.0 * PI * 1_180f * t * 0.170).toFloat() * exp((-8.5f * t).toDouble()).toFloat()
        val secondStart = (t - 0.33f).coerceAtLeast(0f)
        val second = sin(2.0 * PI * 1_560f * secondStart * 0.170).toFloat() * exp((-12f * secondStart).toDouble()).toFloat()
        (first + second * 0.75f) * 0.12f
    }

    private fun render(durationMs: Int, sample: (time: Float, index: Int) -> Float): ShortArray {
        val count = sampleRate * durationMs / 1_000
        return ShortArray(count) { index ->
            (sample(index.toFloat() / count, index).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
