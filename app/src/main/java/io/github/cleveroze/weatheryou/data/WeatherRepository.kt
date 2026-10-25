package io.github.cleveroze.weatheryou.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherRepository(context: Context) {

    private val prefs = context.getSharedPreferences("weatheryou", Context.MODE_PRIVATE)

    fun savedCity(): City {
        val raw = prefs.getString(KEY_CITY, null) ?: return DEFAULT_CITY
        return runCatching {
            val json = JSONObject(raw)
            City(
                name = json.getString("name"),
                latitude = json.getDouble("lat"),
                longitude = json.getDouble("lon"),
                admin = json.optString("admin").takeIf { it.isNotBlank() },
                country = json.optString("country").takeIf { it.isNotBlank() }
            )
        }.getOrDefault(DEFAULT_CITY)
    }

    fun saveCity(city: City) {
        val json = JSONObject().apply {
            put("name", city.name)
            put("lat", city.latitude)
            put("lon", city.longitude)
            put("admin", city.admin.orEmpty())
            put("country", city.country.orEmpty())
        }
        prefs.edit().putString(KEY_CITY, json.toString()).apply()
    }

    fun cached(city: City): Weather? {
        val raw = prefs.getString(cacheKey(city), null) ?: return null
        return runCatching { parseForecast(raw) }.getOrNull()
    }

    fun cachedAt(city: City): Long = prefs.getLong(cacheTimeKey(city), 0L)

    suspend fun fetch(city: City): Weather = withContext(Dispatchers.IO) {
        val raw = httpGet(forecastUrl(city))
        val weather = parseForecast(raw)
        prefs.edit()
            .putString(cacheKey(city), raw)
            .putLong(cacheTimeKey(city), System.currentTimeMillis())
            .apply()
        weather
    }

    suspend fun search(query: String): List<City> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
            URLEncoder.encode(trimmed, "UTF-8") +
            "&count=8&language=ru&format=json"
        val root = JSONObject(httpGet(url))
        val results = root.optJSONArray("results") ?: return@withContext emptyList()

        buildList {
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                val lat = item.optDouble("latitude", Double.NaN)
                val lon = item.optDouble("longitude", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue
                add(
                    City(
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        admin = item.optString("admin1").takeIf { it.isNotBlank() },
                        country = item.optString("country").takeIf { it.isNotBlank() }
                            ?: item.optString("country_code").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun forecastUrl(city: City): String {
        val lat = String.format(Locale.US, "%.4f", city.latitude)
        val lon = String.format(Locale.US, "%.4f", city.longitude)
        return buildString {
            append("https://api.open-meteo.com/v1/forecast?latitude=")
            append(lat)
            append("&longitude=")
            append(lon)
            append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,")
            append("precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m")
            append("&hourly=temperature_2m,weather_code,precipitation_probability")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset")
            append("&timezone=auto&forecast_days=7&wind_speed_unit=ms")
        }
    }

    private fun parseForecast(raw: String): Weather {
        val root = JSONObject(raw)
        val currentJson = root.getJSONObject("current")
        val currentTime = runCatching {
            LocalDateTime.parse(currentJson.getString("time"))
        }.getOrElse { LocalDateTime.now() }
        val nowHour = currentTime.withMinute(0).withSecond(0).withNano(0)

        val current = CurrentWeather(
            temperature = currentJson.optDouble("temperature_2m", 0.0),
            feelsLike = currentJson.optDouble("apparent_temperature", 0.0),
            code = currentJson.optInt("weather_code", 0),
            isDay = currentJson.optInt("is_day", 1) == 1,
            humidity = currentJson.optInt("relative_humidity_2m", 0),
            windSpeed = currentJson.optDouble("wind_speed_10m", 0.0),
            windDirection = currentJson.optInt("wind_direction_10m", 0),
            precipitation = currentJson.optDouble("precipitation", 0.0),
            pressure = currentJson.optDouble("surface_pressure", 0.0)
        )

        val hourlyJson = root.getJSONObject("hourly")
        val times = hourlyJson.getJSONArray("time")
        val temperatures = hourlyJson.getJSONArray("temperature_2m")
        val codes = hourlyJson.getJSONArray("weather_code")
        val probabilities = hourlyJson.optJSONArray("precipitation_probability")

        val hourly = ArrayList<HourForecast>(24)
        for (i in 0 until times.length()) {
            val time = runCatching { LocalDateTime.parse(times.getString(i)) }.getOrNull() ?: continue
            if (time.isBefore(nowHour)) continue
            hourly += HourForecast(
                time = time,
                temperature = temperatures.optDouble(i, 0.0),
                code = codes.optInt(i, 0),
                precipitationProbability = probabilities?.optInt(i, 0) ?: 0
            )
            if (hourly.size == 24) break
        }

        val dailyJson = root.getJSONObject("daily")
        val dates = dailyJson.getJSONArray("time")
        val dailyCodes = dailyJson.getJSONArray("weather_code")
        val maxes = dailyJson.getJSONArray("temperature_2m_max")
        val mins = dailyJson.getJSONArray("temperature_2m_min")
        val sunrises = dailyJson.optJSONArray("sunrise")
        val sunsets = dailyJson.optJSONArray("sunset")

        val daily = ArrayList<DayForecast>(7)
        for (i in 0 until dates.length()) {
            val date = runCatching { LocalDate.parse(dates.getString(i)) }.getOrNull() ?: continue
            daily += DayForecast(
                date = date,
                code = dailyCodes.optInt(i, 0),
                max = maxes.optDouble(i, 0.0),
                min = mins.optDouble(i, 0.0),
                sunrise = formatClock(sunrises?.optString(i).orEmpty()),
                sunset = formatClock(sunsets?.optString(i).orEmpty())
            )
        }

        return Weather(
            current = current,
            hourly = if (hourly.isEmpty()) buildFallbackHours(nowHour, current) else hourly,
            daily = daily
        )
    }

    private fun buildFallbackHours(now: LocalDateTime, current: CurrentWeather): List<HourForecast> =
        (0 until 24).map { index ->
            HourForecast(
                time = now.plusHours(index.toLong()),
                temperature = current.temperature,
                code = current.code,
                precipitationProbability = 0
            )
        }

    private fun formatClock(value: String): String = runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("—")

    private fun cacheKey(city: City): String =
        "cache_${String.format(Locale.US, "%.3f_%.3f", city.latitude, city.longitude)}"

    private fun cacheTimeKey(city: City): String =
        "cache_time_${String.format(Locale.US, "%.3f_%.3f", city.latitude, city.longitude)}"

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "WeatherYou/0.1 (Android)")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("Open-Meteo HTTP $code")
            body
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val KEY_CITY = "city"

        val DEFAULT_CITY = City(
            name = "Новосибирск",
            latitude = 55.0302,
            longitude = 82.9204,
            admin = "Новосибирская область",
            country = "Россия"
        )
    }
}
