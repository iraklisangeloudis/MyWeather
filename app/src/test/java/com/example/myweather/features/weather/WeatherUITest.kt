package com.example.myweather.features.weather

import android.app.Activity
import android.app.Application
import android.view.View
import com.example.myweather.R
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.domain.GetWeatherForecastUseCase
import com.example.myweather.features.weather.ui.TemperatureAdapter
import com.example.myweather.features.weather.ui.WeatherUI
import com.example.myweather.features.weather.ui.WeatherViewState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WeatherUITest {
    @Test
    fun refreshingKeepsWeatherVisibleAndUsesCorrectTemperatureUnit() = runBlocking {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val weatherUI = WeatherUI(activity) {}
            val binding = ActivityMainBinding.bind(weatherUI.createView())
            binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
            weatherUI.render(WeatherViewState())
            assertEquals(View.GONE, binding.recyclerViewTemperatures.visibility)
            val state = WeatherViewState(weather = GetWeatherForecastUseCase()(forecast()), cityName = "Athens")
            weatherUI.render(state)
            weatherUI.render(state.copy(isRefreshing = true))
            assertEquals("20\u00b0", binding.textViewTemperature.text.toString())
            assertEquals("Athens", binding.textViewCityName.text.toString())
            assertEquals(View.VISIBLE, binding.textViewTemperature.visibility)
            assertTrue(binding.swipeRefresh.isRefreshing)
            assertEquals(activity.getString(R.string.refreshing_weather), androidx.core.view.ViewCompat.getStateDescription(binding.swipeRefresh))
            assertTrue(binding.swipeRefresh.isEnabled)
            assertFalse(binding.searchButton.isEnabled)
            weatherUI.render(state.copy(isLocating = true))
            assertTrue(binding.swipeRefresh.isRefreshing)
            weatherUI.render(state.copy(message = "Unable to refresh weather"))
            assertFalse(binding.swipeRefresh.isRefreshing)
            assertTrue(binding.searchButton.isEnabled)
            weatherUI.onDestroyView()
        }
    }

    @Test
    fun stateUpdatesKeepSuggestionsUntilTheyAreCleared() = runBlocking {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val weatherUI = WeatherUI(activity) {}
            val binding = ActivityMainBinding.bind(weatherUI.createView())
            binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
            val state = WeatherViewState(weather = GetWeatherForecastUseCase()(forecast()),
                suggestions = listOf(LocationSuggestion(1.0, 2.0, "Athens")))
            weatherUI.render(state)
            val adapter = binding.listViewLocations.adapter
            weatherUI.render(state.copy(isRefreshing = true))
            assertSame(adapter, binding.listViewLocations.adapter)
            assertEquals(View.VISIBLE, binding.listViewLocations.visibility)
            assertEquals(View.GONE, binding.swipeRefresh.visibility)
            weatherUI.render(state.copy(suggestions = emptyList()))
            assertEquals(View.GONE, binding.listViewLocations.visibility)
            assertEquals(View.VISIBLE, binding.textViewTemperature.visibility)
        }
    }
    @Test fun searchShowsLoadingEmptyAndFailureStatesWithoutHidingItsStatus() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val ui = WeatherUI(activity) {}
            val binding = ActivityMainBinding.bind(ui.createView())
            val searching = WeatherViewState(searchQuery = "Athens", isSearching = true)
            ui.render(searching)
            assertEquals(View.VISIBLE, binding.searchResultsPanel.visibility)
            assertEquals(View.GONE, binding.listViewLocations.visibility)
            assertFalse(binding.swipeRefresh.isRefreshing)
            assertEquals(activity.getString(R.string.search_in_progress, "Athens"), binding.searchStatus.text)
            ui.render(searching.copy(isSearching = false))
            assertEquals(activity.getString(R.string.search_no_results), binding.searchStatus.text)
            ui.render(searching.copy(isSearching = false, searchFailed = true))
            assertEquals(activity.getString(R.string.search_failed), binding.searchStatus.text)
            ui.render(WeatherViewState())
            assertEquals(View.GONE, binding.searchResultsPanel.visibility)
            assertEquals(View.VISIBLE, binding.swipeRefresh.visibility)
        }
    }

    @Test fun searchRowsSeparateCityFromRegionAndClearRecycledSubtitles() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val ui = WeatherUI(activity) {}
            val binding = ActivityMainBinding.bind(ui.createView())
            ui.render(WeatherViewState(searchQuery = "Athens", suggestions = listOf(
                LocationSuggestion(1.0, 2.0, "Athens, Attica, Greece"),
                LocationSuggestion(3.0, 4.0, "Athens")
            )))
            val adapter = binding.listViewLocations.adapter
            val row = adapter.getView(0, null, binding.listViewLocations)
            assertEquals("Athens", row.findViewById<android.widget.TextView>(R.id.cityName).text)
            assertEquals("Attica, Greece", row.findViewById<android.widget.TextView>(R.id.cityRegion).text)
            assertEquals("Athens, Attica, Greece", row.contentDescription)
            val recycled = adapter.getView(1, row, binding.listViewLocations)
            assertSame(row, recycled)
            assertEquals(View.GONE, row.findViewById<View>(R.id.cityRegion).visibility)
        }
    }

    @Test fun longLocationKeepsFullTextAndTapRevealsTheCompleteLocation() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val ui = WeatherUI(activity) {}
            val binding = ActivityMainBinding.bind(ui.createView())
            activity.setContentView(binding.root)
            val city = "Llanfairpwllgwyngyll and its surrounding neighbourhoods"
            val fullName = city + "\nAnglesey, Wales, United Kingdom"
            ui.render(WeatherViewState(cityName = fullName))
            val density = activity.resources.displayMetrics.density
            val width = (320 * density).toInt()
            val height = (800 * density).toInt()
            binding.root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            binding.root.layout(0, 0, width, height)
            assertEquals(fullName, binding.locationHeader.contentDescription)
            assertEquals(city, binding.textViewCityName.text)
            assertEquals("Anglesey, Wales, United Kingdom", binding.locationContext.text)
            assertTrue(binding.locationContext.top >= binding.textViewCityName.bottom)
            assertTrue(binding.locationHeader.performClick())
            val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
            assertEquals(fullName, dialog.findViewById<android.widget.TextView>(android.R.id.message).text)
            ui.onDestroyView()
            assertFalse(dialog.isShowing)
        }
    }

}
