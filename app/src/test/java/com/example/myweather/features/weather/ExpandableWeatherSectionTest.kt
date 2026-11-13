package com.example.myweather.features.weather

import android.app.Activity
import android.app.Application
import android.view.View
import com.example.myweather.R
import com.example.myweather.databinding.ActivityMainBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ExpandableWeatherSectionTest {
    @Test fun expansionReversesWithoutChangingTheMeasuredFullContentSize() {
        Robolectric.buildActivity(Activity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.setTheme(R.style.Theme_MyWeather)
            val section = ActivityMainBinding.inflate(activity.layoutInflater).metricsSection
            section.visibility = View.VISIBLE
            val width = View.MeasureSpec.makeMeasureSpec(390, View.MeasureSpec.EXACTLY)
            val height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            section.setExpansion(0f)
            section.measure(width, height)
            assertEquals(section.compactHeight, section.measuredHeight)
            assertFalse(section.performClick())
            assertFalse(section.isClickable)
            val fullHeight = section.expandedHeight
            section.setExpansion(.5f)
            section.measure(width, height)
            assertTrue(section.measuredHeight in (section.compactHeight + 1) until fullHeight)
            assertEquals(fullHeight, section.expandedHeight)
            section.setExpansion(1f)
            section.measure(width, height)
            assertEquals(fullHeight, section.measuredHeight)
            section.setExpansion(0f)
            section.measure(width, height)
            assertEquals(section.compactHeight, section.measuredHeight)
            section.setExpansion(0f, staticLayout = true)
            section.measure(width, height)
            assertEquals(fullHeight, section.measuredHeight)
        }
    }
}
