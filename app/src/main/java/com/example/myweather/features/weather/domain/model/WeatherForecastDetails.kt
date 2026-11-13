package com.example.myweather.features.weather.domain.model

data class DailyForecast(val day: String, val precipitation: Int, val maximum: Double, val minimum: Double)
data class WeatherForecastDetails(
    val current: CurrentWeather,
    val updatedAt: String,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val sunrise: String,
    val sunset: String
)
