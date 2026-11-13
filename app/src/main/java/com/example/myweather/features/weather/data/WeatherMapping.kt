package com.example.myweather.features.weather.data

import com.example.myweather.features.weather.data.api.model.WeatherResponse
import com.example.myweather.features.weather.domain.model.*

internal fun WeatherResponse.toForecast(): WeatherForecast {
    require(hourly.time.isNotEmpty() && daily.time.isNotEmpty()) { "Empty forecast" }
    require(listOf(hourly.temperature.size, hourly.weatherCode.size, hourly.isDay.size).all { it == hourly.time.size })
    require(listOf(daily.weatherCode.size, daily.temperatureMax.size, daily.temperatureMin.size,
        daily.sunrise.size, daily.sunset.size, daily.precipitationProbabilityMax.size).all { it == daily.time.size })
    return WeatherForecast(
        CurrentWeather(current.time, current.temperature, current.humidity, current.apparentTemperature,
            current.isDay, current.weatherCode, current.windSpeed),
        HourlyWeather(hourly.time, hourly.temperature, hourly.weatherCode, hourly.isDay),
        DailyWeather(daily.time, daily.weatherCode, daily.temperatureMax, daily.temperatureMin,
            daily.sunrise, daily.sunset, daily.precipitationProbabilityMax)
    )
}
