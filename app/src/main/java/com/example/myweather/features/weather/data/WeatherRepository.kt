package com.example.myweather.features.weather.data

import com.example.myweather.features.location.domain.model.Coordinates
import com.example.myweather.features.weather.data.api.WeatherApi
import com.example.myweather.features.weather.data.dao.WeatherCacheDao
import com.example.myweather.features.weather.data.dao.WeatherCacheSnapshot
import com.example.myweather.features.weather.data.model.*
import com.example.myweather.features.weather.domain.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock

interface WeatherRepository {
    suspend fun getCachedWeather(): WeatherForecast?
    suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast
    suspend fun updateLocationName(latitude: Double, longitude: Double, name: String) = Unit
}

class WeatherRepositoryImpl(
    private val api: WeatherApi,
    private val cacheDao: WeatherCacheDao,
    private val cacheNotifier: WeatherCacheNotifier,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC()
) : WeatherRepository {
    override suspend fun getCachedWeather(): WeatherForecast? = withContext(ioDispatcher) {
        val (current, hourly, daily) = cacheDao.getSnapshot() ?: return@withContext null
        WeatherForecast(
            CurrentWeather(current.time, current.temperature, current.humidity, current.apparentTemperature,
                current.isDay, current.weatherCode, current.windSpeed),
            HourlyWeather(hourly.map { it.time }, hourly.map { it.temperature },
                hourly.map { it.weatherCode }, hourly.map { it.isDay }),
            DailyWeather(daily.map { it.date }, daily.map { it.weatherCode },
                daily.map { it.maxTemperature }, daily.map { it.minTemperature },
                daily.map { it.sunrise }, daily.map { it.sunset }, daily.map { it.precipitationProbability }),
            current.locationName,
            if (current.latitude != null && current.longitude != null) Coordinates(current.latitude, current.longitude) else null,
            current.checkedAtMillis
        )
    }

    override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast = withContext(ioDispatcher) {
        val response = api.fetchWeather(
            latitude, longitude,
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m",
            "temperature_2m,weather_code,is_day",
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max"
        )
        val weather = response.toForecast().copy(
            coordinates = Coordinates(latitude, longitude),
            checkedAtMillis = clock.millis()
        )
        saveWeather(weather)
        cacheNotifier.notifyChanged()
        weather
    }

    override suspend fun updateLocationName(latitude: Double, longitude: Double, name: String) = withContext(ioDispatcher) {
        cacheDao.updateLocationName(latitude, longitude, name)
        cacheNotifier.notifyChanged()
    }

    private suspend fun saveWeather(weather: WeatherForecast) {
        val current = weather.current
        val hourly = weather.hourly
        val daily = weather.daily
        val currentEntity = CurrentWeatherEntity(time = current.time, temperature = current.temperature,
            humidity = current.humidity, apparentTemperature = current.apparentTemperature,
            isDay = current.isDay, weatherCode = current.weatherCode, windSpeed = current.windSpeed,
            locationName = weather.locationName, latitude = weather.coordinates?.latitude, longitude = weather.coordinates?.longitude,
            checkedAtMillis = weather.checkedAtMillis)
        val hourlyEntities = hourly.time.indices.map { i ->
            HourlyWeatherEntity(time = hourly.time[i], temperature = hourly.temperature[i],
                weatherCode = hourly.weatherCode[i], isDay = hourly.isDay[i])
        }
        val dailyEntities = daily.time.indices.map { i ->
            DailyWeatherEntity(date = daily.time[i], weatherCode = daily.weatherCode[i],
                maxTemperature = daily.temperatureMax[i], minTemperature = daily.temperatureMin[i],
                sunrise = daily.sunrise[i], sunset = daily.sunset[i], precipitationProbability = daily.precipitationProbabilityMax[i])
        }
        cacheDao.replaceSnapshot(WeatherCacheSnapshot(currentEntity, hourlyEntities, dailyEntities))
    }
}
