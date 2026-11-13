package com.example.myweather.data.repository

import android.util.Log
import com.example.myweather.data.remote.api.GeocodingApi
import kotlinx.coroutines.CancellationException

interface CityNameRepository {
    suspend fun fetchCityName(latitude: Double, longitude: Double): String?
}

class CityNameRepositoryImpl(
    private val geocodingApi: GeocodingApi,
    private val reverseGeocodeApiKey: String
) : CityNameRepository {

    override suspend fun fetchCityName(latitude: Double, longitude: Double): String? {
        return try {
            val response = geocodingApi.fetchAddress(latitude, longitude, reverseGeocodeApiKey)
            val address = response.address
            when {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CityNameRepository", requestFailureMessage(e))
            null
        }
    }
}