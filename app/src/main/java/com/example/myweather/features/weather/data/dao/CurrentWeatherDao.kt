package com.example.myweather.features.weather.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myweather.features.weather.data.model.CurrentWeatherEntity

@Dao
interface CurrentWeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(weather: CurrentWeatherEntity)

    @Query("SELECT * FROM current_weather")
    suspend fun getCurrentWeather(): CurrentWeatherEntity?

    @Query("UPDATE current_weather SET locationName = :name WHERE latitude = :latitude AND longitude = :longitude")
    suspend fun updateLocationName(latitude: Double, longitude: Double, name: String)

    @Query("DELETE FROM current_weather")
    suspend fun clearCurrentWeather()
}
