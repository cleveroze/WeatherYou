@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.weatheryou.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.weatheryou.data.Place
import io.github.cleveroze.weatheryou.data.PressureUnit
import io.github.cleveroze.weatheryou.data.Settings
import io.github.cleveroze.weatheryou.data.TempUnit
import io.github.cleveroze.weatheryou.data.ThemeMode
import io.github.cleveroze.weatheryou.data.WindUnit
import io.github.cleveroze.weatheryou.ui.components.MorphingShapeBox
import io.github.cleveroze.weatheryou.ui.components.PolygonShape
import io.github.cleveroze.weatheryou.ui.theme.SeedPresets
import io.github.cleveroze.weatheryou.ui.theme.dynamicColorAvailable
import io.github.cleveroze.weatheryou.ui.theme.seedScheme

@Composable
fun PlacesSheet(vm: WeatherViewModel, onDismiss: () -> Unit, onUseLocation: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val saved by vm.savedPlaces.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = { vm.search(""); onDismiss() }, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Города", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            TextField(
                value = query,
                onValueChange = { query = it; vm.search(it) },
                placeholder = { Text("Найти город") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (searching) LoadingIndicator(Modifier.size(28.dp))
                    else if (query.isNotEmpty()) IconButton(onClick = { query = ""; vm.search("") }) { Icon(Icons.Rounded.Close, "Очистить") }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.fillMaxWidth().height(460.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (query.isBlank()) {
                    item {
                        PlaceRow(
                            title = "Моё местоположение", subtitle = "Определять автоматически",
                            icon = { Icon(Icons.Rounded.LocationOn, null) },
                            selected = selected == null,
                            onClick = { onUseLocation(); onDismiss() },
                        )
                    }
                    if (saved.isNotEmpty()) item {
                        Text(
                            "Сохранённые", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(saved, key = { "s${it.lat},${it.lon}" }) { p ->
                        PlaceRow(
                            p.name, p.region, icon = { Icon(Icons.Rounded.Star, null) },
                            selected = selected?.lat == p.lat && selected?.lon == p.lon,
                            onClick = { vm.selectPlace(p); onDismiss() },
                            onRemove = { vm.removePlace(p) },
                        )
                    }
                } else {
                    items(results, key = { "r${it.lat},${it.lon}" }) { p ->
                        PlaceRow(p.name, p.region, icon = { Icon(Icons.Rounded.Place, null) }, selected = false, onClick = {
                            vm.selectPlace(p); vm.search(""); onDismiss()
                        })
                    }
                    if (results.isEmpty() && !searching) item {
                        Text(
                            "Ничего не нашлось", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(
    title: String, subtitle: String, icon: @Composable () -> Unit, selected: Boolean,
    onClick: () -> Unit, onRemove: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) cs.secondaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(PolygonShape(if (selected) MaterialShapes.Cookie9Sided else MaterialShapes.Circle))
                    .background(if (selected) cs.primary else cs.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides if (selected) cs.onPrimary else cs.onSurfaceVariant,
                ) { icon() }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Удалить") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(settings: Settings, update: ((Settings) -> Settings) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { Text("Настройки", style = MaterialTheme.typography.headlineSmall) }
            item {
                Group("Цвет") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (dynamicColorAvailable()) {
                            Swatch(
                                selected = settings.colorSource == "wallpaper",
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondaryContainer),
                                label = "Обои",
                                onClick = { update { it.copy(colorSource = "wallpaper") } },
                            )
                        }
                        SeedPresets.forEach { (name, hex) ->
                            val sc = remember(hex) { seedScheme(hex, false) }
                            Swatch(
                                selected = settings.colorSource.equals(hex, ignoreCase = true),
                                colors = listOf(sc.primary, sc.tertiary, sc.secondaryContainer),
                                label = name,
                                onClick = { update { it.copy(colorSource = hex) } },
                            )
                        }
                    }
                }
            }
            item {
                Group("Тема") {
                    Segmented(
                        listOf(ThemeMode.SYSTEM to "Авто", ThemeMode.LIGHT to "Светлая", ThemeMode.DARK to "Тёмная"),
                        settings.themeMode,
                    ) { v -> update { it.copy(themeMode = v) } }
                }
            }
            item {
                Group("Температура") {
                    Segmented(listOf(TempUnit.C to "°C", TempUnit.F to "°F"), settings.tempUnit) { v -> update { it.copy(tempUnit = v) } }
                }
            }
            item {
                Group("Ветер") {
                    Segmented(listOf(WindUnit.MS to "м/с", WindUnit.KMH to "км/ч"), settings.windUnit) { v -> update { it.copy(windUnit = v) } }
                }
            }
            item {
                Group("Давление") {
                    Segmented(listOf(PressureUnit.MMHG to "мм рт. ст.", PressureUnit.HPA to "гПа"), settings.pressureUnit) { v -> update { it.copy(pressureUnit = v) } }
                }
            }
            item {
                ToggleRow("Анимации", "Живое небо, морфинг фигур, частицы", settings.animations) { v -> update { it.copy(animations = v) } }
            }
            item {
                ToggleRow("Цвет неба по погоде", "Подмешивать настроение погоды к палитре", settings.weatherTint) { v -> update { it.copy(weatherTint = v) } }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MorphingShapeBox(MaterialShapes.Cookie12Sided, Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, spinMillis = 12_000)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("WeatherYou", style = MaterialTheme.typography.titleMedium)
                        Text("Данные: Open-Meteo · Material 3 Expressive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (v, label) ->
            SegmentedButton(
                selected = v == selected,
                onClick = { onSelect(v) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.clickable { onChange(!checked) }.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked, onCheckedChange = onChange,
                thumbContent = if (checked) { { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) } } else { null },
            )
        }
    }
}

/** Color swatch: a circle that morphs into a cookie when selected. */
@Composable
private fun Swatch(selected: Boolean, colors: List<Color>, label: String, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "s")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            MorphingShapeBox(
                target = if (selected) MaterialShapes.Cookie9Sided else MaterialShapes.Circle,
                modifier = Modifier.size(60.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                spin = selected,
                spinMillis = 16_000,
            )
            Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)) {
                Column {
                    Box(Modifier.size(40.dp, 20.dp).background(colors[0]))
                    Row {
                        Box(Modifier.size(20.dp).background(colors[1]))
                        Box(Modifier.size(20.dp).background(colors[2]))
                    }
                }
            }
            if (selected) Box(
                Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
