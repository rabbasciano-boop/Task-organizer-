package com.example.taskorganizer

import android.content.Context
import android.app.NotificationManager
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.taskorganizer.ui.theme.OrganizerTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Small theme-specific confirmation sounds, generated locally with no audio files or network access. */
internal object ThemeCompletionSound {
    private const val sampleRate = 22_050
    private const val preferencesName = "task_organizer_settings"
    private const val soundsEnabledKey = "completion_sounds_enabled"
    private val soundExecutor = ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, ArrayBlockingQueue(1), ThreadPoolExecutor.DiscardPolicy())

    fun isEnabled(context: Context): Boolean = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getBoolean(soundsEnabledKey, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().putBoolean(soundsEnabledKey, enabled).apply()
    }

    fun play(context: Context, theme: OrganizerTheme) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return
        soundExecutor.execute {
            val audioManager = appContext.getSystemService(AudioManager::class.java) ?: return@execute
            val notificationManager = appContext.getSystemService(NotificationManager::class.java)
            if (!isEnabled(appContext) || audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                (notificationManager != null && notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL)) return@execute
            if (theme == OrganizerTheme.CARTA_DORATA) {
                playCrumpledPaper(appContext)
                return@execute
            }
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
            }.getOrNull() ?: return@execute
            try {
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep((samples.size * 1_000L / sampleRate) + 45L)
            } catch (_: Exception) {
                // Audio is optional; a playback failure must not affect the saved task.
            } finally {
                track.release()
            }
        }
    }

    private fun samplesFor(theme: OrganizerTheme): ShortArray = when (theme) {
        OrganizerTheme.ORIGINAL_DARK -> harpSamples()
        OrganizerTheme.AURORA_MIDNIGHT -> increaseVolume(spaceSamples())
        OrganizerTheme.AURORA_NORDIC -> increaseVolume(snowSamples())
        OrganizerTheme.SUNSET_GLOW, OrganizerTheme.SUNSET_GLOW_DARK -> increaseVolume(bubbleSamples())
        OrganizerTheme.AGENDA_REALE -> royalMagicSamples()
        OrganizerTheme.CARTA_DORATA -> error("Carta Dorata uses its bundled paper sound")
    }

    private fun playCrumpledPaper(context: Context) {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            player.setDataSource(context, Uri.parse("android.resource://${context.packageName}/${R.raw.sound_carta_stropicciata}"))
            player.prepare()
            player.start()
            Thread.sleep(player.duration.toLong().coerceAtLeast(0L) + 45L)
        } catch (_: Exception) {
            // Keep audio failures separate from saving the user's data.
        } finally {
            player.release()
        }
    }

    private fun bubbleSamples(): ShortArray = render(145) { t, _ ->
        val frequency = 260f + 260f * t
        val envelope = sin(PI * t).toFloat().let { it * it } * (1f - t * 0.35f)
        sin(2.0 * PI * frequency * t * 0.145).toFloat() * envelope * 0.19f
    }

    /** A short ascending arpeggio made with gently damped plucked strings. */
    private fun harpSamples(): ShortArray {
        val count = sampleRate * 1100 / 1000
        val mix = DoubleArray(count)
        val random = Random(71)
        val notes = doubleArrayOf(329.63, 392.00, 493.88, 659.25)
        notes.forEachIndexed { noteIndex, frequency ->
            val start = noteIndex * sampleRate * 65 / 1000
            val stringLength = (sampleRate / frequency).toInt().coerceAtLeast(2)
            val string = DoubleArray(stringLength) { random.nextDouble(-1.0, 1.0) }
            var position = 0
            for (index in start until count) {
                val next = (position + 1) % stringLength
                val value = string[position]
                string[position] = (value + string[next]) * 0.498
                val elapsed = (index - start).toDouble() / sampleRate
                val fadeOut = min(1.0, (count - index).toDouble() / (sampleRate * 0.12))
                mix[index] += value * exp(-elapsed * 1.8) * fadeOut * 0.5
                position = next
            }
        }
        val peak = mix.maxOfOrNull { abs(it) }?.takeIf { it > 0.0 } ?: 1.0
        return ShortArray(count) { index ->
            (mix[index] / peak * 0.72 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /** Increase audibility without clipping or changing the device's volume settings. */
    private fun increaseVolume(samples: ShortArray): ShortArray {
        val peak = samples.maxOfOrNull { abs(it.toInt()) } ?: return samples
        if (peak == 0) return samples
        val gain = (Short.MAX_VALUE * 0.78f) / peak
        return ShortArray(samples.size) { index ->
            (samples[index] * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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

    /** Rosa e argento: arpeggio leggero, ispirato alla bacchetta magica del tema Agenda Reale. */
    private fun royalMagicSamples(): ShortArray {
        val durationMs = 920
        val count = sampleRate * durationMs / 1_000
        val mix = DoubleArray(count)
        val notes = doubleArrayOf(1046.50, 1318.51, 1567.98, 2093.00, 2349.32, 2637.02, 3135.96, 4186.01)
        notes.forEachIndexed { index, frequency ->
            val start = (index * 0.042 * sampleRate).toInt()
            val noteDuration = 0.42 + index * 0.03
            val end = min(count, ((index * 0.042 + noteDuration) * sampleRate).toInt())
            for (sampleIndex in start until end) {
                val time = (sampleIndex - start).toDouble() / sampleRate
                val envelope = min(1.0, time / 0.012) * exp(-time / (noteDuration * 0.28))
                val sweep = frequency * (1.0 + 0.018 * (time / noteDuration))
                mix[sampleIndex] += 0.20 * envelope * sin(2.0 * PI * sweep * time)
                mix[sampleIndex] += 0.06 * envelope * sin(2.0 * PI * sweep * 2.004 * time)
            }
        }
        val random = Random(42)
        var previous = 0.0
        repeat((0.35 * sampleRate).toInt()) { index ->
            val time = index.toDouble() / sampleRate
            val noise = random.nextDouble(-1.0, 1.0)
            mix[index] += (noise - previous) * exp(-time / 0.09) * 0.045
            previous = noise
        }
        val peak = mix.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0.0 } ?: 1.0
        return ShortArray(count) { index -> (mix[index] / peak * 0.78 * Short.MAX_VALUE).toInt().coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() }
    }

    private fun render(durationMs: Int, sample: (time: Float, index: Int) -> Float): ShortArray {
        val count = sampleRate * durationMs / 1_000
        return ShortArray(count) { index ->
            (sample(index.toFloat() / count, index).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
