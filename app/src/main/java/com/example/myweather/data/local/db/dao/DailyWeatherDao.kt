package com.example.myweather.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myweather.data.local.db.entities.DailyWeatherEntity

@Dao
interface DailyWeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyWeather(weather: List<DailyWeatherEntity>)

    @Query("SELECT * FROM daily_weather ORDER BY date")
    suspend fun getDailyWeather(): List<DailyWeatherEntity>

    @Query("DELETE FROM daily_weather")
    suspend fun clearDailyWeather()
}