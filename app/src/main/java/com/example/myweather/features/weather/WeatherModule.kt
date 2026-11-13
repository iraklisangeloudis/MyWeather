package com.example.myweather.features.weather

import com.example.myweather.database.WeatherDatabase
import com.example.myweather.features.weather.data.*
import com.example.myweather.features.weather.data.api.WeatherApi
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.ui.WeatherViewModel
import com.example.myweather.network.ApiClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val weatherModule = module {
    single<WeatherApi> { get<ApiClient>().weatherApi }
    single { get<WeatherDatabase>().currentWeatherDao() }
    single { get<WeatherDatabase>().hourlyWeatherDao() }
    single { get<WeatherDatabase>().dailyWeatherDao() }
    single<WeatherRepository> { WeatherRepositoryImpl(get(), get()) }
    factory { GetWeatherForecastUseCase() }
    viewModel { WeatherViewModel(get(), get(), get(), get()) }
}
