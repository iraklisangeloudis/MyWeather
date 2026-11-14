package com.example.myweather.features.weather.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.myweather.network.requestFailureMessage
import kotlinx.coroutines.CancellationException

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        var startRefresh = false
        updateAppWidgetState(context, glanceId) { state ->
            if (state[WeatherWidgetWorker.STATUS] != "loading") {
                state[WeatherWidgetWorker.STATUS] = "loading"
                startRefresh = true
            }
        }
        if (!startRefresh) return

        val widget = WeatherWidget()
        try {
            // Request feedback before waiting for the refresh worker to be scheduled.
            widget.update(context, glanceId)
            WeatherWidgetUpdates.refresh(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("WeatherWidget", requestFailureMessage(e))
            updateAppWidgetState(context, glanceId) { it[WeatherWidgetWorker.STATUS] = "failed" }
            widget.update(context, glanceId)
        }
    }
}
