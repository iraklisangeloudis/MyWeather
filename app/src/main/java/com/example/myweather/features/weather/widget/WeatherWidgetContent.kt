package com.example.myweather.features.weather.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.myweather.MainActivity
import com.example.myweather.R
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails
import com.example.myweather.features.weather.ui.util.WeatherConditionDescriptions

internal data class WidgetSnapshot(
    val weather: WeatherForecastDetails,
    val icon: Bitmap?,
    val dailyIcons: List<Bitmap?>
)

@SuppressLint("RestrictedApi")
private val ink = ColorProvider(Color.White)
@SuppressLint("RestrictedApi")
private val muted = ColorProvider(Color(0xFFC9DAE9))
private val divider = Color(0x33FFFFFF)

@Composable
internal fun WidgetContent(context: Context, snapshot: WidgetSnapshot?, status: String) {
    val weather = snapshot?.weather
    val size = LocalSize.current
    val wide = size.width >= 300.dp
    val tall = size.height >= 240.dp
    val background = when {
        weather?.current?.isDay == 0 -> R.drawable.widget_sky_night
        weather != null && weather.current.weatherCode > 2 -> R.drawable.widget_sky_cloudy
        else -> R.drawable.widget_sky_day
    }
    Column(
        GlanceModifier.fillMaxSize().background(ImageProvider(background))
            .appWidgetBackground().cornerRadius(28.dp).padding(if (wide) 16.dp else 12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        if (snapshot == null) {
            EmptyWidget(context)
        } else {
            if (wide) {
                Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    CurrentSummary(context, snapshot, tall, GlanceModifier.width(120.dp).fillMaxHeight())
                    Spacer(GlanceModifier.width(16.dp))
                    DailyOutlook(context, snapshot, tall, GlanceModifier.defaultWeight().fillMaxHeight())
                }
            } else {
                CompactConditions(context, snapshot, tall, GlanceModifier.fillMaxWidth().defaultWeight())
            }
            WidgetFooter(context, status)
        }
    }
}

@Composable
private fun EmptyWidget(context: Context) {
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(context.getString(R.string.app_name), style = TextStyle(color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(8.dp))
        Text(context.getString(R.string.widget_empty), style = TextStyle(color = muted, fontSize = 13.sp))
    }
}

@Composable
private fun CurrentSummary(context: Context, snapshot: WidgetSnapshot, tall: Boolean, modifier: GlanceModifier) {
    val weather = snapshot.weather
    Column(modifier) {
        if (tall) {
            Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                WeatherArtwork(snapshot.icon, 88)
            }
            Temperature(context, weather.current.temperature, 52)
        } else {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Temperature(context, weather.current.temperature, 40, GlanceModifier.defaultWeight())
                WeatherArtwork(snapshot.icon, 52)
            }
        }
        Text(context.getString(WeatherConditionDescriptions.description(weather.current.weatherCode, weather.current.isDay)),
            style = TextStyle(color = ink, fontSize = 14.sp), maxLines = 2)
        Spacer(GlanceModifier.defaultWeight())
        LocationLabel(context, weather.locationName)
        weather.daily.firstOrNull()?.let {
            Text(context.getString(R.string.widget_high_low, it.maximum, it.minimum),
                style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 1)
        }
    }
}

@Composable
private fun CompactConditions(context: Context, snapshot: WidgetSnapshot, tall: Boolean, modifier: GlanceModifier) {
    val weather = snapshot.weather
    Column(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Temperature(context, weather.current.temperature, if (tall) 52 else 44, GlanceModifier.defaultWeight())
            WeatherArtwork(snapshot.icon, if (tall) 80 else 64)
        }
        Text(context.getString(WeatherConditionDescriptions.description(weather.current.weatherCode, weather.current.isDay)),
            style = TextStyle(color = ink, fontSize = 14.sp), maxLines = 1)
        Spacer(GlanceModifier.height(4.dp))
        LocationLabel(context, weather.locationName)
        if (tall) {
            Spacer(GlanceModifier.height(10.dp))
            weather.daily.firstOrNull()?.let {
                Text(context.getString(R.string.high_low, it.maximum, it.minimum),
                    style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 1)
            }
            Spacer(GlanceModifier.height(14.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                weather.hourly.take(3).forEach {
                    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(it.time.substringAfter('T'), style = TextStyle(color = muted, fontSize = 10.sp))
                        Text(context.getString(R.string.temperature_value, it.temperature),
                            style = TextStyle(color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyOutlook(context: Context, snapshot: WidgetSnapshot, tall: Boolean, modifier: GlanceModifier) {
    Column(modifier) {
        Text(context.getString(R.string.widget_next_days),
            style = TextStyle(color = muted, fontSize = 10.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(4.dp))
        snapshot.weather.daily.take(if (tall) 4 else 3).forEachIndexed { index, day ->
            if (index > 0) Box(GlanceModifier.fillMaxWidth().height(1.dp).background(divider)) {}
            Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                val label = if (day.day == "Today") context.getString(R.string.widget_today) else day.day.take(3)
                Text(label, GlanceModifier.defaultWeight(), style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 1)
                WeatherArtwork(snapshot.dailyIcons.getOrNull(index), 28,
                    context.getString(WeatherConditionDescriptions.description(day.weatherCode, 1)))
                Spacer(GlanceModifier.width(6.dp))
                Text(context.getString(R.string.temperature_value, day.maximum),
                    GlanceModifier.width(30.dp), style = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End), maxLines = 1)
                Text(context.getString(R.string.temperature_value, day.minimum),
                    GlanceModifier.width(28.dp), style = TextStyle(color = muted, fontSize = 12.sp, textAlign = TextAlign.End), maxLines = 1)
            }
        }
    }
}

@Composable
private fun LocationLabel(context: Context, name: String) {
    val label = name.lineSequence().firstOrNull().orEmpty().trim().trimEnd(',')
        .ifBlank { context.getString(R.string.saved_location) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(R.drawable.weather_location_icon), contentDescription = null,
            modifier = GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(muted))
        Spacer(GlanceModifier.width(3.dp))
        Text(label, style = TextStyle(color = muted, fontSize = 12.sp), maxLines = 1)
    }
}

@Composable
private fun Temperature(context: Context, value: Double, size: Int, modifier: GlanceModifier = GlanceModifier) {
    Text(context.getString(R.string.temperature_value, value), modifier,
        style = TextStyle(color = ink, fontSize = size.sp), maxLines = 1)
}

@Composable
private fun WeatherArtwork(bitmap: Bitmap?, size: Int, description: String? = null) {
    if (bitmap != null) Image(ImageProvider(bitmap), contentDescription = description, modifier = GlanceModifier.size(size.dp))
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetFooter(context: Context, status: String) {
    Spacer(GlanceModifier.height(8.dp))
    Box(GlanceModifier.fillMaxWidth().height(1.dp).background(divider)) {}
    Row(GlanceModifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val label = when (status) {
            "loading" -> context.getString(R.string.refreshing_weather)
            "failed" -> context.getString(R.string.widget_refresh_failed)
            "location" -> context.getString(R.string.widget_no_coordinates)
            else -> null
        }
        if (label != null) {
            Text(label, GlanceModifier.defaultWeight(), style = TextStyle(color = muted, fontSize = 10.sp), maxLines = 2)
        } else {
            Spacer(GlanceModifier.defaultWeight())
        }
        RefreshControl(context, status == "loading")
    }
}

@Composable
private fun RefreshControl(context: Context, refreshing: Boolean) {
    Box(
        modifier = GlanceModifier.size(32.dp).background(Color(0x18FFFFFF)).cornerRadius(16.dp)
            .clickable(actionRunCallback<RefreshWeatherAction>()).padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        if (refreshing) {
            CircularProgressIndicator(modifier = GlanceModifier.size(18.dp), color = ink)
        } else {
            Image(ImageProvider(R.drawable.widget_refresh_icon), contentDescription = context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.size(18.dp))
        }
    }
}
