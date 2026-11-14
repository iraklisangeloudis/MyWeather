package com.example.myweather

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import com.example.myweather.features.weather.ui.WeatherUI
import com.example.myweather.features.location.ui.LocationPermissionHost

/** Android host: creates the screen and forwards Android permission results. */
class MainActivity : AppCompatActivity(), LocationPermissionHost {
    private lateinit var weatherUI: WeatherUI
    private val locationPermissionRequest = registerForActivityResult(RequestMultiplePermissions()) {
        onLocationPermissionResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
    }

    override fun requestLocationPermission() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    override fun onDestroy() {
        super.onDestroy()
        weatherUI.onDestroy()
    }

    private fun setupUI() {
        weatherUI = WeatherUI(this, this)
        setContentView(weatherUI.createView())
        weatherUI.onViewCreated()
    }

    private fun onLocationPermissionResult(permissions: Map<String, Boolean>) {
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        weatherUI.onLocationPermissionResult(granted)
    }
}
