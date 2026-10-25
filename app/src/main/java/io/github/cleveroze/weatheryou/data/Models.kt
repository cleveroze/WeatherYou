package io.github.cleveroze.weatheryou.data

/** A place the forecast is shown for. */
data class Place(
    val name: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    /** true when the place came from the device location or IP lookup */
    val isCurrent: Boolean = false,
    /** true when only an approximate (IP based) location was available */
    val approximate: Boolean = false,
)

data class CurrentWeather(
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val dewPoint: Double,
    val pressureHpa: Double,
    val windSpeedMs: Double,
    val windGustMs: Double,
    val windDir: Int,
    val cloudCover: Int,
    val precipitation: Double,
    val uvIndex: Double,
    val visibilityM: Double,
    val isDay: Boolean,
    val code: Int,
)

data class HourWeather(
    val time: String,      // ISO local time "2026-10-25T14:00"
    val temp: Double,
    val code: Int,
    val precipProb: Int,
    val isDay: Boolean,
)

data class DayWeather(
    val date: String,      // "2026-10-25"
    val code: Int,
    val max: Double,
    val min: Double,
    val precipProb: Int,
    val precipSum: Double,
    val sunrise: String,
    val sunset: String,
    val uvMax: Double,
    val windMaxMs: Double,
)

data class AirQuality(val europeanAqi: Int, val pm25: Double, val pm10: Double)

data class Forecast(
    val place: Place,
    val current: CurrentWeather,
    val hours: List<HourWeather>,
    val days: List<DayWeather>,
    val air: AirQuality?,
    val nowLocal: String,  // local time at the place, "2026-10-25T14:23"
    val fetchedAt: Long,
)

/** Visual family of a WMO weather code. */
enum class Sky { Clear, PartlyCloudy, Cloudy, Fog, Drizzle, Rain, Snow, Thunder }

object Wmo {
    fun sky(code: Int): Sky = when (code) {
        0, 1 -> Sky.Clear
        2 -> Sky.PartlyCloudy
        3 -> Sky.Cloudy
        45, 48 -> Sky.Fog
        51, 53, 55, 56, 57 -> Sky.Drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> Sky.Rain
        71, 73, 75, 77, 85, 86 -> Sky.Snow
        95, 96, 99 -> Sky.Thunder
        else -> Sky.Cloudy
    }

    fun describe(code: Int, isDay: Boolean = true): String = when (code) {
        0 -> if (isDay) "Ясно" else "Ясная ночь"
        1 -> if (isDay) "Преимущественно ясно" else "Почти ясно"
        2 -> "Переменная облачность"
        3 -> "Пасмурно"
        45 -> "Туман"
        48 -> "Изморозь и туман"
        51 -> "Слабая морось"
        53 -> "Морось"
        55 -> "Сильная морось"
        56, 57 -> "Ледяная морось"
        61 -> "Небольшой дождь"
        63 -> "Дождь"
        65 -> "Ливень"
        66, 67 -> "Ледяной дождь"
        71 -> "Небольшой снег"
        73 -> "Снег"
        75 -> "Сильный снегопад"
        77 -> "Снежные зёрна"
        80 -> "Кратковременный дождь"
        81 -> "Ливневый дождь"
        82 -> "Сильный ливень"
        85 -> "Снегопад"
        86 -> "Сильный снегопад"
        95 -> "Гроза"
        96, 99 -> "Гроза с градом"
        else -> "Облачно"
    }
}
