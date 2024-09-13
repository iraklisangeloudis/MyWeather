package com.example.myweather.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myweather.data.db.entities.CurrentWeatherEntity

@Dao
interface CurrentWeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(weather: CurrentWeatherEntity)

    @Query("SELECT * FROM current_weather")
    suspend fun getCurrentWeather(): CurrentWeatherEntity?

    @Query("DELETE FROM current_weather")
    suspend fun clearCurrentWeather()
}