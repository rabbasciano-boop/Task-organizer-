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
        var previousNoise = 0f
        var softNoise = 0f
        return render(390) { t, _ ->
            seed = seed * 1_103_515_245 + 12_345
            val noise = ((seed ushr 16 and 0x7FFF) / 16_383.5f) - 1f
            softNoise = softNoise * 0.78f + noise * 0.22f
            val crinkle = noise - previousNoise * 0.62f
            previousNoise = noise
            val folds = listOf(0.05f, 0.15f, 0.27f, 0.41f, 0.58f, 0.73f).sumOf { center ->
                exp((-(t - center) * (t - center) / 0.0028f).toDouble())
            }.toFloat()
            val envelope = sin(PI * t).toFloat().let { it * it } * folds.coerceAtMost(1.25f)
            (crinkle * 0.78f + softNoise * 0.28f) * envelope * 0.22f
        }
    }

    private fun windSamples(): ShortArray {
        var seed = 0x2569
        var air = 0f
        var gust = 0f
        return render(620) { t, _ ->
            seed = seed * 1_664_525 + 1_013_904_223
            val noise = ((seed ushr 16 and 0x7FFF) / 16_383.5f) - 1f
            air = air * 0.84f + noise * 0.16f
            gust = gust * 0.975f + noise * 0.025f
            val swell = 0.62f + 0.38f * sin(PI * (t * 1.18f - 0.11f)).toFloat().coerceAtLeast(0f)
            val envelope = sin(PI * t).toFloat().let { it * it } * swell
            val airyWhistle = sin(2.0 * PI * (125f + 36f * sin(t * PI)) * t * 0.620).toFloat() * 0.045f
            (air * 0.72f + gust * 0.50f + airyWhistle) * envelope * 0.24f
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
