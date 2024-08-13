package com.example.myweather.data.network.services

import com.example.myweather.data.network.responses.AddressNameResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AddressNameService {
    @GET("reverse")
    //https://geocode.maps.co/reverse
    fun getAddressName(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("key") apiKey: String
    ): Call<AddressNameResponse>
}