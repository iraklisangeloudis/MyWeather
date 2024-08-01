package com.example.myweather

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

interface LocationIQService {
    @GET("v1/autocomplete.php")
    fun getAutocomplete(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("format") format: String = "json"
    ): Call<List<LocationResponse>>
}

data class LocationResponse(
    @SerializedName("place_id") val placeId: String,
    @SerializedName("lat") val latitude: String,
    @SerializedName("lon") val longitude: String,
    @SerializedName("display_name") val displayName: String
)

interface WeatherService {
    @GET("v1/forecast")
    fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("hourly") hourly: String,
        @Query("daily") daily: String,
        @Query("timezone") timezone: String = "auto"
    ): Call<WeatherResponse>
}

data class WeatherResponse(
    @SerializedName("current") val current: Current,
    @SerializedName("hourly") val hourly: Hourly,
    @SerializedName("daily") val daily: Daily
)

data class Current(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double,
    @SerializedName("is_day") val isDay: Int,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed: Double
)

data class Hourly(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperature: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("is_day") val isDay: List<Int>
)

data class Daily(
    @SerializedName("time") val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerializedName("sunrise") val sunrise: List<String>,
    @SerializedName("sunset") val sunset: List<String>,
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>
)

interface AddressNameService {
    @GET("reverse")
    //https://geocode.maps.co/reverse
    fun getAddressName(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("key") apiKey: String
    ): Call<AddressNameResponse>
}

data class AddressNameResponse(
    @SerializedName("address") val address: Address
)

data class Address(
    @SerializedName("hamlet") val hamlet: String? = null,
    @SerializedName("village") val village: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("town") val town: String? = null,
    @SerializedName("county") val county: String?= null,
    @SerializedName("suburb") val suburb: String?= null,
    @SerializedName("region") val region: String?= null,
    @SerializedName("state_district") val stateDistrict: String?= null,
    @SerializedName("country") val country: String?= null,
    @SerializedName("country_code") val countryCode: String? = null
)

class TemperatureAdapter(private val hourlyData: List<HourlyData>) : RecyclerView.Adapter<TemperatureAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.textViewItemTime)
        val weatherImageView: ImageView = view.findViewById(R.id.imageViewItemWeather)
        val temperatureTextView: TextView = view.findViewById(R.id.textViewItemTemperature)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_temperature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = hourlyData[position]
        // Parse the time string to LocalDateTime
        val dateTime = LocalDateTime.parse(data.time)
        // Format the time part
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val formattedTime = dateTime.format(timeFormatter)
        // Set the formatted time to the TextView
        holder.timeTextView.text = formattedTime

        holder.temperatureTextView.text = "${data.temperature}°C"

        val weatherIconResId = getWeatherIcon(weatherCode = data.weatherCode, isDay = data.isDay)
        holder.weatherImageView.setImageResource(weatherIconResId)
    }

    override fun getItemCount() = hourlyData.size

    private fun getWeatherIcon(weatherCode: Int, isDay:Int): Int {
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
}

data class HourlyData(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Int
)

class MainActivity : AppCompatActivity() {

    private lateinit var editTextLocation: EditText
    private lateinit var listViewLocations: ListView
    private lateinit var textViewTemperature: TextView
    private lateinit var textViewWindSpeed: TextView
    private lateinit var useLocationStatusButton: Button
    private lateinit var textViewHumidity: TextView
    private lateinit var textViewFeelsLike: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var imageViewWeather: ImageView
    private lateinit var textViewCityName: TextView
    private lateinit var textViewTime: TextView
    private lateinit var secondLayout: LinearLayout
    private lateinit var recyclerViewTemperatures: RecyclerView
    private lateinit var sunRiseTextView: TextView
    private lateinit var sunSetTextView: TextView
    private lateinit var thirdLayout: LinearLayout
    private lateinit var dailyWeatherLayout: LinearLayout

    private lateinit var locationManager: LocationManager
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private val locationIQApiKey = BuildConfig.LOCATION_IQ_API_KEY
    private val reverseGeocodeApiKey = BuildConfig.REVERSE_GEOCODE_API_KEY

    private var isKeyboardVisible = false
    private var backPressCount = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextLocation = findViewById(R.id.editTextLocation)
        listViewLocations = findViewById(R.id.listViewLocations)
        textViewTemperature = findViewById(R.id.textViewTemperature)
        textViewWindSpeed = findViewById(R.id.textViewWindSpeed)
        useLocationStatusButton = findViewById(R.id.useLocationStatusButton)
        progressBar = findViewById(R.id.progressBar)
        textViewHumidity = findViewById(R.id.textViewHumidity)
        textViewFeelsLike = findViewById(R.id.textViewFeelsLike)
        imageViewWeather = findViewById(R.id.imageViewWeather)
        textViewCityName = findViewById(R.id.textViewCityName)
        textViewTime = findViewById(R.id.textViewTime)
        secondLayout = findViewById(R.id.secondLayout)
        sunRiseTextView = findViewById(R.id.sunRiseTextView)
        sunSetTextView = findViewById(R.id.sunSetTextView)
        thirdLayout = findViewById(R.id.thirdLayout)
        dailyWeatherLayout = findViewById(R.id.dailyWeatherLayout)

        recyclerViewTemperatures = findViewById(R.id.recyclerViewTemperatures)
        recyclerViewTemperatures.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Initialize recyclerViewTemperatures with an empty list
        var temperatureAdapter = TemperatureAdapter(listOf())
        recyclerViewTemperatures.adapter = temperatureAdapter

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

        useLocationStatusButton.setOnClickListener {
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
            editTextLocation.clearFocus()
        }

        editTextLocation.addTextChangedListener(object : TextWatcher {
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
                                    listViewLocations.adapter = adapter
                                    hideMain()
                                    listViewLocations.visibility = View.VISIBLE
                                    listViewLocations.setOnItemClickListener { _, _, position, _ ->
                                        val selectedLocation = locations[position]
                                        fetchWeatherData(selectedLocation.latitude.toDouble(), selectedLocation.longitude.toDouble())
                                        fetchAndDisplayCityName(selectedLocation.latitude.toDouble(), selectedLocation.longitude.toDouble())
                                        hideKeyboardAndListView()
                                        editTextLocation.setText("")
                                        showMain()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<List<LocationResponse>>, t: Throwable) {
                                Toast.makeText(this@MainActivity, "Failed to load location data", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        editTextLocation.clearFocus()
                        useLocationStatusButton.visibility = View.VISIBLE
                        hideKeyboardAndListView()
                        showMain()
                    }
                }
            }
        })

        //End of onCreate function
    }

    override fun onBackPressed() {
        if (isKeyboardVisible || listViewLocations.visibility == View.VISIBLE) {
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
        editTextLocation.clearFocus()
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
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
                        textViewTemperature.text = "${it.current.temperature} °C"
                        textViewHumidity.text = "${it.current.humidity} %"
                        textViewWindSpeed.text = "${it.current.windSpeed} km/h"
                        textViewFeelsLike.text = "${it.current.apparentTemperature} °C"
                        val weatherIconResId = getWeatherIcon(weatherCode = it.current.weatherCode, isDay = it.current.isDay)
                        imageViewWeather.setImageResource(weatherIconResId)
                        textViewTime.text = formattedDateTime(it.current.time)

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
                        recyclerViewTemperatures.adapter = temperatureAdapter
                        recyclerViewTemperatures.visibility = View.VISIBLE

                        sunRiseTextView.text= it.daily.sunrise[0].split('T')[1]
                        sunSetTextView.text= it.daily.sunset[0].split('T')[1]

                        // Clear the container to remove previous data
                        dailyWeatherLayout.removeAllViews()
                        for (i in it.daily.time.indices) {
                            addWeatherData(
                                dailyWeatherLayout,
                                it.daily.time[i],
                                it.daily.precipitationProbabilityMax[i],
                                it.daily.temperatureMax[i],
                                it.daily.temperatureMin[i]
                            )
                        }
                        dailyWeatherLayout.visibility=View.VISIBLE
                    }
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                textViewTemperature.text = "--°C"
                textViewHumidity.text = "--%"
                textViewWindSpeed.text = "--km/h"
                textViewFeelsLike.text = "--°C"
                imageViewWeather.setImageResource(getWeatherIcon(weatherCode = -1, isDay = -1))
                textViewTime.text = ""
                recyclerViewTemperatures.visibility = View.GONE
                dailyWeatherLayout.visibility=View.GONE
                sunRiseTextView.text= "-"
                sunSetTextView.text= "-"
            }
        })
        showLoading(isLoading = false)
        // Dismiss the keyboard
        hideKeyboardAndListView()
    }

    private fun fetchAndDisplayCityName(latitude: Double, longitude: Double){
        textViewCityName.text = ""
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
                        textViewCityName.text = when {
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
                textViewCityName.text = "Failed to load data"
            }
        })
        hideKeyboardAndListView()
    }

    private fun hideKeyboardAndListView() {
        // Hide the keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTextLocation.windowToken, 0)
        // Hide the list view
        listViewLocations.visibility = View.GONE
        // Clear the adapter to ensure no residual data
        listViewLocations.adapter = null
    }

    private fun hideMain(){
        useLocationStatusButton.visibility = View.GONE
        textViewTemperature.visibility = View.GONE
        textViewWindSpeed.visibility = View.GONE
        textViewHumidity.visibility = View.GONE
        textViewFeelsLike.visibility = View.GONE
        imageViewWeather.visibility = View.GONE
        textViewCityName.visibility = View.GONE
        secondLayout.visibility = View.GONE
        recyclerViewTemperatures.visibility = View.GONE
        textViewTime.visibility = View.GONE
        thirdLayout.visibility = View.GONE
        dailyWeatherLayout.visibility = View.GONE
    }

    private fun showMain(){
        useLocationStatusButton.visibility = View.VISIBLE
        textViewTemperature.visibility = View.VISIBLE
        textViewWindSpeed.visibility = View.VISIBLE
        textViewHumidity.visibility = View.VISIBLE
        textViewFeelsLike.visibility = View.VISIBLE
        imageViewWeather.visibility = View.VISIBLE
        textViewCityName.visibility = View.VISIBLE
        secondLayout.visibility = View.VISIBLE
        recyclerViewTemperatures.visibility = View.VISIBLE
        textViewTime.visibility = View.VISIBLE
        thirdLayout.visibility = View.VISIBLE
        dailyWeatherLayout.visibility = View.VISIBLE
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
            //val color = ContextCompat.getColor(applicationContext, R.color.Clear_Day_Blue)
            //setOpaqueLayout(color)
        }
    }

//    private fun setOpaqueLayout(color: Int){
//        secondLayout = findViewById(R.id.secondLayout)
//        // Retrieve the current drawable
//        val backgroundDrawable = ContextCompat.getDrawable(this, R.drawable.rounded_border) as GradientDrawable
//        // Set the color in the drawable
//        val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.ADD)
//        backgroundDrawable.colorFilter = colorFilter
//        // Apply the modified drawable as the background
//        secondLayout.background = backgroundDrawable
//        // Manually set padding (ensure this matches the padding defined in your XML)
//        val paddingInDp = 8 // Replace with the actual padding value you want
//        val scale = resources.displayMetrics.density
//        val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
//        secondLayout.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
//    }

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

}
