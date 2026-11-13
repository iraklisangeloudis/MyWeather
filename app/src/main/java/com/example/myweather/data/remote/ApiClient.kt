package com.example.myweather.data.remote

import com.example.myweather.data.remote.api.*
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
