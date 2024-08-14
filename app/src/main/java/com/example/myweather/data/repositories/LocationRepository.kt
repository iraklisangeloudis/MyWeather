package com.example.myweather.data.repositories

import com.example.myweather.data.network.responses.LocationResponse
import com.example.myweather.data.network.services.LocationIQService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    fun getAutocomplete(query: String, apiKey: String, callback: (List<LocationResponse>?) -> Unit) {
        locationIQService.getAutocomplete(apiKey, query)
            .enqueue(object : Callback<List<LocationResponse>> {
                override fun onResponse(call: Call<List<LocationResponse>>, response: Response<List<LocationResponse>>) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }

                override fun onFailure(call: Call<List<LocationResponse>>, t: Throwable) {
                    callback(null)
                }
            })
    }
}