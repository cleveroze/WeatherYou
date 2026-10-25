@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.weatheryou.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import io.github.cleveroze.weatheryou.data.Sky
import io.github.cleveroze.weatheryou.data.Wmo

/** Colors used by weather glyphs: recognisable but tinted by the current scheme. */
data class GlyphColors(
    val sun: Color,
    val moon: Color,
    val cloud: Color,
    val cloudDark: Color,
    val rain: Color,
    val snow: Color,
    val bolt: Color,
)

@Composable
fun glyphColors(): GlyphColors {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        GlyphColors(
            sun = lerp(Color(0xFFFFB938), cs.tertiary, 0.18f),
            moon = lerp(Color(0xFFF3E7C9), cs.primaryContainer, 0.3f),
            cloud = lerp(Color(0xFFF4F4F8), cs.secondaryContainer, 0.35f),
            cloudDark = lerp(Color(0xFFB4B7C3), cs.secondary, 0.25f),
            rain = lerp(Color(0xFF5AA9FF), cs.primary, 0.3f),
            snow = lerp(Color.White, cs.primaryContainer, 0.2f),
            bolt = Color(0xFFFFD54A),
        )
    }
}

/**
 * Weather icon composed from Material 3 Expressive shapes:
 * the sun is the "Very sunny" star, snowflakes are flowers, clouds are puffs.
 */
@Composable
fun WeatherGlyph(code: Int, isDay: Boolean, modifier: Modifier = Modifier, animated: Boolean = false) {
    val c = glyphColors()
    val sky = Wmo.sky(code)
    val t = if (animated) {
        rememberInfiniteTransition(label = "glyph")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "t").value
    } else 0f
    val sunShape = remember { MaterialShapes.VerySunny.unit() }
    val flower = remember { MaterialShapes.Flower.unit() }
    Canvas(modifier) {
        val s = size.minDimension
        when (sky) {
            Sky.Clear -> if (isDay) sun(sunShape, c, Offset(0.5f, 0.5f), 0.78f, t) else moon(c, Offset(0.5f, 0.5f), 0.62f)
            Sky.PartlyCloudy -> {
                if (isDay) sun(sunShape, c, Offset(0.62f, 0.36f), 0.56f, t) else moon(c, Offset(0.64f, 0.36f), 0.46f)
                cloud(c.cloud, Offset(0.42f, 0.62f), 0.66f, t)
            }
            Sky.Cloudy -> {
                cloud(c.cloudDark, Offset(0.62f, 0.42f), 0.52f, t)
                cloud(c.cloud, Offset(0.44f, 0.6f), 0.7f, -t)
            }
            Sky.Fog -> {
                cloud(c.cloud, Offset(0.5f, 0.42f), 0.62f, t)
                for (i in 0..2) {
                    val y = s * (0.7f + i * 0.1f)
                    val shift = s * 0.06f * kotlin.math.sin((t + i * 0.3f) * 6.283f)
                    drawLine(c.cloudDark, Offset(s * 0.2f + shift, y), Offset(s * 0.8f + shift, y), s * 0.05f, StrokeCap.Round)
                }
            }
            Sky.Drizzle, Sky.Rain -> {
                cloud(c.cloud, Offset(0.5f, 0.4f), 0.72f, t)
                val n = if (sky == Sky.Rain) 3 else 2
                for (i in 0 until n) {
                    val x = s * (0.32f + i * (0.36f / (n - 1).coerceAtLeast(1)))
                    val fall = ((t * 3 + i * 0.37f) % 1f)
                    val y = s * (0.66f + fall * 0.2f)
                    drawLine(c.rain.copy(alpha = 1f - fall * 0.6f), Offset(x, y), Offset(x - s * 0.04f, y + s * 0.12f), s * 0.06f, StrokeCap.Round)
                }
            }
            Sky.Snow -> {
                cloud(c.cloud, Offset(0.5f, 0.38f), 0.72f, t)
                for (i in 0..2) {
                    val fall = ((t * 2 + i * 0.33f) % 1f)
                    val fs = s * 0.15f
                    drawPolygon(
                        flower, c.snow, left = s * (0.24f + i * 0.2f), top = s * (0.64f + fall * 0.18f),
                        size = Size(fs, fs), rotation = t * 360f + i * 40f,
                    )
                }
            }
            Sky.Thunder -> {
                cloud(c.cloudDark, Offset(0.5f, 0.38f), 0.74f, t)
                val bolt = Path().apply {
                    moveTo(s * 0.52f, s * 0.52f); lineTo(s * 0.38f, s * 0.76f); lineTo(s * 0.5f, s * 0.76f)
                    lineTo(s * 0.42f, s * 0.96f); lineTo(s * 0.66f, s * 0.68f); lineTo(s * 0.53f, s * 0.68f)
                    lineTo(s * 0.62f, s * 0.52f); close()
                }
                val flash = if (animated && t % 0.5f < 0.05f) 1f else 0.92f
                drawPath(bolt, c.bolt.copy(alpha = flash))
            }
        }
    }
}

private fun DrawScope.sun(shape: androidx.graphics.shapes.RoundedPolygon, c: GlyphColors, center: Offset, scale: Float, t: Float) {
    val s = size.minDimension * scale
    drawCircle(c.sun.copy(alpha = 0.25f), radius = s * 0.62f, center = Offset(size.width * center.x, size.height * center.y))
    drawPolygon(
        shape, c.sun,
        left = size.width * center.x - s / 2, top = size.height * center.y - s / 2,
        size = Size(s, s), rotation = t * 360f,
    )
}

private fun DrawScope.moon(c: GlyphColors, center: Offset, scale: Float) {
    val r = size.minDimension * scale / 2
    val cx = size.width * center.x
    val cy = size.height * center.y
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx, cy), r)) }
    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx + r * 0.55f, cy - r * 0.45f), r * 0.85f)) }
    val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
    drawPath(crescent, c.moon)
}

private fun DrawScope.cloud(color: Color, center: Offset, scale: Float, t: Float) {
    val w = size.minDimension * scale
    val drift = size.minDimension * 0.025f * kotlin.math.sin(t * 6.283f)
    val cx = size.width * center.x + drift
    val cy = size.height * center.y
    // three puffs + a flat base, like a Material "Puffy" cloud
    drawCircle(color, radius = w * 0.24f, center = Offset(cx - w * 0.2f, cy + w * 0.04f))
    drawCircle(color, radius = w * 0.31f, center = Offset(cx + w * 0.02f, cy - w * 0.08f))
    drawCircle(color, radius = w * 0.22f, center = Offset(cx + w * 0.24f, cy + w * 0.06f))
    drawRoundRect(
        color, topLeft = Offset(cx - w * 0.42f, cy + w * 0.02f), size = Size(w * 0.86f, w * 0.26f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.13f),
    )
}
