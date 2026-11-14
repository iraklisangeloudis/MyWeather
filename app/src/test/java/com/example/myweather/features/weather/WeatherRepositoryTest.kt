package com.example.myweather.features.weather

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.myweather.database.WeatherDatabase
import com.example.myweather.features.weather.data.WeatherRepositoryImpl
import com.example.myweather.features.weather.data.WeatherCacheNotifier
import com.example.myweather.features.weather.data.api.WeatherApi
import com.example.myweather.features.weather.data.dao.WeatherCacheDao
import com.example.myweather.features.weather.data.api.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WeatherRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: WeatherDatabase
    private lateinit var cacheDao: WeatherCacheDao
    private lateinit var repository: WeatherRepositoryImpl
    private var response = response(20.0)
    private var cacheChanges = 0
    private var checkTime = Instant.parse("2026-11-14T12:44:00Z")
    private var apiFailure: IOException? = null
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(checkTime, zone)
        override fun instant(): Instant = checkTime
    }
    private val cacheNotifier = object : WeatherCacheNotifier {
        override fun notifyChanged() { cacheChanges++ }
    }
    private val api = object : WeatherApi {
        override suspend fun fetchWeather(latitude: Double, longitude: Double, current: String,
            hourly: String, daily: String, timezone: String): WeatherResponse {
            apiFailure?.let { throw it }
            return response
        }
    }

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, WeatherDatabase::class.java).build()
        cacheDao = database.weatherCacheDao()
        repository = WeatherRepositoryImpl(api, cacheDao, cacheNotifier, clock = clock)
    }

    @After fun tearDown() = database.close()

    @Test fun completeForecastSurvivesCacheRestoration() = runBlocking {
        assertNull(repository.getCachedWeather())
        val fresh = repository.refreshWeather(37.98, 23.72)
        val restored = WeatherRepositoryImpl(api, cacheDao, cacheNotifier)
            .getCachedWeather()
        assertEquals(fresh, restored)
        assertEquals(48, restored!!.hourly.time.size)
        assertEquals(1, cacheChanges)
        assertEquals(checkTime.toEpochMilli(), restored.checkedAtMillis)
    }

    @Test fun successfulRecheckAdvancesCheckTimeEvenWhenModelTimeIsUnchanged() = runBlocking {
        val first = repository.refreshWeather(37.98, 23.72)
        checkTime = checkTime.plusSeconds(60)
        val checkedAgain = repository.refreshWeather(37.98, 23.72)
        assertEquals(first.current.time, checkedAgain.current.time)
        assertEquals(checkTime.toEpochMilli(), checkedAgain.checkedAtMillis)
        assertNotEquals(first.checkedAtMillis, checkedAgain.checkedAtMillis)
        repository.updateLocationName(37.98, 23.72, "Athens")
        assertEquals(checkedAgain.checkedAtMillis, repository.getCachedWeather()!!.checkedAtMillis)
    }

    @Test fun failedRequestKeepsTheLastSuccessfulCheckTime() = runBlocking {
        val cached = repository.refreshWeather(37.98, 23.72)
        checkTime = checkTime.plusSeconds(600)
        apiFailure = IOException("offline")
        try {
            repository.refreshWeather(37.98, 23.72)
            fail("Expected a network failure")
        } catch (_: IOException) {
            assertEquals(cached, repository.getCachedWeather())
        }
    }

    @Test fun locationLabelIsSavedOnlyForMatchingForecastCoordinates() = runBlocking {
        repository.refreshWeather(37.98, 23.72)
        repository.updateLocationName(37.98, 23.72, "Athens")
        repository.updateLocationName(51.5, -.12, "An older request")
        val restored = repository.getCachedWeather()!!
        assertEquals("Athens", restored.locationName)
        assertEquals(com.example.myweather.features.location.domain.model.Coordinates(37.98, 23.72), restored.coordinates)
    }

    @Test fun versionOneCacheIsMigratedWithoutLosingWeather() = runBlocking {
        val name = "migration-weather.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE current_weather (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time TEXT NOT NULL, temperature REAL NOT NULL, humidity INTEGER NOT NULL, apparentTemperature REAL NOT NULL, isDay INTEGER NOT NULL, weatherCode INTEGER NOT NULL, windSpeed REAL NOT NULL)")
            old.execSQL("CREATE TABLE hourly_weather (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time TEXT NOT NULL, temperature REAL NOT NULL, weatherCode INTEGER NOT NULL, isDay INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE daily_weather (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, weatherCode INTEGER NOT NULL, maxTemperature REAL NOT NULL, minTemperature REAL NOT NULL, sunrise TEXT NOT NULL, sunset TEXT NOT NULL, precipitationProbability INTEGER NOT NULL)")
            old.execSQL("INSERT INTO current_weather VALUES (1, '2026-11-13T12:15', 20, 50, 20, 1, 0, 5)")
            old.version = 1
        }
        val migrated = Room.databaseBuilder(context, WeatherDatabase::class.java, name)
            .addMigrations(WeatherDatabase.MIGRATION_1_2, WeatherDatabase.MIGRATION_2_3).build()
        try {
            val current = migrated.weatherCacheDao().getCurrentWeather()!!
            assertEquals(20.0, current.temperature, 0.0)
            assertEquals("", current.locationName)
            assertNull(current.latitude)
            assertNull(current.checkedAtMillis)
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun versionTwoCacheKeepsAllWeatherAndAnUnknownCheckTime() = runBlocking {
        val name = "migration-weather-v2.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE current_weather (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time TEXT NOT NULL, temperature REAL NOT NULL, humidity INTEGER NOT NULL, apparentTemperature REAL NOT NULL, isDay INTEGER NOT NULL, weatherCode INTEGER NOT NULL, windSpeed REAL NOT NULL, locationName TEXT NOT NULL DEFAULT '', latitude REAL, longitude REAL)")
            old.execSQL("CREATE TABLE hourly_weather (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time TEXT NOT NULL, temperature REAL NOT NULL, weatherCode INTEGER NOT NULL, isDay INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE daily_weather (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, weatherCode INTEGER NOT NULL, maxTemperature REAL NOT NULL, minTemperature REAL NOT NULL, sunrise TEXT NOT NULL, sunset TEXT NOT NULL, precipitationProbability INTEGER NOT NULL)")
            old.execSQL("INSERT INTO current_weather VALUES (1, '2026-11-13T12:15', 20, 50, 20, 1, 0, 5, 'Athens', 37.98, 23.72)")
            old.execSQL("INSERT INTO hourly_weather VALUES (1, '2026-11-13T13:00', 21, 0, 1)")
            old.execSQL("INSERT INTO daily_weather VALUES (1, '2026-11-13', 0, 25, 15, '2026-11-13T07:00', '2026-11-13T17:00', 10)")
            old.version = 2
        }
        val migrated = Room.databaseBuilder(context, WeatherDatabase::class.java, name)
            .addMigrations(WeatherDatabase.MIGRATION_2_3).build()
        try {
            val snapshot = migrated.weatherCacheDao().getSnapshot()!!
            assertEquals("Athens", snapshot.current.locationName)
            assertEquals(37.98, snapshot.current.latitude!!, 0.0)
            assertEquals(20.0, snapshot.current.temperature, 0.0)
            assertEquals(21.0, snapshot.hourly.single().temperature, 0.0)
            assertEquals(25.0, snapshot.daily.single().maxTemperature, 0.0)
            assertNull(snapshot.current.checkedAtMillis)
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun failedSnapshotReplacementRollsBackAllTables() = runBlocking {
        val original = repository.refreshWeather(37.98, 23.72)
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_daily BEFORE INSERT ON daily_weather BEGIN SELECT RAISE(ABORT, 'test failure'); END"
        )
        response = response(35.0)
        checkTime = checkTime.plusSeconds(600)
        try {
            repository.refreshWeather(1.0, 2.0)
            fail("Expected the database write to fail")
        } catch (_: android.database.sqlite.SQLiteException) {
            // All earlier deletes/inserts must have been rolled back too.
        }
        assertEquals(original, repository.getCachedWeather())
        assertEquals(1, cacheChanges)
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
