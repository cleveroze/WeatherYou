package io.github.cleveroze.weatheryou.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.cleveroze.weatheryou.data.WeatherKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SunLight = Color(0xFFFFE7BA)
private val SunDeep = Color(0xFFFFB45E)
private val SnowWhite = Color(0xFFF7FBFF)
private val BoltYellow = Color(0xFFFFD166)

fun moodAccent(kind: WeatherKind, isDay: Boolean): Color = when (kind) {
    WeatherKind.CLEAR -> if (isDay) Color(0xFFFFB25C) else Color(0xFF9FB4FF)
    WeatherKind.PARTLY_CLOUDY -> Color(0xFF83B7A4)
    WeatherKind.CLOUDY -> Color(0xFF93A6AD)
    WeatherKind.FOG -> Color(0xFFA4B0A6)
    WeatherKind.DRIZZLE -> Color(0xFF6FA3B5)
    WeatherKind.RAIN -> Color(0xFF5B8FAA)
    WeatherKind.SNOW -> Color(0xFFA6C8E4)
    WeatherKind.STORM -> Color(0xFF8B7BC0)
}

@Composable
fun WeatherArt(kind: WeatherKind, isDay: Boolean, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val accent = moodAccent(kind, isDay)
    val cloud = if (dark) Color(0xFFDCE5EA) else Color(0xFFFDFDFC)

    val transition = rememberInfiniteTransition(label = "weather-art")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing)),
        label = "spin"
    )
    val bob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val fall by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "fall"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    val flash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6200
                0f at 0
                0f at 5350
                1f at 5450
                0.1f at 5580
                0.85f at 5680
                0f at 5860
                0f at 6200
            }
        ),
        label = "flash"
    )

    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = if (dark) 0.22f else 0.30f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = s * 0.62f
            ),
            radius = s * 0.62f,
            center = Offset(cx, cy)
        )

        when (kind) {
            WeatherKind.CLEAR -> {
                if (isDay) {
                    drawSun(cx, cy, s * 0.21f, spin)
                } else {
                    drawMoon(cx, cy, s * 0.19f)
                    drawStars(cx, cy, s, shimmer)
                }
            }

            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) {
                    drawSun(cx + s * 0.13f, cy - s * 0.13f, s * 0.15f, spin)
                } else {
                    drawMoon(cx + s * 0.13f, cy - s * 0.14f, s * 0.13f)
                    drawStars(cx, cy, s, shimmer)
                }
                drawCloudArt(cx - s * 0.08f, cy + s * 0.14f + bob * s * 0.012f, s * 0.56f, cloud)
            }

            WeatherKind.CLOUDY -> {
                if (!isDay) drawStars(cx, cy, s, shimmer * 0.7f)
                drawCloudArt(cx - s * 0.10f, cy - s * 0.02f + bob * s * 0.010f, s * 0.58f, cloud.copy(alpha = 0.9f))
                drawCloudArt(cx + s * 0.18f, cy + s * 0.18f, s * 0.42f, cloud.copy(alpha = 0.7f))
            }

            WeatherKind.FOG -> {
                drawCloudArt(cx, cy - s * 0.08f, s * 0.50f, cloud.copy(alpha = 0.5f))
                repeat(3) { index ->
                    val lineWidth = s * (0.56f - index * 0.06f)
                    val y = cy + s * (0.08f + index * 0.075f) + bob * s * 0.006f
                    drawRoundRect(
                        color = accent.copy(alpha = 0.30f - index * 0.06f),
                        topLeft = Offset(cx - lineWidth / 2f + index * s * 0.02f, y),
                        size = Size(lineWidth, s * 0.016f),
                        cornerRadius = CornerRadius(s * 0.008f)
                    )
                }
            }

            WeatherKind.DRIZZLE -> {
                drawCloudArt(cx, cy - s * 0.06f, s * 0.54f, cloud)
                drawRain(cx, cy, s, fall, accent, count = 3, length = s * 0.06f)
            }

            WeatherKind.RAIN -> {
                drawCloudArt(cx, cy - s * 0.08f, s * 0.56f, cloud)
                drawRain(cx, cy, s, fall, accent, count = 5, length = s * 0.12f)
            }

            WeatherKind.SNOW -> {
                drawCloudArt(cx, cy - s * 0.08f, s * 0.56f, cloud)
                repeat(9) { index ->
                    val progress = (fall * 0.7f + index * 0.11f) % 1f
                    val x = cx + sin((progress * 2 * PI + index).toFloat()) * s * 0.17f +
                        (index % 3 - 1) * s * 0.13f
                    val y = cy + s * 0.14f + progress * s * 0.40f
                    drawCircle(
                        color = SnowWhite.copy(alpha = 0.9f * (1f - progress * 0.55f)),
                        radius = s * 0.013f,
                        center = Offset(x, y)
                    )
                }
            }

            WeatherKind.STORM -> {
                if (flash > 0.05f) {
                    drawCircle(
                        color = BoltYellow.copy(alpha = 0.22f * flash),
                        radius = s * 0.32f,
                        center = Offset(cx, cy + s * 0.06f)
                    )
                }
                drawCloudArt(cx, cy - s * 0.11f, s * 0.58f, cloud.copy(alpha = 0.82f))
                drawRain(cx, cy, s, fall, accent, count = 4, length = s * 0.10f)
                drawBolt(cx, cy + s * 0.02f, s, flash)
            }
        }
    }
}

@Composable
fun WeatherGlyph(kind: WeatherKind, isDay: Boolean, modifier: Modifier = Modifier) {
    val accent = moodAccent(kind, isDay)
    val cloudColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sunColor = SunDeep

    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f

        when (kind) {
            WeatherKind.CLEAR -> {
                if (isDay) drawGlyphSun(cx, cy, s * 0.20f, sunColor)
                else drawGlyphMoon(cx, cy, s * 0.22f, accent)
            }

            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) drawGlyphSun(cx + s * 0.17f, cy - s * 0.17f, s * 0.13f, sunColor)
                else drawGlyphMoon(cx + s * 0.17f, cy - s * 0.17f, s * 0.12f, accent)
                drawGlyphCloud(cx - s * 0.07f, cy + s * 0.10f, s * 0.64f, cloudColor)
            }

            WeatherKind.CLOUDY -> drawGlyphCloud(cx, cy + s * 0.02f, s * 0.78f, cloudColor)

            WeatherKind.FOG -> {
                drawGlyphCloud(cx, cy - s * 0.08f, s * 0.66f, cloudColor.copy(alpha = 0.55f))
                repeat(2) { index ->
                    drawLine(
                        color = accent.copy(alpha = 0.7f),
                        start = Offset(cx - s * 0.26f, cy + s * (0.16f + index * 0.12f)),
                        end = Offset(cx + s * 0.26f, cy + s * (0.16f + index * 0.12f)),
                        strokeWidth = s * 0.06f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherKind.DRIZZLE, WeatherKind.RAIN -> {
                drawGlyphCloud(cx, cy - s * 0.05f, s * 0.72f, cloudColor)
                repeat(if (kind == WeatherKind.RAIN) 3 else 2) { index ->
                    val x = cx + (index - 1) * s * 0.18f
                    drawLine(
                        color = accent,
                        start = Offset(x, cy + s * 0.18f),
                        end = Offset(x - s * 0.04f, cy + s * 0.30f),
                        strokeWidth = s * 0.055f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherKind.SNOW -> {
                drawGlyphCloud(cx, cy - s * 0.05f, s * 0.72f, cloudColor)
                repeat(3) { index ->
                    drawCircle(
                        color = accent,
                        radius = s * 0.035f,
                        center = Offset(cx + (index - 1) * s * 0.18f, cy + s * 0.24f)
                    )
                }
            }

            WeatherKind.STORM -> {
                drawGlyphCloud(cx, cy - s * 0.06f, s * 0.72f, cloudColor)
                val bolt = Path().apply {
                    moveTo(cx + s * 0.03f, cy + s * 0.10f)
                    lineTo(cx - s * 0.07f, cy + s * 0.24f)
                    lineTo(cx + s * 0.01f, cy + s * 0.24f)
                    lineTo(cx - s * 0.05f, cy + s * 0.38f)
                    lineTo(cx + s * 0.10f, cy + s * 0.20f)
                    lineTo(cx + s * 0.02f, cy + s * 0.20f)
                    close()
                }
                drawPath(bolt, BoltYellow)
            }
        }
    }
}

private fun DrawScope.drawSun(cx: Float, cy: Float, radius: Float, spin: Float) {
    repeat(12) { index ->
        val angle = Math.toRadians((index * 30 + spin).toDouble())
        val inner = radius * 1.28f
        val outer = radius * 1.48f
        drawLine(
            color = SunDeep.copy(alpha = if (index % 2 == 0) 0.85f else 0.45f),
            start = Offset(
                cx + cos(angle).toFloat() * inner,
                cy + sin(angle).toFloat() * inner
            ),
            end = Offset(
                cx + cos(angle).toFloat() * outer,
                cy + sin(angle).toFloat() * outer
            ),
            strokeWidth = radius * 0.07f,
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SunLight, SunDeep),
            center = Offset(cx - radius * 0.3f, cy - radius * 0.3f),
            radius = radius * 1.9f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawMoon(cx: Float, cy: Float, radius: Float) {
    val disc = Path().apply { addOval(Rect(center = Offset(cx, cy), radius = radius)) }
    val bite = Path().apply {
        addOval(Rect(center = Offset(cx + radius * 0.52f, cy - radius * 0.28f), radius = radius * 0.92f))
    }
    val crescent = Path.combine(PathOperation.Difference, disc, bite)
    drawPath(
        path = crescent,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF7F4FF), Color(0xFFC7D1F2)),
            start = Offset(cx - radius, cy - radius),
            end = Offset(cx + radius, cy + radius)
        )
    )
}

private fun DrawScope.drawStars(cx: Float, cy: Float, s: Float, shimmer: Float) {
    val stars = listOf(
        Offset(-0.28f, -0.24f),
        Offset(0.22f, -0.29f),
        Offset(0.31f, 0.02f),
        Offset(-0.31f, 0.13f),
        Offset(0.05f, -0.36f)
    )
    stars.forEachIndexed { index, point ->
        val alpha = 0.35f + 0.55f * ((shimmer + index * 0.23f) % 1f)
        drawCircle(
            color = Color(0xFFF4F6FF).copy(alpha = alpha),
            radius = s * (0.008f + 0.003f * (index % 2)),
            center = Offset(cx + point.x * s, cy + point.y * s)
        )
    }
}

private fun DrawScope.drawCloudArt(cx: Float, cy: Float, width: Float, color: Color) {
    val r = width * 0.20f
    drawCircle(color, r, Offset(cx - width * 0.23f, cy + r * 0.18f))
    drawCircle(color, r * 1.18f, Offset(cx, cy - r * 0.22f))
    drawCircle(color, r * 0.90f, Offset(cx + width * 0.25f, cy + r * 0.12f))
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - width * 0.42f, cy + r * 0.12f),
        size = Size(width * 0.84f, r * 1.05f),
        cornerRadius = CornerRadius(r * 0.55f)
    )
}

private fun DrawScope.drawRain(
    cx: Float,
    cy: Float,
    s: Float,
    fall: Float,
    accent: Color,
    count: Int,
    length: Float
) {
    val startY = cy + s * 0.12f
    val span = s * 0.42f
    for (index in 0 until count) {
        val x = cx + (index - (count - 1) / 2f) * s * 0.12f
        val progress = (fall + index * 0.17f) % 1f
        val y = startY + progress * span
        drawLine(
            color = accent.copy(alpha = 1f - progress * 0.65f),
            start = Offset(x, y),
            end = Offset(x - s * 0.014f, y + length),
            strokeWidth = s * 0.012f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawBolt(cx: Float, cy: Float, s: Float, flash: Float) {
    val bolt = Path().apply {
        moveTo(cx + s * 0.03f, cy)
        lineTo(cx - s * 0.07f, cy + s * 0.19f)
        lineTo(cx + s * 0.02f, cy + s * 0.19f)
        lineTo(cx - s * 0.05f, cy + s * 0.37f)
        lineTo(cx + s * 0.13f, cy + s * 0.12f)
        lineTo(cx + s * 0.03f, cy + s * 0.12f)
        close()
    }
    drawPath(bolt, BoltYellow.copy(alpha = 0.55f + 0.45f * flash))
}

private fun DrawScope.drawGlyphSun(cx: Float, cy: Float, radius: Float, color: Color) {
    drawCircle(color, radius, Offset(cx, cy))
    repeat(8) { index ->
        val angle = Math.toRadians((index * 45).toDouble())
        drawLine(
            color = color.copy(alpha = 0.75f),
            start = Offset(
                cx + cos(angle).toFloat() * radius * 1.35f,
                cy + sin(angle).toFloat() * radius * 1.35f
            ),
            end = Offset(
                cx + cos(angle).toFloat() * radius * 1.75f,
                cy + sin(angle).toFloat() * radius * 1.75f
            ),
            strokeWidth = radius * 0.28f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawGlyphMoon(cx: Float, cy: Float, radius: Float, color: Color) {
    val disc = Path().apply { addOval(Rect(center = Offset(cx, cy), radius = radius)) }
    val bite = Path().apply {
        addOval(Rect(center = Offset(cx + radius * 0.55f, cy - radius * 0.3f), radius = radius * 0.92f))
    }
    drawPath(Path.combine(PathOperation.Difference, disc, bite), color)
}

private fun DrawScope.drawGlyphCloud(cx: Float, cy: Float, width: Float, color: Color) {
    val r = width * 0.20f
    drawCircle(color, r, Offset(cx - width * 0.21f, cy + r * 0.10f))
    drawCircle(color, r * 1.1f, Offset(cx, cy - r * 0.20f))
    drawCircle(color, r * 0.85f, Offset(cx + width * 0.22f, cy + r * 0.08f))
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - width * 0.38f, cy + r * 0.08f),
        size = Size(width * 0.76f, r * 0.95f),
        cornerRadius = CornerRadius(r * 0.45f)
    )
}
