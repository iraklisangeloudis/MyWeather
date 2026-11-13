package com.example.myweather.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myweather.data.local.db.dao.CurrentWeatherDao
import com.example.myweather.data.local.db.dao.DailyWeatherDao
import com.example.myweather.data.local.db.dao.HourlyWeatherDao
import com.example.myweather.data.local.db.entities.*

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
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
