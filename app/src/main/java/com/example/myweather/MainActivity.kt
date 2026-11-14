package com.example.myweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myweather.features.weather.ui.WeatherUI
import com.example.myweather.features.location.ui.LocationPermissionHost

/** Android host: creates the screen and forwards Android permission results. */
class MainActivity : AppCompatActivity(), LocationPermissionHost {
    private lateinit var weatherUI: WeatherUI
    private var awaitingLocationPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        awaitingLocationPermission = savedInstanceState?.getBoolean("awaitingLocationPermission") ?: false
        weatherUI = WeatherUI(this, this)
        setContentView(weatherUI.createView())
        weatherUI.onViewCreated()
    }

    override fun requestLocationPermission() {
        awaitingLocationPermission = true
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && awaitingLocationPermission) {
            awaitingLocationPermission = false
            weatherUI.onLocationPermissionResult(grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("awaitingLocationPermission", awaitingLocationPermission)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::weatherUI.isInitialized) weatherUI.onDestroyView()
        super.onDestroy()
    }

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
