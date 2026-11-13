package com.example.myweather.presentation.ui

import android.content.Context
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.model.LocationSuggestion
import com.example.myweather.presentation.adapter.TemperatureAdapter
import com.example.myweather.presentation.utils.DailyWeatherUtils
import com.example.myweather.presentation.utils.WeatherTheme

class WeatherUiController(
    private val binding: ActivityMainBinding,
    private val context: Context,
    private val window: Window,
    private val onLocationSelected: (LocationSuggestion) -> Unit
) {

    // Main visibility handling
    fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun hideKeyboardAndListView() {
        // Dismiss the keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextLocation.windowToken, 0)
        // Hide the list view
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


    fun renderWeather(weather: WeatherUiModel) {
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
        showMain()
        showLoading(false)
        hideKeyboardAndListView()
    }

    fun handleWeatherFailure() {
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
        showLoading(false)
        hideKeyboardAndListView()
    }

    fun dismissSuggestions() {
        binding.listViewLocations.visibility = View.GONE
        binding.listViewLocations.adapter = null
        showMain()
    }

    fun updateLocationList(locations: List<LocationSuggestion>) {
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
