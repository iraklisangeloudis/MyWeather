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
            val binding = ActivityMainBinding.inflate(activity.layoutInflater)
            binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
            val weatherUI = WeatherUI(binding, activity, activity.window) {}
            weatherUI.render(WeatherViewState())
            assertEquals(View.GONE, binding.recyclerViewTemperatures.visibility)
            val state = WeatherViewState(weather = GetWeatherForecastUseCase()(forecast()), cityName = "Athens")
            weatherUI.render(state)
            weatherUI.render(state.copy(isRefreshing = true))
            assertEquals("20.0 \u00b0C", binding.textViewTemperature.text.toString())
            assertEquals("Athens", binding.textViewCityName.text.toString())
            assertEquals(View.VISIBLE, binding.textViewTemperature.visibility)
            assertEquals(View.VISIBLE, binding.progressBar.visibility)
        }
    }

    @Test
    fun stateUpdatesKeepSuggestionsUntilTheyAreCleared() = runBlocking {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val binding = ActivityMainBinding.inflate(activity.layoutInflater)
            binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
            val weatherUI = WeatherUI(binding, activity, activity.window) {}
            val state = WeatherViewState(weather = GetWeatherForecastUseCase()(forecast()),
                suggestions = listOf(LocationSuggestion(1.0, 2.0, "Athens")))
            weatherUI.render(state)
            val adapter = binding.listViewLocations.adapter
            weatherUI.render(state.copy(isRefreshing = true))
            assertSame(adapter, binding.listViewLocations.adapter)
            assertEquals(View.VISIBLE, binding.listViewLocations.visibility)
            assertEquals(View.GONE, binding.textViewTemperature.visibility)
            weatherUI.render(state.copy(suggestions = emptyList()))
            assertEquals(View.GONE, binding.listViewLocations.visibility)
            assertEquals(View.VISIBLE, binding.textViewTemperature.visibility)
        }
    }
}
