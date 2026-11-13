package com.example.myweather.features.weather.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myweather.features.location.data.*
import com.example.myweather.features.location.domain.model.Coordinates
import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.data.*
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails
import com.example.myweather.network.requestFailureMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val cityNameRepository: CityNameRepository,
    private val locationRepository: LocationRepository,
    private val getWeatherForecast: GetWeatherForecastUseCase
) : ViewModel() {
    private val _viewState = MutableStateFlow(WeatherViewState())
    val viewState: StateFlow<WeatherViewState> = _viewState.asStateFlow()
    private val searchQuery = MutableStateFlow("")
    // Replay keeps the latest selection if it arrives while the startup cache is loading.
    private val selections = MutableSharedFlow<Coordinates>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        selections.flatMapLatest(::weatherFor)
            .buffer(0)
            .onStart {
                try {
                    weatherRepository.getCachedWeather()?.let { cached ->
                        emit(WeatherUpdate.Content(getWeatherForecast(cached), cached.locationName))
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("WeatherViewModel", requestFailureMessage(e))
                }
            }
            .onEach { update ->
                _viewState.update { state ->
                    when (update) {
                        WeatherUpdate.Loading -> state.copy(isRefreshing = true)
                        is WeatherUpdate.Content -> state.copy(weather = update.weather, cityName = update.cityName, isRefreshing = false)
                        WeatherUpdate.Failed -> state.copy(isRefreshing = false, message = "Unable to refresh weather")
                    }
                }
            }
            .launchIn(viewModelScope)

        searchQuery.transformLatest { query ->
            emit(SearchUpdate(query, isSearching = query.isNotEmpty()))
            if (query.isNotEmpty()) {
                // Keep the debounce inside transformLatest: new input cancels the request immediately.
                delay(1000)
                val locations = locationRepository.fetchLocationSuggestions(query)
                emit(SearchUpdate(query, suggestions = locations.orEmpty(), failed = locations == null))
            }
        }.buffer(0)
            .onEach { update ->
                if (update.query == searchQuery.value) {
                    _viewState.update { state ->
                        state.copy(searchQuery = update.query, isSearching = update.isSearching,
                            suggestions = update.suggestions, searchFailed = update.failed,
                            message = if (update.failed) "Failed to load location data" else state.message)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun weatherFor(coordinates: Coordinates): Flow<WeatherUpdate> {
        val forecast = flow<ForecastUpdate> {
            emit(ForecastUpdate.Loading)
            val weather = weatherRepository.refreshWeather(coordinates.latitude, coordinates.longitude)
            emit(ForecastUpdate.Content(getWeatherForecast(weather)))
        }.catch { error ->
            Log.w("WeatherViewModel", requestFailureMessage(error))
            emit(ForecastUpdate.Failed)
        }
        val cityName = flow {
            emit("")
            emit(cityNameRepository.fetchCityName(coordinates.latitude, coordinates.longitude).orEmpty())
        }.catch { error ->
            Log.w("WeatherViewModel", requestFailureMessage(error))
            emit("")
        }
        return combine(forecast, cityName) { weather, name ->
            when (weather) {
                ForecastUpdate.Loading -> WeatherUpdate.Loading
                is ForecastUpdate.Content -> WeatherUpdate.Content(weather.weather, name)
                ForecastUpdate.Failed -> WeatherUpdate.Failed
            }
        }.distinctUntilChanged().onEach { update ->
            if (update is WeatherUpdate.Content && update.cityName.isNotBlank()) {
                // The coordinate guard prevents a delayed label from naming a different cached place.
                runCatching { weatherRepository.updateLocationName(coordinates.latitude, coordinates.longitude, update.cityName) }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        Log.w("WeatherViewModel", requestFailureMessage(error))
                    }
            }
        }
    }

    fun selectLocation(latitude: Double, longitude: Double) {
        searchLocations("")
        _viewState.update { it.copy(isLocating = false, isRefreshing = true, message = null) }
        selections.tryEmit(Coordinates(latitude, longitude))
    }

    fun searchLocations(query: String) {
        val text = query.trim()
        if (text == searchQuery.value) return
        searchQuery.value = text
        _viewState.update { it.copy(searchQuery = text, suggestions = emptyList(), isSearching = text.isNotEmpty(), searchFailed = false) }
    }

    fun locationLookupStarted() {
        searchLocations("")
        _viewState.update { it.copy(isLocating = true, message = null) }
    }

    fun locationLookupFinished(message: String? = null) {
        _viewState.update { it.copy(isLocating = false, message = message ?: it.message) }
    }

    fun messageShown(message: String) {
        _viewState.update { if (it.message == message) it.copy(message = null) else it }
    }

    private data class SearchUpdate(
        val query: String,
        val isSearching: Boolean = false,
        val suggestions: List<LocationSuggestion> = emptyList(),
        val failed: Boolean = false
    )
    private sealed interface ForecastUpdate {
        data object Loading : ForecastUpdate
        data class Content(val weather: WeatherForecastDetails) : ForecastUpdate
        data object Failed : ForecastUpdate
    }
    private sealed interface WeatherUpdate {
        data object Loading : WeatherUpdate
        data class Content(val weather: WeatherForecastDetails, val cityName: String) : WeatherUpdate
        data object Failed : WeatherUpdate
    }
}
