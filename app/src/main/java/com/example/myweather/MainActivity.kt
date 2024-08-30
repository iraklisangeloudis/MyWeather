package com.example.myweather

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.example.myweather.data.db.WeatherDatabase
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.data.network.responses.*
import com.example.myweather.data.preferences.WeatherPreferences
import com.example.myweather.data.repositories.*
import com.example.myweather.presentation.*
import com.example.myweather.presentation.utils.DateTimeUtils.formattedDateTime
import com.example.myweather.presentation.utils.LocationHandler.getLocation
import com.example.myweather.presentation.utils.LocationHandler.requestLocationPermission
import com.example.myweather.presentation.utils.LocationHandler.isLocationPermissionGranted
import com.example.myweather.presentation.utils.LocationHandler.isLocationEnabled
import com.example.myweather.presentation.utils.WeatherTheme.changeTheme
import com.example.myweather.presentation.utils.WeatherTheme.getWeatherIcon
import com.example.myweather.presentation.utils.DailyWeatherUtils.addWeatherData

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cityNameRepository: CityNameRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var locationRepository: LocationRepository
    private val weatherPreferences by lazy { WeatherPreferences(this) }
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(
            weatherRepository,
            cityNameRepository,
            weatherPreferences,
            DatabaseRepositoryImpl(WeatherDatabase.getDatabase(this)),
            locationRepository
        )
    }
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val locationIQApiKey = BuildConfig.LOCATION_IQ_API_KEY
    private val reverseGeocodeApiKey = BuildConfig.REVERSE_GEOCODE_API_KEY
    private var isKeyboardVisible = false
    private var backPressCount = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeRepositories()
        initialSetup()
        observeViewModel()
        setupListeners()
        requestLocationPermission(this, LOCATION_PERMISSION_REQUEST_CODE)

        // Checks if shared preferences contains the weather data
        val sharedPreferences = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.contains("current_weather") &&
            sharedPreferences.contains("hourly_weather") &&
            sharedPreferences.contains("daily_weather")
        ){
            loadWeatherDataFromSharedPreferences()
        }
        // Fetch Weather Data on start
        checkLocationAndFetchData()
    }

    override fun onBackPressed() {
        if (isKeyboardVisible || binding.listViewLocations.visibility == View.VISIBLE) {
            // Dismiss the keyboard and list view
            hideKeyboardAndListView()
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
        showMain()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location fetch
                checkLocationAndFetchData()
            } else {
                Log.e("WeatherApp", "Location permission denied.")
            }
        }
    }

    // Loading and keyboard visibility handling
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun hideKeyboardAndListView() {
        // Hide the keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextLocation.windowToken, 0)
        // Hide the list view
        binding.listViewLocations.visibility = View.GONE
        // Clear the adapter to ensure no residual data
        binding.listViewLocations.adapter = null
    }

    // Main Visibility
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

    private fun hideMain() {
        setMainVisibility(View.GONE)
    }

    private fun showMain() {
        setMainVisibility(View.VISIBLE)
    }


    // UI Updates
    private fun loadWeatherDataFromSharedPreferences() {
        val currentWeather = weatherPreferences.getCurrentWeather()
        val hourlyWeather = weatherPreferences.getHourlyWeather()
        val dailyWeather = weatherPreferences.getDailyWeather()

        // Display the data if it's not null
        currentWeather?.let {
            updateCurrentWeatherUI(it)
        }

        hourlyWeather?.let {
            updateHourlyWeatherUI(it,currentWeather?.time ?: "")
        }

        dailyWeather?.let {
            updateDailyWeatherUI(it)
        }
    }

    private fun checkLocationAndFetchData() {
        showLoading(isLoading = true)
        // Check if location permissions are granted
        if (isLocationPermissionGranted(this)) {
            // Check if location services are enabled
            if (isLocationEnabled(this)) {
                // Fetch the current location
                getLocation(this) { location ->
                    location?.let {
                        viewModel.fetchWeather(it.latitude, it.longitude)
                        viewModel.fetchCityName(it.latitude, it.longitude, reverseGeocodeApiKey)
                    } ?: run {
                        Toast.makeText(this@MainActivity, "Unable to get location", Toast.LENGTH_SHORT).show()
                        showLoading(isLoading = false)
                    }
                }
            } else {
                Toast.makeText(this@MainActivity, "Location disabled", Toast.LENGTH_SHORT).show()
                showLoading(isLoading = false)
            }
        } else {
            Toast.makeText(this@MainActivity, "Location permission not granted", Toast.LENGTH_SHORT).show()
            showLoading(isLoading = false)
            // Request location permissions if not granted
            requestLocationPermission(this, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun handleWeatherResponse(weather: WeatherResponse) {
        weather.let {
            // Update UI
            updateCurrentWeatherUI(it.current)
            updateHourlyWeatherUI(it.hourly, it.current.time)
            updateDailyWeatherUI(it.daily)

            showLoading(isLoading = false)
            hideKeyboardAndListView()
        }
    }

    private fun updateCurrentWeatherUI(currentWeather: Current) {
        binding.textViewTemperature.text = "${currentWeather.temperature} °C"
        binding.textViewHumidity.text = "${currentWeather.humidity} %"
        binding.textViewWindSpeed.text = "${currentWeather.windSpeed} km/h"
        binding.textViewFeelsLike.text = "${currentWeather.apparentTemperature} °C"

        val weatherIconResId = getWeatherIcon(currentWeather.weatherCode, currentWeather.isDay)
        binding.imageViewWeather.setImageResource(weatherIconResId)
        binding.textViewTime.text = formattedDateTime(currentWeather.time)

        changeTheme(currentWeather.weatherCode, currentWeather.isDay, this ,window, binding.mainLayout)
    }

    private fun updateHourlyWeatherUI(hourlyWeather: Hourly, currentTime: String) {
        val currentDateTime = LocalDateTime.parse(currentTime)

        // Combine the hourly weather data into a list of HourlyData objects
        val hourlyData = hourlyWeather.time.indices.map { index ->
            HourlyData(
                time = hourlyWeather.time[index],
                temperature = hourlyWeather.temperature[index],
                weatherCode = hourlyWeather.weatherCode[index],
                isDay = hourlyWeather.isDay[index]
            )
        }
            // Filter to include only data after the current time
            .filter { data -> LocalDateTime.parse(data.time).isAfter(currentDateTime) }
            // Take the next 24 hours of data
            .take(24)

        // Create the adapter with the filtered hourly data
        val temperatureAdapter = TemperatureAdapter(hourlyData)

        // Update UI elements with the new adapter
        binding.recyclerViewTemperatures.adapter = temperatureAdapter
        binding.recyclerViewTemperatures.visibility = View.VISIBLE
    }

    private fun updateDailyWeatherUI(dailyWeather: Daily) {
        binding.sunRiseTextView.text = dailyWeather.sunrise[0].split('T')[1]
        binding.sunSetTextView.text = dailyWeather.sunset[0].split('T')[1]

        binding.dailyWeatherLayout.removeAllViews()
        for (i in dailyWeather.time.indices) {
            addWeatherData(
                this,
                binding.dailyWeatherLayout,
                dailyWeather.time[i],
                dailyWeather.precipitationProbabilityMax[i],
                dailyWeather.temperatureMax[i],
                dailyWeather.temperatureMin[i]
            )
        }
        binding.dailyWeatherLayout.visibility = View.VISIBLE
    }

    private fun handleWeatherFailure() {
        binding.textViewTemperature.text = "--°C"
        binding.textViewHumidity.text = "--%"
        binding.textViewWindSpeed.text = "--km/h"
        binding.textViewFeelsLike.text = "--°C"
        binding.imageViewWeather.setImageResource(getWeatherIcon(weatherCode = -1, isDay = -1))
        binding.textViewTime.text = ""
        binding.recyclerViewTemperatures.visibility = View.GONE
        binding.dailyWeatherLayout.visibility = View.GONE
        binding.sunRiseTextView.text = "-"
        binding.sunSetTextView.text = "-"

        showLoading(isLoading = false)
        hideKeyboardAndListView()
    }

    private fun updateLocationList(locations: List<LocationResponse>) {
        val adapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_list_item_1,
            locations.map { location -> location.displayName }
        )
        binding.listViewLocations.adapter = adapter

        hideMain()
        binding.listViewLocations.visibility = View.VISIBLE

        binding.listViewLocations.setOnItemClickListener { _, _, position, _ ->
            val selectedLocation = locations[position]
            viewModel.fetchWeather(selectedLocation.latitude.toDouble(), selectedLocation.longitude.toDouble())
            viewModel.fetchCityName(selectedLocation.latitude.toDouble(), selectedLocation.longitude.toDouble(), reverseGeocodeApiKey)
            hideKeyboardAndListView()
            binding.editTextLocation.setText("")
            showMain()
        }
    }


    // Initialize
    private fun initializeRepositories() {
        cityNameRepository = CityNameRepositoryImpl()
        weatherRepository = WeatherRepositoryImpl()
        locationRepository = LocationRepositoryImpl()
    }

    private fun initialSetup() {
        // setup the RecyclerView
        binding.recyclerViewTemperatures.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Initialize recyclerViewTemperatures with an empty list
        var temperatureAdapter = TemperatureAdapter(listOf())
        binding.recyclerViewTemperatures.adapter = temperatureAdapter

        // Set the status and navigation bar colors to Clear Day Blue on load
        window.statusBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
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
            private var debounceJob: Job? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                debounceJob?.cancel()
                debounceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(1000) // debounce delay
                    val query = s.toString()
                    if (query.isNotEmpty()) {
                        viewModel.getAutocomplete(query,locationIQApiKey)
                    } else {
                        binding.editTextLocation.clearFocus()
                        hideKeyboardAndListView()
                        showMain()
                    }
                }
            }
        })
    }

    //ViewModel Observer
    private fun observeViewModel() {
        viewModel.weatherState.observe(this) { state ->
            when (state) {
                is WeatherState.Loading -> showLoading(true)
                is WeatherState.Success -> handleWeatherResponse(state.weatherResponse)
                is WeatherState.Error -> handleWeatherFailure()
            }
        }
        viewModel.cityNameState.observe(this) { state ->
            when (state) {
                is CityNameState.Success -> binding.textViewCityName.text = state.cityName
                is CityNameState.Error -> binding.textViewCityName.text = ""
            }
        }
        viewModel.locationState.observe(this) { state ->
            when (state) {
                is LocationState.Success -> updateLocationList(state.locationResponse)
                is LocationState.Error -> Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}