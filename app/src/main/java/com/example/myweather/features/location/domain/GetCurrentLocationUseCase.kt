package com.example.myweather.features.location.domain

import com.example.myweather.features.location.domain.model.Coordinates
import kotlinx.coroutines.withTimeoutOrNull

class GetCurrentLocationUseCase(private val provider: DeviceLocationProvider) {
    suspend operator fun invoke(): Result {
        if (!provider.hasPermission()) return Result.PermissionRequired
        if (!provider.isEnabled()) return Result.Disabled
        val coordinates = withTimeoutOrNull(15_000) { provider.getLocation() }
        return coordinates?.let { Result.Located(it) } ?: Result.Unavailable
    }

    sealed interface Result {
        data object PermissionRequired : Result
        data object Disabled : Result
        data object Unavailable : Result
        data class Located(val coordinates: Coordinates) : Result
    }
}
