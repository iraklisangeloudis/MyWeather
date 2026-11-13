package com.example.myweather.features.weather.ui

import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails

data class WeatherViewState(
    val weather: WeatherForecastDetails? = null,
    val cityName: String = "",
    val isRefreshing: Boolean = false,
    val isLocating: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val suggestions: List<LocationSuggestion> = emptyList(),
    val message: String? = null
) {
    val isLoading: Boolean get() = isRefreshing || isLocating || isSearching
}
