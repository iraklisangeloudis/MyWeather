package com.example.myweather.features.location

import com.example.myweather.BuildConfig
import com.example.myweather.features.location.data.*
import com.example.myweather.features.location.data.api.*
import com.example.myweather.features.location.domain.DeviceLocationProvider
import com.example.myweather.features.location.domain.GetCurrentLocationUseCase
import com.example.myweather.network.ApiClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val locationModule = module {
    single<LocationApi> { get<ApiClient>().locationApi }
    single<GeocodingApi> { get<ApiClient>().geocodingApi }
    single<LocationRepository> { LocationRepositoryImpl(get(), BuildConfig.LOCATION_IQ_API_KEY) }
    single<CityNameRepository> { CityNameRepositoryImpl(get(), BuildConfig.REVERSE_GEOCODE_API_KEY) }
    single<DeviceLocationProvider> { AndroidLocationProvider(androidContext()) }
    factory { GetCurrentLocationUseCase(get()) }
}
