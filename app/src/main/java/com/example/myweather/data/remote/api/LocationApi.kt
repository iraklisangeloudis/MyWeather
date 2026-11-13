package com.example.myweather.data.remote.api

import com.example.myweather.data.remote.dto.LocationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LocationApi {
    @GET("v1/autocomplete.php")
    suspend fun fetchLocationSuggestions(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("format") format: String = "json"
    ): List<LocationResponse>
}