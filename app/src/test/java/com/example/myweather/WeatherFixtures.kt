package com.example.myweather

import com.example.myweather.model.*
import java.time.LocalDateTime

internal fun forecast(temperature: Double = 20.0): WeatherForecast {
    val start = LocalDateTime.parse("2026-11-13T00:00")
    return WeatherForecast(
        CurrentWeather("2026-11-13T12:15", temperature, 50, temperature, 1, 0, 5.0),
        HourlyWeather((0L until 48L).map { start.plusHours(it).toString() }, List(48) { temperature }, List(48) { 0 }, List(48) { 1 }),
        DailyWeather(listOf("2026-11-13"), listOf(0), listOf(22.0), listOf(15.0),
            listOf("2026-11-13T07:00"), listOf("2026-11-13T17:00"), listOf(10))
    )
}
