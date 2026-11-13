package com.example.myweather.features.weather

import android.app.Application
import com.airbnb.lottie.LottieCompositionFactory
import com.example.myweather.R
import com.example.myweather.features.weather.ui.WeatherIcons
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WeatherIconsTest {
    @Test fun everyBundledWeatherAnimationLoadsWithoutExternalImages() {
        val context = RuntimeEnvironment.getApplication()
        val resources = R.raw::class.java.fields.filter { it.name.startsWith("weather_") }
        assertTrue(resources.isNotEmpty())
        resources.forEach { field ->
            val result = LottieCompositionFactory.fromRawResSync(context, field.getInt(null))
            assertNotNull("${field.name}: ${result.exception}", result.value)
            val composition = result.value!!
            assertTrue(field.name, composition.duration > 0)
            assertTrue(field.name, composition.bounds.width() > 0 && composition.bounds.height() > 0)
            assertTrue("${field.name} needs external images", composition.images.isEmpty())
        }
    }

    @Test fun conditionsRemainDistinctAndHaveDayNightArtwork() {
        assertNotEquals(WeatherIcons.resource(0, 1), WeatherIcons.resource(0, 0))
        assertNotEquals(WeatherIcons.resource(1, 1), WeatherIcons.resource(3, 1))
        assertNotEquals(WeatherIcons.resource(61, 1), WeatherIcons.resource(66, 1))
        assertNotEquals(WeatherIcons.resource(95, 1), WeatherIcons.resource(96, 1))
        assertEquals(R.raw.weather_not_available, WeatherIcons.resource(-1, 1))
        assertEquals(R.string.condition_heavy_storm, com.example.myweather.features.weather.ui.WeatherTheme.description(97, 1))
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 97, 99)
        codes.forEach { code ->
            assertNotEquals("day code $code", R.raw.weather_not_available, WeatherIcons.resource(code, 1))
            assertNotEquals("night code $code", R.raw.weather_not_available, WeatherIcons.resource(code, 0))
        }
    }
}
