package io.github.cleveroze.weatheryou.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class TempUnit { C, F }
enum class WindUnit { MS, KMH }
enum class PressureUnit { MMHG, HPA }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val tempUnit: TempUnit = TempUnit.C,
    val windUnit: WindUnit = WindUnit.MS,
    val pressureUnit: PressureUnit = PressureUnit.MMHG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** "wallpaper" = Material You dynamic color, otherwise a seed color like "#8B5E3C" */
    val colorSource: String = "#8B5E3C",
    val animations: Boolean = true,
    val weatherTint: Boolean = true,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("weatheryou", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<Settings> = _settings

    private fun readSettings() = Settings(
        tempUnit = enumOr(sp.getString("tempUnit", null), TempUnit.C),
        windUnit = enumOr(sp.getString("windUnit", null), WindUnit.MS),
        pressureUnit = enumOr(sp.getString("pressureUnit", null), PressureUnit.MMHG),
        themeMode = enumOr(sp.getString("themeMode", null), ThemeMode.SYSTEM),
        colorSource = sp.getString("colorSource", "#8B5E3C") ?: "#8B5E3C",
        animations = sp.getBoolean("animations", true),
        weatherTint = sp.getBoolean("weatherTint", true),
    )

    fun update(block: (Settings) -> Settings) {
        val s = block(_settings.value)
        _settings.value = s
        sp.edit()
            .putString("tempUnit", s.tempUnit.name)
            .putString("windUnit", s.windUnit.name)
            .putString("pressureUnit", s.pressureUnit.name)
            .putString("themeMode", s.themeMode.name)
            .putString("colorSource", s.colorSource)
            .putBoolean("animations", s.animations)
            .putBoolean("weatherTint", s.weatherTint)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(v: String?, def: E): E =
        runCatching { enumValueOf<E>(v!!) }.getOrDefault(def)

    // ---- places ----
    var savedPlaces: List<Place>
        get() = runCatching {
            val arr = JSONArray(sp.getString("places", "[]"))
            (0 until arr.length()).map { placeFrom(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
        set(v) { sp.edit().putString("places", JSONArray(v.map { placeTo(it) }).toString()).apply() }

    /** null = follow the device location */
    var selectedPlace: Place?
        get() = sp.getString("selected", null)?.let { runCatching { placeFrom(JSONObject(it)) }.getOrNull() }
        set(v) { sp.edit().putString("selected", v?.let { placeTo(it).toString() }).apply() }

    var askedLocation: Boolean
        get() = sp.getBoolean("askedLocation", false)
        set(v) { sp.edit().putBoolean("askedLocation", v).apply() }

    // ---- offline cache of the last forecast ----
    fun saveCache(place: Place, forecastJson: String, airJson: String?) {
        sp.edit().putString("cachePlace", placeTo(place).toString())
            .putString("cacheForecast", forecastJson)
            .putString("cacheAir", airJson)
            .putLong("cacheTime", System.currentTimeMillis())
            .apply()
    }

    fun loadCache(): Forecast? = runCatching {
        val place = placeFrom(JSONObject(sp.getString("cachePlace", null)!!))
        val air = sp.getString("cacheAir", null)?.let { WeatherApi.parseAir(JSONObject(it)) }
        WeatherApi.parseForecast(place, JSONObject(sp.getString("cacheForecast", null)!!), air)
            .copy(fetchedAt = sp.getLong("cacheTime", 0))
    }.getOrNull()

    companion object {
        fun placeTo(p: Place) = JSONObject()
            .put("name", p.name).put("region", p.region).put("lat", p.lat).put("lon", p.lon)
            .put("current", p.isCurrent).put("approx", p.approximate)

        fun placeFrom(o: JSONObject) = Place(
            o.getString("name"), o.optString("region"), o.getDouble("lat"), o.getDouble("lon"),
            o.optBoolean("current"), o.optBoolean("approx"),
        )
    }
}
