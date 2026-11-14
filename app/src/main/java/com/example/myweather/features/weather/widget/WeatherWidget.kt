package com.example.myweather.features.weather.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.currentState
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.example.myweather.features.weather.ui.WeatherIcons
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
    override val sizeMode = SizeMode.Responsive(setOf(
        DpSize(180.dp, 160.dp), DpSize(180.dp, 260.dp),
        DpSize(300.dp, 200.dp), DpSize(300.dp, 280.dp)
    ))

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Room keeps an active Glance session current; a new session always reloads the cache.
        val snapshots = dataSource.observe().mapLatest { weather ->
            weather?.let {
                WidgetSnapshot(
                    weather = it,
                    icon = icon(context, it.current.weatherCode, it.current.isDay, 224),
                    dailyIcons = it.daily.take(4).map { day -> icon(context, day.weatherCode, 1, 96) }
                )
            }
        }.catch { emit(null) }.flowOn(Dispatchers.IO)
        val initialSnapshot = snapshots.first()
        provideContent {
            val snapshot by snapshots.collectAsState(initial = initialSnapshot)
            val status = currentState<Preferences>()[WeatherWidgetWorker.STATUS].orEmpty()
            WidgetContent(context, snapshot, status)
        }
    }

    private fun icon(context: Context, weatherCode: Int, isDay: Int, size: Int): Bitmap? = runCatching {
        val composition = LottieCompositionFactory.fromRawResSync(context,
            WeatherIcons.resource(weatherCode, isDay)).value ?: return null
        val drawable = LottieDrawable().apply {
            enableMergePathsForKitKatAndAbove(true)
            setComposition(composition)
            progress = 0f
            setBounds(0, 0, size, size)
        }
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { drawable.draw(Canvas(it)) }
    }.getOrNull()
}
