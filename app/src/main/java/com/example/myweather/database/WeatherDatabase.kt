package com.example.myweather.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myweather.features.weather.data.dao.CurrentWeatherDao
import com.example.myweather.features.weather.data.dao.DailyWeatherDao
import com.example.myweather.features.weather.data.dao.HourlyWeatherDao
import com.example.myweather.features.weather.data.model.*

@Database(
    entities = [CurrentWeatherEntity::class, HourlyWeatherEntity::class, DailyWeatherEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun currentWeatherDao(): CurrentWeatherDao
    abstract fun dailyWeatherDao(): DailyWeatherDao
    abstract fun hourlyWeatherDao(): HourlyWeatherDao

    companion object {
        fun create(context: Context): WeatherDatabase = Room.databaseBuilder(
            context.applicationContext,
            WeatherDatabase::class.java,
            "weather.db"
        ).fallbackToDestructiveMigration().build()
    }
}
