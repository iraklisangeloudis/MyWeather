package com.example.myweather.data.repositories

import android.util.Log
import com.example.myweather.data.db.dao.DailyWeatherDao
import com.example.myweather.data.db.entities.DailyWeatherEntity
import com.example.myweather.data.network.responses.Daily

interface DailyWeatherRepository {
    suspend fun insertDailyWeather(daily: Daily)
    suspend fun getDailyWeather(): List<DailyWeatherEntity>
    suspend fun clearDailyWeather()
    suspend fun logDailyWeather()
}

class DailyWeatherRepositoryImpl(private val dailyWeatherDao: DailyWeatherDao) : DailyWeatherRepository {

    override suspend fun insertDailyWeather(daily: Daily) {
        try {
            // Clear the old daily weather data
            clearDailyWeather()

            // Prepare and insert new daily weather data
            val entities = daily.time.indices.map { i ->
                DailyWeatherEntity(
                    date = daily.time[i],
                    weatherCode = daily.weatherCode[i],
                    maxTemperature = daily.temperatureMax[i],
                    minTemperature = daily.temperatureMin[i],
                    sunrise = daily.sunrise[i],
                    sunset = daily.sunset[i],
                    precipitationProbability = daily.precipitationProbabilityMax[i]
                )
            }
            dailyWeatherDao.insertDailyWeather(entities)
            Log.d("DailyWeatherRepo", "insertDailyWeather: Insertion successful")
        } catch (e: Exception) {
            Log.e("DailyWeatherRepo", "insertDailyWeather: Insertion failed", e)
        }
    }

    override suspend fun getDailyWeather(): List<DailyWeatherEntity> {
        return try {
            dailyWeatherDao.getDailyWeather()
        } catch (e: Exception) {
            Log.e("DailyWeatherRepo", "getDailyWeather: Retrieval failed", e)
            emptyList()
        }
    }

    override suspend fun clearDailyWeather() {
        try {
            dailyWeatherDao.clearDailyWeather()
            Log.d("DailyWeatherRepo", "clearDailyWeather: Clear successful")
        } catch (e: Exception) {
            Log.e("DailyWeatherRepo", "clearDailyWeather: Clear failed", e)
        }
    }

    override suspend fun logDailyWeather() {
        try {
            val dailyWeatherList = getDailyWeather()
            if (dailyWeatherList.isNotEmpty()) {
                Log.d("DailyWeatherLog", "Daily Weather Data:")
                dailyWeatherList.forEach { dailyWeather ->
                    Log.d("DailyWeatherLog", "Date: ${dailyWeather.date}")
                    Log.d("DailyWeatherLog", "Weather Code: ${dailyWeather.weatherCode}")
                    Log.d("DailyWeatherLog", "Max Temperature: ${dailyWeather.maxTemperature}")
                    Log.d("DailyWeatherLog", "Min Temperature: ${dailyWeather.minTemperature}")
                    Log.d("DailyWeatherLog", "Sunrise: ${dailyWeather.sunrise}")
                    Log.d("DailyWeatherLog", "Sunset: ${dailyWeather.sunset}")
                    Log.d("DailyWeatherLog", "Precipitation Probability: ${dailyWeather.precipitationProbability}")
                    Log.d("DailyWeatherLog", "----------------------")
                }
            } else {
                Log.d("DailyWeatherLog", "No daily weather data available.")
            }
        } catch (e: Exception) {
            Log.e("DailyWeatherLog", "Error fetching daily weather data", e)
        }
    }
}