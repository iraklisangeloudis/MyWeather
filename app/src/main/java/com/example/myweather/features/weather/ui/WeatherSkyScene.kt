package com.example.myweather.features.weather.ui

enum class SkyCondition { STILL, CLEAR, CLOUDY, FOG, RAIN, SNOW, STORM }

/** Describes the weather already on screen; this never fetches data or uses device location. */
data class WeatherSkyScene(val condition: SkyCondition, val isNight: Boolean, val cloudCover: Float = 0f) {
    companion object {
        fun fromWeather(code: Int?, isDay: Int?, systemNight: Boolean): WeatherSkyScene {
            val night = isDay?.let { it == 0 } ?: systemNight
            val condition = when (code) {
                0, 1 -> SkyCondition.CLEAR
                2, 3 -> SkyCondition.CLOUDY
                45, 48 -> SkyCondition.FOG
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> SkyCondition.RAIN
                71, 73, 75, 77, 85, 86 -> SkyCondition.SNOW
                95, 96, 97, 99 -> SkyCondition.STORM
                else -> SkyCondition.STILL
            }
            val cover = when (code) {
                0 -> 0f
                1 -> .2f
                2 -> .45f
                3 -> .8f
                else -> if (condition == SkyCondition.STILL) 0f else .65f
            }
            return WeatherSkyScene(condition, night, cover)
        }
    }
}
