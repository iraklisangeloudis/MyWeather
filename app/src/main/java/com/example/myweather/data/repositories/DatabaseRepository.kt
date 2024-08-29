package com.example.myweather.data.repositories

import android.util.Log
import com.example.myweather.data.db.WeatherDatabase
import com.example.myweather.data.db.entities.CurrentWeatherEntity
import com.example.myweather.data.db.entities.DailyWeatherEntity
import com.example.myweather.data.db.entities.HourlyWeatherEntity
import com.example.myweather.domain.HourlyData
import com.example.myweather.data.network.responses.Current
import com.example.myweather.data.network.responses.Daily
import com.example.myweather.data.network.responses.Hourly
import java.time.LocalDateTime
import java.util.concurrent.Executors

interface DatabaseRepository {
    fun insertCurrentWeather(current: Current)
    fun logCurrentWeather()
    fun insertDailyWeather(daily: Daily)
    fun logDailyWeather()
    fun insertHourlyWeather(hourly: Hourly, currentTime: String)
    fun logHourlyWeather()
}

class DatabaseRepositoryImpl(private val database: WeatherDatabase) : DatabaseRepository {

    private val executor = Executors.newSingleThreadExecutor()

    override fun insertCurrentWeather(current: Current) {
        executor.execute {
            try {
                //Log.d("WeatherApp", "insertCurrentWeather is running on thread: ${Thread.currentThread().name}")
                val weatherDao = database.weatherDao()
                weatherDao.clearCurrentWeather()
                val entity = CurrentWeatherEntity(
                    time = current.time,
                    temperature = current.temperature,
                    humidity = current.humidity,
                    apparentTemperature = current.apparentTemperature,
                    isDay = current.isDay,
                    weatherCode = current.weatherCode,
                    windSpeed = current.windSpeed
                )
                weatherDao.insertCurrentWeather(entity)
                Log.d("WeatherApp", "insertCurrentWeather: Insertion successful")
            } catch (e: Exception) {
                Log.e("WeatherApp", "insertCurrentWeather: Insertion failed", e)
            }
        }
    }

    override fun logCurrentWeather() {
        executor.execute {
            try {
                val weatherDao = database.weatherDao()
                val currentWeather = weatherDao.getCurrentWeather()
                if (currentWeather != null) {
                    Log.d("CurrentWeatherLog", "Current Weather Data:")
                    Log.d("CurrentWeatherLog", "Time: ${currentWeather.time}")
                    Log.d("CurrentWeatherLog", "Temperature: ${currentWeather.temperature}")
                    Log.d("CurrentWeatherLog", "Humidity: ${currentWeather.humidity}")
                    Log.d("CurrentWeatherLog", "Apparent Temperature: ${currentWeather.apparentTemperature}")
                    Log.d("CurrentWeatherLog", "Is Day: ${currentWeather.isDay}")
                    Log.d("CurrentWeatherLog", "Weather Code: ${currentWeather.weatherCode}")
                    Log.d("CurrentWeatherLog", "Wind Speed: ${currentWeather.windSpeed}")
                    Log.d("CurrentWeatherLog", "----------------------")
                } else {
                    Log.d("CurrentWeatherLog", "No current weather data available.")
                }
            } catch (e: Exception) {
                Log.e("CurrentWeatherLog", "Error fetching current weather data", e)
            }
        }
    }

    override fun insertDailyWeather(daily: Daily) {
        executor.execute {
            try {
                //Log.d("WeatherApp", "insertDailyWeather is running on thread: ${Thread.currentThread().name}")
                val weatherDao = database.weatherDao()

                // Clear existing entries
                weatherDao.clearDailyWeather()

                // Prepare and insert new entries
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
                weatherDao.insertDailyWeather(entities)
                Log.d("WeatherApp", "insertDailyWeather: Insertion successful")
            } catch (e: Exception) {
                Log.e("WeatherApp", "insertDailyWeather: Insertion failed", e)
            }
        }
    }

    override fun logDailyWeather() {
        executor.execute {
            try {
                val weatherDao = database.weatherDao()
                val dailyWeatherList = weatherDao.getDailyWeather()
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

    override fun insertHourlyWeather(hourly: Hourly, currentTime: String) {
        executor.execute {
            try {
                //Log.d("WeatherApp", "insertHourlyWeather is running on thread: ${Thread.currentThread().name}")
                val weatherDao = database.weatherDao()

                // Clear existing entries
                weatherDao.clearHourlyWeather()

                // Prepare and filter data for the next 24 hours
                val currentDateTime = LocalDateTime.parse(currentTime)
                val hourlyDataForNext24Hours = hourly.time.zip(hourly.temperature.zip(hourly.weatherCode.zip(hourly.isDay))) { time, triple ->
                    val (temperature, pair) = triple
                    val (weatherCode, isDay) = pair
                    HourlyData(time, temperature, weatherCode, isDay)
                }.filter { data ->
                    LocalDateTime.parse(data.time).isAfter(currentDateTime)
                }.take(24) // Take the next 24 hours

                // Convert filtered data to entities and insert into database
                val entities = hourlyDataForNext24Hours.map { data ->
                    HourlyWeatherEntity(
                        time = data.time,
                        temperature = data.temperature,
                        weatherCode = data.weatherCode,
                        isDay = data.isDay
                    )
                }
                weatherDao.insertHourlyWeather(entities)
                Log.d("WeatherApp", "insertHourlyWeather: Insertion successful")
            } catch (e: Exception) {
                Log.e("WeatherApp", "insertHourlyWeather: Insertion failed", e)
            }
        }
    }

    override fun logHourlyWeather() {
        executor.execute {
            try {
                val weatherDao = database.weatherDao()
                val hourlyWeatherList = weatherDao.getHourlyWeather()
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
}