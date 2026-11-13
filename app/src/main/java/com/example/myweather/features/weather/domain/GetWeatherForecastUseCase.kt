package com.example.myweather.features.weather.domain

import com.example.myweather.features.weather.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetWeatherForecastUseCase(
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(forecast: WeatherForecast, today: LocalDate = LocalDate.now()): WeatherForecastDetails =
        withContext(computationDispatcher) {
            val currentTime = LocalDateTime.parse(forecast.current.time)
            val hourly = forecast.hourly
            val daily = forecast.daily
            val nextHours = hourly.time.indices.asSequence()
                .filter { LocalDateTime.parse(hourly.time[it]).isAfter(currentTime) }
                .take(24)
                .map { HourlyForecast(hourly.time[it], hourly.temperature[it], hourly.weatherCode[it], hourly.isDay[it]) }
                .toList()
            val dayName = currentTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val updatedAt = "$dayName, ${currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            WeatherForecastDetails(forecast.current, updatedAt, nextHours,
                daily.time.indices.map { i ->
                    val date = LocalDate.parse(daily.time[i])
                    val label = if (date == today) "Today" else date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    DailyForecast(label, daily.precipitationProbabilityMax[i], daily.temperatureMax[i], daily.temperatureMin[i])
                }, daily.sunrise.firstOrNull()?.substringAfter('T') ?: "-",
                daily.sunset.firstOrNull()?.substringAfter('T') ?: "-")
        }
}
