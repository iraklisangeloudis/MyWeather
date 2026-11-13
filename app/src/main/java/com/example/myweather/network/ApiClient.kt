package com.example.myweather.network

import com.example.myweather.features.location.data.api.*
import com.example.myweather.features.weather.data.api.WeatherApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    private val client = OkHttpClient.Builder().build()
    val weatherApi: WeatherApi = create("https://api.open-meteo.com/")
    val locationApi: LocationApi = create("https://us1.locationiq.com/")
    val geocodingApi: GeocodingApi = create("https://geocode.maps.co/")

    private inline fun <reified T> create(baseUrl: String): T = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(T::class.java)
}
