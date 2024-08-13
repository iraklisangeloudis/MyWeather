package com.example.myweather

data class HourlyData(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Int
)
