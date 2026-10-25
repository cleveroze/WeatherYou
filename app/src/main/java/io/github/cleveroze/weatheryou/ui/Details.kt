@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.weatheryou.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.cleveroze.weatheryou.data.Forecast
import io.github.cleveroze.weatheryou.data.Settings
import io.github.cleveroze.weatheryou.ui.components.drawPolygon
import io.github.cleveroze.weatheryou.ui.components.toPath
import io.github.cleveroze.weatheryou.ui.components.unit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Details(f: Forecast, settings: Settings) {
    val c = f.current
    val today = f.days.firstOrNull()
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val (w, wu) = settings.wind(c.windSpeedMs)
            val (g, _) = settings.wind(c.windGustMs)
            DetailCard("Ветер", Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BigValue(w, wu)
                        Text("порывы до $g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(windDirLong(c.windDir), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Compass(c.windDir, Modifier.size(76.dp))
                }
            }
            DetailCard("Влажность", Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BigValue("${c.humidity}", "%")
                        Text("точка росы ${settings.temp(c.dewPoint)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HumidityClover(c.humidity, Modifier.size(70.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val uv = maxOf(c.uvIndex, 0.0)
            DetailCard("УФ-индекс", Modifier.weight(1f)) {
                BigValue("${uv.roundToInt()}", uvLevel(uv).lowercase())
                Spacer(Modifier.height(10.dp))
                GradientScale(
                    (uv / 11.0).toFloat(),
                    listOf(Color(0xFF6CC56E), Color(0xFFF2D14B), Color(0xFFF2994A), Color(0xFFE5484D), Color(0xFF9B5DE5)),
                )
            }
            val (p, pu) = settings.pressure(c.pressureHpa)
            DetailCard("Давление", Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BigValue(p, "")
                        Text(pu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            when {
                                c.pressureHpa < 1005 -> "пониженное"
                                c.pressureHpa > 1022 -> "повышенное"
                                else -> "в норме"
                            },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PressureGauge(c.pressureHpa, Modifier.size(72.dp))
                }
            }
        }
        if (today != null) SunCard(today.sunrise, today.sunset, f.nowLocal, c.isDay)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailCard("Ощущается", Modifier.weight(1f)) {
                BigValue(settings.temp(c.feelsLike), "")
                val diff = c.feelsLike - c.temp
                Text(
                    when {
                        diff < -2 && c.windSpeedMs > 3 -> "Ветер делает прохладнее"
                        diff < -2 -> "Кажется холоднее"
                        diff > 2 && c.humidity > 60 -> "Влажность добавляет жару"
                        diff > 2 -> "Кажется теплее"
                        else -> "Как и на термометре"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DetailCard("Видимость", Modifier.weight(1f)) {
                val km = c.visibilityM / 1000
                BigValue(if (km >= 10) "${km.roundToInt()}" else String.format(java.util.Locale("ru"), "%.1f", km), "км")
                Text(
                    when {
                        km >= 10 -> "Отличная"
                        km >= 4 -> "Хорошая"
                        km >= 1 -> "Умеренная"
                        else -> "Плохая, туман"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailCard("Осадки", Modifier.weight(1f)) {
                BigValue(String.format(java.util.Locale("ru"), "%.1f", today?.precipSum ?: c.precipitation), "мм")
                Text("за сегодня · вероятность ${today?.precipProb ?: 0}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DetailCard("Облачность", Modifier.weight(1f)) {
                BigValue("${c.cloudCover}", "%")
                Spacer(Modifier.height(10.dp))
                GradientScale(c.cloudCover / 100f, listOf(Color(0xFFFFC857), Color(0xFFB8C4D6), Color(0xFF7D8AA0)))
            }
        }
        f.air?.let { air ->
            DetailCard("Качество воздуха", Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BigValue("${air.europeanAqi}", aqiLevel(air.europeanAqi).lowercase())
                        Text(
                            "PM2.5 ${air.pm25.roundToInt()} · PM10 ${air.pm10.roundToInt()} мкг/м³",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        GradientScale(
                            (air.europeanAqi / 100f).coerceIn(0f, 1f),
                            listOf(Color(0xFF50C878), Color(0xFFC9E265), Color(0xFFF2D14B), Color(0xFFF2994A), Color(0xFFE5484D), Color(0xFF8E3A9D)),
                        )
                    }
                    AqiBurst(air.europeanAqi, Modifier.padding(start = 16.dp).size(72.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.86f),
        modifier = modifier.heightIn(min = 140.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun BigValue(value: String, unit: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Normal)
        if (unit.isNotEmpty()) Text(
            unit, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
    }
}

/** Animated "grow from 0" value for gauges. */
@Composable
private fun grow(target: Float): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { a.animateTo(target, spring(dampingRatio = 0.6f, stiffness = 60f)) }
    return a.value
}

@Composable
private fun GradientScale(fraction: Float, colors: List<Color>) {
    val cs = MaterialTheme.colorScheme
    val v = grow(fraction.coerceIn(0f, 1f))
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(Brush.horizontalGradient(colors), cornerRadius = r, alpha = 0.9f)
        val x = (v * size.width).coerceIn(size.height / 2, size.width - size.height / 2)
        drawCircle(cs.surface, radius = size.height, center = Offset(x, size.height / 2))
        drawCircle(cs.onSurface, radius = size.height * 0.6f, center = Offset(x, size.height / 2))
    }
}

/** Wind compass on a 12-sided cookie with an arrow that springs to the wind direction. */
@Composable
private fun Compass(deg: Int, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val cookie = remember { MaterialShapes.Cookie12Sided.unit() }
    val angle = grow(deg.toFloat())
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold)
    Canvas(modifier) {
        drawPolygon(cookie, cs.surfaceContainerHighest)
        val c = Offset(size.width / 2, size.height / 2)
        listOf("С" to 0f, "В" to 90f, "Ю" to 180f, "З" to 270f).forEach { (t, a) ->
            val l = measurer.measure(t, style)
            val rad = (a - 90) * PI.toFloat() / 180
            val rr = size.minDimension * 0.34f
            drawText(l, topLeft = Offset(c.x + cos(rad) * rr - l.size.width / 2, c.y + sin(rad) * rr - l.size.height / 2))
        }
        // wind blows FROM deg, arrow points where it goes
        rotate(angle + 180f, c) {
            val r = size.minDimension * 0.26f
            val arrow = Path().apply {
                moveTo(c.x, c.y - r); lineTo(c.x + r * 0.32f, c.y + r * 0.5f); lineTo(c.x, c.y + r * 0.28f)
                lineTo(c.x - r * 0.32f, c.y + r * 0.5f); close()
            }
            drawPath(arrow, cs.primary)
        }
    }
}

/** Humidity as water filling a four-leaf clover, with a gentle wave. */
@Composable
private fun HumidityClover(humidity: Int, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val clover = remember { MaterialShapes.Clover4Leaf.unit() }
    val level = grow(humidity / 100f)
    val t = rememberInfiniteTransition(label = "wave")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "w").value
    Canvas(modifier) {
        val path = clover.cubics.toPath(size)
        drawPath(path, cs.surfaceContainerHighest)
        clipPath(path) {
            val top = size.height * (1 - level)
            val wave = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, top)
                val steps = 24
                for (i in 0..steps) {
                    val x = size.width * i / steps
                    lineTo(x, top + sin((i.toFloat() / steps * 2 + t) * 2 * PI).toFloat() * size.height * 0.035f)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(wave, Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.75f), cs.primary)))
        }
    }
}

@Composable
private fun PressureGauge(hpa: Double, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val frac = grow((((hpa - 970) / (1050 - 970)).toFloat()).coerceIn(0f, 1f))
    Canvas(modifier) {
        val stroke = size.minDimension * 0.12f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(cs.surfaceContainerHighest, 135f, 270f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(cs.tertiary, 135f, 270f * frac, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        val a = (135f + 270f * frac) * PI.toFloat() / 180
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - stroke / 2
        drawCircle(cs.onTertiaryContainer, radius = stroke * 0.7f, center = Offset(c.x + cos(a) * r, c.y + sin(a) * r))
    }
}

@Composable
private fun AqiBurst(aqi: Int, modifier: Modifier) {
    val burst = remember { MaterialShapes.SoftBurst.unit() }
    val color = when {
        aqi <= 20 -> Color(0xFF50C878); aqi <= 40 -> Color(0xFF9BCB4F); aqi <= 60 -> Color(0xFFF2C94C)
        aqi <= 80 -> Color(0xFFF2994A); aqi <= 100 -> Color(0xFFE5484D); else -> Color(0xFF8E3A9D)
    }
    val spin = rememberInfiniteTransition(label = "aqi")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "r").value
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawPolygon(burst, color.copy(alpha = 0.85f), rotation = spin) }
        Text("$aqi", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold)
    }
}

/** Sunrise → sunset arc with the sun's current position, drawn on a dashed path. */
@Composable
private fun SunCard(sunrise: String, sunset: String, now: String, isDay: Boolean) {
    val cs = MaterialTheme.colorScheme
    val rise = minutesOfDay(sunrise)
    val set = minutesOfDay(sunset)
    val cur = minutesOfDay(now)
    val target = if (set > rise) ((cur - rise).toFloat() / (set - rise)).coerceIn(0f, 1f) else 0.5f
    val p = grow(target)
    val sunny = remember { MaterialShapes.Sunny.unit() }
    val dayLen = (set - rise).coerceAtLeast(0)
    DetailCard("Солнце", Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 8.dp)) {
            val baseY = size.height - 8.dp.toPx()
            val h = size.height - 24.dp.toPx()
            val path = Path().apply {
                moveTo(0f, baseY)
                cubicTo(size.width * 0.25f, baseY - h * 1.3f, size.width * 0.75f, baseY - h * 1.3f, size.width, baseY)
            }
            drawLine(cs.outlineVariant, Offset(0f, baseY), Offset(size.width, baseY), 2.dp.toPx())
            drawPath(path, cs.outline, style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))))
            // point on the cubic
            val t = p
            val x = size.width * (3 * (1 - t) * (1 - t) * t * 0.25f + 3 * (1 - t) * t * t * 0.75f + t * t * t)
            val y = baseY + (-(3 * (1 - t) * (1 - t) * t + 3 * (1 - t) * t * t) * h * 1.3f)
            val s = 30.dp.toPx()
            if (isDay) {
                drawCircle(Color(0xFFFFC857).copy(alpha = 0.3f), radius = s * 0.8f, center = Offset(x, y))
                drawPolygon(sunny, Color(0xFFFFB938), left = x - s / 2, top = y - s / 2, size = Size(s, s), rotation = t * 180)
            } else {
                drawCircle(cs.outline, radius = s * 0.3f, center = Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Восход", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(hhmm(sunrise), style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Световой день", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text("${dayLen / 60} ч ${dayLen % 60} мин", style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Закат", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(hhmm(sunset), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
