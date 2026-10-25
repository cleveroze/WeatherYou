@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.weatheryou.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.weatheryou.data.DayWeather
import io.github.cleveroze.weatheryou.data.Forecast
import io.github.cleveroze.weatheryou.data.HourWeather
import io.github.cleveroze.weatheryou.data.Settings
import io.github.cleveroze.weatheryou.data.Wmo
import io.github.cleveroze.weatheryou.ui.components.MorphingShapeBox
import io.github.cleveroze.weatheryou.ui.components.PolygonShape
import io.github.cleveroze.weatheryou.ui.components.SkyBackground
import io.github.cleveroze.weatheryou.ui.components.SkyShapes
import io.github.cleveroze.weatheryou.ui.components.WeatherGlyph
import io.github.cleveroze.weatheryou.ui.theme.HugeTemp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun WeatherApp(vm: WeatherViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var showPlaces by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (vm.state.value is UiState.Ready) vm.refresh() else vm.start()
    }
    val askLocation = {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    LaunchedEffect(Unit) {
        if (!vm.hasLocationPermission() && !vm.prefs.askedLocation && vm.selected.value == null) {
            vm.prefs.askedLocation = true
            askLocation()
        } else vm.start()
    }

    val forecast = (state as? UiState.Ready)?.forecast
    val motion = MaterialTheme.motionScheme
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        SkyBackground(
            sky = forecast?.let { Wmo.sky(it.current.code) },
            isDay = forecast?.current?.isDay ?: true,
            tint = settings.weatherTint,
            animate = settings.animations,
        )
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(motion.slowEffectsSpec()) + scaleIn(motion.slowSpatialSpec(), initialScale = 0.92f))
                    .togetherWith(fadeOut(motion.fastEffectsSpec()) + scaleOut(targetScale = 1.04f))
            },
            label = "state",
        ) { s ->
            when (s) {
                is UiState.Loading -> LoadingScreen(s.stage)
                is UiState.Error -> ErrorScreen(s.message, onRetry = vm::refresh, onPickCity = { showPlaces = true })
                is UiState.Ready -> ReadyScreen(
                    s, settings,
                    onRefresh = vm::refresh,
                    onPlaces = { showPlaces = true },
                    onSettings = { showSettings = true },
                    onAskLocation = askLocation,
                )
            }
        }
    }

    if (showPlaces) PlacesSheet(vm, onDismiss = { showPlaces = false }, onUseLocation = {
        if (!vm.hasLocationPermission()) askLocation()
        vm.selectPlace(null)
    })
    if (showSettings) SettingsSheet(settings, vm::updateSettings, onDismiss = { showSettings = false })
}

/** Staggered spring entrance for list sections. */
@Composable
fun Enter(index: Int, animate: Boolean, content: @Composable () -> Unit) {
    val a = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (a.value < 1f) {
            delay(60L * index)
            a.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
        }
    }
    Box(
        Modifier.graphicsLayer {
            alpha = a.value.coerceIn(0f, 1f)
            translationY = (1f - a.value) * 80.dp.toPx()
            val s = 0.92f + 0.08f * a.value
            scaleX = s; scaleY = s
        }
    ) { content() }
}

@Composable
private fun ReadyScreen(
    s: UiState.Ready,
    settings: Settings,
    onRefresh: () -> Unit,
    onPlaces: () -> Unit,
    onSettings: () -> Unit,
    onAskLocation: () -> Unit,
) {
    val f = s.forecast
    val pull = rememberPullToRefreshState()
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    PullToRefreshBox(
        isRefreshing = s.refreshing,
        onRefresh = onRefresh,
        state = pull,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            val frac = if (s.refreshing) 1f else pull.distanceFraction.coerceIn(0f, 1.2f)
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = insets.calculateTopPadding())
                    .graphicsLayer {
                        alpha = frac.coerceAtMost(1f)
                        scaleX = 0.4f + 0.6f * frac.coerceAtMost(1f)
                        scaleY = scaleX
                        translationY = frac * 56.dp.toPx() - 24.dp.toPx()
                        rotationZ = frac * 180f
                    }
            ) {
                if (frac > 0.01f) ContainedLoadingIndicator(Modifier.size(56.dp))
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = insets.calculateTopPadding() + 4.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") { Header(f, onPlaces, onSettings) }
            item(key = "hero") { Enter(0, settings.animations) { Hero(f, settings) } }
            if (s.offline || f.place.approximate) item(key = "banner") {
                Enter(1, settings.animations) { Banner(s.offline, f.place.approximate, onAskLocation) }
            }
            item(key = "hourly") {
                Enter(2, settings.animations) {
                    SectionCard("По часам", trailing = "${f.hours.size - 1} ч") { HourlyStrip(f.hours, settings) }
                }
            }
            item(key = "daily") {
                Enter(3, settings.animations) {
                    SectionCard("На ${f.days.size} дней") { DailyList(f, settings) }
                }
            }
            item(key = "details") { Enter(4, settings.animations) { Details(f, settings) } }
            item(key = "footer") {
                Text(
                    "Данные: Open-Meteo · обновлено ${relativeUpdated(f.fetchedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(f: Forecast, onPlaces: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onPlaces,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Row(Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(PolygonShape(MaterialShapes.Cookie6Sided)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (f.place.isCurrent) Icons.Rounded.LocationOn else Icons.Rounded.Place, null,
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp),
                    )
                }
                Column(Modifier.padding(start = 10.dp).weight(1f, fill = false)) {
                    Text(f.place.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (f.place.isCurrent) "Вы здесь · ${hhmm(f.nowLocal)}" else f.place.region.ifBlank { hhmm(f.nowLocal) },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.padding(start = 4.dp))
            }
        }
        Spacer(Modifier.weight(0.01f))
        FilledTonalIconButton(onClick = onSettings, modifier = Modifier.padding(start = 8.dp).size(48.dp)) {
            Icon(Icons.Rounded.Settings, contentDescription = "Настройки")
        }
    }
}

@Composable
private fun Hero(f: Forecast, settings: Settings) {
    val c = f.current
    val sky = Wmo.sky(c.code)
    val cs = MaterialTheme.colorScheme
    val today = f.days.firstOrNull()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(310.dp)) {
            MorphingShapeBox(
                target = SkyShapes.hero(sky, c.isDay),
                modifier = Modifier.fillMaxSize().padding(14.dp),
                brush = Brush.linearGradient(listOf(cs.primaryContainer, lerp(cs.primaryContainer, cs.tertiaryContainer, 0.85f))),
                spin = settings.animations,
                spinMillis = 40_000,
            ) {
                val target = settings.tempValue(c.temp).toFloat()
                val shown by animateFloatAsState(target, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow), label = "temp")
                val v = shown.roundToInt()
                Text(
                    (if (v < 0) "−${-v}" else "$v") + "°",
                    style = HugeTemp,
                    color = cs.onPrimaryContainer,
                    modifier = Modifier.offset(x = 10.dp),
                )
            }
            WeatherGlyph(
                c.code, c.isDay,
                Modifier.size(118.dp).align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
                animated = settings.animations,
            )
        }
        Text(Wmo.describe(c.code, c.isDay), style = MaterialTheme.typography.headlineMedium, color = cs.onSurface, textAlign = TextAlign.Center)
        Text(
            "Ощущается как ${settings.temp(c.feelsLike)}",
            style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (today != null) {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("↑ ${settings.temp(today.max)}", cs.tertiaryContainer, cs.onTertiaryContainer)
                Pill("↓ ${settings.temp(today.min)}", cs.secondaryContainer, cs.onSecondaryContainer)
                if (today.precipProb > 0) Pill("☂ ${today.precipProb}%", cs.primaryContainer, cs.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color) {
    Surface(shape = CircleShape, color = bg, contentColor = fg) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
private fun Banner(offline: Boolean, approximate: Boolean, onAskLocation: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (offline) Icons.Rounded.Warning else Icons.Rounded.LocationOn, null)
            Text(
                if (offline) "Нет сети — показан сохранённый прогноз"
                else "Место определено примерно, по IP. Разрешите геолокацию для точного прогноза",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            if (!offline && approximate) TextButton(onClick = onAskLocation) { Text("Разрешить") }
        }
    }
}

@Composable
fun SectionCard(title: String, trailing: String? = null, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.86f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

// --------------------------------------------------------------------------------------
// Hourly
// --------------------------------------------------------------------------------------

@Composable
private fun HourlyStrip(hours: List<HourWeather>, settings: Settings) {
    if (hours.isEmpty()) return
    var selected by remember { mutableIntStateOf(0) }
    val temps = hours.map { settings.tempValue(it.temp) }
    val tMin = temps.min()
    val tMax = temps.max()
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp)) {
        hours.forEachIndexed { i, h ->
            HourItem(
                h, i, temps, tMin, tMax, settings,
                selected = i == selected,
                onClick = { selected = i },
            )
        }
    }
}

@Composable
private fun HourItem(
    h: HourWeather, i: Int, temps: List<Double>, tMin: Double, tMax: Double, settings: Settings,
    selected: Boolean, onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sel by animateFloatAsState(if (selected) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "sel")
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = cs.onSurface, fontWeight = FontWeight.SemiBold)
    Column(
        Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(31.dp))
            .background(cs.primaryContainer.copy(alpha = 0.9f * sel))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (i == 0) "Сейчас" else hhmm(h.time),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
            maxLines = 1,
        )
        Box(Modifier.padding(vertical = 6.dp).size(40.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(40.dp).graphicsLayer { scaleX = sel; scaleY = sel; rotationZ = 90f * sel }
                    .clip(PolygonShape(MaterialShapes.Clover4Leaf)).background(cs.tertiaryContainer)
            )
            WeatherGlyph(h.code, h.isDay, Modifier.size(30.dp))
        }
        // temperature curve segment
        Canvas(Modifier.fillMaxWidth().height(58.dp)) {
            val range = (tMax - tMin).coerceAtLeast(1.0)
            fun y(t: Double) = (size.height - 8.dp.toPx()) - ((t - tMin) / range).toFloat() * (size.height - 30.dp.toPx())
            val cy = y(temps[i])
            val ly = if (i > 0) (y(temps[i - 1]) + cy) / 2 else cy
            val ry = if (i < temps.lastIndex) (y(temps[i + 1]) + cy) / 2 else cy
            val path = Path().apply {
                moveTo(0f, ly)
                cubicTo(size.width * 0.25f, ly, size.width * 0.25f, cy, size.width / 2, cy)
                cubicTo(size.width * 0.75f, cy, size.width * 0.75f, ry, size.width, ry)
            }
            drawPath(path, cs.primary.copy(alpha = 0.7f), style = Stroke(3.dp.toPx()))
            drawCircle(cs.surface, radius = 6.dp.toPx(), center = Offset(size.width / 2, cy))
            drawCircle(cs.primary, radius = 4.dp.toPx(), center = Offset(size.width / 2, cy))
            val layout = measurer.measure(settings.temp(h.temp), labelStyle)
            drawText(layout, topLeft = Offset(size.width / 2 - layout.size.width / 2, cy - layout.size.height - 6.dp.toPx()))
        }
        Text(
            if (h.precipProb >= 10) "${h.precipProb}%" else " ",
            style = MaterialTheme.typography.labelSmall,
            color = cs.primary,
        )
    }
}

// --------------------------------------------------------------------------------------
// Daily
// --------------------------------------------------------------------------------------

private fun tempColor(t: Double): Color {
    val stops = listOf(
        -25.0 to Color(0xFF5B5BD6), -10.0 to Color(0xFF4F8FE8), 0.0 to Color(0xFF6CC4E8),
        10.0 to Color(0xFF7FCB8E), 20.0 to Color(0xFFF2C94C), 30.0 to Color(0xFFF2994A), 40.0 to Color(0xFFE5484D),
    )
    if (t <= stops.first().first) return stops.first().second
    for (k in 1 until stops.size) {
        if (t <= stops[k].first) {
            val (t0, c0) = stops[k - 1]
            val (t1, c1) = stops[k]
            return lerp(c0, c1, ((t - t0) / (t1 - t0)).toFloat())
        }
    }
    return stops.last().second
}

@Composable
private fun DailyList(f: Forecast, settings: Settings) {
    var expanded by rememberSaveable { mutableIntStateOf(-1) }
    val gMin = f.days.minOf { it.min }
    val gMax = f.days.maxOf { it.max }
    Column(Modifier.padding(horizontal = 8.dp)) {
        f.days.forEachIndexed { i, d ->
            DayRow(d, i, gMin, gMax, if (i == 0) f.current.temp else null, settings, expanded == i) {
                expanded = if (expanded == i) -1 else i
            }
        }
    }
}

@Composable
private fun DayRow(
    d: DayWeather, i: Int, gMin: Double, gMax: Double, now: Double?, settings: Settings,
    expanded: Boolean, onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateFloatAsState(if (expanded) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "bg")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((20 + 8 * bg).dp))
            .background(cs.surfaceContainerHighest.copy(alpha = 0.7f * bg))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayLabel(d.date, i), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(92.dp), maxLines = 1)
            WeatherGlyph(d.code, true, Modifier.size(30.dp))
            Text(
                if (d.precipProb >= 10) "${d.precipProb}%" else "",
                style = MaterialTheme.typography.labelMedium, color = cs.primary,
                modifier = Modifier.width(40.dp).padding(start = 4.dp),
            )
            Text(settings.temp(d.min), color = cs.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
            Canvas(Modifier.weight(1f).height(10.dp).padding(horizontal = 10.dp)) {
                val range = (gMax - gMin).coerceAtLeast(1.0)
                val r = CornerRadius(size.height / 2)
                drawRoundRect(cs.surfaceVariant, cornerRadius = r)
                val x0 = ((d.min - gMin) / range).toFloat() * size.width
                val x1 = ((d.max - gMin) / range).toFloat() * size.width
                drawRoundRect(
                    Brush.horizontalGradient(listOf(tempColor(d.min), tempColor(d.max)), startX = x0, endX = x1),
                    topLeft = Offset(x0, 0f), size = Size((x1 - x0).coerceAtLeast(size.height), size.height), cornerRadius = r,
                )
                if (now != null) {
                    val xn = ((now - gMin) / range).toFloat().coerceIn(0f, 1f) * size.width
                    drawCircle(cs.surface, radius = size.height * 0.75f, center = Offset(xn, size.height / 2))
                    drawCircle(cs.onSurface, radius = size.height * 0.45f, center = Offset(xn, size.height / 2))
                }
            }
            Text(settings.temp(d.max), fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp))
        }
        AnimatedVisibility(
            expanded,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
        ) {
            Column(Modifier.padding(top = 10.dp)) {
                Text(Wmo.describe(d.code), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val (w, wu) = settings.wind(d.windMaxMs)
                    MiniStat("Восход", hhmm(d.sunrise), Modifier.weight(1f))
                    MiniStat("Закат", hhmm(d.sunset), Modifier.weight(1f))
                    MiniStat("Ветер", "$w $wu", Modifier.weight(1f))
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("УФ", "${d.uvMax.roundToInt()} · ${uvLevel(d.uvMax).lowercase()}", Modifier.weight(1f))
                    MiniStat("Осадки", String.format(java.util.Locale("ru"), "%.1f мм", d.precipSum), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontSize = 14.sp, maxLines = 1)
        }
    }
}
