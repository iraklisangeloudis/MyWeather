package com.example.myweather.features.weather.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.example.myweather.MainActivity
import com.example.myweather.R
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails
import com.example.myweather.features.weather.ui.WeatherIcons
import com.example.myweather.features.weather.ui.WeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherWidget : GlanceAppWidget(), KoinComponent {
    private val dataSource: WeatherWidgetDataSource by inject()
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp, 140.dp), DpSize(300.dp, 180.dp)))

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Room keeps an active Glance session current; a new session always reloads the cache.
        val snapshots = dataSource.observe().mapLatest { weather ->
            weather?.let {
                WidgetSnapshot(it, icon(context, it))
            }
        }.catch { emit(null) }.flowOn(Dispatchers.IO)
        val initialSnapshot = snapshots.first()
        provideContent {
            val snapshot by snapshots.collectAsState(initial = initialSnapshot)
            val status = currentState<Preferences>()[WeatherWidgetWorker.STATUS].orEmpty()
            WidgetContent(context, snapshot, status)
        }
    }

    private fun icon(context: Context, weather: WeatherForecastDetails): Bitmap? = runCatching {
        val composition = LottieCompositionFactory.fromRawResSync(context,
            WeatherIcons.resource(weather.current.weatherCode, weather.current.isDay)).value ?: return null
        val drawable = LottieDrawable().apply {
            enableMergePathsForKitKatAndAbove(true)
            setComposition(composition)
            progress = 0f
            setBounds(0, 0, 160, 160)
        }
        Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888).also { drawable.draw(Canvas(it)) }
    }.getOrNull()
}

private data class WidgetSnapshot(val weather: WeatherForecastDetails, val icon: Bitmap?)

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetContent(context: Context, snapshot: WidgetSnapshot?, status: String) {
    val weather = snapshot?.weather
    val night = weather?.current?.isDay == 0
    val ink = ColorProvider(if (night) Color(0xFFE4EDF7) else Color(0xFF23495F))
    val muted = ColorProvider(if (night) Color(0xFFB4C6D8) else Color(0xFF55778B))
    val background = if (night) Color(0xFF182D43) else Color(0xFFE9F4FB)
    val wide = LocalSize.current.width >= 300.dp
    Column(GlanceModifier.fillMaxSize().background(background).appWidgetBackground()
        .cornerRadius(24.dp).padding(if (wide) 16.dp else 12.dp).clickable(actionStartActivity<MainActivity>())) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(weather?.locationName?.lineSequence()?.firstOrNull()?.trim()?.trimEnd(',')
                ?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name),
                GlanceModifier.defaultWeight(), style = TextStyle(color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            if (weather != null && status != "loading") {
                Text(context.getString(R.string.widget_refresh),
                    GlanceModifier.clickable(actionRunCallback<RefreshWeatherAction>()).padding(horizontal = 6.dp, vertical = 4.dp),
                    style = TextStyle(color = ink, fontSize = 12.sp))
            }
        }
        if (weather == null) {
            Spacer(GlanceModifier.height(12.dp))
            Text(context.getString(R.string.widget_empty), style = TextStyle(color = muted, fontSize = 14.sp))
        } else {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(context.getString(R.string.temperature_value, weather.current.temperature),
                        style = TextStyle(color = ink, fontSize = 36.sp))
                    Text(context.getString(WeatherTheme.description(weather.current.weatherCode, weather.current.isDay)),
                        style = TextStyle(color = ink, fontSize = 13.sp), maxLines = 1)
                }
                snapshot.icon?.let {
                    Image(ImageProvider(it), contentDescription = null, modifier = GlanceModifier.size(64.dp))
                }
            }
            weather.daily.firstOrNull()?.let {
                Text(context.getString(R.string.high_low, it.maximum, it.minimum),
                    style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 1)
            }
            if (wide) {
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    weather.hourly.take(3).forEach {
                        Text(context.getString(R.string.widget_hour_temperature, it.time.substringAfter('T'), it.temperature),
                            GlanceModifier.defaultWeight(), style = TextStyle(color = ink, fontSize = 12.sp), maxLines = 1)
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            val footer = when (status) {
                "loading" -> context.getString(R.string.refreshing_weather)
                "failed" -> context.getString(R.string.widget_refresh_failed)
                "location" -> context.getString(R.string.widget_no_coordinates)
                else -> context.getString(R.string.weather_as_of, weather.weatherAsOf)
            }
            Text(footer, style = TextStyle(color = muted, fontSize = 11.sp), maxLines = if (wide) 2 else 1)
        }
    }
}

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WeatherWidgetUpdates.refresh(context)
    }
}
