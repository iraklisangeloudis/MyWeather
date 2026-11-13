package com.example.myweather.data.local

import android.content.Context
import com.example.myweather.data.remote.dto.*
import com.example.myweather.data.mapper.toForecast
import com.example.myweather.model.WeatherForecast
import com.google.gson.Gson

// Read only migration path. New weather data is stored exclusively in Room.
class LegacyWeatherCache(context: Context) {
    private val preferences = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)

    fun getWeather(): WeatherForecast? = try {
        val gson = Gson()
        val current = preferences.getString("current_weather", null)
        val hourly = preferences.getString("hourly_weather", null)
        val daily = preferences.getString("daily_weather", null)
        if (current == null || hourly == null || daily == null) null else WeatherResponse(
            gson.fromJson(current, Current::class.java),
            gson.fromJson(hourly, Hourly::class.java),
            gson.fromJson(daily, Daily::class.java)
        ).toForecast()
    } catch (_: RuntimeException) {
        null
    }

    fun clear() {
        preferences.edit().remove("current_weather").remove("hourly_weather").remove("daily_weather").apply()
    }
}
