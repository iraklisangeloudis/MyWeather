package com.example.myweather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myweather.data.db.entities.*

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCurrentWeather(weather: CurrentWeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHourlyWeather(weather: List<HourlyWeatherEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDailyWeather(weather: List<DailyWeatherEntity>)

    @Query("SELECT * FROM current_weather")
    fun getCurrentWeather(): CurrentWeatherEntity?

    @Query("DELETE FROM current_weather")
    fun clearCurrentWeather()

    @Query("SELECT * FROM hourly_weather")
    fun getHourlyWeather(): List<HourlyWeatherEntity>

    @Query("DELETE FROM hourly_weather")
    fun clearHourlyWeather()

    @Query("SELECT * FROM daily_weather")
    fun getDailyWeather(): List<DailyWeatherEntity>

    @Query("DELETE FROM daily_weather")
    fun clearDailyWeather()
}
