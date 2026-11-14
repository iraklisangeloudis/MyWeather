package com.example.myweather.features.weather

import com.example.myweather.database.WeatherDatabase
import com.example.myweather.features.weather.data.WeatherCacheNotifier
import com.example.myweather.features.weather.data.WeatherRepository
import com.example.myweather.features.weather.data.WeatherRepositoryImpl
import com.example.myweather.features.weather.data.api.WeatherApi
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.domain.GetWeatherCheckAgeUseCase
import com.example.myweather.features.weather.domain.RefreshSavedWeatherUseCase
import com.example.myweather.features.weather.ui.WeatherViewModel
import com.example.myweather.features.weather.ui.WeatherCheckTimeFormatter
import com.example.myweather.features.weather.widget.WeatherWidgetCacheNotifier
import com.example.myweather.features.weather.widget.WeatherWidgetDataSource
import com.example.myweather.network.ApiClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock

val weatherModule = module {
    single<Clock> { Clock.systemUTC() }
    single<WeatherApi> { get<ApiClient>().weatherApi }
    single { get<WeatherDatabase>().weatherCacheDao() }
    single<WeatherCacheNotifier> { WeatherWidgetCacheNotifier(androidContext()) }
    single<WeatherRepository> { WeatherRepositoryImpl(get(), get(), get(), clock = get()) }
    factory { GetWeatherCheckAgeUseCase(get()) }
    single { WeatherCheckTimeFormatter(get()) }
    factory { GetWeatherForecastUseCase() }
    factory { RefreshSavedWeatherUseCase(get()) }
    factory { WeatherWidgetDataSource(get(), get(), get()) }
    viewModel { WeatherViewModel(get(), get(), get(), get()) }
}
