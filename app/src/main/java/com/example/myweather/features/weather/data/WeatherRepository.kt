package com.example.myweather.features.weather.data

import androidx.room.withTransaction
import com.example.myweather.database.WeatherDatabase
import com.example.myweather.features.location.domain.model.Coordinates
import com.example.myweather.features.weather.data.api.WeatherApi
import com.example.myweather.features.weather.data.model.*
import com.example.myweather.features.weather.domain.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface WeatherRepository {
    suspend fun getCachedWeather(): WeatherForecast?
    suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast
    suspend fun updateLocationName(latitude: Double, longitude: Double, name: String) = Unit
}

class WeatherRepositoryImpl(
    private val api: WeatherApi,
    private val database: WeatherDatabase,
    private val legacyCache: LegacyWeatherCache,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {
    override suspend fun getCachedWeather(): WeatherForecast? = withContext(ioDispatcher) {
        // Older releases saved a complete forecast in preferences but truncated Room's hourly data.
        legacyCache.getWeather()?.let { weather ->
            saveWeather(weather)
            legacyCache.clear()
            return@withContext weather
        }
        database.withTransaction {
            val current = database.currentWeatherDao().getCurrentWeather() ?: return@withTransaction null
            val hourly = database.hourlyWeatherDao().getHourlyWeather()
            val daily = database.dailyWeatherDao().getDailyWeather()
            if (hourly.isEmpty() || daily.isEmpty()) return@withTransaction null
            WeatherForecast(
                CurrentWeather(current.time, current.temperature, current.humidity, current.apparentTemperature,
                    current.isDay, current.weatherCode, current.windSpeed),
                HourlyWeather(hourly.map { it.time }, hourly.map { it.temperature },
                    hourly.map { it.weatherCode }, hourly.map { it.isDay }),
                DailyWeather(daily.map { it.date }, daily.map { it.weatherCode },
                    daily.map { it.maxTemperature }, daily.map { it.minTemperature },
                    daily.map { it.sunrise }, daily.map { it.sunset }, daily.map { it.precipitationProbability }),
                current.locationName,
                if (current.latitude != null && current.longitude != null) Coordinates(current.latitude, current.longitude) else null
            )
        }
    }

    override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast = withContext(ioDispatcher) {
        val response = api.fetchWeather(
            latitude, longitude,
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m",
            "temperature_2m,weather_code,is_day",
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max"
        )
        val weather = response.toForecast().copy(coordinates = Coordinates(latitude, longitude))
        saveWeather(weather)
        legacyCache.clear()
        weather
    }

    override suspend fun updateLocationName(latitude: Double, longitude: Double, name: String) = withContext(ioDispatcher) {
        database.currentWeatherDao().updateLocationName(latitude, longitude, name)
    }

    private suspend fun saveWeather(weather: WeatherForecast) {
        val current = weather.current
        val hourly = weather.hourly
        val daily = weather.daily
        val currentEntity = CurrentWeatherEntity(time = current.time, temperature = current.temperature,
            humidity = current.humidity, apparentTemperature = current.apparentTemperature,
            isDay = current.isDay, weatherCode = current.weatherCode, windSpeed = current.windSpeed,
            locationName = weather.locationName, latitude = weather.coordinates?.latitude, longitude = weather.coordinates?.longitude)
        val hourlyEntities = hourly.time.indices.map { i ->
            HourlyWeatherEntity(time = hourly.time[i], temperature = hourly.temperature[i],
                weatherCode = hourly.weatherCode[i], isDay = hourly.isDay[i])
        }
        val dailyEntities = daily.time.indices.map { i ->
            DailyWeatherEntity(date = daily.time[i], weatherCode = daily.weatherCode[i],
                maxTemperature = daily.temperatureMax[i], minTemperature = daily.temperatureMin[i],
                sunrise = daily.sunrise[i], sunset = daily.sunset[i], precipitationProbability = daily.precipitationProbabilityMax[i])
        }
        // Replace the complete snapshot atomically; failures retain the previous cache.
        database.withTransaction {
            database.currentWeatherDao().clearCurrentWeather()
            database.hourlyWeatherDao().clearHourlyWeather()
            database.dailyWeatherDao().clearDailyWeather()
            database.currentWeatherDao().insertCurrentWeather(currentEntity)
            database.hourlyWeatherDao().insertHourlyWeather(hourlyEntities)
            database.dailyWeatherDao().insertDailyWeather(dailyEntities)
        }
    }
}
