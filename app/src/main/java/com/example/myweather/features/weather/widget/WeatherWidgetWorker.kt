package com.example.myweather.features.weather.widget

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myweather.features.weather.domain.RefreshSavedWeatherUseCase
import com.example.myweather.network.requestFailureMessage
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherWidgetWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters), KoinComponent {
    private val refreshSavedWeather: RefreshSavedWeatherUseCase by inject()
    override suspend fun doWork(): Result {
        if (!WeatherWidgetUpdates.installed(applicationContext)) return Result.success()
        if (!inputData.getBoolean(WeatherWidgetUpdates.REFRESH, false)) {
            GlanceAppWidgetManager(applicationContext).getGlanceIds(WeatherWidget::class.java).forEach { id ->
                updateAppWidgetState(applicationContext, id) {
                    if (it[STATUS] != "loading") it.remove(STATUS)
                }
            }
            WeatherWidget().updateAll(applicationContext)
            return Result.success()
        }
        return try {
            status("loading")
            status(if (refreshSavedWeather()) "" else "location")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("WeatherWidget", requestFailureMessage(e))
            status("failed")
            Result.failure()
        }
    }

    private suspend fun status(value: String) {
        GlanceAppWidgetManager(applicationContext).getGlanceIds(WeatherWidget::class.java).forEach { id ->
            updateAppWidgetState(applicationContext, id) { it[STATUS] = value }
        }
        WeatherWidget().updateAll(applicationContext)
    }

    companion object {
        val STATUS = stringPreferencesKey("weather_status")
    }
}
