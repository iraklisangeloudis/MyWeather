package com.example.myweather

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.myweather.data.local.LegacyWeatherCache
import com.example.myweather.data.local.db.WeatherDatabase
import com.example.myweather.data.remote.api.WeatherApi
import com.example.myweather.data.remote.dto.*
import com.example.myweather.data.repository.WeatherRepositoryImpl
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WeatherRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: WeatherDatabase
    private lateinit var repository: WeatherRepositoryImpl
    private var response = response(20.0)
    private val api = object : WeatherApi {
        override suspend fun fetchWeather(latitude: Double, longitude: Double, current: String,
            hourly: String, daily: String, timezone: String) = response
    }

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        repository = WeatherRepositoryImpl(api, database, LegacyWeatherCache(context))
    }

    @After fun tearDown() = database.close()

    @Test fun completeForecastSurvivesCacheRestoration() = runBlocking {
        assertNull(repository.getCachedWeather())
        val fresh = repository.refreshWeather(37.98, 23.72)
        val restored = WeatherRepositoryImpl(api, database, LegacyWeatherCache(context)).getCachedWeather()
        assertEquals(fresh, restored)
        assertEquals(48, restored!!.hourly.time.size)
    }

    @Test fun legacyPreferencesAreMigratedOnceAndRemoved() = runBlocking {
        val preferences = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        preferences.edit()
            .putString("current_weather", gson.toJson(response.current))
            .putString("hourly_weather", gson.toJson(response.hourly))
            .putString("daily_weather", gson.toJson(response.daily))
            .commit()
        val migrated = repository.getCachedWeather()
        assertNotNull(migrated)
        assertEquals(48, migrated!!.hourly.time.size)
        assertFalse(preferences.contains("hourly_weather"))
        assertEquals(migrated, repository.getCachedWeather())
    }

    @Test fun failedSnapshotReplacementRollsBackAllTables() = runBlocking {
        val original = repository.refreshWeather(37.98, 23.72)
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_daily BEFORE INSERT ON daily_weather BEGIN SELECT RAISE(ABORT, 'test failure'); END"
        )
        response = response(35.0)
        try {
            repository.refreshWeather(1.0, 2.0)
            fail("Expected the database write to fail")
        } catch (_: android.database.sqlite.SQLiteException) {
            // All earlier deletes/inserts must have been rolled back too.
        }
        assertEquals(original, repository.getCachedWeather())
    }

    private fun response(temperature: Double): WeatherResponse {
        val weather = forecast(temperature)
        return WeatherResponse(
            weather.current.let { Current(it.time, it.temperature, it.humidity, it.apparentTemperature, it.isDay, it.weatherCode, it.windSpeed) },
            weather.hourly.let { Hourly(it.time, it.temperature, it.weatherCode, it.isDay) },
            weather.daily.let { Daily(it.time, it.weatherCode, it.temperatureMax, it.temperatureMin, it.sunrise, it.sunset, it.precipitationProbabilityMax) }
        )
    }
}
