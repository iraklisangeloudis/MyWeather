package com.example.myweather.features.weather

import com.example.myweather.features.weather.ui.background.SkyCondition
import com.example.myweather.features.weather.ui.background.WeatherSkyScene
import org.junit.Assert.*
import org.junit.Test

class WeatherSkySceneTest {
    @Test fun weatherCodesSelectTheAppropriateAtmosphere() {
        val groups = mapOf(
            SkyCondition.CLEAR to listOf(0, 1),
            SkyCondition.CLOUDY to listOf(2, 3),
            SkyCondition.FOG to listOf(45, 48),
            SkyCondition.RAIN to listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82),
            SkyCondition.SNOW to listOf(71, 73, 75, 77, 85, 86),
            SkyCondition.STORM to listOf(95, 96, 97, 99)
        )
        groups.forEach { (condition, codes) -> codes.forEach { code ->
            assertEquals("Weather code $code", condition, WeatherSkyScene.fromWeather(code, 1, false).condition)
        } }
    }

    @Test fun forecastDayNightTakesPriorityOverDeviceTheme() {
        listOf(0, 3, 45, 61, 71, 95).forEach { code ->
            assertTrue(WeatherSkyScene.fromWeather(code, 0, false).isNight)
            assertFalse(WeatherSkyScene.fromWeather(code, 1, true).isNight)
        }
    }

    @Test fun missingOrUnknownWeatherKeepsAQuietBackground() {
        listOf(null, -1, 1000).forEach { code ->
            val scene = WeatherSkyScene.fromWeather(code, null, true)
            assertEquals(SkyCondition.STILL, scene.condition)
            assertTrue(scene.isNight)
            assertEquals(0f, scene.cloudCover)
        }
    }
}
