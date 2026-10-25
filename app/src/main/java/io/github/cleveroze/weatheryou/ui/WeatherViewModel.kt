package io.github.cleveroze.weatheryou.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cleveroze.weatheryou.data.Forecast
import io.github.cleveroze.weatheryou.data.LocationRepo
import io.github.cleveroze.weatheryou.data.Place
import io.github.cleveroze.weatheryou.data.Prefs
import io.github.cleveroze.weatheryou.data.Settings
import io.github.cleveroze.weatheryou.data.WeatherApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data class Loading(val stage: String) : UiState
    data class Ready(
        val forecast: Forecast,
        val refreshing: Boolean = false,
        /** shown from cache because the network failed */
        val offline: Boolean = false,
    ) : UiState
    data class Error(val message: String) : UiState
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val location = LocationRepo(app)

    val settings: StateFlow<Settings> = prefs.settings

    private val _state = MutableStateFlow<UiState>(UiState.Loading("Запускаемся…"))
    val state: StateFlow<UiState> = _state

    private val _saved = MutableStateFlow(prefs.savedPlaces)
    val savedPlaces: StateFlow<List<Place>> = _saved

    private val _search = MutableStateFlow<List<Place>>(emptyList())
    val searchResults: StateFlow<List<Place>> = _search
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    /** null = follow device location */
    private val _selected = MutableStateFlow(prefs.selectedPlace)
    val selected: StateFlow<Place?> = _selected

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var started = false

    fun hasLocationPermission() = location.hasPermission()

    fun start() {
        if (started) return
        started = true
        prefs.loadCache()?.let { cached ->
            if (_selected.value == null || cached.place.lat == _selected.value?.lat) {
                _state.value = UiState.Ready(cached, refreshing = true)
            }
        }
        load()
    }

    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = _state.value
            if (current is UiState.Ready) _state.value = current.copy(refreshing = true)
            try {
                val place = _selected.value ?: run {
                    if (current !is UiState.Ready) _state.value = UiState.Loading("Определяем, где вы…")
                    location.currentPlace()
                }
                if (_state.value is UiState.Loading) _state.value = UiState.Loading("Загружаем прогноз для ${place.name}…")
                val t0 = System.currentTimeMillis()
                val r = WeatherApi.forecast(place)
                // let the expressive loading indicator finish at least one morph cycle
                val elapsed = System.currentTimeMillis() - t0
                if (current !is UiState.Ready && elapsed < 700) delay(700 - elapsed)
                prefs.saveCache(place, r.forecastJson, r.airJson)
                _state.value = UiState.Ready(r.forecast)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val cached = (current as? UiState.Ready)?.forecast ?: prefs.loadCache()
                _state.value = if (cached != null) UiState.Ready(cached, offline = true)
                else UiState.Error(
                    if (e is java.io.IOException) "Нет связи с сервером погоды. Проверьте интернет."
                    else "Не удалось загрузить погоду: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    fun selectPlace(place: Place?) {
        _selected.value = place
        prefs.selectedPlace = place
        if (place != null && _saved.value.none { it.lat == place.lat && it.lon == place.lon }) {
            _saved.value = listOf(place) + _saved.value
            prefs.savedPlaces = _saved.value
        }
        _state.value = (_state.value as? UiState.Ready)?.copy(refreshing = true) ?: UiState.Loading("Загружаем…")
        load()
    }

    fun removePlace(place: Place) {
        _saved.value = _saved.value.filterNot { it.lat == place.lat && it.lon == place.lon }
        prefs.savedPlaces = _saved.value
        if (_selected.value?.lat == place.lat && _selected.value?.lon == place.lon) selectPlace(null)
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) { _search.value = emptyList(); _searching.value = false; return }
        searchJob = viewModelScope.launch {
            _searching.value = true
            delay(300) // debounce
            _search.value = runCatching { WeatherApi.searchCities(query) }.getOrDefault(emptyList())
            _searching.value = false
        }
    }

    fun updateSettings(block: (Settings) -> Settings) = prefs.update(block)
}
