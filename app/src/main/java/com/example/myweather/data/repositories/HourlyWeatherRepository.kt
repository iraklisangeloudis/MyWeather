package com.example.myweather.data.repositories

import android.util.Log
import com.example.myweather.data.db.dao.HourlyWeatherDao
import com.example.myweather.data.db.entities.HourlyWeatherEntity
import com.example.myweather.data.network.responses.Hourly

interface HourlyWeatherRepository {
    suspend fun insertHourlyWeather(hourly: Hourly, currentTime: String)
    suspend fun getHourlyWeather(): List<HourlyWeatherEntity>
    suspend fun clearHourlyWeather()
    suspend fun logHourlyWeather()
}

class HourlyWeatherRepositoryImpl(private val hourlyWeatherDao: HourlyWeatherDao) : HourlyWeatherRepository {

    override suspend fun insertHourlyWeather(hourly: Hourly, currentTime: String) {
        try {
            // Clear the old hourly weather data
            clearHourlyWeather()

            // Prepare and insert new hourly weather data for the next 24 hours
            val entities = hourly.time.indices.map { i ->
                HourlyWeatherEntity(
                    time = hourly.time[i],
                    temperature = hourly.temperature[i],
                    weatherCode = hourly.weatherCode[i],
                    isDay = hourly.isDay[i]
                )
            }.take(24)
            hourlyWeatherDao.insertHourlyWeather(entities)
            Log.d("HourlyWeatherRepo", "insertHourlyWeather: Insertion successful")
        } catch (e: Exception) {
            Log.e("HourlyWeatherRepo", "insertHourlyWeather: Insertion failed", e)
        }
    }

    override suspend fun getHourlyWeather(): List<HourlyWeatherEntity> {
        return try {
            hourlyWeatherDao.getHourlyWeather()
        } catch (e: Exception) {
            Log.e("HourlyWeatherRepo", "getHourlyWeather: Retrieval failed", e)
            emptyList()
        }
    }

    override suspend fun clearHourlyWeather() {
        try {
            hourlyWeatherDao.clearHourlyWeather()
            Log.d("HourlyWeatherRepo", "clearHourlyWeather: Clear successful")
        } catch (e: Exception) {
            Log.e("HourlyWeatherRepo", "clearHourlyWeather: Clear failed", e)
        }
    }

    override suspend fun logHourlyWeather() {
        try {
            val hourlyWeatherList = getHourlyWeather()
            if (hourlyWeatherList.isNotEmpty()) {
                Log.d("HourlyWeatherLog", "Hourly Weather Data:")
                hourlyWeatherList.forEach { hourlyWeather ->
                    Log.d("HourlyWeatherLog", "Time: ${hourlyWeather.time}")
                    Log.d("HourlyWeatherLog", "Temperature: ${hourlyWeather.temperature}")
                    Log.d("HourlyWeatherLog", "Weather Code: ${hourlyWeather.weatherCode}")
                    Log.d("HourlyWeatherLog", "Is Day: ${hourlyWeather.isDay}")
                    Log.d("HourlyWeatherLog", "----------------------")
                }
            } else {
                Log.d("HourlyWeatherLog", "No hourly weather data available.")
            }
        } catch (e: Exception) {
            Log.e("HourlyWeatherLog", "Error fetching hourly weather data", e)
        }
    }
}