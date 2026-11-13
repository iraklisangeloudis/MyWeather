package com.example.myweather.presentation.viewmodel

import com.example.myweather.model.LocationSuggestion
import com.example.myweather.presentation.ui.WeatherUiModel

data class WeatherScreenState(
    val weather: WeatherUiModel? = null,
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
