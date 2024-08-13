package com.example.myweather.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hourly_weather")
data class HourlyWeatherEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Int
)
