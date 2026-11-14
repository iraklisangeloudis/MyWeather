package com.example.myweather.features.weather.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.myweather.features.weather.data.model.CurrentWeatherEntity
import com.example.myweather.features.weather.data.model.DailyWeatherEntity
import com.example.myweather.features.weather.data.model.HourlyWeatherEntity
import kotlinx.coroutines.flow.Flow

data class WeatherCacheSnapshot(
    val current: CurrentWeatherEntity,
    val hourly: List<HourlyWeatherEntity>,
    val daily: List<DailyWeatherEntity>
)

@Dao
interface WeatherCacheDao {
    @Transaction
    suspend fun getSnapshot(): WeatherCacheSnapshot? {
        val current = getCurrentWeather() ?: return null
        val hourly = getHourlyWeather()
        val daily = getDailyWeather()
        if (hourly.isEmpty() || daily.isEmpty()) return null
        return WeatherCacheSnapshot(current, hourly, daily)
    }

    /** Replace all three tables together; a failed write retains the previous forecast. */
    @Transaction
    suspend fun replaceSnapshot(snapshot: WeatherCacheSnapshot) {
        clearCurrentWeather()
        clearHourlyWeather()
        clearDailyWeather()
        insertCurrentWeather(snapshot.current)
        insertHourlyWeather(snapshot.hourly)
        insertDailyWeather(snapshot.daily)
    }

    @Query("SELECT * FROM current_weather")
    fun observeCurrentWeather(): Flow<CurrentWeatherEntity?>

    @Query("SELECT * FROM current_weather")
    suspend fun getCurrentWeather(): CurrentWeatherEntity?

    @Query("SELECT * FROM hourly_weather ORDER BY time")
    suspend fun getHourlyWeather(): List<HourlyWeatherEntity>

    @Query("SELECT * FROM daily_weather ORDER BY date")
    suspend fun getDailyWeather(): List<DailyWeatherEntity>

    @Query("UPDATE current_weather SET locationName = :name WHERE latitude = :latitude AND longitude = :longitude")
    suspend fun updateLocationName(latitude: Double, longitude: Double, name: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(weather: CurrentWeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyWeather(weather: List<HourlyWeatherEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyWeather(weather: List<DailyWeatherEntity>)

    @Query("DELETE FROM current_weather")
    suspend fun clearCurrentWeather()

    @Query("DELETE FROM hourly_weather")
    suspend fun clearHourlyWeather()

    @Query("DELETE FROM daily_weather")
    suspend fun clearDailyWeather()
}
