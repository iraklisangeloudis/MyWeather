package com.example.myweather.features.weather.ui

import com.example.myweather.R

object WeatherTheme {
    fun description(weatherCode: Int, isDay: Int): Int = when (weatherCode) {
        0 -> if (isDay == 1) R.string.condition_sunny else R.string.condition_clear
        1 -> R.string.condition_mostly_clear
        2 -> R.string.condition_cloudy
        3 -> R.string.condition_overcast
        45, 48 -> R.string.condition_fog
        in 51..57 -> R.string.condition_drizzle
        in 61..67, in 80..82 -> R.string.condition_rain
        in 71..77, 85, 86 -> R.string.condition_snow
        97 -> R.string.condition_heavy_storm
        95, 96, 99 -> R.string.condition_storm
        else -> R.string.condition_unknown
    }

}
