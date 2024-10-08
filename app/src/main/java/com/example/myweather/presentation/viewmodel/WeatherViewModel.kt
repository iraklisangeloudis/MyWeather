package com.example.myweather.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myweather.data.network.responses.*
import com.example.myweather.data.preferences.WeatherPreferences
import com.example.myweather.data.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val weatherApiRepository: WeatherRepository,
    private val cityNameRepository: CityNameRepository,
    private val weatherPreferences: WeatherPreferences,
    private val currentWeatherRepository: CurrentWeatherRepository,
    private val dailyWeatherRepository: DailyWeatherRepository,
    private val hourlyWeatherRepository: HourlyWeatherRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _weatherState = MutableLiveData<WeatherState>()
    val weatherState: LiveData<WeatherState> = _weatherState

    private val _cityNameState = MutableLiveData<CityNameState>()
    val cityNameState: LiveData<CityNameState> = _cityNameState

    private val _locationState = MutableLiveData<LocationState>()
    val locationState: LiveData<LocationState> = _locationState

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            _weatherState.postValue(WeatherState.Loading)
            val weatherResponse = weatherApiRepository.fetchWeatherData(latitude, longitude)
            weatherResponse?.let {
                _weatherState.postValue(WeatherState.Success(it))
                weatherPreferences.saveWeatherData(it.current, it.hourly, it.daily)
                saveWeatherData(it)
            } ?: run {
                _weatherState.postValue(WeatherState.Error("Failed to fetch weather data"))
            }
        }
    }

    private fun saveWeatherData(weather: WeatherResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            currentWeatherRepository.insertCurrentWeather(weather.current)
            dailyWeatherRepository.insertDailyWeather(weather.daily)
            hourlyWeatherRepository.insertHourlyWeather(weather.hourly, weather.current.time)

//            currentWeatherRepository.logCurrentWeather()
//            dailyWeatherRepository.logDailyWeather()
//            hourlyWeatherRepository.logHourlyWeather()
        }
    }

    fun fetchCityName(latitude: Double, longitude: Double, reverseGeocodeApiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cityName = cityNameRepository.fetchCityName(latitude, longitude, reverseGeocodeApiKey)
            cityName?.let {
                _cityNameState.postValue(CityNameState.Success(it))
            } ?: run {
                _cityNameState.postValue(CityNameState.Error("Failed to load city name"))
            }
        }
    }

    fun getAutocomplete(query: String, apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val locations = locationRepository.getAutocomplete(query, apiKey)
            locations?.let {
                _locationState.postValue(LocationState.Success(locations))
            } ?: run {
                _locationState.postValue(LocationState.Error("Failed to load location data"))
            }
        }
    }

}

sealed class WeatherState {
    data object Loading : WeatherState()
    data class Success(val weatherResponse: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

sealed class CityNameState {
    data class Success(val cityName: String) : CityNameState()
    data class Error(val message: String) : CityNameState()
}

sealed class LocationState {
    data class Success(val locationResponse: List<LocationResponse>) : LocationState()
    data class Error(val message: String) : LocationState()
}