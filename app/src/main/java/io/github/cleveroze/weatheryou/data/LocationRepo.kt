package io.github.cleveroze.weatheryou.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume

/** Finds where the user is: GPS/network location first, IP geolocation as a fallback. */
class LocationRepo(private val context: Context) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun currentPlace(): Place {
        val loc = if (hasPermission()) deviceLocation() else null
        if (loc != null) {
            val (name, region) = reverseGeocode(loc.latitude, loc.longitude)
            return Place(name, region, loc.latitude, loc.longitude, isCurrent = true)
        }
        return ipPlace()
    }

    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null

        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 30 * 60 * 1000) return last

        val fresh = withTimeoutOrNull(9_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
                if (provider == null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val signal = android.os.CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    lm.getCurrentLocation(provider, signal, context.mainExecutor) { l ->
                        if (cont.isActive) cont.resume(l)
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            lm.removeUpdates(this)
                            if (cont.isActive) cont.resume(location)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }
            }
        }
        return fresh ?: last
    }

    private suspend fun reverseGeocode(lat: Double, lon: Double): Pair<String, String> {
        // 1) Android geocoder
        val g = runCatching {
            withTimeoutOrNull(5_000) { geocoder(lat, lon) }
        }.getOrNull()
        if (g != null) return g
        // 2) BigDataCloud (free, keyless)
        return runCatching {
            val o = JSONObject(
                WeatherApi.get(
                    "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=ru"
                )
            )
            val city = o.optString("city").ifBlank { o.optString("locality") }.ifBlank { "Здесь" }
            city to listOf(o.optString("principalSubdivision"), o.optString("countryName"))
                .filter { it.isNotBlank() }.joinToString(", ")
        }.getOrDefault("Моё место" to "")
    }

    @Suppress("DEPRECATION")
    private suspend fun geocoder(lat: Double, lon: Double): Pair<String, String>? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale("ru"))
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }
                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
        } else withContext(Dispatchers.IO) { geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() }
        address ?: return null
        val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: return null
        val region = listOfNotNull(address.adminArea, address.countryName).distinct().joinToString(", ")
        return name to region
    }

    /** Approximate location by IP address — used when permission is denied or GPS is off. */
    suspend fun ipPlace(): Place {
        runCatching {
            val o = JSONObject(WeatherApi.get("https://ipwho.is/?lang=ru"))
            if (o.optBoolean("success", false)) {
                return Place(
                    o.optString("city", "Моё место"),
                    listOf(o.optString("region"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                    o.getDouble("latitude"), o.getDouble("longitude"),
                    isCurrent = true, approximate = true,
                )
            }
        }
        val o = JSONObject(WeatherApi.get("https://get.geojs.io/v1/ip/geo.json"))
        return Place(
            o.optString("city", "Моё место"),
            listOf(o.optString("region"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
            o.getString("latitude").toDouble(), o.getString("longitude").toDouble(),
            isCurrent = true, approximate = true,
        )
    }
}
