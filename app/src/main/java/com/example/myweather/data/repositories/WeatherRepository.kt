package com.example.myweather.data.repositories

import com.example.myweather.data.network.responses.WeatherResponse
import com.example.myweather.data.network.services.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {

    private val weatherService: WeatherService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherService = retrofit.create(WeatherService::class.java)
    }

    fun fetchWeatherData(
        latitude: Double,
        longitude: Double,
        callback: (WeatherResponse?) -> Unit
    ) {
        weatherService.getCurrentWeather(
            latitude,
            longitude,
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m",
            "temperature_2m,weather_code,is_day",
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max"
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                callback(null)
            }
        })
    }
}