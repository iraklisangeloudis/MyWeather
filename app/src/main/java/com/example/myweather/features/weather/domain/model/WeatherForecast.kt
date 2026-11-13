package com.example.myweather.features.weather.domain.model

data class WeatherForecast(
    val current: CurrentWeather,
    val hourly: HourlyWeather,
    val daily: DailyWeather
)

data class CurrentWeather(
    val time: String,
    val temperature: Double,
    val humidity: Int,
    val apparentTemperature: Double,
    val isDay: Int,
    val weatherCode: Int,
    val windSpeed: Double
)

data class HourlyWeather(
    val time: List<String>,
    val temperature: List<Double>,
    val weatherCode: List<Int>,
    val isDay: List<Int>
)

data class DailyWeather(
    val time: List<String>,
    val weatherCode: List<Int>,
    val temperatureMax: List<Double>,
    val temperatureMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    val precipitationProbabilityMax: List<Int>
)
