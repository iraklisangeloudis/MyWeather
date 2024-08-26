package com.example.myweather.data.network.services

import com.example.myweather.data.network.responses.LocationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationIQService {
    @GET("v1/autocomplete.php")
    suspend fun getAutocomplete(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("format") format: String = "json"
    ): List<LocationResponse>
}