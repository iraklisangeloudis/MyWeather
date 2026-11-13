package com.example.myweather.features.weather.ui

import android.content.Context
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails

class WeatherUI(
    private val binding: ActivityMainBinding,
    private val context: Context,
    private val window: Window,
    private val onLocationSelected: (LocationSuggestion) -> Unit
) {

    private var renderedWeather: WeatherForecastDetails? = null
    private var hasRenderedWeather = false
    private var renderedSuggestions: List<LocationSuggestion> = emptyList()

    fun render(state: WeatherViewState) {
        if (!hasRenderedWeather || renderedWeather != state.weather) {
            state.weather?.let(::renderWeather) ?: renderEmptyWeather()
            renderedWeather = state.weather
            hasRenderedWeather = true
        }
        binding.textViewCityName.text = state.cityName
        if (state.suggestions.isEmpty()) {
            dismissSuggestions()
            if (state.weather == null) {
                binding.recyclerViewTemperatures.visibility = View.GONE
                binding.dailyWeatherLayout.visibility = View.GONE
            }
        } else if (renderedSuggestions != state.suggestions) {
            updateLocationList(state.suggestions)
        }
        showLoading(state.isLoading)
    }

    // Main visibility handling
    fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun hideKeyboardAndListView() {
        // Dismiss the keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextLocation.windowToken, 0)
        // Hide the list view
        renderedSuggestions = emptyList()
        binding.listViewLocations.visibility = View.GONE
        // Clear the adapter to ensure no residual data
        binding.listViewLocations.adapter = null
    }

    private fun setMainVisibility(visibility: Int) {
        val views = listOf(
            binding.useLocationStatusButton,
            binding.textViewTemperature,
            binding.textViewWindSpeed,
            binding.textViewHumidity,
            binding.textViewFeelsLike,
            binding.imageViewWeather,
            binding.textViewCityName,
            binding.secondLayout,
            binding.recyclerViewTemperatures,
            binding.textViewTime,
            binding.thirdLayout,
            binding.dailyWeatherLayout
        )
        views.forEach { it.visibility = visibility }
    }

    fun hideMain() {
        setMainVisibility(View.GONE)
    }

    fun showMain() {
        setMainVisibility(View.VISIBLE)
    }

    private fun renderWeather(weather: WeatherForecastDetails) {
        val current = weather.current
        binding.apply {
            textViewTemperature.text = "${current.temperature} °C"
            textViewHumidity.text = "${current.humidity} %"
            textViewWindSpeed.text = "${current.windSpeed} km/h"
            textViewFeelsLike.text = "${current.apparentTemperature} °C"
            imageViewWeather.setImageResource(WeatherTheme.getWeatherIcon(current.weatherCode, current.isDay))
            textViewTime.text = weather.updatedAt
            WeatherTheme.changeTheme(current.weatherCode, current.isDay, context, window, mainLayout)
            (recyclerViewTemperatures.adapter as TemperatureAdapter).submitList(weather.hourly)
            sunRiseTextView.text = weather.sunrise
            sunSetTextView.text = weather.sunset
            dailyWeatherLayout.removeAllViews()
            weather.daily.forEach { day ->
                DailyWeatherUtils.addWeatherData(context, dailyWeatherLayout, day.day, day.precipitation, day.maximum, day.minimum)
            }
        }
    }

    private fun renderEmptyWeather() {
        binding.apply {
            textViewTemperature.text = "--°C"
            textViewHumidity.text = "--%"
            textViewWindSpeed.text = "--km/h"
            textViewFeelsLike.text = "--°C"
            imageViewWeather.setImageResource(WeatherTheme.getWeatherIcon(-1, -1))
            textViewTime.text = ""
            recyclerViewTemperatures.visibility = View.GONE
            dailyWeatherLayout.visibility = View.GONE
            sunRiseTextView.text = "-"
            sunSetTextView.text = "-"
        }
    }

    private fun dismissSuggestions() {
        renderedSuggestions = emptyList()
        binding.listViewLocations.visibility = View.GONE
        binding.listViewLocations.adapter = null
        showMain()
    }

    private fun updateLocationList(locations: List<LocationSuggestion>) {
        renderedSuggestions = locations
        binding.listViewLocations.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, locations.map { it.displayName })
        binding.listViewLocations.setOnItemClickListener { _, _, position, _ ->
            onLocationSelected(locations[position])
            hideKeyboardAndListView()
            binding.editTextLocation.setText("")
            showMain()
        }
        hideMain()
        binding.listViewLocations.visibility = View.VISIBLE
    }
}
