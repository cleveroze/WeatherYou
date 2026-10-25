package io.github.cleveroze.weatheryou.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import io.github.cleveroze.weatheryou.data.Sky
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Top/bottom sky colors: the scheme's surface, tinted by the weather mood. */
@Composable
fun skyColors(sky: Sky?, isDay: Boolean, tint: Boolean): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    val dark = cs.surface.luminance() < 0.4f
    val mood: Color = when {
        sky == null || !tint -> cs.primaryContainer
        !isDay -> Color(0xFF1B2450)
        sky == Sky.Clear -> Color(0xFFFFD58A)
        sky == Sky.PartlyCloudy -> Color(0xFFBFD9F5)
        sky == Sky.Thunder -> Color(0xFF4B4470)
        sky == Sky.Snow -> Color(0xFFDDE9F7)
        sky == Sky.Rain || sky == Sky.Drizzle -> Color(0xFF8FA8C4)
        else -> Color(0xFFC3C7D1)
    }
    val top = lerp(cs.primaryContainer, mood, if (tint) 0.45f else 0f)
        .let { lerp(cs.surface, it, if (dark) 0.42f else 0.75f) }
    val bottom = cs.surface
    val animTop by animateColorAsState(top, tween(1200), label = "top")
    val animBottom by animateColorAsState(bottom, tween(1200), label = "bottom")
    return animTop to animBottom
}

private class Particle(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

/** Full-screen animated backdrop: gradient sky, floating M3 shapes and weather particles. */
@Composable
fun SkyBackground(sky: Sky?, isDay: Boolean, tint: Boolean, animate: Boolean, modifier: Modifier = Modifier) {
    val (top, bottom) = skyColors(sky, isDay, tint)
    val cs = MaterialTheme.colorScheme
    val particles = remember { List(70) { Random(it).let { r -> Particle(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, r.nextFloat(), r.nextFloat()) } } }
    val floaters = SkyShapes.floaters
    val time = if (animate) {
        rememberInfiniteTransition(label = "sky")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "t").value
    } else 0.2f
    val shapeColor = lerp(cs.primary, cs.tertiary, 0.4f)

    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(0f to top, 0.55f to lerp(top, bottom, 0.7f), 1f to bottom))

        // big soft glow (sun / moon light)
        if (sky == Sky.Clear || sky == Sky.PartlyCloudy) {
            val glow = if (isDay) Color(0xFFFFE3A3) else Color(0xFFB9C6FF)
            drawCircle(
                Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), center = Offset(size.width * 0.82f, size.height * 0.08f), radius = size.width * 0.8f),
                radius = size.width * 0.8f, center = Offset(size.width * 0.82f, size.height * 0.08f),
            )
        }

        // floating Material shapes (cookies, clovers, suns)
        floaters.forEachIndexed { i, poly ->
            val r = Random(i * 31 + 7)
            val s = size.width * (0.22f + r.nextFloat() * 0.3f)
            val baseX = r.nextFloat() * size.width - s / 2
            val baseY = r.nextFloat() * size.height * 0.9f
            val dy = sin((time * 2 * PI + i).toFloat()) * size.height * 0.02f
            val dx = sin((time * 2 * PI * 0.5 + i * 2).toFloat()) * size.width * 0.03f
            drawPolygon(
                poly, shapeColor.copy(alpha = 0.06f), left = baseX + dx, top = baseY + dy, size = Size(s, s),
                rotation = time * 360f * (if (i % 2 == 0) 1 else -1) + i * 30,
            )
        }

        when (sky) {
            Sky.Rain, Sky.Drizzle, Sky.Thunder -> {
                val n = if (sky == Sky.Drizzle) 30 else 60
                val drop = if (cs.surface.luminance() < 0.4f) Color(0xFF9CC8FF) else Color(0xFF3C6E9E)
                for (i in 0 until n) {
                    val p = particles[i]
                    val y = ((p.y + time * 40 * p.speed) % 1f) * (size.height + 80) - 40
                    val x = p.x * size.width - (y / size.height) * 30
                    val len = 18f + p.size * 22f
                    drawLine(drop.copy(alpha = 0.35f), Offset(x, y), Offset(x - 5f, y + len), 2.5f, StrokeCap.Round)
                }
                if (sky == Sky.Thunder && animate) {
                    val f = (time * 60) % 7f
                    if (f < 0.12f) drawRect(Color.White.copy(alpha = 0.35f * (1 - f / 0.12f)))
                }
            }
            Sky.Snow -> {
                for (i in 0 until 55) {
                    val p = particles[i]
                    val y = ((p.y + time * 12 * p.speed) % 1f) * (size.height + 40) - 20
                    val x = p.x * size.width + sin((time * 60 * p.speed + p.phase * 6).toDouble()).toFloat() * 18f
                    drawCircle(Color.White.copy(alpha = 0.75f), radius = 2.5f + p.size * 4f, center = Offset(x, y))
                }
            }
            Sky.Fog -> {
                for (i in 0 until 5) {
                    val y = size.height * (0.15f + i * 0.17f)
                    val x = ((time * (1 + i * 0.3f) + i * 0.2f) % 1f) * size.width * 1.6f - size.width * 0.3f
                    drawLine(cs.onSurface.copy(alpha = 0.05f), Offset(x - size.width * 0.5f, y), Offset(x + size.width * 0.5f, y), 60f, StrokeCap.Round)
                }
            }
            else -> {}
        }
        if (!isDay && sky != null && sky != Sky.Rain && sky != Sky.Snow && sky != Sky.Thunder) {
            for (i in 0 until 45) {
                val p = particles[i]
                val tw = 0.35f + 0.65f * ((sin((time * 120 * p.speed + p.phase * 10).toDouble()).toFloat() + 1) / 2)
                drawCircle(Color.White.copy(alpha = 0.7f * tw), radius = 1.2f + p.size * 1.8f, center = Offset(p.x * size.width, p.y * size.height * 0.6f))
            }
        }
        // subtle outline ring near the top, like a Material "orbit"
        drawCircle(shapeColor.copy(alpha = 0.05f), radius = size.width * 0.7f, center = Offset(size.width * 0.5f, -size.width * 0.15f), style = Stroke(2f))
    }
}
