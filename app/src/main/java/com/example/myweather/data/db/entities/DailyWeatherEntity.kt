package com.example.myweather.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_weather")
data class DailyWeatherEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val weatherCode: Int,
    val maxTemperature: Double,
    val minTemperature: Double,
    val sunrise: String,
    val sunset: String,
    val precipitationProbability: Int
)
