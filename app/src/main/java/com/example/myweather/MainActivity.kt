package com.example.myweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Rect
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.Executors
import com.example.myweather.data.db.WeatherDatabase
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.data.network.services.*
import com.example.myweather.data.network.responses.*
import com.example.myweather.data.db.entities.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var locationManager: LocationManager
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


        binding.recyclerViewTemperatures.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Initialize recyclerViewTemperatures with an empty list
        var temperatureAdapter = TemperatureAdapter(listOf())
        binding.recyclerViewTemperatures.adapter = temperatureAdapter

        //Set the status and navigation bar colors to Clear Day Blue on load
        window.statusBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)

        //Request permission to use location the first time the app is downloaded
        requestLocationPermission()

        //Retrofit set up for http requests
        val locationIQRetrofit = Retrofit.Builder()
            .baseUrl("https://us1.locationiq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val locationIQService = locationIQRetrofit.create(LocationIQService::class.java)

        // Listener to detect keyboard visibility
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            isKeyboardVisible = keypadHeight > screenHeight * 0.15 // 15% of the screen height to consider it keyboard
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        val sharedPreferences = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.contains("current_weather") &&
            sharedPreferences.contains("hourly_weather") &&
            sharedPreferences.contains("daily_weather")
        ){
            loadWeatherDataFromSharedPreferences()
        }
        checkLocationAndFetchData()

        binding.useLocationStatusButton.setOnClickListener {
            showLoading(isLoading = true)
            if(isLocationEnabled()){
                if (isLocationPermissionGranted()) {
                    getLocation { latitude, longitude ->
                        if (latitude != null && longitude != null) {
                            fetchWeatherData(latitude,longitude)
                            fetchAndDisplayCityName(latitude,longitude)
                        } else {
                            Toast.makeText(this@MainActivity, "Unable to get location", Toast.LENGTH_SHORT).show()
                            showLoading(isLoading = false)
                        }
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Location permission not granted", Toast.LENGTH_SHORT).show()
                    showLoading(isLoading = false)
                }
            }
            else{
                Toast.makeText(this@MainActivity, "Location disabled", Toast.LENGTH_SHORT).show()
                showLoading(isLoading = false)
            }
            binding.editTextLocation.clearFocus()
        }

        binding.editTextLocation.addTextChangedListener(object : TextWatcher {
            private var debounceJob: Job? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                debounceJob?.cancel()
                debounceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(800) // debounce delay
                    val query = s.toString()
                    if (query.isNotEmpty()) {
                        locationIQService.getAutocomplete(locationIQApiKey, query).enqueue(object : Callback<List<LocationResponse>> {
                            override fun onResponse(call: Call<List<LocationResponse>>, response: Response<List<LocationResponse>>) {
                                if (response.isSuccessful) {
                                    val locations = response.body() ?: emptyList()
                                    val adapter = ArrayAdapter(
                                        this@MainActivity,
                                        android.R.layout.simple_list_item_1,
                                        locations.map { it.displayName }
                                    )
                                    binding.listViewLocations.adapter = adapter
                                    hideMain()
                                    binding.listViewLocations.visibility = View.VISIBLE
                                    binding.listViewLocations.setOnItemClickListener { _, _, position, _ ->
                                        val selectedLocation = locations[position]
                                        fetchWeatherData(selectedLocation.latitude.toDouble(), selectedLocation.longitude.toDouble())
                                        fetchAndDisplayCityName(selectedLocation.latitude.toDouble(), selectedLocation.longitude.toDouble())
                                        hideKeyboardAndListView()
                                        binding.editTextLocation.setText("")
                                        showMain()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<List<LocationResponse>>, t: Throwable) {
                                Toast.makeText(this@MainActivity, "Failed to load location data", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        binding.editTextLocation.clearFocus()
                        binding.useLocationStatusButton.visibility = View.VISIBLE
                        hideKeyboardAndListView()
                        showMain()
                    }
                }
            }
        })

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

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getLocation(callback: (Double?, Double?) -> Unit) {
        try {
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    callback(latitude, longitude)
                    locationManager.removeUpdates(this)  // Remove updates after getting the location
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Request location updates from both GPS and Network providers
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                locationListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0f,
                locationListener
            )

        } catch (e: SecurityException) {
            callback(null, null)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun formattedDateTime(dateTimeString: String):String {
        // Define the input and output formatters
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        // Parse the input string to a LocalDateTime object
        val currentDateTime = LocalDateTime.parse(dateTimeString, inputFormatter)
        // Format the time
        val formattedTime = currentDateTime.format(timeFormatter)
        // Get the day of the week
        val dayOfWeek = currentDateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        // Combine the day of the week, formatted date and time
        val formattedDateTime = "$dayOfWeek, $formattedTime"
        // Set the formatted date-time to the TextView
        return formattedDateTime
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val weatherRetrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val weatherService = weatherRetrofit.create(WeatherService::class.java)


        weatherService.getCurrentWeather(latitude, longitude, "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m", "temperature_2m,weather_code,is_day","weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max").enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    weather?.let {

                        binding.textViewTemperature.text = "${it.current.temperature} °C"
                        binding.textViewHumidity.text = "${it.current.humidity} %"
                        binding.textViewWindSpeed.text = "${it.current.windSpeed} km/h"
                        binding.textViewFeelsLike.text = "${it.current.apparentTemperature} °C"
                        val weatherIconResId = getWeatherIcon(weatherCode = it.current.weatherCode, isDay = it.current.isDay)
                        binding.imageViewWeather.setImageResource(weatherIconResId)
                        binding.textViewTime.text = formattedDateTime(it.current.time)

                        changeTheme(it.current.weatherCode,it.current.isDay)

                        val currentTime = LocalDateTime.parse(it.current.time) // Parse the current time
                        val hourlyData = it.hourly.time.zip(it.hourly.temperature.zip(it.hourly.weatherCode.zip(it.hourly.isDay))) { time, triple ->
                            val (temperature, pair) = triple
                            val (weatherCode, isDay) = pair
                            HourlyData(time, temperature, weatherCode, isDay)
                        }.filter { data ->
                            LocalDateTime.parse(data.time).isAfter(currentTime)
                        }.take(24) // Take the 24 hours after the current time
                        var temperatureAdapter = TemperatureAdapter(hourlyData)
                        binding.recyclerViewTemperatures.adapter = temperatureAdapter
                        binding.recyclerViewTemperatures.visibility = View.VISIBLE

                        binding.sunRiseTextView.text= it.daily.sunrise[0].split('T')[1]
                        binding.sunSetTextView.text= it.daily.sunset[0].split('T')[1]

                        // Clear the container to remove previous data
                        binding.dailyWeatherLayout.removeAllViews()
                        for (i in it.daily.time.indices) {
                            addWeatherData(
                                binding.dailyWeatherLayout,
                                it.daily.time[i],
                                it.daily.precipitationProbabilityMax[i],
                                it.daily.temperatureMax[i],
                                it.daily.temperatureMin[i]
                            )
                        }
                        binding.dailyWeatherLayout.visibility=View.VISIBLE

                        val database = WeatherDatabase.getDatabase(this@MainActivity)
                        //this@MainActivity.deleteDatabase("weather.db")

                        insertCurrentWeather(database, it.current)
                        logCurrentWeather(database)
                        insertDailyWeather(database, it.daily)
                        logDailyWeather(database)
                        insertHourlyWeather(database, it.hourly, it.current.time)
                        logHourlyWeather(database)

                        // Save data to SharedPreferences
                        saveWeatherDataToSharedPreferences(it.current, it.hourly, it.daily)
                    }
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                binding.textViewTemperature.text = "--°C"
                binding.textViewHumidity.text = "--%"
                binding.textViewWindSpeed.text = "--km/h"
                binding.textViewFeelsLike.text = "--°C"
                binding.imageViewWeather.setImageResource(getWeatherIcon(weatherCode = -1, isDay = -1))
                binding.textViewTime.text = ""
                binding.recyclerViewTemperatures.visibility = View.GONE
                binding.dailyWeatherLayout.visibility=View.GONE
                binding.sunRiseTextView.text= "-"
                binding.sunSetTextView.text= "-"
            }
        })
        showLoading(isLoading = false)
        // Dismiss the keyboard
        hideKeyboardAndListView()
    }

    private fun deleteDatabase() {
        val databaseName = "weather.db"
        val success = deleteDatabase(databaseName)
        if (success) {
            Toast.makeText(this, "Database deleted successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete database", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAndDisplayCityName(latitude: Double, longitude: Double){
        binding.textViewCityName.text = ""
        val addressRetrofit = Retrofit.Builder()
            .baseUrl("https://geocode.maps.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val addressNameService = addressRetrofit.create(AddressNameService::class.java)

        addressNameService.getAddressName(latitude, longitude,reverseGeocodeApiKey).enqueue(object : Callback<AddressNameResponse> {

            override fun onResponse(call: Call<AddressNameResponse>, response: Response<AddressNameResponse>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    res?.let {
                        val address = it.address
                        binding.textViewCityName.text = when {
                            address.village != null && address.county !=null -> "${address.village},\n${address.county}"
                            address.village != null -> address.village
                            address.town != null && address.county !=null -> "${address.town},\n${address.county}"
                            address.town != null -> address.town
                            address.city != null && address.county !=null -> "${address.city},\n${address.county}"
                            address.city != null -> address.city
                            address.county != null -> address.county
                            address.region != null -> address.region
                            address.stateDistrict != null -> address.stateDistrict
                            address.hamlet != null -> address.hamlet
                            address.suburb != null -> address.suburb
                            address.country != null -> address.country
                            else -> ""
                        }
                    }
                }
            }
            override fun onFailure(call: Call<AddressNameResponse>, t: Throwable) {
                binding.textViewCityName.text = "Failed to load data"
            }
        })
        hideKeyboardAndListView()
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

    private fun hideMain(){
        binding.useLocationStatusButton.visibility = View.GONE
        binding.textViewTemperature.visibility = View.GONE
        binding.textViewWindSpeed.visibility = View.GONE
        binding.textViewHumidity.visibility = View.GONE
        binding.textViewFeelsLike.visibility = View.GONE
        binding.imageViewWeather.visibility = View.GONE
        binding.textViewCityName.visibility = View.GONE
        binding.secondLayout.visibility = View.GONE
        binding.recyclerViewTemperatures.visibility = View.GONE
        binding.textViewTime.visibility = View.GONE
        binding.thirdLayout.visibility = View.GONE
        binding.dailyWeatherLayout.visibility = View.GONE
    }

    private fun showMain(){
        binding.useLocationStatusButton.visibility = View.VISIBLE
        binding.textViewTemperature.visibility = View.VISIBLE
        binding.textViewWindSpeed.visibility = View.VISIBLE
        binding.textViewHumidity.visibility = View.VISIBLE
        binding.textViewFeelsLike.visibility = View.VISIBLE
        binding.imageViewWeather.visibility = View.VISIBLE
        binding.textViewCityName.visibility = View.VISIBLE
        binding.secondLayout.visibility = View.VISIBLE
        binding.recyclerViewTemperatures.visibility = View.VISIBLE
        binding.textViewTime.visibility = View.VISIBLE
        binding.thirdLayout.visibility = View.VISIBLE
        binding.dailyWeatherLayout.visibility = View.VISIBLE
    }

    fun getWeatherIcon(weatherCode: Int, isDay:Int): Int {
        return when (weatherCode) {
            0 -> if (isDay==1) R.drawable.clear1 else R.drawable.clear0
            1 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            2 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            3 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            45 -> R.drawable.fog
            48 -> R.drawable.fog
            51 -> R.drawable.mc_cloudy
            53 -> R.drawable.mc_cloudy
            55 -> R.drawable.mc_cloudy
            56 -> R.drawable.mc_cloudy
            57 -> R.drawable.mc_cloudy
            61 -> R.drawable.rain
            63 -> R.drawable.rain
            65 -> R.drawable.rain
            66 -> R.drawable.sleet
            67 -> R.drawable.sleet
            71 -> R.drawable.l_snow
            73 -> R.drawable.l_snow
            75 -> R.drawable.l_snow
            77 -> R.drawable.hail
            80 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            81 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            82 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            85 -> if (isDay==1) R.drawable.l_snow1 else R.drawable.l_snow0
            86 -> if (isDay==1) R.drawable.l_snow1 else R.drawable.l_snow0
            95 -> R.drawable.tstorm
            96 -> R.drawable.tshower
            99 -> R.drawable.tshower
            else -> R.drawable.unknown
        }
    }

    private fun changeTheme(weatherCode: Int, isDay:Int) {
        if(weatherCode in 45..77 || weatherCode in 95..99){
            window.statusBarColor = ContextCompat.getColor(this, R.color.Rain_Blue)
            window.navigationBarColor = ContextCompat.getColor(this, R.color.Rain_Blue)
            val mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.Rain_Blue))
        }
        else if(isDay==0){
            window.statusBarColor = ContextCompat.getColor(this, R.color.Night_Blue)
            window.navigationBarColor = ContextCompat.getColor(this, R.color.Night_Blue)
            val mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.Night_Blue))
        }
        else{
            window.statusBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
            window.navigationBarColor = ContextCompat.getColor(this, R.color.Clear_Day_Blue)
            val mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
            mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.Clear_Day_Blue))
        }
    }

    private fun addWeatherData(container: LinearLayout, day: String, precipitationProbability: Int, maxTemperature: Double, minTemperature: Double) {
        val dayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        val textColor = ContextCompat.getColor(this, R.color.light)

        // Parse the date string to a LocalDate object
        val date = LocalDate.parse(day, DateTimeFormatter.ISO_DATE)
        // Get the day of the week and return it as a string
        var newDay: String? = null
        // Check if the date is today
        val today = LocalDate.now()
        if (date == today) {
            newDay = "Today"
        } else {
            // Get the day of the week and return it as a string in English
            newDay = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        }
        val dayView = TextView(this).apply {
            text = newDay
            setTextColor(textColor)
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.3f
            ).apply {
                setPadding(16, 0, 0, 0)
            }
        }

        val precipitationLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                .7f
            ).apply {
                setPadding(0, 8, 16, 0)
            }
        }

        val precipitationImage = ImageView(this).apply {
            setImageResource(R.drawable.raindrop)
            setSizeInDp(24f, 24f) // Set size in dp
        }

        val precipitationView = TextView(this).apply {
            text = "$precipitationProbability%"
            setTextColor(textColor)
            setPadding(8, 0, 0, 0)
        }

        precipitationLayout.addView(precipitationImage)
        precipitationLayout.addView(precipitationView)

        val maxTempView = TextView(this).apply {
            text = "${maxTemperature}°C"
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                .5f
            ).apply {
                setPadding(0, 0, 16, 0)
            }
        }

        val minTempView = TextView(this).apply {
            text = "${minTemperature}°C"
            setTextColor(textColor)
            layoutParams = LinearLayout.LayoutParams(
                0, // width 0 to distribute space based on weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                .5f
            ).apply {
                setPadding(0, 0, 16, 0)
            }
        }

        dayLayout.addView(dayView)
        dayLayout.addView(precipitationLayout)
        dayLayout.addView(maxTempView)
        dayLayout.addView(minTempView)

        container.addView(dayLayout)
    }

    private fun ImageView.setSizeInDp(widthDp: Float, heightDp: Float) {
        val widthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, Resources.getSystem().displayMetrics).toInt()
        val heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, Resources.getSystem().displayMetrics).toInt()
        layoutParams = LinearLayout.LayoutParams(widthPx, heightPx).apply {
            setMargins(0, 0, 8, 0) // Margin right
        }
    }

    private fun saveWeatherDataToSharedPreferences(current: Current, hourly: Hourly, daily: Daily) {
        val sharedPreferences = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Serialize the weather data to JSON strings
        val gson = Gson()
        val currentWeatherJson = gson.toJson(current)
        val hourlyWeatherJson = gson.toJson(hourly)
        val dailyWeatherJson = gson.toJson(daily)

        // Save the JSON strings to SharedPreferences
        editor.putString("current_weather", currentWeatherJson)
        editor.putString("hourly_weather", hourlyWeatherJson)
        editor.putString("daily_weather", dailyWeatherJson)

        editor.apply() // Apply changes asynchronously
    }

    private fun loadWeatherDataFromSharedPreferences() {
        val sharedPreferences = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)

        // Retrieve the JSON strings from SharedPreferences
        val gson = Gson()
        val currentWeatherJson = sharedPreferences.getString("current_weather", null)
        val hourlyWeatherJson = sharedPreferences.getString("hourly_weather", null)
        val dailyWeatherJson = sharedPreferences.getString("daily_weather", null)

        // Deserialize the JSON strings back into objects
        val currentWeather = gson.fromJson(currentWeatherJson, Current::class.java)
        val hourlyWeather = gson.fromJson(hourlyWeatherJson, Hourly::class.java)
        val dailyWeather = gson.fromJson(dailyWeatherJson, Daily::class.java)

        // Display the data if it's not null
        currentWeather?.let {
            binding.textViewTemperature.text = "${it.temperature} °C"
            binding.textViewHumidity.text = "${it.humidity} %"
            binding.textViewWindSpeed.text = "${it.windSpeed} km/h"
            binding.textViewFeelsLike.text = "${it.apparentTemperature} °C"
            val weatherIconResId = getWeatherIcon(weatherCode = it.weatherCode, isDay = it.isDay)
            binding.imageViewWeather.setImageResource(weatherIconResId)
            binding.textViewTime.text = formattedDateTime(it.time)

            changeTheme(it.weatherCode, it.isDay)
        }

        // Update the hourly and daily weather data similarly
        hourlyWeather?.let {
            val currentTime = LocalDateTime.parse(currentWeather?.time ?: "")
            val hourlyData = it.time.zip(it.temperature.zip(it.weatherCode.zip(it.isDay))) { time, triple ->
                val (temperature, pair) = triple
                val (weatherCode, isDay) = pair
                HourlyData(time, temperature, weatherCode, isDay)
            }.filter { data ->
                LocalDateTime.parse(data.time).isAfter(currentTime)
            }.take(24) // Take the 24 hours after the current time
            val temperatureAdapter = TemperatureAdapter(hourlyData)
            binding.recyclerViewTemperatures.adapter = temperatureAdapter
            binding.recyclerViewTemperatures.visibility = View.VISIBLE
        }

        dailyWeather?.let {
            binding.sunRiseTextView.text = it.sunrise[0].split('T')[1]
            binding.sunSetTextView.text = it.sunset[0].split('T')[1]

            binding.dailyWeatherLayout.removeAllViews()
            for (i in it.time.indices) {
                addWeatherData(
                    binding.dailyWeatherLayout,
                    it.time[i],
                    it.precipitationProbabilityMax[i],
                    it.temperatureMax[i],
                    it.temperatureMin[i]
                )
            }
            binding.dailyWeatherLayout.visibility = View.VISIBLE
        }
    }

    private fun checkLocationAndFetchData() {
        showLoading(isLoading = true)
        // Check if location permissions are granted
        if (isLocationPermissionGranted()) {
            // Check if location services are enabled
            if (isLocationEnabled()) {
                // Fetch the current location
                getLocation { latitude, longitude ->
                    if (latitude != null && longitude != null) {
                        // Trigger a new data fetch with the current location
                        fetchWeatherData(latitude, longitude)
                        fetchAndDisplayCityName(latitude,longitude)
                    } else {
                        // Handle location fetch failure
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
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

    private val executor = Executors.newSingleThreadExecutor()

    private  fun insertCurrentWeather(database: WeatherDatabase, current: Current) {
        executor.execute {
            try {
                Log.d("WeatherApp", "insertCurrentWeather is running on thread: ${Thread.currentThread().name}")
                val weatherDao = database.weatherDao()
                weatherDao.clearCurrentWeather()
                val entity = CurrentWeatherEntity(
                    time = current.time,
                    temperature = current.temperature,
                    humidity = current.humidity,
                    apparentTemperature = current.apparentTemperature,
                    isDay = current.isDay,
                    weatherCode = current.weatherCode,
                    windSpeed = current.windSpeed
                )
                weatherDao.insertCurrentWeather(entity)
                Log.d("WeatherApp", "insertCurrentWeather: Insertion successful")
            } catch (e: Exception) {
                Log.e("WeatherApp", "insertCurrentWeather: Insertion failed", e)
            }
        }
    }

    fun logCurrentWeather(database: WeatherDatabase) {
        executor.execute {
            try {
                val weatherDao = database.weatherDao()
                val currentWeather = weatherDao.getCurrentWeather()
                if (currentWeather != null) {
                    Log.d("CurrentWeatherLog", "Current Weather Data:")
                    Log.d("CurrentWeatherLog", "Time: ${currentWeather.time}")
                    Log.d("CurrentWeatherLog", "Temperature: ${currentWeather.temperature}")
                    Log.d("CurrentWeatherLog", "Humidity: ${currentWeather.humidity}")
                    Log.d("CurrentWeatherLog", "Apparent Temperature: ${currentWeather.apparentTemperature}")
                    Log.d("CurrentWeatherLog", "Is Day: ${currentWeather.isDay}")
                    Log.d("CurrentWeatherLog", "Weather Code: ${currentWeather.weatherCode}")
                    Log.d("CurrentWeatherLog", "Wind Speed: ${currentWeather.windSpeed}")
                    Log.d("CurrentWeatherLog", "----------------------")
                } else {
                    Log.d("CurrentWeatherLog", "No current weather data available.")
                }
            } catch (e: Exception) {
                Log.e("CurrentWeatherLog", "Error fetching current weather data", e)
            }
        }
    }

    fun insertDailyWeather(database: WeatherDatabase, daily: Daily) {
        executor.execute {
            try {
                Log.d("WeatherApp", "insertDailyWeather is running on thread: ${Thread.currentThread().name}")
                val weatherDao = database.weatherDao()

                // Clear existing entries
                weatherDao.clearDailyWeather()
                Log.d("WeatherApp", "Daily weather table cleared")

                // Prepare and insert new entries
                val entities = daily.time.indices.map { i ->
                    DailyWeatherEntity(
                        date = daily.time[i],
                        weatherCode = daily.weatherCode[i],
                        maxTemperature = daily.temperatureMax[i],
                        minTemperature = daily.temperatureMin[i],
                        sunrise = daily.sunrise[i],
                        sunset = daily.sunset[i],
                        precipitationProbability = daily.precipitationProbabilityMax[i]
                    )
                }
                weatherDao.insertDailyWeather(entities)
                Log.d("WeatherApp", "insertDailyWeather: Insertion successful")
            } catch (e: Exception) {
                Log.e("WeatherApp", "insertDailyWeather: Insertion failed", e)
            }
        }
    }

    fun logDailyWeather(database: WeatherDatabase) {
        executor.execute {
            try {
                val weatherDao = database.weatherDao()
                val dailyWeatherList = weatherDao.getDailyWeather()
                if (dailyWeatherList.isNotEmpty()) {
                    Log.d("DailyWeatherLog", "Daily Weather Data:")
                    dailyWeatherList.forEach { dailyWeather ->
                        Log.d("DailyWeatherLog", "Date: ${dailyWeather.date}")
                        Log.d("DailyWeatherLog", "Weather Code: ${dailyWeather.weatherCode}")
                        Log.d("DailyWeatherLog", "Max Temperature: ${dailyWeather.maxTemperature}")
                        Log.d("DailyWeatherLog", "Min Temperature: ${dailyWeather.minTemperature}")
                        Log.d("DailyWeatherLog", "Sunrise: ${dailyWeather.sunrise}")
                        Log.d("DailyWeatherLog", "Sunset: ${dailyWeather.sunset}")
                        Log.d("DailyWeatherLog", "Precipitation Probability: ${dailyWeather.precipitationProbability}")
                        Log.d("DailyWeatherLog", "----------------------")
                    }
                } else {
                    Log.d("DailyWeatherLog", "No daily weather data available.")
                }
            } catch (e: Exception) {
                Log.e("DailyWeatherLog", "Error fetching daily weather data", e)
            }
        }
    }

    fun insertHourlyWeather(database: WeatherDatabase, hourly: Hourly, currentTime: String) {
        executor.execute {
            try {
                Log.d("WeatherApp", "insertHourlyWeather is running on thread: ${Thread.currentThread().name}")
                val weatherDao = database.weatherDao()

                // Clear existing entries
                weatherDao.clearHourlyWeather()
                Log.d("WeatherApp", "Hourly weather table cleared")

                // Prepare and filter data for the next 24 hours
                val currentDateTime = LocalDateTime.parse(currentTime)
                val hourlyDataForNext24Hours = hourly.time.zip(hourly.temperature.zip(hourly.weatherCode.zip(hourly.isDay))) { time, triple ->
                    val (temperature, pair) = triple
                    val (weatherCode, isDay) = pair
                    HourlyData(time, temperature, weatherCode, isDay)
                }.filter { data ->
                    LocalDateTime.parse(data.time).isAfter(currentDateTime)
                }.take(24) // Take the next 24 hours

                // Convert filtered data to entities and insert into database
                val entities = hourlyDataForNext24Hours.map { data ->
                    HourlyWeatherEntity(
                        time = data.time,
                        temperature = data.temperature,
                        weatherCode = data.weatherCode,
                        isDay = data.isDay
                    )
                }
                weatherDao.insertHourlyWeather(entities)
                Log.d("WeatherApp", "insertHourlyWeather: Insertion successful")
            } catch (e: Exception) {
                Log.e("WeatherApp", "insertHourlyWeather: Insertion failed", e)
            }
        }
    }

    fun logHourlyWeather(database: WeatherDatabase) {
        executor.execute {
            try {
                val weatherDao = database.weatherDao()
                val hourlyWeatherList = weatherDao.getHourlyWeather()
                if (hourlyWeatherList.isNotEmpty()) {
                    Log.d("HourlyWeatherLog", "Hourly Weather Data:")
                    hourlyWeatherList.forEach { hourlyWeather ->
                        Log.d("HourlyWeatherLog", "Time: ${hourlyWeather.time}")
                        Log.d("HourlyWeatherLog", "Temperature: ${hourlyWeather.temperature}")
                        Log.d("HourlyWeatherLog", "Weather Code: ${hourlyWeather.weatherCode}")
                        Log.d("HourlyWeatherLog", "Is Day: ${hourlyWeather.isDay}")
                        Log.d("HourlyWeatherLog", "----------------------")
                    }
                } else {
                    Log.d("HourlyWeatherLog", "No hourly weather data available.")
                }
            } catch (e: Exception) {
                Log.e("HourlyWeatherLog", "Error fetching hourly weather data", e)
            }
        }
    }

}