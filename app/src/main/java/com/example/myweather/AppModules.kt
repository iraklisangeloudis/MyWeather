package com.example.myweather

import com.example.myweather.database.WeatherDatabase
import com.example.myweather.features.location.locationModule
import com.example.myweather.features.weather.weatherModule
import com.example.myweather.network.ApiClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val networkModule = module {
    single { ApiClient() }
}
private val databaseModule = module {
    single { WeatherDatabase.create(androidContext()) }
}
val appModules = listOf(networkModule, databaseModule, locationModule, weatherModule)
