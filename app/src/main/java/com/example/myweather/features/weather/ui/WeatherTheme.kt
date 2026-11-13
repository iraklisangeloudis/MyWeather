package com.example.myweather.features.weather.ui

import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.myweather.R

object WeatherTheme {
    fun getWeatherIcon(weatherCode: Int, isDay:Int): Int {
        return when (weatherCode) {
            0 -> if (isDay==1) R.drawable.clear1 else R.drawable.clear0
            1, 2, 3 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            45, 48 -> R.drawable.fog
            51, 53, 55, 56, 57 -> R.drawable.mc_cloudy
            61, 63, 65 -> R.drawable.rain
            66, 67 -> R.drawable.sleet
            71, 73, 75 -> R.drawable.l_snow
            77 -> R.drawable.hail
            80, 81, 82 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            85, 86 -> if (isDay==1) R.drawable.l_snow1 else R.drawable.l_snow0
            95 -> R.drawable.tstorm
            96, 99 -> R.drawable.tshower
            else -> R.drawable.unknown
        }
    }
    fun bindIcon(view: ImageView, weatherCode: Int, isDay: Int) {
        view.setImageResource(getWeatherIcon(weatherCode, isDay))
        if (weatherCode == 45 || weatherCode == 48) {
            view.setColorFilter(ContextCompat.getColor(view.context, R.color.weather_muted))
        } else view.clearColorFilter()
    }

    fun description(weatherCode: Int, isDay: Int): Int = when (weatherCode) {
        0 -> if (isDay == 1) R.string.condition_sunny else R.string.condition_clear
        1, 2, 3 -> R.string.condition_cloudy
        45, 48 -> R.string.condition_fog
        in 51..57 -> R.string.condition_drizzle
        in 61..67, in 80..82 -> R.string.condition_rain
        in 71..77, 85, 86 -> R.string.condition_snow
        in 95..99 -> R.string.condition_storm
        else -> R.string.condition_unknown
    }

}
