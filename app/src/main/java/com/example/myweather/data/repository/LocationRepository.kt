package com.example.myweather.data.repository

import android.util.Log
import com.example.myweather.model.LocationSuggestion
import com.example.myweather.data.remote.api.LocationApi
import kotlinx.coroutines.CancellationException

interface LocationRepository {
    suspend fun fetchLocationSuggestions(query: String): List<LocationSuggestion>?
}

class LocationRepositoryImpl(
    private val locationApi: LocationApi,
    private val apiKey: String
) : LocationRepository {

    override suspend fun fetchLocationSuggestions(query: String): List<LocationSuggestion>? {
        return try {
            locationApi.fetchLocationSuggestions(apiKey, query).map {
                LocationSuggestion(it.latitude.toDouble(), it.longitude.toDouble(), it.displayName)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("LocationRepository", requestFailureMessage(e))
            null
        }
    }
}