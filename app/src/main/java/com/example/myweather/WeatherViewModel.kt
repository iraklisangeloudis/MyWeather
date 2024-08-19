package com.example.myweather

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myweather.data.db.entities.*
import com.example.myweather.data.network.responses.*
import com.example.myweather.data.preferences.WeatherPreferences
import com.example.myweather.data.repositories.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherViewModel(
    private val weatherApiRepository: WeatherRepository,
    private val cityNameRepository: CityNameRepository,
    private val weatherPreferences: WeatherPreferences,
    private val databaseRepository: DatabaseRepository
) : ViewModel() {

    private val _weatherState = MutableLiveData<WeatherState>()
    val weatherState: LiveData<WeatherState> = _weatherState

    private val _cityNameState = MutableLiveData<CityNameState>()
    val cityNameState: LiveData<CityNameState> = _cityNameState

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            weatherApiRepository.fetchWeatherData(latitude, longitude) { weatherResponse ->
                weatherResponse?.let {
                    handleWeatherResponse(it)
                    weatherPreferences.saveWeatherData(it.current, it.hourly, it.daily)
                    saveWeatherData(it)
                } ?: run {
                    handleWeatherFailure()
                }
            }
        }
    }

    private fun handleWeatherResponse(response: WeatherResponse) {
        //saveWeatherData(response)
        _weatherState.value = WeatherState.Success(response)
    }

    private fun handleWeatherFailure() {
        _weatherState.value = WeatherState.Error("Failed to fetch weather data")
    }

    private fun saveWeatherData(weather: WeatherResponse) {
        viewModelScope.launch {
            databaseRepository.insertCurrentWeather(weather.current)
            databaseRepository.logCurrentWeather()
            databaseRepository.insertDailyWeather(weather.daily)
            databaseRepository.logDailyWeather()
            databaseRepository.insertHourlyWeather(weather.hourly, weather.current.time)
            databaseRepository.logHourlyWeather()
        }
    }

    fun fetchCityName(latitude: Double, longitude: Double, reverseGeocodeApiKey: String) {
        viewModelScope.launch {
            cityNameRepository.fetchCityName(latitude, longitude, reverseGeocodeApiKey) { cityName ->
                cityName?.let {
                    _cityNameState.value = CityNameState.Success(it)
                } ?: run {
                    _cityNameState.value = CityNameState.Error("Failed to load city name")
                }
            }
        }
    }

}

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val weatherResponse: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

sealed class CityNameState {
    data class Success(val cityName: String) : CityNameState()
    data class Error(val message: String) : CityNameState()
}