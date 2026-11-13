package com.example.myweather.features.weather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/** Work is shared by all instances: every widget follows the app's one saved location. */
object WeatherWidgetUpdates {
    private const val PERIODIC = "weather-widget-periodic"
    const val REFRESH = "refresh"

    fun installed(context: Context): Boolean = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, WeatherWidgetReceiver::class.java)).isNotEmpty()

    fun render(context: Context) {
        // A launcher update must never turn a successful cache write into a weather failure.
        runCatching {
            if (installed(context)) WorkManager.getInstance(context).enqueueUniqueWork(
                "weather-widget-render", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WeatherWidgetWorker>().build()
            )
        }.onFailure { Log.w("WeatherWidget", "Unable to schedule widget update", it) }
    }

    fun refresh(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "weather-widget-refresh", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WeatherWidgetWorker>()
                .setInputData(workDataOf(REFRESH to true)).build()
        )
    }

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeatherWidgetWorker>(1, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(REFRESH to true)).build()
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(PERIODIC)
            cancelUniqueWork("weather-widget-refresh")
            cancelUniqueWork("weather-widget-render")
        }
    }
}
