package com.example.myweather.features.weather.widget

import android.content.Context
import com.example.myweather.features.weather.data.WeatherCacheNotifier

class WeatherWidgetCacheNotifier(private val context: Context) : WeatherCacheNotifier {
    override fun notifyChanged() {
        WeatherWidgetUpdates.render(context)
    }
}
