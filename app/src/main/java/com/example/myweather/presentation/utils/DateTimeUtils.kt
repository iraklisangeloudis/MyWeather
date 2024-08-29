package com.example.myweather.presentation.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateTimeUtils {
    fun formattedDateTime(dateTimeString: String): String {
        // Define the input and output formatters
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        // Parse the input string to a LocalDateTime object
        val currentDateTime = LocalDateTime.parse(dateTimeString, inputFormatter)
        // Format the time
        val formattedTime = currentDateTime.format(timeFormatter)
        // Get the day of the week
        val dayOfWeek = currentDateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        // Combine the day of the week, formatted date and time
        // Set the formatted date-time to the TextView
        return "$dayOfWeek, $formattedTime"
    }
}