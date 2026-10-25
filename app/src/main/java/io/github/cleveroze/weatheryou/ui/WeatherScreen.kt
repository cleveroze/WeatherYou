package io.github.cleveroze.weatheryou.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.weatheryou.data.City
import io.github.cleveroze.weatheryou.data.DayForecast
import io.github.cleveroze.weatheryou.data.HourForecast
import io.github.cleveroze.weatheryou.data.Weather
import io.github.cleveroze.weatheryou.data.WeatherKind
import io.github.cleveroze.weatheryou.data.conditionText
import io.github.cleveroze.weatheryou.data.dayLabel
import io.github.cleveroze.weatheryou.data.formatHour
import io.github.cleveroze.weatheryou.data.formatTemp
import io.github.cleveroze.weatheryou.data.formatTempShort
import io.github.cleveroze.weatheryou.data.formatUpdateTime
import io.github.cleveroze.weatheryou.data.weatherKind
import io.github.cleveroze.weatheryou.data.windDirection
import io.github.cleveroze.weatheryou.ui.theme.WeatherSystemBars
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WeatherRoute(viewModel: WeatherViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()

    WeatherSystemBars(darkTheme = dark)

    Box(modifier = Modifier.fillMaxSize()) {
        WeatherBackdrop(weather = state.weather, dark = dark)

        val weather = state.weather
        when {
            weather != null -> WeatherContent(
                state = state,
                onRefresh = viewModel::refresh,
                onCityClick = viewModel::openSearch
            )

            state.loading -> LoadingScreen(city = state.city)

            else -> ErrorScreen(
                message = state.error ?: "Не удалось загрузить погоду",
                onRetry = viewModel::refresh,
                onChangeCity = viewModel::openSearch
            )
        }
    }

    if (state.searchOpen) {
        CityPickerSheet(
            state = state,
            onDismiss = viewModel::closeSearch,
            onQueryChange = viewModel::onQueryChange,
            onPick = viewModel::selectCity
        )
    }
}

@Composable
private fun WeatherBackdrop(weather: Weather?, dark: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val accent = weather?.let { moodAccent(weatherKind(it.current.code), it.current.isDay) }
        ?: scheme.primary

    val top = lerp(scheme.surface, accent, if (dark) 0.20f else 0.28f)
    val middle = lerp(scheme.surface, accent, if (dark) 0.07f else 0.11f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to top,
                    0.52f to middle,
                    1f to scheme.surface
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.78f, size.height * 0.06f)
            val radius = size.width * 1.05f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (dark) 0.16f else 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherContent(
    state: UiState,
    onRefresh: () -> Unit,
    onCityClick: () -> Unit
) {
    val weather = state.weather ?: return
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = insets.calculateTopPadding() + 6.dp,
                bottom = insets.calculateBottomPadding() + 30.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Reveal(0) {
                    TopBar(state = state, onCityClick = onCityClick, onRefresh = onRefresh)
                }
            }
            item {
                Reveal(1) {
                    Hero(weather = weather)
                }
            }
            item {
                Reveal(2) {
                    HourlySection(weather = weather)
                }
            }
            item {
                Reveal(3) {
                    DetailsSection(weather = weather)
                }
            }
            item {
                Reveal(4) {
                    DailySection(weather = weather)
                }
            }
            item {
                Reveal(5) {
                    Footer()
                }
            }
        }
    }
}

@Composable
private fun TopBar(state: UiState, onCityClick: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onCityClick)
                .padding(horizontal = 2.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.city.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Выбрать город",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when {
                    state.offline -> "нет сети · данные в кэше"
                    state.refreshing -> "обновляем…"
                    state.updatedAt != null -> "обновлено ${formatUpdateTime(state.updatedAt!!)}"
                    else -> "сегодня"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (state.offline) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.refreshing && !state.loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить погоду",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Hero(weather: Weather) {
    val current = weather.current
    val kind = weatherKind(current.code)
    val target = current.temperature.roundToInt()
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 160f),
        label = "temperature"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(382.dp)
    ) {
        WeatherArt(
            kind = kind,
            isDay = current.isDay,
            modifier = Modifier
                .size(222.dp)
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTemp(animated.toDouble()),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = conditionText(current.code),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ощущается как ${formatTemp(current.feelsLike)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HourlySection(weather: Weather) {
    Column {
        SectionHeader("По часам", "24 часа")
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(weather.hourly) { index, hour ->
                    HourlyCell(hour = hour, isNow = index == 0)
                }
            }
        }
    }
}

@Composable
private fun HourlyCell(hour: HourForecast, isNow: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val container = if (isNow) scheme.primaryContainer else Color.Transparent
    val content = if (isNow) scheme.onPrimaryContainer else scheme.onSurface
    val kind = weatherKind(hour.code)
    val isDay = hour.time.hour in 7..20

    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isNow) "Сейчас" else formatHour(hour.time),
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.75f),
            maxLines = 1
        )
        Spacer(Modifier.height(7.dp))
        WeatherGlyph(kind = kind, isDay = isDay, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(7.dp))
        Text(
            text = formatTempShort(hour.temperature),
            style = MaterialTheme.typography.titleMedium,
            color = content
        )
        Text(
            text = if (hour.precipitationProbability > 0) "${hour.precipitationProbability}%" else " ",
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.5f),
            maxLines = 1
        )
    }
}

@Composable
private fun DetailsSection(weather: Weather) {
    val current = weather.current
    Column {
        SectionHeader("Детали")
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailTile(
                label = "Ветер",
                value = "${current.windSpeed.roundToInt()} м/с",
                sub = windDirection(current.windDirection),
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = "Влажность",
                value = "${current.humidity}%",
                sub = "относительная",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailTile(
                label = "Осадки",
                value = String.format(Locale.US, "%.1f", current.precipitation),
                sub = "мм за час",
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = "Давление",
                value = "${current.pressure.roundToInt()}",
                sub = "гПа",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        val day = weather.daily.firstOrNull()
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(Modifier.padding(18.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Рассвет",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        day?.sunrise ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Закат",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        day?.sunset ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailySection(weather: Weather) {
    if (weather.daily.isEmpty()) return

    val weekMin = weather.daily.minOf { it.min }
    val weekMax = weather.daily.maxOf { it.max }

    Column {
        SectionHeader("На неделю")
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                weather.daily.forEach { day ->
                    DayRow(day = day, weekMin = weekMin, weekMax = weekMax)
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DayForecast, weekMin: Double, weekMax: Double) {
    val scheme = MaterialTheme.colorScheme
    val isToday = day.date == java.time.LocalDate.now()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel(day.date),
            modifier = Modifier.width(68.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isToday) scheme.primary else scheme.onSurface,
            fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
        WeatherGlyph(
            kind = weatherKind(day.code),
            isDay = true,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = formatTempShort(day.min),
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        TempRange(
            day = day,
            weekMin = weekMin,
            weekMax = weekMax,
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatTempShort(day.max),
            modifier = Modifier.width(34.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface
        )
    }
}

@Composable
private fun TempRange(
    day: DayForecast,
    weekMin: Double,
    weekMax: Double,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Canvas(modifier) {
        val height = size.height
        val corner = CornerRadius(height / 2f)
        drawRoundRect(color = track, size = size, cornerRadius = corner)

        val span = (weekMax - weekMin).coerceAtLeast(1.0)
        val startFraction = ((day.min - weekMin) / span).toFloat().coerceIn(0f, 1f)
        val endFraction = ((day.max - weekMin) / span).toFloat().coerceIn(0f, 1f)
        val startX = size.width * startFraction
        val width = (size.width * (endFraction - startFraction)).coerceAtLeast(height)
        val clampedX = startX.coerceAtMost((size.width - width).coerceAtLeast(0f))

        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(primary.copy(alpha = 0.55f), primary)
            ),
            topLeft = Offset(clampedX, 0f),
            size = Size(width, height),
            cornerRadius = corner
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Footer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Open-Meteo · WeatherYou",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun LoadingScreen(city: City) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WeatherArt(
            kind = WeatherKind.PARTLY_CLOUDY,
            isDay = true,
            modifier = Modifier.size(198.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Загружаем погоду",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = city.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit, onChangeCity: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WeatherArt(
            kind = WeatherKind.CLOUDY,
            isDay = true,
            modifier = Modifier.size(172.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Не получилось",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text("Повторить")
        }
        TextButton(onClick = onChangeCity) {
            Text("Выбрать другой город")
        }
    }
}

@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(460, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(460, easing = FastOutSlowInEasing)) { height -> height / 22 }
    ) {
        content()
    }
}
