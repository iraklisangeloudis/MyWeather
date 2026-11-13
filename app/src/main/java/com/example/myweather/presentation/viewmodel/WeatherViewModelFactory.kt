package com.example.myweather.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myweather.data.repository.*

class WeatherViewModelFactory(
    private val weatherRepository: WeatherRepository,
    private val cityNameRepository: CityNameRepository,
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WeatherViewModel::class.java)) { "Unknown ViewModel class" }
        @Suppress("UNCHECKED_CAST")
        return WeatherViewModel(weatherRepository, cityNameRepository, locationRepository) as T
    }
}
