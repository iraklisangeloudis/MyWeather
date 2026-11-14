package com.example.myweather.features.weather

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.example.myweather.features.location.data.*
import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.data.*
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.domain.model.*
import com.example.myweather.features.weather.ui.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
        val vm = WeatherViewModel(weather, city, locations, GetWeatherForecastUseCase(dispatcher))
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

    private val noLocations = object : LocationRepository {
        override suspend fun fetchLocationSuggestions(query: String) = emptyList<LocationSuggestion>()
    }

    @Test fun startupRemainsLoadingUntilCachedWeatherHasBeenRead() = runTest(dispatcher) {
        val cache = CompletableDeferred<WeatherForecast?>()
        var fetches = 0
        val repository = object : WeatherRepository {
            override suspend fun getCachedWeather() = cache.await()
            override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast {
                fetches++
                return forecast()
            }
        }
        val vm = model(repository, noLocations)
        assertTrue(vm.viewState.value.isRestoringCache)
        runCurrent()
        assertTrue(vm.viewState.value.isLoading)
        assertNull(vm.viewState.value.weather)
        cache.complete(forecast().copy(locationName = "Cached city"))
        advanceUntilIdle()
        assertFalse(vm.viewState.value.isRestoringCache)
        assertEquals("Cached city", vm.viewState.value.cityName)
        assertNotNull(vm.viewState.value.weather)
        assertEquals(0, fetches)
    }

    @Test fun missingOrFailedCacheFinishesStartupLoading() = runTest(dispatcher) {
        for (fail in listOf(false, true)) {
            val repository = object : WeatherRepository {
                override suspend fun getCachedWeather(): WeatherForecast? {
                    delay(100)
                    if (fail) throw IOException("cache unavailable")
                    return null
                }
                override suspend fun refreshWeather(latitude: Double, longitude: Double) = forecast()
            }
            val vm = model(repository, noLocations)
            assertTrue(vm.viewState.value.isRestoringCache)
            advanceUntilIdle()
            assertFalse(vm.viewState.value.isLoading)
            assertNull(vm.viewState.value.weather)
        }
    }

    @Test fun newWeatherWaitsForItsLocationNameWithoutReplacingThePreviousForecast() = runTest(dispatcher) {
        val name = CompletableDeferred<String?>()
        val repository = object : WeatherRepository {
            override suspend fun getCachedWeather() = forecast().copy(locationName = "Old city")
            override suspend fun refreshWeather(latitude: Double, longitude: Double) = forecast(latitude)
        }
        val names = object : CityNameRepository {
            override suspend fun fetchCityName(latitude: Double, longitude: Double) = name.await()
        }
        val vm = model(repository, noLocations, names)
        advanceUntilIdle()
        vm.selectLocation(30.0, 1.0)
        runCurrent()
        assertEquals("Old city", vm.viewState.value.cityName)
        assertEquals(20.0, vm.viewState.value.weather!!.current.temperature, 0.0)
        assertTrue(vm.viewState.value.isRefreshing)
        name.complete("New city")
        advanceUntilIdle()
        assertEquals("New city", vm.viewState.value.cityName)
        assertEquals(30.0, vm.viewState.value.weather!!.current.temperature, 0.0)
        assertFalse(vm.viewState.value.isRefreshing)
    }

    @Test fun failedLocationNameStillDisplaysNewWeatherAndStopsLoading() = runTest(dispatcher) {
        val names = object : CityNameRepository {
            override suspend fun fetchCityName(latitude: Double, longitude: Double): String? {
                throw IOException("name unavailable")
            }
        }
        val vm = model(weather, noLocations, names)
        advanceUntilIdle()
        vm.selectLocation(30.0, 1.0)
        advanceUntilIdle()
        assertEquals(30.0, vm.viewState.value.weather!!.current.temperature, 0.0)
        assertEquals("", vm.viewState.value.cityName)
        assertFalse(vm.viewState.value.isLoading)
    }

    @Test fun resumingReadsTheLatestCachedCheckWithoutFetchingWeather() = runTest(dispatcher) {
        var cached = forecast().copy(checkedAtMillis = 1_800_000_000_000L)
        var fetches = 0
        val repository = object : WeatherRepository {
            override suspend fun getCachedWeather() = cached
            override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast {
                fetches++
                return cached
            }
        }
        val locations = object : LocationRepository {
            override suspend fun fetchLocationSuggestions(query: String) = emptyList<LocationSuggestion>()
        }
        val vm = model(repository, locations)
        advanceUntilIdle()
        cached = cached.copy(checkedAtMillis = cached.checkedAtMillis!! + 7 * 60 * 60_000L)
        vm.reloadCachedWeather()
        advanceUntilIdle()
        assertEquals(cached.checkedAtMillis, vm.viewState.value.weather!!.checkedAtMillis)
        assertEquals(0, fetches)
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
        assertEquals("London", vm.viewState.value.suggestions.single().displayName)
        vm.searchLocations("Paris")
        advanceTimeBy(1000)
        runCurrent()
        vm.searchLocations("")
        advanceUntilIdle()
        assertEquals(listOf("Athens", "Paris"), cancelled)
        assertTrue(vm.viewState.value.suggestions.isEmpty())
        assertFalse(vm.viewState.value.isSearching)
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
        assertEquals(20.0, vm.viewState.value.weather!!.current.temperature, 0.0)
        vm.selectLocation(10.0, 1.0)
        runCurrent()
        vm.selectLocation(30.0, 1.0)
        advanceUntilIdle()
        assertEquals(listOf(10.0), cancelled)
        assertEquals(30.0, vm.viewState.value.weather!!.current.temperature, 0.0)
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
        assertEquals("Athens", vm.viewState.value.suggestions.single().displayName)
        assertFalse(vm.viewState.value.isSearching)
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
        assertEquals(20.0, vm.viewState.value.weather!!.current.temperature, 0.0)
        assertFalse(vm.viewState.value.isLoading)
        val message = vm.viewState.value.message!!
        vm.messageShown(message)
        assertNull(vm.viewState.value.message)
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
        assertEquals(30.0, vm.viewState.value.weather!!.current.temperature, 0.0)
        assertEquals("City 30.0", vm.viewState.value.cityName)
    }

}
