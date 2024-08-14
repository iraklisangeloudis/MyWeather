package com.example.myweather.data.repositories

import com.example.myweather.data.network.responses.AddressNameResponse
import com.example.myweather.data.network.services.AddressNameService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CityNameRepository {

    private val addressNameService: AddressNameService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://geocode.maps.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        addressNameService = retrofit.create(AddressNameService::class.java)
    }

    fun fetchCityName(latitude: Double, longitude: Double, reverseGeocodeApiKey: String, callback: (String?) -> Unit) {
        addressNameService.getAddressName(latitude, longitude, reverseGeocodeApiKey).enqueue(object :
            Callback<AddressNameResponse> {

            override fun onResponse(call: Call<AddressNameResponse>, response: Response<AddressNameResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    res?.let {
                        val address = it.address
                        val cityName = when {
                            address.village != null && address.county != null -> "${address.village},\n${address.county}"
                            address.village != null -> address.village
                            address.town != null && address.county != null -> "${address.town},\n${address.county}"
                            address.town != null -> address.town
                            address.city != null && address.county != null -> "${address.city},\n${address.county}"
                            address.city != null -> address.city
                            address.county != null -> address.county
                            address.region != null -> address.region
                            address.stateDistrict != null -> address.stateDistrict
                            address.hamlet != null -> address.hamlet
                            address.suburb != null -> address.suburb
                            address.country != null -> address.country
                            else -> ""
                        }
                        callback(cityName)
                    } ?: callback(null)
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<AddressNameResponse>, t: Throwable) {
                callback(null)
            }
        })
    }
}