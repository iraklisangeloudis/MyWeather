package com.example.myweather.presentation.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myweather.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DailyWeatherUtils {
    fun addWeatherData(context: Context, container: LinearLayout, day: String, precipitationProbability: Int, maxTemperature: Double, minTemperature: Double) {
        val dayLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        val textColor = ContextCompat.getColor(context, R.color.light)

        // Parse the date string to a LocalDate object
        val date = LocalDate.parse(day, DateTimeFormatter.ISO_DATE)
        // Get the day of the week and return it as a string
        var newDay: String? = null
        // Check if the date is today
        val today = LocalDate.now()
        if (date == today) {
            newDay = "Today"
        } else {
            // Get the day of the week and return it as a string in English
            newDay = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        }
        val dayView = TextView(context).apply {
            text = newDay
            setTextColor(textColor)
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.3f
            ).apply {
                setPadding(16, 0, 0, 0)
            }
        }

        val precipitationLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                .7f
            ).apply {
                setPadding(0, 8, 16, 0)
            }
        }

        val precipitationImage = ImageView(context).apply {
            setImageResource(R.drawable.raindrop)
            setSizeInDp(24f, 24f) // Set size in dp
        }

        val precipitationView = TextView(context).apply {
            text = "$precipitationProbability%"
            setTextColor(textColor)
            setPadding(8, 0, 0, 0)
        }

        precipitationLayout.addView(precipitationImage)
        precipitationLayout.addView(precipitationView)

        val maxTempView = TextView(context).apply {
            text = "${maxTemperature}°C"
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                .5f
            ).apply {
                setPadding(0, 0, 16, 0)
            }
        }

        val minTempView = TextView(context).apply {
            text = "${minTemperature}°C"
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                .5f
            ).apply {
                setPadding(0, 0, 16, 0)
            }
        }

        dayLayout.addView(dayView)
        dayLayout.addView(precipitationLayout)
        dayLayout.addView(maxTempView)
        dayLayout.addView(minTempView)

        container.addView(dayLayout)
    }

    private fun ImageView.setSizeInDp(widthDp: Float, heightDp: Float) {
        val widthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, Resources.getSystem().displayMetrics).toInt()
        val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, Resources.getSystem().displayMetrics).toInt()
        layoutParams = LinearLayout.LayoutParams(widthPx, heightPx).apply {
            setMargins(0, 0, 8, 0) // Margin right
        }
    }
}