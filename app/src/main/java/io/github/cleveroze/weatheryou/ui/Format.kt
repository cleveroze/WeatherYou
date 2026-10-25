package io.github.cleveroze.weatheryou.ui

import io.github.cleveroze.weatheryou.data.PressureUnit
import io.github.cleveroze.weatheryou.data.Settings
import io.github.cleveroze.weatheryou.data.TempUnit
import io.github.cleveroze.weatheryou.data.WindUnit
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val RU = Locale("ru")

fun Settings.tempValue(c: Double): Double = if (tempUnit == TempUnit.F) c * 9 / 5 + 32 else c

fun Settings.temp(c: Double): String {
    val v = tempValue(c).roundToInt()
    return (if (v < 0) "−${-v}" else "$v") + "°"
}

fun Settings.wind(ms: Double): Pair<String, String> =
    if (windUnit == WindUnit.KMH) (ms * 3.6).roundToInt().toString() to "км/ч"
    else (if (ms < 10) String.format(RU, "%.1f", ms).replace(",0", "") else ms.roundToInt().toString()) to "м/с"

fun Settings.pressure(hpa: Double): Pair<String, String> =
    if (pressureUnit == PressureUnit.HPA) hpa.roundToInt().toString() to "гПа"
    else (hpa * 0.750062).roundToInt().toString() to "мм рт. ст."

/** "2026-10-25T14:00" → "14:00" */
fun hhmm(iso: String): String = iso.substringAfter('T', iso).take(5)

fun minutesOfDay(iso: String): Int = runCatching {
    val t = hhmm(iso)
    t.take(2).toInt() * 60 + t.substring(3, 5).toInt()
}.getOrDefault(0)

fun dayLabel(date: String, index: Int): String = when (index) {
    0 -> "Сегодня"
    1 -> "Завтра"
    else -> runCatching {
        val d = LocalDate.parse(date)
        d.dayOfWeek.getDisplayName(TextStyle.SHORT, RU).replaceFirstChar { it.titlecase(RU) } + ", " + d.dayOfMonth
    }.getOrDefault(date)
}

fun windDirName(deg: Int): String {
    val names = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
    return names[(((deg % 360) + 360) % 360 + 22) / 45 % 8]
}

fun windDirLong(deg: Int): String = when (windDirName(deg)) {
    "С" -> "северный"; "СВ" -> "северо-восточный"; "В" -> "восточный"; "ЮВ" -> "юго-восточный"
    "Ю" -> "южный"; "ЮЗ" -> "юго-западный"; "З" -> "западный"; else -> "северо-западный"
}

fun uvLevel(uv: Double): String = when {
    uv < 3 -> "Низкий"
    uv < 6 -> "Умеренный"
    uv < 8 -> "Высокий"
    uv < 11 -> "Очень высокий"
    else -> "Экстремальный"
}

fun aqiLevel(aqi: Int): String = when {
    aqi <= 20 -> "Отличный"
    aqi <= 40 -> "Хороший"
    aqi <= 60 -> "Умеренный"
    aqi <= 80 -> "Плохой"
    aqi <= 100 -> "Очень плохой"
    else -> "Опасный"
}

fun relativeUpdated(fetchedAt: Long, now: Long = System.currentTimeMillis()): String {
    val min = ((now - fetchedAt) / 60_000).toInt()
    return when {
        min < 1 -> "только что"
        min < 60 -> "$min мин назад"
        min < 60 * 24 -> "${min / 60} ч назад"
        else -> "давно"
    }
}
