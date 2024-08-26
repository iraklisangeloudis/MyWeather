package com.example.myweather.data.repositories

import com.example.myweather.data.network.responses.LocationResponse
import com.example.myweather.data.network.services.LocationIQService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LocationRepository {

    private val locationIQService: LocationIQService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://us1.locationiq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        locationIQService = retrofit.create(LocationIQService::class.java)
    }

    suspend fun getAutocomplete(query: String, apiKey: String): List<LocationResponse>? {
        return try {
            locationIQService.getAutocomplete(apiKey, query)
        } catch (e: Exception) {
            null
        }
    }
}