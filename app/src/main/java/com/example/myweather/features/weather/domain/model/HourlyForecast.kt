package com.example.myweather.features.weather.domain.model

// Helper class
// Used in Recycler View updates
data class HourlyForecast(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Int
)
