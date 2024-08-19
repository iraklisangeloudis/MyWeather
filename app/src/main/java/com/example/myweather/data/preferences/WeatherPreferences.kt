package com.example.myweather.data.preferences

import android.content.Context
import com.example.myweather.data.network.responses.*
import com.google.gson.Gson

class WeatherPreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveWeatherData(current: Current, hourly: Hourly, daily: Daily) {
        val editor = sharedPreferences.edit()
        editor.putString("current_weather", gson.toJson(current))
        editor.putString("hourly_weather", gson.toJson(hourly))
        editor.putString("daily_weather", gson.toJson(daily))
        editor.apply()
    }

    // methods to retrieve data
    fun getCurrentWeather(): Current? {
        val currentWeatherJson = sharedPreferences.getString("current_weather", null)
        return if (currentWeatherJson != null) {
            gson.fromJson(currentWeatherJson, Current::class.java)
        } else {
            null
        }
    }

    fun getHourlyWeather(): Hourly? {
        val hourlyWeatherJson = sharedPreferences.getString("hourly_weather", null)
        return if (hourlyWeatherJson != null) {
            gson.fromJson(hourlyWeatherJson, Hourly::class.java)
        } else {
            null
        }
    }

    fun getDailyWeather(): Daily? {
        val dailyWeatherJson = sharedPreferences.getString("daily_weather", null)
        return if (dailyWeatherJson != null) {
            gson.fromJson(dailyWeatherJson, Daily::class.java)
        } else {
            null
        }
    }

}