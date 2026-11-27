package com.example.taskorganizer

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val royalSparkleColors = listOf(
    Color(0xFFF5F8FC), Color(0xFFE8EEF5), Color(0xFFC8D3E0),
    Color(0xFFFFD6EB), Color(0xFFFF85C8), Color(0xFFFF69B4)
)

private class RoyalParticle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val radius: Float, var alpha: Float, val decay: Float,
    val color: Color, val star: Boolean, val ambient: Boolean
)

/** Adapts the pink and silver particles from Agenda Reale to the current screen size. */
@Composable
internal fun RoyalSparklesContainer(enabled: Boolean, content: @Composable () -> Unit) {
    val particles = remember { mutableStateListOf<RoyalParticle>() }
    var size by remember { androidx.compose.runtime.mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    fun animationsEnabled(): Boolean = try {
        listOf(Settings.Global.ANIMATOR_DURATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE, Settings.Global.WINDOW_ANIMATION_SCALE)
            .all { Settings.Global.getFloat(context.contentResolver, it, 1f) > 0f } &&
            (Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled())
    } catch (_: SecurityException) { true }
    var systemAnimationsEnabled by remember { androidx.compose.runtime.mutableStateOf(animationsEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { systemAnimationsEnabled = animationsEnabled() }
        }
        listOf(Settings.Global.ANIMATOR_DURATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE, Settings.Global.WINDOW_ANIMATION_SCALE).forEach {
            context.contentResolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) systemAnimationsEnabled = animationsEnabled()
        }
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    val effectsEnabled = enabled && systemAnimationsEnabled

    // Opening the keyboard changes the available height; keep existing sparkles
    // alive instead of restarting the animation on every resize.
    LaunchedEffect(effectsEnabled, size) {
        if (!effectsEnabled) {
            particles.clear()
            return@LaunchedEffect
        }
        if (size.width <= 0 || size.height <= 0 || particles.any { it.ambient }) return@LaunchedEffect
        particles.addAll(List(28) {
            RoyalParticle(
                Random.nextFloat() * size.width, Random.nextFloat() * size.height,
                (Random.nextFloat() - .5f) * .45f, -(Random.nextFloat() * .55f + .2f),
                Random.nextFloat() * 3.2f + 1.5f, Random.nextFloat() * .6f + .2f,
                0f, royalSparkleColors.random(), Random.nextFloat() > .35f, true
            )
        })
    }

    LaunchedEffect(effectsEnabled, lifecycle) {
        if (!effectsEnabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previousFrame = 0L
            while (true) {
                delay(if (particles.any { !it.ambient }) 16L else 50L)
                withFrameNanos { now ->
                    val step = if (previousFrame == 0L) 1f else ((now - previousFrame) / 16_666_667f).coerceIn(0f, 4f)
                    previousFrame = now
                    val iterator = particles.listIterator()
                    while (iterator.hasNext()) {
                        val particle = iterator.next()
                        particle.x += particle.vx * step
                        particle.y += particle.vy * step
                        if (particle.ambient) {
                            if (particle.y < -20f) particle.y = size.height + 20f
                            if (particle.x < -20f) particle.x = size.width + 20f
                            if (particle.x > size.width + 20f) particle.x = -20f
                        } else {
                            particle.vy += .16f * step
                            particle.alpha -= particle.decay * step
                            if (particle.alpha <= .02f) iterator.remove()
                        }
                    }
                    frame++
                }
            }
        }
    }

    val observeTaps = if (effectsEnabled) Modifier.pointerInput(effectsEnabled) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val start = down.position
                while (particles.count { !it.ambient } > 100) {
                    val index = particles.indexOfFirst { !it.ambient }
                    if (index < 0) break
                    particles.removeAt(index)
                }
                val burst = List(34) {
                    val angle = Random.nextFloat() * (2f * PI.toFloat())
                    val speed = Random.nextFloat() * 7.5f + 2f
                    RoyalParticle(
                        start.x + Random.nextFloat() * 24f - 12f, start.y + Random.nextFloat() * 18f - 9f,
                        cos(angle) * speed, sin(angle) * speed - 2.5f,
                        Random.nextFloat() * 4.5f + 2.2f, 1f, Random.nextFloat() * .022f + .014f,
                        royalSparkleColors.random(), Random.nextFloat() > .25f, false
                    )
                }
                // Begin before the target handles the touch, including during screen transitions.
                particles.addAll(burst)
                frame++
                var moved = false
                var released = false
                while (!released) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                    if ((pointer.position - start).getDistance() > viewConfiguration.touchSlop) moved = true
                    if (event.changes.count { it.pressed } > 1) moved = true
                    released = !pointer.pressed
                }
                if (!released || moved) particles.removeAll(burst.toSet())
            }
        }
    } else Modifier

    Box(Modifier.fillMaxSize().onSizeChanged { size = it }.then(observeTaps)) {
        content()
        if (effectsEnabled) Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE") val redraw = frame
            particles.forEach { particle ->
                val color = particle.color.copy(alpha = particle.alpha.coerceIn(0f, 1f))
                if (particle.star) {
                    val path = Path()
                    for (index in 0 until 8) {
                        val radius = particle.radius * if (index % 2 == 0) 2.4f else .55f
                        val angle = (index * PI / 4.0).toFloat()
                        val x = particle.x + cos(angle) * radius
                        val y = particle.y + sin(angle) * radius
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, color)
                } else {
                    drawCircle(color, radius = particle.radius, center = Offset(particle.x, particle.y))
                }
            }
        }
    }
}
