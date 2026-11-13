package com.example.myweather.features.weather.widget

import com.example.myweather.features.weather.data.WeatherRepository
import com.example.myweather.features.weather.data.dao.CurrentWeatherDao
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/** Reload complete snapshots after Room commits, including updated location labels. */
class WeatherWidgetDataSource(
    private val repository: WeatherRepository,
    private val currentWeatherDao: CurrentWeatherDao,
    private val getWeatherForecast: GetWeatherForecastUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<WeatherForecastDetails?> = currentWeatherDao.observeCurrentWeather().mapLatest {
        repository.getCachedWeather()?.let { getWeatherForecast(it) }
    }
}
