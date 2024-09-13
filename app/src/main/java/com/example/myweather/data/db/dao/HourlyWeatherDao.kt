package com.example.myweather.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myweather.data.db.entities.HourlyWeatherEntity

@Dao
interface HourlyWeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyWeather(weather: List<HourlyWeatherEntity>)

    @Query("SELECT * FROM hourly_weather")
    suspend fun getHourlyWeather(): List<HourlyWeatherEntity>

    @Query("DELETE FROM hourly_weather")
    suspend fun clearHourlyWeather()
}