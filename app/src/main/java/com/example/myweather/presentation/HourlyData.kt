package com.example.myweather.presentation

// Helper class
// Used in Recycler View updates
data class HourlyData(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Int
)
