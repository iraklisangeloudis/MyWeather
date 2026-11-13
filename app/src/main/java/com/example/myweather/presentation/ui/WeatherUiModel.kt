package com.example.myweather.presentation.ui

import com.example.myweather.model.CurrentWeather
import com.example.myweather.model.WeatherForecast
import com.example.myweather.presentation.HourlyData
import com.example.myweather.presentation.utils.DateTimeUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

data class DailyForecastUi(val day: String, val precipitation: Int, val maximum: Double, val minimum: Double)
data class WeatherUiModel(
    val current: CurrentWeather,
    val updatedAt: String,
    val hourly: List<HourlyData>,
    val daily: List<DailyForecastUi>,
    val sunrise: String,
    val sunset: String
)

internal fun WeatherForecast.toUiModel(today: LocalDate = LocalDate.now()): WeatherUiModel {
    val currentTime = LocalDateTime.parse(current.time)
    val nextHours = hourly.time.indices.asSequence()
        .filter { LocalDateTime.parse(hourly.time[it]).isAfter(currentTime) }
        .take(24)
        .map { HourlyData(hourly.time[it], hourly.temperature[it], hourly.weatherCode[it], hourly.isDay[it]) }
        .toList()
    return WeatherUiModel(current, DateTimeUtils.formattedDateTime(current.time), nextHours,
        daily.time.indices.map { i ->
            val date = LocalDate.parse(daily.time[i])
            val label = if (date == today) "Today" else date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            DailyForecastUi(label, daily.precipitationProbabilityMax[i], daily.temperatureMax[i], daily.temperatureMin[i])
        }, daily.sunrise.firstOrNull()?.substringAfter('T') ?: "-",
        daily.sunset.firstOrNull()?.substringAfter('T') ?: "-")
}
