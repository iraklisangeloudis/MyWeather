package com.example.myweather.features.weather.data

/** Notifies cache consumers after a successful write without failing the saved forecast. */
interface WeatherCacheNotifier {
    fun notifyChanged()
}
