package com.example.myweather.features.weather.ui

import androidx.annotation.RawRes
import com.airbnb.lottie.LottieAnimationView
import com.example.myweather.R

/** Meteocons Flat artwork shared by animated current conditions and static forecast rows. */
object WeatherIcons {
    @RawRes
    fun resource(weatherCode: Int, isDay: Int): Int {
        val day = isDay == 1
        return when (weatherCode) {
            0 -> if (day) R.raw.weather_clear_day else R.raw.weather_clear_night
            1 -> if (day) R.raw.weather_mostly_clear_day else R.raw.weather_mostly_clear_night
            2 -> if (day) R.raw.weather_partly_cloudy_day else R.raw.weather_partly_cloudy_night
            3 -> if (day) R.raw.weather_overcast_day else R.raw.weather_overcast_night
            45, 48 -> if (day) R.raw.weather_fog_day else R.raw.weather_fog_night
            51, 53, 55 -> if (day) R.raw.weather_overcast_day_drizzle else R.raw.weather_overcast_night_drizzle
            56, 57, 66, 67 -> if (day) R.raw.weather_overcast_day_sleet else R.raw.weather_overcast_night_sleet
            61, 63, 65 -> if (day) R.raw.weather_overcast_day_rain else R.raw.weather_overcast_night_rain
            71, 73, 75 -> if (day) R.raw.weather_overcast_day_snow else R.raw.weather_overcast_night_snow
            77 -> R.raw.weather_snowflake
            80, 81, 82 -> if (day) R.raw.weather_partly_cloudy_day_rain else R.raw.weather_partly_cloudy_night_rain
            85, 86 -> if (day) R.raw.weather_partly_cloudy_day_snow else R.raw.weather_partly_cloudy_night_snow
            95, 97 -> if (day) R.raw.weather_thunderstorms_day_rain else R.raw.weather_thunderstorms_night_rain
            96, 99 -> if (day) R.raw.weather_thunderstorms_day_hail else R.raw.weather_thunderstorms_night_hail
            else -> R.raw.weather_not_available
        }
    }

    fun bind(view: LottieAnimationView, weatherCode: Int, isDay: Int) {
        view.pauseAnimation()
        view.enableMergePathsForKitKatAndAbove(true)
        view.setAnimation(resource(weatherCode, isDay))
        view.progress = 0f
    }
}
