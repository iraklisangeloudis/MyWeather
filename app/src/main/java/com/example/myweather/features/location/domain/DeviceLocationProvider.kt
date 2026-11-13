package com.example.myweather.features.location.domain

import com.example.myweather.features.location.domain.model.Coordinates

interface DeviceLocationProvider {
    fun hasPermission(): Boolean
    fun isEnabled(): Boolean
    suspend fun getLocation(): Coordinates?
}
