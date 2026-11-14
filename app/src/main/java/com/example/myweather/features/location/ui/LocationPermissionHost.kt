package com.example.myweather.features.location.ui

/** Android hosts own permission dialogs; screens request access through this contract. */
fun interface LocationPermissionHost {
    fun requestLocationPermission()
}
