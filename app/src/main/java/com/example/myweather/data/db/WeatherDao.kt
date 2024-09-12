package com.example.myweather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myweather.data.db.entities.*

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(weather: CurrentWeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyWeather(weather: List<HourlyWeatherEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyWeather(weather: List<DailyWeatherEntity>)

    @Query("SELECT * FROM current_weather")
    suspend fun getCurrentWeather(): CurrentWeatherEntity?

    @Query("DELETE FROM current_weather")
    suspend fun clearCurrentWeather()

    @Query("SELECT * FROM hourly_weather")
    suspend fun getHourlyWeather(): List<HourlyWeatherEntity>

    @Query("DELETE FROM hourly_weather")
    suspend fun clearHourlyWeather()

    @Query("SELECT * FROM daily_weather")
    suspend fun getDailyWeather(): List<DailyWeatherEntity>

    @Query("DELETE FROM daily_weather")
    suspend fun clearDailyWeather()
}
