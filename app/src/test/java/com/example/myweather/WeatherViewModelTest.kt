package com.example.myweather

import android.app.Application
import androidx.lifecycle.ViewModelStore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import com.example.myweather.data.repository.*
import com.example.myweather.model.*
import com.example.myweather.presentation.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WeatherViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private fun model(weather: WeatherRepository, locations: LocationRepository, city: CityNameRepository = this.city): WeatherViewModel {
        val vm = WeatherViewModel(weather, city, locations, dispatcher)
        stores += ViewModelStore().also { it.put("weather", vm) }
        return vm
    }

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
        val vm = model(weather, locations)
        runCurrent()
        vm.searchLocations("A")
        advanceTimeBy(500)
        vm.searchLocations("Athens")
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(listOf("Athens"), started)
        vm.searchLocations("London")
        runCurrent()
        assertEquals(listOf("Athens"), cancelled)
        advanceUntilIdle()
        assertEquals(listOf("Athens"), cancelled)
        assertEquals("London", vm.screenState.value.suggestions.single().displayName)
        vm.searchLocations("Paris")
        advanceTimeBy(1000)
        runCurrent()
        vm.searchLocations("")
        advanceUntilIdle()
        assertEquals(listOf("Athens", "Paris"), cancelled)
        assertTrue(vm.screenState.value.suggestions.isEmpty())
        assertFalse(vm.screenState.value.isSearching)
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
        val vm = model(delayedWeather, locations)
        runCurrent()
        assertEquals(20.0, vm.screenState.value.weather!!.current.temperature, 0.0)
        vm.selectLocation(10.0, 1.0)
        runCurrent()
        vm.selectLocation(30.0, 1.0)
        advanceUntilIdle()
        assertEquals(listOf(10.0), cancelled)
        assertEquals(30.0, vm.screenState.value.weather!!.current.temperature, 0.0)
    }
    @Test
    fun repeatingSameQueryKeepsItsCompletedResults() = runTest(dispatcher) {
        val locations = object : LocationRepository {
            override suspend fun fetchLocationSuggestions(query: String) = listOf(LocationSuggestion(1.0, 2.0, query))
        }
        val vm = model(weather, locations)
        vm.searchLocations("Athens")
        advanceUntilIdle()
        vm.searchLocations("Athens")
        assertEquals("Athens", vm.screenState.value.suggestions.single().displayName)
        assertFalse(vm.screenState.value.isSearching)
    }

    @Test
    fun failedRefreshRetainsCachedWeatherAndAcknowledgesMessage() = runTest(dispatcher) {
        val failing = object : WeatherRepository {
            override suspend fun getCachedWeather() = forecast()
            override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast = throw IOException("offline")
        }
        val locations = object : LocationRepository {
            override suspend fun fetchLocationSuggestions(query: String) = emptyList<LocationSuggestion>()
        }
        val vm = model(failing, locations)
        vm.selectLocation(10.0, 1.0)
        advanceUntilIdle()
        assertEquals(20.0, vm.screenState.value.weather!!.current.temperature, 0.0)
        assertFalse(vm.screenState.value.isLoading)
        val message = vm.screenState.value.message!!
        vm.messageShown(message)
        assertNull(vm.screenState.value.message)
    }

    @Test
    fun selectionDuringCacheRestorationIsKeptAndNewCityMatchesWeather() = runTest(dispatcher) {
        val delayedCache = object : WeatherRepository {
            override suspend fun getCachedWeather(): WeatherForecast {
                delay(5000)
                return forecast()
            }
            override suspend fun refreshWeather(latitude: Double, longitude: Double) = forecast(latitude)
        }
        val locations = object : LocationRepository {
            override suspend fun fetchLocationSuggestions(query: String) = emptyList<LocationSuggestion>()
        }
        val names = object : CityNameRepository {
            override suspend fun fetchCityName(latitude: Double, longitude: Double): String {
                delay(2000)
                return "City $latitude"
            }
        }
        val vm = model(delayedCache, locations, names)
        vm.selectLocation(10.0, 1.0)
        vm.selectLocation(30.0, 1.0)
        advanceUntilIdle()
        assertEquals(30.0, vm.screenState.value.weather!!.current.temperature, 0.0)
        assertEquals("City 30.0", vm.screenState.value.cityName)
    }

}
