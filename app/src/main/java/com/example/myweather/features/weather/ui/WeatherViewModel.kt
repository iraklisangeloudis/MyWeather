package com.example.myweather.features.weather.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myweather.features.location.data.CityNameRepository
import com.example.myweather.features.location.data.LocationRepository
import com.example.myweather.features.location.domain.model.Coordinates
import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.data.WeatherRepository
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails
import com.example.myweather.network.requestFailureMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

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
        observeWeatherSelections()
        observeLocationSearch()
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
        _viewState.update {
            it.copy(
                searchQuery = text,
                suggestions = emptyList(),
                isSearching = text.isNotEmpty(),
                searchFailed = false
            )
        }
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

    fun reloadCachedWeather() {
        val previous = _viewState.value
        if (previous.isLoading) return
        viewModelScope.launch {
            val cached = loadCachedWeather() ?: return@launch
            _viewState.update { state ->
                // A delayed cache read must not overwrite a newer selection or refresh.
                if (state.isLoading || state.weather != previous.weather || state.cityName != previous.cityName) state
                else state.copy(weather = cached.weather, cityName = cached.cityName)
            }
        }
    }

    private fun observeWeatherSelections() {
        selections.flatMapLatest(::weatherFor)
            .buffer(0)
            .onStart { loadCachedWeather()?.let { emit(it) } }
            .onEach(::updateWeatherState)
            .launchIn(viewModelScope)
    }

    private suspend fun loadCachedWeather(): WeatherUpdate.Content? {
        return try {
            weatherRepository.getCachedWeather()?.let { cached ->
                WeatherUpdate.Content(getWeatherForecast(cached), cached.locationName)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("WeatherViewModel", requestFailureMessage(e))
            null
        }
    }

    private fun weatherFor(coordinates: Coordinates): Flow<WeatherUpdate> {
        return combine(forecastFor(coordinates), cityNameFor(coordinates)) { forecast, name ->
            when (forecast) {
                ForecastUpdate.Loading -> WeatherUpdate.Loading
                is ForecastUpdate.Content -> WeatherUpdate.Content(forecast.weather, name)
                ForecastUpdate.Failed -> WeatherUpdate.Failed
            }
        }.distinctUntilChanged().onEach { update ->
            if (update is WeatherUpdate.Content) cacheLocationName(coordinates, update.cityName)
        }
    }

    private fun forecastFor(coordinates: Coordinates): Flow<ForecastUpdate> = flow<ForecastUpdate> {
        emit(ForecastUpdate.Loading)
        val forecast = weatherRepository.refreshWeather(coordinates.latitude, coordinates.longitude)
        emit(ForecastUpdate.Content(getWeatherForecast(forecast)))
    }.catch { error ->
        Log.w("WeatherViewModel", requestFailureMessage(error))
        emit(ForecastUpdate.Failed)
    }

    private fun cityNameFor(coordinates: Coordinates): Flow<String> = flow {
        emit("")
        emit(cityNameRepository.fetchCityName(coordinates.latitude, coordinates.longitude).orEmpty())
    }.catch { error ->
        Log.w("WeatherViewModel", requestFailureMessage(error))
        emit("")
    }

    private suspend fun cacheLocationName(coordinates: Coordinates, name: String) {
        if (name.isBlank()) return
        try {
            // The coordinate guard prevents a delayed label from naming a different cached place.
            weatherRepository.updateLocationName(coordinates.latitude, coordinates.longitude, name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("WeatherViewModel", requestFailureMessage(e))
        }
    }

    private fun updateWeatherState(update: WeatherUpdate) {
        _viewState.update { state ->
            when (update) {
                WeatherUpdate.Loading -> state.copy(isRefreshing = true)
                is WeatherUpdate.Content -> state.copy(
                    weather = update.weather,
                    cityName = update.cityName,
                    isRefreshing = false
                )
                WeatherUpdate.Failed -> state.copy(isRefreshing = false, message = "Unable to refresh weather")
            }
        }
    }

    private fun observeLocationSearch() {
        searchQuery.transformLatest { query ->
            emit(SearchUpdate(query, isSearching = query.isNotEmpty()))
            if (query.isNotEmpty()) emit(fetchSearchSuggestions(query))
        }.buffer(0)
            .onEach(::updateSearchState)
            .launchIn(viewModelScope)
    }

    private suspend fun fetchSearchSuggestions(query: String): SearchUpdate {
        // Called inside transformLatest so new input cancels both the delay and the request immediately.
        delay(1000.milliseconds)
        val locations = locationRepository.fetchLocationSuggestions(query)
        return SearchUpdate(query, suggestions = locations.orEmpty(), failed = locations == null)
    }

    private fun updateSearchState(update: SearchUpdate) {
        if (update.query != searchQuery.value) return
        _viewState.update { state ->
            state.copy(
                searchQuery = update.query,
                isSearching = update.isSearching,
                suggestions = update.suggestions,
                searchFailed = update.failed,
                message = if (update.failed) "Failed to load location data" else state.message
            )
        }
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
