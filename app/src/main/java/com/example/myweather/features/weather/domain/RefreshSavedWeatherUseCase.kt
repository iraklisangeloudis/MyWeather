package com.example.myweather.features.weather.domain

import com.example.myweather.features.weather.data.WeatherRepository

/** Widgets refresh the saved place, without requesting device location in the background. */
class RefreshSavedWeatherUseCase(private val repository: WeatherRepository) {
    suspend operator fun invoke(): Boolean {
        val cached = repository.getCachedWeather() ?: return false
        val coordinates = cached.coordinates ?: return false
        repository.refreshWeather(coordinates.latitude, coordinates.longitude)
        if (cached.locationName.isNotBlank()) {
            repository.updateLocationName(coordinates.latitude, coordinates.longitude, cached.locationName)
        }
        return true
    }
}
