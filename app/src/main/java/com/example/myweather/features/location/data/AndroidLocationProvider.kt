package com.example.myweather.features.location.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.example.myweather.features.location.domain.DeviceLocationProvider
import com.example.myweather.features.location.domain.model.Coordinates
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidLocationProvider(private val context: Context) : DeviceLocationProvider {
    override fun hasPermission(): Boolean {
        return hasPrecisePermission() || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return availableProvider(locationManager) != null
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLocation(): Coordinates? = suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = availableProvider(locationManager)
        if (provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (continuation.isActive) continuation.resume(Coordinates(location.latitude, location.longitude))
                locationManager.removeUpdates(this)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(
            provider,
            0L,
            0f,
            locationListener
        )

        continuation.invokeOnCancellation {
            locationManager.removeUpdates(locationListener)
        }
    }

    private fun hasPrecisePermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun availableProvider(manager: LocationManager): String? = when {
        hasPrecisePermission() && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
}
