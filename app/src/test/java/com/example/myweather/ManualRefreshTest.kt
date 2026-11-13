package com.example.myweather

import android.Manifest
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import com.example.myweather.features.location.data.CityNameRepository
import com.example.myweather.features.location.data.LocationRepository
import com.example.myweather.features.location.domain.DeviceLocationProvider
import com.example.myweather.features.location.domain.model.Coordinates
import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.data.WeatherRepository
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.domain.model.WeatherForecast
import com.example.myweather.features.weather.forecast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = WeatherApplication::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ManualRefreshTest {
    private val dispatcher = StandardTestDispatcher()
    private val fetched = mutableListOf<Coordinates>()
    private var locationRequests = 0
    private var locationResult: CompletableDeferred<Coordinates>? = null
    private var cachedIsDay = 1
    private var hasLocationPermission = true
    private var cacheReads = 0

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        loadKoinModules(module {
            single<WeatherRepository> { object : WeatherRepository {
                override suspend fun getCachedWeather(): WeatherForecast {
                    cacheReads++
                    val cached = forecast()
                    return cached.copy(locationName = "Cached city", current = cached.current.copy(isDay = cachedIsDay))
                }
                override suspend fun refreshWeather(latitude: Double, longitude: Double): WeatherForecast {
                    fetched += Coordinates(latitude, longitude)
                    return forecast()
                }
            } }
            single<DeviceLocationProvider> { object : DeviceLocationProvider {
                override fun hasPermission() = hasLocationPermission
                override fun isEnabled() = true
                override suspend fun getLocation(): Coordinates {
                    locationRequests++
                    return locationResult?.await() ?: Coordinates(37.98, 23.72)
                }
            } }
            single<CityNameRepository> { object : CityNameRepository {
                override suspend fun fetchCityName(latitude: Double, longitude: Double) = "Resolved city"
            } }
            single<LocationRepository> { object : LocationRepository {
                override suspend fun fetchLocationSuggestions(query: String) = listOf(LocationSuggestion(51.5, -.12, "London"))
            } }
            factory { GetWeatherForecastUseCase(dispatcher) }
        })
    }

    @After fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test fun launchWithPermissionOnlyRestoresCache() = runTest(dispatcher) {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            advanceUntilIdle()
            assertEquals(0, locationRequests)
            assertTrue(fetched.isEmpty())
            assertEquals("Cached city", controller.get().findViewById<android.widget.TextView>(R.id.textViewCityName).text.toString())
        }
    }

    @Test fun manualRefreshReturnsToCurrentLocationAfterSearchingAnotherCity() = runTest(dispatcher) {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            advanceUntilIdle()
            activity.findViewById<ImageButton>(R.id.searchButton).performClick()
            activity.findViewById<EditText>(R.id.editTextLocation).setText("London")
            advanceUntilIdle()
            activity.findViewById<ListView>(R.id.listViewLocations).performItemClick(View(activity), 0, 0)
            advanceUntilIdle()
            assertEquals(listOf(Coordinates(51.5, -.12)), fetched)
            activity.findViewById<View>(R.id.weatherScroll).performAccessibilityAction(R.id.action_refresh_weather, null)
            advanceUntilIdle()
            assertEquals(1, locationRequests)
            assertEquals(listOf(Coordinates(51.5, -.12), Coordinates(37.98, 23.72)), fetched)
        }
    }
    @Test fun pendingLocationKeepsRefreshVisibleAndFailureRestoresControls() = runTest(dispatcher) {
        locationResult = CompletableDeferred()
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            advanceUntilIdle()
            val refresh = activity.findViewById<View>(R.id.weatherScroll)
            val search = activity.findViewById<ImageButton>(R.id.searchButton)
            val swipe = activity.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
            refresh.performAccessibilityAction(R.id.action_refresh_weather, null)
            runCurrent()
            assertTrue(swipe.isRefreshing)
            assertTrue(swipe.isEnabled)
            assertFalse(search.isEnabled)
            assertEquals(activity.getString(R.string.refreshing_weather), androidx.core.view.ViewCompat.getStateDescription(swipe))
            // Repeating the accessible action must not queue a second lookup.
            refresh.performAccessibilityAction(R.id.action_refresh_weather, null)
            runCurrent()
            assertEquals(1, locationRequests)
            locationResult!!.completeExceptionally(IllegalStateException("Location unavailable"))
            advanceUntilIdle()
            assertFalse(swipe.isRefreshing)
            assertTrue(refresh.isEnabled)
            assertTrue(search.isEnabled)
            assertTrue(fetched.isEmpty())
        }
    }

    @Test fun cachedNightForecastSelectsNightPaletteWithoutFetching() = runTest(dispatcher) {
        cachedIsDay = 0
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            advanceUntilIdle()
            assertEquals(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES, controller.get().delegate.localNightMode)
            assertEquals(0, locationRequests)
            assertTrue(fetched.isEmpty())
        }
    }

    @Test fun recreatedHostKeepsTheScreensViewModelAndDoesNotReloadOrFetch() = runTest(dispatcher) {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            advanceUntilIdle()
            assertEquals(1, cacheReads)
            controller.recreate()
            advanceUntilIdle()
            assertEquals(1, cacheReads)
            assertEquals("Cached city", controller.get().findViewById<android.widget.TextView>(R.id.textViewCityName).text.toString())
            assertEquals(0, locationRequests)
            assertTrue(fetched.isEmpty())
        }
    }

    @Test fun hostPermissionDenialEndsTheScreensRefresh() = runTest(dispatcher) {
        hasLocationPermission = false
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            advanceUntilIdle()
            val refresh = activity.findViewById<View>(R.id.weatherScroll)
            refresh.performAccessibilityAction(R.id.action_refresh_weather, null)
            advanceUntilIdle()
            activity.onRequestPermissionsResult(1, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), intArrayOf(android.content.pm.PackageManager.PERMISSION_DENIED))
            advanceUntilIdle()
            assertEquals(0, locationRequests)
            assertTrue(fetched.isEmpty())
            assertTrue(refresh.isEnabled)
        }
    }

    @Test fun hostPermissionGrantResumesTheScreensRefresh() = runTest(dispatcher) {
        hasLocationPermission = false
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            advanceUntilIdle()
            val refresh = activity.findViewById<View>(R.id.weatherScroll)
            refresh.performAccessibilityAction(R.id.action_refresh_weather, null)
            advanceUntilIdle()
            assertNotNull(shadowOf(activity).lastRequestedPermission)
            assertEquals(0, locationRequests)
            hasLocationPermission = true
            activity.onRequestPermissionsResult(1, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), intArrayOf(android.content.pm.PackageManager.PERMISSION_GRANTED))
            advanceUntilIdle()
            assertEquals(1, locationRequests)
            assertEquals(listOf(Coordinates(37.98, 23.72)), fetched)
            assertTrue(refresh.isEnabled)
        }
    }

}
