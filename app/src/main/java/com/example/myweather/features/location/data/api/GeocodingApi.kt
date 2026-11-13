package com.example.myweather.features.location.data.api

import com.example.myweather.features.location.data.api.model.AddressNameResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    // https://geocode.maps.co/reverse
    @GET("reverse")
    suspend fun fetchAddress(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("api_key") apiKey: String
    ): AddressNameResponse
}
