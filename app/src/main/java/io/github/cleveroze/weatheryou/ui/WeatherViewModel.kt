package io.github.cleveroze.weatheryou.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.cleveroze.weatheryou.data.City
import io.github.cleveroze.weatheryou.data.Weather
import io.github.cleveroze.weatheryou.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val city: City,
    val weather: Weather? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val offline: Boolean = false,
    val updatedAt: Long? = null,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<City> = emptyList(),
    val searching: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application)
    private val _state = MutableStateFlow(UiState(city = repository.savedCity()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        val city = _state.value.city
        val cached = repository.cached(city)
        if (cached != null) {
            _state.update {
                it.copy(
                    weather = cached,
                    loading = false,
                    updatedAt = repository.cachedAt(city).takeIf { time -> time > 0 }
                )
            }
        }
        refresh()
    }

    fun refresh() {
        val city = _state.value.city
        val hasWeather = _state.value.weather != null

        loadJob?.cancel()
        _state.update {
            it.copy(
                loading = !hasWeather,
                refreshing = hasWeather,
                error = null
            )
        }

        loadJob = viewModelScope.launch {
            runCatching { repository.fetch(city) }
                .onSuccess { weather ->
                    _state.update {
                        it.copy(
                            weather = weather,
                            loading = false,
                            refreshing = false,
                            error = null,
                            offline = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure {
                    _state.update { current ->
                        current.copy(
                            loading = false,
                            refreshing = false,
                            offline = current.weather != null,
                            error = if (current.weather == null) {
                                "Не удалось загрузить погоду. Проверь подключение."
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    fun openSearch() {
        _state.update {
            it.copy(
                searchOpen = true,
                searchQuery = "",
                searchResults = emptyList(),
                searching = false
            )
        }
    }

    fun closeSearch() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                searchOpen = false,
                searchQuery = "",
                searchResults = emptyList(),
                searching = false
            )
        }
    }

    fun onQueryChange(query: String) {
        searchJob?.cancel()

        if (query.trim().length < 2) {
            _state.update { it.copy(searchQuery = query, searchResults = emptyList(), searching = false) }
            return
        }

        _state.update { it.copy(searchQuery = query, searching = true) }

        searchJob = viewModelScope.launch {
            delay(320)
            val results = runCatching { repository.search(query) }.getOrDefault(emptyList())
            if (_state.value.searchQuery == query) {
                _state.update { it.copy(searchResults = results, searching = false) }
            }
        }
    }

    fun selectCity(city: City) {
        if (city.latitude == _state.value.city.latitude &&
            city.longitude == _state.value.city.longitude
        ) {
            closeSearch()
            return
        }

        repository.saveCity(city)
        val cached = repository.cached(city)
        _state.value = UiState(
            city = city,
            weather = cached,
            loading = cached == null,
            updatedAt = repository.cachedAt(city).takeIf { it > 0 }
        )
        refresh()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeatherViewModel(application) as T
        }
    }
}
