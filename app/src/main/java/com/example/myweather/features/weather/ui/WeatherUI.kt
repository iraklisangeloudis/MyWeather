package com.example.myweather.features.weather.ui

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myweather.R
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.databinding.ItemDailyWeatherBinding
import com.example.myweather.features.location.domain.GetCurrentLocationUseCase
import com.example.myweather.features.location.domain.model.LocationSuggestion
import com.example.myweather.features.weather.domain.model.WeatherForecastDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherUI(
    private val context: Context,
    private val requestLocationPermission: () -> Unit
) : KoinComponent {
    private val activity: AppCompatActivity get() = context as AppCompatActivity
    // The screen accesses the ViewModel; Android retains it in the host's ViewModelStore.
    private val viewModel: WeatherViewModel by lazy { activity.getViewModel<WeatherViewModel>() }
    private val getCurrentLocation: GetCurrentLocationUseCase by inject()
    private val locationRequests = Channel<LocationRequest>(Channel.CONFLATED)
    private lateinit var binding: ActivityMainBinding
    private lateinit var searchHeader: ExpandableSearchHeader

    fun createView(): View {
        if (context is AppCompatActivity) applyNightMode(context, viewModel.viewState.value.weather)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        searchHeader = ExpandableSearchHeader(binding)
        ViewCompat.setAccessibilityHeading(binding.searchResultsTitle, true)
        binding.locationHeader.setOnClickListener {
            if (fullLocationName.isNotBlank()) {
                locationDialog?.dismiss()
                locationDialog = MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.location_details)
                    .setMessage(fullLocationName)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
        return binding.root
    }

    private var scrollExpansion: WeatherScrollExpansion? = null
    private var refreshing = false
    private var lastBackPress = 0L
    private var fullLocationName = ""
    private var locationDialog: AlertDialog? = null

    /** View setup and state observation belong to the screen, while the host owns permissions. */
    fun onViewCreated() {
        collectLocationRequests()
        binding.imageViewWeather.repeatCount = ValueAnimator.INFINITE
        binding.imageViewWeather.speed = .75f
        binding.recyclerViewTemperatures.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
        scrollExpansion = WeatherScrollExpansion(binding.weatherScroll, binding.weatherContent)
        binding.searchButton.setOnClickListener { toggleSearch() }
        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ -> binding.weatherScroll.canScrollVertically(-1) }
        binding.swipeRefresh.setColorSchemeResources(R.color.weather_accent)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.weather_surface)
        val density = context.resources.displayMetrics.density
        binding.swipeRefresh.setProgressViewOffset(false, (-40 * density).toInt(), (56 * density).toInt())
        val refresh = {
            if (!refreshing) {
                closeSearch()
                renderRefresh(true)
                refreshCurrentLocation()
            }
        }
        binding.swipeRefresh.setOnRefreshListener { refresh() }
        // Screen readers can refresh without performing the pull gesture.
        val refreshLabel = context.getString(R.string.refresh_current_location)
        ViewCompat.replaceAccessibilityAction(binding.weatherScroll,
            AccessibilityNodeInfoCompat.AccessibilityActionCompat(R.id.action_refresh_weather, refreshLabel), refreshLabel
        ) { _, _ ->
            if (refreshing) false else { refresh(); true }
        }
        binding.editTextLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) { viewModel.searchLocations(s?.toString().orEmpty()) }
        })
        activity.onBackPressedDispatcher.addCallback(activity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchHeader.isExpanded || binding.listViewLocations.isVisible) {
                    closeSearch()
                } else if (SystemClock.elapsedRealtime() - lastBackPress < 2000 && lastBackPress != 0L) {
                    activity.finish()
                } else {
                    lastBackPress = SystemClock.elapsedRealtime()
                    Toast.makeText(context, R.string.press_back_again, Toast.LENGTH_SHORT).show()
                }
            }
        })
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    viewModel.viewState.collect { state ->
                        render(state)
                        state.message?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            viewModel.messageShown(it)
                        }
                    }
                } finally {
                    binding.imageViewWeather.pauseAnimation()
                }
            }
        }
        updateSystemBars(activity)
    }

    private fun collectLocationRequests() {
        activity.lifecycleScope.launch {
            locationRequests.receiveAsFlow().collectLatest { request ->
                when (request) {
                    LocationRequest.Device -> lookupDeviceLocation()
                    is LocationRequest.Selected -> viewModel.selectLocation(request.latitude, request.longitude)
                }
            }
        }
    }

    private fun refreshCurrentLocation() {
        viewModel.locationLookupStarted()
        locationRequests.trySend(LocationRequest.Device)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) refreshCurrentLocation()
        else viewModel.locationLookupFinished(context.getString(R.string.permission_denied))
    }

    private suspend fun lookupDeviceLocation() {
        viewModel.locationLookupStarted()
        try {
            when (val result = getCurrentLocation()) {
                GetCurrentLocationUseCase.Result.PermissionRequired -> requestLocationPermission()
                GetCurrentLocationUseCase.Result.Disabled -> viewModel.locationLookupFinished(context.getString(R.string.location_disabled))
                GetCurrentLocationUseCase.Result.Unavailable -> viewModel.locationLookupFinished(context.getString(R.string.location_unavailable))
                is GetCurrentLocationUseCase.Result.Located ->
                    viewModel.selectLocation(result.coordinates.latitude, result.coordinates.longitude)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            viewModel.locationLookupFinished(context.getString(R.string.location_unavailable))
        } finally {
            viewModel.locationLookupFinished()
        }
    }

    private sealed interface LocationRequest {
        data object Device : LocationRequest
        data class Selected(val latitude: Double, val longitude: Double) : LocationRequest
    }

    private fun updateSystemBars(activity: AppCompatActivity) {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        activity.window.statusBarColor = ContextCompat.getColor(context, R.color.weather_sky)
        activity.window.navigationBarColor = ContextCompat.getColor(context,
            if (Build.VERSION.SDK_INT >= 27 || night) R.color.weather_canvas else R.color.weather_ink)
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
    }

    private fun renderRefresh(isRefreshing: Boolean) {
        refreshing = isRefreshing
        // Disabling SwipeRefreshLayout clears its progress indicator, even during a request.
        binding.swipeRefresh.isRefreshing = isRefreshing
        binding.searchButton.isEnabled = !isRefreshing
        binding.editTextLocation.isEnabled = !isRefreshing
        binding.listViewLocations.isEnabled = !isRefreshing
        ViewCompat.setStateDescription(binding.swipeRefresh,
            if (isRefreshing) context.getString(R.string.refreshing_weather) else null)
    }

    fun onDestroyView() {
        locationRequests.close()
        binding.imageViewWeather.cancelAnimation()
        searchHeader.dispose()
        locationDialog?.dismiss()
        locationDialog = null
        scrollExpansion?.dispose()
        scrollExpansion = null
    }

    private companion object {
        fun applyNightMode(activity: AppCompatActivity, weather: WeatherForecastDetails?) {
            activity.delegate.localNightMode = when (weather?.current?.isDay) {
                0 -> AppCompatDelegate.MODE_NIGHT_YES
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
    }

    private var renderedWeather: WeatherForecastDetails? = null
    private var renderedSuggestions: List<LocationSuggestion> = emptyList()

    fun render(state: WeatherViewState) {
        (context as? AppCompatActivity)?.let { applyNightMode(it, state.weather) }
        val hasWeather = state.weather != null
        binding.emptyState.visibility = if (hasWeather) View.GONE else View.VISIBLE
        listOf(binding.weatherHero, binding.hourlyCard, binding.metricsSection, binding.sunSection, binding.dailySection).forEach {
            it.visibility = if (hasWeather) View.VISIBLE else View.GONE
        }
        binding.recyclerViewTemperatures.visibility = if (hasWeather) View.VISIBLE else View.GONE
        binding.dailyWeatherLayout.visibility = if (hasWeather) View.VISIBLE else View.GONE
        if (renderedWeather != state.weather) {
            state.weather?.let(::renderWeather)
            renderedWeather = state.weather
        }
        fullLocationName = state.cityName.trim()
        binding.locationHeader.isClickable = fullLocationName.isNotBlank()
        binding.locationHeader.isFocusable = fullLocationName.isNotBlank()
        binding.locationHeader.contentDescription = fullLocationName.ifBlank { context.getString(R.string.app_name) }
        binding.textViewCityName.text = state.cityName.lineSequence().firstOrNull().orEmpty().trim().trimEnd(',').ifBlank { context.getString(if (hasWeather) R.string.saved_location else R.string.app_name) }
        binding.locationContext.text = state.cityName.lineSequence().drop(1).joinToString(" ").trim().ifBlank { context.getString(R.string.weather_home) }
        renderRefresh(state.isLocating || state.isRefreshing)
        binding.swipeRefresh.isEnabled = state.searchQuery.isBlank()
        renderSearch(state)
        updateWeatherAnimation()
    }

    fun toggleSearch() {
        if (searchHeader.isExpanded) closeSearch() else {
            searchHeader.setExpanded(true)
            binding.searchResultsPanel.visibility = View.VISIBLE
            binding.searchStatus.setText(R.string.search_prompt)
            binding.swipeRefresh.visibility = View.GONE
            updateWeatherAnimation()
        }
    }

    fun closeSearch() {
        searchHeader.setExpanded(false)
        binding.editTextLocation.setText("")
        hideKeyboardAndListView()
        showMain()
    }

    fun hideKeyboardAndListView() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.editTextLocation.windowToken, 0)
        renderedSuggestions = emptyList()
        binding.listViewLocations.visibility = View.GONE
        binding.listViewLocations.adapter = null
        binding.searchResultsPanel.visibility = View.GONE
    }

    fun showMain() {
        binding.swipeRefresh.visibility = View.VISIBLE
        updateWeatherAnimation()
    }

    private fun updateWeatherAnimation() {
        val started = (context as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
        if (started && binding.swipeRefresh.isVisible && binding.weatherHero.isVisible && ValueAnimator.areAnimatorsEnabled()) {
            if (!binding.imageViewWeather.isAnimating) binding.imageViewWeather.resumeAnimation()
        } else {
            binding.imageViewWeather.pauseAnimation()
            if (!ValueAnimator.areAnimatorsEnabled()) binding.imageViewWeather.progress = 0f
        }
    }

    private fun renderWeather(weather: WeatherForecastDetails) {
        val current = weather.current
        binding.textViewTemperature.text = context.getString(R.string.temperature_value, current.temperature)
        binding.textViewTemperature.contentDescription = context.getString(R.string.temperature_accessible, current.temperature)
        binding.textViewCondition.setText(WeatherTheme.description(current.weatherCode, current.isDay))
        weather.daily.firstOrNull()?.let { binding.textViewHighLow.text = context.getString(R.string.high_low, it.maximum, it.minimum) }
        binding.textViewHumidity.text = context.getString(R.string.humidity_value, current.humidity)
        binding.textViewWindSpeed.text = context.getString(R.string.wind_value, current.windSpeed)
        binding.textViewFeelsLike.text = context.getString(R.string.temperature_value, current.apparentTemperature)
        WeatherIcons.bind(binding.imageViewWeather, current.weatherCode, current.isDay)
        binding.textViewTime.text = context.getString(R.string.weather_as_of, weather.weatherAsOf)
        (binding.recyclerViewTemperatures.adapter as TemperatureAdapter).submitList(weather.hourly)
        binding.sunRiseTextView.text = weather.sunrise
        binding.sunSetTextView.text = weather.sunset
        binding.daylightText.visibility = if (weather.daylightMinutes == null) View.GONE else View.VISIBLE
        weather.daylightMinutes?.let { binding.daylightText.text = context.getString(R.string.daylight_duration, it / 60, it % 60) }
        binding.dayCount.text = context.getString(R.string.forecast_days, weather.daily.size)
        binding.dailyWeatherLayout.removeAllViews()
        weather.daily.forEach { day ->
            val row = ItemDailyWeatherBinding.inflate(LayoutInflater.from(context), binding.dailyWeatherLayout, false)
            row.dayName.text = day.day
            WeatherIcons.bind(row.dayIcon, day.weatherCode, 1)
            row.dayPrecipitation.text = context.getString(R.string.humidity_value, day.precipitation)
            row.dayMaximum.text = context.getString(R.string.temperature_value, day.maximum)
            row.dayMinimum.text = context.getString(R.string.temperature_value, day.minimum)
            binding.dailyWeatherLayout.addView(row.root)
        }
    }

    private fun renderSearch(state: WeatherViewState) {
        val showingSearch = searchHeader.isExpanded || state.searchQuery.isNotBlank() || state.suggestions.isNotEmpty()
        binding.searchResultsPanel.isVisible = showingSearch
        binding.swipeRefresh.isVisible = !showingSearch
        binding.searchStatus.text = when {
            state.isSearching -> context.getString(R.string.search_in_progress, state.searchQuery)
            state.searchFailed -> context.getString(R.string.search_failed)
            state.suggestions.isNotEmpty() -> context.getString(R.string.search_results_hint)
            state.searchQuery.isNotBlank() -> context.getString(R.string.search_no_results)
            else -> context.getString(R.string.search_prompt)
        }
        if (state.suggestions.isEmpty()) {
            renderedSuggestions = emptyList()
            binding.listViewLocations.visibility = View.GONE
            binding.listViewLocations.adapter = null
        } else if (renderedSuggestions != state.suggestions) {
            updateLocationList(state.suggestions)
        }
    }

    private fun updateLocationList(locations: List<LocationSuggestion>) {
        renderedSuggestions = locations
        binding.listViewLocations.adapter = LocationSuggestionAdapter(context, locations)
        binding.listViewLocations.setOnItemClickListener { _, _, position, _ ->
            renderRefresh(true)
            val location = locations[position]
            locationRequests.trySend(LocationRequest.Selected(location.latitude, location.longitude))
            closeSearch()
            showMain()
        }
        binding.swipeRefresh.visibility = View.GONE
        binding.listViewLocations.visibility = View.VISIBLE
    }
}
