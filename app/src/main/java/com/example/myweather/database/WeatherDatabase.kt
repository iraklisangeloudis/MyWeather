package com.example.myweather.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myweather.features.weather.data.dao.WeatherCacheDao
import com.example.myweather.features.weather.data.model.*

@Database(
    entities = [CurrentWeatherEntity::class, HourlyWeatherEntity::class, DailyWeatherEntity::class],
    version = 3,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherCacheDao(): WeatherCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE current_weather ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE current_weather ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE current_weather ADD COLUMN longitude REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Historical caches have no trustworthy request timestamp.
                db.execSQL("ALTER TABLE current_weather ADD COLUMN checkedAtMillis INTEGER DEFAULT NULL")
            }
        }

        fun create(context: Context): WeatherDatabase = Room.databaseBuilder(
            context.applicationContext,
            WeatherDatabase::class.java,
            "weather.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
