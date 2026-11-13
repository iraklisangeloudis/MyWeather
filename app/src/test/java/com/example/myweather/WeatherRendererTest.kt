package com.example.myweather

import android.app.Activity
import android.app.Application
import android.view.View
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.model.LocationSuggestion
import com.example.myweather.presentation.adapter.TemperatureAdapter
import com.example.myweather.presentation.ui.WeatherRenderer
import com.example.myweather.presentation.ui.toUiModel
import com.example.myweather.presentation.viewmodel.WeatherScreenState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WeatherRendererTest {
    @Test
    fun refreshingKeepsWeatherVisibleAndUsesCorrectTemperatureUnit() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val binding = ActivityMainBinding.inflate(activity.layoutInflater)
            binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
            val renderer = WeatherRenderer(binding, activity, activity.window) {}
            renderer.render(WeatherScreenState())
            assertEquals(View.GONE, binding.recyclerViewTemperatures.visibility)
            val state = WeatherScreenState(weather = forecast().toUiModel(), cityName = "Athens")
            renderer.render(state)
            renderer.render(state.copy(isRefreshing = true))
            assertEquals("20.0 \u00b0C", binding.textViewTemperature.text.toString())
            assertEquals("Athens", binding.textViewCityName.text.toString())
            assertEquals(View.VISIBLE, binding.textViewTemperature.visibility)
            assertEquals(View.VISIBLE, binding.progressBar.visibility)
        }
    }

    @Test
    fun stateUpdatesKeepSuggestionsUntilTheyAreCleared() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val binding = ActivityMainBinding.inflate(activity.layoutInflater)
            binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
            val renderer = WeatherRenderer(binding, activity, activity.window) {}
            val state = WeatherScreenState(weather = forecast().toUiModel(),
                suggestions = listOf(LocationSuggestion(1.0, 2.0, "Athens")))
            renderer.render(state)
            val adapter = binding.listViewLocations.adapter
            renderer.render(state.copy(isRefreshing = true))
            assertSame(adapter, binding.listViewLocations.adapter)
            assertEquals(View.VISIBLE, binding.listViewLocations.visibility)
            assertEquals(View.GONE, binding.textViewTemperature.visibility)
            renderer.render(state.copy(suggestions = emptyList()))
            assertEquals(View.GONE, binding.listViewLocations.visibility)
            assertEquals(View.VISIBLE, binding.textViewTemperature.visibility)
        }
    }
}
