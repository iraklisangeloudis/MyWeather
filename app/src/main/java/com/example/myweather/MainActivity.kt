package com.example.myweather

import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import com.example.myweather.presentation.utils.LocationHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.presentation.adapter.TemperatureAdapter
import com.example.myweather.presentation.ui.WeatherUiController
import com.example.myweather.presentation.utils.LocationHandler.requestLocationPermission
import com.example.myweather.presentation.viewmodel.CityNameState
import com.example.myweather.presentation.viewmodel.LocationState
import com.example.myweather.presentation.viewmodel.WeatherState
import com.example.myweather.presentation.viewmodel.WeatherViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels {
        (application as WeatherApplication).container.viewModelFactory
    }
    private var locationJob: Job? = null
    private lateinit var uiController: WeatherUiController
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var isKeyboardVisible = false
    private var backPressCount = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uiController = WeatherUiController(binding, this, window) { location ->
            locationJob?.cancel()
            viewModel.selectLocation(location.latitude, location.longitude)
        }
        setupRecyclerView()
        setupTheme()
        setupViewModelObserver()
        setupListeners()
        if (savedInstanceState == null && LocationHandler.isLocationPermissionGranted(this)) {
            checkLocationAndFetchData()
        }
    }

    override fun onBackPressed() {
        if (isKeyboardVisible || binding.listViewLocations.visibility == View.VISIBLE) {
            // Dismiss the keyboard and list view
            viewModel.searchLocations("")
            uiController.hideKeyboardAndListView()
        } else {
            if (backPressCount == 0) {
                backPressCount++
                // Inform user to press again to exit
                Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show()
                // Reset the count after 2 seconds
                handler.postDelayed({ backPressCount = 0 }, 2000)
            } else {
                super.onBackPressed()
            }
        }
        binding.editTextLocation.clearFocus()
        uiController.showMain()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location fetch from API
                checkLocationAndFetchData()
            } else {
                Log.e("WeatherApp", "Location permission denied.")
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTemperatures.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Initialize recyclerViewTemperatures with an empty list
        binding.recyclerViewTemperatures.adapter = TemperatureAdapter()
    }

    private fun setupTheme() {
        // Set the status and navigation bar colors to Clear Day Blue on load
        window.statusBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
    }

    //ViewModel Observer
    private fun setupViewModelObserver() {
        viewModel.weatherState.observe(this) { state ->
            when (state) {
                is WeatherState.Loading -> uiController.showLoading(true)
                is WeatherState.Success -> uiController.renderWeather(state.weather)
                is WeatherState.Error -> {
                    state.cachedWeather?.let { uiController.renderWeather(it) } ?: uiController.handleWeatherFailure()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewModel.cityNameState.observe(this) { state ->
            when (state) {
                is CityNameState.Success -> binding.textViewCityName.text = state.cityName
                CityNameState.Error -> binding.textViewCityName.text = ""
            }
        }
        viewModel.locationState.observe(this) { state ->
            when (state) {
                LocationState.Idle -> uiController.dismissSuggestions()
                is LocationState.Success -> uiController.updateLocationList(state.locations)
                is LocationState.Error -> Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Event Listeners
    private fun setupListeners() {

        // Listener to detect keyboard visibility
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            isKeyboardVisible = keypadHeight > screenHeight * 0.15 // 15% of the screen height to consider it keyboard
        }

        // Use Location button listener
        binding.useLocationStatusButton.setOnClickListener {
            checkLocationAndFetchData()
            binding.editTextLocation.clearFocus()
        }

        // User typing listener
        binding.editTextLocation.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                viewModel.searchLocations(s?.toString().orEmpty())
            }
        })
    }
    private fun checkLocationAndFetchData() {
        locationJob?.cancel()
        locationJob = lifecycleScope.launch {
            if (!LocationHandler.isLocationPermissionGranted(this@MainActivity)) {
                requestLocationPermission(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
                return@launch
            }
            if (!LocationHandler.isLocationEnabled(this@MainActivity)) {
                Toast.makeText(this@MainActivity, "Location disabled", Toast.LENGTH_SHORT).show()
                return@launch
            }
            viewModel.searchLocations("")
            uiController.showLoading(true)
            try {
                val location = withTimeoutOrNull(15_000) { LocationHandler.getLocation(this@MainActivity) }
                if (location == null) {
                    uiController.showLoading(false)
                    Toast.makeText(this@MainActivity, "Unable to get location", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.selectLocation(location.latitude, location.longitude)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                uiController.showLoading(false)
                Toast.makeText(this@MainActivity, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

}