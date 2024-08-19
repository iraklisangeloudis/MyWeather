package com.example.myweather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myweather.data.preferences.WeatherPreferences
import com.example.myweather.data.repositories.*

class WeatherViewModelFactory(
    private val weatherApiRepository: WeatherRepository,
    private val cityNameRepository: CityNameRepository,
    private val weatherPreferences: WeatherPreferences,
    private val databaseRepository: DatabaseRepository

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(
                weatherApiRepository,
                cityNameRepository,
                weatherPreferences,
                databaseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}