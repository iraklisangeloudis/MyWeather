package com.example.myweather.features.weather

import com.example.myweather.features.location.domain.model.Coordinates
import com.example.myweather.features.weather.data.WeatherRepository
import com.example.myweather.features.weather.domain.RefreshSavedWeatherUseCase
import com.example.myweather.features.weather.domain.model.WeatherForecast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class RefreshSavedWeatherUseCaseTest {
    private class Repository(var cached: WeatherForecast?) : WeatherRepository {
        var requested: Coordinates? = null
        var failure: Exception? = null
        override suspend fun getCachedWeather() = cached
        override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast {
            requested = Coordinates(latitude, longitude)
            failure?.let { throw it }
            return forecast().copy(coordinates = requested).also { cached = it }
        }
        override suspend fun updateLocationName(latitude: Double, longitude: Double, name: String) {
            if (cached?.coordinates == Coordinates(latitude, longitude)) cached = cached?.copy(locationName = name)
        }
    }

    @Test fun refreshesSavedCoordinatesAndRestoresLabel() = runBlocking {
        val coordinates = Coordinates(37.98, 23.72)
        val repository = Repository(forecast().copy(coordinates = coordinates, locationName = "Athens"))
        assertTrue(RefreshSavedWeatherUseCase(repository)())
        assertEquals(coordinates, repository.requested)
        assertEquals("Athens", repository.cached?.locationName)
    }

    @Test fun emptyOrMigratedCacheDoesNotFetchWeather() = runBlocking {
        for (cached in listOf(null, forecast().copy(coordinates = null))) {
            val repository = Repository(cached)
            assertFalse(RefreshSavedWeatherUseCase(repository)())
            assertNull(repository.requested)
        }
    }

    @Test fun networkFailureKeepsPreviousForecast() = runBlocking {
        val cached = forecast().copy(coordinates = Coordinates(37.98, 23.72), locationName = "Athens")
        val repository = Repository(cached).apply { failure = IOException("Offline") }
        try {
            RefreshSavedWeatherUseCase(repository)()
            fail("Expected refresh failure")
        } catch (_: IOException) {
            assertEquals(cached, repository.cached)
        }
    }

    @Test fun cancellationPropagates() = runBlocking {
        val repository = Repository(forecast().copy(coordinates = Coordinates(1.0, 2.0)))
            .apply { failure = CancellationException("Cancelled") }
        try {
            RefreshSavedWeatherUseCase(repository)()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertNotNull(repository.cached)
        }
    }
}
