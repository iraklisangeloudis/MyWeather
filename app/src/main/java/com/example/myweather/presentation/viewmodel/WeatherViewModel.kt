package com.example.myweather.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myweather.data.repository.*
import com.example.myweather.model.LocationSuggestion
import com.example.myweather.presentation.ui.WeatherUiModel
import com.example.myweather.presentation.ui.toUiModel
import kotlinx.coroutines.*

class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val cityNameRepository: CityNameRepository,
    private val locationRepository: LocationRepository,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val _weatherState = MutableLiveData<WeatherState>()
    val weatherState: LiveData<WeatherState> = _weatherState
    private val _cityNameState = MutableLiveData<CityNameState>()
    val cityNameState: LiveData<CityNameState> = _cityNameState
    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState
    private val cacheJob: Job
    private var weatherJob: Job? = null
    private var searchJob: Job? = null
    private var searchVersion = 0
    private var displayedWeather: WeatherUiModel? = null

    init {
        cacheJob = viewModelScope.launch {
            try {
                weatherRepository.getCachedWeather()?.let { cached ->
                    val ui = withContext(computationDispatcher) { cached.toUiModel() }
                    displayedWeather = ui
                    _weatherState.value = WeatherState.Success(ui)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("WeatherViewModel", requestFailureMessage(e))
            }
        }
    }

    fun selectLocation(latitude: Double, longitude: Double) {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            // Finish restoration first so a failed initial refresh can keep showing cached data.
            cacheJob.join()
            _weatherState.value = WeatherState.Loading
            _cityNameState.value = CityNameState.Success("")
            val cityJob = launch {
                val name = cityNameRepository.fetchCityName(latitude, longitude)
                ensureActive()
                _cityNameState.value = name?.let { CityNameState.Success(it) } ?: CityNameState.Error
            }
            try {
                val weather = weatherRepository.refreshWeather(latitude, longitude)
                val ui = withContext(computationDispatcher) { weather.toUiModel() }
                displayedWeather = ui
                _weatherState.value = WeatherState.Success(ui)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                cityJob.cancel()
                Log.w("WeatherViewModel", requestFailureMessage(e))
                _weatherState.value = WeatherState.Error("Unable to refresh weather", displayedWeather)
                _cityNameState.value = CityNameState.Error
            }
        }
    }

    fun searchLocations(query: String) {
        searchJob?.cancel()
        val version = ++searchVersion
        val text = query.trim()
        _locationState.value = LocationState.Idle
        if (text.isEmpty()) return
        searchJob = viewModelScope.launch {
            delay(1000)
            val locations = locationRepository.fetchLocationSuggestions(text)
            ensureActive()
            if (version != searchVersion) return@launch
            _locationState.value = locations?.let { LocationState.Success(it) }
                ?: LocationState.Error("Failed to load location data")
        }
    }
}

sealed class WeatherState {
    data object Loading : WeatherState()
    data class Success(val weather: WeatherUiModel) : WeatherState()
    data class Error(val message: String, val cachedWeather: WeatherUiModel?) : WeatherState()
}
sealed class CityNameState {
    data class Success(val cityName: String) : CityNameState()
    data object Error : CityNameState()
}
sealed class LocationState {
    data object Idle : LocationState()
    data class Success(val locations: List<LocationSuggestion>) : LocationState()
    data class Error(val message: String) : LocationState()
}
