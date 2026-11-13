package com.example.myweather

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.myweather.data.repository.*
import com.example.myweather.model.*
import com.example.myweather.presentation.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val city = object : CityNameRepository {
        override suspend fun fetchCityName(latitude: Double, longitude: Double) = "Athens"
    }
    private val weather = object : WeatherRepository {
        override suspend fun getCachedWeather() = forecast()
        override suspend fun refreshWeather(latitude: Double, longitude: Double) = forecast(latitude)
    }

    @Test
    fun typingCancelsDebounceAndAnAlreadyRunningSearch() = runTest(dispatcher) {
        val started = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        val locations = object : LocationRepository {
            override suspend fun fetchLocationSuggestions(query: String): List<LocationSuggestion> {
                started += query
                try {
                    delay(2000)
                    return listOf(LocationSuggestion(1.0, 2.0, query))
                } catch (e: CancellationException) {
                    cancelled += query
                    throw e
                }
            }
        }
        val vm = WeatherViewModel(weather, city, locations, dispatcher)
        runCurrent()
        vm.searchLocations("A")
        advanceTimeBy(500)
        vm.searchLocations("Athens")
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(listOf("Athens"), started)
        vm.searchLocations("London")
        advanceUntilIdle()
        assertEquals(listOf("Athens"), cancelled)
        assertEquals("London", (vm.locationState.value as LocationState.Success).locations.single().displayName)
        vm.searchLocations("Paris")
        advanceTimeBy(1000)
        runCurrent()
        vm.searchLocations("")
        advanceUntilIdle()
        assertEquals(listOf("Athens", "Paris"), cancelled)
        assertEquals(LocationState.Idle, vm.locationState.value)
    }

    @Test
    fun selectingNewLocationCancelsOlderWeatherRequest() = runTest(dispatcher) {
        val cancelled = mutableListOf<Double>()
        val delayedWeather = object : WeatherRepository {
            override suspend fun getCachedWeather() = forecast()
            override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast {
                try {
                    delay(2000)
                    return forecast(latitude)
                } catch (e: CancellationException) {
                    cancelled += latitude
                    throw e
                }
            }
        }
        val locations = object : LocationRepository {
            override suspend fun fetchLocationSuggestions(query: String) = emptyList<LocationSuggestion>()
        }
        val vm = WeatherViewModel(delayedWeather, city, locations, dispatcher)
        runCurrent()
        assertEquals(20.0, (vm.weatherState.value as WeatherState.Success).weather.current.temperature, 0.0)
        vm.selectLocation(10.0, 1.0)
        runCurrent()
        vm.selectLocation(30.0, 1.0)
        advanceUntilIdle()
        assertEquals(listOf(10.0), cancelled)
        assertEquals(30.0, (vm.weatherState.value as WeatherState.Success).weather.current.temperature, 0.0)
    }
}
