package com.example.myweather.features.weather

import android.app.Activity
import android.app.Application
import android.view.View
import com.example.myweather.R
import com.example.myweather.databinding.ActivityMainBinding
import com.example.myweather.features.weather.ui.ExpandableSearchHeader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ExpandableSearchHeaderTest {
    @Test fun searchUsesTheHeaderWidthWithoutMovingContentAndRestoresTheLocationOnClose() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val binding = ActivityMainBinding.inflate(activity.layoutInflater)
            activity.setContentView(binding.root)
            binding.textViewCityName.text = "San Fernando del Valle de Catamarca"
            val ui = ExpandableSearchHeader(binding)
            val density = activity.resources.displayMetrics.density
            val width = (360 * density).toInt()
            val height = (800 * density).toInt()
            fun layout() {
                binding.root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                binding.root.layout(0, 0, width, height)
            }
            try {
                layout()
                val originalHeight = binding.weatherHeader.height
                val compactWidth = binding.searchSurface.width
                ui.setExpanded(true, animate = false)
                layout()
                assertEquals(originalHeight, binding.weatherHeader.height)
                assertEquals(binding.weatherHeader.width - binding.weatherHeader.paddingLeft - binding.weatherHeader.paddingRight,
                    binding.searchSurface.width)
                assertEquals(View.INVISIBLE, binding.locationHeader.visibility)
                assertEquals(View.VISIBLE, binding.searchRow.visibility)
                assertEquals(activity.getString(R.string.close_search), binding.searchButton.contentDescription)
                assertTrue(binding.editTextLocation.hasFocus())
                ui.setExpanded(false, animate = false)
                layout()
                assertEquals(originalHeight, binding.weatherHeader.height)
                assertEquals(compactWidth, binding.searchSurface.width)
                assertEquals(View.VISIBLE, binding.locationHeader.visibility)
                assertEquals(1f, binding.locationHeader.alpha)
                assertEquals(View.GONE, binding.searchRow.visibility)
                assertEquals(activity.getString(R.string.search_city), binding.searchButton.contentDescription)
                assertFalse(binding.editTextLocation.hasFocus())
            } finally {
                ui.dispose()
            }
        }
    }
}
