package com.example.myweather.features.weather.ui.util

import android.content.Context
import com.example.myweather.R
import com.example.myweather.features.weather.domain.GetWeatherCheckAgeUseCase
import com.example.myweather.features.weather.domain.GetWeatherCheckAgeUseCase.Age

/** Shared, local-only label for the last successful weather request. */
class WeatherCheckTimeFormatter(private val getCheckAge: GetWeatherCheckAgeUseCase) {
    fun format(context: Context, checkedAtMillis: Long?): String {
        return when (val age = getCheckAge(checkedAtMillis)) {
            Age.Unknown -> context.getString(R.string.weather_check_unknown)
            Age.JustNow -> context.getString(R.string.weather_checked_just_now)
            is Age.Minutes -> context.resources.getQuantityString(R.plurals.weather_checked_minutes, age.count, age.count)
            is Age.Hours -> context.resources.getQuantityString(R.plurals.weather_checked_hours, age.count, age.count)
            is Age.Days -> context.resources.getQuantityString(R.plurals.weather_checked_days, age.count, age.count)
        }
    }
}
