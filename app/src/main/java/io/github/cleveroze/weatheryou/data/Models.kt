package io.github.cleveroze.weatheryou.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val admin: String? = null,
    val country: String? = null
) {
    val subtitle: String
        get() = listOfNotNull(admin, country)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
}

enum class WeatherKind {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM
}

fun weatherKind(code: Int): WeatherKind = when (code) {
    0, 1 -> WeatherKind.CLEAR
    2 -> WeatherKind.PARTLY_CLOUDY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    51, 53, 55, 56, 57 -> WeatherKind.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
    95, 96, 99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

fun conditionText(code: Int): String = when (code) {
    0 -> "Ясно"
    1 -> "Почти ясно"
    2 -> "Переменная облачность"
    3 -> "Пасмурно"
    45, 48 -> "Туман"
    51 -> "Слабая морось"
    53 -> "Морось"
    55 -> "Сильная морось"
    56, 57 -> "Ледяная морось"
    61 -> "Небольшой дождь"
    63 -> "Дождь"
    65 -> "Сильный дождь"
    66, 67 -> "Ледяной дождь"
    71 -> "Небольшой снег"
    73 -> "Снег"
    75 -> "Сильный снег"
    77 -> "Снежная крупа"
    80 -> "Небольшой ливень"
    81 -> "Ливень"
    82 -> "Сильный ливень"
    85, 86 -> "Снегопад"
    95 -> "Гроза"
    96, 99 -> "Гроза с градом"
    else -> "Облачно"
}

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val code: Int,
    val isDay: Boolean,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val precipitation: Double,
    val pressure: Double
)

data class HourForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val code: Int,
    val precipitationProbability: Int
)

data class DayForecast(
    val date: LocalDate,
    val code: Int,
    val max: Double,
    val min: Double,
    val sunrise: String,
    val sunset: String
)

data class Weather(
    val current: CurrentWeather,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>
)

fun formatTemp(value: Double): String {
    val rounded = value.roundToInt()
    return if (rounded > 0) "+$rounded°" else "$rounded°"
}

fun formatTempShort(value: Double): String = "${value.roundToInt()}°"

fun formatHour(time: LocalDateTime): String =
    time.format(DateTimeFormatter.ofPattern("HH:mm"))

fun formatUpdateTime(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Сегодня"
        today.plusDays(1) -> "Завтра"
        else -> date.format(DateTimeFormatter.ofPattern("EE", Locale("ru")))
            .replaceFirstChar { it.uppercase() }
    }
}

fun windDirection(degrees: Int): String {
    val directions = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
    val index = (((degrees % 360) + 360) % 360 + 22) / 45 % 8
    return directions[index]
}
