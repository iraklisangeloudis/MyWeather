package com.example.myweather

import android.app.Application
import android.content.Context
import com.example.myweather.data.local.LegacyWeatherCache
import com.example.myweather.data.local.db.WeatherDatabase
import com.example.myweather.data.remote.ApiClient
import com.example.myweather.data.repository.*
import com.example.myweather.presentation.viewmodel.WeatherViewModelFactory

class WeatherApplication : Application() {
    val container by lazy { AppContainer(applicationContext) }
}

class AppContainer(context: Context) {
    private val api = ApiClient()
    private val weatherRepository = WeatherRepositoryImpl(api.weatherApi, WeatherDatabase.getDatabase(context), LegacyWeatherCache(context))
    private val cityRepository = CityNameRepositoryImpl(api.geocodingApi, BuildConfig.REVERSE_GEOCODE_API_KEY)
    private val locationRepository = LocationRepositoryImpl(api.locationApi, BuildConfig.LOCATION_IQ_API_KEY)
    val viewModelFactory = WeatherViewModelFactory(weatherRepository, cityRepository, locationRepository)
}
