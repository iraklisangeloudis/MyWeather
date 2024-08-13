package com.example.myweather.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: String,
    val temperature: Double,
    val humidity: Int,
    val apparentTemperature: Double,
    val isDay: Int,
    val weatherCode: Int,
    val windSpeed: Double
)
