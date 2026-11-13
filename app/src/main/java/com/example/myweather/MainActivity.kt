package com.example.myweather

import android.Manifest
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.features.location.domain.GetCurrentLocationUseCase
import com.example.myweather.features.weather.ui.TemperatureAdapter
import com.example.myweather.features.weather.ui.WeatherUI
import com.example.myweather.features.weather.ui.WeatherViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModel()
    private val getCurrentLocation: GetCurrentLocationUseCase by inject()
    private val locationRequests = Channel<LocationRequest>(Channel.CONFLATED)
    private lateinit var renderer: WeatherUI
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var isKeyboardVisible = false
    private var backPressCount = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderer = WeatherUI(binding, this, window) { location ->
            locationRequests.trySend(LocationRequest.Selected(location.latitude, location.longitude))
        }
        setupRecyclerView()
        setupTheme()
        collectViewState()
        collectLocationRequests()
        setupListeners()
        if (savedInstanceState == null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkLocationAndFetchData()
        }
    }

    override fun onBackPressed() {
        if (isKeyboardVisible || binding.listViewLocations.visibility == View.VISIBLE) {
            // Dismiss the keyboard and list view
            viewModel.searchLocations("")
            renderer.hideKeyboardAndListView()
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
        renderer.showMain()
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

    private fun collectViewState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect { state ->
                    renderer.render(state)
                    state.message?.let { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.messageShown(message)
                    }
                }
            }
        }
    }

    private fun collectLocationRequests() {
        lifecycleScope.launch {
            locationRequests.receiveAsFlow().collectLatest { request ->
                when (request) {
                    LocationRequest.Device -> lookupDeviceLocation()
                    is LocationRequest.Selected -> viewModel.selectLocation(request.latitude, request.longitude)
                }
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
        locationRequests.trySend(LocationRequest.Device)
    }

    private suspend fun lookupDeviceLocation() {
        viewModel.locationLookupStarted()
        try {
            when (val result = getCurrentLocation()) {
                GetCurrentLocationUseCase.Result.PermissionRequired -> ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                GetCurrentLocationUseCase.Result.Disabled -> viewModel.locationLookupFinished("Location disabled")
                GetCurrentLocationUseCase.Result.Unavailable -> viewModel.locationLookupFinished("Unable to get location")
                is GetCurrentLocationUseCase.Result.Located ->
                    viewModel.selectLocation(result.coordinates.latitude, result.coordinates.longitude)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            viewModel.locationLookupFinished("Unable to get location")
        } finally {
            viewModel.locationLookupFinished()
        }
    }

    private sealed interface LocationRequest {
        data object Device : LocationRequest
        data class Selected(val latitude: Double, val longitude: Double) : LocationRequest
    }

    override fun onDestroy() {
        locationRequests.close()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

}
