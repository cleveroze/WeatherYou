package io.github.cleveroze.weatheryou.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Open-Meteo (forecast, air quality, geocoding) — free and keyless. */
object WeatherApi {

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "WeatherYou/1.0 (Android)")
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun fmt(d: Double) = String.format(Locale.US, "%.4f", d)

    class Result(val forecast: Forecast, val forecastJson: String, val airJson: String?)

    suspend fun forecast(place: Place): Result = coroutineScope {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${fmt(place.lat)}&longitude=${fmt(place.lon)}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code," +
            "cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,dew_point_2m,uv_index,visibility" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
            "precipitation_probability_max,precipitation_sum,wind_speed_10m_max" +
            "&wind_speed_unit=ms&timezone=auto&forecast_days=10"
        val airUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${fmt(place.lat)}" +
            "&longitude=${fmt(place.lon)}&current=european_aqi,pm2_5,pm10&timezone=auto"
        val airJson = async { runCatching { get(airUrl) }.getOrNull() }
        val json = get(url)
        val airRaw = airJson.await()
        val air = airRaw?.let { runCatching { parseAir(JSONObject(it)) }.getOrNull() }
        Result(parseForecast(place, JSONObject(json), air), json, airRaw)
    }

    fun parseAir(o: JSONObject): AirQuality? {
        val c = o.optJSONObject("current") ?: return null
        if (c.isNull("european_aqi")) return null
        return AirQuality(c.optInt("european_aqi"), c.optDouble("pm2_5", 0.0), c.optDouble("pm10", 0.0))
    }

    fun parseForecast(place: Place, o: JSONObject, air: AirQuality?): Forecast {
        val c = o.getJSONObject("current")
        val current = CurrentWeather(
            temp = c.optDouble("temperature_2m", 0.0),
            feelsLike = c.optDouble("apparent_temperature", 0.0),
            humidity = c.optInt("relative_humidity_2m"),
            dewPoint = c.optDouble("dew_point_2m", 0.0),
            pressureHpa = c.optDouble("pressure_msl", 1013.0),
            windSpeedMs = c.optDouble("wind_speed_10m", 0.0),
            windGustMs = c.optDouble("wind_gusts_10m", 0.0),
            windDir = c.optInt("wind_direction_10m"),
            cloudCover = c.optInt("cloud_cover"),
            precipitation = c.optDouble("precipitation", 0.0),
            uvIndex = c.optDouble("uv_index", 0.0).let { if (it.isNaN()) 0.0 else it },
            visibilityM = c.optDouble("visibility", 10_000.0).let { if (it.isNaN()) 10_000.0 else it },
            isDay = c.optInt("is_day", 1) == 1,
            code = c.optInt("weather_code"),
        )
        val nowLocal = c.optString("time")

        val h = o.getJSONObject("hourly")
        val ht = h.getJSONArray("time")
        val hours = (0 until ht.length()).map { i ->
            HourWeather(
                time = ht.getString(i),
                temp = h.getJSONArray("temperature_2m").optD(i),
                code = h.getJSONArray("weather_code").optInt(i),
                precipProb = h.getJSONArray("precipitation_probability").optInt(i),
                isDay = h.getJSONArray("is_day").optInt(i, 1) == 1,
            )
        }.filter { it.time.take(13) >= nowLocal.take(13) }.take(25)

        val d = o.getJSONObject("daily")
        val dt = d.getJSONArray("time")
        val days = (0 until dt.length()).map { i ->
            DayWeather(
                date = dt.getString(i),
                code = d.getJSONArray("weather_code").optInt(i),
                max = d.getJSONArray("temperature_2m_max").optD(i),
                min = d.getJSONArray("temperature_2m_min").optD(i),
                precipProb = d.getJSONArray("precipitation_probability_max").optInt(i),
                precipSum = d.getJSONArray("precipitation_sum").optD(i),
                sunrise = d.getJSONArray("sunrise").optString(i),
                sunset = d.getJSONArray("sunset").optString(i),
                uvMax = d.getJSONArray("uv_index_max").optD(i),
                windMaxMs = d.getJSONArray("wind_speed_10m_max").optD(i),
            )
        }
        return Forecast(place, current, hours, days, air, nowLocal, System.currentTimeMillis())
    }

    private fun JSONArray.optD(i: Int): Double = optDouble(i, 0.0).let { if (it.isNaN()) 0.0 else it }

    /** City search (Open-Meteo geocoding, Russian names). */
    suspend fun searchCities(query: String): List<Place> {
        if (query.isBlank()) return emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val o = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=12&language=ru&format=json"))
        val arr = o.optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            val region = listOf(r.optString("admin1"), r.optString("country"))
                .filter { it.isNotBlank() && it != "null" }.distinct().joinToString(", ")
            Place(r.getString("name"), region, r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }
}
