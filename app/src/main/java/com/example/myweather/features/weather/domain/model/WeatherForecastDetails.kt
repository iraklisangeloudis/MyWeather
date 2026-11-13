package com.example.myweather.features.weather.domain.model

data class DailyForecast(val day: String, val precipitation: Int, val maximum: Double, val minimum: Double, val weatherCode: Int = 0)
data class WeatherForecastDetails(
    val current: CurrentWeather,
    val weatherAsOf: String,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val sunrise: String,
    val sunset: String,
    val locationName: String = "",
    val daylightMinutes: Long? = null
)
